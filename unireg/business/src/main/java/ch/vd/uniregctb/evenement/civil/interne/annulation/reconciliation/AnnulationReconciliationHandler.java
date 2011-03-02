package ch.vd.uniregctb.evenement.civil.interne.annulation.reconciliation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
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

	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		// Cas d'annulation de réconciliation
		AnnulationReconciliation annulation = (AnnulationReconciliation) target;
		// Obtention du tiers correspondant au conjoint principal.
		PersonnePhysique principal = getService().getPersonnePhysiqueByNumeroIndividu(annulation.getNoIndividu());
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

		final Individu individuConjoint = getServiceCivil().getConjoint(annulation.getNoIndividu(), target.getDate());
		if (individuConjoint != null) {
			// Obtention du tiers correspondant au conjoint.
			PersonnePhysique conjoint = getService().getPersonnePhysiqueByNumeroIndividu(individuConjoint.getNoTechnique());
			// Vérification de la cohérence
			if (!menageComplet.estComposeDe(principal, conjoint)) {
				throw new EvenementCivilHandlerException("Les tiers composant le tiers ménage trouvé ne correspondent pas avec les individus unis dans le civil");
			}
		}
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		// Cas d'annulation de réconciliation
		AnnulationReconciliation annulation = (AnnulationReconciliation) evenement;
		// Récupération du tiers principal.
		PersonnePhysique principal = getService().getPersonnePhysiqueByNumeroIndividu(annulation.getNoIndividu());
		// Obtention du tiers correspondant au conjoint.
		PersonnePhysique conjoint = null;
		final Individu individuConjoint = getServiceCivil().getConjoint(evenement.getNoIndividu(), evenement.getDate());
		if (individuConjoint != null) {
			conjoint = getService().getPersonnePhysiqueByNumeroIndividu(individuConjoint.getNoTechnique());
		}
		// Traitement de l'annulation de réconciliation
		getMetier().annuleReconciliation(principal, conjoint, annulation.getDate(), annulation.getNumeroEvenement());
		return null;
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.ANNUL_RECONCILIATION);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new AnnulationReconciliationAdapter(event, context, this);
	}

}
