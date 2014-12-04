package ch.vd.uniregctb.document;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.servlet.ServletService;

/**
 * Ce controlleur permet de gérer les documents produits par Unireg.
 */
public class DocumentController extends AbstractSimpleFormController {

	private DocumentService docService;
	private ServletService servletService;

	public void setDocService(DocumentService docService) {
		this.docService = docService;
	}

	public void setServletService(ServletService servletService) {
		this.servletService = servletService;
	}

	@Override
	public ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

		final String action = request.getParameter("action");
		if (action != null) {
			if ("download".equals(action)) {
				download(request, response);
				return null; // pas de redirection nécessaire après le download
			}
			else if ("delete".equals(action)) {
				delete(request, response);
			}
		}

		return new ModelAndView(new RedirectView(getSuccessView()));
	}

	private void download(HttpServletRequest request, final HttpServletResponse response) throws Exception {

		final Document doc = getDoc(request);
		if (doc == null) {
			return;
		}

		// On veut que la réponse provoque un téléchargement de fichier
		docService.readDoc(doc, new DocumentService.ReadDocCallback<Document>() {
			@Override
			public void readDoc(Document doc, InputStream is) throws Exception {
				servletService.downloadAsFile(doc.getFileName(), is, (int) doc.getFileSize(), response);
			}
		});

		Audit.info("Le document '" + doc.getNom() + "' a été téléchargé par l'utilisateur " + AuthenticationHelper.getCurrentPrincipal() + ".");
	}

	private void delete(HttpServletRequest request, HttpServletResponse response) throws Exception {

		final Document doc = getDoc(request);
		if (doc == null) {
			return;
		}

		Audit.info("Le document '" + doc.getNom() + "' a été effacé du serveur.");
		docService.delete(doc);
	}

	/**
	 * Interprète la requête et retourne le document concerné.
	 */
	private Document getDoc(HttpServletRequest request) throws Exception {

		final String idAsString = request.getParameter("id");
		if (idAsString == null) {
			return null;
		}

		final Long id;
		try {
			id = Long.valueOf(idAsString);
		}
		catch (NumberFormatException ignored) {
			return null;
		}

		final Document doc = docService.get(id);
		return doc;
	}
}
