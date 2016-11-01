package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.registrefoncier.processor.MutationRFProcessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DataRFMutationsProcessorTest extends BusinessTest {

	private DataRFMutationsProcessor processor;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
	}

	/**
	 * Ce test vérifie que le processor passe bien les mutations à TRAITE lorsque le traitement se déroule sans erreur.
	 */
	@Test
	public void testProcessCasNominal() throws Exception {

		// on insère quelques mutations
		final Long importId = addImportAndThreeMutations();

		// un processor de mutations qui ne fait rien de spécial (ce n'est pas lui que l'on veut tester)
		final MutationRFProcessor immeubleRFProcessor = mutation -> {
			// on ne fait rien
		};

		// on déclenche le traitement des mutations
		processor = new DataRFMutationsProcessor(evenementRFMutationDAO, immeubleRFProcessor, transactionManager);
		processor.processImport(importId);

		// on s'assure que les mutations sont toutes passées dans l'état TRAITE et qu'il n'y a pas d'erreur
		doInNewTransaction(status -> {
			final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
			assertEquals(3, mutations.size());

			final EvenementRFMutation mut0 = mutations.get(0);
			assertEquals(EtatEvenementRF.TRAITE, mut0.getEtat());
			assertNull(mut0.getErrorMessage());

			final EvenementRFMutation mut1 = mutations.get(1);
			assertEquals(EtatEvenementRF.TRAITE, mut1.getEtat());
			assertNull(mut1.getErrorMessage());

			final EvenementRFMutation mut2 = mutations.get(2);
			assertEquals(EtatEvenementRF.TRAITE, mut2.getEtat());
			assertNull(mut2.getErrorMessage());
			return null;
		});
	}

	/**
	 * Ce test vérifie que le processor passe bien les mutations à EN_ERREUR lorsque le traitement lève des exceptions.
	 */
	@Test
	public void testProcessCasAvecExceptions() throws Exception {

		// on insère quelques mutations
		final Long importId = addImportAndThreeMutations();

		// un processor de mutations qui lève des exceptions
		final MutationRFProcessor immeubleRFProcessor = mutation -> {
			throw new RuntimeException("Exception de test");
		};

		// on déclenche le traitement des mutations
		processor = new DataRFMutationsProcessor(evenementRFMutationDAO, immeubleRFProcessor, transactionManager);
		processor.processImport(importId);

		// on s'assure que les mutations sont toutes passées dans l'état EN_ERREUR et que le message d'erreur est renseigné
		doInNewTransaction(status -> {
			final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
			assertEquals(3, mutations.size());

			final EvenementRFMutation mut0 = mutations.get(0);
			assertEquals(EtatEvenementRF.EN_ERREUR, mut0.getEtat());
			assertContains("java.lang.RuntimeException: Exception de test", mut0.getErrorMessage());

			final EvenementRFMutation mut1 = mutations.get(1);
			assertEquals(EtatEvenementRF.EN_ERREUR, mut1.getEtat());
			assertContains("java.lang.RuntimeException: Exception de test", mut1.getErrorMessage());

			final EvenementRFMutation mut2 = mutations.get(2);
			assertEquals(EtatEvenementRF.EN_ERREUR, mut2.getEtat());
			assertContains("java.lang.RuntimeException: Exception de test", mut2.getErrorMessage());
			return null;
		});
	}

	private Long addImportAndThreeMutations() throws Exception {
		return doInNewTransaction(status -> {
			EvenementRFImport parentImport = addParentImport(EtatEvenementRF.A_TRAITER, RegDate.get(2000, 1, 1));

			addMutation(parentImport, EtatEvenementRF.A_TRAITER, EvenementRFMutation.TypeEntite.IMMEUBLE, EvenementRFMutation.TypeMutation.CREATION, null);
			addMutation(parentImport, EtatEvenementRF.A_TRAITER, EvenementRFMutation.TypeEntite.IMMEUBLE, EvenementRFMutation.TypeMutation.CREATION, null);
			addMutation(parentImport, EtatEvenementRF.A_TRAITER, EvenementRFMutation.TypeEntite.IMMEUBLE, EvenementRFMutation.TypeMutation.CREATION, null);

			return parentImport.getId();
		});
	}

	private EvenementRFImport addParentImport(EtatEvenementRF etat, RegDate dateEvenement) {
		final EvenementRFImport parentImport = new EvenementRFImport();
		parentImport.setEtat(etat);
		parentImport.setDateEvenement(dateEvenement);
		return evenementRFImportDAO.save(parentImport);
	}

	private EvenementRFMutation addMutation(EvenementRFImport parentImport, EtatEvenementRF etat, EvenementRFMutation.TypeEntite typeEntite, EvenementRFMutation.TypeMutation typeMutation, String content) {
		final EvenementRFMutation mutation = new EvenementRFMutation();
		mutation.setEtat(etat);
		mutation.setTypeEntite(typeEntite);
		mutation.setTypeMutation(typeMutation);
		mutation.setParentImport(parentImport);
		mutation.setXmlContent(content);
		return evenementRFMutationDAO.save(mutation);
	}
}