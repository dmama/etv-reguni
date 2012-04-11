package ch.vd.uniregctb.mouvement;

import java.util.HashMap;
import java.util.Map;

import org.springframework.ui.Model;

import ch.vd.uniregctb.common.ApplicationConfig;
import ch.vd.uniregctb.common.CommonMapHelper;
import ch.vd.uniregctb.type.Localisation;
import ch.vd.uniregctb.type.TypeMouvement;

public class MouvementMapHelper extends CommonMapHelper {

	private Map<Localisation, String> mapLocalisations;
	private Map<TypeMouvement, String> mapTypesMouvement;
	private Map<EtatMouvementDossier, String> mapEtatsMouvement;

	public static final String TYPE_MOUVEMENT_MAP_NAME = "typesMouvement";
	public static final String LOCALISATION_MAP_NAME = "localisations";
	public static final String ETAT_MOUVEMENT_MAP_NAME = "etatsMouvement";

	/**
	 * Initialise la map des localisations
	 * @return une map
	 */
	public Map<Localisation, String> getMapLocalisations() {
		if (mapLocalisations == null) {
			mapLocalisations = initMapLocalisations();
		}
		return mapLocalisations;
	}

	private synchronized Map<Localisation, String> initMapLocalisations() {
		if (mapLocalisations == null) {
			//noinspection unchecked
			mapLocalisations = initMapEnum(ApplicationConfig.masterKeyLocalisation, Localisation.class);
		}
		return mapLocalisations;
	}

	/**
	 * Initialise la map des types de mouvement
	 * @return une map
	 */
	public Map<TypeMouvement, String> getMapTypesMouvement() {
		if (mapTypesMouvement == null) {
			mapTypesMouvement = initMapTypesMouvement();
		}
		return mapTypesMouvement;
	}

	private synchronized Map<TypeMouvement, String> initMapTypesMouvement() {
		if (mapTypesMouvement == null) {
			//noinspection unchecked
			mapTypesMouvement = initMapEnum(ApplicationConfig.masterKeyTypeMouvement, TypeMouvement.class);
		}
		return mapTypesMouvement;
	}

	/**
	 * Initialize la map des états des mouvements
	 * @return la map en question
	 */
	public Map<EtatMouvementDossier, String> getMapEtatsMouvement() {
		if (mapEtatsMouvement == null) {
			mapEtatsMouvement = initMapEtatsMouvement();
		}
		return mapEtatsMouvement;
	}

	private synchronized Map<EtatMouvementDossier, String> initMapEtatsMouvement() {
		if (mapEtatsMouvement == null) {
			mapEtatsMouvement = initMapEnum(ApplicationConfig.masterKeyEtatMouvement, EtatMouvementDossier.class, EtatMouvementDossier.RECU_BORDEREAU);
		}
		return mapEtatsMouvement;
	}

	public Map<String, Object> getMaps() {
		final Map<String, Object> map = new HashMap<String, Object>(3);
		map.put(LOCALISATION_MAP_NAME, getMapLocalisations());
		map.put(ETAT_MOUVEMENT_MAP_NAME, getMapEtatsMouvement());
		map.put(TYPE_MOUVEMENT_MAP_NAME, getMapTypesMouvement());
		return map;
	}

	public void putMapsIntoModel(Model model) {
		final Map<String, Object> maps = getMaps();
		for (Map.Entry<String, Object> entry : maps.entrySet()) {
			model.addAttribute(entry.getKey(), entry.getValue());
		}
	}
}
