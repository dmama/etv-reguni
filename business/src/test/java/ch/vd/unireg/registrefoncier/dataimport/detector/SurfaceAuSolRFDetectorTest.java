package ch.vd.unireg.registrefoncier.dataimport.detector;

import javax.persistence.FlushModeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.CapiCode;
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
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.SurfaceAuSolRF;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dao.MockImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.MutationComparator;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRFImpl;
import ch.vd.unireg.registrefoncier.key.ImmeubleRFKey;
import ch.vd.unireg.transaction.MockTransactionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SurfaceAuSolRFDetectorTest {

	private static final Long IMPORT_ID = 1L;
	private XmlHelperRF xmlHelperRF;
	private PlatformTransactionManager transactionManager;
	private ImmeubleRFDAO immeubleRFDAO;
	private PersistentCache<ArrayList<Bodenbedeckung>> cacheSurfaces;

	@Before
	public void setUp() throws Exception {
		xmlHelperRF = new XmlHelperRFImpl();
		transactionManager = new MockTransactionManager();
		immeubleRFDAO = new MockImmeubleRFDAO();
		cacheSurfaces = new MockPersistentCache<>();
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

		final SurfaceAuSolRFDetector detector = new SurfaceAuSolRFDetector(xmlHelperRF, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, cacheSurfaces);

		// on envoie deux nouvelles surfaces
		Bodenbedeckung surface1 = newSurfaceAuSol("382929efa218", "Forêt", 37823);
		Bodenbedeckung surface2 = newSurfaceAuSol("382929efa218", "Paturage", 4728211);
		List<Bodenbedeckung> surfaces = Arrays.asList(surface1, surface2);
		detector.processSurfaces(IMPORT_ID, 2, surfaces.iterator(), null);

		// on devrait avoir seul événement de mutation de type CREATION qui concerne un immeuble
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(1, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.SURFACE_AU_SOL, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
		assertEquals("382929efa218", mut0.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<BodenbedeckungList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <Bodenbedeckung>\n" +
				             "        <GrundstueckIDREF>382929efa218</GrundstueckIDREF>\n" +
				             "        <Art>\n" +
				             "            <TextDe></TextDe>\n" +
				             "            <TextFr>Forêt</TextFr>\n" +
				             "        </Art>\n" +
				             "        <Flaeche>37823</Flaeche>\n" +
				             "    </Bodenbedeckung>\n" +
				             "    <Bodenbedeckung>\n" +
				             "        <GrundstueckIDREF>382929efa218</GrundstueckIDREF>\n" +
				             "        <Art>\n" +
				             "            <TextDe></TextDe>\n" +
				             "            <TextFr>Paturage</TextFr>\n" +
				             "        </Art>\n" +
				             "        <Flaeche>4728211</Flaeche>\n" +
				             "    </Bodenbedeckung>\n" +
				             "</BodenbedeckungList>\n", mut0.getXmlContent());
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées lorsque les surfaces correspondent à plusieurs immeubles et arrivent dans le désordre
	 */
	@Test
	public void testNouvellesSurfacesPlusieursImmeublesDansLeDesordre() throws Exception {

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

		final SurfaceAuSolRFDetector detector = new SurfaceAuSolRFDetector(xmlHelperRF, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, cacheSurfaces);

		// on envoie quatre nouvelles surfaces appartenant à deux immeubles
		Bodenbedeckung surface1 = newSurfaceAuSol("382929efa218", "Forêt", 37823);
		Bodenbedeckung surface2 = newSurfaceAuSol("457372892821", "Forêt", 34623);
		Bodenbedeckung surface3 = newSurfaceAuSol("382929efa218", "Paturage", 478323);
		Bodenbedeckung surface4 = newSurfaceAuSol("457372892821", "Paturage", 4728211);
		List<Bodenbedeckung> surfaces = Arrays.asList(surface1, surface2, surface3, surface4);
		detector.processSurfaces(IMPORT_ID, 2, surfaces.iterator(), null);

		// on devrait avoir deux événements de mutation de type CREATION qui concernent chacun des immeubles
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(2, mutations.size());
		Collections.sort(mutations, new MutationComparator());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.SURFACE_AU_SOL, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut0.getTypeMutation());
		assertEquals("382929efa218", mut0.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<BodenbedeckungList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <Bodenbedeckung>\n" +
				             "        <GrundstueckIDREF>382929efa218</GrundstueckIDREF>\n" +
				             "        <Art>\n" +
				             "            <TextDe></TextDe>\n" +
				             "            <TextFr>Forêt</TextFr>\n" +
				             "        </Art>\n" +
				             "        <Flaeche>37823</Flaeche>\n" +
				             "    </Bodenbedeckung>\n" +
				             "    <Bodenbedeckung>\n" +
				             "        <GrundstueckIDREF>382929efa218</GrundstueckIDREF>\n" +
				             "        <Art>\n" +
				             "            <TextDe></TextDe>\n" +
				             "            <TextFr>Paturage</TextFr>\n" +
				             "        </Art>\n" +
				             "        <Flaeche>478323</Flaeche>\n" +
				             "    </Bodenbedeckung>\n" +
				             "</BodenbedeckungList>\n", mut0.getXmlContent());

		final EvenementRFMutation mut1 = mutations.get(1);
		assertEquals(IMPORT_ID, mut1.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut1.getEtat());
		assertEquals(TypeEntiteRF.SURFACE_AU_SOL, mut1.getTypeEntite());
		assertEquals(TypeMutationRF.CREATION, mut1.getTypeMutation());
		assertEquals("457372892821", mut1.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<BodenbedeckungList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <Bodenbedeckung>\n" +
				             "        <GrundstueckIDREF>457372892821</GrundstueckIDREF>\n" +
				             "        <Art>\n" +
				             "            <TextDe></TextDe>\n" +
				             "            <TextFr>Forêt</TextFr>\n" +
				             "        </Art>\n" +
				             "        <Flaeche>34623</Flaeche>\n" +
				             "    </Bodenbedeckung>\n" +
				             "    <Bodenbedeckung>\n" +
				             "        <GrundstueckIDREF>457372892821</GrundstueckIDREF>\n" +
				             "        <Art>\n" +
				             "            <TextDe></TextDe>\n" +
				             "            <TextFr>Paturage</TextFr>\n" +
				             "        </Art>\n" +
				             "        <Flaeche>4728211</Flaeche>\n" +
				             "    </Bodenbedeckung>\n" +
				             "</BodenbedeckungList>\n", mut1.getXmlContent());
	}

	/**
	 * Ce test vérifie que des mutations sont bien créées si les surfaces au sol dans l'import existent dans la base de données mais pas avec les mêmes valeurs.
	 */
	@Test
	public void testSurfacesModifiees() throws Exception {

		final BienFondsRF immeuble = new BienFondsRF();
		immeuble.setIdRF("382929efa218");

		final SurfaceAuSolRF s1 = new SurfaceAuSolRF();
		s1.setImmeuble(immeuble);
		s1.setType("Forêt");
		s1.setSurface(37823);

		final SurfaceAuSolRF s2 = new SurfaceAuSolRF();
		s2.setImmeuble(immeuble);
		s2.setType("Paturage");
		s2.setSurface(4728211);

		immeuble.setSurfacesAuSol(new HashSet<>(Arrays.asList(s1, s2)));

		// un mock de DAO qui simule l'existence d'un immeuble
		immeubleRFDAO = new MockImmeubleRFDAO() {
			@Override
			public ImmeubleRF find(@NotNull ImmeubleRFKey key, FlushModeType flushModeOverride) {
				if (key.getIdRF().equals(immeuble.getIdRF())) {
					return immeuble;
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

		final SurfaceAuSolRFDetector detector = new SurfaceAuSolRFDetector(xmlHelperRF, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, cacheSurfaces);

		// on envoie deux nouveaux surfaces aves des modifications
		// - type différent
		final Bodenbedeckung surface1 = newSurfaceAuSol("382929efa218", "Forêt pluviale", 37823);
		// - surface différent
		final Bodenbedeckung surface2 = newSurfaceAuSol("382929efa218", "Paturage", 2);
		final List<Bodenbedeckung> surfaces = Arrays.asList(surface1, surface2);
		detector.processSurfaces(IMPORT_ID, 2, surfaces.iterator(), null);

		// on devrait avoir un seul événement de mutation de type MODIFICATION à l'état A_TRAITER dans la base
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(1, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.SURFACE_AU_SOL, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.MODIFICATION, mut0.getTypeMutation());
		assertEquals("382929efa218", mut0.getIdRF());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				             "<BodenbedeckungList xmlns=\"http://bedag.ch/capitastra/schemas/A51/v20140310/Datenexport/Grundstueck\">\n" +
				             "    <Bodenbedeckung>\n" +
				             "        <GrundstueckIDREF>382929efa218</GrundstueckIDREF>\n" +
				             "        <Art>\n" +
				             "            <TextDe></TextDe>\n" +
				             "            <TextFr>Forêt pluviale</TextFr>\n" +
				             "        </Art>\n" +
				             "        <Flaeche>37823</Flaeche>\n" +
				             "    </Bodenbedeckung>\n" +
				             "    <Bodenbedeckung>\n" +
				             "        <GrundstueckIDREF>382929efa218</GrundstueckIDREF>\n" +
				             "        <Art>\n" +
				             "            <TextDe></TextDe>\n" +
				             "            <TextFr>Paturage</TextFr>\n" +
				             "        </Art>\n" +
				             "        <Flaeche>2</Flaeche>\n" +
				             "    </Bodenbedeckung>\n" +
				             "</BodenbedeckungList>\n", mut0.getXmlContent());
	}

	/**
	 * Ce test vérifie qu'aucune mutation n'est créée si les données des immeubles dans l'import sont identiques avec l'état courant des immeubles stockés dans la DB.
	 */
	@Test
	public void testSurfacesIdentiques() throws Exception {

		final BienFondsRF immeuble = new BienFondsRF();
		immeuble.setIdRF("382929efa218");

		final SurfaceAuSolRF s1 = new SurfaceAuSolRF();
		s1.setImmeuble(immeuble);
		s1.setType("Forêt");
		s1.setSurface(37823);

		final SurfaceAuSolRF s2 = new SurfaceAuSolRF();
		s2.setImmeuble(immeuble);
		s2.setType("Paturage");
		s2.setSurface(4728211);

		immeuble.setSurfacesAuSol(new HashSet<>(Arrays.asList(s1, s2)));

		// un mock de DAO qui simule l'existence d'un immeuble
		immeubleRFDAO = new MockImmeubleRFDAO() {
			@Nullable
			@Override
			public ImmeubleRF find(@NotNull ImmeubleRFKey key, @Nullable FlushModeType flushModeOverride) {
				if (key.getIdRF().equals(immeuble.getIdRF())) {
					return immeuble;
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

		final SurfaceAuSolRFDetector detector = new SurfaceAuSolRFDetector(xmlHelperRF, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, cacheSurfaces);

		// on envoie deux nouveaux surfaces avec les données que celles dans la DB
		final Bodenbedeckung surface1 = newSurfaceAuSol("382929efa218", "Forêt", 37823);
		final Bodenbedeckung surface2 = newSurfaceAuSol("382929efa218", "Paturage", 4728211);
		final List<Bodenbedeckung> surfaces = Arrays.asList(surface1, surface2);
		detector.processSurfaces(IMPORT_ID, 2, surfaces.iterator(), null);

		// on ne devrait pas avoir de mutation
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(0, mutations.size());
	}

	/**
	 * Ce test vérifie que des mutations de suppression sont créées si les surfaces d'un immeuble n'apparaissent plus dans le fichier XML alors qu'elles existent dans la DB.
	 */
	@Test
	public void testSurfacesSupprimees() throws Exception {

		final BienFondsRF immeuble = new BienFondsRF();
		immeuble.setIdRF("382929efa218");

		final SurfaceAuSolRF s1 = new SurfaceAuSolRF();
		s1.setImmeuble(immeuble);
		s1.setType("Forêt");
		s1.setSurface(37823);
		s1.setDateDebut(RegDate.get(2003, 11, 22));
		s1.setDateFin(RegDate.get(2005, 1, 2));

		final SurfaceAuSolRF s2 = new SurfaceAuSolRF();
		s2.setImmeuble(immeuble);
		s2.setType("Paturage");
		s2.setSurface(4728211);
		s2.setDateDebut(RegDate.get(2003, 11, 22));

		immeuble.setSurfacesAuSol(new HashSet<>(Arrays.asList(s1, s2)));

		// un mock de DAO qui simule l'existence d'un immeuble
		immeubleRFDAO = new MockImmeubleRFDAO(immeuble);

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

		final SurfaceAuSolRFDetector detector = new SurfaceAuSolRFDetector(xmlHelperRF, immeubleRFDAO, evenementRFImportDAO, evenementRFMutationDAO, transactionManager, cacheSurfaces);

		// on envoie une liste d'immeuble vide
		final List<Bodenbedeckung> surfaces = Collections.emptyList();
		detector.processSurfaces(IMPORT_ID, 2, surfaces.iterator(), null);

		// on devrait avoir un seul événement de mutation de type SUPPRESSION à l'état A_TRAITER dans la base
		final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
		assertEquals(1, mutations.size());

		final EvenementRFMutation mut0 = mutations.get(0);
		assertEquals(IMPORT_ID, mut0.getParentImport().getId());
		assertEquals(EtatEvenementRF.A_TRAITER, mut0.getEtat());
		assertEquals(TypeEntiteRF.SURFACE_AU_SOL, mut0.getTypeEntite());
		assertEquals(TypeMutationRF.SUPPRESSION, mut0.getTypeMutation());
		assertEquals("382929efa218", mut0.getIdRF());
		assertNull(mut0.getXmlContent());
	}

	@NotNull
	private static Bodenbedeckung newSurfaceAuSol(String idImmeubleRF, String type, int surface) {
		final Bodenbedeckung s = new Bodenbedeckung();
		s.setGrundstueckIDREF(idImmeubleRF);
		s.setArt(new CapiCode("", type));
		s.setFlaeche(surface);
		return s;
	}
}
