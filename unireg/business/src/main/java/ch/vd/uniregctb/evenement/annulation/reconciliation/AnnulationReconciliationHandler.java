package ch.vd.uniregctb.evenement.annulation.reconciliation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitements métier pour événements d'annulation de réconciliation.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationReconciliationHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// Cas d'annulation de réconciliation
		AnnulationReconciliation annulation = (AnnulationReconciliation) target;
		// Obtention du tiers correspondant au conjoint principal.
		PersonnePhysique principal = getService().getPersonnePhysiqueByNumeroIndividu(annulation.getIndividu().getNoTechnique());
		// Récupération de l'ensemble tiers couple
		EnsembleTiersCouple menageComplet = getService().getEnsembleTiersCouple(principal, annulation.getDate());
		// Récupération du tiers MenageCommun
		MenageCommun menage = null;
		if (menageComplet != null) {
			menage = menageComplet.getMenage();
		}
		// Si le tiers MenageCommun n'est pas trouvé, la base fiscale est inconsistente => mise en erreur de l'événement
		if (menage == null) {
			throw new EvenementCivilHandlerException("Le tiers ménage commun n'a pu être trouvé");
		}
		if (annulation.getIndividu().getConjoint() != null) {
			// Obtention du tiers correspondant au conjoint.
			PersonnePhysique conjoint = getService().getPersonnePhysiqueByNumeroIndividu(annulation.getIndividu().getConjoint().getNoTechnique());
			// Vérification de la cohérence
			if (!menageComplet.estComposeDe(principal, conjoint)) {
				throw new EvenementCivilHandlerException("Les tiers composant le tiers ménage trouvé ne correspondent pas avec les individus unis dans le civil");
			}
		}
	}

	@Override
	public void handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		// Cas d'annulation de réconciliation
		AnnulationReconciliation annulation = (AnnulationReconciliation) evenement;
		// Récupération du tiers principal.
		PersonnePhysique principal = getService().getPersonnePhysiqueByNumeroIndividu(annulation.getIndividu().getNoTechnique());
		// Obtention du tiers correspondant au conjoint.
		PersonnePhysique conjoint = null;
		if (annulation.getIndividu().getConjoint() != null) {
			conjoint = getService().getPersonnePhysiqueByNumeroIndividu(annulation.getIndividu().getConjoint().getNoTechnique());
		}
		// Traitement de l'annulation de réconciliation
		getMetier().annuleReconciliation(principal, conjoint, annulation.getDate(), annulation.getNumeroEvenement());
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.ANNUL_RECONCILIATION);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() { 
		return new AnnulationReconciliationAdapter();
	}

}
