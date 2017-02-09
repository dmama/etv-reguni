package ch.vd.uniregctb.registrefoncier.dataimport.detector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.grundstueck.CapiCode;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.GemeinschaftsArt;
import ch.vd.capitastra.grundstueck.NatuerlichePersonGb;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.PersonEigentumsform;
import ch.vd.capitastra.grundstueck.Quote;
import ch.vd.capitastra.grundstueck.Rechtsgrund;
import ch.vd.capitastra.rechteregister.BelastetesGrundstueck;
import ch.vd.capitastra.rechteregister.Beleg;
import ch.vd.capitastra.rechteregister.DienstbarkeitDiscrete;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.cache.MockPersistentCache;
import ch.vd.uniregctb.cache.PersistentCache;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.UniregJUnit4Runner;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.MockEvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.MockEvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantDroitRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.MockAyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationComparator;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRFImpl;
import ch.vd.uniregctb.registrefoncier.dataimport.helper.ServitudesRFHelperTest;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.transaction.MockTransactionManager;

import static ch.vd.uniregctb.registrefoncier.dataimport.helper.ServitudesRFHelperTest.newNatuerlichePersonGb;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SuppressWarnings("Duplicates")
@RunWith(UniregJUnit4Runner.class)
public class DroitRFDetectorTest {

	private static final Long IMPORT_ID = 1L;
	private XmlHelperRF xmlHelperRF;
	private PlatformTransactionManager transactionManager;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private PersistentCache<ArrayList<PersonEigentumAnteil>> cacheDroits;
	private PersistentCache<ArrayList<DienstbarkeitDiscrete>> cacheServitudes;

	@Before
	public void setUp() throws Exception {
		xmlHelperRF = new XmlHelperRFImpl();
		transactionManager = new MockTransactionManager();
		ayantDroitRFDAO = new MockAyantDroitRFDAO();
		cacheDroits = new MockPersistentCache<>();
		cacheServitudes = new MockPersistentCache<>();
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

		final AyantDroitRFDetector ayantDroitRFDetector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits, cacheServitudes);

		// on envoie trois nouveaux droits sur deux propriétaires qui concernent deux immeubles
		final PersonEigentumAnteil droit1 = newDroitPP("9a9c9e94923", "37838sc9d94de", "382929efa218", new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		final PersonEigentumAnteil droit2 = newDroitPP("45729cd9e20", "029191d4fec44", "382929efa218", new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		final PersonEigentumAnteil droit3 = newDroitPP("38458fa0ac3", "029191d4fec44", "202930c0e0f3", new Fraction(1, 1), PersonEigentumsform.ALLEINEIGENTUM, RegDate.get(2010, 3, 28), new IdentifiantAffaireRF(6, 2013, 28, 4), "Achat");

		List<PersonEigentumAnteil> droits = Arrays.asList(droit1, droit2, droit3);
		detector.processDroitsPropriete(IMPORT_ID, 2, droits.iterator(), false, null);

		// on devrait avoir deux événements de mutation de type CREATION sur chacun des propriétaires
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		Collections.sort(mutations, new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
		assertEquals("029191d4fec44", mut0.getIdRF());  // le premier propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<PersonEigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil MasterID=\"45729cd9e20\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>2</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>382929efa218</BelastetesGrundstueckIDREF>\n" +
				             "        <NatuerlichePersonGb>\n" +
				             "            <Rechtsgruende>\n" +
				             "                <AmtNummer>6</AmtNummer>\n" +
				             "                <RechtsgrundCode>\n" +
				             "                    <TextFr>Achat</TextFr>\n" +
				             "                </RechtsgrundCode>\n" +
				             "                <BelegDatum>2010-04-23</BelegDatum>\n" +
				             "                <BelegAlt>2013/33/1</BelegAlt>\n" +
				             "            </Rechtsgruende>\n" +
				             "            <PersonstammIDREF>029191d4fec44</PersonstammIDREF>\n" +
				             "        </NatuerlichePersonGb>\n" +
				             "        <PersonEigentumsForm>miteigentum</PersonEigentumsForm>\n" +
				             "    </PersonEigentumAnteil>\n" +
				             "    <PersonEigentumAnteil MasterID=\"38458fa0ac3\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>1</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>202930c0e0f3</BelastetesGrundstueckIDREF>\n" +
				             "        <NatuerlichePersonGb>\n" +
				             "            <Rechtsgruende>\n" +
				             "                <AmtNummer>6</AmtNummer>\n" +
				             "                <RechtsgrundCode>\n" +
				             "                    <TextFr>Achat</TextFr>\n" +
				             "                </RechtsgrundCode>\n" +
				             "                <BelegDatum>2010-03-28</BelegDatum>\n" +
				             "                <BelegAlt>2013/28/4</BelegAlt>\n" +
				             "            </Rechtsgruende>\n" +
				             "            <PersonstammIDREF>029191d4fec44</PersonstammIDREF>\n" +
				             "        </NatuerlichePersonGb>\n" +
				             "        <PersonEigentumsForm>alleineigentum</PersonEigentumsForm>\n" +
				             "    </PersonEigentumAnteil>\n" +
				             "</PersonEigentumAnteilList>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
		assertEquals("37838sc9d94de", mut1.getIdRF());  // le second propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<PersonEigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil MasterID=\"9a9c9e94923\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>2</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>382929efa218</BelastetesGrundstueckIDREF>\n" +
				             "        <NatuerlichePersonGb>\n" +
				             "            <Rechtsgruende>\n" +
				             "                <AmtNummer>6</AmtNummer>\n" +
				             "                <RechtsgrundCode>\n" +
				             "                    <TextFr>Achat</TextFr>\n" +
				             "                </RechtsgrundCode>\n" +
				             "                <BelegDatum>2010-04-23</BelegDatum>\n" +
				             "                <BelegAlt>2013/33/1</BelegAlt>\n" +
				             "            </Rechtsgruende>\n" +
				             "            <PersonstammIDREF>37838sc9d94de</PersonstammIDREF>\n" +
				             "        </NatuerlichePersonGb>\n" +
				             "        <PersonEigentumsForm>miteigentum</PersonEigentumsForm>\n" +
				             "    </PersonEigentumAnteil>\n" +
				             "</PersonEigentumAnteilList>\n", mut1.getXmlContent());
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

		final AyantDroitRFDetector ayantDroitRFDetector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits, cacheServitudes);

		final String idRfPP1 = "029191d4fec44";
		final String idRfPP2 = "37838sc9d94de";
		final String idRfImmeuble = "382929efa218";
		final String idRFCommunaute = "72828ce8f830a";

		// on envoie trois nouveaux droits sur deux propriétaires et une communaué qui concernent un immeuble
		final PersonEigentumAnteil droit1 = newDroitPP("9a9c9e94923", idRfPP1, idRfImmeuble, new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Héritage");
		droit1.getNatuerlichePersonGb().setGemeinschatIDREF(idRFCommunaute);
		final PersonEigentumAnteil droit2 = newDroitPP("45729cd9e20", idRfPP2, idRfImmeuble, new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Héritage");
		droit2.getNatuerlichePersonGb().setGemeinschatIDREF(idRFCommunaute);
		final PersonEigentumAnteil droit3 =
				newDroitColl("38458fa0ac3", idRFCommunaute, idRfImmeuble, GemeinschaftsArt.ERBENGEMEINSCHAFT, new Fraction(1, 1), PersonEigentumsform.ALLEINEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Héritage");

		List<PersonEigentumAnteil> droits = Arrays.asList(droit1, droit2, droit3);
		detector.processDroitsPropriete(IMPORT_ID, 2, droits.iterator(), false, null);

		// on devrait avoir 4 événements de mutation de type CREATION :
		//  - 3 pour chacun droits (2 propriétaires pp + 1 propriétaire communauté)
		//  - 1 pour la communauté (ayant-droit)
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(4, mutations.size());
		Collections.sort(mutations, new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.AYANT_DROIT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
		assertEquals(idRFCommunaute, mut0.getIdRF());  // le communauté elle-même
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Gemeinschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <Rechtsgruende>\n" +
				             "        <AmtNummer>6</AmtNummer>\n" +
				             "        <RechtsgrundCode>\n" +
				             "            <TextFr>Héritage</TextFr>\n" +
				             "        </RechtsgrundCode>\n" +
				             "        <BelegDatum>2010-04-23</BelegDatum>\n" +
				             "        <BelegAlt>2013/33/1</BelegAlt>\n" +
				             "    </Rechtsgruende>\n" +
				             "    <GemeinschatID>72828ce8f830a</GemeinschatID>\n" +
				             "    <Art>Erbengemeinschaft</Art>\n" +
				             "</Gemeinschaft>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
		assertEquals(idRfPP1, mut1.getIdRF());  // le premier propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<PersonEigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil MasterID=\"9a9c9e94923\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>2</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>382929efa218</BelastetesGrundstueckIDREF>\n" +
				             "        <NatuerlichePersonGb>\n" +
				             "            <GemeinschatIDREF>72828ce8f830a</GemeinschatIDREF>\n" +
				             "            <Rechtsgruende>\n" +
				             "                <AmtNummer>6</AmtNummer>\n" +
				             "                <RechtsgrundCode>\n" +
				             "                    <TextFr>Héritage</TextFr>\n" +
				             "                </RechtsgrundCode>\n" +
				             "                <BelegDatum>2010-04-23</BelegDatum>\n" +
				             "                <BelegAlt>2013/33/1</BelegAlt>\n" +
				             "            </Rechtsgruende>\n" +
				             "            <PersonstammIDREF>029191d4fec44</PersonstammIDREF>\n" +
				             "        </NatuerlichePersonGb>\n" +
				             "        <PersonEigentumsForm>miteigentum</PersonEigentumsForm>\n" +
				             "    </PersonEigentumAnteil>\n" +
				             "</PersonEigentumAnteilList>\n", mut1.getXmlContent());

		final EvenementRFMutation mut2 = mutations.get(2);
		assertEquals(IMPORT_ID, mut2.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut2.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut2.getTypeMutation());
		assertEquals(idRfPP2, mut2.getIdRF());  // le deuxième propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<PersonEigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil MasterID=\"45729cd9e20\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>2</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>382929efa218</BelastetesGrundstueckIDREF>\n" +
				             "        <NatuerlichePersonGb>\n" +
				             "            <GemeinschatIDREF>72828ce8f830a</GemeinschatIDREF>\n" +
				             "            <Rechtsgruende>\n" +
				             "                <AmtNummer>6</AmtNummer>\n" +
				             "                <RechtsgrundCode>\n" +
				             "                    <TextFr>Héritage</TextFr>\n" +
				             "                </RechtsgrundCode>\n" +
				             "                <BelegDatum>2010-04-23</BelegDatum>\n" +
				             "                <BelegAlt>2013/33/1</BelegAlt>\n" +
				             "            </Rechtsgruende>\n" +
				             "            <PersonstammIDREF>37838sc9d94de</PersonstammIDREF>\n" +
				             "        </NatuerlichePersonGb>\n" +
				             "        <PersonEigentumsForm>miteigentum</PersonEigentumsForm>\n" +
				             "    </PersonEigentumAnteil>\n" +
				             "</PersonEigentumAnteilList>\n", mut2.getXmlContent());

		final EvenementRFMutation mut3 = mutations.get(3);
		assertEquals(IMPORT_ID, mut3.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut3.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut3.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut3.getTypeMutation());
		assertEquals(idRFCommunaute, mut3.getIdRF());  // le troisième propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<PersonEigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil MasterID=\"38458fa0ac3\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>1</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>382929efa218</BelastetesGrundstueckIDREF>\n" +
				             "        <Gemeinschaft>\n" +
				             "            <Rechtsgruende>\n" +
				             "                <AmtNummer>6</AmtNummer>\n" +
				             "                <RechtsgrundCode>\n" +
				             "                    <TextFr>Héritage</TextFr>\n" +
				             "                </RechtsgrundCode>\n" +
				             "                <BelegDatum>2010-04-23</BelegDatum>\n" +
				             "                <BelegAlt>2013/33/1</BelegAlt>\n" +
				             "            </Rechtsgruende>\n" +
				             "            <GemeinschatID>72828ce8f830a</GemeinschatID>\n" +
				             "            <Art>Erbengemeinschaft</Art>\n" +
				             "        </Gemeinschaft>\n" +
				             "        <PersonEigentumsForm>alleineigentum</PersonEigentumsForm>\n" +
				             "    </PersonEigentumAnteil>\n" +
				             "</PersonEigentumAnteilList>\n", mut3.getXmlContent());
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsque les servitudes reçues n'existent pas dans la base de données.
	 */
	@Test
	public void testNouvellesServitudes() throws Exception {

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

		final AyantDroitRFDetector ayantDroitRFDetector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits, cacheServitudes);

		// on envoie trois nouvelles servitudes pour deux bénéficiaires qui concernent deux immeubles
		final BelastetesGrundstueck grundstueck1 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final BelastetesGrundstueck grundstueck2 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe090827e1", null, null);
		final ch.vd.capitastra.rechteregister.NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final ch.vd.capitastra.rechteregister.NatuerlichePersonGb natuerlichePerson2 = newNatuerlichePersonGb("Anne-Lise", "Lassueur", "_1f109152380ffd8901380ffda8131c65");
		final DienstbarkeitDiscrete dienstbarkeit1 = ServitudesRFHelperTest.newDienstbarkeitDiscrete(grundstueck1, natuerlichePerson1,
		                                                                                             ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380ffed6694392",
		                                                                                                                                     "_1f109152380ffd8901380ffed6694392",
		                                                                                                                                     2005, 699, 8,
		                                                                                                                                     "Usufruit",
		                                                                                                                                     "2002/392", null,
		                                                                                                                                     RegDate.get(2002, 9, 2),
		                                                                                                                                     null),
		                                                                                             null);
		final DienstbarkeitDiscrete dienstbarkeit2 = ServitudesRFHelperTest.newDienstbarkeitDiscrete(grundstueck1, natuerlichePerson2,
		                                                                                             ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380ffed6694392",
		                                                                                                                                     "_1f109152380ffd8901380ffed6694392",
		                                                                                                                                     2005, 699, 8,
		                                                                                                                                     "Usufruit",
		                                                                                                                                     "2002/392", null,
		                                                                                                                                     RegDate.get(2002, 9, 2),
		                                                                                                                                     null),
		                                                                                             null);
		final DienstbarkeitDiscrete dienstbarkeit3 = ServitudesRFHelperTest.newDienstbarkeitDiscrete(grundstueck2, natuerlichePerson2,
		                                                                                             ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380fff10ca631e",
		                                                                                                                                     "_1f109152380ffd8901380fff10ca631e",
		                                                                                                                                     2007, 375, 8,
		                                                                                                                                     "Usufruit",
		                                                                                                                                     null, new Beleg(8, 2007, 266, 0),
		                                                                                                                                     RegDate.get(2007, 6, 25),
		                                                                                                                                     null),
		                                                                                             null);

		List<DienstbarkeitDiscrete> servitudes = Arrays.asList(dienstbarkeit1, dienstbarkeit2, dienstbarkeit3);
		detector.processServitudes(IMPORT_ID, 2, servitudes.iterator(), false, null);

		// on devrait avoir deux événements de mutation de type CREATION sur chacun des bénéficiaires
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		Collections.sort(mutations, new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
		assertEquals("_1f109152380ffd8901380ffda8131c65", mut0.getIdRF());  // le premier propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<DienstbarkeitDiscreteList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
				             "    <DienstbarkeitDiscretes>\n" +
				             "        <Dienstbarkeit MasterID=\"1f109152380ffd8901380ffed6694392\">\n" +
				             "            <StandardRechtID>_1f109152380ffd8901380ffed6694392</StandardRechtID>\n" +
				             "            <RechtEintragJahrID>2005</RechtEintragJahrID>\n" +
				             "            <RechtEintragNummerID>699</RechtEintragNummerID>\n" +
				             "            <AmtNummer>8</AmtNummer>\n" +
				             "            <Stichwort>\n" +
				             "                <TextFr>Usufruit</TextFr>\n" +
				             "            </Stichwort>\n" +
				             "            <BelegAlt>2002/392</BelegAlt>\n" +
				             "            <BeginDatum>2002-09-02</BeginDatum>\n" +
				             "        </Dienstbarkeit>\n" +
				             "        <BelastetesGrundstueck>\n" +
				             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe15bb729c</BelastetesGrundstueckIDREF>\n" +
				             "        </BelastetesGrundstueck>\n" +
				             "        <BerechtigtePerson>\n" +
				             "            <NatuerlichePersonGb>\n" +
				             "                <Name>Lassueur</Name>\n" +
				             "                <Vorname>Anne-Lise</Vorname>\n" +
				             "                <PersonstammIDREF>_1f109152380ffd8901380ffda8131c65</PersonstammIDREF>\n" +
				             "            </NatuerlichePersonGb>\n" +
				             "        </BerechtigtePerson>\n" +
				             "    </DienstbarkeitDiscretes>\n" +
				             "    <DienstbarkeitDiscretes>\n" +
				             "        <Dienstbarkeit MasterID=\"1f109152380ffd8901380fff10ca631e\">\n" +
				             "            <StandardRechtID>_1f109152380ffd8901380fff10ca631e</StandardRechtID>\n" +
				             "            <RechtEintragJahrID>2007</RechtEintragJahrID>\n" +
				             "            <RechtEintragNummerID>375</RechtEintragNummerID>\n" +
				             "            <AmtNummer>8</AmtNummer>\n" +
				             "            <Stichwort>\n" +
				             "                <TextFr>Usufruit</TextFr>\n" +
				             "            </Stichwort>\n" +
				             "            <Beleg>\n" +
				             "                <AmtNummer>8</AmtNummer>\n" +
				             "                <BelegJahr>2007</BelegJahr>\n" +
				             "                <BelegNummer>266</BelegNummer>\n" +
				             "                <BelegNummerIndex>0</BelegNummerIndex>\n" +
				             "            </Beleg>\n" +
				             "            <BeginDatum>2007-06-25</BeginDatum>\n" +
				             "        </Dienstbarkeit>\n" +
				             "        <BelastetesGrundstueck>\n" +
				             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe090827e1</BelastetesGrundstueckIDREF>\n" +
				             "        </BelastetesGrundstueck>\n" +
				             "        <BerechtigtePerson>\n" +
				             "            <NatuerlichePersonGb>\n" +
				             "                <Name>Lassueur</Name>\n" +
				             "                <Vorname>Anne-Lise</Vorname>\n" +
				             "                <PersonstammIDREF>_1f109152380ffd8901380ffda8131c65</PersonstammIDREF>\n" +
				             "            </NatuerlichePersonGb>\n" +
				             "        </BerechtigtePerson>\n" +
				             "    </DienstbarkeitDiscretes>\n" +
				             "</DienstbarkeitDiscreteList>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
		assertEquals("_1f109152380ffd8901380ffdabcc2441", mut1.getIdRF());  // le second propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<DienstbarkeitDiscreteList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
				             "    <DienstbarkeitDiscretes>\n" +
				             "        <Dienstbarkeit MasterID=\"1f109152380ffd8901380ffed6694392\">\n" +
				             "            <StandardRechtID>_1f109152380ffd8901380ffed6694392</StandardRechtID>\n" +
				             "            <RechtEintragJahrID>2005</RechtEintragJahrID>\n" +
				             "            <RechtEintragNummerID>699</RechtEintragNummerID>\n" +
				             "            <AmtNummer>8</AmtNummer>\n" +
				             "            <Stichwort>\n" +
				             "                <TextFr>Usufruit</TextFr>\n" +
				             "            </Stichwort>\n" +
				             "            <BelegAlt>2002/392</BelegAlt>\n" +
				             "            <BeginDatum>2002-09-02</BeginDatum>\n" +
				             "        </Dienstbarkeit>\n" +
				             "        <BelastetesGrundstueck>\n" +
				             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe15bb729c</BelastetesGrundstueckIDREF>\n" +
				             "        </BelastetesGrundstueck>\n" +
				             "        <BerechtigtePerson>\n" +
				             "            <NatuerlichePersonGb>\n" +
				             "                <Name>Gaillard</Name>\n" +
				             "                <Vorname>Roger</Vorname>\n" +
				             "                <PersonstammIDREF>_1f109152380ffd8901380ffdabcc2441</PersonstammIDREF>\n" +
				             "            </NatuerlichePersonGb>\n" +
				             "        </BerechtigtePerson>\n" +
				             "    </DienstbarkeitDiscretes>\n" +
				             "</DienstbarkeitDiscreteList>\n", mut1.getXmlContent());
	}

	/**
	 * Ce test vérifie que des mutations de création de communauté sont bien créées lorsque les servitudes s'appliquent sur une communauté
	 */
	@Test
	public void testNouvellesServitudesSurCommunaute() throws Exception {

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

		final AyantDroitRFDetector ayantDroitRFDetector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits, cacheServitudes);

		// on envoie deux nouvelles servitudes pour deux bénéficiaires en communauté
		final BelastetesGrundstueck grundstueck1 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final ch.vd.capitastra.rechteregister.NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final ch.vd.capitastra.rechteregister.NatuerlichePersonGb natuerlichePerson2 = newNatuerlichePersonGb("Anne-Lise", "Lassueur", "_1f109152380ffd8901380ffda8131c65");
		final List<ch.vd.capitastra.rechteregister.NatuerlichePersonGb> gemienschaft = Arrays.asList(natuerlichePerson1, natuerlichePerson2);
		final DienstbarkeitDiscrete dienstbarkeit1 = ServitudesRFHelperTest.newDienstbarkeitDiscrete(grundstueck1, natuerlichePerson1,
		                                                                                             ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380ffed6694392",
		                                                                                                                                     "_1f109152380ffd8901380ffed6694392",
		                                                                                                                                     2005, 699, 8,
		                                                                                                                                     "Usufruit",
		                                                                                                                                     "2002/392", null,
		                                                                                                                                     RegDate.get(2002, 9, 2),
		                                                                                                                                     null),
		                                                                                             gemienschaft);
		final DienstbarkeitDiscrete dienstbarkeit2 = ServitudesRFHelperTest.newDienstbarkeitDiscrete(grundstueck1, natuerlichePerson2,
		                                                                                             ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380ffed6694392",
		                                                                                                                                     "_1f109152380ffd8901380ffed6694392",
		                                                                                                                                     2005, 699, 8,
		                                                                                                                                     "Usufruit",
		                                                                                                                                     "2002/392", null,
		                                                                                                                                     RegDate.get(2002, 9, 2),
		                                                                                                                                     null),
		                                                                                             gemienschaft);

		List<DienstbarkeitDiscrete> servitudes = Arrays.asList(dienstbarkeit1, dienstbarkeit2);
		detector.processServitudes(IMPORT_ID, 2, servitudes.iterator(), false, null);

		// on devrait avoir deux événements de mutation de type CREATION sur chacun des bénéficiaires + 1 pour la communauté
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(3, mutations.size());
		Collections.sort(mutations, new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.AYANT_DROIT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
		assertEquals("_1f109152380ffd8901380ffed6694392", mut0.getIdRF());  // la communauté
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Gemeinschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GemeinschatID>_1f109152380ffd8901380ffed6694392</GemeinschatID>\n" +
				             "    <Art>Gemeinderschaft</Art>\n" +
				             "</Gemeinschaft>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
		assertEquals("_1f109152380ffd8901380ffda8131c65", mut1.getIdRF());  // le premier propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<DienstbarkeitDiscreteList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
				             "    <DienstbarkeitDiscretes>\n" +
				             "        <Dienstbarkeit MasterID=\"1f109152380ffd8901380ffed6694392\">\n" +
				             "            <StandardRechtID>_1f109152380ffd8901380ffed6694392</StandardRechtID>\n" +
				             "            <RechtEintragJahrID>2005</RechtEintragJahrID>\n" +
				             "            <RechtEintragNummerID>699</RechtEintragNummerID>\n" +
				             "            <AmtNummer>8</AmtNummer>\n" +
				             "            <Stichwort>\n" +
				             "                <TextFr>Usufruit</TextFr>\n" +
				             "            </Stichwort>\n" +
				             "            <BelegAlt>2002/392</BelegAlt>\n" +
				             "            <BeginDatum>2002-09-02</BeginDatum>\n" +
				             "        </Dienstbarkeit>\n" +
				             "        <BelastetesGrundstueck>\n" +
				             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe15bb729c</BelastetesGrundstueckIDREF>\n" +
				             "        </BelastetesGrundstueck>\n" +
				             "        <BerechtigtePerson>\n" +
				             "            <NatuerlichePersonGb>\n" +
				             "                <Name>Lassueur</Name>\n" +
				             "                <Vorname>Anne-Lise</Vorname>\n" +
				             "                <PersonstammIDREF>_1f109152380ffd8901380ffda8131c65</PersonstammIDREF>\n" +
				             "            </NatuerlichePersonGb>\n" +
				             "        </BerechtigtePerson>\n" +
				             "        <Gemeinschaft>\n" +
				             "            <NatuerlichePersonGb>\n" +
				             "                <Name>Gaillard</Name>\n" +
				             "                <Vorname>Roger</Vorname>\n" +
				             "                <PersonstammIDREF>_1f109152380ffd8901380ffdabcc2441</PersonstammIDREF>\n" +
				             "            </NatuerlichePersonGb>\n" +
				             "        </Gemeinschaft>\n" +
				             "        <Gemeinschaft>\n" +
				             "            <NatuerlichePersonGb>\n" +
				             "                <Name>Lassueur</Name>\n" +
				             "                <Vorname>Anne-Lise</Vorname>\n" +
				             "                <PersonstammIDREF>_1f109152380ffd8901380ffda8131c65</PersonstammIDREF>\n" +
				             "            </NatuerlichePersonGb>\n" +
				             "        </Gemeinschaft>\n" +
				             "    </DienstbarkeitDiscretes>\n" +
				             "</DienstbarkeitDiscreteList>\n", mut1.getXmlContent());

		final EvenementRFMutation mut2 = mutations.get(2);
		assertEquals(IMPORT_ID, mut2.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut2.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut2.getTypeMutation());
		assertEquals("_1f109152380ffd8901380ffdabcc2441", mut2.getIdRF());  // le second propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<DienstbarkeitDiscreteList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
				             "    <DienstbarkeitDiscretes>\n" +
				             "        <Dienstbarkeit MasterID=\"1f109152380ffd8901380ffed6694392\">\n" +
				             "            <StandardRechtID>_1f109152380ffd8901380ffed6694392</StandardRechtID>\n" +
				             "            <RechtEintragJahrID>2005</RechtEintragJahrID>\n" +
				             "            <RechtEintragNummerID>699</RechtEintragNummerID>\n" +
				             "            <AmtNummer>8</AmtNummer>\n" +
				             "            <Stichwort>\n" +
				             "                <TextFr>Usufruit</TextFr>\n" +
				             "            </Stichwort>\n" +
				             "            <BelegAlt>2002/392</BelegAlt>\n" +
				             "            <BeginDatum>2002-09-02</BeginDatum>\n" +
				             "        </Dienstbarkeit>\n" +
				             "        <BelastetesGrundstueck>\n" +
				             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe15bb729c</BelastetesGrundstueckIDREF>\n" +
				             "        </BelastetesGrundstueck>\n" +
				             "        <BerechtigtePerson>\n" +
				             "            <NatuerlichePersonGb>\n" +
				             "                <Name>Gaillard</Name>\n" +
				             "                <Vorname>Roger</Vorname>\n" +
				             "                <PersonstammIDREF>_1f109152380ffd8901380ffdabcc2441</PersonstammIDREF>\n" +
				             "            </NatuerlichePersonGb>\n" +
				             "        </BerechtigtePerson>\n" +
				             "        <Gemeinschaft>\n" +
				             "            <NatuerlichePersonGb>\n" +
				             "                <Name>Gaillard</Name>\n" +
				             "                <Vorname>Roger</Vorname>\n" +
				             "                <PersonstammIDREF>_1f109152380ffd8901380ffdabcc2441</PersonstammIDREF>\n" +
				             "            </NatuerlichePersonGb>\n" +
				             "        </Gemeinschaft>\n" +
				             "        <Gemeinschaft>\n" +
				             "            <NatuerlichePersonGb>\n" +
				             "                <Name>Lassueur</Name>\n" +
				             "                <Vorname>Anne-Lise</Vorname>\n" +
				             "                <PersonstammIDREF>_1f109152380ffd8901380ffda8131c65</PersonstammIDREF>\n" +
				             "            </NatuerlichePersonGb>\n" +
				             "        </Gemeinschaft>\n" +
				             "    </DienstbarkeitDiscretes>\n" +
				             "</DienstbarkeitDiscreteList>\n", mut2.getXmlContent());
	}

	/**
	 * [SIFISC-22366] Ce test vérifie que les droits sur des immeubles blacklistés sont bien ignorés.
	 */
	@Test
	public void testNouveauxDroitsSurImmeubleBlackliste() throws Exception {

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

		final AyantDroitRFDetector ayantDroitRFDetector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits, cacheServitudes);

		// on envoie un nouveau droit qui concerne un immeuble blacklisté
		final PersonEigentumAnteil droit1 = newDroitPP("9a9c9e94923", "37838sc9d94de", "_1f1091523810108101381012b3d64cb4", new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");

		List<PersonEigentumAnteil> droits = Collections.singletonList(droit1);
		detector.processDroitsPropriete(IMPORT_ID, 2, droits.iterator(), false, null);

		// on ne devrait pas avoir de mutations
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(0, mutations.size());
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
		droitPP1.setDateDebutMetier(RegDate.get(2010, 4, 23));
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
		droitPP2.setDateDebutMetier(RegDate.get(2010, 4, 23));
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
		droitPP3.setDateDebutMetier(RegDate.get(2010, 3, 28));
		droitPP3.setNumeroAffaire(new IdentifiantAffaireRF(6, 2013, 28, 4));
		droitPP3.setPart(new Fraction(1, 1));
		droitPP3.setRegime(GenrePropriete.INDIVIDUELLE);
		pp2.addDroit(droitPP3);

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

		final AyantDroitRFDetector ayantDroitRFDetector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits, cacheServitudes);

		// on envoie trois droits différents sur les mêmes propriétaires et immeubles
		//  - part différente
		final PersonEigentumAnteil droit1 = newDroitPP("9a9c9e94923", "37838sc9d94de", "382929efa218", new Fraction(2, 5), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		//  - motif différent
		final PersonEigentumAnteil droit2 = newDroitPP("45729cd9e20", "029191d4fec44", "382929efa218", new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Vol autorisé");
		//  - type de propriété différent
		final PersonEigentumAnteil droit3 = newDroitPP("38458fa0ac3", "029191d4fec44", "202930c0e0f3", new Fraction(1, 1), PersonEigentumsform.GESAMTEIGENTUM, RegDate.get(2010, 3, 28), new IdentifiantAffaireRF(6, 2013, 28, 4), "Achat");

		List<PersonEigentumAnteil> droits = Arrays.asList(droit1, droit2, droit3);
		detector.processDroitsPropriete(IMPORT_ID, 2, droits.iterator(), false, null);

		// on devrait avoir deux événements de mutation de type MODIFICATION sur chacun des propriétaires
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		Collections.sort(mutations, new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
		assertEquals("029191d4fec44", mut0.getIdRF());  // le premier propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<PersonEigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil MasterID=\"45729cd9e20\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>2</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>382929efa218</BelastetesGrundstueckIDREF>\n" +
				             "        <NatuerlichePersonGb>\n" +
				             "            <Rechtsgruende>\n" +
				             "                <AmtNummer>6</AmtNummer>\n" +
				             "                <RechtsgrundCode>\n" +
				             "                    <TextFr>Vol autorisé</TextFr>\n" +
				             "                </RechtsgrundCode>\n" +
				             "                <BelegDatum>2010-04-23</BelegDatum>\n" +
				             "                <BelegAlt>2013/33/1</BelegAlt>\n" +
				             "            </Rechtsgruende>\n" +
				             "            <PersonstammIDREF>029191d4fec44</PersonstammIDREF>\n" +
				             "        </NatuerlichePersonGb>\n" +
				             "        <PersonEigentumsForm>miteigentum</PersonEigentumsForm>\n" +
				             "    </PersonEigentumAnteil>\n" +
				             "    <PersonEigentumAnteil MasterID=\"38458fa0ac3\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>1</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>202930c0e0f3</BelastetesGrundstueckIDREF>\n" +
				             "        <NatuerlichePersonGb>\n" +
				             "            <Rechtsgruende>\n" +
				             "                <AmtNummer>6</AmtNummer>\n" +
				             "                <RechtsgrundCode>\n" +
				             "                    <TextFr>Achat</TextFr>\n" +
				             "                </RechtsgrundCode>\n" +
				             "                <BelegDatum>2010-03-28</BelegDatum>\n" +
				             "                <BelegAlt>2013/28/4</BelegAlt>\n" +
				             "            </Rechtsgruende>\n" +
				             "            <PersonstammIDREF>029191d4fec44</PersonstammIDREF>\n" +
				             "        </NatuerlichePersonGb>\n" +
				             "        <PersonEigentumsForm>gesamteigentum</PersonEigentumsForm>\n" +
				             "    </PersonEigentumAnteil>\n" +
				             "</PersonEigentumAnteilList>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut1.getTypeMutation());
		assertEquals("37838sc9d94de", mut1.getIdRF());  // le second propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<PersonEigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil MasterID=\"9a9c9e94923\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>2</AnteilZaehler>\n" +
				             "            <AnteilNenner>5</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>382929efa218</BelastetesGrundstueckIDREF>\n" +
				             "        <NatuerlichePersonGb>\n" +
				             "            <Rechtsgruende>\n" +
				             "                <AmtNummer>6</AmtNummer>\n" +
				             "                <RechtsgrundCode>\n" +
				             "                    <TextFr>Achat</TextFr>\n" +
				             "                </RechtsgrundCode>\n" +
				             "                <BelegDatum>2010-04-23</BelegDatum>\n" +
				             "                <BelegAlt>2013/33/1</BelegAlt>\n" +
				             "            </Rechtsgruende>\n" +
				             "            <PersonstammIDREF>37838sc9d94de</PersonstammIDREF>\n" +
				             "        </NatuerlichePersonGb>\n" +
				             "        <PersonEigentumsForm>miteigentum</PersonEigentumsForm>\n" +
				             "    </PersonEigentumAnteil>\n" +
				             "</PersonEigentumAnteilList>\n", mut1.getXmlContent());
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
		droitPP1.setDateDebutMetier(RegDate.get(2010, 4, 23));
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
		droitPP2.setDateDebutMetier(RegDate.get(2010, 4, 23));
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
		droitPP3.setDateDebutMetier(RegDate.get(2010, 3, 28));
		droitPP3.setNumeroAffaire(new IdentifiantAffaireRF(6, 2013, 28, 4));
		droitPP3.setPart(new Fraction(1, 1));
		droitPP3.setRegime(GenrePropriete.INDIVIDUELLE);
		pp2.addDroit(droitPP3);

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

		final AyantDroitRFDetector ayantDroitRFDetector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits, cacheServitudes);

		// on envoie les trois mêmes droits sur les mêmes propriétaires et immeubles
		final PersonEigentumAnteil droit1 = newDroitPP("9a9c9e94923", "37838sc9d94de", "382929efa218", new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		final PersonEigentumAnteil droit2 = newDroitPP("45729cd9e20", "029191d4fec44", "382929efa218", new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		final PersonEigentumAnteil droit3 = newDroitPP("38458fa0ac3", "029191d4fec44", "202930c0e0f3", new Fraction(1, 1), PersonEigentumsform.ALLEINEIGENTUM, RegDate.get(2010, 3, 28), new IdentifiantAffaireRF(6, 2013, 28, 4), "Achat");

		List<PersonEigentumAnteil> droits = Arrays.asList(droit1, droit2, droit3);
		detector.processDroitsPropriete(IMPORT_ID, 2, droits.iterator(), false, null);

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
		droitPP1.setDateDebutMetier(RegDate.get(2010, 4, 23));
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
		droitPP2.setDateDebutMetier(RegDate.get(2010, 4, 23));
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
		droitPP3.setDateDebutMetier(RegDate.get(2010, 3, 28));
		droitPP3.setNumeroAffaire(new IdentifiantAffaireRF(6, 2013, 28, 4));
		droitPP3.setPart(new Fraction(1, 1));
		droitPP3.setRegime(GenrePropriete.INDIVIDUELLE);
		pp2.addDroit(droitPP3);

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

		final AyantDroitRFDetector ayantDroitRFDetector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits, cacheServitudes);

		// on envoie une liste de droits vide
		detector.processDroitsPropriete(IMPORT_ID, 2, Collections.<PersonEigentumAnteil>emptyList().iterator(), false, null);

		// on devrait avoir deux événements de mutation de type SUPPRESSION sur chacun des propriétaires
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		Collections.sort(mutations, new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut0.getTypeMutation());
		assertEquals("029191d4fec44", mut0.getIdRF());  // le premier propriétaire
		assertNull(mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut1.getTypeMutation());
		assertEquals("37838sc9d94de", mut1.getIdRF());  // le second propriétaire
		assertNull(mut1.getXmlContent());
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées si les servitudes existent dans la base de données mais pas avec les mêmes valeurs.
	 */
	@Test
	public void testServitudesModifiees() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 6, 1);

		// les données déjà existantes dans le DB
		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setIdRF("_1f109152380ffd8901380ffda8131c65");

		final BienFondRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final BienFondRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("_1f109152380ffd8901380ffe090827e1");

		final UsufruitRF usufruit1 = new UsufruitRF();
		usufruit1.setMasterIdRF("1f109152380ffd8901380ffed6694392");
		usufruit1.setAyantDroit(pp1);
		usufruit1.setImmeuble(immeuble1);
		usufruit1.setCommunaute(null);
		usufruit1.setDateDebut(dateImportInitial);
		usufruit1.setDateFin(null);
		usufruit1.setMotifDebut(null);
		usufruit1.setMotifFin(null);
		usufruit1.setDateDebutMetier(RegDate.get(2002, 9, 2));
		usufruit1.setDateFinMetier(null);
		usufruit1.setIdentifiantDroit(new IdentifiantDroitRF(8,2005, 699));
		usufruit1.setNumeroAffaire(new IdentifiantAffaireRF(8, 2002, 392, null));
		pp1.addDroit(usufruit1);

		final UsufruitRF usufruit2 = new UsufruitRF();
		usufruit2.setMasterIdRF("1f109152380ffd8901380ffed6694392");
		usufruit2.setAyantDroit(pp2);
		usufruit2.setImmeuble(immeuble1);
		usufruit2.setCommunaute(null);
		usufruit2.setDateDebut(dateImportInitial);
		usufruit2.setDateFin(null);
		usufruit2.setMotifDebut(null);
		usufruit2.setMotifFin(null);
		usufruit2.setDateDebutMetier(RegDate.get(2002, 9, 2));
		usufruit2.setDateFinMetier(null);
		usufruit2.setIdentifiantDroit(new IdentifiantDroitRF(8,2005, 699));
		usufruit2.setNumeroAffaire(new IdentifiantAffaireRF(8, 2002, 392, null));
		pp2.addDroit(usufruit2);

		final UsufruitRF usufruit3 = new UsufruitRF();
		usufruit3.setMasterIdRF("1f109152380ffd8901380fff10ca631e");
		usufruit3.setAyantDroit(pp2);
		usufruit3.setImmeuble(immeuble2);
		usufruit3.setCommunaute(null);
		usufruit3.setDateDebut(dateImportInitial);
		usufruit3.setDateFin(null);
		usufruit3.setMotifDebut(null);
		usufruit3.setMotifFin(null);
		usufruit3.setDateDebutMetier(RegDate.get(2007, 6, 25));
		usufruit3.setDateFinMetier(null);
		usufruit3.setIdentifiantDroit(new IdentifiantDroitRF(8,2007, 375));
		usufruit3.setNumeroAffaire(new IdentifiantAffaireRF(8, 2007, 266, 0));
		pp2.addDroit(usufruit3);

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

		final AyantDroitRFDetector ayantDroitRFDetector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits, cacheServitudes);

		// on envoie trois nouvelles servitudes pour deux bénéficiaires qui concernent deux immeubles
		final BelastetesGrundstueck grundstueck1 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final BelastetesGrundstueck grundstueck2 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe090827e1", null, null);
		final ch.vd.capitastra.rechteregister.NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final ch.vd.capitastra.rechteregister.NatuerlichePersonGb natuerlichePerson2 = newNatuerlichePersonGb("Anne-Lise", "Lassueur", "_1f109152380ffd8901380ffda8131c65");
		// - date de fin différente
		final DienstbarkeitDiscrete dienstbarkeit1 = ServitudesRFHelperTest.newDienstbarkeitDiscrete(grundstueck1, natuerlichePerson1,
		                                                                                             ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380ffed6694392",
		                                                                                                                                     "_1f109152380ffd8901380ffed6694392",
		                                                                                                                                     2005, 699, 8,
		                                                                                                                                     "Usufruit",
		                                                                                                                                     "2002/392", null,
		                                                                                                                                     RegDate.get(2002, 9, 2),
		                                                                                                                                     RegDate.get(2111, 2, 23)),
		                                                                                             null);
		// - numéro d'affaire différent
		final DienstbarkeitDiscrete dienstbarkeit2 = ServitudesRFHelperTest.newDienstbarkeitDiscrete(grundstueck1, natuerlichePerson2,
		                                                                                             ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380ffed6694392",
		                                                                                                                                     "_1f109152380ffd8901380ffed6694392",
		                                                                                                                                     2005, 699, 8,
		                                                                                                                                     "Usufruit",
		                                                                                                                                     "2017/201", null,
		                                                                                                                                     RegDate.get(2002, 9, 2),
		                                                                                                                                     null),
		                                                                                             null);
		// - pas de différence
		final DienstbarkeitDiscrete dienstbarkeit3 = ServitudesRFHelperTest.newDienstbarkeitDiscrete(grundstueck2, natuerlichePerson2,
		                                                                                             ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380fff10ca631e",
		                                                                                                                                     "_1f109152380ffd8901380fff10ca631e",
		                                                                                                                                     2007, 375, 8,
		                                                                                                                                     "Usufruit",
		                                                                                                                                     null, new Beleg(8, 2007, 266, 0),
		                                                                                                                                     RegDate.get(2007, 6, 25),
		                                                                                                                                     null),
		                                                                                             null);

		List<DienstbarkeitDiscrete> servitudes = Arrays.asList(dienstbarkeit1, dienstbarkeit2, dienstbarkeit3);
		detector.processServitudes(IMPORT_ID, 2, servitudes.iterator(), false, null);

		// on devrait avoir deux événements de mutation de type MODIFICATION sur chacun des propriétaires
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		Collections.sort(mutations, new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
		assertEquals("_1f109152380ffd8901380ffda8131c65", mut0.getIdRF());  // le premier propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<DienstbarkeitDiscreteList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
				             "    <DienstbarkeitDiscretes>\n" +
				             "        <Dienstbarkeit MasterID=\"1f109152380ffd8901380ffed6694392\">\n" +
				             "            <StandardRechtID>_1f109152380ffd8901380ffed6694392</StandardRechtID>\n" +
				             "            <RechtEintragJahrID>2005</RechtEintragJahrID>\n" +
				             "            <RechtEintragNummerID>699</RechtEintragNummerID>\n" +
				             "            <AmtNummer>8</AmtNummer>\n" +
				             "            <Stichwort>\n" +
				             "                <TextFr>Usufruit</TextFr>\n" +
				             "            </Stichwort>\n" +
				             "            <BelegAlt>2017/201</BelegAlt>\n" +
				             "            <BeginDatum>2002-09-02</BeginDatum>\n" +
				             "        </Dienstbarkeit>\n" +
				             "        <BelastetesGrundstueck>\n" +
				             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe15bb729c</BelastetesGrundstueckIDREF>\n" +
				             "        </BelastetesGrundstueck>\n" +
				             "        <BerechtigtePerson>\n" +
				             "            <NatuerlichePersonGb>\n" +
				             "                <Name>Lassueur</Name>\n" +
				             "                <Vorname>Anne-Lise</Vorname>\n" +
				             "                <PersonstammIDREF>_1f109152380ffd8901380ffda8131c65</PersonstammIDREF>\n" +
				             "            </NatuerlichePersonGb>\n" +
				             "        </BerechtigtePerson>\n" +
				             "    </DienstbarkeitDiscretes>\n" +
				             "    <DienstbarkeitDiscretes>\n" +
				             "        <Dienstbarkeit MasterID=\"1f109152380ffd8901380fff10ca631e\">\n" +
				             "            <StandardRechtID>_1f109152380ffd8901380fff10ca631e</StandardRechtID>\n" +
				             "            <RechtEintragJahrID>2007</RechtEintragJahrID>\n" +
				             "            <RechtEintragNummerID>375</RechtEintragNummerID>\n" +
				             "            <AmtNummer>8</AmtNummer>\n" +
				             "            <Stichwort>\n" +
				             "                <TextFr>Usufruit</TextFr>\n" +
				             "            </Stichwort>\n" +
				             "            <Beleg>\n" +
				             "                <AmtNummer>8</AmtNummer>\n" +
				             "                <BelegJahr>2007</BelegJahr>\n" +
				             "                <BelegNummer>266</BelegNummer>\n" +
				             "                <BelegNummerIndex>0</BelegNummerIndex>\n" +
				             "            </Beleg>\n" +
				             "            <BeginDatum>2007-06-25</BeginDatum>\n" +
				             "        </Dienstbarkeit>\n" +
				             "        <BelastetesGrundstueck>\n" +
				             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe090827e1</BelastetesGrundstueckIDREF>\n" +
				             "        </BelastetesGrundstueck>\n" +
				             "        <BerechtigtePerson>\n" +
				             "            <NatuerlichePersonGb>\n" +
				             "                <Name>Lassueur</Name>\n" +
				             "                <Vorname>Anne-Lise</Vorname>\n" +
				             "                <PersonstammIDREF>_1f109152380ffd8901380ffda8131c65</PersonstammIDREF>\n" +
				             "            </NatuerlichePersonGb>\n" +
				             "        </BerechtigtePerson>\n" +
				             "    </DienstbarkeitDiscretes>\n" +
				             "</DienstbarkeitDiscreteList>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut1.getTypeMutation());
		assertEquals("_1f109152380ffd8901380ffdabcc2441", mut1.getIdRF());  // le second propriétaire
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<DienstbarkeitDiscreteList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
				             "    <DienstbarkeitDiscretes>\n" +
				             "        <Dienstbarkeit MasterID=\"1f109152380ffd8901380ffed6694392\">\n" +
				             "            <StandardRechtID>_1f109152380ffd8901380ffed6694392</StandardRechtID>\n" +
				             "            <RechtEintragJahrID>2005</RechtEintragJahrID>\n" +
				             "            <RechtEintragNummerID>699</RechtEintragNummerID>\n" +
				             "            <AmtNummer>8</AmtNummer>\n" +
				             "            <Stichwort>\n" +
				             "                <TextFr>Usufruit</TextFr>\n" +
				             "            </Stichwort>\n" +
				             "            <BelegAlt>2002/392</BelegAlt>\n" +
				             "            <BeginDatum>2002-09-02</BeginDatum>\n" +
				             "            <AblaufDatum>2111-02-23</AblaufDatum>\n" +
				             "        </Dienstbarkeit>\n" +
				             "        <BelastetesGrundstueck>\n" +
				             "            <BelastetesGrundstueckIDREF>_1f109152380ffd8901380ffe15bb729c</BelastetesGrundstueckIDREF>\n" +
				             "        </BelastetesGrundstueck>\n" +
				             "        <BerechtigtePerson>\n" +
				             "            <NatuerlichePersonGb>\n" +
				             "                <Name>Gaillard</Name>\n" +
				             "                <Vorname>Roger</Vorname>\n" +
				             "                <PersonstammIDREF>_1f109152380ffd8901380ffdabcc2441</PersonstammIDREF>\n" +
				             "            </NatuerlichePersonGb>\n" +
				             "        </BerechtigtePerson>\n" +
				             "    </DienstbarkeitDiscretes>\n" +
				             "</DienstbarkeitDiscreteList>\n", mut1.getXmlContent());
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées si les servitudes existent dans la base de données mais pas avec les mêmes valeurs.
	 */
	@Test
	public void testServitudesIdentiques() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 6, 1);

		// les données déjà existantes dans le DB
		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setIdRF("_1f109152380ffd8901380ffda8131c65");

		final BienFondRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final BienFondRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("_1f109152380ffd8901380ffe090827e1");

		final UsufruitRF usufruit1 = new UsufruitRF();
		usufruit1.setMasterIdRF("1f109152380ffd8901380ffed6694392");
		usufruit1.setAyantDroit(pp1);
		usufruit1.setImmeuble(immeuble1);
		usufruit1.setCommunaute(null);
		usufruit1.setDateDebut(dateImportInitial);
		usufruit1.setDateFin(null);
		usufruit1.setMotifDebut(null);
		usufruit1.setMotifFin(null);
		usufruit1.setDateDebutMetier(RegDate.get(2002, 9, 2));
		usufruit1.setDateFinMetier(null);
		usufruit1.setIdentifiantDroit(new IdentifiantDroitRF(8,2005, 699));
		usufruit1.setNumeroAffaire(new IdentifiantAffaireRF(8, 2002, 392, null));
		pp1.addDroit(usufruit1);

		final UsufruitRF usufruit2 = new UsufruitRF();
		usufruit2.setMasterIdRF("1f109152380ffd8901380ffed6694392");
		usufruit2.setAyantDroit(pp2);
		usufruit2.setImmeuble(immeuble1);
		usufruit2.setCommunaute(null);
		usufruit2.setDateDebut(dateImportInitial);
		usufruit2.setDateFin(null);
		usufruit2.setMotifDebut(null);
		usufruit2.setMotifFin(null);
		usufruit2.setDateDebutMetier(RegDate.get(2002, 9, 2));
		usufruit2.setDateFinMetier(null);
		usufruit2.setIdentifiantDroit(new IdentifiantDroitRF(8,2005, 699));
		usufruit2.setNumeroAffaire(new IdentifiantAffaireRF(8, 2002, 392, null));
		pp2.addDroit(usufruit2);

		final UsufruitRF usufruit3 = new UsufruitRF();
		usufruit3.setMasterIdRF("1f109152380ffd8901380fff10ca631e");
		usufruit3.setAyantDroit(pp2);
		usufruit3.setImmeuble(immeuble2);
		usufruit3.setCommunaute(null);
		usufruit3.setDateDebut(dateImportInitial);
		usufruit3.setDateFin(null);
		usufruit3.setMotifDebut(null);
		usufruit3.setMotifFin(null);
		usufruit3.setDateDebutMetier(RegDate.get(2007, 6, 25));
		usufruit3.setDateFinMetier(null);
		usufruit3.setIdentifiantDroit(new IdentifiantDroitRF(8,2007, 375));
		usufruit3.setNumeroAffaire(new IdentifiantAffaireRF(8, 2007, 266, 0));
		pp2.addDroit(usufruit3);

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

		final AyantDroitRFDetector ayantDroitRFDetector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits, cacheServitudes);

		// on envoie trois nouvelles servitudes pour deux bénéficiaires qui concernent deux immeubles
		final BelastetesGrundstueck grundstueck1 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final BelastetesGrundstueck grundstueck2 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe090827e1", null, null);
		final ch.vd.capitastra.rechteregister.NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final ch.vd.capitastra.rechteregister.NatuerlichePersonGb natuerlichePerson2 = newNatuerlichePersonGb("Anne-Lise", "Lassueur", "_1f109152380ffd8901380ffda8131c65");
		final DienstbarkeitDiscrete dienstbarkeit1 = ServitudesRFHelperTest.newDienstbarkeitDiscrete(grundstueck1, natuerlichePerson1,
		                                                                                             ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380ffed6694392",
		                                                                                                                                     "_1f109152380ffd8901380ffed6694392",
		                                                                                                                                     2005, 699, 8,
		                                                                                                                                     "Usufruit",
		                                                                                                                                     "2002/392", null,
		                                                                                                                                     RegDate.get(2002, 9, 2),
		                                                                                                                                     null),
		                                                                                             null);
		final DienstbarkeitDiscrete dienstbarkeit2 = ServitudesRFHelperTest.newDienstbarkeitDiscrete(grundstueck1, natuerlichePerson2,
		                                                                                             ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380ffed6694392",
		                                                                                                                                     "_1f109152380ffd8901380ffed6694392",
		                                                                                                                                     2005, 699, 8,
		                                                                                                                                     "Usufruit",
		                                                                                                                                     "2002/392", null,
		                                                                                                                                     RegDate.get(2002, 9, 2),
		                                                                                                                                     null),
		                                                                                             null);
		final DienstbarkeitDiscrete dienstbarkeit3 = ServitudesRFHelperTest.newDienstbarkeitDiscrete(grundstueck2, natuerlichePerson2,
		                                                                                             ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380fff10ca631e",
		                                                                                                                                     "_1f109152380ffd8901380fff10ca631e",
		                                                                                                                                     2007, 375, 8,
		                                                                                                                                     "Usufruit",
		                                                                                                                                     null, new Beleg(8, 2007, 266, 0),
		                                                                                                                                     RegDate.get(2007, 6, 25),
		                                                                                                                                     null),
		                                                                                             null);

		List<DienstbarkeitDiscrete> servitudes = Arrays.asList(dienstbarkeit1, dienstbarkeit2, dienstbarkeit3);
		detector.processServitudes(IMPORT_ID, 2, servitudes.iterator(), false, null);

		// on devrait avoir deux événements de mutation de type MODIFICATION sur chacun des propriétaires
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(0, mutations.size());
	}

	/**
	 * Ce test vérifie que des mutations de suppression sont créées si des bénéficiaires de servitudes dans la DB n'en ont plus dans le fichier d'import.
	 */
	@Test
	public void testSuppressionDeServitudes() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 6, 1);

		// les données déjà existantes dans le DB
		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setIdRF("_1f109152380ffd8901380ffda8131c65");

		final BienFondRF immeuble1 = new BienFondRF();
		immeuble1.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final BienFondRF immeuble2 = new BienFondRF();
		immeuble2.setIdRF("_1f109152380ffd8901380ffe090827e1");

		final UsufruitRF usufruit1 = new UsufruitRF();
		usufruit1.setMasterIdRF("1f109152380ffd8901380ffed6694392");
		usufruit1.setAyantDroit(pp1);
		usufruit1.setImmeuble(immeuble1);
		usufruit1.setCommunaute(null);
		usufruit1.setDateDebut(dateImportInitial);
		usufruit1.setDateFin(null);
		usufruit1.setMotifDebut(null);
		usufruit1.setMotifFin(null);
		usufruit1.setDateDebutMetier(RegDate.get(2002, 9, 2));
		usufruit1.setDateFinMetier(null);
		usufruit1.setIdentifiantDroit(new IdentifiantDroitRF(8,2005, 699));
		usufruit1.setNumeroAffaire(new IdentifiantAffaireRF(8, 2002, 392, null));
		pp1.addDroit(usufruit1);

		final UsufruitRF usufruit2 = new UsufruitRF();
		usufruit2.setMasterIdRF("1f109152380ffd8901380ffed6694392");
		usufruit2.setAyantDroit(pp2);
		usufruit2.setImmeuble(immeuble1);
		usufruit2.setCommunaute(null);
		usufruit2.setDateDebut(dateImportInitial);
		usufruit2.setDateFin(null);
		usufruit2.setMotifDebut(null);
		usufruit2.setMotifFin(null);
		usufruit2.setDateDebutMetier(RegDate.get(2002, 9, 2));
		usufruit2.setDateFinMetier(null);
		usufruit2.setIdentifiantDroit(new IdentifiantDroitRF(8,2005, 699));
		usufruit2.setNumeroAffaire(new IdentifiantAffaireRF(8, 2002, 392, null));
		pp2.addDroit(usufruit2);

		final UsufruitRF usufruit3 = new UsufruitRF();
		usufruit3.setMasterIdRF("1f109152380ffd8901380fff10ca631e");
		usufruit3.setAyantDroit(pp2);
		usufruit3.setImmeuble(immeuble2);
		usufruit3.setCommunaute(null);
		usufruit3.setDateDebut(dateImportInitial);
		usufruit3.setDateFin(null);
		usufruit3.setMotifDebut(null);
		usufruit3.setMotifFin(null);
		usufruit3.setDateDebutMetier(RegDate.get(2007, 6, 25));
		usufruit3.setDateFinMetier(null);
		usufruit3.setIdentifiantDroit(new IdentifiantDroitRF(8,2007, 375));
		usufruit3.setNumeroAffaire(new IdentifiantAffaireRF(8, 2007, 266, 0));
		pp2.addDroit(usufruit3);

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

		final AyantDroitRFDetector ayantDroitRFDetector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits, cacheServitudes);

		// on envoie une liste de servitudes vide
		detector.processServitudes(IMPORT_ID, 2, Collections.<DienstbarkeitDiscrete>emptyList().iterator(), false, null);

		// on devrait avoir deux événements de mutation de type SUPPRESSION sur chacun des propriétaires
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		Collections.sort(mutations, new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut0.getTypeMutation());
		assertEquals("_1f109152380ffd8901380ffda8131c65", mut0.getIdRF());  // le premier propriétaire
		assertNull(mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut1.getTypeMutation());
		assertEquals("_1f109152380ffd8901380ffdabcc2441", mut1.getIdRF());  // le second propriétaire
		assertNull(mut1.getXmlContent());
	}
	@NotNull
	private static PersonEigentumAnteil newDroitPP(String idRfDroit, String idRfPP, String idRfImmeuble, Fraction part, PersonEigentumsform typePropriete, RegDate dateDebutEffective, IdentifiantAffaireRF affaire, String motifDebut) {

		final Rechtsgrund recht = new Rechtsgrund();
		recht.setBelegDatum(dateDebutEffective);
		recht.setAmtNummer(affaire.getNumeroOffice());
		recht.setBelegAlt(affaire.getNumeroAffaire());
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
		recht.setBelegAlt(affaire.getNumeroAffaire());
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
