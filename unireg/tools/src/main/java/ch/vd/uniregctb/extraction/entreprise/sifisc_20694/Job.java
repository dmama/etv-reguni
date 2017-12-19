package ch.vd.uniregctb.extraction.entreprise.sifisc_20694;

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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import ch.vd.fidor.xml.regimefiscal.v2.RegimeFiscal;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.ws.parties.v7.Entry;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.wsclient.WebClientPool;
import ch.vd.unireg.xml.common.v2.DateRange;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.corporation.v5.LegalForm;
import ch.vd.unireg.xml.party.corporation.v5.TaxSystem;
import ch.vd.unireg.xml.party.taxpayer.v5.LegalFormCategory;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.uniregctb.common.BatchIterator;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.StandardBatchIterator;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.utils.WebServiceV7Helper;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClient;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClientImpl;
import ch.vd.uniregctb.xml.DataHelper;

public class Job {

	// PRE-PRODUCTION
	private static final String urlWebServiceUnireg = "http://unireg-pp.etat-de-vaud.ch/fiscalite/unireg/ws/v7";
	private static final String userWebServiceUnireg = "web-it";
	private static final String pwdWebServiceUnireg = "unireg_1014";
	private static final String urlWebServiceFidor = "http://rp-ws-va.etat-de-vaud.ch/fiscalite/fidor/ws/v5";
	private static final String userWebServiceFidor = "gvd0unireg";
	private static final String pwdWebServiceFidor = "Welc0me_";

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
		final List<InputData> input = loadInputData();

		// récupération des régimes fiscaux depuis fidor
		final Map<String, String> codesRegimesFiscaux = fetchRegimesFiscaux();

		// découpage en petit (?) lots
		final BatchIterator<InputData> iteratorLot = new StandardBatchIterator<>(input, TAILLE_LOT);

		// mise en place de l'infrastructure d'exécuteurs
		final ExecutorService execService = Executors.newFixedThreadPool(NB_THREADS);
		try {
			final ExecutorCompletionService<List<OutputData>> completionService = new ExecutorCompletionService<>(execService);

			// lancement des récupérations d'information
			int nbLotsEnvoyes = 0;
			while (iteratorLot.hasNext()) {
				completionService.submit(new DataExtractor(iteratorLot.next(), codesRegimesFiscaux));
				++ nbLotsEnvoyes;
			}

			// ça y est, on a fini de semer, il va bientôt falloir récolter...
			execService.shutdown();

			final List<StatsCollector> statsCollectors = Arrays.asList(new CountStatsCollector(),
			                                                           new ByLegalFormStatsCollector(),
			                                                           new ByRegimeFiscalCHStatsCollector(),
			                                                           new ByRegimeFiscalVDStatsCollector());

			// préparation du fichier de sortie
			try (OutputStream os = new FileOutputStream(outputFilename);
			     Writer w = new OutputStreamWriter(os, outputCharset);
			     BufferedWriter bw = new BufferedWriter(w)) {

				bw.write("NO_CTB;NO_IDE;RAISON_SOCIALE;CODE_FORME_JURIDIQUE;LIBELLE_FORME_JURIDIQUE;CATEGORIE;NO_CANTONAL;NO_CANTONAL_ETB_PRN;REGIME_FISCAL_VD;LIBELLE_REGIME_FISCAL_VD;REGIME_FISCAL_CH;LIBELLE_REGIME_FISCAL_CH;DEBUT;FIN");
				bw.newLine();

				// et maintenant on traite les résulats reçus
				for (int i = 0; i < nbLotsEnvoyes; ++i) {
					while (true) {
						final Future<List<OutputData>> future = completionService.poll(10, TimeUnit.SECONDS);
						if (future != null) {
							final List<OutputData> taskResult = future.get();
							for (OutputData data : taskResult) {
								// collecte des statistiques
								for (StatsCollector statsCollector : statsCollectors) {
									statsCollector.collect(data);
								}

								// dump dans le fichier de sortie
								dump(bw, data);
							}
							break;
						}

						System.err.println("Pas de résultat pendant les 10 dernières secondes... on attend...");
					}
				}
			}

			// puis dump des statistiques
			for (StatsCollector statsCollector : statsCollectors) {
				System.err.println(statsCollector);
			}
		}
		finally {
			execService.shutdownNow();
			while (!execService.isTerminated()) {
				execService.awaitTermination(1, TimeUnit.SECONDS);
			}
		}
	}

	private static void dump(BufferedWriter bw, OutputData data) throws IOException {
		bw.write(String.format("%d;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s",
		                       data.input.idEntreprise,
		                       data.noIDE,
		                       clean(data.raisonSociale),
		                       quote(data.codeFormeJuridique),
		                       data.libelleFormeJuridique,
		                       data.categorieEntreprise,
		                       data.input.noCantonalEntreprise != null ? Long.toString(data.input.noCantonalEntreprise) : StringUtils.EMPTY,
		                       data.input.noCantonalEtablissementPrincipal != null ? Long.toString(data.input.noCantonalEtablissementPrincipal) : StringUtils.EMPTY,
		                       quote(data.codeRegimeFiscalVD),
		                       data.libelleRegimeFiscalVD,
		                       quote(data.codeRegimeFiscalCH),
		                       data.libelleRegimeFiscalCH,
		                       RegDateHelper.dateToDashString(data.dateDebut),
		                       RegDateHelper.dateToDashString(data.dateFin)));
		bw.newLine();
	}

	private static String clean(String string) {
		return string.replaceAll("\"", StringUtils.EMPTY);
	}

	/**
	 * Pour pouvoir charger le CSV dans Excel -> les nombres qui n'en sont pas doivent être entourés de guillemets
	 */
	private static String quote(String string) {
		if (StringUtils.isBlank(string)) {
			return StringUtils.EMPTY;
		}
		return String.format("\"%s\"", string);
	}

	private interface StatsCollector {
		void collect(OutputData data);
	}

	private static class CountStatsCollector implements StatsCollector {
		private int count = 0;

		@Override
		public void collect(OutputData data) {
			++ count;
		}

		public String toString() {
			return String.format("Nombre d'entreprises : %d", count);
		}
	}

	private abstract static class ByKeyStatsCollector<T extends Comparable<T>> implements StatsCollector {
		private final Map<T, MutableInt> map = new TreeMap<>();

		@Override
		public void collect(OutputData data) {
			final T key = extractKey(data);
			if (key != null) {
				MutableInt count = map.get(key);
				if (count == null) {
					count = new MutableInt(1);
					map.put(key, count);
				}
				else {
					count.increment();
				}
			}
		}

		protected abstract T extractKey(OutputData data);

		protected abstract String getLibelle();

		public String toString() {
			final StringBuilder b = new StringBuilder(getLibelle()).append(" :");
			for (Map.Entry<T, MutableInt> entry : map.entrySet()) {
				b.append(String.format("\n- %s : %d", entry.getKey(), entry.getValue().intValue()));
			}
			return b.toString();
		}
	}

	private static class ByLegalFormStatsCollector extends ByKeyStatsCollector<String> {
		@Override
		protected String extractKey(OutputData data) {
			return StringUtils.trimToNull(data.codeFormeJuridique);
		}

		@Override
		protected String getLibelle() {
			return "Nombre d'entreprises par forme juridique";
		}
	}

	private static class ByRegimeFiscalVDStatsCollector extends ByKeyStatsCollector<String> {
		@Override
		protected String extractKey(OutputData data) {
			return StringUtils.trimToNull(data.codeRegimeFiscalVD);
		}

		@Override
		protected String getLibelle() {
			return "Nombre d'entreprises par régime fiscal VD";
		}
	}

	private static class ByRegimeFiscalCHStatsCollector extends ByKeyStatsCollector<String> {
		@Override
		protected String extractKey(OutputData data) {
			return StringUtils.trimToNull(data.codeRegimeFiscalCH);
		}

		@Override
		protected String getLibelle() {
			return "Nombre d'entreprises par régime fiscal CH";
		}
	}

	private static List<InputData> loadInputData() throws IOException {
		try (InputStream is = Job.class.getResourceAsStream(inputDataFilename);
		     Reader r = new InputStreamReader(is);
		     BufferedReader br = new BufferedReader(r)) {

			final List<InputData> data = new LinkedList<>();
			String line = br.readLine();
			while (line != null) {
				// on extrait déjà ici les lignes de commentaires
				if (!line.startsWith("#")) {
					try {
						final InputData parsed = InputData.of(line);
						data.add(parsed);
					}
					catch (ParseException e) {
						System.err.println("Ligne ignorée : " + line);
					}
				}
				line = br.readLine();
			}

			return data;
		}
	}

	/**
	 * @return une map des libellés de régimes fiscaux indexés par leur code
	 */
	private static Map<String, String> fetchRegimesFiscaux() {
		final FidorClient fidorClient = buildFidorClient();
		final List<RegimeFiscal> regimes = fidorClient.getRegimesFiscaux();
		final Map<String, String> map = new HashMap<>(regimes.size());
		for (RegimeFiscal regime : regimes) {
			map.put(regime.getCode(), regime.getLibelle());
		}
		return map;
	}

	private static FidorClient buildFidorClient() {
		final WebClientPool wcPool = new WebClientPool();
		wcPool.setBaseUrl(urlWebServiceFidor);
		wcPool.setUsername(userWebServiceFidor);
		wcPool.setPassword(pwdWebServiceFidor);

		final FidorClientImpl client = new FidorClientImpl();
		client.setWcPool(wcPool);
		return client;
	}

	/**
	 * Tâche d'extraction de données
	 */
	private static class DataExtractor implements Callable<List<OutputData>> {

		private static final Set<PartyPart> PARTS = EnumSet.of(PartyPart.LEGAL_FORMS,
		                                                       PartyPart.TAX_SYSTEMS,
		                                                       PartyPart.TAX_RESIDENCES,
		                                                       PartyPart.TAX_LIABILITIES);

		private final List<InputData> input;
		private final Map<String, String> regimesFiscaux;

		public DataExtractor(List<InputData> input, Map<String, String> regimesFiscaux) {
			this.input = input;
			this.regimesFiscaux = regimesFiscaux;
		}

		@Override
		public List<OutputData> call() throws Exception {
			final Map<Integer, InputData> inputDataMap = new HashMap<>(input.size());
			for (InputData data : input) {
				inputDataMap.put((int) data.idEntreprise, data);
			}
			final Parties parties = WebServiceV7Helper.getParties(urlWebServiceUnireg, userWebServiceUnireg, pwdWebServiceUnireg, userId, oid, inputDataMap.keySet(), PARTS);
			final List<OutputData> output = new ArrayList<>(input.size());
			for (Entry partyEntry : parties.getEntries()) {
				final InputData correspondingInput = inputDataMap.get(partyEntry.getPartyNo());
				if (partyEntry.getError() != null) {
					System.err.println(String.format("%d;%s;%d;%s;\"%s\"",
					                                 partyEntry.getPartyNo(),
					                                 correspondingInput.noCantonalEntreprise != null ? Long.toString(correspondingInput.noCantonalEntreprise) : StringUtils.EMPTY,
					                                 correspondingInput.idEtablissementPrincipal,
					                                 correspondingInput.noCantonalEtablissementPrincipal != null ? Long.toString(correspondingInput.noCantonalEtablissementPrincipal) : StringUtils.EMPTY,
					                                 partyEntry.getError().getErrorMessage().replaceAll("\n", " ")));
					continue;
				}

				final Corporation party = (Corporation) partyEntry.getParty();
				final String noIDE = party.getUidNumbers() != null && party.getUidNumbers().getUidNumber() != null && !party.getUidNumbers().getUidNumber().isEmpty() ? party.getUidNumbers().getUidNumber().get(0) : StringUtils.EMPTY;
				final LegalForm legalForm = CollectionsUtils.getLastElement(party.getLegalForms());
				final TaxSystem rfVD = party.getTaxSystemsVD().isEmpty() ? null : CollectionsUtils.getLastElement(party.getTaxSystemsVD());
				final TaxSystem rfCH = party.getTaxSystemsCH().isEmpty() ? null : CollectionsUtils.getLastElement(party.getTaxSystemsCH());
				final List<? extends DateRange> refDebutFin;
				if (legalForm.getType() == ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.GENERAL_PARTNERSHIP || legalForm.getType() == ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.LIMITED_PARTNERSHIP) {
					// on va voir les fors
					refDebutFin = party.getMainTaxResidences();
				}
				else {
					// on va voir l'assujettissement
					refDebutFin = party.getTaxLiabilities();
				}
				final RegDate dateDebut;
				final RegDate dateFin;
				if (refDebutFin.isEmpty()) {
					dateDebut = null;
					dateFin = null;
				}
				else {
					dateDebut = DataHelper.xmlToCore(refDebutFin.get(0).getDateFrom());
					dateFin = DataHelper.xmlToCore(CollectionsUtils.getLastElement(refDebutFin).getDateTo());
				}

				final OutputData out = new OutputData(correspondingInput,
				                                      party.getName(),
				                                      noIDE,
				                                      toLegalFormCode(legalForm.getType()),
				                                      legalForm.getLabel(),
				                                      toLegalFormCategoryCode(legalForm.getLegalFormCategory()),
				                                      rfVD != null ? rfVD.getType() : StringUtils.EMPTY,
				                                      StringUtils.trimToEmpty(regimesFiscaux.get(rfVD != null ? rfVD.getType() : null)),
				                                      rfCH != null ? rfCH.getType() : StringUtils.EMPTY,
				                                      StringUtils.trimToEmpty(regimesFiscaux.get(rfCH != null ? rfCH.getType() : null)),
				                                      dateDebut,
				                                      dateFin);
				output.add(out);
			}
			return output;
		}
	}

	private static String toLegalFormCode(ch.vd.unireg.xml.party.taxpayer.v5.LegalForm lf) {
		if (lf == null) {
			return StringUtils.EMPTY;
		}
		switch (lf) {
		case ASSOCIATION:
			return FormeLegale.N_0109_ASSOCIATION.getCode();
		case BRANCH_OF_SWISS_COMPANY:
			return FormeLegale.N_0151_SUCCURSALE_SUISSE_AU_RC.getCode();
		case CANTONAL_ADMINISTRATION:
			return FormeLegale.N_0221_ADMINISTRATION_CANTON.getCode();
		case CANTONAL_CORPORATION:
			return FormeLegale.N_0231_ENTREPRISE_CANTON.getCode();
		case CLOSED_END_INVESTMENT_TRUST:
			return FormeLegale.N_0116_SOCIETE_INVESTISSEMENT_CAPITAL_FIXE.getCode();
		case COOPERATIVE_SOCIETY:
			return FormeLegale.N_0108_SOCIETE_COOPERATIVE.getCode();
		case DISTRICT_ADMINISTRATION:
			return FormeLegale.N_0222_ADMINISTRATION_DISTRICT.getCode();
		case DISTRICT_CORPORATION:
			return FormeLegale.N_0232_ENTREPRISE_DISTRICT.getCode();
		case FEDERAL_ADMINISTRATION:
			return FormeLegale.N_0220_ADMINISTRATION_CONFEDERATION.getCode();
		case FEDERAL_CORPORATION:
			return FormeLegale.N_0230_ENTREPRISE_CONFEDERATION.getCode();
		case FOREIGN_CORPORATION:
			return FormeLegale.N_0441_ENTREPRISE_ETRANGERE.getCode();
		case FOREIGN_STATUTORY_ADMINISTRATION:
			return FormeLegale.N_0328_ADMINISTRATION_PUBLIQUE_ETRANGERE.getCode();
		case FOREIGN_STATUTORY_CORPORATION:
			return FormeLegale.N_0327_ENTREPRISE_PUBLIQUE_ETRANGERE.getCode();
		case FOUNDATION:
			return FormeLegale.N_0110_FONDATION.getCode();
		case GENERAL_PARTNERSHIP:
			return FormeLegale.N_0103_SOCIETE_NOM_COLLECTIF.getCode();
		case INTERNATIONAL_ORGANIZATION:
			return FormeLegale.N_0329_ORGANISATION_INTERNATIONALE.getCode();
		case JOINT_POSSESSION:
			return FormeLegale.N_0119_CHEF_INDIVISION.getCode();
		case LIMITED_COMPANY:
			return FormeLegale.N_0106_SOCIETE_ANONYME.getCode();
		case LIMITED_JOINT_STOCK_PARTNERSHIP:
			return FormeLegale.N_0105_SOCIETE_EN_COMMANDITE_PAR_ACTIONS.getCode();
		case LIMITED_LIABILITY_COMPANY:
			return FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE.getCode();
		case LIMITED_PARTNERSHIP:
			return FormeLegale.N_0104_SOCIETE_EN_COMMANDITE.getCode();
		case LIMITED_PARTNERSHIP_FOR_COLLECTIVE_INVESTMENTS:
			return FormeLegale.N_0114_SOCIETE_EN_COMMANDITE_POUR_PLACEMENTS_CAPITAUX.getCode();
		case MUNICIPALITY_ADMINISTRATION:
			return FormeLegale.N_0223_ADMINISTRATION_COMMUNE.getCode();
		case MUNICIPALITY_CORPORATION:
			return FormeLegale.N_0233_ENTREPRISE_COMMUNE.getCode();
		case NON_COMMERCIAL_PROXY:
			return FormeLegale.N_0118_PROCURATIONS_NON_COMMERCIALES.getCode();
		case OPEN_ENDED_INVESTMENT_TRUST:
			return FormeLegale.N_0115_SOCIETE_INVESTISSEMENT_CAPITAL_VARIABLE.getCode();
		case OTHER:
			return FormeLegale.N_0113_FORME_JURIDIQUE_PARTICULIERE.getCode();
		case REGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY:
			return FormeLegale.N_0111_FILIALE_ETRANGERE_AU_RC.getCode();
		case SIMPLE_COMPANY:
			return FormeLegale.N_0302_SOCIETE_SIMPLE.getCode();
		case SOLE_PROPRIETORSHIP:
			return FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE.getCode();
		case STATUTORY_ADMINISTRATION:
			return FormeLegale.N_0224_CORPORATION_DE_DROIT_PUBLIC_ADMINISTRATION.getCode();
		case STATUTORY_CORPORATION:
			return FormeLegale.N_0234_CORPORATION_DE_DROIT_PUBLIC_ENTREPRISE.getCode();
		case STATUTORY_INSTITUTE:
			return FormeLegale.N_0117_INSTITUT_DE_DROIT_PUBLIC.getCode();
		case UNREGISTERED_BRANCH_OF_FOREIGN_BASED_COMPANY:
			return FormeLegale.N_0312_FILIALE_ETRANGERE_NON_AU_RC.getCode();
		default:
			throw new IllegalArgumentException("Type de forme juridique non-supporté : " + lf);
		}
	}

	private static String toLegalFormCategoryCode(LegalFormCategory category) {
		if (category == null) {
			return StringUtils.EMPTY;
		}
		switch (category) {
		case ASSOCIATION_FOUNDATION:
			return CategorieEntreprise.APM.name();
		case CAPITAL_COMPANY:
			return CategorieEntreprise.PM.name();
		case OTHER:
			return CategorieEntreprise.AUTRE.name();
		case SOLE_OWNERSHIP_COMPANY:
			return CategorieEntreprise.SP.name();
		default:
			throw new IllegalArgumentException("Type de catégorie d'entreprise non-supporté : " + category);
		}
	}
}
