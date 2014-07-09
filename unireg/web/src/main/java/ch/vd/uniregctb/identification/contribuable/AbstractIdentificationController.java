package ch.vd.uniregctb.identification.contribuable;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.CustomBooleanEditor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.web.bind.ServletRequestDataBinder;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.evenement.identification.contribuable.Demande;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.evenement.identification.contribuable.TypeDemande;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tache.TacheMapHelper;
import ch.vd.uniregctb.utils.RegDateEditor;

public class AbstractIdentificationController extends AbstractSimpleFormController {

	protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractIdentificationController.class);

	protected TacheMapHelper tacheMapHelper;
	protected IdentificationMapHelper identificationMapHelper;
	protected SecurityProviderInterface securityProvider;

	public void setTacheMapHelper(TacheMapHelper tacheMapHelper) {
		this.tacheMapHelper = tacheMapHelper;
	}

	public void setIdentificationMapHelper(IdentificationMapHelper identificationMapHelper) {
		this.identificationMapHelper = identificationMapHelper;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public AbstractIdentificationController() {
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
		binder.registerCustomEditor(RegDate.class, "dateNaissance", new RegDateEditor(true, true, false));
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {
		// TracePoint tp = TracingManager.begin();
		// TracingManager.end(tp);
		Map<String, Object> data = new HashMap<>();
		data.put(PERIODE_FISCALE_MAP_NAME,initMapPeriodeFiscale());
		data.put(EMETTEUR_MAP_NAME, initMapEmetteurId());
		data.put(ETAT_MESSAGE_MAP_NAME, initMapEtatMessage());
		data.put(TYPE_MESSAGE_MAP_NAME, initMapTypeMessage());
		data.put(PRIORITE_EMETTEUR_MAP_NAME, initMapPrioriteEmetteur());
		data.put(ERREUR_MESSAGE_MAP_NAME, identificationMapHelper.initErreurMessage());
		data.put(TRAITEMENT_USER_MAP_NAME, identificationMapHelper.initMapUser());
		return data;
	}

	protected boolean areUsed(String parametreEtat, String parametrePeriode, String parametreTypeMessage) {
		return (parametreEtat != null) || (parametrePeriode != null) || (parametreTypeMessage != null);
	}

	/**
	 * Initialise la map des états du message en fonction du type de controleur
	 *
	 * @return une map
	 */
	protected Map<Etat, String> initMapEtatMessage() {
		return null;

	}

    /**
	 * Initialise la map des emetteurs du message en fonction des etats et du type de controleur
	 *
	 * @return une map
	 */
	protected Map<String, String> initMapEmetteurId() {
		return null;

	}

	/**
	 * Initialise la map des emetteurs du message en fonction des etats et du type de controleur
	 *
	 * @return une map
	 */
	protected Map<String, String> initMapTypeMessage() {
		return null;

	}

		/**
	 * Initialise la map des prioritées du message en fonction des etats et du type de controleur
	 *
	 * @return une map
	 */
	protected Map<Demande.PrioriteEmetteur, String> initMapPrioriteEmetteur() {
		return null;

	}

		/**
	 * Initialise la map des périodes fiscales du message en fonction des etats et du type de controleur
	 *
	 * @return une map
	 */
	protected Map<Integer, String> initMapPeriodeFiscale() {
		return null;

	}


	/**
	 * Le nom de l'objet de criteres de recherche
	 */
	public static final String IDENTIFICATION_CRITERIA_NAME = "identificationCriteria";

	/**
	 * Le nom de l'attribut utilise pour la liste.
	 */
	public static final String IDENTIFICATION_LIST_ATTRIBUTE_NAME = "listIdentifications";

	/**
	 * Le nom de l'attribut utilise pour la taille de la liste
	 */
	public static final String IDENTIFICATION_LIST_ATTRIBUTE_SIZE = "listIdentificationsSize";

	/**
	 * Le nom de l'attribut utilise pour la liste des types d'identification
	 */
	public static final String TYPE_MESSAGE_MAP_NAME = "typesMessage";

	/**
	 * Le nom de l'attribut utilise pour la liste des émetteurs
	 */
	public static final String EMETTEUR_MAP_NAME = "emetteurs";

	/**
	 * Le nom de l'attribut utilise pour la liste des etats du message
	 */
	public static final String ETAT_MESSAGE_MAP_NAME = "etatsMessage";

	/**
	 * Le nom de l'attribut utilise pour la liste des etats du message
	 */
	public static final String ERREUR_MESSAGE_MAP_NAME = "erreursMessage";


	/**
	 * Le nom de l'attribut utilise pour la liste des priorités
	 */
	public static final String PRIORITE_EMETTEUR_MAP_NAME = "priorites";

	/**
	 * Le nom de l'attribut utilise pour la liste des priorités
	 */
	public static final String TRAITEMENT_USER_MAP_NAME = "traitementUsers";


	/**
	 * Le nom de l'id de l'identification
	 */
	public static final String IDENTIFICATION_ID_PARAMETER_NAME = "id";

	/**
	 * Le nom de l'attribut utilise pour les periodes fiscales
	 */
	public static final String PERIODE_FISCALE_MAP_NAME = "periodesFiscales";


	/**
	 * nom de l'action  de type archiveMessage
	 */
	public static final String IDENTIFICATION_TRAITE_MESSAGE = "messageTraite";

	/**
	 * nom de l'action  de type archiveMessage
	 */
	public static final String IDENTIFICATION_EN_COURS_MESSAGE = "messageEnCours";

	/**
	 * Paramètres venant du tableau de bord
	 */

	public static final String ETAT_PARAMETER_NAME = "etat";

	public static final String TYPE_MESSAGE_PARAMETER_NAME = "typeMessage";

	public static final String PERIODE_PARAMETER_NAME = "periode";

	/**
	 * Retourne les types de demandes autorisés par rapports aux droits du principal courant
	 * @return un tableau (potentiellement vide...) des types autorisés
	 */
	protected TypeDemande[] getAllowedTypes() {
		final Set<TypeDemande> types = new HashSet<>();
		if (SecurityHelper.isAnyGranted(securityProvider, Role.MW_IDENT_CTB_CELLULE_BO, Role.MW_IDENT_CTB_ADMIN, Role.MW_IDENT_CTB_VISU, Role.MW_IDENT_CTB_GEST_BO)) {
			types.add(TypeDemande.MELDEWESEN);
			types.add(TypeDemande.NCS);
			types.add(TypeDemande.IMPOT_SOURCE);
		}
		if (SecurityHelper.isGranted(securityProvider, Role.NCS_IDENT_CTB_CELLULE_BO)) {
			types.add(TypeDemande.NCS);
		}
		if (SecurityHelper.isGranted(securityProvider, Role.LISTE_IS_IDENT_CTB_CELLULE_BO)) {
			types.add(TypeDemande.IMPOT_SOURCE);
		}

		if (types.size() == 0) {
			// aucun droit... plutôt que tous
			return new TypeDemande[] { null };
		}
		else {
			return types.toArray(new TypeDemande[types.size()]);
		}
	}
}
