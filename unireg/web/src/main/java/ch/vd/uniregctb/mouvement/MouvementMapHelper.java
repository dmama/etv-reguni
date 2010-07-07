package ch.vd.uniregctb.mouvement;

import java.util.Map;

import ch.vd.uniregctb.common.ApplicationConfig;
import ch.vd.uniregctb.common.CommonMapHelper;
import ch.vd.uniregctb.type.Localisation;
import ch.vd.uniregctb.type.TypeMouvement;

public class MouvementMapHelper extends CommonMapHelper {

	private Map<Localisation, String> mapLocalisations;

	private Map<TypeMouvement, String> mapTypesMouvement;

	private Map<EtatMouvementDossier, String> mapEtatsMouvement;

	/**
	 * Initialise la map des localisations
	 * @return une map
	 */
	public Map<Localisation, String> initMapLocalisation() {
		if (mapLocalisations == null) {
			mapLocalisations = initMapEnum(ApplicationConfig.masterKeyLocalisation, Localisation.class);
		}
		return mapLocalisations;
	}

	/**
	 * Initialise la map des types de mouvement
	 * @return une map
	 */
	public Map<TypeMouvement, String> initMapTypeMouvement() {
		if (mapTypesMouvement == null) {
			mapTypesMouvement = initMapEnum(ApplicationConfig.masterKeyTypeMouvement, TypeMouvement.class);
		}
		return mapTypesMouvement;
	}

	/**
	 * Initialize la map des états des mouvements
	 * @return la map en question
	 */
	public Map<EtatMouvementDossier, String> initMapEtatMouvement() {
		if (mapEtatsMouvement == null) {
			mapEtatsMouvement = initMapEnum(ApplicationConfig.masterKeyEtatMouvement, EtatMouvementDossier.class, EtatMouvementDossier.RECU_BORDEREAU);
		}
		return mapEtatsMouvement;
	}
}
