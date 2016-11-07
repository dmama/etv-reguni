package ch.vd.uniregctb.registrefoncier;

import java.util.Arrays;
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
import ch.vd.uniregctb.transaction.MockTransactionManager;

import static org.junit.Assert.assertEquals;

public class DataRFMutationsDetectorImmeubleTest {

	private static final Long IMPORT_ID = 1L;
	private XmlHelperRF xmlHelperRF;
	private PlatformTransactionManager transactionManager;
	private AyantDroitRFDAO ayantDroitRFDAO;

	@Before
	public void setUp() throws Exception {
		xmlHelperRF = new XmlHelperRFImpl();
		transactionManager = new MockTransactionManager();
		ayantDroitRFDAO = new MockAyantDroitRFDAO();
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
			public ImmeubleRF find(@NotNull ImmeubleRFKey key) {
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

		final DataRFMutationsDetector detector = new DataRFMutationsDetector(xmlHelperRF, immeubleRFDAO, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie deux nouveaux immeubles
		final UnbekanntesGrundstueck kopie0 = newKopie(2233, 109, 17, 500000L, "2016", RegDate.get(2016, 1, 1), true, "382929efa218", "CH282891891");
		final UnbekanntesGrundstueck kopie1 = newKopie(5586, 1022, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, "23af3efe44", "CH8383820002");
		final List<Grundstueck> immeubles = Arrays.asList(kopie0, kopie1);
		detector.processImmeubles(IMPORT_ID, 2, immeubles.iterator());

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
			public ImmeubleRF find(@NotNull ImmeubleRFKey key) {
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

		final DataRFMutationsDetector detector = new DataRFMutationsDetector(xmlHelperRF, immeubleRFDAO, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie deux nouveaux immeubles
		final Liegenschaft bienfond = newBienFond(2233, 109, 17, 500000L, "2016", RegDate.get(2016, 1, 1), true, "382929efa218", "CH282891891", true);
		final StockwerksEinheit ppe = newPPE(5586, 1022, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, "23af3efe44", "CH8383820002", new Fraction(1, 1));
		final List<Grundstueck> immeubles = Arrays.asList(bienfond, ppe);
		detector.processImmeubles(IMPORT_ID, 2, immeubles.iterator());

		// on devrait avoir deux événements de mutation de type CREATION à l'état A_TRAITER dans la base
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.IMMEUBLE, mut0.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut0.getTypeMutation());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Liegenschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckID>382929efa218</GrundstueckID>\n" +
				             "    <EGrid>CH282891891</EGrid>\n" +
				             "    <GrundstueckNummer>\n" +
				             "        <BfsNr>2233</BfsNr>\n" +
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
		assertEquals(EvenementRFMutation.TypeEntite.IMMEUBLE, mut1.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut1.getTypeMutation());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<StockwerksEinheit xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckID>23af3efe44</GrundstueckID>\n" +
				             "    <EGrid>CH8383820002</EGrid>\n" +
				             "    <GrundstueckNummer>\n" +
				             "        <BfsNr>5586</BfsNr>\n" +
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
	 * Ce test vérifie que des mutations de type MODIFICATION sont bien créées les immeubles dans l'import existent dans la base de données mais pas avec les mêmes valeurs.
	 */
	@Test
	public void testImmeublesModifies() throws Exception {

		final String idRfBienFond = "382929efa218";
		final String idRfPPE = "23af3efe44";

		final BienFondRF bienFond = new BienFondRF();
		{
			final SituationRF situation = new SituationRF();
			situation.setNoRfCommune(2233);
			situation.setNoParcelle(109);
			situation.setIndex1(17);

			final EstimationRF estimation = new EstimationRF();
			estimation.setMontant(450000L);
			estimation.setReference("2015");
			estimation.setDateEstimation(RegDate.get(2015, 7, 1));
			estimation.setEnRevision(false);
			estimation.setDateDebut(RegDate.get(2000, 1, 1));

			bienFond.setIdRF(idRfBienFond);
			bienFond.setCfa(true);
			bienFond.setEgrid("CH282891891");
			bienFond.addSituation(situation);
			bienFond.addEstimation(estimation);
		}

		final ProprieteParEtageRF ppe = new ProprieteParEtageRF();
		{
			final SituationRF situation = new SituationRF();
			situation.setNoRfCommune(5689); // Thierrens
			situation.setNoParcelle(46);

			final EstimationRF estimation = new EstimationRF();
			estimation.setMontant(250000L);
			estimation.setReference("RG97");
			estimation.setDateEstimation(RegDate.get(1997, 1, 1));
			estimation.setEnRevision(false);
			estimation.setDateDebut(RegDate.get(2000, 1, 1));

			ppe.setIdRF(idRfPPE);
			ppe.setQuotePart(new Fraction(1, 1));
			ppe.setEgrid("CH8383820002");
			ppe.addSituation(situation);
			ppe.addEstimation(estimation);
		}


		// un mock de DAO qui simule l'existence des deux immeubles
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAO() {
			@Nullable
			@Override
			public ImmeubleRF find(@NotNull ImmeubleRFKey key) {
				if (key.getIdRF().equals(idRfBienFond)) {
					return bienFond;
				}
				else if (key.getIdRF().equals(idRfPPE)) {
					return ppe;
				}
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

		final DataRFMutationsDetector detector = new DataRFMutationsDetector(xmlHelperRF, immeubleRFDAO, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les immeubles avec des modifications
		// - nouvelle estimation fiscale
		final Liegenschaft bienfondImport = newBienFond(2233, 109, 17, 500000L, "2016", RegDate.get(2016, 1, 1), false, idRfBienFond, "CH282891891", true);
		// - fusion de commune (Thierrens -> Montanair) et changement de numéro de parcelle
		final StockwerksEinheit ppeImport = newPPE(5693 /* Montanair */, 1022, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, idRfPPE, "CH8383820002", new Fraction(1, 1));
		final List<Grundstueck> immeublesImport = Arrays.asList(bienfondImport, ppeImport);
		detector.processImmeubles(IMPORT_ID, 2, immeublesImport.listIterator());

		// on devrait avoir deux événements de mutation de type MODIFICATION à l'état A_TRAITER dans la base
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.IMMEUBLE, mut0.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.MODIFICATION, mut0.getTypeMutation());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Liegenschaft xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckID>382929efa218</GrundstueckID>\n" +
				             "    <EGrid>CH282891891</EGrid>\n" +
				             "    <GrundstueckNummer>\n" +
				             "        <BfsNr>2233</BfsNr>\n" +
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
		assertEquals(EvenementRFMutation.TypeEntite.IMMEUBLE, mut1.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.MODIFICATION, mut1.getTypeMutation());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<StockwerksEinheit xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckID>23af3efe44</GrundstueckID>\n" +
				             "    <EGrid>CH8383820002</EGrid>\n" +
				             "    <GrundstueckNummer>\n" +
				             "        <BfsNr>5693</BfsNr>\n" +
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
	 * Ce test vérifie qu'une mutation n'est créée si les données des immeubles dans l'import sont identiques avec l'état courant des immeubles stockés dans la DB.
	 */
	@Test
	public void testImmeublesIdentiques() throws Exception {

		final String idRfBienFond = "382929efa218";
		final String idRfPPE = "23af3efe44";

		final BienFondRF bienFond = new BienFondRF();
		{
			final SituationRF situation = new SituationRF();
			situation.setNoRfCommune(2233);
			situation.setNoParcelle(109);
			situation.setIndex1(17);

			final EstimationRF estimation = new EstimationRF();
			estimation.setMontant(450000L);
			estimation.setReference("2015");
			estimation.setDateEstimation(RegDate.get(2015, 7, 1));
			estimation.setEnRevision(false);
			estimation.setDateDebut(RegDate.get(2000, 1, 1));

			bienFond.setIdRF(idRfBienFond);
			bienFond.setCfa(true);
			bienFond.setEgrid("CH282891891");
			bienFond.addSituation(situation);
			bienFond.addEstimation(estimation);
		}

		final ProprieteParEtageRF ppe = new ProprieteParEtageRF();
		{
			final SituationRF situation = new SituationRF();
			situation.setNoRfCommune(5689); // Thierrens
			situation.setNoParcelle(46);

			final EstimationRF estimation = new EstimationRF();
			estimation.setMontant(250000L);
			estimation.setReference("RG97");
			estimation.setDateEstimation(RegDate.get(1997, 1, 1));
			estimation.setEnRevision(false);
			estimation.setDateDebut(RegDate.get(2000, 1, 1));

			ppe.setIdRF(idRfPPE);
			ppe.setQuotePart(new Fraction(1, 1));
			ppe.setEgrid("CH8383820002");
			ppe.addSituation(situation);
			ppe.addEstimation(estimation);
		}


		// un mock de DAO qui simule l'existence des deux immeubles
		final ImmeubleRFDAO immeubleRFDAO = new MockImmeubleRFDAO() {
			@Nullable
			@Override
			public ImmeubleRF find(@NotNull ImmeubleRFKey key) {
				if (key.getIdRF().equals(idRfBienFond)) {
					return bienFond;
				}
				else if (key.getIdRF().equals(idRfPPE)) {
					return ppe;
				}
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

		final DataRFMutationsDetector detector = new DataRFMutationsDetector(xmlHelperRF, immeubleRFDAO, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les immeubles avec les mêmes donneés que celles dans la DB
		final Liegenschaft bienfondImport = newBienFond(2233, 109, 17, 450000, "2015", RegDate.get(2015, 7, 1), false, idRfBienFond, "CH282891891", true);
		final StockwerksEinheit ppeImport = newPPE(5689, 46, null, 250000, "RG97", RegDate.get(1997, 1, 1), false, idRfPPE, "CH8383820002", new Fraction(1, 1));
		final List<Grundstueck> immeublesImport = Arrays.asList(bienfondImport, ppeImport);
		detector.processImmeubles(IMPORT_ID, 2, immeublesImport.listIterator());

		// on ne devrait pas avoir de mutation
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(0, mutations.size());
	}

	@NotNull
	private static Liegenschaft newBienFond(int noRfCommune, int noParcelle, Integer index1,
	                                        long estimationFiscale, String referenceEstimation, RegDate dateEstimation,
	                                        boolean enRevision, String idRF, String egrid, boolean cfa) {
		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(noRfCommune);
		grundstueckNummer.setStammNr(noParcelle);
		grundstueckNummer.setIndexNr1(index1);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(estimationFiscale);
		amtlicheBewertung.setProtokollNr(referenceEstimation);
		amtlicheBewertung.setProtokollDatum(dateEstimation);
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
	private static StockwerksEinheit newPPE(int noRfCommune, int noParcelle, Integer index1,
	                                        long estimationFiscale, String referenceEstimation, RegDate dateEstimation,
	                                        boolean enRevision, String idRF, String egrid, Fraction quotepart) {
		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(noRfCommune);
		grundstueckNummer.setStammNr(noParcelle);
		grundstueckNummer.setIndexNr1(index1);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(estimationFiscale);
		amtlicheBewertung.setProtokollNr(referenceEstimation);
		amtlicheBewertung.setProtokollDatum(dateEstimation);
		amtlicheBewertung.setProtokollGueltig(!enRevision);

		final StockwerksEinheit grundstueck = new StockwerksEinheit();
		grundstueck.setGrundstueckID(idRF);
		grundstueck.setStammGrundstueck(new StammGrundstueck(new Quote((long) quotepart.getNumerateur(), (long) quotepart.getDenominateur(), null, false), null, null));
		grundstueck.setEGrid(egrid);
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);

		return grundstueck;
	}

	private static UnbekanntesGrundstueck newKopie(int noRfCommune, int noParcelle, Integer index1,
	                                               long estimationFiscale, String referenceEstimation, RegDate dateEstimation,
	                                               boolean enRevision, String idRF, String egrid) {

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(noRfCommune);
		grundstueckNummer.setStammNr(noParcelle);
		grundstueckNummer.setIndexNr1(index1);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(estimationFiscale);
		amtlicheBewertung.setProtokollNr(referenceEstimation);
		amtlicheBewertung.setProtokollDatum(dateEstimation);
		amtlicheBewertung.setProtokollGueltig(!enRevision);

		final UnbekanntesGrundstueck grundstueck = new UnbekanntesGrundstueck();
		grundstueck.setGrundstueckID(idRF);
		grundstueck.setEGrid(egrid);
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);
		grundstueck.setIstKopie(true);

		return grundstueck;
	}
}