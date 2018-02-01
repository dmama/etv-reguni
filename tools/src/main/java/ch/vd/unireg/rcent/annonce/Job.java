package ch.vd.unireg.rcent.annonce;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Job de construction du fichier agrégé pour l'annonce des APM à RCEnt avant même la MeP Unireg
 */
public class Job {

	private static final Charset INPUT_CHARSET = Charset.forName("UTF-8");
	private static final Charset OUTPUT_CHARSET = Charset.forName("ISO-8859-1");

	private static final String adressesResourceName = "adresses.csv";
	private static final String discriminantResourceName = "discriminant.csv";
	private static final String donneesEntreprisesResourceName = "donnees-entreprises.csv";

	private static final String outputFilename = "/tmp/annonce-rcent.csv";

	/**
	 * Point d'entrée du job
	 */
	public static void main(String[] args) throws Exception {

		// chargement des fichiers en mémoire
		final Map<Long, DiscriminantData> discriminants = chargementFichier(discriminantResourceName, DiscriminantData.class);
		final Map<Long, DonneesEntrepriseData> donnees = chargementFichier(donneesEntreprisesResourceName, DonneesEntrepriseData.class);
		final Map<Long, AdresseData> adresses = chargementFichier(adressesResourceName, AdresseData.class);

		// ouvrons le fichier de sortie
		try (OutputStream os = new FileOutputStream(outputFilename);
		     OutputStreamWriter osw = new OutputStreamWriter(os, OUTPUT_CHARSET);
		     BufferedWriter bw = new BufferedWriter(osw)) {

			// la ligne décrivant les colonnes
			bw.write("NO_CTB;RAISON_SOCIALE;CHEZ;RUE;NPA;LIEU;CANTON_SIEGE;PAYS_SIEGE;FORME_JURIDIQUE;CODE_FORME_JURIDIQUE;LANGUE;SIEGE");
			bw.newLine();

			// on boucle sur les sociétés qui doivent être exportées
			for (Map.Entry<Long, DiscriminantData> entry : discriminants.entrySet()) {
				if (entry.getValue().isExported()) {

					// récupération des données chargées pour cette entreprise
					final AdresseData adresse = adresses.get(entry.getKey());
					if (adresse == null) {
						System.err.println(String.format("Aucune adresse pour l'entreprise %d.", entry.getKey()));
					}
					final DonneesEntrepriseData donneesEntreprise = donnees.get(entry.getKey());
					if (donneesEntreprise == null) {
						System.err.println(String.format("Aucune donnée d'entreprise pour l'entreprise %d.", entry.getKey()));
					}

					// export des données
					if (adresse != null && donneesEntreprise != null) {
						final Pair<String, String> typeEntite = getTypeEntite(donneesEntreprise.getFormeJuridique());
						bw.write(String.format("%d;%s;%s;%s;%s;%s;%s;%s;%s;%s;Français;%s",
						                       entry.getKey(),
						                       StringUtils.trimToEmpty(donneesEntreprise.getRaisonSociale()),
						                       StringUtils.trimToEmpty(adresse.getChez()),
						                       StringUtils.trimToEmpty(adresse.getRue()),
						                       StringUtils.trimToEmpty(adresse.getNpa()),
						                       StringUtils.trimToEmpty(adresse.getLieu()),
						                       StringUtils.trimToEmpty(donneesEntreprise.getCantonSiege()),
						                       StringUtils.isNotBlank(donneesEntreprise.getCantonSiege()) ? "CH" : StringUtils.trimToEmpty(donneesEntreprise.getPaysSiege()),
						                       typeEntite.getLeft(),
						                       typeEntite.getRight(),
						                       donneesEntreprise.getSiege()));
						bw.newLine();
					}
				}
			}
		}
	}

	private static Pair<String, String> getTypeEntite(String formeJuridiqueRegpm) {
		switch (formeJuridiqueRegpm) {
		case "ASS":
			return Pair.of("association", "0109");
		case "DP":
			return Pair.of("droit public", "0224");
		case "FDS. PLAC.":
			return Pair.of("fonds de placements", "0114");
		case "FOND":
			return Pair.of("fondation", "0110");
		default:
			throw new IllegalArgumentException("Pas de mapping pour la forme juridique RegPM '" + formeJuridiqueRegpm + "'");
		}
	}

	/**
	 * Chargement du fichier donné avec les
	 * @param resourceName nom de la resource à charger (= nom du fichier csv)
	 * @param clazz classe de la donnée à extraire du fichier
	 * @param <T> type de la donnée à extraire du fichier
	 * @return une map indexée par numéro d'entreprise
	 */
	private static <T extends WithEntrepriseId> Map<Long, T> chargementFichier(String resourceName, Class<T> clazz) throws IOException, NoSuchMethodException, IllegalAccessException {
		final Method valueOf = clazz.getDeclaredMethod("valueOf", String.class);
		if (!Modifier.isStatic(valueOf.getModifiers()) || !clazz.isAssignableFrom(valueOf.getReturnType())) {
			throw new NoSuchMethodException("static valueOf returning " + clazz.getName());
		}
		try (InputStream is = Job.class.getResourceAsStream(resourceName);
		     InputStreamReader isr = new InputStreamReader(is, INPUT_CHARSET);
		     BufferedReader br = new BufferedReader(isr)) {

			final Map<Long, T> map = new LinkedHashMap<>();     // pour conserver l'ordre d'insertion lors d'une itération sur le set
			String line;
			while ((line = br.readLine()) != null) {
				try {
					//noinspection unchecked
					final T data = (T) valueOf.invoke(null, line);
					if (data != null) {
						final T oldData = map.put(data.getNoEntreprise(), data);
						if (oldData != null) {
							System.err.println(String.format("Fichier %s, clé %d utilisée plusieurs fois... (seule la dernière est conservée)", resourceName, data.getNoEntreprise()));
						}
					}
				}
				catch (InvocationTargetException e) {
					if (e.getCause() instanceof UnreckognizedLineException) {
						System.err.println(String.format("Fichier %s, ligne non-reconnue : '%s'", resourceName, line));
					}
					else if (e.getCause() instanceof RuntimeException) {
						throw (RuntimeException) e.getCause();
					}
					else {
						throw new RuntimeException(e.getCause());
					}
				}
			}

			return map;
		}
	}

}
