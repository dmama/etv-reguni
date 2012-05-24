package ch.vd.uniregctb.evenement.civil.interne.annulation.separation;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Adapter pour l'annulation de séparation.
 *
 * @author Pavel BLANCO
 *
 */
public abstract class AnnulationSeparationOuDivorce extends EvenementCivilInterne {

	protected AnnulationSeparationOuDivorce(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	protected AnnulationSeparationOuDivorce(EvenementCivilEch evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	public AnnulationSeparationOuDivorce(Individu individu, Individu conjoint, RegDate dateEvenement, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, dateEvenement, numeroOfsCommuneAnnonce, context);
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// Obtention du tiers correspondant au conjoint principal.
		PersonnePhysique principal = getPrincipalPP();
		// Récupération de l'ensemble tiers couple
		EnsembleTiersCouple menageComplet = context.getTiersService().getEnsembleTiersCouple(principal, getDate().getOneDayBefore());
		// Récupération du tiers MenageCommun
		MenageCommun menage = null;
		if (menageComplet != null) {
			menage = menageComplet.getMenage();
		}
		// Si le tiers MenageCommun n'est pas trouvé, la base fiscale est inconsistente => mise en erreur de l'événement
		if (menage == null) {
			throw new EvenementCivilException("Le tiers ménage commun n'a pu être trouvé");
		}
		PersonnePhysique conjoint = null;
		final Individu individuConjoint = context.getServiceCivil().getConjoint(getNoIndividu(), getDate());
		if (individuConjoint != null) {
			// Obtention du tiers correspondant au conjoint.
			conjoint = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(individuConjoint.getNoTechnique());
		}
		// Vérification de la cohérence
		if (!menageComplet.estComposeDe(principal, conjoint)) {
			throw new EvenementCivilException("Les tiers composant le tiers ménage trouvé ne correspondent pas avec les individus unis dans le civil");
		}
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		if (isAnnulationRedondante()) {
			return HandleStatus.REDONDANT;
		}

		// Récupération du tiers principal.
		PersonnePhysique principal = getPrincipalPP();
		// Récupération du menage du tiers
		MenageCommun menage = context.getTiersService().getEnsembleTiersCouple(principal, getDate().getOneDayBefore()).getMenage();
		// Traitement de l'annulation de séparation
		try {
			context.getMetierService().annuleSeparation(menage, getDate(), getNumeroEvenement());
		}
		catch (MetierServiceException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}
		return HandleStatus.TRAITE;
	}

	private  boolean isAnnulationRedondante(){
		PersonnePhysique principal = getPrincipalPP();
		Set<ForFiscal> forFiscaux =  principal.getForsFiscaux();
		for (ForFiscal forFiscal : forFiscaux) {
			if (forFiscal.getDateDebut().equals(getDate()) && forFiscal.isAnnule() && forFiscal.isPrincipal()) {
				ForFiscalPrincipal forPrincipal = (ForFiscalPrincipal)forFiscal;
				if (MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT == forPrincipal.getMotifOuverture()) {
					return true;
				}
			}
		}
		return false;
	}


}
