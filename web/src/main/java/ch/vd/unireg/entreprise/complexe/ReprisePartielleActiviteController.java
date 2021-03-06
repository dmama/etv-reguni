package ch.vd.unireg.entreprise.complexe;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.tiers.Entreprise;

@Controller
@RequestMapping("/processuscomplexe/repriseactivite")
public class ReprisePartielleActiviteController extends AnnulationRepriseFinActiviteController {

	private static final String CRITERIA_NAME = "ReprisePartielleActiviteCriteria";

	public ReprisePartielleActiviteController() {
		super("de reprise partielle d'activité", "reprise");
	}

	@Override
	protected String getSearchCriteriaSessionName() {
		return CRITERIA_NAME;
	}

	@Override
	protected void doJob(Entreprise entreprise, FinActiviteView view) throws MetierServiceException {
		metierService.annuleFinActivite(entreprise, view.getDateFinActivite(), view.getRemarque(), false);
	}
}
