package ch.vd.uniregctb.param.validator;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.param.view.ParametrePeriodeFiscalePMEditView;

public class ParametrePeriodeFiscalePMValidator implements Validator {

	/**
	 * Un logger pour {@link ParametrePeriodeFiscalePMValidator}
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ParametrePeriodeFiscalePMValidator.class);

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return clazz.isAssignableFrom(ParametrePeriodeFiscalePMEditView.class);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final ParametrePeriodeFiscalePMEditView view = (ParametrePeriodeFiscalePMEditView) target;
		final String[] champsDelai = {
				"delaiImprimeSansMandataireVaud",
				"delaiImprimeAvecMandataireVaud",
				"delaiEffectifSansMandataireVaud",
				"delaiEffectifAvecMandataireVaud",
				"delaiImprimeSansMandataireHorsCanton",
				"delaiImprimeAvecMandataireHorsCanton",
				"delaiEffectifSansMandataireHorsCanton",
				"delaiEffectifAvecMandataireHorsCanton",
				"delaiImprimeSansMandataireHorsSuisse",
				"delaiImprimeAvecMandataireHorsSuisse",
				"delaiEffectifSansMandataireHorsSuisse",
				"delaiEffectifAvecMandataireHorsSuisse"
		};
		for (String champ : champsDelai) {
			try {
				if (PropertyUtils.getProperty(view, champ) == null) {
					errors.rejectValue(champ, "error.champ.obligatoire");
				}
				else if (((Integer) PropertyUtils.getProperty(view, champ)) < 0) {
					errors.rejectValue(champ, "error.delai.negatif.interdit");
				}
				else if (((Integer) PropertyUtils.getProperty(view, champ)) > 720) {
					errors.rejectValue(champ, "error.delai.trop.grand");
				}
			}
			catch (Exception e) {
				errors.rejectValue(champ, e.getMessage());
				LOGGER.error(e.getMessage(), e);
			}
		}
	}
}
