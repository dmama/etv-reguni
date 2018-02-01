package ch.vd.unireg.tiers.validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.Errors;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.view.DateRangeViewValidator;
import ch.vd.unireg.tiers.view.ValidableRegimeFiscalView;

public abstract class AbstractRegimeFiscalViewValidator {

	private static final DateRangeViewValidator RANGE_VALIDATOR = new DateRangeViewValidator(false, true, true, true);

	private ServiceInfrastructureService infraService;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	/**
	 * Reconstitution d'une map des régimes fiscaux disponibles indexés par code
	 * @return la map en question
	 */
	@NotNull
	protected Map<String, TypeRegimeFiscal> getMapRegimesFiscauxParCode() {
		// map des régimes fiscaux existants indexés par code
		final List<TypeRegimeFiscal> typesRegime = infraService.getRegimesFiscaux();
		final Map<String, TypeRegimeFiscal> mapRegimesParCode = new HashMap<>(typesRegime.size());
		for (TypeRegimeFiscal type : typesRegime) {
			mapRegimesParCode.put(type.getCode(), type);
		}
		return mapRegimesParCode;
	}

	protected void doValidate(ValidableRegimeFiscalView view, Errors errors) {

		final DateRange range = new DateRangeHelper.Range(view.getDateDebut(), null);

		// validations de base sur le range lui-même
		RANGE_VALIDATOR.validate(range, errors);

		// la portée est obligatoire
		if (view.getPortee() == null) {
			errors.rejectValue("portee", "error.portee.obligatoire");
		}

		// le code du régime
		if (StringUtils.isBlank(view.getCode())) {
			errors.rejectValue("code", "error.type.regime.fiscal.obligatoire");
		}
		else {
			final Map<String, TypeRegimeFiscal> types = getMapRegimesFiscauxParCode();
			if (types.get(view.getCode()) == null) {
				errors.rejectValue("code", "error.type.regime.fiscal.invalide");
			}
		}
	}
}
