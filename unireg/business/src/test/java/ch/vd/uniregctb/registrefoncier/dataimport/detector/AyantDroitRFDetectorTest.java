package ch.vd.uniregctb.registrefoncier.dataimport.detector;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.grundstueck.GeburtsDatum;
import ch.vd.capitastra.grundstueck.JuristischePersonUnterart;
import ch.vd.capitastra.grundstueck.JuristischePersonstamm;
import ch.vd.capitastra.grundstueck.NatuerlichePersonstamm;
import ch.vd.capitastra.grundstueck.Personstamm;
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
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.MockAyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRFImpl;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;
import ch.vd.uniregctb.transaction.MockTransactionManager;

import static org.junit.Assert.assertEquals;

@RunWith(UniregJUnit4Runner.class)
public class AyantDroitRFDetectorTest {

	private static final Long IMPORT_ID = 1L;
	private XmlHelperRF xmlHelperRF;
	private PlatformTransactionManager transactionManager;

	@Before
	public void setUp() throws Exception {
		xmlHelperRF = new XmlHelperRFImpl();
		transactionManager = new MockTransactionManager();
		AuthenticationHelper.pushPrincipal("test-user");
	}

	@After
	public void tearDown() throws Exception {
		AuthenticationHelper.popPrincipal();
	}

	/**
	 * Ce test vérifie que des mutations de type CREATION sont bien créées lorsqu'aucun des ayant-droits dans l'import n'existe dans la base de données.
	 */
	@Test
	public void testNouveauxAyantsDroits() throws Exception {

		// un mock de DAO qui simule une base vide
		final AyantDroitRFDAO ayantDroitRFDAO = new MockAyantDroitRFDAO() {
			@Nullable
			@Override
			public AyantDroitRF find(@NotNull AyantDroitRFKey key) {
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

		final AyantDroitRFDetector detector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie trois nouveaux ayant-droits
		final NatuerlichePersonstamm natuerliche = newPersonnePhysique("3893728273382823", 3727L, 827288022L, "Nom", "Prénom", RegDate.get(1956, 1, 23));
		final JuristischePersonstamm juristische = newPersonneMorale("48349384890202", 3727L, 827288022L, "Raison sociale");
		final JuristischePersonstamm collectivite = newCollectivitePublique("574739202303482", 3727L, 827288022L, "Raison sociale");
		final List<Personstamm> proprietaires = Arrays.asList(natuerliche, juristische, collectivite);
		detector.processProprietaires(IMPORT_ID, 2, proprietaires.iterator(), null);

		// on devrait avoir trois événements de mutation de type CREATION à l'état A_TRAITER dans la base
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(3, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.AYANT_DROIT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
		assertEquals("3893728273382823", mut0.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<NatuerlichePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonstammID>3893728273382823</PersonstammID>\n" +
				             "    <Name>Nom</Name>\n" +
				             "    <Gueltig>false</Gueltig>\n" +
				             "    <NoRF>3727</NoRF>\n" +
				             "    <Vorname>Prénom</Vorname>\n" +
				             "    <Geburtsdatum>\n" +
				             "        <Tag>23</Tag>\n" +
				             "        <Monat>1</Monat>\n" +
				             "        <Jahr>1956</Jahr>\n" +
				             "    </Geburtsdatum>\n" +
				             "    <NrIROLE>827288022</NrIROLE>\n" +
				             "</NatuerlichePersonstamm>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.AYANT_DROIT, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
		assertEquals("48349384890202", mut1.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<JuristischePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonstammID>48349384890202</PersonstammID>\n" +
				             "    <Name>Raison sociale</Name>\n" +
				             "    <Gueltig>false</Gueltig>\n" +
				             "    <NrACI>827288022</NrACI>\n" +
				             "    <NoRF>3727</NoRF>\n" +
				             "    <Unterart>SchweizerischeJuristischePerson</Unterart>\n" +
				             "</JuristischePersonstamm>\n", mut1.getXmlContent());

		final EvenementRFMutation mut2 = mutations.get(2);
		assertEquals(IMPORT_ID, mut2.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
		assertEquals(TypeEntiteRF.AYANT_DROIT, mut2.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut2.getTypeMutation());
		assertEquals("574739202303482", mut2.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<JuristischePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonstammID>574739202303482</PersonstammID>\n" +
				             "    <Name>Raison sociale</Name>\n" +
				             "    <Gueltig>false</Gueltig>\n" +
				             "    <NrACI>827288022</NrACI>\n" +
				             "    <NoRF>3727</NoRF>\n" +
				             "    <Unterart>OeffentlicheKoerperschaft</Unterart>\n" +
				             "</JuristischePersonstamm>\n", mut2.getXmlContent());
	}

	@NotNull
	public static JuristischePersonstamm newCollectivitePublique(String idRF, long noRF, long noACI, String raisonSociale) {
		final JuristischePersonstamm collectivite = new JuristischePersonstamm();
		collectivite.setPersonstammID(idRF);
		collectivite.setNoRF(noRF);
		collectivite.setNrACI(noACI);
		collectivite.setName(raisonSociale);
		collectivite.setUnterart(JuristischePersonUnterart.OEFFENTLICHE_KOERPERSCHAFT);
		return collectivite;
	}

	@NotNull
	public static JuristischePersonstamm newPersonneMorale(String idRF, long noRF, long noACI, String name) {
		final JuristischePersonstamm juristische = new JuristischePersonstamm();
		juristische.setPersonstammID(idRF);
		juristische.setNoRF(noRF);
		juristische.setNrACI(noACI);
		juristische.setName(name);
		juristische.setUnterart(JuristischePersonUnterart.SCHWEIZERISCHE_JURISTISCHE_PERSON);
		return juristische;
	}

	@NotNull
	public static NatuerlichePersonstamm newPersonnePhysique(String idRF, long noRF, long noIrole, String nom, String prenom, RegDate dateNaissance) {
		final NatuerlichePersonstamm natuerliche = new NatuerlichePersonstamm();
		natuerliche.setPersonstammID(idRF);
		natuerliche.setNoRF(noRF);
		natuerliche.setNrIROLE(noIrole);
		natuerliche.setName(nom);
		natuerliche.setVorname(prenom);
		natuerliche.setGeburtsdatum(new GeburtsDatum(dateNaissance.day(), dateNaissance.month(), dateNaissance.year()));
		return natuerliche;
	}

	/**
	 * Ce test vérifie que des mutations de type MODIFICATION sont bien créées les immeubles dans l'import existent dans la base de données mais pas avec les mêmes valeurs.
	 */
	@Test
	public void testAyantsDroitsModifies() throws Exception {

		final String idRFPP = "3893728273382823";
		final String idRFPM = "48349384890202";
		final String idRFColl = "574739202303482";

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setIdRF(idRFPP);
		pp.setNoRF(3727);
		pp.setNoContribuable(827288022L);
		pp.setNom("Nom");
		pp.setPrenom("Prénom");
		pp.setDateNaissance(RegDate.get(1956, 1, 23));

		final PersonneMoraleRF pm = new PersonneMoraleRF();
		pm.setIdRF(idRFPM);
		pm.setNoRF(3727);
		pm.setNoContribuable(827288022L);
		pm.setRaisonSociale("Raison sociale");

		final CollectivitePubliqueRF coll = new CollectivitePubliqueRF();
		coll.setIdRF(idRFColl);
		coll.setNoRF(3727);
		coll.setNoContribuable(827288022L);
		coll.setRaisonSociale("Raison sociale");


		// un mock de DAO qui simule l'existence des trois ayants-droits
		final AyantDroitRFDAO ayantDroitRFDAO = new MockAyantDroitRFDAO() {
			@Nullable
			@Override
			public AyantDroitRF find(@NotNull AyantDroitRFKey key) {
				switch (key.getIdRF()) {
				case idRFPP:
					return pp;
				case idRFPM:
					return pm;
				case idRFColl:
					return coll;
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

		final AyantDroitRFDetector detector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les immeubles avec des modifications
		// - nouveau prénom
		final NatuerlichePersonstamm natuerliche = newPersonnePhysique(idRFPP, 3727L, 827288022L, "Nom", "Nouveau Prénom", RegDate.get(1956, 1, 23));
		// - nouveau noRF
		final JuristischePersonstamm juristische = newPersonneMorale(idRFPM, 100229L, 827288022L, "Raison sociale");
		// - nouveau noACI
		final JuristischePersonstamm collectivite = newCollectivitePublique(idRFColl, 3727L, 183482392L, "Raison sociale");
		final List<Personstamm> proprietaires = Arrays.asList(natuerliche, juristische, collectivite);
		detector.processProprietaires(IMPORT_ID, 2, proprietaires.iterator(), null);

		// on devrait avoir trois événements de mutation de type MODIFICATION à l'état A_TRAITER dans la base
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(3, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.AYANT_DROIT, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
		assertEquals("3893728273382823", mut0.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<NatuerlichePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonstammID>3893728273382823</PersonstammID>\n" +
				             "    <Name>Nom</Name>\n" +
				             "    <Gueltig>false</Gueltig>\n" +
				             "    <NoRF>3727</NoRF>\n" +
				             "    <Vorname>Nouveau Prénom</Vorname>\n" +
				             "    <Geburtsdatum>\n" +
				             "        <Tag>23</Tag>\n" +
				             "        <Monat>1</Monat>\n" +
				             "        <Jahr>1956</Jahr>\n" +
				             "    </Geburtsdatum>\n" +
				             "    <NrIROLE>827288022</NrIROLE>\n" +
				             "</NatuerlichePersonstamm>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.AYANT_DROIT, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut1.getTypeMutation());
		assertEquals("48349384890202", mut1.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<JuristischePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonstammID>48349384890202</PersonstammID>\n" +
				             "    <Name>Raison sociale</Name>\n" +
				             "    <Gueltig>false</Gueltig>\n" +
				             "    <NrACI>827288022</NrACI>\n" +
				             "    <NoRF>100229</NoRF>\n" +
				             "    <Unterart>SchweizerischeJuristischePerson</Unterart>\n" +
				             "</JuristischePersonstamm>\n", mut1.getXmlContent());

		final EvenementRFMutation mut2 = mutations.get(2);
		assertEquals(IMPORT_ID, mut2.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut2.getEtat());
		assertEquals(TypeEntiteRF.AYANT_DROIT, mut2.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut2.getTypeMutation());
		assertEquals("574739202303482", mut2.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<JuristischePersonstamm xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <PersonstammID>574739202303482</PersonstammID>\n" +
				             "    <Name>Raison sociale</Name>\n" +
				             "    <Gueltig>false</Gueltig>\n" +
				             "    <NrACI>183482392</NrACI>\n" +
				             "    <NoRF>3727</NoRF>\n" +
				             "    <Unterart>OeffentlicheKoerperschaft</Unterart>\n" +
				             "</JuristischePersonstamm>\n", mut2.getXmlContent());
	}

	/**
	 * Ce test vérifie qu'aucune mutation n'est créée si les données des immeubles dans l'import sont identiques avec l'état courant des immeubles stockés dans la DB.
	 */
	@Test
	public void testAyantsDroitsIdentiques() throws Exception {

		final String idRFPP = "3893728273382823";
		final String idRFPM = "48349384890202";
		final String idRFColl = "574739202303482";

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setIdRF(idRFPP);
		pp.setNoRF(3727);
		pp.setNoContribuable(827288022L);
		pp.setNom("Nom");
		pp.setPrenom("Prénom");
		pp.setDateNaissance(RegDate.get(1956, 1, 23));

		final PersonneMoraleRF pm = new PersonneMoraleRF();
		pm.setIdRF(idRFPM);
		pm.setNoRF(3727);
		pm.setNoContribuable(827288022L);
		pm.setRaisonSociale("Raison sociale");

		final CollectivitePubliqueRF coll = new CollectivitePubliqueRF();
		coll.setIdRF(idRFColl);
		coll.setNoRF(3727);
		coll.setNoContribuable(827288022L);
		coll.setRaisonSociale("Raison sociale");


		// un mock de DAO qui simule l'existence des trois ayants-droits
		final AyantDroitRFDAO ayantDroitRFDAO = new MockAyantDroitRFDAO() {
			@Nullable
			@Override
			public AyantDroitRF find(@NotNull AyantDroitRFKey key) {
				switch (key.getIdRF()) {
				case idRFPP:
					return pp;
				case idRFPM:
					return pm;
				case idRFColl:
					return coll;
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

		final AyantDroitRFDetector detector = new AyantDroitRFDetector(xmlHelperRF, ayantDroitRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie les immeubles avec les mêmes donneés que celles dans la DB
		final NatuerlichePersonstamm natuerliche = newPersonnePhysique("3893728273382823", 3727L, 827288022L, "Nom", "Prénom", RegDate.get(1956, 1, 23));
		final JuristischePersonstamm juristische = newPersonneMorale("48349384890202", 3727L, 827288022L, "Raison sociale");
		final JuristischePersonstamm collectivite = newCollectivitePublique("574739202303482", 3727L, 827288022L, "Raison sociale");
		final List<Personstamm> proprietaires = Arrays.asList(natuerliche, juristische, collectivite);
		detector.processProprietaires(IMPORT_ID, 2, proprietaires.iterator(), null);

		// on ne devrait pas avoir de mutation
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(0, mutations.size());
	}

}