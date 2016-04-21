package ch.vd.uniregctb.entreprise.complexe;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.view.TiersCriteriaView;

public abstract class AbstractProcessusComplexeRechercheController extends AbstractProcessusComplexeController {

	private static final String TYPES_RECHERCHE_NOM_ENUM = "typesRechercheNom";
	private static final String TYPES_RECHERCHE_FJ_ENUM = "formesJuridiquesEnum";
	private static final String TYPES_RECHERCHE_CAT_ENUM = "categoriesEntreprisesEnum";

	private static final String LIST = "list";
	private static final String ERROR_MESSAGE = "errorMessage";
	protected static final String COMMAND = "command";

	/**
	 * Vérifie les droits d'accès IFOSEC de l'utilisateur connecté et explose si quelque chose ne va pas
	 * @throws AccessDeniedException en cas d'accès interdit
	 */
	protected abstract void checkDroitAcces() throws AccessDeniedException;

	/**
	 * @return le nom du bean stocké en session pour les critères de recherche de l'entreprise de départ
	 */
	protected abstract String getSearchCriteriaSessionName();

	/**
	 * Arrivée sur la page de recherche
	 */
	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	public String showFormulaireRecherche(Model model, HttpSession session) {
		checkDroitAcces();
		final TiersCriteriaView criteria = (TiersCriteriaView) session.getAttribute(getSearchCriteriaSessionName());
		fillModelForRecherche(model, criteria, false);
		return getSearchResultViewPath();
	}

	/**
	 * Effacement des critères de recherche
	 */
	@RequestMapping(value = "/reset-search.do", method = RequestMethod.GET)
	public String resetCriteresRecherche(HttpSession session) {
		checkDroitAcces();
		session.removeAttribute(getSearchCriteriaSessionName());
		return "redirect:list.do";
	}

	/**
	 * Lancement de la recherche (en fait, c'est le GET du redirect qui fera effectivement la recherche)
	 */
	@RequestMapping(value = "/list.do", method = RequestMethod.POST)
	public String doRecherche(@Valid @ModelAttribute(value = COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model) {
		checkDroitAcces();
		if (bindingResult.hasErrors()) {
			fillModelForRecherche(model, view, true);
			return getSearchResultViewPath();
		}
		else {
			session.setAttribute(getSearchCriteriaSessionName(), view);
		}
		return "redirect:list.do";
	}

	/**
	 * Remplit les critères avec les champs implicites nécessaires au bon fonctionnement du cas complexe
	 * @param criteria les critères à adapter
	 */
	protected abstract void fillCriteriaWithImplicitValues(TiersCriteriaView criteria);

	/**
	 * @return le chemin vers la page jsp (sans l'extension jsp, cependant) d'affichage du formulaire et des résultats de recherche
	 */
	protected abstract String getSearchResultViewPath();

	/**
	 * Lancement de la recherche pour de vrai et remplissage des données correspondantes dans le modèle
	 * @param model le modèle
	 * @param criteria les critères de recherche renseignés dans le formulaire
	 * @param error <code>true</code> si nous sommes dans un cas d'erreur (= ne pas faire la recherche)
	 */
	private void fillModelForRecherche(Model model, TiersCriteriaView criteria, boolean error) {
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

			fillCriteriaWithImplicitValues(criteria);
			model.addAttribute(LIST, searchTiers(criteria, model, ERROR_MESSAGE));
		}

		model.addAttribute(COMMAND, criteria);
		model.addAttribute(TYPES_RECHERCHE_NOM_ENUM, tiersMapHelper.getMapTypeRechercheNom());
		model.addAttribute(TYPES_RECHERCHE_FJ_ENUM, tiersMapHelper.getMapFormeJuridiqueEntreprise());
		model.addAttribute(TYPES_RECHERCHE_CAT_ENUM, tiersMapHelper.getMapCategoriesEntreprise());
	}
}
