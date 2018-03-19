package ch.vd.unireg.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.AbstractSimpleFormController;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.param.manager.ParamApplicationManager;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.view.TiersCriteriaView;

/**
 * Controller spring permettant la visualisation ou la saisie d'une objet métier donné.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public abstract class AbstractTiersController extends AbstractSimpleFormController {

	public static final String URL_RETOUR_SESSION_NAME = "urlRetour";

	protected TiersService tiersService;
	private ParamApplicationManager paramApplicationManager;
	protected SecurityProviderInterface securityProvider;

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {

		Map<String, Object> data = new HashMap<>();
		data.put(TYPE_RECHERCHE_NOM_MAP_NAME, tiersMapHelper.getMapTypeRechercheNom());
		data.put(TYPE_RECHERCHE_LOCALITE_PAYS_MAP_NAME, tiersMapHelper.getMapTypeRechercheLocalitePays());
		data.put(TYPE_RECHERCHE_FOR_FISCAL, tiersMapHelper.getMapTypeRechercheForFiscal());
		data.put(FORME_JURIDIQUE_MAP_NAME, tiersMapHelper.getMapFormesJuridiquesEntreprise());
		data.put(CATEGORIE_ENTREPRISE_MAP_NAME, tiersMapHelper.getMapCategoriesEntreprise());
		data.put(NATURE_JURIDIQUE_MAP_NAME, tiersMapHelper.getMapNatureJuridique());
		data.put(CATEGORIE_ETRANGER_MAP_NAME, tiersMapHelper.getMapCategorieEtranger());
		data.put(SEXE_MAP_NAME, tiersMapHelper.getMapSexe());
		data.put(RATTACHEMENT_MAP_NAME, tiersMapHelper.getMapRattachement());
		data.put(GENRE_IMPOT_MAP_NAME, tiersMapHelper.getMapGenreImpot());
		data.put(TYPE_FOR_FISCAL_MAP_NAME, tiersMapHelper.getMapTypeAutoriteFiscale());
		data.put(TYPE_FOR_FISCAL_DPI_MAP_NAME, tiersMapHelper.getMapTypeAutoriteFiscaleDPI());
		data.put(MODE_IMPOSITION_MAP_NAME, tiersMapHelper.getMapModeImposition());
		data.put(MODE_COMMUNICATION_MAP_NAME, tiersMapHelper.getMapModeCommunication());
		data.put(PERIODICITE_DECOMPTE_MAP_NAME, tiersMapHelper.getMapPeriodiciteDecompte());
		data.put(CATEGORIE_IMPOT_SOURCE_MAP_NAME, tiersMapHelper.getMapCategorieImpotSource());
		data.put(TEXTE_CASE_POSTALE_MAP_NAME, tiersMapHelper.getMapTexteCasePostale());
		data.put(ETAT_CIVIL, tiersMapHelper.getMapEtatsCivil());
		data.put(TYPE_ADRESSE_TIERS, tiersMapHelper.getMapTypeAdresse());
		data.put(TYPE_ADRESSE_FISCALE_TIERS,tiersMapHelper.getMapTypeAdresseFiscale());
		data.put(TARIF_IMPOT_SOURCE_MAP_NAME, tiersMapHelper.getTarifsImpotSource());
		data.put(PERIODE_DECOMPTE_MAP_NAME, tiersMapHelper.getPeriodeDecomptes());
		data.put(PARAMETRES_APP, paramApplicationManager.getForm());

		return data;
	}

	/**
	 * Removes the mapping for this module.
	 *
	 * @param request HttpRequest
	 * @param module  Name of the specific module
	 */
	public static void removeModuleFromSession(HttpServletRequest request, String module) {
		HttpSession session = request.getSession(true);
		session.removeAttribute(module);
	}

	public static final String ACTION_COMMON_REFRESH = "refresh";

	/**
	 * Le nom du parametre action pour gerer bouton effacer
	 */
	public static final String ACTION_PARAMETER_NAME = "action";
	public static final String EFFACER_PARAMETER_VALUE = "effacer";

	/**
	 * Le nom du parametre utilise dans la request.
	 */
	public static final String TIERS_ID_PARAMETER_NAME = "id";

	/**
	 * La nature du tiers
	 */
	public static final String TIERS_NATURE_PARAMETER_NAME = "nature";

	/**
	 * Le type du submit
	 */
	public static final String TYPE_SUBMIT_PARAMETER_NAME = "typeSubmit";

	/**
	 * La valeur creeFor pour le type du submit
	 */
	public static final String TYPE_SUBMIT_CREE_FOR_VALUE = "creeFor";

	/**
	 * La valeur creeModeImp pour le type du submit
	 */
	public static final String TYPE_SUBMIT_CREE_MODE_IMP_VALUE = "creeModeImp";

	/**
	 * La valeur nature pour le type du submit
	 */
	public static final String TYPE_SUBMIT_NATURE_VALUE = "nature";

	/**
	 * Le nom de l'attribut utilise pour la liste.
	 */
	public static final String TIERS_LIST_ATTRIBUTE_NAME = "list";

	/**
	 * Le nom de l'attribut utilise pour la liste des formes juridiques
	 */
	public static final String FORME_JURIDIQUE_MAP_NAME = "formesJuridiquesEnum";

	/**
	 * Le nom de l'attribut utilise pour la liste des catégories d'entreprise
	 */
	public static final String CATEGORIE_ENTREPRISE_MAP_NAME = "categoriesEntreprisesEnum";

	/**
	 * Le nom de l'attribut utilise pour la liste des natures juridiques
	 */
	public static final String NATURE_JURIDIQUE_MAP_NAME = "naturesJuridiques";

	/**
	 * Le nom de l'attribut utilise pour la liste des roles
	 */
	public static final String ROLE_MAP_NAME = "roles";

	/**
	 * Le nom de l'attribut utilise pour la liste des situations de familles
	 */
	public static final String SITUATION_FISCALE_MAP_NAME = "situationsFiscales";

	/**
	 * Le nom de l'attribut utilise pour la liste des situations de familles
	 */
	public static final String CATEGORIE_ETRANGER_MAP_NAME = "categoriesEtrangers";

	/**
	 * Le nom de l'attribut utilise pour la liste des sexes
	 */
	public static final String SEXE_MAP_NAME = "sexes";

	/**
	 * Le nom de l'attribut utilise pour la liste des genres d'impots
	 */
	public static final String GENRE_IMPOT_MAP_NAME = "genresImpot";

	/**
	 * Le nom de l'attribut utilise pour la liste des types de for fiscal
	 */
	public static final String TYPE_FOR_FISCAL_MAP_NAME = "typesForFiscal";

	/**
	 * Le nom de l'attribut utilise pour la liste des types de for fiscal des DPI
	 */
	public static final String TYPE_FOR_FISCAL_DPI_MAP_NAME = "typesForFiscalDPI";

	/**
	 * Le nom de l'attribut utilise pour la liste des rattachements
	 */
	public static final String RATTACHEMENT_MAP_NAME = "rattachements";

	/**
	 * Le nom de l'attribut utilise pour la liste des types de recherche par nom
	 */
	public static final String TYPE_RECHERCHE_NOM_MAP_NAME = "typesRechercheNom";

	/**
	 * Le nom de l'attribut utilise pour la liste des types de recherche par localite/pays
	 */
	public static final String TYPE_RECHERCHE_LOCALITE_PAYS_MAP_NAME = "typesRechercheLocalitePays";

	/**
	 * Le nom de l'attribut utilise pour la liste des types de recherche par localite/pays
	 */
	public static final String TYPE_RECHERCHE_FOR_FISCAL = "typesRechercheForFiscal";

	/**
	 * Le nom de l'attribut utilise pour la liste des categories d'impot ordinaire
	 */
	public static final String MODE_IMPOSITION_MAP_NAME = "modesImposition";

	/**
	 * Le nom de l'attribut utilise pour la liste des categories d'impot a la source
	 */
	public static final String CATEGORIE_IMPOT_SOURCE_MAP_NAME = "categoriesImpotSource";

	/**
	 * Le nom de l'attribut utilise pour la periodicite decompte
	 */
	public static final String PERIODICITE_DECOMPTE_MAP_NAME = "periodicitesDecompte";

	/**
	 * Le nom de l'attribut utilise pour les textes case postal
	 */
	public static final String TEXTE_CASE_POSTALE_MAP_NAME = "textesCasePostale";

	/**
	 * Le nom de l'attribut utilise pour les modes de communication
	 */
	public static final String MODE_COMMUNICATION_MAP_NAME = "modesCommunication";

	/**
	 * Le nom de l'attribut utilise pour les tarifs impot a la source
	 */
	public static final String TARIF_IMPOT_SOURCE_MAP_NAME = "tarifsImpotSource";

	/**
	 * Le nom de l'attribut utilise pour les mois
	 */
	public static final String PERIODE_DECOMPTE_MAP_NAME = "periodeDecomptes";

	/**
	 * Le nom de l'attribut utilise pour l'objet stockant les paramètres de l'application
	 */
	public static final String PARAMETRES_APP = "parametresApp";

	/**
	 * Le nom de l'objet de criteres de recherche
	 */
	public static final String TIERS_CRITERIA_NAME = "tiersCriteria";

	/**
	 * Etat Civil
	 */
	public static final String ETAT_CIVIL = "etatCivil";

	/**
	 * Etat Civil - SEPARATION
	 */
	public static final String SEPARATION = "separations";

	/**
	 * Etat Civil - PARTENARIAT DISSOUS
	 */
	public static final String PARTENARIAT_DISSOUS = "partenariatsDissous";

	/**
	 * Type Adresse Tiers
	 */
	public static final String TYPE_ADRESSE_TIERS = "typeAdresseTiers";

	/**
	 * Type Adresse Fiscale autorisé pour les Tiers
	 */
	public static final String TYPE_ADRESSE_FISCALE_TIERS = "typeAdresseFiscaleTiers";

	public static final String BUTTON_BACK_TO_LIST = "retourList";

	public static final String BUTTON_BACK_TO_VISU = "retourVisualisation";

	public static final String BUTTON_SAVE = "__confirmed_save";

	protected TiersMapHelper tiersMapHelper;

	/**
	 * @param tiersMapHelper the tiersMapHelper to set
	 */
	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setParamApplicationManager(ParamApplicationManager paramApplicationManager) {
		this.paramApplicationManager = paramApplicationManager;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	protected List<TiersIndexedDataView> searchTiers(TiersCriteriaView bean) {
		final List<TiersIndexedData> results = tiersService.search(bean.asCore());
		Assert.notNull(results);

		final List<TiersIndexedDataView> list = new ArrayList<>(results.size());
		for (TiersIndexedData d : results) {
			list.add(new TiersIndexedDataView(d));
		}

		return list;
	}

	protected static boolean getBooleanParam(HttpServletRequest request, String paramName) {
		return getBooleanParam(request, paramName, false);
	}

	protected static boolean getBooleanParam(HttpServletRequest request, String paramName, boolean defaultValue) {
		final Boolean value = getOptionalBooleanParam(request, paramName);
		return value == null ? defaultValue : value;
	}

	@Nullable
	protected static Boolean getOptionalBooleanParam(HttpServletRequest request, String paramName) {
		final String param = request.getParameter(paramName);
		return StringUtils.isBlank(param) ? null : param.equalsIgnoreCase("true");
	}
}
