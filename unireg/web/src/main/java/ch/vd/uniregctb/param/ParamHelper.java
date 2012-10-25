package ch.vd.uniregctb.param;


import java.util.Map;

import ch.vd.uniregctb.common.ApplicationConfig;
import ch.vd.uniregctb.common.CommonMapHelper;
import ch.vd.uniregctb.type.ModeleFeuille;

/**
 * Cette classe expose les différents enums sours forme de map enum->description.
 */

public class ParamHelper extends CommonMapHelper {
	private Map<ModeleFeuille, String> mapModeleFeuilleComplete;
	private Map<ModeleFeuille, String> mapModeleFeuilleVaudTax;
	private Map<ModeleFeuille, String> mapModeleFeuilleDepense;
	private Map<ModeleFeuille, String> mapModeleFeuilleHC;



	public Map<ModeleFeuille, String> getMapModeleFeuilleForComplete() {
		if (mapModeleFeuilleComplete == null) {
			mapModeleFeuilleComplete = initMapEnum(ApplicationConfig.masterKeyModelFeuille, ModeleFeuille.class,
					ModeleFeuille.ANNEXE_200,
					ModeleFeuille.ANNEXE_250,
					ModeleFeuille.ANNEXE_270
					);
		}
		return mapModeleFeuilleComplete;
	}

	public Map<ModeleFeuille, String> getMapModeleFeuilleForVaudTax() {
		if (mapModeleFeuilleVaudTax == null) {
			mapModeleFeuilleVaudTax = initMapEnum(ApplicationConfig.masterKeyModelFeuille, ModeleFeuille.class,
					ModeleFeuille.ANNEXE_200,
					ModeleFeuille.ANNEXE_210,
					ModeleFeuille.ANNEXE_220,
					ModeleFeuille.ANNEXE_230,
					ModeleFeuille.ANNEXE_240,
					ModeleFeuille.ANNEXE_270,
					ModeleFeuille.ANNEXE_310,
					ModeleFeuille.ANNEXE_320,
					ModeleFeuille.ANNEXE_330
					);
		}
		return mapModeleFeuilleVaudTax;
	}

	public Map<ModeleFeuille, String> getMapModeleFeuilleForDepense() {
		if (mapModeleFeuilleDepense == null) {
			mapModeleFeuilleDepense = initMapEnum(ApplicationConfig.masterKeyModelFeuille, ModeleFeuille.class,
					ModeleFeuille.ANNEXE_200,
					ModeleFeuille.ANNEXE_210,
					ModeleFeuille.ANNEXE_220,
					ModeleFeuille.ANNEXE_230,
					ModeleFeuille.ANNEXE_240,
					ModeleFeuille.ANNEXE_250,
					ModeleFeuille.ANNEXE_310,
					ModeleFeuille.ANNEXE_320,
					ModeleFeuille.ANNEXE_330
					);
		}
		return mapModeleFeuilleDepense;
	}
	public Map<ModeleFeuille, String> getMapModeleFeuilleForHC() {
		if (mapModeleFeuilleHC == null) {
			mapModeleFeuilleHC = initMapEnum(ApplicationConfig.masterKeyModelFeuille, ModeleFeuille.class,
					ModeleFeuille.ANNEXE_210,
					ModeleFeuille.ANNEXE_220,
					ModeleFeuille.ANNEXE_230,
					ModeleFeuille.ANNEXE_240,
					ModeleFeuille.ANNEXE_250,
					ModeleFeuille.ANNEXE_270,
					ModeleFeuille.ANNEXE_310,
					ModeleFeuille.ANNEXE_320,
					ModeleFeuille.ANNEXE_330
					);
		}
		return mapModeleFeuilleHC;
	}
}
