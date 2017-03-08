package ch.vd.uniregctb.registrefoncier.importcleanup;

import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeImportRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierImportService;
import ch.vd.uniregctb.registrefoncier.dataimport.ImportRFTestClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CleanupRFProcessorItTest extends ImportRFTestClass {

	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private CleanupRFProcessor cleanupRFProcessor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		final RegistreFoncierImportService registreFoncierService = getBean(RegistreFoncierImportService.class, "serviceImportRF");
		cleanupRFProcessor = new CleanupRFProcessor(evenementRFImportDAO, evenementRFMutationDAO, registreFoncierService, transactionManager, 1);
	}

	/**
	 * Ce vérifie que le job de cleanup fonctionne avec des données réelles.
	 */
	@Test
	public void testCleanup() throws Exception {

		class Ids {
			Long imp1;
			Long imp2;
			Long imp3;
			Long imp4;
			Long imp5;
		}
		final Ids ids = new Ids();

		// on insère plusieurs imports et mutations à différents états
		doInNewTransaction(status -> {
			// un import qui peut être effacé
			ids.imp1 = newImportEtMutation(RegDate.get(2016, 12, 31), EtatEvenementRF.TRAITE, EtatEvenementRF.TRAITE).getId();
			// un autre import qui peut être effacé
			ids.imp2 = newImportEtMutation(RegDate.get(2017, 1, 1), EtatEvenementRF.FORCE, EtatEvenementRF.FORCE).getId();
			// un import qui ne peut pas être effacé parce qu'il y a des mutations en erreur
			ids.imp3 = newImportEtMutation(RegDate.get(2017, 1, 7), EtatEvenementRF.TRAITE, EtatEvenementRF.EN_ERREUR).getId();
			// un import qui ne peut pas être effacé parce qu'il est en erreur
			ids.imp4 = newImportEtMutation(RegDate.get(2017, 1, 14), EtatEvenementRF.EN_ERREUR, EtatEvenementRF.A_TRAITER).getId();
			// un import qui ne peut pas être effacé parce qu'il n'est pas traité (et en plus c'est le plus récent)
			ids.imp5 = newImportEtMutation(RegDate.get(2017, 1, 21), EtatEvenementRF.A_TRAITER, EtatEvenementRF.A_TRAITER).getId();
			return null;
		});

		// on lance le cleanup
		final CleanupRFProcessorResults results = cleanupRFProcessor.cleanupImports(null);
		assertNotNull(results);
		assertEmpty(results.getErrors());

		// on vérifie les résultats
		final List<CleanupRFProcessorResults.Ignored> ignored = results.getIgnored();
		assertEquals(3, ignored.size());
		assertIgnored(ids.imp5, CleanupRFProcessorResults.IgnoreReason.RETAINED, ignored.get(0));
		assertIgnored(ids.imp4, CleanupRFProcessorResults.IgnoreReason.NOT_TREATED, ignored.get(1));
		assertIgnored(ids.imp3, CleanupRFProcessorResults.IgnoreReason.MUTATIONS_NOT_TREATED, ignored.get(2));

		final List<CleanupRFProcessorResults.Processed> processed = results.getProcessed();
		assertEquals(2, processed.size());
		assertProcessed(ids.imp1, RegDate.get(2016, 12, 31), processed.get(0));
		assertProcessed(ids.imp2, RegDate.get(2017, 1, 1), processed.get(1));

		// on vérifie les données dans la base
		doInNewTransaction(status -> {
			final List<EvenementRFImport> list = evenementRFImportDAO.getAll();
			assertEquals(3, list.size());
			list.sort(Comparator.comparing(EvenementRFImport::getId));
			assertEquals(ids.imp3, list.get(0).getId());
			assertEquals(ids.imp4, list.get(1).getId());
			assertEquals(ids.imp5, list.get(2).getId());
			return null;
		});

	}

	@NotNull
	private EvenementRFImport newImportEtMutation(RegDate dateEvenement, EtatEvenementRF etatImport, EtatEvenementRF etatMutation) {
		EvenementRFImport imp = new EvenementRFImport();
		imp.setType(TypeImportRF.PRINCIPAL);
		imp.setDateEvenement(dateEvenement);
		imp.setEtat(etatImport);
		imp.setFileUrl("http://turlututu");
		imp = evenementRFImportDAO.save(imp);

		final EvenementRFMutation mut = new EvenementRFMutation();
		mut.setParentImport(imp);
		mut.setEtat(etatMutation);
		mut.setTypeEntite(TypeEntiteRF.IMMEUBLE);
		mut.setTypeMutation(TypeMutationRF.CREATION);
		mut.setXmlContent("");
		evenementRFMutationDAO.save(mut);

		return imp;
	}

	private static void assertProcessed(long importId, RegDate dateValeur, CleanupRFProcessorResults.Processed processed) {
		assertNotNull(processed);
		assertEquals(importId, processed.getImportId());
		assertEquals(dateValeur, processed.getDateValeur());
	}

	private static void assertIgnored(long importId, CleanupRFProcessorResults.IgnoreReason reason, CleanupRFProcessorResults.Ignored ignored) {
		assertNotNull(ignored);
		assertEquals(importId, ignored.getImportId());
		assertEquals(reason, ignored.getReason());
	}
}
