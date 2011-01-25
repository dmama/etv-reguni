package ch.vd.uniregctb.identification.contribuable;

import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.di.view.DeclarationImpotDetailView;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.identification.contribuable.manager.IdentificationMessagesEditManager;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesEditView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.servlet.ServletService;
import ch.vd.uniregctb.tiers.AbstractTiersListController;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.utils.RegDateEditor;

public class IdentificationMessagesEditController extends AbstractTiersListController {

	protected static final Logger LOGGER = Logger.getLogger(IdentificationMessagesEditController.class);

	private IdentificationMessagesEditManager identificationMessagesEditManager;
	private ServletService servletService;

	public void setIdentificationMessagesEditManager(IdentificationMessagesEditManager identificationMessagesEditManager) {
		this.identificationMessagesEditManager = identificationMessagesEditManager;
	}


	public void setServletService(ServletService servletService) {
		this.servletService = servletService;
	}

	public static final String PP_CRITERIA_NAME = "ppCriteria";
	public final static String BOUTON_EXPERTISER = "expertiser";
	public final static String BOUTON_FICHIER_ACICOM = "fichier_acicom";
	public final static String BOUTON_IMPOSSIBLE_A_IDENTIFIER = "impossibleAIdentifier";
	public final static String TARGET_IDENTIFIER = "identifier";
	public final static String ID_PARAMETER_NAME = "id";
	public final static String SOURCE_PARAMETER = "source";
	public final static String UNLOCK_PARAMETER = "unlock";


	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of IdentificationMessagesEditController:formBackingObject");
		}

		HttpSession session = request.getSession();

		String buttonEffacer = request.getParameter(ACTION_PARAMETER_NAME);
		IdentificationMessagesEditView bean = null;

		if ((buttonEffacer != null) && (buttonEffacer.equals(EFFACER_PARAMETER_VALUE))) {
			removeModuleFromSession(request, PP_CRITERIA_NAME);
			String idAsString = request.getParameter(ID_PARAMETER_NAME);
			if (idAsString != null) {
				Long id = Long.valueOf(idAsString);
				identificationMessagesEditManager.verouillerMessage(id);
				bean = identificationMessagesEditManager.getView(id);
			}
		}
		else {
			bean = (IdentificationMessagesEditView) session.getAttribute(PP_CRITERIA_NAME);
			String idAsString = request.getParameter(ID_PARAMETER_NAME);
			if (idAsString != null) {
				Long id = Long.valueOf(idAsString);
				if ((bean == null) || (bean != null && bean.getDemandeIdentificationView().getId().longValue() != id.longValue())) {
					identificationMessagesEditManager.verouillerMessage(id);
					bean = identificationMessagesEditManager.getView(id);
					//gestion des droits
					if (!SecurityProvider.isGranted(Role.VISU_ALL)) {
						if (!SecurityProvider.isGranted(Role.VISU_LIMITE)) {
							throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
						}
						bean.setTypeVisualisation(TiersCriteriaView.TypeVisualisation.LIMITEE);
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("utilisateur avec visualisation limitée");
						}
					}
				}
			}

			session.setAttribute(PP_CRITERIA_NAME, bean);
		}

		//Permet de savoir si l'on vient de listEnCours
		String parameter = request.getParameter(SOURCE_PARAMETER);
		if (parameter != null) {
			session.setAttribute(SOURCE_PARAMETER, parameter);
		}


		return bean;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException,
	 *      java.util.Map)
	 */

	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
			throws Exception {

		HttpSession session = request.getSession();
		IdentificationMessagesEditView bean = (IdentificationMessagesEditView) session.getAttribute(PP_CRITERIA_NAME);
		ModelAndView mav = showFormForList(request, response, errors, model, PP_CRITERIA_NAME, TIERS_LIST_ATTRIBUTE_NAME, bean, true);


		String parameter = request.getParameter(UNLOCK_PARAMETER);
		if (parameter != null) {

			identificationMessagesEditManager.deVerouillerMessage(bean.getDemandeIdentificationView().getId());
			removeModuleFromSession(request, PP_CRITERIA_NAME);


			mav.setView(new RedirectView("listEnCours.do"));

		}

		return mav;
	}

		@Override
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws ServletException {
		super.initBinder(request, binder);

		// on doit autoriser les dates partielles sur la date de naissance
		binder.registerCustomEditor(RegDate.class, "dateNaissance", new RegDateEditor(true, true));
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		ModelAndView mav = super.onSubmit(request, response, command, errors);

		IdentificationMessagesEditView bean = (IdentificationMessagesEditView) command;
		HttpSession session = request.getSession();


		if (getTarget() != null) {
			if (TARGET_IDENTIFIER.equals(getTarget())) {
				String idCtbAsString = getEventArgument();
				Long idCtb = Long.parseLong(idCtbAsString);
				if (SecurityProvider.isGranted(Role.MW_IDENT_CTB_GEST_BO) || SecurityProvider.isGranted(Role.MW_IDENT_CTB_ADMIN)) {
					identificationMessagesEditManager.forceIdentification(bean.getDemandeIdentificationView().getId(), idCtb,
							Etat.TRAITE_MAN_EXPERT);
				}
				else if (SecurityProvider.isGranted(Role.MW_IDENT_CTB_CELLULE_BO)) {
					identificationMessagesEditManager.forceIdentification(bean.getDemandeIdentificationView().getId(), idCtb,
							Etat.TRAITE_MANUELLEMENT);
				}

				identificationMessagesEditManager.deVerouillerMessage(bean.getDemandeIdentificationView().getId());

				mav.setView(new RedirectView("listEnCours.do"));

				return mav;
			}
		}

		if (request.getParameter(BOUTON_EXPERTISER) != null) {
			identificationMessagesEditManager.expertiser(bean.getDemandeIdentificationView().getId());
			identificationMessagesEditManager.deVerouillerMessage(bean.getDemandeIdentificationView().getId());
			// permettre que la vu soit recharger après modif
			removeModuleFromSession(request, PP_CRITERIA_NAME);

			mav.setView(new RedirectView("listEnCours.do"));

			return mav;
		}

		if (request.getParameter(BOUTON_IMPOSSIBLE_A_IDENTIFIER) != null) {
			identificationMessagesEditManager.impossibleAIdentifier(null);
			identificationMessagesEditManager.deVerouillerMessage(bean.getDemandeIdentificationView().getId());

			mav.setView(new RedirectView("listEnCours.do"));

			return mav;
		}

		if (request.getParameter(BOUTON_FICHIER_ACICOM)!= null) {

			retournerFichierOrigine(request, response, command, errors);

			return null;

		}

		session.setAttribute(PP_CRITERIA_NAME, bean);
		if (request.getParameter(BOUTON_EFFACER) != null) {
			mav.setView(new RedirectView("edit.do?action=effacer&id=" + bean.getDemandeIdentificationView().getId()));
		}
		else {
			mav.setView(new RedirectView("edit.do?id=" + bean.getDemandeIdentificationView().getId()));
		}
		return mav;
	}

	private ModelAndView retournerFichierOrigine(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		IdentificationMessagesEditView bean = (IdentificationMessagesEditView) command;
		final FichierOrigine resultat = identificationMessagesEditManager.getMessageFile(bean.getDemandeIdentificationView().getBusinessId());
		if (resultat != null) {
			servletService.downloadAsFile("message." + resultat.getExtension(), resultat.getContent(), response);
		}
		else {
			//errors.reject("global.error.communication.editique");
		}


		return null;
	}

}