package ch.vd.uniregctb.registrefoncier.dataimport.detector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.common.Rechteinhaber;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.GemeinschaftsArt;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.rechteregister.BerechtigtePerson;
import ch.vd.capitastra.rechteregister.DienstbarkeitDiscrete;
import ch.vd.capitastra.rechteregister.DienstbarkeitDiscreteList;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.cache.ObjectKey;
import ch.vd.uniregctb.cache.PersistentCache;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.ServitudeRF;
import ch.vd.uniregctb.registrefoncier.TypeDroit;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.elements.DienstbarkeitExtendedElement;
import ch.vd.uniregctb.registrefoncier.dataimport.elements.PersonEigentumAnteilListElement;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.BlacklistRFHelper;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.DroitRFHelper;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.ServitudesRFHelper;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class DroitRFDetector {

	private static final Logger LOGGER = LoggerFactory.getLogger(DroitRFDetector.class);

	private final int batchSize;
	private final XmlHelperRF xmlHelperRF;
	private final AyantDroitRFDAO ayantDroitRFDAO;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;
	private final AyantDroitRFDetector ayantDroitRFDetector;

	private final PersistentCache<ArrayList<PersonEigentumAnteil>> cacheDroits;
	private final PersistentCache<ArrayList<DienstbarkeitDiscrete>> cacheServitudes;

	public DroitRFDetector(XmlHelperRF xmlHelperRF,
	                       AyantDroitRFDAO ayantDroitRFDAO,
	                       EvenementRFImportDAO evenementRFImportDAO,
	                       EvenementRFMutationDAO evenementRFMutationDAO,
	                       PlatformTransactionManager transactionManager,
	                       AyantDroitRFDetector ayantDroitRFDetector,
	                       PersistentCache<ArrayList<PersonEigentumAnteil>> cacheDroits,
	                       PersistentCache<ArrayList<DienstbarkeitDiscrete>> cacheServitudes) {
		this(20, xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits, cacheServitudes);
	}

	public DroitRFDetector(int batchSize,
	                       XmlHelperRF xmlHelperRF,
	                       AyantDroitRFDAO ayantDroitRFDAO,
	                       EvenementRFImportDAO evenementRFImportDAO,
	                       EvenementRFMutationDAO evenementRFMutationDAO,
	                       PlatformTransactionManager transactionManager,
	                       AyantDroitRFDetector ayantDroitRFDetector,
	                       PersistentCache<ArrayList<PersonEigentumAnteil>> cacheDroits,
	                       PersistentCache<ArrayList<DienstbarkeitDiscrete>> cacheServitudes) {
		this.batchSize = batchSize;
		this.xmlHelperRF = xmlHelperRF;
		this.ayantDroitRFDAO = ayantDroitRFDAO;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;
		this.ayantDroitRFDetector = ayantDroitRFDetector;
		this.cacheDroits = cacheDroits;
		this.cacheServitudes = cacheServitudes;
	}

	public void processDroitsPropriete(long importId, int nbThreads, Iterator<PersonEigentumAnteil> iterator, boolean importInitial, @Nullable StatusManager statusManager) {

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les droits de propriété... (regroupement)");
		}

		// Note: on utilise un cache persistant pour limiter l'utilisation mémoire, mais on est pas
		//       du tout intéressé par le côté persistent des données, on commence donc par tout effacer.
		cacheDroits.clear();

		// on regroupe tous les droits par ayant-droit
		groupByAyantDroit(iterator, cacheDroits, PersonEigentumAnteil::getBelastetesGrundstueckIDREF, DroitRFHelper::getIdRF);

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les droits de propriété...", 50);
		}

		// on détecte les mutations qui doivent être générées
		forEachAyantDroit(cacheDroits, this::detecterMutationsDroitsPropriete, importId, importInitial, nbThreads, statusManager);

		// détection des mutations de type SUPRESSION
		detectMutationsDeSuppression(cacheDroits, TypeDroit.DROIT_PROPRIETE, importId);

		// inutile de garder des données en mémoire ou sur le disque
		cacheDroits.clear();
	}

	public void processServitudes(long importId, int nbThreads, Iterator<DienstbarkeitDiscrete> iterator, boolean importInitial, @Nullable StatusManager statusManager) {

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les servitudes... (regroupement)");
		}

		// Note: on utilise un cache persistant pour limiter l'utilisation mémoire, mais on est pas
		//       du tout intéressé par le côté persistent des données, on commence donc par tout effacer.
		cacheServitudes.clear();

		// on regroupe tous les droits par bénéficiaires
		groupByAyantDroit(iterator, cacheServitudes, DroitRFDetector::getImmeubleIdRef, DroitRFDetector::getBeneficiaireIdRef);

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les servitudes...", 50);
		}

		// on détecte les mutations qui doivent être générées
		forEachAyantDroit(cacheServitudes, this::detecterMutationsServitudes, importId, importInitial, nbThreads, statusManager);

		// détection des mutations de type SUPRESSION
		detectMutationsDeSuppression(cacheServitudes, TypeDroit.SERVITUDE, importId);

		// détection des communautés
		processCommunautesServitudes(cacheServitudes, importId);

		// inutile de garder des données en mémoire ou sur le disque
		cacheDroits.clear();
	}

	/**
	 * Extrait les communautés définies dans les servitudes passées en paramètre et génère les mutations de création/modification.
	 */
	private void processCommunautesServitudes(PersistentCache<ArrayList<DienstbarkeitDiscrete>> cacheServitudes, long importId) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(s -> {
			final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

			// on traite aussi toutes les communautés que l'on trouve
			//
			// Note : dans l'export du registre foncier, les communautés ne sont pas fournies dans la liste des propriétaires. A la place,
			//        elles sont fournies en tant qu'entité implicite dans la liste des droits. Dans Unireg, nous sommes partis sur le principe
			//        de traiter les communautés comme des ayant-droits et de les stocker dans la base.

			// on recherche tous les communautés sur les servitudes (une communauté peut se retrouver plusieurs fois)
			final Map<String, Gemeinschaft> communautes = new HashMap<>();
			cacheServitudes.entrySet().stream()
					.map(Map.Entry::getValue)
					.flatMap(Collection::stream)
					.map(DroitRFDetector::getCommunauteFromServitude)
					.filter(Objects::nonNull)
					.forEach(c -> communautes.computeIfAbsent(c.getGemeinschatID(), (k) -> c));

			// on détecte les mutations sur les communautés
			communautes.values()
					.forEach(g -> processAyantDroit(parentImport, g));

			return null;
		});
	}

	private static String getImmeubleIdRef(DienstbarkeitDiscrete servitude) {
		return servitude.getBelastetesGrundstueck().getBelastetesGrundstueckIDREF();
	}

	private static String getBeneficiaireIdRef(DienstbarkeitDiscrete servitude) {
		final BerechtigtePerson beneficiaire = servitude.getBerechtigtePerson();
		if (beneficiaire == null) {
			throw new IllegalArgumentException("Il n'y a pas de bénéficiaire sur la servitude standardRechtId=[" + servitude.getDienstbarkeit().getStandardRechtID() + "]");
		}
		return DienstbarkeitExtendedElement.getPersonIDRef(DienstbarkeitExtendedElement.getPerson(beneficiaire));
	}


	private static <T> void groupByAyantDroit(Iterator<T> iterator, PersistentCache<ArrayList<T>> cacheDroits, Function<T, String> immeubleIdRefProvider, Function<T, String> ayantDroitIdRefProvider) {
		while (iterator.hasNext()) {
			final T eigentumAnteil = iterator.next();
			if (eigentumAnteil == null) {
				break;
			}
			if (BlacklistRFHelper.isBlacklisted(immeubleIdRefProvider.apply(eigentumAnteil))) {
				// on ignore les droits sur les bâtiments blacklistés
				continue;
			}
			final String idRF = ayantDroitIdRefProvider.apply(eigentumAnteil);
			final IdRfCacheKey key = new IdRfCacheKey(idRF);
			ArrayList<T> list = cacheDroits.get(key);
			if (list == null) {
				list = new ArrayList<>();
			}
			list.add(eigentumAnteil);
			cacheDroits.put(key, list);
		}
	}

	@FunctionalInterface
	private interface MutationsSurUnAyantDroitDetector<T> {
		void apply(@NotNull String idRF, @NotNull List<T> droits, @NotNull EvenementRFImport parentImport, boolean importInitial);
	}

	private <T> void forEachAyantDroit(@NotNull final PersistentCache<ArrayList<T>> cacheDroits,
	                                   @NotNull MutationsSurUnAyantDroitDetector<T> detector,
	                                   final long importId,
	                                   final boolean importInitial,
	                                   final int nbThreads,
	                                   @Nullable final StatusManager statusManager) {

		final ParallelBatchTransactionTemplate<Map.Entry<ObjectKey, ArrayList<T>>> template
				= new ParallelBatchTransactionTemplate<Map.Entry<ObjectKey, ArrayList<T>>>(cacheDroits.entrySet().iterator(),
				                                                                           batchSize,
				                                                                           nbThreads,
				                                                                           Behavior.REPRISE_AUTOMATIQUE,
				                                                                           transactionManager,
				                                                                           null,
				                                                                           AuthenticationInterface.INSTANCE) {
			@Override
			protected int getBlockingQueueCapacity() {
				// on limite la queue interne du template à 10 lots de BATCH_SIZE, autrement
				// on sature rapidemment la mémoire de la JVM avec l'entier du fichier d'import.
				return 10;
			}
		};

		final AtomicInteger processed = new AtomicInteger();

		// détection des mutations de type CREATION et MODIFICATION
		template.execute(new BatchCallback<Map.Entry<ObjectKey, ArrayList<T>>>() {

			private final ThreadLocal<Map.Entry<ObjectKey, ArrayList<T>>> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Map.Entry<ObjectKey, ArrayList<T>>> batch) throws Exception {
				first.set(batch.get(0));

				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				batch.forEach(e -> {
					final String idRF = ((IdRfCacheKey) e.getKey()).getIdRF();  // l'idRF de l'ayant-droit
					final List<T> nouveauxDroits = e.getValue();                // les droits de l'ayant-droit
					detector.apply(idRF, nouveauxDroits, parentImport, importInitial);
				});

				processed.addAndGet(batch.size());
				if (statusManager != null) {
					statusManager.setMessage("Détection des mutations sur les droits... (" + processed.get() + " processés)", 50);
				}

				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.error("Exception sur le traitement des droits de l'ayant-droit idRF=[" + first.get().getKey() + "]", e);
					throw new RuntimeException(e);
				}
			}
		}, null);
	}

	/**
	 * Cette méthode détecte les changements (création ou update) sur les droits de propriété d'un ayant-droit et crée les mutations correspondantes.
	 */
	private void detecterMutationsDroitsPropriete(@NotNull String idRF, @NotNull List<PersonEigentumAnteil> droits, @NotNull EvenementRFImport parentImport, boolean importInitial) {

		final AyantDroitRF ayantDroit = ayantDroitRFDAO.find(new AyantDroitRFKey(idRF));
		if (ayantDroit == null) {
			// l'ayant-droit n'existe pas : il va être créé et on doit donc sauver une mutation en mode création.
			final EvenementRFMutation mutation = new EvenementRFMutation();
			mutation.setParentImport(parentImport);
			mutation.setEtat(EtatEvenementRF.A_TRAITER);
			mutation.setTypeEntite(TypeEntiteRF.DROIT);
			mutation.setTypeMutation(TypeMutationRF.CREATION);
			mutation.setIdRF(idRF); // idRF de l'ayant-droit
			mutation.setXmlContent(xmlHelperRF.toXMLString(new PersonEigentumAnteilListElement(droits)));
			evenementRFMutationDAO.save(mutation);
		}
		else {
			// on récupère les droits actifs actuels
			final Set<DroitRF> activesDroits = ayantDroit.getDroits().stream()
					.filter(d -> d.isValidAt(null))
					.filter(d -> d instanceof DroitProprieteRF)
					.collect(Collectors.toSet());

			//noinspection StatementWithEmptyBody
			if (!DroitRFHelper.dataEquals(activesDroits, droits, importInitial)) {
				// les droits sont différents : on sauve une mutation en mode modification
				final EvenementRFMutation mutation = new EvenementRFMutation();
				mutation.setParentImport(parentImport);
				mutation.setEtat(EtatEvenementRF.A_TRAITER);
				mutation.setTypeEntite(TypeEntiteRF.DROIT);
				mutation.setTypeMutation(TypeMutationRF.MODIFICATION);
				mutation.setIdRF(idRF); // idRF de l'ayant-droit
				mutation.setXmlContent(xmlHelperRF.toXMLString(new PersonEigentumAnteilListElement(droits)));
				evenementRFMutationDAO.save(mutation);
			}
			else {
				// les droits sont égaux : rien à faire
			}
		}

		// on traite aussi toutes les communautés que l'on trouve

		// Note : dans l'export du registre foncier, les communautés ne sont pas fournies dans la liste des propriétaires. A la place,
		//        elles sont fournies en tant qu'entité implicite dans la liste des droits. Dans Unireg, nous sommes partis sur le principe
		//        de traiter les communautés comme des ayant-droits et de les stocker dans la base.
		droits.stream()
				.map(PersonEigentumAnteil::getGemeinschaft)
				.filter(Objects::nonNull)
				.forEach(g -> processAyantDroit(parentImport, g));
	}

	/**
	 * Cette méthode détecte les changements (création ou update) sur les servitudes d'un bénéficiaire et crée les mutations correspondantes.
	 */
	private void detecterMutationsServitudes(@NotNull String idRF, @NotNull List<DienstbarkeitDiscrete> servitudes, @NotNull EvenementRFImport parentImport, boolean importInitial) {

		final AyantDroitRF ayantDroit = ayantDroitRFDAO.find(new AyantDroitRFKey(idRF));
		if (ayantDroit == null) {
			// l'ayant-droit n'existe pas : il va être créé et on doit donc sauver une mutation en mode création.
			final EvenementRFMutation mutation = new EvenementRFMutation();
			mutation.setParentImport(parentImport);
			mutation.setEtat(EtatEvenementRF.A_TRAITER);
			mutation.setTypeEntite(TypeEntiteRF.SERVITUDE);
			mutation.setTypeMutation(TypeMutationRF.CREATION);
			mutation.setIdRF(idRF); // idRF de l'ayant-droit
			mutation.setXmlContent(xmlHelperRF.toXMLString(new DienstbarkeitDiscreteList(servitudes)));
			evenementRFMutationDAO.save(mutation);
		}
		else {
			// on récupère les servitudes actives actuelles
			final Set<DroitRF> activesServitudes = ayantDroit.getDroits().stream()
					.filter(d -> d.isValidAt(RegDate.get()))    // les usufruits peuvent posséder des dates de fin dans le futur
					.filter(d -> d instanceof ServitudeRF)
					.collect(Collectors.toSet());

			//noinspection StatementWithEmptyBody
			if (!ServitudesRFHelper.dataEquals(activesServitudes, servitudes)) {
				// les droits sont différents : on sauve une mutation en mode modification
				final EvenementRFMutation mutation = new EvenementRFMutation();
				mutation.setParentImport(parentImport);
				mutation.setEtat(EtatEvenementRF.A_TRAITER);
				mutation.setTypeEntite(TypeEntiteRF.SERVITUDE);
				mutation.setTypeMutation(TypeMutationRF.MODIFICATION);
				mutation.setIdRF(idRF); // idRF de l'ayant-droit
				mutation.setXmlContent(xmlHelperRF.toXMLString(new DienstbarkeitDiscreteList(servitudes)));
				evenementRFMutationDAO.save(mutation);
			}
			else {
				// les droits sont égaux : rien à faire
			}
		}
	}

	@Nullable
	private static Gemeinschaft getCommunauteFromServitude(@NotNull DienstbarkeitDiscrete servitude) {
		if (servitude.getGemeinschaft().isEmpty()) {
			return null;
		}
		// on instancie une communauté à partir des données de l'usufruit, faute de mieux
		final Gemeinschaft gemeinschaft = new Gemeinschaft();
		gemeinschaft.setGemeinschatID(servitude.getDienstbarkeit().getStandardRechtID());
		gemeinschaft.setArt(GemeinschaftsArt.GEMEINDERSCHAFT);
		return gemeinschaft;
	}

	private <T> void detectMutationsDeSuppression(PersistentCache<ArrayList<T>> cacheDroits, TypeDroit typeDroit, long importId) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(s -> {
			final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

			final Set<String> existingAyantsDroits = ayantDroitRFDAO.findAvecDroitsActifs(typeDroit);
			final Set<String> nouveauAyantsDroits = cacheDroits.keySet().stream()
					.map(k -> ((IdRfCacheKey) k).getIdRF())
					.collect(Collectors.toSet());

			// on ne garde que les ayants-droits existants dans la DB qui n'existent pas dans le fichier XML
			existingAyantsDroits.removeAll(nouveauAyantsDroits);

			// on crée des mutations de suppression de droits pour tous ces ayants-droits
			existingAyantsDroits.forEach(idRF -> {
				final EvenementRFMutation mutation = new EvenementRFMutation();
				mutation.setParentImport(parentImport);
				mutation.setEtat(EtatEvenementRF.A_TRAITER);
				mutation.setTypeEntite(typeDroit == TypeDroit.DROIT_PROPRIETE ? TypeEntiteRF.DROIT : TypeEntiteRF.SERVITUDE);
				mutation.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mutation.setIdRF(idRF); // idRF de l'ayant-droit
				mutation.setXmlContent(null);
				evenementRFMutationDAO.save(mutation);
			});

			return null;
		});
	}

	private void processAyantDroit(EvenementRFImport parentImport, Rechteinhaber rechteinhaber) {
		ayantDroitRFDetector.processAyantDroit(parentImport, rechteinhaber);
	}
}
