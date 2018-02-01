package ch.vd.unireg.fusion;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.unireg.common.AbstractSimpleFormController;
import ch.vd.unireg.fusion.manager.FusionRecapManager;
import ch.vd.unireg.fusion.view.FusionRecapView;

public class FusionRecapController  extends AbstractSimpleFormController {

	protected final Logger LOGGER = LoggerFactory.getLogger(FusionRecapController.class);

	private static final String NUMERO_NON_HABITANT_PARAMETER_NAME = "numeroNonHab";
	private static final String NUMERO_HABITANT_NAME = "numeroHab";

	private FusionRecapManager fusionRecapManager;

	public FusionRecapManager getFusionRecapManager() {
		return fusionRecapManager;
	}

	public void setFusionRecapManager(FusionRecapManager fusionRecapManager) {
		this.fusionRecapManager = fusionRecapManager;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {

		String numeroNonHabParam = request.getParameter(NUMERO_NON_HABITANT_PARAMETER_NAME);
		String numeroHabParam = request.getParameter(NUMERO_HABITANT_NAME);
		Long numeroNonHab = Long.parseLong(numeroNonHabParam);
		Long numeroHab = Long.parseLong(numeroHabParam);

		checkAccesDossierEnLecture(numeroNonHab);
		checkAccesDossierEnLecture(numeroHab);

		return fusionRecapManager.get(numeroNonHab, numeroHab);
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
		throws Exception {
		ModelAndView mav = super.onSubmit(request, response, command, errors);

		FusionRecapView fusionRecapView = (FusionRecapView) command;
		checkAccesDossierEnEcriture(fusionRecapView.getHabitant().getNumero());
		checkAccesDossierEnLecture(fusionRecapView.getNonHabitant().getNumero());

		fusionRecapManager.save(fusionRecapView);

		mav.setView(new RedirectView("../tiers/visu.do?id=" + fusionRecapView.getHabitant().getNumero()));


		return mav;
	}

}
