package ch.vd.uniregctb.di;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.AbstractSimpleFormEditiqueAwareController;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.tiers.TiersMapHelper;

public class AbstractDeclarationImpotController extends AbstractSimpleFormEditiqueAwareController {

	private TiersMapHelper tiersMapHelper;

	private DeclarationImpotMapHelper diMapHelper;

	/**
	 * Ajoute juste une erreur globale de communication éditique dans les erreurs de binding
	 */
	protected static final class ErreurGlobaleCommunicationEditique implements TraitementRetourEditique {

		private final BindException errors;

		public ErreurGlobaleCommunicationEditique(BindException errors) {
			this.errors = errors;
		}

		@Override
		public ModelAndView doJob(EditiqueResultat resultat) {
			errors.reject("global.error.communication.editique");
			return null;
		}
	}

	public void setTiersMapHelper(TiersMapHelper tiersMapHelper) {
		this.tiersMapHelper = tiersMapHelper;
	}

	public void setDiMapHelper(DeclarationImpotMapHelper diMapHelper) {
		this.diMapHelper = diMapHelper;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {

		Map<String, Object> data = new HashMap<String, Object>();
		data.put(ETAT_DOCUMENT_MAP_NAME, tiersMapHelper.getMapTypeEtatDeclaration());
		data.put(PERIODE_FISCALE_MAP_NAME, diMapHelper.initMapPeriodeFiscale());
		data.put(TYPE_DECLARATION_IMPOT_MAP_NAME, tiersMapHelper.getTypesDeclarationImpot());
		data.put(TYPE_DECLARATION_IMPOT_ORD_MAP_NAME, tiersMapHelper.getTypesDeclarationsImpotOrdinaires());
		data.put(TYPE_ADRESSE_RETOUR_MAP_NAME, tiersMapHelper.getTypesAdresseRetour());
		return data;
	}

	/**
	 * Le nom de l'attribut utilise pour la liste des etats d'avancement
	 */
	public static final String ETAT_DOCUMENT_MAP_NAME = "etatsDocument";

	/**
	 * Le nom de l'attribut utilise pour les periodes fiscales
	 */
	public static final String PERIODE_FISCALE_MAP_NAME = "periodesFiscales";

	/**
	 * Le nom de l'attribut utilise pour les types de declaration d'impot
	 */
	public static final String TYPE_DECLARATION_IMPOT_MAP_NAME = "typesDeclarationImpot";

	/**
	 * Le nom de l'attribut utilise pour les types de declarations d'impôt ordinaires (quittancement)
	 */
	public static final String TYPE_DECLARATION_IMPOT_ORD_MAP_NAME = "typesDeclarationImpotOrdinaire";

	/**
	 * Le nom de l'attribut utilise pour les types d'adresse de retour
	 */
	public static final String TYPE_ADRESSE_RETOUR_MAP_NAME = "typesAdresseRetour";

	/**
	 * Le nom de l'objet de criteres de recherche
	 */
	public static final String DI_CRITERIA_NAME = "diCriteria";

	/**
	 * Le nom de l'attribut utilise pour la liste.
	 */
	public static final String DI_LIST_ATTRIBUTE_NAME = "dis";

	/**
	 * Le nom du parametre action pour gerer bouton effacer
	 */
	public final static String ACTION_PARAMETER_NAME = "action";
	public final static String EFFACER_PARAMETER_VALUE = "effacer";

}