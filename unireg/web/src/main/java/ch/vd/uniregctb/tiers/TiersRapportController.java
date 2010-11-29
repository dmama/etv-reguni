package ch.vd.uniregctb.tiers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.rapport.manager.RapportEditManager;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.rt.AbstractRapportPrestationController;

public class TiersRapportController extends AbstractRapportPrestationController {

	private RapportEditManager rapportEditManager;

	private final static String ID_RAPPORT_PARAMETER_NAME = "idRapport";
	private final static String SENS_RAPPORT_PARAMETER_NAME = "sens";

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		RapportView rapportView = new RapportView();

		final String idRapport = request.getParameter(ID_RAPPORT_PARAMETER_NAME);
		final String sensRapport = request.getParameter(SENS_RAPPORT_PARAMETER_NAME);

		if (StringUtils.isNotBlank(idRapport)) {
			final Long id = Long.parseLong(idRapport);
			final SensRapportEntreTiers sens = SensRapportEntreTiers.valueOf(sensRapport);
			//gestion des droits dans rapportEditManager
			rapportView = rapportEditManager.get(id, sens);
			checkAccesDossierEnLecture(rapportView.getNumero());
		}
		return rapportView;
	}

	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
		super.onBindAndValidate(request, command, errors);
		RapportView view = (RapportView) command;
		if (view.getDateFin() != null && view.getDateDebut() != null && view.getDateFin().before(view.getDateDebut())) {
			errors.rejectValue("dateFin", "rapport.interval.dateFin", "la date ne peut être antérieur à la date de début.");
		}
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		ModelAndView mav = super.onSubmit(request, response, command, errors);
		RapportView rapportView = (RapportView) command;

		checkAccesDossierEnEcriture(rapportView.getNumero());

		rapportEditManager.save(rapportView);

		return mav;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRapportEditManager(RapportEditManager rapportEditManager) {
		this.rapportEditManager = rapportEditManager;
	}
}
