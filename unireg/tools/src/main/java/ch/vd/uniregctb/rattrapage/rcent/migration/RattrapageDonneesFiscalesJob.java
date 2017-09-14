package ch.vd.uniregctb.rattrapage.rcent.migration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Cas de ces entreprises dont les données fiscales n'ont carrément pas été migrées depuis SIMPA...
 * Il est maintenant temps de les retrouver...
 */
public class RattrapageDonneesFiscalesJob {

	private static final String INPUT_FILE = "migration-donnees-fiscales-non-migrees.input.txt";
	private static final Set<Long> ID_ENTREPRISES_SP = new HashSet<>(Collections.singletonList(49597L));

	private static final Pattern NEW_ENTREPRISE = Pattern.compile("\\++ PM ([0-9]+)");
	private static final Pattern NEW_CATEGORY = Pattern.compile("Catégorie ([A-Z_]+) :");
	private static final Pattern RELEVANT_LINE = Pattern.compile("- (.*)$");

	private static final Map<String, CategoryHandler> HANDLERS = buildHandlers();

	private static Map<String, CategoryHandler> buildHandlers() {
		final Map<String, CategoryHandler> handlers = new HashMap<>();
		handlers.put("MAPPINGS_REGIMES_FISCAUX", new RegimesFiscauxHandler());
		handlers.put("FORS", new ForsHandler(ID_ENTREPRISES_SP));
		handlers.put("SUIVI", new SuiviHandler());
		handlers.put("DECLARATIONS", new DeclarationsHandler());
		return handlers;
	}

	public static void main(String[] args) throws IOException {

		final Map<Long, Map<String, List<String>>> data;
		try (InputStream is = RattrapageDonneesFiscalesJob.class.getResourceAsStream(INPUT_FILE);
		     Reader r = new InputStreamReader(is, Charset.defaultCharset())) {
			if (r != null) {
				throw new UnsupportedOperationException("SIFISC-21866: Ce job n'est plus utilisable, car le schéma de base de données a changé.");
			}

			data = lectureFichierInput(r);
		}

		final StringBuilder b = new StringBuilder();

		// traitement par entreprise
		for (Map.Entry<Long, Map<String, List<String>>> entrepriseEntry : data.entrySet()) {
			b.append("-- Entreprise ").append(entrepriseEntry.getKey()).append(System.lineSeparator());
			b.append(System.lineSeparator());

			for (Map.Entry<String, List<String>> categoryEntry : entrepriseEntry.getValue().entrySet()) {
				final CategoryHandler handler = HANDLERS.get(categoryEntry.getKey());
				if (handler == null) {
					throw new IllegalArgumentException("Unknown category : " + categoryEntry.getKey());
				}
				try {
					handler.buildSql(b, categoryEntry.getValue());
				}
				catch (ParseException e) {
					throw new IllegalArgumentException("Entreprise " + entrepriseEntry.getKey() + ", catégorie " + categoryEntry.getKey(), e);
				}
			}
		}

		System.out.print(b.toString());
	}

	private static Map<Long, Map<String, List<String>>> lectureFichierInput(Reader reader) throws IOException {
		final Map<Long, Map<String, List<String>>> result = new HashMap<>();
		try (BufferedReader br = new BufferedReader(reader)) {

			Map<String, List<String>> entrepriseData = null;
			List<String> entrepriseCategoryData = null;

			String line;
			while ((line = br.readLine()) != null) {
				if (StringUtils.isNotBlank(line)) {
					final Matcher newEntrepriseMatcher = NEW_ENTREPRISE.matcher(line);
					if (newEntrepriseMatcher.matches()) {
						final long noEntreprise = Long.parseLong(newEntrepriseMatcher.group(1));
						entrepriseData = result.computeIfAbsent(noEntreprise, k -> new HashMap<>());
						entrepriseCategoryData = null;
					}
					else {
						final Matcher newCategoryMatcher = NEW_CATEGORY.matcher(line);
						if (newCategoryMatcher.matches()) {
							final String category = newCategoryMatcher.group(1);
							if (entrepriseData == null) {
								throw new IllegalStateException("Nouvelle catégorie " + category + " déclarée en dehors du scope d'une entreprise...");
							}
							entrepriseCategoryData = entrepriseData.computeIfAbsent(category, k -> new LinkedList<>());
						}
						else {
							final Matcher relevantLineMatcher = RELEVANT_LINE.matcher(line);
							if (relevantLineMatcher.matches()) {
								final String relevantLine = relevantLineMatcher.group(1);
								if (entrepriseCategoryData == null) {
									throw new IllegalStateException("Nouvelle ligne en dehors de toute catégorie : " + line);
								}
								entrepriseCategoryData.add(relevantLine);
							}
							else {
								System.err.println("Ligne ignorée dans le fichier d'entrée : " + line);
							}
						}
					}
				}
			}
		}

		return result;
	}
}
