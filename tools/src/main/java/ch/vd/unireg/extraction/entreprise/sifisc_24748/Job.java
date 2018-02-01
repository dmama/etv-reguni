package ch.vd.unireg.extraction.entreprise.sifisc_24748;

import javax.xml.datatype.XMLGregorianCalendar;
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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import ch.vd.evd0007.v1.Country;
import ch.vd.evd0007.v1.ValidityDate;
import ch.vd.evd0012.v1.CommuneFiscale;
import ch.vd.fidor.xml.regimefiscal.v2.RegimeFiscal;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.CommuneImpl;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.PaysImpl;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscalFidor;
import ch.vd.unireg.ws.parties.v7.Entry;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.wsclient.WebClientPool;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.corporation.v5.TaxSystem;
import ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason;
import ch.vd.unireg.xml.party.taxresidence.v4.OrdinaryResident;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxationAuthorityType;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.unireg.common.BatchIterator;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.StandardBatchIterator;
import ch.vd.unireg.utils.WebServiceV7Helper;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClient;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClientImpl;
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
		final List<Integer> input = loadIdsContribuables();

		// récupération des données depuis fidor
		final Map<String, TypeRegimeFiscal> regimesFiscaux = fetchRegimesFiscaux();
		final Map<Integer, Commune> communes = fetchCommunes();
		final Map<Integer, Pays> pays = fetchPays();

		// découpage en petit (?) lots
		final BatchIterator<Integer> iteratorLot = new StandardBatchIterator<>(input, TAILLE_LOT);

		// mise en place de l'infrastructure d'exécuteurs
		final ExecutorService execService = Executors.newFixedThreadPool(NB_THREADS);
		try {
			final ExecutorCompletionService<List<OutputData>> completionService = new ExecutorCompletionService<>(execService);

			// lancement des récupérations d'information
			int nbLotsEnvoyes = 0;
			while (iteratorLot.hasNext()) {
				completionService.submit(new DataExtractor(iteratorLot.next(), regimesFiscaux, communes, pays));
				++ nbLotsEnvoyes;
			}

			// ça y est, on a fini de semer, il va bientôt falloir récolter...
			execService.shutdown();

			// préparation du fichier de sortie
			try (OutputStream os = new FileOutputStream(outputFilename);
			     Writer w = new OutputStreamWriter(os, outputCharset);
			     BufferedWriter bw = new BufferedWriter(w)) {

				bw.write("NO_CTB;NO_IDE;RAISON_SOCIALE;REGIME_FISCAL_VD;LIBELLE_REGIME_FISCAL_VD;NO_OFS_FOR_PRINCIPAL;LIEU_FOR_PRINCIPAL;NOM_FOR_PRINCIPAL;DEBUT_ASSUJETISSEMENT;MOTIF_DEBUT_ASSUJETTISSEMENT;FIN_ASSUJETTISSEMENT;MOTIF_FIN_ASSUJETTISSEMENT;TYPE_ASSUJETTISSEMENT");
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
		bw.write(String.format("%d;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s",
		                       data.noCtb,
		                       data.noIDE,
		                       clean(data.raisonSociale),
		                       quote(data.codeRegimeFiscalVD),
		                       quote(data.libelleRegimeFiscalVD),
		                       data.noOfsForPrincipal,
		                       toString(data.tafForPrincipal),
		                       clean(data.nomForPrincipal),
		                       RegDateHelper.dateToDashString(data.debutAssujettissement),
		                       toString(data.motifDebutAssujettissement),
		                       RegDateHelper.dateToDashString(data.finAssujettissement),
		                       toString(data.motifFinAssujettissement),
		                       data.illimite ? "Illimité" : "Limité"));
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

	private static List<Integer> loadIdsContribuables() throws IOException {
		try (InputStream is = Job.class.getResourceAsStream(inputDataFilename);
		     Reader r = new InputStreamReader(is);
		     BufferedReader br = new BufferedReader(r)) {

			final List<Integer> data = new LinkedList<>();
			String line = br.readLine();
			while (line != null) {
				// on extrait déjà ici les lignes de commentaires
				if (!line.startsWith("#")) {
					try {
						final Integer id = Integer.parseInt(line);
						data.add(id);
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
	private static Map<String, TypeRegimeFiscal> fetchRegimesFiscaux() {
		final FidorClient fidorClient = buildFidorClient();
		final List<RegimeFiscal> regimes = fidorClient.getRegimesFiscaux();
		final Map<String, TypeRegimeFiscal> map = new HashMap<>(regimes.size());
		for (RegimeFiscal regime : regimes) {
			map.put(regime.getCode(), TypeRegimeFiscalFidor.get(regime));
		}
		return map;
	}

	private static Map<Integer, Commune> fetchCommunes() {
		final FidorClient fidorClient = buildFidorClient();
		final List<CommuneFiscale> communes = fidorClient.getToutesLesCommunes();
		return communes.stream()
				.sorted(Comparator.comparing(CommuneFiscale::getDateDebutValidite,
				                             Comparator.comparing(XMLGregorianCalendar::getYear)
						                             .thenComparing(XMLGregorianCalendar::getMonth)
						                             .thenComparing(XMLGregorianCalendar::getDay)))
				.collect(Collectors.toMap(CommuneFiscale::getNumeroOfs,
				                          CommuneImpl::get,
				                          (c1, c2) -> c2));
	}

	private static Map<Integer, Pays> fetchPays() {
		final FidorClient fidorClient = buildFidorClient();
		final List<Country> countries = fidorClient.getTousLesPays();
		return countries.stream()
				.sorted(Comparator.comparing(c -> Optional.of(c).map(Country::getValidityDates).map(ValidityDate::getAdmissionDate).orElse(null),
				                             Comparator.nullsFirst(Comparator.comparing(XMLGregorianCalendar::getYear)
						                                                   .thenComparing(XMLGregorianCalendar::getMonth)
						                                                   .thenComparing(XMLGregorianCalendar::getDay))))
				.collect(Collectors.toMap(c -> c.getCountry().getId(),
				                          PaysImpl::get,
				                          (c1, c2) -> c2));
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

	private static String toString(TaxationAuthorityType taf) {
		if (taf == null) {
			return StringUtils.EMPTY;
		}
		switch (taf) {
		case FOREIGN_COUNTRY:
			return "HS";
		case OTHER_CANTON_MUNICIPALITY:
			return "HC";
		case VAUD_MUNICIPALITY:
			return "VD";
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnu : " + taf);
		}
	}

	private static String toString(LiabilityChangeReason motif) {
		if (motif == null) {
			return StringUtils.EMPTY;
		}
		switch (motif) {
		case BANKRUPTCY:
			return "Faillite";
		case C_PERMIT_SWISS:
			return "Obtention de permis C ou de nationalité Suisse";
		case CANCELLATION:
			return "Désactivation";
		case CHANGE_OF_TAXATION_METHOD:
			return "Changement de mode d'imposition";
		case CORPORATION_MERGER:
			return "Fusion d'entreprises";
		case DEPARTURE_TO_FOREIGN_COUNTRY:
			return "Départ hors-Suisse";
		case DEPARTURE_TO_OTHER_CANTON:
			return "Départ hors-canton";
		case END_ACTIVITY_MERGER_BANKRUPTCY:
			return "Fin d'activité / fusion / faillite";
		case END_COMMERCIAL_EXPLOITATION:
			return "Fin d'exploitation";
		case END_DIPLOMATIC_ACTVITY:
			return "Fin d'activité diplomatique";
		case END_WITHHOLDING_ACTIVITY:
			return "Fin de prestation IS";
		case MAJORITY:
			return "Majorité";
		case MARRIAGE_PARTNERSHIP_END_OF_SEPARATION:
			return "Mariage / Partenariat / Réconciliation";
		case MERGE_OF_MUNICIPALITIES:
			return "Fusion de communes";
		case MOVE_HEADQUARTERS:
			return "Déménagement de siège";
		case MOVE_IN_FROM_FOREIGN_COUNTRY:
			return "Arrivée hors-Suisse";
		case MOVE_IN_FROM_OTHER_CANTON:
			return "Arrivée hors-canton";
		case MOVE_VD:
			return "Déménagement";
		case PURCHASE_REAL_ESTATE:
			return "Achat immobilier";
		case REACTIVATION:
			return "Ré-activation";
		case SALE_REAL_ESTATE:
			return "Vente immobilière";
		case SEASONAL_JOURNEY:
			return "Début / fin d'activité saisonnière";
		case SEPARATION_DIVORCE_PARTNERSHIP_ABOLITION:
			return "Séparation / divorce / dissolution de partenariat";
		case START_COMMERCIAL_EXPLOITATION:
			return "Début d'exploitation";
		case START_DIPLOMATIC_ACTVITY:
			return "Début d'activité dimplomatique";
		case START_WITHHOLDING_ACTIVITY:
			return "Début de prestation IS";
		case UNDETERMINED:
			return "Indéterminé";
		case WIDOWHOOD_DEATH:
			return "Veuvage / décès";
		default:
			throw new IllegalArgumentException("Motif de début / fin d'assujettissement inconnu : " + motif);
		}
	}

	private static boolean isIllimite(TaxLiability assujettissement) {
		return assujettissement instanceof OrdinaryResident;
	}

	/**
	 * Tâche d'extraction de données
	 */
	private static class DataExtractor implements Callable<List<OutputData>> {

		private static final Set<PartyPart> PARTS = EnumSet.of(PartyPart.LEGAL_FORMS,
		                                                       PartyPart.TAX_SYSTEMS,
		                                                       PartyPart.TAX_RESIDENCES,
		                                                       PartyPart.TAX_LIABILITIES);

		private final List<Integer> input;
		private final Map<String, TypeRegimeFiscal> regimesFiscaux;
		private final Map<Integer, Commune> communes;
		private final Map<Integer, Pays> pays;

		public DataExtractor(List<Integer> input,
		                     Map<String, TypeRegimeFiscal> regimesFiscaux,
		                     Map<Integer, Commune> communes,
		                     Map<Integer, Pays> pays) {
			this.input = input;
			this.regimesFiscaux = regimesFiscaux;
			this.communes = communes;
			this.pays = pays;
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
				final String noIDE = FormatNumeroHelper.formatNumIDE(party.getUidNumbers() != null && party.getUidNumbers().getUidNumber() != null && !party.getUidNumbers().getUidNumber().isEmpty() ? party.getUidNumbers().getUidNumber().get(0) : StringUtils.EMPTY);
				final TaxSystem rfVD = party.getTaxSystemsVD().isEmpty() ? null : CollectionsUtils.getLastElement(party.getTaxSystemsVD());

				// on considère les sociétés qui ont eu un assujettissement depuis 2014
				final DateRange rangeUtile = new DateRangeHelper.Range(RegDate.get(2014, 1, 1), null);
				final List<Pair<DateRange, TaxLiability>> assuj = party.getTaxLiabilities().stream()
						.map(a -> Pair.<DateRange, TaxLiability>of(new DateRangeHelper.Range(DataHelper.xmlToCore(a.getDateFrom()), DataHelper.xmlToCore(a.getDateTo())), a))
						.filter(r -> DateRangeHelper.intersect(r.getKey(), rangeUtile))
						.collect(Collectors.toList());
				if (assuj.isEmpty()) {
					continue;
				}

				// début et fin d'assujettissement
				final Pair<DateRange, TaxLiability> debutAssuj = assuj.get(0);
				final Pair<DateRange, TaxLiability> finAssuj = CollectionsUtils.getLastElement(assuj);
				final RegDate dateDebutAssuj = debutAssuj.getLeft().getDateDebut();
				final RegDate dateFinAssuj = finAssuj.getLeft().getDateFin();
				final LiabilityChangeReason motifDebutAssuj = debutAssuj.getRight().getStartReason();
				final LiabilityChangeReason motifFinAssuj = finAssuj.getRight().getEndReason();

				// on prend les dernières valeurs avant la date de fin d'assujetissement
				final List<TaxResidence> taxResidences = party.getMainTaxResidences();
				final Optional<TaxResidence> last = taxResidences.stream()
						.filter(residence -> residence.getCancellationDate() == null)
						.filter(residence -> RegDateHelper.isBeforeOrEqual(DataHelper.xmlToCore(residence.getDateFrom()), dateFinAssuj, NullDateBehavior.LATEST))
						.max(Comparator.comparing(residence -> DataHelper.xmlToCore(residence.getDateFrom())));
				final Function<TaxResidence, String> toName = taxResidence -> {
					switch (taxResidence.getTaxationAuthorityType()) {
					case FOREIGN_COUNTRY: {
						final Pays pays = this.pays.get(taxResidence.getTaxationAuthorityFSOId());
						return pays != null ? pays.getNomCourt() : "Pays inconnu";
					}
					case OTHER_CANTON_MUNICIPALITY:
					case VAUD_MUNICIPALITY: {
						final Commune commune = communes.get(taxResidence.getTaxationAuthorityFSOId());
						return commune != null ? commune.getNomOfficielAvecCanton() : "Commune suisse inconnue";
					}
					default:
						throw new IllegalArgumentException("Type d'autorité fiscale inconnu : " + taxResidence.getTaxationAuthorityType());
					}
				};

				final String codeRegimeFiscal = rfVD != null ? rfVD.getType() : null;
				final OutputData out = new OutputData(party.getNumber(),
				                                      party.getName(),
				                                      noIDE,
				                                      StringUtils.trimToEmpty(codeRegimeFiscal),
				                                      Optional.ofNullable(codeRegimeFiscal).map(regimesFiscaux::get).map(TypeRegimeFiscal::getLibelle).orElse(StringUtils.EMPTY),
				                                      dateDebutAssuj,
				                                      motifDebutAssuj,
				                                      dateFinAssuj,
				                                      motifFinAssuj,
				                                      isIllimite(finAssuj.getRight()),
				                                      last.map(TaxResidence::getTaxationAuthorityType).orElse(null),
				                                      last.map(TaxResidence::getTaxationAuthorityFSOId).orElse(null),
				                                      last.map(toName).orElse(null));
				output.add(out);
			}
			return output;
		}
	}
}
