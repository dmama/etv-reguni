package ch.vd.uniregctb.rapport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.rapport.manager.RapportEditManager;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.rt.AbstractRapportPrestationController;

public class RapportEditController extends AbstractRapportPrestationController {

	protected final Logger LOGGER = Logger.getLogger(RapportEditController.class);

	private final String NUMERO_TIERS_PARAMETER_NAME = "numeroTiers";
	private final String NUMERO_TIERS_LIE_PARAMETER_NAME = "numeroTiersLie";

	private RapportEditManager rapportEditManager;

	public RapportEditManager getRapportEditManager() {
		return rapportEditManager;
	}

	public void setRapportEditManager(RapportEditManager rapportEditManager) {
		this.rapportEditManager = rapportEditManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		String numeroTiersParam = request.getParameter(NUMERO_TIERS_PARAMETER_NAME);
		String numeroTiersLieParam = request.getParameter(NUMERO_TIERS_LIE_PARAMETER_NAME);

		Long numeroTiers = Long.parseLong(numeroTiersParam);
		Long numeroTiersLie = Long.parseLong(numeroTiersLieParam);
		checkAccesDossierEnLecture(numeroTiers);
		checkAccesDossierEnLecture(numeroTiersLie);

		//vérification des droits de création de rapport entre tiers non travail par rapportEditManager
		RapportView rapportView = rapportEditManager.get(numeroTiers, numeroTiersLie);

		return rapportView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		ModelAndView mav = super.onSubmit(request, response, command, errors);

		RapportView rapportView = (RapportView) command;
		checkAccesDossierEnEcriture(rapportView.getTiers().getNumero());
		checkAccesDossierEnEcriture(rapportView.getTiersLie().getNumero());

		try {
			rapportEditManager.save(rapportView);
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			errors.reject(e.getMessage());
			return showForm(request, response, errors);
		}

		mav.setView(new RedirectView("../tiers/visu.do?id=" + rapportView.getTiers().getNumero()));

		return mav;
	}

}
