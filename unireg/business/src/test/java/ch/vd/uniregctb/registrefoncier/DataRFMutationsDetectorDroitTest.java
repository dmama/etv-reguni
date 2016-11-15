package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.grundstueck.CapiCode;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.GemeinschaftsArt;
import ch.vd.capitastra.grundstueck.NatuerlichePersonGb;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.PersonEigentumsform;
import ch.vd.capitastra.grundstueck.Quote;
import ch.vd.capitastra.grundstueck.Rechtsgrund;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.cache.MockPersistentCache;
import ch.vd.uniregctb.cache.PersistentCache;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.MockEvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.MockEvenementRFMutationDAO;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.MockAyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.MockImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRFImpl;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.transaction.MockTransactionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DataRFMutationsDetectorDroitTest {

	private static final Long IMPORT_ID = 1L;
	private XmlHelperRF xmlHelperRF;
	private PlatformTransactionManager transactionManager;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private PersistentCache<ArrayList<PersonEigentumAnteil>> cacheDroits;

	@Before
	public void setUp() throws Exception {
		xmlHelperRF = new XmlHelperRFImpl();
		transactionManager = new MockTransactionManager();
		ayantDroitRFDAO = new MockAyantDroitRFDAO();
		immeubleRFDAO = new MockImmeubleRFDAO();
		cacheDroits = new MockPersistentCache<>();
		AuthenticationHelper.pushPrincipal("test-user");
	}

	@After
	public void tearDown() throws Exception {
		AuthenticationHelper.popPrincipal();
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsque les droits n'existent pas dans la base de données.
	 */
	@Test
	public void testNouveauxDroits() throws Exception {

		// un mock de DAO avec un import du registre foncier
		final EvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO() {
			@Override
			public EvenementRFImport get(Long id) {
				final EvenementRFImport imp = new EvenementRFImport();
				imp.setId(IMPORT_ID);
				return imp;
			}
		};

		// un mock qui mémorise toutes les mutations sauvées
		final EvenementRFMutationDAO evenementRFMutationDAO = new MockEvenementRFMutationDAO();

		final DataRFMutationsDetector detector = new DataRFMutationsDetector(xmlHelperRF, immeubleRFDAO, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, cacheDroits);

		// on envoie trois nouveaux droits sur deux propriétaires qui concernent deux immeubles
		final PersonEigentumAnteil droit1 = newDroitPP("9a9c9e94923", "37838sc9d94de", "382929efa218", new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		final PersonEigentumAnteil droit2 = newDroitPP("45729cd9e20", "029191d4fec44", "382929efa218", new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		final PersonEigentumAnteil droit3 = newDroitPP("38458fa0ac3", "029191d4fec44", "202930c0e0f3", new Fraction(1, 1), PersonEigentumsform.ALLEINEIGENTUM, RegDate.get(2010, 3, 28), new IdentifiantAffaireRF(6, 2013, 28, 4), "Achat");

		List<PersonEigentumAnteil> droits = Arrays.asList(droit1, droit2, droit3);
		detector.processDroits(IMPORT_ID, 2, droits.iterator());

		// on devrait avoir deux événements de mutation de type CREATION sur chacun des propriétaires
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		Collections.sort(mutations, (o1, o2) -> o1.getIdRF().compareTo(o2.getIdRF()));

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut0.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut0.getTypeMutation());
		assertEquals("029191d4fec44", mut0.getIdRF());  // le premier propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<ns2:PersonEigentumAnteilList xmlns:ns2=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil MasterID=\"45729cd9e20\">\n" +
				             "        <ns2:Quote>\n" +
				             "            <ns2:AnteilZaehler>1</ns2:AnteilZaehler>\n" +
				             "            <ns2:AnteilNenner>2</ns2:AnteilNenner>\n" +
				             "        </ns2:Quote>\n" +
				             "        <ns2:BelastetesGrundstueckIDREF>382929efa218</ns2:BelastetesGrundstueckIDREF>\n" +
				             "        <ns2:NatuerlichePersonGb>\n" +
				             "            <ns2:Rechtsgruende>\n" +
				             "                <ns2:AmtNummer>6</ns2:AmtNummer>\n" +
				             "                <ns2:RechtsgrundCode>\n" +
				             "                    <ns2:TextFr>Achat</ns2:TextFr>\n" +
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
				             "    <PersonEigentumAnteil MasterID=\"38458fa0ac3\">\n" +
				             "        <ns2:Quote>\n" +
				             "            <ns2:AnteilZaehler>1</ns2:AnteilZaehler>\n" +
				             "            <ns2:AnteilNenner>1</ns2:AnteilNenner>\n" +
				             "        </ns2:Quote>\n" +
				             "        <ns2:BelastetesGrundstueckIDREF>202930c0e0f3</ns2:BelastetesGrundstueckIDREF>\n" +
				             "        <ns2:NatuerlichePersonGb>\n" +
				             "            <ns2:Rechtsgruende>\n" +
				             "                <ns2:AmtNummer>6</ns2:AmtNummer>\n" +
				             "                <ns2:RechtsgrundCode>\n" +
				             "                    <ns2:TextFr>Achat</ns2:TextFr>\n" +
				             "                </ns2:RechtsgrundCode>\n" +
				             "                <ns2:BelegDatum>2010-03-28</ns2:BelegDatum>\n" +
				             "                <ns2:BelegJahr>2013</ns2:BelegJahr>\n" +
				             "                <ns2:BelegNummer>28</ns2:BelegNummer>\n" +
				             "                <ns2:BelegNummerIndex>4</ns2:BelegNummerIndex>\n" +
				             "            </ns2:Rechtsgruende>\n" +
				             "            <ns2:PersonstammIDREF>029191d4fec44</ns2:PersonstammIDREF>\n" +
				             "        </ns2:NatuerlichePersonGb>\n" +
				             "        <ns2:PersonEigentumsForm>alleineigentum</ns2:PersonEigentumsForm>\n" +
				             "    </PersonEigentumAnteil>\n" +
				             "</ns2:PersonEigentumAnteilList>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut1.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut1.getTypeMutation());
		assertEquals("37838sc9d94de", mut1.getIdRF());  // le second propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<ns2:PersonEigentumAnteilList xmlns:ns2=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil MasterID=\"9a9c9e94923\">\n" +
				             "        <ns2:Quote>\n" +
				             "            <ns2:AnteilZaehler>1</ns2:AnteilZaehler>\n" +
				             "            <ns2:AnteilNenner>2</ns2:AnteilNenner>\n" +
				             "        </ns2:Quote>\n" +
				             "        <ns2:BelastetesGrundstueckIDREF>382929efa218</ns2:BelastetesGrundstueckIDREF>\n" +
				             "        <ns2:NatuerlichePersonGb>\n" +
				             "            <ns2:Rechtsgruende>\n" +
				             "                <ns2:AmtNummer>6</ns2:AmtNummer>\n" +
				             "                <ns2:RechtsgrundCode>\n" +
				             "                    <ns2:TextFr>Achat</ns2:TextFr>\n" +
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
	}

	/**
	 * Ce test vérifie que des mutations de création de communauté sont bien créées lorsque les droits s'appliquent sur une communauté
	 */
	@Test
	public void testNouveauxDroitsSurCommunaute() throws Exception {

		// un mock de DAO avec un import du registre foncier
		final EvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO() {
			@Override
			public EvenementRFImport get(Long id) {
				final EvenementRFImport imp = new EvenementRFImport();
				imp.setId(IMPORT_ID);
				return imp;
			}
		};

		// un mock qui mémorise toutes les mutations sauvées
		final EvenementRFMutationDAO evenementRFMutationDAO = new MockEvenementRFMutationDAO();

		final DataRFMutationsDetector detector = new DataRFMutationsDetector(xmlHelperRF, immeubleRFDAO, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, cacheDroits);

		final String idRfPP1 = "029191d4fec44";
		final String idRfPP2 = "37838sc9d94de";
		final String idRfImmeuble = "382929efa218";
		final String idRFCommunaute = "72828ce8f830a";

		// on envoie trois nouveaux droits sur deux propriétaires et une communaué qui concernent un immeuble
		final PersonEigentumAnteil droit1 = newDroitPP("9a9c9e94923", idRfPP1, idRfImmeuble, new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Héritage");
		droit1.getNatuerlichePersonGb().setGemeinschatIDREF(idRFCommunaute);
		final PersonEigentumAnteil droit2 = newDroitPP("45729cd9e20", idRfPP2, idRfImmeuble, new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Héritage");
		droit2.getNatuerlichePersonGb().setGemeinschatIDREF(idRFCommunaute);
		final PersonEigentumAnteil droit3 = newDroitColl("38458fa0ac3", idRFCommunaute, idRfImmeuble, GemeinschaftsArt.ERBENGEMEINSCHAFT, new Fraction(1, 1), PersonEigentumsform.ALLEINEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Héritage");

		List<PersonEigentumAnteil> droits = Arrays.asList(droit1, droit2, droit3);
		detector.processDroits(IMPORT_ID, 2, droits.iterator());

		// on devrait avoir 4 événements de mutation de type CREATION :
		//  - 3 pour chacun droits (2 propriétaires pp + 1 propriétaire communauté)
		//  - 1 pour la communauté (ayant-droit)
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(4, mutations.size());
		Collections.sort(mutations, (o1, o2) -> o1.getIdRF().compareTo(o2.getIdRF()));

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut0.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut0.getTypeMutation());
		assertEquals(idRfPP1, mut0.getIdRF());  // le premier propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<ns2:PersonEigentumAnteilList xmlns:ns2=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil MasterID=\"9a9c9e94923\">\n" +
				             "        <ns2:Quote>\n" +
				             "            <ns2:AnteilZaehler>1</ns2:AnteilZaehler>\n" +
				             "            <ns2:AnteilNenner>2</ns2:AnteilNenner>\n" +
				             "        </ns2:Quote>\n" +
				             "        <ns2:BelastetesGrundstueckIDREF>382929efa218</ns2:BelastetesGrundstueckIDREF>\n" +
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
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut1.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut1.getTypeMutation());
		assertEquals(idRfPP2, mut1.getIdRF());  // le deuxième propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<ns2:PersonEigentumAnteilList xmlns:ns2=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil MasterID=\"45729cd9e20\">\n" +
				             "        <ns2:Quote>\n" +
				             "            <ns2:AnteilZaehler>1</ns2:AnteilZaehler>\n" +
				             "            <ns2:AnteilNenner>2</ns2:AnteilNenner>\n" +
				             "        </ns2:Quote>\n" +
				             "        <ns2:BelastetesGrundstueckIDREF>382929efa218</ns2:BelastetesGrundstueckIDREF>\n" +
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
		assertEquals(IMPORT_ID, mut2.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut2.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut2.getTypeMutation());
		assertEquals(idRFCommunaute, mut2.getIdRF());  // le troisième propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<ns2:PersonEigentumAnteilList xmlns:ns2=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil MasterID=\"38458fa0ac3\">\n" +
				             "        <ns2:Quote>\n" +
				             "            <ns2:AnteilZaehler>1</ns2:AnteilZaehler>\n" +
				             "            <ns2:AnteilNenner>1</ns2:AnteilNenner>\n" +
				             "        </ns2:Quote>\n" +
				             "        <ns2:BelastetesGrundstueckIDREF>382929efa218</ns2:BelastetesGrundstueckIDREF>\n" +
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

		final EvenementRFMutation mut3 = mutations.get(3);
		assertEquals(IMPORT_ID, mut3.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut3.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.AYANT_DROIT, mut3.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut3.getTypeMutation());
		assertEquals(idRFCommunaute, mut3.getIdRF());  // le communauté elle-même
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
				             "</Gemeinschaft>\n", mut3.getXmlContent());
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées si les droits existent dans la base de données mais pas avec les mêmes valeurs.
	 */
	@Test
	public void testDroitsModifies() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 6, 1);

		// les données déjà existantes dans le DB
		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("37838sc9d94de");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setIdRF("029191d4fec44");

		final BienFondRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("382929efa218");

		final BienFondRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("202930c0e0f3");

		final DroitProprietePersonnePhysiqueRF droitPP1 = new DroitProprietePersonnePhysiqueRF();
		droitPP1.setMasterIdRF("9a9c9e94923");
		droitPP1.setAyantDroit(pp1);
		droitPP1.setImmeuble(immeuble1);
		droitPP1.setCommunaute(null);
		droitPP1.setDateDebut(dateImportInitial);
		droitPP1.setMotifDebut("Achat");
		droitPP1.setDateFin(null);
		droitPP1.setMotifFin(null);
		droitPP1.setDateDebutOfficielle(RegDate.get(2010, 4, 23));
		droitPP1.setNumeroAffaire(new IdentifiantAffaireRF(6, 2013, 33, 1));
		droitPP1.setPart(new Fraction(1, 2));
		droitPP1.setRegime(GenrePropriete.COPROPRIETE);
		pp1.addDroit(droitPP1);

		final DroitProprietePersonnePhysiqueRF droitPP2 = new DroitProprietePersonnePhysiqueRF();
		droitPP2.setMasterIdRF("45729cd9e20");
		droitPP2.setAyantDroit(pp2);
		droitPP2.setImmeuble(immeuble1);
		droitPP2.setCommunaute(null);
		droitPP2.setDateDebut(dateImportInitial);
		droitPP2.setMotifDebut("Achat");
		droitPP2.setDateFin(null);
		droitPP2.setMotifFin(null);
		droitPP2.setDateDebutOfficielle(RegDate.get(2010, 4, 23));
		droitPP2.setNumeroAffaire(new IdentifiantAffaireRF(6, 2013, 33, 1));
		droitPP2.setPart(new Fraction(1, 2));
		droitPP2.setRegime(GenrePropriete.COPROPRIETE);
		pp2.addDroit(droitPP2);

		final DroitProprietePersonnePhysiqueRF droitPP3 = new DroitProprietePersonnePhysiqueRF();
		droitPP3.setMasterIdRF("38458fa0ac3");
		droitPP3.setAyantDroit(pp2);
		droitPP3.setImmeuble(immeuble2);
		droitPP3.setCommunaute(null);
		droitPP3.setDateDebut(dateImportInitial);
		droitPP3.setMotifDebut("Achat");
		droitPP3.setDateFin(null);
		droitPP3.setMotifFin(null);
		droitPP3.setDateDebutOfficielle(RegDate.get(2010, 3, 28));
		droitPP3.setNumeroAffaire(new IdentifiantAffaireRF(6, 2013, 28, 4));
		droitPP3.setPart(new Fraction(1, 1));
		droitPP3.setRegime(GenrePropriete.INDIVIDUELLE);
		pp2.addDroit(droitPP3);

		// un mock de DAO qui simule l'existence d'un immeuble
		immeubleRFDAO = new MockImmeubleRFDAO() {
			@Nullable
			@Override
			public ImmeubleRF find(@NotNull ImmeubleRFKey key) {
				if (key.getIdRF().equals(immeuble1.getIdRF())) {
					return immeuble1;
				}
				else if (key.getIdRF().equals(immeuble2.getIdRF())) {
					return immeuble2;
				}
				return null;
			}
		};

		// un mock avec les deux ayants-droits.
		ayantDroitRFDAO.save(pp1);
		ayantDroitRFDAO.save(pp2);

		// un mock de DAO avec un import du registre foncier
		final EvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO() {
			@Override
			public EvenementRFImport get(Long id) {
				final EvenementRFImport imp = new EvenementRFImport();
				imp.setId(IMPORT_ID);
				return imp;
			}
		};

		// un mock qui mémorise toutes les mutations sauvées
		final EvenementRFMutationDAO evenementRFMutationDAO = new MockEvenementRFMutationDAO();

		final DataRFMutationsDetector detector = new DataRFMutationsDetector(xmlHelperRF, immeubleRFDAO, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, cacheDroits);

		// on envoie trois droits différents sur les mêmes propriétaires et immeubles
		//  - part différente
		final PersonEigentumAnteil droit1 = newDroitPP("9a9c9e94923", "37838sc9d94de", "382929efa218", new Fraction(2, 5), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		//  - motif différent
		final PersonEigentumAnteil droit2 = newDroitPP("45729cd9e20", "029191d4fec44", "382929efa218", new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Vol autorisé");
		//  - type de propriété différent
		final PersonEigentumAnteil droit3 = newDroitPP("38458fa0ac3", "029191d4fec44", "202930c0e0f3", new Fraction(1, 1), PersonEigentumsform.GESAMTEIGENTUM, RegDate.get(2010, 3, 28), new IdentifiantAffaireRF(6, 2013, 28, 4), "Achat");

		List<PersonEigentumAnteil> droits = Arrays.asList(droit1, droit2, droit3);
		detector.processDroits(IMPORT_ID, 2, droits.iterator());

		// on devrait avoir deux événements de mutation de type MODIFICATION sur chacun des propriétaires
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		Collections.sort(mutations, (o1, o2) -> o1.getIdRF().compareTo(o2.getIdRF()));

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut0.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.MODIFICATION, mut0.getTypeMutation());
		assertEquals("029191d4fec44", mut0.getIdRF());  // le premier propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<ns2:PersonEigentumAnteilList xmlns:ns2=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil MasterID=\"45729cd9e20\">\n" +
				             "        <ns2:Quote>\n" +
				             "            <ns2:AnteilZaehler>1</ns2:AnteilZaehler>\n" +
				             "            <ns2:AnteilNenner>2</ns2:AnteilNenner>\n" +
				             "        </ns2:Quote>\n" +
				             "        <ns2:BelastetesGrundstueckIDREF>382929efa218</ns2:BelastetesGrundstueckIDREF>\n" +
				             "        <ns2:NatuerlichePersonGb>\n" +
				             "            <ns2:Rechtsgruende>\n" +
				             "                <ns2:AmtNummer>6</ns2:AmtNummer>\n" +
				             "                <ns2:RechtsgrundCode>\n" +
				             "                    <ns2:TextFr>Vol autorisé</ns2:TextFr>\n" +
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
				             "    <PersonEigentumAnteil MasterID=\"38458fa0ac3\">\n" +
				             "        <ns2:Quote>\n" +
				             "            <ns2:AnteilZaehler>1</ns2:AnteilZaehler>\n" +
				             "            <ns2:AnteilNenner>1</ns2:AnteilNenner>\n" +
				             "        </ns2:Quote>\n" +
				             "        <ns2:BelastetesGrundstueckIDREF>202930c0e0f3</ns2:BelastetesGrundstueckIDREF>\n" +
				             "        <ns2:NatuerlichePersonGb>\n" +
				             "            <ns2:Rechtsgruende>\n" +
				             "                <ns2:AmtNummer>6</ns2:AmtNummer>\n" +
				             "                <ns2:RechtsgrundCode>\n" +
				             "                    <ns2:TextFr>Achat</ns2:TextFr>\n" +
				             "                </ns2:RechtsgrundCode>\n" +
				             "                <ns2:BelegDatum>2010-03-28</ns2:BelegDatum>\n" +
				             "                <ns2:BelegJahr>2013</ns2:BelegJahr>\n" +
				             "                <ns2:BelegNummer>28</ns2:BelegNummer>\n" +
				             "                <ns2:BelegNummerIndex>4</ns2:BelegNummerIndex>\n" +
				             "            </ns2:Rechtsgruende>\n" +
				             "            <ns2:PersonstammIDREF>029191d4fec44</ns2:PersonstammIDREF>\n" +
				             "        </ns2:NatuerlichePersonGb>\n" +
				             "        <ns2:PersonEigentumsForm>gesamteigentum</ns2:PersonEigentumsForm>\n" +
				             "    </PersonEigentumAnteil>\n" +
				             "</ns2:PersonEigentumAnteilList>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut1.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.MODIFICATION, mut1.getTypeMutation());
		assertEquals("37838sc9d94de", mut1.getIdRF());  // le second propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<ns2:PersonEigentumAnteilList xmlns:ns2=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil MasterID=\"9a9c9e94923\">\n" +
				             "        <ns2:Quote>\n" +
				             "            <ns2:AnteilZaehler>2</ns2:AnteilZaehler>\n" +
				             "            <ns2:AnteilNenner>5</ns2:AnteilNenner>\n" +
				             "        </ns2:Quote>\n" +
				             "        <ns2:BelastetesGrundstueckIDREF>382929efa218</ns2:BelastetesGrundstueckIDREF>\n" +
				             "        <ns2:NatuerlichePersonGb>\n" +
				             "            <ns2:Rechtsgruende>\n" +
				             "                <ns2:AmtNummer>6</ns2:AmtNummer>\n" +
				             "                <ns2:RechtsgrundCode>\n" +
				             "                    <ns2:TextFr>Achat</ns2:TextFr>\n" +
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
	}

	/**
	 * Ce test vérifie qu'aucune mutation n'est créée si les données des immeubles dans l'import sont identiques avec l'état courant des immeubles stockés dans la DB.
	 */
	@Test
	public void testDroitsIdentiques() throws Exception {


		final RegDate dateImportInitial = RegDate.get(2010, 6, 1);

		// les données déjà existantes dans le DB
		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("37838sc9d94de");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setIdRF("029191d4fec44");

		final BienFondRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("382929efa218");

		final BienFondRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("202930c0e0f3");

		final DroitProprietePersonnePhysiqueRF droitPP1 = new DroitProprietePersonnePhysiqueRF();
		droitPP1.setMasterIdRF("9a9c9e94923");
		droitPP1.setAyantDroit(pp1);
		droitPP1.setImmeuble(immeuble1);
		droitPP1.setCommunaute(null);
		droitPP1.setDateDebut(dateImportInitial);
		droitPP1.setMotifDebut("Achat");
		droitPP1.setDateFin(null);
		droitPP1.setMotifFin(null);
		droitPP1.setDateDebutOfficielle(RegDate.get(2010, 4, 23));
		droitPP1.setNumeroAffaire(new IdentifiantAffaireRF(6, 2013, 33, 1));
		droitPP1.setPart(new Fraction(1, 2));
		droitPP1.setRegime(GenrePropriete.COPROPRIETE);
		pp1.addDroit(droitPP1);

		final DroitProprietePersonnePhysiqueRF droitPP2 = new DroitProprietePersonnePhysiqueRF();
		droitPP2.setMasterIdRF("45729cd9e20");
		droitPP2.setAyantDroit(pp2);
		droitPP2.setImmeuble(immeuble1);
		droitPP2.setCommunaute(null);
		droitPP2.setDateDebut(dateImportInitial);
		droitPP2.setMotifDebut("Achat");
		droitPP2.setDateFin(null);
		droitPP2.setMotifFin(null);
		droitPP2.setDateDebutOfficielle(RegDate.get(2010, 4, 23));
		droitPP2.setNumeroAffaire(new IdentifiantAffaireRF(6, 2013, 33, 1));
		droitPP2.setPart(new Fraction(1, 2));
		droitPP2.setRegime(GenrePropriete.COPROPRIETE);
		pp2.addDroit(droitPP2);

		final DroitProprietePersonnePhysiqueRF droitPP3 = new DroitProprietePersonnePhysiqueRF();
		droitPP3.setMasterIdRF("38458fa0ac3");
		droitPP3.setAyantDroit(pp2);
		droitPP3.setImmeuble(immeuble2);
		droitPP3.setCommunaute(null);
		droitPP3.setDateDebut(dateImportInitial);
		droitPP3.setMotifDebut("Achat");
		droitPP3.setDateFin(null);
		droitPP3.setMotifFin(null);
		droitPP3.setDateDebutOfficielle(RegDate.get(2010, 3, 28));
		droitPP3.setNumeroAffaire(new IdentifiantAffaireRF(6, 2013, 28, 4));
		droitPP3.setPart(new Fraction(1, 1));
		droitPP3.setRegime(GenrePropriete.INDIVIDUELLE);
		pp2.addDroit(droitPP3);

		// un mock de DAO qui simule l'existence d'un immeuble
		immeubleRFDAO = new MockImmeubleRFDAO() {
			@Nullable
			@Override
			public ImmeubleRF find(@NotNull ImmeubleRFKey key) {
				if (key.getIdRF().equals(immeuble1.getIdRF())) {
					return immeuble1;
				}
				else if (key.getIdRF().equals(immeuble2.getIdRF())) {
					return immeuble2;
				}
				return null;
			}
		};

		// un mock avec les deux ayants-droits.
		ayantDroitRFDAO.save(pp1);
		ayantDroitRFDAO.save(pp2);

		// un mock de DAO avec un import du registre foncier
		final EvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO() {
			@Override
			public EvenementRFImport get(Long id) {
				final EvenementRFImport imp = new EvenementRFImport();
				imp.setId(IMPORT_ID);
				return imp;
			}
		};

		// un mock qui mémorise toutes les mutations sauvées
		final EvenementRFMutationDAO evenementRFMutationDAO = new MockEvenementRFMutationDAO();

		final DataRFMutationsDetector detector = new DataRFMutationsDetector(xmlHelperRF, immeubleRFDAO, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, cacheDroits);

		// on envoie les trois mêmes droits sur les mêmes propriétaires et immeubles
		final PersonEigentumAnteil droit1 = newDroitPP("9a9c9e94923", "37838sc9d94de", "382929efa218", new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		final PersonEigentumAnteil droit2 = newDroitPP("45729cd9e20", "029191d4fec44", "382929efa218", new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		final PersonEigentumAnteil droit3 = newDroitPP("38458fa0ac3", "029191d4fec44", "202930c0e0f3", new Fraction(1, 1), PersonEigentumsform.ALLEINEIGENTUM, RegDate.get(2010, 3, 28), new IdentifiantAffaireRF(6, 2013, 28, 4), "Achat");

		List<PersonEigentumAnteil> droits = Arrays.asList(droit1, droit2, droit3);
		detector.processDroits(IMPORT_ID, 2, droits.iterator());

		// on ne devrait pas avoir de mutation
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(0, mutations.size());
	}

	/**
	 * Ce test vérifie que des mutations de suppression sont créées si des propriétaires avec des droits dans la DB n'en ont plus dans le fichier d'import.
	 */
	@Test
	public void testSuppressionDeDroits() throws Exception {


		final RegDate dateImportInitial = RegDate.get(2010, 6, 1);

		// les données déjà existantes dans le DB
		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("37838sc9d94de");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setIdRF("029191d4fec44");

		final BienFondRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("382929efa218");

		final BienFondRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("202930c0e0f3");

		final DroitProprietePersonnePhysiqueRF droitPP1 = new DroitProprietePersonnePhysiqueRF();
		droitPP1.setMasterIdRF("9a9c9e94923");
		droitPP1.setAyantDroit(pp1);
		droitPP1.setImmeuble(immeuble1);
		droitPP1.setCommunaute(null);
		droitPP1.setDateDebut(dateImportInitial);
		droitPP1.setMotifDebut("Achat");
		droitPP1.setDateFin(null);
		droitPP1.setMotifFin(null);
		droitPP1.setDateDebutOfficielle(RegDate.get(2010, 4, 23));
		droitPP1.setNumeroAffaire(new IdentifiantAffaireRF(6, 2013, 33, 1));
		droitPP1.setPart(new Fraction(1, 2));
		droitPP1.setRegime(GenrePropriete.COPROPRIETE);
		pp1.addDroit(droitPP1);

		final DroitProprietePersonnePhysiqueRF droitPP2 = new DroitProprietePersonnePhysiqueRF();
		droitPP2.setMasterIdRF("45729cd9e20");
		droitPP2.setAyantDroit(pp2);
		droitPP2.setImmeuble(immeuble1);
		droitPP2.setCommunaute(null);
		droitPP2.setDateDebut(dateImportInitial);
		droitPP2.setMotifDebut("Achat");
		droitPP2.setDateFin(null);
		droitPP2.setMotifFin(null);
		droitPP2.setDateDebutOfficielle(RegDate.get(2010, 4, 23));
		droitPP2.setNumeroAffaire(new IdentifiantAffaireRF(6, 2013, 33, 1));
		droitPP2.setPart(new Fraction(1, 2));
		droitPP2.setRegime(GenrePropriete.COPROPRIETE);
		pp2.addDroit(droitPP2);

		final DroitProprietePersonnePhysiqueRF droitPP3 = new DroitProprietePersonnePhysiqueRF();
		droitPP3.setMasterIdRF("38458fa0ac3");
		droitPP3.setAyantDroit(pp2);
		droitPP3.setImmeuble(immeuble2);
		droitPP3.setCommunaute(null);
		droitPP3.setDateDebut(dateImportInitial);
		droitPP3.setMotifDebut("Achat");
		droitPP3.setDateFin(null);
		droitPP3.setMotifFin(null);
		droitPP3.setDateDebutOfficielle(RegDate.get(2010, 3, 28));
		droitPP3.setNumeroAffaire(new IdentifiantAffaireRF(6, 2013, 28, 4));
		droitPP3.setPart(new Fraction(1, 1));
		droitPP3.setRegime(GenrePropriete.INDIVIDUELLE);
		pp2.addDroit(droitPP3);

		// un mock de DAO qui simule l'existence d'un immeuble
		immeubleRFDAO = new MockImmeubleRFDAO() {
			@Nullable
			@Override
			public ImmeubleRF find(@NotNull ImmeubleRFKey key) {
				if (key.getIdRF().equals(immeuble1.getIdRF())) {
					return immeuble1;
				}
				else if (key.getIdRF().equals(immeuble2.getIdRF())) {
					return immeuble2;
				}
				return null;
			}
		};

		// un mock avec les deux ayants-droits.
		ayantDroitRFDAO.save(pp1);
		ayantDroitRFDAO.save(pp2);

		// un mock de DAO avec un import du registre foncier
		final EvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO() {
			@Override
			public EvenementRFImport get(Long id) {
				final EvenementRFImport imp = new EvenementRFImport();
				imp.setId(IMPORT_ID);
				return imp;
			}
		};

		// un mock qui mémorise toutes les mutations sauvées
		final EvenementRFMutationDAO evenementRFMutationDAO = new MockEvenementRFMutationDAO();

		final DataRFMutationsDetector detector = new DataRFMutationsDetector(xmlHelperRF, immeubleRFDAO, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, cacheDroits);

		// on envoie une liste de droits vide
		detector.processDroits(IMPORT_ID, 2, Collections.<PersonEigentumAnteil>emptyList().iterator());

		// on devrait avoir deux événements de mutation de type SUPPRESSION sur chacun des propriétaires
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		Collections.sort(mutations, (o1, o2) -> o1.getIdRF().compareTo(o2.getIdRF()));

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut0.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.SUPPRESSION, mut0.getTypeMutation());
		assertEquals("029191d4fec44", mut0.getIdRF());  // le premier propriétaire
		assertNull(mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.DROIT, mut1.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.SUPPRESSION, mut1.getTypeMutation());
		assertEquals("37838sc9d94de", mut1.getIdRF());  // le second propriétaire
		assertNull(mut1.getXmlContent());
	}

	@NotNull
	private static PersonEigentumAnteil newDroitPP(String idRfDroit, String idRfPP, String idRfImmeuble, Fraction part, PersonEigentumsform typePropriete, RegDate dateDebutEffective, IdentifiantAffaireRF affaire, String motifDebut) {

		final Rechtsgrund recht = new Rechtsgrund();
		recht.setBelegDatum(dateDebutEffective);
		recht.setAmtNummer(affaire.getNumeroOffice());
		recht.setBelegJahr(affaire.getAnnee());
		recht.setBelegNummer(affaire.getNumero());
		recht.setBelegNummerIndex(affaire.getIndex());
		recht.setRechtsgrundCode(new CapiCode(null, motifDebut));

		final NatuerlichePersonGb natuerliche = new NatuerlichePersonGb();
		natuerliche.setPersonstammIDREF(idRfPP);
		natuerliche.getRechtsgruende().add(recht);

		final PersonEigentumAnteil droit = new PersonEigentumAnteil();
		droit.setMasterID(idRfDroit);
		droit.setNatuerlichePersonGb(natuerliche);
		droit.setBelastetesGrundstueckIDREF(idRfImmeuble);
		droit.setQuote(new Quote((long) part.getNumerateur(), (long) part.getDenominateur(), null, null));
		droit.setPersonEigentumsForm(typePropriete);

		return droit;
	}

	@NotNull
	private static PersonEigentumAnteil newDroitColl(String idRfDroit, String idRfColl, String idRfImmeuble, GemeinschaftsArt typeColl, Fraction part, PersonEigentumsform typePropriete, RegDate dateDebutEffective, IdentifiantAffaireRF affaire,
	                                                 String motifDebut) {

		final Rechtsgrund recht = new Rechtsgrund();
		recht.setBelegDatum(dateDebutEffective);
		recht.setAmtNummer(affaire.getNumeroOffice());
		recht.setBelegJahr(affaire.getAnnee());
		recht.setBelegNummer(affaire.getNumero());
		recht.setBelegNummerIndex(affaire.getIndex());
		recht.setRechtsgrundCode(new CapiCode(null, motifDebut));

		final Gemeinschaft gemeinschaft = new Gemeinschaft();
		gemeinschaft.setGemeinschatID(idRfColl);
		gemeinschaft.setArt(typeColl);
		gemeinschaft.getRechtsgruende().add(recht);

		final PersonEigentumAnteil droit = new PersonEigentumAnteil();
		droit.setMasterID(idRfDroit);
		droit.setGemeinschaft(gemeinschaft);
		droit.setBelastetesGrundstueckIDREF(idRfImmeuble);
		droit.setQuote(new Quote((long) part.getNumerateur(), (long) part.getDenominateur(), null, null));
		droit.setPersonEigentumsForm(typePropriete);

		return droit;
	}
}
