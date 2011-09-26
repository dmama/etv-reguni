package ch.vd.uniregctb.evenement;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.web.bind.ServletRequestDataBinder;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.evenement.manager.EvenementManager;
import ch.vd.uniregctb.tiers.TiersListController;
import ch.vd.uniregctb.tiers.TiersMapHelper;

public class AbstractEvenementController extends AbstractSimpleFormController {

	protected static final Logger LOGGER = Logger.getLogger(TiersListController.class);

	public AbstractEvenementController() {
		// TracingManager.setActive(true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#initBinder(javax.servlet.http.HttpServletRequest,
	 *      org.springframework.web.bind.ServletRequestDataBinder)
	 */
	@Override
	protected final void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) {
		Locale locale = request.getLocale();
		SimpleDateFormat sdf = new SimpleDateFormat(DateHelper.DATE_FORMAT_DISPLAY, locale);
		sdf.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(sdf, true));
		NumberFormat numberFormat = NumberFormat.getInstance(locale);
		numberFormat.setGroupingUsed(true);
		binder.registerCustomEditor(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, numberFormat, true));
		binder.registerCustomEditor(Integer.class, new CustomNumberEditor(Integer.class, numberFormat, true));
		binder.registerCustomEditor(Long.class, new CustomNumberEditor(Long.class, numberFormat, true));
		binder.registerCustomEditor(List.class, new CustomCollectionEditor(List.class, true));
		binder.registerCustomEditor(List.class, new CustomCollectionEditor(Set.class, true));
		binder.registerCustomEditor(boolean.class, new CustomBooleanEditor(true));
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {

		// TracePoint tp = TracingManager.begin();
		// TracingManager.end(tp);
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(TYPE_RECHERCHE_NOM_MAP_NAME, getTiersMapHelper().getMapTypeRechercheNom());
		data.put(TYPE_EVENEMENT_MAP_NAME, getTiersMapHelper().getMapTypeEvenementCivil());
		data.put(ETAT_EVENEMENT_MAP_NAME, getTiersMapHelper().getMapEtatsEvenementCivil());
		data.put(ETAT_EVENEMENT_MAP_NAME, getTiersMapHelper().getMapEtatsEvenementCivil());

		return data;
	}

	/**
	 * Le nom de l'objet de criteres de recherche
	 */
	public static final String EVENEMENT_CRITERIA_NAME = "evenementCriteria";

	/**
	 * Le nom de l'attribut utilise pour la liste.
	 */
	public static final String EVENEMENT_LIST_ATTRIBUTE_NAME = "listEvenements";

	/**
	 * Le nom de l'attribut utilise pour la taille de la liste
	 */
	public static final String EVENEMENT_LIST_ATTRIBUTE_SIZE = "listEvenementsSize";

	/**
	 * Le nom de l'attribut utilisé pour les informations de pagination en cas de retour
	 */
	public static final String EVENEMENT_LIST_PAGE_INFO = "listEvenementsPageInfo";

	/**
	 * Le nom de l'attribut utilise pour la liste des types de recherche par nom
	 */
	public static final String TYPE_RECHERCHE_NOM_MAP_NAME = "typesRechercheNom";

	/**
	 * Le nom de l'attribut utilise pour la liste des types d'evenement
	 */
	public static final String TYPE_EVENEMENT_MAP_NAME = "typesEvenement";

	/**
	 * Le nom de l'attribut utilise pour la liste des etats d'evenement
	 */
	public static final String ETAT_EVENEMENT_MAP_NAME = "etatsEvenement";

	/**
	 * Le nom de l'id de l'evenement
	 */
	public static final String EVENEMENT_ID_PARAMETER_NAME = "id";

	/**
	 * Le tiersHelper.
	 */
	private TiersMapHelper tiersMapHelper;

	private EvenementManager evenementManager;

	public EvenementManager getEvenementManager() {
		return evenementManager;
	}

	public void setEvenementManager(EvenementManager evenementManager) {
		this.evenementManager = evenementManager;
	}

	public TiersMapHelper getTiersMapHelper() {
		return tiersMapHelper;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}
}
