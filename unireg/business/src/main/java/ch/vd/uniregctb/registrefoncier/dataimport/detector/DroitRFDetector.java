package ch.vd.uniregctb.registrefoncier.dataimport.detector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.capitastra.common.Rechteinhaber;
import ch.vd.capitastra.grundstueck.EigentumAnteil;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.GrundstueckEigentumAnteil;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.UnbekanntesGrundstueck;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.uniregctb.cache.ObjectKey;
import ch.vd.uniregctb.cache.PersistentCache;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.TypeDroit;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.elements.principal.PersonEigentumAnteilListElement;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.BlacklistRFHelper;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.DroitRFHelper;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;

public class DroitRFDetector {

	private static final Logger LOGGER = LoggerFactory.getLogger(DroitRFDetector.class);

	private final int batchSize;
	private final XmlHelperRF xmlHelperRF;
	private final BlacklistRFHelper blacklistRFHelper;
	private final AyantDroitRFDAO ayantDroitRFDAO;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;
	private final AyantDroitRFDetector ayantDroitRFDetector;

	private final PersistentCache<ArrayList<EigentumAnteil>> cacheDroits;

	public DroitRFDetector(XmlHelperRF xmlHelperRF,
	                       BlacklistRFHelper blacklistRFHelper,
	                       AyantDroitRFDAO ayantDroitRFDAO,
	                       EvenementRFImportDAO evenementRFImportDAO,
	                       EvenementRFMutationDAO evenementRFMutationDAO,
	                       PlatformTransactionManager transactionManager,
	                       AyantDroitRFDetector ayantDroitRFDetector,
	                       PersistentCache<ArrayList<EigentumAnteil>> cacheDroits) {
		this(20, xmlHelperRF, blacklistRFHelper, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits);
	}

	public DroitRFDetector(int batchSize,
	                       XmlHelperRF xmlHelperRF,
	                       BlacklistRFHelper blacklistRFHelper,
	                       AyantDroitRFDAO ayantDroitRFDAO,
	                       EvenementRFImportDAO evenementRFImportDAO,
	                       EvenementRFMutationDAO evenementRFMutationDAO,
	                       PlatformTransactionManager transactionManager,
	                       AyantDroitRFDetector ayantDroitRFDetector,
	                       PersistentCache<ArrayList<EigentumAnteil>> cacheDroits) {
		this.batchSize = batchSize;
		this.xmlHelperRF = xmlHelperRF;
		this.blacklistRFHelper = blacklistRFHelper;
		this.ayantDroitRFDAO = ayantDroitRFDAO;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;
		this.ayantDroitRFDetector = ayantDroitRFDetector;
		this.cacheDroits = cacheDroits;
	}

	public void processDroitsPropriete(long importId, int nbThreads, Iterator<EigentumAnteil> iterator, boolean importInitial, @Nullable StatusManager statusManager) {

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les droits de propriété... (regroupement)");
		}

		// Note: on utilise un cache persistant pour limiter l'utilisation mémoire, mais on est pas
		//       du tout intéressé par le côté persistent des données, on commence donc par tout effacer.
		cacheDroits.clear();

		// on regroupe tous les droits par ayant-droit
		groupByAyantDroit(iterator, cacheDroits);

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

	private void groupByAyantDroit(Iterator<EigentumAnteil> iterator, PersistentCache<ArrayList<EigentumAnteil>> cacheDroits) {
		while (iterator.hasNext()) {
			final EigentumAnteil eigentumAnteil = iterator.next();
			if (eigentumAnteil == null) {
				break;
			}
			if (blacklistRFHelper.isBlacklisted(eigentumAnteil.getBelastetesGrundstueckIDREF()) ||  // fond servant
					(eigentumAnteil instanceof GrundstueckEigentumAnteil                            // fond dominant
							&& blacklistRFHelper.isBlacklisted(((GrundstueckEigentumAnteil) eigentumAnteil).getBerechtigtesGrundstueckIDREF()))) {
				// on ignore les droits sur les immeubles blacklistés
				continue;
			}
			final String idRF = DroitRFHelper.getAyantDroitIdRF(eigentumAnteil);
			final IdRfCacheKey key = new IdRfCacheKey(idRF);
			ArrayList<EigentumAnteil> list = cacheDroits.get(key);
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
	private void detecterMutationsDroitsPropriete(@NotNull String idRF, @NotNull List<EigentumAnteil> droits, @NotNull EvenementRFImport parentImport, boolean importInitial) {

		final AyantDroitRF ayantDroit = ayantDroitRFDAO.find(new AyantDroitRFKey(idRF), FlushMode.MANUAL);
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
			final Set<DroitProprieteRF> activesDroits = ayantDroit.getDroitsPropriete().stream()
					.filter(d -> d.isValidAt(null))
					.collect(Collectors.toSet());

			//noinspection StatementWithEmptyBody
			if (!DroitRFHelper.dataEquals(activesDroits, droits)) {
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
				.filter(PersonEigentumAnteil.class::isInstance)
				.map(PersonEigentumAnteil.class::cast)
				.map(PersonEigentumAnteil::getGemeinschaft)
				.filter(Objects::nonNull)
				.forEach(g -> processAyantDroit(parentImport, g));

		// on traite aussi tous les immeubles dominants que l'on trouve

		// Note: dans l'export du registre foncier, les immeubles dominants ne sont pas fournis dans la liste des propriétéaires.
		//       A la place, ils sont fournis comme simple référence (IDRef) dans les droits entre immeubles (GrundstueckEigentumAnteilType).
		//       Dans Unireg, les immeubles dominants sont considérés comme des ayants-droits à part entière et doivent être
		//       persistés dans la base.
		droits.stream()
				.filter(GrundstueckEigentumAnteil.class::isInstance)
				.map(GrundstueckEigentumAnteil.class::cast)
				.map(GrundstueckEigentumAnteil::getBerechtigtesGrundstueckIDREF)
				.filter(idRf -> !blacklistRFHelper.isBlacklisted(idRf))
				.map(DroitRFDetector::newDummyGrundstueck)
				.forEach(g -> processAyantDroit(parentImport, g));
	}

	@NotNull
	private static Grundstueck newDummyGrundstueck(@NotNull String idRF) {
		// note: la seule information qui nous intéresse ici est l'idRF, on utilise donc l'immeuble inconnu
		// pour éviter de devoir résoudre le type d'immeuble exacte.
		final Grundstueck g = new UnbekanntesGrundstueck();
		g.setGrundstueckID(idRF);
		return g;
	}

	private <T> void detectMutationsDeSuppression(PersistentCache<ArrayList<T>> cacheDroits, TypeDroit typeDroit, long importId) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(s -> {
			final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

			final Set<String> existingAyantsDroits = ayantDroitRFDAO.findAvecDroitsActifs(typeDroit);
			final Set<String> nouveauAyantsDroits = cacheDroits.keySet().stream()
					.map(IdRfCacheKey.class::cast)
					.map(IdRfCacheKey::getIdRF)
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
