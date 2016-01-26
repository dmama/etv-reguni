package ch.vd.uniregctb.tiers.validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public abstract class AbstractRegimeFiscalViewValidator {

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


}
