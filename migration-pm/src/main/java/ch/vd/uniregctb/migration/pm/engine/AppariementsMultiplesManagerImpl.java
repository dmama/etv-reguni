package ch.vd.uniregctb.migration.pm.engine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.migration.pm.utils.DataLoadHelper;

/**
 * Implémentation du bean qui permet au job de migration de savoir si un numéro cantonal a été
 * utilisé pour plusieurs entreprises distinctes (et, le cas échéant, lesquelles)
 */
public class AppariementsMultiplesManagerImpl implements AppariementsMultiplesManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppariementsMultiplesManagerImpl.class);

	private final Map<Long, Set<Long>> appariementsMultiples;

	public AppariementsMultiplesManagerImpl(String filename) throws IOException {
		// récupération des données des appariements multiples du fichier fourni
		try (Reader reader = StringUtils.isBlank(filename) ? null : new FileReader(filename)) {
			this.appariementsMultiples = readData(reader);
		}

		// un peu de log
		LOGGER.info("Nombre de numéros cantonaux d'organisation utilisés dans plus d'une entreprise : " + this.appariementsMultiples.size());
	}

	/**
	 * Constructeur utilisable pendant les tests pour ne pas dépendre d'un fichier sur disque
	 * @param is input stream
	 * @throws IOException en cas de souci
	 */
	AppariementsMultiplesManagerImpl(InputStream is) throws IOException {
		// récupération des données des appariements multiples du fichier fourni
		try (Reader reader = new InputStreamReader(is)) {
			this.appariementsMultiples = readData(reader);
		}

		// un peu de log
		LOGGER.info("Nombre de numéros cantonaux d'organisation utilisés dans plus d'une entreprise : " + this.appariementsMultiples.size());
	}

	@NotNull
	private static Map<Long, Set<Long>> readData(@Nullable Reader reader) throws IOException {
		if (reader != null) {

			// extraction des données ligne par ligne
			final Pattern pattern = Pattern.compile("([0-9]{1,18});([0-9]{1,9});([0-9]{1,6}(?:,[0-9]{1,6})*)");
			final List<Pair<Long, Set<Long>>> data;
			try (BufferedReader br = new BufferedReader(reader)) {
				data = DataLoadHelper.loadData(br, pattern, matcher -> {
					final int nb = Integer.parseInt(matcher.group(2));
					if (nb > 1) {
						final long noCantonal = Long.parseLong(matcher.group(1));
						final String[] nosEntreprises = matcher.group(3).split(",");
						final Set<Long> set = new TreeSet<>();      // pour avoir un ordre bien défini
						Stream.of(nosEntreprises)
								.map(Long::parseLong)
								.forEach(set::add);
						return Pair.of(noCantonal, set);
					}
					else {
						return null;
					}
				});
			}

			// un peu de filtrage (on ne conserve que les lignes pour lesquelles il y avait plusieurs entreprises...)
			return data.stream()
					.filter(Objects::nonNull)
					.collect(Collectors.toMap(Pair::getLeft,
					                          Pair::getRight,
					                          (s1, s2) -> { throw new IllegalArgumentException(String.format("Les groupes d'entreprises %s et %s sont associées au même numéro cantonal mais sont présentes sur deux lignes différentes dans le fichier d'entrée.",
					                                                                                         CollectionsUtils.toString(s1, String::valueOf, ","),
					                                                                                         CollectionsUtils.toString(s2, String::valueOf, ",")));
					                          }));
		}
		else {
			LOGGER.warn("Aucune donnée d'appariement fournie...");
			return Collections.emptyMap();
		}
	}

	@NotNull
	@Override
	public Set<Long> getIdentifiantsEntreprisesAvecMemeAppariement(long noCantonal) {
		return appariementsMultiples.getOrDefault(noCantonal, Collections.emptySet());
	}
}
