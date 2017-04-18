package ch.vd.uniregctb.registrefoncier.dataimport;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import ch.vd.uniregctb.evenement.registrefoncier.TypeImportRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("Duplicates")
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

		final Long importId = insertImport(TypeImportRF.PRINCIPAL, RegDate.get(2016, 10, 1), EtatEvenementRF.TRAITE, "http://turlututu");
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
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
				assertEquals("L'import RF avec l'id = [" + importId + "] a déjà été traité.", importEvent.getErrorMessage());
				assertTrue(importEvent.getCallstack().contains("L'import RF avec l'id = [" + importId + "] a déjà été traité."));
			}
		});
	}

	/**
	 * Ce test vérifie que le démarrage du job sur un import déjà processé lève bien une exception lorsqu'un import précédent n'a pas encore été processé.
	 */
	@Test
	public void testImportAutreImportPasEncoreProcesse() throws Exception {

		// on insère les données de l'import dans la base
		final long precedent = insertImport(TypeImportRF.PRINCIPAL, RegDate.get(2016, 9, 1), EtatEvenementRF.EN_ERREUR, "http://turlututu");
		final long suivant = insertImport(TypeImportRF.PRINCIPAL, RegDate.get(2016, 10, 1), EtatEvenementRF.A_TRAITER, "http://turlututu");

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
		params.put(TraiterImportRFJob.ID, suivant);
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
				final EvenementRFImport importEvent = evenementRFImportDAO.get(suivant);
				assertNotNull(importEvent);
				assertEquals(EtatEvenementRF.EN_ERREUR, importEvent.getEtat());
				assertEquals("L'import RF avec l'id = [" + suivant + "] doit être traité après l'import RF avec l'id = [" + precedent + "].", importEvent.getErrorMessage());
				assertTrue(importEvent.getCallstack().contains("L'import RF avec l'id = [" + suivant + "] doit être traité après l'import RF avec l'id = [" + precedent + "]."));
			}
		});
	}

	/**
	 * [SIFISC-22393] Ce test vérifie que le démarrage du job sur un import dont la date de valeur ne suit pas la logique chronologique lève bien une exception.
	 */
	@Test
	public void testImportDateValeurAnterieure() throws Exception {

		// on insère les données de l'import dans la base
		insertImport(TypeImportRF.PRINCIPAL, RegDate.get(2016, 9, 1), EtatEvenementRF.TRAITE, "http://turlututu");
		final long suivant = insertImport(TypeImportRF.PRINCIPAL, RegDate.get(2015, 3, 7), EtatEvenementRF.A_TRAITER, "http://turlututu");

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
		params.put(TraiterImportRFJob.ID, suivant);
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
				final EvenementRFImport importEvent = evenementRFImportDAO.get(suivant);
				assertNotNull(importEvent);
				assertEquals(EtatEvenementRF.EN_ERREUR, importEvent.getEtat());
				assertEquals("L'import RF avec l'id = [" + suivant + "] possède une date de valeur [2015.03.07] antérieure ou égale à la date de valeur du dernier import traité [2016.09.01].", importEvent.getErrorMessage());
				assertTrue(importEvent.getCallstack().contains("L'import RF avec l'id = [" + suivant + "] possède une date de valeur [2015.03.07] antérieure ou égale à la date de valeur du dernier import traité [2016.09.01]."));
			}
		});
	}

	/**
	 * [SIFISC-22393] Ce test vérifie que le démarrage du job sur un import dont la date de valeur ne suit pas la logique chronologique lève bien une exception.
	 */
	@Test
	public void testImportDateValeurIdentique() throws Exception {

		// on insère les données de l'import dans la base
		insertImport(TypeImportRF.PRINCIPAL, RegDate.get(2016, 9, 1), EtatEvenementRF.TRAITE, "http://turlututu");
		final long suivant = insertImport(TypeImportRF.PRINCIPAL, RegDate.get(2016, 9, 1), EtatEvenementRF.A_TRAITER, "http://turlututu");

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
		params.put(TraiterImportRFJob.ID, suivant);
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
				final EvenementRFImport importEvent = evenementRFImportDAO.get(suivant);
				assertNotNull(importEvent);
				assertEquals(EtatEvenementRF.EN_ERREUR, importEvent.getEtat());
				assertEquals("L'import RF avec l'id = [" + suivant + "] possède une date de valeur [2016.09.01] antérieure ou égale à la date de valeur du dernier import traité [2016.09.01].", importEvent.getErrorMessage());
				assertTrue(importEvent.getCallstack().contains("L'import RF avec l'id = [" + suivant + "] possède une date de valeur [2016.09.01] antérieure ou égale à la date de valeur du dernier import traité [2016.09.01]."));
			}
		});
	}

	/**
	 * Ce test vérifie que le démarrage du job sur un import de type B alors que l'import de A à une date de valeur plus récente est bien autorisé à démarrer.
	 */
	@Test
	public void testImportAutreTypeDateValeurAnterieure() throws Exception {

		// on va chercher un fichier d'import (vide, mais ça n'a pas d'importance)
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_servitudes_vide_rf.xml");
		assertNotNull(importFile);

		// on l'upload dans Raft
		final String raftUrl;
		try (FileInputStream is = new FileInputStream(importFile)) {
			raftUrl = zipRaftEsbStore.store("Fiscalite", "UnitTest", "Unireg", is);
		}
		assertNotNull(raftUrl);

		// un import principal déjà processé + un import des servitudes à processer
		insertImport(TypeImportRF.PRINCIPAL, RegDate.get(2016, 9, 1), EtatEvenementRF.TRAITE, "http://turlututu");
		final long suivant = insertImport(TypeImportRF.SERVITUDES, RegDate.get(2016, 3, 1), EtatEvenementRF.A_TRAITER, raftUrl);

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
		params.put(TraiterImportRFJob.ID, suivant);
		params.put(TraiterImportRFJob.NB_THREADS, 2);
		params.put(TraiterImportRFJob.CONTINUE_WITH_MUTATIONS_JOB, false);

		final JobDefinition job = batchScheduler.startJob(TraiterImportRFJob.NAME, params);
		assertNotNull(job);

		// le job doit bien démarrer
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());
	}

	/**
	 * Ce test vérifie que le démarrage du job sur un import de type B alors que l'import de A à la même date de valeur est bien autorisé à démarrer.
	 */
	@Test
	public void testImportAutreTypeDateValeurIdentique() throws Exception {

		// on va chercher un fichier d'import (vide, mais ça n'a pas d'importance)
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_servitudes_vide_rf.xml");
		assertNotNull(importFile);

		// on l'upload dans Raft
		final String raftUrl;
		try (FileInputStream is = new FileInputStream(importFile)) {
			raftUrl = zipRaftEsbStore.store("Fiscalite", "UnitTest", "Unireg", is);
		}
		assertNotNull(raftUrl);

		// un import principal déjà processé + un import des servitudes à processer
		insertImport(TypeImportRF.PRINCIPAL, RegDate.get(2016, 9, 1), EtatEvenementRF.TRAITE, "http://turlututu");
		final long suivant = insertImport(TypeImportRF.SERVITUDES, RegDate.get(2016, 9, 1), EtatEvenementRF.A_TRAITER, raftUrl);

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
		params.put(TraiterImportRFJob.ID, suivant);
		params.put(TraiterImportRFJob.NB_THREADS, 2);
		params.put(TraiterImportRFJob.CONTINUE_WITH_MUTATIONS_JOB, false);

		final JobDefinition job = batchScheduler.startJob(TraiterImportRFJob.NAME, params);
		assertNotNull(job);

		// le job doit bien démarrer
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());
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
				importEvent.setType(TypeImportRF.PRINCIPAL);
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
		final Map<String, Object> params = new HashMap<>();
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
				mutations.sort(new MutationComparator());

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
				importEvent.setType(TypeImportRF.PRINCIPAL);
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
				importEvent.setType(TypeImportRF.PRINCIPAL);
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
		final Map<String, Object> params = new HashMap<>();
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
				assertEquals("L'import RF avec l'id = [" + ids.suivant + "] ne peut être traité car des mutations de l'import RF avec l'id = [" + ids.precedent + "] n'ont pas été traitées.", importEvent.getErrorMessage());
				assertTrue(importEvent.getCallstack().contains("L'import RF avec l'id = [" + ids.suivant + "] ne peut être traité car des mutations de l'import RF avec l'id = [" + ids.precedent + "] n'ont pas été traitées."));
			}
		});
	}

	private Long insertImport(final TypeImportRF type, final RegDate dateEvenement, final EtatEvenementRF etat, final String fileUrl) throws Exception {
		// on insère les données de l'import dans la base
		return doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setType(type);
				importEvent.setDateEvenement(dateEvenement);
				importEvent.setEtat(etat);
				importEvent.setFileUrl(fileUrl);
				return evenementRFImportDAO.save(importEvent).getId();
			}
		});
	}
}