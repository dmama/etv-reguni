package ch.vd.uniregctb.entreprise.complexe;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.EnumSet;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

@Controller
@RequestMapping("/processuscomplexe/annulation/faillite")
public class AnnulationFailliteController extends AbstractProcessusComplexeController {

	public static final String CRITERIA_NAME = "AnnulationFailliteCriteria";

	private static final String TYPES_RECHERCHE_NOM_ENUM = "typesRechercheNom";
	private static final String TYPES_RECHERCHE_FJ_ENUM = "formesJuridiquesEnum";
	private static final String TYPES_RECHERCHE_CAT_ENUM = "categoriesEntreprisesEnum";

	private static final String LIST = "list";
	private static final String COMMAND = "command";
	private static final String ERROR_MESSAGE = "errorMessage";

	private void checkDroitAcces() throws AccessDeniedException {
		checkAnyGranted("Vous ne possédez aucun droit IfoSec pour l'accès au processus complexe d'annulation de faillite d'entreprise.",
		                Role.FAILLITE_ENTREPRISE);
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	public String showFormulaireRecherche(Model model, HttpSession session) {
		checkDroitAcces();
		final TiersCriteriaView criteria = (TiersCriteriaView) session.getAttribute(CRITERIA_NAME);
		return showRecherche(model, criteria, false);
	}

	@RequestMapping(value = "/reset-search.do", method = RequestMethod.GET)
	public String resetCriteresRecherche(HttpSession session) {
		checkDroitAcces();
		session.removeAttribute(CRITERIA_NAME);
		return "redirect:list.do";
	}

	@RequestMapping(value = "/list.do", method = RequestMethod.POST)
	public String doRecherche(@Valid @ModelAttribute(value = COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model) {
		checkDroitAcces();
		if (bindingResult.hasErrors()) {
			return showRecherche(model, view, true);
		}
		else {
			session.setAttribute(CRITERIA_NAME, view);
		}
		return "redirect:list.do";
	}

	private String showRecherche(Model model, @Nullable TiersCriteriaView criteria, boolean error) {
		if (criteria == null) {
			criteria = new TiersCriteriaView();
			criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
		}
		else if (!error) {
			// lancement de la recherche selon les critères donnés

			// reformattage du numéro IDE
			if (StringUtils.isNotBlank(criteria.getNumeroIDE())) {
				criteria.setNumeroIDE(NumeroIDEHelper.normalize(criteria.getNumeroIDE()));
			}

			criteria.setTiersActif(Boolean.FALSE);
			criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.ENTREPRISE);
			criteria.setEtatsEntrepriseInterdits(EnumSet.of(TypeEtatEntreprise.ABSORBEE, TypeEtatEntreprise.DISSOUTE));
			criteria.setEtatEntrepriseCourant(TypeEtatEntreprise.EN_FAILLITE);
			model.addAttribute(LIST, searchTiers(criteria, model, ERROR_MESSAGE));
		}

		model.addAttribute(COMMAND, criteria);
		model.addAttribute(TYPES_RECHERCHE_NOM_ENUM, tiersMapHelper.getMapTypeRechercheNom());
		model.addAttribute(TYPES_RECHERCHE_FJ_ENUM, tiersMapHelper.getMapFormeJuridiqueEntreprise());
		model.addAttribute(TYPES_RECHERCHE_CAT_ENUM, tiersMapHelper.getMapCategoriesEntreprise());
		return "entreprise/annulation-faillite/list";
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.GET)
	public String showStart(final Model model, @RequestParam("id") final long idEntreprise) {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(idEntreprise);

		return doInReadOnlyTransaction(new TransactionCallback<String>() {
			@Override
			public String doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = getTiers(Entreprise.class, idEntreprise);
				final EtatEntreprise etat = entreprise.getEtatActuel();
				if (etat == null || etat.getType() != TypeEtatEntreprise.EN_FAILLITE) {
					Flash.error("L'entreprise sélectionnée n'est pas/plus dans l'état 'En faillite'.", 4000);
					return "redirect:list.do";
				}

				final FailliteView view = new FailliteView(idEntreprise);
				view.setDatePrononceFaillite(etat.getDateObtention());
				return showStart(model, view);
			}
		});
	}

	private String showStart(Model model, FailliteView view) {
		model.addAttribute(COMMAND, view);
		return "entreprise/annulation-faillite/start";
	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	public String doFaillite(Model model, @Valid @ModelAttribute(value = COMMAND) final FailliteView view, BindingResult bindingResult) throws Exception {
		checkDroitAcces();
		controllerUtils.checkAccesDossierEnEcriture(view.getIdEntreprise());
		if (bindingResult.hasErrors()) {
			return showStart(model, view);
		}
		controllerUtils.checkTraitementContribuableAvecDecisionAci(view.getIdEntreprise());

		doInTransaction(new MetierServiceExceptionAwareWithoutResultCallback() {
			@Override
			protected void doExecute(TransactionStatus status) throws MetierServiceException {
				final Entreprise entreprise = getTiers(Entreprise.class, view.getIdEntreprise());
				metierService.annuleFaillite(entreprise, view.getDatePrononceFaillite(), view.getRemarque());
			}
		});

		return "redirect:/tiers/visu.do?id=" + view.getIdEntreprise();
	}
}
