package ch.vd.uniregctb.migration.pm.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe utilitaire pour les méthodes d'extraction de données depuis des fichiers plats
 */
public abstract class DataLoadHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataLoadHelper.class);

	/**
	 * Les numéros de PM sont constitués de 1 à 5 chiffres pour l'instant...
	 */
	private static final Pattern ID_PM = Pattern.compile("[0-9]{1,5}");

	/**
	 * Chargement d'identifiants de PM depuis un fichier plat (ligne à ligne)
	 * @param filename nom du fichier source
	 * @param usage description de l'utilisation des données pour les logs de suivi
	 * @return une liste des identifiants trouvés
	 * @throws IOException en cas de souci avec l'accès au fichier
	 */
	public static List<Long> loadIdentifiantsPM(String filename, Supplier<String> usage) throws IOException {
		LOGGER.info("Chargement du fichier " + filename + (usage == null ? StringUtils.EMPTY : " pour " + usage.get()));
		try (FileInputStream fis = new FileInputStream(filename); Reader reader = new InputStreamReader(fis)) {
			return loadIdentifiantsPM(reader);
		}
	}

	/**
	 * Chargement d'identifiants de PM depuis un reader (ligne à ligne)
	 * @param reader extracteur de caractères
	 * @return une liste des identifiants trouvés
	 * @throws IOException en cas de souci avec l'accès aux données
	 */
	public static List<Long> loadIdentifiantsPM(Reader reader) throws IOException {
		try (BufferedReader br = new BufferedReader(reader)) {
			return loadData(br, ID_PM, matcher -> Long.parseLong(matcher.group()));
		}
	}

	/**
	 * Méthode générique de récupération de données depuis un fichier plat (ligne à ligne)
	 * @param reader extracteur de lignes
	 * @param pattern pattern qui permet de ne conserver que les lignes valides
	 * @param builder transcripteur de {@link Matcher} en donnée structurée
	 * @param <T> type de la donnée structurée renvoyée
	 * @return la liste des données lues
	 * @throws IOException en cas de souci avec l'accès aux données
	 */
	public static <T> List<T> loadData(BufferedReader reader, Pattern pattern, Function<Matcher, ? extends T> builder) throws IOException {

		// une liste chaînée car on n'a aucune idée du nombre d'éléments
		final List<T> liste = new LinkedList<>();

		// ligne par ligne, on lit les données
		String ligne;
		while ((ligne = reader.readLine()) != null) {
			final Matcher matcher = pattern.matcher(ligne);
			if (matcher.matches()) {
				final T data = builder.apply(matcher);
				liste.add(data);
			}
			else {
				LOGGER.warn("Ligne ignorée : '" + ligne + "'");
			}
		}

		return liste;
	}
}
