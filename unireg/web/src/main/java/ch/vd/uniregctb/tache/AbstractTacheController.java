package ch.vd.uniregctb.tache;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import ch.vd.uniregctb.common.AbstractSimpleFormEditiqueAwareController;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.tiers.TiersMapHelper;

public class AbstractTacheController extends AbstractSimpleFormEditiqueAwareController {

	private TiersMapHelper tiersMapHelper;

	private TacheMapHelper tacheMapHelper;

	public TiersMapHelper getTiersMapHelper() {
		return tiersMapHelper;
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public TacheMapHelper getTacheMapHelper() {
		return tacheMapHelper;
	}

	public void setTacheMapHelper(TacheMapHelper tacheMapHelper) {
		this.tacheMapHelper = tacheMapHelper;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		data.put(PERIODE_FISCALE_MAP_NAME, tacheMapHelper.initMapPeriodeFiscale());
		data.put(OFFICE_IMPOT_UTILISATEUR_MAP_NAME, tacheMapHelper.initMapOfficeImpotUtilisateur());
		data.put(ETAT_TACHE_MAP_NAME, tacheMapHelper.initMapEtatTache());
		data.put(TYPE_TACHE_MAP_NAME, tacheMapHelper.initMapTypeTache());
		return data;
	}

	/**
	 * Le nom de l'attribut utilise pour la liste des offices d'impôt de l'utilisateur
	 */
	public static final String OFFICE_IMPOT_UTILISATEUR_MAP_NAME = "officesImpotUtilisateur";


	/**
	 * Le nom de l'attribut utilise pour la liste des etats de tache
	 */
	public static final String ETAT_TACHE_MAP_NAME = "etatsTache";

	/**
	 * Le nom de l'attribut utilise pour la liste des etats de tache
	 */
	public static final String TYPE_TACHE_MAP_NAME = "typesTache";

	/**
	 * Le nom de l'attribut utilise pour les periodes fiscales
	 */
	public static final String PERIODE_FISCALE_MAP_NAME = "periodesFiscales";

	/**
	 * Bouton effacer
	 */
	public static final String BOUTON_EFFACER = "effacer";

	/**
	 * Removes the mapping for this module.
	 * @param	request	HttpRequest
	 * @param	module	Name of the specific module
	 */
	public static void removeModuleFromSession(HttpServletRequest request, String module) {
		HttpSession session = request.getSession(true);
		session.removeAttribute(module);

	}

	/**
	 * @return le sigle de l'office d'impôt utilisé actuellement par l'utilisateur, ou TOUS si le sigle ne peut être déterminé.
	 */
	protected static String getDefaultOID() {
		Integer officeImpot = AuthenticationHelper.getCurrentOID();
		if (officeImpot == null) {
			return "TOUS";
		}
		return officeImpot.toString();
	}

	protected static boolean isAppuiSurEffacer(HttpServletRequest request) {
		return request.getParameter(BOUTON_EFFACER) != null;
	}

	@Override
	protected boolean isFormSubmission(HttpServletRequest request) {
		// le bouton "effacer" n'est pas une réelle soumission de formulaire!
		return super.isFormSubmission(request) && !isAppuiSurEffacer(request);
	}

}