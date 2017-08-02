package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.io.File;
import java.util.ArrayList;
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
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.QuotePartRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceTotaleRF;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.processor.MutationRFProcessorTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("Duplicates")
public class ImmeubleRFProcessorTest extends MutationRFProcessorTestCase {

	private EvenementRFMutationDAO evenementRFMutationDAO;

	private CommuneRFDAO communeRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private ImmeubleRFProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		final XmlHelperRF xmlHelperRF = getBean(XmlHelperRF.class, "xmlHelperRF");

		this.processor = new ImmeubleRFProcessor(communeRFDAO, immeubleRFDAO, xmlHelperRF);
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

		// on insère la commune qui correspond à l'immeuble
		insertCommune(294, "Oron", 5555);

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.CREATION, "1f109152381026b501381028a74018e1", null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
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
				assertEquals(294, situation0.getCommune().getNoRf());
				assertEquals(5089, situation0.getNoParcelle());
				assertNull(situation0.getIndex1());
				assertNull(situation0.getIndex2());
				assertNull(situation0.getIndex3());

				final Set<EstimationRF> estimations = immeuble0.getEstimations();
				assertEquals(1, estimations.size());

				final EstimationRF estimation0 = estimations.iterator().next();
				assertEquals(RegDate.get(2016, 10, 1), estimation0.getDateDebut());
				assertNull(estimation0.getDateFin());
				assertEquals(Long.valueOf(260000), estimation0.getMontant());
				assertEquals("RG93", estimation0.getReference());
				assertEquals(Integer.valueOf(1993), estimation0.getAnneeReference());
				assertNull(estimation0.getDateInscription());
				assertEquals(RegDate.get(1993, 1, 1), estimation0.getDateDebutMetier());
				assertNull(estimation0.getDateFinMetier());
				assertFalse(estimation0.isEnRevision());

				final Set<SurfaceTotaleRF> surfacesTotales = immeuble0.getSurfacesTotales();
				assertEquals(1, surfacesTotales.size());

				final SurfaceTotaleRF surface0 = surfacesTotales.iterator().next();
				assertEquals(RegDate.get(2016, 10, 1), surface0.getDateDebut());
				assertNull(surface0.getDateFin());
				assertEquals(707, surface0.getSurface());
			}
		});
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de création fonctionne bien même si l'immeuble ne possède pas d'estimation fiscale.
	 */
	@Test
	public void testProcessMutationCreationImmeubleSansEstimationFiscale() throws Exception {

		// précondition : la base est vide
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				assertEquals(0, immeubleRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf_sans_estimation_fiscale.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la commune qui correspond à l'immeuble
		insertCommune(13, "Roche (VD)", 5555);

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.CREATION, "_8af80e62567f816f01571d91f3e56a38", null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée et l'immeuble est créé en base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {


				final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
				assertEquals(1, immeubles.size());

				final ProprieteParEtageRF immeuble0 = (ProprieteParEtageRF) immeubles.get(0);
				assertEquals("_8af80e62567f816f01571d91f3e56a38", immeuble0.getIdRF());
				assertEquals("CH776584246539", immeuble0.getEgrid());

				final Set<SituationRF> situations = immeuble0.getSituations();
				assertEquals(1, situations.size());

				final SituationRF situation0 = situations.iterator().next();
				assertEquals(RegDate.get(2016, 10, 1), situation0.getDateDebut());
				assertNull(situation0.getDateFin());
				assertEquals(13, situation0.getCommune().getNoRf());
				assertEquals(917, situation0.getNoParcelle());
				assertEquals(Integer.valueOf(106), situation0.getIndex1());
				assertNull(situation0.getIndex2());
				assertNull(situation0.getIndex3());

				final Set<EstimationRF> estimations = immeuble0.getEstimations();
				assertEquals(0, estimations.size());
			}
		});
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de modification l'estimation fiscale met bien à jour l'immeuble existant dans la DB
	 */
	@Test
	public void testProcessMutationModificationEstimation() throws Exception {

		// précondition : il y a déjà un immeuble dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				CommuneRF commune = new CommuneRF();
				commune.setNoRf(294);
				commune.setNomRf("Pétahouchnok");
				commune.setNoOfs(66666);
				commune = communeRFDAO.save(commune);

				final BienFondRF bienFond = new BienFondRF();
				bienFond.setIdRF("_1f109152381026b501381028a73d1852");
				bienFond.setEgrid("CH938391457759");
				bienFond.setCfa(false);

				final SituationRF situation = new SituationRF();
				situation.setDateDebut(RegDate.get(1988, 1, 1));
				situation.setCommune(commune);
				situation.setNoParcelle(5089);
				bienFond.addSituation(situation);

				final EstimationRF estimation = new EstimationRF();
				estimation.setDateDebut(RegDate.get(1988, 1, 1));
				estimation.setMontant(240000L);
				estimation.setReference("RG88");
				estimation.setAnneeReference(1988);
				estimation.setDateDebutMetier(RegDate.get(1988, 1, 1));
				estimation.setEnRevision(false);
				bienFond.addEstimation(estimation);

				final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
				surfaceTotale.setDateDebut(RegDate.get(1988, 1, 1));
				surfaceTotale.setSurface(707);
				bienFond.addSurfaceTotale(surfaceTotale);

				immeubleRFDAO.save(bienFond);
				assertEquals(1, immeubleRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
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
				assertEquals(294, situation0.getCommune().getNoRf());
				assertEquals(5089, situation0.getNoParcelle());
				assertNull(situation0.getIndex1());
				assertNull(situation0.getIndex2());
				assertNull(situation0.getIndex3());

				// par contre, il y a une nouvelle estimation
				final Set<EstimationRF> estimations = immeuble0.getEstimations();
				assertEquals(2, estimations.size());

				final List<EstimationRF> estimationList = new ArrayList<>(estimations);
				estimationList.sort(new DateRangeComparator<>());

				// la première estimation doit être fermée la veille de la date d'import
				final EstimationRF estimation0 = estimationList.get(0);
				assertEquals(RegDate.get(1988, 1, 1), estimation0.getDateDebut());
				assertEquals(RegDate.get(2016, 9, 30), estimation0.getDateFin());
				assertEquals(Long.valueOf(240000), estimation0.getMontant());
				assertEquals("RG88", estimation0.getReference());
				assertEquals(Integer.valueOf(1988), estimation0.getAnneeReference());
				assertNull(estimation0.getDateInscription());
				assertEquals(RegDate.get(1988, 1, 1), estimation0.getDateDebutMetier());
				assertEquals(RegDate.get(1992, 12, 31), estimation0.getDateFinMetier());
				assertFalse(estimation0.isEnRevision());

				// la seconde estimation doit commencer à la date de l'import
				final EstimationRF estimation1 = estimationList.get(1);
				assertEquals(RegDate.get(2016, 10, 1), estimation1.getDateDebut());
				assertNull(estimation1.getDateFin());
				assertEquals(Long.valueOf(260000), estimation1.getMontant());
				assertEquals("RG93", estimation1.getReference());
				assertEquals(Integer.valueOf(1993), estimation1.getAnneeReference());
				assertNull(estimation1.getDateInscription());
				assertEquals(RegDate.get(1993, 1, 1), estimation1.getDateDebutMetier());
				assertNull(estimation1.getDateFinMetier());
				assertFalse(estimation1.isEnRevision());

				// la surface totale n'a pas changé
				final Set<SurfaceTotaleRF> surfacesTotales = immeuble0.getSurfacesTotales();
				assertEquals(1, surfacesTotales.size());

				final SurfaceTotaleRF surface0 = surfacesTotales.iterator().next();
				assertEquals(RegDate.get(1988, 1, 1), surface0.getDateDebut());
				assertNull(surface0.getDateFin());
				assertEquals(707, surface0.getSurface());
			}
		});
	}

	/**
	 * [SIFISC-22995] Ce test vérifie que le processing d'une mutation de modification sur l'estimation fiscale corrige bien l'estimation fiscale existante s'il s'agit du passage en révision de l'estimation (il ne doit pas y avoir de création d'une
	 * nouvelle estimation fiscale).
	 */
	@Test
	public void testProcessMutationModificationEstimationPassageEnRevision() throws Exception {

		// précondition : il y a déjà un immeuble dans la base avec une estimation fiscale pas en révision
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				CommuneRF commune = new CommuneRF();
				commune.setNoRf(294);
				commune.setNomRf("Pétahouchnok");
				commune.setNoOfs(66666);
				commune = communeRFDAO.save(commune);

				final BienFondRF bienFond = new BienFondRF();
				bienFond.setIdRF("_1f109152381026b501381028a73d1852");
				bienFond.setEgrid("CH938391457759");
				bienFond.setCfa(false);

				final SituationRF situation = new SituationRF();
				situation.setDateDebut(RegDate.get(1988, 1, 1));
				situation.setCommune(commune);
				situation.setNoParcelle(5089);
				bienFond.addSituation(situation);

				final EstimationRF estimation = new EstimationRF();
				estimation.setDateDebut(RegDate.get(1988, 1, 1));
				estimation.setMontant(240000L);
				estimation.setReference("RG88");
				estimation.setAnneeReference(1988);
				estimation.setDateDebutMetier(RegDate.get(1988, 1, 1));
				estimation.setEnRevision(false);
				bienFond.addEstimation(estimation);

				final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
				surfaceTotale.setDateDebut(RegDate.get(1988, 1, 1));
				surfaceTotale.setSurface(707);
				bienFond.addSurfaceTotale(surfaceTotale);

				immeubleRFDAO.save(bienFond);
				assertEquals(1, immeubleRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf_estimation_en_revision.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
				assertEquals(1, immeubles.size());

				final ImmeubleRF immeuble0 = immeubles.get(0);
				assertEquals("_1f109152381026b501381028a73d1852", immeuble0.getIdRF());
				assertEquals("CH938391457759", immeuble0.getEgrid());

				// l'estimation fiscale existante a été corrigée (= ajout du flag en révision)
				final Set<EstimationRF> estimations = immeuble0.getEstimations();
				assertEquals(1, estimations.size());

				// la première estimation doit maintenant avoir le flag en révision
				final EstimationRF estimation0 = estimations.iterator().next();
				assertEquals(RegDate.get(1988, 1, 1), estimation0.getDateDebut());
				assertNull(estimation0.getDateFin());
				assertEquals(Long.valueOf(240000), estimation0.getMontant());
				assertEquals("RG88", estimation0.getReference());
				assertEquals(Integer.valueOf(1988), estimation0.getAnneeReference());
				assertNull(estimation0.getDateInscription());
				assertEquals(RegDate.get(1988, 1, 1), estimation0.getDateDebutMetier());
				assertNull(estimation0.getDateFinMetier());
				assertTrue(estimation0.isEnRevision());
			}
		});
	}

	/**
	 * [SIFISC-22995] Ce test vérifie que le processing d'une mutation de modification sur l'estimation fiscale annule bien l'estimation fiscale existante s'il les années de début métier sont identiques.
	 */
	@Test
	public void testProcessMutationModificationEstimationCorrectionEstimation() throws Exception {

		// précondition : il y a déjà un immeuble dans la base avec une estimation fiscale pas en révision
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				CommuneRF commune = new CommuneRF();
				commune.setNoRf(294);
				commune.setNomRf("Pétahouchnok");
				commune.setNoOfs(66666);
				commune = communeRFDAO.save(commune);

				final BienFondRF bienFond = new BienFondRF();
				bienFond.setIdRF("_1f109152381026b501381028a73d1852");
				bienFond.setEgrid("CH938391457759");
				bienFond.setCfa(false);

				final SituationRF situation = new SituationRF();
				situation.setDateDebut(RegDate.get(1988, 1, 1));
				situation.setCommune(commune);
				situation.setNoParcelle(5089);
				bienFond.addSituation(situation);

				final EstimationRF estimation = new EstimationRF();
				estimation.setDateDebut(RegDate.get(1988, 1, 1));
				estimation.setMontant(240000L);
				estimation.setReference("RG93");
				estimation.setAnneeReference(1993);
				estimation.setDateDebutMetier(RegDate.get(1993, 1, 1));
				estimation.setEnRevision(false);
				bienFond.addEstimation(estimation);

				final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
				surfaceTotale.setDateDebut(RegDate.get(1988, 1, 1));
				surfaceTotale.setSurface(707);
				bienFond.addSurfaceTotale(surfaceTotale);

				immeubleRFDAO.save(bienFond);
				assertEquals(1, immeubleRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
				assertEquals(1, immeubles.size());

				final ImmeubleRF immeuble0 = immeubles.get(0);
				assertEquals("_1f109152381026b501381028a73d1852", immeuble0.getIdRF());
				assertEquals("CH938391457759", immeuble0.getEgrid());

				// par contre, il y a une nouvelle estimation
				final Set<EstimationRF> estimations = immeuble0.getEstimations();
				assertEquals(2, estimations.size());

				final List<EstimationRF> estimationList = new ArrayList<>(estimations);
				estimationList.sort(new DateRangeComparator<>());

				// la première estimation doit être annule
				final EstimationRF estimation0 = estimationList.get(0);
				assertEquals(RegDate.get(1988, 1, 1), estimation0.getDateDebut());
				assertNull(estimation0.getDateFin());
				assertEquals(Long.valueOf(240000), estimation0.getMontant());
				assertEquals("RG93", estimation0.getReference());
				assertEquals(Integer.valueOf(1993), estimation0.getAnneeReference());
				assertNull(estimation0.getDateInscription());
				assertEquals(RegDate.get(1993, 1, 1), estimation0.getDateDebutMetier());
				assertNull(estimation0.getDateFinMetier());
				assertFalse(estimation0.isEnRevision());
				assertTrue(estimation0.isAnnule());

				// la seconde estimation doit être la seule valide
				final EstimationRF estimation1 = estimationList.get(1);
				assertEquals(RegDate.get(2016, 10, 1), estimation1.getDateDebut());
				assertNull(estimation1.getDateFin());
				assertEquals(Long.valueOf(260000), estimation1.getMontant());
				assertEquals("RG93", estimation1.getReference());
				assertEquals(Integer.valueOf(1993), estimation1.getAnneeReference());
				assertNull(estimation1.getDateInscription());
				assertEquals(RegDate.get(1993, 1, 1), estimation1.getDateDebutMetier());
				assertNull(estimation1.getDateFinMetier());
				assertFalse(estimation1.isEnRevision());
				assertFalse(estimation1.isAnnule());
			}
		});
	}

	/**
	 * [SIFISC-22995] Ce test vérifie que le processing d'une mutation de modification sur l'estimation fiscale annule bien l'estimation fiscale existante si elle était en révision et que l'intervalle de validité résultante est négatif.
	 */
	@Test
	public void testProcessMutationModificationEstimationAnnulationDeRevision() throws Exception {

		// précondition : il y a déjà un immeuble dans la base avec une estimation fiscale en révision
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				CommuneRF commune = new CommuneRF();
				commune.setNoRf(294);
				commune.setNomRf("Pétahouchnok");
				commune.setNoOfs(66666);
				commune = communeRFDAO.save(commune);

				final BienFondRF bienFond = new BienFondRF();
				bienFond.setIdRF("_1f109152381026b501381028a73d1852");
				bienFond.setEgrid("CH938391457759");
				bienFond.setCfa(false);

				final SituationRF situation = new SituationRF();
				situation.setDateDebut(RegDate.get(1988, 1, 1));
				situation.setCommune(commune);
				situation.setNoParcelle(5089);
				bienFond.addSituation(situation);

				final EstimationRF estimation = new EstimationRF();
				estimation.setDateDebut(RegDate.get(1988, 1, 1));
				estimation.setMontant(0L);
				estimation.setReference("2015");
				estimation.setAnneeReference(2015);
				estimation.setDateDebutMetier(RegDate.get(2015, 1, 1));
				estimation.setEnRevision(true);
				bienFond.addEstimation(estimation);

				final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
				surfaceTotale.setDateDebut(RegDate.get(1988, 1, 1));
				surfaceTotale.setSurface(707);
				bienFond.addSurfaceTotale(surfaceTotale);

				immeubleRFDAO.save(bienFond);
				assertEquals(1, immeubleRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
				assertEquals(1, immeubles.size());

				final ImmeubleRF immeuble0 = immeubles.get(0);
				assertEquals("_1f109152381026b501381028a73d1852", immeuble0.getIdRF());
				assertEquals("CH938391457759", immeuble0.getEgrid());

				// par contre, il y a une nouvelle estimation
				final Set<EstimationRF> estimations = immeuble0.getEstimations();
				assertEquals(2, estimations.size());

				final List<EstimationRF> estimationList = new ArrayList<>(estimations);
				estimationList.sort(new DateRangeComparator<>());

				// la première estimation doit être annulée
				final EstimationRF estimation0 = estimationList.get(0);
				assertEquals(RegDate.get(1988, 1, 1), estimation0.getDateDebut());
				assertEquals(RegDate.get(2016, 9, 30), estimation0.getDateFin());
				assertEquals(Long.valueOf(0), estimation0.getMontant());
				assertEquals("2015", estimation0.getReference());
				assertEquals(Integer.valueOf(2015), estimation0.getAnneeReference());
				assertNull(estimation0.getDateInscription());
				assertEquals(RegDate.get(2015, 1, 1), estimation0.getDateDebutMetier());
				assertNull(estimation0.getDateFinMetier());
				assertTrue(estimation0.isEnRevision());
				assertTrue(estimation0.isAnnule());

				// la seconde estimation doit être la seule valide
				final EstimationRF estimation1 = estimationList.get(1);
				assertEquals(RegDate.get(2016, 10, 1), estimation1.getDateDebut());
				assertNull(estimation1.getDateFin());
				assertEquals(Long.valueOf(260000), estimation1.getMontant());
				assertEquals("RG93", estimation1.getReference());
				assertEquals(Integer.valueOf(1993), estimation1.getAnneeReference());
				assertNull(estimation1.getDateInscription());
				assertEquals(RegDate.get(1993, 1, 1), estimation1.getDateDebutMetier());
				assertNull(estimation1.getDateFinMetier());
				assertFalse(estimation1.isEnRevision());
				assertFalse(estimation1.isAnnule());
			}
		});
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de modification de la surface totale met bien à jour l'immeuble existant dans la DB
	 */
	@Test
	public void testProcessMutationModificationSurfaceTotale() throws Exception {

		// précondition : il y a déjà un immeuble dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				CommuneRF commune = new CommuneRF();
				commune.setNoRf(294);
				commune.setNomRf("Pétahouchnok");
				commune.setNoOfs(66666);
				commune = communeRFDAO.save(commune);

				final BienFondRF bienFond = new BienFondRF();
				bienFond.setIdRF("_1f109152381026b501381028a73d1852");
				bienFond.setEgrid("CH938391457759");
				bienFond.setCfa(false);

				final SituationRF situation = new SituationRF();
				situation.setDateDebut(RegDate.get(1988, 1, 1));
				situation.setCommune(commune);
				situation.setNoParcelle(5089);
				bienFond.addSituation(situation);

				final EstimationRF estimation = new EstimationRF();
				estimation.setDateDebut(RegDate.get(1988, 1, 1));
				estimation.setMontant(260000L);
				estimation.setReference("RG93");
				estimation.setAnneeReference(1993);
				estimation.setDateDebutMetier(RegDate.get(1993, 1, 1));
				estimation.setEnRevision(false);
				bienFond.addEstimation(estimation);

				final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
				surfaceTotale.setDateDebut(RegDate.get(1988, 1, 1));
				surfaceTotale.setSurface(532);
				bienFond.addSurfaceTotale(surfaceTotale);

				immeubleRFDAO.save(bienFond);
				assertEquals(1, immeubleRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
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
				assertEquals(294, situation0.getCommune().getNoRf());
				assertEquals(5089, situation0.getNoParcelle());
				assertNull(situation0.getIndex1());
				assertNull(situation0.getIndex2());
				assertNull(situation0.getIndex3());

				// l'estimaiton n'a pas changé
				final Set<EstimationRF> estimations = immeuble0.getEstimations();
				assertEquals(1, estimations.size());

				final EstimationRF estimation0 = estimations.iterator().next();
				assertEquals(RegDate.get(1988, 1, 1), estimation0.getDateDebut());
				assertNull(estimation0.getDateFin());
				assertEquals(Long.valueOf(260000), estimation0.getMontant());
				assertEquals("RG93", estimation0.getReference());
				assertEquals(Integer.valueOf(1993), estimation0.getAnneeReference());
				assertNull(estimation0.getDateInscription());
				assertEquals(RegDate.get(1993, 1, 1), estimation0.getDateDebutMetier());
				assertNull(estimation0.getDateFinMetier());
				assertFalse(estimation0.isEnRevision());

				// par contre, la surface totale a bien changé
				final Set<SurfaceTotaleRF> surfacesTotales = immeuble0.getSurfacesTotales();
				assertEquals(2, surfacesTotales.size());

				final List<SurfaceTotaleRF> surfacesList = new ArrayList<>(surfacesTotales);
				surfacesList.sort(new DateRangeComparator<>());

				// la première surface doit être fermée la veille de la date d'import
				final SurfaceTotaleRF surface0 = surfacesList.get(0);
				assertEquals(RegDate.get(1988, 1, 1), surface0.getDateDebut());
				assertEquals(RegDate.get(2016, 9, 30), surface0.getDateFin());
				assertEquals(532, surface0.getSurface());

				// la seconde surface doit commencer à la date de l'import
				final SurfaceTotaleRF surface1 = surfacesList.get(1);
				assertEquals(RegDate.get(2016, 10, 1), surface1.getDateDebut());
				assertNull(surface1.getDateFin());
				assertEquals(707, surface1.getSurface());
			}
		});
	}

	/**
	 * [SIFISC-24013] Ce test vérifie que le processing d'une mutation sur un immeuble radiée réactive bien l'immeuble.
	 */
	@Test
	public void testProcessMutationModificationImmeubleRadie() throws Exception {

		final RegDate dateRadiation = RegDate.get(2010, 12, 31);
		final RegDate dateValeur = RegDate.get(2016, 10, 1);

		// précondition : il y a un immeuble radié dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				CommuneRF commune = new CommuneRF();
				commune.setNoRf(294);
				commune.setNomRf("Pétahouchnok");
				commune.setNoOfs(66666);
				commune = communeRFDAO.save(commune);

				final BienFondRF bienFond = new BienFondRF();
				bienFond.setIdRF("_1f109152381026b501381028a73d1852");
				bienFond.setEgrid("CH938391457759");
				bienFond.setCfa(false);
				bienFond.setDateRadiation(dateRadiation);   // <---- immeuble radié

				final SituationRF situation = new SituationRF();
				situation.setDateDebut(RegDate.get(1988, 1, 1));
				situation.setDateFin(dateRadiation);
				situation.setCommune(commune);
				situation.setNoParcelle(5089);
				bienFond.addSituation(situation);

				immeubleRFDAO.save(bienFond);
				assertEquals(1, immeubleRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateValeur, TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée et l'immeuble n'est plus radié dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
				assertEquals(1, immeubles.size());

				// l'immeuble n'est plus radié
				final ImmeubleRF immeuble0 = immeubles.get(0);
				assertEquals("_1f109152381026b501381028a73d1852", immeuble0.getIdRF());
				assertEquals("CH938391457759", immeuble0.getEgrid());
				assertNull(immeuble0.getDateRadiation());

				// la situation est de nouveau active
				final Set<SituationRF> situations = immeuble0.getSituations();
				assertEquals(1, situations.size());

				final SituationRF situation0 = situations.iterator().next();
				assertEquals(RegDate.get(1988, 1, 1), situation0.getDateDebut());
				assertNull(situation0.getDateFin());
				assertEquals(294, situation0.getCommune().getNoRf());
				assertEquals(5089, situation0.getNoParcelle());
				assertNull(situation0.getIndex1());
				assertNull(situation0.getIndex2());
				assertNull(situation0.getIndex3());
			}
		});
	}

	/**
	 * [SIFISC-25610] Ce test vérifie que le processing d'une mutation d'egrid sur un immeuble modifie bien l'immeuble en base.
	 */
	@Test
	public void testProcessMutationModificationEgrid() throws Exception {

		final RegDate dateValeur = RegDate.get(2016, 10, 1);

		// précondition : il y a un immeuble sans egrid dans la base
		doInNewTransaction(status -> {

			CommuneRF commune = new CommuneRF();
			commune.setNoRf(294);
			commune.setNomRf("Pétahouchnok");
			commune.setNoOfs(66666);
			commune = communeRFDAO.save(commune);

			final BienFondRF bienFonds = new BienFondRF();
			bienFonds.setIdRF("_1f109152381026b501381028a73d1852");
			bienFonds.setEgrid(null);   // <---- pas d'egrid
			bienFonds.setCfa(false);

			final SituationRF situation = new SituationRF();
			situation.setDateDebut(RegDate.get(1988, 1, 1));
			situation.setCommune(commune);
			situation.setNoParcelle(5089);
			bienFonds.addSituation(situation);

			final EstimationRF estimation = new EstimationRF();
			estimation.setDateDebut(RegDate.get(1988, 1, 1));
			estimation.setMontant(260000L);
			estimation.setReference("RG93");
			estimation.setAnneeReference(1993);
			estimation.setDateDebutMetier(RegDate.get(1993, 1, 1));
			estimation.setEnRevision(false);
			bienFonds.addEstimation(estimation);

			final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
			surfaceTotale.setDateDebut(RegDate.get(1988, 1, 1));
			surfaceTotale.setSurface(707);
			bienFonds.addSurfaceTotale(surfaceTotale);

			immeubleRFDAO.save(bienFonds);
			assertEquals(1, immeubleRFDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, dateValeur, TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et l'egrid de l'immeuble est renseigné en base
		doInNewTransaction(status -> {

			final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
			assertEquals(1, immeubles.size());

			// l'egrid est renseigné
			final ImmeubleRF immeuble0 = immeubles.get(0);
			assertEquals("_1f109152381026b501381028a73d1852", immeuble0.getIdRF());
			assertEquals("CH938391457759", immeuble0.getEgrid());

			return null;
		});
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de modification d'un immeuble sans estimation fiscale fonctionne bien.
	 */
	@Test
	public void testProcessMutationModificationImmeubleSansEstimationFiscale() throws Exception {

		// précondition : il y a déjà un immeuble dans la base (celui-ci possède une estimation fiscale)
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				CommuneRF commune = new CommuneRF();
				commune.setNoRf(13);
				commune.setNomRf("Pétahouchnok");
				commune.setNoOfs(66666);
				commune = communeRFDAO.save(commune);

				final ProprieteParEtageRF ppe = new ProprieteParEtageRF();
				ppe.setIdRF("_8af80e62567f816f01571d91f3e56a38");
				ppe.setEgrid("CH776584246539");
				ppe.addQuotePart(new QuotePartRF(RegDate.get(1988, 1, 1), null, new Fraction(8, 1000)));

				final SituationRF situation = new SituationRF();
				situation.setDateDebut(RegDate.get(1988, 1, 1));
				situation.setCommune(commune);
				situation.setNoParcelle(917);
				situation.setIndex1(106);
				ppe.addSituation(situation);

				final EstimationRF estimation = new EstimationRF();
				estimation.setDateDebut(RegDate.get(1988, 1, 1));
				estimation.setMontant(240000L);
				estimation.setReference("RG88");
				estimation.setAnneeReference(1988);
				estimation.setDateDebutMetier(RegDate.get(1988, 1, 1));
				estimation.setEnRevision(false);
				ppe.addEstimation(estimation);

				immeubleRFDAO.save(ppe);
				assertEquals(1, immeubleRFDAO.getAll().size());
			}
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf_sans_estimation_fiscale.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_8af80e62567f816f01571d91f3e56a38", null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée, l'immeuble est créé en base et son ancienne estimation fiscale est fermée
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
				assertEquals(1, immeubles.size());

				final ImmeubleRF immeuble0 = immeubles.get(0);
				assertEquals("_8af80e62567f816f01571d91f3e56a38", immeuble0.getIdRF());
				assertEquals("CH776584246539", immeuble0.getEgrid());

				// la situation n'a pas changé
				final Set<SituationRF> situations = immeuble0.getSituations();
				assertEquals(1, situations.size());

				final SituationRF situation0 = situations.iterator().next();
				assertEquals(RegDate.get(1988, 1, 1), situation0.getDateDebut());
				assertNull(situation0.getDateFin());
				assertEquals(13, situation0.getCommune().getNoRf());
				assertEquals(917, situation0.getNoParcelle());
				assertEquals(Integer.valueOf(106), situation0.getIndex1());
				assertNull(situation0.getIndex2());
				assertNull(situation0.getIndex3());

				// l'ancienne estimation fiscale est dorénavant fermée
				final Set<EstimationRF> estimations = immeuble0.getEstimations();
				assertEquals(1, estimations.size());

				// la première estimation doit être fermée la veille de la date d'import
				final EstimationRF estimation0 = estimations.iterator().next();
				assertEquals(RegDate.get(1988, 1, 1), estimation0.getDateDebut());
				assertEquals(RegDate.get(2016, 9, 30), estimation0.getDateFin());
				assertEquals(Long.valueOf(240000), estimation0.getMontant());
				assertEquals("RG88", estimation0.getReference());
				assertEquals(Integer.valueOf(1988), estimation0.getAnneeReference());
				assertNull(estimation0.getDateInscription());
				assertEquals(RegDate.get(1988, 1, 1), estimation0.getDateDebutMetier());
				assertEquals(RegDate.get(2016, 9, 30), estimation0.getDateFinMetier());
				assertFalse(estimation0.isEnRevision());
			}
		});
	}

	/**
	 * [SIFISC-24715] Ce test vérifie que le processing d'une mutation de modification de la quote-part d'un immeuble met bien à jour l'historique des quotes-parts.
	 */
	@Test
	public void testProcessMutationModificationQuotePartImmeuble() throws Exception {

		// précondition : il y a déjà un immeuble avec quote-part dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				CommuneRF commune = new CommuneRF();
				commune.setNoRf(13);
				commune.setNomRf("Pétahouchnok");
				commune.setNoOfs(66666);
				commune = communeRFDAO.save(commune);

				final ProprieteParEtageRF ppe = new ProprieteParEtageRF();
				ppe.setIdRF("_8af80e62567f816f01571d91f3e56a38");
				ppe.setEgrid("CH776584246539");
				ppe.addQuotePart(new QuotePartRF(RegDate.get(1988, 1, 1), null, new Fraction(900, 1000)));

				final SituationRF situation = new SituationRF();
				situation.setDateDebut(RegDate.get(1988, 1, 1));
				situation.setCommune(commune);
				situation.setNoParcelle(917);
				situation.setIndex1(106);
				ppe.addSituation(situation);

				immeubleRFDAO.save(ppe);
				assertEquals(1, immeubleRFDAO.getAll().size());
			}
		});

		// le même immeuble mais avec une quote-part différente
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf_sans_estimation_fiscale.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_8af80e62567f816f01571d91f3e56a38", null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée, l'ancienne quote-part est fermée et une nouvelle est créée
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
				assertEquals(1, immeubles.size());

				final ImmeubleRF immeuble0 = immeubles.get(0);
				assertEquals("_8af80e62567f816f01571d91f3e56a38", immeuble0.getIdRF());
				assertEquals("CH776584246539", immeuble0.getEgrid());

				final ProprieteParEtageRF ppe =(ProprieteParEtageRF) immeuble0;
				final List<QuotePartRF> quotesParts = new ArrayList<>(ppe.getQuotesParts());
				quotesParts.sort(new DateRangeComparator<>());
				assertEquals(2, quotesParts.size());

				// la première quote-part doit être fermée la veille de la date d'import
				final QuotePartRF quotePart0 = quotesParts.get(0);
				assertEquals(RegDate.get(1988, 1, 1), quotePart0.getDateDebut());
				assertEquals(RegDate.get(2016, 9, 30), quotePart0.getDateFin());
				assertEquals(new Fraction(900, 1000), quotePart0.getQuotePart());

				// une nouvelle quote-part doit être créée
				final QuotePartRF quotePart1 = quotesParts.get(1);
				assertEquals(RegDate.get(2016, 10, 1), quotePart1.getDateDebut());
				assertNull(quotePart1.getDateFin());
				assertEquals(new Fraction(8, 1000), quotePart1.getQuotePart());
			}
		});
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de suppression d'un immeuble ajoute bien une date de radiation.
	 */
	@Test
	public void testProcessMutationSuppression() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);
		final RegDate veilleImport = dateImport.getOneDayBefore();

		// précondition : il y a un immeuble dans la base
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				CommuneRF commune = new CommuneRF();
				commune.setNoRf(294);
				commune.setNomRf("Pétahouchnok");
				commune.setNoOfs(66666);
				commune = communeRFDAO.save(commune);

				final BienFondRF bienFond = new BienFondRF();
				bienFond.setIdRF("_1f109152381026b501381028a73d1852");
				bienFond.setEgrid("CH938391457759");
				bienFond.setCfa(false);

				final SituationRF situation = new SituationRF();
				situation.setDateDebut(RegDate.get(1988, 1, 1));
				situation.setDateFin(veilleImport); // on pré-renseigne la date de fermeture pour que la validation de l'immeuble passe (on ne veut tester que la mutation sur l'immeuble)
				situation.setCommune(commune);
				situation.setNoParcelle(5089);
				bienFond.addSituation(situation);

				final EstimationRF estimation = new EstimationRF();
				estimation.setDateDebut(RegDate.get(1988, 1, 1));
				estimation.setDateFin(veilleImport);
				estimation.setMontant(260000L);
				estimation.setReference("RG93");
				estimation.setAnneeReference(1993);
				estimation.setEnRevision(false);
				bienFond.addEstimation(estimation);

				final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
				surfaceTotale.setDateDebut(RegDate.get(1988, 1, 1));
				surfaceTotale.setDateFin(veilleImport);
				surfaceTotale.setSurface(532);
				bienFond.addSurfaceTotale(surfaceTotale);

				immeubleRFDAO.save(bienFond);
				assertEquals(1, immeubleRFDAO.getAll().size());
			}
		});

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(null, dateImport, TypeEntiteRF.IMMEUBLE, TypeMutationRF.SUPPRESSION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée et l'immeuble est radié
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
				assertEquals(1, immeubles.size());

				// l'immeuble est radié
				final ImmeubleRF immeuble0 = immeubles.get(0);
				assertEquals("_1f109152381026b501381028a73d1852", immeuble0.getIdRF());
				assertEquals("CH938391457759", immeuble0.getEgrid());
				assertEquals(veilleImport, immeuble0.getDateRadiation());
			}
		});
	}

	/**
	 * [SIFISC-24715] Ce test vérifie que le processing d'une mutation de suppression d'un immeuble ferme bien les quotes-parts
	 */
	@Test
	public void testProcessMutationSuppressionImmeubleAvecQuotesParts() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);
		final RegDate veilleImport = dateImport.getOneDayBefore();

		// précondition : il y a déjà un immeuble dans la base avec une quote-part
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				CommuneRF commune = new CommuneRF();
				commune.setNoRf(13);
				commune.setNomRf("Pétahouchnok");
				commune.setNoOfs(66666);
				commune = communeRFDAO.save(commune);

				final ProprieteParEtageRF ppe = new ProprieteParEtageRF();
				ppe.setIdRF("_8af80e62567f816f01571d91f3e56a38");
				ppe.setEgrid("CH776584246539");
				ppe.addQuotePart(new QuotePartRF(RegDate.get(1988, 1, 1), null, new Fraction(8, 1000)));

				final SituationRF situation = new SituationRF();
				situation.setDateDebut(RegDate.get(1988, 1, 1));
				situation.setCommune(commune);
				situation.setNoParcelle(917);
				situation.setIndex1(106);
				ppe.addSituation(situation);

				final EstimationRF estimation = new EstimationRF();
				estimation.setDateDebut(RegDate.get(1988, 1, 1));
				estimation.setMontant(240000L);
				estimation.setReference("RG88");
				estimation.setAnneeReference(1988);
				estimation.setDateDebutMetier(RegDate.get(1988, 1, 1));
				estimation.setEnRevision(false);
				ppe.addEstimation(estimation);

				immeubleRFDAO.save(ppe);
				assertEquals(1, immeubleRFDAO.getAll().size());
			}
		});

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(null, dateImport, TypeEntiteRF.IMMEUBLE, TypeMutationRF.SUPPRESSION, "_8af80e62567f816f01571d91f3e56a38", null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée et l'immeuble est radié
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
				assertEquals(1, immeubles.size());

				// l'immeuble est radié
				final ProprieteParEtageRF ppe = (ProprieteParEtageRF) immeubles.get(0);
				assertEquals("_8af80e62567f816f01571d91f3e56a38", ppe.getIdRF());
				assertEquals("CH776584246539", ppe.getEgrid());
				assertEquals(veilleImport, ppe.getDateRadiation());

				// la situation est fermée
				final Set<SituationRF> situations = ppe.getSituations();
				assertEquals(1, situations.size());
				assertEquals(veilleImport, situations.iterator().next().getDateFin());

				// la quote-part est fermée
				final Set<QuotePartRF> quoteParts = ppe.getQuotesParts();
				assertEquals(1, quoteParts.size());
				assertEquals(veilleImport, quoteParts.iterator().next().getDateFin());
			}
		});
	}
}