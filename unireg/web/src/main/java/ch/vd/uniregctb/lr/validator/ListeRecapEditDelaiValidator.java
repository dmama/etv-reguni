package ch.vd.uniregctb.lr.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.di.view.DelaiDeclarationView;
import ch.vd.uniregctb.utils.ValidatorUtils;

public class ListeRecapEditDelaiValidator implements Validator {

	private ListeRecapitulativeDAO lrDAO;

	public void setLrDAO(ListeRecapitulativeDAO lrDAO) {
		this.lrDAO = lrDAO;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class<?> clazz) {
		return DelaiDeclarationView.class.equals(clazz);
	}

	@Override
	public void validate(Object obj, Errors errors) {
		Assert.isTrue(obj instanceof DelaiDeclarationView);
		final DelaiDeclarationView view = (DelaiDeclarationView) obj;

		final DeclarationImpotSource lr = lrDAO.get(view.getIdDeclaration());
		if (lr == null) {
			errors.reject("error.lr.inexistante");
			return;
		}

		if (view.getDelaiAccordeAu() == null) {
			ValidationUtils.rejectIfEmpty(errors, "delaiAccordeAu", "error.delai.accorde.vide");
		}
		else {
			final RegDate ancienDelaiAccorde = lr.getDelaiAccordeAu();
			if (view.getDelaiAccordeAu().isBefore(RegDate.get()) || (ancienDelaiAccorde != null && view.getDelaiAccordeAu().isBeforeOrEqual(ancienDelaiAccorde))) {
				errors.rejectValue("delaiAccordeAu", "error.delai.accorde.invalide");
			}
		}

		if (view.getDateDemande() == null) {
			ValidationUtils.rejectIfEmpty(errors, "dateDemande", "error.date.demande.vide");
		}
		else if (view.getDateDemande().isAfter(RegDate.get())) {
			if (!ValidatorUtils.alreadyHasErrorOnField(errors, "dateDemande")) {
				errors.rejectValue("dateDemande", "error.date.demande.future");
			}
		}
	}
}


