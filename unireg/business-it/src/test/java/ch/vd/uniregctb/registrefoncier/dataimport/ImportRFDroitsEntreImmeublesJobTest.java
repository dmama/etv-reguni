package ch.vd.uniregctb.registrefoncier.dataimport;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.GenrePropriete;
import ch.vd.uniregctb.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ImportRFDroitsEntreImmeublesJobTest extends ImportRFTestClass {

	private BatchScheduler batchScheduler;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private ZipRaftEsbStore zipRaftEsbStore;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		zipRaftEsbStore = getBean(ZipRaftEsbStore.class, "zipRaftEsbStore");
	}

	/**
	 * Ce test vérifie que les mutations de type CREATION sont bien créées lorsqu'on importe un fichier RF sur une base vide
	 */
	@Test
	public void testImportBaseVide() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_droits_immeubles_rf_hebdo.xml");
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
				importEvent.setType(TypeImportRF.PRINCIPAL);
				importEvent.setDateEvenement(RegDate.get(2016, 10, 1));
				importEvent.setEtat(EtatEvenementRF.A_TRAITER);
				importEvent.setFileUrl(raftUrl);
				return evenementRFImportDAO.save(importEvent).getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job de détection des mutations
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

		// on vérifie que les mutations attendues sont bien dans la DB
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
				assertEquals(5, mutations.size());    // il y a 1 commune + 2 immeubles + 1 ayant-droit + 1 droit dans le fichier d'import et la DB était vide
				mutations.sort(new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(TypeEntiteRF.AYANT_DROIT, mut0.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
				assertEquals("_1f1091523810912201381096f93f6bfe", mut0.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<UnbekanntesGrundstueck xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckID>_1f1091523810912201381096f93f6bfe</GrundstueckID>\n" +
						             "    <IstKopie>false</IstKopie>\n" +
						             "</UnbekanntesGrundstueck>\n", mut0.getXmlContent());

				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(TypeEntiteRF.DROIT, mut1.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
				assertEquals("_1f1091523810912201381096f5f65a60", mut1.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<EigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckEigentumAnteil VersionID=\"1f109152381091220138109daa84404c\" MasterID=\"1f109152381091220138109daa7e4012\">\n" +
						             "        <Quote>\n" +
						             "            <AnteilZaehler>32</AnteilZaehler>\n" +
						             "            <AnteilNenner>1000</AnteilNenner>\n" +
						             "            <QuoteUnbekannt>false</QuoteUnbekannt>\n" +
						             "        </Quote>\n" +
						             "        <BelastetesGrundstueckIDREF>_1f1091523810912201381096f5f65a60</BelastetesGrundstueckIDREF>\n" +
						             "        <BerechtigtesGrundstueckIDREF>_1f1091523810912201381096f93f6bfe</BerechtigtesGrundstueckIDREF>\n" +
						             "        <GrundstueckEigentumsForm>Stockwerk</GrundstueckEigentumsForm>\n" +
						             "        <Rechtsgruende>\n" +
						             "            <AmtNummer>7</AmtNummer>\n" +
						             "            <RechtsgrundCode>\n" +
						             "                <TextDe>*Constitution de propriété par étages</TextDe>\n" +
						             "                <TextFr>Constitution de PPE</TextFr>\n" +
						             "            </RechtsgrundCode>\n" +
						             "            <BelegDatum>1989-12-22</BelegDatum>\n" +
						             "            <BelegAlt>489253</BelegAlt>\n" +
						             "        </Rechtsgruende>\n" +
						             "    </GrundstueckEigentumAnteil>\n" +
						             "</EigentumAnteilList>\n", mut1.getXmlContent());

				final EvenementRFMutation mut2 = mutations.get(2);
				assertEquals(importId, mut2.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
				assertEquals(TypeEntiteRF.IMMEUBLE, mut2.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut2.getTypeMutation());
				assertEquals("_1f1091523810912201381096f5f65a60", mut2.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<Liegenschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckID>_1f1091523810912201381096f5f65a60</GrundstueckID>\n" +
						             "    <EGrid>CH774575838377</EGrid>\n" +
						             "    <GrundstueckNummer VersionID=\"1f1091523810912201381096f5f85aa6\">\n" +
						             "        <BfsNr>132</BfsNr>\n" +
						             "        <Gemeindenamen>Lausanne</Gemeindenamen>\n" +
						             "        <StammNr>5198</StammNr>\n" +
						             "    </GrundstueckNummer>\n" +
						             "    <IstKopie>false</IstKopie>\n" +
						             "    <AmtlicheBewertung VersionID=\"1f109152381091220138109b0a56434f\">\n" +
						             "        <AmtlicherWert>0</AmtlicherWert>\n" +
						             "        <Ertragswert>0</Ertragswert>\n" +
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
						             "    <GrundstueckFlaeche VersionID=\"1f109152381091220138109df95e1a45\" MasterID=\"1f109152381091220138109de9bf782c\">\n" +
						             "        <Flaeche>389</Flaeche>\n" +
						             "        <Qualitaet>\n" +
						             "            <TextDe>*numérique</TextDe>\n" +
						             "            <TextFr>numérique</TextFr>\n" +
						             "        </Qualitaet>\n" +
						             "        <ProjektMutation>false</ProjektMutation>\n" +
						             "        <GeometrischDarstellbar>false</GeometrischDarstellbar>\n" +
						             "        <UeberlagerndeRechte>false</UeberlagerndeRechte>\n" +
						             "        <Rechtsgruende>\n" +
						             "            <AmtNummer>7</AmtNummer>\n" +
						             "            <RechtsgrundCode>\n" +
						             "                <TextDe>*Mensuration</TextDe>\n" +
						             "                <TextFr>Mensuration</TextFr>\n" +
						             "            </RechtsgrundCode>\n" +
						             "            <BelegDatum>2001-09-21</BelegDatum>\n" +
						             "            <BelegJahr>2001</BelegJahr>\n" +
						             "            <BelegNummer>3850</BelegNummer>\n" +
						             "            <BelegNummerIndex>0</BelegNummerIndex>\n" +
						             "        </Rechtsgruende>\n" +
						             "    </GrundstueckFlaeche>\n" +
						             "</Liegenschaft>\n", mut2.getXmlContent());

				final EvenementRFMutation mut3 = mutations.get(3);
				assertEquals(importId, mut3.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut3.getEtat());
				assertEquals(TypeEntiteRF.IMMEUBLE, mut3.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut3.getTypeMutation());
				assertEquals("_1f1091523810912201381096f93f6bfe", mut3.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<StockwerksEinheit xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GrundstueckID>_1f1091523810912201381096f93f6bfe</GrundstueckID>\n" +
						             "    <EGrid>CH768065658534</EGrid>\n" +
						             "    <GrundstueckNummer VersionID=\"1f1091523810912201381096f9416c6c\">\n" +
						             "        <BfsNr>132</BfsNr>\n" +
						             "        <Gemeindenamen>Lausanne</Gemeindenamen>\n" +
						             "        <StammNr>16590</StammNr>\n" +
						             "    </GrundstueckNummer>\n" +
						             "    <IstKopie>false</IstKopie>\n" +
						             "    <AmtlicheBewertung VersionID=\"1f109152381091220138109b0a564356\">\n" +
						             "        <AmtlicherWert>109000</AmtlicherWert>\n" +
						             "        <Ertragswert>0</Ertragswert>\n" +
						             "        <ProtokollNr>2010</ProtokollNr>\n" +
						             "        <ProtokollDatum>2010-12-09</ProtokollDatum>\n" +
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
						             "    <StammGrundstueck VersionID=\"1f109152381091220138109daa84404c\">\n" +
						             "        <Quote>\n" +
						             "            <AnteilZaehler>32</AnteilZaehler>\n" +
						             "            <AnteilNenner>1000</AnteilNenner>\n" +
						             "            <QuoteUnbekannt>false</QuoteUnbekannt>\n" +
						             "        </Quote>\n" +
						             "        <BelastetesGrundstueck VersionID=\"1f1091523810912201381096f5f65a60\">\n" +
						             "            <EGrid>CH774575838377</EGrid>\n" +
						             "            <GrundstueckNummer>\n" +
						             "                <BfsNr>132</BfsNr>\n" +
						             "                <Gemeindenamen>Lausanne</Gemeindenamen>\n" +
						             "                <StammNr>5198</StammNr>\n" +
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
				assertEquals("132", mut4.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <BfsNr>132</BfsNr>\n" +
						             "    <Gemeindenamen>Lausanne</Gemeindenamen>\n" +
						             "    <StammNr>0</StammNr>\n" +
						             "</GrundstueckNummer>\n", mut4.getXmlContent());
			}
		});

		// on déclenche le démarrage du job de traitement des mutations
		final Map<String, Object> params2 = new HashMap<>();
		params2.put(TraiterMutationsRFJob.ID, importId);
		params2.put(TraiterMutationsRFJob.NB_THREADS, 2);
		params2.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, Boolean.FALSE);

		final JobDefinition job2 = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params2);
		assertNotNull(job2);

		// le job doit se terminer correctement
		waitForJobCompletion(job2);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job2.getStatut());

		// on vérifie que les mutations ont bien été traitées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
				assertEquals(5, mutations.size());
				mutations.sort(new MutationComparator());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(0).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(1).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(2).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(3).getEtat());
				assertEquals(EtatEvenementRF.TRAITE, mutations.get(4).getEtat());
			}
		});

		// on vérifie que tous les entités ont bien été créées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final BienFondsRF bienFonds = (BienFondsRF) immeubleRFDAO.find(new ImmeubleRFKey("_1f1091523810912201381096f5f65a60"), null);
				assertNotNull(bienFonds);
				assertEquals("_1f1091523810912201381096f5f65a60", bienFonds.getIdRF());
				assertEquals("CH774575838377", bienFonds.getEgrid());

				final ProprieteParEtageRF ppe = (ProprieteParEtageRF) immeubleRFDAO.find(new ImmeubleRFKey("_1f1091523810912201381096f93f6bfe"), null);
				assertNotNull(ppe);
				assertEquals("_1f1091523810912201381096f93f6bfe", ppe.getIdRF());
				assertEquals("CH768065658534", ppe.getEgrid());

				final ImmeubleBeneficiaireRF immeubleDominant = (ImmeubleBeneficiaireRF) ayantDroitRFDAO.find(new AyantDroitRFKey("_1f1091523810912201381096f93f6bfe"), null);
				assertNotNull(immeubleDominant);
				assertEquals("_1f1091523810912201381096f93f6bfe", immeubleDominant.getIdRF());
				assertEquals("_1f1091523810912201381096f93f6bfe", immeubleDominant.getImmeuble().getIdRF());

				final Set<DroitProprieteRF> droits = immeubleDominant.getDroitsPropriete();
				assertEquals(1, droits.size());

				final DroitProprieteImmeubleRF droit21 = (DroitProprieteImmeubleRF) droits.iterator().next();
				assertNotNull(droit21);
				assertEquals("1f109152381091220138109daa7e4012", droit21.getMasterIdRF());
				assertNull(droit21.getDateDebut());
				assertNull(droit21.getDateFin());
				assertEquals("Constitution de PPE", droit21.getMotifDebut());
				assertEquals(RegDate.get(1989,12,22), droit21.getDateDebutMetier());
				assertEquals("_1f1091523810912201381096f5f65a60", droit21.getImmeuble().getIdRF());
				assertEquals(new Fraction(32, 1000), droit21.getPart());
				assertEquals(GenrePropriete.PPE, droit21.getRegime());
			}
		});
	}
}