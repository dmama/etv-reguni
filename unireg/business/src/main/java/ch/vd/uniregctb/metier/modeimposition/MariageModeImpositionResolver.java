package ch.vd.uniregctb.metier.modeimposition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import org.apache.commons.lang.mutable.MutableBoolean;

/**
 * Resolver du mode d'imposition pour le cas mariage.
 * 
 * @author Pavel BLANCO
 *
 */
public class MariageModeImpositionResolver extends TiersModeImpositionResolver {

	private final Long numeroEvenement;

	public MariageModeImpositionResolver(TiersService tiersService, Long numeroEvenement) {
		super(tiersService);
		this.numeroEvenement = numeroEvenement;
	}

	/**
	 * Calcule le nouveau mode d'imposition lors d'un mariage.

	 * @param contribuable le nouveau MenageCommun
	 * @param date la date de mariage
	 * @param imposition ignoré car un nouveau ménage commun ne doit pas avoir un mode d'imposition valide.
	 * @return le nouveau mode d'imposition
	 * 
	 * @see ch.vd.uniregctb.metier.modeimposition.ModeImpositionResolver#resolve(ch.vd.uniregctb.tiers.Contribuable, ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.type.ModeImposition)
	 */
	public Imposition resolve(Contribuable contribuable, RegDate date, ModeImposition imposition) throws ModeImpositionResolverException {
		if (!(contribuable instanceof MenageCommun)) {
			throw new ModeImpositionResolverException("Le contribuable n° " + FormatNumeroHelper.numeroCTBToDisplay(contribuable.getNumero()) + " n'est pas un ménage commun");
		}

		MenageCommun menageCommun = (MenageCommun) contribuable;
		
		EnsembleTiersCouple ensemble = getTiersService().getEnsembleTiersCouple(menageCommun, date);
		
		PersonnePhysique principal = ensemble.getPrincipal();
		PersonnePhysique conjoint = ensemble.getConjoint();
		
		if (principal != null && conjoint != null) {
			return resolveCouple(principal, conjoint, date);
		}
		else {
			return resolveSeul(principal != null ? principal : conjoint, date);
		}
	}

	private Imposition resolveSeul(PersonnePhysique principal, RegDate date) throws ModeImpositionResolverException {
		Audit.info(numeroEvenement, "Mariage seul détecté");
		final ForFiscalPrincipal forFPPrincipal = principal.getForFiscalPrincipalAt(null);
		/*
		 * le contribuable est assujetti
		 */
		if (forFPPrincipal != null) {
			final ModeImposition modeImpositionContribuable = forFPPrincipal.getModeImposition();
			boolean etrangerSansPermisC = false;
			try {
				etrangerSansPermisC = getTiersService().isEtrangerSansPermisC(principal, forFPPrincipal.getDateDebut());
			}
			catch (TiersException e) {
				// ne rien faire si le permis ne peut pas être déterminé
				// le test de cohérence ne sera pas effectué
			}
			
			return new Imposition(date, modeImpositionContribuable);
		}
		
		return null;
	}

	private ModeImposition getModeImposition(PersonnePhysique pp, RegDate date, MutableBoolean sansFor) throws TiersException {
		final ForFiscalPrincipal ffp = pp.getForFiscalPrincipalAt(null);
		final ModeImposition modeImposition;
		if (ffp != null) {
			modeImposition = ffp.getModeImposition();
			sansFor.setValue(false);
		}
		else {
			// s'il est suisse, titulaire d'un permis C ou a obtenu le statut de réfugié => ordianire
			if (!getTiersService().isEtrangerSansPermisC(pp, date) || (pp.isHabitant() && getTiersService().isHabitantRefugie(pp, date))) {
				modeImposition = ModeImposition.ORDINAIRE;
			}
			else {
				modeImposition = ModeImposition.SOURCE;
			}
			sansFor.setValue(true);
		}
		return modeImposition;
	}

	private Imposition resolveCouple(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date) throws ModeImpositionResolverException {
		final MutableBoolean principalSansFor = new MutableBoolean(false);
		final MutableBoolean conjointSansFor = new MutableBoolean(false);

		try {
			final ModeImposition impositionPrincipal = getModeImposition(principal, date, principalSansFor);
			final ModeImposition impositionConjoint = getModeImposition(conjoint, date, conjointSansFor);

			if (principalSansFor.booleanValue() && conjointSansFor.booleanValue()) {
				Audit.info(numeroEvenement, "les 2 maries ne sont pas assujetti : aucune action sur les fors");
				return null;
			}

			return internalResolveCouple(date, impositionPrincipal, impositionConjoint);
		}
		catch (TiersException e) {
			throw new ModeImpositionResolverException("Impossible de déterminer le mode d'imposition requis", e);
		}
	}
	
	protected Imposition internalResolveCouple(RegDate date, ModeImposition impositionContribuable, ModeImposition impositionConjoint) throws ModeImpositionResolverException {
		
		/*
		 * Paramétrage de la catégorie de l'impôt ordre de priorité : dépense, ordinaire, mixte loi, mixte pratique, source
		 */
		ModeImposition modeImposition;
		
		String logAudit = "Le nouveau for fiscal principal pour le tiers ménage commun aura le role ";
		if (isAuMoinsUnDepense(impositionContribuable, impositionConjoint)) {
			modeImposition = ModeImposition.DEPENSE;
			logAudit += "dépense";
		}
		else if (isAuMoinsUncontribuableOrdinaire(impositionContribuable, impositionConjoint)) {
			modeImposition = ModeImposition.ORDINAIRE;
			logAudit += "ordinaire";
		}
		else if (isAuMoinsUnIndigent(impositionContribuable, impositionConjoint)) {
			if (isIndigent(impositionContribuable) && isIndigent(impositionConjoint)) {
				modeImposition = ModeImposition.INDIGENT;
				logAudit += "indigent";
			}
			else {
				modeImposition = ModeImposition.ORDINAIRE;
				logAudit += "ordinaire";
			}
		}
		else if (isAuMoinsUnSourcierMixteLoi(impositionContribuable, impositionConjoint)) {
			modeImposition = ModeImposition.MIXTE_137_1;
			logAudit += "mixte_tou";
		}
		else if (isAuMoinsUnSourcierMixtePratique(impositionContribuable, impositionConjoint)) {
			modeImposition = ModeImposition.MIXTE_137_2;
			logAudit += "mixte_toc";
		}
		else if (isAuMoinsUnSourcierPur(impositionContribuable, impositionConjoint)) {
			modeImposition = ModeImposition.SOURCE;
			logAudit += "source";
		}
		else { // les deux sont sans revenu ni fortune => ordinaire
			modeImposition = ModeImposition.ORDINAIRE;
			logAudit += "ordinaire";
		}
		Audit.info(numeroEvenement, logAudit);
		
		return new Imposition(date, modeImposition);
	}

	private boolean isAuMoinsUncontribuableOrdinaire(ModeImposition impositionPrincipal, ModeImposition impositionConjoint) {
		return isOrdinaire(impositionPrincipal) || isOrdinaire(impositionConjoint);
	}

	private boolean isAuMoinsUnDepense(ModeImposition impositionPrincipal, ModeImposition impositionConjoint) {
		return isDepense(impositionPrincipal) || isDepense(impositionConjoint);
	}
	
	private boolean isAuMoinsUnSourcierPur(ModeImposition impositionPrincipal, ModeImposition impositionConjoint) {
		return isSourcierPur(impositionPrincipal) || isSourcierPur(impositionConjoint);
	}

	private boolean isAuMoinsUnSourcierMixteLoi(ModeImposition impositionPrincipal, ModeImposition impositionConjoint) {
		return isSourcierMixteLoi(impositionPrincipal) || isSourcierMixteLoi(impositionConjoint);
	}

	private boolean isAuMoinsUnSourcierMixtePratique(ModeImposition impositionPrincipal, ModeImposition impositionConjoint) {
		return isSourcierMixtePratique(impositionPrincipal) || isSourcierMixtePratique(impositionConjoint);
	}

	private boolean isAuMoinsUnIndigent(ModeImposition impositionPrincipal, ModeImposition impositionConjoint) {
		return isIndigent(impositionPrincipal) || isIndigent(impositionConjoint);
	}
	
	private boolean isOrdinaire(ModeImposition modeImposition) {
		return ModeImposition.ORDINAIRE.equals(modeImposition);
	}

	private boolean isDepense(ModeImposition modeImposition) {
		return ModeImposition.DEPENSE.equals(modeImposition);
	}
	
	private boolean isSourcierPur(ModeImposition modeImposition) {
		return ModeImposition.SOURCE.equals(modeImposition);
	}

	private boolean isSourcierMixteLoi(ModeImposition modeImposition) {
		return ModeImposition.MIXTE_137_1.equals(modeImposition);
	}
	
	private boolean isSourcierMixtePratique(ModeImposition modeImposition) {
		return ModeImposition.MIXTE_137_2.equals(modeImposition);
	}

	private boolean isIndigent(ModeImposition modeImposition) {
		return ModeImposition.INDIGENT.equals(modeImposition);
	}
}
