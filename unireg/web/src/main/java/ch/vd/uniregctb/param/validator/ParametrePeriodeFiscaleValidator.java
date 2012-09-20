package ch.vd.uniregctb.param.validator;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.param.view.ParametrePeriodeFiscaleView;

public class ParametrePeriodeFiscaleValidator implements Validator{
	
	/**
	 * Un logger pour {@link ParametrePeriodeFiscaleValidator}
	 */
	private static final Logger LOGGER = Logger.getLogger(ParametrePeriodeFiscaleValidator.class);

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return clazz.isAssignableFrom(ParametrePeriodeFiscaleView.class);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final ParametrePeriodeFiscaleView view = (ParametrePeriodeFiscaleView)target;
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
				if (PropertyUtils.getProperty(view, champ) == null ) {
					errors.rejectValue(champ, "error.champ.obligatoire");
				} else if (((RegDate)PropertyUtils.getProperty(view, champ)).isPartial()) {
					errors.rejectValue(champ, "error.date.partielle.interdite");
				} else if (((RegDate)PropertyUtils.getProperty(view, champ)).year() != requestedYear) {
					errors.rejectValue(champ, "error.param.annee.incorrecte",new Object[] {requestedYear.toString()},"erreur sur l''année");
				}
			} catch (Exception e) {
					errors.rejectValue(champ, e.getMessage());
					LOGGER.error(e.getMessage(), e);
			}
		}
		
		
	}

}
