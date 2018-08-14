package ch.vd.unireg.registrefoncier.dataimport;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.technical.esb.store.raft.ZipRaftEsbStore;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImport;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeImportRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.IdentifiantDroitRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.UsufruitRF;
import ch.vd.unireg.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.CommuneRFDAO;
import ch.vd.unireg.registrefoncier.dao.DroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.scheduler.BatchScheduler;
import ch.vd.unireg.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("Duplicates")
public class TraiterImportRFServitudesJobTest extends ImportRFTestClass {

	private BatchScheduler batchScheduler;
	private DroitRFDAO droitRFDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private ZipRaftEsbStore zipRaftEsbStore;
	private CommuneRFDAO communeRFDAO;

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
		communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
	}

	/**
	 * [SIFISC-24647] Ce test vérifie que le job d'import des servitudes ne démarre pas si l'import principal correspondant n'existe pas.
	 */
	@Test
	public void testImportServitudeImportPrincipalInexistant() throws Exception {

		// on insère les données de l'import dans la base
		final RegDate dateEvenement = RegDate.get(2016, 9, 1);
		final long servitudes = insertImport(TypeImportRF.SERVITUDES, dateEvenement, EtatEvenementRF.A_TRAITER, "http://turlututu");

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
		params.put(TraiterImportRFJob.ID, servitudes);
		params.put(TraiterImportRFJob.NB_THREADS, 2);
		params.put(TraiterImportRFJob.CONTINUE_WITH_MUTATIONS_JOB, false);

		final JobDefinition job = batchScheduler.startJob(TraiterImportRFJob.NAME, params);
		assertNotNull(job);

		// le job ne doit pas démarrer
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_EXCEPTION, job.getStatut());

		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = evenementRFImportDAO.get(servitudes);
				assertNotNull(importEvent);
				assertEquals(EtatEvenementRF.A_TRAITER, importEvent.getEtat());
				assertEquals("L'import des servitudes RF avec la date valeur = [01.09.2016] ne peut pas être traité car il n'y a pas d'import principal RF à la même date.", importEvent.getErrorMessage());
				assertTrue(importEvent.getCallstack().contains("L'import des servitudes RF avec la date valeur = [01.09.2016] ne peut pas être traité car il n'y a pas d'import principal RF à la même date."));
			}
		});
	}

	/**
	 * [SIFISC-24647] Ce test vérifie que le job d'import des servitudes ne démarre pas si l'import principal correspondant n'est pas traité
	 */
	@Test
	public void testImportServitudeImportPrincipalPasTraite() throws Exception {

		// on insère les données de l'import dans la base
		final RegDate dateEvenement = RegDate.get(2016, 9, 1);
		insertImport(TypeImportRF.PRINCIPAL, dateEvenement, EtatEvenementRF.A_TRAITER, "http://turlututu");
		final long servitudes = insertImport(TypeImportRF.SERVITUDES, dateEvenement, EtatEvenementRF.A_TRAITER, "http://turlututu");

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
		params.put(TraiterImportRFJob.ID, servitudes);
		params.put(TraiterImportRFJob.NB_THREADS, 2);
		params.put(TraiterImportRFJob.CONTINUE_WITH_MUTATIONS_JOB, false);

		final JobDefinition job = batchScheduler.startJob(TraiterImportRFJob.NAME, params);
		assertNotNull(job);

		// le job ne doit pas démarrer
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_EXCEPTION, job.getStatut());

		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = evenementRFImportDAO.get(servitudes);
				assertNotNull(importEvent);
				assertEquals(EtatEvenementRF.A_TRAITER, importEvent.getEtat());
				assertEquals("L'import des servitudes RF avec la date valeur = [01.09.2016] ne peut pas être traité car l'import principal RF à la même date n'est pas traité.", importEvent.getErrorMessage());
				assertTrue(importEvent.getCallstack().contains("L'import des servitudes RF avec la date valeur = [01.09.2016] ne peut pas être traité car l'import principal RF à la même date n'est pas traité."));
			}
		});
	}

	/**
	 * [SIFISC-24647] Ce test vérifie que le job d'import des servitudes ne démarre pas si l'import principal est traité mais que toutes les mutations correspondantes ne sont pas traitées.
	 */
	@Test
	public void testImportServitudeImportPrincipalMutationsPasTraitees() throws Exception {

		// on insère les données de l'import dans la base
		final RegDate dateEvenement = RegDate.get(2016, 9, 1);
		final Long principal = insertImport(TypeImportRF.PRINCIPAL, dateEvenement, EtatEvenementRF.TRAITE, "http://turlututu");
		doInNewTransaction(status -> {
			final EvenementRFImport parentImport = evenementRFImportDAO.get(principal);

			// une mutation dans l'état "à traiter"
			final EvenementRFMutation mut0 = new EvenementRFMutation();
			mut0.setEtat(EtatEvenementRF.A_TRAITER);
			mut0.setIdRF("238382389");
			mut0.setParentImport(parentImport);
			mut0.setTypeEntite(TypeEntiteRF.IMMEUBLE);
			mut0.setTypeMutation(TypeMutationRF.CREATION);
			mut0.setXmlContent("");
			evenementRFMutationDAO.save(mut0);

			// une mutation dans l'état "en erreur"
			final EvenementRFMutation mut1 = new EvenementRFMutation();
			mut1.setEtat(EtatEvenementRF.EN_ERREUR);
			mut1.setIdRF("484848");
			mut1.setParentImport(parentImport);
			mut1.setTypeEntite(TypeEntiteRF.IMMEUBLE);
			mut1.setTypeMutation(TypeMutationRF.MODIFICATION);
			mut1.setXmlContent("");
			evenementRFMutationDAO.save(mut1);

			return null;
		});
		final long servitudes = insertImport(TypeImportRF.SERVITUDES, dateEvenement, EtatEvenementRF.A_TRAITER, "http://turlututu");

		// on déclenche le démarrage du job
		final Map<String, Object> params = new HashMap<>();
		params.put(TraiterImportRFJob.ID, servitudes);
		params.put(TraiterImportRFJob.NB_THREADS, 2);
		params.put(TraiterImportRFJob.CONTINUE_WITH_MUTATIONS_JOB, false);

		final JobDefinition job = batchScheduler.startJob(TraiterImportRFJob.NAME, params);
		assertNotNull(job);

		// le job ne doit pas démarrer
		waitForJobCompletion(job);
		assertEquals(JobDefinition.JobStatut.JOB_EXCEPTION, job.getStatut());

		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = evenementRFImportDAO.get(servitudes);
				assertNotNull(importEvent);
				assertEquals(EtatEvenementRF.A_TRAITER, importEvent.getEtat());
				assertEquals("L'import des servitudes RF avec la date valeur = [01.09.2016] ne peut pas être traité car il y a encore 2 mutations à traiter sur l'import principal RF à la même date.", importEvent.getErrorMessage());
				assertTrue(importEvent.getCallstack().contains("L'import des servitudes RF avec la date valeur = [01.09.2016] ne peut pas être traité car il y a encore 2 mutations à traiter sur l'import principal RF à la même date."));
			}
		});
	}

	/**
	 * Ce test vérifie que les mutations sont bien créées lorsqu'on importe un fichier RF de servitudes sur une base vide
	 */
	@Test
	public void testImportBaseVide() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/export_servitudes_rf.xml");
		assertNotNull(importFile);

		// on l'upload dans Raft
		final String raftUrl;
		try (FileInputStream is = new FileInputStream(importFile)) {
			raftUrl = zipRaftEsbStore.store("Fiscalite", "UnitTest", "Unireg", is);
		}
		assertNotNull(raftUrl);

		// l'import principal préalablement traité (précondition pour l'exécution de l'import des servitudes)
		insertImport(TypeImportRF.PRINCIPAL, RegDate.get(2016, 10, 1), EtatEvenementRF.TRAITE, "http://turlututu");

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setType(TypeImportRF.SERVITUDES);
				importEvent.setDateEvenement(RegDate.get(2016, 10, 1));
				importEvent.setEtat(EtatEvenementRF.A_TRAITER);
				importEvent.setFileUrl(raftUrl);
				return evenementRFImportDAO.save(importEvent).getId();
			}
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
				assertEquals(5, mutations.size());    // il y a 2 usufruits + 3 personnes physiques
				mutations.sort(new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(TypeEntiteRF.AYANT_DROIT, mut0.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffda5511644", mut0.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<NatuerlichePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <PersonstammID>_1f109152380ffd8901380ffda5511644</PersonstammID>\n" +
						             "    <Name>Lassueur</Name>\n" +
						             "    <Gueltig>true</Gueltig>\n" +
						             "    <ClientRegulier>false</ClientRegulier>\n" +
						             "    <NoSCC>0</NoSCC>\n" +
						             "    <Status>definitiv</Status>\n" +
						             "    <Sprache>\n" +
						             "        <TextDe>Französisch</TextDe>\n" +
						             "        <TextFr>Français</TextFr>\n" +
						             "    </Sprache>\n" +
						             "    <Anrede>\n" +
						             "        <TextDe>*Monsieur</TextDe>\n" +
						             "        <TextFr>Monsieur</TextFr>\n" +
						             "    </Anrede>\n" +
						             "    <NrACI>0</NrACI>\n" +
						             "    <Adressen>\n" +
						             "        <Strasse>Rue des Sauges 22</Strasse>\n" +
						             "        <PLZ>1347</PLZ>\n" +
						             "        <Ort>Le Sentier</Ort>\n" +
						             "        <Rolle>ACI</Rolle>\n" +
						             "    </Adressen>\n" +
						             "    <Vorname>Jean-Claude</Vorname>\n" +
						             "    <Zivilstand>unbekannt</Zivilstand>\n" +
						             "    <Geburtsdatum>\n" +
						             "        <Tag>27</Tag>\n" +
						             "        <Monat>6</Monat>\n" +
						             "        <Jahr>1941</Jahr>\n" +
						             "    </Geburtsdatum>\n" +
						             "    <NameDerEltern>René</NameDerEltern>\n" +
						             "    <Geschlecht>unbekannt</Geschlecht>\n" +
						             "    <WeitereVornamen>René</WeitereVornamen>\n" +
						             "    <NrIROLE>10385019</NrIROLE>\n" +
						             "</NatuerlichePersonstamm>\n", mut0.getXmlContent());

				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(TypeEntiteRF.AYANT_DROIT, mut1.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffda8131c65", mut1.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<NatuerlichePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <PersonstammID>_1f109152380ffd8901380ffda8131c65</PersonstammID>\n" +
						             "    <Name>Lassueur</Name>\n" +
						             "    <Gueltig>true</Gueltig>\n" +
						             "    <ClientRegulier>false</ClientRegulier>\n" +
						             "    <NoSCC>0</NoSCC>\n" +
						             "    <Status>definitiv</Status>\n" +
						             "    <Sprache>\n" +
						             "        <TextDe>Französisch</TextDe>\n" +
						             "        <TextFr>Français</TextFr>\n" +
						             "    </Sprache>\n" +
						             "    <Anrede>\n" +
						             "        <TextDe>*Madame</TextDe>\n" +
						             "        <TextFr>Madame</TextFr>\n" +
						             "    </Anrede>\n" +
						             "    <NrACI>0</NrACI>\n" +
						             "    <Adressen>\n" +
						             "        <Strasse>Rue des Sauges 22</Strasse>\n" +
						             "        <PLZ>1347</PLZ>\n" +
						             "        <Ort>Le Sentier</Ort>\n" +
						             "        <Rolle>ACI</Rolle>\n" +
						             "    </Adressen>\n" +
						             "    <Vorname>Anne-Lise</Vorname>\n" +
						             "    <Zivilstand>unbekannt</Zivilstand>\n" +
						             "    <Geburtsdatum>\n" +
						             "        <Tag>9</Tag>\n" +
						             "        <Monat>3</Monat>\n" +
						             "        <Jahr>1945</Jahr>\n" +
						             "    </Geburtsdatum>\n" +
						             "    <LedigName>Audemars</LedigName>\n" +
						             "    <NameDerEltern>Louis</NameDerEltern>\n" +
						             "    <Geschlecht>unbekannt</Geschlecht>\n" +
						             "    <NameEhegatte>Jean-Claude</NameEhegatte>\n" +
						             "    <NrIROLE>10385020</NrIROLE>\n" +
						             "</NatuerlichePersonstamm>\n", mut1.getXmlContent());

				final EvenementRFMutation mut2 = mutations.get(2);
				assertEquals(importId, mut2.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
				assertEquals(TypeEntiteRF.AYANT_DROIT, mut2.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut2.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffdabcc2441", mut2.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<NatuerlichePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <PersonstammID>_1f109152380ffd8901380ffdabcc2441</PersonstammID>\n" +
						             "    <Name>Gaillard</Name>\n" +
						             "    <Gueltig>true</Gueltig>\n" +
						             "    <ClientRegulier>false</ClientRegulier>\n" +
						             "    <NoSCC>0</NoSCC>\n" +
						             "    <Status>definitiv</Status>\n" +
						             "    <Sprache>\n" +
						             "        <TextDe>Französisch</TextDe>\n" +
						             "        <TextFr>Français</TextFr>\n" +
						             "    </Sprache>\n" +
						             "    <Anrede>\n" +
						             "        <TextDe>*Monsieur</TextDe>\n" +
						             "        <TextFr>Monsieur</TextFr>\n" +
						             "    </Anrede>\n" +
						             "    <NrACI>0</NrACI>\n" +
						             "    <Adressen>\n" +
						             "        <Strasse>Le Charroux 1</Strasse>\n" +
						             "        <PLZ>1345</PLZ>\n" +
						             "        <Ort>Le Lieu</Ort>\n" +
						             "        <Rolle>ACI</Rolle>\n" +
						             "    </Adressen>\n" +
						             "    <Vorname>Roger</Vorname>\n" +
						             "    <Zivilstand>unbekannt</Zivilstand>\n" +
						             "    <Geburtsdatum>\n" +
						             "        <Tag>2</Tag>\n" +
						             "        <Monat>2</Monat>\n" +
						             "        <Jahr>1938</Jahr>\n" +
						             "    </Geburtsdatum>\n" +
						             "    <NameDerEltern>Albert</NameDerEltern>\n" +
						             "    <Geschlecht>unbekannt</Geschlecht>\n" +
						             "    <WeitereVornamen>Albert</WeitereVornamen>\n" +
						             "    <NrIROLE>10386724</NrIROLE>\n" +
						             "</NatuerlichePersonstamm>\n", mut2.getXmlContent());

				final EvenementRFMutation mut3 = mutations.get(3);
				assertEquals(importId, mut3.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut3.getEtat());
				assertEquals(TypeEntiteRF.SERVITUDE, mut3.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut3.getTypeMutation());
				assertEquals("1f109152380ffd8901380ffed6694392", mut3.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<DienstbarkeitExtended xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <Dienstbarkeit VersionID=\"1f109152380ffd8901380ffed66943a2\" MasterID=\"1f109152380ffd8901380ffed6694392\">\n" +
						             "        <StandardRechtID>_1f109152380ffd8901380ffed6694392</StandardRechtID>\n" +
						             "        <BeteiligtesGrundstueckIDREF>_1f109152380ffd8901380ffe15bb729c</BeteiligtesGrundstueckIDREF>\n" +
						             "        <RechtEintragJahrID>2005</RechtEintragJahrID>\n" +
						             "        <RechtEintragNummerID>699</RechtEintragNummerID>\n" +
						             "        <Bereinigungsmarkierung>false</Bereinigungsmarkierung>\n" +
						             "        <AmtNummer>8</AmtNummer>\n" +
						             "        <Stichwort>\n" +
						             "            <TextDe>*Usufruit</TextDe>\n" +
						             "            <TextFr>Usufruit</TextFr>\n" +
						             "        </Stichwort>\n" +
						             "        <Rechtzusatz>conventionnel</Rechtzusatz>\n" +
						             "        <BelegAlt>2002/392</BelegAlt>\n" +
						             "        <BeginDatum>2002-09-02</BeginDatum>\n" +
						             "        <Entschaedigung>0</Entschaedigung>\n" +
						             "        <Wert>0</Wert>\n" +
						             "        <Meldungspflichtig>gem_code</Meldungspflichtig>\n" +
						             "        <Personenberechtigt>true</Personenberechtigt>\n" +
						             "        <Grundstueckeberechtigt>false</Grundstueckeberechtigt>\n" +
						             "        <EintragungAlsSdR>false</EintragungAlsSdR>\n" +
						             "    </Dienstbarkeit>\n" +
						             "    <LastRechtGruppe VersionID=\"1f109152380ffd8901380ffed66a43c1\">\n" +
						             "        <StandardRechtIDREF>_1f109152380ffd8901380ffed6694392</StandardRechtIDREF>\n" +
						             "        <BelastetesGrundstueck VersionID=\"1f109152380ffd8901380ffed672445a\">\n" +
						             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe15bb729c</BelastetesGrundstueckIDREF>\n" +
						             "        </BelastetesGrundstueck>\n" +
						             "        <BerechtigtePerson VersionID=\"1f109152380ffd8901380ffed66d4417\">\n" +
						             "            <NatuerlichePersonGb VersionID=\"1f109152380ffd8901380ffe24b81b93\" MasterID=\"1f109152380ffd8901380ffe24b31b61\">\n" +
						             "                <Name>Gaillard</Name>\n" +
						             "                <Status>definitiv</Status>\n" +
						             "                <Vorname>Roger</Vorname>\n" +
						             "                <Geburtsdatum>\n" +
						             "                    <Tag>2</Tag>\n" +
						             "                    <Monat>2</Monat>\n" +
						             "                    <Jahr>1938</Jahr>\n" +
						             "                </Geburtsdatum>\n" +
						             "                <Zivilstand>unbekannt</Zivilstand>\n" +
						             "                <NameEltern>Albert</NameEltern>\n" +
						             "                <WeitereVornamen>Albert</WeitereVornamen>\n" +
						             "                <PersonstammIDREF>_1f109152380ffd8901380ffdabcc2441</PersonstammIDREF>\n" +
						             "            </NatuerlichePersonGb>\n" +
						             "        </BerechtigtePerson>\n" +
						             "    </LastRechtGruppe>\n" +
						             "</DienstbarkeitExtended>\n", mut3.getXmlContent());

				final EvenementRFMutation mut4 = mutations.get(4);
				assertEquals(importId, mut4.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut4.getEtat());
				assertEquals(TypeEntiteRF.SERVITUDE, mut4.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut4.getTypeMutation());
				assertEquals("1f109152380ffd8901380ffefad54360", mut4.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<DienstbarkeitExtended xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <Dienstbarkeit VersionID=\"1f109152380ffd8901380ffefad64374\" MasterID=\"1f109152380ffd8901380ffefad54360\">\n" +
						             "        <StandardRechtID>_1f109152380ffd8901380ffefad54360</StandardRechtID>\n" +
						             "        <BeteiligtesGrundstueckIDREF>_1f109152380ffd8901380ffe090827e1</BeteiligtesGrundstueckIDREF>\n" +
						             "        <RechtEintragJahrID>2006</RechtEintragJahrID>\n" +
						             "        <RechtEintragNummerID>361</RechtEintragNummerID>\n" +
						             "        <Bereinigungsmarkierung>false</Bereinigungsmarkierung>\n" +
						             "        <AmtNummer>8</AmtNummer>\n" +
						             "        <Stichwort>\n" +
						             "            <TextDe>*Usufruit</TextDe>\n" +
						             "            <TextFr>Usufruit</TextFr>\n" +
						             "        </Stichwort>\n" +
						             "        <Rechtzusatz>conventionnel</Rechtzusatz>\n" +
						             "        <Beleg>\n" +
						             "            <AmtNummer>8</AmtNummer>\n" +
						             "            <BelegJahr>2006</BelegJahr>\n" +
						             "            <BelegNummer>285</BelegNummer>\n" +
						             "            <BelegNummerIndex>0</BelegNummerIndex>\n" +
						             "        </Beleg>\n" +
						             "        <BeginDatum>2006-06-30</BeginDatum>\n" +
						             "        <Entschaedigung>0</Entschaedigung>\n" +
						             "        <Wert>0</Wert>\n" +
						             "        <Meldungspflichtig>gem_code</Meldungspflichtig>\n" +
						             "        <Personenberechtigt>true</Personenberechtigt>\n" +
						             "        <Grundstueckeberechtigt>false</Grundstueckeberechtigt>\n" +
						             "        <EintragungAlsSdR>false</EintragungAlsSdR>\n" +
						             "    </Dienstbarkeit>\n" +
						             "    <LastRechtGruppe VersionID=\"1f109152380ffd8901380ffefad743bc\">\n" +
						             "        <StandardRechtIDREF>_1f109152380ffd8901380ffefad54360</StandardRechtIDREF>\n" +
						             "        <BelastetesGrundstueck VersionID=\"1f109152380ffd8901380ffefadb441c\">\n" +
						             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe090827e1</BelastetesGrundstueckIDREF>\n" +
						             "        </BelastetesGrundstueck>\n" +
						             "        <BerechtigtePerson VersionID=\"1f109152380ffd8901380ffefada43f6\">\n" +
						             "            <NatuerlichePersonGb VersionID=\"1f109152380ffd8901380ffe25b22067\" MasterID=\"1f109152380ffd8901380ffe25ae2003\">\n" +
						             "                <Name>Lassueur</Name>\n" +
						             "                <Status>definitiv</Status>\n" +
						             "                <Vorname>Anne-Lise</Vorname>\n" +
						             "                <Ledigname>Audemars</Ledigname>\n" +
						             "                <Geburtsdatum>\n" +
						             "                    <Tag>9</Tag>\n" +
						             "                    <Monat>3</Monat>\n" +
						             "                    <Jahr>1945</Jahr>\n" +
						             "                </Geburtsdatum>\n" +
						             "                <Zivilstand>unbekannt</Zivilstand>\n" +
						             "                <NameEltern>Louis</NameEltern>\n" +
						             "                <NameEhegatte>Jean-Claude</NameEhegatte>\n" +
						             "                <PersonstammIDREF>_1f109152380ffd8901380ffda8131c65</PersonstammIDREF>\n" +
						             "            </NatuerlichePersonGb>\n" +
						             "        </BerechtigtePerson>\n" +
						             "        <BerechtigtePerson VersionID=\"1f109152380ffd8901380ffefada43f7\">\n" +
						             "            <NatuerlichePersonGb VersionID=\"1f109152380ffd8901380ffe25b22068\" MasterID=\"1f109152380ffd8901380ffe25ae2004\">\n" +
						             "                <Name>Lassueur</Name>\n" +
						             "                <Status>definitiv</Status>\n" +
						             "                <Vorname>Jean-Claude</Vorname>\n" +
						             "                <Geburtsdatum>\n" +
						             "                    <Tag>27</Tag>\n" +
						             "                    <Monat>6</Monat>\n" +
						             "                    <Jahr>1941</Jahr>\n" +
						             "                </Geburtsdatum>\n" +
						             "                <Zivilstand>unbekannt</Zivilstand>\n" +
						             "                <NameEltern>René</NameEltern>\n" +
						             "                <WeitereVornamen>René</WeitereVornamen>\n" +
						             "                <PersonstammIDREF>_1f109152380ffd8901380ffda5511644</PersonstammIDREF>\n" +
						             "            </NatuerlichePersonGb>\n" +
						             "        </BerechtigtePerson>\n" +
						             "    </LastRechtGruppe>\n" +
						             "</DienstbarkeitExtended>\n", mut4.getXmlContent());
			}
		});

	}

	/**
	 * Ce test vérifie qu'aucune mutation n'est créées lorsqu'on importe un fichier RF et que les surfaces au sol dans la base sont déjà à jour.
	 */
	@Test
	public void testImportBaseDejaAJour() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2008, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2010, 1, 1);

		// l'import principal préalablement traité (précondition pour l'exécution de l'import des servitudes)
		insertImport(TypeImportRF.PRINCIPAL, dateSecondImport, EtatEvenementRF.TRAITE, "http://turlututu");

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/export_servitudes_rf.xml");
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
				importEvent.setType(TypeImportRF.SERVITUDES);
				importEvent.setDateEvenement(dateSecondImport);
				importEvent.setEtat(EtatEvenementRF.A_TRAITER);
				importEvent.setFileUrl(raftUrl);
				return evenementRFImportDAO.save(importEvent).getId();
			}
		});
		assertNotNull(importId);

		// on insère les données des immeubles et des servitudes dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				// données équivalentes au fichier export_servitudes_rf.xml
				BienFondsRF bienFonds1 = new BienFondsRF();
				bienFonds1.setIdRF("_1f109152380ffd8901380ffe15bb729c");
				bienFonds1 = (BienFondsRF) immeubleRFDAO.save(bienFonds1);

				BienFondsRF bienFonds2 = new BienFondsRF();
				bienFonds2.setIdRF("_1f109152380ffd8901380ffe090827e1");
				bienFonds2 = (BienFondsRF) immeubleRFDAO.save(bienFonds2);

				PersonnePhysiqueRF pp1 = newPersonnePhysique("_1f109152380ffd8901380ffdabcc2441", 0L, 10386724L, "Gaillard", "Roger", RegDate.get(1938, 2, 2));
				pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

				PersonnePhysiqueRF pp2 = newPersonnePhysique("_1f109152380ffd8901380ffda8131c65", 0L, 10385020L, "Lassueur", "Anne-Lise", RegDate.get(1945, 3, 9));
				pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

				PersonnePhysiqueRF pp3 = newPersonnePhysique("_1f109152380ffd8901380ffda5511644", 0L, 10385019L, "Lassueur", "Jean-Claude", RegDate.get(1941, 6, 27));
				pp3 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp3);

				// les servitudes
				UsufruitRF usu1 = newUsufruitRF("1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2", bienFonds1, pp1, dateImportInitial, RegDate.get(2002, 9, 2), null, null,
				                                new IdentifiantDroitRF(8, 2005, 699), new IdentifiantAffaireRF(8, 2002, 392, null));
				UsufruitRF usu2 = newUsufruitRF("1f109152380ffd8901380ffefad54360", "1f109152380ffd8901380ffefad64374", Collections.singletonList(bienFonds2), Arrays.asList(pp2, pp3), dateImportInitial, RegDate.get(2006, 6, 30), null, null,
				                                new IdentifiantDroitRF(8, 2006, 361), new IdentifiantAffaireRF(8, 2006, 285, 0));
				droitRFDAO.save(usu1);
				droitRFDAO.save(usu2);
			}
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
			assertEquals(0, mutations.size());
			return null;
		});
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsqu'on importe un fichier RF et que les servitudes dans la base sont différentes.
	 */
	@Test
	public void testImportAvecModifications() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/export_servitudes_rf.xml");
		assertNotNull(importFile);

		// on l'upload dans Raft
		final String raftUrl;
		try (FileInputStream is = new FileInputStream(importFile)) {
			raftUrl = zipRaftEsbStore.store("Fiscalite", "UnitTest", "Unireg", is);
		}
		assertNotNull(raftUrl);

		final RegDate dateImportInitial = RegDate.get(2010, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// l'import principal préalablement traité (précondition pour l'exécution de l'import des servitudes)
		insertImport(TypeImportRF.PRINCIPAL, dateSecondImport, EtatEvenementRF.TRAITE, "http://turlututu");

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setType(TypeImportRF.SERVITUDES);
				importEvent.setDateEvenement(dateSecondImport);
				importEvent.setEtat(EtatEvenementRF.A_TRAITER);
				importEvent.setFileUrl(raftUrl);
				return evenementRFImportDAO.save(importEvent).getId();
			}
		});
		assertNotNull(importId);

		// on insère les données des immeubles et des servitudes dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final CommuneRF rances = communeRFDAO.save(newCommuneRF(273, "Rances", 5555));

				// données équivalentes au fichier export_servitudes_rf.xml
				BienFondsRF bienFonds1 = new BienFondsRF();
				bienFonds1.setIdRF("_1f109152380ffd8901380ffe15bb729c");
				bienFonds1 = (BienFondsRF) immeubleRFDAO.save(bienFonds1);

				BienFondsRF bienFonds2 = new BienFondsRF();
				bienFonds2.setIdRF("_1f109152380ffd8901380ffe090827e1");
				bienFonds2 = (BienFondsRF) immeubleRFDAO.save(bienFonds2);

				PersonnePhysiqueRF pp1 = newPersonnePhysique("_1f109152380ffd8901380ffdabcc2441", 0L, 10386724L, "Gaillard", "Roger", RegDate.get(1938, 2, 2));
				pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

				PersonnePhysiqueRF pp2 = newPersonnePhysique("_1f109152380ffd8901380ffda8131c65", 0L, 10385020L, "Lassueur", "Anne-Lise", RegDate.get(1945, 3, 9));
				pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

				PersonnePhysiqueRF pp3 = newPersonnePhysique("_1f109152380ffd8901380ffda5511644", 0L, 10385019L, "Lassueur", "Jean-Claude", RegDate.get(1941, 6, 27));
				pp3 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp3);

				// identifiant du droit différent
				UsufruitRF usu1 = newUsufruitRF("1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2", bienFonds1, pp1, dateImportInitial, RegDate.get(2002, 9, 2), null, null,
				                                new IdentifiantDroitRF(8, 2002, 699), new IdentifiantAffaireRF(8, 2002, 392, null));
				// index non-renseigné
				UsufruitRF usu2 = newUsufruitRF("1f109152380ffd8901380ffefad54360", "1f109152380ffd8901380ffefad64374", Collections.singletonList(bienFonds2), Arrays.asList(pp2, pp3), dateImportInitial, RegDate.get(2006, 6, 30),
				                                null, null, new IdentifiantDroitRF(8, 2006, 361), new IdentifiantAffaireRF(8, 2006, 285, null));
				droitRFDAO.save(usu1);
				droitRFDAO.save(usu2);
			}
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
				assertEquals(2, mutations.size());    // les 2 usufruits dans le fichier d'import sont différents
				mutations.sort(new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(TypeEntiteRF.SERVITUDE, mut0.getTypeEntite());
				assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
				assertEquals("1f109152380ffd8901380ffed6694392", mut0.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<DienstbarkeitExtended xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <Dienstbarkeit VersionID=\"1f109152380ffd8901380ffed66943a2\" MasterID=\"1f109152380ffd8901380ffed6694392\">\n" +
						             "        <StandardRechtID>_1f109152380ffd8901380ffed6694392</StandardRechtID>\n" +
						             "        <BeteiligtesGrundstueckIDREF>_1f109152380ffd8901380ffe15bb729c</BeteiligtesGrundstueckIDREF>\n" +
						             "        <RechtEintragJahrID>2005</RechtEintragJahrID>\n" +
						             "        <RechtEintragNummerID>699</RechtEintragNummerID>\n" +
						             "        <Bereinigungsmarkierung>false</Bereinigungsmarkierung>\n" +
						             "        <AmtNummer>8</AmtNummer>\n" +
						             "        <Stichwort>\n" +
						             "            <TextDe>*Usufruit</TextDe>\n" +
						             "            <TextFr>Usufruit</TextFr>\n" +
						             "        </Stichwort>\n" +
						             "        <Rechtzusatz>conventionnel</Rechtzusatz>\n" +
						             "        <BelegAlt>2002/392</BelegAlt>\n" +
						             "        <BeginDatum>2002-09-02</BeginDatum>\n" +
						             "        <Entschaedigung>0</Entschaedigung>\n" +
						             "        <Wert>0</Wert>\n" +
						             "        <Meldungspflichtig>gem_code</Meldungspflichtig>\n" +
						             "        <Personenberechtigt>true</Personenberechtigt>\n" +
						             "        <Grundstueckeberechtigt>false</Grundstueckeberechtigt>\n" +
						             "        <EintragungAlsSdR>false</EintragungAlsSdR>\n" +
						             "    </Dienstbarkeit>\n" +
						             "    <LastRechtGruppe VersionID=\"1f109152380ffd8901380ffed66a43c1\">\n" +
						             "        <StandardRechtIDREF>_1f109152380ffd8901380ffed6694392</StandardRechtIDREF>\n" +
						             "        <BelastetesGrundstueck VersionID=\"1f109152380ffd8901380ffed672445a\">\n" +
						             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe15bb729c</BelastetesGrundstueckIDREF>\n" +
						             "        </BelastetesGrundstueck>\n" +
						             "        <BerechtigtePerson VersionID=\"1f109152380ffd8901380ffed66d4417\">\n" +
						             "            <NatuerlichePersonGb VersionID=\"1f109152380ffd8901380ffe24b81b93\" MasterID=\"1f109152380ffd8901380ffe24b31b61\">\n" +
						             "                <Name>Gaillard</Name>\n" +
						             "                <Status>definitiv</Status>\n" +
						             "                <Vorname>Roger</Vorname>\n" +
						             "                <Geburtsdatum>\n" +
						             "                    <Tag>2</Tag>\n" +
						             "                    <Monat>2</Monat>\n" +
						             "                    <Jahr>1938</Jahr>\n" +
						             "                </Geburtsdatum>\n" +
						             "                <Zivilstand>unbekannt</Zivilstand>\n" +
						             "                <NameEltern>Albert</NameEltern>\n" +
						             "                <WeitereVornamen>Albert</WeitereVornamen>\n" +
						             "                <PersonstammIDREF>_1f109152380ffd8901380ffdabcc2441</PersonstammIDREF>\n" +
						             "            </NatuerlichePersonGb>\n" +
						             "        </BerechtigtePerson>\n" +
						             "    </LastRechtGruppe>\n" +
						             "</DienstbarkeitExtended>\n", mut0.getXmlContent());

				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(TypeEntiteRF.SERVITUDE, mut1.getTypeEntite());
				assertEquals(TypeMutationRF.MODIFICATION, mut1.getTypeMutation());
				assertEquals("1f109152380ffd8901380ffefad54360", mut1.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<DienstbarkeitExtended xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <Dienstbarkeit VersionID=\"1f109152380ffd8901380ffefad64374\" MasterID=\"1f109152380ffd8901380ffefad54360\">\n" +
						             "        <StandardRechtID>_1f109152380ffd8901380ffefad54360</StandardRechtID>\n" +
						             "        <BeteiligtesGrundstueckIDREF>_1f109152380ffd8901380ffe090827e1</BeteiligtesGrundstueckIDREF>\n" +
						             "        <RechtEintragJahrID>2006</RechtEintragJahrID>\n" +
						             "        <RechtEintragNummerID>361</RechtEintragNummerID>\n" +
						             "        <Bereinigungsmarkierung>false</Bereinigungsmarkierung>\n" +
						             "        <AmtNummer>8</AmtNummer>\n" +
						             "        <Stichwort>\n" +
						             "            <TextDe>*Usufruit</TextDe>\n" +
						             "            <TextFr>Usufruit</TextFr>\n" +
						             "        </Stichwort>\n" +
						             "        <Rechtzusatz>conventionnel</Rechtzusatz>\n" +
						             "        <Beleg>\n" +
						             "            <AmtNummer>8</AmtNummer>\n" +
						             "            <BelegJahr>2006</BelegJahr>\n" +
						             "            <BelegNummer>285</BelegNummer>\n" +
						             "            <BelegNummerIndex>0</BelegNummerIndex>\n" +
						             "        </Beleg>\n" +
						             "        <BeginDatum>2006-06-30</BeginDatum>\n" +
						             "        <Entschaedigung>0</Entschaedigung>\n" +
						             "        <Wert>0</Wert>\n" +
						             "        <Meldungspflichtig>gem_code</Meldungspflichtig>\n" +
						             "        <Personenberechtigt>true</Personenberechtigt>\n" +
						             "        <Grundstueckeberechtigt>false</Grundstueckeberechtigt>\n" +
						             "        <EintragungAlsSdR>false</EintragungAlsSdR>\n" +
						             "    </Dienstbarkeit>\n" +
						             "    <LastRechtGruppe VersionID=\"1f109152380ffd8901380ffefad743bc\">\n" +
						             "        <StandardRechtIDREF>_1f109152380ffd8901380ffefad54360</StandardRechtIDREF>\n" +
						             "        <BelastetesGrundstueck VersionID=\"1f109152380ffd8901380ffefadb441c\">\n" +
						             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe090827e1</BelastetesGrundstueckIDREF>\n" +
						             "        </BelastetesGrundstueck>\n" +
						             "        <BerechtigtePerson VersionID=\"1f109152380ffd8901380ffefada43f6\">\n" +
						             "            <NatuerlichePersonGb VersionID=\"1f109152380ffd8901380ffe25b22067\" MasterID=\"1f109152380ffd8901380ffe25ae2003\">\n" +
						             "                <Name>Lassueur</Name>\n" +
						             "                <Status>definitiv</Status>\n" +
						             "                <Vorname>Anne-Lise</Vorname>\n" +
						             "                <Ledigname>Audemars</Ledigname>\n" +
						             "                <Geburtsdatum>\n" +
						             "                    <Tag>9</Tag>\n" +
						             "                    <Monat>3</Monat>\n" +
						             "                    <Jahr>1945</Jahr>\n" +
						             "                </Geburtsdatum>\n" +
						             "                <Zivilstand>unbekannt</Zivilstand>\n" +
						             "                <NameEltern>Louis</NameEltern>\n" +
						             "                <NameEhegatte>Jean-Claude</NameEhegatte>\n" +
						             "                <PersonstammIDREF>_1f109152380ffd8901380ffda8131c65</PersonstammIDREF>\n" +
						             "            </NatuerlichePersonGb>\n" +
						             "        </BerechtigtePerson>\n" +
						             "        <BerechtigtePerson VersionID=\"1f109152380ffd8901380ffefada43f7\">\n" +
						             "            <NatuerlichePersonGb VersionID=\"1f109152380ffd8901380ffe25b22068\" MasterID=\"1f109152380ffd8901380ffe25ae2004\">\n" +
						             "                <Name>Lassueur</Name>\n" +
						             "                <Status>definitiv</Status>\n" +
						             "                <Vorname>Jean-Claude</Vorname>\n" +
						             "                <Geburtsdatum>\n" +
						             "                    <Tag>27</Tag>\n" +
						             "                    <Monat>6</Monat>\n" +
						             "                    <Jahr>1941</Jahr>\n" +
						             "                </Geburtsdatum>\n" +
						             "                <Zivilstand>unbekannt</Zivilstand>\n" +
						             "                <NameEltern>René</NameEltern>\n" +
						             "                <WeitereVornamen>René</WeitereVornamen>\n" +
						             "                <PersonstammIDREF>_1f109152380ffd8901380ffda5511644</PersonstammIDREF>\n" +
						             "            </NatuerlichePersonGb>\n" +
						             "        </BerechtigtePerson>\n" +
						             "    </LastRechtGruppe>\n" +
						             "</DienstbarkeitExtended>\n", mut1.getXmlContent());
			}
		});
	}

	/**
	 * Ce test vérifie que des mutations de suppression sont créées si des propriétaires avec des servitudes dans la DB n'en ont plus dans le fichier d'import.
	 */
	@Test
	public void testImportSuppression() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/export_servitudes_vide_rf.xml");
		assertNotNull(importFile);

		// on l'upload dans Raft
		final String raftUrl;
		try (FileInputStream is = new FileInputStream(importFile)) {
			raftUrl = zipRaftEsbStore.store("Fiscalite", "UnitTest", "Unireg", is);
		}
		assertNotNull(raftUrl);

		final RegDate dateImportInitial = RegDate.get(2010, 1, 1);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// l'import principal préalablement traité (précondition pour l'exécution de l'import des servitudes)
		insertImport(TypeImportRF.PRINCIPAL, dateSecondImport, EtatEvenementRF.TRAITE, "http://turlututu");

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setType(TypeImportRF.SERVITUDES);
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

				// données équivalentes au fichier export_servitudes_rf.xml
				BienFondsRF bienFonds1 = new BienFondsRF();
				bienFonds1.setIdRF("_1f109152380ffd8901380ffe15bb729c");
				bienFonds1 = (BienFondsRF) immeubleRFDAO.save(bienFonds1);

				BienFondsRF bienFonds2 = new BienFondsRF();
				bienFonds2.setIdRF("_1f109152380ffd8901380ffe090827e1");
				bienFonds2 = (BienFondsRF) immeubleRFDAO.save(bienFonds2);

				PersonnePhysiqueRF pp1 = newPersonnePhysique("_1f109152380ffd8901380ffdabcc2441", 0L, 10386724L, "Gaillard", "Roger", RegDate.get(1938, 2, 2));
				pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

				PersonnePhysiqueRF pp2 = newPersonnePhysique("_1f109152380ffd8901380ffda8131c65", 0L, 10385020L, "Lassueur", "Anne-Lise", RegDate.get(1945, 3, 9));
				pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

				PersonnePhysiqueRF pp3 = newPersonnePhysique("_1f109152380ffd8901380ffda5511644", 0L, 10385019L, "Lassueur", "Jean-Claude", RegDate.get(1941, 6, 27));
				pp3 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp3);

				// les servitudes
				UsufruitRF usu1 = newUsufruitRF("1f109152380ffd8901380ffed6694392", "1f109152380ffd8901380ffed66943a2", bienFonds1, pp1, dateImportInitial, RegDate.get(2002, 9, 2), null, null,
				                                new IdentifiantDroitRF(8, 2005, 699), new IdentifiantAffaireRF(8, 2002, 392, null));
				UsufruitRF usu2 = newUsufruitRF("1f109152380ffd8901380ffefad54360", "1f109152380ffd8901380ffefad64374", Collections.singletonList(bienFonds2), Arrays.asList(pp2, pp3), dateImportInitial, RegDate.get(2006, 6, 30), null, null,
				                                new IdentifiantDroitRF(8, 2006, 361), new IdentifiantAffaireRF(8, 2006, 285, 0));
				droitRFDAO.save(usu1);
				droitRFDAO.save(usu2);
			}
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
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = evenementRFImportDAO.get(importId);
				assertNotNull(importEvent);
				assertEquals(EtatEvenementRF.TRAITE, importEvent.getEtat());
			}
		});

		// on vérifie que les mutations attendues sont bien dans la DB
		doInNewTransaction(status -> {
			final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
			assertEquals(2, mutations.size());    // les 3 droits qui existent dans la DB devront être fermés
			mutations.sort(new MutationComparator());

			final EvenementRFMutation mut0 = mutations.get(0);
			assertEquals(importId, mut0.getParentImport().getId());
			assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
			assertEquals(TypeEntiteRF.SERVITUDE, mut0.getTypeEntite());
			assertEquals(TypeMutationRF.SUPPRESSION, mut0.getTypeMutation());
			assertEquals("1f109152380ffd8901380ffed6694392", mut0.getIdRF());
			assertNull(mut0.getXmlContent());

			final EvenementRFMutation mut1 = mutations.get(1);
			assertEquals(importId, mut1.getParentImport().getId());
			assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
			assertEquals(TypeEntiteRF.SERVITUDE, mut1.getTypeEntite());
			assertEquals(TypeMutationRF.SUPPRESSION, mut1.getTypeMutation());
			assertEquals("1f109152380ffd8901380ffefad54360", mut1.getIdRF());
			assertNull(mut1.getXmlContent());
			return null;
		});
	}

	/**
	 * [SIFISC-29540] Vérifie que l'import de servitudes fonctionne bien dans le cas des bénéficiaires variables en fonction des immeubles.
	 */
	@Test
	public void testImportServitudeAvecBeneficiairesVariables() throws Exception {

		// un fichier d'import avec un usufruits et deux immeubles mais des bénéficiaires différents en fonction des immeubles
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/export_servitudes_beneficiaires_variables_rf.xml");
		assertNotNull(importFile);

		// on l'upload dans Raft
		final String raftUrl;
		try (FileInputStream is = new FileInputStream(importFile)) {
			raftUrl = zipRaftEsbStore.store("Fiscalite", "UnitTest", "Unireg", is);
		}
		assertNotNull(raftUrl);

		// l'import principal préalablement traité (précondition pour l'exécution de l'import des servitudes)
		insertImport(TypeImportRF.PRINCIPAL, RegDate.get(2016, 10, 1), EtatEvenementRF.TRAITE, "http://turlututu");

		// on insère les données de l'import dans la base
		final Long importId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final EvenementRFImport importEvent = new EvenementRFImport();
				importEvent.setType(TypeImportRF.SERVITUDES);
				importEvent.setDateEvenement(RegDate.get(2016, 10, 1));
				importEvent.setEtat(EtatEvenementRF.A_TRAITER);
				importEvent.setFileUrl(raftUrl);
				return evenementRFImportDAO.save(importEvent).getId();
			}
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
				assertEquals(4, mutations.size());    // il y a 1 usufruit + 3 personnes physiques
				mutations.sort(new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(TypeEntiteRF.AYANT_DROIT, mut0.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffda5511644", mut0.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<NatuerlichePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <PersonstammID>_1f109152380ffd8901380ffda5511644</PersonstammID>\n" +
						             "    <Name>Lassueur</Name>\n" +
						             "    <Gueltig>true</Gueltig>\n" +
						             "    <ClientRegulier>false</ClientRegulier>\n" +
						             "    <NoSCC>0</NoSCC>\n" +
						             "    <Status>definitiv</Status>\n" +
						             "    <Sprache>\n" +
						             "        <TextDe>Französisch</TextDe>\n" +
						             "        <TextFr>Français</TextFr>\n" +
						             "    </Sprache>\n" +
						             "    <Anrede>\n" +
						             "        <TextDe>*Monsieur</TextDe>\n" +
						             "        <TextFr>Monsieur</TextFr>\n" +
						             "    </Anrede>\n" +
						             "    <NrACI>0</NrACI>\n" +
						             "    <Adressen>\n" +
						             "        <Strasse>Rue des Sauges 22</Strasse>\n" +
						             "        <PLZ>1347</PLZ>\n" +
						             "        <Ort>Le Sentier</Ort>\n" +
						             "        <Rolle>ACI</Rolle>\n" +
						             "    </Adressen>\n" +
						             "    <Vorname>Jean-Claude</Vorname>\n" +
						             "    <Zivilstand>unbekannt</Zivilstand>\n" +
						             "    <Geburtsdatum>\n" +
						             "        <Tag>27</Tag>\n" +
						             "        <Monat>6</Monat>\n" +
						             "        <Jahr>1941</Jahr>\n" +
						             "    </Geburtsdatum>\n" +
						             "    <NameDerEltern>René</NameDerEltern>\n" +
						             "    <Geschlecht>unbekannt</Geschlecht>\n" +
						             "    <WeitereVornamen>René</WeitereVornamen>\n" +
						             "    <NrIROLE>10385019</NrIROLE>\n" +
						             "</NatuerlichePersonstamm>\n", mut0.getXmlContent());

				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(TypeEntiteRF.AYANT_DROIT, mut1.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffda8131c65", mut1.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<NatuerlichePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <PersonstammID>_1f109152380ffd8901380ffda8131c65</PersonstammID>\n" +
						             "    <Name>Lassueur</Name>\n" +
						             "    <Gueltig>true</Gueltig>\n" +
						             "    <ClientRegulier>false</ClientRegulier>\n" +
						             "    <NoSCC>0</NoSCC>\n" +
						             "    <Status>definitiv</Status>\n" +
						             "    <Sprache>\n" +
						             "        <TextDe>Französisch</TextDe>\n" +
						             "        <TextFr>Français</TextFr>\n" +
						             "    </Sprache>\n" +
						             "    <Anrede>\n" +
						             "        <TextDe>*Madame</TextDe>\n" +
						             "        <TextFr>Madame</TextFr>\n" +
						             "    </Anrede>\n" +
						             "    <NrACI>0</NrACI>\n" +
						             "    <Adressen>\n" +
						             "        <Strasse>Rue des Sauges 22</Strasse>\n" +
						             "        <PLZ>1347</PLZ>\n" +
						             "        <Ort>Le Sentier</Ort>\n" +
						             "        <Rolle>ACI</Rolle>\n" +
						             "    </Adressen>\n" +
						             "    <Vorname>Anne-Lise</Vorname>\n" +
						             "    <Zivilstand>unbekannt</Zivilstand>\n" +
						             "    <Geburtsdatum>\n" +
						             "        <Tag>9</Tag>\n" +
						             "        <Monat>3</Monat>\n" +
						             "        <Jahr>1945</Jahr>\n" +
						             "    </Geburtsdatum>\n" +
						             "    <LedigName>Audemars</LedigName>\n" +
						             "    <NameDerEltern>Louis</NameDerEltern>\n" +
						             "    <Geschlecht>unbekannt</Geschlecht>\n" +
						             "    <NameEhegatte>Jean-Claude</NameEhegatte>\n" +
						             "    <NrIROLE>10385020</NrIROLE>\n" +
						             "</NatuerlichePersonstamm>\n", mut1.getXmlContent());

				final EvenementRFMutation mut2 = mutations.get(2);
				assertEquals(importId, mut2.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
				assertEquals(TypeEntiteRF.AYANT_DROIT, mut2.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut2.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffdabcc2441", mut2.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<NatuerlichePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <PersonstammID>_1f109152380ffd8901380ffdabcc2441</PersonstammID>\n" +
						             "    <Name>Gaillard</Name>\n" +
						             "    <Gueltig>true</Gueltig>\n" +
						             "    <ClientRegulier>false</ClientRegulier>\n" +
						             "    <NoSCC>0</NoSCC>\n" +
						             "    <Status>definitiv</Status>\n" +
						             "    <Sprache>\n" +
						             "        <TextDe>Französisch</TextDe>\n" +
						             "        <TextFr>Français</TextFr>\n" +
						             "    </Sprache>\n" +
						             "    <Anrede>\n" +
						             "        <TextDe>*Monsieur</TextDe>\n" +
						             "        <TextFr>Monsieur</TextFr>\n" +
						             "    </Anrede>\n" +
						             "    <NrACI>0</NrACI>\n" +
						             "    <Adressen>\n" +
						             "        <Strasse>Le Charroux 1</Strasse>\n" +
						             "        <PLZ>1345</PLZ>\n" +
						             "        <Ort>Le Lieu</Ort>\n" +
						             "        <Rolle>ACI</Rolle>\n" +
						             "    </Adressen>\n" +
						             "    <Vorname>Roger</Vorname>\n" +
						             "    <Zivilstand>unbekannt</Zivilstand>\n" +
						             "    <Geburtsdatum>\n" +
						             "        <Tag>2</Tag>\n" +
						             "        <Monat>2</Monat>\n" +
						             "        <Jahr>1938</Jahr>\n" +
						             "    </Geburtsdatum>\n" +
						             "    <NameDerEltern>Albert</NameDerEltern>\n" +
						             "    <Geschlecht>unbekannt</Geschlecht>\n" +
						             "    <WeitereVornamen>Albert</WeitereVornamen>\n" +
						             "    <NrIROLE>10386724</NrIROLE>\n" +
						             "</NatuerlichePersonstamm>\n", mut2.getXmlContent());

				// les bénéficiaires sont fusionnés dans la même mutation
				final EvenementRFMutation mut3 = mutations.get(3);
				assertEquals(importId, mut3.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut3.getEtat());
				assertEquals(TypeEntiteRF.SERVITUDE, mut3.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut3.getTypeMutation());
				assertEquals("1f109152380ffd8901380ffed6694392", mut3.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<DienstbarkeitExtended xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <Dienstbarkeit VersionID=\"1f109152380ffd8901380ffed66943a2\" MasterID=\"1f109152380ffd8901380ffed6694392\">\n" +
						             "        <StandardRechtID>_1f109152380ffd8901380ffed6694392</StandardRechtID>\n" +
						             "        <BeteiligtesGrundstueckIDREF>_1f109152380ffd8901380ffe15bb729c</BeteiligtesGrundstueckIDREF>\n" +
						             "        <RechtEintragJahrID>2005</RechtEintragJahrID>\n" +
						             "        <RechtEintragNummerID>699</RechtEintragNummerID>\n" +
						             "        <Bereinigungsmarkierung>false</Bereinigungsmarkierung>\n" +
						             "        <AmtNummer>8</AmtNummer>\n" +
						             "        <Stichwort>\n" +
						             "            <TextDe>*Usufruit</TextDe>\n" +
						             "            <TextFr>Usufruit</TextFr>\n" +
						             "        </Stichwort>\n" +
						             "        <Rechtzusatz>conventionnel</Rechtzusatz>\n" +
						             "        <BelegAlt>2002/392</BelegAlt>\n" +
						             "        <BeginDatum>2002-09-02</BeginDatum>\n" +
						             "        <Entschaedigung>0</Entschaedigung>\n" +
						             "        <Wert>0</Wert>\n" +
						             "        <Meldungspflichtig>gem_code</Meldungspflichtig>\n" +
						             "        <Personenberechtigt>true</Personenberechtigt>\n" +
						             "        <Grundstueckeberechtigt>false</Grundstueckeberechtigt>\n" +
						             "        <EintragungAlsSdR>false</EintragungAlsSdR>\n" +
						             "    </Dienstbarkeit>\n" +
						             "    <LastRechtGruppe VersionID=\"1f109152380ffd8901380ffed66a43c1\">\n" +
						             "        <StandardRechtIDREF>_1f109152380ffd8901380ffed6694392</StandardRechtIDREF>\n" +
						             "        <BelastetesGrundstueck VersionID=\"1f109152380ffd8901380ffed672445a\">\n" +
						             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe15bb729c</BelastetesGrundstueckIDREF>\n" +
						             "        </BelastetesGrundstueck>\n" +
						             "        <BelastetesGrundstueck VersionID=\"1f109152380ffd8901380ffefadb441c\">\n" +
						             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe090827e1</BelastetesGrundstueckIDREF>\n" +
						             "        </BelastetesGrundstueck>\n" +
						             "        <BerechtigtePerson VersionID=\"1f109152380ffd8901380ffed66d4417\">\n" +
						             "            <NatuerlichePersonGb VersionID=\"1f109152380ffd8901380ffe24b81b93\" MasterID=\"1f109152380ffd8901380ffe24b31b61\">\n" +
						             "                <Name>Gaillard</Name>\n" +
						             "                <Status>definitiv</Status>\n" +
						             "                <Vorname>Roger</Vorname>\n" +
						             "                <Geburtsdatum>\n" +
						             "                    <Tag>2</Tag>\n" +
						             "                    <Monat>2</Monat>\n" +
						             "                    <Jahr>1938</Jahr>\n" +
						             "                </Geburtsdatum>\n" +
						             "                <Zivilstand>unbekannt</Zivilstand>\n" +
						             "                <NameEltern>Albert</NameEltern>\n" +
						             "                <WeitereVornamen>Albert</WeitereVornamen>\n" +
						             "                <PersonstammIDREF>_1f109152380ffd8901380ffdabcc2441</PersonstammIDREF>\n" +
						             "            </NatuerlichePersonGb>\n" +
						             "        </BerechtigtePerson>\n" +
						             "        <BerechtigtePerson VersionID=\"1f109152380ffd8901380ffefada43f6\">\n" +
						             "            <NatuerlichePersonGb VersionID=\"1f109152380ffd8901380ffe25b22067\" MasterID=\"1f109152380ffd8901380ffe25ae2003\">\n" +
						             "                <Name>Lassueur</Name>\n" +
						             "                <Status>definitiv</Status>\n" +
						             "                <Vorname>Anne-Lise</Vorname>\n" +
						             "                <Ledigname>Audemars</Ledigname>\n" +
						             "                <Geburtsdatum>\n" +
						             "                    <Tag>9</Tag>\n" +
						             "                    <Monat>3</Monat>\n" +
						             "                    <Jahr>1945</Jahr>\n" +
						             "                </Geburtsdatum>\n" +
						             "                <Zivilstand>unbekannt</Zivilstand>\n" +
						             "                <NameEltern>Louis</NameEltern>\n" +
						             "                <NameEhegatte>Jean-Claude</NameEhegatte>\n" +
						             "                <PersonstammIDREF>_1f109152380ffd8901380ffda8131c65</PersonstammIDREF>\n" +
						             "            </NatuerlichePersonGb>\n" +
						             "        </BerechtigtePerson>\n" +
						             "        <BerechtigtePerson VersionID=\"1f109152380ffd8901380ffefada43f7\">\n" +
						             "            <NatuerlichePersonGb VersionID=\"1f109152380ffd8901380ffe25b22068\" MasterID=\"1f109152380ffd8901380ffe25ae2004\">\n" +
						             "                <Name>Lassueur</Name>\n" +
						             "                <Status>definitiv</Status>\n" +
						             "                <Vorname>Jean-Claude</Vorname>\n" +
						             "                <Geburtsdatum>\n" +
						             "                    <Tag>27</Tag>\n" +
						             "                    <Monat>6</Monat>\n" +
						             "                    <Jahr>1941</Jahr>\n" +
						             "                </Geburtsdatum>\n" +
						             "                <Zivilstand>unbekannt</Zivilstand>\n" +
						             "                <NameEltern>René</NameEltern>\n" +
						             "                <WeitereVornamen>René</WeitereVornamen>\n" +
						             "                <PersonstammIDREF>_1f109152380ffd8901380ffda5511644</PersonstammIDREF>\n" +
						             "            </NatuerlichePersonGb>\n" +
						             "        </BerechtigtePerson>\n" +
						             "    </LastRechtGruppe>\n" +
						             "</DienstbarkeitExtended>\n", mut3.getXmlContent());

			}
		});

	}
}