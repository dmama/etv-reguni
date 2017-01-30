package ch.vd.uniregctb.foncier.migration.ifonc;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import au.com.bytecode.opencsv.CSVParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.MultipleSwitch;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.foncier.ExonerationIFONC;
import ch.vd.uniregctb.foncier.migration.MigrationParcelle;
import ch.vd.uniregctb.foncier.migration.ParsingHelper;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.tache.TacheSynchronizerInterceptor;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.validation.ValidationInterceptor;

public class MigrationExoIFONCImporter {

	private static final int BATCH_SIZE = 100;

	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationExoIFONCImporter.class);

	private final ServiceInfrastructureService infraService;
	private final PlatformTransactionManager transactionManager;
	private final ImmeubleRFDAO immeubleRFDAO;
	private final ValidationInterceptor validationInterceptor;
	private final GlobalTiersIndexer tiersIndexer;
	private final TacheSynchronizerInterceptor tacheSynchronizerInterceptor;
	private final HibernateTemplate hibernateTemplate;

	public MigrationExoIFONCImporter(ServiceInfrastructureService infraService, PlatformTransactionManager transactionManager, ImmeubleRFDAO immeubleRFDAO, ValidationInterceptor validationInterceptor, GlobalTiersIndexer tiersIndexer,
	                                 TacheSynchronizerInterceptor tacheSynchronizerInterceptor, HibernateTemplate hibernateTemplate) {
		this.infraService = infraService;
		this.transactionManager = transactionManager;
		this.immeubleRFDAO = immeubleRFDAO;
		this.validationInterceptor = validationInterceptor;
		this.tiersIndexer = tiersIndexer;
		this.tacheSynchronizerInterceptor = tacheSynchronizerInterceptor;
		this.hibernateTemplate = hibernateTemplate;
	}

	public MigrationExoIFONCImporterResults loadCSV(@NotNull InputStream csvStream, @NotNull String encoding, int nbThreads, @Nullable StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);
		final MigrationExoIFONCImporterResults rapportFinal = new MigrationExoIFONCImporterResults(nbThreads);

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
			status.setMessage("L'import des éxonérations IFONC a été interrompu."
					                  + " Nombre d'exonérations importées au moment de l'interruption = " + rapportFinal.getNbLignesLues());
			rapportFinal.setInterrompu(true);
		}
		else {
			status.setMessage("L'import des exonérations IFONC est terminé."
					                  + " Nombre d'exonération importées = " + rapportFinal.getNbLignesLues() + ". Nombre d'erreurs = " + rapportFinal.getExonerationsEnErreur().size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	private void processAllLines(Scanner csv, int nbThreads, MigrationExoIFONCImporterResults rapport, StatusManager status) throws IOException, ParseException {

		// index pour pouvoir suivre, dans les rapports d'erreurs, la ligne incriminée
		int index = 1;

		// on saute les lignes d'entête (la dernière d'entre elles est une suite de tirets)
		while (csv.hasNext()) {
			final String line = csv.nextLine();
			rapport.addLigneLue();
			++ index;
			if (line.startsWith("----")) {
				// c'était la dernière ligne d'entête...
				break;
			}
		}

		// pour des raisons d'optimisation, on va chercher toutes les communes et on va les classer par nom "canonique"
		final Map<String, Commune> mapCommunes = infraService.getCommunes().stream()
				.collect(Collectors.toMap(commune -> canonizeName(commune.getNomOfficiel()),
				                          Function.identity(),
				                          (c1, c2) -> Stream.of(c1, c2).max(Comparator.comparing(Commune::getDateDebutValidite, NullDateBehavior.EARLIEST::compare)).get()));

		// on lit maintenant toutes les lignes du fichier ...
		final List<MigrationExoIFONC> exonerations = new LinkedList<>();
		while (csv.hasNext()) {

			final MigrationExoIFONC data;
			try {
				rapport.addLigneLue();
				data = parseLine(csv.nextLine());
			}
			catch (RuntimeException e) {
				rapport.addLigneEnErreur(index, e.getMessage());
				continue;
			}
			finally {
				++ index;
			}

			exonerations.add(data);

			// même pendant la lecture du fichier, on peut devoir interrompre le traitement
			if (status.interrupted()) {
				break;
			}
		}

		// ... et finalement on génère les données en base Unireg

		// regroupons les données par contribuable
		final Map<Long, List<MigrationExoIFONC>> parContribuable = exonerations.stream()
				.collect(Collectors.toMap(MigrationExoIFONC::getNumeroEntreprise,
				                          Collections::singletonList,
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

		// on désactive la validation automatique des données sauvées car la validation
		// des entreprises est très coûteuse et - surtout - les demandes de dégrèvements
		// migrées de SIMPA-PM n'influence pas sur l'état valide ou non des entreprises.
		final MultipleSwitch mainSwitch = new MultipleSwitch(validationInterceptor,
		                                                     tiersIndexer.onTheFlyIndexationSwitch(),
		                                                     tacheSynchronizerInterceptor);

		final SimpleProgressMonitor monitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplate<Map.Entry<Long, List<MigrationExoIFONC>>> template =
				new ParallelBatchTransactionTemplate<>(new ArrayList<>(parContribuable.entrySet()), BATCH_SIZE, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, AuthenticationInterface.INSTANCE);
		template.execute(new BatchCallback<Map.Entry<Long, List<MigrationExoIFONC>>>() {

			private final ThreadLocal<MigrationExoIFONCImporterResults> subRapport = new ThreadLocal<>();
			private final ThreadLocal<MigrationExoIFONC> last = new ThreadLocal<>();

			@Override
			public void beforeTransaction() {
				subRapport.set(new MigrationExoIFONCImporterResults(nbThreads));
				mainSwitch.pushState();
				mainSwitch.setEnabled(false);
			}

			@Override
			public boolean doInTransaction(List<Map.Entry<Long, List<MigrationExoIFONC>>> batch) throws Exception {
				batch.forEach(batchEntry -> {

					// ensuite, on traite contribuable par contribuable
					final Entreprise entreprise = getEntreprise(batchEntry.getKey());

					// map des dégrèvements à persister pour cette entreprise par identifiant d'immeuble
					final Map<Long, List<ExonerationIFONC>> exosParImmeuble = batchEntry.getValue().stream()
							.map(data -> {
								last.set(data);
								final ExonerationIFONC exo = mapToExoneration(data, entreprise, mapCommunes);
								subRapport.get().incExonerationsTraitees();
								return exo;
							})
							.collect(Collectors.toMap(d -> d.getImmeuble().getId(),
							                          Collections::singletonList,
							                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));

					// persistence
					exosParImmeuble.values().stream()
							.flatMap(List::stream)
							.map(hibernateTemplate::merge)
							.forEach(entreprise::addAllegementFoncier);
				});
				status.setMessage("Migration des exonérations IFONC...", monitor.getProgressInPercent());
				return true;
			}

			@Override
			public void afterTransactionCommit() {
				mainSwitch.popState();
				synchronized (rapport) {
					rapport.addAll(subRapport.get());
				}
				subRapport.remove();
				last.remove();
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				mainSwitch.popState();
				if (!willRetry) {
					synchronized (rapport) {
						rapport.addExonerationEnErreur(last.get(), e.getMessage());
					}
				}
				subRapport.remove();
				last.remove();
			}
		}, monitor);
	}

	private MigrationExoIFONC parseLine(String line) throws IOException, ParseException {
		final CSVParser parser = new CSVParser(';', '"');
		final String[] tokens = parser.parseLine(line);

		final MigrationExoIFONC exo = new MigrationExoIFONC();
		exo.setNumeroEntreprise(Long.parseLong(tokens[0]));
		exo.setNomEntreprise(tokens[1]);
		exo.setNoAciCommune(Long.parseLong(tokens[2]));
		exo.setNoOfsCommune(Integer.parseInt(tokens[3]));
		exo.setNomCommune(tokens[4]);
		exo.setNoBaseParcelle(tokens[5]);
		exo.setNoParcelle(tokens[6]);
		exo.setNoLotPPE(tokens[7]);
		exo.setDateDebutRattachement(ParsingHelper.parseDate(tokens[8]));
		exo.setDateFinRattachement(ParsingHelper.parseDate(tokens[9]));
		exo.setPourdixmilleExoneration(ParsingHelper.parsePourdixmille(tokens[10]));
		exo.setAnneeDebutExoneration(Integer.parseInt(tokens[11]));
		exo.setAnneeFinExoneration(Optional.of(Integer.parseInt(tokens[12])).filter(i -> i != 0).orElse(null));     // 0 -> null

		return exo;
	}

	private Entreprise getEntreprise(long id) {
		return hibernateTemplate.get(Entreprise.class, id);
	}

	@NotNull
	private ImmeubleRF determinerImmeuble(MigrationExoIFONC demande, Map<String, Commune> mapCommunes) {

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

		final ImmeubleRF immeuble = immeubleRFDAO.findImmeubleActif(commune.getNoOFS(), parcelle.getNoParcelle(), parcelle.getIndex1(), parcelle.getIndex2(), parcelle.getIndex3());
		if (immeuble == null) {
			throw new IllegalArgumentException("L'immeuble avec la parcelle [" + parcelle + "] n'existe pas sur la commune de " + commune.getNomOfficiel() + " (" + commune.getNoOFS() + ").");
		}

		return immeuble;
	}

	private static String canonizeName(String name) {
		return name.replaceAll("[-.()]", " ").replaceAll("[\\s]+", " ").trim().toLowerCase();
	}

	ExonerationIFONC mapToExoneration(MigrationExoIFONC data, Entreprise entreprise, Map<String, Commune> mapCommunes) {
		final ImmeubleRF immeuble = determinerImmeuble(data, mapCommunes);
		final ExonerationIFONC exo = new ExonerationIFONC();
		exo.setContribuable(entreprise);
		exo.setImmeuble(immeuble);
		exo.setDateDebut(RegDate.get(data.getAnneeDebutExoneration(), 1, 1));
		if (data.getAnneeFinExoneration() != null) {
			exo.setDateFin(RegDate.get(data.getAnneeFinExoneration(), 12, 31));
		}
		exo.setPourcentageExoneration(BigDecimal.valueOf(data.getPourdixmilleExoneration(), 2));
		return exo;
	}
}
