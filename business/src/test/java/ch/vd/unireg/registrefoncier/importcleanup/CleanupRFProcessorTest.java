package ch.vd.unireg.registrefoncier.importcleanup;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.MockStatusManager;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImport;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.MockEvenementRFImportDAO;
import ch.vd.unireg.evenement.registrefoncier.MockEvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeImportRF;
import ch.vd.unireg.registrefoncier.MockRegistreFoncierImportService;
import ch.vd.unireg.transaction.MockTransactionManager;

import static ch.vd.unireg.common.WithoutSpringTest.assertEmpty;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public class CleanupRFProcessorTest {

	/**
	 * Ce test vérifie que le paramètre du nombre d'imports à retenir est bien respecté et que les types des imports sont bien pris en compte.
	 */
	@Test
	public void testDetermineImportsToDeleteRetainSize() throws Exception {

		final EvenementRFImport imp1 = newImport(1L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp2 = newImport(2L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp3 = newImport(3L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp4 = newImport(4L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp5 = newImport(5L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp6 = newImport(6L, EtatEvenementRF.TRAITE, TypeImportRF.SERVITUDES);
		final EvenementRFImport imp7 = newImport(7L, EtatEvenementRF.TRAITE, TypeImportRF.SERVITUDES);
		final EvenementRFImport imp8 = newImport(8L, EtatEvenementRF.TRAITE, TypeImportRF.SERVITUDES);
		final EvenementRFImport imp9 = newImport(9L, EtatEvenementRF.TRAITE, TypeImportRF.SERVITUDES);
		final EvenementRFImport imp10 = newImport(10L, EtatEvenementRF.TRAITE, TypeImportRF.SERVITUDES);
		final MockEvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO(imp1, imp2, imp3, imp4, imp5, imp6, imp7, imp8, imp9, imp10);
		final EvenementRFMutationDAO evenmentRFMutationDAO = new MockEvenementRFMutationDAO();

		final PlatformTransactionManager transactionManager = new MockTransactionManager();
		final CleanupRFProcessorResults results = new CleanupRFProcessorResults();

		// on ne garde aucun des imports récents
		{
			final CleanupRFProcessor processor = new CleanupRFProcessor(evenementRFImportDAO, evenmentRFMutationDAO, null, transactionManager, 0);
			final List<Long> ids = processor.determineImportsToDelete(results, TypeImportRF.PRINCIPAL);
			assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), ids);
		}

		// on garde les 2 plus récents
		{
			final CleanupRFProcessor processor = new CleanupRFProcessor(evenementRFImportDAO, evenmentRFMutationDAO, null, transactionManager, 2);
			final List<Long> ids = processor.determineImportsToDelete(results, TypeImportRF.PRINCIPAL);
			assertEquals(Arrays.asList(1L, 2L, 3L), ids);
		}

		// on garde les 4 plus récents
		{
			final CleanupRFProcessor processor = new CleanupRFProcessor(evenementRFImportDAO, evenmentRFMutationDAO, null, transactionManager, 4);
			final List<Long> ids = processor.determineImportsToDelete(results, TypeImportRF.PRINCIPAL);
			assertEquals(Collections.singletonList(1L), ids);
		}

		// on garde les 6 plus récents
		{
			final CleanupRFProcessor processor = new CleanupRFProcessor(evenementRFImportDAO, evenmentRFMutationDAO, null, transactionManager, 6);
			final List<Long> ids = processor.determineImportsToDelete(results, TypeImportRF.PRINCIPAL);
			assertEmpty(ids);
		}
	}

	/**
	 * Ce test vérifie que seuls les imports traités sont candidats à être effacés.
	 */
	@Test
	public void testDetermineImportsToDeleteByState() throws Exception {

		final EvenementRFImport imp1 = newImport(1L, EtatEvenementRF.A_TRAITER, TypeImportRF.SERVITUDES);
		final EvenementRFImport imp2 = newImport(2L, EtatEvenementRF.EN_ERREUR, TypeImportRF.SERVITUDES);
		final EvenementRFImport imp3 = newImport(3L, EtatEvenementRF.EN_TRAITEMENT, TypeImportRF.SERVITUDES);
		final EvenementRFImport imp4 = newImport(4L, EtatEvenementRF.FORCE, TypeImportRF.SERVITUDES);
		final EvenementRFImport imp5 = newImport(5L, EtatEvenementRF.TRAITE, TypeImportRF.SERVITUDES);
		final MockEvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO(imp1, imp2, imp3, imp4, imp5);
		final EvenementRFMutationDAO evenmentRFMutationDAO = new MockEvenementRFMutationDAO();

		final PlatformTransactionManager transactionManager = new MockTransactionManager();
		final CleanupRFProcessorResults results = new CleanupRFProcessorResults();

		final CleanupRFProcessor processor = new CleanupRFProcessor(evenementRFImportDAO, evenmentRFMutationDAO, null, transactionManager, 0);
		final List<Long> ids = processor.determineImportsToDelete(results, TypeImportRF.SERVITUDES);
		assertEquals(Arrays.asList(4L, 5L), ids);
	}

	/**
	 * Ce test vérifie que seuls les imports traités avec toutes les mutations traitées sont candidats à être effacés.
	 */
	@Test
	public void testDetermineImportsToDeleteByMutationState() throws Exception {

		final EvenementRFImport imp1 = newImport(1L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp2 = newImport(2L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp3 = newImport(3L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp4 = newImport(4L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp5 = newImport(5L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final MockEvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO(imp1, imp2, imp3, imp4, imp5);

		final EvenementRFMutation mut1 = newMutation(1L, EtatEvenementRF.A_TRAITER, imp1);
		final EvenementRFMutation mut2 = newMutation(2L, EtatEvenementRF.EN_ERREUR, imp2);
		final EvenementRFMutation mut3 = newMutation(3L, EtatEvenementRF.EN_TRAITEMENT, imp3);
		final EvenementRFMutation mut4 = newMutation(4L, EtatEvenementRF.FORCE, imp4);
		final EvenementRFMutation mut5 = newMutation(5L, EtatEvenementRF.TRAITE, imp5);
		final EvenementRFMutationDAO evenmentRFMutationDAO = new MockEvenementRFMutationDAO(mut1, mut2, mut3, mut4, mut5);

		final PlatformTransactionManager transactionManager = new MockTransactionManager();
		final CleanupRFProcessorResults results = new CleanupRFProcessorResults();

		final CleanupRFProcessor processor = new CleanupRFProcessor(evenementRFImportDAO, evenmentRFMutationDAO, null, transactionManager, 0);
		final List<Long> ids = processor.determineImportsToDelete(results, TypeImportRF.PRINCIPAL);
		assertEquals(Arrays.asList(4L, 5L), ids);
	}

	/**
	 * Ce test vérifie que l'effacement de un ou plusieurs imports appelle bien les méthodes des services et DAOs qui vont bien.
	 */
	@Test
	public void testDeleteImports() throws Exception {

		final EvenementRFImport imp1 = newImport(1L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp2 = newImport(2L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp3 = newImport(3L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp4 = newImport(4L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp5 = newImport(5L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final MockEvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO(imp1, imp2, imp3, imp4, imp5);
		final EvenementRFMutationDAO evenmentRFMutationDAO = new MockEvenementRFMutationDAO();

		final PlatformTransactionManager transactionManager = new MockTransactionManager();
		final CleanupRFProcessorResults results = new CleanupRFProcessorResults();

		final MockRegistreFoncierImportService rfService = new MockRegistreFoncierImportService();
		final CleanupRFProcessor processor = new CleanupRFProcessor(evenementRFImportDAO, evenmentRFMutationDAO, rfService, transactionManager, 0);
		processor.deleteImports(Arrays.asList(1L, 4L), results, new MockStatusManager());

		// les imports 1 et 4 doivent être effacés
		assertEquals(Arrays.asList(1L, 4L), rfService.getDeletedImportMutations());
		// il ne doit rester que les imports 2, 3 et 5
		assertEquals(Arrays.asList(2L, 3L, 5L), evenementRFImportDAO.getAll().stream().map(EvenementRFImport::getId).collect(toList()));
	}

	@Test
	public void testCleanupImport() throws Exception {

		final EvenementRFImport imp1 = newImport(1L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp2 = newImport(2L, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp3 = newImport(3L, EtatEvenementRF.FORCE, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp4 = newImport(4L, EtatEvenementRF.EN_ERREUR, TypeImportRF.PRINCIPAL);
		final EvenementRFImport imp5 = newImport(5L, EtatEvenementRF.A_TRAITER, TypeImportRF.PRINCIPAL);
		final MockEvenementRFImportDAO evenementRFImportDAO = new MockEvenementRFImportDAO(imp1, imp2, imp3, imp4, imp5);

		final EvenementRFMutation mut1 = newMutation(1L, EtatEvenementRF.TRAITE, imp1);
		final EvenementRFMutation mut2 = newMutation(2L, EtatEvenementRF.TRAITE, imp2);
		final EvenementRFMutation mut3 = newMutation(3L, EtatEvenementRF.FORCE, imp3);
		final EvenementRFMutation mut4 = newMutation(4L, EtatEvenementRF.EN_ERREUR, imp4);
		final EvenementRFMutation mut5 = newMutation(5L, EtatEvenementRF.A_TRAITER, imp5);
		final EvenementRFMutationDAO evenmentRFMutationDAO = new MockEvenementRFMutationDAO(mut1, mut2, mut3, mut4, mut5);

		final PlatformTransactionManager transactionManager = new MockTransactionManager();
		final CleanupRFProcessorResults results = new CleanupRFProcessorResults();

		final MockRegistreFoncierImportService rfService = new MockRegistreFoncierImportService();
		final CleanupRFProcessor processor = new CleanupRFProcessor(evenementRFImportDAO, evenmentRFMutationDAO, rfService, transactionManager, 3);
		processor.cleanupImports(new MockStatusManager());

		// les imports 1 et 2 doivent être effacés
		assertEquals(Arrays.asList(1L, 2L), rfService.getDeletedImportMutations());
		// il ne doit rester que les imports 3, 4 et 5
		assertEquals(Arrays.asList(3L, 4L, 5L), evenementRFImportDAO.getAll().stream().map(EvenementRFImport::getId).collect(toList()));
	}

	private static EvenementRFImport newImport(long id, EtatEvenementRF etat, TypeImportRF type) {
		final EvenementRFImport imp = new EvenementRFImport();
		imp.setId(id);
		imp.setEtat(etat);
		imp.setType(type);
		imp.setDateEvenement(RegDate.get(2000, 1, (int) id));
		return imp;
	}

	private static EvenementRFMutation newMutation(long id, EtatEvenementRF etat, EvenementRFImport parent) {
		final EvenementRFMutation mut = new EvenementRFMutation();
		mut.setId(id);
		mut.setEtat(etat);
		mut.setParentImport(parent);
		return mut;
	}
}