package ch.vd.uniregctb.registrefoncier;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.technical.esb.store.raft.ZipRaftEsbStore;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TraiterImportRFJobTest extends ImportRFTestClass {

	private BatchScheduler batchScheduler;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private ZipRaftEsbStore zipRaftEsbStore;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		zipRaftEsbStore = getBean(ZipRaftEsbStore.class, "zipRaftEsbStore");
	}

	/**
	 * Ce test vérifie que le démarrage du job sur un import déjà processé lève bien une exception.
	 */
	@Test
	public void testImportDejaProcesse() throws Exception {

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(RegDate.get(2016, 10, 1));
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				return evenementRFImportDAO.save(importEvent).getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterImportRFJob.ID, importId);
		params.put(TraiterImportRFJob.NB_THREADS, 2);
		params.put(TraiterImportRFJob.CONTINUE_WITH_MUTATIONS_JOB, false);

		final JobDefinition job = batchScheduler.startJob(TraiterImportRFJob.NAME, params);
		assertNotNull(job);

		// le job ne doit pas démarrer
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_EXCEPTION, job.getStatut());

		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = evenementRFImportDAO.get(importId);
				assertNotNull(importEvent);
				assertEquals(EtatEvenementRF.EN_ERREUR, importEvent.getEtat());
				assertTrue(importEvent.getErrorMessage().contains("L'import RF avec l'id = [" + importId + "] a déjà été traité."));
			}
		});
	}

	/**
	 * Ce test vérifie que le démarrage du job sur un import déjà processé lève bien une exception lorsqu'un import précédent n'a pas encore été processé.
	 */
	@Test
	public void testImportAutreImportPasEncoreProcesse() throws Exception {

		class Ids {
			long precedent;
			long suivant;
		}
		final Ids ids = new Ids();

		// on insère les données de l'import dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				// un import précédent en erreur
				final EvenementRFImport importEventPrecedant = new EvenementRFImport();
				importEventPrecedant.setDateEvenement(RegDate.get(2016, 9, 1));
				importEventPrecedant.setEtat(EtatEvenementRF.EN_ERREUR);
				importEventPrecedant.setFileUrl("http://turlututu");
				ids.precedent = evenementRFImportDAO.save(importEventPrecedant).getId();

				final EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(RegDate.get(2016, 10, 1));
				importEvent.setEtat(EtatEvenementRF.A_TRAITER);
				importEvent.setFileUrl("http://turlututu");
				ids.suivant = evenementRFImportDAO.save(importEvent).getId();
			}
		});

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterImportRFJob.ID, ids.suivant);
		params.put(TraiterImportRFJob.NB_THREADS, 2);
		params.put(TraiterImportRFJob.CONTINUE_WITH_MUTATIONS_JOB, false);

		final JobDefinition job = batchScheduler.startJob(TraiterImportRFJob.NAME, params);
		assertNotNull(job);

		// le job ne doit pas démarrer
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_EXCEPTION, job.getStatut());

		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = evenementRFImportDAO.get(ids.suivant);
				assertNotNull(importEvent);
				assertEquals(EtatEvenementRF.EN_ERREUR, importEvent.getEtat());
				assertTrue(importEvent.getErrorMessage().contains("L'import RF avec l'id = [" + ids.suivant + "] doit être traité après l'import RF avec l'id = [" + ids.precedent + "]."));
			}
		});
	}

	/**
	 * Ce vérifie que l'exécution d'un import en erreur et avec des mutations prééexistantes commence bien par effacer ces mutations pour repartir de zéro.
	 */
	@Test
	public void testImportEnErreurAvecMutationExistantes() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_ayantsdroits_rf_hebdo.xml");
		assertNotNull(importFile);

		// on l'upload dans Raft
		final String raftUrl;
		try (FileInputStream is = new FileInputStream(importFile)) {
			raftUrl = zipRaftEsbStore.store("Fiscalite", "UnitTest", "Unireg", is);
		}
		assertNotNull(raftUrl);

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(RegDate.get(2016, 10, 1));
				importEvent.setEtat(EtatEvenementRF.EN_ERREUR);
				importEvent.setFileUrl(raftUrl);
				importEvent = evenementRFImportDAO.save(importEvent);

				// on insère deux mutations pour simuler une exécution partielle antérieure
				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.AYANT_DROIT);
				mut0.setTypeMutation(TypeMutationRF.CREATION);
				mut0.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   " <NatuerlichePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "     <PersonstammID>1</PersonstammID>\n" +
						                   "     <Name>Nom</Name>\n" +
						                   "     <Gueltig>false</Gueltig>\n" +
						                   "     <NoRF>3727</NoRF>\n" +
						                   "     <Vorname>Prénom</Vorname>\n" +
						                   "     <Geburtsdatum>\n" +
						                   "         <Tag>23</Tag>\n" +
						                   "         <Monat>1</Monat>\n" +
						                   "         <Jahr>1956</Jahr>\n" +
						                   "     </Geburtsdatum>\n" +
						                   "     <NrIROLE>827288022</NrIROLE>\n" +
						                   " </NatuerlichePersonstamm>\n\n");
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.AYANT_DROIT);
				mut1.setTypeMutation(TypeMutationRF.CREATION);
				mut1.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<JuristischePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <PersonstammID>2</PersonstammID>\n" +
						                   "    <Name>Raison sociale</Name>\n" +
						                   "    <Gueltig>false</Gueltig>\n" +
						                   "    <NrACI>827288022</NrACI>\n" +
						                   "    <NoRF>3727</NoRF>\n" +
						                   "    <Unterart>SchweizerischeJuristischePerson</Unterart>\n" +
						                   "</JuristischePersonstamm>\n");
				evenementRFMutationDAO.save(mut1);

				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterImportRFJob.ID, importId);
		params.put(TraiterImportRFJob.NB_THREADS, 2);
		params.put(TraiterImportRFJob.CONTINUE_WITH_MUTATIONS_JOB, false);

		final JobDefinition job = batchScheduler.startJob(TraiterImportRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que l'import est bien passé au statut TRAITE
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = evenementRFImportDAO.get(importId);
				assertNotNull(importEvent);
				assertEquals(EtatEvenementRF.TRAITE, importEvent.getEtat());
			}
		});

		// on vérifie que les mutations attendues sont bien dans la DB (et que les mutations pré-existantes ont été supprimées)
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
				assertEquals(3, mutations.size());    // il y a 3 ayants-droits dans le fichier d'import et la DB était vide
				Collections.sort(mutations, new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(TypeEntiteRF.AYANT_DROIT, mut0.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
				assertEquals("3893728273382823", mut0.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<NatuerlichePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <PersonstammID>3893728273382823</PersonstammID>\n" +
						             "    <Name>Nom</Name>\n" +
						             "    <Gueltig>false</Gueltig>\n" +
						             "    <NoRF>3727</NoRF>\n" +
						             "    <Vorname>Prénom</Vorname>\n" +
						             "    <Geburtsdatum>\n" +
						             "        <Tag>23</Tag>\n" +
						             "        <Monat>1</Monat>\n" +
						             "        <Jahr>1956</Jahr>\n" +
						             "    </Geburtsdatum>\n" +
						             "    <NrIROLE>827288022</NrIROLE>\n" +
						             "</NatuerlichePersonstamm>\n", mut0.getXmlContent());

				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(TypeEntiteRF.AYANT_DROIT, mut1.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
				assertEquals("48349384890202", mut1.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<JuristischePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <PersonstammID>48349384890202</PersonstammID>\n" +
						             "    <Name>Raison sociale</Name>\n" +
						             "    <Gueltig>false</Gueltig>\n" +
						             "    <NrACI>827288022</NrACI>\n" +
						             "    <NoRF>3727</NoRF>\n" +
						             "    <Unterart>SchweizerischeJuristischePerson</Unterart>\n" +
						             "</JuristischePersonstamm>\n", mut1.getXmlContent());

				final EvenementRFMutation mut2 = mutations.get(2);
				assertEquals(importId, mut2.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
				assertEquals(TypeEntiteRF.AYANT_DROIT, mut2.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut2.getTypeMutation());
				assertEquals("574739202303482", mut2.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<JuristischePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <PersonstammID>574739202303482</PersonstammID>\n" +
						             "    <Name>Raison sociale</Name>\n" +
						             "    <Gueltig>false</Gueltig>\n" +
						             "    <NrACI>827288022</NrACI>\n" +
						             "    <NoRF>3727</NoRF>\n" +
						             "    <Unterart>OeffentlicheKoerperschaft</Unterart>\n" +
						             "</JuristischePersonstamm>\n", mut2.getXmlContent());
			}
		});

	}

	/**
	 * Ce vérifie que l'exécution d'un import lève une exception si des mutations d'un autre import n'ont pas été traitées.
	 */
	@Test
	public void testImportAvecMutationAutreImportNonTraitees() throws Exception {

		class Ids {
			long precedent;
			long suivant;
		}
		final Ids ids = new Ids();

		// on insère les données d'un import précédent dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(RegDate.get(2016, 9, 1));
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://radada");
				importEvent = evenementRFImportDAO.save(importEvent);
				ids.precedent = importEvent.getId();

				// on insère deux mutations non traitées pour simuler une exécution partielle antérieure
				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.AYANT_DROIT);
				mut0.setTypeMutation(TypeMutationRF.CREATION);
				mut0.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   " <NatuerlichePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "     <PersonstammID>1</PersonstammID>\n" +
						                   "     <Name>Nom</Name>\n" +
						                   "     <Gueltig>false</Gueltig>\n" +
						                   "     <NoRF>3727</NoRF>\n" +
						                   "     <Vorname>Prénom</Vorname>\n" +
						                   "     <Geburtsdatum>\n" +
						                   "         <Tag>23</Tag>\n" +
						                   "         <Monat>1</Monat>\n" +
						                   "         <Jahr>1956</Jahr>\n" +
						                   "     </Geburtsdatum>\n" +
						                   "     <NrIROLE>827288022</NrIROLE>\n" +
						                   " </NatuerlichePersonstamm>\n\n");
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.AYANT_DROIT);
				mut1.setTypeMutation(TypeMutationRF.CREATION);
				mut1.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<JuristischePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <PersonstammID>2</PersonstammID>\n" +
						                   "    <Name>Raison sociale</Name>\n" +
						                   "    <Gueltig>false</Gueltig>\n" +
						                   "    <NrACI>827288022</NrACI>\n" +
						                   "    <NoRF>3727</NoRF>\n" +
						                   "    <Unterart>SchweizerischeJuristischePerson</Unterart>\n" +
						                   "</JuristischePersonstamm>\n");
				evenementRFMutationDAO.save(mut1);
			}
		});

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_ayantsdroits_rf_hebdo.xml");
		assertNotNull(importFile);

		// on l'upload dans Raft
		final String raftUrl;
		try (FileInputStream is = new FileInputStream(importFile)) {
			raftUrl = zipRaftEsbStore.store("Fiscalite", "UnitTest", "Unireg", is);
		}
		assertNotNull(raftUrl);

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(RegDate.get(2016, 10, 1));
				importEvent.setEtat(EtatEvenementRF.EN_ERREUR);
				importEvent.setFileUrl(raftUrl);
				importEvent = evenementRFImportDAO.save(importEvent);
				ids.suivant = importEvent.getId();
				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterImportRFJob.ID, importId);
		params.put(TraiterImportRFJob.NB_THREADS, 2);
		params.put(TraiterImportRFJob.CONTINUE_WITH_MUTATIONS_JOB, false);

		final JobDefinition job = batchScheduler.startJob(TraiterImportRFJob.NAME, params);
		assertNotNull(job);

		// le job ne doit pas démarrer
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_EXCEPTION, job.getStatut());

		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = evenementRFImportDAO.get(ids.suivant);
				assertNotNull(importEvent);
				assertEquals(EtatEvenementRF.EN_ERREUR, importEvent.getEtat());
				assertTrue(importEvent.getErrorMessage().contains("L'import RF avec l'id = [" + ids.suivant + "] ne peut être traité car des mutations de l'import RF avec l'id = [" + ids.precedent + "] n'ont pas été traitées."));
			}
		});
	}
}