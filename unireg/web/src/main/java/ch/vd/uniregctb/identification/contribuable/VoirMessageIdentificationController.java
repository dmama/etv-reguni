package ch.vd.uniregctb.identification.contribuable;

import javax.servlet.http.HttpServletResponse;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.MimeTypeHelper;
import ch.vd.uniregctb.identification.contribuable.manager.IdentificationMessagesEditManager;
import ch.vd.uniregctb.identification.contribuable.view.DemandeIdentificationView;
import ch.vd.uniregctb.servlet.ServletService;
import ch.vd.uniregctb.utils.HttpDocumentFetcher;

@Controller
@RequestMapping(value = "/identification/gestion-messages")
public class VoirMessageIdentificationController {

	private static final Logger LOGGER = Logger.getLogger(VoirMessageIdentificationController.class);

	private static final String ID_PARAMETER_NAME = "id";

	/**
	 * En millisecondes, le temps d'affichage d'une erreur de récupération du document
	 */
	private static final long ERROR_FADING_TIMEOUT = 5000L;

	private IdentificationMessagesEditManager identificationMessagesEditManager;
	private ServletService servletService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIdentificationMessagesEditManager(IdentificationMessagesEditManager identificationMessagesEditManager) {
		this.identificationMessagesEditManager = identificationMessagesEditManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServletService(ServletService servletService) {
		this.servletService = servletService;
	}

	@RequestMapping(value = "voirMessage.do", method = RequestMethod.GET)
	public String voirMessageAttachment(HttpServletResponse response, @RequestParam(value = ID_PARAMETER_NAME, required = true) long messageId) throws Exception {

		final DemandeIdentificationView bean = identificationMessagesEditManager.getDemandeIdentificationView(messageId);
		final String urlString = StringUtils.trimToNull(bean.getDocumentUrl());
		final String erreur = retournerFichierOrigine(response, urlString);
		if (erreur != null) {
			Flash.error(String.format("Erreur à la récupération du document : '%s'", erreur), ERROR_FADING_TIMEOUT);
			return String.format("redirect:/identification/gestion-messages/edit.do?id=%d", messageId);
		}
		return null;
	}

	private String retournerFichierOrigine(HttpServletResponse response, String documentUrl) throws Exception {
		if (documentUrl != null) {
			final URL url = new URL(documentUrl);
			try {
				final HttpDocumentFetcher.HttpDocument document = HttpDocumentFetcher.fetch(url);
				try {
					final String proposedFilename = document.getProposedContentFilename();
					final String filename;
					if (proposedFilename == null) {
						final String extension = MimeTypeHelper.getFileExtensionForType(document.getContentType());
						filename = String.format("message%s", extension);
					}
					else {
						filename = proposedFilename;
					}
					servletService.downloadAsFile(filename, document.getContentType(), document.getContent(), document.getContentLength(), response);
					return null;
				}
				finally {
					document.release();
				}
			}
			catch (HttpDocumentFetcher.HttpDocumentException e) {
				LOGGER.error(String.format("Erreur lors de l'extraction du document à l'URL '%s'", url), e);
				return e.getMessage();
			}
		}
		return "Lien inexistant";
	}
}
