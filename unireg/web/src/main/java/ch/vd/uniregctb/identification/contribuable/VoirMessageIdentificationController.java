package ch.vd.uniregctb.identification.contribuable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.URL;
import java.util.Map;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.MimeTypeHelper;
import ch.vd.uniregctb.identification.contribuable.manager.IdentificationMessagesEditManager;
import ch.vd.uniregctb.identification.contribuable.view.IdentificationMessagesEditView;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.servlet.ServletService;
import ch.vd.uniregctb.tiers.AbstractTiersListController;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.utils.HttpDocumentFetcher;

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

		final HttpSession session = request.getSession();
		IdentificationMessagesEditView bean = (IdentificationMessagesEditView) session.getAttribute(PP_CRITERIA_NAME);
		final String idAsString = request.getParameter(ID_PARAMETER_NAME);
		if (idAsString != null) {
			final Long id = Long.valueOf(idAsString);
			if (bean == null || bean.getDemandeIdentificationView().getId().longValue() != id.longValue()) {
				identificationMessagesEditManager.verouillerMessage(id);
				bean = identificationMessagesEditManager.getView(id);
				//gestion des droits
				if (!SecurityProvider.isGranted(Role.VISU_ALL)) {
					if (!SecurityProvider.isGranted(Role.VISU_LIMITE)) {
						throw new AccessDeniedException("vous ne possédez aucun droit IfoSec de consultation pour l'application Unireg");
					}
					bean.setTypeVisualisation(TiersCriteria.TypeVisualisation.LIMITEE);
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
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws Exception {
		final HttpSession session = request.getSession();
		final IdentificationMessagesEditView bean = (IdentificationMessagesEditView) session.getAttribute(PP_CRITERIA_NAME);
		retournerFichierOrigine(response, bean.getDemandeIdentificationView().getDocumentUrl());
		return null;
	}

	private void retournerFichierOrigine(HttpServletResponse response, String documentUrl) throws Exception {
		if (documentUrl != null) {
			final URL url = new URL(documentUrl);
			try {
				final HttpDocumentFetcher.HttpDocument document = HttpDocumentFetcher.fetch(url);
				final String extension = MimeTypeHelper.getFileExtensionForType(document.getContentType());
				servletService.downloadAsFile(String.format("message%s", extension), document.getContentType(), document.getContent(), document.getContentLength(), response);
			}
			catch (HttpDocumentFetcher.HttpDocumentException e) {
				LOGGER.error(String.format("Erreur lors de l'extraction du document à l'URL '%s'", url), e);
			}
		}
	}
}
