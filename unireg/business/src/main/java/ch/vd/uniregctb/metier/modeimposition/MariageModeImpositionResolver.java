package ch.vd.uniregctb.metier.modeimposition;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Resolver du mode d'imposition pour le cas mariage.
 * 
 * @author Pavel BLANCO
 *
 */
public class MariageModeImpositionResolver extends CreationCoupleModeImpositionResolver {

	private final Long numeroEvenement;

	public MariageModeImpositionResolver(TiersService tiersService, Long numeroEvenement) {
		super(tiersService);
		this.numeroEvenement = numeroEvenement;
	}

	/**
	 * Calcule le nouveau mode d'imposition lors d'un mariage.
	 * @param contribuable le nouveau MenageCommun
	 * @param date la date de mariage
	 *
	 * @see CreationCoupleModeImpositionResolver#resolve(ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques, ch.vd.registre.base.date.RegDate)
	 */
	@Override
	public Imposition resolve(ContribuableImpositionPersonnesPhysiques contribuable, RegDate date) throws ModeImpositionResolverException {
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
		final ForFiscalPrincipalPP forFPPrincipal = principal.getForFiscalPrincipalAt(null);
		/*
		 * le contribuable est assujetti
		 */
		if (forFPPrincipal != null) {
			final ModeImposition modeImpositionContribuable = forFPPrincipal.getModeImposition();
			return new Imposition(date, modeImpositionContribuable);
		}
		
		return null;
	}

	/**
	 * Détermine le mode d'imposition courant (à partir de son for fiscal principal) ou théorique (à partir de sa nationalité et de ses permis).
	 * <p/>
	 * <b>Note:</b> si la personne ne possède ni permis C ni nationalité, on retourne une valeur nulle et c'est à l'appelant de gérer le cas correctement (SIFISC-7881)
	 *
	 * @param pp      une personne physique
	 * @param date    la date de valeur
	 * @param sansFor paramètre de sortie mis à vrai si la personne ne possède pas de for fiscal principal
	 * @return le mode d'imposition déterminé, ou <b>null</b> s'il n'a pas pu l'être.
	 */
	@Nullable
	private ModeImposition getModeImposition(PersonnePhysique pp, RegDate date, MutableBoolean sansFor) {

		final ForFiscalPrincipalPP ffp = pp.getForFiscalPrincipalAt(null);
		sansFor.setValue(ffp == null);

		final ModeImposition modeImposition;
		if (ffp != null) {
			modeImposition = ffp.getModeImposition();
		}
		else {
			try {
				if (getTiersService().isSuisseOuPermisC(pp, date)) {
					// suisse ou titulaire d'un permis C => ordinaire
					modeImposition = ModeImposition.ORDINAIRE;
				}
				else {
					modeImposition = ModeImposition.SOURCE;
				}
			}
			catch (TiersException e) {
				return null;
			}
		}
		return modeImposition;
	}

	private Imposition resolveCouple(PersonnePhysique principal, PersonnePhysique conjoint, RegDate date) throws ModeImpositionResolverException {
		final MutableBoolean principalSansFor = new MutableBoolean(false);
		final MutableBoolean conjointSansFor = new MutableBoolean(false);

		final ModeImposition impositionPrincipal = getModeImposition(principal, date, principalSansFor);
		final ModeImposition impositionConjoint = getModeImposition(conjoint, date, conjointSansFor);

		// [SIFISC-7881] Si l'un des deux conjoints est suisse ou permis C, on peut autoriser la création du couple
		// (qui de toute façon sera en ordinaire) sans se préoccuper de la nationalité ou du permis du second.
		if (impositionPrincipal == null && impositionConjoint == null) {
			throw new ModeImpositionResolverException("Impossible de déterminer le mode d'imposition requis (que ce soit sur le principal ou le conjoint)");
		}
		else if (impositionConjoint == null && impositionPrincipal == ModeImposition.SOURCE) {
			throw new ModeImpositionResolverException("Impossible de déterminer le mode d'imposition requis sur le conjoint");
		}
		else if (impositionPrincipal == null && impositionConjoint == ModeImposition.SOURCE) {
			throw new ModeImpositionResolverException("Impossible de déterminer le mode d'imposition requis sur le principal");
		}

		if (principalSansFor.booleanValue() && conjointSansFor.booleanValue()) {
			Audit.info(numeroEvenement, "les 2 maries ne sont pas assujetti : aucune action sur les fors");
			return null;
		}

		return internalResolveCouple(date, impositionPrincipal, impositionConjoint);
	}
	
	protected Imposition internalResolveCouple(RegDate date, ModeImposition impositionContribuable, ModeImposition impositionConjoint) throws ModeImpositionResolverException {
		
		/*
		 * Paramétrage de la catégorie de l'impôt ordre de priorité : dépense, ordinaire, mixte loi, mixte pratique, source
		 */
		final ModeImposition modeImposition;
		if (isAuMoinsUnDepense(impositionContribuable, impositionConjoint)) {
			modeImposition = ModeImposition.DEPENSE;
		}
		else if (isAuMoinsUncontribuableOrdinaire(impositionContribuable, impositionConjoint)) {
			modeImposition = ModeImposition.ORDINAIRE;
		}
		else if (isAuMoinsUnIndigent(impositionContribuable, impositionConjoint)) {
			if (isIndigent(impositionContribuable) && isIndigent(impositionConjoint)) {
				modeImposition = ModeImposition.INDIGENT;
			}
			else {
				modeImposition = ModeImposition.ORDINAIRE;
			}
		}
		else if (isAuMoinsUnSourcierMixteLoi(impositionContribuable, impositionConjoint)) {
			modeImposition = ModeImposition.MIXTE_137_1;
		}
		else if (isAuMoinsUnSourcierMixtePratique(impositionContribuable, impositionConjoint)) {
			modeImposition = ModeImposition.MIXTE_137_2;
		}
		else if (isAuMoinsUnSourcierPur(impositionContribuable, impositionConjoint)) {
			modeImposition = ModeImposition.SOURCE;
		}
		else { // les deux sont sans revenu ni fortune => ordinaire
			modeImposition = ModeImposition.ORDINAIRE;
		}

		final String logAudit = String.format("Le nouveau for fiscal principal pour le tiers ménage commun aura le role %s", modeImposition.texte());
		Audit.info(numeroEvenement, logAudit);
		
		return new Imposition(date, modeImposition);
	}

	private static boolean isAuMoinsUncontribuableOrdinaire(ModeImposition impositionPrincipal, ModeImposition impositionConjoint) {
		return isOrdinaire(impositionPrincipal) || isOrdinaire(impositionConjoint);
	}

	private static boolean isAuMoinsUnDepense(ModeImposition impositionPrincipal, ModeImposition impositionConjoint) {
		return isDepense(impositionPrincipal) || isDepense(impositionConjoint);
	}
	
	private static boolean isAuMoinsUnSourcierPur(ModeImposition impositionPrincipal, ModeImposition impositionConjoint) {
		return isSourcierPur(impositionPrincipal) || isSourcierPur(impositionConjoint);
	}

	private static boolean isAuMoinsUnSourcierMixteLoi(ModeImposition impositionPrincipal, ModeImposition impositionConjoint) {
		return isSourcierMixteLoi(impositionPrincipal) || isSourcierMixteLoi(impositionConjoint);
	}

	private static boolean isAuMoinsUnSourcierMixtePratique(ModeImposition impositionPrincipal, ModeImposition impositionConjoint) {
		return isSourcierMixtePratique(impositionPrincipal) || isSourcierMixtePratique(impositionConjoint);
	}

	private static boolean isAuMoinsUnIndigent(ModeImposition impositionPrincipal, ModeImposition impositionConjoint) {
		return isIndigent(impositionPrincipal) || isIndigent(impositionConjoint);
	}
	
	private static boolean isOrdinaire(ModeImposition modeImposition) {
		return ModeImposition.ORDINAIRE == modeImposition;
	}

	private static boolean isDepense(ModeImposition modeImposition) {
		return ModeImposition.DEPENSE == modeImposition;
	}
	
	private static boolean isSourcierPur(ModeImposition modeImposition) {
		return ModeImposition.SOURCE == modeImposition;
	}

	private static boolean isSourcierMixteLoi(ModeImposition modeImposition) {
		return ModeImposition.MIXTE_137_1 == modeImposition;
	}
	
	private static boolean isSourcierMixtePratique(ModeImposition modeImposition) {
		return ModeImposition.MIXTE_137_2 == modeImposition;
	}

	private static boolean isIndigent(ModeImposition modeImposition) {
		return ModeImposition.INDIGENT == modeImposition;
	}
}
