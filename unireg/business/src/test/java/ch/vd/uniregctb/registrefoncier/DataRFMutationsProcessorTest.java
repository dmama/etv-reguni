package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.processor.AyantDroitRFProcessor;
import ch.vd.uniregctb.registrefoncier.processor.BatimentRFProcessor;
import ch.vd.uniregctb.registrefoncier.processor.DroitRFProcessor;
import ch.vd.uniregctb.registrefoncier.processor.MutationRFProcessor;
import ch.vd.uniregctb.registrefoncier.processor.SurfaceAuSolRFProcessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class DataRFMutationsProcessorTest extends BusinessTest {

	private DataRFMutationsProcessor processor;
	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;
	private AyantDroitRFProcessor ayantDroitRFProcessor;
	private DroitRFProcessor droitRFProcessor;
	private SurfaceAuSolRFProcessor surfaceAuSolRFProcessor;
	private BatimentRFProcessor batimentRFProcessor;
	private MutationRFProcessor communeRFProcessor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		ayantDroitRFProcessor = getBean(AyantDroitRFProcessor.class, "ayantDroitRFProcessor");
		droitRFProcessor = getBean(DroitRFProcessor.class, "droitRFProcessor");
		surfaceAuSolRFProcessor = getBean(SurfaceAuSolRFProcessor.class, "surfaceAuSolRFProcessor");
		batimentRFProcessor = getBean(BatimentRFProcessor.class, "batimentRFProcessor");
		communeRFProcessor = getBean(MutationRFProcessor.class, "communeRFProcessor");
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
		processor = new DataRFMutationsProcessor(evenementRFMutationDAO, communeRFProcessor, immeubleRFProcessor, ayantDroitRFProcessor, droitRFProcessor, surfaceAuSolRFProcessor, batimentRFProcessor, transactionManager);
		processor.processImport(importId, 2, null);

		// on s'assure que les mutations sont toutes passées dans l'état TRAITE et qu'il n'y a pas d'erreur
		doInNewTransaction(status -> {
			final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
			assertEquals(3, mutations.size());

			final EvenementRFMutation mut0 = mutations.get(0);
			assertEquals(EtatEvenementRF.TRAITE, mut0.getEtat());
			assertNull(mut0.getErrorMessage());
			assertNull(mut0.getCallstack());

			final EvenementRFMutation mut1 = mutations.get(1);
			assertEquals(EtatEvenementRF.TRAITE, mut1.getEtat());
			assertNull(mut1.getErrorMessage());
			assertNull(mut1.getCallstack());

			final EvenementRFMutation mut2 = mutations.get(2);
			assertEquals(EtatEvenementRF.TRAITE, mut2.getEtat());
			assertNull(mut2.getErrorMessage());
			assertNull(mut2.getCallstack());
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
		processor = new DataRFMutationsProcessor(evenementRFMutationDAO, communeRFProcessor, immeubleRFProcessor, ayantDroitRFProcessor, droitRFProcessor, surfaceAuSolRFProcessor, batimentRFProcessor, transactionManager);
		processor.processImport(importId, 2, null);

		// on s'assure que les mutations sont toutes passées dans l'état EN_ERREUR et que le message d'erreur est renseigné
		doInNewTransaction(status -> {
			final List<EvenementRFMutation> mutations = evenementRFMutationDAO.getAll();
			assertEquals(3, mutations.size());

			final EvenementRFMutation mut0 = mutations.get(0);
			assertEquals(EtatEvenementRF.EN_ERREUR, mut0.getEtat());
			assertEquals("Exception de test", mut0.getErrorMessage());
			assertContains("java.lang.RuntimeException: Exception de test", mut0.getCallstack());

			final EvenementRFMutation mut1 = mutations.get(1);
			assertEquals(EtatEvenementRF.EN_ERREUR, mut1.getEtat());
			assertEquals("Exception de test", mut1.getErrorMessage());
			assertContains("java.lang.RuntimeException: Exception de test", mut1.getCallstack());

			final EvenementRFMutation mut2 = mutations.get(2);
			assertEquals(EtatEvenementRF.EN_ERREUR, mut2.getEtat());
			assertEquals("Exception de test", mut2.getErrorMessage());
			assertContains("java.lang.RuntimeException: Exception de test", mut2.getCallstack());
			return null;
		});
	}

	/**
	 * Ce test vérifie que le processeur lève une exception s'il existe des mutations non-traitées (A_TRAITER ou EN_ERREUR) d'un import précédent.
	 */
	@Test
	public void testProcessErreurMutationsPrecedentesNonTraitees() throws Exception {

		class Ids {
			long precedent;
			long suivant;
		}
		final Ids ids = new Ids();

		// on insère quelques mutations
		doInNewTransaction(status -> {

			final EvenementRFImport importPrecedent = addParentImport(EtatEvenementRF.TRAITE, RegDate.get(2010, 1, 1));
			addMutation(importPrecedent, EtatEvenementRF.EN_ERREUR, TypeEntiteRF.IMMEUBLE, TypeMutationRF.CREATION, "389383", null);
			addMutation(importPrecedent, EtatEvenementRF.EN_ERREUR, TypeEntiteRF.IMMEUBLE, TypeMutationRF.CREATION, "482922", null);
			addMutation(importPrecedent, EtatEvenementRF.TRAITE, TypeEntiteRF.IMMEUBLE, TypeMutationRF.CREATION, "492308", null);
			ids.precedent = importPrecedent.getId();

			final EvenementRFImport importSuivant = addParentImport(EtatEvenementRF.A_TRAITER, RegDate.get(2010, 3, 1));
			addMutation(importSuivant, EtatEvenementRF.A_TRAITER, TypeEntiteRF.IMMEUBLE, TypeMutationRF.CREATION, "389383", null);
			addMutation(importSuivant, EtatEvenementRF.A_TRAITER, TypeEntiteRF.IMMEUBLE, TypeMutationRF.CREATION, "482922", null);
			addMutation(importSuivant, EtatEvenementRF.A_TRAITER, TypeEntiteRF.IMMEUBLE, TypeMutationRF.CREATION, "492308", null);
			ids.suivant = importSuivant.getId();

			return null;
		});

		// un processor de mutations qui ne fait rien
		final MutationRFProcessor immeubleRFProcessor = mutation -> {
		};

		// on devrait avoir une exception parce que les mutations de l'import précédent ne sont pas toutes traitées
		processor = new DataRFMutationsProcessor(evenementRFMutationDAO, communeRFProcessor, immeubleRFProcessor, ayantDroitRFProcessor, droitRFProcessor, surfaceAuSolRFProcessor, batimentRFProcessor, transactionManager);
		try {
			processor.processImport(ids.suivant, 2, null);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("Les mutations de l'import RF avec l'id = [" + ids.suivant + "] ne peuvent être traitées car les mutations de l'import RF avec l'id = [" + ids.precedent + "] n'ont pas été traitées.", e.getMessage());
		}
	}

	private Long addImportAndThreeMutations() throws Exception {
		return doInNewTransaction(status -> {
			EvenementRFImport parentImport = addParentImport(EtatEvenementRF.A_TRAITER, RegDate.get(2000, 1, 1));

			addMutation(parentImport, EtatEvenementRF.A_TRAITER, TypeEntiteRF.IMMEUBLE, TypeMutationRF.CREATION, "389383", null);
			addMutation(parentImport, EtatEvenementRF.A_TRAITER, TypeEntiteRF.IMMEUBLE, TypeMutationRF.CREATION, "482922", null);
			addMutation(parentImport, EtatEvenementRF.A_TRAITER, TypeEntiteRF.IMMEUBLE, TypeMutationRF.CREATION, "492308", null);

			return parentImport.getId();
		});
	}

	private EvenementRFImport addParentImport(EtatEvenementRF etat, RegDate dateEvenement) {
		final EvenementRFImport parentImport = new EvenementRFImport();
		parentImport.setEtat(etat);
		parentImport.setDateEvenement(dateEvenement);
		return evenementRFImportDAO.save(parentImport);
	}

	private EvenementRFMutation addMutation(EvenementRFImport parentImport, EtatEvenementRF etat, TypeEntiteRF typeEntite, TypeMutationRF typeMutation, @Nullable String idRF, String content) {
		final EvenementRFMutation mutation = new EvenementRFMutation();
		mutation.setEtat(etat);
		mutation.setTypeEntite(typeEntite);
		mutation.setTypeMutation(typeMutation);
		mutation.setIdRF(idRF);
		mutation.setParentImport(parentImport);
		mutation.setXmlContent(content);
		return evenementRFMutationDAO.save(mutation);
	}
}