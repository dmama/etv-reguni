package ch.vd.unireg.registrefoncier.dataimport.detector;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.common.Rechteinhaber;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
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
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.dataimport.helper.AyantDroitRFHelper;
import ch.vd.unireg.registrefoncier.key.AyantDroitRFKey;

public class AyantDroitRFDetector {

	private static final Logger LOGGER = LoggerFactory.getLogger(AyantDroitRFDetector.class);

	private final int batchSize;
	private final XmlHelperRF xmlHelperRF;
	private final AyantDroitRFDAO ayantDroitRFDAO;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;

	public AyantDroitRFDetector(XmlHelperRF xmlHelperRF,
	                            AyantDroitRFDAO ayantDroitRFDAO,
	                            EvenementRFImportDAO evenementRFImportDAO,
	                            EvenementRFMutationDAO evenementRFMutationDAO,
	                            PlatformTransactionManager transactionManager) {
		this(20, xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
	}

	public AyantDroitRFDetector(int batchSize,
	                            XmlHelperRF xmlHelperRF,
	                            AyantDroitRFDAO ayantDroitRFDAO,
	                            EvenementRFImportDAO evenementRFImportDAO,
	                            EvenementRFMutationDAO evenementRFMutationDAO,
	                            PlatformTransactionManager transactionManager) {
		this.batchSize = batchSize;
		this.xmlHelperRF = xmlHelperRF;
		this.ayantDroitRFDAO = ayantDroitRFDAO;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;
	}

	public <T extends Rechteinhaber> void processAyantDroits(long importId, int nbThreads, Iterator<T> iterator, @Nullable StatusManager statusManager) {

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les ayant-droits...");
		}

		final ParallelBatchTransactionTemplate<T> template = new ParallelBatchTransactionTemplate<T>(iterator,
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

		template.execute(new BatchCallback<T>() {

			private final ThreadLocal<T> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<T> batch) throws Exception {
				first.set(batch.get(0));

				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				for (T person : batch) {
					processAyantDroit(parentImport, person);
				}

				processed.addAndGet(batch.size());
				if (statusManager != null) {
					statusManager.setMessage("Détection des mutations sur les ayant-droits... (" + processed.get() + " processés)");
				}

				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					final AyantDroitRFKey key = AyantDroitRFHelper.newAyantDroitKey(first.get());
					LOGGER.error("Exception sur le traitement de l'ayant-droits idRF=[" + key.getIdRF() + "]", e);
					throw new RuntimeException(e);
				}
			}
		}, null);
	}

	public void processAyantDroit(EvenementRFImport parentImport, Rechteinhaber rechteinhaber) {

		final boolean isImmeuble = (rechteinhaber instanceof Grundstueck);
		final AyantDroitRFKey key = AyantDroitRFHelper.newAyantDroitKey(rechteinhaber);
		final AyantDroitRF ayantDroitRF = ayantDroitRFDAO.find(key, FlushMode.MANUAL);

		// on détermine ce qu'il faut faire
		final TypeMutationRF typeMutation;
		if (ayantDroitRF == null) {
			if (isImmeuble && existsMutationCreation(key, parentImport)) {
				// les immeubles bénéficiaires sont créés à la volée lors de l'import des droits entre immeubles :
				// il peut arriver qu'on détecte plusieurs fois le même immeuble qui n'existe pas encore dans
				// la DB. Il faut bien évidemment le créer qu'une seule fois : on évite donc de créer plusieurs
				// mutations de création pour les immeubles (le problème ne se pose pas pour les autres types
				// d'ayants-droits, c'est pour ça qu'on ne teste que les immeubles).
				return;
			}

			typeMutation = TypeMutationRF.CREATION;
		}
		else if (!AyantDroitRFHelper.dataEquals(ayantDroitRF, rechteinhaber)) {
			typeMutation = TypeMutationRF.MODIFICATION;
		}
		else {
			// rien à faire
			return;
		}

		// on ajoute l'événement à traiter
		final String ayantDroitAsXml = xmlHelperRF.toXMLString(rechteinhaber);

		final EvenementRFMutation mutation = new EvenementRFMutation();
		mutation.setParentImport(parentImport);
		mutation.setEtat(EtatEvenementRF.A_TRAITER);
		mutation.setTypeEntite(TypeEntiteRF.AYANT_DROIT);
		mutation.setIdRF(key.getIdRF());
		mutation.setTypeMutation(typeMutation);
		mutation.setXmlContent(ayantDroitAsXml);

		evenementRFMutationDAO.save(mutation);
	}

	private boolean existsMutationCreation(@NotNull AyantDroitRFKey key, @NotNull EvenementRFImport parentImport) {
		return evenementRFMutationDAO.find(parentImport.getId(), TypeEntiteRF.AYANT_DROIT, TypeMutationRF.CREATION, key.getIdRF()) != null;
	}
}
