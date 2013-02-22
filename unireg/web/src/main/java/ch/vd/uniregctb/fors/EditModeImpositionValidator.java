package ch.vd.uniregctb.fors;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;

public class EditModeImpositionValidator implements Validator {

	private HibernateTemplate hibernateTemplate;
	private AutorisationManager autorisationManager;

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
		if (view.getDateChangement() == null) {
			errors.rejectValue("dateChangement", "error.date.changement.vide");
		}
		else if (view.getDateChangement().isAfter(RegDate.get())) {
			errors.rejectValue("dateChangement", "error.date.changement.posterieure.date.jour");
		}
		else if (view.getDateChangement().isBefore(ffp.getDateDebut())) {
			errors.rejectValue("dateChangement", "error.date.changement.anterieur.date.debut.for");
		}

		// le mode d'imposition lui-même
		if (view.getModeImposition() == null) {
			errors.rejectValue("modeImposition", "error.mode.imposition.incorrect");
		}
		else {
			if (!autorisationManager.isModeImpositionAllowed(ffp.getTiers(), view.getModeImposition(), view.getTypeAutoriteFiscale(), view.getMotifRattachement(), view.getDateDebut(),
			                                                 AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID())) {
				errors.rejectValue("modeImposition", "error.mode.imposition.interdit");
			}
		}
	}
}
