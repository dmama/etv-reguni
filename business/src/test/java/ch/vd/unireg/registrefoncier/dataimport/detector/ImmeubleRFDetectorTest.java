package ch.vd.unireg.registrefoncier.dataimport.detector;

import javax.persistence.FlushModeType;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.ProprieteParEtageRF;
import ch.vd.unireg.registrefoncier.QuotePartRF;
import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.registrefoncier.dao.CommuneRFDAO;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dao.MockCommuneRFDAO;
import ch.vd.unireg.registrefoncier.dao.MockImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRFImpl;
import ch.vd.unireg.registrefoncier.dataimport.helper.BlacklistRFHelper;
import ch.vd.unireg.registrefoncier.key.ImmeubleRFKey;
import ch.vd.unireg.transaction.MockTransactionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ImmeubleRFDetectorTest {

	private static final Long IMPORT_ID = 1L;
	private static final int NO_RF_LAUSANNE = 238;
	private static final int NO_RF_THIERRENS = 273;
	private static final int NO_RF_GROS_DU_LAC = 888;
	private static final int NO_OFS_LAUSANNE = 5586;
	private static final int NO_OFS_THIERRENS = 5689;
	private static final int NO_OFS_GROS_DU_LAC = 8888;

	private XmlHelperRF xmlHelperRF;
	private BlacklistRFHelper blacklistRFHelper;
	private PlatformTransactionManager transactionManager;
	private CommuneRFDAO communeRFDAO;

	@Before
	public void setUp() throws Exception {
		xmlHelperRF = new XmlHelperRFImpl();
		blacklistRFHelper = idRF -> idRF.equals("_1f1091523810108101381012b3d64cb4");
		transactionManager = new MockTransactionManager();
		communeRFDAO = new MockCommuneRFDAO(new CommuneRF(NO_RF_GROS_DU_LAC, "Le-gros-du-lac", NO_OFS_GROS_DU_LAC),
		                                    new CommuneRF(NO_RF_LAUSANNE, "Lausanne", NO_OFS_LAUSANNE),
		                                    new CommuneRF(NO_RF_THIERRENS, "Thierrens", NO_OFS_THIERRENS));
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
			public ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushModeType flushModeOverride) {
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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie deux nouveaux immeubles
		final UnbekanntesGrundstueck kopie0 = newKopie(NO_RF_GROS_DU_LAC, "Le-gros-du-lac", 109, 17, 500000L, "2016", RegDate.get(2016, 1, 1), true, "382929efa218", "CH282891891");
		final UnbekanntesGrundstueck kopie1 = newKopie(NO_RF_LAUSANNE, "Lausanne", 1022, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, "23af3efe44", "CH8383820002");
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
			public ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushModeType flushModeOverride) {
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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie deux nouveaux immeubles
		final Liegenschaft bienfonds = newBienFonds(NO_RF_GROS_DU_LAC, "Le-gros-du-lac", 109, 17, 500000L, "2016", RegDate.get(2016, 1, 1), true, "382929efa218", "CH282891891", true);
		final StockwerksEinheit ppe = newPPE(NO_RF_LAUSANNE, "Lausanne", 1022, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, "23af3efe44", "CH8383820002", new Fraction(1, 1));
		final List<Grundstueck> immeubles = Arrays.asList(bienfonds, ppe);
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
				             "        <BfsNr>888</BfsNr>\n" +
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
			public ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushModeType flushModeOverride) {
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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie un immeuble blacklisté
		final Liegenschaft bienfonds = newBienFonds(NO_RF_GROS_DU_LAC, "Le-gros-du-lac", 109, 17, 500000L, "2016", RegDate.get(2016, 1, 1), true, "_1f1091523810108101381012b3d64cb4", "CH282891891", true);
		final List<Grundstueck> immeubles = Collections.singletonList(bienfonds);
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

		final String idRfBienFonds = "382929efa218";
		final String idRfPPE = "23af3efe44";

		// un mock de DAO qui simule l'existence des deux immeubles
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAODeuxImmeubles(idRfBienFonds, idRfPPE);

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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les immeubles avec des modifications
		// - nouvelle estimation fiscale
		final Liegenschaft bienfondsImport = newBienFonds(NO_RF_GROS_DU_LAC, "Le-gros-du-lac", 109, 17, 500000L, "2016", RegDate.get(2016, 1, 1), false, idRfBienFonds, "CH282891891", true);
		// - changement de numéro de parcelle
		final StockwerksEinheit ppeImport = newPPE(NO_RF_THIERRENS, "Thierrens", 1022, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, idRfPPE, "CH8383820002", new Fraction(1, 1));
		final List<Grundstueck> immeublesImport = Arrays.asList(bienfondsImport, ppeImport);
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
				             "        <BfsNr>888</BfsNr>\n" +
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

		final CommuneRF commune = new CommuneRF(NO_RF_GROS_DU_LAC, "Le-gros-du-lac", 6666);

		final SituationRF situation = new SituationRF();
		situation.setDateDebut(RegDate.get(2016, 1, 1));
		situation.setNoParcelle(212);
		situation.setCommune(commune);

		final EstimationRF estimation = new EstimationRF();
		estimation.setAnneeReference(2016);
		estimation.setMontant(500000L);
		estimation.setReference("2016");
		estimation.setDateInscription(RegDate.get(2016, 1, 1));

		final BienFondsRF bienFonds = new BienFondsRF();
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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les immeubles avec des modifications
		// - egrid renseigné
		final Liegenschaft bienfondsImport = newBienFonds(NO_RF_GROS_DU_LAC, "Le-gros-du-lac", 212, null,
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
				             "        <BfsNr>888</BfsNr>\n" +
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

		final String idRfBienFonds = "382929efa218";
		final String idRfPPE = "23af3efe44";

		// un mock de DAO qui simule l'existence des deux immeubles
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAODeuxImmeubles(idRfBienFonds, idRfPPE);

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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les immeubles avec les mêmes données que celles dans la DB
		final Liegenschaft bienfondsImport = newBienFonds(NO_RF_GROS_DU_LAC, "Le-gros-du-lac", 109, 17, 450000, "2015", RegDate.get(2015, 7, 1), false, idRfBienFonds, "CH282891891", true);
		final StockwerksEinheit ppeImport = newPPE(NO_RF_THIERRENS, "Thierrens", 46, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, idRfPPE, "CH8383820002", new Fraction(1, 1));
		final List<Grundstueck> immeublesImport = Arrays.asList(bienfondsImport, ppeImport);
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

		final BienFondsRF bienFonds = new BienFondsRF();
		bienFonds.setIdRF("382929efa218");
		final ProprieteParEtageRF ppe = new ProprieteParEtageRF();
		ppe.setIdRF("23af3efe44");

		// un mock de DAO qui simule l'existence des deux immeubles
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAO(bienFonds, ppe);

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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie un import sans immeuble
		final List<Grundstueck> immeublesImport = Collections.emptyList();
		detector.processImmeubles(IMPORT_ID, 2, immeublesImport.listIterator(), null);

		// on devrait avoir deux mutations de suppression des immeubles
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		mutations.sort(Comparator.comparing(EvenementRFMutation::getIdRF));

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
			commune.setNoRf(NO_RF_THIERRENS);
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

		final String idRfBienFonds = "382929efa218";
		final String idRfPPE = "23af3efe44";

		// des mocks de DAO qui simulent une base vide
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAO() {
			@Nullable
			@Override
			public ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushModeType flushModeOverride) {
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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie deux nouveaux immeubles sur deux nouvelles communes
		final Liegenschaft bienfondsImport = newBienFonds(NO_RF_GROS_DU_LAC, "Le-gros-du-lac", 109, 17, 450000, "2015", RegDate.get(2015, 7, 1), false, idRfBienFonds, "CH282891891", true);
		final StockwerksEinheit ppeImport = newPPE(NO_RF_THIERRENS, "Thierrens", 46, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, idRfPPE, "CH8383820002", new Fraction(1, 1));
		final List<Grundstueck> immeublesImport = Arrays.asList(bienfondsImport, ppeImport);
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
				             "        <BfsNr>888</BfsNr>\n" +
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
		assertEquals("888", mut3.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <BfsNr>888</BfsNr>\n" +
				             "    <Gemeindenamen>Le-gros-du-lac</Gemeindenamen>\n" +
				             "    <StammNr>0</StammNr>\n" +
				             "</GrundstueckNummer>\n", mut3.getXmlContent());
	}

	/**
	 * Ce test vérifie que des mutations de type MODIFICATION sur les communes sont bien créées si les communes dans l'import existent dans la base de données mais pas avec les mêmes valeurs.
	 */
	@Test
	public void testCommunesModifiees() throws Exception {

		final String idRfBienFonds = "382929efa218";
		final String idRfPPE = "23af3efe44";

		// un mock de DAO qui simule l'existence des deux immeubles
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAODeuxImmeubles(idRfBienFonds, idRfPPE);

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

		final ImmeubleRFDetector detector = new ImmeubleRFDetector(xmlHelperRF, blacklistRFHelper, immeubleRFDAO, communeRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les immeubles
		// - fusion de commune (Le-gros-du-lac -> Lac-Amour *avec* réutilisation du numéro de commune) et changement de numéro de parcelle
		final Liegenschaft bienfondsImport = newBienFonds(NO_RF_GROS_DU_LAC, "Lac-Amour", 2304, 17, 450000, "2015", RegDate.get(2015, 7, 1), false, idRfBienFonds, "CH282891891", true);
		// - fusion de commune (Thierrens -> Montanair *sans* réutilisation du numéro de commune) et changement de numéro de parcelle
		final StockwerksEinheit ppeImport = newPPE(108, "Montanair", 1022, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, idRfPPE, "CH8383820002", new Fraction(1, 1));
		final List<Grundstueck> immeublesImport = Arrays.asList(bienfondsImport, ppeImport);
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
				             "        <BfsNr>888</BfsNr>\n" +
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
		assertEquals("888", mut2.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<GrundstueckNummer xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <BfsNr>888</BfsNr>\n" +
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
	private static BienFondsRF newBienFondsRFGrosDuLac(String idRfBienFonds) {
		final BienFondsRF bienFonds = new BienFondsRF();
		{
			final CommuneRF commune = new CommuneRF();
			commune.setNoRf(NO_RF_GROS_DU_LAC);
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

			bienFonds.setIdRF(idRfBienFonds);
			bienFonds.setCfa(true);
			bienFonds.setEgrid("CH282891891");
			bienFonds.addSituation(situation);
			bienFonds.addEstimation(estimation);
			bienFonds.setSurfacesTotales(Collections.emptySet());
		}
		return bienFonds;
	}

	@NotNull
	private static Liegenschaft newBienFonds(int noRfCommune, String nomCommune, int noParcelle, Integer index1,
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

		private final BienFondsRF bienFonds;
		private final ProprieteParEtageRF ppe;
		private final String idRfBienFonds;
		private final String idRfPPE;

		public MockImmeubleRFDAODeuxImmeubles(String idRfBienFonds, String idRfPPE) {
			this.idRfBienFonds = idRfBienFonds;
			this.idRfPPE = idRfPPE;
			this.bienFonds = newBienFondsRFGrosDuLac(idRfBienFonds);
			this.ppe = newProprieteParEtageRFThierrens(idRfPPE);
		}

		@Nullable
		@Override
		public ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushModeType flushModeOverride) {
			if (key.getIdRF().equals(idRfBienFonds)) {
				return bienFonds;
			}
			else if (key.getIdRF().equals(idRfPPE)) {
				return ppe;
			}
			return null;
		}
	}
}