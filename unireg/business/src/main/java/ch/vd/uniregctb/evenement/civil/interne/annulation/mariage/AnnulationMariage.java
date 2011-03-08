package ch.vd.uniregctb.evenement.civil.interne.annulation.mariage;

import java.text.MessageFormat;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementErreur;

/**
 * Adapter pour l'annulation de mariage.
 *
 * @author Pavel BLANCO
 *
 */
public class AnnulationMariage extends EvenementCivilInterne {

	protected AnnulationMariage(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected AnnulationMariage(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, TypeEvenementCivil.ANNUL_MARIAGE, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {

		// Cas d'annulation de mariage
		final ServiceCivilService serviceCivil = context.getTiersService().getServiceCivilService();

		final EtatCivil ec = serviceCivil.getEtatCivilActif(getNoIndividu(), getDate());
		if (EtatCivilHelper.estMarieOuPacse(ec)) {
			erreurs.add(new EvenementCivilExterneErreur("L'individu est toujours marié ou pacsé dans le civil"));
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		// Obtention du tiers correspondant au conjoint principal.
		final PersonnePhysique principal = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getNoIndividu());

		// Récupération de l'ensemble tiers couple
		final EnsembleTiersCouple menageComplet = context.getTiersService().getEnsembleTiersCouple(principal, getDate());
		// Vérification de la cohérence
		if (menageComplet == null) {
			throw new EvenementCivilHandlerException("Le tiers ménage commun n'a pu être trouvé");
		}
		else if (!menageComplet.contient(principal)) {
			throw new EvenementCivilHandlerException("Le tiers ménage commun n'a pu être trouvé");
		}
		else {
			// Récupération du tiers MenageCommun
			MenageCommun menage = menageComplet.getMenage();
			// Si le tiers MenageCommun n'est pas trouvé, la base fiscale est inconsistente => mise en erreur de l'événement
			if (menage == null) {
				throw new EvenementCivilHandlerException("Le tiers ménage commun n'a pu être trouvé");
			}
		}

		final PersonnePhysique conjoint = menageComplet.getConjoint(principal);

		checkForsAnnulesApres(principal, getDate(), warnings);
		if (conjoint != null) {
			checkForsAnnulesApres(conjoint, getDate(), warnings);
		}

		// Traitement de l'annulation de mariage
		context.getMetierService().annuleMariage(principal, conjoint, getDate(), getNumeroEvenement());

		// On signale que le conjoint a changé dans le registre civil (=> va rafraîchir le cache des individus)
		if (conjoint != null && conjoint.isHabitantVD()) {
			context.getDataEventService().onIndividuChange(conjoint.getNumeroIndividu());
		}
		return null;
	}

	/**
	 * Ajoute des warnings si le contribuable a plus d'un for fiscal principal après la date de mariage.
	 * @param pp le contribuablle
	 * @param date la date de mariage
	 * @param warnings la liste de warnings
	 */
	private void checkForsAnnulesApres(PersonnePhysique pp, RegDate date, List<EvenementCivilExterneErreur> warnings) {
		final List<ForFiscalPrincipal> forsFiscaux = pp.getForsFiscauxPrincipauxOuvertsApres(date);
		final int nombreFors = forsFiscaux.size();
		if (nombreFors > 1 || (nombreFors == 1 && !isAnnuleEtOuvert(forsFiscaux.get(0)))) {
			String message = MessageFormat.format("Le tiers n° {0} possède au moins un for fiscal principal après la date de mariage ({1})",
					FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()), RegDateHelper.dateToDisplayString(date));
			warnings.add(new EvenementCivilExterneErreur(message, TypeEvenementErreur.WARNING));
		}
	}

	private boolean isAnnuleEtOuvert(ForFiscalPrincipal ffp) {
		return ffp.isAnnule() && ffp.getDateFin() == null;
	}
}
