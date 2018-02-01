package ch.vd.uniregctb.registrefoncier.dataimport.detector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.capitastra.rechteregister.BelastetesGrundstueck;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
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
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ServitudeRF;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationsRFDetectorResults;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.elements.servitude.DienstbarkeitExtendedElement;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.ServitudesRFHelper;
import ch.vd.uniregctb.registrefoncier.key.DroitRFKey;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

public class ServitudeRFDetector {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServitudeRFDetector.class);

	private final int batchSize;
	private final XmlHelperRF xmlHelperRF;
	private final DroitRFDAO droitRFDAO;
	private final ImmeubleRFDAO immeubleRFDAO;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;

	public ServitudeRFDetector(XmlHelperRF xmlHelperRF,
	                           DroitRFDAO droitRFDAO,
	                           ImmeubleRFDAO immeubleRFDAO,
	                           EvenementRFImportDAO evenementRFImportDAO,
	                           EvenementRFMutationDAO evenementRFMutationDAO,
	                           PlatformTransactionManager transactionManager) {
		this(20, xmlHelperRF, droitRFDAO, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
	}

	public ServitudeRFDetector(int batchSize, XmlHelperRF xmlHelperRF, DroitRFDAO droitRFDAO, ImmeubleRFDAO immeubleRFDAO, EvenementRFImportDAO evenementRFImportDAO, EvenementRFMutationDAO evenementRFMutationDAO,
	                           PlatformTransactionManager transactionManager) {
		this.batchSize = batchSize;
		this.xmlHelperRF = xmlHelperRF;
		this.droitRFDAO = droitRFDAO;
		this.immeubleRFDAO = immeubleRFDAO;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;
	}

	public void processServitudes(long importId, int nbThreads, Iterator<DienstbarkeitExtendedElement> iterator, @Nullable MutationsRFDetectorResults rapport, @Nullable StatusManager statusManager) {

		if (statusManager != null) {
			statusManager.setMessage("Détection des mutations sur les servitudes...");
		}

		final ParallelBatchTransactionTemplate<DienstbarkeitExtendedElement> template = new ParallelBatchTransactionTemplate<DienstbarkeitExtendedElement>(iterator,
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

		final Set<DroitRFKey> servitudesIdsRF = Collections.synchronizedSet(new HashSet<>());
		final AtomicInteger processed = new AtomicInteger();

		// on détecte les mutations sur les immeubles
		template.execute(new BatchCallback<DienstbarkeitExtendedElement>() {

			private final ThreadLocal<DienstbarkeitExtendedElement> first = new ThreadLocal<>();
			private final ThreadLocal<List<MutationsRFDetectorResults.Avertissement>> warnings = ThreadLocal.withInitial(ArrayList::new);

			@Override
			public void beforeTransaction() {
				warnings.get().clear();
			}

			@Override
			public boolean doInTransaction(List<DienstbarkeitExtendedElement> batch) throws Exception {
				first.set(batch.get(0));

				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				for (DienstbarkeitExtendedElement dienstbarkeit : batch) {

					final String masterIDRF = dienstbarkeit.getDienstbarkeit().getMasterID();
					final String versionIDRF = dienstbarkeit.getDienstbarkeit().getVersionID();
					servitudesIdsRF.add(new DroitRFKey(masterIDRF, versionIDRF));

					// SIFISC-23744 : on renseigne les servitudes vides dans le rapport
					if (dienstbarkeit.getLastRechtGruppe().getBerechtigtePerson().isEmpty()) {
						final List<String> egrids = dienstbarkeit.getLastRechtGruppe().getBelastetesGrundstueck().stream()
								.map(BelastetesGrundstueck::getBelastetesGrundstueckIDREF)
								.map(id -> resolveEgrid(id))
								.collect(Collectors.toList());
						final String message = "La servitude standardRechtID=[" + masterIDRF + "] sur les immeubles egrids=[" + String.join(", ", egrids) + "] ne possède pas de bénéficiaire.";
						warnings.get().add(new MutationsRFDetectorResults.Avertissement(masterIDRF, String.join(", ", egrids), message));
					}

					// on va voir si la servitude existe dans la base
					final DroitRFKey key = ServitudesRFHelper.newServitudeRFKey(dienstbarkeit);
					final DroitRF droit = droitRFDAO.find(key);
					if (droit != null && !(droit instanceof ServitudeRF)) {
						throw new IllegalArgumentException("Le droit avec le masterIDRF=[" + key.getMasterIdRF() + "] n'est pas une servitude");
					}
					final ServitudeRF servitude = (ServitudeRF) droit;

					// on détermine ce qu'il faut faire
					final TypeMutationRF typeMutation;
					if (droit == null) {
						typeMutation = TypeMutationRF.CREATION;
					}
					else if (!ServitudesRFHelper.dataEquals(servitude, dienstbarkeit)) {
						typeMutation = TypeMutationRF.MODIFICATION;
					}
					else {
						// rien à faire
						continue;
					}

					// on ajoute l'événement à traiter
					final String servitudeAsXml = xmlHelperRF.toXMLString(dienstbarkeit);

					final EvenementRFMutation mutation = new EvenementRFMutation();
					mutation.setParentImport(parentImport);
					mutation.setEtat(EtatEvenementRF.A_TRAITER);
					mutation.setTypeEntite(TypeEntiteRF.SERVITUDE);
					mutation.setTypeMutation(typeMutation);
					mutation.setIdRF(key.getMasterIdRF());
					mutation.setVersionRF(key.getVersionIdRF());
					mutation.setXmlContent(servitudeAsXml);

					evenementRFMutationDAO.save(mutation);
				}

				processed.addAndGet(batch.size());
				if (statusManager != null) {
					statusManager.setMessage("Détection des mutations sur les servitudes... (" + processed.get() + " processés)");
				}

				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.error("Exception sur le traitement de la servitude masterIdRF=[" + first.get().getDienstbarkeit().getMasterID() + "]", e);
					throw new RuntimeException(e);
				}
			}

			@Override
			public void afterTransactionCommit() {
				if (rapport != null) {
					warnings.get().forEach(a -> rapport.addAvertissement(a.getIdRF(), a.getEgrid(), a.getMessage()));
				}
			}
		}, null);

		// détection des mutations de type SUPRESSION
		final TransactionTemplate t1 = new TransactionTemplate(transactionManager);
		t1.execute(s -> {
			final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

			final Set<DroitRFKey> existingServitudes = droitRFDAO.findIdsServitudesActives();
			//noinspection UnnecessaryLocalVariable
			final Set<DroitRFKey> nouvellesServitudes = servitudesIdsRF;

			// on ne garde que les servitudes existantes dans la DB qui n'existent pas dans le fichier XML
			existingServitudes.removeAll(nouvellesServitudes);

			// on crée des mutations de suppression pour tous ces servitudes
			existingServitudes.forEach(key -> {
				final EvenementRFMutation mutation = new EvenementRFMutation();
				mutation.setParentImport(parentImport);
				mutation.setEtat(EtatEvenementRF.A_TRAITER);
				mutation.setTypeEntite(TypeEntiteRF.SERVITUDE);
				mutation.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mutation.setIdRF(key.getMasterIdRF()); // idRF de la servitude
				mutation.setVersionRF(key.getVersionIdRF());
				mutation.setXmlContent(null);
				evenementRFMutationDAO.save(mutation);
			});

			return null;
		});
	}

	/**
	 * [SIFISC-24514] Cette méthode retourne l'egrid d'un immeuble à partir de son masterIdRF.
	 */
	@NotNull
	private String resolveEgrid(@NotNull String masterIdRF) {
		final ImmeubleRF immeuble = immeubleRFDAO.find(new ImmeubleRFKey(masterIdRF), FlushMode.MANUAL);
		if (immeuble == null) {
			// l'immeuble n'existe pas dans le base, on utilise le masterIdRF
			return masterIdRF;
		}
		final String egrid = immeuble.getEgrid();
		if (StringUtils.isBlank(egrid)) {
			// l'immeuble ne possède pas d'EGRID, on utilise le masterIdRF
			return masterIdRF;
		}
		else {
			// on retourne l'EGRID
			return egrid;
		}
	}
}
