package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.common.Rechteinhaber;
import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.GrundstueckExport;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.uniregctb.cache.ObjectKey;
import ch.vd.uniregctb.cache.PersistentCache;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation.TypeEntite;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation.TypeMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.PersonEigentumAnteilListElement;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.helper.AyantDroitRFHelper;
import ch.vd.uniregctb.registrefoncier.helper.DroitRFHelper;
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
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;

	private final PersistentCache<ArrayList<PersonEigentumAnteil>> cacheDroits;

	public DataRFMutationsDetector(XmlHelperRF xmlHelperRF,
	                               ImmeubleRFDAO immeubleRFDAO,
	                               AyantDroitRFDAO ayantDroitRFDAO,
	                               EvenementRFImportDAO evenementRFImportDAO,
	                               EvenementRFMutationDAO evenementRFMutationDAO,
	                               PlatformTransactionManager transactionManager, PersistentCache<ArrayList<PersonEigentumAnteil>> cacheDroits) {
		this(20, xmlHelperRF, immeubleRFDAO, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, cacheDroits);
	}

	public DataRFMutationsDetector(int batchSize,
	                               XmlHelperRF xmlHelperRF,
	                               ImmeubleRFDAO immeubleRFDAO,
	                               AyantDroitRFDAO ayantDroitRFDAO,
	                               EvenementRFImportDAO evenementRFImportDAO,
	                               EvenementRFMutationDAO evenementRFMutationDAO,
	                               PlatformTransactionManager transactionManager, PersistentCache<ArrayList<PersonEigentumAnteil>> cacheDroits) {
		this.batchSize = batchSize;
		this.xmlHelperRF = xmlHelperRF;
		this.immeubleRFDAO = immeubleRFDAO;
		this.ayantDroitRFDAO = ayantDroitRFDAO;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;
		this.cacheDroits = cacheDroits;
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
					final TypeMutation typeMutation;
					if (immeubleRF == null) {
						typeMutation = TypeMutation.CREATION;
					}
					else if (!ImmeubleRFHelper.currentDataEquals(immeubleRF, immeuble)) {
						typeMutation = TypeMutation.MODIFICATION;
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
					mutation.setTypeEntite(TypeEntite.IMMEUBLE);
					mutation.setTypeMutation(typeMutation);
					mutation.setIdRF(key.getIdRF());
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

	public void processDroits(long importId, int nbThreads, Iterator<PersonEigentumAnteil> iterator) {


		// on regroupe toutes les droits
		cacheDroits.clear();    // on utilise un cache persistant pour limiter l'utilisation mémoire, mais on est pas
		// du tout intéressé par le côté persistent des données, on commence donc par tout effacer.
		while (iterator.hasNext()) {
			final PersonEigentumAnteil eigentumAnteil = iterator.next();
			if (eigentumAnteil == null) {
				break;
			}

			final String idRF = DroitRFHelper.getIdRF(eigentumAnteil);
			final IdRfKey key = new IdRfKey(idRF);
			ArrayList<PersonEigentumAnteil> list = cacheDroits.get(key);
			if (list == null) {
				list = new ArrayList<>();
			}
			list.add(eigentumAnteil);
			cacheDroits.put(key, list);
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

		// détection des mutations de type CREATION et MODIFICATION
		template.execute(new BatchCallback<Map.Entry<ObjectKey, ArrayList<PersonEigentumAnteil>>>() {

			private final ThreadLocal<Map.Entry<ObjectKey, ArrayList<PersonEigentumAnteil>>> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Map.Entry<ObjectKey, ArrayList<PersonEigentumAnteil>>> batch) throws Exception {
				first.set(batch.get(0));

				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				batch.forEach(e -> {
					final String idRF = ((IdRfKey) e.getKey()).getIdRF();
					final List<PersonEigentumAnteil> nouveauxDroits = e.getValue();

					final AyantDroitRF ayantDroit = ayantDroitRFDAO.find(new AyantDroitRFKey(idRF));
					if (ayantDroit == null) {
						// l'ayant-droit n'existe pas : il va être créé et on doit donc sauver une mutation en mode création.
						final EvenementRFMutation mutation = new EvenementRFMutation();
						mutation.setParentImport(parentImport);
						mutation.setEtat(EtatEvenementRF.A_TRAITER);
						mutation.setTypeEntite(TypeEntite.DROIT);
						mutation.setTypeMutation(TypeMutation.CREATION);
						mutation.setIdRF(idRF); // idRF de l'ayant-droit
						mutation.setXmlContent(xmlHelperRF.toXMLString(new PersonEigentumAnteilListElement(nouveauxDroits)));
						evenementRFMutationDAO.save(mutation);
					}
					else {
						// on récupère les droits actives actuelles
						final Set<DroitRF> activesDroits = ayantDroit.getDroits().stream()
								.filter(s -> s.isValidAt(null))
								.collect(Collectors.toSet());

						//noinspection StatementWithEmptyBody
						if (!DroitRFHelper.dataEquals(activesDroits, nouveauxDroits)) {
							// les droits sont différents : on sauve une mutation en mode modification
							final EvenementRFMutation mutation = new EvenementRFMutation();
							mutation.setParentImport(parentImport);
							mutation.setEtat(EtatEvenementRF.A_TRAITER);
							mutation.setTypeEntite(TypeEntite.DROIT);
							mutation.setTypeMutation(TypeMutation.MODIFICATION);
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

				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.error("Exception sur le traitement des droits de l'ayant-droit idRF=[" + first.get().getKey() + "]", e);
				}
			}
		}, null);

		// détection des mutations de type SUPRESSION
		final TransactionTemplate t1 = new TransactionTemplate(transactionManager);
		t1.execute(s -> {
			final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

			final Set<String> existingAyantsDroits = ayantDroitRFDAO.findAvecDroitsActifs();
			final Set<String> nouveauAyantsDroits = cacheDroits.keySet().stream()
					.map(k -> ((IdRfKey) k).getIdRF())
					.collect(Collectors.toSet());

			// on ne garde que les ayants-droits existants dans la DB qui n'existent pas dans le fichier XML
			existingAyantsDroits.removeAll(nouveauAyantsDroits);

			// on crée des mutations de suppression de droits pour tous ces ayants-droits
			existingAyantsDroits.forEach(idRF -> {
				final EvenementRFMutation mutation = new EvenementRFMutation();
				mutation.setParentImport(parentImport);
				mutation.setEtat(EtatEvenementRF.A_TRAITER);
				mutation.setTypeEntite(TypeEntite.DROIT);
				mutation.setTypeMutation(TypeMutation.SUPPRESSION);
				mutation.setIdRF(idRF); // idRF de l'ayant-droit
				mutation.setXmlContent(null);
				evenementRFMutationDAO.save(mutation);
			});

			return null;
		});
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
					processAyantDroit(parentImport, person);
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

	private void processAyantDroit(EvenementRFImport parentImport, Rechteinhaber rechteinhaber) {

		final AyantDroitRFKey key = AyantDroitRFHelper.newAyantDroitKey(rechteinhaber);
		final AyantDroitRF ayantDroitRF = ayantDroitRFDAO.find(key);

		// on détermine ce qu'il faut faire
		final TypeMutation typeMutation;
		if (ayantDroitRF == null) {
			typeMutation = TypeMutation.CREATION;
		}
		else if (!AyantDroitRFHelper.dataEquals(ayantDroitRF, rechteinhaber)) {
			typeMutation = TypeMutation.MODIFICATION;
		}
		else {
			// rien à faire
			return;
		}

		// on ajoute l'événement à traiter
		final String immeubleAsXml = xmlHelperRF.toXMLString(rechteinhaber);

		final EvenementRFMutation mutation = new EvenementRFMutation();
		mutation.setParentImport(parentImport);
		mutation.setEtat(EtatEvenementRF.A_TRAITER);
		mutation.setTypeEntite(TypeEntite.AYANT_DROIT);
		mutation.setIdRF(key.getIdRF());
		mutation.setTypeMutation(typeMutation);
		mutation.setXmlContent(immeubleAsXml);

		evenementRFMutationDAO.save(mutation);
	}

	public void processConstructions(long importId, Iterator<Gebaeude> iterator) {
		// TODO (msi) implémenter la détection des mutations des bâtiments
		while (iterator.hasNext()) {
			iterator.next();
		}
	}

	public void processSurfaces(long importId, int nbThreads, Iterator<Bodenbedeckung> iterator) {

		// on regroupe toutes les surfaces (en mémoire, il y a en ~400'00 mais elles sont petites et la consommation mesurée est d'environ 160Mb)
		final Map<String, List<Bodenbedeckung>> map = new TreeMap<>();
		while (iterator.hasNext()) {
			final Bodenbedeckung bodenbedeckung = iterator.next();
			if (bodenbedeckung == null) {
				break;
			}
			final String idRF = bodenbedeckung.getGrundstueckIDREF();
			map.computeIfAbsent(idRF, k -> new ArrayList<>()).add(bodenbedeckung);
		}

		// on détecte les mutations qui doivent être générées
		final ParallelBatchTransactionTemplate<Map.Entry<String, List<Bodenbedeckung>>> template
				= new ParallelBatchTransactionTemplate<Map.Entry<String, List<Bodenbedeckung>>>(map.entrySet().iterator(), batchSize, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, AuthenticationInterface.INSTANCE) {
			@Override
			protected int getBlockingQueueCapacity() {
				// on limite la queue interne du template à 10 lots de BATCH_SIZE, autrement
				// on sature rapidemment la mémoire de la JVM avec l'entier du fichier d'import.
				return 10;
			}
		};
		template.execute(new BatchCallback<Map.Entry<String, List<Bodenbedeckung>>>() {

			private final ThreadLocal<Map.Entry<String, List<Bodenbedeckung>>> first = new ThreadLocal<>();

			@Override
			public boolean doInTransaction(List<Map.Entry<String, List<Bodenbedeckung>>> batch) throws Exception {
				first.set(batch.get(0));

				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				batch.forEach(e -> {

					final String idRF = e.getKey();
					final List<Bodenbedeckung> nouvellesSurfaces = e.getValue();

					final ImmeubleRF immeuble = immeubleRFDAO.find(new ImmeubleRFKey(idRF));
					if (immeuble == null) {
						// l'immeuble n'existe pas : il va être créé et on doit donc sauver une mutation en mode création.
						final EvenementRFMutation mut = new EvenementRFMutation();
						mut.setParentImport(parentImport);
						mut.setEtat(EtatEvenementRF.A_TRAITER);
						mut.setTypeEntite(TypeEntite.SURFACE_AU_SOL);
						mut.setIdRF(idRF);
						mut.setTypeMutation(TypeMutation.CREATION);
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
							mut.setTypeEntite(TypeEntite.SURFACE_AU_SOL);
							mut.setIdRF(idRF);
							mut.setTypeMutation(TypeMutation.MODIFICATION);
							mut.setXmlContent(xmlHelperRF.toXMLString(new GrundstueckExport.BodenbedeckungList(nouvellesSurfaces)));
							evenementRFMutationDAO.save(mut);
						}
						else {
							// les surfaces sont égales : rien à faire
						}
					}
				});
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.error("Exception sur le traitement des surfaces au sol de l'immeuble idRF=[" + first.get().getKey() + "]", e);
				}
			}
		}, null);

		// TODO (msi) faut-il détecter les immeubles qui perdraient toutes les surfaces au sol ?
	}

	private static class IdRfKey implements ObjectKey {

		private static final long serialVersionUID = 261902516311503497L;

		@NotNull
		private final String idRF;

		public IdRfKey(@NotNull String idRF) {
			this.idRF = idRF;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final IdRfKey idRfKey = (IdRfKey) o;
			return Objects.equals(idRF, idRfKey.idRF);
		}

		@Override
		public int hashCode() {
			return Objects.hash(idRF);
		}

		@NotNull
		public String getIdRF() {
			return idRF;
		}

		@Override
		public long getId() {
			return 0;
		}

		@Override
		public String getComplement() {
			return idRF;
		}
	}
}
