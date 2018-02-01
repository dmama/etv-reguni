package ch.vd.unireg.entreprise.complexe;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.tiers.view.TiersCriteriaView;

public abstract class AbstractProcessusComplexeRechercheController extends AbstractProcessusComplexeController implements InitializingBean {

	private SearchTiersComponent searchComponent;

	/**
	 * Vérifie les droits d'accès IFOSEC de l'utilisateur connecté et explose si quelque chose ne va pas
	 * @throws AccessDeniedException en cas d'accès interdit
	 */
	protected abstract void checkDroitAcces() throws AccessDeniedException;

	/**
	 * @return le nom du bean stocké en session pour les critères de recherche de l'entreprise de départ
	 */
	protected abstract String getSearchCriteriaSessionName();

	@Override
	public void afterPropertiesSet() {
		this.searchComponent = buildSearchComponent(getSearchCriteriaSessionName(), getSearchResultViewPath(),
		                                            this::fillCriteriaWithImplicitValues);
	}

	/**
	 * Arrivée sur la page de recherche
	 */
	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	public String showFormulaireRecherche(Model model, HttpSession session) {
		checkDroitAcces();
		return searchComponent.showFormulaireRecherche(model, session);
	}

	/**
	 * Effacement des critères de recherche
	 */
	@RequestMapping(value = "/reset-search.do", method = RequestMethod.GET)
	public String resetCriteresRecherche(HttpSession session) {
		checkDroitAcces();
		return searchComponent.resetCriteresRecherche(session, "list.do");
	}

	/**
	 * Lancement de la recherche (en fait, c'est le GET du redirect qui fera effectivement la recherche)
	 */
	@RequestMapping(value = "/list.do", method = RequestMethod.POST)
	public String doRecherche(@Valid @ModelAttribute(value = SearchTiersComponent.COMMAND) TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model) {
		checkDroitAcces();
		return searchComponent.doRecherche(view, bindingResult, session, model, "list.do");
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
}
