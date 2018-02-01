package ch.vd.unireg.registrefoncier.importcleanup;

import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImport;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeImportRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierImportService;
import ch.vd.unireg.registrefoncier.dataimport.ImportRFTestClass;

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
			Long imp6;
			Long imp7;
			Long imp8;
		}
		final Ids ids = new Ids();

		// on insère plusieurs imports et mutations à différents états
		doInNewTransaction(status -> {
			// les imports principaux
			{
				// un import qui peut être effacé
				ids.imp1 = newImportEtMutation(RegDate.get(2016, 12, 31), EtatEvenementRF.TRAITE, EtatEvenementRF.TRAITE, TypeImportRF.PRINCIPAL).getId();
				// un autre import qui peut être effacé
				ids.imp2 = newImportEtMutation(RegDate.get(2017, 1, 1), EtatEvenementRF.FORCE, EtatEvenementRF.FORCE, TypeImportRF.PRINCIPAL).getId();
				// un import qui ne peut pas être effacé parce qu'il y a des mutations en erreur
				ids.imp3 = newImportEtMutation(RegDate.get(2017, 1, 7), EtatEvenementRF.TRAITE, EtatEvenementRF.EN_ERREUR, TypeImportRF.PRINCIPAL).getId();
				// un import qui ne peut pas être effacé parce qu'il est en erreur
				ids.imp4 = newImportEtMutation(RegDate.get(2017, 1, 14), EtatEvenementRF.EN_ERREUR, EtatEvenementRF.A_TRAITER, TypeImportRF.PRINCIPAL).getId();
				// un import qui ne peut pas être effacé parce qu'il n'est pas traité (et en plus c'est le plus récent)
				ids.imp5 = newImportEtMutation(RegDate.get(2017, 1, 21), EtatEvenementRF.A_TRAITER, EtatEvenementRF.A_TRAITER, TypeImportRF.PRINCIPAL).getId();
			}
			// les imports de servitudes
			{
				// un import qui peut être effacé
				ids.imp6 = newImportEtMutation(RegDate.get(2016, 12, 31), EtatEvenementRF.TRAITE, EtatEvenementRF.FORCE, TypeImportRF.SERVITUDES).getId();
				// un import qui ne peut pas être effacé parce qu'il y a des mutations en erreur
				ids.imp7 = newImportEtMutation(RegDate.get(2017, 1, 7), EtatEvenementRF.TRAITE, EtatEvenementRF.EN_ERREUR, TypeImportRF.SERVITUDES).getId();
				// un import qui ne peut pas être effacé parce qu'il n'est pas traité (et en plus c'est le plus récent)
				ids.imp8 = newImportEtMutation(RegDate.get(2017, 1, 21), EtatEvenementRF.A_TRAITER, EtatEvenementRF.A_TRAITER, TypeImportRF.SERVITUDES).getId();
			}
			return null;
		});

		// on lance le cleanup
		final CleanupRFProcessorResults results = cleanupRFProcessor.cleanupImports(null);
		assertNotNull(results);
		assertEmpty(results.getErrors());

		// on vérifie les résultats
		final List<CleanupRFProcessorResults.Ignored> ignored = results.getIgnored();
		assertEquals(5, ignored.size());
		assertIgnored(ids.imp5, RegDate.get(2017, 1, 21), TypeImportRF.PRINCIPAL, CleanupRFProcessorResults.IgnoreReason.RETAINED, ignored.get(0));
		assertIgnored(ids.imp4, RegDate.get(2017, 1, 14), TypeImportRF.PRINCIPAL, CleanupRFProcessorResults.IgnoreReason.NOT_TREATED, ignored.get(1));
		assertIgnored(ids.imp3, RegDate.get(2017, 1, 7), TypeImportRF.PRINCIPAL, CleanupRFProcessorResults.IgnoreReason.MUTATIONS_NOT_TREATED, ignored.get(2));
		assertIgnored(ids.imp8, RegDate.get(2017, 1, 21), TypeImportRF.SERVITUDES, CleanupRFProcessorResults.IgnoreReason.RETAINED, ignored.get(3));
		assertIgnored(ids.imp7, RegDate.get(2017, 1, 7), TypeImportRF.SERVITUDES, CleanupRFProcessorResults.IgnoreReason.MUTATIONS_NOT_TREATED, ignored.get(4));

		final List<CleanupRFProcessorResults.Processed> processed = results.getProcessed();
		assertEquals(3, processed.size());
		assertProcessed(ids.imp1, RegDate.get(2016, 12, 31), TypeImportRF.PRINCIPAL, processed.get(0));
		assertProcessed(ids.imp2, RegDate.get(2017, 1, 1), TypeImportRF.PRINCIPAL, processed.get(1));
		assertProcessed(ids.imp6, RegDate.get(2016, 12, 31), TypeImportRF.SERVITUDES, processed.get(2));

		// on vérifie les données dans la base
		doInNewTransaction(status -> {
			final List<EvenementRFImport> list = evenementRFImportDAO.getAll();
			assertEquals(5, list.size());
			list.sort(Comparator.comparing(EvenementRFImport::getId));
			assertEquals(ids.imp3, list.get(0).getId());
			assertEquals(ids.imp4, list.get(1).getId());
			assertEquals(ids.imp5, list.get(2).getId());
			assertEquals(ids.imp7, list.get(3).getId());
			assertEquals(ids.imp8, list.get(4).getId());
			return null;
		});
	}

	@NotNull
	private EvenementRFImport newImportEtMutation(RegDate dateEvenement, EtatEvenementRF etatImport, EtatEvenementRF etatMutation, TypeImportRF type) {
		EvenementRFImport imp = new EvenementRFImport();
		imp.setType(type);
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

	private static void assertProcessed(long importId, RegDate dateValeur, TypeImportRF type, CleanupRFProcessorResults.Processed processed) {
		assertNotNull(processed);
		assertEquals(importId, processed.getImportId());
		assertEquals(dateValeur, processed.getDateValeur());
		assertEquals(type, processed.getType());
	}

	private static void assertIgnored(long importId, RegDate dateValeur, TypeImportRF type, CleanupRFProcessorResults.IgnoreReason reason, CleanupRFProcessorResults.Ignored ignored) {
		assertNotNull(ignored);
		assertEquals(importId, ignored.getImportId());
		assertEquals(dateValeur, ignored.getDateValeur());
		assertEquals(type, ignored.getType());
		assertEquals(reason, ignored.getReason());
	}
}
