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
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.PartCoproprieteRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TraiterImportRFImmeublesJobTest extends ImportRFTestClass {

	private BatchScheduler batchScheduler;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private ZipRaftEsbStore zipRaftEsbStore;
	private CommuneRFDAO communeRFDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		zipRaftEsbStore = getBean(ZipRaftEsbStore.class, "zipRaftEsbStore");
		communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
	}

	/**
	 * Ce test vérifie que les mutations de type CREATION sont bien créées lorsqu'on importe un fichier RF sur une base vide
	 */
	@Test
	public void testImportImmeublesBaseVide() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_immeubles_rf_hebdo.xml");
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
				assertEquals(7, mutations.size());    // il y a 3 communes + 4 immeubles dans le fichier d'import et la DB était vide
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
				assertEquals(TypeEntiteRF.IMMEUBLE, mut1.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
				assertEquals("_8af806cc3971feb60139e36d062130f3", mut1.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<Sdr xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckID>_8af806cc3971feb60139e36d062130f3</GrundstueckID>\n" +
						             "    <EGrid>CH729253834531</EGrid>\n" +
						             "    <GrundstueckNummer VersionID=\"1f109152381026b501381028ab3f31b8\">\n" +
						             "        <BfsNr>294</BfsNr>\n" +
						             "        <Gemeindenamen>Oron</Gemeindenamen>\n" +
						             "        <StammNr>692</StammNr>\n" +
						             "    </GrundstueckNummer>\n" +
						             "    <IstKopie>false</IstKopie>\n" +
						             "    <AmtlicheBewertung VersionID=\"8af8064d567f817b015722fa93bc63c8\">\n" +
						             "        <AmtlicherWert>2120000</AmtlicherWert>\n" +
						             "        <ProtokollNr>2016</ProtokollNr>\n" +
						             "        <ProtokollDatum>2016-09-13</ProtokollDatum>\n" +
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
						             "    <NutzungWald>nein</NutzungWald>\n" +
						             "    <NutzungEisenbahn>nein</NutzungEisenbahn>\n" +
						             "    <NutzungVerwaltungsvermoegen>nein</NutzungVerwaltungsvermoegen>\n" +
						             "    <GrundstueckFlaeche VersionID=\"8af806cc3971feb60139e36d088230f9\" MasterID=\"8af806cc3971feb60139e36d088230f8\">\n" +
						             "        <Flaeche>4896</Flaeche>\n" +
						             "        <Qualitaet>\n" +
						             "            <TextDe>*numérisé</TextDe>\n" +
						             "            <TextFr>numérisé</TextFr>\n" +
						             "        </Qualitaet>\n" +
						             "        <ProjektMutation>false</ProjektMutation>\n" +
						             "        <GeometrischDarstellbar>false</GeometrischDarstellbar>\n" +
						             "        <UeberlagerndeRechte>false</UeberlagerndeRechte>\n" +
						             "        <Rechtsgruende>\n" +
						             "            <AmtNummer>9</AmtNummer>\n" +
						             "            <RechtsgrundCode>\n" +
						             "                <TextDe>*Cadastration</TextDe>\n" +
						             "                <TextFr>Cadastration</TextFr>\n" +
						             "            </RechtsgrundCode>\n" +
						             "            <BelegDatum>2016-02-22</BelegDatum>\n" +
						             "            <BelegJahr>2016</BelegJahr>\n" +
						             "            <BelegNummer>559</BelegNummer>\n" +
						             "            <BelegNummerIndex>0</BelegNummerIndex>\n" +
						             "        </Rechtsgruende>\n" +
						             "    </GrundstueckFlaeche>\n" +
						             "    <RechtArt>bauRecht</RechtArt>\n" +
						             "</Sdr>\n", mut1.getXmlContent());

				final EvenementRFMutation mut2 = mutations.get(2);
				assertEquals(importId, mut2.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
				assertEquals(TypeEntiteRF.IMMEUBLE, mut2.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut2.getTypeMutation());
				assertEquals("_8af806cc5043853201508e1e8a3a1a71", mut2.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<GewoehnlichesMiteigentum xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckID>_8af806cc5043853201508e1e8a3a1a71</GrundstueckID>\n" +
						             "    <EGrid>CH516579658411</EGrid>\n" +
						             "    <GrundstueckNummer VersionID=\"8af806fc4e7cb94d014ffe532ff45108\">\n" +
						             "        <BfsNr>308</BfsNr>\n" +
						             "        <Gemeindenamen>Corcelles-près-Payerne</Gemeindenamen>\n" +
						             "        <StammNr>3601</StammNr>\n" +
						             "        <IndexNr1>7</IndexNr1>\n" +
						             "        <IndexNr2>13</IndexNr2>\n" +
						             "    </GrundstueckNummer>\n" +
						             "    <IstKopie>false</IstKopie>\n" +
						             "    <AmtlicheBewertung VersionID=\"8af806cc5043853201508ec7ede11e40\">\n" +
						             "        <AmtlicherWert>550</AmtlicherWert>\n" +
						             "        <ProtokollNr>2015</ProtokollNr>\n" +
						             "        <ProtokollDatum>2015-10-22</ProtokollDatum>\n" +
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
						             "    <NutzungWald>nein</NutzungWald>\n" +
						             "    <NutzungEisenbahn>nein</NutzungEisenbahn>\n" +
						             "    <NutzungVerwaltungsvermoegen>nein</NutzungVerwaltungsvermoegen>\n" +
						             "    <StammGrundstueck VersionID=\"8af806cc5043853201508e1e8a811a75\">\n" +
						             "        <Quote>\n" +
						             "            <AnteilZaehler>1</AnteilZaehler>\n" +
						             "            <AnteilNenner>18</AnteilNenner>\n" +
						             "            <QuoteUnbekannt>false</QuoteUnbekannt>\n" +
						             "        </Quote>\n" +
						             "        <BelastetesGrundstueck VersionID=\"8af806cc5043853201508e1c04c419e6\">\n" +
						             "            <EGrid>CH487965658402</EGrid>\n" +
						             "            <GrundstueckNummer>\n" +
						             "                <BfsNr>308</BfsNr>\n" +
						             "                <Gemeindenamen>Corcelles-près-Payerne</Gemeindenamen>\n" +
						             "                <StammNr>3601</StammNr>\n" +
						             "                <IndexNr1>7</IndexNr1>\n" +
						             "            </GrundstueckNummer>\n" +
						             "            <GrundstueckArt>StockwerksEinheit</GrundstueckArt>\n" +
						             "        </BelastetesGrundstueck>\n" +
						             "    </StammGrundstueck>\n" +
						             "</GewoehnlichesMiteigentum>\n", mut2.getXmlContent());

				final EvenementRFMutation mut3 = mutations.get(3);
				assertEquals(importId, mut3.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut3.getEtat());
				assertEquals(TypeEntiteRF.IMMEUBLE, mut3.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut3.getTypeMutation());
				assertEquals("_8af806fc45d223e60149c23f475365d5", mut3.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<StockwerksEinheit xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckID>_8af806fc45d223e60149c23f475365d5</GrundstueckID>\n" +
						             "    <EGrid>CH336583651349</EGrid>\n" +
						             "    <GrundstueckNummer VersionID=\"8af806fc45d223e60149139250510365\">\n" +
						             "        <BfsNr>190</BfsNr>\n" +
						             "        <Gemeindenamen>Boulens</Gemeindenamen>\n" +
						             "        <StammNr>19</StammNr>\n" +
						             "        <IndexNr1>4</IndexNr1>\n" +
						             "    </GrundstueckNummer>\n" +
						             "    <IstKopie>false</IstKopie>\n" +
						             "    <AmtlicheBewertung VersionID=\"8af8064d567f817b015723af4e8167a3\">\n" +
						             "        <AmtlicherWert>495000</AmtlicherWert>\n" +
						             "        <ProtokollNr>2016</ProtokollNr>\n" +
						             "        <ProtokollDatum>2016-09-13</ProtokollDatum>\n" +
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
						             "    <NutzungWald>nein</NutzungWald>\n" +
						             "    <NutzungEisenbahn>nein</NutzungEisenbahn>\n" +
						             "    <NutzungVerwaltungsvermoegen>nein</NutzungVerwaltungsvermoegen>\n" +
						             "    <StammGrundstueck VersionID=\"8af806fc45d223e60149c23f47cf65d9\">\n" +
						             "        <Quote>\n" +
						             "            <AnteilZaehler>293</AnteilZaehler>\n" +
						             "            <AnteilNenner>1000</AnteilNenner>\n" +
						             "            <QuoteUnbekannt>false</QuoteUnbekannt>\n" +
						             "        </Quote>\n" +
						             "        <BelastetesGrundstueck VersionID=\"1f109152381026b501381028c81f449a\">\n" +
						             "            <EGrid>CH528963834590</EGrid>\n" +
						             "            <GrundstueckNummer>\n" +
						             "                <BfsNr>190</BfsNr>\n" +
						             "                <Gemeindenamen>Boulens</Gemeindenamen>\n" +
						             "                <StammNr>19</StammNr>\n" +
						             "            </GrundstueckNummer>\n" +
						             "            <GrundstueckArt>Liegenschaft</GrundstueckArt>\n" +
						             "        </BelastetesGrundstueck>\n" +
						             "    </StammGrundstueck>\n" +
						             "</StockwerksEinheit>\n", mut3.getXmlContent());

				final EvenementRFMutation mut4 = mutations.get(4);
				assertEquals(importId, mut4.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut4.getEtat());
				assertEquals(TypeEntiteRF.COMMUNE, mut4.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut4.getTypeMutation());
				assertEquals("190", mut4.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <BfsNr>190</BfsNr>\n" +
						             "    <Gemeindenamen>Boulens</Gemeindenamen>\n" +
						             "    <StammNr>0</StammNr>\n" +
						             "</GrundstueckNummer>\n", mut4.getXmlContent());

				final EvenementRFMutation mut5 = mutations.get(5);
				assertEquals(importId, mut5.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut5.getEtat());
				assertEquals(TypeEntiteRF.COMMUNE, mut5.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut5.getTypeMutation());
				assertEquals("294", mut5.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <BfsNr>294</BfsNr>\n" +
						             "    <Gemeindenamen>Oron</Gemeindenamen>\n" +
						             "    <StammNr>0</StammNr>\n" +
						             "</GrundstueckNummer>\n", mut5.getXmlContent());

				final EvenementRFMutation mut6 = mutations.get(6);
				assertEquals(importId, mut6.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut6.getEtat());
				assertEquals(TypeEntiteRF.COMMUNE, mut6.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut6.getTypeMutation());
				assertEquals("308", mut6.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <BfsNr>308</BfsNr>\n" +
						             "    <Gemeindenamen>Corcelles-près-Payerne</Gemeindenamen>\n" +
						             "    <StammNr>0</StammNr>\n" +
						             "</GrundstueckNummer>\n", mut6.getXmlContent());
			}
		});

	}

	/**
	 * Ce test vérifie qu'aucune mutation n'est créées lorsqu'on importe un fichier RF et que les immeubles dans la base sont déjà à jour.
	 */
	@Test
	public void testImportImmeublesDejaAJour() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_immeubles_rf_hebdo.xml");
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
				final CommuneRF boulens = communeRFDAO.save(newCommuneRF(190, "Boulens", 5556));
				final CommuneRF corcelles = communeRFDAO.save(newCommuneRF(308, "Corcelles-près-Payerne", 5557));

				// données équivalentes au fichier export_immeubles_rf_hebdo.xml
				final BienFondRF bienFond = newBienFondRF("_1f109152381026b501381028a73d1852", "CH938391457759", oron, 5089, 260000L, "RG93", null, RegDate.get(1993, 1, 1), false, false, RegDate.get(2010, 1, 1), 707);
				final DroitDistinctEtPermanentRF droitDistinctEtPermanent = newDroitDistinctEtPermanentRF("_8af806cc3971feb60139e36d062130f3", "CH729253834531", oron, 692, 2120000L, "2016", RegDate.get(2016, 9, 13), RegDate.get(2016, 1, 1), false, RegDate.get(2010, 1, 1),
				                                                                                          4896);
				final ProprieteParEtageRF ppe = newProprieteParEtageRF("_8af806fc45d223e60149c23f475365d5", "CH336583651349", boulens, 19, 4, 495000L, "2016", RegDate.get(2016, 9, 13), RegDate.get(2016, 1, 1), false, new Fraction(293, 1000), RegDate.get(2010, 1, 1));
				final PartCoproprieteRF copropriete = newPartCoproprieteRF("_8af806cc5043853201508e1e8a3a1a71", "CH516579658411", corcelles, 3601, 7, 13, 550L, "2015", RegDate.get(2015, 10, 22), RegDate.get(2015, 1, 1), false, new Fraction(1, 18), RegDate.get(2010, 1, 1));

				immeubleRFDAO.save(bienFond);
				immeubleRFDAO.save(droitDistinctEtPermanent);
				immeubleRFDAO.save(ppe);
				immeubleRFDAO.save(copropriete);
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
				assertEquals(0, mutations.size());    // il y a 4 immeubles dans le fichier d'import et ils sont tous identiques à ceux dans la DB
			}
		});
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsqu'on importe un fichier RF et que les immeubles dans la base ne correspondent pas.
	 */
	@Test
	public void testImportImmeublesAvecModifications() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_immeubles_rf_hebdo.xml");
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
				final CommuneRF boulens = communeRFDAO.save(newCommuneRF(190, "Boulens", 5556));
				final CommuneRF corcelles = communeRFDAO.save(newCommuneRF(308, "Corcelles-près-Payerne", 5557));

				// données partiellement différentes de celles du fichier export_immeubles_rf_hebdo.xml
				// - données identiques
				final BienFondRF bienFond = newBienFondRF("_1f109152381026b501381028a73d1852", "CH938391457759", oron, 5089, 260000L, "RG93", null, RegDate.get(1993, 1, 1), false, false, dateImportInitial, 707);
				// - estimation fiscale différente
				final DroitDistinctEtPermanentRF droitDistinctEtPermanent = newDroitDistinctEtPermanentRF("_8af806cc3971feb60139e36d062130f3", "CH729253834531", oron, 692, 2000000L, "2015", RegDate.get(2015, 1, 1), RegDate.get(2015, 1, 1), false, dateImportInitial, 4896);
				// - données identiques
				final ProprieteParEtageRF ppe = newProprieteParEtageRF("_8af806fc45d223e60149c23f475365d5", "CH336583651349", boulens, 19, 4, 495000L, "2016", RegDate.get(2016, 9, 13), RegDate.get(2016, 1, 1), false, new Fraction(293, 1000), dateImportInitial);
				// - numéro de parcelle différente
				final PartCoproprieteRF copropriete = newPartCoproprieteRF("_8af806cc5043853201508e1e8a3a1a71", "CH516579658411", corcelles, 777, 7, 13, 550L, "2015", RegDate.get(2015, 10, 22), RegDate.get(2015, 1, 1), false, new Fraction(1, 18), dateImportInitial);

				immeubleRFDAO.save(bienFond);
				immeubleRFDAO.save(droitDistinctEtPermanent);
				immeubleRFDAO.save(ppe);
				immeubleRFDAO.save(copropriete);
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
				assertEquals(2, mutations.size());    // il y a 4 immeubles dans le fichier d'import et 4 immeubles dans le DB, dont 2 sont ont des données différentes
				Collections.sort(mutations, new MutationComparator());

				// l'estimation fiscales est différente
				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(TypeEntiteRF.IMMEUBLE, mut0.getTypeEntite());
				assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
				assertEquals("_8af806cc3971feb60139e36d062130f3", mut0.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<Sdr xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckID>_8af806cc3971feb60139e36d062130f3</GrundstueckID>\n" +
						             "    <EGrid>CH729253834531</EGrid>\n" +
						             "    <GrundstueckNummer VersionID=\"1f109152381026b501381028ab3f31b8\">\n" +
						             "        <BfsNr>294</BfsNr>\n" +
						             "        <Gemeindenamen>Oron</Gemeindenamen>\n" +
						             "        <StammNr>692</StammNr>\n" +
						             "    </GrundstueckNummer>\n" +
						             "    <IstKopie>false</IstKopie>\n" +
						             "    <AmtlicheBewertung VersionID=\"8af8064d567f817b015722fa93bc63c8\">\n" +
						             "        <AmtlicherWert>2120000</AmtlicherWert>\n" +
						             "        <ProtokollNr>2016</ProtokollNr>\n" +
						             "        <ProtokollDatum>2016-09-13</ProtokollDatum>\n" +
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
						             "    <NutzungWald>nein</NutzungWald>\n" +
						             "    <NutzungEisenbahn>nein</NutzungEisenbahn>\n" +
						             "    <NutzungVerwaltungsvermoegen>nein</NutzungVerwaltungsvermoegen>\n" +
						             "    <GrundstueckFlaeche VersionID=\"8af806cc3971feb60139e36d088230f9\" MasterID=\"8af806cc3971feb60139e36d088230f8\">\n" +
						             "        <Flaeche>4896</Flaeche>\n" +
						             "        <Qualitaet>\n" +
						             "            <TextDe>*numérisé</TextDe>\n" +
						             "            <TextFr>numérisé</TextFr>\n" +
						             "        </Qualitaet>\n" +
						             "        <ProjektMutation>false</ProjektMutation>\n" +
						             "        <GeometrischDarstellbar>false</GeometrischDarstellbar>\n" +
						             "        <UeberlagerndeRechte>false</UeberlagerndeRechte>\n" +
						             "        <Rechtsgruende>\n" +
						             "            <AmtNummer>9</AmtNummer>\n" +
						             "            <RechtsgrundCode>\n" +
						             "                <TextDe>*Cadastration</TextDe>\n" +
						             "                <TextFr>Cadastration</TextFr>\n" +
						             "            </RechtsgrundCode>\n" +
						             "            <BelegDatum>2016-02-22</BelegDatum>\n" +
						             "            <BelegJahr>2016</BelegJahr>\n" +
						             "            <BelegNummer>559</BelegNummer>\n" +
						             "            <BelegNummerIndex>0</BelegNummerIndex>\n" +
						             "        </Rechtsgruende>\n" +
						             "    </GrundstueckFlaeche>\n" +
						             "    <RechtArt>bauRecht</RechtArt>\n" +
						             "</Sdr>\n", mut0.getXmlContent());

				// le numéro de parcelle est différent
				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(TypeEntiteRF.IMMEUBLE, mut1.getTypeEntite());
				assertEquals(TypeMutationRF.MODIFICATION, mut1.getTypeMutation());
				assertEquals("_8af806cc5043853201508e1e8a3a1a71", mut1.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<GewoehnlichesMiteigentum xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckID>_8af806cc5043853201508e1e8a3a1a71</GrundstueckID>\n" +
						             "    <EGrid>CH516579658411</EGrid>\n" +
						             "    <GrundstueckNummer VersionID=\"8af806fc4e7cb94d014ffe532ff45108\">\n" +
						             "        <BfsNr>308</BfsNr>\n" +
						             "        <Gemeindenamen>Corcelles-près-Payerne</Gemeindenamen>\n" +
						             "        <StammNr>3601</StammNr>\n" +
						             "        <IndexNr1>7</IndexNr1>\n" +
						             "        <IndexNr2>13</IndexNr2>\n" +
						             "    </GrundstueckNummer>\n" +
						             "    <IstKopie>false</IstKopie>\n" +
						             "    <AmtlicheBewertung VersionID=\"8af806cc5043853201508ec7ede11e40\">\n" +
						             "        <AmtlicherWert>550</AmtlicherWert>\n" +
						             "        <ProtokollNr>2015</ProtokollNr>\n" +
						             "        <ProtokollDatum>2015-10-22</ProtokollDatum>\n" +
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
						             "    <NutzungWald>nein</NutzungWald>\n" +
						             "    <NutzungEisenbahn>nein</NutzungEisenbahn>\n" +
						             "    <NutzungVerwaltungsvermoegen>nein</NutzungVerwaltungsvermoegen>\n" +
						             "    <StammGrundstueck VersionID=\"8af806cc5043853201508e1e8a811a75\">\n" +
						             "        <Quote>\n" +
						             "            <AnteilZaehler>1</AnteilZaehler>\n" +
						             "            <AnteilNenner>18</AnteilNenner>\n" +
						             "            <QuoteUnbekannt>false</QuoteUnbekannt>\n" +
						             "        </Quote>\n" +
						             "        <BelastetesGrundstueck VersionID=\"8af806cc5043853201508e1c04c419e6\">\n" +
						             "            <EGrid>CH487965658402</EGrid>\n" +
						             "            <GrundstueckNummer>\n" +
						             "                <BfsNr>308</BfsNr>\n" +
						             "                <Gemeindenamen>Corcelles-près-Payerne</Gemeindenamen>\n" +
						             "                <StammNr>3601</StammNr>\n" +
						             "                <IndexNr1>7</IndexNr1>\n" +
						             "            </GrundstueckNummer>\n" +
						             "            <GrundstueckArt>StockwerksEinheit</GrundstueckArt>\n" +
						             "        </BelastetesGrundstueck>\n" +
						             "    </StammGrundstueck>\n" +
						             "</GewoehnlichesMiteigentum>\n", mut1.getXmlContent());
			}
		});
	}

	/**
	 * Ce test vérifie que des mutations de suppression sont bien créées lorsque des immeubles actifs dans la base n'apparaissent pas dans l'import.
	 */
	@Test
	public void testImportImmeublesRadie() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_immeubles_vide_rf_hebdo.xml");
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
				final CommuneRF boulens = communeRFDAO.save(newCommuneRF(190, "Boulens", 5556));
				final CommuneRF corcelles = communeRFDAO.save(newCommuneRF(308, "Corcelles-près-Payerne", 5557));

				// données équivalentes au fichier export_immeubles_rf_hebdo.xml
				final BienFondRF bienFond = newBienFondRF("_1f109152381026b501381028a73d1852", "CH938391457759", oron, 5089, 260000L, "RG93", null, RegDate.get(1993, 1, 1), false, false, RegDate.get(2010, 1, 1), 707);
				final DroitDistinctEtPermanentRF droitDistinctEtPermanent = newDroitDistinctEtPermanentRF("_8af806cc3971feb60139e36d062130f3", "CH729253834531", oron, 692, 2120000L, "2016", RegDate.get(2016, 9, 13), RegDate.get(2016, 1, 1), false, RegDate.get(2010, 1, 1),
				                                                                                          4896);
				final ProprieteParEtageRF ppe = newProprieteParEtageRF("_8af806fc45d223e60149c23f475365d5", "CH336583651349", boulens, 19, 4, 495000L, "2016", RegDate.get(2016, 9, 13), RegDate.get(2016, 1, 1), false, new Fraction(293, 1000), RegDate.get(2010, 1, 1));
				final PartCoproprieteRF copropriete = newPartCoproprieteRF("_8af806cc5043853201508e1e8a3a1a71", "CH516579658411", corcelles, 3601, 7, 13, 550L, "2015", RegDate.get(2015, 10, 22), RegDate.get(2015, 1, 1), false, new Fraction(1, 18), RegDate.get(2010, 1, 1));

				immeubleRFDAO.save(bienFond);
				immeubleRFDAO.save(droitDistinctEtPermanent);
				immeubleRFDAO.save(ppe);
				immeubleRFDAO.save(copropriete);
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

		// on vérifie qu'il y a bien les mutations de suppression dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
				assertEquals(4, mutations.size());    // il y a 4 immeubles dans la base et aucun dans le fichier d'import
				Collections.sort(mutations, (l,r) -> l.getIdRF().compareTo(r.getIdRF()));

				final EvenementRFMutation mutation0 = mutations.get(0);
				assertEquals(TypeEntiteRF.IMMEUBLE, mutation0.getTypeEntite());
				assertEquals(TypeMutationRF.SUPPRESSION, mutation0.getTypeMutation());
				assertEquals("_1f109152381026b501381028a73d1852", mutation0.getIdRF());

				final EvenementRFMutation mutation1 = mutations.get(1);
				assertEquals(TypeEntiteRF.IMMEUBLE, mutation1.getTypeEntite());
				assertEquals(TypeMutationRF.SUPPRESSION, mutation1.getTypeMutation());
				assertEquals("_8af806cc3971feb60139e36d062130f3", mutation1.getIdRF());

				final EvenementRFMutation mutation2 = mutations.get(2);
				assertEquals(TypeEntiteRF.IMMEUBLE, mutation2.getTypeEntite());
				assertEquals(TypeMutationRF.SUPPRESSION, mutation2.getTypeMutation());
				assertEquals("_8af806cc5043853201508e1e8a3a1a71", mutation2.getIdRF());

				final EvenementRFMutation mutation3 = mutations.get(3);
				assertEquals(TypeEntiteRF.IMMEUBLE, mutation3.getTypeEntite());
				assertEquals(TypeMutationRF.SUPPRESSION, mutation3.getTypeMutation());
				assertEquals("_8af806fc45d223e60149c23f475365d5", mutation3.getIdRF());
			}
		});
	}
}