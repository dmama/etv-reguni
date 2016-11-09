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
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TraiterImportRFAyantsDroitsJobTest extends ImportRFTestClass {

	private BatchScheduler batchScheduler;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private ZipRaftEsbStore zipRaftEsbStore;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		zipRaftEsbStore = getBean(ZipRaftEsbStore.class, "zipRaftEsbStore");
	}

	/**
	 * Ce test vérifie que les mutations de type CREATION sont bien créées lorsqu'on importe un fichier RF sur une base vide
	 */
	@Test
	public void testImportAyantsDroitsBaseVide() throws Exception {

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
				final EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(RegDate.get(2016, 10, 1));
				importEvent.setEtat(EtatEvenementRF.A_TRAITER);
				importEvent.setFileUrl(raftUrl);
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

		// on vérifie que les mutations attendues sont bien dans la DB
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
				assertEquals(3, mutations.size());    // il y a 3 ayants-droits dans le fichier d'import et la DB était vide
				Collections.sort(mutations, (o1, o2) -> o1.getId().compareTo(o2.getId()));

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.AYANT_DROIT, mut0.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut0.getTypeMutation());
				assertNull(mut0.getIdImmeubleRF());
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
				assertEquals(EvenementRFMutation.TypeEntite.AYANT_DROIT, mut1.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut1.getTypeMutation());
				assertNull(mut1.getIdImmeubleRF());
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
				assertEquals(EvenementRFMutation.TypeEntite.AYANT_DROIT, mut2.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut2.getTypeMutation());
				assertNull(mut2.getIdImmeubleRF());
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
	 * Ce test vérifie qu'aucune mutation n'est créées lorsqu'on importe un fichier RF et que les immeubles dans la base sont déjà à jour.
	 */
	@Test
	public void testImportAyantsDroitsDejaAJour() throws Exception {

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
				final EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(RegDate.get(2016, 10, 1));
				importEvent.setEtat(EtatEvenementRF.A_TRAITER);
				importEvent.setFileUrl(raftUrl);
				return evenementRFImportDAO.save(importEvent).getId();
			}
		});
		assertNotNull(importId);

		// on insère les données des immeubles dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				// données équivalentes au fichier export_ayantsdroits_rf_hebdo.xm.xml
				final PersonnePhysiqueRF pp = newPersonnePhysique("3893728273382823", 3727L, 827288022L, "Nom", "Prénom", RegDate.get(1956, 1, 23));
				final PersonneMoraleRF pm = newPersonneMorale("48349384890202", 3727L, 827288022L, "Raison sociale");
				final CollectivitePubliqueRF coll = newCollectivitePublique("574739202303482", 3727L, 827288022L, "Raison sociale");
				ayantDroitRFDAO.save(pp);
				ayantDroitRFDAO.save(pm);
				ayantDroitRFDAO.save(coll);
			}
		});

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

		// on vérifie qu'il n'y a pas de mutations dans la DB
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
				assertEquals(0, mutations.size());    // il y a 3 ayants-droits dans le fichier d'import et ils sont tous identiques à ceux dans la DB
			}
		});
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsqu'on importe un fichier RF et que les immeubles dans la base ne correspondent pas.
	 */
	@Test
	public void testImportAyantsDroitsAvecModifications() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_ayantsdroits_rf_hebdo.xml");
		assertNotNull(importFile);

		// on l'upload dans Raft
		final String raftUrl;
		try (FileInputStream is = new FileInputStream(importFile)) {
			raftUrl = zipRaftEsbStore.store("Fiscalite", "UnitTest", "Unireg", is);
		}
		assertNotNull(raftUrl);

		final RegDate dateImportInitial = RegDate.get(2010, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(dateSecondImport);
				importEvent.setEtat(EtatEvenementRF.A_TRAITER);
				importEvent.setFileUrl(raftUrl);
				return evenementRFImportDAO.save(importEvent).getId();
			}
		});
		assertNotNull(importId);

		// on insère les données des immeubles dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				// données partiellement différentes de celles du fichier export_ayantsdroits_rf_hebdo.xm.xml
				//  - no RF différent
				final PersonnePhysiqueRF pp = newPersonnePhysique("3893728273382823", 48322L, 827288022L, "Nom", "Prénom", RegDate.get(1956, 1, 23));
				// - raison sociale différente
				final PersonneMoraleRF pm = newPersonneMorale("48349384890202", 3727L, 827288022L, "Raison sociale différente");
				// - no CTB différent
				final CollectivitePubliqueRF coll = newCollectivitePublique("574739202303482", 3727L, 584323450L, "Raison sociale");
				ayantDroitRFDAO.save(pp);
				ayantDroitRFDAO.save(pm);
				ayantDroitRFDAO.save(coll);
			}
		});

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

		// on vérifie que les mutations attendues sont bien dans la DB
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
				assertEquals(3, mutations.size());    // les 3 ayants-droits dans le fichier d'import sont tous différents
				Collections.sort(mutations, (o1, o2) -> o1.getId().compareTo(o2.getId()));

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.AYANT_DROIT, mut0.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.MODIFICATION, mut0.getTypeMutation());
				assertNull(mut0.getIdImmeubleRF());
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
				assertEquals(EvenementRFMutation.TypeEntite.AYANT_DROIT, mut1.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.MODIFICATION, mut1.getTypeMutation());
				assertNull(mut1.getIdImmeubleRF());
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
				assertEquals(EvenementRFMutation.TypeEntite.AYANT_DROIT, mut2.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.MODIFICATION, mut2.getTypeMutation());
				assertNull(mut2.getIdImmeubleRF());
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

}