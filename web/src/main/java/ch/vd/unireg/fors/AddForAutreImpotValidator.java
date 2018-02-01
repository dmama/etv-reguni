package ch.vd.unireg.fors;

import org.springframework.validation.Errors;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.type.TypeAutoriteFiscale;

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

		if (view.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			errors.rejectValue("typeAutoriteFiscale", "error.type.autorite.incorrect");
		}

		// validation de la date
		final RegDate dateEvenement = view.getDateEvenement();
		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateEvenement")) {
			if (dateEvenement == null) {
				errors.rejectValue("dateEvenement", "error.date.debut.vide");
			}
			else if (RegDate.get().isBefore(dateEvenement)) {
				errors.rejectValue("dateEvenement", "error.date.debut.future");
			}
		}

		if (view.getGenreImpot() == null) {
			errors.rejectValue("genreImpot", "error.genre.impot.vide");
		}
	}
}
