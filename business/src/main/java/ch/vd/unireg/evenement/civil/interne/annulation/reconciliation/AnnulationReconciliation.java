package ch.vd.unireg.evenement.civil.interne.annulation.reconciliation;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Adapter pour l'annulation de réconciliation.
 *
 * @author Pavel BLANCO
 *
 */
public class AnnulationReconciliation extends EvenementCivilInterne {

	protected AnnulationReconciliation(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	protected AnnulationReconciliation(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected AnnulationReconciliation(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// Obtention du tiers correspondant au conjoint principal.
		PersonnePhysique principal = getPrincipalPP();
		verifierPresenceDecisionEnCours(principal,getDate());
		verifierPresenceDecisionsEnCoursSurCouple(principal);
		// Récupération de l'ensemble tiers couple
		EnsembleTiersCouple menageComplet = context.getTiersService().getEnsembleTiersCouple(principal, getDate());
		// Récupération du tiers MenageCommun
		MenageCommun menage = null;
		if (menageComplet != null) {
			menage = menageComplet.getMenage();
		}
		// Si le tiers MenageCommun n'est pas trouvé, la base fiscale est inconsistente => mise en erreur de l'événement
		if (menage == null && !isAnnulationRedondante()) {
			throw new EvenementCivilException("Le tiers ménage commun n'a pu être trouvé");
		}

		final Individu individuConjoint = context.getServiceCivil().getConjoint(getNoIndividu(), getDate());
		if (individuConjoint != null) {
			// Obtention du tiers correspondant au conjoint.
			PersonnePhysique conjoint = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(individuConjoint.getNoTechnique());
			verifierPresenceDecisionEnCours(conjoint,principal,getDate());
			// Vérification de la cohérence
			if (menageComplet!=null && !menageComplet.estComposeDe(principal, conjoint)) {
				throw new EvenementCivilException("Les tiers composant le tiers ménage trouvé ne correspondent pas avec les individus unis dans le civil");
			}
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
		// Obtention du tiers correspondant au conjoint.
		PersonnePhysique conjoint = null;
		final Individu individuConjoint = context.getServiceCivil().getConjoint(getNoIndividu(), getDate());
		if (individuConjoint != null) {
			conjoint = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(individuConjoint.getNoTechnique());
		}
		// Traitement de l'annulation de réconciliation
		try {
			context.getMetierService().annuleReconciliation(principal, conjoint, getDate(), getNumeroEvenement());
		}
		catch (MetierServiceException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}
		return HandleStatus.TRAITE;
	}

	private boolean isAnnulationRedondante() {

		PersonnePhysique personne = getPrincipalPP();
		MenageCommun menageCommun = null;

		final Set<RapportEntreTiers> rapportsSujet = personne.getRapportsSujet();
		if (rapportsSujet != null) {
			for (RapportEntreTiers rapportSujet : rapportsSujet) {
				if (TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapportSujet.getType() &&
						rapportSujet.getDateDebut().equals(getDate()) && rapportSujet.isAnnule()) {
					// le rapport annulée de l'apartenance a été trouvé, on est redondant
					return true;

				}
			}

		}

		return false;
	}
}