package ch.vd.uniregctb.entreprise.complexe;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.Entreprise;

@Controller
@RequestMapping("/processuscomplexe/annulation/faillite")
public class AnnulationFailliteController extends AnnulationRevocationFailliteController {

	public static final String CRITERIA_NAME = "AnnulationFailliteCriteria";

	public AnnulationFailliteController() {
		super(CRITERIA_NAME, "d'annulation", "annulation");
	}

	@Override
	protected void doJob(Entreprise entreprise, FailliteView view) throws MetierServiceException {
		metierService.annuleFaillite(entreprise, view.getDatePrononceFaillite(), view.getRemarque());
	}
}
