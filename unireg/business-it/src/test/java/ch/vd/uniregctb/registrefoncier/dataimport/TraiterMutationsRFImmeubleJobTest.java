package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
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
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.PartCoproprieteRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceTotaleRF;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TraiterMutationsRFImmeubleJobTest extends ImportRFTestClass {

	private BatchScheduler batchScheduler;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private CommuneRFDAO communeRFDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
	}

	/**
	 * Ce test vérifie que les mutations de type CREATION sont bien créées lorsqu'on importe un fichier RF sur une base vide
	 */
	@Test
	public void testTraiterMutationsCreation() throws Exception {

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(RegDate.get(2016, 10, 1));
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.IMMEUBLE);
				mut0.setTypeMutation(TypeMutationRF.CREATION);
				mut0.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
						                   "</Liegenschaft>\n");
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.IMMEUBLE);
				mut1.setTypeMutation(TypeMutationRF.CREATION);
				mut1.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
						                   "</Sdr>\n");
				evenementRFMutationDAO.save(mut1);

				final EvenementRFMutation mut2 = new EvenementRFMutation();
				mut2.setParentImport(importEvent);
				mut2.setEtat(EtatEvenementRF.A_TRAITER);
				mut2.setTypeEntite(TypeEntiteRF.IMMEUBLE);
				mut2.setTypeMutation(TypeMutationRF.CREATION);
				mut2.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
						                   "</StockwerksEinheit>\n");
				evenementRFMutationDAO.save(mut2);

				final EvenementRFMutation mut3 = new EvenementRFMutation();
				mut3.setParentImport(importEvent);
				mut3.setEtat(EtatEvenementRF.A_TRAITER);
				mut3.setTypeEntite(TypeEntiteRF.IMMEUBLE);
				mut3.setTypeMutation(TypeMutationRF.CREATION);
				mut3.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
						                   "</GewoehnlichesMiteigentum>\n");
				evenementRFMutationDAO.save(mut3);

				final EvenementRFMutation mut4 = new EvenementRFMutation();
				mut4.setParentImport(importEvent);
				mut4.setEtat(EtatEvenementRF.A_TRAITER);
				mut4.setTypeEntite(TypeEntiteRF.COMMUNE);
				mut4.setTypeMutation(TypeMutationRF.CREATION);
				mut4.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <BfsNr>190</BfsNr>\n" +
						                   "    <Gemeindenamen>Boulens</Gemeindenamen>\n" +
						                   "    <StammNr>0</StammNr>\n" +
						                   "</GrundstueckNummer>\n");
				evenementRFMutationDAO.save(mut4);

				final EvenementRFMutation mut5 = new EvenementRFMutation();
				mut5.setParentImport(importEvent);
				mut5.setEtat(EtatEvenementRF.A_TRAITER);
				mut5.setTypeEntite(TypeEntiteRF.COMMUNE);
				mut5.setTypeMutation(TypeMutationRF.CREATION);
				mut5.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <BfsNr>294</BfsNr>\n" +
						                   "    <Gemeindenamen>Oron</Gemeindenamen>\n" +
						                   "    <StammNr>0</StammNr>\n" +
						                   "</GrundstueckNummer>\n");
				evenementRFMutationDAO.save(mut5);

				final EvenementRFMutation mut6 = new EvenementRFMutation();
				mut6.setParentImport(importEvent);
				mut6.setEtat(EtatEvenementRF.A_TRAITER);
				mut6.setTypeEntite(TypeEntiteRF.COMMUNE);
				mut6.setTypeMutation(TypeMutationRF.CREATION);
				mut6.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <BfsNr>308</BfsNr>\n" +
						                   "    <Gemeindenamen>Corcelles-près-Payerne</Gemeindenamen>\n" +
						                   "    <StammNr>0</StammNr>\n" +
						                   "</GrundstueckNummer>\n");
				evenementRFMutationDAO.save(mut6);

				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_DATES_FIN_JOB, Boolean.FALSE);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, Boolean.FALSE);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
				assertEquals(7, mutations.size());    // il y a 4 immeubles + 3 communes dans le fichier d'import et 4 immeubles + 3 communes dans le DB, dont 2 immeubles ont des données différentes
				Collections.sort(mutations, new MutationComparator());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(0).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(1).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(2).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(3).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(4).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(5).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(6).getEtat());
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
					assertNull(situation.getDateDebut());                   // date vide en import initial
					assertNull(situation.getDateFin());
					assertEquals(294, situation.getCommune().getNoRf());
					assertEquals(5089, situation.getNoParcelle());
					assertNull(situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = bienFond.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertNull(estimation.getDateDebut());                  // date vide en import initial
					assertNull(estimation.getDateFin());
					assertEquals(Long.valueOf(260000), estimation.getMontant());
					assertEquals("RG93", estimation.getReference());
					assertEquals(Integer.valueOf(1993), estimation.getAnneeReference());
					assertNull(estimation.getDateInscription());
					assertEquals(RegDate.get(1993, 1, 1), estimation.getDateDebutMetier());
					assertNull(estimation.getDateFinMetier());
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
					assertNull(situation.getDateDebut());                   // date vide en import initial
					assertNull(situation.getDateFin());
					assertEquals(294, situation.getCommune().getNoRf());
					assertEquals(692, situation.getNoParcelle());
					assertNull(situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = ddo.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertNull(estimation.getDateDebut());                  // date vide en import initial
					assertNull(estimation.getDateFin());
					assertEquals(Long.valueOf(2120000), estimation.getMontant());
					assertEquals("2016", estimation.getReference());
					assertEquals(Integer.valueOf(2016), estimation.getAnneeReference());
					assertEquals(RegDate.get(2016, 9, 13), estimation.getDateInscription());
					assertEquals(RegDate.get(2016, 1, 1), estimation.getDateDebutMetier());
					assertNull(estimation.getDateFinMetier());
					assertFalse(estimation.isEnRevision());
				}

				final ProprieteParEtageRF ppe = (ProprieteParEtageRF) immeubleRFDAO.find(new ImmeubleRFKey("_8af806fc45d223e60149c23f475365d5"));
				assertNotNull(ppe);
				{
					assertEquals("_8af806fc45d223e60149c23f475365d5", ppe.getIdRF());
					assertEquals("CH336583651349", ppe.getEgrid());
					Assert.assertEquals(new Fraction(293, 1000), ppe.getQuotePart());

					final Set<SituationRF> situations = ppe.getSituations();
					assertEquals(1, situations.size());

					final SituationRF situation = situations.iterator().next();
					assertNull(situation.getDateDebut());                   // date vide en import initial
					assertNull(situation.getDateFin());
					assertEquals(190, situation.getCommune().getNoRf());
					assertEquals(19, situation.getNoParcelle());
					assertEquals(Integer.valueOf(4), situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = ppe.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertNull(estimation.getDateDebut());                  // date vide en import initial
					assertNull(estimation.getDateFin());
					assertEquals(Long.valueOf(495000), estimation.getMontant());
					assertEquals("2016", estimation.getReference());
					assertEquals(Integer.valueOf(2016), estimation.getAnneeReference());
					assertEquals(RegDate.get(2016, 9, 13), estimation.getDateInscription());
					assertEquals(RegDate.get(2016, 1, 1), estimation.getDateDebutMetier());
					assertNull(estimation.getDateFinMetier());
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
					assertNull(situation.getDateDebut());                   // date vide en import initial
					assertNull(situation.getDateFin());
					assertEquals(308, situation.getCommune().getNoRf());
					assertEquals(3601, situation.getNoParcelle());
					assertEquals(Integer.valueOf(7), situation.getIndex1());
					assertEquals(Integer.valueOf(13), situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = copro.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertNull(estimation.getDateDebut());                  // date vide en import initial
					assertNull(estimation.getDateFin());
					assertEquals(Long.valueOf(550), estimation.getMontant());
					assertEquals("2015", estimation.getReference());
					assertEquals(Integer.valueOf(2015), estimation.getAnneeReference());
					assertEquals(RegDate.get(2015, 10, 22), estimation.getDateInscription());
					assertEquals(RegDate.get(2015, 1, 1), estimation.getDateDebutMetier());
					assertNull(estimation.getDateFinMetier());
					assertFalse(estimation.isEnRevision());
				}
			}
		});
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsqu'on importe un fichier RF et que les immeubles dans la base ne correspondent pas.
	 */
	@Test
	public void testTraiterMutationsModification() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// on insère les données du second import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(dateSecondImport);
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.IMMEUBLE);
				mut0.setTypeMutation(TypeMutationRF.MODIFICATION);
				mut0.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
						                   "</Liegenschaft>\n");
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.IMMEUBLE);
				mut1.setTypeMutation(TypeMutationRF.MODIFICATION);
				mut1.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
						                   "</Sdr>\n");
				evenementRFMutationDAO.save(mut1);

				final EvenementRFMutation mut2 = new EvenementRFMutation();
				mut2.setParentImport(importEvent);
				mut2.setEtat(EtatEvenementRF.A_TRAITER);
				mut2.setTypeEntite(TypeEntiteRF.IMMEUBLE);
				mut2.setTypeMutation(TypeMutationRF.MODIFICATION);
				mut2.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
						                   "</StockwerksEinheit>\n");
				evenementRFMutationDAO.save(mut2);

				final EvenementRFMutation mut3 = new EvenementRFMutation();
				mut3.setParentImport(importEvent);
				mut3.setEtat(EtatEvenementRF.A_TRAITER);
				mut3.setTypeEntite(TypeEntiteRF.IMMEUBLE);
				mut3.setTypeMutation(TypeMutationRF.MODIFICATION);
				mut3.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
						                   "</GewoehnlichesMiteigentum>\n");
				evenementRFMutationDAO.save(mut3);

				return importEvent.getId();
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
				final BienFondRF bienFond = newBienFondRF("_1f109152381026b501381028a73d1852", "CH938391457759", oron, 5089, 260000L, "RG93", 1993, null, RegDate.get(1993, 1, 1), false, false, dateImportInitial, 707);
				// - estimation fiscale différente
				final DroitDistinctEtPermanentRF droitDistinctEtPermanent = newDroitDistinctEtPermanentRF("_8af806cc3971feb60139e36d062130f3", "CH729253834531", oron, 692, 2000000L, "2015", 2015, RegDate.get(2015, 1, 1), RegDate.get(2015, 1, 1), false, dateImportInitial, 4896);
				// - données identiques
				final ProprieteParEtageRF ppe = newProprieteParEtageRF("_8af806fc45d223e60149c23f475365d5", "CH336583651349", boulens, 19, 4, 495000L, "2016", 2016, RegDate.get(2016, 9, 13), RegDate.get(2016, 1, 1), false, new Fraction(293, 1000), dateImportInitial);
				// - numéro de parcelle différente
				final PartCoproprieteRF copropriete = newPartCoproprieteRF("_8af806cc5043853201508e1e8a3a1a71", "CH516579658411", corcelles, 777, 7, 13, 550L, "2015", 2015, RegDate.get(2015, 10, 22), RegDate.get(2015, 1, 1), false, new Fraction(1, 18), dateImportInitial);

				immeubleRFDAO.save(bienFond);
				immeubleRFDAO.save(droitDistinctEtPermanent);
				immeubleRFDAO.save(ppe);
				immeubleRFDAO.save(copropriete);
			}
		});

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_DATES_FIN_JOB, Boolean.FALSE);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, Boolean.FALSE);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
				assertEquals(4, mutations.size());    // il y a 4 immeubles dans le fichier d'import et 4 immeubles dans le DB, dont 2 sont ont des données différentes
				Collections.sort(mutations, new MutationComparator());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(0).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(1).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(2).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(3).getEtat());
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
					assertEquals(294, situation.getCommune().getNoRf());
					assertEquals(5089, situation.getNoParcelle());
					assertNull(situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = bienFond.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(dateImportInitial, estimation.getDateDebut());
					assertNull(estimation.getDateFin());
					assertEquals(Long.valueOf(260000), estimation.getMontant());
					assertEquals("RG93", estimation.getReference());
					assertEquals(Integer.valueOf(1993), estimation.getAnneeReference());
					assertNull(estimation.getDateInscription());
					assertEquals(RegDate.get(1993, 1, 1), estimation.getDateDebutMetier());
					assertNull(estimation.getDateFinMetier());
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
					assertEquals(294, situation.getCommune().getNoRf());
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
					assertEquals(Long.valueOf(2000000), estimation0.getMontant());
					assertEquals("2015", estimation0.getReference());
					assertEquals(Integer.valueOf(2015), estimation0.getAnneeReference());
					assertEquals(RegDate.get(2015, 1, 1), estimation0.getDateInscription());
					assertEquals(RegDate.get(2015, 1, 1), estimation0.getDateDebutMetier());
					assertEquals(RegDate.get(2015, 12, 31), estimation0.getDateFinMetier());
					assertFalse(estimation0.isEnRevision());

					final EstimationRF estimation1 = estimationList.get(1);
					assertEquals(dateSecondImport, estimation1.getDateDebut());
					assertNull(estimation1.getDateFin());
					assertEquals(Long.valueOf(2120000), estimation1.getMontant());
					assertEquals("2016", estimation1.getReference());
					assertEquals(Integer.valueOf(2016), estimation1.getAnneeReference());
					assertEquals(RegDate.get(2016, 9, 13), estimation1.getDateInscription());
					assertEquals(RegDate.get(2016, 1, 1), estimation1.getDateDebutMetier());
					assertNull(estimation1.getDateFinMetier());
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
					assertEquals(190, situation.getCommune().getNoRf());
					assertEquals(19, situation.getNoParcelle());
					assertEquals(Integer.valueOf(4), situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = ppe.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(dateImportInitial, estimation.getDateDebut());
					assertNull(estimation.getDateFin());
					assertEquals(Long.valueOf(495000), estimation.getMontant());
					assertEquals("2016", estimation.getReference());
					assertEquals(Integer.valueOf(2016), estimation.getAnneeReference());
					assertEquals(RegDate.get(2016, 9, 13), estimation.getDateInscription());
					assertEquals(RegDate.get(2016, 1, 1), estimation.getDateDebutMetier());
					assertNull(estimation.getDateFinMetier());
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
					assertEquals(308, situation0.getCommune().getNoRf());
					assertEquals(777, situation0.getNoParcelle());
					assertEquals(Integer.valueOf(7), situation0.getIndex1());
					assertEquals(Integer.valueOf(13), situation0.getIndex2());
					assertNull(situation0.getIndex3());

					final SituationRF situation1 = situationList.get(1);
					assertEquals(dateSecondImport, situation1.getDateDebut());
					assertNull(situation1.getDateFin());
					assertEquals(308, situation1.getCommune().getNoRf());
					assertEquals(3601, situation1.getNoParcelle());
					assertEquals(Integer.valueOf(7), situation1.getIndex1());
					assertEquals(Integer.valueOf(13), situation1.getIndex2());
					assertNull(situation1.getIndex3());

					final Set<EstimationRF> estimations = copro.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(dateImportInitial, estimation.getDateDebut());
					assertNull(estimation.getDateFin());
					assertEquals(Long.valueOf(550), estimation.getMontant());
					assertEquals("2015", estimation.getReference());
					assertEquals(Integer.valueOf(2015), estimation.getAnneeReference());
					assertEquals(RegDate.get(2015, 10, 22), estimation.getDateInscription());
					assertEquals(RegDate.get(2015, 1, 1), estimation.getDateDebutMetier());
					assertNull(estimation.getDateFinMetier());
					assertFalse(estimation.isEnRevision());
				}
			}
		});
	}

	/**
	 * Ce test vérifie que des mutations de suppression sont bien traitées et que les immeubles correspondant sont radiés.
	 */
	@Test
	public void testTraiterMutationsSuppression() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// on insère les données du second import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setDateEvenement(dateSecondImport);
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.IMMEUBLE);
				mut0.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mut0.setIdRF("_1f109152381026b501381028a73d1852");
				mut0.setXmlContent(null);
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.IMMEUBLE);
				mut1.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mut1.setIdRF("_8af806cc3971feb60139e36d062130f3");
				mut1.setXmlContent(null);
				evenementRFMutationDAO.save(mut1);

				final EvenementRFMutation mut2 = new EvenementRFMutation();
				mut2.setParentImport(importEvent);
				mut2.setEtat(EtatEvenementRF.A_TRAITER);
				mut2.setTypeEntite(TypeEntiteRF.IMMEUBLE);
				mut2.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mut2.setIdRF("_8af806cc5043853201508e1e8a3a1a71");
				mut2.setXmlContent(null);
				evenementRFMutationDAO.save(mut2);

				final EvenementRFMutation mut3 = new EvenementRFMutation();
				mut3.setParentImport(importEvent);
				mut3.setEtat(EtatEvenementRF.A_TRAITER);
				mut3.setTypeEntite(TypeEntiteRF.IMMEUBLE);
				mut3.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mut3.setIdRF("_8af806fc45d223e60149c23f475365d5");
				mut3.setXmlContent(null);
				evenementRFMutationDAO.save(mut3);

				return importEvent.getId();
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

				// données identiques de celles du fichier export_immeubles_rf_hebdo.xml
				final BienFondRF bienFond = newBienFondRF("_1f109152381026b501381028a73d1852", "CH938391457759", oron, 5089, 260000L, "RG93", 1993, null, RegDate.get(1993, 1, 1), false, false, dateImportInitial, 707);
				final DroitDistinctEtPermanentRF droitDistinctEtPermanent = newDroitDistinctEtPermanentRF("_8af806cc3971feb60139e36d062130f3", "CH729253834531", oron, 692, 2120000L, "2016", 2016, RegDate.get(2016, 9, 13), RegDate.get(2016, 1, 1), false, dateImportInitial, 4896);
				final ProprieteParEtageRF ppe = newProprieteParEtageRF("_8af806fc45d223e60149c23f475365d5", "CH336583651349", boulens, 19, 4, 495000L, "2016", 2016, RegDate.get(2016, 9, 13), RegDate.get(2016, 1, 1), false, new Fraction(293, 1000), dateImportInitial);
				final PartCoproprieteRF copropriete = newPartCoproprieteRF("_8af806cc5043853201508e1e8a3a1a71", "CH516579658411", corcelles, 3601, 7, 13, 550L, "2015", 2015, RegDate.get(2015, 10, 22), RegDate.get(2015, 1, 1), false, new Fraction(1, 18), dateImportInitial);

				immeubleRFDAO.save(bienFond);
				immeubleRFDAO.save(droitDistinctEtPermanent);
				immeubleRFDAO.save(ppe);
				immeubleRFDAO.save(copropriete);
			}
		});

		// on déclenche le démarrage du job
		final HashMap<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_DATES_FIN_JOB, Boolean.FALSE);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, Boolean.FALSE);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
				assertEquals(4, mutations.size());    // il n'y a pas d'immeubles dans le fichier d'import et 4 immeubles dans le DB
				Collections.sort(mutations, new MutationComparator());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(0).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(1).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(2).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(3).getEtat());
			}
		});

		// on vérifie que les immeubles ont bien été radié
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				// l'immeuble est radié, ainsi que les situations, les estimations et les surfaces totales
				final BienFondRF bienFond = (BienFondRF) immeubleRFDAO.find(new ImmeubleRFKey("_1f109152381026b501381028a73d1852"));
				assertNotNull(bienFond);
				{
					assertEquals("_1f109152381026b501381028a73d1852", bienFond.getIdRF());
					assertEquals("CH938391457759", bienFond.getEgrid());
					assertFalse(bienFond.isCfa());
					assertEquals(dateSecondImport.getOneDayBefore(), bienFond.getDateRadiation());

					final Set<SituationRF> situations = bienFond.getSituations();
					assertEquals(1, situations.size());

					final SituationRF situation = situations.iterator().next();
					assertEquals(dateImportInitial, situation.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), situation.getDateFin());
					assertEquals(294, situation.getCommune().getNoRf());
					assertEquals(5089, situation.getNoParcelle());
					assertNull(situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = bienFond.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(dateImportInitial, estimation.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), estimation.getDateFin());
					assertEquals(Long.valueOf(260000), estimation.getMontant());
					assertEquals("RG93", estimation.getReference());
					assertEquals(Integer.valueOf(1993), estimation.getAnneeReference());
					assertNull(estimation.getDateInscription());
					assertEquals(RegDate.get(1993, 1, 1), estimation.getDateDebutMetier());
					assertEquals(dateSecondImport.getOneDayBefore(), estimation.getDateFinMetier());
					assertFalse(estimation.isEnRevision());

					final Set<SurfaceTotaleRF> surfacesTotales = bienFond.getSurfacesTotales();
					assertEquals(1, surfacesTotales.size());

					final SurfaceTotaleRF surfaceTotale = surfacesTotales.iterator().next();
					assertEquals(dateImportInitial, surfaceTotale.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), surfaceTotale.getDateFin());
					assertEquals(707, surfaceTotale.getSurface());
				}

				// l'immeuble est radié, ainsi que les situations, les estimations et les surfaces totales
				final DroitDistinctEtPermanentRF ddo = (DroitDistinctEtPermanentRF) immeubleRFDAO.find(new ImmeubleRFKey("_8af806cc3971feb60139e36d062130f3"));
				assertNotNull(ddo);
				{
					assertEquals("_8af806cc3971feb60139e36d062130f3", ddo.getIdRF());
					assertEquals("CH729253834531", ddo.getEgrid());
					assertEquals(dateSecondImport.getOneDayBefore(), ddo.getDateRadiation());

					final Set<SituationRF> situations = ddo.getSituations();
					assertEquals(1, situations.size());

					final SituationRF situation = situations.iterator().next();
					assertEquals(dateImportInitial, situation.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), situation.getDateFin());
					assertEquals(294, situation.getCommune().getNoRf());
					assertEquals(692, situation.getNoParcelle());
					assertNull(situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = ddo.getEstimations();
					assertEquals(1, estimations.size());
					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(dateImportInitial, estimation.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), estimation.getDateFin());
					assertEquals(Long.valueOf(2120000), estimation.getMontant());
					assertEquals("2016", estimation.getReference());
					assertEquals(Integer.valueOf(2016), estimation.getAnneeReference());
					assertEquals(RegDate.get(2016, 9, 13), estimation.getDateInscription());
					assertEquals(RegDate.get(2016, 1, 1), estimation.getDateDebutMetier());
					assertEquals(dateSecondImport.getOneDayBefore(), estimation.getDateFinMetier());
					assertFalse(estimation.isEnRevision());

					final Set<SurfaceTotaleRF> surfacesTotales = bienFond.getSurfacesTotales();
					assertEquals(1, surfacesTotales.size());

					final SurfaceTotaleRF surfaceTotale = surfacesTotales.iterator().next();
					assertEquals(dateImportInitial, surfaceTotale.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), surfaceTotale.getDateFin());
					assertEquals(707, surfaceTotale.getSurface());
				}

				// l'immeuble est radié, ainsi que les situations, les estimations et les surfaces totales
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
					assertEquals(dateSecondImport.getOneDayBefore(), situation.getDateFin());
					assertEquals(190, situation.getCommune().getNoRf());
					assertEquals(19, situation.getNoParcelle());
					assertEquals(Integer.valueOf(4), situation.getIndex1());
					assertNull(situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = ppe.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(dateImportInitial, estimation.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), estimation.getDateFin());
					assertEquals(Long.valueOf(495000), estimation.getMontant());
					assertEquals("2016", estimation.getReference());
					assertEquals(Integer.valueOf(2016), estimation.getAnneeReference());
					assertEquals(RegDate.get(2016, 9, 13), estimation.getDateInscription());
					assertEquals(RegDate.get(2016, 1, 1), estimation.getDateDebutMetier());
					assertEquals(dateSecondImport.getOneDayBefore(), estimation.getDateFinMetier());
					assertFalse(estimation.isEnRevision());

					final Set<SurfaceTotaleRF> surfacesTotales = bienFond.getSurfacesTotales();
					assertEquals(1, surfacesTotales.size());

					final SurfaceTotaleRF surfaceTotale = surfacesTotales.iterator().next();
					assertEquals(dateImportInitial, surfaceTotale.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), surfaceTotale.getDateFin());
					assertEquals(707, surfaceTotale.getSurface());
				}

				// l'immeuble est radié, ainsi que les situations, les estimations et les surfaces totales
				final PartCoproprieteRF copro = (PartCoproprieteRF) immeubleRFDAO.find(new ImmeubleRFKey("_8af806cc5043853201508e1e8a3a1a71"));
				assertNotNull(copro);
				{
					assertEquals("_8af806cc5043853201508e1e8a3a1a71", copro.getIdRF());
					assertEquals("CH516579658411", copro.getEgrid());
					assertEquals(new Fraction(1, 18), copro.getQuotePart());

					final Set<SituationRF> situations = copro.getSituations();
					assertEquals(1, situations.size());

					final SituationRF situation = situations.iterator().next();
					assertEquals(dateImportInitial, situation.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), situation.getDateFin());
					assertEquals(308, situation.getCommune().getNoRf());
					assertEquals(3601, situation.getNoParcelle());
					assertEquals(Integer.valueOf(7), situation.getIndex1());
					assertEquals(Integer.valueOf(13), situation.getIndex2());
					assertNull(situation.getIndex3());

					final Set<EstimationRF> estimations = copro.getEstimations();
					assertEquals(1, estimations.size());

					final EstimationRF estimation = estimations.iterator().next();
					assertEquals(dateImportInitial, estimation.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), estimation.getDateFin());
					assertEquals(Long.valueOf(550), estimation.getMontant());
					assertEquals("2015", estimation.getReference());
					assertEquals(Integer.valueOf(2015), estimation.getAnneeReference());
					assertEquals(RegDate.get(2015, 10, 22), estimation.getDateInscription());
					assertEquals(RegDate.get(2015, 1, 1), estimation.getDateDebutMetier());
					assertEquals(dateSecondImport.getOneDayBefore(), estimation.getDateFinMetier());
					assertFalse(estimation.isEnRevision());

					final Set<SurfaceTotaleRF> surfacesTotales = bienFond.getSurfacesTotales();
					assertEquals(1, surfacesTotales.size());

					final SurfaceTotaleRF surfaceTotale = surfacesTotales.iterator().next();
					assertEquals(dateImportInitial, surfaceTotale.getDateDebut());
					assertEquals(dateSecondImport.getOneDayBefore(), surfaceTotale.getDateFin());
					assertEquals(707, surfaceTotale.getSurface());
				}
			}
		});
	}
}