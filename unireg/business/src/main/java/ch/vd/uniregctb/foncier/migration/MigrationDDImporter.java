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
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.common.MultipleSwitch;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
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
					                  + " Nombre de dégrèvements importés = " + rapportFinal.getNbLignes() + ". Nombre d'erreurs = " + rapportFinal.getDemandesEnErreur().size());
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

		// Clé principale : clé d'identification du couple entreprise / immeuble
		// Valeur (map) : clé = période fiscale, valeur = données à migrée
		final Map<MigrationDDKey, Map<Integer, MigrationDD>> map = new HashMap<>();

		// on regroupe les demandes pour gérer correctement les cas où il y a plusieurs usages

		while (csvIterator.hasNext()) {

			// on parse la ligne
			final MigrationDD dd;
			try {
				dd = parseLine(csvIterator.next());
			}
			catch (RuntimeException e) {
				rapport.addLineEnErreur(index, e.getMessage());
				continue;
			}
			finally {
				++index;
			}

			final MigrationDDKey key = new MigrationDDKey(dd);
			final Map<Integer, MigrationDD> immeubleData = map.computeIfAbsent(key, k -> new HashMap<>());

			final MigrationDD val = immeubleData.get(dd.getAnneeFiscale());
			if (val == null) {
				// il n'y a pas de demande de dégrèvement pré-existante, on l'ajoute
				immeubleData.put(dd.getAnneeFiscale(), dd);
				rapport.incNbDemandesExtraites();
			}
			else {
				// il y a déjà une demande dégrèvement pré-existante, il s'agit simplement d'un nouvel usage : on l'ajoute sur la DD existante.
				try {
					assertDataEquals(val, dd);
					val.addUsage(dd.getUsages().iterator().next());
				}
				catch (IllegalArgumentException e) {
					rapport.addDemandeEnErreur(dd, "Inconsistence dans les usages : " + e.getMessage());
				}
			}

			final int lineProcessed = rapport.incNbLignes();
			if (lineProcessed % 100 == 0) {
				status.setMessage("Lecture de la ligne n°" + lineProcessed + "...");
			}

			if (status.interrupted()) {
				break;
			}
		}

		// pour des raisons d'optimisation, on va chercher toutes les communes et on va les classer par nom "canonique"
		final Map<String, Commune> mapCommunes = infraService.getCommunes().stream()
				.collect(Collectors.toMap(commune -> canonizeName(commune.getNomOfficiel()),
				                          Function.identity(),
				                          (c1, c2) -> Stream.of(c1, c2).max(Comparator.comparing(Commune::getDateDebutValidite, NullDateBehavior.EARLIEST::compare)).get()));

		// on va devoir traiter les dossiers contribuable par contribuable
		final Map<Long, List<Map<Integer, MigrationDD>>> mapParContribuable = map.entrySet().stream()
				.collect(Collectors.toMap(entry -> entry.getKey().numeroEntreprise,
				                          entry -> Collections.singletonList(entry.getValue()),
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

		// on désactive la validation automatique des données sauvées car la validation
		// des entreprises est très coûteuse et - surtout - les demandes de dégrèvements
		// migrées de SIMPA-PM n'influence pas sur l'état valide ou non des entreprises.
		final MultipleSwitch mainSwitch = new MultipleSwitch(validationInterceptor,
		                                                     tiersIndexer.onTheFlyIndexationSwitch(),
		                                                     tacheSynchronizerInterceptor);

		final SimpleProgressMonitor monitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplate<Map.Entry<Long, List<Map<Integer, MigrationDD>>>> template =
				new ParallelBatchTransactionTemplate<>(new ArrayList<>(mapParContribuable.entrySet()), BATCH_SIZE, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, AuthenticationInterface.INSTANCE);
		template.execute(new BatchCallback<Map.Entry<Long, List<Map<Integer, MigrationDD>>>>() {

			private final ThreadLocal<MigrationDDImporterResults> subRapport = new ThreadLocal<>();
			private final ThreadLocal<Long> contribuable = new ThreadLocal<>();

			@Override
			public void beforeTransaction() {
				subRapport.set(new MigrationDDImporterResults(nbThreads));
				mainSwitch.pushState();
				mainSwitch.setEnabled(false);
			}

			@Override
			public boolean doInTransaction(List<Map.Entry<Long, List<Map<Integer, MigrationDD>>>> batch) throws Exception {
				batch.forEach(batchEntry -> {

					// ensuite, on traite contribuable par contribuable
					contribuable.set(batchEntry.getKey());
					final Entreprise entreprise = getEntreprise(batchEntry.getKey());

					// map des dégrèvements à persister pour cette entreprise par identifiant d'immeuble
					final List<DemandeDegrevementICI> demandes = new LinkedList<>();
					final Map<Long, List<DegrevementICI>> degrevementsParImmeuble = batchEntry.getValue().stream()
							.map(map -> {
								final List<MigrationDD> list = new ArrayList<>(map.values());
								final MigrationDD toSave = getDDToSave(list, subRapport.get());
								try {
									final ResultatTraitementDemande resultatTraitement = traiterDemande(toSave, mapCommunes);
									subRapport.get().incNbDemandesTraitees();
									demandes.add(resultatTraitement.demande);
									return resultatTraitement.degrevements;
								}
								catch (Exception e) {
									subRapport.get().addDemandeEnErreur(toSave, e.getMessage());
									return null;
								}
							})
							.filter(Objects::nonNull)
							.flatMap(List::stream)
							.collect(Collectors.toMap(d -> d.getImmeuble().getId(),
							                          Collections::singletonList,
							                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

					// assignation des dates de fin
					for (List<DegrevementICI> degrevements : degrevementsParImmeuble.values()) {
						if (degrevements.size() > 1) {
							degrevements.sort(Comparator.comparing(DegrevementICI::getDateDebut));
							final MovingWindow<DegrevementICI> wnd = new MovingWindow<>(degrevements);
							while (wnd.hasNext()) {
								final MovingWindow.Snapshot<DegrevementICI> snapshot = wnd.next();
								final DegrevementICI current = snapshot.getCurrent();
								final DegrevementICI next = snapshot.getNext();
								if (next != null) {
									current.setDateFin(next.getDateDebut().getOneDayBefore());
								}
							}
						}
					}

					// persistence des demandes
					demandes.stream()
							.map(hibernateTemplate::merge)
							.forEach(entreprise::addAutreDocumentFiscal);

					// persistence des dégrèvements
					degrevementsParImmeuble.values().stream()
							.flatMap(List::stream)
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
	 * Classe qui contient les données (non-persistées) qu'il faudra persister une pour la donnée en provenance de SIMPA
	 */
	private static final class ResultatTraitementDemande {
		final DemandeDegrevementICI demande;
		final List<DegrevementICI> degrevements;

		public ResultatTraitementDemande(DemandeDegrevementICI demande, List<DegrevementICI> degrevements) {
			this.demande = demande;
			this.degrevements = degrevements;
		}
	}

	/**
	 * Migre une demande et extrait les dégrèvements ICI
	 * @param demande la demande en entrée
	 * @return la liste des dégrèvements à persister
	 */
	@NotNull
	private ResultatTraitementDemande traiterDemande(MigrationDD demande, Map<String, Commune> mapCommunes) {

		final Entreprise entreprise = determinerEntreprise(demande);
		final ImmeubleRF immeuble = determinerImmeuble(demande, mapCommunes);

		final DemandeDegrevementICI dd = new DemandeDegrevementICI();
		dd.setImmeuble(immeuble);
		dd.setEntreprise(entreprise);
		dd.setPeriodeFiscale(demande.getAnneeFiscale());
		dd.setDateEnvoi(demande.getDateEnvoi());
		dd.setDelaiRetour(demande.getDelaiRetour());
		dd.setDateRappel(demande.getDateRappel());
		dd.setDateRetour(demande.getDateRetour());

		final Map<TypeUsage, MigrationDDUsage> usagesParType = demande.getUsages().stream()
				.collect(Collectors.toMap(MigrationDDUsage::getTypeUsage,
				                          Function.identity(),
				                          (u1, u2) -> u2,
				                          () -> new EnumMap<>(TypeUsage.class)));

		final List<DegrevementICI> degrevements = new ArrayList<>(usagesParType.size());
		if (!usagesParType.isEmpty()) {
			final DegrevementICI degrevement = new DegrevementICI();
			degrevement.setImmeuble(immeuble);
			degrevement.setContribuable(entreprise);
			degrevement.setPropreUsage(extractDonneesUtilisation(usagesParType.get(TypeUsage.USAGE_PROPRE)));
			degrevement.setLocation(extractDonneesUtilisation(usagesParType.get(TypeUsage.LOUE_TIERS)));
			degrevement.setLoiLogement(null);           // TODO comment migrer ça ?
			if (degrevement.getPropreUsage() != null || degrevement.getLocation() != null || degrevement.getLoiLogement() != null) {
				degrevement.setDateDebut(RegDate.get(demande.getAnneeFiscale(), 1, 1));
				degrevements.add(degrevement);
			}
		}

		return new ResultatTraitementDemande(dd, degrevements);
	}

	private static DonneesUtilisation extractDonneesUtilisation(MigrationDDUsage usage) {
		if (usage == null) {
			return null;
		}
		final BigDecimal pourcentage = BigDecimal.valueOf(usage.getPourdixmilleUsage(), 2);     // pour-dix-mille -> pour-cent
		return new DonneesUtilisation(usage.getRevenuLocation(), usage.getVolume(), usage.getSurface(), pourcentage, pourcentage);
	}

	@NotNull
	private ImmeubleRF determinerImmeuble(MigrationDD demande, Map<String, Commune> mapCommunes) {

		final Commune commune = mapCommunes.get(canonizeName(demande.getNomCommune()));
		if (commune == null) {
			throw new IllegalArgumentException("La commune avec le nom [" + demande.getNomCommune() + "] n'existe pas.");
		}

		final MigrationParcelle parcelle;
		try {
			parcelle = new MigrationParcelle(demande.getNoBaseParcelle(), demande.getNoParcelle(), demande.getNoLotPPE());
		}
		catch (RuntimeException e) {
			throw new IllegalArgumentException("Impossible de parser le numéro de parcelle : " + e.getMessage());
		}

		final ImmeubleRF immeuble = immeubleRFDAO.findImmeubleActif(commune.getNoOFS(), parcelle.getNoParcelle(), parcelle.getIndex1(), parcelle.getIndex2(), parcelle.getIndex3(), FlushMode.MANUAL);
		if (immeuble == null) {
			throw new IllegalArgumentException("L'immeuble avec la parcelle [" + parcelle + "] n'existe pas sur la commune de " + commune.getNomOfficiel() + " (" + commune.getNoOFS() + ").");
		}

		return immeuble;
	}

	private static String canonizeName(String name) {
		return name.replaceAll("[-.()]", " ").replaceAll("[\\s]+", " ").trim().toLowerCase();
	}

	@NotNull
	private Entreprise determinerEntreprise(MigrationDD demande) {
		final long numero = demande.getNumeroEntreprise();
		return getEntreprise(numero);
	}

	@NotNull
	private Entreprise getEntreprise(long noEntreprise) {
		final Tiers tiers = tiersDAO.get(noEntreprise);
		if (tiers == null) {
			throw new IllegalArgumentException("Le tiers n°" + noEntreprise + " n'existe pas.");
		}
		else if (!(tiers instanceof Entreprise)) {
			throw new IllegalArgumentException("Le tiers n°" + noEntreprise + " n'est pas une entreprise (type = " + tiers.getClass().getSimpleName() + ").");
		}

		return (Entreprise) tiers;
	}

	/**
	 * @return la demande de dégrèvement à sauver dans la DB
	 */
	@NotNull
	static MigrationDD getDDToSave(@NotNull List<MigrationDD> list, @NotNull MigrationDDImporterResults rapport) {
		final int size = list.size();
		final MigrationDD toSave;
		if (size == 1) {
			// il n'y a qu'une seule année de dégrèvement (= la dernière), tout va bien
			toSave = list.get(0);
		}
		else {
			// il y a plusieurs années, il s'agit d'un bug de l'export SIMPA, on corrige en prenant la dernière année fiscale
			Collections.sort(list, Comparator.comparingInt(MigrationDD::getAnneeFiscale));
			toSave = list.get(size - 1);
			for (int i = 0; i < size - 1; ++i) {
				rapport.addDemandeIgnoree(list.get(i), "Une demande de dégrèvement plus récente (" + toSave.getAnneeFiscale() + ") existe dans l'export (cette demande = " + list.get(i).getAnneeFiscale() + ").");
			}
		}
		return toSave;
	}

	private void assertDataEquals(MigrationDD right, MigrationDD left) {
		assertEquals(right.getNumeroEntreprise(), left.getNumeroEntreprise(), "numeroEntreprise");
		assertEquals(right.getNomEntreprise(), left.getNomEntreprise(), "nomEntreprise");
		assertEquals(right.getNoAciCommune(), left.getNoAciCommune(), "noAciCommune");
		assertEquals(right.getNoOfsCommune(), left.getNoOfsCommune(), "noOfsCommune");
		assertEquals(right.getNomCommune(), left.getNomCommune(), "nomCommune");
		assertEquals(right.getNoBaseParcelle(), left.getNoBaseParcelle(), "noBaseParcelle");
		assertEquals(right.getNoParcelle(), left.getNoParcelle(), "noParcelle");
		assertEquals(right.getNoLotPPE(), left.getNoLotPPE(), "noLotPPE");
		assertEquals(right.getDateDebutRattachement(), left.getDateDebutRattachement(), "dateDebutRattachement");
		assertEquals(right.getDateFinRattachement(), left.getDateFinRattachement(), "dateFinRattachement");
		assertEquals(right.getModeRattachement(), left.getModeRattachement(), "modeRattachement");
		assertEquals(right.getMotifEnvoi(), left.getMotifEnvoi(), "motifEnvoi");
		assertEquals(right.getDateDebutValidite(), left.getDateDebutValidite(), "dateDebutValidite");
		assertEquals(right.getAnneeFiscale(), left.getAnneeFiscale(), "anneeFiscale");
		assertEquals(right.getDateEnvoi(), left.getDateEnvoi(), "dateEnvoi");
		assertEquals(right.getDelaiRetour(), left.getDelaiRetour(), "delaiRetour");
		assertEquals(right.getDateRetour(), left.getDateRetour(), "dateRetour");
		assertEquals(right.getDateRappel(), left.getDateRappel(), "dateRappel");
		assertEquals(right.getDelaiRappel(), left.getDelaiRappel(), "delaiRappel");
		assertEquals(right.getEstimationFiscale(), left.getEstimationFiscale(), "estimationFiscale");
		assertEquals(right.getEstimationSoumise(), left.getEstimationSoumise(), "estimationSoumise");
		assertEquals(right.getEstimationExoneree(), left.getEstimationExoneree(), "estimationExoneree");
		assertEquals(right.getEstimationCaractereSocial(), left.getEstimationCaractereSocial(), "estimationCaractereSocial");
		assertEquals(right.isEtabliParCtb(), left.isEtabliParCtb(), "etabliParCtb");
	}

	private void assertEquals(Object left, Object right, String field) {
		if (!Objects.equals(left, right)) {
			throw new IllegalArgumentException("les valeurs du champ '" + field + "' sont différentes : left=[" + left + "] right=[" + right + "]");
		}
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
		dd.addUsage(usage);

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