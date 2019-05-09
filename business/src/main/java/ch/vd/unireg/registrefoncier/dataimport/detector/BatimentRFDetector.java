package ch.vd.unireg.registrefoncier.dataimport.detector;

import javax.persistence.FlushModeType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.capitastra.grundstueck.Gebaeude;
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
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.dao.BatimentRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.dataimport.helper.BatimentRFHelper;
import ch.vd.unireg.registrefoncier.key.BatimentRFKey;

public class BatimentRFDetector {

	private static final Logger LOGGER = LoggerFactory.getLogger(BatimentRFDetector.class);

	private final int batchSize;
	private final XmlHelperRF xmlHelperRF;
	private final BatimentRFDAO batimentRFDAO;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;

	public BatimentRFDetector(XmlHelperRF xmlHelperRF,
	                          BatimentRFDAO batimentRFDAO,
	                          EvenementRFImportDAO evenementRFImportDAO,
	                          EvenementRFMutationDAO evenementRFMutationDAO,
	                          PlatformTransactionManager transactionManager) {
		this(20, xmlHelperRF, batimentRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
	}

	public BatimentRFDetector(int batchSize, XmlHelperRF xmlHelperRF, BatimentRFDAO batimentRFDAO, EvenementRFImportDAO evenementRFImportDAO, EvenementRFMutationDAO evenementRFMutationDAO,
	                          PlatformTransactionManager transactionManager) {
		this.batchSize = batchSize;
		this.xmlHelperRF = xmlHelperRF;
		this.batimentRFDAO = batimentRFDAO;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;
	}

	public void processBatiments(long importId, int nbThreads, Iterator<Gebaeude> iterator, @Nullable StatusManager statusManager) {

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les bâtiments...");
		}

		final ParallelBatchTransactionTemplate<Gebaeude> template = new ParallelBatchTransactionTemplate<Gebaeude>(iterator,
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

		final Set<String> batimentIdsRF = Collections.synchronizedSet(new HashSet<>());
		final AtomicInteger processed = new AtomicInteger();

		// on détecte les mutations sur les immeubles
		template.execute(new BatchCallback<Gebaeude>() {

			private final ThreadLocal<Gebaeude> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Gebaeude> batch) throws Exception {
				first.set(batch.get(0));

				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				for (Gebaeude gebaeude : batch) {

					batimentIdsRF.add(gebaeude.getMasterID());

					// on va voir si le bâtiment existe dans la base
					final BatimentRFKey key = BatimentRFHelper.newBatimentRFKey(gebaeude);
					final BatimentRF batiment = batimentRFDAO.find(key, FlushModeType.COMMIT);

					// on détermine ce qu'il faut faire
					final TypeMutationRF typeMutation;
					if (batiment == null) {
						typeMutation = TypeMutationRF.CREATION;
					}
					else if (!BatimentRFHelper.currentDataEquals(batiment, gebaeude)) {
						typeMutation = TypeMutationRF.MODIFICATION;
					}
					else {
						// rien à faire
						continue;
					}

					// on ajoute l'événement à traiter
					final String batimentAsXml = xmlHelperRF.toXMLString(gebaeude);

					final EvenementRFMutation mutation = new EvenementRFMutation();
					mutation.setParentImport(parentImport);
					mutation.setEtat(EtatEvenementRF.A_TRAITER);
					mutation.setTypeEntite(TypeEntiteRF.BATIMENT);
					mutation.setTypeMutation(typeMutation);
					mutation.setIdRF(key.getMasterIdRF());
					mutation.setXmlContent(batimentAsXml);

					evenementRFMutationDAO.save(mutation);
				}

				processed.addAndGet(batch.size());
				if (statusManager != null) {
					statusManager.setMessage("Détection des mutations sur les bâtiments... (" + processed.get() + " processés)");
				}

				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.error("Exception sur le traitement du bâtiment masterIdRF=[" + first.get().getMasterID() + "]", e);
					throw new RuntimeException(e);
				}
			}
		}, null);

		// détection des mutations de type SUPRESSION
		final TransactionTemplate t1 = new TransactionTemplate(transactionManager);
		t1.execute(s -> {
			final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

			final Set<String> existingBatiments = batimentRFDAO.findActifs();
			//noinspection UnnecessaryLocalVariable
			final Set<String> nouveauBatiments = batimentIdsRF;

			// on ne garde que les bâtiments existants dans la DB qui n'existent pas dans le fichier XML
			existingBatiments.removeAll(nouveauBatiments);

			// on crée des mutations de suppression pour tous ces bâtiments
			existingBatiments.forEach(idRF -> {
				final EvenementRFMutation mutation = new EvenementRFMutation();
				mutation.setParentImport(parentImport);
				mutation.setEtat(EtatEvenementRF.A_TRAITER);
				mutation.setTypeEntite(TypeEntiteRF.BATIMENT);
				mutation.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mutation.setIdRF(idRF); // idRF du bâtiment
				mutation.setXmlContent(null);
				evenementRFMutationDAO.save(mutation);
			});

			return null;
		});
	}
}
