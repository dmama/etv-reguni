package ch.vd.uniregctb.registrefoncier.dataimport.detector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.GrundstueckExport;
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
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.SurfaceAuSolRFHelper;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

public class SurfaceAuSolRFDetector {

	private static final Logger LOGGER = LoggerFactory.getLogger(SurfaceAuSolRFDetector.class);

	private final int batchSize;
	private final XmlHelperRF xmlHelperRF;
	private final ImmeubleRFDAO immeubleRFDAO;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;

	private final PersistentCache<ArrayList<Bodenbedeckung>> cacheSurfaces;

	public SurfaceAuSolRFDetector(XmlHelperRF xmlHelperRF,
	                              ImmeubleRFDAO immeubleRFDAO,
	                              EvenementRFImportDAO evenementRFImportDAO,
	                              EvenementRFMutationDAO evenementRFMutationDAO,
	                              PlatformTransactionManager transactionManager,
	                              PersistentCache<ArrayList<Bodenbedeckung>> cacheSurfaces) {
		this(20, xmlHelperRF, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, cacheSurfaces);
	}

	public SurfaceAuSolRFDetector(int batchSize,
	                              XmlHelperRF xmlHelperRF,
	                              ImmeubleRFDAO immeubleRFDAO,
	                              EvenementRFImportDAO evenementRFImportDAO,
	                              EvenementRFMutationDAO evenementRFMutationDAO,
	                              PlatformTransactionManager transactionManager,
	                              PersistentCache<ArrayList<Bodenbedeckung>> cacheSurfaces) {
		this.batchSize = batchSize;
		this.xmlHelperRF = xmlHelperRF;
		this.immeubleRFDAO = immeubleRFDAO;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;
		this.cacheSurfaces = cacheSurfaces;
	}

	public void processSurfaces(long importId, int nbThreads, Iterator<Bodenbedeckung> iterator, @Nullable StatusManager statusManager) {

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les surfaces... (regroupement)");
		}

		// on regroupe toutes les droits
		// Note: on utilise un cache persistant pour limiter l'utilisation mémoire, mais on est pas
		//       du tout intéressé par le côté persistent des données, on commence donc par tout effacer.
		cacheSurfaces.clear();
		while (iterator.hasNext()) {
			final Bodenbedeckung bodenbedeckung = iterator.next();
			if (bodenbedeckung == null) {
				break;
			}

			final String idRF = bodenbedeckung.getGrundstueckIDREF();
			final IdRfCacheKey key = new IdRfCacheKey(idRF);
			ArrayList<Bodenbedeckung> list = cacheSurfaces.get(key);
			if (list == null) {
				list = new ArrayList<>();
			}
			list.add(bodenbedeckung);
			cacheSurfaces.put(key, list);
		}

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les surfaces...", 50);
		}

		// on détecte les mutations qui doivent être générées
		final ParallelBatchTransactionTemplate<Map.Entry<ObjectKey, ArrayList<Bodenbedeckung>>> template
				= new ParallelBatchTransactionTemplate<Map.Entry<ObjectKey, ArrayList<Bodenbedeckung>>>(cacheSurfaces.entrySet().iterator(), batchSize, nbThreads,
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

		template.execute(new BatchCallback<Map.Entry<ObjectKey, ArrayList<Bodenbedeckung>>>() {

			private final ThreadLocal<Map.Entry<ObjectKey, ArrayList<Bodenbedeckung>>> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Map.Entry<ObjectKey, ArrayList<Bodenbedeckung>>> batch) throws Exception {
				first.set(batch.get(0));

				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				batch.forEach(e -> {

					final String idRF = ((IdRfCacheKey) e.getKey()).getIdRF();
					final List<Bodenbedeckung> nouvellesSurfaces = e.getValue();

					final ImmeubleRF immeuble = immeubleRFDAO.find(new ImmeubleRFKey(idRF), FlushMode.MANUAL);
					if (immeuble == null) {
						// l'immeuble n'existe pas : il va être créé et on doit donc sauver une mutation en mode création.
						final EvenementRFMutation mut = new EvenementRFMutation();
						mut.setParentImport(parentImport);
						mut.setEtat(EtatEvenementRF.A_TRAITER);
						mut.setTypeEntite(TypeEntiteRF.SURFACE_AU_SOL);
						mut.setIdRF(idRF);
						mut.setTypeMutation(TypeMutationRF.CREATION);
						mut.setXmlContent(xmlHelperRF.toXMLString(new GrundstueckExport.BodenbedeckungList(nouvellesSurfaces)));
						evenementRFMutationDAO.save(mut);
					}
					else {
						// on récupère les surfaces actives actuelles
						final Set<SurfaceAuSolRF> activesSurfaces = immeuble.getSurfacesAuSol().stream()
								.filter(s -> s.isValidAt(null))
								.collect(Collectors.toSet());

						//noinspection StatementWithEmptyBody
						if (!SurfaceAuSolRFHelper.dataEquals(activesSurfaces, nouvellesSurfaces)) {
							// les surfaces sont différentes : on sauve une mutation en mode modification
							final EvenementRFMutation mut = new EvenementRFMutation();
							mut.setParentImport(parentImport);
							mut.setEtat(EtatEvenementRF.A_TRAITER);
							mut.setTypeEntite(TypeEntiteRF.SURFACE_AU_SOL);
							mut.setIdRF(idRF);
							mut.setTypeMutation(TypeMutationRF.MODIFICATION);
							mut.setXmlContent(xmlHelperRF.toXMLString(new GrundstueckExport.BodenbedeckungList(nouvellesSurfaces)));
							evenementRFMutationDAO.save(mut);
						}
						else {
							// les surfaces sont égales : rien à faire
						}
					}

				});

				processed.addAndGet(batch.size());
				if (statusManager != null) {
					statusManager.setMessage("Détection des mutations sur les surfaces... (" + processed.get() + " processées)", 50);
				}

				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.error("Exception sur le traitement des surfaces au sol de l'immeuble idRF=[" + first.get().getKey() + "]", e);
					throw new RuntimeException(e);
				}
			}
		}, null);

		// détection des mutations de type SUPRESSION
		final TransactionTemplate t1 = new TransactionTemplate(transactionManager);
		t1.execute(s -> {
			final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

			final Set<String> existingImmeublesAvecSurfaces = immeubleRFDAO.findWithActiveSurfacesAuSol();
			//noinspection UnnecessaryLocalVariable
			final Set<String> nouveauImmeublesAvecSurfaces = cacheSurfaces.keySet().stream()
					.map(IdRfCacheKey.class::cast)
					.map(IdRfCacheKey::getIdRF)
					.collect(Collectors.toSet());

			// on ne garde que les bâtiments existants dans la DB qui n'existent pas dans le fichier XML
			existingImmeublesAvecSurfaces.removeAll(nouveauImmeublesAvecSurfaces);

			// on crée des mutations de suppression pour les surfaces de tous ces bâtiments
			existingImmeublesAvecSurfaces.forEach(idRF -> {
				final EvenementRFMutation mutation = new EvenementRFMutation();
				mutation.setParentImport(parentImport);
				mutation.setEtat(EtatEvenementRF.A_TRAITER);
				mutation.setTypeEntite(TypeEntiteRF.SURFACE_AU_SOL);
				mutation.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mutation.setIdRF(idRF); // idRF du bâtiment
				mutation.setXmlContent(null);
				evenementRFMutationDAO.save(mutation);
			});

			return null;
		});

		// inutile de garder des données en mémoire ou sur le disque
		cacheSurfaces.clear();
	}
}
