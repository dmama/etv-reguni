package ch.vd.unireg.registrefoncier.dataimport.processor;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.dao.CommuneRFDAO;
import ch.vd.unireg.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.unireg.registrefoncier.processor.MutationRFProcessorTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class CommuneRFProcessorTest extends MutationRFProcessorTestCase {

	private EvenementRFMutationDAO evenementRFMutationDAO;

	private CommuneRFDAO communeRFDAO;
	private CommuneRFProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");

		final XmlHelperRF xmlHelperRF = getBean(XmlHelperRF.class, "xmlHelperRF");
		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");

		this.processor = new CommuneRFProcessor(communeRFDAO, infraService, xmlHelperRF);
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
	 * Ce test vérifie que le processing d'une mutation de création crée bien un nouvel commune dans la DB
	 */
	@Test
	public void testProcessMutationCreation() throws Exception {

		// précondition : la base est vide
		doInNewTransaction(status -> {
			assertEquals(0, communeRFDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_commune_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.COMMUNE, TypeMutationRF.CREATION, "246", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et la commune est créée en base
		doInNewTransaction(status -> {
			final List<CommuneRF> communes = communeRFDAO.getAll();
			assertEquals(1, communes.size());

			final CommuneRF commune0 = communes.get(0);
			assertNotNull(commune0);
			assertEquals(246, commune0.getNoRf());
			assertEquals("Nyon", commune0.getNomRf());
			assertEquals(5724, commune0.getNoOfs());
			return null;
		});
	}

	/**
	 * Ce test vérifie que le processing d'une fusion de commune avec réutilisation du noRF ferme bien l'ancienne commune à la veille de l'import et ouvre bien la nouvelle commune à la date de l'import.
	 */
	@Test
	public void testProcessMutationModification() throws Exception {

		// précondition : il y a déjà une commune dans la base
		doInNewTransaction(status -> {
			final CommuneRF commune = new CommuneRF();
			commune.setNoRf(246);
			commune.setNomRf("Pétahouchnok");
			commune.setNoOfs(5000);
			communeRFDAO.save(commune);

			assertEquals(1, communeRFDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/processor/mutation_commune_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.COMMUNE, TypeMutationRF.MODIFICATION, "246", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et la commune est modifiée en base
		doInNewTransaction(status -> {
			final List<CommuneRF> communes = communeRFDAO.getAll();
			assertEquals(2, communes.size());
			Collections.sort(communes, new DateRangeComparator<>());

			// l'ancienne commune est fermée
			final CommuneRF commune0 = communes.get(0);
			assertNotNull(commune0);
			assertEquals(246, commune0.getNoRf());
			assertEquals("Pétahouchnok", commune0.getNomRf());
			assertEquals(5000, commune0.getNoOfs());
			assertNull(commune0.getDateDebut());
			assertEquals(RegDate.get(2016, 9, 30), commune0.getDateFin());

			// la nouvelle commune est créée
			final CommuneRF commune1 = communes.get(1);
			assertNotNull(commune1);
			assertEquals(246, commune1.getNoRf());
			assertEquals("Nyon", commune1.getNomRf());
			assertEquals(5724, commune1.getNoOfs());
			assertEquals(RegDate.get(2016, 10, 1), commune1.getDateDebut());
			assertNull(commune1.getDateFin());
			return null;
		});
	}
}