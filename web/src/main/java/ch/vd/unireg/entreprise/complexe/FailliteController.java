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
@RequestMapping("/processuscomplexe/faillite")
public class FailliteController extends AbstractProcessusComplexeRechercheController {

	public static final String CRITERIA_NAME = "FailliteCriteria";

	@Override
	protected void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez pas les droits d'accès au processus complexe de faillite d'entreprise.",
		                Role.FAILLITE_ENTREPRISE);
	}

	@Override
	protected String getSearchCriteriaSessionName() {
		return CRITERIA_NAME;
	}

	@Override
	protected void fillCriteriaWithImplicitValues(TiersCriteriaView criteria) {
		criteria.setTiersActif(Boolean.TRUE);
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
		criteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE, TypeEtatEntreprise.RADIEE_RC));
		criteria.setEtatsEntrepriseCourantsInterdits(EnumSet.of(TypeEtatEntreprise.EN_FAILLITE));
	}

	@Override
	protected String getSearchResultViewPath() {
		return "entreprise/faillite/list";
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.GET)
	public String showStart(Model model, @RequestParam("id") long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);
		return showStart(model, new FailliteView(idEntreprise));
	}

	private String showStart(Model model, FailliteView view) {
		model.addAttribute(ACTION_COMMAND, view);
		return "entreprise/faillite/start";
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	public String doFaillite(Model model, @Valid @ModelAttribute(value = ACTION_COMMAND) final FailliteView view, BindingResult bindingResult) throws Exception {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(view.getIdEntreprise());
		if (bindingResult.hasErrors()) {
			return showStart(model, view);
		}
		controllerUtils.checkTraitementContribuableAvecDecisionAci(view.getIdEntreprise());

		doInTransaction(status -> {
			final Entreprise entreprise = getTiers(Entreprise.class, view.getIdEntreprise());
			metierService.faillite(entreprise, view.getDatePrononceFaillite(), view.getRemarque());
		});

		return "redirect:/tiers/visu.do?id=" + view.getIdEntreprise();
	}
}
