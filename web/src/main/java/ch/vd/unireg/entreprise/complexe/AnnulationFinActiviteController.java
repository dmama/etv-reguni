package ch.vd.uniregctb.entreprise.complexe;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.Entreprise;

@Controller
@RequestMapping("/processuscomplexe/annulation/finactivite")
public class AnnulationFinActiviteController extends AnnulationRepriseFinActiviteController {

	private static final String CRITERIA_NAME = "AnnulationFinActiviteCriteria";

	public AnnulationFinActiviteController() {
		super("d'annulation de fin d'activité", "annulation");
	}

	@Override
	protected String getSearchCriteriaSessionName() {
		return CRITERIA_NAME;
	}

	@Override
	protected void doJob(Entreprise entreprise, FinActiviteView view) throws MetierServiceException {
		metierService.annuleFinActivite(entreprise, view.getDateFinActivite(), view.getRemarque(), true);
	}
}
