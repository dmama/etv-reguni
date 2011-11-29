package ch.vd.uniregctb.annulation.separation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.annulation.separation.manager.AnnulationSeparationRecapManager;
import ch.vd.uniregctb.annulation.separation.view.AnnulationSeparationRecapView;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.deces.DecesRecapController;
import ch.vd.uniregctb.tiers.MenageCommun;

public class AnnulationSeparationRecapController extends AbstractSimpleFormController {

	protected final Logger LOGGER = Logger.getLogger(DecesRecapController.class);

	private static final String NUMERO_PARAMETER_NAME = "numero";

	private AnnulationSeparationRecapManager annulationSeparationRecapManager;

	public AnnulationSeparationRecapManager getAnnulationSeparationRecapManager() {
		return annulationSeparationRecapManager;
	}

	public void setAnnulationSeparationRecapManager(AnnulationSeparationRecapManager annulationSeparationRecapManager) {
		this.annulationSeparationRecapManager = annulationSeparationRecapManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		String numeroParam = request.getParameter(NUMERO_PARAMETER_NAME);
		Long numero = Long.parseLong(numeroParam);

		AnnulationSeparationRecapView annulationSeparationRecapView = annulationSeparationRecapManager.get(numero);
		
		checkAccesDossierEnLecture(annulationSeparationRecapView.getPremierePersonne().getNumero());
		
	 	// [UNIREG-1499] La 2nde personne peut etre null dans le cas d'un marié seul
		if (annulationSeparationRecapView.getSecondePersonne() != null) {
			checkAccesDossierEnLecture(annulationSeparationRecapView.getSecondePersonne().getNumero());			
		}
		

		return annulationSeparationRecapView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse,
	 *      org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws Exception {
		ModelAndView mav = super.showForm(request, response, errors, model);
		return mav;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.BaseCommandController#onBindAndValidate(javax.servlet.http.HttpServletRequest,
	 *      java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
		super.onBindAndValidate(request, command, errors);
	}


	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
		throws Exception {

		AnnulationSeparationRecapView annulationSeparationRecapView = (AnnulationSeparationRecapView) command;
		checkAccesDossierEnEcriture(annulationSeparationRecapView.getPremierePersonne().getNumero());

		// [UNIREG-1499] La 2nde personne peut etre null dans le cas d'un marié seul
		if (annulationSeparationRecapView.getSecondePersonne() != null) {
			checkAccesDossierEnEcriture(annulationSeparationRecapView.getSecondePersonne().getNumero());
		}

		MenageCommun menage = annulationSeparationRecapManager.save(annulationSeparationRecapView);
		return new ModelAndView( new RedirectView("/tiers/visu.do?id=" + menage.getNumero(), true));
	}
}
