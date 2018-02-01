package ch.vd.unireg.entreprise;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;

import ch.vd.unireg.common.LiteralStringHelper;

public class RaisonSocialeViewValidator extends DateRangeViewValidator<RaisonSocialeView> {

	public RaisonSocialeViewValidator() {
		super(RaisonSocialeView.class);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final RaisonSocialeView view = (RaisonSocialeView) target;

		if (StringUtils.isBlank(LiteralStringHelper.stripExtraSpacesAndBlanks(view.getRaisonSociale()))) {
			errors.rejectValue("raisonSociale", "error.tiers.raison.sociale.vide");
		}
	}
}
