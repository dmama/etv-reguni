package ch.vd.uniregctb.evenement.civil.interne.reconciliation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class ReconciliationHandler extends EvenementCivilHandlerBase {

	private static final Logger LOGGER = Logger.getLogger(ReconciliationHandler.class);

	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivilInterne target, List<EvenementCivilExterneErreur> errors, List<EvenementCivilExterneErreur> warnings) {

		Individu individu = target.getIndividu();

		/*
		 * Le tiers correspondant doit exister
		 */
		PersonnePhysique habitant = getPersonnePhysiqueOrFillErrors(individu.getNoTechnique(), errors);
		if (habitant == null) {
			return;
		}

		/*
		 * Validation de l'état civil de l'individu
		 */
		validateEtatCivil(habitant, target.getDate(), errors);

		/*
		 * Dans le cas où le conjoint réside dans le canton, il faut que le tiers contribuable existe.
		 */
		PersonnePhysique habitantConjoint = null;
		Individu conjoint = getServiceCivil().getConjoint(individu.getNoTechnique(),target.getDate());
		if (conjoint != null) {

			/*
			 * Le tiers correspondant doit exister
			 */
			habitantConjoint = getPersonnePhysiqueOrFillErrors(conjoint.getNoTechnique(), errors);
			if (habitantConjoint == null) {
				return;
			}

			/*
			 * Validation de l'état civil du conjoint
			 */
			validateEtatCivil(habitantConjoint, target.getDate(), errors);
		}

		/*
		 * Validations métier
		 */
		ValidationResults results = getMetier().validateReconciliation(habitant, habitantConjoint, target.getDate());
		addValidationResults(errors, warnings, results);
	}

	private void validateEtatCivil(PersonnePhysique habitant, RegDate date, List<EvenementCivilExterneErreur> erreurs) {

		ServiceCivilService serviceCivil = getService().getServiceCivilService();
		EtatCivil etatCivil = serviceCivil.getEtatCivilActif(habitant.getNumeroIndividu(), date);
		if (etatCivil == null) {
			erreurs.add(new EvenementCivilExterneErreur("L'individu n°" + habitant.getNumeroIndividu() + " ne possède pas d'état civil à la date de l'événement"));
		}

		if (!EtatCivilHelper.estMarieOuPacse(etatCivil)) {
			erreurs.add(new EvenementCivilExterneErreur("L'individu n°" + habitant.getNumeroIndividu() + " n'est pas marié dans le civil"));
		}
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

		final Reconciliation reconciliation = (Reconciliation) evenement;
		try {
			final PersonnePhysique contribuable = getPersonnePhysiqueOrThrowException(reconciliation.getNoIndividu());
			final Individu individuConjoint = getServiceCivil().getConjoint(reconciliation.getNoIndividu(),reconciliation.getDate());
			final PersonnePhysique conjoint = (individuConjoint == null) ? null : getPersonnePhysiqueOrThrowException(individuConjoint.getNoTechnique());

			getMetier().reconcilie(contribuable, conjoint, reconciliation.getDateReconciliation(), null, false, reconciliation.getNumeroEvenement());
			return null;
		}
		catch (Exception e) {
			LOGGER.error("Erreur lors du traitement de réconciliation", e);
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.RECONCILIATION);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new ReconciliationAdapter(event, context, this);
	}

}
