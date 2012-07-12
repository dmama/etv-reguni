package ch.vd.uniregctb.search;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.indexer.TooManyClausesIndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.indexer.tiers.TopList;
import ch.vd.uniregctb.tiers.TiersCriteria;

@Controller
@RequestMapping(value = "/search")
public class SearchTiersController {

	private GlobalTiersSearcher searcher;
	private ApplicationContext applicationContext;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSearcher(GlobalTiersSearcher searcher) {
		this.searcher = searcher;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@RequestMapping(value = "/quick.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public SearchTiersResults quickSearch(@RequestParam(value = "query", required = true) String query,
	                                 @RequestParam(value = "filterBean", required = false) String filterBean,
	                                 @RequestParam(value = "filterParams", required = false) String filterParams,
	                                 @RequestParam(value = "saveQueryTo", required = false) String saveQueryTo,
	                                 HttpServletRequest request) throws UnsupportedEncodingException {

		SearchTiersResults results;

		// les urls sont envoyées en UTF-8 par jQuery mais interprétées en ISO-8859-1 par Tomcat (voir http://wiki.apache.org/tomcat/FAQ/CharacterEncoding#Q8 pour une correction plus othrodoxe
		// mais qui nécessiterait de changer la configuration de Tomcat)
		final byte[] bytes = query.getBytes("ISO-8859-1");
		query = new String(bytes, "UTF-8");

		if (StringUtils.isNotBlank(saveQueryTo)) {
			// on stocke la requête en session si on nous l'a demandé
			request.getSession().setAttribute(saveQueryTo, query);
		}

		final SearchTiersFilter filter = extractFilter(filterBean, filterParams);

		if (isLessThan3Chars(query)) {
			results = new SearchTiersResults("Veuillez saisir au minimum 3 caractères.");
		}
		else {
			try {
				final TopList<TiersIndexedData> list = searcher.searchTop(query, filter, 50);
				postFilter(filter, list);

				if (list != null && !list.isEmpty()) {
					results = new SearchTiersResults(buildSummary(list), list);
				}
				else {
					results = new SearchTiersResults("Aucun tiers n'a été trouvé.");
				}
			}
			catch (TooManyClausesIndexerException e) {
				results = new SearchTiersResults("Un ou plusieurs mots-clés sont trop généraux.");
			}
		}

		if (filter != null) {
			results.setFilterDescription("Note: " + filter.getDescription());
		}

		return results;
	}

	@RequestMapping(value = "/full.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public SearchTiersResults fullSearch(@RequestParam(value = "id", required = false) String id,
	                                @RequestParam(value = "nomRaison", required = false) String nomRaison,
	                                @RequestParam(value = "localite", required = false) String localite,
	                                @RequestParam(value = "dateNaissance", required = false) String dateNaissance,
	                                @RequestParam(value = "noAvs", required = false) String noAvs,
	                                @RequestParam(value = "filterBean", required = false) String filterBean,
	                                @RequestParam(value = "filterParams", required = false) String filterParams) {

		SearchTiersResults results;

		final SearchTiersFilter filter = extractFilter(filterBean, filterParams);
		id = (id == null ? id : id.replaceAll("[^0-9]", "")); // [UNIREG-3253] supprime tous les caractères non-numériques

		if (isLessThan3Chars(id) && isLessThan3Chars(nomRaison) && isLessThan3Chars(localite) && isLessThan3Chars(dateNaissance) && isLessThan3Chars(noAvs)) {
			results = new SearchTiersResults("Veuillez saisir au minimum 3 caractères.");
		}
		else {
			try {
				TiersCriteria criteria = new TiersCriteria();

				if (filter != null) {
					criteria.setTypeVisualisation(filter.getTypeVisualisation());
					criteria.setTypesTiers(filter.getTypesTiers());
					criteria.setInclureI107(filter.isInclureI107());
					criteria.setInclureTiersAnnules(filter.isInclureTiersAnnules());
					criteria.setTiersAnnulesSeulement(filter.isTiersAnnulesSeulement());
					criteria.setTiersActif(filter.isTiersActif());
				}

				criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.CONTIENT);
				criteria.setTypeRechercheDuPaysLocalite(TiersCriteria.TypeRechercheLocalitePays.ALL);
				if (!isLessThan3Chars(id)) {
					criteria.setNumero(Long.valueOf(id));
				}
				if (!isLessThan3Chars(nomRaison)) {
					criteria.setNomRaison(nomRaison);
				}
				if (!isLessThan3Chars(localite)) {
					criteria.setLocaliteOuPays(localite);
				}
				if (!isLessThan3Chars(dateNaissance)) {
					criteria.setDateNaissance(RegDateHelper.displayStringToRegDate(dateNaissance, true));
				}
				if (!isLessThan3Chars(noAvs)) {
					criteria.setNumeroAVS(noAvs);
				}

				final TopList<TiersIndexedData> list = searcher.searchTop(criteria, 50);
				postFilter(filter, list);

				if (list != null && !list.isEmpty()) {
					results = new SearchTiersResults(buildSummary(list), list);
				}
				else {
					results = new SearchTiersResults("Aucun tiers trouvé.");
				}
			}
			catch (TooManyClausesIndexerException e) {
				results = new SearchTiersResults("Un ou plusieurs mots-clés sont trop généraux.");
			}
			catch (NumberFormatException e) {
				results = new SearchTiersResults("Données erronées : " + e.getMessage());
			}
			catch (ParseException e) {
				results = new SearchTiersResults("Données erronées : " + e.getMessage());
			}
		}

		return results;
	}

	private SearchTiersFilter extractFilter(String filterBean, String filterParams) {
		SearchTiersFilter filter = null;
		if (StringUtils.isNotBlank(filterBean)) {
			final SearchTiersFilterFactory filterFactory = (SearchTiersFilterFactory) applicationContext.getBean(filterBean);
			filter = filterFactory.parse(filterParams);
		}
		return filter;
	}

	private void postFilter(SearchTiersFilter filter, TopList<TiersIndexedData> list) {
		if (filter instanceof SearchTiersFilterWithPostFiltering) {
			((SearchTiersFilterWithPostFiltering) filter).postFilter(list);
		}
	}

	private static boolean isLessThan3Chars(String s) {
		return s == null || s.trim().length() < 3;
	}

	private static String buildSummary(TopList<TiersIndexedData> list) {
		String summary = "Trouvé " + list.getTotalHits() + " tiers";
		if (list.size() < list.getTotalHits()) {
			summary += " (affichage des " + list.size() + " premiers)";
		}
		return summary;
	}
}
