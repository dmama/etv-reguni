package ch.vd.uniregctb.evenement.civil.interne.reconciliation;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;

public class Reconciliation extends EvenementCivilInterne {

	private static final Logger LOGGER = LoggerFactory.getLogger(Reconciliation.class);

	protected Reconciliation(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	public Reconciliation(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Reconciliation(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	public RegDate getDateReconciliation() {
		return getDate();
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		Individu individu = getIndividu();

		/*
		 * Le tiers correspondant doit exister
		 */
		PersonnePhysique habitant = getPersonnePhysiqueOrFillErrors(individu.getNoTechnique(), erreurs);
		if (habitant == null) {
			return;
		}

		//presence d'une décision ACI
		final DecisionAci decisionAci = habitant.getDecisionAciValideAt(getDate());
		if (decisionAci != null) {
			erreurs.addErreur(String.format("Le contribuable trouvé (%s) fait l'objet d'une décision ACI (%s)",
					FormatNumeroHelper.numeroCTBToDisplay(habitant.getNumero()),decisionAci));
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

			final DecisionAci decisionAciConjoint = habitantConjoint.getDecisionAciValideAt(getDate());
			if (decisionAciConjoint != null) {
				erreurs.addErreur(String.format("Le contribuable trouvé (%s) a un conjoint (%s) qui fait l'objet d'une décision ACI (%s)",
						FormatNumeroHelper.numeroCTBToDisplay(habitant.getNumero()),FormatNumeroHelper.numeroCTBToDisplay(habitantConjoint.getNumero()),decisionAciConjoint));
			}

			//On récupère un éventuel couple
			final EnsembleTiersCouple couple = context.getTiersService().getEnsembleTiersCouple(habitant, getDate());
			if (couple != null && couple.getMenage() != null) {
				final DecisionAci decisionSurCouple = couple.getMenage().getDecisionAciValideAt(getDate());
				if (decisionSurCouple != null) {
					erreurs.addErreur(String.format("Le contribuable trouvé (%s) appartient à un ménage  (%s) qui fait l'objet d'une décision ACI (%s)",
							FormatNumeroHelper.numeroCTBToDisplay(habitant.getNumero()),FormatNumeroHelper.numeroCTBToDisplay(couple.getMenage().getNumero()),decisionSurCouple));
				}
			}


			/*
			 * Validation de l'état civil du conjoint
			 */
			validateEtatCivil(habitantConjoint, getDate(), erreurs);
		}

		/*
		 * Validations métier
		 */
		ValidationResults results = context.getMetierService().validateReconciliation(habitant, habitantConjoint, getDate(), true);
		addValidationResults(erreurs, warnings, results);
	}

	private void validateEtatCivil(PersonnePhysique habitant, RegDate date, EvenementCivilErreurCollector erreurs) {

		final ServiceCivilService serviceCivil = context.getServiceCivil();
		EtatCivil etatCivil = serviceCivil.getEtatCivilActif(habitant.getNumeroIndividu(), date);
		if (etatCivil == null) {
			erreurs.addErreur("L'individu n°" + habitant.getNumeroIndividu() + " ne possède pas d'état civil à la date de l'événement");
		}

		if (!EtatCivilHelper.estMarieOuPacse(etatCivil)) {
			erreurs.addErreur("L'individu n°" + habitant.getNumeroIndividu() + " n'est pas marié dans le civil");
		}
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		final PersonnePhysique contribuable = getPersonnePhysiqueOrThrowException(getNoIndividu());
		final Individu individuConjoint = context.getServiceCivil().getConjoint(getNoIndividu(), getDate());
		final PersonnePhysique conjoint = (individuConjoint == null) ? null : getPersonnePhysiqueOrThrowException(individuConjoint.getNoTechnique());
		
		// détermination du cas redondant : ils ont un for qui commencent pour motif "MARIAGE_RECONCILIATION..." le jour de l'événement, justement...
		final EnsembleTiersCouple couple = context.getTiersService().getEnsembleTiersCouple(contribuable, getDate());
		if (couple != null) {
			// c'est bien le même conjoint ?
			if (couple.estComposeDe(contribuable, conjoint)) {
				final ForFiscalPrincipal ffp = couple.getMenage().getForFiscalPrincipalAt(getDate());
				if (ffp != null && ffp.getDateDebut() == getDate() && ffp.getMotifOuverture() == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION) {
					return HandleStatus.REDONDANT;
				}
			}
		}

		try {
			context.getMetierService().reconcilie(contribuable, conjoint, getDateReconciliation(), null, getNumeroEvenement());
		}
		catch (MetierServiceException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}
		return HandleStatus.TRAITE;
	}
}
