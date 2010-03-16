package ch.vd.uniregctb.mouvement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.mouvement.view.MouvementMasseCriteriaTraitementView;

public class MouvementMasseTraitementController extends AbstractMouvementMasseRechercheController {

	private static final String INCLURE_BORDEREAU = "inclure";

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
		final MouvementMasseCriteriaTraitementView view = (MouvementMasseCriteriaTraitementView) command;
		if (view.getRetireMvtId() != null) {
			getMouvementManager().changeEtat(EtatMouvementDossier.RETIRE, view.getRetireMvtId());
		}
		else if (view.getReinitMvtId() != null) {
			getMouvementManager().changeEtat(EtatMouvementDossier.A_TRAITER, view.getReinitMvtId());
		}
		else if (request.getParameter(INCLURE_BORDEREAU) != null && view.getTabIdsMvts() != null) {
			getMouvementManager().changeEtat(EtatMouvementDossier.A_ENVOYER, view.getTabIdsMvts());
			view.setTabIdsMvts(null);
		}
		return super.onSubmit(request, response, command, errors);
	}
}
