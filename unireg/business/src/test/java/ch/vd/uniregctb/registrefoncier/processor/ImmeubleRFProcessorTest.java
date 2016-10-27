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
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;
import ch.vd.uniregctb.transaction.MockBlob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ImmeubleRFProcessorTest extends BusinessTest {

	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;

	private ImmeubleRFDAO immeubleRFDAO;
	private ImmeubleRFProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		this.evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");

		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		final XmlHelperRF xmlHelperRF = getBean(XmlHelperRF.class, "xmlHelperRF");

		this.processor = new ImmeubleRFProcessor(immeubleRFDAO, xmlHelperRF);
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
				assertEquals(0, immeubleRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = doInNewTransaction(new ch.vd.registre.base.tx.TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport parentImport = new EvenementRFImport();
				parentImport.setEtat(EtatEvenementRF.A_TRAITER);
				parentImport.setDateEvenement(RegDate.get(2016, 10, 1));
				parentImport = evenementRFImportDAO.save(parentImport);

				final EvenementRFMutation mutation = new EvenementRFMutation();
				mutation.setTypeEntite(EvenementRFMutation.TypeEntite.IMMEUBLE);
				mutation.setTypeMutation(EvenementRFMutation.TypeMutation.CREATION);
				mutation.setEtat(EtatEvenementRF.A_TRAITER);
				mutation.setParentImport(parentImport);
				mutation.setXmlContent(new MockBlob(xml.getBytes("UTF-8")));

				return evenementRFMutationDAO.save(mutation).getId();
			}
		});

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation);
			}
		});

		// postcondition : la mutation est traitée et l'immeuble est créé en base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {


				final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
				assertEquals(1, immeubles.size());

				final BienFondRF immeuble0 = (BienFondRF) immeubles.get(0);
				assertEquals("_1f109152381026b501381028a73d1852", immeuble0.getIdRF());
				assertEquals("CH938391457759", immeuble0.getEgrid());
				assertFalse(immeuble0.isCfa());

				final Set<SituationRF> situations = immeuble0.getSituations();
				assertEquals(1, situations.size());

				final SituationRF situation0 = situations.iterator().next();
				assertEquals(RegDate.get(2016, 10, 1), situation0.getDateDebut());
				assertNull(situation0.getDateFin());
				assertEquals(294, situation0.getNoRfCommune());
				assertEquals(5089, situation0.getNoParcelle());
				assertNull(situation0.getIndex1());
				assertNull(situation0.getIndex2());
				assertNull(situation0.getIndex3());

				final Set<EstimationRF> estimations = immeuble0.getEstimations();
				assertEquals(1, estimations.size());

				final EstimationRF estimation0 = estimations.iterator().next();
				assertEquals(RegDate.get(2016, 10, 1), estimation0.getDateDebut());
				assertNull(estimation0.getDateFin());
				assertEquals(260000, estimation0.getMontant());
				assertEquals("RG93", estimation0.getReference());
				assertNull(estimation0.getDateEstimation());
				assertFalse(estimation0.isEnRevision());
			}
		});
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de modification met bien à jour l'immeuble existant dans la DB
	 */
	@Test
	public void testProcessMutationModification() throws Exception {

		// précondition : il y a déjà un immeuble dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final BienFondRF bienFond = new BienFondRF();
				bienFond.setIdRF("_1f109152381026b501381028a73d1852");
				bienFond.setEgrid("CH938391457759");
				bienFond.setCfa(false);

				final SituationRF situation = new SituationRF();
				situation.setDateDebut(RegDate.get(1988, 1, 1));
				situation.setNoRfCommune(294);
				situation.setNoParcelle(5089);
				bienFond.addSituation(situation);

				final EstimationRF estimation = new EstimationRF();
				estimation.setDateDebut(RegDate.get(1988, 1, 1));
				estimation.setMontant(240000);
				estimation.setReference("RG88");
				estimation.setEnRevision(false);
				bienFond.addEstimation(estimation);

				immeubleRFDAO.save(bienFond);
				assertEquals(1, immeubleRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = doInNewTransaction(new ch.vd.registre.base.tx.TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport parentImport = new EvenementRFImport();
				parentImport.setEtat(EtatEvenementRF.A_TRAITER);
				parentImport.setDateEvenement(RegDate.get(2016, 10, 1));
				parentImport = evenementRFImportDAO.save(parentImport);

				final EvenementRFMutation mutation = new EvenementRFMutation();
				mutation.setTypeEntite(EvenementRFMutation.TypeEntite.IMMEUBLE);
				mutation.setTypeMutation(EvenementRFMutation.TypeMutation.MODIFICATION);
				mutation.setEtat(EtatEvenementRF.A_TRAITER);
				mutation.setParentImport(parentImport);
				mutation.setXmlContent(new MockBlob(xml.getBytes("UTF-8")));

				return evenementRFMutationDAO.save(mutation).getId();
			}
		});

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation);
			}
		});

		// postcondition : la mutation est traitée et l'immeuble est créé en base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
				assertEquals(1, immeubles.size());

				final ImmeubleRF immeuble0 = immeubles.get(0);
				assertEquals("_1f109152381026b501381028a73d1852", immeuble0.getIdRF());
				assertEquals("CH938391457759", immeuble0.getEgrid());

				// la situation n'a pas changé
				final Set<SituationRF> situations = immeuble0.getSituations();
				assertEquals(1, situations.size());

				final SituationRF situation0 = situations.iterator().next();
				assertEquals(RegDate.get(1988, 1, 1), situation0.getDateDebut());
				assertNull(situation0.getDateFin());
				assertEquals(294, situation0.getNoRfCommune());
				assertEquals(5089, situation0.getNoParcelle());
				assertNull(situation0.getIndex1());
				assertNull(situation0.getIndex2());
				assertNull(situation0.getIndex3());

				// par contre, il y a une nouvelle estimation
				final Set<EstimationRF> estimations = immeuble0.getEstimations();
				assertEquals(2, estimations.size());

				final List<EstimationRF> estimationList = new ArrayList<EstimationRF>(estimations);
				Collections.sort(estimationList, new DateRangeComparator<>());

				// la première estimation doit être fermée la veille de la date d'import
				final EstimationRF estimation0 = estimationList.get(0);
				assertEquals(RegDate.get(1988, 1, 1), estimation0.getDateDebut());
				assertEquals(RegDate.get(2016, 9, 30), estimation0.getDateFin());
				assertEquals(240000, estimation0.getMontant());
				assertEquals("RG88", estimation0.getReference());
				assertNull(estimation0.getDateEstimation());
				assertFalse(estimation0.isEnRevision());

				// la seconde estimation doit commencer à la date de l'import
				final EstimationRF estimation1 = estimationList.get(1);
				assertEquals(RegDate.get(2016, 10, 1), estimation1.getDateDebut());
				assertNull(estimation1.getDateFin());
				assertEquals(260000, estimation1.getMontant());
				assertEquals("RG93", estimation1.getReference());
				assertNull(estimation1.getDateEstimation());
				assertFalse(estimation1.isEnRevision());
			}
		});
	}
}