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
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TraiterImportRFDroitJobTest extends ImportRFTestClass {

	private BatchScheduler batchScheduler;
	private DroitRFDAO droitRFDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private ZipRaftEsbStore zipRaftEsbStore;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		droitRFDAO = getBean(DroitRFDAO.class, "droitRFDAO");
		ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		zipRaftEsbStore = getBean(ZipRaftEsbStore.class, "zipRaftEsbStore");
	}

	/**
	 * Ce test vérifie que les mutations sont bien créées lorsqu'on importe un fichier RF sur une base vide
	 */
	@Test
	public void testImportDroitsBaseVide() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_droits_rf_hebdo.xml");
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
				assertEquals(5, mutations.size());    // il y a 1 immeuble + 3 droits + 1 ayant-droit dans le fichier d'import et la DB était vide
				Collections.sort(mutations, new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.AYANT_DROIT, mut0.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut0.getTypeMutation());
				assertEquals("72828ce8f830a", mut0.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<Gemeinschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <Rechtsgruende>\n" +
						             "        <AmtNummer>6</AmtNummer>\n" +
						             "        <RechtsgrundCode>\n" +
						             "            <TextFr>Héritage</TextFr>\n" +
						             "        </RechtsgrundCode>\n" +
						             "        <BelegDatum>2010-04-23</BelegDatum>\n" +
						             "        <BelegJahr>2013</BelegJahr>\n" +
						             "        <BelegNummer>33</BelegNummer>\n" +
						             "        <BelegNummerIndex>1</BelegNummerIndex>\n" +
						             "    </Rechtsgruende>\n" +
						             "    <GemeinschatID>72828ce8f830a</GemeinschatID>\n" +
						             "    <Art>Erbengemeinschaft</Art>\n" +
						             "</Gemeinschaft>\n", mut0.getXmlContent());

				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut1.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut1.getTypeMutation());
				assertEquals("029191d4fec44", mut1.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<ns2:PersonEigentumAnteilList xmlns:ns2=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <PersonEigentumAnteil MasterID=\"9a9c9e94923\">\n" +
						             "        <ns2:Quote>\n" +
						             "            <ns2:AnteilZaehler>1</ns2:AnteilZaehler>\n" +
						             "            <ns2:AnteilNenner>2</ns2:AnteilNenner>\n" +
						             "        </ns2:Quote>\n" +
						             "        <ns2:BelastetesGrundstueckIDREF>_1f109152381009be0138100bc9f139e0</ns2:BelastetesGrundstueckIDREF>\n" +
						             "        <ns2:NatuerlichePersonGb>\n" +
						             "            <ns2:GemeinschatIDREF>72828ce8f830a</ns2:GemeinschatIDREF>\n" +
						             "            <ns2:Rechtsgruende>\n" +
						             "                <ns2:AmtNummer>6</ns2:AmtNummer>\n" +
						             "                <ns2:RechtsgrundCode>\n" +
						             "                    <ns2:TextFr>Héritage</ns2:TextFr>\n" +
						             "                </ns2:RechtsgrundCode>\n" +
						             "                <ns2:BelegDatum>2010-04-23</ns2:BelegDatum>\n" +
						             "                <ns2:BelegJahr>2013</ns2:BelegJahr>\n" +
						             "                <ns2:BelegNummer>33</ns2:BelegNummer>\n" +
						             "                <ns2:BelegNummerIndex>1</ns2:BelegNummerIndex>\n" +
						             "            </ns2:Rechtsgruende>\n" +
						             "            <ns2:PersonstammIDREF>029191d4fec44</ns2:PersonstammIDREF>\n" +
						             "        </ns2:NatuerlichePersonGb>\n" +
						             "        <ns2:PersonEigentumsForm>miteigentum</ns2:PersonEigentumsForm>\n" +
						             "    </PersonEigentumAnteil>\n" +
						             "</ns2:PersonEigentumAnteilList>\n", mut1.getXmlContent());

				final EvenementRFMutation mut2 = mutations.get(2);
				assertEquals(importId, mut2.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut2.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut2.getTypeMutation());
				assertEquals("37838sc9d94de", mut2.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<ns2:PersonEigentumAnteilList xmlns:ns2=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <PersonEigentumAnteil MasterID=\"45729cd9e20\">\n" +
						             "        <ns2:Quote>\n" +
						             "            <ns2:AnteilZaehler>1</ns2:AnteilZaehler>\n" +
						             "            <ns2:AnteilNenner>2</ns2:AnteilNenner>\n" +
						             "        </ns2:Quote>\n" +
						             "        <ns2:BelastetesGrundstueckIDREF>_1f109152381009be0138100bc9f139e0</ns2:BelastetesGrundstueckIDREF>\n" +
						             "        <ns2:NatuerlichePersonGb>\n" +
						             "            <ns2:GemeinschatIDREF>72828ce8f830a</ns2:GemeinschatIDREF>\n" +
						             "            <ns2:Rechtsgruende>\n" +
						             "                <ns2:AmtNummer>6</ns2:AmtNummer>\n" +
						             "                <ns2:RechtsgrundCode>\n" +
						             "                    <ns2:TextFr>Héritage</ns2:TextFr>\n" +
						             "                </ns2:RechtsgrundCode>\n" +
						             "                <ns2:BelegDatum>2010-04-23</ns2:BelegDatum>\n" +
						             "                <ns2:BelegJahr>2013</ns2:BelegJahr>\n" +
						             "                <ns2:BelegNummer>33</ns2:BelegNummer>\n" +
						             "                <ns2:BelegNummerIndex>1</ns2:BelegNummerIndex>\n" +
						             "            </ns2:Rechtsgruende>\n" +
						             "            <ns2:PersonstammIDREF>37838sc9d94de</ns2:PersonstammIDREF>\n" +
						             "        </ns2:NatuerlichePersonGb>\n" +
						             "        <ns2:PersonEigentumsForm>miteigentum</ns2:PersonEigentumsForm>\n" +
						             "    </PersonEigentumAnteil>\n" +
						             "</ns2:PersonEigentumAnteilList>\n", mut2.getXmlContent());

				final EvenementRFMutation mut3 = mutations.get(3);
				assertEquals(importId, mut3.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut3.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut3.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut3.getTypeMutation());
				assertEquals("72828ce8f830a", mut3.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<ns2:PersonEigentumAnteilList xmlns:ns2=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <PersonEigentumAnteil MasterID=\"38458fa0ac3\">\n" +
						             "        <ns2:Quote>\n" +
						             "            <ns2:AnteilZaehler>1</ns2:AnteilZaehler>\n" +
						             "            <ns2:AnteilNenner>1</ns2:AnteilNenner>\n" +
						             "        </ns2:Quote>\n" +
						             "        <ns2:BelastetesGrundstueckIDREF>_1f109152381009be0138100bc9f139e0</ns2:BelastetesGrundstueckIDREF>\n" +
						             "        <ns2:Gemeinschaft>\n" +
						             "            <ns2:Rechtsgruende>\n" +
						             "                <ns2:AmtNummer>6</ns2:AmtNummer>\n" +
						             "                <ns2:RechtsgrundCode>\n" +
						             "                    <ns2:TextFr>Héritage</ns2:TextFr>\n" +
						             "                </ns2:RechtsgrundCode>\n" +
						             "                <ns2:BelegDatum>2010-04-23</ns2:BelegDatum>\n" +
						             "                <ns2:BelegJahr>2013</ns2:BelegJahr>\n" +
						             "                <ns2:BelegNummer>33</ns2:BelegNummer>\n" +
						             "                <ns2:BelegNummerIndex>1</ns2:BelegNummerIndex>\n" +
						             "            </ns2:Rechtsgruende>\n" +
						             "            <ns2:GemeinschatID>72828ce8f830a</ns2:GemeinschatID>\n" +
						             "            <ns2:Art>Erbengemeinschaft</ns2:Art>\n" +
						             "        </ns2:Gemeinschaft>\n" +
						             "        <ns2:PersonEigentumsForm>alleineigentum</ns2:PersonEigentumsForm>\n" +
						             "    </PersonEigentumAnteil>\n" +
						             "</ns2:PersonEigentumAnteilList>\n", mut3.getXmlContent());

				final EvenementRFMutation mut4 = mutations.get(4);
				assertEquals(importId, mut4.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut4.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.IMMEUBLE, mut4.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut4.getTypeMutation());
				assertEquals("_1f109152381009be0138100bc9f139e0", mut4.getIdRF());
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
						             "</Liegenschaft>\n", mut4.getXmlContent());

			}
		});

	}

	/**
	 * Ce test vérifie qu'aucune mutation n'est créées lorsqu'on importe un fichier RF et que les surfaces au sol dans la base sont déjà à jour.
	 */
	@Test
	public void testImportDroitsDejaAJour() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2008, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2010, 1, 1);
		final RegDate dateTroisiemeImport = RegDate.get(2016, 10, 1);

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_droits_rf_hebdo.xml");
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
				// données équivalentes au fichier export_droits_rf_hebdo.xml
				BienFondRF bienFond = newBienFondRF("_1f109152381009be0138100bc9f139e0", "CH938383459516", 273, 3, 1100000L, "RG96", null, false, false, dateImportInitial, 2969451);
				bienFond = (BienFondRF) immeubleRFDAO.save(bienFond);

				PersonnePhysiqueRF pp1 = newPersonnePhysique("029191d4fec44", 123344L, 238282L, "Peuplu", "Jean", RegDate.get(1955, 5, 15));
				pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

				PersonnePhysiqueRF pp2 = newPersonnePhysique("37838sc9d94de", 123345L, 238262, "Peuplu", "Jeannette", RegDate.get(1955, 11, 2));
				pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

				CommunauteRF communaute = newCommunauté("72828ce8f830a", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
				communaute = (CommunauteRF) ayantDroitRFDAO.save(communaute);

				// quelques données historiques (qui doivent être ignorées)
				final DroitProprietePersonnePhysiqueRF droit1_1 =
						newDroitPP("9a9c9e94923", pp1, bienFond, communaute, new Fraction(3, 7), GenrePropriete.COPROPRIETE, RegDate.get(2010, 4, 23),
						           new IdentifiantAffaireRF(6, 2013, 33, 1), dateImportInitial, "Héritage", dateSecondImport.getOneDayBefore());
				final DroitProprietePersonnePhysiqueRF droit2_1 =
						newDroitPP("45729cd9e20", pp2, bienFond, communaute, new Fraction(4, 7), GenrePropriete.COPROPRIETE, RegDate.get(2010, 4, 23),
						           new IdentifiantAffaireRF(6, 2013, 33, 1), dateImportInitial, "Héritage", dateSecondImport.getOneDayBefore());
				final DroitProprieteCommunauteRF droit3_1 =
						newDroitColl("38458fa0ac3", communaute, bienFond, new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, RegDate.get(2010, 4, 23),
						             new IdentifiantAffaireRF(6, 2013, 33, 1), dateImportInitial, "Vol à main armée", dateSecondImport.getOneDayBefore());
				droitRFDAO.save(droit1_1);
				droitRFDAO.save(droit2_1);
				droitRFDAO.save(droit3_1);

				// les données courantes
				final DroitProprietePersonnePhysiqueRF droit1_2 =
						newDroitPP("9a9c9e94923", pp1, bienFond, communaute, new Fraction(1, 2), GenrePropriete.COPROPRIETE, RegDate.get(2010, 4, 23),
						           new IdentifiantAffaireRF(6, 2013, 33, 1), dateSecondImport, "Héritage", null);
				final DroitProprietePersonnePhysiqueRF droit2_2 =
						newDroitPP("45729cd9e20", pp2, bienFond, communaute, new Fraction(1, 2), GenrePropriete.COPROPRIETE, RegDate.get(2010, 4, 23),
						           new IdentifiantAffaireRF(6, 2013, 33, 1), dateSecondImport, "Héritage", null);
				final DroitProprieteCommunauteRF droit3_2 =
						newDroitColl("38458fa0ac3", communaute, bienFond, new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, RegDate.get(2010, 4, 23),
						             new IdentifiantAffaireRF(6, 2013, 33, 1), dateSecondImport, "Héritage", null);
				droitRFDAO.save(droit1_2);
				droitRFDAO.save(droit2_2);
				droitRFDAO.save(droit3_2);
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
				assertEquals(0, mutations.size());    // il y a 1 immeuble + 3 droits + 1 ayant-droit dans le fichier d'import et ils sont tous identiques à ceux dans la DB
			}
		});
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsqu'on importe un fichier RF et que les droits dans la base sont différents.
	 */
	@Test
	public void testImportDroitsAvecModifications() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_droits_rf_hebdo.xml");
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

				// données partiellement différentes de celles du fichier export_droits_rf_hebdo.xml
				BienFondRF bienFond = newBienFondRF("_1f109152381009be0138100bc9f139e0", "CH938383459516", 273, 3, 1100000L, "RG96", null, false, false, dateImportInitial, 2969451);
				bienFond = (BienFondRF) immeubleRFDAO.save(bienFond);

				PersonnePhysiqueRF pp1 = newPersonnePhysique("029191d4fec44", 123344L, 238282L, "Peuplu", "Jean", RegDate.get(1955, 5, 15));
				pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

				PersonnePhysiqueRF pp2 = newPersonnePhysique("37838sc9d94de", 123345L, 238262, "Peuplu", "Jeannette", RegDate.get(1955, 11, 2));
				pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

				CommunauteRF communaute = newCommunauté("72828ce8f830a", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
				communaute = (CommunauteRF) ayantDroitRFDAO.save(communaute);

				// - part différente
				final DroitProprietePersonnePhysiqueRF droit1_2 =
						newDroitPP("9a9c9e94923", pp1, bienFond, communaute, new Fraction(3, 7), GenrePropriete.COPROPRIETE, RegDate.get(2010, 4, 23),
						           new IdentifiantAffaireRF(6, 2013, 33, 1), dateImportInitial, "Héritage", null);
				// - part différente
				final DroitProprietePersonnePhysiqueRF droit2_2 =
						newDroitPP("45729cd9e20", pp2, bienFond, communaute, new Fraction(4, 7), GenrePropriete.COPROPRIETE, RegDate.get(2010, 4, 23),
						           new IdentifiantAffaireRF(6, 2013, 33, 1), dateImportInitial, "Héritage", null);
				// - motif différent
				final DroitProprieteCommunauteRF droit3_2 =
						newDroitColl("38458fa0ac3", communaute, bienFond, new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, RegDate.get(2010, 4, 23),
						             new IdentifiantAffaireRF(6, 2013, 33, 1), dateImportInitial, "Vol à main armée", null);
				droitRFDAO.save(droit1_2);
				droitRFDAO.save(droit2_2);
				droitRFDAO.save(droit3_2);
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
				assertEquals(3, mutations.size());    // les 3 droits dans le fichier d'import sont différents
				Collections.sort(mutations, new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut0.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.MODIFICATION, mut0.getTypeMutation());
				assertEquals("029191d4fec44", mut0.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<ns2:PersonEigentumAnteilList xmlns:ns2=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <PersonEigentumAnteil MasterID=\"9a9c9e94923\">\n" +
						             "        <ns2:Quote>\n" +
						             "            <ns2:AnteilZaehler>1</ns2:AnteilZaehler>\n" +
						             "            <ns2:AnteilNenner>2</ns2:AnteilNenner>\n" +
						             "        </ns2:Quote>\n" +
						             "        <ns2:BelastetesGrundstueckIDREF>_1f109152381009be0138100bc9f139e0</ns2:BelastetesGrundstueckIDREF>\n" +
						             "        <ns2:NatuerlichePersonGb>\n" +
						             "            <ns2:GemeinschatIDREF>72828ce8f830a</ns2:GemeinschatIDREF>\n" +
						             "            <ns2:Rechtsgruende>\n" +
						             "                <ns2:AmtNummer>6</ns2:AmtNummer>\n" +
						             "                <ns2:RechtsgrundCode>\n" +
						             "                    <ns2:TextFr>Héritage</ns2:TextFr>\n" +
						             "                </ns2:RechtsgrundCode>\n" +
						             "                <ns2:BelegDatum>2010-04-23</ns2:BelegDatum>\n" +
						             "                <ns2:BelegJahr>2013</ns2:BelegJahr>\n" +
						             "                <ns2:BelegNummer>33</ns2:BelegNummer>\n" +
						             "                <ns2:BelegNummerIndex>1</ns2:BelegNummerIndex>\n" +
						             "            </ns2:Rechtsgruende>\n" +
						             "            <ns2:PersonstammIDREF>029191d4fec44</ns2:PersonstammIDREF>\n" +
						             "        </ns2:NatuerlichePersonGb>\n" +
						             "        <ns2:PersonEigentumsForm>miteigentum</ns2:PersonEigentumsForm>\n" +
						             "    </PersonEigentumAnteil>\n" +
						             "</ns2:PersonEigentumAnteilList>\n", mut0.getXmlContent());

				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut1.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.MODIFICATION, mut1.getTypeMutation());
				assertEquals("37838sc9d94de", mut1.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<ns2:PersonEigentumAnteilList xmlns:ns2=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <PersonEigentumAnteil MasterID=\"45729cd9e20\">\n" +
						             "        <ns2:Quote>\n" +
						             "            <ns2:AnteilZaehler>1</ns2:AnteilZaehler>\n" +
						             "            <ns2:AnteilNenner>2</ns2:AnteilNenner>\n" +
						             "        </ns2:Quote>\n" +
						             "        <ns2:BelastetesGrundstueckIDREF>_1f109152381009be0138100bc9f139e0</ns2:BelastetesGrundstueckIDREF>\n" +
						             "        <ns2:NatuerlichePersonGb>\n" +
						             "            <ns2:GemeinschatIDREF>72828ce8f830a</ns2:GemeinschatIDREF>\n" +
						             "            <ns2:Rechtsgruende>\n" +
						             "                <ns2:AmtNummer>6</ns2:AmtNummer>\n" +
						             "                <ns2:RechtsgrundCode>\n" +
						             "                    <ns2:TextFr>Héritage</ns2:TextFr>\n" +
						             "                </ns2:RechtsgrundCode>\n" +
						             "                <ns2:BelegDatum>2010-04-23</ns2:BelegDatum>\n" +
						             "                <ns2:BelegJahr>2013</ns2:BelegJahr>\n" +
						             "                <ns2:BelegNummer>33</ns2:BelegNummer>\n" +
						             "                <ns2:BelegNummerIndex>1</ns2:BelegNummerIndex>\n" +
						             "            </ns2:Rechtsgruende>\n" +
						             "            <ns2:PersonstammIDREF>37838sc9d94de</ns2:PersonstammIDREF>\n" +
						             "        </ns2:NatuerlichePersonGb>\n" +
						             "        <ns2:PersonEigentumsForm>miteigentum</ns2:PersonEigentumsForm>\n" +
						             "    </PersonEigentumAnteil>\n" +
						             "</ns2:PersonEigentumAnteilList>\n", mut1.getXmlContent());

				final EvenementRFMutation mut2 = mutations.get(2);
				assertEquals(importId, mut2.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut2.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.MODIFICATION, mut2.getTypeMutation());
				assertEquals("72828ce8f830a", mut2.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<ns2:PersonEigentumAnteilList xmlns:ns2=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <PersonEigentumAnteil MasterID=\"38458fa0ac3\">\n" +
						             "        <ns2:Quote>\n" +
						             "            <ns2:AnteilZaehler>1</ns2:AnteilZaehler>\n" +
						             "            <ns2:AnteilNenner>1</ns2:AnteilNenner>\n" +
						             "        </ns2:Quote>\n" +
						             "        <ns2:BelastetesGrundstueckIDREF>_1f109152381009be0138100bc9f139e0</ns2:BelastetesGrundstueckIDREF>\n" +
						             "        <ns2:Gemeinschaft>\n" +
						             "            <ns2:Rechtsgruende>\n" +
						             "                <ns2:AmtNummer>6</ns2:AmtNummer>\n" +
						             "                <ns2:RechtsgrundCode>\n" +
						             "                    <ns2:TextFr>Héritage</ns2:TextFr>\n" +
						             "                </ns2:RechtsgrundCode>\n" +
						             "                <ns2:BelegDatum>2010-04-23</ns2:BelegDatum>\n" +
						             "                <ns2:BelegJahr>2013</ns2:BelegJahr>\n" +
						             "                <ns2:BelegNummer>33</ns2:BelegNummer>\n" +
						             "                <ns2:BelegNummerIndex>1</ns2:BelegNummerIndex>\n" +
						             "            </ns2:Rechtsgruende>\n" +
						             "            <ns2:GemeinschatID>72828ce8f830a</ns2:GemeinschatID>\n" +
						             "            <ns2:Art>Erbengemeinschaft</ns2:Art>\n" +
						             "        </ns2:Gemeinschaft>\n" +
						             "        <ns2:PersonEigentumsForm>alleineigentum</ns2:PersonEigentumsForm>\n" +
						             "    </PersonEigentumAnteil>\n" +
						             "</ns2:PersonEigentumAnteilList>\n", mut2.getXmlContent());
			}
		});
	}

	/**
	 * Ce test vérifie que des mutations de suppression sont créées si des propriétaires avec des droits dans la DB n'en ont plus dans le fichier d'import.
	 */
	@Test
	public void testImportSuppressionDeDroits() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_droits_vides_rf_hebdo.xml");
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
				// données équivalentes au fichier export_droits_rf_hebdo.xml
				BienFondRF bienFond = newBienFondRF("_1f109152381009be0138100bc9f139e0", "CH938383459516", 273, 3, 1100000L, "RG96", null, false, false, dateImportInitial, 2969451);
				bienFond = (BienFondRF) immeubleRFDAO.save(bienFond);

				PersonnePhysiqueRF pp1 = newPersonnePhysique("029191d4fec44", 123344L, 238282L, "Peuplu", "Jean", RegDate.get(1955, 5, 15));
				pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

				PersonnePhysiqueRF pp2 = newPersonnePhysique("37838sc9d94de", 123345L, 238262, "Peuplu", "Jeannette", RegDate.get(1955, 11, 2));
				pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

				CommunauteRF communaute = newCommunauté("72828ce8f830a", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
				communaute = (CommunauteRF) ayantDroitRFDAO.save(communaute);

				// les données courantes
				final DroitProprietePersonnePhysiqueRF droit1 =
						newDroitPP("9a9c9e94923", pp1, bienFond, communaute, new Fraction(1, 2), GenrePropriete.COPROPRIETE, RegDate.get(2010, 4, 23),
						           new IdentifiantAffaireRF(6, 2013, 33, 1), dateImportInitial, "Héritage", null);
				final DroitProprietePersonnePhysiqueRF droit2 =
						newDroitPP("45729cd9e20", pp2, bienFond, communaute, new Fraction(1, 2), GenrePropriete.COPROPRIETE, RegDate.get(2010, 4, 23),
						           new IdentifiantAffaireRF(6, 2013, 33, 1), dateImportInitial, "Héritage", null);
				final DroitProprieteCommunauteRF droit3 =
						newDroitColl("38458fa0ac3", communaute, bienFond, new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, RegDate.get(2010, 4, 23),
						             new IdentifiantAffaireRF(6, 2013, 33, 1), dateImportInitial, "Héritage", null);
				droitRFDAO.save(droit1);
				droitRFDAO.save(droit2);
				droitRFDAO.save(droit3);
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
				assertEquals(3, mutations.size());    // les 3 droits qui existent dans la DB devront être fermés
				Collections.sort(mutations, new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut0.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.SUPPRESSION, mut0.getTypeMutation());
				assertEquals("029191d4fec44", mut0.getIdRF());
				assertNull(mut0.getXmlContent());

				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut1.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.SUPPRESSION, mut1.getTypeMutation());
				assertEquals("37838sc9d94de", mut1.getIdRF());
				assertNull(mut1.getXmlContent());

				final EvenementRFMutation mut2 = mutations.get(2);
				assertEquals(importId, mut2.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
				assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut2.getTypeEntite());
				assertEquals(EvenementRFMutation.TypeMutation.SUPPRESSION, mut2.getTypeMutation());
				assertEquals("72828ce8f830a", mut2.getIdRF());
				assertNull(mut2.getXmlContent());
			}
		});
	}
}