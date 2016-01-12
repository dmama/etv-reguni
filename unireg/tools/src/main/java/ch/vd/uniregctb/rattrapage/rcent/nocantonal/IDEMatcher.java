package ch.vd.uniregctb.rattrapage.rcent.nocantonal;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;

/**
 * Classe de job utilitaire qui prend deux fichiers, l'un contenant un mapping entre des numéros IDE et des numéros cantonaux (en provenance de RCEnt),
 * l'autre contenant un mapping entre des numéros IDE et des numéros de contribuable (en provenance de RegPM), les accorde entre eux et génère un
 * script de mise à jour de la base de données Unireg pour associer le numéro cantonal au bon numéro de contribuable...
 */
public class IDEMatcher {

	/**
	 * Fichier composé de deux colonnes : le numéro IDE et le numéro cantonal, séparés par ";" ou "," (à choix)
	 * (les lignes où il n'y a pas de numéro IDE sont ignorées)
	 */
	private static final String rcentResourceName = "ide-nocantonal.csv";

	/**
	 * Fichier composé de deux colonnes : le numéro IDE et le numéro de contribuable, séparés par ";" ou "," (à choix)
	 * (les lignes où il n'y a pas de numéro IDE sont ignorées)
	 */
	private static final String regpmResourceName = "ide-noctb.csv";

	/**
	 * Le nom du fichier de sortie pour le script de rattrapage (qui sera écrasé s'il existe, pas de pitié !)
	 */
	private static final String outputFile = "/dev/stdout";

	/**
	 * Visa utilisé dans le script DB
	 */
	private static final String VISA = String.format("[Assignation-no-cantonal-%s]", RegDateHelper.dateToDashString(RegDate.get()));

	/**
	 * Pas de paramètres... tout est en dur dans les constantes plus haut...
	 */
	public static void main(String[] args) throws Exception {
		final Map<String, Long> rcentData = readFile(rcentResourceName);
		final Map<String, Long> regpmData = readFile(regpmResourceName);
		int count = 0;

		try (FileOutputStream fos = new FileOutputStream(outputFile);
		     PrintStream ps = new PrintStream(fos)) {

			for (Map.Entry<String, Long> regpmEntry : regpmData.entrySet()) {
				final String ide = regpmEntry.getKey();
				final Long noCantonal = rcentData.get(ide);
				if (noCantonal != null) {
					final Long noCtb = regpmEntry.getValue();
					ps.println("-- Numéro IDE " + ide + ", numéro cantonal " + noCantonal + ", numéro de contribuable " + FormatNumeroHelper.numeroCTBToDisplay(noCtb));
					ps.printf("UPDATE TIERS SET NO_ENTREPRISE=%d, LOG_MUSER='%s', LOG_MDATE=CURRENT_DATE WHERE NUMERO=%d%n", noCantonal, VISA, noCtb);
					ps.println();
					++count;
				}
			}

			// si on a trouvé des correspondances, il faut maintenant enlever les données civiles migrées
			if (count > 0) {
				ps.println("-- Effacement des données civiles");
				ps.printf("DELETE FROM DONNEE_CIVILE_ENTREPRISE WHERE ENTREPRISE_ID IN (SELECT NUMERO FROM TIERS WHERE TIERS_TYPE='Entreprise' AND NO_ENTREPRISE IS NOT NULL AND LOG_MUSER='%s');%n", VISA);
			}
		}
	}

	/**
	 * Lecture du fichier (composé de deux colonnes, la première étant un numéro IDE, la seconde un identifiant numérique).
	 * <br/><b>Remarques&nbsp;:</b>
	 * <ul>
	 *     <li>les lignes dont la première colonne (numéro IDE) est vide sont ignorées&nbsp;;</li>
	 *     <li>si un numéro IDE est présent plusieurs fois, seule la dernière valeur est prise en compte&nbsp;;</li>
	 *     <li>les lignes qui ne correspondent pas au format attendu sont ignorées.</li>
	 * </ul>
	 * @param resourceName nom du fichier CSV à lire
	 * @return une map contenant les informations lues du fichier
	 * @throws IOException en cas de problème à la lecture du fichier
	 */
	private static Map<String, Long> readFile(String resourceName) throws IOException {

		final Pattern pattern = Pattern.compile("([A-Z]{3}[0-9]{9})[;,]([0-9]{1,18})");

		final Map<String, Long> map = new LinkedHashMap<>();
		try (InputStream is = IDEMatcher.class.getResourceAsStream(resourceName);
		     InputStreamReader isr = new InputStreamReader(is);
		     BufferedReader br = new BufferedReader(isr)) {

			String line = br.readLine();
			while (line != null) {
				final Matcher matcher = pattern.matcher(line);
				if (!matcher.matches()) {
					System.err.println("Ligne ignorée (" + resourceName + ") : " + line);
				}
				else {
					final String ide = matcher.group(1);
					final long identifiant = Long.valueOf(matcher.group(2));
					if (map.containsKey(ide)) {
						System.err.print("IDE présent en plusieurs exemplaires (" + resourceName + ") : " + ide);
					}
					map.put(ide, identifiant);
				}
				line = br.readLine();
			}
		}
		return map;
	}

}
