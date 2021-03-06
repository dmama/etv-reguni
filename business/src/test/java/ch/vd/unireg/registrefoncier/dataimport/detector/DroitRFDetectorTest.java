package ch.vd.unireg.registrefoncier.dataimport.detector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.grundstueck.CapiCode;
import ch.vd.capitastra.grundstueck.EigentumAnteil;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.GemeinschaftsArt;
import ch.vd.capitastra.grundstueck.GrundstueckEigentumAnteil;
import ch.vd.capitastra.grundstueck.GrundstueckEigentumsform;
import ch.vd.capitastra.grundstueck.NatuerlichePersonGb;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.PersonEigentumsform;
import ch.vd.capitastra.grundstueck.Quote;
import ch.vd.capitastra.grundstueck.Rechtsgrund;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.cache.MockPersistentCache;
import ch.vd.unireg.cache.PersistentCache;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImport;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.MockEvenementRFImportDAO;
import ch.vd.unireg.evenement.registrefoncier.MockEvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.ProprieteParEtageRF;
import ch.vd.unireg.registrefoncier.RaisonAcquisitionRF;
import ch.vd.unireg.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dao.MockAyantDroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.MockImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.MutationComparator;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRFImpl;
import ch.vd.unireg.registrefoncier.dataimport.helper.BlacklistRFHelper;
import ch.vd.unireg.transaction.MockTransactionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SuppressWarnings("Duplicates")
public class DroitRFDetectorTest {

	private static final Long IMPORT_ID = 1L;
	private XmlHelperRF xmlHelperRF;
	private BlacklistRFHelper blacklistRFHelper;
	private PlatformTransactionManager transactionManager;
	private ImmeubleRFDAO immeubleRFDAO;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private PersistentCache<ArrayList<EigentumAnteil>> cacheDroits;

	@Before
	public void setUp() throws Exception {
		xmlHelperRF = new XmlHelperRFImpl();
		blacklistRFHelper = idRF -> idRF.equals("_1f1091523810108101381012b3d64cb4") || idRF.equals("_1f1091523810190f0138101cd6404148");
		transactionManager = new MockTransactionManager();
		immeubleRFDAO = new MockImmeubleRFDAO();
		ayantDroitRFDAO = new MockAyantDroitRFDAO();
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

		final String idRfImmeuble1 = "202930c0e0f3";
		final String idRfImmeuble2 = "382929efa218";

		// un mock qui mémorise toutes les mutations sauvées
		final EvenementRFMutationDAO evenementRFMutationDAO = new MockEvenementRFMutationDAO();

		final AyantDroitRFDetector ayantDroitRFDetector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits);

		// on envoie trois nouveaux droits sur deux propriétaires qui concernent deux immeubles
		final PersonEigentumAnteil droit1 = newDroitPP("9a9c9e94923", "1", "37838sc9d94de", idRfImmeuble1, new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		final PersonEigentumAnteil droit2 = newDroitPP("45729cd9e20", "1", "029191d4fec44", idRfImmeuble1, new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		final PersonEigentumAnteil droit3 = newDroitPP("38458fa0ac3", "1", "029191d4fec44", idRfImmeuble2, new Fraction(1, 1), PersonEigentumsform.ALLEINEIGENTUM, RegDate.get(2010, 3, 28), new IdentifiantAffaireRF(6, 2013, 28, 4), "Achat");

		final List<EigentumAnteil> droits = Arrays.asList(droit1, droit2, droit3);
		detector.processDroitsPropriete(IMPORT_ID, 2, droits.iterator(), null);

		// on devrait avoir deux événements de mutation de type CREATION sur chacun des immeubles
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		mutations.sort(new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
		assertEquals(idRfImmeuble1, mut0.getIdRF());  // le premier immeuble
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<EigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil VersionID=\"1\" MasterID=\"9a9c9e94923\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>2</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>202930c0e0f3</BelastetesGrundstueckIDREF>\n" +
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
				             "    <PersonEigentumAnteil VersionID=\"1\" MasterID=\"45729cd9e20\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>2</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>202930c0e0f3</BelastetesGrundstueckIDREF>\n" +
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
				             "</EigentumAnteilList>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
		assertEquals(idRfImmeuble2, mut1.getIdRF());  // le second immeuble
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<EigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil VersionID=\"1\" MasterID=\"38458fa0ac3\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>1</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>382929efa218</BelastetesGrundstueckIDREF>\n" +
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
				             "</EigentumAnteilList>\n", mut1.getXmlContent());
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
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits);

		final String idRfPP1 = "029191d4fec44";
		final String idRfPP2 = "37838sc9d94de";
		final String idRfImmeuble = "382929efa218";
		final String idRFCommunaute = "72828ce8f830a";

		// on envoie trois nouveaux droits sur deux propriétaires et une communaué qui concernent un immeuble
		final PersonEigentumAnteil droit1 = newDroitPP("9a9c9e94923", "1", idRfPP1, idRfImmeuble, new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Héritage");
		droit1.getNatuerlichePersonGb().setGemeinschatIDREF(idRFCommunaute);
		final PersonEigentumAnteil droit2 = newDroitPP("45729cd9e20", "1", idRfPP2, idRfImmeuble, new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Héritage");
		droit2.getNatuerlichePersonGb().setGemeinschatIDREF(idRFCommunaute);
		final PersonEigentumAnteil droit3 =
				newDroitColl("38458fa0ac3", idRFCommunaute, idRfImmeuble, GemeinschaftsArt.ERBENGEMEINSCHAFT, new Fraction(1, 1), PersonEigentumsform.ALLEINEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Héritage");

		final List<EigentumAnteil> droits = Arrays.asList(droit1, droit2, droit3);
		detector.processDroitsPropriete(IMPORT_ID, 2, droits.iterator(), null);

		// on devrait avoir 2 événements de mutation de type CREATION :
		//  - 1 pour les droits de l'immeuble
		//  - 1 pour la communauté (ayant-droit)
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		mutations.sort(new MutationComparator());

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
		assertEquals(idRfImmeuble, mut1.getIdRF());  // l'immeuble
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<EigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil VersionID=\"1\" MasterID=\"9a9c9e94923\">\n" +
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
				             "    <PersonEigentumAnteil VersionID=\"1\" MasterID=\"45729cd9e20\">\n" +
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
				             "</EigentumAnteilList>\n", mut1.getXmlContent());
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsqu'un droit entre immeuble n'existe pas dans la base de données.
	 */
	@Test
	public void testNouveauxDroitsEntreImmeubles() throws Exception {

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
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits);

		// on envoie un nouveau droit entre deux immeubles
		final GrundstueckEigentumAnteil droit1 = newDroitImm("3838292", "1", "48238919011", "202930c0e0f3", new Fraction(1, 1), GrundstueckEigentumsform.DOMINIERENDES_GRUNDSTUECK, RegDate.get(2010, 4, 11), new IdentifiantAffaireRF(6, 2013, 17, 0), "Constitution de PPE");

		final List<EigentumAnteil> droits = Collections.singletonList(droit1);
		detector.processDroitsPropriete(IMPORT_ID, 2, droits.iterator(), null);

		// on devrait avoir en tout 2 événements de mutation de type CREATION : un pour l'immeuble bénéficiare et un autre pour le droit
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		mutations.sort(new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.AYANT_DROIT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
		assertEquals("48238919011", mut0.getIdRF());  // l'immeuble dominant (= ayant-droit)
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<UnbekanntesGrundstueck xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckID>48238919011</GrundstueckID>\n" +
				             "    <IstKopie>false</IstKopie>\n" +
				             "</UnbekanntesGrundstueck>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
		assertEquals("202930c0e0f3", mut1.getIdRF());  // l'immeuble servant
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<EigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckEigentumAnteil VersionID=\"1\" MasterID=\"3838292\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>1</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>202930c0e0f3</BelastetesGrundstueckIDREF>\n" +
				             "        <BerechtigtesGrundstueckIDREF>48238919011</BerechtigtesGrundstueckIDREF>\n" +
				             "        <GrundstueckEigentumsForm>DominierendesGrundstueck</GrundstueckEigentumsForm>\n" +
				             "        <Rechtsgruende>\n" +
				             "            <AmtNummer>6</AmtNummer>\n" +
				             "            <RechtsgrundCode>\n" +
				             "                <TextFr>Constitution de PPE</TextFr>\n" +
				             "            </RechtsgrundCode>\n" +
				             "            <BelegDatum>2010-04-11</BelegDatum>\n" +
				             "            <BelegAlt>2013/17/0</BelegAlt>\n" +
				             "        </Rechtsgruende>\n" +
				             "    </GrundstueckEigentumAnteil>\n" +
				             "</EigentumAnteilList>\n", mut1.getXmlContent());
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
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits);

		// on envoie un nouveau droit qui concerne un immeuble blacklisté
		final PersonEigentumAnteil droit1 = newDroitPP("9a9c9e94923", "1", "37838sc9d94de", "_1f1091523810108101381012b3d64cb4", new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");

		final List<EigentumAnteil> droits = Collections.singletonList(droit1);
		detector.processDroitsPropriete(IMPORT_ID, 2, droits.iterator(), null);

		// on ne devrait pas avoir de mutations
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(0, mutations.size());
	}

	/**
	 * [SIFISC-24611] Ce test vérifie que les droits entre immeubles avec des immeubles dominants blacklistés sont bien ignorés.
	 */
	@Test
	public void testNouveauxDroitsEntreImmeubleSurImmeubleDominantBlackliste() throws Exception {

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
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits);

		// on envoie un nouveau droit entre deux immeubles avec l'immeuble dominant blacklisté
		final GrundstueckEigentumAnteil droit1 = newDroitImm("3838292", "1", "_1f1091523810190f0138101cd6404148", "202930c0e0f3",
		                                                     new Fraction(1, 1), GrundstueckEigentumsform.DOMINIERENDES_GRUNDSTUECK, RegDate.get(2010, 4, 11),
		                                                     new IdentifiantAffaireRF(6, 2013, 17, 0), "Constitution de PPE");

		final List<EigentumAnteil> droits = Collections.singletonList(droit1);
		detector.processDroitsPropriete(IMPORT_ID, 2, droits.iterator(), null);

		// on ne devrait pas avoir de mutations (= pas de création sur l'immeuble bénéficiaire, ni de création du droit)
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(0, mutations.size());
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées si les droits existent dans la base de données mais pas avec les mêmes valeurs.
	 */
	@Test
	public void testDroitsModifies() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 6, 1);
		final String idRfImmeuble1 = "202930c0e0f3";
		final String idRfImmeuble2 = "382929efa218";
		final String idRfImmeuble3 = "48238919011";

		// les données déjà existantes dans le DB
		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("37838sc9d94de");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setIdRF("029191d4fec44");

		final ImmeubleBeneficiaireRF ib3 = new ImmeubleBeneficiaireRF();
		ib3.setIdRF(idRfImmeuble3);

		final BienFondsRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF(idRfImmeuble1);

		final BienFondsRF immeuble2 = new BienFondsRF();
		immeuble2.setIdRF(idRfImmeuble2);

		final ProprieteParEtageRF immeuble3 = new ProprieteParEtageRF();
		immeuble3.setIdRF(ib3.getIdRF());

		immeubleRFDAO.save(immeuble1);
		immeubleRFDAO.save(immeuble2);
		immeubleRFDAO.save(immeuble3);
		immeuble3.setDroitsPropriete(Collections.emptySet());

		final DroitProprietePersonnePhysiqueRF droitPP1 = new DroitProprietePersonnePhysiqueRF();
		droitPP1.setMasterIdRF("9a9c9e94923");
		droitPP1.setVersionIdRF("1");
		droitPP1.setAyantDroit(pp1);
		droitPP1.setImmeuble(immeuble1);
		droitPP1.setCommunaute(null);
		droitPP1.setDateDebut(dateImportInitial);
		droitPP1.setDateFin(null);
		droitPP1.setDateDebutMetier(RegDate.get(2010, 4, 23));
		droitPP1.setMotifDebut("Achat");
		droitPP1.setMotifFin(null);
		droitPP1.setPart(new Fraction(1, 2));
		droitPP1.setRegime(GenrePropriete.COPROPRIETE);
		droitPP1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 23), "Achat", new IdentifiantAffaireRF(6, 2013, 33, 1)));
		pp1.addDroitPropriete(droitPP1);
		immeuble1.addDroitPropriete(droitPP1);

		final DroitProprietePersonnePhysiqueRF droitPP2 = new DroitProprietePersonnePhysiqueRF();
		droitPP2.setMasterIdRF("45729cd9e20");
		droitPP2.setVersionIdRF("1");
		droitPP2.setAyantDroit(pp2);
		droitPP2.setImmeuble(immeuble1);
		droitPP2.setCommunaute(null);
		droitPP2.setDateDebut(dateImportInitial);
		droitPP2.setDateFin(null);
		droitPP2.setDateDebutMetier(RegDate.get(2010, 4, 23));
		droitPP2.setMotifDebut("Achat");
		droitPP2.setMotifFin(null);
		droitPP2.setPart(new Fraction(1, 2));
		droitPP2.setRegime(GenrePropriete.COPROPRIETE);
		droitPP2.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 23), "Achat", new IdentifiantAffaireRF(6, 2013, 33, 1)));
		pp2.addDroitPropriete(droitPP2);
		immeuble1.addDroitPropriete(droitPP2);

		final DroitProprietePersonnePhysiqueRF droitPP3 = new DroitProprietePersonnePhysiqueRF();
		droitPP3.setMasterIdRF("38458fa0ac3");
		droitPP3.setVersionIdRF("1");
		droitPP3.setAyantDroit(pp2);
		droitPP3.setImmeuble(immeuble2);
		droitPP3.setCommunaute(null);
		droitPP3.setDateDebut(dateImportInitial);
		droitPP3.setDateFin(null);
		droitPP3.setDateDebutMetier(RegDate.get(2010, 3, 28));
		droitPP3.setMotifDebut("Achat");
		droitPP3.setMotifFin(null);
		droitPP3.setPart(new Fraction(1, 1));
		droitPP3.setRegime(GenrePropriete.INDIVIDUELLE);
		droitPP3.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 3, 28), "Achat", new IdentifiantAffaireRF(6, 2013, 28, 4)));
		pp2.addDroitPropriete(droitPP3);
		immeuble2.addDroitPropriete(droitPP3);

		final DroitProprieteImmeubleRF droitImm4 = new DroitProprieteImmeubleRF();
		droitImm4.setMasterIdRF("282002020");
		droitImm4.setVersionIdRF("1");
		droitImm4.setAyantDroit(ib3);
		droitImm4.setImmeuble(immeuble1);
		droitImm4.setDateDebut(dateImportInitial);
		droitImm4.setDateFin(null);
		droitImm4.setDateDebutMetier(RegDate.get(2010, 4, 4));
		droitImm4.setMotifDebut("Constitution de PPE");
		droitImm4.setMotifFin(null);
		droitImm4.setPart(new Fraction(1,14));
		droitImm4.setRegime(GenrePropriete.PPE);
		droitImm4.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 4), "Constitution de PPE", new IdentifiantAffaireRF(6, 2014, 203, 0)));
		ib3.addDroitPropriete(droitImm4);
		immeuble1.addDroitPropriete(droitImm4);

		// un mock avec les ayants-droits.
		ayantDroitRFDAO.save(pp1);
		ayantDroitRFDAO.save(pp2);
		ayantDroitRFDAO.save(ib3);

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
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits);

		// on envoie trois droits différents sur les mêmes propriétaires et immeubles
		//  - part différente
		final PersonEigentumAnteil droit1 = newDroitPP("9a9c9e94923", "1", "37838sc9d94de", idRfImmeuble1,
		                                               new Fraction(2, 5), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23),
		                                               new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		//  - motif différent
		final PersonEigentumAnteil droit2 = newDroitPP("45729cd9e20", "1", "029191d4fec44", idRfImmeuble1,
		                                               new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23),
		                                               new IdentifiantAffaireRF(6, 2013, 33, 1), "Vol autorisé");
		//  - type de propriété différent
		final PersonEigentumAnteil droit3 = newDroitPP("38458fa0ac3", "1", "029191d4fec44", idRfImmeuble2,
		                                               new Fraction(1, 1), PersonEigentumsform.GESAMTEIGENTUM, RegDate.get(2010, 3, 28),
		                                               new IdentifiantAffaireRF(6, 2013, 28, 4), "Achat");
		//  - raison d'acquisition différente
		final GrundstueckEigentumAnteil droit4 = newDroitImm("282002020", "1", idRfImmeuble3, idRfImmeuble1,
		                                                     new Fraction(1, 14), GrundstueckEigentumsform.STOCKWERK, RegDate.get(2016, 4, 4),
		                                                     new IdentifiantAffaireRF(6, 2016, 1, 0), "Remaniement parcellaire");

		final List<EigentumAnteil> droits = Arrays.asList(droit1, droit2, droit3, droit4);
		detector.processDroitsPropriete(IMPORT_ID, 2, droits.iterator(), null);

		// on devrait avoir 2 événements de mutation de type MODIFICATION (un par immeuble concerné)
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		mutations.sort(new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
		assertEquals(idRfImmeuble1, mut0.getIdRF());  // le premier immeuble
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<EigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil VersionID=\"1\" MasterID=\"9a9c9e94923\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>2</AnteilZaehler>\n" +
				             "            <AnteilNenner>5</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>202930c0e0f3</BelastetesGrundstueckIDREF>\n" +
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
				             "    <PersonEigentumAnteil VersionID=\"1\" MasterID=\"45729cd9e20\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>2</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>202930c0e0f3</BelastetesGrundstueckIDREF>\n" +
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
				             "    <GrundstueckEigentumAnteil VersionID=\"1\" MasterID=\"282002020\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>14</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>202930c0e0f3</BelastetesGrundstueckIDREF>\n" +
				             "        <BerechtigtesGrundstueckIDREF>48238919011</BerechtigtesGrundstueckIDREF>\n" +
				             "        <GrundstueckEigentumsForm>Stockwerk</GrundstueckEigentumsForm>\n" +
				             "        <Rechtsgruende>\n" +
				             "            <AmtNummer>6</AmtNummer>\n" +
				             "            <RechtsgrundCode>\n" +
				             "                <TextFr>Remaniement parcellaire</TextFr>\n" +
				             "            </RechtsgrundCode>\n" +
				             "            <BelegDatum>2016-04-04</BelegDatum>\n" +
				             "            <BelegAlt>2016/1/0</BelegAlt>\n" +
				             "        </Rechtsgruende>\n" +
				             "    </GrundstueckEigentumAnteil>\n" +
				             "</EigentumAnteilList>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut1.getTypeMutation());
		assertEquals(idRfImmeuble2, mut1.getIdRF());  // le second immeuble
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<EigentumAnteilList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonEigentumAnteil VersionID=\"1\" MasterID=\"38458fa0ac3\">\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>1</AnteilNenner>\n" +
				             "        </Quote>\n" +
				             "        <BelastetesGrundstueckIDREF>382929efa218</BelastetesGrundstueckIDREF>\n" +
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
				             "</EigentumAnteilList>\n", mut1.getXmlContent());
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

		final ImmeubleBeneficiaireRF ib3 = new ImmeubleBeneficiaireRF();
		ib3.setIdRF("48238919011");

		final BienFondsRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF("382929efa218");

		final BienFondsRF immeuble2 = new BienFondsRF();
		immeuble2.setIdRF("202930c0e0f3");

		final ProprieteParEtageRF immeuble3 = new ProprieteParEtageRF();
		immeuble3.setIdRF(ib3.getIdRF());

		immeubleRFDAO.save(immeuble1);
		immeubleRFDAO.save(immeuble2);
		immeubleRFDAO.save(immeuble3);
		immeuble3.setDroitsPropriete(Collections.emptySet());

		final DroitProprietePersonnePhysiqueRF droitPP1 = new DroitProprietePersonnePhysiqueRF();
		droitPP1.setMasterIdRF("9a9c9e94923");
		droitPP1.setVersionIdRF("1");
		droitPP1.setAyantDroit(pp1);
		droitPP1.setImmeuble(immeuble1);
		droitPP1.setCommunaute(null);
		droitPP1.setDateDebut(dateImportInitial);
		droitPP1.setDateFin(null);
		droitPP1.setDateDebutMetier(RegDate.get(2010, 4, 23));
		droitPP1.setMotifDebut("Achat");
		droitPP1.setMotifFin(null);
		droitPP1.setPart(new Fraction(1, 2));
		droitPP1.setRegime(GenrePropriete.COPROPRIETE);
		droitPP1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 23), "Achat", new IdentifiantAffaireRF(6, 2013, 33, 1)));
		pp1.addDroitPropriete(droitPP1);
		immeuble1.addDroitPropriete(droitPP1);

		final DroitProprietePersonnePhysiqueRF droitPP2 = new DroitProprietePersonnePhysiqueRF();
		droitPP2.setMasterIdRF("45729cd9e20");
		droitPP2.setVersionIdRF("1");
		droitPP2.setAyantDroit(pp2);
		droitPP2.setImmeuble(immeuble1);
		droitPP2.setCommunaute(null);
		droitPP2.setDateDebut(dateImportInitial);
		droitPP2.setDateFin(null);
		droitPP2.setDateDebutMetier(RegDate.get(2010, 4, 23));
		droitPP2.setMotifDebut("Achat");
		droitPP2.setMotifFin(null);
		droitPP2.setPart(new Fraction(1, 2));
		droitPP2.setRegime(GenrePropriete.COPROPRIETE);
		droitPP2.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 23), "Achat", new IdentifiantAffaireRF(6, 2013, 33, 1)));
		pp2.addDroitPropriete(droitPP2);
		immeuble1.addDroitPropriete(droitPP2);

		final DroitProprietePersonnePhysiqueRF droitPP3 = new DroitProprietePersonnePhysiqueRF();
		droitPP3.setMasterIdRF("38458fa0ac3");
		droitPP3.setVersionIdRF("1");
		droitPP3.setAyantDroit(pp2);
		droitPP3.setImmeuble(immeuble2);
		droitPP3.setCommunaute(null);
		droitPP3.setDateDebut(dateImportInitial);
		droitPP3.setDateFin(null);
		droitPP3.setDateDebutMetier(RegDate.get(2010, 3, 28));
		droitPP3.setMotifDebut("Achat");
		droitPP3.setMotifFin(null);
		droitPP3.setPart(new Fraction(1, 1));
		droitPP3.setRegime(GenrePropriete.INDIVIDUELLE);
		droitPP3.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 3, 28), "Achat", new IdentifiantAffaireRF(6, 2013, 28, 4)));
		pp2.addDroitPropriete(droitPP3);
		immeuble2.addDroitPropriete(droitPP3);

		final DroitProprieteImmeubleRF droitImm4 = new DroitProprieteImmeubleRF();
		droitImm4.setMasterIdRF("282002020");
		droitImm4.setVersionIdRF("1");
		droitImm4.setAyantDroit(ib3);
		droitImm4.setImmeuble(immeuble1);
		droitImm4.setDateDebut(dateImportInitial);
		droitImm4.setDateFin(null);
		droitImm4.setDateDebutMetier(RegDate.get(2010, 4, 4));
		droitImm4.setMotifDebut("Constitution de PPE");
		droitImm4.setMotifFin(null);
		droitImm4.setPart(new Fraction(1,14));
		droitImm4.setRegime(GenrePropriete.PPE);
		droitImm4.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 4), "Constitution de PPE", new IdentifiantAffaireRF(6, 2014, 203, 0)));
		ib3.addDroitPropriete(droitImm4);
		immeuble1.addDroitPropriete(droitImm4);

		// un mock avec les deux ayants-droits.
		ayantDroitRFDAO.save(pp1);
		ayantDroitRFDAO.save(pp2);
		ayantDroitRFDAO.save(ib3);

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
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits);

		// on envoie les trois mêmes droits sur les mêmes propriétaires et immeubles
		final PersonEigentumAnteil droit1 = newDroitPP("9a9c9e94923", "1", "37838sc9d94de", "382929efa218", new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		final PersonEigentumAnteil droit2 = newDroitPP("45729cd9e20", "1", "029191d4fec44", "382929efa218", new Fraction(1, 2), PersonEigentumsform.MITEIGENTUM, RegDate.get(2010, 4, 23), new IdentifiantAffaireRF(6, 2013, 33, 1), "Achat");
		final PersonEigentumAnteil droit3 = newDroitPP("38458fa0ac3", "1", "029191d4fec44", "202930c0e0f3", new Fraction(1, 1), PersonEigentumsform.ALLEINEIGENTUM, RegDate.get(2010, 3, 28), new IdentifiantAffaireRF(6, 2013, 28, 4), "Achat");
		final GrundstueckEigentumAnteil droit4 = newDroitImm("282002020", "1", "48238919011", "382929efa218", new Fraction(1, 14), GrundstueckEigentumsform.STOCKWERK, RegDate.get(2010, 4, 4), new IdentifiantAffaireRF(6, 2014, 203, 0), "Constitution de PPE");

		final List<EigentumAnteil> droits = Arrays.asList(droit1, droit2, droit3, droit4);
		detector.processDroitsPropriete(IMPORT_ID, 2, droits.iterator(), null);

		// on ne devrait pas avoir de mutation
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(0, mutations.size());
	}

	/**
	 * [SIFISC-28213 Ce test vérifie qu'aucune mutation n'est créée si les données des immeubles dans l'import sont identiques avec l'état courant des immeubles stockés dans la DB,
	 * malgré la présence d'une raison d'acquisition annulée..
	 */
	@Test
	public void testDroitsIdentiquesAvecRaisonAcquisitionAnnulee() throws Exception {


		final RegDate dateImportInitial = RegDate.get(2010, 6, 1);

		// les données déjà existantes dans le DB
		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setIdRF("029191d4fec44");

		final BienFondsRF immeuble2 = new BienFondsRF();
		immeuble2.setIdRF("202930c0e0f3");

		immeubleRFDAO.save(immeuble2);

		final DroitProprietePersonnePhysiqueRF droitPP3 = new DroitProprietePersonnePhysiqueRF();
		droitPP3.setMasterIdRF("38458fa0ac3");
		droitPP3.setVersionIdRF("1");
		droitPP3.setAyantDroit(pp2);
		droitPP3.setImmeuble(immeuble2);
		droitPP3.setCommunaute(null);
		droitPP3.setDateDebut(dateImportInitial);
		droitPP3.setDateFin(null);
		droitPP3.setDateDebutMetier(RegDate.get(2010, 3, 28));
		droitPP3.setMotifDebut("Achat");
		droitPP3.setMotifFin(null);
		droitPP3.setPart(new Fraction(1, 1));
		droitPP3.setRegime(GenrePropriete.INDIVIDUELLE);
		// on enregistre dans la base de données une raison d'acquisition annulée qui doit être ignorée par le détecteur
		final RaisonAcquisitionRF raisonAnnulee = new RaisonAcquisitionRF(RegDate.get(2000, 1, 1), "Génération spontanée", new IdentifiantAffaireRF(6, 2000, 1, 1));
		raisonAnnulee.setAnnule(true);
		droitPP3.addRaisonAcquisition(raisonAnnulee);
		droitPP3.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 3, 28), "Achat", new IdentifiantAffaireRF(6, 2013, 28, 4)));
		pp2.addDroitPropriete(droitPP3);
		immeuble2.addDroitPropriete(droitPP3);

		// un mock avec les deux ayants-droits.
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
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits);

		// on envoie les trois mêmes droits sur les mêmes propriétaires et immeubles
		final PersonEigentumAnteil droit3 = newDroitPP("38458fa0ac3", "1", "029191d4fec44", "202930c0e0f3", new Fraction(1, 1), PersonEigentumsform.ALLEINEIGENTUM, RegDate.get(2010, 3, 28), new IdentifiantAffaireRF(6, 2013, 28, 4), "Achat");

		final List<EigentumAnteil> droits = Collections.singletonList(droit3);
		detector.processDroitsPropriete(IMPORT_ID, 2, droits.iterator(), null);

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

		final ImmeubleBeneficiaireRF ib3 = new ImmeubleBeneficiaireRF();
		ib3.setIdRF("48238919011");

		final BienFondsRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF("382929efa218");

		final BienFondsRF immeuble2 = new BienFondsRF();
		immeuble2.setIdRF("202930c0e0f3");

		final ProprieteParEtageRF immeuble3 = new ProprieteParEtageRF();
		immeuble3.setIdRF(ib3.getIdRF());

		immeubleRFDAO.save(immeuble1);
		immeubleRFDAO.save(immeuble2);
		immeubleRFDAO.save(immeuble3);
		immeuble3.setDroitsPropriete(Collections.emptySet());

		final DroitProprietePersonnePhysiqueRF droitPP1 = new DroitProprietePersonnePhysiqueRF();
		droitPP1.setMasterIdRF("9a9c9e94923");
		droitPP1.setVersionIdRF("1");
		droitPP1.setAyantDroit(pp1);
		droitPP1.setImmeuble(immeuble1);
		droitPP1.setCommunaute(null);
		droitPP1.setDateDebut(dateImportInitial);
		droitPP1.setDateFin(null);
		droitPP1.setDateDebutMetier(RegDate.get(2010, 4, 23));
		droitPP1.setMotifDebut("Achat");
		droitPP1.setMotifFin(null);
		droitPP1.setPart(new Fraction(1, 2));
		droitPP1.setRegime(GenrePropriete.COPROPRIETE);
		droitPP1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 23), "Achat", new IdentifiantAffaireRF(6, 2013, 33, 1)));
		pp1.addDroitPropriete(droitPP1);
		immeuble1.addDroitPropriete(droitPP1);

		final DroitProprietePersonnePhysiqueRF droitPP2 = new DroitProprietePersonnePhysiqueRF();
		droitPP2.setMasterIdRF("45729cd9e20");
		droitPP2.setVersionIdRF("1");
		droitPP2.setAyantDroit(pp2);
		droitPP2.setImmeuble(immeuble1);
		droitPP2.setCommunaute(null);
		droitPP2.setDateDebut(dateImportInitial);
		droitPP2.setDateFin(null);
		droitPP2.setDateDebutMetier(RegDate.get(2010, 4, 23));
		droitPP2.setMotifDebut("Achat");
		droitPP2.setMotifFin(null);
		droitPP2.setPart(new Fraction(1, 2));
		droitPP2.setRegime(GenrePropriete.COPROPRIETE);
		droitPP2.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 23), "Achat", new IdentifiantAffaireRF(6, 2013, 33, 1)));
		pp2.addDroitPropriete(droitPP2);
		immeuble1.addDroitPropriete(droitPP2);

		final DroitProprietePersonnePhysiqueRF droitPP3 = new DroitProprietePersonnePhysiqueRF();
		droitPP3.setMasterIdRF("38458fa0ac3");
		droitPP3.setVersionIdRF("1");
		droitPP3.setAyantDroit(pp2);
		droitPP3.setImmeuble(immeuble2);
		droitPP3.setCommunaute(null);
		droitPP3.setDateDebut(dateImportInitial);
		droitPP3.setDateFin(null);
		droitPP3.setDateDebutMetier(RegDate.get(2010, 3, 28));
		droitPP3.setMotifDebut("Achat");
		droitPP3.setMotifFin(null);
		droitPP3.setPart(new Fraction(1, 1));
		droitPP3.setRegime(GenrePropriete.INDIVIDUELLE);
		droitPP3.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 3, 28), "Achat", new IdentifiantAffaireRF(6, 2013, 28, 4)));
		pp2.addDroitPropriete(droitPP3);
		immeuble2.addDroitPropriete(droitPP3);

		final DroitProprieteImmeubleRF droitImm4 = new DroitProprieteImmeubleRF();
		droitImm4.setMasterIdRF("282002020");
		droitImm4.setVersionIdRF("1");
		droitImm4.setAyantDroit(ib3);
		droitImm4.setImmeuble(immeuble1);
		droitImm4.setDateDebut(dateImportInitial);
		droitImm4.setDateFin(null);
		droitImm4.setDateDebutMetier(RegDate.get(2010, 4, 4));
		droitImm4.setMotifDebut("Constitution de PPE");
		droitImm4.setMotifFin(null);
		droitImm4.setPart(new Fraction(1,14));
		droitImm4.setRegime(GenrePropriete.PPE);
		droitImm4.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 4), "Constitution de PPE", new IdentifiantAffaireRF(6, 2014, 203, 0)));
		ib3.addDroitPropriete(droitImm4);
		immeuble1.addDroitPropriete(droitImm4);

		// un mock avec les deux ayants-droits.
		ayantDroitRFDAO.save(pp1);
		ayantDroitRFDAO.save(pp2);
		ayantDroitRFDAO.save(ib3);

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
		final DroitRFDetector detector = new DroitRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, ayantDroitRFDetector, cacheDroits);

		// on envoie une liste de droits vide
		detector.processDroitsPropriete(IMPORT_ID, 2, Collections.<EigentumAnteil>emptyList().iterator(), null);

		// on devrait avoir 2 événements de mutation de type SUPPRESSION (un par immeuble ayant des droits)
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		mutations.sort(new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut0.getTypeMutation());
		assertEquals("202930c0e0f3", mut0.getIdRF());  // le premier immeuble
		assertNull(mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.DROIT, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut1.getTypeMutation());
		assertEquals("382929efa218", mut1.getIdRF());  // le second immeuble
		assertNull(mut1.getXmlContent());
	}

	private static PersonEigentumAnteil newDroitPP(String idRfDroit, String versionIdDroit, String idRfPP, String idRfImmeuble, Fraction part, PersonEigentumsform typePropriete, RegDate dateDebutEffective, IdentifiantAffaireRF affaire,
	                                               String motifDebut) {

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
		droit.setVersionID(versionIdDroit);
		droit.setNatuerlichePersonGb(natuerliche);
		droit.setBelastetesGrundstueckIDREF(idRfImmeuble);
		droit.setQuote(new Quote((long) part.getNumerateur(), (long) part.getDenominateur(), null, null));
		droit.setPersonEigentumsForm(typePropriete);

		return droit;
	}

	private static GrundstueckEigentumAnteil newDroitImm(String idRfDroit, String versionIdDroit, String idRfImmeubleDominant, String idRfImmeubleServant, Fraction part, GrundstueckEigentumsform typePropriete, RegDate dateDebutEffective,
	                                                     IdentifiantAffaireRF affaire, String motifDebut) {

		final Rechtsgrund recht = new Rechtsgrund();
		recht.setBelegDatum(dateDebutEffective);
		recht.setAmtNummer(affaire.getNumeroOffice());
		recht.setBelegAlt(affaire.getNumeroAffaire());
		recht.setRechtsgrundCode(new CapiCode(null, motifDebut));

		final GrundstueckEigentumAnteil droit = new GrundstueckEigentumAnteil();
		droit.setMasterID(idRfDroit);
		droit.setVersionID(versionIdDroit);
		droit.setBerechtigtesGrundstueckIDREF(idRfImmeubleDominant);
		droit.setBelastetesGrundstueckIDREF(idRfImmeubleServant);
		droit.setQuote(new Quote((long) part.getNumerateur(), (long) part.getDenominateur(), null, null));
		droit.setGrundstueckEigentumsForm(typePropriete);
		droit.getRechtsgruende().add(recht);

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
