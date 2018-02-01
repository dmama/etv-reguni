package ch.vd.unireg.param.validator;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.param.view.ParametrePeriodeFiscalePMEditView;

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
		final String[] champsDelaiMois = {
				"delaiImprimeMoisVaud",
				"delaiImprimeMoisHorsCanton",
				"delaiImprimeMoisHorsSuisse"
		};
		final String[] champsDelaiJours = {
				"toleranceJoursVaud",
				"toleranceJoursHorsCanton",
				"toleranceJoursHorsSuisse"
		};
		checkDelais(champsDelaiMois, 24, view, errors);
		checkDelais(champsDelaiJours, 180, view, errors);
	}

	private void checkDelais(String[] champs, int maxValue, ParametrePeriodeFiscalePMEditView view, Errors errors) {
		for (String champ : champs) {
			// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
			if (!errors.hasFieldErrors(champ)) {
				try {
					if (PropertyUtils.getProperty(view, champ) == null) {
						errors.rejectValue(champ, "error.champ.obligatoire");
					}
					else if (((Integer) PropertyUtils.getProperty(view, champ)) < 0) {
						errors.rejectValue(champ, "error.delai.negatif.interdit");
					}
					else if (((Integer) PropertyUtils.getProperty(view, champ)) > maxValue) {
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
}
