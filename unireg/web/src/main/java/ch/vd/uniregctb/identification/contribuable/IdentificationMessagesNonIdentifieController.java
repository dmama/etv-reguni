package ch.vd.uniregctb.identification.contribuable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.identification.contribuable.manager.IdentificationMessagesEditManager;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesEditView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class IdentificationMessagesNonIdentifieController extends AbstractIdentificationController {

	protected static final Logger LOGGER = Logger.getLogger(IdentificationMessagesNonIdentifieController.class);

	private IdentificationMessagesEditManager identificationMessagesEditManager;

	public void setIdentificationMessagesEditManager(IdentificationMessagesEditManager identificationMessagesEditManager) {
		this.identificationMessagesEditManager = identificationMessagesEditManager;
	}

	public static final String PP_CRITERIA_NAME = "ppCriteria";
	public final static String BOUTON_ANNULER = "annuler";
	public final static String ID_PARAMETER_NAME = "id";
	public final static String SOURCE_PARAMETER = "source";
	public final static String UNLOCK_PARAMETER = "unlock";



	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Start of IdentificationMessagesNonIdentifieController:formBackingObject");
		}

		HttpSession session = request.getSession();

		IdentificationMessagesEditView bean = null;

		bean = (IdentificationMessagesEditView) session.getAttribute(PP_CRITERIA_NAME);
		String idAsString = request.getParameter(ID_PARAMETER_NAME);
		if (idAsString != null) {
			Long id = Long.valueOf(idAsString);
			if ((bean == null) || (bean != null && bean.getDemandeIdentificationView().getId().longValue() != id.longValue())) {
				identificationMessagesEditManager.verouillerMessage(id);
				bean = identificationMessagesEditManager.getView(id);
				// gestion des droits
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

		// Permet de savoir si l'on vient de listEnCours
		String parameter = request.getParameter(SOURCE_PARAMETER);
		if (parameter != null) {
			session.setAttribute(SOURCE_PARAMETER, parameter);
		}

		return bean;
	}



	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		ModelAndView mav = super.onSubmit(request, response, command, errors);

		IdentificationMessagesEditView bean = (IdentificationMessagesEditView) command;
		HttpSession session = request.getSession();



		session.setAttribute(PP_CRITERIA_NAME, bean);
		if (request.getParameter(BOUTON_ANNULER) != null) {
			mav.setView(new RedirectView("edit.do?id=" + bean.getDemandeIdentificationView().getId()));
		} else {


			identificationMessagesEditManager.impossibleAIdentifier(bean);
			identificationMessagesEditManager.deVerouillerMessage(bean.getDemandeIdentificationView().getId());

			mav.setView(new RedirectView("listEnCours.do"));

		}
		return mav;
	}


}