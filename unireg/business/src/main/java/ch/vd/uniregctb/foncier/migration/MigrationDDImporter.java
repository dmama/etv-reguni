package ch.vd.uniregctb.foncier.migration;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import au.com.bytecode.opencsv.CSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.MultipleSwitch;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DonneesLoiLogement;
import ch.vd.uniregctb.foncier.DonneesUtilisation;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.tache.TacheSynchronizerInterceptor;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.validation.ValidationInterceptor;

public class MigrationDDImporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationDDImporter.class);

	private static final int BATCH_SIZE = 100;
	private static final Pattern PERCENT_PATTERN = Pattern.compile("([0-9]{1,3})\\.?([0-9])?([0-9])?");
	private static final SimpleDateFormat DATE_FORMAT_SLASH = new SimpleDateFormat("dd/MM/yyyy");
	private static final SimpleDateFormat DATE_FORMAT_DOT = new SimpleDateFormat("dd.MM.yyyy");

	private final TiersDAO tiersDAO;
	private final HibernateTemplate hibernateTemplate;
	private final ServiceInfrastructureService infraService;
	private final ImmeubleRFDAO immeubleRFDAO;
	private final GlobalTiersIndexer tiersIndexer;
	private final ValidationInterceptor validationInterceptor;
	private final TacheSynchronizerInterceptor tacheSynchronizerInterceptor;
	private final PlatformTransactionManager transactionManager;

	public MigrationDDImporter(TiersDAO tiersDAO,
	                           HibernateTemplate hibernateTemplate,
	                           ServiceInfrastructureService infraService,
	                           ImmeubleRFDAO immeubleRFDAO,
	                           GlobalTiersIndexer tiersIndexer,
	                           ValidationInterceptor validationInterceptor,
	                           TacheSynchronizerInterceptor tacheSynchronizerInterceptor,
	                           PlatformTransactionManager transactionManager) {
		this.tiersDAO = tiersDAO;
		this.hibernateTemplate = hibernateTemplate;
		this.infraService = infraService;
		this.immeubleRFDAO = immeubleRFDAO;
		this.tiersIndexer = tiersIndexer;
		this.validationInterceptor = validationInterceptor;
		this.tacheSynchronizerInterceptor = tacheSynchronizerInterceptor;
		this.transactionManager = transactionManager;
	}

	public MigrationDDImporterResults loadCSV(@NotNull InputStream csvStream, @NotNull String encoding, int nbThreads, @Nullable StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final MigrationDDImporterResults rapportFinal = new MigrationDDImporterResults(nbThreads);

		// Ouverture du flux CSV
		status.setMessage("Ouverture du fichier...");
		final Scanner csvIterator = new Scanner(csvStream, encoding);
		csvIterator.useDelimiter(Pattern.compile("[\r]?\n"));

		if (!csvIterator.hasNext()) {
			throw new IllegalArgumentException("Le fichier est vide !");
		}

		// Import de toutes les lignes du flux CSV
		try {
			processAllLines(csvIterator, nbThreads, rapportFinal, status);
		}
		catch (IOException | ParseException e) {
			throw new RuntimeException(e);
		}

		// Logging
		if (status.interrupted()) {
			status.setMessage("L'import des dégrèvements a été interrompu."
					                  + " Nombre de dégrèvements importés au moment de l'interruption = " + rapportFinal.getNbLignes());
			rapportFinal.setInterrompu(true);
		}
		else {
			status.setMessage("L'import des immeubles  est terminé."
					                  + " Nombre de dégrèvements importés = " + rapportFinal.getNbLignes() + ". Nombre d'erreurs = " + rapportFinal.getErreurs().size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	private void processAllLines(@NotNull Scanner csvIterator, int nbThreads, @NotNull MigrationDDImporterResults rapport, @NotNull StatusManager status) throws IOException, ParseException {

		int index = 1;

		// on saute les lignes d'entête
		while (csvIterator.hasNext()) {
			final String line = csvIterator.next();
			++index;
			if (line.startsWith("----")) {
				break;
			}
		}

		// on lit d'abord tout le fichier
		final List<MigrationDD> input = new LinkedList<>();
		while (csvIterator.hasNext()) {
			// on parse la ligne
			final MigrationDD dd;
			try {
				dd = parseLine(csvIterator.next());
				input.add(dd);
			}
			catch (RuntimeException e) {
				rapport.addLineEnErreur(index, e.getMessage());
			}
			finally {
				++index;
			}

			final int lineProcessed = rapport.incNbLignes();
			if (lineProcessed % 100 == 0) {
				status.setMessage("Lecture de la ligne n°" + lineProcessed + "...");
			}

			if (status.interrupted()) {
				break;
			}
		}

		// maintenant, on va tout regrouper par couple {ctb / immeuble}
		final Map<MigrationDDKey, List<MigrationDD>> byCtbImmeuble = input.stream()
				.collect(Collectors.toMap(MigrationDDKey::new,
				                          Collections::singletonList,
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

		// par couple {ctb / immeuble}, on ne garde qu'une seule ligne par usage (celle dont la PF est la plus élevée, et en cas d'égalité celle dont la date d'envoi est la plus récente)
		final Map<MigrationDDKey, ValeurDegrevement> byCtbImmeubleFiltered = new HashMap<>(byCtbImmeuble.size());
		for (Map.Entry<MigrationDDKey, List<MigrationDD>> entry : byCtbImmeuble.entrySet()) {

			// quelle est la PF la plus élevée ?
			final int pf = entry.getValue().stream()
					.mapToInt(MigrationDD::getAnneeFiscale)
					.max()
					.getAsInt();

			// on ne garde que pour cette PF
			final Map<TypeUsage, MigrationDD> perUsage = entry.getValue().stream()
					.filter(dd -> {
						if (dd.getAnneeFiscale() != pf) {
							rapport.addLigneIgnoree(dd, "Une donnée pour une PF plus récente (" + pf + ") est présente.");
							return false;
						}
						return true;
					})
					.collect(Collectors.toMap(dd -> dd.getUsage().getTypeUsage(),
					                          Function.identity(),
					                          (dd1, dd2) -> {
						                          final List<MigrationDD> sorted = Stream.of(dd1, dd2)
								                          .sorted(Comparator.comparing(MigrationDD::getDateEnvoi))
								                          .collect(Collectors.toList());
						                          final MigrationDD elected = sorted.get(1);
						                          rapport.addLigneIgnoree(sorted.get(0), "Une donnée plus récente (même PF mais date d'envoi plus récente (" + RegDateHelper.dateToDisplayString(elected.getDateEnvoi()) + ")) est présente.");
						                          return elected;
					                          },
					                          () -> new EnumMap<>(TypeUsage.class)));

			// et finalement on ne conserve que les usages
			final Map<TypeUsage, MigrationDDUsage> usages = perUsage.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, mapEntry -> mapEntry.getValue().getUsage()));

			byCtbImmeubleFiltered.put(entry.getKey(), new ValeurDegrevement(pf, usages));
		}

    	// pour des raisons d'optimisation, on va chercher toutes les communes et on va les classer par nom "canonique"
		final Map<String, Commune> mapCommunes = infraService.getCommunes().stream()
				.collect(Collectors.toMap(commune -> canonizeName(commune.getNomOfficiel()),
				                          Function.identity(),
				                          (c1, c2) -> Stream.of(c1, c2).max(Comparator.comparing(Commune::getDateDebutValidite, NullDateBehavior.EARLIEST::compare)).get()));

		// on va devoir traiter les dossiers contribuable par contribuable
		final Map<Long, List<Map.Entry<MigrationDDKey, ValeurDegrevement>>> mapParContribuable = byCtbImmeubleFiltered.entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getKey().numeroEntreprise,
				                          Collections::singletonList,
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

		// on désactive la validation automatique des données sauvées car la validation
		// des entreprises est très coûteuse et - surtout - les demandes de dégrèvements
		// migrées de SIMPA-PM n'influence pas sur l'état valide ou non des entreprises.
		final MultipleSwitch mainSwitch = new MultipleSwitch(validationInterceptor,
		                                                     tiersIndexer.onTheFlyIndexationSwitch(),
		                                                     tacheSynchronizerInterceptor);

		final SimpleProgressMonitor monitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplate<Map.Entry<Long, List<Map.Entry<MigrationDDKey, ValeurDegrevement>>>> template =
				new ParallelBatchTransactionTemplate<>(new ArrayList<>(mapParContribuable.entrySet()), BATCH_SIZE, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, AuthenticationInterface.INSTANCE);
		template.execute(new BatchCallback<Map.Entry<Long, List<Map.Entry<MigrationDDKey, ValeurDegrevement>>>>() {

			private final ThreadLocal<MigrationDDImporterResults> subRapport = new ThreadLocal<>();
			private final ThreadLocal<Long> contribuable = new ThreadLocal<>();

			@Override
			public void beforeTransaction() {
				subRapport.set(new MigrationDDImporterResults(nbThreads));
				mainSwitch.pushState();
				mainSwitch.setEnabled(false);
			}

			@Override
			public boolean doInTransaction(List<Map.Entry<Long, List<Map.Entry<MigrationDDKey, ValeurDegrevement>>>> batch) throws Exception {
				batch.forEach(batchEntry -> {

					// ensuite, on traite contribuable par contribuable
					contribuable.set(batchEntry.getKey());
					final Entreprise entreprise = getEntreprise(batchEntry.getKey());

					// map des dégrèvements à persister pour cette entreprise par identifiant d'immeuble
					final Map<Long, List<Pair<DegrevementICI, MigrationDDKey>>> degrevementsParImmeuble = batchEntry.getValue().stream()
							.map(dd -> {
								try {
									final DegrevementICI deg = traiterDegrevement(dd, mapCommunes);
									if (deg == null) {
										subRapport.get().addDonneeDegrevementVide(dd);
										return null;
									}
									return Pair.of(deg, dd.getKey());
								}
								catch (Exception e) {
									subRapport.get().addErreur(dd, e.getMessage());
									return null;
								}
							})
							.filter(Objects::nonNull)
							.collect(Collectors.toMap(d -> d.getLeft().getImmeuble().getId(),
							                          Collections::singletonList,
							                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

					// [SIFISC-23259] on ne garde que le dernier... (il peut y en avoir plusieurs quand l'immeuble n'est pas exactement désigné
					// de la même façon à chaque fois, mais que c'est toujours le même - voir les règles dans MigrationParcelle)
					degrevementsParImmeuble.values().stream()
							.map(list -> {
								final List<Pair<DegrevementICI, MigrationDDKey>> sorted = list.stream()
										.sorted((p1, p2) -> NullDateBehavior.EARLIEST.compare(p1.getLeft().getDateDebut(), p2.getLeft().getDateDebut()))
										.collect(Collectors.toList());

								final Pair<DegrevementICI, MigrationDDKey> last = sorted.get(sorted.size() - 1);
								sorted.subList(0, sorted.size() - 1)
										.forEach(pair -> subRapport.get().addDegrevementIgnoreValeurPlusRecente(pair.getRight(), pair.getLeft().getDateDebut(), last.getLeft().getDateDebut()));

								subRapport.get().addDegrevementTraite(last.getLeft(), last.getRight());
								return last.getLeft();
							})
							.map(hibernateTemplate::merge)
							.forEach(entreprise::addAllegementFoncier);
				});
				status.setMessage("Migration des dégrèvements...", monitor.getProgressInPercent());
				return true;
			}

			@Override
			public void afterTransactionCommit() {
				mainSwitch.popState();
				synchronized (rapport) {
					rapport.addAll(subRapport.get());
				}
				subRapport.remove();
				contribuable.remove();
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				mainSwitch.popState();
				if (!willRetry) {
					synchronized (rapport) {
						rapport.addContribuableEnErreur(contribuable.get(), e.getMessage());
					}
				}
				subRapport.remove();
				contribuable.remove();
			}
		}, monitor);
	}

	/**
	 * Migre les dégrèvements ICI
	 * @param degrevement les données du dégrèvement
	 * @return le dégrèvement ICI à persister
	 */
	@Nullable
	private DegrevementICI traiterDegrevement(Map.Entry<MigrationDDKey, ValeurDegrevement> degrevement, Map<String, Commune> mapCommunes) throws ObjectNotFoundException {

		final Entreprise entreprise = determinerEntreprise(degrevement.getKey());
		final ImmeubleRF immeuble = determinerImmeuble(degrevement.getKey(), mapCommunes);

		final DegrevementICI data = new DegrevementICI();
		final Map<TypeUsage, MigrationDDUsage> usages = degrevement.getValue().getUsages();
		data.setContribuable(entreprise);
		data.setImmeuble(immeuble);
		data.setLocation(extractDonneesUtilisation(usages.get(TypeUsage.LOUE_TIERS)));
		data.setPropreUsage(extractDonneesUtilisation(usages.get(TypeUsage.USAGE_PROPRE)));
		data.setLoiLogement(extractDonneesLoiLogement(usages.get(TypeUsage.CARACTERE_SOCIAL)));
		if (data.getPropreUsage() != null || data.getLocation() != null || data.getLoiLogement() != null) {
			data.setDateDebut(RegDate.get(degrevement.getValue().getPeriodeFiscale(), 1, 1));
			return data;
		}
		return null;
	}

	private static DonneesUtilisation extractDonneesUtilisation(MigrationDDUsage usage) {
		if (usage == null) {
			return null;
		}
		final BigDecimal pourcentage = BigDecimal.valueOf(usage.getPourdixmilleUsage(), 2);     // pour-dix-mille -> pour-cent
		return new DonneesUtilisation(usage.getRevenuLocation(), usage.getVolume(), usage.getSurface(), pourcentage, pourcentage);
	}

	private static DonneesLoiLogement extractDonneesLoiLogement(MigrationDDUsage usage) {
		if (usage == null) {
			return null;
		}
		final BigDecimal pourcentage = BigDecimal.valueOf(usage.getPourdixmilleUsage(), 2);     // pour-dix-mille -> pour-cent
		return new DonneesLoiLogement(null, null, pourcentage);
	}

	@NotNull
	private ImmeubleRF determinerImmeuble(MigrationDDKey key, Map<String, Commune> mapCommunes) throws ObjectNotFoundException {

		final Commune commune = mapCommunes.get(canonizeName(key.nomCommune));
		if (commune == null) {
			throw new ObjectNotFoundException("La commune avec le nom [" + key.nomCommune + "] n'existe pas.");
		}

		final MigrationParcelle parcelle;
		try {
			parcelle = new MigrationParcelle(key.noBaseParcelle, key.noParcelle, key.noLotPPE);
		}
		catch (RuntimeException e) {
			throw new IllegalArgumentException("Impossible de parser le numéro de parcelle : " + e.getMessage());
		}

		final ImmeubleRF immeuble = immeubleRFDAO.findImmeubleActif(commune.getNoOFS(), parcelle.getNoParcelle(), parcelle.getIndex1(), parcelle.getIndex2(), parcelle.getIndex3(), FlushMode.MANUAL);
		if (immeuble == null) {
			// [SIFISC-23185] peut-être que l'immeuble n'est connu que sur la commune faîtière dans le RF...
			if (commune.isFraction()) {
				final Commune communeFaitiere = mapCommunes.values().stream()
						.filter(Commune::isPrincipale)
						.filter(c -> c.getNoOFS() == commune.getOfsCommuneMere())
						.findFirst()
						.orElse(null);
				if (communeFaitiere != null) {
					final ImmeubleRF immeubleCommuneFaitiere = immeubleRFDAO.findImmeubleActif(communeFaitiere.getNoOFS(), parcelle.getNoParcelle(), parcelle.getIndex1(), parcelle.getIndex2(), parcelle.getIndex3(), FlushMode.MANUAL);
					if (immeubleCommuneFaitiere != null) {
						// ah, on a trouvé quelque chose... on prend ça
						return immeubleCommuneFaitiere;
					}
				}
			}
			else if (commune.getDateFinValidite() != null) {
				// [SIFISC-23184] cette commune a disparu (= fusion)... pas étonnant qu'on ne trouve plus rien dans les données RF...
				// les données que nous avons ne sont donc vraissemblablement plus valide (renumérotation des parcelles lors de la fusion...)
				throw new ObjectNotFoundException("La commune de " + key.nomCommune + " (" + + commune.getNoOFS() + ") a fusionné (fiscalement) au " + RegDateHelper.dateToDisplayString(commune.getDateFinValidite()) + ".");
			}

			// pas mieux, on laisse passer...
			throw new IllegalArgumentException("L'immeuble avec la parcelle [" + parcelle + "] n'existe pas sur la commune de " + commune.getNomOfficiel() + " (" + commune.getNoOFS() + ").");
		}

		return immeuble;
	}

	private static String canonizeName(String name) {
		return name.replaceAll("[-.()]", " ").replaceAll("[\\s]+", " ").trim().toLowerCase();
	}

	@NotNull
	private Entreprise determinerEntreprise(MigrationDDKey demande) {
		final long numero = demande.numeroEntreprise;
		return getEntreprise(numero);
	}

	@NotNull
	private Entreprise getEntreprise(long noEntreprise) throws ObjectNotFoundException {
		final Tiers tiers = tiersDAO.get(noEntreprise);
		if (tiers == null) {
			throw new TiersNotFoundException(noEntreprise);
		}
		else if (!(tiers instanceof Entreprise)) {
			throw new ObjectNotFoundException("Le tiers n°" + noEntreprise + " n'est pas une entreprise (type = " + tiers.getClass().getSimpleName() + ").");
		}

		return (Entreprise) tiers;
	}

	private MigrationDD parseLine(String line) throws IOException, ParseException {
		final CSVParser parser = new CSVParser(';', '"');
		final String[] tokens = parser.parseLine(line);

		final MigrationDD dd = new MigrationDD();
		dd.setNumeroEntreprise(Long.parseLong(tokens[0]));
		dd.setNomEntreprise(tokens[1]);
		dd.setNoAciCommune(Long.parseLong(tokens[2]));
		dd.setNoOfsCommune(Integer.parseInt(tokens[3]));
		dd.setNomCommune(tokens[4]);
		dd.setNoBaseParcelle(tokens[5]);
		dd.setNoParcelle(tokens[6]);
		dd.setNoLotPPE(tokens[7]);
		dd.setDateDebutRattachement(parseDate(tokens[8]));
		dd.setDateFinRattachement(parseDate(tokens[9]));
		dd.setMotifEnvoi(tokens[10]);
		dd.setDateDebutValidite(parseDate(tokens[12]));
		dd.setAnneeFiscale(Integer.parseInt(tokens[13]));
		dd.setDateEnvoi(parseDate(tokens[14]));
		dd.setDelaiRetour(parseDate(tokens[15]));
		dd.setDateRetour(parseDate(tokens[16]));
		dd.setDateRappel(parseDate(tokens[17]));
		dd.setDelaiRappel(parseDate(tokens[18]));
		dd.setEstimationFiscale(parseAmount(tokens[19]));
		dd.setEstimationSoumise(parseAmount(tokens[20]));
		dd.setEstimationExoneree(parseAmount(tokens[21]));
		dd.setEstimationCaractereSocial(parseAmount(tokens[22]));
		dd.setEtabliParCtb(parseBoolean(tokens[23]));
		dd.setModeRattachement(tokens[24]);

		final MigrationDDUsage usage = new MigrationDDUsage();
		usage.setRevenuLocation(parseAmount(tokens[25]));
		usage.setSurface(parseAmount(tokens[26]));
		usage.setVolume(parseAmount(tokens[27]));
		usage.setPourdixmilleUsage(parsePourdixmille(tokens[28]));
		usage.setTypeUsage(parseTypeUsage(tokens[29]));
		dd.setUsage(usage);

		return dd;
	}

	/**
	 * Converti un pourcent avec deux décimales en pour-dix-millièmes ("4.03" -> 403).
	 *
	 * @param token un pourcent sous forme de string
	 * @return le pour-dix-millième correspondant
	 */
	static int parsePourdixmille(String token) throws ParseException {

		final Matcher matcher = PERCENT_PATTERN.matcher(token);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Le pourcent [" + token + "] est invalide");
		}

		final String units = matcher.group(1);
		final String dec = matcher.group(2);
		final String cent = matcher.group(3);

		int val = Integer.parseInt(units) * 100;
		if (dec != null) {
			val += Integer.parseInt(dec) * 10;
		}
		if (cent != null) {
			val += Integer.parseInt(cent);
		}

		return val;
	}

	private static boolean parseBoolean(@Nullable String token) {
		return !StringUtils.isBlank(token) && token.equals("O");
	}

	private static int parseAmount(@Nullable String token) {
		if (StringUtils.isBlank(token)) {
			return 0;
		}
		return Integer.valueOf(token.replaceAll(",", ""));
	}

	@NotNull
	private static TypeUsage parseTypeUsage(@NotNull String token) {
		switch (token) {
		case "PR. USAGE":
			return TypeUsage.USAGE_PROPRE;
		case "LOUE TIERS":
			return TypeUsage.LOUE_TIERS;
		case "CAR. SOC.":
			return TypeUsage.CARACTERE_SOCIAL;
		default:
			throw new NotImplementedException("Le type d'usage = [" + token + "] est inconnu");
		}
	}

	@Nullable
	private static RegDate parseDate(@Nullable String token) throws ParseException {
		if (StringUtils.isBlank(token)) {
			return null;
		}
		Date date;
		try {
			// la plupart des dates sont au format dd/MM/yyyy
			date = DATE_FORMAT_SLASH.parse(token);
		}
		catch (ParseException e) {
			// mais certaines sont au format dd.MM.yyyy
			date = DATE_FORMAT_DOT.parse(token);
		}
		final RegDate d = RegDateHelper.get(date);
		if (d == null) {
			throw new IllegalArgumentException("La date [" + token + "] n'est pas valide.");
		}
		return d;
	}
}
