package ch.vd.uniregctb.tiers;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.rapport.manager.RapportEditManager;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.rt.AbstractRapportPrestationController;
import ch.vd.uniregctb.type.SensRapportEntreTiers;

public class TiersRapportController extends AbstractRapportPrestationController {

	/**
	 * Un LOGGER.
	 */
	protected final Logger LOGGER = Logger.getLogger(TiersRapportController.class);

	private RapportEditManager rapportEditManager;

	private final static String ID_RAPPORT_PARAMETER_NAME = "idRapport";
	private final static String SENS_RAPPORT_PARAMETER_NAME = "sens";

	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		RapportView rapportView = new RapportView();

		String idRapport =  request.getParameter(ID_RAPPORT_PARAMETER_NAME);
		String sensRapport =  request.getParameter(SENS_RAPPORT_PARAMETER_NAME);

		LOGGER.debug("Id rapport :" + idRapport);

		if (idRapport != null && !"".equals(idRapport.trim())) {
			Long id = Long.parseLong(idRapport);
			SensRapportEntreTiers sens  = SensRapportEntreTiers.valueOf(sensRapport);
			//gestion des droits dans rapportEditManager
			rapportView = rapportEditManager.get(id, sens);

			checkAccesDossierEnLecture(rapportView.getNumero());
		}
		return rapportView;
	}

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model)
			throws Exception {
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
		RapportView view = (RapportView) command;
		if ( view.getDateFin() != null && view.getDateDebut() != null && view.getDateFin().before(view.getDateDebut())) {
			errors.rejectValue("dateFin", "rapport.interval.dateFin", "la date ne peut être antérieur à la date de début.");
		}
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

		checkAccesDossierEnEcriture(rapportView.getNumero());

		rapportEditManager.save(rapportView);

		return mav;
	}

	public RapportEditManager getRapportEditManager() {
		return rapportEditManager;
	}

	public void setRapportEditManager(RapportEditManager rapportEditManager) {
		this.rapportEditManager = rapportEditManager;
	}



}
