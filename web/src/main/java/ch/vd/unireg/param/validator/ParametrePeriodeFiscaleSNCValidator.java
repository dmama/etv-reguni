package ch.vd.uniregctb.param.validator;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.param.view.ParametrePeriodeFiscaleSNCEditView;

public class ParametrePeriodeFiscaleSNCValidator implements Validator {

	/**
	 * Un logger pour {@link ParametrePeriodeFiscaleSNCValidator}
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ParametrePeriodeFiscaleSNCValidator.class);

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return clazz.isAssignableFrom(ParametrePeriodeFiscaleSNCEditView.class);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final ParametrePeriodeFiscaleSNCEditView view = (ParametrePeriodeFiscaleSNCEditView) target;
		final Integer requestedYear = view.getAnneePeriodeFiscale() + 1;
		final String[] champsDate = {
				"rappelReglementaire",
				"rappelEffectif"
		};
		for (String champ : champsDate) {
			if (!errors.hasFieldErrors(champ)) {
				try {
					if (PropertyUtils.getProperty(view, champ) == null) {
						errors.rejectValue(champ, "error.champ.obligatoire");
					}
					else if (((RegDate) PropertyUtils.getProperty(view, champ)).isPartial()) {
						errors.rejectValue(champ, "error.date.partielle.interdite");
					}
					else if (((RegDate) PropertyUtils.getProperty(view, champ)).year() != requestedYear) {
						errors.rejectValue(champ, "error.param.annee.incorrecte", new Object[]{requestedYear.toString()}, "erreur sur l''année");
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
