package ch.vd.uniregctb.evenement.civil.interne.reconciliation;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Reconciliation extends EvenementCivilInterne {

	private static final Logger LOGGER = Logger.getLogger(Reconciliation.class);

	protected Reconciliation(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Reconciliation(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, TypeEvenementCivil.RECONCILIATION, date, numeroOfsCommuneAnnonce, context);
	}

	public RegDate getDateReconciliation() {
		return getDate();
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

		Individu individu = getIndividu();

		/*
		 * Le tiers correspondant doit exister
		 */
		PersonnePhysique habitant = getPersonnePhysiqueOrFillErrors(individu.getNoTechnique(), erreurs);
		if (habitant == null) {
			return;
		}

		/*
		 * Validation de l'état civil de l'individu
		 */
		validateEtatCivil(habitant, getDate(), erreurs);

		/*
		 * Dans le cas où le conjoint réside dans le canton, il faut que le tiers contribuable existe.
		 */
		PersonnePhysique habitantConjoint = null;
		Individu conjoint = context.getServiceCivil().getConjoint(individu.getNoTechnique(), getDate());
		if (conjoint != null) {

			/*
			 * Le tiers correspondant doit exister
			 */
			habitantConjoint = getPersonnePhysiqueOrFillErrors(conjoint.getNoTechnique(), erreurs);
			if (habitantConjoint == null) {
				return;
			}

			/*
			 * Validation de l'état civil du conjoint
			 */
			validateEtatCivil(habitantConjoint, getDate(), erreurs);
		}

		/*
		 * Validations métier
		 */
		ValidationResults results = context.getMetierService().validateReconciliation(habitant, habitantConjoint, getDate());
		addValidationResults(erreurs, warnings, results);
	}

	private void validateEtatCivil(PersonnePhysique habitant, RegDate date, List<EvenementCivilExterneErreur> erreurs) {

		final ServiceCivilService serviceCivil = context.getTiersService().getServiceCivilService();
		EtatCivil etatCivil = serviceCivil.getEtatCivilActif(habitant.getNumeroIndividu(), date);
		if (etatCivil == null) {
			erreurs.add(new EvenementCivilExterneErreur("L'individu n°" + habitant.getNumeroIndividu() + " ne possède pas d'état civil à la date de l'événement"));
		}

		if (!EtatCivilHelper.estMarieOuPacse(etatCivil)) {
			erreurs.add(new EvenementCivilExterneErreur("L'individu n°" + habitant.getNumeroIndividu() + " n'est pas marié dans le civil"));
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

		try {
			final PersonnePhysique contribuable = getPersonnePhysiqueOrThrowException(getNoIndividu());
			final Individu individuConjoint = context.getServiceCivil().getConjoint(getNoIndividu(), getDate());
			final PersonnePhysique conjoint = (individuConjoint == null) ? null : getPersonnePhysiqueOrThrowException(individuConjoint.getNoTechnique());

			context.getMetierService().reconcilie(contribuable, conjoint, getDateReconciliation(), null, false, getNumeroEvenement());
			return null;
		}
		catch (Exception e) {
			LOGGER.error("Erreur lors du traitement de réconciliation", e);
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}
	}
}
