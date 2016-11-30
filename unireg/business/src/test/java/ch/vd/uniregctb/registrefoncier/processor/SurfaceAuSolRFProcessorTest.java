package ch.vd.uniregctb.registrefoncier.processor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.SurfaceAuSolRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class SurfaceAuSolRFProcessorTest extends MutationRFProcessorTestCase {

	private ImmeubleRFDAO immeubleRFDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private SurfaceAuSolRFDAO surfaceAuSolRFDAO;
	private SurfaceAuSolRFProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		this.surfaceAuSolRFDAO = getBean(SurfaceAuSolRFDAO.class, "surfaceAuSolRFDAO");
		final XmlHelperRF xmlHelperRF = getBean(XmlHelperRF.class, "xmlHelperRF");

		this.processor = new SurfaceAuSolRFProcessor(immeubleRFDAO, surfaceAuSolRFDAO, xmlHelperRF);
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
			processor.process(mutation);
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
			processor.process(mutation);
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
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				assertEquals(0, surfaceAuSolRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_surfaceausol_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long immeubleId = insertImmeuble("382929efa218");
		final Long mutationId = insertMutation(xml, dateImport, TypeEntiteRF.SURFACE_AU_SOL, TypeMutationRF.CREATION, "382929efa218");

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation);
			}
		});

		// postcondition : la mutation est traitée et les nouvelles surfaces au sol sont créées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final ImmeubleRF immeuble = immeubleRFDAO.get(immeubleId);
				assertNotNull(immeuble);

				final Set<SurfaceAuSolRF> surfaces = immeuble.getSurfacesAuSol();
				assertNotNull(surfaces);
				assertEquals(2, surfaces.size());

				final List<SurfaceAuSolRF> surfacesList = new ArrayList<>(surfaces);
				Collections.sort(surfacesList, (o1, o2) -> Integer.compare(o1.getSurface(), o2.getSurface()));

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
			}
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
		final Long immeubleId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				BienFondRF immeuble = new BienFondRF();
				immeuble.setIdRF(idImmeubleRF);
				immeuble = (BienFondRF) immeubleRFDAO.save(immeuble);

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
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_surfaceausol_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.SURFACE_AU_SOL, TypeMutationRF.MODIFICATION, "382929efa218");

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation);
			}
		});

		// postcondition : la mutation est traitée et :
		//  - une surface au sol est fermée
		//  - une surface au sol n'est pas changée
		//  - une nouvelle surface au sol est créée
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final ImmeubleRF immeuble = immeubleRFDAO.get(immeubleId);
				assertNotNull(immeuble);

				final Set<SurfaceAuSolRF> surfaces = immeuble.getSurfacesAuSol();
				assertNotNull(surfaces);
				assertEquals(3, surfaces.size());

				final List<SurfaceAuSolRF> surfacesList = new ArrayList<>(surfaces);
				Collections.sort(surfacesList, (o1, o2) -> Integer.compare(o1.getSurface(), o2.getSurface()));

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
			}
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
		final Long immeubleId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				BienFondRF immeuble = new BienFondRF();
				immeuble.setIdRF(idImmeubleRF);
				immeuble = (BienFondRF) immeubleRFDAO.save(immeuble);

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
			}
		});

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(null, dateSecondImport, TypeEntiteRF.SURFACE_AU_SOL, TypeMutationRF.SUPPRESSION, "382929efa218");

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation);
			}
		});

		// postcondition : la mutation est traitée et tous les surfaces au sol ouvertes sont maintenant fermées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final ImmeubleRF immeuble = immeubleRFDAO.get(immeubleId);
				assertNotNull(immeuble);

				final Set<SurfaceAuSolRF> surfaces = immeuble.getSurfacesAuSol();
				assertNotNull(surfaces);
				assertEquals(2, surfaces.size());

				final List<SurfaceAuSolRF> surfacesList = new ArrayList<>(surfaces);
				Collections.sort(surfacesList, (o1, o2) -> Integer.compare(o1.getSurface(), o2.getSurface()));

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
			}
		});
	}
}