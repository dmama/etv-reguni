package ch.vd.uniregctb.registrefoncier;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.CapiCode;
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
import ch.vd.uniregctb.registrefoncier.dao.MockSurfaceAuSolRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.SurfaceAuSolRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRFImpl;
import ch.vd.uniregctb.registrefoncier.key.SurfaceAuSolRFKey;
import ch.vd.uniregctb.transaction.MockTransactionManager;

import static org.junit.Assert.assertEquals;

public class DataRFMutationsDetectorSurfaceAuSolTest {

	private static final Long IMPORT_ID = 1L;
	private XmlHelperRF xmlHelperRF;
	private PlatformTransactionManager transactionManager;
	private AyantDroitRFDAO ayantDroitRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;

	@Before
	public void setUp() throws Exception {
		xmlHelperRF = new XmlHelperRFImpl();
		transactionManager = new MockTransactionManager();
		ayantDroitRFDAO = new MockAyantDroitRFDAO();
		immeubleRFDAO = new MockImmeubleRFDAO();
		AuthenticationHelper.pushPrincipal("test-user");
	}

	@After
	public void tearDown() throws Exception {
		AuthenticationHelper.popPrincipal();
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsque la surface n'existe dans la base de données.
	 */
	@Test
	public void testNouvellesSurfaces() throws Exception {

		// un mock de DAO qui simule une base vide
		final SurfaceAuSolRFDAO surfaceAuSolRFDAO = new MockSurfaceAuSolRFDAO() {
			@Nullable
			@Override
			public SurfaceAuSolRF findActive(@NotNull SurfaceAuSolRFKey key) {
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

		final DataRFMutationsDetector detector = new DataRFMutationsDetector(xmlHelperRF, immeubleRFDAO, ayantDroitRFDAO, surfaceAuSolRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie deux nouveaux surfaces
		Bodenbedeckung surface1 = newSurfaceAuSol("382929efa218", "Forêt", 37823);
		Bodenbedeckung surface2 = newSurfaceAuSol("382929efa218", "Paturage", 4728211);
		List<Bodenbedeckung> surfaces = Arrays.asList(surface1, surface2);
		detector.processSurfaces(IMPORT_ID, 2, surfaces.iterator());

		// on devrait avoir deux événements de mutation de type CREATION à l'état A_TRAITER dans la base
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.SURFACE_AU_SOL, mut0.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut0.getTypeMutation());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Bodenbedeckung xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckIDREF>382929efa218</GrundstueckIDREF>\n" +
				             "    <Art>\n" +
				             "        <TextDe></TextDe>\n" +
				             "        <TextFr>Forêt</TextFr>\n" +
				             "    </Art>\n" +
				             "    <Flaeche>37823</Flaeche>\n" +
				             "</Bodenbedeckung>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.SURFACE_AU_SOL, mut1.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut1.getTypeMutation());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Bodenbedeckung xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckIDREF>382929efa218</GrundstueckIDREF>\n" +
				             "    <Art>\n" +
				             "        <TextDe></TextDe>\n" +
				             "        <TextFr>Paturage</TextFr>\n" +
				             "    </Art>\n" +
				             "    <Flaeche>4728211</Flaeche>\n" +
				             "</Bodenbedeckung>\n", mut1.getXmlContent());
	}

	@NotNull
	private static Bodenbedeckung newSurfaceAuSol(String idImmeubleRF, String type, int surface) {
		final Bodenbedeckung s = new Bodenbedeckung();
		s.setGrundstueckIDREF(idImmeubleRF);
		s.setArt(new CapiCode("", type));
		s.setFlaeche(surface);
		return s;
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées si les surfaces au sol dans l'import existent dans la base de données mais pas avec les mêmes valeurs.
	 */
	@Test
	public void testSurfacesModifiees() throws Exception {

		final BienFondRF immeuble = new BienFondRF();
		immeuble.setIdRF("382929efa218");

		final SurfaceAuSolRF s1 = new SurfaceAuSolRF();
		s1.setImmeuble(immeuble);
		s1.setType("Forêt");
		s1.setSurface(37823);

		final SurfaceAuSolRF s2 = new SurfaceAuSolRF();
		s2.setImmeuble(immeuble);
		s2.setType("Paturage");
		s2.setSurface(4728211);


		// un mock de DAO qui simule l'existence des deux surfaces au sol
		final SurfaceAuSolRFDAO surfaceAuSolRFDAO = new MockSurfaceAuSolRFDAO() {
			@Nullable
			@Override
			public SurfaceAuSolRF findActive(@NotNull SurfaceAuSolRFKey key) {
				if (key.equals(new SurfaceAuSolRFKey(s1))) {
					return s1;
				}
				else if (key.equals(new SurfaceAuSolRFKey(s2))) {
					return s2;
				}
				else {
					return null;
				}
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

		final DataRFMutationsDetector detector = new DataRFMutationsDetector(xmlHelperRF, immeubleRFDAO, ayantDroitRFDAO, surfaceAuSolRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie deux nouveaux surfaces aves des modifications
		// - type différent
		final Bodenbedeckung surface1 = newSurfaceAuSol("382929efa218", "Forêt pluviale", 37823);
		// - surface différent
		final Bodenbedeckung surface2 = newSurfaceAuSol("382929efa218", "Paturage", 2);
		final List<Bodenbedeckung> surfaces = Arrays.asList(surface1, surface2);
		detector.processSurfaces(IMPORT_ID, 2, surfaces.iterator());

		// on devrait avoir deux événements de mutation de type MODIFICATION à l'état A_TRAITER dans la base
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.SURFACE_AU_SOL, mut0.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut0.getTypeMutation());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Bodenbedeckung xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckIDREF>382929efa218</GrundstueckIDREF>\n" +
				             "    <Art>\n" +
				             "        <TextDe></TextDe>\n" +
				             "        <TextFr>Forêt pluviale</TextFr>\n" +
				             "    </Art>\n" +
				             "    <Flaeche>37823</Flaeche>\n" +
				             "</Bodenbedeckung>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(EvenementRFMutation.TypeEntite.SURFACE_AU_SOL, mut1.getTypeEntite());
		assertEquals(EvenementRFMutation.TypeMutation.CREATION, mut1.getTypeMutation());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<Bodenbedeckung xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <GrundstueckIDREF>382929efa218</GrundstueckIDREF>\n" +
				             "    <Art>\n" +
				             "        <TextDe></TextDe>\n" +
				             "        <TextFr>Paturage</TextFr>\n" +
				             "    </Art>\n" +
				             "    <Flaeche>2</Flaeche>\n" +
				             "</Bodenbedeckung>\n", mut1.getXmlContent());
	}

	/**
	 * Ce test vérifie qu'aucune mutation n'est créée si les données des immeubles dans l'import sont identiques avec l'état courant des immeubles stockés dans la DB.
	 */
	@Test
	public void testSurfacesIdentiques() throws Exception {
		final BienFondRF immeuble = new BienFondRF();
		immeuble.setIdRF("382929efa218");

		final SurfaceAuSolRF s1 = new SurfaceAuSolRF();
		s1.setImmeuble(immeuble);
		s1.setType("Forêt");
		s1.setSurface(37823);

		final SurfaceAuSolRF s2 = new SurfaceAuSolRF();
		s2.setImmeuble(immeuble);
		s2.setType("Paturage");
		s2.setSurface(4728211);


		// un mock de DAO qui simule l'existence des deux surfaces au sol
		final SurfaceAuSolRFDAO surfaceAuSolRFDAO = new MockSurfaceAuSolRFDAO() {
			@Nullable
			@Override
			public SurfaceAuSolRF findActive(@NotNull SurfaceAuSolRFKey key) {
				if (key.equals(new SurfaceAuSolRFKey(s1))) {
					return s1;
				}
				else if (key.equals(new SurfaceAuSolRFKey(s2))) {
					return s2;
				}
				else {
					return null;
				}
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

		final DataRFMutationsDetector detector = new DataRFMutationsDetector(xmlHelperRF, immeubleRFDAO, ayantDroitRFDAO, surfaceAuSolRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager);

		// on envoie deux nouveaux surfaces avec les données que celles dans la DB
		final Bodenbedeckung surface1 = newSurfaceAuSol("382929efa218", "Forêt", 37823);
		final Bodenbedeckung surface2 = newSurfaceAuSol("382929efa218", "Paturage", 4728211);
		final List<Bodenbedeckung> surfaces = Arrays.asList(surface1, surface2);
		detector.processSurfaces(IMPORT_ID, 2, surfaces.iterator());

		// on ne devrait pas avoir de mutation
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(0, mutations.size());
	}
}
