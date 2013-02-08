package ch.vd.uniregctb.identification.contribuable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable.Etat;
import ch.vd.uniregctb.identification.contribuable.manager.IdentificationMessagesEditManager;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesEditView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.tiers.AbstractTiersListController;
import ch.vd.uniregctb.tiers.TiersCriteria;

public class IdentificationMessagesEditController extends AbstractTiersListController {

	protected static final Logger LOGGER = Logger.getLogger(IdentificationMessagesEditController.class);

	private IdentificationMessagesEditManager identificationMessagesEditManager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIdentificationMessagesEditManager(IdentificationMessagesEditManager identificationMessagesEditManager) {
		this.identificationMessagesEditManager = identificationMessagesEditManager;
	}

	public static final String PP_CRITERIA_NAME = "identCriteria";
	public final static String BOUTON_EXPERTISER = "expertiser";
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
				if (bean == null || bean.getDemandeIdentificationView().getId().longValue() != id.longValue()) {
					identificationMessagesEditManager.verouillerMessage(id);
					bean = identificationMessagesEditManager.getView(id);
					//gestion des droits
					if (!SecurityHelper.isGranted(securityProvider, Role.VISU_ALL)) {
						if (!SecurityHelper.isGranted(securityProvider, Role.VISU_LIMITE)) {
							throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
						}
						bean.setTypeVisualisation(TiersCriteria.TypeVisualisation.LIMITEE);
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Utilisateur avec visualisation limitée");
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

			identificationMessagesEditManager.deVerouillerMessage(bean.getDemandeIdentificationView().getId(), false);
			removeModuleFromSession(request, PP_CRITERIA_NAME);

			calculerView(request, mav);

		}
		return mav;
	}

	private void calculerView(HttpServletRequest request, ModelAndView mav) {
		final String source = (String) request.getSession().getAttribute(SOURCE_PARAMETER);
		if ("enCours".equals(source)) {
			mav.setView(new RedirectView("listEnCours.do"));
		}


		if ("suspendu".equals(source)) {
			mav.setView(new RedirectView("listSuspendu.do"));
		}
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
		final Long idMessage = bean.getDemandeIdentificationView().getId();
		final IdentificationMessagesEditView messageAModifier = identificationMessagesEditManager.getView(idMessage);
		if (IdentificationMapHelper.isMessageATraiter(messageAModifier)) {


			HttpSession session = request.getSession();


			if (getTarget() != null) {
				if (TARGET_IDENTIFIER.equals(getTarget())) {
					String idCtbAsString = getEventArgument();
					Long idCtb = Long.parseLong(idCtbAsString);
					if (SecurityHelper.isAnyGranted(securityProvider, Role.MW_IDENT_CTB_GEST_BO, Role.MW_IDENT_CTB_ADMIN)) {
						identificationMessagesEditManager.forceIdentification(bean.getDemandeIdentificationView().getId(), idCtb,
								Etat.TRAITE_MAN_EXPERT);
					}
					else if (SecurityHelper.isAnyGranted(securityProvider, Role.MW_IDENT_CTB_CELLULE_BO, Role.NCS_IDENT_CTB_CELLULE_BO, Role.LISTE_IS_IDENT_CTB_CELLULE_BO)) {
						identificationMessagesEditManager.forceIdentification(bean.getDemandeIdentificationView().getId(), idCtb,
								Etat.TRAITE_MANUELLEMENT);
					}
					else {
						throw new AccessDeniedException("Vous ne possédez pas le droit d'identifier un contribuable manuellement");
					}

					identificationMessagesEditManager.deVerouillerMessage(bean.getDemandeIdentificationView().getId(), false);

					calculerView(request, mav);

					return mav;
				}
			}

			if (request.getParameter(BOUTON_EXPERTISER) != null) {
				identificationMessagesEditManager.expertiser(bean.getDemandeIdentificationView().getId());
				identificationMessagesEditManager.deVerouillerMessage(bean.getDemandeIdentificationView().getId(), false);
				// permettre que la vu soit recharger après modif
				removeModuleFromSession(request, PP_CRITERIA_NAME);

				calculerView(request, mav);

				return mav;
			}

			if (request.getParameter(BOUTON_IMPOSSIBLE_A_IDENTIFIER) != null) {
				identificationMessagesEditManager.impossibleAIdentifier(null);
				identificationMessagesEditManager.deVerouillerMessage(bean.getDemandeIdentificationView().getId(), false);

				calculerView(request, mav);

				return mav;
			}

			session.setAttribute(PP_CRITERIA_NAME, bean);
			if (request.getParameter(BOUTON_EFFACER) != null) {
				mav.setView(new RedirectView("edit.do?action=effacer&id=" + bean.getDemandeIdentificationView().getId()));
			}
			else {
				mav.setView(new RedirectView("edit.do?id=" + bean.getDemandeIdentificationView().getId()));
			}

		}
		else {
			//Le message est déjà traité, l'utilisateur s'amuse avec le bouton back
			Flash.warning(String.format("Ce message a déjà été traité, vous avez été redirigé vers la liste de messages en cours"));
			calculerView(request, mav);
		}

		return mav;
	}


}