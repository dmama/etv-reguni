package ch.vd.uniregctb.registrefoncier.dataimport.detector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.common.Rechteinhaber;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
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
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.elements.PersonEigentumAnteilListElement;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.DroitRFHelper;
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

	public DroitRFDetector(XmlHelperRF xmlHelperRF,
	                       AyantDroitRFDAO ayantDroitRFDAO,
	                       EvenementRFImportDAO evenementRFImportDAO,
	                       EvenementRFMutationDAO evenementRFMutationDAO,
	                       PlatformTransactionManager transactionManager,
	                       AyantDroitRFDetector ayantDroitRFDetector,
	                       PersistentCache<ArrayList<PersonEigentumAnteil>> cacheDroits) {
		this(20, xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits);
	}

	public DroitRFDetector(int batchSize,
	                       XmlHelperRF xmlHelperRF,
	                       AyantDroitRFDAO ayantDroitRFDAO,
	                       EvenementRFImportDAO evenementRFImportDAO,
	                       EvenementRFMutationDAO evenementRFMutationDAO,
	                       PlatformTransactionManager transactionManager,
	                       AyantDroitRFDetector ayantDroitRFDetector,
	                       PersistentCache<ArrayList<PersonEigentumAnteil>> cacheDroits) {
		this.batchSize = batchSize;
		this.xmlHelperRF = xmlHelperRF;
		this.ayantDroitRFDAO = ayantDroitRFDAO;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;
		this.ayantDroitRFDetector = ayantDroitRFDetector;
		this.cacheDroits = cacheDroits;
	}

	public void processDroits(long importId, int nbThreads, Iterator<PersonEigentumAnteil> iterator, @Nullable StatusManager statusManager) {

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les droits... (regroupement)");
		}

		// on regroupe toutes les droits
		cacheDroits.clear();    // on utilise un cache persistant pour limiter l'utilisation mémoire, mais on est pas
		// du tout intéressé par le côté persistent des données, on commence donc par tout effacer.
		while (iterator.hasNext()) {
			final PersonEigentumAnteil eigentumAnteil = iterator.next();
			if (eigentumAnteil == null) {
				break;
			}

			final String idRF = DroitRFHelper.getIdRF(eigentumAnteil);
			final IdRfCacheKey key = new IdRfCacheKey(idRF);
			ArrayList<PersonEigentumAnteil> list = cacheDroits.get(key);
			if (list == null) {
				list = new ArrayList<>();
			}
			list.add(eigentumAnteil);
			cacheDroits.put(key, list);
		}

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les droits...", 50);
		}

		// on détecte les mutations qui doivent être générées
		final ParallelBatchTransactionTemplate<Map.Entry<ObjectKey, ArrayList<PersonEigentumAnteil>>> template
				= new ParallelBatchTransactionTemplate<Map.Entry<ObjectKey, ArrayList<PersonEigentumAnteil>>>(cacheDroits.entrySet().iterator(), batchSize, nbThreads,
				                                                                                              Behavior.REPRISE_AUTOMATIQUE, transactionManager, null,
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
		template.execute(new BatchCallback<Map.Entry<ObjectKey, ArrayList<PersonEigentumAnteil>>>() {

			private final ThreadLocal<Map.Entry<ObjectKey, ArrayList<PersonEigentumAnteil>>> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Map.Entry<ObjectKey, ArrayList<PersonEigentumAnteil>>> batch) throws Exception {
				first.set(batch.get(0));

				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				batch.forEach(e -> {
					final String idRF = ((IdRfCacheKey) e.getKey()).getIdRF();
					final List<PersonEigentumAnteil> nouveauxDroits = e.getValue();

					final AyantDroitRF ayantDroit = ayantDroitRFDAO.find(new AyantDroitRFKey(idRF));
					if (ayantDroit == null) {
						// l'ayant-droit n'existe pas : il va être créé et on doit donc sauver une mutation en mode création.
						final EvenementRFMutation mutation = new EvenementRFMutation();
						mutation.setParentImport(parentImport);
						mutation.setEtat(EtatEvenementRF.A_TRAITER);
						mutation.setTypeEntite(TypeEntiteRF.DROIT);
						mutation.setTypeMutation(TypeMutationRF.CREATION);
						mutation.setIdRF(idRF); // idRF de l'ayant-droit
						mutation.setXmlContent(xmlHelperRF.toXMLString(new PersonEigentumAnteilListElement(nouveauxDroits)));
						evenementRFMutationDAO.save(mutation);
					}
					else {
						// on récupère les droits actifs actuels
						final Set<DroitRF> activesDroits = ayantDroit.getDroits().stream()
								.filter(s -> s.isValidAt(null))
								.collect(Collectors.toSet());

						//noinspection StatementWithEmptyBody
						if (!DroitRFHelper.dataEquals(activesDroits, nouveauxDroits)) {
							// les droits sont différents : on sauve une mutation en mode modification
							final EvenementRFMutation mutation = new EvenementRFMutation();
							mutation.setParentImport(parentImport);
							mutation.setEtat(EtatEvenementRF.A_TRAITER);
							mutation.setTypeEntite(TypeEntiteRF.DROIT);
							mutation.setTypeMutation(TypeMutationRF.MODIFICATION);
							mutation.setIdRF(idRF); // idRF de l'ayant-droit
							mutation.setXmlContent(xmlHelperRF.toXMLString(new PersonEigentumAnteilListElement(nouveauxDroits)));
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
					nouveauxDroits.stream()
							.map(PersonEigentumAnteil::getGemeinschaft)
							.filter(g -> g != null)
							.forEach(g -> processAyantDroit(parentImport, g));

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

		// détection des mutations de type SUPRESSION
		final TransactionTemplate t1 = new TransactionTemplate(transactionManager);
		t1.execute(s -> {
			final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

			final Set<String> existingAyantsDroits = ayantDroitRFDAO.findAvecDroitsActifs();
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
				mutation.setTypeEntite(TypeEntiteRF.DROIT);
				mutation.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mutation.setIdRF(idRF); // idRF de l'ayant-droit
				mutation.setXmlContent(null);
				evenementRFMutationDAO.save(mutation);
			});

			return null;
		});

		// inutile de garder des données en mémoire ou sur le disque
		cacheDroits.clear();
	}

	private void processAyantDroit(EvenementRFImport parentImport, Rechteinhaber rechteinhaber) {
		ayantDroitRFDetector.processAyantDroit(parentImport, rechteinhaber);
	}
}
