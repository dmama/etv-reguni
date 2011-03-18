package ch.vd.uniregctb.lr;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import ch.vd.uniregctb.common.AbstractSimpleFormEditiqueAwareController;
import ch.vd.uniregctb.tiers.TiersMapHelper;

public class AbstractListeRecapController extends AbstractSimpleFormEditiqueAwareController {

	private TiersMapHelper tiersMapHelper;

	public TiersMapHelper getTiersMapHelper() {
		return tiersMapHelper;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {

		// TracePoint tp = TracingManager.begin();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(MODE_COMMUNICATION_MAP_NAME, getTiersMapHelper().getMapModeCommunication());
		data.put(PERIODICITE_DECOMPTE_MAP_NAME, getTiersMapHelper().getMapPeriodiciteDecompte());
		data.put(CATEGORIE_IMPOT_SOURCE_MAP_NAME, getTiersMapHelper().getMapCategorieImpotSource());
		data.put(ETAT_DOCUMENT_MAP_NAME, getTiersMapHelper().getMapTypeEtatDeclaration());
		// TracingManager.end(tp);

		return data;
	}

	/**
	 * Le nom de l'attribut utilise pour les modes de communication
	 */
	public static final String MODE_COMMUNICATION_MAP_NAME = "modesCommunication";

	/**
	 * Le nom de l'attribut utilise pour la periodicite decompte
	 */
	public static final String PERIODICITE_DECOMPTE_MAP_NAME = "periodicitesDecompte";

	/**
	 * Le nom de l'attribut utilise pour la liste des categories d'impot a la source
	 */
	public static final String CATEGORIE_IMPOT_SOURCE_MAP_NAME = "categoriesImpotSource";

	/**
	 * Le nom de l'attribut utilise pour la liste des categories d'impot a la source
	 */
	public static final String ETAT_DOCUMENT_MAP_NAME = "etatsDocument";

	/**
	 * Le nom de l'objet de criteres de recherche
	 */
	public static final String LR_CRITERIA_NAME = "lrCriteria";

	/**
	 * Le nom de l'attribut utilise pour la liste.
	 */
	public static final String LR_LIST_ATTRIBUTE_NAME = "lrs";

}
