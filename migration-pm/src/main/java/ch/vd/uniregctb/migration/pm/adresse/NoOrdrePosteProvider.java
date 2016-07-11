package ch.vd.uniregctb.migration.pm.adresse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

public class NoOrdrePosteProvider implements FactoryBean<Map<Integer, List<Integer>>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(NoOrdrePosteProvider.class);

	private final Map<Integer, List<Integer>> mapping;

	public NoOrdrePosteProvider(String mappingFilename) throws IOException {

		// seules les première (numéro ordre poste source) et dernières colonnes (mapping) nous intéressent
		final Pattern pattern = Pattern.compile("([0-9]+);.*;(([0-9]+)(?:-[0-9]+)*)");

		try (Reader r = new FileReader(mappingFilename);
		     BufferedReader br = new BufferedReader(r)) {

			final Map<Integer, List<Integer>> map = new TreeMap<>();
			String line;
			while ((line = br.readLine()) != null) {
				final Matcher matcher = pattern.matcher(line);
				if (!matcher.matches()) {
					LOGGER.warn("Ligne du fichier de mapping ignorée : " + line);
				}
				else {
					final int noOrdrePosteSource = Integer.parseInt(matcher.group(1));
					final String mappingStr = matcher.group(2);
					final List<Integer> mapping = extractMappingValues(mappingStr, '-');
					if (map.put(noOrdrePosteSource, mapping) != null) {
						LOGGER.warn("Plusieurs mappings pour le numéro d'ordre postal " + noOrdrePosteSource + ", seul le dernier sera pris en compte.");
					}
				}
			}
			this.mapping = Collections.unmodifiableMap(map);
		}
		catch (FileNotFoundException e) {
			// pour donner un peu plus d'aide!!
			throw new FileNotFoundException(new File(mappingFilename).getAbsolutePath());
		}
	}

	private static List<Integer> extractMappingValues(String mappingStr, char separator) {
		final String[] splitted = mappingStr.split(String.format("%c", separator));
		final List<Integer> values = new ArrayList<>(splitted.length);
		for (int i = 0 ; i < splitted.length ; ++ i) {
			values.add(Integer.parseInt(splitted[i]));
		}
		return values;
	}

	@Override
	public Map<Integer, List<Integer>> getObject() throws Exception {
		return this.mapping;
	}

	@Override
	public Class<Map> getObjectType() {
		return Map.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
