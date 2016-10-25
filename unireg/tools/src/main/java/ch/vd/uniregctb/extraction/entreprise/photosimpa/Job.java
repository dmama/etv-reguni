package ch.vd.uniregctb.extraction.entreprise.photosimpa;

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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0007.v1.Country;
import ch.vd.evd0012.v1.CommuneFiscale;
import ch.vd.fidor.xml.regimefiscal.v1.RegimeFiscal;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.ws.parties.v7.Entry;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.xml.party.address.v3.Address;
import ch.vd.unireg.xml.party.address.v3.AddressInformation;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.corporation.v5.LegalForm;
import ch.vd.unireg.xml.party.corporation.v5.TaxSystem;
import ch.vd.unireg.xml.party.taxresidence.v4.SimplifiedTaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxResidence;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.uniregctb.common.BatchIterator;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.StandardBatchIterator;
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
	private static final int NB_THREADS = 4;

	public static void main(String[] args) throws Exception {

		// lecture des données d'entrée
		final List<Integer> input = loadInputData();

		// récupération des régimes fiscaux depuis fidor
		final Map<String, String> codesRegimesFiscaux = fetchRegimesFiscaux();

		// découpage en petit (?) lots
		final BatchIterator<Integer> iteratorLot = new StandardBatchIterator<>(input, TAILLE_LOT);

		// client fidor avec cache
		final FidorClient fidorClient = buildCache(FidorClient.class, buildFidorClient());

		// mise en place de l'infrastructure d'exécuteurs
		final ExecutorService execService = Executors.newFixedThreadPool(NB_THREADS);
		try {
			final ExecutorCompletionService<List<OutputData>> completionService = new ExecutorCompletionService<>(execService);

			// lancement des récupérations d'information
			int nbLotsEnvoyes = 0;
			while (iteratorLot.hasNext()) {
				completionService.submit(new DataExtractor(iteratorLot.next(), codesRegimesFiscaux, fidorClient));
				++ nbLotsEnvoyes;
			}

			// ça y est, on a fini de semer, il va bientôt falloir récolter...
			execService.shutdown();

			// préparation du fichier de sortie
			try (OutputStream os = new FileOutputStream(outputFilename);
			     Writer w = new OutputStreamWriter(os, outputCharset);
			     BufferedWriter bw = new BufferedWriter(w)) {

				bw.write("NO_CTB;NO_IDE;RAISON_SOCIALE;FOR_PRINCIPAL;NPA_NO_POLICE;FORME_JURIDIQUE;DEPUIS_LE;REGIME_FISCAL_CH;REGIME_FISCAL_VD;DEBUT_ICC;FIN_ICC;");
				bw.newLine();

				// et maintenant on traite les résulats reçus
				for (int i = 0; i < nbLotsEnvoyes; ++i) {
					while (true) {
						final Future<List<OutputData>> future = completionService.poll(10, TimeUnit.SECONDS);
						if (future != null) {
							final List<OutputData> taskResult = future.get();
							for (OutputData data : taskResult) {
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

	private static void dump(BufferedWriter bw, OutputData data) throws IOException {
		final String pairSeparator = "-";
		bw.write(String.format("%d;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;",
		                       data.noEntreprise,
		                       FormatNumeroHelper.formatNumIDE(data.noIDE),
		                       clean(data.raisonSociale),
		                       quote(pairToString(data.noOfsNomDernierForPrincipal, Objects::toString, Function.identity(), pairSeparator)),
		                       quote(pairToString(data.npaNoPolice, Objects::toString, StringUtils::trimToEmpty, pairSeparator)),
		                       quote(pairToString(data.codeLibelleFormeJuridique, Function.identity(), Function.identity(), pairSeparator)),
		                       RegDateHelper.dateToDisplayString(data.depuisLe),
		                       quote(pairToString(data.codeLibelleRegimeFiscalCH, Function.identity(), Function.identity(), pairSeparator)),
		                       quote(pairToString(data.codeLibelleRegimeFiscalVD, Function.identity(), Function.identity(), pairSeparator)),
		                       RegDateHelper.dateToDisplayString(data.debutAssujettissementICC),
		                       RegDateHelper.dateToDisplayString(data.finAssujettissementICC)));
		bw.newLine();
	}

	private static <K, V> String pairToString(Pair<K, V> pair,
	                                          Function<? super K, String> keyDumper,
	                                          Function<? super V, String> valueDumper,
	                                          String separator) {
		if (pair == null) {
			return StringUtils.EMPTY;
		}
		return String.format("%s%s%s", keyDumper.apply(pair.getKey()), separator, valueDumper.apply(pair.getValue()));
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

	private static List<Integer> loadInputData() throws IOException {
		try (InputStream is = Job.class.getResourceAsStream(inputDataFilename);
		     Reader r = new InputStreamReader(is);
		     BufferedReader br = new BufferedReader(r)) {

			final List<Integer> data = new LinkedList<>();
			String line = br.readLine();
			while (line != null) {
				// on extrait déjà ici les lignes de commentaires
				if (!line.startsWith("#")) {
					try {
						final Integer parsed = Integer.parseInt(line);
						data.add(parsed);
					}
					catch (NumberFormatException e) {
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
		final FidorClientImpl client = new FidorClientImpl();
		client.setServiceUrl(urlWebServiceFidor);
		client.setUsername(userWebServiceFidor);
		client.setPassword(pwdWebServiceFidor);
		return client;
	}

	private static <T> T buildCache(Class<T> clazz, T target) {
		return ServiceCache.of(clazz, target);
	}

	/**
	 * Tâche d'extraction de données
	 */
	private static class DataExtractor implements Callable<List<OutputData>> {

		private static final Set<PartyPart> PARTS = EnumSet.of(PartyPart.LEGAL_FORMS,
		                                                       PartyPart.ADDRESSES,
		                                                       PartyPart.TAX_SYSTEMS,
		                                                       PartyPart.TAX_RESIDENCES,
		                                                       PartyPart.SIMPLIFIED_TAX_LIABILITIES);

		private final List<Integer> input;
		private final Map<String, String> regimesFiscaux;
		private final FidorClient fidorClient;

		public DataExtractor(List<Integer> input,
		                     Map<String, String> regimesFiscaux,
		                     FidorClient fidorClient) {
			this.input = input;
			this.regimesFiscaux = regimesFiscaux;
			this.fidorClient = fidorClient;
		}

		@Override
		public List<OutputData> call() throws Exception {
			final Parties parties = WebServiceV7Helper.getParties(urlWebServiceUnireg, userWebServiceUnireg, pwdWebServiceUnireg, userId, oid, input, PARTS);
			final List<OutputData> output = new ArrayList<>(input.size());
			for (Entry partyEntry : parties.getEntries()) {
				if (partyEntry.getError() != null) {
					System.err.println(String.format("%d;\"%s\"",
					                                 partyEntry.getPartyNo(),
					                                 partyEntry.getError().getErrorMessage().replaceAll("\n", " ")));
					continue;
				}

				final Corporation party = (Corporation) partyEntry.getParty();
				final String noIDE = party.getUidNumbers() != null && party.getUidNumbers().getUidNumber() != null && !party.getUidNumbers().getUidNumber().isEmpty() ? party.getUidNumbers().getUidNumber().get(0) : StringUtils.EMPTY;

				final Pair<Integer, String> forPrincipal;
				final Pair<Integer, String> npaNoPolice;
				final Pair<String, String> formeJuridique;
				final Pair<String, String> regimeFiscalVD;
				final Pair<String, String> regimeFiscalCH;
				final DateRange icc;

				// for principal
				{
					final TaxResidence ffp = getLastFromFiltered(party.getMainTaxResidences(), elt -> elt.getCancellationDate() == null);
					if (ffp == null) {
						forPrincipal = null;
					}
					else {
						final int noOfs = ffp.getTaxationAuthorityFSOId();
						final RegDate dateFin = DataHelper.xmlToCore(ffp.getDateTo());
						final String nom;
						switch (ffp.getTaxationAuthorityType()) {
						case OTHER_CANTON_MUNICIPALITY:
						case VAUD_MUNICIPALITY:
							nom = getNomCommune(noOfs, dateFin);
							break;
						case FOREIGN_COUNTRY:
							nom = getNomPays(noOfs, dateFin);
							break;
						default:
							throw new IllegalArgumentException("Unsupported type of taxation authority : " + ffp.getTaxationAuthorityType());
						}
						forPrincipal = Pair.of(noOfs, nom);
					}
				}

				// NPA et no police
				{
					final Address adresse = getLastFromFiltered(party.getMailAddresses(), elt -> !elt.isFake());
					if (adresse == null || adresse.getPostAddress() == null || adresse.getPostAddress().getDestination() == null) {
						npaNoPolice = null;
					}
					else {
						final AddressInformation destination = adresse.getPostAddress().getDestination();
						final Integer npa = toInteger(destination.getSwissZipCode());
						final String noPolice = destination.getHouseNumber();
						if (npa != null || noPolice != null) {
							npaNoPolice = Pair.of(npa, noPolice);
						}
						else {
							npaNoPolice = null;
						}
					}
				}

				// forme juridique
				{
					final LegalForm legalForm = CollectionsUtils.getLastElement(party.getLegalForms());
					formeJuridique = Pair.of(toLegalFormCode(legalForm.getType()), legalForm.getLabel());
				}

				// régime fiscal CH
				{
					final TaxSystem rfCH = party.getTaxSystemsCH().isEmpty() ? null : CollectionsUtils.getLastElement(party.getTaxSystemsCH());
					if (rfCH == null) {
						regimeFiscalCH = null;
					}
					else {
						regimeFiscalCH = Pair.of(rfCH.getType(), regimesFiscaux.getOrDefault(rfCH.getType(), "???"));
					}
				}

				// régime fiscal VD
				{
					final TaxSystem rfVD = party.getTaxSystemsVD().isEmpty() ? null : CollectionsUtils.getLastElement(party.getTaxSystemsVD());
					if (rfVD == null) {
						regimeFiscalVD = null;
					}
					else {
						regimeFiscalVD = Pair.of(rfVD.getType(), regimesFiscaux.getOrDefault(rfVD.getType(), "???"));
					}
				}

				// début et fin de l'assujettissement ICC
				{
					final List<SimplifiedTaxLiability> assujettissements = party.getSimplifiedTaxLiabilityVD();
					if (assujettissements == null) {
						icc = null;
					}
					else {
						final List<DateRange> ranges = DateRangeHelper.merge(assujettissements.stream()
								                                                     .map(assuj -> new DateRangeHelper.Range(DataHelper.xmlToCore(assuj.getDateFrom()), DataHelper.xmlToCore(assuj.getDateTo())))
								                                                     .collect(Collectors.toList()));
						icc = ranges == null || ranges.isEmpty() ? null : CollectionsUtils.getLastElement(ranges);
					}

				}

				final OutputData out = new OutputData(party.getNumber(),
				                                      noIDE,
				                                      party.getName(),
				                                      forPrincipal,
				                                      npaNoPolice,
				                                      formeJuridique,
				                                      null,
				                                      regimeFiscalCH,
				                                      regimeFiscalVD,
				                                      icc == null ? null : icc.getDateDebut(),
				                                      icc == null ? null : icc.getDateFin());
				output.add(out);
			}
			return output;
		}

		private String getNomCommune(int noOfs, RegDate date) {
			final CommuneFiscale commune = fidorClient.getCommuneParNoOFS(noOfs, date);
			return commune == null ? "???" : commune.getNomOfficiel();
		}

		private String getNomPays(int noOfs, RegDate date) {
			final Country pays = fidorClient.getPaysDetail(noOfs, date);
			return pays == null ? "???" : pays.getCountry().getShortNameFr();
		}
	}

	@Nullable
	private static <T> T getLastFromFiltered(List<T> src, Predicate<? super T> filter) {
		if (src == null || src.isEmpty()) {
			return null;
		}
		return src.stream()
				.filter(filter)
				.reduce((elt1, elt2) -> elt2)
				.orElse(null);
	}

	@Nullable
	private static Integer toInteger(@Nullable Long value) {
		if (value == null) {
			return null;
		}
		if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Value too big for an int... " + value);
		}
		return value.intValue();
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

}
