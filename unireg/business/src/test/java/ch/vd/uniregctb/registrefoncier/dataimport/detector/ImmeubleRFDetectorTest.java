package ch.vd.uniregctb.registrefoncier.dataimport.detector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.grundstueck.AmtlicheBewertung;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.GrundstueckNummer;
import ch.vd.capitastra.grundstueck.Liegenschaft;
import ch.vd.capitastra.grundstueck.Quote;
import ch.vd.capitastra.grundstueck.StammGrundstueck;
import ch.vd.capitastra.grundstueck.StockwerksEinheit;
import ch.vd.capitastra.grundstueck.UnbekanntesGrundstueck;
import ch.vd.registre.base.date.RegDate;
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
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.QuotePartRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.MockCommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.MockImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRFImpl;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;
import ch.vd.uniregctb.transaction.MockTransactionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(UniregJUnit4Runner.class)
public class ImmeubleRFDetectorTest {

	private static final Long IMPORT_ID = 1L;
	private XmlHelperRF xmlHelperRF;
	private PlatformTransactionManager transactionManager;
	private CommuneRFDAO communeRFDAO;

	@Before
	public void setUp() throws Exception {
		xmlHelperRF = new XmlHelperRFImpl();
		transactionManager = new MockTransactionManager();
		communeRFDAO = new MockCommuneRFDAO(new CommuneRF(2233, "Le-gros-du-lac", 5555),
		                                    new CommuneRF(238, "Lausanne", 5586),
		                                    new CommuneRF(273, "Thierrens", 5689));
		AuthenticationHelper.pushPrincipal("test-user");
	}

	@After
	public void tearDown() throws Exception {
		AuthenticationHelper.popPrincipal();
	}

	/**
	 * Ce test vérifie que les immeubles flaggés comme des "copies" sont complétement ignorés
	 */
	@Test
	public void testImmeublesCopies() throws Exception {

		// un mock de DAO qui simule une base vide
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAO() {
			@Nullable
			@Override
			public ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushMode flushModeOverride) {
				return null;
			}
		};

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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie deux nouveaux immeubles
		final UnbekanntesGrundstueck kopie0 = newKopie(2233, "Le-gros-du-lac", 109, 17, 500000L, "2016", RegDate.get(2016, 1, 1), true, "382929efa218", "CH282891891");
		final UnbekanntesGrundstueck kopie1 = newKopie(238, "Lausanne", 1022, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, "23af3efe44", "CH8383820002");
		final List<Grundstueck> immeubles = Arrays.asList(kopie0, kopie1);
		detector.processImmeubles(IMPORT_ID, 2, immeubles.iterator(), null);

		// on ne devrait avoir aucune mutation
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(0, mutations.size());
	}

	/**
	 * Ce test vérifie que des mutations de type CREATION sont bien créées lorsqu'aucun des immeubles dans l'import n'existe dans la base de données.
	 */
	@Test
	public void testNouveauxImmeubles() throws Exception {

		// un mock de DAO qui simule une base vide
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAO() {
			@Nullable
			@Override
			public ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushMode flushModeOverride) {
				return null;
			}
		};

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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie deux nouveaux immeubles
		final Liegenschaft bienfond = newBienFond(2233, "Le-gros-du-lac", 109, 17, 500000L, "2016", RegDate.get(2016, 1, 1), true, "382929efa218", "CH282891891", true);
		final StockwerksEinheit ppe = newPPE(238, "Lausanne", 1022, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, "23af3efe44", "CH8383820002", new Fraction(1, 1));
		final List<Grundstueck> immeubles = Arrays.asList(bienfond, ppe);
		detector.processImmeubles(IMPORT_ID, 2, immeubles.iterator(), null);

		// on devrait avoir deux événements de mutation de type CREATION à l'état A_TRAITER dans la base
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.IMMEUBLE, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
		assertEquals("382929efa218", mut0.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Liegenschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckID>382929efa218</GrundstueckID>\n" +
				             "    <EGrid>CH282891891</EGrid>\n" +
				             "    <GrundstueckNummer>\n" +
				             "        <BfsNr>2233</BfsNr>\n" +
				             "        <Gemeindenamen>Le-gros-du-lac</Gemeindenamen>\n" +
				             "        <StammNr>109</StammNr>\n" +
				             "        <IndexNr1>17</IndexNr1>\n" +
				             "    </GrundstueckNummer>\n" +
				             "    <IstKopie>false</IstKopie>\n" +
				             "    <AmtlicheBewertung>\n" +
				             "        <AmtlicherWert>500000</AmtlicherWert>\n" +
				             "        <ProtokollNr>2016</ProtokollNr>\n" +
				             "        <ProtokollDatum>2016-01-01</ProtokollDatum>\n" +
				             "        <ProtokollGueltig>false</ProtokollGueltig>\n" +
				             "    </AmtlicheBewertung>\n" +
				             "    <LigUnterartEnum>cfa</LigUnterartEnum>\n" +
				             "</Liegenschaft>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.IMMEUBLE, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
		assertEquals("23af3efe44", mut1.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<StockwerksEinheit xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckID>23af3efe44</GrundstueckID>\n" +
				             "    <EGrid>CH8383820002</EGrid>\n" +
				             "    <GrundstueckNummer>\n" +
				             "        <BfsNr>238</BfsNr>\n" +
				             "        <Gemeindenamen>Lausanne</Gemeindenamen>\n" +
				             "        <StammNr>1022</StammNr>\n" +
				             "    </GrundstueckNummer>\n" +
				             "    <IstKopie>false</IstKopie>\n" +
				             "    <AmtlicheBewertung>\n" +
				             "        <AmtlicherWert>250000</AmtlicherWert>\n" +
				             "        <ProtokollNr>RG97</ProtokollNr>\n" +
				             "        <ProtokollDatum>1997-01-01</ProtokollDatum>\n" +
				             "        <ProtokollGueltig>true</ProtokollGueltig>\n" +
				             "    </AmtlicheBewertung>\n" +
				             "    <StammGrundstueck>\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>1</AnteilNenner>\n" +
				             "            <QuoteUnbekannt>false</QuoteUnbekannt>\n" +
				             "        </Quote>\n" +
				             "    </StammGrundstueck>\n" +
				             "</StockwerksEinheit>\n", mut1.getXmlContent());
	}

	/**
	 * [SIFISC-22366] Ce test vérifie que les immeubles blacklistés sont bien ignorés.
	 */
	@Test
	public void testNouvelImmeubleBlackliste() throws Exception {

		// un mock de DAO qui simule une base vide
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAO() {
			@Nullable
			@Override
			public ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushMode flushModeOverride) {
				return null;
			}
		};

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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie un immeuble blacklisté
		final Liegenschaft bienfond = newBienFond(2233, "Le-gros-du-lac", 109, 17, 500000L, "2016", RegDate.get(2016, 1, 1), true, "_1f1091523810108101381012b3d64cb4", "CH282891891", true);
		final List<Grundstueck> immeubles = Collections.singletonList(bienfond);
		detector.processImmeubles(IMPORT_ID, 2, immeubles.iterator(), null);

		// on ne devrait pas avoir de mutations
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(0, mutations.size());
	}

	/**
	 * Ce test vérifie que des mutations de type MODIFICATION sont bien créées les immeubles dans l'import existent dans la base de données mais pas avec les mêmes valeurs.
	 */
	@Test
	public void testImmeublesModifies() throws Exception {

		final String idRfBienFond = "382929efa218";
		final String idRfPPE = "23af3efe44";

		// un mock de DAO qui simule l'existence des deux immeubles
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAODeuxImmeubles(idRfBienFond, idRfPPE);

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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les immeubles avec des modifications
		// - nouvelle estimation fiscale
		final Liegenschaft bienfondImport = newBienFond(2233, "Le-gros-du-lac", 109, 17, 500000L, "2016", RegDate.get(2016, 1, 1), false, idRfBienFond, "CH282891891", true);
		// - changement de numéro de parcelle
		final StockwerksEinheit ppeImport = newPPE(273, "Thierrens", 1022, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, idRfPPE, "CH8383820002", new Fraction(1, 1));
		final List<Grundstueck> immeublesImport = Arrays.asList(bienfondImport, ppeImport);
		detector.processImmeubles(IMPORT_ID, 2, immeublesImport.listIterator(), null);

		// on devrait avoir deux événements de mutation de type MODIFICATION à l'état A_TRAITER dans la base
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.IMMEUBLE, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
		assertEquals("382929efa218", mut0.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Liegenschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckID>382929efa218</GrundstueckID>\n" +
				             "    <EGrid>CH282891891</EGrid>\n" +
				             "    <GrundstueckNummer>\n" +
				             "        <BfsNr>2233</BfsNr>\n" +
				             "        <Gemeindenamen>Le-gros-du-lac</Gemeindenamen>\n" +
				             "        <StammNr>109</StammNr>\n" +
				             "        <IndexNr1>17</IndexNr1>\n" +
				             "    </GrundstueckNummer>\n" +
				             "    <IstKopie>false</IstKopie>\n" +
				             "    <AmtlicheBewertung>\n" +
				             "        <AmtlicherWert>500000</AmtlicherWert>\n" +
				             "        <ProtokollNr>2016</ProtokollNr>\n" +
				             "        <ProtokollDatum>2016-01-01</ProtokollDatum>\n" +
				             "        <ProtokollGueltig>true</ProtokollGueltig>\n" +
				             "    </AmtlicheBewertung>\n" +
				             "    <LigUnterartEnum>cfa</LigUnterartEnum>\n" +
				             "</Liegenschaft>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.IMMEUBLE, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut1.getTypeMutation());
		assertEquals("23af3efe44", mut1.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<StockwerksEinheit xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckID>23af3efe44</GrundstueckID>\n" +
				             "    <EGrid>CH8383820002</EGrid>\n" +
				             "    <GrundstueckNummer>\n" +
				             "        <BfsNr>273</BfsNr>\n" +
				             "        <Gemeindenamen>Thierrens</Gemeindenamen>\n" +
				             "        <StammNr>1022</StammNr>\n" +
				             "    </GrundstueckNummer>\n" +
				             "    <IstKopie>false</IstKopie>\n" +
				             "    <AmtlicheBewertung>\n" +
				             "        <AmtlicherWert>250000</AmtlicherWert>\n" +
				             "        <ProtokollNr>RG97</ProtokollNr>\n" +
				             "        <ProtokollDatum>1997-01-01</ProtokollDatum>\n" +
				             "        <ProtokollGueltig>true</ProtokollGueltig>\n" +
				             "    </AmtlicheBewertung>\n" +
				             "    <StammGrundstueck>\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>1</AnteilNenner>\n" +
				             "            <QuoteUnbekannt>false</QuoteUnbekannt>\n" +
				             "        </Quote>\n" +
				             "    </StammGrundstueck>\n" +
				             "</StockwerksEinheit>\n", mut1.getXmlContent());
	}

	/**
	 * [SIFISC-25610] Ce test vérifie qu'une mutation de type MODIFICATION est bien créée sur un immeuble qui existe dans l'import avec un egrid différent de celui stocké en DB.
	 */
	@Test
	public void testImmeublesEgridModifies() throws Exception {

		final String idRfBienFonds = "382929efa218";

		final CommuneRF commune = new CommuneRF(2233, "Le-gros-du-lac", 6666);

		final SituationRF situation = new SituationRF();
		situation.setDateDebut(RegDate.get(2016, 1, 1));
		situation.setNoParcelle(212);
		situation.setCommune(commune);

		final EstimationRF estimation = new EstimationRF();
		estimation.setAnneeReference(2016);
		estimation.setMontant(500000L);
		estimation.setReference("2016");
		estimation.setDateInscription(RegDate.get(2016, 1, 1));

		final BienFondRF bienFonds = new BienFondRF();
		bienFonds.setIdRF(idRfBienFonds);
		bienFonds.addSituation(situation);
		bienFonds.addEstimation(estimation);
		bienFonds.setEgrid(null);   // <--- Egrid non-renseigné
		bienFonds.setSurfacesTotales(Collections.emptySet());

		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAO(bienFonds);

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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les immeubles avec des modifications
		// - egrid renseigné
		final Liegenschaft bienfondsImport = newBienFond(2233, "Le-gros-du-lac", 212, null,
		                                                 500000L, "2016", RegDate.get(2016, 1, 1), false,
		                                                 idRfBienFonds, "CH282891891", false);
		final List<Grundstueck> immeublesImport = Collections.singletonList(bienfondsImport);
		detector.processImmeubles(IMPORT_ID, 2, immeublesImport.listIterator(), null);

		// on devrait avoir deux événements de mutation de type MODIFICATION à l'état A_TRAITER dans la base
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(1, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.IMMEUBLE, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
		assertEquals("382929efa218", mut0.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Liegenschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckID>382929efa218</GrundstueckID>\n" +
				             "    <EGrid>CH282891891</EGrid>\n" +
				             "    <GrundstueckNummer>\n" +
				             "        <BfsNr>2233</BfsNr>\n" +
				             "        <Gemeindenamen>Le-gros-du-lac</Gemeindenamen>\n" +
				             "        <StammNr>212</StammNr>\n" +
				             "    </GrundstueckNummer>\n" +
				             "    <IstKopie>false</IstKopie>\n" +
				             "    <AmtlicheBewertung>\n" +
				             "        <AmtlicherWert>500000</AmtlicherWert>\n" +
				             "        <ProtokollNr>2016</ProtokollNr>\n" +
				             "        <ProtokollDatum>2016-01-01</ProtokollDatum>\n" +
				             "        <ProtokollGueltig>true</ProtokollGueltig>\n" +
				             "    </AmtlicheBewertung>\n" +
				             "    <LigUnterartEnum></LigUnterartEnum>\n" +
				             "</Liegenschaft>\n", mut0.getXmlContent());

	}

	/**
	 * Ce test vérifie qu'aucune mutation n'est créée si les données des immeubles et des communes dans l'import sont identiques avec les états courants des immeubles et des communes stockés dans la DB.
	 */
	@Test
	public void testImmeublesEtCommunesIdentiques() throws Exception {

		final String idRfBienFond = "382929efa218";
		final String idRfPPE = "23af3efe44";

		// un mock de DAO qui simule l'existence des deux immeubles
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAODeuxImmeubles(idRfBienFond, idRfPPE);

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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les immeubles avec les mêmes données que celles dans la DB
		final Liegenschaft bienfondImport = newBienFond(2233, "Le-gros-du-lac", 109, 17, 450000, "2015", RegDate.get(2015, 7, 1), false, idRfBienFond, "CH282891891", true);
		final StockwerksEinheit ppeImport = newPPE(273, "Thierrens", 46, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, idRfPPE, "CH8383820002", new Fraction(1, 1));
		final List<Grundstueck> immeublesImport = Arrays.asList(bienfondImport, ppeImport);
		detector.processImmeubles(IMPORT_ID, 2, immeublesImport.listIterator(), null);

		// on ne devrait pas avoir de mutation
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(0, mutations.size());
	}

	/**
	 * Ce test vérifie que des mutations de suppression sont créées si des immeubles actifs existent dans la base mais pas dans le fichier d'import.
	 */
	@Test
	public void testImmeublesRadies() throws Exception {

		final BienFondRF bienFond = new BienFondRF();
		bienFond.setIdRF("382929efa218");
		final ProprieteParEtageRF ppe = new ProprieteParEtageRF();
		ppe.setIdRF("23af3efe44");

		// un mock de DAO qui simule l'existence des deux immeubles
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAO(bienFond, ppe);

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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie un import sans immeuble
		final List<Grundstueck> immeublesImport = Collections.emptyList();
		detector.processImmeubles(IMPORT_ID, 2, immeublesImport.listIterator(), null);

		// on devrait avoir deux mutations de suppression des immeubles
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		Collections.sort(mutations, (l, r) -> l.getIdRF().compareTo(r.getIdRF()));

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.IMMEUBLE, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut0.getTypeMutation());
		assertEquals("23af3efe44", mut0.getIdRF());
		assertNull(mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.IMMEUBLE, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut1.getTypeMutation());
		assertEquals("382929efa218", mut1.getIdRF());
		assertNull(mut1.getXmlContent());
	}

	@NotNull
	private static ProprieteParEtageRF newProprieteParEtageRFThierrens(String idRfPPE) {
		final ProprieteParEtageRF ppe = new ProprieteParEtageRF();
		{
			final CommuneRF commune = new CommuneRF();
			commune.setNoRf(273);
			commune.setNomRf("Thierrens");

			final SituationRF situation = new SituationRF();
			situation.setCommune(commune); // Thierrens
			situation.setNoParcelle(46);

			final EstimationRF estimation = new EstimationRF();
			estimation.setMontant(250000L);
			estimation.setReference("RG97");
			estimation.setAnneeReference(1997);
			estimation.setDateInscription(RegDate.get(1997, 1, 1));
			estimation.setEnRevision(false);
			estimation.setDateDebut(RegDate.get(2000, 1, 1));

			ppe.setIdRF(idRfPPE);
			ppe.addQuotePart(new QuotePartRF(null, null, new Fraction(1, 1)));
			ppe.setEgrid("CH8383820002");
			ppe.addSituation(situation);
			ppe.addEstimation(estimation);
			ppe.setSurfacesTotales(Collections.emptySet());
		}
		return ppe;
	}

	/**
	 * Ce test vérifie que des mutations de type CREATION sur les communes sont bien créées lorsqu'aucune des communes dans l'import n'existe dans la base de données.
	 */
	@Test
	public void testNouvellesCommunes() throws Exception {

		final String idRfBienFond = "382929efa218";
		final String idRfPPE = "23af3efe44";

		// des mocks de DAO qui simulent une base vide
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAO() {
			@Nullable
			@Override
			public ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushMode flushModeOverride) {
				return null;
			}
		};
		communeRFDAO = new MockCommuneRFDAO();

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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie deux nouveaux immeubles sur deux nouvelles communes
		final Liegenschaft bienfondImport = newBienFond(2233, "Le-gros-du-lac", 109, 17, 450000, "2015", RegDate.get(2015, 7, 1), false, idRfBienFond, "CH282891891", true);
		final StockwerksEinheit ppeImport = newPPE(273, "Thierrens", 46, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, idRfPPE, "CH8383820002", new Fraction(1, 1));
		final List<Grundstueck> immeublesImport = Arrays.asList(bienfondImport, ppeImport);
		detector.processImmeubles(IMPORT_ID, 2, immeublesImport.listIterator(), null);

		// on devrait avoir quatre événements de mutation de type CREATION à l'état A_TRAITER dans la base (deux pour les immeubles et deux pour les communes)
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(4, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.IMMEUBLE, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
		assertEquals("382929efa218", mut0.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Liegenschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckID>382929efa218</GrundstueckID>\n" +
				             "    <EGrid>CH282891891</EGrid>\n" +
				             "    <GrundstueckNummer>\n" +
				             "        <BfsNr>2233</BfsNr>\n" +
				             "        <Gemeindenamen>Le-gros-du-lac</Gemeindenamen>\n" +
				             "        <StammNr>109</StammNr>\n" +
				             "        <IndexNr1>17</IndexNr1>\n" +
				             "    </GrundstueckNummer>\n" +
				             "    <IstKopie>false</IstKopie>\n" +
				             "    <AmtlicheBewertung>\n" +
				             "        <AmtlicherWert>450000</AmtlicherWert>\n" +
				             "        <ProtokollNr>2015</ProtokollNr>\n" +
				             "        <ProtokollDatum>2015-07-01</ProtokollDatum>\n" +
				             "        <ProtokollGueltig>true</ProtokollGueltig>\n" +
				             "    </AmtlicheBewertung>\n" +
				             "    <LigUnterartEnum>cfa</LigUnterartEnum>\n" +
				             "</Liegenschaft>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.IMMEUBLE, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
		assertEquals("23af3efe44", mut1.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<StockwerksEinheit xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckID>23af3efe44</GrundstueckID>\n" +
				             "    <EGrid>CH8383820002</EGrid>\n" +
				             "    <GrundstueckNummer>\n" +
				             "        <BfsNr>273</BfsNr>\n" +
				             "        <Gemeindenamen>Thierrens</Gemeindenamen>\n" +
				             "        <StammNr>46</StammNr>\n" +
				             "    </GrundstueckNummer>\n" +
				             "    <IstKopie>false</IstKopie>\n" +
				             "    <AmtlicheBewertung>\n" +
				             "        <AmtlicherWert>250000</AmtlicherWert>\n" +
				             "        <ProtokollNr>RG97</ProtokollNr>\n" +
				             "        <ProtokollDatum>1997-01-01</ProtokollDatum>\n" +
				             "        <ProtokollGueltig>true</ProtokollGueltig>\n" +
				             "    </AmtlicheBewertung>\n" +
				             "    <StammGrundstueck>\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>1</AnteilNenner>\n" +
				             "            <QuoteUnbekannt>false</QuoteUnbekannt>\n" +
				             "        </Quote>\n" +
				             "    </StammGrundstueck>\n" +
				             "</StockwerksEinheit>\n", mut1.getXmlContent());

		final EvenementRFMutation mut2 = mutations.get(2);
		assertEquals(IMPORT_ID, mut2.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
		assertEquals(TypeEntiteRF.COMMUNE, mut2.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut2.getTypeMutation());
		assertEquals("273", mut2.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <BfsNr>273</BfsNr>\n" +
				             "    <Gemeindenamen>Thierrens</Gemeindenamen>\n" +
				             "    <StammNr>0</StammNr>\n" +
				             "</GrundstueckNummer>\n", mut2.getXmlContent());

		final EvenementRFMutation mut3 = mutations.get(3);
		assertEquals(IMPORT_ID, mut3.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut3.getEtat());
		assertEquals(TypeEntiteRF.COMMUNE, mut3.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut3.getTypeMutation());
		assertEquals("2233", mut3.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <BfsNr>2233</BfsNr>\n" +
				             "    <Gemeindenamen>Le-gros-du-lac</Gemeindenamen>\n" +
				             "    <StammNr>0</StammNr>\n" +
				             "</GrundstueckNummer>\n", mut3.getXmlContent());
	}

	/**
	 * Ce test vérifie que des mutations de type MODIFICATION sur les commuens sont bien créées si les communes dans l'import existent dans la base de données mais pas avec les mêmes valeurs.
	 */
	@Test
	public void testCommunesModifiees() throws Exception {

		final String idRfBienFond = "382929efa218";
		final String idRfPPE = "23af3efe44";

		// un mock de DAO qui simule l'existence des deux immeubles
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAODeuxImmeubles(idRfBienFond, idRfPPE);

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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les immeubles
		// - fusion de commune (Le-gros-du-lac -> Lac-Amour *avec* réutilisation du numéro de commune) et changement de numéro de parcelle
		final Liegenschaft bienfondImport = newBienFond(2233, "Lac-Amour", 2304, 17, 450000, "2015", RegDate.get(2015, 7, 1), false, idRfBienFond, "CH282891891", true);
		// - fusion de commune (Thierrens -> Montanair *sans* réutilisation du numéro de commune) et changement de numéro de parcelle
		final StockwerksEinheit ppeImport = newPPE(108, "Montanair", 1022, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, idRfPPE, "CH8383820002", new Fraction(1, 1));
		final List<Grundstueck> immeublesImport = Arrays.asList(bienfondImport, ppeImport);
		detector.processImmeubles(IMPORT_ID, 2, immeublesImport.listIterator(), null);

		// on devrait avoir quatre événements de mutation de type MODIFICATION à l'état A_TRAITER dans la base (deux sur les immeubles et deux autres sur les communes)
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(4, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.IMMEUBLE, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
		assertEquals("382929efa218", mut0.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Liegenschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckID>382929efa218</GrundstueckID>\n" +
				             "    <EGrid>CH282891891</EGrid>\n" +
				             "    <GrundstueckNummer>\n" +
				             "        <BfsNr>2233</BfsNr>\n" +
				             "        <Gemeindenamen>Lac-Amour</Gemeindenamen>\n" +
				             "        <StammNr>2304</StammNr>\n" +
				             "        <IndexNr1>17</IndexNr1>\n" +
				             "    </GrundstueckNummer>\n" +
				             "    <IstKopie>false</IstKopie>\n" +
				             "    <AmtlicheBewertung>\n" +
				             "        <AmtlicherWert>450000</AmtlicherWert>\n" +
				             "        <ProtokollNr>2015</ProtokollNr>\n" +
				             "        <ProtokollDatum>2015-07-01</ProtokollDatum>\n" +
				             "        <ProtokollGueltig>true</ProtokollGueltig>\n" +
				             "    </AmtlicheBewertung>\n" +
				             "    <LigUnterartEnum>cfa</LigUnterartEnum>\n" +
				             "</Liegenschaft>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.IMMEUBLE, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut1.getTypeMutation());
		assertEquals("23af3efe44", mut1.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<StockwerksEinheit xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckID>23af3efe44</GrundstueckID>\n" +
				             "    <EGrid>CH8383820002</EGrid>\n" +
				             "    <GrundstueckNummer>\n" +
				             "        <BfsNr>108</BfsNr>\n" +
				             "        <Gemeindenamen>Montanair</Gemeindenamen>\n" +
				             "        <StammNr>1022</StammNr>\n" +
				             "    </GrundstueckNummer>\n" +
				             "    <IstKopie>false</IstKopie>\n" +
				             "    <AmtlicheBewertung>\n" +
				             "        <AmtlicherWert>250000</AmtlicherWert>\n" +
				             "        <ProtokollNr>RG97</ProtokollNr>\n" +
				             "        <ProtokollDatum>1997-01-01</ProtokollDatum>\n" +
				             "        <ProtokollGueltig>true</ProtokollGueltig>\n" +
				             "    </AmtlicheBewertung>\n" +
				             "    <StammGrundstueck>\n" +
				             "        <Quote>\n" +
				             "            <AnteilZaehler>1</AnteilZaehler>\n" +
				             "            <AnteilNenner>1</AnteilNenner>\n" +
				             "            <QuoteUnbekannt>false</QuoteUnbekannt>\n" +
				             "        </Quote>\n" +
				             "    </StammGrundstueck>\n" +
				             "</StockwerksEinheit>\n", mut1.getXmlContent());

		final EvenementRFMutation mut2 = mutations.get(2);
		assertEquals(IMPORT_ID, mut2.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
		assertEquals(TypeEntiteRF.COMMUNE, mut2.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut2.getTypeMutation());
		assertEquals("2233", mut2.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <BfsNr>2233</BfsNr>\n" +
				             "    <Gemeindenamen>Lac-Amour</Gemeindenamen>\n" +
				             "    <StammNr>0</StammNr>\n" +
				             "</GrundstueckNummer>\n", mut2.getXmlContent());

		final EvenementRFMutation mut3 = mutations.get(3);
		assertEquals(IMPORT_ID, mut3.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut3.getEtat());
		assertEquals(TypeEntiteRF.COMMUNE, mut3.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut3.getTypeMutation());
		assertEquals("108", mut3.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <BfsNr>108</BfsNr>\n" +
				             "    <Gemeindenamen>Montanair</Gemeindenamen>\n" +
				             "    <StammNr>0</StammNr>\n" +
				             "</GrundstueckNummer>\n", mut3.getXmlContent());
	}

	@NotNull
	private static BienFondRF newBienFondRFGrosDuLac(String idRfBienFond) {
		final BienFondRF bienFond = new BienFondRF();
		{
			final CommuneRF commune = new CommuneRF();
			commune.setNoRf(2233);
			commune.setNomRf("Le-gros-du-lac");

			final SituationRF situation = new SituationRF();
			situation.setCommune(commune);
			situation.setNoParcelle(109);
			situation.setIndex1(17);

			final EstimationRF estimation = new EstimationRF();
			estimation.setMontant(450000L);
			estimation.setReference("2015");
			estimation.setAnneeReference(2015);
			estimation.setDateInscription(RegDate.get(2015, 7, 1));
			estimation.setEnRevision(false);
			estimation.setDateDebut(RegDate.get(2000, 1, 1));

			bienFond.setIdRF(idRfBienFond);
			bienFond.setCfa(true);
			bienFond.setEgrid("CH282891891");
			bienFond.addSituation(situation);
			bienFond.addEstimation(estimation);
			bienFond.setSurfacesTotales(Collections.emptySet());
		}
		return bienFond;
	}

	@NotNull
	private static Liegenschaft newBienFond(int noRfCommune, String nomCommune, int noParcelle, Integer index1,
	                                        long estimationFiscale, String referenceEstimation, RegDate dateInscription,
	                                        boolean enRevision, String idRF, String egrid, boolean cfa) {
		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(noRfCommune);
		grundstueckNummer.setGemeindenamen(nomCommune);
		grundstueckNummer.setStammNr(noParcelle);
		grundstueckNummer.setIndexNr1(index1);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(estimationFiscale);
		amtlicheBewertung.setProtokollNr(referenceEstimation);
		amtlicheBewertung.setProtokollDatum(dateInscription);
		amtlicheBewertung.setProtokollGueltig(!enRevision);

		final Liegenschaft grundstueck = new Liegenschaft();
		grundstueck.setGrundstueckID(idRF);
		grundstueck.setLigUnterartEnum(cfa ? "cfa" : "");
		grundstueck.setEGrid(egrid);
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);

		return grundstueck;
	}

	@NotNull
	private static StockwerksEinheit newPPE(int noRfCommune, String nomCommune, int noParcelle, Integer index1,
	                                        long estimationFiscale, String referenceEstimation, RegDate dateInscription,
	                                        boolean enRevision, String idRF, String egrid, Fraction quotepart) {
		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(noRfCommune);
		grundstueckNummer.setGemeindenamen(nomCommune);
		grundstueckNummer.setStammNr(noParcelle);
		grundstueckNummer.setIndexNr1(index1);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(estimationFiscale);
		amtlicheBewertung.setProtokollNr(referenceEstimation);
		amtlicheBewertung.setProtokollDatum(dateInscription);
		amtlicheBewertung.setProtokollGueltig(!enRevision);

		final StockwerksEinheit grundstueck = new StockwerksEinheit();
		grundstueck.setGrundstueckID(idRF);
		grundstueck.setStammGrundstueck(new StammGrundstueck(new Quote((long) quotepart.getNumerateur(), (long) quotepart.getDenominateur(), null, false), null, null));
		grundstueck.setEGrid(egrid);
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);

		return grundstueck;
	}

	private static UnbekanntesGrundstueck newKopie(int noRfCommune, String nomCommune, int noParcelle, Integer index1,
	                                               long estimationFiscale, String referenceEstimation, RegDate dateInscription,
	                                               boolean enRevision, String idRF, String egrid) {

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(noRfCommune);
		grundstueckNummer.setGemeindenamen(nomCommune);
		grundstueckNummer.setStammNr(noParcelle);
		grundstueckNummer.setIndexNr1(index1);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(estimationFiscale);
		amtlicheBewertung.setProtokollNr(referenceEstimation);
		amtlicheBewertung.setProtokollDatum(dateInscription);
		amtlicheBewertung.setProtokollGueltig(!enRevision);

		final UnbekanntesGrundstueck grundstueck = new UnbekanntesGrundstueck();
		grundstueck.setGrundstueckID(idRF);
		grundstueck.setEGrid(egrid);
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);
		grundstueck.setIstKopie(true);

		return grundstueck;
	}

	private class MockImmeubleRFDAODeuxImmeubles extends MockImmeubleRFDAO {

		private final BienFondRF bienFond;
		private final ProprieteParEtageRF ppe;
		private final String idRfBienFond;
		private final String idRfPPE;

		public MockImmeubleRFDAODeuxImmeubles(String idRfBienFond, String idRfPPE) {
			this.idRfBienFond = idRfBienFond;
			this.idRfPPE = idRfPPE;
			this.bienFond = newBienFondRFGrosDuLac(idRfBienFond);
			this.ppe = newProprieteParEtageRFThierrens(idRfPPE);
		}

		@Nullable
		@Override
		public ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushMode flushModeOverride) {
			if (key.getIdRF().equals(idRfBienFond)) {
				return bienFond;
			}
			else if (key.getIdRF().equals(idRfPPE)) {
				return ppe;
			}
			return null;
		}
	}
}