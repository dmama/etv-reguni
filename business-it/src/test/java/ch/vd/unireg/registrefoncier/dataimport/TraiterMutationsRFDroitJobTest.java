package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeImportRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.GenrePropriete;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static ch.vd.uniregctb.registrefoncier.processor.MutationRFProcessorTestCase.assertRaisonAcquisition;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TraiterMutationsRFDroitJobTest extends ImportRFTestClass {

	private BatchScheduler batchScheduler;
	private ImmeubleRFDAO immeubleRFDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private DroitRFDAO droitRFDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		droitRFDAO = getBean(DroitRFDAO.class, "droitRFDAO");
	}

	/**
	 * Ce test vérifie que les mutations de type CREATION créent bien de nouveaux droits
	 */
	@Test
	public void testTraiterMutationsCreation() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);

		class Ids {
			long pp1;
			long pp2;
		}
		final Ids ids = new Ids();

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setType(TypeImportRF.PRINCIPAL);
				importEvent.setDateEvenement(dateImport);
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
				pp1.setIdRF("_1f109152381009be0138100a1d442eee");
				pp1.setPrenom("Alodie");
				pp1.setNom("Schulz");
				pp1.setDateNaissance(RegDate.get(1970, 1, 1));
				pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);
				ids.pp1 = pp1.getId();

				PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
				pp2.setIdRF("_22222222222222222222222222222222");
				pp2.setPrenom("Jocker");
				pp2.setNom("Bad");
				pp2.setDateNaissance(RegDate.get(1970, 1, 1));
				pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);
				ids.pp2 = pp2.getId();

				BienFondsRF immeuble1 = new BienFondsRF();
				immeuble1.setIdRF("_8af806fc4a35927c014ae2a6e76041b8");
				immeubleRFDAO.save(immeuble1);

				BienFondsRF immeuble2 = new BienFondsRF();
				immeuble2.setIdRF("_1f109152381009be0138100ba7e31031");
				immeubleRFDAO.save(immeuble2);

				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.DROIT);
				mut0.setTypeMutation(TypeMutationRF.CREATION);
				mut0.setIdRF("_8af806fc4a35927c014ae2a6e76041b8");
				mut0.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<EigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <PersonEigentumAnteil VersionID=\"8af806fa4a4dd302014b16fc17256a06\" MasterID=\"8af806fa4a4dd302014b16fc17266a0b\">\n" +
						                   "        <Quote>\n" +
						                   "            <AnteilZaehler>1</AnteilZaehler>\n" +
						                   "            <AnteilNenner>2</AnteilNenner>\n" +
						                   "            <QuoteUnbekannt>false</QuoteUnbekannt>\n" +
						                   "        </Quote>\n" +
						                   "        <BelastetesGrundstueckIDREF>_8af806fc4a35927c014ae2a6e76041b8</BelastetesGrundstueckIDREF>\n" +
						                   "        <NatuerlichePersonGb VersionID=\"8af806fa4a4dd302014b16fc17266a0a\" MasterID=\"8af806fa4a4dd302014b16fc17276a0e\">\n" +
						                   "            <Name>Schulz</Name>\n" +
						                   "            <Status>definitiv</Status>\n" +
						                   "            <Rechtsgruende>\n" +
						                   "                <AmtNummer>6</AmtNummer>\n" +
						                   "                <RechtsgrundCode>\n" +
						                   "                    <TextDe>*Achat</TextDe>\n" +
						                   "                    <TextFr>Achat</TextFr>\n" +
						                   "                </RechtsgrundCode>\n" +
						                   "                <BelegDatum>2014-12-23</BelegDatum>\n" +
						                   "                <BelegJahr>2014</BelegJahr>\n" +
						                   "                <BelegNummer>9593</BelegNummer>\n" +
						                   "                <BelegNummerIndex>0</BelegNummerIndex>\n" +
						                   "            </Rechtsgruende>\n" +
						                   "            <Vorname>Alodie</Vorname>\n" +
						                   "            <Geburtsdatum>\n" +
						                   "                <Tag>5</Tag>\n" +
						                   "                <Monat>11</Monat>\n" +
						                   "                <Jahr>1987</Jahr>\n" +
						                   "            </Geburtsdatum>\n" +
						                   "            <Zivilstand>unbekannt</Zivilstand>\n" +
						                   "            <NameEltern>Claude Patrick</NameEltern>\n" +
						                   "            <WeitereVornamen>Lydia</WeitereVornamen>\n" +
						                   "            <PersonstammIDREF>_1f109152381009be0138100a1d442eee</PersonstammIDREF>\n" +
						                   "        </NatuerlichePersonGb>\n" +
						                   "        <PersonEigentumsForm>miteigentum</PersonEigentumsForm>\n" +
						                   "    </PersonEigentumAnteil>\n" +
						                   "</EigentumAnteilList>\n");
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.DROIT);
				mut1.setTypeMutation(TypeMutationRF.CREATION);
				mut1.setIdRF("_1f109152381009be0138100ba7e31031");
				mut1.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<EigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <PersonEigentumAnteil VersionID=\"1f109152381009be0138100e4c7c00e5\" MasterID=\"1f109152381009be0138100c87276e68\">\n" +
						                   "        <Quote>\n" +
						                   "            <AnteilZaehler>1</AnteilZaehler>\n" +
						                   "            <AnteilNenner>1</AnteilNenner>\n" +
						                   "            <QuoteUnbekannt>false</QuoteUnbekannt>\n" +
						                   "        </Quote>\n" +
						                   "        <BelastetesGrundstueckIDREF>_1f109152381009be0138100ba7e31031</BelastetesGrundstueckIDREF>\n" +
						                   "        <NatuerlichePersonGb VersionID=\"1f109152381009be0138100c87296e9a\" MasterID=\"1f109152381009be0138100e4c7c00e6\">\n" +
						                   "            <Name>Schulz</Name>\n" +
						                   "            <Status>definitiv</Status>\n" +
						                   "            <Rechtsgruende>\n" +
						                   "                <AmtNummer>13</AmtNummer>\n" +
						                   "                <RechtsgrundCode>\n" +
						                   "                    <TextDe>*Donation</TextDe>\n" +
						                   "                    <TextFr>Donation</TextFr>\n" +
						                   "                </RechtsgrundCode>\n" +
						                   "                <BelegDatum>2007-02-07</BelegDatum>\n" +
						                   "                <BelegJahr>2007</BelegJahr>\n" +
						                   "                <BelegNummer>173</BelegNummer>\n" +
						                   "                <BelegNummerIndex>0</BelegNummerIndex>\n" +
						                   "            </Rechtsgruende>\n" +
						                   "            <Vorname>Alodie</Vorname>\n" +
						                   "            <Geburtsdatum>\n" +
						                   "                <Tag>5</Tag>\n" +
						                   "                <Monat>11</Monat>\n" +
						                   "                <Jahr>1987</Jahr>\n" +
						                   "            </Geburtsdatum>\n" +
						                   "            <Zivilstand>unbekannt</Zivilstand>\n" +
						                   "            <NameEltern>Claude</NameEltern>\n" +
						                   "            <WeitereVornamen>Lydia</WeitereVornamen>\n" +
						                   "            <PersonstammIDREF>_22222222222222222222222222222222</PersonstammIDREF>\n" +
						                   "        </NatuerlichePersonGb>\n" +
						                   "        <PersonEigentumsForm>alleineigentum</PersonEigentumsForm>\n" +
						                   "        <AnzahlPaquiers>0</AnzahlPaquiers>\n" +
						                   "    </PersonEigentumAnteil>\n" +
						                   "</EigentumAnteilList>\n");
				evenementRFMutationDAO.save(mut1);

				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, Boolean.FALSE);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		assertEtatMutations(2, EtatEvenementRF.TRAITE); // il y a 2 propriétaires différents dans le fichier d'import

		// on vérifie que les droits ont bien été créées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final AyantDroitRF pp1 = ayantDroitRFDAO.get(ids.pp1);
				assertNotNull(pp1);

				final List<DroitProprieteRF> droits1 = new ArrayList<>(pp1.getDroitsPropriete());
				assertEquals(1, droits1.size());

				final DroitProprietePersonnePhysiqueRF droit11 = (DroitProprietePersonnePhysiqueRF) droits1.get(0);
				assertNotNull(droit11);
				assertEquals("8af806fa4a4dd302014b16fc17266a0b", droit11.getMasterIdRF());
				assertEquals(dateImport, droit11.getDateDebut());
				assertNull(droit11.getDateFin());
				assertEquals("Achat", droit11.getMotifDebut());
				assertEquals(RegDate.get(2014, 12, 23), droit11.getDateDebutMetier());
				assertEquals("_8af806fc4a35927c014ae2a6e76041b8", droit11.getImmeuble().getIdRF());
				assertEquals(new Fraction(1, 2), droit11.getPart());
				assertEquals(GenrePropriete.COPROPRIETE, droit11.getRegime());

				final Set<RaisonAcquisitionRF> raisons11 = droit11.getRaisonsAcquisition();
				assertEquals(1, raisons11.size());
				assertRaisonAcquisition(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0), raisons11.iterator().next());

				final AyantDroitRF pp2 = ayantDroitRFDAO.get(ids.pp2);
				assertNotNull(pp2);

				final List<DroitProprieteRF> droits2 = new ArrayList<>(pp2.getDroitsPropriete());
				assertEquals(1, droits2.size());

				final DroitProprietePersonnePhysiqueRF droit21 = (DroitProprietePersonnePhysiqueRF) droits2.get(0);
				assertNotNull(droit21);
				assertEquals("1f109152381009be0138100c87276e68", droit21.getMasterIdRF());
				assertEquals(dateImport, droit21.getDateDebut());
				assertNull(droit21.getDateFin());
				assertEquals("Donation", droit21.getMotifDebut());
				assertEquals(RegDate.get(2007, 2, 7), droit21.getDateDebutMetier());
				assertEquals("_1f109152381009be0138100ba7e31031", droit21.getImmeuble().getIdRF());
				assertEquals(new Fraction(1, 1), droit21.getPart());
				assertEquals(GenrePropriete.INDIVIDUELLE, droit21.getRegime());

				final Set<RaisonAcquisitionRF> raisons21 = droit21.getRaisonsAcquisition();
				assertEquals(1, raisons21.size());
				assertRaisonAcquisition(RegDate.get(2007, 2, 7), "Donation", new IdentifiantAffaireRF(13, 2007, 173, 0), raisons21.iterator().next());
			}
		});
	}

	/**
	 * Ce test vérifie que les mutations de type MODIFICATION modifient bien des droits existants
	 */
	@Test
	public void testTraiterMutationsModification() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		class Ids {
			long pp1;
			long pp2;
		}
		final Ids ids = new Ids();

		// on insère les données de l'import initial dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
				pp1.setIdRF("_1f109152381009be0138100a1d442eee");
				pp1.setPrenom("Alodie");
				pp1.setNom("Schulz");
				pp1.setDateNaissance(RegDate.get(1970, 1, 1));
				pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);
				ids.pp1 = pp1.getId();

				PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
				pp2.setIdRF("_22222222222222222222222222222222");
				pp2.setPrenom("Jocker");
				pp2.setNom("Bad");
				pp2.setDateNaissance(RegDate.get(1970, 1, 1));
				pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);
				ids.pp2 = pp2.getId();

				BienFondsRF immeuble1 = new BienFondsRF();
				immeuble1.setIdRF("_1f109152381009be0138100ba7e31031");
				immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);

				// on droit différent de celui qui arrive dans le fichier XML
				final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
				droit1.setMasterIdRF("1f109152381009be0138100c87000000");
				droit1.setVersionIdRF("1f109152381009be0138100c87000001");
				droit1.setDateDebut(dateImportInitial);
				droit1.setDateDebutMetier(RegDate.get(2004, 2, 7));
				droit1.setMotifDebut("Appropriation illégitime");
				droit1.setAyantDroit(pp1);
				droit1.setImmeuble(immeuble1);
				droit1.setPart(new Fraction(1, 1));
				droit1.setRegime(GenrePropriete.INDIVIDUELLE);
				// motif différent
				droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2004, 2, 7), "Appropriation illégitime", new IdentifiantAffaireRF(13, 2007, 173, 0)));
				droitRFDAO.save(droit1);

				BienFondsRF immeuble2 = new BienFondsRF();
				immeuble2.setIdRF("_8af806fc4a35927c014ae2a6e76041b8");
				immeuble2 = (BienFondsRF) immeubleRFDAO.save(immeuble2);

				// on droit identique à celui qui arrive dans le fichier XML
				final DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
				droit2.setMasterIdRF("8af806fa4a4dd302014b16fc17266a0b");
				droit2.setVersionIdRF("8af806fa4a4dd302014b16fc17256a06");
				droit2.setDateDebut(dateImportInitial);
				droit2.setDateDebutMetier(RegDate.get(2014, 12, 23));
				droit2.setMotifDebut("Achat");
				droit2.setAyantDroit(pp2);
				droit2.setImmeuble(immeuble2);
				droit2.setPart(new Fraction(1, 2));
				droit2.setRegime(GenrePropriete.COPROPRIETE);
				droit2.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0)));
				droitRFDAO.save(droit2);
			}
		});

		// on insère les données du second import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setType(TypeImportRF.PRINCIPAL);
				importEvent.setDateEvenement(dateSecondImport);
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.DROIT);
				mut0.setTypeMutation(TypeMutationRF.MODIFICATION);
				mut0.setIdRF("_1f109152381009be0138100ba7e31031");
				mut0.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<EigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <PersonEigentumAnteil VersionID=\"1f109152381009be0138100e4c7c00e5\" MasterID=\"1f109152381009be0138100c87276e68\">\n" +
						                   "        <Quote>\n" +
						                   "            <AnteilZaehler>1</AnteilZaehler>\n" +
						                   "            <AnteilNenner>1</AnteilNenner>\n" +
						                   "            <QuoteUnbekannt>false</QuoteUnbekannt>\n" +
						                   "        </Quote>\n" +
						                   "        <BelastetesGrundstueckIDREF>_1f109152381009be0138100ba7e31031</BelastetesGrundstueckIDREF>\n" +
						                   "        <NatuerlichePersonGb VersionID=\"1f109152381009be0138100c87296e9a\" MasterID=\"1f109152381009be0138100e4c7c00e6\">\n" +
						                   "            <Name>Schulz</Name>\n" +
						                   "            <Status>definitiv</Status>\n" +
						                   "            <Rechtsgruende>\n" +
						                   "                <AmtNummer>13</AmtNummer>\n" +
						                   "                <RechtsgrundCode>\n" +
						                   "                    <TextDe>*Donation</TextDe>\n" +
						                   "                    <TextFr>Donation</TextFr>\n" +
						                   "                </RechtsgrundCode>\n" +
						                   "                <BelegDatum>2007-02-07</BelegDatum>\n" +
						                   "                <BelegJahr>2007</BelegJahr>\n" +
						                   "                <BelegNummer>173</BelegNummer>\n" +
						                   "                <BelegNummerIndex>0</BelegNummerIndex>\n" +
						                   "            </Rechtsgruende>\n" +
						                   "            <Vorname>Alodie</Vorname>\n" +
						                   "            <Geburtsdatum>\n" +
						                   "                <Tag>5</Tag>\n" +
						                   "                <Monat>11</Monat>\n" +
						                   "                <Jahr>1987</Jahr>\n" +
						                   "            </Geburtsdatum>\n" +
						                   "            <Zivilstand>unbekannt</Zivilstand>\n" +
						                   "            <NameEltern>Claude</NameEltern>\n" +
						                   "            <WeitereVornamen>Lydia</WeitereVornamen>\n" +
						                   "            <PersonstammIDREF>_1f109152381009be0138100a1d442eee</PersonstammIDREF>\n" +
						                   "        </NatuerlichePersonGb>\n" +
						                   "        <PersonEigentumsForm>alleineigentum</PersonEigentumsForm>\n" +
						                   "        <AnzahlPaquiers>0</AnzahlPaquiers>\n" +
						                   "    </PersonEigentumAnteil>\n" +
						                   "</EigentumAnteilList>\n");
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.DROIT);
				mut1.setTypeMutation(TypeMutationRF.MODIFICATION);
				mut1.setIdRF("_8af806fc4a35927c014ae2a6e76041b8");
				mut1.setXmlContent("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						                   "<EigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						                   "    <PersonEigentumAnteil VersionID=\"8af806fa4a4dd302014b16fc17256a06\" MasterID=\"8af806fa4a4dd302014b16fc17266a0b\">\n" +
						                   "        <Quote>\n" +
						                   "            <AnteilZaehler>1</AnteilZaehler>\n" +
						                   "            <AnteilNenner>2</AnteilNenner>\n" +
						                   "            <QuoteUnbekannt>false</QuoteUnbekannt>\n" +
						                   "        </Quote>\n" +
						                   "        <BelastetesGrundstueckIDREF>_8af806fc4a35927c014ae2a6e76041b8</BelastetesGrundstueckIDREF>\n" +
						                   "        <NatuerlichePersonGb VersionID=\"8af806fa4a4dd302014b16fc17266a0a\" MasterID=\"8af806fa4a4dd302014b16fc17276a0e\">\n" +
						                   "            <Name>Schulz</Name>\n" +
						                   "            <Status>definitiv</Status>\n" +
						                   "            <Rechtsgruende>\n" +
						                   "                <AmtNummer>6</AmtNummer>\n" +
						                   "                <RechtsgrundCode>\n" +
						                   "                    <TextDe>*Achat</TextDe>\n" +
						                   "                    <TextFr>Achat</TextFr>\n" +
						                   "                </RechtsgrundCode>\n" +
						                   "                <BelegDatum>2014-12-23</BelegDatum>\n" +
						                   "                <BelegJahr>2014</BelegJahr>\n" +
						                   "                <BelegNummer>9593</BelegNummer>\n" +
						                   "                <BelegNummerIndex>0</BelegNummerIndex>\n" +
						                   "            </Rechtsgruende>\n" +
						                   "            <Vorname>Alodie</Vorname>\n" +
						                   "            <Geburtsdatum>\n" +
						                   "                <Tag>5</Tag>\n" +
						                   "                <Monat>11</Monat>\n" +
						                   "                <Jahr>1987</Jahr>\n" +
						                   "            </Geburtsdatum>\n" +
						                   "            <Zivilstand>unbekannt</Zivilstand>\n" +
						                   "            <NameEltern>Claude Patrick</NameEltern>\n" +
						                   "            <WeitereVornamen>Lydia</WeitereVornamen>\n" +
						                   "            <PersonstammIDREF>_22222222222222222222222222222222</PersonstammIDREF>\n" +
						                   "        </NatuerlichePersonGb>\n" +
						                   "        <PersonEigentumsForm>miteigentum</PersonEigentumsForm>\n" +
						                   "    </PersonEigentumAnteil>\n" +
						                   "</EigentumAnteilList>\n");
				evenementRFMutationDAO.save(mut1);
				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, Boolean.FALSE);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		assertEtatMutations(2, EtatEvenementRF.TRAITE);

		// on vérifie que les droits ont bien été mis-à-jour
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final AyantDroitRF pp1 = ayantDroitRFDAO.get(ids.pp1);
				assertNotNull(pp1);

				final List<DroitProprieteRF> droits1 = new ArrayList<>(pp1.getDroitsPropriete());
				assertEquals(2, droits1.size());
				droits1.sort(Comparator.comparing(DroitRF::getMasterIdRF));

				// l'ancien droit a été fermé
				final DroitProprietePersonnePhysiqueRF droit11 = (DroitProprietePersonnePhysiqueRF) droits1.get(0);
				assertNotNull(droit11);
				assertEquals("1f109152381009be0138100c87000000", droit11.getMasterIdRF());
				assertEquals(dateImportInitial, droit11.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), droit11.getDateFin());
				assertEquals("Appropriation illégitime", droit11.getMotifDebut());
				assertEquals(RegDate.get(2004, 2, 7), droit11.getDateDebutMetier());
				assertEquals("_1f109152381009be0138100ba7e31031", droit11.getImmeuble().getIdRF());
				assertEquals(new Fraction(1, 1), droit11.getPart());
				assertEquals(GenrePropriete.INDIVIDUELLE, droit11.getRegime());

				final Set<RaisonAcquisitionRF> raisons11 = droit11.getRaisonsAcquisition();
				assertEquals(1, raisons11.size());
				assertRaisonAcquisition(RegDate.get(2004, 2, 7), "Appropriation illégitime", new IdentifiantAffaireRF(13, 2007, 173, 0), raisons11.iterator().next());

				// un nouveau droit a été créé
				final DroitProprietePersonnePhysiqueRF droit12 = (DroitProprietePersonnePhysiqueRF) droits1.get(1);
				assertNotNull(droit12);
				assertEquals("1f109152381009be0138100c87276e68", droit12.getMasterIdRF());
				assertEquals(dateSecondImport, droit12.getDateDebut());
				assertNull(droit12.getDateFin());
				assertEquals("Donation", droit12.getMotifDebut());
				assertEquals(RegDate.get(2007, 2, 7), droit12.getDateDebutMetier());
				assertEquals("_1f109152381009be0138100ba7e31031", droit12.getImmeuble().getIdRF());
				assertEquals(new Fraction(1, 1), droit12.getPart());
				assertEquals(GenrePropriete.INDIVIDUELLE, droit12.getRegime());

				final Set<RaisonAcquisitionRF> raisons12 = droit12.getRaisonsAcquisition();
				assertEquals(1, raisons12.size());
				assertRaisonAcquisition(RegDate.get(2007, 2, 7), "Donation", new IdentifiantAffaireRF(13, 2007, 173, 0), raisons12.iterator().next());

				final AyantDroitRF pp2 = ayantDroitRFDAO.get(ids.pp2);
				assertNotNull(pp2);

				final List<DroitProprieteRF> droits2 = new ArrayList<>(pp2.getDroitsPropriete());
				assertEquals(1, droits2.size());

				// le droit n'a pas changé
				final DroitProprietePersonnePhysiqueRF droit21 = (DroitProprietePersonnePhysiqueRF) droits2.get(0);
				assertNotNull(droit21);
				assertEquals("8af806fa4a4dd302014b16fc17266a0b", droit21.getMasterIdRF());
				assertEquals(dateImportInitial, droit21.getDateDebut());
				assertNull(droit21.getDateFin());
				assertEquals("Achat", droit21.getMotifDebut());
				assertEquals(RegDate.get(2014, 12, 23), droit21.getDateDebutMetier());
				assertEquals("_8af806fc4a35927c014ae2a6e76041b8", droit21.getImmeuble().getIdRF());
				assertEquals(new Fraction(1, 2), droit21.getPart());
				assertEquals(GenrePropriete.COPROPRIETE, droit21.getRegime());

				final List<RaisonAcquisitionRF> raisons21 = new ArrayList<>(droit21.getRaisonsAcquisition());
				raisons21.sort(Comparator.naturalOrder());
				assertEquals(1, raisons21.size());
				assertRaisonAcquisition(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0), raisons21.get(0));
			}
		});
	}

	/**
	 * Ce test vérifie que les mutations de type SUPPRESSION ferment bien les droits existants
	 */
	@Test
	public void testTraiterMutationsSuppression() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		class Ids {
			long pp1;
			long pp2;
		}
		final Ids ids = new Ids();

		// on insère les données de l'import initial dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
				pp1.setIdRF("_1f109152381009be0138100a1d442eee");
				pp1.setPrenom("Alodie");
				pp1.setNom("Schulz");
				pp1.setDateNaissance(RegDate.get(1970, 1, 1));
				pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);
				ids.pp1 = pp1.getId();

				PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
				pp2.setIdRF("_22222222222222222222222222222222");
				pp2.setPrenom("Jocker");
				pp2.setNom("Bad");
				pp2.setDateNaissance(RegDate.get(1970, 1, 1));
				pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);
				ids.pp2 = pp2.getId();

				BienFondsRF immeuble1 = new BienFondsRF();
				immeuble1.setIdRF("_1f109152381009be0138100ba7e31031");
				immeuble1 = (BienFondsRF) immeubleRFDAO.save(immeuble1);

				final DroitProprietePersonnePhysiqueRF droit1 = new DroitProprietePersonnePhysiqueRF();
				droit1.setMasterIdRF("1f109152381009be0138100c87000000");
				droit1.setVersionIdRF("1f109152381009be0138100c87000001");
				droit1.setDateDebut(dateImportInitial);
				droit1.setDateDebutMetier(RegDate.get(2007, 2, 7));
				droit1.setMotifDebut("Donation");
				droit1.setAyantDroit(pp1);
				droit1.setImmeuble(immeuble1);
				droit1.setPart(new Fraction(1, 1));
				droit1.setRegime(GenrePropriete.INDIVIDUELLE);
				droit1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2007, 2, 7), "Donation", new IdentifiantAffaireRF(13, 2007, 173, 0)));
				droitRFDAO.save(droit1);

				BienFondsRF immeuble2 = new BienFondsRF();
				immeuble2.setIdRF("_8af806fc4a35927c014ae2a6e76041b8");
				immeuble2 = (BienFondsRF) immeubleRFDAO.save(immeuble2);

				final DroitProprietePersonnePhysiqueRF droit2 = new DroitProprietePersonnePhysiqueRF();
				droit2.setMasterIdRF("8af806fa4a4dd302014b16fc17266a0b");
				droit2.setVersionIdRF("8af806fa4a4dd302014b16fc17266a0a");
				droit2.setDateDebut(dateImportInitial);
				droit2.setDateDebutMetier(RegDate.get(2003, 1, 1));
				droit2.setMotifDebut("Succession");
				droit2.setAyantDroit(pp2);
				droit2.setImmeuble(immeuble2);
				droit2.setPart(new Fraction(1, 2));
				droit2.setRegime(GenrePropriete.COPROPRIETE);
				droit2.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0)));
				droit2.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0)));
				droitRFDAO.save(droit2);
			}
		});

		// on insère les données du second import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setType(TypeImportRF.PRINCIPAL);
				importEvent.setDateEvenement(dateSecondImport);
				importEvent.setEtat(EtatEvenementRF.TRAITE);
				importEvent.setFileUrl("http://turlututu");
				importEvent = evenementRFImportDAO.save(importEvent);

				// des mutations de types suppression, c'est-à-dire que l'immeuble n'est plus possédé
				final EvenementRFMutation mut0 = new EvenementRFMutation();
				mut0.setParentImport(importEvent);
				mut0.setEtat(EtatEvenementRF.A_TRAITER);
				mut0.setTypeEntite(TypeEntiteRF.DROIT);
				mut0.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mut0.setIdRF("_1f109152381009be0138100ba7e31031");
				mut0.setXmlContent(null);
				evenementRFMutationDAO.save(mut0);

				final EvenementRFMutation mut1 = new EvenementRFMutation();
				mut1.setParentImport(importEvent);
				mut1.setEtat(EtatEvenementRF.A_TRAITER);
				mut1.setTypeEntite(TypeEntiteRF.DROIT);
				mut1.setTypeMutation(TypeMutationRF.SUPPRESSION);
				mut1.setIdRF("_8af806fc4a35927c014ae2a6e76041b8");
				mut1.setXmlContent(null);
				evenementRFMutationDAO.save(mut1);
				return importEvent.getId();
			}
		});
		assertNotNull(importId);

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
		params.put(TraiterMutationsRFJob.ID, importId);
		params.put(TraiterMutationsRFJob.NB_THREADS, 2);
		params.put(TraiterMutationsRFJob.CONTINUE_WITH_IDENTIFICATION_JOB, Boolean.FALSE);

		final JobDefinition job = batchScheduler.startJob(TraiterMutationsRFJob.NAME, params);
		assertNotNull(job);

		// le job doit se terminer correctement
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());

		// on vérifie que les mutations ont bien été traitées
		assertEtatMutations(2, EtatEvenementRF.TRAITE);

		// on vérifie que les droits ont bien été fermés
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final AyantDroitRF pp1 = ayantDroitRFDAO.get(ids.pp1);
				assertNotNull(pp1);

				final List<DroitProprieteRF> droits1 = new ArrayList<>(pp1.getDroitsPropriete());
				assertEquals(1, droits1.size());

				// l'ancien droit a été fermé
				final DroitProprietePersonnePhysiqueRF droit11 = (DroitProprietePersonnePhysiqueRF) droits1.get(0);
				assertNotNull(droit11);
				assertEquals("1f109152381009be0138100c87000000", droit11.getMasterIdRF());
				assertEquals(dateImportInitial, droit11.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), droit11.getDateFin());
				assertEquals("Donation", droit11.getMotifDebut());
				assertEquals(RegDate.get(2007, 2, 7), droit11.getDateDebutMetier());
				assertEquals("_1f109152381009be0138100ba7e31031", droit11.getImmeuble().getIdRF());
				assertEquals(new Fraction(1, 1), droit11.getPart());
				assertEquals(GenrePropriete.INDIVIDUELLE, droit11.getRegime());

				final Set<RaisonAcquisitionRF> raisons11 = droit11.getRaisonsAcquisition();
				assertEquals(1, raisons11.size());
				assertRaisonAcquisition(RegDate.get(2007, 2, 7), "Donation", new IdentifiantAffaireRF(13, 2007, 173, 0), raisons11.iterator().next());

				final AyantDroitRF pp2 = ayantDroitRFDAO.get(ids.pp2);
				assertNotNull(pp2);

				final List<DroitProprieteRF> droits2 = new ArrayList<>(pp2.getDroitsPropriete());
				assertEquals(1, droits2.size());

				// le droit n'a pas changé
				final DroitProprietePersonnePhysiqueRF droit21 = (DroitProprietePersonnePhysiqueRF) droits2.get(0);
				assertNotNull(droit21);
				assertEquals("8af806fa4a4dd302014b16fc17266a0b", droit21.getMasterIdRF());
				assertEquals(dateImportInitial, droit21.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), droit21.getDateFin());
				assertEquals("Succession", droit21.getMotifDebut());
				assertEquals(RegDate.get(2003, 1, 1), droit21.getDateDebutMetier());
				assertEquals("_8af806fc4a35927c014ae2a6e76041b8", droit21.getImmeuble().getIdRF());
				assertEquals(new Fraction(1, 2), droit21.getPart());
				assertEquals(GenrePropriete.COPROPRIETE, droit21.getRegime());

				final List<RaisonAcquisitionRF> raisons21 = new ArrayList<>(droit21.getRaisonsAcquisition());
				raisons21.sort(Comparator.naturalOrder());
				assertEquals(2, raisons21.size());
				assertRaisonAcquisition(RegDate.get(2003, 1, 1), "Succession", new IdentifiantAffaireRF(6, 2003, 9593, 0), raisons21.get(0));
				assertRaisonAcquisition(RegDate.get(2014, 12, 23), "Achat", new IdentifiantAffaireRF(6, 2014, 9593, 0), raisons21.get(1));
			}
		});
	}
}