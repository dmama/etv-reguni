package ch.vd.unireg.registrefoncier.dataimport.detector;

import javax.persistence.FlushModeType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
import ch.vd.shared.batchtemplate.Interruptible;
import ch.vd.unireg.cache.ObjectKey;
import ch.vd.unireg.cache.PersistentCache;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.ParallelBatchTransactionTemplate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImport;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.TypeDroit;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.EigentumAnteilListElement;
import ch.vd.unireg.registrefoncier.dataimport.helper.BlacklistRFHelper;
import ch.vd.unireg.registrefoncier.dataimport.helper.DroitRFHelper;
import ch.vd.unireg.registrefoncier.key.ImmeubleRFKey;

public class DroitRFDetector {

	private static final Logger LOGGER = LoggerFactory.getLogger(DroitRFDetector.class);

	private final int batchSize;
	private final XmlHelperRF xmlHelperRF;
	private final BlacklistRFHelper blacklistRFHelper;
	private final ImmeubleRFDAO immeubleRFDAO;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;
	private final AyantDroitRFDetector ayantDroitRFDetector;

	private final PersistentCache<ArrayList<EigentumAnteil>> cacheDroits;

	public DroitRFDetector(XmlHelperRF xmlHelperRF,
	                       BlacklistRFHelper blacklistRFHelper,
	                       ImmeubleRFDAO immeubleRFDAO,
	                       EvenementRFImportDAO evenementRFImportDAO,
	                       EvenementRFMutationDAO evenementRFMutationDAO,
	                       PlatformTransactionManager transactionManager,
	                       AyantDroitRFDetector ayantDroitRFDetector,
	                       PersistentCache<ArrayList<EigentumAnteil>> cacheDroits) {
		this(20, xmlHelperRF, blacklistRFHelper, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits);
	}

	public DroitRFDetector(int batchSize,
	                       XmlHelperRF xmlHelperRF,
	                       BlacklistRFHelper blacklistRFHelper,
	                       ImmeubleRFDAO immeubleRFDAO,
	                       EvenementRFImportDAO evenementRFImportDAO,
	                       EvenementRFMutationDAO evenementRFMutationDAO,
	                       PlatformTransactionManager transactionManager,
	                       AyantDroitRFDetector ayantDroitRFDetector,
	                       PersistentCache<ArrayList<EigentumAnteil>> cacheDroits) {
		this.batchSize = batchSize;
		this.xmlHelperRF = xmlHelperRF;
		this.blacklistRFHelper = blacklistRFHelper;
		this.immeubleRFDAO = immeubleRFDAO;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;
		this.ayantDroitRFDetector = ayantDroitRFDetector;
		this.cacheDroits = cacheDroits;
	}

	public void processDroitsPropriete(long importId, int nbThreads, Iterator<EigentumAnteil> iterator, @Nullable StatusManager statusManager) {

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les droits de propriété... (regroupement)");
		}

		// Note: on utilise un cache persistant pour limiter l'utilisation mémoire, mais on est pas
		//       du tout intéressé par le côté persistent des données, on commence donc par tout effacer.
		cacheDroits.clear();

		// on regroupe tous les droits par immeubles
		groupByImmeuble(iterator, cacheDroits, statusManager);

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les droits de propriété...", 50);
		}

		if (statusManager != null && statusManager.isInterrupted()) {
			return;
		}

		// on détecte les mutations qui doivent être générées
		forEachImmeuble(cacheDroits, this::detecterMutationsDroitsPropriete, importId, nbThreads, statusManager);

		if (statusManager != null && statusManager.isInterrupted()) {
			return;
		}

		// détection des mutations de type SUPRESSION
		detectMutationsDeSuppression(cacheDroits, TypeDroit.DROIT_PROPRIETE, importId);

		// inutile de garder des données en mémoire ou sur le disque
		cacheDroits.clear();
	}

	private void groupByImmeuble(Iterator<EigentumAnteil> iterator, PersistentCache<ArrayList<EigentumAnteil>> cacheDroits, @Nullable Interruptible interruptible) {
		while (iterator.hasNext()) {
			final EigentumAnteil eigentumAnteil = iterator.next();
			if (eigentumAnteil == null) {
				break;
			}
			if (interruptible != null && interruptible.isInterrupted()) {
				break;
			}
			if (blacklistRFHelper.isBlacklisted(eigentumAnteil.getBelastetesGrundstueckIDREF()) ||  // fond servant
					(eigentumAnteil instanceof GrundstueckEigentumAnteil                            // fond dominant
							&& blacklistRFHelper.isBlacklisted(((GrundstueckEigentumAnteil) eigentumAnteil).getBerechtigtesGrundstueckIDREF()))) {
				// on ignore les droits sur les immeubles blacklistés
				continue;
			}
			final String idRF = DroitRFHelper.getImmeubleIdRF(eigentumAnteil);
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
		void apply(@NotNull String idRF, @NotNull List<T> droits, @NotNull EvenementRFImport parentImport);
	}

	private <T> void forEachImmeuble(@NotNull final PersistentCache<ArrayList<T>> cacheDroits,
	                                 @NotNull MutationsSurUnAyantDroitDetector<T> detector,
	                                 final long importId,
	                                 final int nbThreads,
	                                 @Nullable final StatusManager statusManager) {

		final ParallelBatchTransactionTemplate<Map.Entry<ObjectKey, ArrayList<T>>> template
				= new ParallelBatchTransactionTemplate<Map.Entry<ObjectKey, ArrayList<T>>>(cacheDroits.entrySet().iterator(),
				                                                                           batchSize,
				                                                                           nbThreads,
				                                                                           Behavior.REPRISE_AUTOMATIQUE,
				                                                                           transactionManager,
				                                                                           statusManager,
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
					final String idRF = ((IdRfCacheKey) e.getKey()).getIdRF();  // l'idRF de l'immeuble
					final List<T> nouveauxDroits = e.getValue();                // les droits de l'immeuble
					detector.apply(idRF, nouveauxDroits, parentImport);
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
					LOGGER.error("Exception sur le traitement des droits de l'immeuble idRF=[" + first.get().getKey() + "]", e);
					throw new RuntimeException(e);
				}
			}
		}, null);
	}

	/**
	 * Cette méthode détecte les changements (création ou update) sur les droits de propriété qui pointent vers un immeuble et crée les mutations correspondantes.
	 */
	private void detecterMutationsDroitsPropriete(@NotNull String idRF, @NotNull List<EigentumAnteil> droits, @NotNull EvenementRFImport parentImport) {

		final ImmeubleRF immeuble = immeubleRFDAO.find(new ImmeubleRFKey(idRF), FlushModeType.COMMIT);
		if (immeuble == null) {
			// l'immeuble n'existe pas : il va être créé et on doit donc sauver une mutation en mode création.
			final EvenementRFMutation mutation = new EvenementRFMutation();
			mutation.setParentImport(parentImport);
			mutation.setEtat(EtatEvenementRF.A_TRAITER);
			mutation.setTypeEntite(TypeEntiteRF.DROIT);
			mutation.setTypeMutation(TypeMutationRF.CREATION);
			mutation.setIdRF(idRF); // idRF de l'immeuble
			mutation.setXmlContent(xmlHelperRF.toXMLString(new EigentumAnteilListElement(droits)));
			evenementRFMutationDAO.save(mutation);
		}
		else {
			// on récupère les droits actifs actuels
			final Set<DroitProprieteRF> activesDroits = immeuble.getDroitsPropriete().stream()
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
				mutation.setIdRF(idRF); // idRF de l'immeuble
				mutation.setXmlContent(xmlHelperRF.toXMLString(new EigentumAnteilListElement(droits)));
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

		final Set<String> immeublesInDB = template.execute(s -> immeubleRFDAO.findAvecDroitsActifs(typeDroit));
		final Set<String> immeublesInImport = cacheDroits.keySet().stream()
				.map(IdRfCacheKey.class::cast)
				.map(IdRfCacheKey::getIdRF)
				.collect(Collectors.toSet());

		// on enlève tous les immeubles qui existent à la fois dans la DB et dans l'import
		immeublesInDB.removeAll(immeublesInImport);

		template.execute(s -> {
			// on crée des mutations de suppression de droits pour tous les immeubles qui existent dans la DB et pas dans l'import
			final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);
			immeublesInDB.forEach(idRF -> {
				final EvenementRFMutation mutation = new EvenementRFMutation();
				mutation.setParentImport(parentImport);
				mutation.setEtat(EtatEvenementRF.A_TRAITER);
				mutation.setTypeEntite(typeDroit == TypeDroit.DROIT_PROPRIETE ? TypeEntiteRF.DROIT : TypeEntiteRF.SERVITUDE);
				mutation.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mutation.setIdRF(idRF); // idRF de l'immeuble
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
