package ch.vd.unireg.identification.contribuable;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.HttpDocumentFetcher;
import ch.vd.unireg.common.MimeTypeHelper;
import ch.vd.unireg.identification.contribuable.manager.IdentificationMessagesEditManager;
import ch.vd.unireg.identification.contribuable.view.DemandeIdentificationView;
import ch.vd.unireg.servlet.ServletService;

@Controller
@RequestMapping(value = "/identification/gestion-messages")
public class VoirMessageIdentificationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(VoirMessageIdentificationController.class);

	private static final String ID_PARAMETER_NAME = "id";

	/**
	 * En millisecondes, le temps d'affichage d'une erreur de récupération du document
	 */
	private static final long ERROR_FADING_TIMEOUT = 5000L;

	/**
	 * En millisecones, le temps d'attente maximale pour la récupération du document
	 */
	private static final int GET_TIMEOUT = 60000;

	private IdentificationMessagesEditManager identificationMessagesEditManager;
	private ServletService servletService;
	private HttpDocumentFetcher documentFetcher;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIdentificationMessagesEditManager(IdentificationMessagesEditManager identificationMessagesEditManager) {
		this.identificationMessagesEditManager = identificationMessagesEditManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServletService(ServletService servletService) {
		this.servletService = servletService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDocumentFetcher(HttpDocumentFetcher documentFetcher) {
		this.documentFetcher = documentFetcher;
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
				try (HttpDocumentFetcher.HttpDocument document = documentFetcher.fetch(url, GET_TIMEOUT)) {
					if (document != null) {
						final String proposedFilename = document.getProposedContentFilename();
						final String filename;
						if (proposedFilename == null) {
							final String extension = MimeTypeHelper.getFileExtensionForType(document.getContentType());
							filename = String.format("message%s", extension);
						}
						else {
							filename = proposedFilename;
						}
						try (InputStream content = document.getContent()) {
							servletService.downloadAsFile(filename, document.getContentType(), content, document.getContentLength(), response);
						}
						return null;
					}
					else {
						return "Le document est vide.";
					}
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
