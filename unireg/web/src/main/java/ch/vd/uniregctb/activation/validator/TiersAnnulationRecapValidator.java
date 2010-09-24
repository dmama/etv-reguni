package ch.vd.uniregctb.activation.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.activation.view.TiersAnnulationRecapView;

public class TiersAnnulationRecapValidator implements Validator {


	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return TiersAnnulationRecapView.class.equals(clazz) ;
	}

	public void validate(Object obj, Errors errors) {

		Assert.isTrue(obj instanceof TiersAnnulationRecapView);
		TiersAnnulationRecapView tiersAnnulationRecapView = (TiersAnnulationRecapView) obj;
		if (tiersAnnulationRecapView.getDateAnnulation() == null) {
			ValidationUtils.rejectIfEmpty(errors, "dateAnnulation", "error.date.annulation.vide");
			return;
		}
		if (tiersAnnulationRecapView.getTiersRemplacant() != null) {
			if (tiersAnnulationRecapView.getTiers().getNumero().longValue() == tiersAnnulationRecapView.getTiersRemplacant().getNumero().longValue()) {
				errors.reject("global.error.msg", "Le tiers remplaçant ne peut pas être le même tiers");
			}
		}
	}
}
