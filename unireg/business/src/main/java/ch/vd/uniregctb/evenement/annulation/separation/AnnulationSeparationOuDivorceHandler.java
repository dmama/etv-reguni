package ch.vd.uniregctb.evenement.annulation.separation;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.MenageCommun;

public abstract class AnnulationSeparationOuDivorceHandler extends EvenementCivilHandlerBase {

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// Obtention du tiers correspondant au conjoint principal.
		PersonnePhysique principal = getService().getPersonnePhysiqueByNumeroIndividu(target.getIndividu().getNoTechnique());
		// Récupération de l'ensemble tiers couple
		EnsembleTiersCouple menageComplet = getService().getEnsembleTiersCouple(principal, target.getDate().getOneDayBefore());
		// Récupération du tiers MenageCommun
		MenageCommun menage = null;
		if (menageComplet != null) {
			menage = menageComplet.getMenage();
		}
		// Si le tiers MenageCommun n'est pas trouvé, la base fiscale est inconsistente => mise en erreur de l'événement
		if (menage == null) {
			throw new EvenementCivilHandlerException("Le tiers ménage commun n'a pu être trouvé");
		}
		PersonnePhysique conjoint = null;
		final Individu individuConjoint = getServiceCivil().getConjoint(target.getIndividu().getNoTechnique(), target.getDate());
		if (individuConjoint != null) {
			// Obtention du tiers correspondant au conjoint.
			conjoint = getService().getPersonnePhysiqueByNumeroIndividu(individuConjoint.getNoTechnique());
		}
		// Vérification de la cohérence
		if (!menageComplet.estComposeDe(principal, conjoint)) {
			throw new EvenementCivilHandlerException("Les tiers composant le tiers ménage trouvé ne correspondent pas avec les individus unis dans le civil");
		}
	}
	
	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		// Récupération du tiers principal.
		PersonnePhysique principal = getService().getPersonnePhysiqueByNumeroIndividu(evenement.getIndividu().getNoTechnique());
		// Récupération du menage du tiers
		MenageCommun menage = getService().getEnsembleTiersCouple(principal, evenement.getDate().getOneDayBefore()).getMenage();
		// Traitement de l'annulation de séparation
		getMetier().annuleSeparation(menage, evenement.getDate(), evenement.getNumeroEvenement());
		return null;
	}

}