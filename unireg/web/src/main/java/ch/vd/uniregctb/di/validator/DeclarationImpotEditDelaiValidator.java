package ch.vd.uniregctb.di.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.delai.DelaiDeclarationView;

public class DeclarationImpotEditDelaiValidator implements Validator {

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return DelaiDeclarationView.class.equals(clazz) ;
	}

	public void validate(Object obj, Errors errors) {
		Assert.isTrue(obj instanceof DelaiDeclarationView);
		DelaiDeclarationView delaiView = (DelaiDeclarationView) obj;
		if (delaiView.getDelaiAccordeAu() == null) {
			ValidationUtils.rejectIfEmpty(errors, "delaiAccordeAu", "error.delai.accorde.vide");
		}
		else if (delaiView.getDelaiAccordeAu().isBefore(RegDate.get()) ||
				(delaiView.getOldDelaiAccorde() != null && delaiView.getDelaiAccordeAu().isBeforeOrEqual(delaiView.getOldDelaiAccorde()))) {
			errors.rejectValue("delaiAccordeAu", "error.delai.accorde.invalide");
		}

		if (delaiView.getDateDemande() == null) {
			ValidationUtils.rejectIfEmpty(errors, "dateDemande", "error.date.demande.vide");
		}
		else if (delaiView.getDateDemande().isAfter(RegDate.get())){
			errors.rejectValue("dateDemande", "error.date.demande.future");
		}
	}

}
