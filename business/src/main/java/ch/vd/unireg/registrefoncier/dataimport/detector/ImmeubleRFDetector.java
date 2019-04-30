package ch.vd.unireg.registrefoncier.dataimport.detector;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.dao.CommuneRFDAO;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.GrundstueckNummerElement;
import ch.vd.unireg.registrefoncier.dataimport.helper.BlacklistRFHelper;
import ch.vd.unireg.registrefoncier.dataimport.helper.ImmeubleRFHelper;
import ch.vd.unireg.registrefoncier.key.CommuneRFKey;
import ch.vd.unireg.registrefoncier.key.ImmeubleRFKey;

public class ImmeubleRFDetector {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImmeubleRFDetector.class);

	private final int batchSize;
	private final XmlHelperRF xmlHelperRF;
	private final BlacklistRFHelper blacklistRFHelper;
	private final ImmeubleRFDAO immeubleRFDAO;
	private final CommuneRFDAO communeRFDAO;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;

	public ImmeubleRFDetector(XmlHelperRF xmlHelperRF,
	                          BlacklistRFHelper blacklistRFHelper,
	                          ImmeubleRFDAO immeubleRFDAO,
	                          CommuneRFDAO communeRFDAO,
	                          EvenementRFImportDAO evenementRFImportDAO,
	                          EvenementRFMutationDAO evenementRFMutationDAO,
	                          PlatformTransactionManager transactionManager) {
		this(20, xmlHelperRF, blacklistRFHelper, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
	}

	public ImmeubleRFDetector(int batchSize,
	                          XmlHelperRF xmlHelperRF,
	                          BlacklistRFHelper blacklistRFHelper,
	                          ImmeubleRFDAO immeubleRFDAO,
	                          CommuneRFDAO communeRFDAO,
	                          EvenementRFImportDAO evenementRFImportDAO,
	                          EvenementRFMutationDAO evenementRFMutationDAO,
	                          PlatformTransactionManager transactionManager) {
		this.batchSize = batchSize;
		this.xmlHelperRF = xmlHelperRF;
		this.blacklistRFHelper = blacklistRFHelper;
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

		final ParallelBatchTransactionTemplate<Grundstueck> template = new ParallelBatchTransactionTemplate<Grundstueck>(iterator,
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

		final Set<String> immeubles = Collections.synchronizedSet(new HashSet<>());
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
					if (blacklistRFHelper.isBlacklisted(immeuble.getGrundstueckID())) {
						// on ignore les bâtiments blacklistés
						continue;
					}

					// on mémorise l'id RF de l'immeuble pour détecter les radiations d'immeubles (en fin de traitement)
					immeubles.add(immeuble.getGrundstueckID());

					// on renseigne la map des communes pour détecter les éventuelles mutations (en fin de traitement)
					final int noRf = immeuble.getGrundstueckNummer().getBfsNr();
					final String nom = immeuble.getGrundstueckNummer().getGemeindenamen();
					communes.putIfAbsent(noRf, nom);

					// on va voir si l'immeuble existe dans la base
					final ImmeubleRFKey key = ImmeubleRFHelper.newImmeubleRFKey(immeuble);
					final ImmeubleRF immeubleRF = immeubleRFDAO.find(key, FlushMode.MANUAL);

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

		if (statusManager != null && statusManager.isInterrupted()) {
			return;
		}

		// on détecte les radiations d'immeubles
		final TransactionTemplate t1 = new TransactionTemplate(transactionManager);
		t1.execute(status -> {
			final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

			// on détecte les immeubles actifs dans la base qui ne sont pas présents dans l'import
			final Set<String> persisted = immeubleRFDAO.findImmeublesActifs();
			persisted.removeAll(immeubles);

			// ... et on crée des mutations de SUPPRESSION dessus
			persisted.forEach(idRf -> {
				final EvenementRFMutation mutation = new EvenementRFMutation();
				mutation.setParentImport(parentImport);
				mutation.setEtat(EtatEvenementRF.A_TRAITER);
				mutation.setTypeEntite(TypeEntiteRF.IMMEUBLE);
				mutation.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mutation.setIdRF(idRf);
				mutation.setXmlContent(null);
				evenementRFMutationDAO.save(mutation);
			});
			return null;
		});
		immeubles.clear();

		if (statusManager != null && statusManager.isInterrupted()) {
			return;
		}

		// on détecte les mutations sur les communes (ajout, fusion, annexion par milice armée, ...)
		final TransactionTemplate t2 = new TransactionTemplate(transactionManager);
		t2.execute(status -> {
			final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

			communes.forEach((noRf, nom) -> {

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
			return null;
		});
	}
}
