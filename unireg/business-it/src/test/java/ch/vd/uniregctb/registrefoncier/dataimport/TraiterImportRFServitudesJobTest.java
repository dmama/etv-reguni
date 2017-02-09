package ch.vd.uniregctb.registrefoncier.dataimport;

import java.io.File;
import java.io.FileInputStream;
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
import ch.vd.uniregctb.evenement.registrefoncier.TypeImportRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantDroitRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.TypeCommunaute;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
	 * Ce test vérifie que les mutations sont bien créées lorsqu'on importe un fichier RF de servitudes sur une base vide
	 */
	@Test
	public void testImportBaseVide() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_servitudes_rf.xml");
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
				assertEquals(7, mutations.size());    // il y a 3 usufruits + 3 personnes physiques + 1 communauté
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
				assertEquals(TypeEntiteRF.AYANT_DROIT, mut3.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut3.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffefad54360", mut3.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<Gemeinschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
						             "    <GemeinschatID>_1f109152380ffd8901380ffefad54360</GemeinschatID>\n" +
						             "    <Art>Gemeinderschaft</Art>\n" +
						             "</Gemeinschaft>\n", mut3.getXmlContent());

				final EvenementRFMutation mut4 = mutations.get(4);
				assertEquals(importId, mut4.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut4.getEtat());
				assertEquals(TypeEntiteRF.SERVITUDE, mut4.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut4.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffda5511644", mut4.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<DienstbarkeitDiscreteList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <DienstbarkeitDiscretes>\n" +
						             "        <Dienstbarkeit VersionID=\"1f109152380ffd8901380ffefad64374\" MasterID=\"1f109152380ffd8901380ffefad54360\">\n" +
						             "            <StandardRechtID>_1f109152380ffd8901380ffefad54360</StandardRechtID>\n" +
						             "            <BeteiligtesGrundstueckIDREF>_1f109152380ffd8901380ffe090827e1</BeteiligtesGrundstueckIDREF>\n" +
						             "            <RechtEintragJahrID>2006</RechtEintragJahrID>\n" +
						             "            <RechtEintragNummerID>361</RechtEintragNummerID>\n" +
						             "            <Bereinigungsmarkierung>false</Bereinigungsmarkierung>\n" +
						             "            <AmtNummer>8</AmtNummer>\n" +
						             "            <Stichwort>\n" +
						             "                <TextDe>*Usufruit</TextDe>\n" +
						             "                <TextFr>Usufruit</TextFr>\n" +
						             "            </Stichwort>\n" +
						             "            <Rechtzusatz>conventionnel</Rechtzusatz>\n" +
						             "            <Beleg>\n" +
						             "                <AmtNummer>8</AmtNummer>\n" +
						             "                <BelegJahr>2006</BelegJahr>\n" +
						             "                <BelegNummer>285</BelegNummer>\n" +
						             "                <BelegNummerIndex>0</BelegNummerIndex>\n" +
						             "            </Beleg>\n" +
						             "            <BeginDatum>2006-06-30</BeginDatum>\n" +
						             "            <Entschaedigung>0</Entschaedigung>\n" +
						             "            <Wert>0</Wert>\n" +
						             "            <Meldungspflichtig>gem_code</Meldungspflichtig>\n" +
						             "            <Personenberechtigt>true</Personenberechtigt>\n" +
						             "            <Grundstueckeberechtigt>false</Grundstueckeberechtigt>\n" +
						             "            <EintragungAlsSdR>false</EintragungAlsSdR>\n" +
						             "        </Dienstbarkeit>\n" +
						             "        <BelastetesGrundstueck VersionID=\"1f109152380ffd8901380ffefadb441c\">\n" +
						             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe090827e1</BelastetesGrundstueckIDREF>\n" +
						             "        </BelastetesGrundstueck>\n" +
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
						             "        <Gemeinschaft VersionID=\"1f109152380ffd8901380ffefada43f6\">\n" +
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
						             "        </Gemeinschaft>\n" +
						             "        <Gemeinschaft VersionID=\"1f109152380ffd8901380ffefada43f7\">\n" +
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
						             "        </Gemeinschaft>\n" +
						             "    </DienstbarkeitDiscretes>\n" +
						             "</DienstbarkeitDiscreteList>\n", mut4.getXmlContent());

				final EvenementRFMutation mut5 = mutations.get(5);
				assertEquals(importId, mut5.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut5.getEtat());
				assertEquals(TypeEntiteRF.SERVITUDE, mut5.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut5.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffda8131c65", mut5.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<DienstbarkeitDiscreteList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <DienstbarkeitDiscretes>\n" +
						             "        <Dienstbarkeit VersionID=\"1f109152380ffd8901380ffefad64374\" MasterID=\"1f109152380ffd8901380ffefad54360\">\n" +
						             "            <StandardRechtID>_1f109152380ffd8901380ffefad54360</StandardRechtID>\n" +
						             "            <BeteiligtesGrundstueckIDREF>_1f109152380ffd8901380ffe090827e1</BeteiligtesGrundstueckIDREF>\n" +
						             "            <RechtEintragJahrID>2006</RechtEintragJahrID>\n" +
						             "            <RechtEintragNummerID>361</RechtEintragNummerID>\n" +
						             "            <Bereinigungsmarkierung>false</Bereinigungsmarkierung>\n" +
						             "            <AmtNummer>8</AmtNummer>\n" +
						             "            <Stichwort>\n" +
						             "                <TextDe>*Usufruit</TextDe>\n" +
						             "                <TextFr>Usufruit</TextFr>\n" +
						             "            </Stichwort>\n" +
						             "            <Rechtzusatz>conventionnel</Rechtzusatz>\n" +
						             "            <Beleg>\n" +
						             "                <AmtNummer>8</AmtNummer>\n" +
						             "                <BelegJahr>2006</BelegJahr>\n" +
						             "                <BelegNummer>285</BelegNummer>\n" +
						             "                <BelegNummerIndex>0</BelegNummerIndex>\n" +
						             "            </Beleg>\n" +
						             "            <BeginDatum>2006-06-30</BeginDatum>\n" +
						             "            <Entschaedigung>0</Entschaedigung>\n" +
						             "            <Wert>0</Wert>\n" +
						             "            <Meldungspflichtig>gem_code</Meldungspflichtig>\n" +
						             "            <Personenberechtigt>true</Personenberechtigt>\n" +
						             "            <Grundstueckeberechtigt>false</Grundstueckeberechtigt>\n" +
						             "            <EintragungAlsSdR>false</EintragungAlsSdR>\n" +
						             "        </Dienstbarkeit>\n" +
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
						             "        <Gemeinschaft VersionID=\"1f109152380ffd8901380ffefada43f6\">\n" +
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
						             "        </Gemeinschaft>\n" +
						             "        <Gemeinschaft VersionID=\"1f109152380ffd8901380ffefada43f7\">\n" +
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
						             "        </Gemeinschaft>\n" +
						             "    </DienstbarkeitDiscretes>\n" +
						             "</DienstbarkeitDiscreteList>\n", mut5.getXmlContent());

				final EvenementRFMutation mut6 = mutations.get(6);
				assertEquals(importId, mut6.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut6.getEtat());
				assertEquals(TypeEntiteRF.SERVITUDE, mut6.getTypeEntite());
				assertEquals(TypeMutationRF.CREATION, mut6.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffdabcc2441", mut6.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<DienstbarkeitDiscreteList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <DienstbarkeitDiscretes>\n" +
						             "        <Dienstbarkeit VersionID=\"1f109152380ffd8901380ffed66943a2\" MasterID=\"1f109152380ffd8901380ffed6694392\">\n" +
						             "            <StandardRechtID>_1f109152380ffd8901380ffed6694392</StandardRechtID>\n" +
						             "            <BeteiligtesGrundstueckIDREF>_1f109152380ffd8901380ffe15bb729c</BeteiligtesGrundstueckIDREF>\n" +
						             "            <RechtEintragJahrID>2005</RechtEintragJahrID>\n" +
						             "            <RechtEintragNummerID>699</RechtEintragNummerID>\n" +
						             "            <Bereinigungsmarkierung>false</Bereinigungsmarkierung>\n" +
						             "            <AmtNummer>8</AmtNummer>\n" +
						             "            <Stichwort>\n" +
						             "                <TextDe>*Usufruit</TextDe>\n" +
						             "                <TextFr>Usufruit</TextFr>\n" +
						             "            </Stichwort>\n" +
						             "            <Rechtzusatz>conventionnel</Rechtzusatz>\n" +
						             "            <BelegAlt>2002/392</BelegAlt>\n" +
						             "            <BeginDatum>2002-09-02</BeginDatum>\n" +
						             "            <Entschaedigung>0</Entschaedigung>\n" +
						             "            <Wert>0</Wert>\n" +
						             "            <Meldungspflichtig>gem_code</Meldungspflichtig>\n" +
						             "            <Personenberechtigt>true</Personenberechtigt>\n" +
						             "            <Grundstueckeberechtigt>false</Grundstueckeberechtigt>\n" +
						             "            <EintragungAlsSdR>false</EintragungAlsSdR>\n" +
						             "        </Dienstbarkeit>\n" +
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
						             "    </DienstbarkeitDiscretes>\n" +
						             "</DienstbarkeitDiscreteList>\n", mut6.getXmlContent());
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

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_servitudes_rf.xml");
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
				BienFondRF bienFond1 = new BienFondRF();
				bienFond1.setIdRF("_1f109152380ffd8901380ffe15bb729c");
				bienFond1 = (BienFondRF) immeubleRFDAO.save(bienFond1);

				BienFondRF bienFond2 = new BienFondRF();
				bienFond2.setIdRF("_1f109152380ffd8901380ffe090827e1");
				bienFond2 = (BienFondRF) immeubleRFDAO.save(bienFond2);

				PersonnePhysiqueRF pp1 = newPersonnePhysique("_1f109152380ffd8901380ffdabcc2441", 0L, 10386724L, "Gaillard", "Roger", RegDate.get(1938, 2, 2));
				pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

				PersonnePhysiqueRF pp2 = newPersonnePhysique("_1f109152380ffd8901380ffda8131c65", 0L, 10385020L, "Lassueur", "Anne-Lise", RegDate.get(1945, 3, 9));
				pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

				PersonnePhysiqueRF pp3 = newPersonnePhysique("_1f109152380ffd8901380ffda5511644", 0L, 10385019L, "Lassueur", "Jean-Claude", RegDate.get(1941, 6, 27));
				pp3 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp3);

				CommunauteRF communaute = newCommunauté("_1f109152380ffd8901380ffefad54360", TypeCommunaute.INDIVISION);
				communaute = (CommunauteRF) ayantDroitRFDAO.save(communaute);

				// les servitudes
				UsufruitRF usu1 = newUsufruitRF("1f109152380ffd8901380ffed6694392", bienFond1, pp1, null, dateImportInitial, RegDate.get(2002, 9, 2), null, null,
				                                new IdentifiantDroitRF(8, 2005, 699), new IdentifiantAffaireRF(8, 2002, 392, null));
				UsufruitRF usu2 = newUsufruitRF("1f109152380ffd8901380ffefad54360", bienFond2, pp2, communaute, dateImportInitial, RegDate.get(2006, 6, 30), null, null,
				                                new IdentifiantDroitRF(8, 2006, 361), new IdentifiantAffaireRF(8, 2006, 285, 0));
				UsufruitRF usu3 = newUsufruitRF("1f109152380ffd8901380ffefad54360", bienFond2, pp3, communaute, dateImportInitial, RegDate.get(2006, 6, 30), null, null,
				                                new IdentifiantDroitRF(8, 2006, 361), new IdentifiantAffaireRF(8, 2006, 285, 0));
				droitRFDAO.save(usu1);
				droitRFDAO.save(usu2);
				droitRFDAO.save(usu3);
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
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_servitudes_rf.xml");
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
				BienFondRF bienFond1 = new BienFondRF();
				bienFond1.setIdRF("_1f109152380ffd8901380ffe15bb729c");
				bienFond1 = (BienFondRF) immeubleRFDAO.save(bienFond1);

				BienFondRF bienFond2 = new BienFondRF();
				bienFond2.setIdRF("_1f109152380ffd8901380ffe090827e1");
				bienFond2 = (BienFondRF) immeubleRFDAO.save(bienFond2);

				PersonnePhysiqueRF pp1 = newPersonnePhysique("_1f109152380ffd8901380ffdabcc2441", 0L, 10386724L, "Gaillard", "Roger", RegDate.get(1938, 2, 2));
				pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

				PersonnePhysiqueRF pp2 = newPersonnePhysique("_1f109152380ffd8901380ffda8131c65", 0L, 10385020L, "Lassueur", "Anne-Lise", RegDate.get(1945, 3, 9));
				pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

				PersonnePhysiqueRF pp3 = newPersonnePhysique("_1f109152380ffd8901380ffda5511644", 0L, 10385019L, "Lassueur", "Jean-Claude", RegDate.get(1941, 6, 27));
				pp3 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp3);

				CommunauteRF communaute = newCommunauté("_1f109152380ffd8901380ffefad54360", TypeCommunaute.INDIVISION);
				communaute = (CommunauteRF) ayantDroitRFDAO.save(communaute);

				// identifiant du droit différent
				UsufruitRF usu1 = newUsufruitRF("1f109152380ffd8901380ffed6694392", bienFond1, pp1, null, dateImportInitial, RegDate.get(2002, 9, 2), null, null,
				                                new IdentifiantDroitRF(8, 2002, 333), new IdentifiantAffaireRF(8, 2002, 392, null));
				// index non-renseigné
				UsufruitRF usu2 = newUsufruitRF("1f109152380ffd8901380ffefad54360", bienFond2, pp2, communaute, dateImportInitial, RegDate.get(2006, 6, 30), null, null,
				                                new IdentifiantDroitRF(8, 2006, 361), new IdentifiantAffaireRF(8, 2006, 285, null));
				// index non-renseigné
				UsufruitRF usu3 = newUsufruitRF("1f109152380ffd8901380ffefad54360", bienFond2, pp3, communaute, dateImportInitial, RegDate.get(2006, 6, 30), null, null,
				                                new IdentifiantDroitRF(8, 2006, 361), new IdentifiantAffaireRF(8, 2006, 285, null));
				droitRFDAO.save(usu1);
				droitRFDAO.save(usu2);
				droitRFDAO.save(usu3);
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
				assertEquals(3, mutations.size());    // les 3 usufruits dans le fichier d'import sont différents
				mutations.sort(new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(TypeEntiteRF.SERVITUDE, mut0.getTypeEntite());
				assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffda5511644", mut0.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<DienstbarkeitDiscreteList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <DienstbarkeitDiscretes>\n" +
						             "        <Dienstbarkeit VersionID=\"1f109152380ffd8901380ffefad64374\" MasterID=\"1f109152380ffd8901380ffefad54360\">\n" +
						             "            <StandardRechtID>_1f109152380ffd8901380ffefad54360</StandardRechtID>\n" +
						             "            <BeteiligtesGrundstueckIDREF>_1f109152380ffd8901380ffe090827e1</BeteiligtesGrundstueckIDREF>\n" +
						             "            <RechtEintragJahrID>2006</RechtEintragJahrID>\n" +
						             "            <RechtEintragNummerID>361</RechtEintragNummerID>\n" +
						             "            <Bereinigungsmarkierung>false</Bereinigungsmarkierung>\n" +
						             "            <AmtNummer>8</AmtNummer>\n" +
						             "            <Stichwort>\n" +
						             "                <TextDe>*Usufruit</TextDe>\n" +
						             "                <TextFr>Usufruit</TextFr>\n" +
						             "            </Stichwort>\n" +
						             "            <Rechtzusatz>conventionnel</Rechtzusatz>\n" +
						             "            <Beleg>\n" +
						             "                <AmtNummer>8</AmtNummer>\n" +
						             "                <BelegJahr>2006</BelegJahr>\n" +
						             "                <BelegNummer>285</BelegNummer>\n" +
						             "                <BelegNummerIndex>0</BelegNummerIndex>\n" +
						             "            </Beleg>\n" +
						             "            <BeginDatum>2006-06-30</BeginDatum>\n" +
						             "            <Entschaedigung>0</Entschaedigung>\n" +
						             "            <Wert>0</Wert>\n" +
						             "            <Meldungspflichtig>gem_code</Meldungspflichtig>\n" +
						             "            <Personenberechtigt>true</Personenberechtigt>\n" +
						             "            <Grundstueckeberechtigt>false</Grundstueckeberechtigt>\n" +
						             "            <EintragungAlsSdR>false</EintragungAlsSdR>\n" +
						             "        </Dienstbarkeit>\n" +
						             "        <BelastetesGrundstueck VersionID=\"1f109152380ffd8901380ffefadb441c\">\n" +
						             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe090827e1</BelastetesGrundstueckIDREF>\n" +
						             "        </BelastetesGrundstueck>\n" +
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
						             "        <Gemeinschaft VersionID=\"1f109152380ffd8901380ffefada43f6\">\n" +
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
						             "        </Gemeinschaft>\n" +
						             "        <Gemeinschaft VersionID=\"1f109152380ffd8901380ffefada43f7\">\n" +
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
						             "        </Gemeinschaft>\n" +
						             "    </DienstbarkeitDiscretes>\n" +
						             "</DienstbarkeitDiscreteList>\n", mut0.getXmlContent());

				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(TypeEntiteRF.SERVITUDE, mut1.getTypeEntite());
				assertEquals(TypeMutationRF.MODIFICATION, mut1.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffda8131c65", mut1.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<DienstbarkeitDiscreteList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <DienstbarkeitDiscretes>\n" +
						             "        <Dienstbarkeit VersionID=\"1f109152380ffd8901380ffefad64374\" MasterID=\"1f109152380ffd8901380ffefad54360\">\n" +
						             "            <StandardRechtID>_1f109152380ffd8901380ffefad54360</StandardRechtID>\n" +
						             "            <BeteiligtesGrundstueckIDREF>_1f109152380ffd8901380ffe090827e1</BeteiligtesGrundstueckIDREF>\n" +
						             "            <RechtEintragJahrID>2006</RechtEintragJahrID>\n" +
						             "            <RechtEintragNummerID>361</RechtEintragNummerID>\n" +
						             "            <Bereinigungsmarkierung>false</Bereinigungsmarkierung>\n" +
						             "            <AmtNummer>8</AmtNummer>\n" +
						             "            <Stichwort>\n" +
						             "                <TextDe>*Usufruit</TextDe>\n" +
						             "                <TextFr>Usufruit</TextFr>\n" +
						             "            </Stichwort>\n" +
						             "            <Rechtzusatz>conventionnel</Rechtzusatz>\n" +
						             "            <Beleg>\n" +
						             "                <AmtNummer>8</AmtNummer>\n" +
						             "                <BelegJahr>2006</BelegJahr>\n" +
						             "                <BelegNummer>285</BelegNummer>\n" +
						             "                <BelegNummerIndex>0</BelegNummerIndex>\n" +
						             "            </Beleg>\n" +
						             "            <BeginDatum>2006-06-30</BeginDatum>\n" +
						             "            <Entschaedigung>0</Entschaedigung>\n" +
						             "            <Wert>0</Wert>\n" +
						             "            <Meldungspflichtig>gem_code</Meldungspflichtig>\n" +
						             "            <Personenberechtigt>true</Personenberechtigt>\n" +
						             "            <Grundstueckeberechtigt>false</Grundstueckeberechtigt>\n" +
						             "            <EintragungAlsSdR>false</EintragungAlsSdR>\n" +
						             "        </Dienstbarkeit>\n" +
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
						             "        <Gemeinschaft VersionID=\"1f109152380ffd8901380ffefada43f6\">\n" +
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
						             "        </Gemeinschaft>\n" +
						             "        <Gemeinschaft VersionID=\"1f109152380ffd8901380ffefada43f7\">\n" +
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
						             "        </Gemeinschaft>\n" +
						             "    </DienstbarkeitDiscretes>\n" +
						             "</DienstbarkeitDiscreteList>\n", mut1.getXmlContent());

				final EvenementRFMutation mut2 = mutations.get(2);
				assertEquals(importId, mut2.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
				assertEquals(TypeEntiteRF.SERVITUDE, mut2.getTypeEntite());
				assertEquals(TypeMutationRF.MODIFICATION, mut2.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffdabcc2441", mut2.getIdRF());
				assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
						             "<DienstbarkeitDiscreteList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
						             "    <DienstbarkeitDiscretes>\n" +
						             "        <Dienstbarkeit VersionID=\"1f109152380ffd8901380ffed66943a2\" MasterID=\"1f109152380ffd8901380ffed6694392\">\n" +
						             "            <StandardRechtID>_1f109152380ffd8901380ffed6694392</StandardRechtID>\n" +
						             "            <BeteiligtesGrundstueckIDREF>_1f109152380ffd8901380ffe15bb729c</BeteiligtesGrundstueckIDREF>\n" +
						             "            <RechtEintragJahrID>2005</RechtEintragJahrID>\n" +
						             "            <RechtEintragNummerID>699</RechtEintragNummerID>\n" +
						             "            <Bereinigungsmarkierung>false</Bereinigungsmarkierung>\n" +
						             "            <AmtNummer>8</AmtNummer>\n" +
						             "            <Stichwort>\n" +
						             "                <TextDe>*Usufruit</TextDe>\n" +
						             "                <TextFr>Usufruit</TextFr>\n" +
						             "            </Stichwort>\n" +
						             "            <Rechtzusatz>conventionnel</Rechtzusatz>\n" +
						             "            <BelegAlt>2002/392</BelegAlt>\n" +
						             "            <BeginDatum>2002-09-02</BeginDatum>\n" +
						             "            <Entschaedigung>0</Entschaedigung>\n" +
						             "            <Wert>0</Wert>\n" +
						             "            <Meldungspflichtig>gem_code</Meldungspflichtig>\n" +
						             "            <Personenberechtigt>true</Personenberechtigt>\n" +
						             "            <Grundstueckeberechtigt>false</Grundstueckeberechtigt>\n" +
						             "            <EintragungAlsSdR>false</EintragungAlsSdR>\n" +
						             "        </Dienstbarkeit>\n" +
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
						             "    </DienstbarkeitDiscretes>\n" +
						             "</DienstbarkeitDiscreteList>\n", mut2.getXmlContent());
			}
		});
	}

	/**
	 * Ce test vérifie que des mutations de suppression sont créées si des propriétaires avec des servitudes dans la DB n'en ont plus dans le fichier d'import.
	 */
	@Test
	public void testImportSuppression() throws Exception {

		// on va chercher le fichier d'import
		final File importFile = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_servitudes_vide_rf.xml");
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
				BienFondRF bienFond1 = new BienFondRF();
				bienFond1.setIdRF("_1f109152380ffd8901380ffe15bb729c");
				bienFond1 = (BienFondRF) immeubleRFDAO.save(bienFond1);

				BienFondRF bienFond2 = new BienFondRF();
				bienFond2.setIdRF("_1f109152380ffd8901380ffe090827e1");
				bienFond2 = (BienFondRF) immeubleRFDAO.save(bienFond2);

				PersonnePhysiqueRF pp1 = newPersonnePhysique("_1f109152380ffd8901380ffdabcc2441", 0L, 10386724L, "Gaillard", "Roger", RegDate.get(1938, 2, 2));
				pp1 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp1);

				PersonnePhysiqueRF pp2 = newPersonnePhysique("_1f109152380ffd8901380ffda8131c65", 0L, 10385020L, "Lassueur", "Anne-Lise", RegDate.get(1945, 3, 9));
				pp2 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp2);

				PersonnePhysiqueRF pp3 = newPersonnePhysique("_1f109152380ffd8901380ffda5511644", 0L, 10385019L, "Lassueur", "Jean-Claude", RegDate.get(1941, 6, 27));
				pp3 = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp3);

				CommunauteRF communaute = newCommunauté("_1f109152380ffd8901380ffefad54360", TypeCommunaute.INDIVISION);
				communaute = (CommunauteRF) ayantDroitRFDAO.save(communaute);

				// les servitudes
				UsufruitRF usu1 = newUsufruitRF("1f109152380ffd8901380ffed6694392", bienFond1, pp1, null, dateImportInitial, RegDate.get(2002, 9, 2), null, null,
				                                new IdentifiantDroitRF(8, 2005, 699), new IdentifiantAffaireRF(8, 2002, 392, null));
				UsufruitRF usu2 = newUsufruitRF("1f109152380ffd8901380ffefad54360", bienFond2, pp2, communaute, dateImportInitial, RegDate.get(2006, 6, 30), null, null,
				                                new IdentifiantDroitRF(8, 2006, 361), new IdentifiantAffaireRF(8, 2006, 285, 0));
				UsufruitRF usu3 = newUsufruitRF("1f109152380ffd8901380ffefad54360", bienFond2, pp3, communaute, dateImportInitial, RegDate.get(2006, 6, 30), null, null,
				                                new IdentifiantDroitRF(8, 2006, 361), new IdentifiantAffaireRF(8, 2006, 285, 0));
				droitRFDAO.save(usu1);
				droitRFDAO.save(usu2);
				droitRFDAO.save(usu3);
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
				mutations.sort(new MutationComparator());

				final EvenementRFMutation mut0 = mutations.get(0);
				assertEquals(importId, mut0.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
				assertEquals(TypeEntiteRF.SERVITUDE, mut0.getTypeEntite());
				assertEquals(TypeMutationRF.SUPPRESSION, mut0.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffda5511644", mut0.getIdRF());
				assertNull(mut0.getXmlContent());

				final EvenementRFMutation mut1 = mutations.get(1);
				assertEquals(importId, mut1.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
				assertEquals(TypeEntiteRF.SERVITUDE, mut1.getTypeEntite());
				assertEquals(TypeMutationRF.SUPPRESSION, mut1.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffda8131c65", mut1.getIdRF());
				assertNull(mut1.getXmlContent());

				final EvenementRFMutation mut2 = mutations.get(2);
				assertEquals(importId, mut2.getParentImport().getId());
				assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
				assertEquals(TypeEntiteRF.SERVITUDE, mut2.getTypeEntite());
				assertEquals(TypeMutationRF.SUPPRESSION, mut2.getTypeMutation());
				assertEquals("_1f109152380ffd8901380ffdabcc2441", mut2.getIdRF());
				assertNull(mut2.getXmlContent());
			}
		});
	}
}