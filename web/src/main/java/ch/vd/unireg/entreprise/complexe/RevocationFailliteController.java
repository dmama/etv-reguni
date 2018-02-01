package ch.vd.unireg.entreprise.complexe;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.tiers.Entreprise;

@Controller
@RequestMapping("/processuscomplexe/revocation/faillite")
public class RevocationFailliteController extends AnnulationRevocationFailliteController {

	public static final String CRITERIA_NAME = "RevocationFailliteCriteria";

	public RevocationFailliteController() {
		super("de révocation", "revocation");
	}

	@Override
	protected String getSearchCriteriaSessionName() {
		return CRITERIA_NAME;
	}

	@Override
	protected void doJob(Entreprise entreprise, FailliteView view) throws MetierServiceException {
		metierService.revoqueFaillite(entreprise, view.getDatePrononceFaillite(), view.getRemarque());
	}
}
