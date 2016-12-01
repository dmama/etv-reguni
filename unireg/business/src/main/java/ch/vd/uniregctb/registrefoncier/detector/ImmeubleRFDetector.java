package ch.vd.uniregctb.registrefoncier.detector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
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
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.GrundstueckNummerElement;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.helper.ImmeubleRFHelper;
import ch.vd.uniregctb.registrefoncier.key.CommuneRFKey;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class ImmeubleRFDetector {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImmeubleRFDetector.class);

	private final int batchSize;
	private final XmlHelperRF xmlHelperRF;
	private final ImmeubleRFDAO immeubleRFDAO;
	private final CommuneRFDAO communeRFDAO;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;

	public ImmeubleRFDetector(XmlHelperRF xmlHelperRF,
	                          ImmeubleRFDAO immeubleRFDAO,
	                          CommuneRFDAO communeRFDAO,
	                          EvenementRFImportDAO evenementRFImportDAO,
	                          EvenementRFMutationDAO evenementRFMutationDAO,
	                          PlatformTransactionManager transactionManager) {
		this(20, xmlHelperRF, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
	}

	public ImmeubleRFDetector(int batchSize,
	                          XmlHelperRF xmlHelperRF,
	                          ImmeubleRFDAO immeubleRFDAO,
	                          CommuneRFDAO communeRFDAO,
	                          EvenementRFImportDAO evenementRFImportDAO,
	                          EvenementRFMutationDAO evenementRFMutationDAO,
	                          PlatformTransactionManager transactionManager) {
		this.batchSize = batchSize;
		this.xmlHelperRF = xmlHelperRF;
		this.immeubleRFDAO = immeubleRFDAO;
		this.communeRFDAO = communeRFDAO;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;
	}

	public void processImmeubles(long importId, final int nbThreads, @NotNull Iterator<Grundstueck> iterator, @Nullable StatusManager statusManager) {

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les immeubles...");
		}

		final ParallelBatchTransactionTemplate<Grundstueck> template = new ParallelBatchTransactionTemplate<Grundstueck>(iterator, batchSize, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, AuthenticationInterface.INSTANCE) {
			@Override
			protected int getBlockingQueueCapacity() {
				// on limite la queue interne du template à 10 lots de BATCH_SIZE, autrement
				// on sature rapidemment la mémoire de la JVM avec l'entier du fichier d'import.
				return 10;
			}
		};

		final Map<Integer, String> communes = Collections.synchronizedMap(new HashMap<>());
		final AtomicInteger processed = new AtomicInteger();

		// on détecte les mutations sur les immeubles
		template.execute(new BatchCallback<Grundstueck>() {

			private final ThreadLocal<Grundstueck> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Grundstueck> batch) throws Exception {
				first.set(batch.get(0));

				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				for (Grundstueck immeuble : batch) {

					if (immeuble.isIstKopie()) {
						// on ignore les bâtiments flaggés comme des copies
						continue;
					}

					// on renseigne la map des communes pour détecter les éventuelles mutations (en fin de traitement)
					final int noRf = immeuble.getGrundstueckNummer().getBfsNr();
					final String nom = immeuble.getGrundstueckNummer().getGemeindenamen();
					communes.putIfAbsent(noRf, nom);

					// on va voir si l'immeuble existe dans la base
					final ImmeubleRFKey key = ImmeubleRFHelper.newImmeubleRFKey(immeuble);
					final ImmeubleRF immeubleRF = immeubleRFDAO.find(key);

					// on détermine ce qu'il faut faire
					final TypeMutationRF typeMutation;
					if (immeubleRF == null) {
						typeMutation = TypeMutationRF.CREATION;
					}
					else if (!ImmeubleRFHelper.currentDataEquals(immeubleRF, immeuble)) {
						typeMutation = TypeMutationRF.MODIFICATION;
					}
					else {
						// rien à faire
						continue;
					}

					// on ajoute l'événement à traiter
					final String immeubleAsXml = xmlHelperRF.toXMLString(immeuble);

					final EvenementRFMutation mutation = new EvenementRFMutation();
					mutation.setParentImport(parentImport);
					mutation.setEtat(EtatEvenementRF.A_TRAITER);
					mutation.setTypeEntite(TypeEntiteRF.IMMEUBLE);
					mutation.setTypeMutation(typeMutation);
					mutation.setIdRF(key.getIdRF());
					mutation.setXmlContent(immeubleAsXml);

					evenementRFMutationDAO.save(mutation);
				}

				processed.addAndGet(batch.size());
				if (statusManager != null) {
					statusManager.setMessage("Détection des mutations sur les immeubles... (" + processed.get() + " processés)");
				}

				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.error("Exception sur le traitement de l'immeuble idRF=[" + first.get().getGrundstueckID() + "]", e);
					throw new RuntimeException(e);
				}
			}
		}, null);

		// on détecte les mutations sur les communes (ajout, fusion, annexion par milice armée, ...)
		final TransactionTemplate t2 = new TransactionTemplate(transactionManager);
		t2.execute(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				communes.entrySet().forEach(e -> {

					final Integer noRf = e.getKey();
					final String nom = e.getValue();

					final CommuneRF communeRF = communeRFDAO.findActive(new CommuneRFKey(noRf));

					final TypeMutationRF typeMutation;
					if (communeRF == null) {
						typeMutation = TypeMutationRF.CREATION;
					}
					else if (!Objects.equals(communeRF.getNomRf(), nom)) {
						typeMutation = TypeMutationRF.MODIFICATION;
					}
					else {
						// rien à faire
						return;
					}

					// on ajoute l'événement à traiter
					final String communeAsXml = xmlHelperRF.toXMLString(new GrundstueckNummerElement(noRf, nom));

					final EvenementRFMutation mutation = new EvenementRFMutation();
					mutation.setParentImport(parentImport);
					mutation.setEtat(EtatEvenementRF.A_TRAITER);
					mutation.setTypeEntite(TypeEntiteRF.COMMUNE);
					mutation.setTypeMutation(typeMutation);
					mutation.setIdRF(String.valueOf(noRf));
					mutation.setXmlContent(communeAsXml);

					evenementRFMutationDAO.save(mutation);
				});
			}
		});
	}
}
