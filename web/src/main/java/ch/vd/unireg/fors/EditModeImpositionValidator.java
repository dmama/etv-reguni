package ch.vd.unireg.fors;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.manager.AutorisationManager;
import ch.vd.unireg.tiers.manager.AutorisationManagerImpl;
import ch.vd.unireg.tiers.manager.RetourModeImpositionAllowed;

public class EditModeImpositionValidator implements Validator {

	private final HibernateTemplate hibernateTemplate;
	private final AutorisationManager autorisationManager;

	public EditModeImpositionValidator(HibernateTemplate hibernateTemplate, AutorisationManager autorisationManager) {
		this.hibernateTemplate = hibernateTemplate;
		this.autorisationManager = autorisationManager;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return EditModeImpositionView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {

		final EditModeImpositionView view = (EditModeImpositionView) target;

		final ForFiscalPrincipal ffp = hibernateTemplate.get(ForFiscalPrincipal.class, view.getId());
		if (ffp == null) {
			throw new TiersNotFoundException(view.getTiersId());
		}
		view.initReadOnlyData(ffp);

		// la date de changement du mode d'imposition
		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateChangement")) {
			if (view.getDateChangement() == null) {
				errors.rejectValue("dateChangement", "error.date.changement.vide");
			}
			else if (view.getDateChangement().isAfter(RegDate.get())) {
				errors.rejectValue("dateChangement", "error.date.changement.posterieure.date.jour");
			}
			else if (view.getDateChangement().isBefore(ffp.getDateDebut())) {
				errors.rejectValue("dateChangement", "error.date.changement.anterieur.date.debut.for");
			}
		}

		// le mode d'imposition lui-même
		if (view.getModeImposition() == null) {
			errors.rejectValue("modeImposition", "error.mode.imposition.incorrect");
		}
		else {
			final StringBuilder messageErreurModeImposition = new StringBuilder();
			RetourModeImpositionAllowed allowed = autorisationManager.isModeImpositionAllowed(ffp.getTiers(), view.getModeImposition(), view.getTypeAutoriteFiscale(), view.getMotifRattachement(), view.getDateDebut(),
			                                                                                 AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());

			switch(allowed) {
				case INTERDIT: messageErreurModeImposition.append("error.mode.imposition.interdit");
					break;
				case DROITS_INCOHERENTS: messageErreurModeImposition.append("error.mode.imposition.droits.incoherents");
					break;
				case REGLES_INCOHERENTES: messageErreurModeImposition.append("error.mode.imposition.regles.incoherentes");
					break;
			}

			if (!RetourModeImpositionAllowed.OK.equals(allowed)) {
				errors.rejectValue("modeImposition",messageErreurModeImposition.toString());
			}
		}
	}
}
