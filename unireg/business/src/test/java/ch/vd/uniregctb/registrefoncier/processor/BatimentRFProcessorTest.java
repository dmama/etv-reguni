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

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceBatimentRF;
import ch.vd.uniregctb.registrefoncier.dao.BatimentRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class BatimentRFProcessorTest extends MutationRFProcessorTestCase {

	private EvenementRFMutationDAO evenementRFMutationDAO;

	private BatimentRFDAO batimentRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private BatimentRFProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.batimentRFDAO = getBean(BatimentRFDAO.class, "batimentRFDAO");
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		final XmlHelperRF xmlHelperRF = getBean(XmlHelperRF.class, "xmlHelperRF");

		this.processor = new BatimentRFProcessor(batimentRFDAO, immeubleRFDAO, xmlHelperRF);
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
	 * Ce test vérifie que le processing d'une mutation de création crée bien un nouveau bâtiment dans la DB
	 */
	@Test
	public void testProcessMutationCreation() throws Exception {

		// précondition : la base est vide
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				assertEquals(0, immeubleRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_batiment_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère l'immeuble qui correspond au bâtiment
		insertImmeuble("_1f109152381026b501381028a73d1852");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.BATIMENT, TypeMutationRF.CREATION, "1f109152381026b50138102aa28557e0");

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation);
			}
		});

		// postcondition : la mutation est traitée et le bâtiment est créé en base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<BatimentRF> batiments = batimentRFDAO.getAll();
				assertEquals(1, batiments.size());

				final BatimentRF batiment0 = batiments.get(0);
				assertEquals("1f109152381026b50138102aa28557e0", batiment0.getMasterIdRF());
				assertEquals("Habitation", batiment0.getType());
				assertEmpty(batiment0.getSurfaces());

				final Set<ImplantationRF> implantations = batiment0.getImplantations();
				assertEquals(1, implantations.size());
				final ImplantationRF implantation0 = implantations.iterator().next();
				assertEquals(RegDate.get(2016, 10, 1), implantation0.getDateDebut());
				assertNull(implantation0.getDateFin());
				assertEquals(Integer.valueOf(104), implantation0.getSurface());
				assertEquals("_1f109152381026b501381028a73d1852", implantation0.getImmeuble().getIdRF());
			}
		});
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de modification met bien à jour le bâtiment existant dans la DB
	 */
	@Test
	public void testProcessMutationModification() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2016, 8, 20);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà un bâtiment dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				BienFondRF bienFond = new BienFondRF();
				bienFond.setIdRF("_1f109152381026b501381028a73d1852");
				bienFond = (BienFondRF) immeubleRFDAO.save(bienFond);
				
				BatimentRF batiment = new BatimentRF();
				batiment.setMasterIdRF("1f109152381026b50138102aa28557e0");
				batiment.setType("Habitation");
				batiment.addImplantation(new ImplantationRF(100, bienFond, dateImportInitial, null));
				
				batimentRFDAO.save(batiment);
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_batiment_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateSecondImport, TypeEntiteRF.BATIMENT, TypeMutationRF.MODIFICATION, "1f109152381026b50138102aa28557e0");

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation);
			}
		});

		// postcondition : la mutation est traitée et la bâtiment est bien mis-à-jour
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<BatimentRF> batiments = batimentRFDAO.getAll();
				assertEquals(1, batiments.size());

				final BatimentRF batiment0 = batiments.get(0);
				assertEquals("1f109152381026b50138102aa28557e0", batiment0.getMasterIdRF());
				assertEquals("Habitation", batiment0.getType());

				// la surface n'est toujours pas renseignée
				assertEmpty(batiment0.getSurfaces());

				// par contre, il y a une nouvelle implantation
				final Set<ImplantationRF> implantations = batiment0.getImplantations();
				assertEquals(2, implantations.size());

				final List<ImplantationRF> implantationList = new ArrayList<>(implantations);
				Collections.sort(implantationList, new DateRangeComparator<>());

				// la première implantation doit être fermée la veille de la date d'import
				final ImplantationRF implantation0 = implantationList.get(0);
				assertEquals(dateImportInitial, implantation0.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), implantation0.getDateFin());
				assertEquals(Integer.valueOf(100), implantation0.getSurface());

				// la seconde implantation doit commencer à la date de l'import
				final ImplantationRF implantation1 = implantationList.get(1);
				assertEquals(dateSecondImport, implantation1.getDateDebut());
				assertNull(implantation1.getDateFin());
				assertEquals(Integer.valueOf(104), implantation1.getSurface());
			}
		});
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de supression femre bien le bâtiment existant dans la DB
	 */
	@Test
	public void testProcessMutationSuppression() throws Exception {

		final RegDate dateImportInitial = RegDate.get(2016, 8, 20);
		final RegDate dateSecondImport = RegDate.get(2016, 10, 1);

		// précondition : il y a déjà un bâtiment dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				BienFondRF bienFond = new BienFondRF();
				bienFond.setIdRF("_1f109152381026b501381028a73d1852");
				bienFond = (BienFondRF) immeubleRFDAO.save(bienFond);

				BatimentRF batiment = new BatimentRF();
				batiment.setMasterIdRF("1f109152381026b50138102aa28557e0");
				batiment.setType("Habitation");
				batiment.addImplantation(new ImplantationRF(100, bienFond, dateImportInitial, null));

				batiment.addSurface(new SurfaceBatimentRF(100, dateImportInitial, null));

				batimentRFDAO.save(batiment);
			}
		});

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(null, dateSecondImport, TypeEntiteRF.BATIMENT, TypeMutationRF.SUPPRESSION, "1f109152381026b50138102aa28557e0");

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation);
			}
		});

		// postcondition : la mutation est traitée et les implantations et surfaces du bâtiment sont toutes fermées
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<BatimentRF> batiments = batimentRFDAO.getAll();
				assertEquals(1, batiments.size());

				final BatimentRF batiment0 = batiments.get(0);
				assertEquals("1f109152381026b50138102aa28557e0", batiment0.getMasterIdRF());
				assertEquals("Habitation", batiment0.getType());

				// la surface est fermée
				final Set<SurfaceBatimentRF> surfaces = batiment0.getSurfaces();
				assertEquals(1, surfaces.size());
				final SurfaceBatimentRF surface0 = surfaces.iterator().next();
				assertEquals(100, surface0.getSurface());
				assertEquals(dateImportInitial, surface0.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), surface0.getDateFin());

				// l'implantation doit être fermée la veille de la date d'import
				final Set<ImplantationRF> implantations = batiment0.getImplantations();
				assertEquals(1, implantations.size());
				final ImplantationRF implantation0 = implantations.iterator().next();
				assertEquals(dateImportInitial, implantation0.getDateDebut());
				assertEquals(dateSecondImport.getOneDayBefore(), implantation0.getDateFin());
				assertEquals(Integer.valueOf(100), implantation0.getSurface());
			}
		});
	}

}