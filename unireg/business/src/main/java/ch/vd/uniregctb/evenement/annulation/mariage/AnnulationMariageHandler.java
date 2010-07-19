package ch.vd.uniregctb.evenement.annulation.mariage;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementErreur;

/**
 * Traitement métier pour événements d'annulation de mariage.
 *
 * @author Pavel BLANCO
 *
 */
public class AnnulationMariageHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// Cas d'annulation de mariage
		final AnnulationMariage annulation = (AnnulationMariage) target;

		final ServiceCivilService serviceCivil = getService().getServiceCivilService();

		final EtatCivil ec = serviceCivil.getEtatCivilActif(annulation.getNoIndividu(), annulation.getDate());
		if (EtatCivilHelper.estMarieOuPacse(ec)) {
			erreurs.add(new EvenementCivilErreur("L'individu est toujours marié ou pacsé dans le civil"));
		}
	}

	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		// Cas d'annulation de mariage
		final AnnulationMariage annulation = (AnnulationMariage) evenement;
		// Obtention du tiers correspondant au conjoint principal.
		final PersonnePhysique principal = getService().getPersonnePhysiqueByNumeroIndividu(annulation.getNoIndividu());

		// Récupération de l'ensemble tiers couple
		final EnsembleTiersCouple menageComplet = getService().getEnsembleTiersCouple(principal, annulation.getDate());
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

		checkForsAnnulesApres(principal, annulation.getDate(), warnings);
		if (conjoint != null) {
			checkForsAnnulesApres(conjoint, annulation.getDate(), warnings);
		}

		// Traitement de l'annulation de mariage
		getMetier().annuleMariage(principal, conjoint, annulation.getDate(), annulation.getNumeroEvenement());

		// On signale que le conjoint a changé dans le registre civil (=> va rafraîchir le cache des individus)
		if (conjoint != null && conjoint.isHabitantVD()) {
			dataEventService.onIndividuChange(conjoint.getNumeroIndividu());
		}
		return null;
	}

	/**
	 * Ajoute des warnings si le contribuable a plus d'un for fiscal principal après la date de mariage.
	 * @param pp le contribuablle
	 * @param date la date de mariage
	 * @param warnings la liste de warnings
	 */
	private void checkForsAnnulesApres(PersonnePhysique pp, RegDate date, List<EvenementCivilErreur> warnings) {
		final List<ForFiscalPrincipal> forsFiscaux = pp.getForsFiscauxPrincipauxApres(date);
		final int nombreFors = forsFiscaux.size();
		if (nombreFors > 1 || (nombreFors == 1 && !isAnnuleEtOuvert(forsFiscaux.get(0)))) {
			String message = MessageFormat.format("Le tiers n° {0} possède au moins un for fiscal principal après la date de mariage ({1})",
					FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()), RegDateHelper.dateToDisplayString(date));
			warnings.add(new EvenementCivilErreur(message, TypeEvenementErreur.WARNING));
		}
	}

	public boolean isAnnuleEtOuvert(ForFiscalPrincipal ffp) {
		return ffp.isAnnule() && ffp.getDateFin() == null;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new AnnulationMariageAdapter();
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.ANNUL_MARIAGE);
		return types;
	}

}
