package ch.vd.uniregctb.param.validator;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.param.view.ParametrePeriodeFiscalePPEditView;

public class ParametrePeriodeFiscalePPValidator implements Validator {

	/**
	 * Un logger pour {@link ParametrePeriodeFiscalePPValidator}
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ParametrePeriodeFiscalePPValidator.class);

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return clazz.isAssignableFrom(ParametrePeriodeFiscalePPEditView.class);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final ParametrePeriodeFiscalePPEditView view = (ParametrePeriodeFiscalePPEditView) target;
		final Integer requestedYear = view.getAnneePeriodeFiscale() + 1;
		String[] champsDate = {
				"sommationReglementaireVaud",
				"sommationReglementaireHorsCanton",
				"sommationReglementaireHorsSuisse",
				"sommationReglementaireDepense",
				"sommationEffectiveVaud",
				"sommationEffectiveHorsCanton",
				"sommationEffectiveHorsSuisse",
				"sommationEffectiveDepense",
				"finEnvoiMasseDIVaud",
				"finEnvoiMasseDIHorsCanton",
				"finEnvoiMasseDIHorsSuisse",
				"finEnvoiMasseDIDepense"
		};
		for (String champ : champsDate) {
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
