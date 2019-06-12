package ch.vd.unireg.entreprise.complexe;

import javax.validation.Valid;
import java.util.EnumSet;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.view.TiersCriteriaView;
import ch.vd.unireg.type.TypeEtatEntreprise;

@Controller
@RequestMapping("/processuscomplexe/finactivite")
public class FinActiviteController extends AbstractProcessusComplexeRechercheController {

	public static final String CRITERIA_NAME = "FinActiviteCriteria";

	@Override
	protected void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez pas les droits d'accès au processus complexe de fin d'activité d'entreprise.",
		                Role.FIN_ACTIVITE_ENTREPRISE);
	}

	@Override
	protected String getSearchCriteriaSessionName() {
		return CRITERIA_NAME;
	}

	@Override
	protected void fillCriteriaWithImplicitValues(TiersCriteriaView criteria) {
		criteria.setTiersActif(Boolean.TRUE);
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
		criteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE));
	}

	@Override
	protected String getSearchResultViewPath() {
		return "entreprise/finactivite/list";
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.GET)
	public String showStart(Model model, @RequestParam("id") long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);
		return showStart(model, new FinActiviteView(idEntreprise));
	}

	private String showStart(Model model, FinActiviteView view) {
		model.addAttribute(ACTION_COMMAND, view);
		return "entreprise/finactivite/start";
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	public String doFinActivite(Model model, @Valid @ModelAttribute(value = ACTION_COMMAND) final FinActiviteView view, BindingResult bindingResult) throws Exception {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(view.getIdEntreprise());
		if (bindingResult.hasErrors()) {
			return showStart(model, view);
		}
		controllerUtils.checkTraitementContribuableAvecDecisionAci(view.getIdEntreprise());

		doInTransaction(status -> {
			final Entreprise entreprise = getTiers(Entreprise.class, view.getIdEntreprise());
			metierService.finActivite(entreprise, view.getDateFinActivite(), view.getRemarque());
		});

		return "redirect:/tiers/visu.do?id=" + view.getIdEntreprise();
	}
}
