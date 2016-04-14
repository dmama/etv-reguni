package ch.vd.uniregctb.entreprise.complexe;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;

public class FusionEntreprisesViewValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return FusionEntreprisesView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final FusionEntreprisesView view = (FusionEntreprisesView) target;

		if (view.getDateContratFusion() == null) {
			errors.rejectValue("dateContratFusion", "error.date.contrat.fusion.vide");
		}
		else if (view.getDateContratFusion().isAfter(RegDate.get())) {
			errors.rejectValue("dateContratFusion", "error.date.contrat.fusion.future");
		}

		if (view.getDateBilanFusion() == null) {
			errors.rejectValue("dateBilanFusion", "error.date.bilan.fusion.vide");
		}
		else if (view.getDateBilanFusion().isAfter(RegDate.get())) {
			errors.rejectValue("dateBilanFusion", "error.date.bilan.fusion.future");
		}
		else if (view.getDateContratFusion() != null && view.getDateBilanFusion().isAfter(view.getDateContratFusion())) {
			errors.rejectValue("dateBilanFusion", "error.date.bilan.fusion.apres.date.contrat");
		}
	}
}