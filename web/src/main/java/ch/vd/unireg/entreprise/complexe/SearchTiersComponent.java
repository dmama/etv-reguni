package ch.vd.unireg.entreprise.complexe;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.NumeroIDEHelper;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.TooManyResultsIndexerException;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersIndexedDataView;
import ch.vd.unireg.tiers.TiersMapHelper;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.view.TiersCriteriaView;
import ch.vd.unireg.utils.WebContextUtils;

public class SearchTiersComponent {

	/**
	 * Interface de callback qui va servir pour completer les données de critère passées en paramètres
	 */
	public interface TiersCriteriaFiller {
		/**
		 * @param criteria le bloc de critères à compléter
		 */
		void fill(TiersCriteriaView criteria);
	}

	/**
	 * Interface de callback qui va servir pour completer les données du modèle lors de l'affichage de l'écran
	 * de formulaire de recherche / de résultats
	 */
	public interface ModelFiller {
		/**
		 * @param model le modèle à compléter
		 * @param session la session HTTP
		 * @throws RedirectException si l'affichage doit être redirigé vers une autre page
		 */
		void fill(Model model, HttpSession session) throws RedirectException;
	}

	/**
	 * Exception lancée par le {@link ModelFiller} quand la page suivante ne doit
	 * pas être la page classique d'affichage des résultats mais une autre
	 */
	public static class RedirectException extends Exception {
		private final String redirectLocation;
		public RedirectException(String redirectLocation) {
			this.redirectLocation = redirectLocation;
		}
	}

	public interface TiersSearchAdapter<T> {
		List<T> adaptSearchResult(List<TiersIndexedDataView> result, HttpSession session);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchTiersComponent.class);

	public static final String TYPES_RECHERCHE_NOM_ENUM = "typesRechercheNom";
	public static final String TYPES_RECHERCHE_FJ_ENUM = "formesJuridiquesEnum";
	public static final String TYPES_RECHERCHE_CAT_ENUM = "categoriesEntreprisesEnum";

	public static final String LIST = "list";
	public static final String ERROR_MESSAGE = "errorMessage";
	public static final String COMMAND = "command";

	private final TiersService tiersService;
	private final MessageSource messageSource;
	private final TiersMapHelper tiersMapHelper;

	private final String searchCriteriaSessionBeanName;
	private final String searchViewPath;
	private final TiersCriteriaFiller criteriaFiller;
	private final ModelFiller modelFiller;
	private final TiersSearchAdapter<?> searchAdapter;

	/**
	 * Construit un composant de recherche
	 * @param searchCriteriaSessionBeanName le nom du bean en session pour les critères de recherche
	 * @param searchViewPath le chemin d'accès à la jsp de visualisation du formulaire de recherche (et des résultats)
	 * @param criteriaFiller callback de remplissage des critères additionnels de recherche (type de tiers, constraintes supplémentaires...)
	 */
	public SearchTiersComponent(TiersService tiersService, MessageSource messageSource, TiersMapHelper tiersMapHelper,
	                            String searchCriteriaSessionBeanName, String searchViewPath,
	                            TiersCriteriaFiller criteriaFiller) {
		this(tiersService, messageSource, tiersMapHelper,
		     searchCriteriaSessionBeanName, searchViewPath,
		     criteriaFiller, null, null);
	}

	/**
	 * Construit un composant de recherche
	 * @param searchCriteriaSessionBeanName le nom du bean en session pour les critères de recherche
	 * @param searchViewPath le chemin d'accès à la jsp de visualisation du formulaire de recherche (et des résultats)
	 * @param criteriaFiller callback de remplissage des critères additionnels de recherche (type de tiers, constraintes supplémentaires...)
	 * @param modelFiller [optionnel] callback de remplissage du modèle à l'affichage de la page de recherche (avec ou sans résultats)
	 */
	public SearchTiersComponent(TiersService tiersService, MessageSource messageSource, TiersMapHelper tiersMapHelper,
	                            String searchCriteriaSessionBeanName, String searchViewPath,
	                            TiersCriteriaFiller criteriaFiller, ModelFiller modelFiller) {
		this(tiersService, messageSource, tiersMapHelper,
		     searchCriteriaSessionBeanName, searchViewPath,
		     criteriaFiller, modelFiller, null);
	}

	/**
	 * Construit un composant de recherche
	 * @param searchCriteriaSessionBeanName le nom du bean en session pour les critères de recherche
	 * @param searchViewPath le chemin d'accès à la jsp de visualisation du formulaire de recherche (et des résultats)
	 * @param criteriaFiller callback de remplissage des critères additionnels de recherche (type de tiers, constraintes supplémentaires...)
	 * @param modelFiller [optionnel] callback de remplissage du modèle à l'affichage de la page de recherche (avec ou sans résultats)
	 * @param searchAdapter [optionnel] callback de transformation des résultats de recherche avant placement dans le modèle
	 */
	public SearchTiersComponent(TiersService tiersService, MessageSource messageSource, TiersMapHelper tiersMapHelper,
	                            String searchCriteriaSessionBeanName, String searchViewPath,
	                            TiersCriteriaFiller criteriaFiller,
	                            ModelFiller modelFiller,
	                            TiersSearchAdapter<?> searchAdapter) {
		this.tiersService = tiersService;
		this.messageSource = messageSource;
		this.tiersMapHelper = tiersMapHelper;
		this.searchCriteriaSessionBeanName = searchCriteriaSessionBeanName;
		this.searchViewPath = searchViewPath;
		this.criteriaFiller = criteriaFiller;
		this.modelFiller = modelFiller;
		this.searchAdapter = searchAdapter;
	}

	@NotNull
	public List<TiersIndexedDataView> _searchTiers(TiersCriteriaView criteriaView) throws IndexerException {
		final List<TiersIndexedData> results = tiersService.search(criteriaView.asCore());
		Assert.notNull(results);

		final List<TiersIndexedDataView> list = new ArrayList<>(results.size());
		for (TiersIndexedData d : results) {
			list.add(new TiersIndexedDataView(d));
		}
		return list;
	}

	/**
	 * Recherche des tiers correspondant aux critères donnés
	 * @param criteriaView les critères, justement
	 * @param model le modéle (pour l'introduction d'un éventuel message d'erreur)
	 * @param champModelMessageErreur le nom du champ à utiliser dans le modèle pour les éventuels messages d'erreur
	 * @return la liste des tiers trouvés (vide si erreur)
	 */
	@NotNull
	public List<TiersIndexedDataView> searchTiers(TiersCriteriaView criteriaView, Model model, String champModelMessageErreur) {
		try {
			return _searchTiers(criteriaView);
		}
		catch (TooManyResultsIndexerException e) {
			LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
			final String msg;
			if (e.getNbResults() > 0) {
				msg = messageSource.getMessage("error.preciser.recherche.trouves", new Object[] {String.valueOf(e.getNbResults())}, WebContextUtils.getDefaultLocale());
			}
			else {
				msg = messageSource.getMessage("error.preciser.recherche", null, WebContextUtils.getDefaultLocale());
			}
			model.addAttribute(champModelMessageErreur, msg);
		}
		catch (IndexerException e) {
			LOGGER.error("Exception dans l'indexer: " + e.getMessage(), e);
			model.addAttribute(champModelMessageErreur, messageSource.getMessage("error.recherche", null, WebContextUtils.getDefaultLocale()));
		}

		return Collections.emptyList();
	}

	/**
	 * Arrivée sur la page de recherche
	 * (à appeler à l'affichage du formulaire de recherche)
	 */
	public String showFormulaireRecherche(Model model, HttpSession session) {
		final TiersCriteriaView criteria = getSessionView(session);
		return fillModelForRecherche(model, session, criteria, false);
	}

	/**
	 * Effacement des critères de recherche
	 * (à appeler à la demande d'effacement du formulaire de recherche)
	 */
	public String resetCriteresRecherche(HttpSession session, String redirectAction) {
		session.removeAttribute(searchCriteriaSessionBeanName);
		return "redirect:" + redirectAction;
	}

	private TiersCriteriaView getSessionView(HttpSession session) {
		return (TiersCriteriaView) session.getAttribute(searchCriteriaSessionBeanName);
	}

	public void fillModel(Model model, HttpSession session, boolean error) {
		fillModelForRecherche(model, session, getSessionView(session), error);
	}

	/**
	 * Lancement de la recherche pour de vrai et remplissage des données correspondantes dans le modèle
	 * @param model le modèle
	 * @param session la session HTTP
	 * @param criteria les critères de recherche renseignés dans le formulaire
	 * @param error <code>true</code> si nous sommes dans un cas d'erreur (= ne pas faire la recherche)
	 * @return le chemin vers la vue d'affichage des résultats, ou redirect vers autre chose
	 */
	private String fillModelForRecherche(Model model, HttpSession session, TiersCriteriaView criteria, boolean error) {

		if (modelFiller != null) {
			try {
				modelFiller.fill(model, session);
			}
			catch (RedirectException e) {
				return "redirect:" + e.redirectLocation;
			}
		}

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

			criteriaFiller.fill(criteria);
			final List<TiersIndexedDataView> searchResults = searchTiers(criteria, model, ERROR_MESSAGE);
			if (searchAdapter != null) {
				model.addAttribute(LIST, searchAdapter.adaptSearchResult(searchResults, session));
			}
			else {
				model.addAttribute(LIST, searchResults);
			}
		}

		model.addAttribute(COMMAND, criteria);
		model.addAttribute(TYPES_RECHERCHE_NOM_ENUM, tiersMapHelper.getMapTypeRechercheNom());
		model.addAttribute(TYPES_RECHERCHE_FJ_ENUM, tiersMapHelper.getMapFormesJuridiquesEntreprise());
		model.addAttribute(TYPES_RECHERCHE_CAT_ENUM, tiersMapHelper.getMapCategoriesEntreprise());
		return searchViewPath;
	}

	/**
	 * Lancement de la recherche (en fait, c'est le GET du redirect qui fera effectivement la recherche)
	 * (à appeler à la soumission du formulaire de recherche)
	 */
	public String doRecherche(TiersCriteriaView view, BindingResult bindingResult, HttpSession session, Model model, String searchSuccessRedirectAction) {
		if (bindingResult.hasErrors()) {
			return fillModelForRecherche(model, session, view, true);
		}
		else {
			session.setAttribute(searchCriteriaSessionBeanName, view);
		}
		return "redirect:" + searchSuccessRedirectAction;
	}
}
