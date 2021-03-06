package ch.vd.unireg.registrefoncier.dataimport.processor;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.TypeCommunaute;
import ch.vd.unireg.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.processor.MutationRFProcessorTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SuppressWarnings("Duplicates")
public class AyantDroitRFProcessorTest extends MutationRFProcessorTestCase {
	private EvenementRFMutationDAO evenementRFMutationDAO;

	private AyantDroitRFDAO ayantDroitRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private AyantDroitRFProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		this.ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		final XmlHelperRF xmlHelperRF = getBean(XmlHelperRF.class, "xmlHelperRF");

		this.processor = new AyantDroitRFProcessor(ayantDroitRFDAO, immeubleRFDAO, xmlHelperRF);
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
	 * Ce test vérifie que le processing d'une mutation de création crée bien un nouvel ayant-droit dans la DB
	 */
	@Test
	public void testProcessMutationCreation() throws Exception {

		// précondition : la base est vide
		doInNewTransaction(status -> {
			assertEquals(0, ayantDroitRFDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_ayantdroit_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.AYANT_DROIT, TypeMutationRF.CREATION, null, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et l'ayant-droit est créé en base
		assertOnePersonnePhysiqueInDB("3893728273382823", 3727, "Nom", "Prénom", RegDate.get(1956, 1, 23), 827288022);
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de création traite bien le cas des communautés
	 */
	@Test
	public void testProcessMutationCreationCommunaute() throws Exception {

		// précondition : la base est vide
		doInNewTransaction(status -> {
			assertEquals(0, ayantDroitRFDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_ayantdroit_communaute_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.AYANT_DROIT, TypeMutationRF.CREATION, null, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et l'ayant-droit est créé en base
		assertOneCommunauteInDB("72828ce8f830a", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de création traite bien le cas des communautés
	 */
	@Test
	public void testProcessMutationCreationImmeubleBeneficiaire() throws Exception {

		// précondition : la base contient un immeuble
		doInNewTransaction(status -> {
			assertEquals(0, ayantDroitRFDAO.getAll().size());
			final ImmeubleRF immeuble = new BienFondsRF();
			immeuble.setIdRF("48238919011");
			immeubleRFDAO.save(immeuble);
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_ayantdroit_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.AYANT_DROIT, TypeMutationRF.CREATION, "48238919011", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et l'ayant-droit est créé en base
		assertOneImmeubleBeneficiaireInDB("48238919011");
	}

	/*
	 * Ce test vérifie que le processing d'une mutation de modification d'un ayant-droit fonctionne bien.
	 */
	@Test
	public void testProcessMutationModification() throws Exception {

		// précondition : il y a déjà un ayant-droit dans la base avec un prénom différent de celui de la mutation
		doInNewTransaction(status -> {
			addPersonnePhysiqueRF("Autre prénom", "Nom", date(1956, 1, 23), "3893728273382823", 3727, 827288022L);
			assertEquals(1, ayantDroitRFDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_ayantdroit_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.AYANT_DROIT, TypeMutationRF.MODIFICATION, null, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée, l'ayant-droit est mis-à-jour en base
		assertOnePersonnePhysiqueInDB("3893728273382823", 3727, "Nom", "Prénom", RegDate.get(1956, 1, 23), 827288022);
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de création crée bien un nouvel ayant-droit dans la DB
	 */
	@Test
	public void testProcessMutationCreationServitude() throws Exception {

		// précondition : la base est vide
		doInNewTransaction(status -> {
			assertEquals(0, ayantDroitRFDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_ayantdroit_servitude_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.AYANT_DROIT, TypeMutationRF.CREATION, null, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et l'ayant-droit est créé en base
		assertOnePersonnePhysiqueInDB("3893728273382823", 0, "Nom", "Prénom", RegDate.get(1956, 1, 23), 827288022);
	}

	/*
	 * Ce test vérifie que le processing d'une mutation de modification d'un ayant-droit fonctionne bien.
	 */
	@Test
	public void testProcessMutationModificationServitude() throws Exception {

		// précondition : il y a déjà un ayant-droit dans la base avec un prénom différent de celui de la mutation
		doInNewTransaction(status -> {
			addPersonnePhysiqueRF("Autre prénom", "Nom", date(1956, 1, 23), "3893728273382823", 3727, 827288022L);
			assertEquals(1, ayantDroitRFDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_ayantdroit_servitude_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.AYANT_DROIT, TypeMutationRF.MODIFICATION, null, null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée, l'ayant-droit est mis-à-jour en base
		assertOnePersonnePhysiqueInDB("3893728273382823", 3727, "Nom", "Prénom", RegDate.get(1956, 1, 23), 827288022);
	}

}