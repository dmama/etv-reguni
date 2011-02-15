package ch.vd.uniregctb.identification.contribuable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.identification.contribuable.manager.IdentificationMessagesEditManager;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesEditView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.servlet.ServletService;
import ch.vd.uniregctb.tiers.AbstractTiersListController;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public class VoirMessageIdentificationController extends AbstractTiersListController {

	private IdentificationMessagesEditManager identificationMessagesEditManager;
	private ServletService servletService;
	public static final String PP_CRITERIA_NAME = "identCriteria";
	public final static String ID_PARAMETER_NAME = "id";

	public void setIdentificationMessagesEditManager(IdentificationMessagesEditManager identificationMessagesEditManager) {
		this.identificationMessagesEditManager = identificationMessagesEditManager;
	}


	public void setServletService(ServletService servletService) {
		this.servletService = servletService;
	}

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		HttpSession session = request.getSession();


		IdentificationMessagesEditView bean = null;


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


		return bean;
	}


	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
			throws Exception {

		HttpSession session = request.getSession();
		IdentificationMessagesEditView bean = (IdentificationMessagesEditView) session.getAttribute(PP_CRITERIA_NAME);
		retournerFichierOrigine(request, response, bean, errors);

		//ModelAndView mav = showFormForList(request, response, errors, model, PP_CRITERIA_NAME, TIERS_LIST_ATTRIBUTE_NAME, bean, true);
		return null;
		//return mav;
	}


	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		ModelAndView mav = super.onSubmit(request, response, command, errors);

		IdentificationMessagesEditView bean = (IdentificationMessagesEditView) command;
		HttpSession session = request.getSession();
		retournerFichierOrigine(request, response, command, errors);
        return null;
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
