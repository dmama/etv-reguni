package ch.vd.uniregctb.registrefoncier.dataimport;

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
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.dao.BatimentRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TraiterImportRFBatimentsJobTest extends ImportRFTestClass {

	private BatchScheduler batchScheduler;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private BatimentRFDAO batimentRFDAO;
	private ZipRaftEsbStore zipRaftEsbStore;
	private CommuneRFDAO communeRFDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		batimentRFDAO = getBean(BatimentRFDAO.class, "batimentRFDAO");
		zipRaftEsbStore = getBean(ZipRaftEsbStore.class, "zipRaftEsbStore");
		communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
	}

	/**
	 * Ce test vérifie que les mutations de type CREATION sont bien créées lorsqu'on importe un fichier RF sur une base vide
	 */
	@Test
	public void testImportBatimentsBaseVide() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_batiments_rf_hebdo.xml");
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
				assertEquals(4, mutations.size());    // il y a 1 immeuble et 2 bâtiments dans le fichier d'import et la DB était vide
				Collections.sort(mutations, new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(TypeEntiteRF.IMMEUBLE, mut0.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
				assertEquals("_1f109152381026b501381028a73d1852", mut0.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<Liegenschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckID>_1f109152381026b501381028a73d1852</GrundstueckID>\n" +
						             "    <EGrid>CH938391457759</EGrid>\n" +
						             "    <GrundstueckNummer VersionID=\"1f109152381026b501381028a74018e1\">\n" +
						             "        <BfsNr>294</BfsNr>\n" +
						             "        <Gemeindenamen>Oron</Gemeindenamen>\n" +
						             "        <StammNr>5089</StammNr>\n" +
						             "    </GrundstueckNummer>\n" +
						             "    <IstKopie>false</IstKopie>\n" +
						             "    <AmtlicheBewertung VersionID=\"1f109152381026b50138102a464e3d9b\">\n" +
						             "        <AmtlicherWert>260000</AmtlicherWert>\n" +
						             "        <Ertragswert>0</Ertragswert>\n" +
						             "        <ProtokollNr>RG93</ProtokollNr>\n" +
						             "        <ProtokollGueltig>true</ProtokollGueltig>\n" +
						             "        <MitEwKomponente>unbekannt</MitEwKomponente>\n" +
						             "    </AmtlicheBewertung>\n" +
						             "    <GrundbuchFuehrung>Eidgenoessisch</GrundbuchFuehrung>\n" +
						             "    <BeschreibungUebergeben>true</BeschreibungUebergeben>\n" +
						             "    <EigentumUebergeben>true</EigentumUebergeben>\n" +
						             "    <PfandrechtUebergeben>true</PfandrechtUebergeben>\n" +
						             "    <DienstbarkeitUebergeben>true</DienstbarkeitUebergeben>\n" +
						             "    <GrundlastUebergeben>true</GrundlastUebergeben>\n" +
						             "    <AnmerkungUebergeben>true</AnmerkungUebergeben>\n" +
						             "    <VormerkungUebergeben>true</VormerkungUebergeben>\n" +
						             "    <Bereinigungsmarkierung>false</Bereinigungsmarkierung>\n" +
						             "    <BereitZurVerifikation>false</BereitZurVerifikation>\n" +
						             "    <NutzungLandwirtschaft>nein</NutzungLandwirtschaft>\n" +
						             "    <NutzungWald>unbekannt</NutzungWald>\n" +
						             "    <NutzungEisenbahn>nein</NutzungEisenbahn>\n" +
						             "    <NutzungVerwaltungsvermoegen>nein</NutzungVerwaltungsvermoegen>\n" +
						             "    <GrundstueckFlaeche VersionID=\"1f109152381026b50138102b2cdc4b91\" MasterID=\"1f109152381026b50138102b2cdc4b90\">\n" +
						             "        <Flaeche>707</Flaeche>\n" +
						             "        <Qualitaet>\n" +
						             "            <TextDe>*numérisé</TextDe>\n" +
						             "            <TextFr>numérisé</TextFr>\n" +
						             "        </Qualitaet>\n" +
						             "        <ProjektMutation>false</ProjektMutation>\n" +
						             "        <GeometrischDarstellbar>false</GeometrischDarstellbar>\n" +
						             "        <UeberlagerndeRechte>false</UeberlagerndeRechte>\n" +
						             "    </GrundstueckFlaeche>\n" +
						             "</Liegenschaft>\n", mut0.getXmlContent());

				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(TypeEntiteRF.BATIMENT, mut1.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
				assertEquals("1f109152381026b50138102aa28557e0", mut1.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<Gebaeude VersionID=\"1f109152381026b50138102aa2875806\" MasterID=\"1f109152381026b50138102aa28557e0\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckZuGebaeude>\n" +
						             "        <GrundstueckIDREF>_1f109152381026b501381028a73d1852</GrundstueckIDREF>\n" +
						             "        <AbschnittFlaeche>104</AbschnittFlaeche>\n" +
						             "    </GrundstueckZuGebaeude>\n" +
						             "    <Einzelobjekt>false</Einzelobjekt>\n" +
						             "    <Unterirdisch>false</Unterirdisch>\n" +
						             "    <MehrereGrundstuecke>false</MehrereGrundstuecke>\n" +
						             "    <GebaeudeArten>\n" +
						             "        <GebaeudeArtCode>\n" +
						             "            <TextDe>*Habitation</TextDe>\n" +
						             "            <TextFr>Habitation</TextFr>\n" +
						             "        </GebaeudeArtCode>\n" +
						             "    </GebaeudeArten>\n" +
						             "    <Versicherungsnummer>3064</Versicherungsnummer>\n" +
						             "</Gebaeude>\n", mut1.getXmlContent());

				final EvenementRFMutation mut2 = mutations.get(2);
				assertEquals(importId, mut2.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
				assertEquals(TypeEntiteRF.BATIMENT, mut2.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut2.getTypeMutation());
				assertEquals("1f10915238102ecd01381032b52802a1", mut2.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<Gebaeude VersionID=\"1f10915238102ecd01381032b52c02fa\" MasterID=\"1f10915238102ecd01381032b52802a1\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckZuGebaeude>\n" +
						             "        <GrundstueckIDREF>_1f109152381026b501381028a73d1852</GrundstueckIDREF>\n" +
						             "        <AbschnittFlaeche>36</AbschnittFlaeche>\n" +
						             "    </GrundstueckZuGebaeude>\n" +
						             "    <Einzelobjekt>false</Einzelobjekt>\n" +
						             "    <Unterirdisch>false</Unterirdisch>\n" +
						             "    <MehrereGrundstuecke>false</MehrereGrundstuecke>\n" +
						             "    <GebaeudeArten>\n" +
						             "        <GebaeudeArtCode>\n" +
						             "            <TextDe>*Garage</TextDe>\n" +
						             "            <TextFr>Garage</TextFr>\n" +
						             "        </GebaeudeArtCode>\n" +
						             "    </GebaeudeArten>\n" +
						             "    <Versicherungsnummer>3007</Versicherungsnummer>\n" +
						             "</Gebaeude>\n", mut2.getXmlContent());

				final EvenementRFMutation mut3 = mutations.get(3);
				assertEquals(importId, mut3.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut3.getEtat());
				assertEquals(TypeEntiteRF.COMMUNE, mut3.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut3.getTypeMutation());
				assertEquals("294", mut3.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <BfsNr>294</BfsNr>\n" +
						             "    <Gemeindenamen>Oron</Gemeindenamen>\n" +
						             "    <StammNr>0</StammNr>\n" +
						             "</GrundstueckNummer>\n", mut3.getXmlContent());
			}
		});

	}

	/**
	 * Ce test vérifie qu'aucune mutation n'est créées lorsqu'on importe un fichier RF et que les immeubles dans la base sont déjà à jour.
	 */
	@Test
	public void testImportBatimentsDejaAJour() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2008, 1, 1);

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_batiments_rf_hebdo.xml");
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

				final CommuneRF oron = communeRFDAO.save(newCommuneRF(294, "Oron", 5555));

				// données équivalentes au fichier export_batiments_rf_hebdo.xml
				final BienFondRF bienFond = (BienFondRF) immeubleRFDAO.save(newBienFondRF("_1f109152381026b501381028a73d1852", "CH938391457759", oron, 5089, 260000L, "RG93", null, false, false, RegDate.get(2010, 1, 1), 707));

				final BatimentRF batiment1 = newBatimentRF("1f109152381026b50138102aa28557e0", "Habitation");
				batiment1.addImplantation(newImplantationRF(bienFond, 104, dateImportInitial, null));
				batimentRFDAO.save(batiment1);

				final BatimentRF batiment2 = newBatimentRF("1f10915238102ecd01381032b52802a1", "Garage");
				batiment2.addImplantation(newImplantationRF(bienFond, 36, dateImportInitial, null));
				batimentRFDAO.save(batiment2);
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
				assertEquals(0, mutations.size());    // il y a 1 immeuble et 2 bâtiments dans le fichier d'import et ils sont tous identiques à ceux dans la DB
			}
		});
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsqu'on importe un fichier RF et que les bâtiments dans la base ne correspondent pas.
	 */
	@Test
	public void testImportBatimentsAvecModifications() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_batiments_rf_hebdo.xml");
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

				final CommuneRF oron = communeRFDAO.save(newCommuneRF(294, "Oron", 5555));

				// données équivalentes au fichier export_batiments_rf_hebdo.xml
				final BienFondRF bienFond = (BienFondRF) immeubleRFDAO.save(newBienFondRF("_1f109152381026b501381028a73d1852", "CH938391457759", oron, 5089, 260000L, "RG93", null, false, false, RegDate.get(2010, 1, 1), 707));

				final BatimentRF batiment1 = newBatimentRF("1f109152381026b50138102aa28557e0", "Habitation");
				// - surface différente
				batiment1.addImplantation(newImplantationRF(bienFond, 140, dateImportInitial, null));
				batimentRFDAO.save(batiment1);

				// - pas de changement sur le garage
				final BatimentRF batiment2 = newBatimentRF("1f10915238102ecd01381032b52802a1", "Garage");
				batiment2.addImplantation(newImplantationRF(bienFond, 36, dateImportInitial, null));
				batimentRFDAO.save(batiment2);

				// - disparition de la volière
				final BatimentRF batiment3 = newBatimentRF("1f10915238102ecd01381032b52802cc", "Vollière");
				batiment3.addImplantation(newImplantationRF(bienFond, 4, dateImportInitial, null));
				batimentRFDAO.save(batiment3);

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
				assertEquals(2, mutations.size());
				Collections.sort(mutations, new MutationComparator());

				// la surface est différente
				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(TypeEntiteRF.BATIMENT, mut0.getTypeEntite());
				assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
				assertEquals("1f109152381026b50138102aa28557e0", mut0.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<Gebaeude VersionID=\"1f109152381026b50138102aa2875806\" MasterID=\"1f109152381026b50138102aa28557e0\" xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckZuGebaeude>\n" +
						             "        <GrundstueckIDREF>_1f109152381026b501381028a73d1852</GrundstueckIDREF>\n" +
						             "        <AbschnittFlaeche>104</AbschnittFlaeche>\n" +
						             "    </GrundstueckZuGebaeude>\n" +
						             "    <Einzelobjekt>false</Einzelobjekt>\n" +
						             "    <Unterirdisch>false</Unterirdisch>\n" +
						             "    <MehrereGrundstuecke>false</MehrereGrundstuecke>\n" +
						             "    <GebaeudeArten>\n" +
						             "        <GebaeudeArtCode>\n" +
						             "            <TextDe>*Habitation</TextDe>\n" +
						             "            <TextFr>Habitation</TextFr>\n" +
						             "        </GebaeudeArtCode>\n" +
						             "    </GebaeudeArten>\n" +
						             "    <Versicherungsnummer>3064</Versicherungsnummer>\n" +
						             "</Gebaeude>\n", mut0.getXmlContent());

				// la volière n'existe plus
				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(TypeEntiteRF.BATIMENT, mut1.getTypeEntite());
				assertEquals(TypeMutationRF.SUPPRESSION, mut1.getTypeMutation());
				assertEquals("1f10915238102ecd01381032b52802cc", mut1.getIdRF());
				assertNull(mut1.getXmlContent());
			}
		});
	}

}