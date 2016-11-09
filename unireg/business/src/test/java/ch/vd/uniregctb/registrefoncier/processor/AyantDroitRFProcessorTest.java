package ch.vd.uniregctb.registrefoncier.processor;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation.TypeEntite;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation.TypeMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AyantDroitRFProcessorTest extends MutationRFProcessorTestCase {
	private EvenementRFMutationDAO evenementRFMutationDAO;

	private AyantDroitRFDAO ayantDroitRFDAO;
	private AyantDroitRFProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		this.ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		final XmlHelperRF xmlHelperRF = getBean(XmlHelperRF.class, "xmlHelperRF");

		this.processor = new AyantDroitRFProcessor(ayantDroitRFDAO, xmlHelperRF);
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
	 * Ce test vérifie que le processing d'une mutation de création crée bien un nouvel immeuble dans la DB
	 */
	@Test
	public void testProcessMutationCreation() throws Exception {

		// précondition : la base est vide
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				assertEquals(0, ayantDroitRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_ayantdroit_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntite.AYANT_DROIT, TypeMutation.CREATION, null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation);
			}
		});

		// postcondition : la mutation est traitée et l'ayant-droit est créé en base
		assertOnePersonnePhysiqueInDB("3893728273382823", 3727, "Nom", "Prénom", RegDate.get(1956, 1, 23), 827288022);
	}

	/*
	 * Ce test vérifie que le processing d'une mutation de modification d'un ayant-droit fonctionne bien.
	 */
	@Test
	public void testProcessMutationModification() throws Exception {

		// précondition : il y a déjà un ayant-droit dans la base avec un prénom différent de celui de la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
				pp.setIdRF("3893728273382823");
				pp.setNoRF(3727);
				pp.setNom("Nom");
				pp.setPrenom("Autre prénom");
				pp.setDateNaissance(RegDate.get(1956,1,23));
				pp.setNoContribuable(827288022L);
				ayantDroitRFDAO.save(pp);

				assertEquals(1, ayantDroitRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_ayantdroit_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntite.AYANT_DROIT, TypeMutation.MODIFICATION, null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation);
			}
		});

		// postcondition : la mutation est traitée, l'ayant-droit est mis-à-jour en base
		assertOnePersonnePhysiqueInDB("3893728273382823", 3727, "Nom", "Prénom", RegDate.get(1956, 1, 23), 827288022);
	}

	private void assertOnePersonnePhysiqueInDB(final String idRF, final int noRF, final String nom, final String prenom, final RegDate dateNaissance, final long noCtb) throws Exception {
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<AyantDroitRF> ayantsDroits = ayantDroitRFDAO.getAll();
				assertEquals(1, ayantsDroits.size());

				final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) ayantsDroits.get(0);
				assertNotNull(pp);
				assertEquals(idRF, pp.getIdRF());
				assertEquals(noRF, pp.getNoRF());
				assertEquals(nom, pp.getNom());
				assertEquals(prenom, pp.getPrenom());
				assertEquals(dateNaissance, pp.getDateNaissance());
				assertEquals(Long.valueOf(noCtb), pp.getNoContribuable());
			}
		});
	}
}