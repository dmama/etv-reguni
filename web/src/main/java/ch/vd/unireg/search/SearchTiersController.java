package ch.vd.unireg.search;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.NumeroIDEHelper;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.TooManyClausesIndexerException;
import ch.vd.unireg.indexer.tiers.GlobalTiersSearcher;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.indexer.tiers.TopList;
import ch.vd.unireg.tiers.TiersCriteria;

@Controller
@RequestMapping(value = "/search")
public class SearchTiersController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchTiersController.class);

	private GlobalTiersSearcher searcher;
	private ApplicationContext applicationContext;

	private static final Pattern PREFIXE_IDE_PATTERN = Pattern.compile("(CHE|ADM)");

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
		// --> en tomcat 8, l'interprétation en UTF-8 par tomcat est maintenant le défaut, plus la peine de changer l'encoding (http://tomcat.apache.org/migration-8.html#URIEncoding)
		//query = EncodingFixHelper.fixFromIso(query);

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
				if (StringUtils.isNotBlank(saveQueryTo) && "simpleSearchQuery".equalsIgnoreCase(saveQueryTo) && PREFIXE_IDE_PATTERN.matcher(query).find()) {
					query = StringUtils.trimToNull(query.replaceAll("[\\s\\u00a0.-]+", StringUtils.EMPTY).toUpperCase(Locale.ENGLISH));
				}
				final TopList<TiersIndexedData> list = searcher.searchTop(query, filter, 200);
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
					criteria.setTypesTiersImperatifs(filter.getTypesTiers());
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
					criteria.setDateNaissanceInscriptionRC(RegDateHelper.displayStringToRegDate(dateNaissance, true));
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
			catch (NumberFormatException | ParseException e) {
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

	/**
	 * @param ide         une chaine de caractère pouvant représenter un numéro IDE
	 * @param excludedIds (optionnel) les identifiants des tiers qui ne doivent pas faire partie de la réponse, même si leur IDE est le même
	 * @return la liste des identifiants des tiers qui ont ce numéro IDE (triée par ordre croissant)
	 */
	@ResponseBody
	@RequestMapping(value = "/byIDE.do", method = RequestMethod.GET)
	public List<Long> getTiersWithIDE(@RequestParam("ide") String ide, @RequestParam(value = "excluded", required = false) Set<Long> excludedIds) {

		// un peu de nettoyage pour commencer
		final String cleanedUp = NumeroIDEHelper.normalize(ide);
		if (cleanedUp == null) {
			return Collections.emptyList();
		}

		// puis la recherche proprement dite
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumeroIDE(cleanedUp);

		try {
			final List<TiersIndexedData> found = searcher.search(criteria);
			return found.stream()
					.map(TiersIndexedData::getNumero)
					.filter(id -> excludedIds == null || !excludedIds.contains(id))
					.distinct()
					.sorted()
					.collect(Collectors.toList());
		}
		catch (IndexerException e) {
			// on ne peut pas faire grand'chose... un petit log et puis s'en va en faisant comme si aucun tiers n'avait été trouvé
			LOGGER.error("Impossible de rechercher les tiers dont le numéro IDE est '" + cleanedUp + "'", e);
			return Collections.emptyList();
		}
	}
}
