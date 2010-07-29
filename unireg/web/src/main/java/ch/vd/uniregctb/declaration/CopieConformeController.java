package ch.vd.uniregctb.declaration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.servlet.ServletService;

public class CopieConformeController extends AbstractSimpleFormController {

	private ServletService servletService;

	private CopieConformeManager copieConformeManager;

	public void setServletService(ServletService servletService) {
		this.servletService = servletService;
	}

	public void setCopieConformeManager(CopieConformeManager copieConformeManager) {
		this.copieConformeManager = copieConformeManager;
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		fetchAndDownload(request, response);
		return new ModelAndView(new RedirectView(getSuccessView()));
	}

	private void fetchAndDownload(HttpServletRequest request, HttpServletResponse response) throws EditiqueException, IOException {
		final String idEtatStr = request.getParameter("idEtat");
		try {
			final Long idEtat = Long.valueOf(idEtatStr);
			final byte[] pdf = copieConformeManager.getPdfCopieConformeSommation(idEtat);
			if (pdf != null) {
				servletService.downloadAsFile("copieSommation.pdf", pdf, response);
			}
			else {
				throw new EditiqueException("Aucun archivage présent pour la sommation de déclaration demandée");
			}
		}
		catch (NumberFormatException ignored) {
			// ignoré...
		}
	}
}
