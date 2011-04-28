package ch.vd.uniregctb.document;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.servlet.ServletService;

/**
 * Ce controlleur permet de gérer les documents produits par Unireg.
 */
public class DocumentController extends AbstractSimpleFormController {

	private DocumentService docService;
	private ServletService servletService;

	public DocumentController() {
		/*
		 * [UNIREG-486] Workaround pour un bug de IE6 qui empêche d'ouvrir correctement les fichier attachés PDFs.
		 *
		 * Voir aussi:
		 *  - http://drupal.org/node/93787
		 *  - http://support.microsoft.com/default.aspx?scid=kb;en-us;316431
		 *  - http://bugs.php.net/bug.php?id=16173
		 *  - http://pkp.sfu.ca/support/forum/viewtopic.php?p=359&sid=4516e6d325c613c7875f67e1b9194c57
		 *  - http://forum.springframework.org/showthread.php?t=24466
		 */
		setCacheSeconds(-1);
	}

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
			public void readDoc(Document doc, InputStream is) throws Exception {
				servletService.downloadAsFile(doc.getFileName(), is, (int) doc.getFileSize(), response);
			}
		});

		Audit.info("Le document '" + doc.getNom() + "' a été téléchargé.");
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
