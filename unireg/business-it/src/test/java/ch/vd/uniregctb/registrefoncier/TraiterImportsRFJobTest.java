package ch.vd.uniregctb.registrefoncier;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.technical.esb.store.raft.ZipRaftEsbStore;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.common.BusinessTestingConstants;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static ch.vd.uniregctb.listes.afc.ExtractionDonneesRptProcessor.LOGGER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_JOBS,
		"classpath:ut/unireg-businessit-jms.xml"
})
public class TraiterImportsRFJobTest extends BusinessItTest {

	private BatchScheduler batchScheduler;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private ZipRaftEsbStore zipRaftEsbStore;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		zipRaftEsbStore = getBean(ZipRaftEsbStore.class, "zipRaftEsbStore");
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
		params.put(TraiterImportsRFJob.ID, importId);

		final JobDefinition job = batchScheduler.startJob(TraiterImportsRFJob.NAME, params);
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
				assertEquals(4, mutations.size());    // il y a 4 immeubles dans le fichier d'import et la DB était vide
				Collections.sort(mutations, (o1, o2) -> o1.getId().compareTo(o2.getId()));

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.TRAITE, mut0.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.IMMEUBLE, mut0.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut0.getTypeMutation());
				assertBlobEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
				assertEquals(EtatEvenementRF.TRAITE, mut1.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.IMMEUBLE, mut1.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut1.getTypeMutation());
				assertBlobEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
				assertEquals(EtatEvenementRF.TRAITE, mut2.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.IMMEUBLE, mut2.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut2.getTypeMutation());
				assertBlobEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
						                 "</StockwerksEinheit>\n", mut2.getXmlContent());

				final EvenementRFMutation mut3 = mutations.get(3);
				assertEquals(importId, mut3.getParentImport().getId());
				assertEquals(EtatEvenementRF.TRAITE, mut3.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.IMMEUBLE, mut3.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut3.getTypeMutation());
				assertBlobEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
						                 "</GewoehnlichesMiteigentum>\n", mut3.getXmlContent());
			}
		});

		// on vérifie que les immeubles ont bien été créées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final BienFondRF bienFond = (BienFondRF) immeubleRFDAO.find(new ImmeubleRFKey("_1f109152381026b501381028a73d1852"));
				assertNotNull(bienFond);
				{
					assertEquals("_1f109152381026b501381028a73d1852", bienFond.getIdRF());
					assertEquals("CH938391457759", bienFond.getEgrid());
					assertFalse(bienFond.isCfa());

					final Set<SituationRF> situations = bienFond.getSituations();
					assertEquals(1, situations.size());

					final SituationRF situation = situations.iterator().next();
					assertEquals(RegDate.get(2016, 10, 1), situation.getDateDebut());
					assertNull(situation.getDateFin());
					assertEquals(294, situation.getNoRfCommune());
					assertEquals(5089, situation.getNoParcelle());
					assertNull(situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = bienFond.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(RegDate.get(2016, 10, 1), estimation.getDateDebut());
					assertNull(estimation.getDateFin());
					assertEquals(260000, estimation.getMontant());
					assertEquals("RG93", estimation.getReference());
					assertNull(estimation.getDateEstimation());
					assertFalse(estimation.isEnRevision());
				}

				final DroitDistinctEtPermanentRF ddo = (DroitDistinctEtPermanentRF) immeubleRFDAO.find(new ImmeubleRFKey("_8af806cc3971feb60139e36d062130f3"));
				assertNotNull(ddo);
				{
					assertEquals("_8af806cc3971feb60139e36d062130f3", ddo.getIdRF());
					assertEquals("CH729253834531", ddo.getEgrid());

					final Set<SituationRF> situations = ddo.getSituations();
					assertEquals(1, situations.size());

					final SituationRF situation = situations.iterator().next();
					assertEquals(RegDate.get(2016, 10, 1), situation.getDateDebut());
					assertNull(situation.getDateFin());
					assertEquals(294, situation.getNoRfCommune());
					assertEquals(692, situation.getNoParcelle());
					assertNull(situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = ddo.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(RegDate.get(2016, 10, 1), estimation.getDateDebut());
					assertNull(estimation.getDateFin());
					assertEquals(2120000, estimation.getMontant());
					assertEquals("2016", estimation.getReference());
					assertEquals(RegDate.get(2016, 9, 13), estimation.getDateEstimation());
					assertFalse(estimation.isEnRevision());
				}

				final ProprieteParEtageRF ppe = (ProprieteParEtageRF) immeubleRFDAO.find(new ImmeubleRFKey("_8af806fc45d223e60149c23f475365d5"));
				assertNotNull(ppe);
				{
					assertEquals("_8af806fc45d223e60149c23f475365d5", ppe.getIdRF());
					assertEquals("CH336583651349", ppe.getEgrid());
					assertEquals(new Fraction(293, 1000), ppe.getQuotePart());

					final Set<SituationRF> situations = ppe.getSituations();
					assertEquals(1, situations.size());

					final SituationRF situation = situations.iterator().next();
					assertEquals(RegDate.get(2016, 10, 1), situation.getDateDebut());
					assertNull(situation.getDateFin());
					assertEquals(190, situation.getNoRfCommune());
					assertEquals(19, situation.getNoParcelle());
					assertEquals(Integer.valueOf(4), situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = ppe.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(RegDate.get(2016, 10, 1), estimation.getDateDebut());
					assertNull(estimation.getDateFin());
					assertEquals(495000, estimation.getMontant());
					assertEquals("2016", estimation.getReference());
					assertEquals(RegDate.get(2016, 9, 13), estimation.getDateEstimation());
					assertFalse(estimation.isEnRevision());
				}

				final PartCoproprieteRF copro = (PartCoproprieteRF) immeubleRFDAO.find(new ImmeubleRFKey("_8af806cc5043853201508e1e8a3a1a71"));
				assertNotNull(copro);
				{
					assertEquals("_8af806cc5043853201508e1e8a3a1a71", copro.getIdRF());
					assertEquals("CH516579658411", copro.getEgrid());
					assertEquals(new Fraction(1, 18), copro.getQuotePart());

					final Set<SituationRF> situations = copro.getSituations();
					assertEquals(1, situations.size());

					final SituationRF situation = situations.iterator().next();
					assertEquals(RegDate.get(2016, 10, 1), situation.getDateDebut());
					assertNull(situation.getDateFin());
					assertEquals(308, situation.getNoRfCommune());
					assertEquals(3601, situation.getNoParcelle());
					assertEquals(Integer.valueOf(7), situation.getIndex1());
					assertEquals(Integer.valueOf(13), situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = copro.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(RegDate.get(2016, 10, 1), estimation.getDateDebut());
					assertNull(estimation.getDateFin());
					assertEquals(550, estimation.getMontant());
					assertEquals("2015", estimation.getReference());
					assertEquals(RegDate.get(2015, 10, 22), estimation.getDateEstimation());
					assertFalse(estimation.isEnRevision());
				}
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

				// données équivalentes au fichier export_immeubles_rf_hebdo.xml
				final BienFondRF bienFond = newBienFondRF("_1f109152381026b501381028a73d1852", "CH938391457759", 294, 5089, 260000, "RG93", null, false, false, RegDate.get(2010, 1, 1));
				final DroitDistinctEtPermanentRF droitDistinctEtPermanent = newDroitDistinctEtPermanentRF("_8af806cc3971feb60139e36d062130f3", "CH729253834531", 294, 692, 2120000, "2016", RegDate.get(2016, 9, 13), false, RegDate.get(2010, 1, 1));
				final ProprieteParEtageRF ppe = newProprieteParEtageRF("_8af806fc45d223e60149c23f475365d5", "CH336583651349", 190, 19, 4, 495000, "2016", RegDate.get(2016, 9, 13), false, new Fraction(293, 1000), RegDate.get(2010, 1, 1));
				final PartCoproprieteRF copropriete = newPartCoproprieteRF("_8af806cc5043853201508e1e8a3a1a71", "CH516579658411", 308, 3601, 7, 13, 550, "2015", RegDate.get(2015, 10, 22), false, new Fraction(1, 18), RegDate.get(2010, 1, 1));

				immeubleRFDAO.save(bienFond);
				immeubleRFDAO.save(droitDistinctEtPermanent);
				immeubleRFDAO.save(ppe);
				immeubleRFDAO.save(copropriete);
			}
		});

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterImportsRFJob.ID, importId);

		final JobDefinition job = batchScheduler.startJob(TraiterImportsRFJob.NAME, params);
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

				// données partiellement différentes de celles du fichier export_immeubles_rf_hebdo.xml
				// - données identiques
				final BienFondRF bienFond = newBienFondRF("_1f109152381026b501381028a73d1852", "CH938391457759", 294, 5089, 260000, "RG93", null, false, false, dateImportInitial);
				// - estimation fiscale différente
				final DroitDistinctEtPermanentRF droitDistinctEtPermanent = newDroitDistinctEtPermanentRF("_8af806cc3971feb60139e36d062130f3", "CH729253834531", 294, 692, 2000000, "2015", RegDate.get(2015, 1, 1), false, dateImportInitial);
				// - données identiques
				final ProprieteParEtageRF ppe = newProprieteParEtageRF("_8af806fc45d223e60149c23f475365d5", "CH336583651349", 190, 19, 4, 495000, "2016", RegDate.get(2016, 9, 13), false, new Fraction(293, 1000), dateImportInitial);
				// - numéro de parcelle différente
				final PartCoproprieteRF copropriete = newPartCoproprieteRF("_8af806cc5043853201508e1e8a3a1a71", "CH516579658411", 308, 777, 7, 13, 550, "2015", RegDate.get(2015, 10, 22), false, new Fraction(1, 18), dateImportInitial);

				immeubleRFDAO.save(bienFond);
				immeubleRFDAO.save(droitDistinctEtPermanent);
				immeubleRFDAO.save(ppe);
				immeubleRFDAO.save(copropriete);
			}
		});

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterImportsRFJob.ID, importId);

		final JobDefinition job = batchScheduler.startJob(TraiterImportsRFJob.NAME, params);
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
				Collections.sort(mutations, (o1, o2) -> o1.getId().compareTo(o2.getId()));

				// l'estimation fiscales est différente
				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.TRAITE, mut0.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.IMMEUBLE, mut0.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.MODIFICATION, mut0.getTypeMutation());
				assertBlobEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
				assertEquals(EtatEvenementRF.TRAITE, mut1.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.IMMEUBLE, mut1.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.MODIFICATION, mut1.getTypeMutation());
				assertBlobEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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

		// on vérifie que les immeubles ont bien été mis-à-jour
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				// pas de changement
				final BienFondRF bienFond = (BienFondRF) immeubleRFDAO.find(new ImmeubleRFKey("_1f109152381026b501381028a73d1852"));
				assertNotNull(bienFond);
				{
					assertEquals("_1f109152381026b501381028a73d1852", bienFond.getIdRF());
					assertEquals("CH938391457759", bienFond.getEgrid());
					assertFalse(bienFond.isCfa());

					final Set<SituationRF> situations = bienFond.getSituations();
					assertEquals(1, situations.size());

					final SituationRF situation = situations.iterator().next();
					assertEquals(dateImportInitial, situation.getDateDebut());
					assertNull(situation.getDateFin());
					assertEquals(294, situation.getNoRfCommune());
					assertEquals(5089, situation.getNoParcelle());
					assertNull(situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = bienFond.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(dateImportInitial, estimation.getDateDebut());
					assertNull(estimation.getDateFin());
					assertEquals(260000, estimation.getMontant());
					assertEquals("RG93", estimation.getReference());
					assertNull(estimation.getDateEstimation());
					assertFalse(estimation.isEnRevision());
				}

				// l'estimation a changé
				final DroitDistinctEtPermanentRF ddo = (DroitDistinctEtPermanentRF) immeubleRFDAO.find(new ImmeubleRFKey("_8af806cc3971feb60139e36d062130f3"));
				assertNotNull(ddo);
				{
					assertEquals("_8af806cc3971feb60139e36d062130f3", ddo.getIdRF());
					assertEquals("CH729253834531", ddo.getEgrid());

					final Set<SituationRF> situations = ddo.getSituations();
					assertEquals(1, situations.size());

					final SituationRF situation = situations.iterator().next();
					assertEquals(dateImportInitial, situation.getDateDebut());
					assertNull(situation.getDateFin());
					assertEquals(294, situation.getNoRfCommune());
					assertEquals(692, situation.getNoParcelle());
					assertNull(situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = ddo.getEstimations();
					assertEquals(2, estimations.size());

					final List<EstimationRF> estimationList = new ArrayList<EstimationRF>(estimations);
					Collections.sort(estimationList, new DateRangeComparator<>());

					final EstimationRF estimation0 = estimationList.get(0);
					assertEquals(dateImportInitial, estimation0.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), estimation0.getDateFin());
					assertEquals(2000000, estimation0.getMontant());
					assertEquals("2015", estimation0.getReference());
					assertEquals(RegDate.get(2015, 1, 1), estimation0.getDateEstimation());
					assertFalse(estimation0.isEnRevision());

					final EstimationRF estimation1 = estimationList.get(1);
					assertEquals(dateSecondImport, estimation1.getDateDebut());
					assertNull(estimation1.getDateFin());
					assertEquals(2120000, estimation1.getMontant());
					assertEquals("2016", estimation1.getReference());
					assertEquals(RegDate.get(2016, 9, 13), estimation1.getDateEstimation());
					assertFalse(estimation1.isEnRevision());
				}

				// pas de changement
				final ProprieteParEtageRF ppe = (ProprieteParEtageRF) immeubleRFDAO.find(new ImmeubleRFKey("_8af806fc45d223e60149c23f475365d5"));
				assertNotNull(ppe);
				{
					assertEquals("_8af806fc45d223e60149c23f475365d5", ppe.getIdRF());
					assertEquals("CH336583651349", ppe.getEgrid());
					assertEquals(new Fraction(293, 1000), ppe.getQuotePart());

					final Set<SituationRF> situations = ppe.getSituations();
					assertEquals(1, situations.size());

					final SituationRF situation = situations.iterator().next();
					assertEquals(dateImportInitial, situation.getDateDebut());
					assertNull(situation.getDateFin());
					assertEquals(190, situation.getNoRfCommune());
					assertEquals(19, situation.getNoParcelle());
					assertEquals(Integer.valueOf(4), situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = ppe.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(dateImportInitial, estimation.getDateDebut());
					assertNull(estimation.getDateFin());
					assertEquals(495000, estimation.getMontant());
					assertEquals("2016", estimation.getReference());
					assertEquals(RegDate.get(2016, 9, 13), estimation.getDateEstimation());
					assertFalse(estimation.isEnRevision());
				}

				// le numéro de parcelle à changé
				final PartCoproprieteRF copro = (PartCoproprieteRF) immeubleRFDAO.find(new ImmeubleRFKey("_8af806cc5043853201508e1e8a3a1a71"));
				assertNotNull(copro);
				{
					assertEquals("_8af806cc5043853201508e1e8a3a1a71", copro.getIdRF());
					assertEquals("CH516579658411", copro.getEgrid());
					assertEquals(new Fraction(1, 18), copro.getQuotePart());

					final Set<SituationRF> situations = copro.getSituations();
					assertEquals(2, situations.size());

					final List<SituationRF> situationList = new ArrayList<SituationRF>(situations);
					Collections.sort(situationList, new DateRangeComparator<>());

					final SituationRF situation0 = situationList.get(0);
					assertEquals(dateImportInitial, situation0.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), situation0.getDateFin());
					assertEquals(308, situation0.getNoRfCommune());
					assertEquals(777, situation0.getNoParcelle());
					assertEquals(Integer.valueOf(7), situation0.getIndex1());
					assertEquals(Integer.valueOf(13), situation0.getIndex2());
					assertNull(situation0.getIndex3());

					final SituationRF situation1 = situationList.get(1);
					assertEquals(dateSecondImport, situation1.getDateDebut());
					assertNull(situation1.getDateFin());
					assertEquals(308, situation1.getNoRfCommune());
					assertEquals(3601, situation1.getNoParcelle());
					assertEquals(Integer.valueOf(7), situation1.getIndex1());
					assertEquals(Integer.valueOf(13), situation1.getIndex2());
					assertNull(situation1.getIndex3());

					final Set<EstimationRF> estimations = copro.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(dateImportInitial, estimation.getDateDebut());
					assertNull(estimation.getDateFin());
					assertEquals(550, estimation.getMontant());
					assertEquals("2015", estimation.getReference());
					assertEquals(RegDate.get(2015, 10, 22), estimation.getDateEstimation());
					assertFalse(estimation.isEnRevision());
				}
			}
		});
	}

	private static BienFondRF newBienFondRF(String idRF, String egrid, int noRfCommune, int noParcelle,
	                                        int montantEstimation, String referenceEstimation, RegDate dateEstimation,
	                                        boolean enRevision, boolean cfa, RegDate dateValeur) {

		final SituationRF situation = new SituationRF();
		situation.setNoRfCommune(noRfCommune);
		situation.setNoParcelle(noParcelle);
		situation.setDateDebut(dateValeur);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(montantEstimation);
		estimation.setReference(referenceEstimation);
		estimation.setDateEstimation(dateEstimation);
		estimation.setEnRevision(enRevision);
		estimation.setDateDebut(dateValeur);

		final BienFondRF immeuble = new BienFondRF();
		immeuble.setIdRF(idRF);
		immeuble.setCfa(cfa);
		immeuble.setEgrid(egrid);
		immeuble.addSituation(situation);
		immeuble.addEstimation(estimation);

		return immeuble;
	}

	private static DroitDistinctEtPermanentRF newDroitDistinctEtPermanentRF(String idRF, String egrid, int noRfCommune, int noParcelle,
	                                                                        int montantEstimation, String referenceEstimation, RegDate dateEstimation,
	                                                                        boolean enRevision, RegDate dateValeur) {

		final SituationRF situation = new SituationRF();
		situation.setNoRfCommune(noRfCommune);
		situation.setNoParcelle(noParcelle);
		situation.setDateDebut(dateValeur);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(montantEstimation);
		estimation.setReference(referenceEstimation);
		estimation.setDateEstimation(dateEstimation);
		estimation.setEnRevision(enRevision);
		estimation.setDateDebut(dateValeur);

		final DroitDistinctEtPermanentRF immeuble = new DroitDistinctEtPermanentRF();
		immeuble.setIdRF(idRF);
		immeuble.setEgrid(egrid);
		immeuble.addSituation(situation);
		immeuble.addEstimation(estimation);

		return immeuble;
	}

	private static ProprieteParEtageRF newProprieteParEtageRF(String idRF, String egrid, int noRfCommune, int noParcelle, Integer index1,
	                                                          int montantEstimation, String referenceEstimation, RegDate dateEstimation,
	                                                          boolean enRevision, Fraction quotePart, RegDate dateValeur) {

		final SituationRF situation = new SituationRF();
		situation.setNoRfCommune(noRfCommune);
		situation.setNoParcelle(noParcelle);
		situation.setIndex1(index1);
		situation.setDateDebut(dateValeur);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(montantEstimation);
		estimation.setReference(referenceEstimation);
		estimation.setDateEstimation(dateEstimation);
		estimation.setEnRevision(enRevision);
		estimation.setDateDebut(dateValeur);

		final ProprieteParEtageRF immeuble = new ProprieteParEtageRF();
		immeuble.setIdRF(idRF);
		immeuble.setEgrid(egrid);
		immeuble.setQuotePart(quotePart);
		immeuble.addSituation(situation);
		immeuble.addEstimation(estimation);

		return immeuble;
	}

	private static PartCoproprieteRF newPartCoproprieteRF(String idRF, String egrid, int noRfCommune, int noParcelle, Integer index1, Integer index2,
	                                                      int montantEstimation, String referenceEstimation, RegDate dateEstimation,
	                                                      boolean enRevision, Fraction quotePart, RegDate dateValeur) {

		final SituationRF situation = new SituationRF();
		situation.setNoRfCommune(noRfCommune);
		situation.setNoParcelle(noParcelle);
		situation.setIndex1(index1);
		situation.setIndex2(index2);
		situation.setDateDebut(dateValeur);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(montantEstimation);
		estimation.setReference(referenceEstimation);
		estimation.setDateEstimation(dateEstimation);
		estimation.setEnRevision(enRevision);
		estimation.setDateDebut(dateValeur);

		final PartCoproprieteRF immeuble = new PartCoproprieteRF();
		immeuble.setIdRF(idRF);
		immeuble.setEgrid(egrid);
		immeuble.setQuotePart(quotePart);
		immeuble.addSituation(situation);
		immeuble.addEstimation(estimation);

		return immeuble;
	}

	private static void waitForJobCompletion(JobDefinition job) throws InterruptedException {
		int count = 0;
		while (job.isRunning()) {
			Thread.sleep(5000);
			count++;
			if (count % 6 == 0) { // 1 minute
				LOGGER.debug("Attente de la fin du job " + job.getName());
			}
			if (count > 30) { // 5 minutes
				LOGGER.debug("Interruption du job " + job.getName() + "...");
				job.interrupt();
				fail("Le job " + job.getName() + " tournait depuis plus de cinq minutes et a été interrompu.");
			}
		}
	}
}