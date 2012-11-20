package ch.vd.uniregctb.fors;

import org.springframework.validation.Errors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class AddForAutreImpotValidator extends AddForValidator {

	protected AddForAutreImpotValidator(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return AddForAutreImpotView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final AddForAutreImpotView view = (AddForAutreImpotView) target;

		// validation de la date
		final RegDate dateEvenement = view.getDateEvenement();
		if (dateEvenement == null) {
			errors.rejectValue("dateEvenement", "error.date.debut.vide");
		}
		else if (RegDate.get().isBefore(dateEvenement)) {
			errors.rejectValue("dateEvenement", "error.date.debut.future");
		}

		if (view.getGenreImpot() == null) {
			errors.rejectValue("genreImpot", "error.genre.impot.vide");
		}
	}
}
