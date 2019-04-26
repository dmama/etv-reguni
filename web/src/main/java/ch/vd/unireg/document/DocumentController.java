package ch.vd.unireg.document;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.ActionException;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.HttpHelper;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.servlet.ServletService;

/**
 * Ce controlleur permet de gérer les documents produits par Unireg.
 */
@Controller
@RequestMapping(value = "/common/docs/")
public class DocumentController {

	private DocumentService docService;
	private ServletService servletService;
	private SecurityProviderInterface securityProvider;
	private AuditManager audit;

	public void setDocService(DocumentService docService) {
		this.docService = docService;
	}

	public void setServletService(ServletService servletService) {
		this.servletService = servletService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setAudit(AuditManager audit) {
		this.audit = audit;
	}

	@RequestMapping(value = "/download.do", method = RequestMethod.GET)
	public String download(@RequestParam("id") long id, HttpServletRequest request, HttpServletResponse response) throws Exception {

		final Document doc = docService.get(id);
		if (doc != null) {
			try {
				// On veut que la réponse provoque un téléchargement de fichier
				docService.readDoc(doc, (doc1, is) -> servletService.downloadAsFile(doc1.getFileName(), is, (int) doc1.getFileSize(), response));

				audit.info("Le document '" + doc.getNom() + "' a été téléchargé par l'utilisateur " + AuthenticationHelper.getCurrentPrincipal() + ".");

				// le document a déjà été placé dans la réponse HTTP, il ne faut surtout rien renvoyer d'autre
				return null;
			}
			catch (IOException e) {
				throw new ActionException(e.getMessage(), e);
			}
		}

		// cette fonctionalité de Download est utilisée depuis plusieurs écrans (tiersImport, batchs...)
		return HttpHelper.getRedirectPagePrecedente(request);
	}

	@RequestMapping(value = "/delete.do", method = RequestMethod.POST)
	public String delete(@RequestParam("id") long id) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits d'administration pour l'application Unireg");
		}

		final Document doc = docService.get(id);
		if (doc != null) {
			audit.info("Le document '" + doc.getNom() + "' a été effacé du serveur.");
			docService.delete(doc);
		}

		return "redirect:/admin/tiersImport/list.do";
	}
}
