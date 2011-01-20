package ch.vd.uniregctb.mouvement;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import ch.vd.uniregctb.common.AbstractSimpleFormController;

public class AbstractMouvementController extends AbstractSimpleFormController{

	private MouvementMapHelper mouvementMapHelper;

	public MouvementMapHelper getMouvementMapHelper() {
		return mouvementMapHelper;
	}

	public void setMouvementMapHelper(MouvementMapHelper mouvementMapHelper) {
		this.mouvementMapHelper = mouvementMapHelper;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		data.put(TYPE_MOUVEMENT_MAP_NAME, mouvementMapHelper.initMapTypeMouvement());
		data.put(LOCALISATION_MAP_NAME, mouvementMapHelper.initMapLocalisation());
		data.put(ETAT_MOUVEMENT_MAP_NAME, mouvementMapHelper.initMapEtatMouvement());
		return data;
	}

	/**
	 * Le nom de l'attribut utilise pour la liste des types de mouvement
	 */
	public static final String TYPE_MOUVEMENT_MAP_NAME = "typesMouvement";

	/**
	 * Le nom de l'attribut utilise pour la liste des localisations
	 */
	public static final String LOCALISATION_MAP_NAME = "localisations";

	/**
	 * Les états d'un mouvement
	 */
	public static final String ETAT_MOUVEMENT_MAP_NAME = "etatsMouvement";

}
