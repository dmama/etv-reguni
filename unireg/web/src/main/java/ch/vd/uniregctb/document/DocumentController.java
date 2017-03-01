package ch.vd.uniregctb.document;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.servlet.ServletService;

/**
 * Ce controlleur permet de gérer les documents produits par Unireg.
 */
@Controller
@RequestMapping(value = "/common/docs/")
public class DocumentController {

	private DocumentService docService;
	private ServletService servletService;
	private SecurityProviderInterface securityProvider;

	public void setDocService(DocumentService docService) {
		this.docService = docService;
	}

	public void setServletService(ServletService servletService) {
		this.servletService = servletService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@RequestMapping(value = "/download.do", method = RequestMethod.GET)
	public String download(@RequestParam("id") long id, HttpServletResponse response) throws Exception {

		final Document doc = docService.get(id);
		if (doc != null) {
			// On veut que la réponse provoque un téléchargement de fichier
			docService.readDoc(doc, (doc1, is) -> {
				servletService.downloadAsFile(doc1.getFileName(), is, (int) doc1.getFileSize(), response);
			});

			Audit.info("Le document '" + doc.getNom() + "' a été téléchargé par l'utilisateur " + AuthenticationHelper.getCurrentPrincipal() + ".");
		}

		return "redirect:/admin/tiersImport/list.do";
	}

	@RequestMapping(value = "/delete.do", method = RequestMethod.POST)
	public String delete(@RequestParam("id") long id) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
		}

		final Document doc = docService.get(id);
		if (doc != null) {
			Audit.info("Le document '" + doc.getNom() + "' a été effacé du serveur.");
			docService.delete(doc);
		}

		return "redirect:/admin/tiersImport/list.do";
	}
}
