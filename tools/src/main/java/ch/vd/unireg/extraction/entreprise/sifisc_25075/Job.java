package ch.vd.unireg.extraction.entreprise.sifisc_25075;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ch.vd.registre.base.date.PartialDateException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.ws.parties.v7.Entry;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.xml.party.corporation.v5.BusinessYear;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.unireg.common.BatchIterator;
import ch.vd.unireg.common.StandardBatchIterator;
import ch.vd.unireg.utils.WebServiceV7Helper;
import ch.vd.unireg.xml.DataHelper;

public class Job {

	// INTEGRATION
	private static final String urlWebServiceUnireg = "http://unireg-in.etat-de-vaud.ch/fiscalite/int-unireg/ws/v7";
	private static final String userWebServiceUnireg = "unireg";
	private static final String pwdWebServiceUnireg = "unireg_1014";
	private static final String urlWebServiceFidor = "http://rp-ws-va.etat-de-vaud.ch/fiscalite/int-fidor/ws/v5";
	private static final String userWebServiceFidor = "gvd0unireg";
	private static final String pwdWebServiceFidor = "Welc0me_";

	// PRE-PRODUCTION
//	private static final String urlWebServiceUnireg = "http://unireg-pp.etat-de-vaud.ch/fiscalite/unireg/ws/v7";
//	private static final String userWebServiceUnireg = "web-it";
//	private static final String pwdWebServiceUnireg = "unireg_1014";
//	private static final String urlWebServiceFidor = "http://rp-ws-va.etat-de-vaud.ch/fiscalite/val-fidor/ws/v5";
//	private static final String userWebServiceFidor = "gvd0unireg";
//	private static final String pwdWebServiceFidor = "Welc0me_";

	// PRODUCTION
//	private static final String urlWebServiceUnireg = "http://unireg-pr.etat-de-vaud.ch/fiscalite/unireg/ws/v7";
//	private static final String userWebServiceUnireg = "se renseigner...";
//	private static final String pwdWebServiceUnireg = "se renseigner...";
//	private static final String urlWebServiceFidor = "http://rp-ws-pr.etat-de-vaud.ch/fiscalite/fidor/ws/v5";
//	private static final String userWebServiceFidor = "gvd0unireg";
//	private static final String pwdWebServiceFidor = "Welc0me_";

	private static final String userId = "usrreg06";
	private static final int oid = 22;

	private static final String inputDataFilename = "input.csv";
	private static final String outputFilename = "/tmp/entreprises.csv";
	private static final String outputCharset = "ISO-8859-1";

	private static final int TAILLE_LOT = 20;
	private static final int NB_THREADS = 2;

	public static void main(String[] args) throws Exception {

		// lecture des données d'entrée
		final Map<Integer, SortedSet<RegDate>> input = loadInputData();

		// découpage en petit (?) lots
		final BatchIterator<Map.Entry<Integer, SortedSet<RegDate>>> iteratorLot = new StandardBatchIterator<>(input.entrySet(), TAILLE_LOT);

		// mise en place de l'infrastructure d'exécuteurs
		final ExecutorService execService = Executors.newFixedThreadPool(NB_THREADS);
		try {
			final ExecutorCompletionService<List<Integer>> completionService = new ExecutorCompletionService<>(execService);

			// lancement des récupérations d'information
			int nbLotsEnvoyes = 0;
			while (iteratorLot.hasNext()) {
				completionService.submit(new DataExtractor(iteratorLot.next()));
				++ nbLotsEnvoyes;
			}

			// ça y est, on a fini de semer, il va bientôt falloir récolter...
			execService.shutdown();

			// préparation du fichier de sortie
			try (OutputStream os = new FileOutputStream(outputFilename);
			     Writer w = new OutputStreamWriter(os, outputCharset);
			     BufferedWriter bw = new BufferedWriter(w)) {

				bw.write("NO_CTB");
				bw.newLine();

				// et maintenant on traite les résulats reçus
				for (int i = 0; i < nbLotsEnvoyes; ++i) {
					while (true) {
						final Future<List<Integer>> future = completionService.poll(10, TimeUnit.SECONDS);
						if (future != null) {
							final List<Integer> taskResult = future.get();
							for (Integer data : taskResult) {
								// dump dans le fichier de sortie
								dump(bw, data);
							}
							break;
						}

						System.err.println("Pas de résultat pendant les 10 dernières secondes... on attend...");
					}
				}
			}
		}
		finally {
			execService.shutdownNow();
			while (!execService.isTerminated()) {
				execService.awaitTermination(1, TimeUnit.SECONDS);
			}
		}
	}

	private static void dump(BufferedWriter bw, Integer data) throws IOException {
		bw.write(String.format("%d", data));
		bw.newLine();
	}

	private static SortedMap<Integer, SortedSet<RegDate>> loadInputData() throws IOException {

		final Pattern pattern = Pattern.compile("^(\\d{1,8})[;,](\\d{8})$");        // un numéro de contribuable suivi d'une date YYYYMMDD
		try (InputStream is = Job.class.getResourceAsStream(inputDataFilename);
		     Reader r = new InputStreamReader(is);
		     BufferedReader br = new BufferedReader(r)) {

			final SortedMap<Integer, SortedSet<RegDate>> data = new TreeMap<>();
			String line = br.readLine();
			while (line != null) {
				// on extrait déjà ici les lignes de commentaires
				if (!line.startsWith("#")) {
					final Matcher matcher = pattern.matcher(line);
					if (matcher.matches()) {
						try {
							final int noContribuable = Integer.parseInt(matcher.group(1));
							final RegDate date = RegDate.fromIndex(Integer.parseInt(matcher.group(2)), false);
							data.computeIfAbsent(noContribuable, x -> new TreeSet<>()).add(date);
						}
						catch (PartialDateException | IllegalArgumentException e) {
							System.err.println("Ligne ignorée : " + line);
						}
					}
				}
				line = br.readLine();
			}

			return data;
		}
	}

	/**
	 * Tâche d'extraction de données
	 */
	private static class DataExtractor implements Callable<List<Integer>> {

		private static final Set<PartyPart> PARTS = EnumSet.of(PartyPart.BUSINESS_YEARS);

		private final Map<Integer, SortedSet<RegDate>> input;

		public DataExtractor(List<Map.Entry<Integer, SortedSet<RegDate>>> input) {
			this.input = input.stream()
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		@Override
		public List<Integer> call() throws Exception {
			final Parties parties = WebServiceV7Helper.getParties(urlWebServiceUnireg, userWebServiceUnireg, pwdWebServiceUnireg, userId, oid, input.keySet(), PARTS);
			final List<Integer> output = new ArrayList<>(input.size());
			for (Entry partyEntry : parties.getEntries()) {
				if (partyEntry.getError() != null) {
					System.err.println(String.format("%d;\"%s\"",
					                                 partyEntry.getPartyNo(),
					                                 partyEntry.getError().getErrorMessage().replaceAll("\n", " ")));
					continue;
				}

				final Corporation party = (Corporation) partyEntry.getParty();
				final Set<RegDate> debutsExercices = party.getBusinessYears().stream()
						.map(BusinessYear::getDateFrom)
						.map(DataHelper::xmlToCore)
						.collect(Collectors.toSet());
				final Set<RegDate> datesDebutHC = new HashSet<>(input.get(party.getNumber()));
				datesDebutHC.removeAll(debutsExercices);
				if (!datesDebutHC.isEmpty()) {
					// pas calé sur les exercices commerciaux
					output.add(party.getNumber());
				}
				else {
					System.err.println(String.format("%d;\"Synchrone avec exercice commercial\"", party.getNumber()));
				}
			}
			return output;
		}
	}
}
