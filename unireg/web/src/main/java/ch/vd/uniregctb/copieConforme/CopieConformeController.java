package ch.vd.uniregctb.copieConforme;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.servlet.ServletService;

@Controller
public class CopieConformeController {

	private static final String ID_DELAI = "idDelai";
	private static final String ID_ETAT = "idEtat";

	private static final String NOCTB= "noCtb";
	private static final String TYPE_DOC = "typeDoc";
	private static final String KEY = "key";

	/**
	 * Temps (ms) après lequel un message d'erreur doit être effacé automatiquement
	 */
	private static final long errorFadingTimeout = 5000L;

	private ServletService servletService;

	private CopieConformeManager copieConformeManager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServletService(ServletService servletService) {
		this.servletService = servletService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setCopieConformeManager(CopieConformeManager copieConformeManager) {
		this.copieConformeManager = copieConformeManager;
	}

	private static interface CopieConformeGetter {
		/**
		 * @return un flux duquel on peut extraire le contenu du document, où <code>null</code> si aucun document n'a pu être trouvé
		 * @throws EditiqueException en cas d'erreur lors de la récupération du contenu du document
		 */
		InputStream getCopieConforme() throws EditiqueException;
	}

	/**
	 * Traitement d'une demande de copie conforme
	 * @param request HTTP request de la demande de copie conforme
	 * @param response HTTP response (dans laquelle le document sera renvoyé)
	 * @param filename nom du fichier du document à renvoyer
	 * @param errorMessageIfNoSuchDocument message d'erreur au cas où le document demandé n'existe pas dans l'archivage
	 * @param getter l'implémentation spécifique de récupération du document
	 * @return <code>null</code> si le document a bien été renvoyé dans la réponse HTTP, "redirect:..." en cas d'erreur
	 * @throws EditiqueException en cas d'erreur lors de la récupération du document depuis les archives
	 * @throws IOException en cas d'erreurs lors du streaming du document
	 */
	private String getDocumentCopieConforme(HttpServletRequest request, HttpServletResponse response, String filename, String errorMessageIfNoSuchDocument, CopieConformeGetter getter) throws EditiqueException, IOException {
		final InputStream is = getter.getCopieConforme();
		if (is != null) {
			downloadFile(is, filename, response);
			return null;
		}
		else {
			if (StringUtils.isNotBlank(errorMessageIfNoSuchDocument)) {
				Flash.error(errorMessageIfNoSuchDocument, errorFadingTimeout);
			}
			return getRedirectPagePrecedente(request);
		}
	}

	@RequestMapping(value = "/declaration/copie-conforme-delai.do", method = RequestMethod.GET)
	public String getDocumentDelai(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = ID_DELAI, required = true) final Long idDelai) throws Exception {
		return getDocumentCopieConforme(request, response, "copieDelai.pdf", "Aucun archivage trouvé pour la confirmation de délai demandée !", new CopieConformeGetter() {
			@Override
			public InputStream getCopieConforme() throws EditiqueException {
				return copieConformeManager.getPdfCopieConformeDelai(idDelai);
			}
		});
	}

	@RequestMapping(value = "/declaration/copie-conforme-sommation.do", method = RequestMethod.GET)
	public String getDocumentSommation(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = ID_ETAT, required = true) final Long idEtat) throws Exception {
		return getDocumentCopieConforme(request, response, "copieSommation.pdf", "Aucun archivage trouvé pour la sommation de déclaration demandée !", new CopieConformeGetter() {
			@Override
			public InputStream getCopieConforme() throws EditiqueException {
				return copieConformeManager.getPdfCopieConformeSommation(idEtat);
			}
		});
	}

	@RequestMapping(value = "/copie-conforme.do", method = RequestMethod.GET)
	public String getDocument(HttpServletRequest request, HttpServletResponse response,
	                          @RequestParam(value = NOCTB, required = true) final long noCtb,
	                          @RequestParam(value = TYPE_DOC, required = true) final TypeDocumentEditique typeDoc,
	                          @RequestParam(value = KEY, required = true) final String key) throws Exception {
		return getDocumentCopieConforme(request, response, "document.pdf", "Aucun archivage trouvé pour le document demandé !", new CopieConformeGetter() {
			@Override
			public InputStream getCopieConforme() throws EditiqueException {
				return copieConformeManager.getPdfCopieConforme(noCtb, typeDoc, key);
			}
		});
	}

	private void downloadFile(InputStream stream, String filename, HttpServletResponse response) throws IOException {
		servletService.downloadAsFile(filename, stream, null, response);
	}

	private static String getRedirectPagePrecedente(HttpServletRequest request) {
		final String previousPage = request.getHeader("referer");
		if (StringUtils.isNotBlank(previousPage)) {
			return String.format("redirect:%s", previousPage);
		}
		else {
			return "redirect:/404.do";
		}
	}
}
