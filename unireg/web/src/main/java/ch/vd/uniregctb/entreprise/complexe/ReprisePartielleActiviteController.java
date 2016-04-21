package ch.vd.uniregctb.entreprise.complexe;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.Entreprise;

@Controller
@RequestMapping("/processuscomplexe/repriseactivite")
public class ReprisePartielleActiviteController extends AnnulationRepriseFinActiviteController {

	private static final String CRITERIA_NAME = "ReprisePartielleActiviteCriteria";

	public ReprisePartielleActiviteController() {
		super(CRITERIA_NAME, "de reprise partielle d'activité", "reprise");
	}

	@Override
	protected void doJob(Entreprise entreprise, FinActiviteView view) throws MetierServiceException {
		metierService.annuleFinActivite(entreprise, view.getDateFinActivite(), view.getRemarque(), false);
	}
}
