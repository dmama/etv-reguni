package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalImmeuble;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.SurfaceAuSolRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.processor.MutationRFProcessorTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class SurfaceAuSolRFProcessorTest extends MutationRFProcessorTestCase {

	private ImmeubleRFDAO immeubleRFDAO;
	private EvenementFiscalDAO evenementFiscalDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private SurfaceAuSolRFDAO surfaceAuSolRFDAO;
	private SurfaceAuSolRFProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		this.evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
		this.surfaceAuSolRFDAO = getBean(SurfaceAuSolRFDAO.class, "surfaceAuSolRFDAO");
		final XmlHelperRF xmlHelperRF = getBean(XmlHelperRF.class, "xmlHelperRF");
		final EvenementFiscalService evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");

		this.processor = new SurfaceAuSolRFProcessor(immeubleRFDAO, surfaceAuSolRFDAO, xmlHelperRF, evenementFiscalService);
	}

	/**
	 * Ce test vérifie qu'une mutation déjà traitée ne peut pas être traitée une seconde fois.
	 */
	@Test
	public void testProcessMutationDejaTraitee() throws Exception {

		final EvenementRFMutation mutation = new EvenementRFMutation();
		mutation.setId(1L);
		mutation.setEtat(EtatEvenementRF.TRAITE);
		try {
			processor.process(mutation, false, null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La mutation n°1 est déjà traitée (état=[TRAITE]).", e.getMessage());
		}
	}

	/**
	 * Ce test vérifie qu'une mutation forcée ne peut pas être traitée.
	 */
	@Test
	public void testProcessMutationForcee() throws Exception {

		final EvenementRFMutation mutation = new EvenementRFMutation();
		mutation.setId(1L);
		mutation.setEtat(EtatEvenementRF.FORCE);
		try {
			processor.process(mutation, false, null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La mutation n°1 est déjà traitée (état=[FORCE]).", e.getMessage());
		}
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de création crée bien des nouvelles surfaces au sol
	 */
	@Test
	public void testProcessMutationCreation() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);

		// précondition : la base est vide
		doInNewTransaction(status -> {
			assertEquals(0, surfaceAuSolRFDAO.getAll().size());
			assertEquals(0, evenementFiscalDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_surfaceausol_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long immeubleId = insertImmeuble("382929efa218");
		final Long mutationId = insertMutation(xml, dateImport, TypeEntiteRF.SURFACE_AU_SOL, TypeMutationRF.CREATION, "382929efa218", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et les nouvelles surfaces au sol sont créées
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(immeubleId);
			assertNotNull(immeuble);

			final Set<SurfaceAuSolRF> surfaces = immeuble.getSurfacesAuSol();
			assertNotNull(surfaces);
			assertEquals(2, surfaces.size());

			final List<SurfaceAuSolRF> surfacesList = new ArrayList<>(surfaces);
			surfacesList.sort(Comparator.comparingInt(SurfaceAuSolRF::getSurface));

			final SurfaceAuSolRF surface0 = surfacesList.get(0);
			assertNotNull(surface0);
			assertEquals(dateImport, surface0.getDateDebut());
			assertNull(surface0.getDateFin());
			assertEquals("382929efa218", surface0.getImmeuble().getIdRF());
			assertEquals(37823, surface0.getSurface());
			assertEquals("Forêt", surface0.getType());

			final SurfaceAuSolRF surface1 = surfacesList.get(1);
			assertNotNull(surface1);
			assertEquals(dateImport, surface1.getDateDebut());
			assertNull(surface1.getDateFin());
			assertEquals("382929efa218", surface1.getImmeuble().getIdRF());
			assertEquals(4728211, surface1.getSurface());
			assertEquals("Paturage", surface1.getType());
			return null;
		});

		// postcondition : l'événement fiscal correspondant a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_SURFACE_AU_SOL, event0.getType());
			assertEquals(RegDate.get(2016, 10, 1), event0.getDateValeur());
			assertEquals("382929efa218", event0.getImmeuble().getIdRF());

			return null;
		});
	}

	/*
	 * Ce test vérifie que le processing d'une mutation de modification de surfaces au sol fonctionne bien.
	 */
	@Test
	public void testProcessMutationModification() throws Exception {

		final String idImmeubleRF = "382929efa218";
		final RegDate dateImportInitial = RegDate.get(2015, 3, 17);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà un immeuble dans la base avec deux surfaces au sol
		final Long immeubleId = doInNewTransaction(status -> {

			BienFondsRF immeuble = new BienFondsRF();
			immeuble.setIdRF(idImmeubleRF);
			immeuble = (BienFondsRF) immeubleRFDAO.save(immeuble);

			final SurfaceAuSolRF surface0 = new SurfaceAuSolRF();
			surface0.setDateDebut(dateImportInitial);
			surface0.setImmeuble(immeuble);
			surface0.setSurface(37823);
			surface0.setType("Forêt");

			final SurfaceAuSolRF surface1 = new SurfaceAuSolRF();
			surface1.setDateDebut(dateImportInitial);
			surface1.setImmeuble(immeuble);
			surface1.setSurface(47347);
			surface1.setType("Héliport");

			surfaceAuSolRFDAO.save(surface0);
			surfaceAuSolRFDAO.save(surface1);

			return immeuble.getId();
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_surfaceausol_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.SURFACE_AU_SOL, TypeMutationRF.MODIFICATION, "382929efa218", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et :
		//  - une surface au sol est fermée
		//  - une surface au sol n'est pas changée
		//  - une nouvelle surface au sol est créée
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(immeubleId);
			assertNotNull(immeuble);

			final Set<SurfaceAuSolRF> surfaces = immeuble.getSurfacesAuSol();
			assertNotNull(surfaces);
			assertEquals(3, surfaces.size());

			final List<SurfaceAuSolRF> surfacesList = new ArrayList<>(surfaces);
			surfacesList.sort(Comparator.comparingInt(SurfaceAuSolRF::getSurface));

			// surface inchangée
			final SurfaceAuSolRF surface0 = surfacesList.get(0);
			assertNotNull(surface0);
			assertEquals(dateImportInitial, surface0.getDateDebut());
			assertNull(surface0.getDateFin());
			assertEquals("382929efa218", surface0.getImmeuble().getIdRF());
			assertEquals(37823, surface0.getSurface());
			assertEquals("Forêt", surface0.getType());

			// surface fermée
			final SurfaceAuSolRF surface1 = surfacesList.get(1);
			assertNotNull(surface1);
			assertEquals(dateImportInitial, surface1.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), surface1.getDateFin());
			assertEquals("382929efa218", surface1.getImmeuble().getIdRF());
			assertEquals(47347, surface1.getSurface());
			assertEquals("Héliport", surface1.getType());

			// surface créée
			final SurfaceAuSolRF surface2 = surfacesList.get(2);
			assertNotNull(surface2);
			assertEquals(dateSecondImport, surface2.getDateDebut());
			assertNull(surface2.getDateFin());
			assertEquals("382929efa218", surface2.getImmeuble().getIdRF());
			assertEquals(4728211, surface2.getSurface());
			assertEquals("Paturage", surface2.getType());
			return null;
		});

		// postcondition : l'événement fiscal correspondant a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_SURFACE_AU_SOL, event0.getType());
			assertEquals(RegDate.get(2016, 10, 1), event0.getDateValeur());
			assertEquals("382929efa218", event0.getImmeuble().getIdRF());

			return null;
		});
	}

	/*
	 * Ce test vérifie que le processing d'une mutation de suppression de surfaces au sol fonctionne bien.
	 */
	@Test
	public void testProcessMutationSuppression() throws Exception {

		final String idImmeubleRF = "382929efa218";
		final RegDate dateImportInitial = RegDate.get(2015, 3, 17);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà un immeuble dans la base avec deux surfaces au sol
		final Long immeubleId = doInNewTransaction(status -> {

			BienFondsRF immeuble = new BienFondsRF();
			immeuble.setIdRF(idImmeubleRF);
			immeuble = (BienFondsRF) immeubleRFDAO.save(immeuble);

			final SurfaceAuSolRF surface0 = new SurfaceAuSolRF();
			surface0.setDateDebut(dateImportInitial);
			surface0.setDateFin(RegDate.get(2015, 6, 2));
			surface0.setImmeuble(immeuble);
			surface0.setSurface(37823);
			surface0.setType("Forêt");

			final SurfaceAuSolRF surface1 = new SurfaceAuSolRF();
			surface1.setDateDebut(dateImportInitial);
			surface1.setImmeuble(immeuble);
			surface1.setSurface(47347);
			surface1.setType("Héliport");

			surfaceAuSolRFDAO.save(surface0);
			surfaceAuSolRFDAO.save(surface1);

			return immeuble.getId();
		});

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(null, dateSecondImport, TypeEntiteRF.SURFACE_AU_SOL, TypeMutationRF.SUPPRESSION, "382929efa218", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et tous les surfaces au sol ouvertes sont maintenant fermées
		doInNewTransaction(status -> {
			final ImmeubleRF immeuble = immeubleRFDAO.get(immeubleId);
			assertNotNull(immeuble);

			final Set<SurfaceAuSolRF> surfaces = immeuble.getSurfacesAuSol();
			assertNotNull(surfaces);
			assertEquals(2, surfaces.size());

			final List<SurfaceAuSolRF> surfacesList = new ArrayList<>(surfaces);
			surfacesList.sort(Comparator.comparingInt(SurfaceAuSolRF::getSurface));

			// surface inchangée
			final SurfaceAuSolRF surface0 = surfacesList.get(0);
			assertNotNull(surface0);
			assertEquals(dateImportInitial, surface0.getDateDebut());
			assertEquals(RegDate.get(2015, 6, 2), surface0.getDateFin());
			assertEquals("382929efa218", surface0.getImmeuble().getIdRF());
			assertEquals(37823, surface0.getSurface());
			assertEquals("Forêt", surface0.getType());

			// surface fermée
			final SurfaceAuSolRF surface1 = surfacesList.get(1);
			assertNotNull(surface1);
			assertEquals(dateImportInitial, surface1.getDateDebut());
			assertEquals(dateSecondImport.getOneDayBefore(), surface1.getDateFin());
			assertEquals("382929efa218", surface1.getImmeuble().getIdRF());
			assertEquals(47347, surface1.getSurface());
			assertEquals("Héliport", surface1.getType());
			return null;
		});

		// postcondition : l'événement fiscal correspondant a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_SURFACE_AU_SOL, event0.getType());
			assertEquals(RegDate.get(2016, 10, 1), event0.getDateValeur());
			assertEquals("382929efa218", event0.getImmeuble().getIdRF());

			return null;
		});
	}
}