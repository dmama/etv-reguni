package ch.vd.uniregctb.registrefoncier;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.GrundstueckExport;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.technical.esb.util.StringSource;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.SurfaceAuSolRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.helper.AyantDroitRFHelper;
import ch.vd.uniregctb.registrefoncier.helper.ImmeubleRFHelper;
import ch.vd.uniregctb.registrefoncier.helper.SurfaceAuSolRFHelper;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * Cette classe reçoit les données extraites de l'import du registre foncier, les compare avec les données en base et génère des événements de mutation correspondants.
 */
public class DataRFMutationsDetector {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataRFMutationsDetector.class);

	private final int batchSize;
	private final XmlHelperRF xmlHelperRF;
	private final ImmeubleRFDAO immeubleRFDAO;
	private final AyantDroitRFDAO ayantDroitRFDAO;
	private final SurfaceAuSolRFDAO surfaceAuSolRFDAO;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;
	private final ThreadLocal<Unmarshaller> surfaceListUnmarshaller;

	public DataRFMutationsDetector(XmlHelperRF xmlHelperRF,
	                               ImmeubleRFDAO immeubleRFDAO,
	                               AyantDroitRFDAO ayantDroitRFDAO,
	                               SurfaceAuSolRFDAO surfaceAuSolRFDAO,
	                               EvenementRFImportDAO evenementRFImportDAO,
	                               EvenementRFMutationDAO evenementRFMutationDAO,
	                               PlatformTransactionManager transactionManager) {
		this(20, xmlHelperRF, immeubleRFDAO, ayantDroitRFDAO, surfaceAuSolRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
	}

	public DataRFMutationsDetector(int batchSize,
	                               XmlHelperRF xmlHelperRF,
	                               ImmeubleRFDAO immeubleRFDAO,
	                               AyantDroitRFDAO ayantDroitRFDAO,
	                               SurfaceAuSolRFDAO surfaceAuSolRFDAO,
	                               EvenementRFImportDAO evenementRFImportDAO,
	                               EvenementRFMutationDAO evenementRFMutationDAO,
	                               PlatformTransactionManager transactionManager) {
		this.batchSize = batchSize;
		this.xmlHelperRF = xmlHelperRF;
		this.immeubleRFDAO = immeubleRFDAO;
		this.ayantDroitRFDAO = ayantDroitRFDAO;
		this.surfaceAuSolRFDAO = surfaceAuSolRFDAO;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;

		surfaceListUnmarshaller = ThreadLocal.withInitial(() -> {
			try {
				return xmlHelperRF.getSurfaceListContext().createUnmarshaller();
			}
			catch (JAXBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public void processImmeubles(long importId, final int nbThreads, @NotNull Iterator<Grundstueck> iterator) {

		final ParallelBatchTransactionTemplate<Grundstueck> template = new ParallelBatchTransactionTemplate<Grundstueck>(iterator, batchSize, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, AuthenticationInterface.INSTANCE) {
			@Override
			protected int getBlockingQueueCapacity() {
				// on limite la queue interne du template à 10 lots de BATCH_SIZE, autrement
				// on sature rapidemment la mémoire de la JVM avec l'entier du fichier d'import.
				return 10;
			}
		};

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

					final ImmeubleRFKey key = ImmeubleRFHelper.newImmeubleRFKey(immeuble);
					final ImmeubleRF immeubleRF = immeubleRFDAO.find(key);

					// on détermine ce qu'il faut faire
					final EvenementRFMutation.TypeMutation typeMutation;
					if (immeubleRF == null) {
						typeMutation = EvenementRFMutation.TypeMutation.CREATION;
					}
					else if (!ImmeubleRFHelper.currentDataEquals(immeubleRF, immeuble)) {
						typeMutation = EvenementRFMutation.TypeMutation.MODIFICATION;
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
					mutation.setTypeEntite(EvenementRFMutation.TypeEntite.IMMEUBLE);
					mutation.setTypeMutation(typeMutation);
					mutation.setIdImmeubleRF(key.getIdRF());
					mutation.setXmlContent(immeubleAsXml);

					evenementRFMutationDAO.save(mutation);
				}

				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.error("Exception sur le traitement de l'immeuble idRF=[" + first.get().getGrundstueckID() + "]", e);
				}
			}
		}, null);
	}

	public void processDroits(long importId, Iterator<PersonEigentumAnteil> iterator) {
		// TODO (msi) implémenter la détection des mutations des droits
		while (iterator.hasNext()) {
			iterator.next();
		}
	}

	public void processProprietaires(long importId, int nbThreads, Iterator<Personstamm> iterator) {

		final ParallelBatchTransactionTemplate<Personstamm> template = new ParallelBatchTransactionTemplate<Personstamm>(iterator, batchSize, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, AuthenticationInterface.INSTANCE) {
			@Override
			protected int getBlockingQueueCapacity() {
				// on limite la queue interne du template à 10 lots de BATCH_SIZE, autrement
				// on sature rapidemment la mémoire de la JVM avec l'entier du fichier d'import.
				return 10;
			}
		};

		template.execute(new BatchCallback<Personstamm>() {

			private final ThreadLocal<Personstamm> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Personstamm> batch) throws Exception {
				first.set(batch.get(0));

				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				for (Personstamm person : batch) {

					final AyantDroitRFKey key = AyantDroitRFHelper.newAyantDroitKey(person);
					final AyantDroitRF ayantDroitRF = ayantDroitRFDAO.find(key);

					// on détermine ce qu'il faut faire
					final EvenementRFMutation.TypeMutation typeMutation;
					if (ayantDroitRF == null) {
						typeMutation = EvenementRFMutation.TypeMutation.CREATION;
					}
					else if (!AyantDroitRFHelper.dataEquals(ayantDroitRF, person)) {
						typeMutation = EvenementRFMutation.TypeMutation.MODIFICATION;
					}
					else {
						// rien à faire
						continue;
					}

					// on ajoute l'événement à traiter
					final String immeubleAsXml = xmlHelperRF.toXMLString(person);

					final EvenementRFMutation mutation = new EvenementRFMutation();
					mutation.setParentImport(parentImport);
					mutation.setEtat(EtatEvenementRF.A_TRAITER);
					mutation.setTypeEntite(EvenementRFMutation.TypeEntite.AYANT_DROIT);
					mutation.setIdImmeubleRF(null); // par définition
					mutation.setTypeMutation(typeMutation);
					mutation.setXmlContent(immeubleAsXml);

					evenementRFMutationDAO.save(mutation);
				}

				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.error("Exception sur le traitement du propriétaire idRF=[" + first.get().getPersonstammID() + "]", e);
				}
			}
		}, null);
	}

	public void processConstructions(long importId, Iterator<Gebaeude> iterator) {
		// TODO (msi) implémenter la détection des mutations des bâtiments
		while (iterator.hasNext()) {
			iterator.next();
		}
	}

	public void processSurfaces(long importId, int nbThreads, Iterator<Bodenbedeckung> iterator) {

		// msi : on ne peut pas utiliser le ParallelBatchTransactionTemplate parce qu'on peut se retrouver à éditer la même mutation dans deux threads différents.
		final BatchTransactionTemplate<Bodenbedeckung> t1 = new BatchTransactionTemplate<>(iterator, batchSize, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null);
		t1.execute(new BatchCallback<Bodenbedeckung>() {

			private final ThreadLocal<Bodenbedeckung> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Bodenbedeckung> batch) throws Exception {
				first.set(batch.get(0));

				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				// on regroupe les surfaces par immeubles
				final Map<String, ListImmeuble<Bodenbedeckung>> map = new HashMap<>();
				batch.forEach(b -> {
					final String idRF = b.getGrundstueckIDREF();
					ListImmeuble<Bodenbedeckung> list = map.get(idRF);
					if (list == null) {
						list = new ListImmeuble<>(idRF);
						map.put(idRF, list);
					}
					list.add(b);
				});

				// pour chaque immeuble, on génère une seule mutation qui doit contenir la liste de toutes les surfaces au sol.
				map.values().forEach(surfaces -> {
					final String idImmeubleRF = surfaces.get(0).getGrundstueckIDREF();
					// on va chercher l'éventuelle mutation déjà existante (on a pu la recevoir dans un autre batch)
					EvenementRFMutation mut = evenementRFMutationDAO.find(importId, EvenementRFMutation.TypeEntite.SURFACE_AU_SOL, idImmeubleRF);
					if (mut == null) {
						// la mutation n'existe pas, on la crée
						mut = new EvenementRFMutation();
						mut.setParentImport(parentImport);
						mut.setEtat(EtatEvenementRF.A_TRAITER);
						mut.setTypeEntite(EvenementRFMutation.TypeEntite.SURFACE_AU_SOL);
						mut.setIdImmeubleRF(idImmeubleRF);
						mut.setTypeMutation(EvenementRFMutation.TypeMutation.CREATION);
						mut.setXmlContent(xmlHelperRF.toXMLString(new GrundstueckExport.BodenbedeckungList(surfaces)));
						evenementRFMutationDAO.save(mut);
					}
					else {
						// si elle existe, on la complète avec les surfaces
						final GrundstueckExport.BodenbedeckungList list = parseAsBodenbedeckungList(mut.getXmlContent());
						list.getBodenbedeckung().addAll(surfaces);
						mut.setXmlContent(xmlHelperRF.toXMLString(list));
					}
				});

				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.error("Exception sur le traitement de la surface au sol VersionID=[" + first.get().getVersionID() + "]", e);
				}
			}
		}, null);

		// une fois que toutes les surfaces sont renseignées, on reprend la liste pour supprimer les mutations
		// qui portent sur des immeubles dont les surfaces n'ont pas changé (il n'a pas moyen de faire autrement
		// parce que les surfaces ne sont pas identifiées formellement et qu'elles arrivent dans le désordre)

		TransactionTemplate t2 = new TransactionTemplate(transactionManager);
		t2.setReadOnly(true);
		final List<Long> ids = t2.execute(status -> evenementRFMutationDAO.findIds(importId, EvenementRFMutation.TypeEntite.SURFACE_AU_SOL, EtatEvenementRF.A_TRAITER));

		final ParallelBatchTransactionTemplate<Long> t3 = new ParallelBatchTransactionTemplate<>(ids, batchSize, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, AuthenticationInterface.INSTANCE);
		t3.execute(new BatchCallback<Long>() {

			private final ThreadLocal<Long> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Long> ids) throws Exception {
				first.set(ids.get(0));

				ids.forEach(id -> {

					final EvenementRFMutation mut = evenementRFMutationDAO.get(id);
					if (mut == null) {
						throw new IllegalArgumentException("La mutation avec l'id=[" + id + "] n'existe pas.");
					}
					final String idRF = mut.getIdImmeubleRF();

					final ImmeubleRF immeuble = immeubleRFDAO.find(new ImmeubleRFKey(idRF));
					if (immeuble == null) {
						// l'immeuble n'existe pas, c'est qu'il doit encore être créé : toutes les mutations sur les surfaces au sol sont correctes et on ne fait rien.
						return;
					}

					final Set<SurfaceAuSolRF> activesSurfaces = immeuble.getSurfacesAuSol().stream()
							.filter(s -> s.isValidAt(null))
							.collect(Collectors.toSet());

					// on récupère les nouvelles surfaces
					final GrundstueckExport.BodenbedeckungList list = parseAsBodenbedeckungList(mut.getXmlContent());
					if (SurfaceAuSolRFHelper.dataEquals(activesSurfaces, list.getBodenbedeckung())) {
						// les surfaces sont égales : les mutations sont superflues et on les supprime
						evenementRFMutationDAO.remove(id);
					}
					else {
						// les surfaces sont différentes : on garde les mutations et on les passe en modification
						mut.setTypeMutation(EvenementRFMutation.TypeMutation.MODIFICATION);
					}
				});
				return false;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.error("Exception sur le traitement de la mutation de surface au sol id=[" + first.get() + "]", e);
				}
			}

		}, null);
	}

	private GrundstueckExport.BodenbedeckungList parseAsBodenbedeckungList(String xmlContent) {
		GrundstueckExport.BodenbedeckungList list;
		try {
			list = (GrundstueckExport.BodenbedeckungList) surfaceListUnmarshaller.get().unmarshal(new StringSource(xmlContent));
		}
		catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		return list;
	}
}
