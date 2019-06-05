package ch.vd.unireg.evenement.civil.interne.annulation.mariage;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.EtatCivilHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Adapter pour l'annulation de mariage.
 *
 * @author Pavel BLANCO
 *
 */
public class AnnulationMariage extends EvenementCivilInterne {

	protected AnnulationMariage(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	protected AnnulationMariage(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected AnnulationMariage(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		final PersonnePhysique principalPP = getPrincipalPP();
		verifierPresenceDecisionEnCours(principalPP,getDate());
		verifierPresenceDecisionsEnCoursSurCouple(principalPP);

		// Cas d'annulation de mariage
		final ServiceCivilService serviceCivil = context.getServiceCivil();

		final EtatCivil ec = serviceCivil.getEtatCivilActif(getNoIndividu(), getDate());
		if (EtatCivilHelper.estMarieOuPacse(ec)) {
			erreurs.addErreur("L'individu est toujours marié ou pacsé dans le civil");
		}
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		// Obtention du tiers correspondant au conjoint principal.
		final PersonnePhysique principal = getPrincipalPP();

		if (isAnnulationRedondante()) {
			return HandleStatus.REDONDANT;
		}
		// Récupération de l'ensemble tiers couple
		final EnsembleTiersCouple menageComplet = context.getTiersService().getEnsembleTiersCouple(principal, getDate());
		// Vérification de la cohérence
		if (menageComplet == null) {
			throw new EvenementCivilException("Le tiers ménage commun n'a pu être trouvé");
		}
		else if (!menageComplet.contient(principal)) {
			throw new EvenementCivilException("Le tiers ménage commun n'a pu être trouvé");
		}
		else {
			// Récupération du tiers MenageCommun
			MenageCommun menage = menageComplet.getMenage();
			// Si le tiers MenageCommun n'est pas trouvé, la base fiscale est inconsistente => mise en erreur de l'événement
			if (menage == null) {
				throw new EvenementCivilException("Le tiers ménage commun n'a pu être trouvé");
			}
		}

		final PersonnePhysique conjoint = menageComplet.getConjoint(principal);

		checkForsAnnulesApres(principal, getDate(), warnings);
		if (conjoint != null) {
			checkForsAnnulesApres(conjoint, getDate(), warnings);
		}

		// Traitement de l'annulation de mariage
		try {
			context.getMetierService().annuleMariage(principal, conjoint, getDate(), getNumeroEvenement());
		}
		catch (MetierServiceException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}

		// On signale que le conjoint a changé dans le registre civil (=> va rafraîchir le cache des individus)
		if (conjoint != null && conjoint.isHabitantVD()) {
			context.getCivilDataEventNotifier().notifyIndividuChange(conjoint.getNumeroIndividu());
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

	/**
	 * Ajoute des warnings si le contribuable a plus d'un for fiscal principal après la date de mariage.
	 * @param pp le contribuablle
	 * @param date la date de mariage
	 * @param warnings la liste de warnings
	 */
	private void checkForsAnnulesApres(PersonnePhysique pp, RegDate date, EvenementCivilWarningCollector warnings) {
		final List<ForFiscalPrincipal> forsFiscaux = pp.getForsFiscauxPrincipauxOuvertsApres(date);
		final int nombreFors = forsFiscaux.size();
		if (nombreFors > 1 || (nombreFors == 1 && !isAnnuleEtOuvert(forsFiscaux.get(0)))) {
			String message = MessageFormat.format("Le tiers n° {0} possède au moins un for fiscal principal après la date de mariage ({1})",
					FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()), RegDateHelper.dateToDisplayString(date));
			warnings.addWarning(message);
		}
	}

	private boolean isAnnuleEtOuvert(ForFiscalPrincipal ffp) {
		return ffp.isAnnule() && ffp.getDateFin() == null;
	}
}
