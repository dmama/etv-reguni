package ch.vd.unireg.registrefoncier.dataimport;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.store.raft.ZipRaftEsbStore;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImport;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeImportRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;
import ch.vd.unireg.registrefoncier.CollectivitePubliqueRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.unireg.scheduler.BatchScheduler;
import ch.vd.unireg.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/export_ayantsdroits_rf_hebdo.xml");
		assertNotNull(importFile);

		// on l'upload dans Raft
		final String raftUrl;
		try (FileInputStream is = new FileInputStream(importFile)) {
			raftUrl = zipRaftEsbStore.store("Fiscalite", "UnitTest", "Unireg", is);
		}
		assertNotNull(raftUrl);

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(status -> {
			final EvenementRFImport importEvent = new EvenementRFImport();
			importEvent.setType(TypeImportRF.PRINCIPAL);
			importEvent.setDateEvenement(RegDate.get(2016, 10, 1));
			importEvent.setEtat(EtatEvenementRF.A_TRAITER);
			importEvent.setFileUrl(raftUrl);
			return evenementRFImportDAO.save(importEvent).getId();
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
		doInNewTransaction(status -> {
			final EvenementRFImport importEvent = evenementRFImportDAO.get(importId);
			assertNotNull(importEvent);
			assertEquals(EtatEvenementRF.TRAITE, importEvent.getEtat());
			return null;
		});

		// on vérifie que les mutations attendues sont bien dans la DB
		doInNewTransaction(status -> {
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
			return null;
		});

	}

	/**
	 * Ce test vérifie qu'aucune mutation n'est créées lorsqu'on importe un fichier RF et que les ayant-droits dans la base sont déjà à jour.
	 */
	@Test
	public void testImportAyantsDroitsDejaAJour() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/export_ayantsdroits_rf_hebdo.xml");
		assertNotNull(importFile);

		// on l'upload dans Raft
		final String raftUrl;
		try (FileInputStream is = new FileInputStream(importFile)) {
			raftUrl = zipRaftEsbStore.store("Fiscalite", "UnitTest", "Unireg", is);
		}
		assertNotNull(raftUrl);

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(status -> {
			final EvenementRFImport importEvent = new EvenementRFImport();
			importEvent.setType(TypeImportRF.PRINCIPAL);
			importEvent.setDateEvenement(RegDate.get(2016, 10, 1));
			importEvent.setEtat(EtatEvenementRF.A_TRAITER);
			importEvent.setFileUrl(raftUrl);
			return evenementRFImportDAO.save(importEvent).getId();
		});
		assertNotNull(importId);

		// on insère les données des ayant-droits dans la base
		doInNewTransaction(status -> {
			// données équivalentes au fichier export_ayantsdroits_rf_hebdo.xm.xml
			final PersonnePhysiqueRF pp = newPersonnePhysique("3893728273382823", 3727L, 827288022L, "Nom", "Prénom", RegDate.get(1956, 1, 23));
			final PersonneMoraleRF pm = newPersonneMorale("48349384890202", 3727L, 827288022L, "Raison sociale");
			final CollectivitePubliqueRF coll = newCollectivitePublique("574739202303482", 3727L, 827288022L, "Raison sociale");
			ayantDroitRFDAO.save(pp);
			ayantDroitRFDAO.save(pm);
			ayantDroitRFDAO.save(coll);
			return null;
		});

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
		doInNewTransaction(status -> {
			final EvenementRFImport importEvent = evenementRFImportDAO.get(importId);
			assertNotNull(importEvent);
			assertEquals(EtatEvenementRF.TRAITE, importEvent.getEtat());
			return null;
		});

		// on vérifie qu'il n'y a pas de mutations dans la DB
		doInNewTransaction(status -> {
			final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
			assertEquals(0, mutations.size());    // il y a 3 ayants-droits dans le fichier d'import et ils sont tous identiques à ceux dans la DB;
			return null;
		});
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsqu'on importe un fichier RF et que les ayant-droits dans la base ne correspondent pas.
	 */
	@Test
	public void testImportAyantsDroitsAvecModifications() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/export_ayantsdroits_rf_hebdo.xml");
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
		final Long importId = doInNewTransaction(status -> {
			final EvenementRFImport importEvent = new EvenementRFImport();
			importEvent.setType(TypeImportRF.PRINCIPAL);
			importEvent.setDateEvenement(dateSecondImport);
			importEvent.setEtat(EtatEvenementRF.A_TRAITER);
			importEvent.setFileUrl(raftUrl);
			return evenementRFImportDAO.save(importEvent).getId();
		});
		assertNotNull(importId);

		// on insère les données des ayant-droits dans la base
		doInNewTransaction(status -> {
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
			return null;
		});

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
		doInNewTransaction(status -> {
			final EvenementRFImport importEvent = evenementRFImportDAO.get(importId);
			assertNotNull(importEvent);
			assertEquals(EtatEvenementRF.TRAITE, importEvent.getEtat());
			return null;
		});

		// on vérifie que les mutations attendues sont bien dans la DB
		doInNewTransaction(status -> {
			final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
			assertEquals(3, mutations.size());    // les 3 ayants-droits dans le fichier d'import sont tous différents
			Collections.sort(mutations, new MutationComparator());

			final EvenementRFMutation mut0 = mutations.get(0);
			assertEquals(importId, mut0.getParentImport().getId());
			assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
			assertEquals(TypeEntiteRF.AYANT_DROIT, mut0.getTypeEntite());
			assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
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
			assertEquals(TypeMutationRF.MODIFICATION, mut1.getTypeMutation());
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
			assertEquals(TypeMutationRF.MODIFICATION, mut2.getTypeMutation());
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
			return null;
		});
	}

}