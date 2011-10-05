package ch.vd.uniregctb.declaration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.servlet.ServletService;

public class CopieConformeController extends AbstractSimpleFormController {

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

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		fetchAndDownload(request, response);
		return null;
	}

	private void fetchAndDownload(HttpServletRequest request, HttpServletResponse response) throws EditiqueException, IOException {

		final String idDelaiStr = request.getParameter("idDelai");
		final String idEtatStr = request.getParameter("idEtat");

		if (idDelaiStr != null) {
			try {

				final Long idDelai = Long.valueOf(idDelaiStr);
				final InputStream pdf = copieConformeManager.getPdfCopieConformeDelai(idDelai);
				if (pdf != null) {
					servletService.downloadAsFile("copieDelai.pdf", pdf, null, response);
				}
				else {
					throw new EditiqueException("Aucun archivage présent pour la confirmation de délai demandée");
				}
			}
			catch (NumberFormatException ignored) {
				// ignoré...
			}
		}
		else if (idEtatStr != null) {

			try {
				final Long idEtat = Long.valueOf(idEtatStr);
				final InputStream pdf = copieConformeManager.getPdfCopieConformeSommation(idEtat);
				if (pdf != null) {
					servletService.downloadAsFile("copieSommation.pdf", pdf, null, response);
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
}
