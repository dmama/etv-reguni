package ch.vd.uniregctb.registrefoncier.dataimport.detector;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.common.Rechteinhaber;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.StatusManager;
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
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.AyantDroitRFHelper;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;

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

	public void processProprietaires(long importId, int nbThreads, Iterator<Personstamm> iterator, @Nullable StatusManager statusManager) {

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les propriétaires...");
		}

		final ParallelBatchTransactionTemplate<Personstamm> template = new ParallelBatchTransactionTemplate<Personstamm>(iterator, batchSize, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, AuthenticationInterface.INSTANCE) {
			@Override
			protected int getBlockingQueueCapacity() {
				// on limite la queue interne du template à 10 lots de BATCH_SIZE, autrement
				// on sature rapidemment la mémoire de la JVM avec l'entier du fichier d'import.
				return 10;
			}
		};

		final AtomicInteger processed = new AtomicInteger();

		template.execute(new BatchCallback<Personstamm>() {

			private final ThreadLocal<Personstamm> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Personstamm> batch) throws Exception {
				first.set(batch.get(0));

				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				for (Personstamm person : batch) {
					processAyantDroit(parentImport, person);
				}

				processed.addAndGet(batch.size());
				if (statusManager != null) {
					statusManager.setMessage("Détection des mutations sur les propriétaires... (" + processed.get() + " processés)");
				}

				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.error("Exception sur le traitement du propriétaire idRF=[" + first.get().getPersonstammID() + "]", e);
					throw new RuntimeException(e);
				}
			}
		}, null);
	}

	public void processAyantDroit(EvenementRFImport parentImport, Rechteinhaber rechteinhaber) {

		final AyantDroitRFKey key = AyantDroitRFHelper.newAyantDroitKey(rechteinhaber);
		final AyantDroitRF ayantDroitRF = ayantDroitRFDAO.find(key);

		// on détermine ce qu'il faut faire
		final TypeMutationRF typeMutation;
		if (ayantDroitRF == null) {
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
}
