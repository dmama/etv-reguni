package ch.vd.unireg.registrefoncier.dataimport.detector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.rechteregister.BelastetesGrundstueck;
import ch.vd.capitastra.rechteregister.Beleg;
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
import ch.vd.unireg.registrefoncier.BeneficeServitudeRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.ChargeServitudeRF;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.IdentifiantDroitRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.UsufruitRF;
import ch.vd.unireg.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.DroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dao.MockAyantDroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.MockDroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.MockImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.MutationComparator;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRFImpl;
import ch.vd.unireg.registrefoncier.dataimport.elements.servitude.DienstbarkeitExtendedElement;
import ch.vd.unireg.registrefoncier.dataimport.helper.ServitudesRFHelperTest;
import ch.vd.unireg.transaction.MockTransactionManager;

import static ch.vd.unireg.registrefoncier.dataimport.helper.ServitudesRFHelperTest.newNatuerlichePersonGb;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SuppressWarnings("Duplicates")
public class ServitudeRFDetectorTest {

	private static final Long IMPORT_ID = 1L;
	private XmlHelperRF xmlHelperRF;
	private PlatformTransactionManager transactionManager;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private DroitRFDAO droitRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;

	@Before
	public void setUp() throws Exception {
		xmlHelperRF = new XmlHelperRFImpl();
		transactionManager = new MockTransactionManager();
		ayantDroitRFDAO = new MockAyantDroitRFDAO();
		droitRFDAO = new MockDroitRFDAO();
		immeubleRFDAO = new MockImmeubleRFDAO();
		AuthenticationHelper.pushPrincipal("test-user");
	}

	@After
	public void tearDown() throws Exception {
		AuthenticationHelper.popPrincipal();
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

		final ServitudeRFDetector detector = new ServitudeRFDetector(xmlHelperRF, droitRFDAO, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie trois nouvelles servitudes pour deux bénéficiaires qui concernent deux immeubles
		final BelastetesGrundstueck grundstueck1 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final BelastetesGrundstueck grundstueck2 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe090827e1", null, null);
		final ch.vd.capitastra.rechteregister.NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final ch.vd.capitastra.rechteregister.NatuerlichePersonGb natuerlichePerson2 = newNatuerlichePersonGb("Anne-Lise", "Lassueur", "_1f109152380ffd8901380ffda8131c65");
		final DienstbarkeitExtendedElement dienstbarkeit1 = ServitudesRFHelperTest.newDienstbarkeitExtended(grundstueck1, natuerlichePerson1,
		                                                                                                    ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380ffed6694392",
		                                                                                                                                            "1f109152380ffd8901380ffed66943a2",
		                                                                                                                                            "_1f109152380ffd8901380ffed6694392",
		                                                                                                                                            2005, 699, 8,
		                                                                                                                                            "Usufruit",
		                                                                                                                                            "2002/392", null,
		                                                                                                                                            RegDate.get(2002, 9, 2),
		                                                                                                                                            null)
		);
		final DienstbarkeitExtendedElement dienstbarkeit2 = ServitudesRFHelperTest.newDienstbarkeitExtended(grundstueck1, natuerlichePerson2,
		                                                                                                    ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380fff10eeeeee",
		                                                                                                                                            "1f109152380ffd8901380ffed6694002",
		                                                                                                                                            "1f109152380ffd8901380fff10eeeeee",
		                                                                                                                                            2005, 699, 8,
		                                                                                                                                            "Usufruit",
		                                                                                                                                            "2002/392", null,
		                                                                                                                                            RegDate.get(2002, 9, 2),
		                                                                                                                                            null)
		);
		final DienstbarkeitExtendedElement dienstbarkeit3 = ServitudesRFHelperTest.newDienstbarkeitExtended(grundstueck2, natuerlichePerson2,
		                                                                                                    ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380fff10ca631e",
		                                                                                                                                            "1f109152380ffd8901380fff10ca6331",
		                                                                                                                                            "_1f109152380ffd8901380fff10ca631e",
		                                                                                                                                            2007, 375, 8,
		                                                                                                                                            "Usufruit",
		                                                                                                                                            null, new Beleg(8, 2007, 266, 0),
		                                                                                                                                            RegDate.get(2007, 6, 25),
		                                                                                                                                            null)
		);

		List<DienstbarkeitExtendedElement> servitudes = Arrays.asList(dienstbarkeit1, dienstbarkeit2, dienstbarkeit3);
		detector.processServitudes(IMPORT_ID, 2, servitudes.iterator(), null, null);

		// on devrait avoir deux événements de mutation de type CREATION sur chacune des servitudes
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(3, mutations.size());
		mutations.sort(new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
		assertEquals("1f109152380ffd8901380ffed6694392", mut0.getIdRF());  // la première servitude
		assertEquals("1f109152380ffd8901380ffed66943a2", mut0.getVersionRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<DienstbarkeitExtended xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
				             "    <Dienstbarkeit VersionID=\"1f109152380ffd8901380ffed66943a2\" MasterID=\"1f109152380ffd8901380ffed6694392\">\n" +
				             "        <StandardRechtID>_1f109152380ffd8901380ffed6694392</StandardRechtID>\n" +
				             "        <RechtEintragJahrID>2005</RechtEintragJahrID>\n" +
				             "        <RechtEintragNummerID>699</RechtEintragNummerID>\n" +
				             "        <AmtNummer>8</AmtNummer>\n" +
				             "        <Stichwort>\n" +
				             "            <TextFr>Usufruit</TextFr>\n" +
				             "        </Stichwort>\n" +
				             "        <BelegAlt>2002/392</BelegAlt>\n" +
				             "        <BeginDatum>2002-09-02</BeginDatum>\n" +
				             "    </Dienstbarkeit>\n" +
				             "    <LastRechtGruppe>\n" +
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
				             "    </LastRechtGruppe>\n" +
				             "</DienstbarkeitExtended>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
		assertEquals("1f109152380ffd8901380fff10ca631e", mut1.getIdRF());  // la deuxième servitude
		assertEquals("1f109152380ffd8901380fff10ca6331", mut1.getVersionRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<DienstbarkeitExtended xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
				             "    <Dienstbarkeit VersionID=\"1f109152380ffd8901380fff10ca6331\" MasterID=\"1f109152380ffd8901380fff10ca631e\">\n" +
				             "        <StandardRechtID>_1f109152380ffd8901380fff10ca631e</StandardRechtID>\n" +
				             "        <RechtEintragJahrID>2007</RechtEintragJahrID>\n" +
				             "        <RechtEintragNummerID>375</RechtEintragNummerID>\n" +
				             "        <AmtNummer>8</AmtNummer>\n" +
				             "        <Stichwort>\n" +
				             "            <TextFr>Usufruit</TextFr>\n" +
				             "        </Stichwort>\n" +
				             "        <Beleg>\n" +
				             "            <AmtNummer>8</AmtNummer>\n" +
				             "            <BelegJahr>2007</BelegJahr>\n" +
				             "            <BelegNummer>266</BelegNummer>\n" +
				             "            <BelegNummerIndex>0</BelegNummerIndex>\n" +
				             "        </Beleg>\n" +
				             "        <BeginDatum>2007-06-25</BeginDatum>\n" +
				             "    </Dienstbarkeit>\n" +
				             "    <LastRechtGruppe>\n" +
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
				             "    </LastRechtGruppe>\n" +
				             "</DienstbarkeitExtended>\n", mut1.getXmlContent());

		final EvenementRFMutation mut2 = mutations.get(2);
		assertEquals(IMPORT_ID, mut2.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut2.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut2.getTypeMutation());
		assertEquals("1f109152380ffd8901380fff10eeeeee", mut2.getIdRF());  // la troisième servitude
		assertEquals("1f109152380ffd8901380ffed6694002", mut2.getVersionRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<DienstbarkeitExtended xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
				             "    <Dienstbarkeit VersionID=\"1f109152380ffd8901380ffed6694002\" MasterID=\"1f109152380ffd8901380fff10eeeeee\">\n" +
				             "        <StandardRechtID>1f109152380ffd8901380fff10eeeeee</StandardRechtID>\n" +
				             "        <RechtEintragJahrID>2005</RechtEintragJahrID>\n" +
				             "        <RechtEintragNummerID>699</RechtEintragNummerID>\n" +
				             "        <AmtNummer>8</AmtNummer>\n" +
				             "        <Stichwort>\n" +
				             "            <TextFr>Usufruit</TextFr>\n" +
				             "        </Stichwort>\n" +
				             "        <BelegAlt>2002/392</BelegAlt>\n" +
				             "        <BeginDatum>2002-09-02</BeginDatum>\n" +
				             "    </Dienstbarkeit>\n" +
				             "    <LastRechtGruppe>\n" +
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
				             "    </LastRechtGruppe>\n" +
				             "</DienstbarkeitExtended>\n", mut2.getXmlContent());
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

		final BienFondsRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final BienFondsRF immeuble2 = new BienFondsRF();
		immeuble2.setIdRF("_1f109152380ffd8901380ffe090827e1");

		final UsufruitRF usufruit1 = new UsufruitRF();
		{
			final BeneficeServitudeRF benefice1 = new BeneficeServitudeRF(null, null, usufruit1, pp1);
			final BeneficeServitudeRF benefice2 = new BeneficeServitudeRF(null, null, usufruit1, pp2);
			usufruit1.setMasterIdRF("1f109152380ffd8901380ffed6694392");
			usufruit1.setVersionIdRF("1f109152380ffd8901380ffed66943a2");
			usufruit1.addBenefice(benefice1);
			usufruit1.addBenefice(benefice2);
			usufruit1.addCharge(new ChargeServitudeRF(null, null, usufruit1, immeuble1));
			usufruit1.setDateDebut(dateImportInitial);
			usufruit1.setDateFin(null);
			usufruit1.setMotifDebut(null);
			usufruit1.setMotifFin(null);
			usufruit1.setDateDebutMetier(RegDate.get(2002, 9, 2));
			usufruit1.setDateFinMetier(null);
			usufruit1.setIdentifiantDroit(new IdentifiantDroitRF(8, 2005, 699));
			usufruit1.setNumeroAffaire(new IdentifiantAffaireRF(8, 2002, 392, null));
			droitRFDAO.save(usufruit1);
			pp1.addBeneficeServitude(benefice1);
			pp2.addBeneficeServitude(benefice2);
		}

		final UsufruitRF usufruit2 = new UsufruitRF();
		{
			final BeneficeServitudeRF benefice1 = new BeneficeServitudeRF(null, null, usufruit2, pp2);
			usufruit2.setMasterIdRF("1f109152380ffd8901380fff10ca631e");
			usufruit2.setVersionIdRF("1f109152380ffd8901380fff10ca6331");
			usufruit2.addBenefice(benefice1);
			usufruit2.addCharge(new ChargeServitudeRF(null, null, usufruit2, immeuble2));
			usufruit2.setDateDebut(dateImportInitial);
			usufruit2.setDateFin(null);
			usufruit2.setMotifDebut(null);
			usufruit2.setMotifFin(null);
			usufruit2.setDateDebutMetier(RegDate.get(2007, 6, 25));
			usufruit2.setDateFinMetier(null);
			usufruit2.setIdentifiantDroit(new IdentifiantDroitRF(8, 2007, 375));
			usufruit2.setNumeroAffaire(new IdentifiantAffaireRF(8, 2007, 266, 0));
			droitRFDAO.save(usufruit2);
			pp2.addBeneficeServitude(benefice1);
		}

		final UsufruitRF usufruit3 = new UsufruitRF();
		{
			final BeneficeServitudeRF benefice1 = new BeneficeServitudeRF(null, null, usufruit3, pp1);
			usufruit3.setMasterIdRF("1f109152380ffd8901380fff10eeeeee");
			usufruit3.setVersionIdRF("1f109152380ffd8901380ffed6694002");
			usufruit3.addBenefice(benefice1);
			usufruit3.addCharge(new ChargeServitudeRF(null, null, usufruit3, immeuble2));
			usufruit3.setDateDebut(dateImportInitial);
			usufruit3.setDateFin(null);
			usufruit3.setMotifDebut(null);
			usufruit3.setMotifFin(null);
			usufruit3.setDateDebutMetier(RegDate.get(2010, 6, 25));
			usufruit3.setDateFinMetier(null);
			usufruit3.setIdentifiantDroit(new IdentifiantDroitRF(8, 2010, 375));
			usufruit3.setNumeroAffaire(new IdentifiantAffaireRF(8, 2010, 266, 0));
			droitRFDAO.save(usufruit3);
			pp1.addBeneficeServitude(benefice1);
		}

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

		final ServitudeRFDetector detector = new ServitudeRFDetector(xmlHelperRF, droitRFDAO, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les mêmes servitudes avec quelques changements
		final BelastetesGrundstueck grundstueck1 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final BelastetesGrundstueck grundstueck2 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe090827e1", null, null);
		final ch.vd.capitastra.rechteregister.NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final ch.vd.capitastra.rechteregister.NatuerlichePersonGb natuerlichePerson2 = newNatuerlichePersonGb("Anne-Lise", "Lassueur", "_1f109152380ffd8901380ffda8131c65");
		// - date de fin différente
		final DienstbarkeitExtendedElement dienstbarkeit1 = ServitudesRFHelperTest.newDienstbarkeitExtended(Collections.singletonList(grundstueck1), Arrays.asList(natuerlichePerson1, natuerlichePerson2),
		                                                                                                    ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380ffed6694392",
		                                                                                                                                            "1f109152380ffd8901380ffed66943a2",
		                                                                                                                                            "_1f109152380ffd8901380ffed6694392",
		                                                                                                                                            2005, 699, 8,
		                                                                                                                                            "Usufruit",
		                                                                                                                                            "2002/392", null,
		                                                                                                                                            RegDate.get(2002, 9, 2),
		                                                                                                                                            RegDate.get(2111, 2, 23))
		);
		// - numéro d'affaire différent
		final DienstbarkeitExtendedElement dienstbarkeit2 = ServitudesRFHelperTest.newDienstbarkeitExtended(grundstueck2, natuerlichePerson2,
		                                                                                                    ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380fff10ca631e",
		                                                                                                                                            "1f109152380ffd8901380fff10ca6331",
		                                                                                                                                            "_1f109152380ffd8901380fff10ca631e",
		                                                                                                                                            2007, 375, 8,
		                                                                                                                                            "Usufruit",
		                                                                                                                                            null, new Beleg(8, 2007, 13, 0),
		                                                                                                                                            RegDate.get(2007, 6, 25),
		                                                                                                                                            null)
		);
		// - pas de différence
		final DienstbarkeitExtendedElement dienstbarkeit3 = ServitudesRFHelperTest.newDienstbarkeitExtended(grundstueck2, natuerlichePerson1,
		                                                                                                    ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380fff10eeeeee",
		                                                                                                                                            "1f109152380ffd8901380ffed6694002",
		                                                                                                                                            "1f109152380ffd8901380fff10eeeeee",
		                                                                                                                                            2010, 375, 8,
		                                                                                                                                            "Usufruit",
		                                                                                                                                            null, new Beleg(8, 2010, 266, 0),
		                                                                                                                                            RegDate.get(2010, 6, 25),
		                                                                                                                                            null)
		);

		final List<DienstbarkeitExtendedElement> servitudes = Arrays.asList(dienstbarkeit1, dienstbarkeit2, dienstbarkeit3);
		detector.processServitudes(IMPORT_ID, 2, servitudes.iterator(), null, null);

		// on devrait avoir deux événements de mutation de type MODIFICATION sur chacune des servitudes
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		mutations.sort(new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
		assertEquals("1f109152380ffd8901380ffed6694392", mut0.getIdRF());  // la première servitude
		assertEquals("1f109152380ffd8901380ffed66943a2", mut0.getVersionRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<DienstbarkeitExtended xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
				             "    <Dienstbarkeit VersionID=\"1f109152380ffd8901380ffed66943a2\" MasterID=\"1f109152380ffd8901380ffed6694392\">\n" +
				             "        <StandardRechtID>_1f109152380ffd8901380ffed6694392</StandardRechtID>\n" +
				             "        <RechtEintragJahrID>2005</RechtEintragJahrID>\n" +
				             "        <RechtEintragNummerID>699</RechtEintragNummerID>\n" +
				             "        <AmtNummer>8</AmtNummer>\n" +
				             "        <Stichwort>\n" +
				             "            <TextFr>Usufruit</TextFr>\n" +
				             "        </Stichwort>\n" +
				             "        <BelegAlt>2002/392</BelegAlt>\n" +
				             "        <BeginDatum>2002-09-02</BeginDatum>\n" +
				             "        <AblaufDatum>2111-02-23</AblaufDatum>\n" +
				             "    </Dienstbarkeit>\n" +
				             "    <LastRechtGruppe>\n" +
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
				             "        <BerechtigtePerson>\n" +
				             "            <NatuerlichePersonGb>\n" +
				             "                <Name>Lassueur</Name>\n" +
				             "                <Vorname>Anne-Lise</Vorname>\n" +
				             "                <PersonstammIDREF>_1f109152380ffd8901380ffda8131c65</PersonstammIDREF>\n" +
				             "            </NatuerlichePersonGb>\n" +
				             "        </BerechtigtePerson>\n" +
				             "    </LastRechtGruppe>\n" +
				             "</DienstbarkeitExtended>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut1.getTypeMutation());
		assertEquals("1f109152380ffd8901380fff10ca631e", mut1.getIdRF());  // la seconde servitude
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<DienstbarkeitExtended xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister\">\n" +
				             "    <Dienstbarkeit VersionID=\"1f109152380ffd8901380fff10ca6331\" MasterID=\"1f109152380ffd8901380fff10ca631e\">\n" +
				             "        <StandardRechtID>_1f109152380ffd8901380fff10ca631e</StandardRechtID>\n" +
				             "        <RechtEintragJahrID>2007</RechtEintragJahrID>\n" +
				             "        <RechtEintragNummerID>375</RechtEintragNummerID>\n" +
				             "        <AmtNummer>8</AmtNummer>\n" +
				             "        <Stichwort>\n" +
				             "            <TextFr>Usufruit</TextFr>\n" +
				             "        </Stichwort>\n" +
				             "        <Beleg>\n" +
				             "            <AmtNummer>8</AmtNummer>\n" +
				             "            <BelegJahr>2007</BelegJahr>\n" +
				             "            <BelegNummer>13</BelegNummer>\n" +
				             "            <BelegNummerIndex>0</BelegNummerIndex>\n" +
				             "        </Beleg>\n" +
				             "        <BeginDatum>2007-06-25</BeginDatum>\n" +
				             "    </Dienstbarkeit>\n" +
				             "    <LastRechtGruppe>\n" +
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
				             "    </LastRechtGruppe>\n" +
				             "</DienstbarkeitExtended>\n", mut1.getXmlContent());
	}

	/**
	 * Ce test vérifie qu'aucune mutations n'est créées si les servitudes existent avec les mêmes valeurs.
	 */
	@Test
	public void testServitudesIdentiques() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2010, 6, 1);

		// les données déjà existantes dans le DB
		final PersonnePhysiqueRF pp1 = new PersonnePhysiqueRF();
		pp1.setIdRF("_1f109152380ffd8901380ffdabcc2441");

		final PersonnePhysiqueRF pp2 = new PersonnePhysiqueRF();
		pp2.setIdRF("_1f109152380ffd8901380ffda8131c65");

		final BienFondsRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final BienFondsRF immeuble2 = new BienFondsRF();
		immeuble2.setIdRF("_1f109152380ffd8901380ffe090827e1");

		final UsufruitRF usufruit1 = new UsufruitRF();
		{
			final BeneficeServitudeRF benefice1 = new BeneficeServitudeRF(null, null, usufruit1, pp1);
			final BeneficeServitudeRF benefice2 = new BeneficeServitudeRF(null, null, usufruit1, pp2);
			usufruit1.setMasterIdRF("1f109152380ffd8901380ffed6694392");
			usufruit1.setVersionIdRF("1f109152380ffd8901380ffed66943a2");
			usufruit1.addBenefice(benefice1);
			usufruit1.addBenefice(benefice2);
			usufruit1.addCharge(new ChargeServitudeRF(null, null, usufruit1, immeuble1));
			usufruit1.setDateDebut(dateImportInitial);
			usufruit1.setDateFin(null);
			usufruit1.setMotifDebut(null);
			usufruit1.setMotifFin(null);
			usufruit1.setDateDebutMetier(RegDate.get(2002, 9, 2));
			usufruit1.setDateFinMetier(null);
			usufruit1.setIdentifiantDroit(new IdentifiantDroitRF(8, 2005, 699));
			usufruit1.setNumeroAffaire(new IdentifiantAffaireRF(8, 2002, 392, null));
			droitRFDAO.save(usufruit1);
			pp1.addBeneficeServitude(benefice1);
			pp2.addBeneficeServitude(benefice2);
		}

		final UsufruitRF usufruit2 = new UsufruitRF();
		{
			final BeneficeServitudeRF benefice1 = new BeneficeServitudeRF(null, null, usufruit2, pp2);
			usufruit2.setMasterIdRF("1f109152380ffd8901380fff10ca631e");
			usufruit2.setVersionIdRF("1f109152380ffd8901380fff10ca6331");
			usufruit2.addBenefice(benefice1);
			usufruit2.addCharge(new ChargeServitudeRF(null, null, usufruit2, immeuble2));
			usufruit2.setDateDebut(dateImportInitial);
			usufruit2.setDateFin(null);
			usufruit2.setMotifDebut(null);
			usufruit2.setMotifFin(null);
			usufruit2.setDateDebutMetier(RegDate.get(2007, 6, 25));
			usufruit2.setDateFinMetier(null);
			usufruit2.setIdentifiantDroit(new IdentifiantDroitRF(8, 2007, 375));
			usufruit2.setNumeroAffaire(new IdentifiantAffaireRF(8, 2007, 266, 0));
			droitRFDAO.save(usufruit2);
			pp2.addBeneficeServitude(benefice1);
		}

		final UsufruitRF usufruit3 = new UsufruitRF();
		{
			final BeneficeServitudeRF benefice1 = new BeneficeServitudeRF(null, null, usufruit3, pp1);
			usufruit3.setMasterIdRF("1f109152380ffd8901380fff10eeeeee");
			usufruit3.setVersionIdRF("1f109152380ffd8901380ffed6694002");
			usufruit3.addBenefice(benefice1);
			usufruit3.addCharge(new ChargeServitudeRF(null, null, usufruit3, immeuble2));
			usufruit3.setDateDebut(dateImportInitial);
			usufruit3.setDateFin(null);
			usufruit3.setMotifDebut(null);
			usufruit3.setMotifFin(null);
			usufruit3.setDateDebutMetier(RegDate.get(2010, 6, 25));
			usufruit3.setDateFinMetier(null);
			usufruit3.setIdentifiantDroit(new IdentifiantDroitRF(8, 2010, 375));
			usufruit3.setNumeroAffaire(new IdentifiantAffaireRF(8, 2010, 266, 0));
			droitRFDAO.save(usufruit3);
			pp1.addBeneficeServitude(benefice1);
		}

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

		final ServitudeRFDetector detector = new ServitudeRFDetector(xmlHelperRF, droitRFDAO, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les mêmes servitudes avec quelques changements
		final BelastetesGrundstueck grundstueck1 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe15bb729c", null, null);
		final BelastetesGrundstueck grundstueck2 = new BelastetesGrundstueck("_1f109152380ffd8901380ffe090827e1", null, null);
		final ch.vd.capitastra.rechteregister.NatuerlichePersonGb natuerlichePerson1 = newNatuerlichePersonGb("Roger", "Gaillard", "_1f109152380ffd8901380ffdabcc2441");
		final ch.vd.capitastra.rechteregister.NatuerlichePersonGb natuerlichePerson2 = newNatuerlichePersonGb("Anne-Lise", "Lassueur", "_1f109152380ffd8901380ffda8131c65");

		final DienstbarkeitExtendedElement dienstbarkeit1 = ServitudesRFHelperTest.newDienstbarkeitExtended(Collections.singletonList(grundstueck1), Arrays.asList(natuerlichePerson1, natuerlichePerson2),
		                                                                                                    ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380ffed6694392",
		                                                                                                                                            "1f109152380ffd8901380ffed66943a2",
		                                                                                                                                            "_1f109152380ffd8901380ffed6694392",
		                                                                                                                                            2005, 699, 8,
		                                                                                                                                            "Usufruit",
		                                                                                                                                            "2002/392", null,
		                                                                                                                                            RegDate.get(2002, 9, 2),
		                                                                                                                                            null)
		);
		final DienstbarkeitExtendedElement dienstbarkeit2 = ServitudesRFHelperTest.newDienstbarkeitExtended(grundstueck2, natuerlichePerson2,
		                                                                                                    ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380fff10ca631e",
		                                                                                                                                            "1f109152380ffd8901380fff10ca6331",
		                                                                                                                                            "_1f109152380ffd8901380fff10ca631e",
		                                                                                                                                            2007, 375, 8,
		                                                                                                                                            "Usufruit",
		                                                                                                                                            null, new Beleg(8, 2007, 266, 0),
		                                                                                                                                            RegDate.get(2007, 6, 25),
		                                                                                                                                            null)
		);
		final DienstbarkeitExtendedElement dienstbarkeit3 = ServitudesRFHelperTest.newDienstbarkeitExtended(grundstueck2, natuerlichePerson1,
		                                                                                                    ServitudesRFHelperTest.newDienstbarkeit("1f109152380ffd8901380fff10eeeeee",
		                                                                                                                                            "1f109152380ffd8901380ffed6694002",
		                                                                                                                                            "1f109152380ffd8901380fff10eeeeee",
		                                                                                                                                            2010, 375, 8,
		                                                                                                                                            "Usufruit",
		                                                                                                                                            null, new Beleg(8, 2010, 266, 0),
		                                                                                                                                            RegDate.get(2010, 6, 25),
		                                                                                                                                            null)
		);

		List<DienstbarkeitExtendedElement> servitudes = Arrays.asList(dienstbarkeit1, dienstbarkeit2, dienstbarkeit3);
		detector.processServitudes(IMPORT_ID, 2, servitudes.iterator(), null, null);

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

		final BienFondsRF immeuble1 = new BienFondsRF();
		immeuble1.setIdRF("_1f109152380ffd8901380ffe15bb729c");

		final BienFondsRF immeuble2 = new BienFondsRF();
		immeuble2.setIdRF("_1f109152380ffd8901380ffe090827e1");

		final UsufruitRF usufruit1 = new UsufruitRF();
		{
			final BeneficeServitudeRF benefice1 = new BeneficeServitudeRF(null, null, usufruit1, pp1);
			usufruit1.setMasterIdRF("1f109152380ffd8901380ffed6694392");
			usufruit1.setVersionIdRF("1f109152380ffd8901380ffed66943a2");
			usufruit1.addBenefice(benefice1);
			usufruit1.addCharge(new ChargeServitudeRF(null, null, usufruit1, immeuble1));
			usufruit1.setDateDebut(dateImportInitial);
			usufruit1.setDateFin(null);
			usufruit1.setMotifDebut(null);
			usufruit1.setMotifFin(null);
			usufruit1.setDateDebutMetier(RegDate.get(2002, 9, 2));
			usufruit1.setDateFinMetier(null);
			usufruit1.setIdentifiantDroit(new IdentifiantDroitRF(8, 2005, 699));
			usufruit1.setNumeroAffaire(new IdentifiantAffaireRF(8, 2002, 392, null));
			droitRFDAO.save(usufruit1);
			pp1.addBeneficeServitude(benefice1);
		}

		final UsufruitRF usufruit2 = new UsufruitRF();
		{
			final BeneficeServitudeRF benefice1 = new BeneficeServitudeRF(null, null, usufruit2, pp2);
			usufruit2.setMasterIdRF("1f109152380ffd8901380fff10eeeeee");
			usufruit2.setVersionIdRF("1f109152380ffd8901380ffed6694002");
			usufruit2.addBenefice(benefice1);
			usufruit2.addCharge(new ChargeServitudeRF(null, null, usufruit2, immeuble2));
			usufruit2.setDateDebut(dateImportInitial);
			usufruit2.setDateFin(null);
			usufruit2.setMotifDebut(null);
			usufruit2.setMotifFin(null);
			usufruit2.setDateDebutMetier(RegDate.get(2002, 9, 2));
			usufruit2.setDateFinMetier(null);
			usufruit2.setIdentifiantDroit(new IdentifiantDroitRF(8, 2005, 699));
			usufruit2.setNumeroAffaire(new IdentifiantAffaireRF(8, 2002, 392, null));
			droitRFDAO.save(usufruit2);
			pp2.addBeneficeServitude(benefice1);
		}

		final UsufruitRF usufruit3 = new UsufruitRF();
		{
			final BeneficeServitudeRF benefice1 = new BeneficeServitudeRF(null, null, usufruit3, pp1);
			usufruit3.setMasterIdRF("1f109152380ffd8901380fff10ca631e");
			usufruit3.setVersionIdRF("1f109152380ffd8901380fff10ca6331");
			usufruit3.addBenefice(benefice1);
			usufruit3.addCharge(new ChargeServitudeRF(null, null, usufruit3, immeuble2));
			usufruit3.setDateDebut(dateImportInitial);
			usufruit3.setDateFin(null);
			usufruit3.setMotifDebut(null);
			usufruit3.setMotifFin(null);
			usufruit3.setDateDebutMetier(RegDate.get(2007, 6, 25));
			usufruit3.setDateFinMetier(null);
			usufruit3.setIdentifiantDroit(new IdentifiantDroitRF(8, 2007, 375));
			usufruit3.setNumeroAffaire(new IdentifiantAffaireRF(8, 2007, 266, 0));
			droitRFDAO.save(usufruit3);
			pp1.addBeneficeServitude(benefice1);
		}

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

		final ServitudeRFDetector detector = new ServitudeRFDetector(xmlHelperRF, droitRFDAO, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie une liste de servitudes vide
		detector.processServitudes(IMPORT_ID, 2, Collections.<DienstbarkeitExtendedElement>emptyList().iterator(), null, null);

		// on devrait avoir deux événements de mutation de type SUPPRESSION sur chacun des propriétaires
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(3, mutations.size());
		mutations.sort(new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut0.getTypeMutation());
		assertEquals("1f109152380ffd8901380ffed6694392", mut0.getIdRF());  // la première servitude
		assertEquals("1f109152380ffd8901380ffed66943a2", mut0.getVersionRF());
		assertNull(mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut1.getTypeMutation());
		assertEquals("1f109152380ffd8901380fff10ca631e", mut1.getIdRF());  // la deuxième servitude
		assertEquals("1f109152380ffd8901380fff10ca6331", mut1.getVersionRF());
		assertNull(mut1.getXmlContent());

		final EvenementRFMutation mut2 = mutations.get(2);
		assertEquals(IMPORT_ID, mut2.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
		assertEquals(TypeEntiteRF.SERVITUDE, mut2.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut2.getTypeMutation());
		assertEquals("1f109152380ffd8901380fff10eeeeee", mut2.getIdRF());  // la troisième servitude
		assertEquals("1f109152380ffd8901380ffed6694002", mut2.getVersionRF());  // la troisième servitude
		assertNull(mut2.getXmlContent());
	}
}
