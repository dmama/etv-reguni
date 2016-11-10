package ch.vd.uniregctb.registrefoncier;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.jetbrains.annotations.Nullable;
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
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.SurfaceAuSolRFDAO;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TraiterImportRFSurfaceAuSolJobTest extends ImportRFTestClass {

	private BatchScheduler batchScheduler;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private SurfaceAuSolRFDAO surfaceAuSolRFDAO;
	private ZipRaftEsbStore zipRaftEsbStore;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		surfaceAuSolRFDAO = getBean(SurfaceAuSolRFDAO.class, "surfaceAuSolRFDAO");
		zipRaftEsbStore = getBean(ZipRaftEsbStore.class, "zipRaftEsbStore");
	}

	/**
	 * Ce test vérifie que les mutations sont bien créées lorsqu'on importe un fichier RF sur une base vide
	 */
	@Test
	public void testImportSurfacesAuSolBaseVide() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_surfaceausol_rf_hebdo.xml");
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
				assertEquals(4, mutations.size());    // il y a 2 immeuble + 3 surfaces (dont deux sur le même immeuble, donc une seule mutation) dans le fichier d'import et la DB était vide
				Collections.sort(mutations, new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.IMMEUBLE, mut0.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut0.getTypeMutation());
				assertEquals("_1f109152381009be0138100bc9f139e0", mut0.getIdImmeubleRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<Liegenschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckID>_1f109152381009be0138100bc9f139e0</GrundstueckID>\n" +
						             "    <EGrid>CH938383459516</EGrid>\n" +
						             "    <GrundstueckNummer VersionID=\"1f109152381009be0138100bc9f33a1a\">\n" +
						             "        <BfsNr>273</BfsNr>\n" +
						             "        <Gemeindenamen>Rances</Gemeindenamen>\n" +
						             "        <StammNr>3</StammNr>\n" +
						             "    </GrundstueckNummer>\n" +
						             "    <IstKopie>false</IstKopie>\n" +
						             "    <AmtlicheBewertung VersionID=\"1f109152381009be0138100cfd906087\">\n" +
						             "        <AmtlicherWert>1100000</AmtlicherWert>\n" +
						             "        <Ertragswert>0</Ertragswert>\n" +
						             "        <ProtokollNr>RG96</ProtokollNr>\n" +
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
						             "    <NutzungLandwirtschaft>unbekannt</NutzungLandwirtschaft>\n" +
						             "    <NutzungWald>unbekannt</NutzungWald>\n" +
						             "    <NutzungEisenbahn>nein</NutzungEisenbahn>\n" +
						             "    <NutzungVerwaltungsvermoegen>unbekannt</NutzungVerwaltungsvermoegen>\n" +
						             "    <GrundstueckFlaeche VersionID=\"1f109152381009be0138100dbd7c1856\" MasterID=\"1f109152381009be0138100dbd7c1855\">\n" +
						             "        <Flaeche>2969451</Flaeche>\n" +
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
				assertEquals(EvenementRFMutation.TypeEntite.IMMEUBLE, mut1.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut1.getTypeMutation());
				assertEquals("_1f109152381037590138103b73cf579a", mut1.getIdImmeubleRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<Liegenschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckID>_1f109152381037590138103b73cf579a</GrundstueckID>\n" +
						             "    <EGrid>CH344583712491</EGrid>\n" +
						             "    <GrundstueckNummer VersionID=\"1f109152381037590138103b73d157ef\">\n" +
						             "        <BfsNr>71</BfsNr>\n" +
						             "        <Gemeindenamen>Penthalaz</Gemeindenamen>\n" +
						             "        <StammNr>428</StammNr>\n" +
						             "    </GrundstueckNummer>\n" +
						             "    <IstKopie>false</IstKopie>\n" +
						             "    <AmtlicheBewertung VersionID=\"1f109152381037590138103e0e937938\">\n" +
						             "        <AmtlicherWert>14000</AmtlicherWert>\n" +
						             "        <Ertragswert>0</Ertragswert>\n" +
						             "        <ProtokollNr>EF01</ProtokollNr>\n" +
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
						             "    <NutzungLandwirtschaft>ja</NutzungLandwirtschaft>\n" +
						             "    <NutzungWald>unbekannt</NutzungWald>\n" +
						             "    <NutzungEisenbahn>nein</NutzungEisenbahn>\n" +
						             "    <NutzungVerwaltungsvermoegen>nein</NutzungVerwaltungsvermoegen>\n" +
						             "    <GrundstueckFlaeche VersionID=\"1f109152381037590138103fe09a0ba1\" MasterID=\"1f109152381037590138103fe09a0ba0\">\n" +
						             "        <Flaeche>17814</Flaeche>\n" +
						             "        <Qualitaet>\n" +
						             "            <TextDe>*numérisé</TextDe>\n" +
						             "            <TextFr>numérisé</TextFr>\n" +
						             "        </Qualitaet>\n" +
						             "        <ProjektMutation>false</ProjektMutation>\n" +
						             "        <GeometrischDarstellbar>false</GeometrischDarstellbar>\n" +
						             "        <UeberlagerndeRechte>false</UeberlagerndeRechte>\n" +
						             "    </GrundstueckFlaeche>\n" +
						             "</Liegenschaft>\n", mut1.getXmlContent());

				final EvenementRFMutation mut2 = mutations.get(2);
				assertEquals(importId, mut2.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.SURFACE_AU_SOL, mut2.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut2.getTypeMutation());
				assertEquals("_1f109152381009be0138100bc9f139e0", mut2.getIdImmeubleRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<BodenbedeckungList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <Bodenbedeckung VersionID=\"1f109152381009be0138100ce8190795\">\n" +
						             "        <GrundstueckIDREF>_1f109152381009be0138100bc9f139e0</GrundstueckIDREF>\n" +
						             "        <Art>\n" +
						             "            <TextDe>*Pâturage</TextDe>\n" +
						             "            <TextFr>Pâturage</TextFr>\n" +
						             "        </Art>\n" +
						             "        <Flaeche>1125519</Flaeche>\n" +
						             "    </Bodenbedeckung>\n" +
						             "    <Bodenbedeckung VersionID=\"1f109152381009be0138100ce7df0741\">\n" +
						             "        <GrundstueckIDREF>_1f109152381009be0138100bc9f139e0</GrundstueckIDREF>\n" +
						             "        <Art>\n" +
						             "            <TextDe>*Pré-champ</TextDe>\n" +
						             "            <TextFr>Pré-champ</TextFr>\n" +
						             "        </Art>\n" +
						             "        <Flaeche>570</Flaeche>\n" +
						             "    </Bodenbedeckung>\n" +
						             "</BodenbedeckungList>\n", mut2.getXmlContent());

				final EvenementRFMutation mut3 = mutations.get(3);
				assertEquals(importId, mut3.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut3.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.SURFACE_AU_SOL, mut3.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut3.getTypeMutation());
				assertEquals("_1f109152381037590138103b73cf579a", mut3.getIdImmeubleRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<BodenbedeckungList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <Bodenbedeckung VersionID=\"1f109152381037590138103dd5f12466\">\n" +
						             "        <GrundstueckIDREF>_1f109152381037590138103b73cf579a</GrundstueckIDREF>\n" +
						             "        <Art>\n" +
						             "            <TextDe>*Pré-champ</TextDe>\n" +
						             "            <TextFr>Pré-champ</TextFr>\n" +
						             "        </Art>\n" +
						             "        <Flaeche>17814</Flaeche>\n" +
						             "    </Bodenbedeckung>\n" +
						             "</BodenbedeckungList>\n", mut3.getXmlContent());
			}
		});

	}

	/**
	 * Ce test vérifie qu'aucune mutation n'est créées lorsqu'on importe un fichier RF et que les surfaces au sol dans la base sont déjà à jour.
	 */
	@Test
	public void testImportSurfacesAuSolDejaAJour() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2008, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2010, 1, 1);
		final RegDate dateTroisiemeImport = RegDate.get(2016, 10, 1);

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_surfaceausol_rf_hebdo.xml");
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
				importEvent.setDateEvenement(dateTroisiemeImport);
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
				// données équivalentes au fichier export_surfaceausol_rf_hebdo.xml
				BienFondRF bienFond1 = newBienFondRF("_1f109152381009be0138100bc9f139e0", "CH938383459516", 273, 3, 1100000L, "RG96", null, false, false, dateImportInitial, 2969451);
				BienFondRF bienFond2 = newBienFondRF("_1f109152381037590138103b73cf579a", "CH344583712491", 71, 428, 14000L, "EF01", null, false, false, dateImportInitial, 17814);
				bienFond1 = (BienFondRF) immeubleRFDAO.save(bienFond1);
				bienFond2 = (BienFondRF) immeubleRFDAO.save(bienFond2);

				// quelques données historiques (qui doivent être ignorées)
				SurfaceAuSolRF surface1_1 = newSurfaceAuSol(bienFond1, "Pâturage pluvial", 1125519, dateImportInitial, dateSecondImport.getOneDayBefore());
				SurfaceAuSolRF surface2_1 = newSurfaceAuSol(bienFond1, "Pré-champ", 270, dateImportInitial, dateSecondImport.getOneDayBefore());
				SurfaceAuSolRF surface3_1 = newSurfaceAuSol(bienFond2, "Pré-champ-cathédrale", 17814, dateImportInitial, dateSecondImport.getOneDayBefore());
				surfaceAuSolRFDAO.save(surface1_1);
				surfaceAuSolRFDAO.save(surface2_1);
				surfaceAuSolRFDAO.save(surface3_1);

				SurfaceAuSolRF surface1_2 = newSurfaceAuSol(bienFond1, "Pâturage", 1125519, dateSecondImport, null);
				SurfaceAuSolRF surface2_2 = newSurfaceAuSol(bienFond1, "Pré-champ", 570, dateSecondImport, null);
				SurfaceAuSolRF surface3_2 = newSurfaceAuSol(bienFond2, "Pré-champ", 17814, dateSecondImport, null);
				surfaceAuSolRFDAO.save(surface1_2);
				surfaceAuSolRFDAO.save(surface2_2);
				surfaceAuSolRFDAO.save(surface3_2);
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
				assertEquals(0, mutations.size());    // il y a 2 immeubles et 3 surfaces au sol dans le fichier d'import et ils sont tous identiques à ceux dans la DB
			}
		});
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsqu'on importe un fichier RF et que les surfaces au sol dans la base ne correspondent pas.
	 */
	@Test
	public void testImportSurfacesAuSolAvecModifications() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_surfaceausol_rf_hebdo.xml");
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

				// données partiellement différentes de celles du fichier export_surfaceausol_rf_hebdo.xml
				BienFondRF bienFond1 = newBienFondRF("_1f109152381009be0138100bc9f139e0", "CH938383459516", 273, 3, 1100000L, "RG96", null, false, false, dateImportInitial, 2969451);
				BienFondRF bienFond2 = newBienFondRF("_1f109152381037590138103b73cf579a", "CH344583712491", 71, 428, 14000L, "EF01", null, false, false, dateImportInitial, 17814);
				bienFond1 = (BienFondRF) immeubleRFDAO.save(bienFond1);
				bienFond2 = (BienFondRF) immeubleRFDAO.save(bienFond2);

				// - surface différente
				SurfaceAuSolRF surface1 = newSurfaceAuSol(bienFond1, "Pâturage", 660066, dateImportInitial, null);
				// (identique)
				SurfaceAuSolRF surface2 = newSurfaceAuSol(bienFond1, "Pré-champ", 570, dateImportInitial, null);
				// - désignation différente
				SurfaceAuSolRF surface3 = newSurfaceAuSol(bienFond2, "Décharge nucléaire", 17814, dateImportInitial, null);
				surfaceAuSolRFDAO.save(surface1);
				surfaceAuSolRFDAO.save(surface2);
				surfaceAuSolRFDAO.save(surface3);
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
				assertEquals(2, mutations.size());    // les 2 surfaces au sol dans le fichier d'import sont différentes
				Collections.sort(mutations, new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.SURFACE_AU_SOL, mut0.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.MODIFICATION, mut0.getTypeMutation());
				assertEquals("_1f109152381009be0138100bc9f139e0", mut0.getIdImmeubleRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<BodenbedeckungList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <Bodenbedeckung VersionID=\"1f109152381009be0138100ce8190795\">\n" +
						             "        <GrundstueckIDREF>_1f109152381009be0138100bc9f139e0</GrundstueckIDREF>\n" +
						             "        <Art>\n" +
						             "            <TextDe>*Pâturage</TextDe>\n" +
						             "            <TextFr>Pâturage</TextFr>\n" +
						             "        </Art>\n" +
						             "        <Flaeche>1125519</Flaeche>\n" +
						             "    </Bodenbedeckung>\n" +
						             "    <Bodenbedeckung VersionID=\"1f109152381009be0138100ce7df0741\">\n" +
						             "        <GrundstueckIDREF>_1f109152381009be0138100bc9f139e0</GrundstueckIDREF>\n" +
						             "        <Art>\n" +
						             "            <TextDe>*Pré-champ</TextDe>\n" +
						             "            <TextFr>Pré-champ</TextFr>\n" +
						             "        </Art>\n" +
						             "        <Flaeche>570</Flaeche>\n" +
						             "    </Bodenbedeckung>\n" +
						             "</BodenbedeckungList>\n", mut0.getXmlContent());

				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.SURFACE_AU_SOL, mut1.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.MODIFICATION, mut1.getTypeMutation());
				assertEquals("_1f109152381037590138103b73cf579a", mut1.getIdImmeubleRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<BodenbedeckungList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <Bodenbedeckung VersionID=\"1f109152381037590138103dd5f12466\">\n" +
						             "        <GrundstueckIDREF>_1f109152381037590138103b73cf579a</GrundstueckIDREF>\n" +
						             "        <Art>\n" +
						             "            <TextDe>*Pré-champ</TextDe>\n" +
						             "            <TextFr>Pré-champ</TextFr>\n" +
						             "        </Art>\n" +
						             "        <Flaeche>17814</Flaeche>\n" +
						             "    </Bodenbedeckung>\n" +
						             "</BodenbedeckungList>\n", mut1.getXmlContent());
			}
		});
	}

	/**
	 * Trie par type, par idRF et par id pour avoir qqch de stable.
 	 */
	private static class MutationComparator implements Comparator<EvenementRFMutation> {
		@Override
		public int compare(EvenementRFMutation o1, EvenementRFMutation o2) {
			final int c1 = o1.getTypeEntite().compareTo(o2.getTypeEntite());
			if (c1 != 0) {
				return c1;
			}
			final int c2 = compareString(o1.getIdImmeubleRF(), o2.getIdImmeubleRF());
			if (c2 != 0) {
				return c2;
			}
			return o1.getId().compareTo(o2.getId());
		}
	}

	private static int compareString(@Nullable String idRF1, @Nullable String idRF2) {
		if (idRF1 == null && idRF2 == null) {
			return 0;
		}
		else if (idRF1 == null) {
			return -1;
		}
		else if (idRF2 == null) {
			return 1;
		}
		else {
			return idRF1.compareTo(idRF2);
		}
	}

}