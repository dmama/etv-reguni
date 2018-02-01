package ch.vd.unireg.entreprise.complexe;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.tiers.Entreprise;

@Controller
@RequestMapping("/processuscomplexe/annulation/faillite")
public class AnnulationFailliteController extends AnnulationRevocationFailliteController {

	public static final String CRITERIA_NAME = "AnnulationFailliteCriteria";

	public AnnulationFailliteController() {
		super("d'annulation", "annulation");
	}

	@Override
	protected String getSearchCriteriaSessionName() {
		return CRITERIA_NAME;
	}

	@Override
	protected void doJob(Entreprise entreprise, FailliteView view) throws MetierServiceException {
		metierService.annuleFaillite(entreprise, view.getDatePrononceFaillite(), view.getRemarque());
	}
}
