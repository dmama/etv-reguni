package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalImmeuble;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantDroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ModeleCommunauteRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.QuotePartRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.RegroupementCommunauteRF;
import ch.vd.uniregctb.registrefoncier.ServitudeRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceTotaleRF;
import ch.vd.uniregctb.registrefoncier.TypeCommunaute;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.registrefoncier.dao.AyantDroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.CommuneRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.DroitRFDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.dataimport.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.processor.MutationRFProcessorTestCase;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeRapprochementRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("Duplicates")
public class ImmeubleRFProcessorTest extends MutationRFProcessorTestCase {

	private EvenementRFMutationDAO evenementRFMutationDAO;
	private EvenementFiscalDAO evenementFiscalDAO;

	private AyantDroitRFDAO ayantDroitRFDAO;
	private DroitRFDAO droitRFDAO;
	private CommuneRFDAO communeRFDAO;
	private ImmeubleRFDAO immeubleRFDAO;
	private ImmeubleRFProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.ayantDroitRFDAO = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
		this.droitRFDAO = getBean(DroitRFDAO.class, "droitRFDAO");
		this.communeRFDAO = getBean(CommuneRFDAO.class, "communeRFDAO");
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
		this.evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
		this.immeubleRFDAO = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
		final XmlHelperRF xmlHelperRF = getBean(XmlHelperRF.class, "xmlHelperRF");
		final EvenementFiscalService evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");
		final CommunauteRFProcessor communauteRFProcessor = getBean(CommunauteRFProcessor.class, "communauteRFProcessor");

		this.processor = new ImmeubleRFProcessor(communeRFDAO, immeubleRFDAO, communauteRFProcessor, xmlHelperRF, evenementFiscalService);
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
		doInNewTransaction(status -> {
			assertEquals(0, immeubleRFDAO.getAll().size());
			assertEquals(0, evenementFiscalDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la commune qui correspond à l'immeuble
		insertCommune(294, "Oron", 5555);

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.CREATION, "1f109152381026b501381028a74018e1", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et l'immeuble est créé en base
		doInNewTransaction(status -> {

			final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
			assertEquals(1, immeubles.size());

			final BienFondsRF immeuble0 = (BienFondsRF) immeubles.get(0);
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
			return null;
		});

		// postcondition : l'événement fiscal correspondant a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.CREATION, event0.getType());
			assertEquals(RegDate.get(2016, 10, 1), event0.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event0.getImmeuble().getIdRF());

			return null;
		});
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de création fonctionne bien même si l'immeuble ne possède pas d'estimation fiscale.
	 */
	@Test
	public void testProcessMutationCreationImmeubleSansEstimationFiscale() throws Exception {

		// précondition : la base est vide
		doInNewTransaction(status -> {
			assertEquals(0, immeubleRFDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf_sans_estimation_fiscale.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la commune qui correspond à l'immeuble
		insertCommune(13, "Roche (VD)", 5555);

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.CREATION, "_8af80e62567f816f01571d91f3e56a38", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et l'immeuble est créé en base
		doInNewTransaction(status -> {

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
			return null;
		});
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de modification l'estimation fiscale met bien à jour l'immeuble existant dans la DB
	 */
	@Test
	public void testProcessMutationModificationEstimation() throws Exception {

		// précondition : il y a déjà un immeuble dans la base
		doInNewTransaction(status -> {

			CommuneRF commune = new CommuneRF();
			commune.setNoRf(294);
			commune.setNomRf("Pétahouchnok");
			commune.setNoOfs(66666);
			commune = communeRFDAO.save(commune);

			final BienFondsRF bienFonds = new BienFondsRF();
			bienFonds.setIdRF("_1f109152381026b501381028a73d1852");
			bienFonds.setEgrid("CH938391457759");
			bienFonds.setCfa(false);

			final SituationRF situation = new SituationRF();
			situation.setDateDebut(RegDate.get(1988, 1, 1));
			situation.setCommune(commune);
			situation.setNoParcelle(5089);
			bienFonds.addSituation(situation);

			final EstimationRF estimation = new EstimationRF();
			estimation.setDateDebut(RegDate.get(1988, 1, 1));
			estimation.setMontant(240000L);
			estimation.setReference("RG88");
			estimation.setAnneeReference(1988);
			estimation.setDateDebutMetier(RegDate.get(1988, 1, 1));
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
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et l'immeuble est créé en base
		doInNewTransaction(status -> {

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
			return null;
		});

		// postcondition : les événements fiscaux a bien été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getDateValeur));

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.FIN_ESTIMATION, event0.getType());
			assertEquals(RegDate.get(1992, 12, 31), event0.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event0.getImmeuble().getIdRF());

			final EvenementFiscalImmeuble event1 = (EvenementFiscalImmeuble) events.get(1);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.DEBUT_ESTIMATION, event1.getType());
			assertEquals(RegDate.get(1993, 1, 1), event1.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event1.getImmeuble().getIdRF());

			return null;
		});
	}

	/**
	 * [SIFISC-22995] Ce test vérifie que le processing d'une mutation de modification sur l'estimation fiscale corrige bien l'estimation fiscale existante s'il s'agit du passage en révision de l'estimation (il ne doit pas y avoir de création d'une
	 * nouvelle estimation fiscale).
	 */
	@Test
	public void testProcessMutationModificationEstimationPassageEnRevision() throws Exception {

		// précondition : il y a déjà un immeuble dans la base avec une estimation fiscale pas en révision
		doInNewTransaction(status -> {

			CommuneRF commune = new CommuneRF();
			commune.setNoRf(294);
			commune.setNomRf("Pétahouchnok");
			commune.setNoOfs(66666);
			commune = communeRFDAO.save(commune);

			final BienFondsRF bienFonds = new BienFondsRF();
			bienFonds.setIdRF("_1f109152381026b501381028a73d1852");
			bienFonds.setEgrid("CH938391457759");
			bienFonds.setCfa(false);

			final SituationRF situation = new SituationRF();
			situation.setDateDebut(RegDate.get(1988, 1, 1));
			situation.setCommune(commune);
			situation.setNoParcelle(5089);
			bienFonds.addSituation(situation);

			final EstimationRF estimation = new EstimationRF();
			estimation.setDateDebut(RegDate.get(1988, 1, 1));
			estimation.setMontant(240000L);
			estimation.setReference("RG88");
			estimation.setAnneeReference(1988);
			estimation.setDateDebutMetier(RegDate.get(1988, 1, 1));
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

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf_estimation_en_revision.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée
		doInNewTransaction(status -> {

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
			return null;
		});

		// postcondition : l'événement fiscal a bien été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_STATUT_REVISION_ESTIMATION, event0.getType());
			assertEquals(RegDate.get(2016, 10, 1), event0.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event0.getImmeuble().getIdRF());

			return null;
		});
	}

	/**
	 * [SIFISC-22995] Ce test vérifie que le processing d'une mutation de modification sur l'estimation fiscale annule bien l'estimation fiscale existante s'il les années de début métier sont identiques.
	 */
	@Test
	public void testProcessMutationModificationEstimationCorrectionEstimation() throws Exception {

		// précondition : il y a déjà un immeuble dans la base avec une estimation fiscale pas en révision
		doInNewTransaction(status -> {

			CommuneRF commune = new CommuneRF();
			commune.setNoRf(294);
			commune.setNomRf("Pétahouchnok");
			commune.setNoOfs(66666);
			commune = communeRFDAO.save(commune);

			final BienFondsRF bienFonds = new BienFondsRF();
			bienFonds.setIdRF("_1f109152381026b501381028a73d1852");
			bienFonds.setEgrid("CH938391457759");
			bienFonds.setCfa(false);

			final SituationRF situation = new SituationRF();
			situation.setDateDebut(RegDate.get(1988, 1, 1));
			situation.setCommune(commune);
			situation.setNoParcelle(5089);
			bienFonds.addSituation(situation);

			final EstimationRF estimation = new EstimationRF();
			estimation.setDateDebut(RegDate.get(1988, 1, 1));
			estimation.setMontant(240000L);
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
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée
		doInNewTransaction(status -> {

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
			return null;
		});

		// postcondition : les événements fiscaux a bien été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getId));

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.ANNULATION_ESTIMATION, event0.getType());
			assertEquals(RegDate.get(2016, 10, 1), event0.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event0.getImmeuble().getIdRF());

			final EvenementFiscalImmeuble event1 = (EvenementFiscalImmeuble) events.get(1);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.DEBUT_ESTIMATION, event1.getType());
			assertEquals(RegDate.get(1993, 1, 1), event1.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event1.getImmeuble().getIdRF());

			return null;
		});
	}

	/**
	 * [SIFISC-22995] Ce test vérifie que le processing d'une mutation de modification sur l'estimation fiscale annule bien l'estimation fiscale existante si elle était en révision et que l'intervalle de validité résultante est négatif.
	 */
	@Test
	public void testProcessMutationModificationEstimationAnnulationDeRevision() throws Exception {

		// précondition : il y a déjà un immeuble dans la base avec une estimation fiscale en révision
		doInNewTransaction(status -> {

			CommuneRF commune = new CommuneRF();
			commune.setNoRf(294);
			commune.setNomRf("Pétahouchnok");
			commune.setNoOfs(66666);
			commune = communeRFDAO.save(commune);

			final BienFondsRF bienFonds = new BienFondsRF();
			bienFonds.setIdRF("_1f109152381026b501381028a73d1852");
			bienFonds.setEgrid("CH938391457759");
			bienFonds.setCfa(false);

			final SituationRF situation = new SituationRF();
			situation.setDateDebut(RegDate.get(1988, 1, 1));
			situation.setCommune(commune);
			situation.setNoParcelle(5089);
			bienFonds.addSituation(situation);

			final EstimationRF estimation = new EstimationRF();
			estimation.setDateDebut(RegDate.get(1988, 1, 1));
			estimation.setMontant(0L);
			estimation.setReference("2015");
			estimation.setAnneeReference(2015);
			estimation.setDateDebutMetier(RegDate.get(2015, 1, 1));
			estimation.setEnRevision(true);
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
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée
		doInNewTransaction(status -> {

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
			return null;
		});

		// postcondition : les événements fiscaux a bien été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getId));

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.DEBUT_ESTIMATION, event0.getType());
			assertEquals(RegDate.get(1993, 1, 1), event0.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event0.getImmeuble().getIdRF());

			final EvenementFiscalImmeuble event1 = (EvenementFiscalImmeuble) events.get(1);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.ANNULATION_ESTIMATION, event1.getType());
			assertEquals(RegDate.get(2016, 10, 1), event1.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event1.getImmeuble().getIdRF());

			return null;
		});
	}

	/**
	 * [SIFISC-25028] Ce test vérifie que le processing d'une mutation de modification sur l'estimation fiscale ajoute bien une nouvelle estimation lorsqu'une ancienne estimation avec les mêmes valeurs existe mais avec une date de fin métier.
	 */
	@Test
	public void testProcessMutationModificationEstimationReapparitionEstimationFermee() throws Exception {

		// précondition : il y a déjà un immeuble dans la base avec une estimation fiscale fermée
		doInNewTransaction(status -> {

			CommuneRF commune = new CommuneRF();
			commune.setNoRf(294);
			commune.setNomRf("Pétahouchnok");
			commune.setNoOfs(66666);
			commune = communeRFDAO.save(commune);

			final BienFondsRF bienFonds = new BienFondsRF();
			bienFonds.setIdRF("_1f109152381026b501381028a73d1852");
			bienFonds.setEgrid("CH938391457759");
			bienFonds.setCfa(false);

			final SituationRF situation = new SituationRF();
			situation.setDateDebut(RegDate.get(1988, 1, 1));
			situation.setCommune(commune);
			situation.setNoParcelle(5089);
			bienFonds.addSituation(situation);

			final EstimationRF estimation = new EstimationRF();
			estimation.setDateDebut(RegDate.get(1988, 1, 1));
			estimation.setDateFin(RegDate.get(2016, 7, 1));
			estimation.setMontant(260000L);
			estimation.setReference("RG93");
			estimation.setAnneeReference(1993);
			estimation.setDateDebutMetier(RegDate.get(1993, 1, 1));
			estimation.setDateFinMetier(RegDate.get(2016, 7, 1));
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
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée
		doInNewTransaction(status -> {

			final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
			assertEquals(1, immeubles.size());

			final ImmeubleRF immeuble0 = immeubles.get(0);
			assertEquals("_1f109152381026b501381028a73d1852", immeuble0.getIdRF());
			assertEquals("CH938391457759", immeuble0.getEgrid());

			// il y a une deux estimations :
			// - l'estimation fermée préexistante qui est maintenant annulée
			// - une nouvelle estimation qui correspond aux valeurs reçues dans l'import
			final Set<EstimationRF> estimations = immeuble0.getEstimations();
			assertEquals(2, estimations.size());

			final List<EstimationRF> estimationList = new ArrayList<>(estimations);
			estimationList.sort(new DateRangeComparator<>());

			// la première estimation maintenant annulée
			final EstimationRF estimation0 = estimationList.get(0);
			assertEquals(RegDate.get(1988, 1, 1), estimation0.getDateDebut());
			assertEquals(RegDate.get(2016, 7, 1), estimation0.getDateFin());
			assertEquals(Long.valueOf(260000L), estimation0.getMontant());
			assertEquals("RG93", estimation0.getReference());
			assertEquals(Integer.valueOf(1993), estimation0.getAnneeReference());
			assertNull(estimation0.getDateInscription());
			assertEquals(RegDate.get(1993, 1, 1), estimation0.getDateDebutMetier());
			assertEquals(RegDate.get(2016, 7, 1), estimation0.getDateFinMetier());
			assertFalse(estimation0.isEnRevision());
			assertTrue(estimation0.isAnnule());

			// la nouvelle estimation qui doit être la seule valide
			final EstimationRF estimation1 = estimationList.get(1);
			assertEquals(RegDate.get(2016, 10, 1), estimation1.getDateDebut());
			assertNull(estimation1.getDateFin());
			assertEquals(Long.valueOf(260000L), estimation1.getMontant());
			assertEquals("RG93", estimation1.getReference());
			assertEquals(Integer.valueOf(1993), estimation1.getAnneeReference());
			assertNull(estimation1.getDateInscription());
			assertEquals(RegDate.get(1993, 1, 1), estimation1.getDateDebutMetier());
			assertNull(estimation1.getDateFinMetier());
			assertFalse(estimation1.isEnRevision());
			assertFalse(estimation1.isAnnule());
			return null;
		});

		// postcondition : les événements fiscaux a bien été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(2, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getId));

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.DEBUT_ESTIMATION, event0.getType());
			assertEquals(RegDate.get(1993, 1, 1), event0.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event0.getImmeuble().getIdRF());

			final EvenementFiscalImmeuble event1 = (EvenementFiscalImmeuble) events.get(1);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.ANNULATION_ESTIMATION, event1.getType());
			assertEquals(RegDate.get(2016, 10, 1), event1.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event1.getImmeuble().getIdRF());

			return null;
		});
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de modification de la surface totale met bien à jour l'immeuble existant dans la DB
	 */
	@Test
	public void testProcessMutationModificationSurfaceTotale() throws Exception {

		// précondition : il y a déjà un immeuble dans la base
		doInNewTransaction(status -> {

			CommuneRF commune = new CommuneRF();
			commune.setNoRf(294);
			commune.setNomRf("Pétahouchnok");
			commune.setNoOfs(66666);
			commune = communeRFDAO.save(commune);

			final BienFondsRF bienFonds = new BienFondsRF();
			bienFonds.setIdRF("_1f109152381026b501381028a73d1852");
			bienFonds.setEgrid("CH938391457759");
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
			surfaceTotale.setSurface(532);
			bienFonds.addSurfaceTotale(surfaceTotale);

			immeubleRFDAO.save(bienFonds);
			assertEquals(1, immeubleRFDAO.getAll().size());
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et l'immeuble est créé en base
		doInNewTransaction(status -> {

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
			return null;
		});

		// postcondition : les événements fiscaux a bien été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getId));

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_SURFACE_TOTALE, event0.getType());
			assertEquals(RegDate.get(2016, 10, 1), event0.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event0.getImmeuble().getIdRF());

			return null;
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
		doInNewTransaction(status -> {

			CommuneRF commune = new CommuneRF();
			commune.setNoRf(294);
			commune.setNomRf("Pétahouchnok");
			commune.setNoOfs(66666);
			commune = communeRFDAO.save(commune);

			final BienFondsRF bienFonds = new BienFondsRF();
			bienFonds.setIdRF("_1f109152381026b501381028a73d1852");
			bienFonds.setEgrid("CH938391457759");
			bienFonds.setCfa(false);
			bienFonds.setDateRadiation(dateRadiation);   // <---- immeuble radié

			final SituationRF situation = new SituationRF();
			situation.setDateDebut(RegDate.get(1988, 1, 1));
			situation.setDateFin(dateRadiation);
			situation.setCommune(commune);
			situation.setNoParcelle(5089);
			bienFonds.addSituation(situation);

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

		// postcondition : la mutation est traitée et l'immeuble n'est plus radié dans la base
		doInNewTransaction(status -> {

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
			return null;
		});

		// postcondition : les événements fiscaux a bien été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getId));

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.REACTIVATION, event0.getType());
			assertEquals(RegDate.get(2016, 10, 1), event0.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event0.getImmeuble().getIdRF());

			return null;
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

			final BienFondsRF bienFonds = new BienFondsRF();
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

		// postcondition : les événements fiscaux a bien été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getId));

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_EGRID, event0.getType());
			assertEquals(RegDate.get(2016, 10, 1), event0.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event0.getImmeuble().getIdRF());

			return null;
		});
	}

	/**
	 * Ce test vérifie que le processing d'une mutation de modification d'un immeuble sans estimation fiscale fonctionne bien.
	 */
	@Test
	public void testProcessMutationModificationImmeubleSansEstimationFiscale() throws Exception {

		// précondition : il y a déjà un immeuble dans la base (celui-ci possède une estimation fiscale)
		doInNewTransaction(status -> {

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
			return null;
		});

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf_sans_estimation_fiscale.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_8af80e62567f816f01571d91f3e56a38", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée, l'immeuble est créé en base et son ancienne estimation fiscale est fermée
		doInNewTransaction(status -> {

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
			return null;
		});

		// postcondition : les événements fiscaux a bien été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getId));

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.FIN_ESTIMATION, event0.getType());
			assertEquals(RegDate.get(2016, 9, 30), event0.getDateValeur());
			assertEquals("_8af80e62567f816f01571d91f3e56a38", event0.getImmeuble().getIdRF());

			return null;
		});
	}

	/**
	 * [SIFISC-24715] Ce test vérifie que le processing d'une mutation de modification de la quote-part d'un immeuble met bien à jour l'historique des quotes-parts.
	 */
	@Test
	public void testProcessMutationModificationQuotePartImmeuble() throws Exception {

		// précondition : il y a déjà un immeuble avec quote-part dans la base
		doInNewTransaction(status -> {

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
			return null;
		});

		// le même immeuble mais avec une quote-part différente
		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/processor/mutation_immeuble_rf_sans_estimation_fiscale.xml");
		final String xml = FileUtils.readFileToString(file, "UTF-8");

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(xml, RegDate.get(2016, 10, 1), TypeEntiteRF.IMMEUBLE, TypeMutationRF.MODIFICATION, "_8af80e62567f816f01571d91f3e56a38", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée, l'ancienne quote-part est fermée et une nouvelle est créée
		doInNewTransaction(status -> {

			final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
			assertEquals(1, immeubles.size());

			final ImmeubleRF immeuble0 = immeubles.get(0);
			assertEquals("_8af80e62567f816f01571d91f3e56a38", immeuble0.getIdRF());
			assertEquals("CH776584246539", immeuble0.getEgrid());

			final ProprieteParEtageRF ppe = (ProprieteParEtageRF) immeuble0;
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
			return null;
		});

		// postcondition : les événements fiscaux a bien été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getId));

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_QUOTE_PART, event0.getType());
			assertEquals(RegDate.get(2016, 10, 1), event0.getDateValeur());
			assertEquals("_8af80e62567f816f01571d91f3e56a38", event0.getImmeuble().getIdRF());

			return null;
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
		doInNewTransaction(status -> {

			CommuneRF commune = new CommuneRF();
			commune.setNoRf(294);
			commune.setNomRf("Pétahouchnok");
			commune.setNoOfs(66666);
			commune = communeRFDAO.save(commune);

			final BienFondsRF bienFonds = new BienFondsRF();
			bienFonds.setIdRF("_1f109152381026b501381028a73d1852");
			bienFonds.setEgrid("CH938391457759");
			bienFonds.setCfa(false);

			final SituationRF situation = new SituationRF();
			situation.setDateDebut(RegDate.get(1988, 1, 1));
			situation.setDateFin(veilleImport); // on pré-renseigne la date de fermeture pour que la validation de l'immeuble passe (on ne veut tester que la mutation sur l'immeuble)
			situation.setCommune(commune);
			situation.setNoParcelle(5089);
			bienFonds.addSituation(situation);

			final EstimationRF estimation = new EstimationRF();
			estimation.setDateDebut(RegDate.get(1988, 1, 1));
			estimation.setDateFin(veilleImport);
			estimation.setMontant(260000L);
			estimation.setReference("RG93");
			estimation.setAnneeReference(1993);
			estimation.setEnRevision(false);
			bienFonds.addEstimation(estimation);

			final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
			surfaceTotale.setDateDebut(RegDate.get(1988, 1, 1));
			surfaceTotale.setDateFin(veilleImport);
			surfaceTotale.setSurface(532);
			bienFonds.addSurfaceTotale(surfaceTotale);

			immeubleRFDAO.save(bienFonds);
			assertEquals(1, immeubleRFDAO.getAll().size());
			return null;
		});

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(null, dateImport, TypeEntiteRF.IMMEUBLE, TypeMutationRF.SUPPRESSION, "_1f109152381026b501381028a73d1852", null);

		// on process la mutation
		doInNewTransaction(status -> {
			final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
			processor.process(mutation, false, null);
			return null;
		});

		// postcondition : la mutation est traitée et l'immeuble est radié
		doInNewTransaction(status -> {

			final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
			assertEquals(1, immeubles.size());

			// l'immeuble est radié
			final ImmeubleRF immeuble0 = immeubles.get(0);
			assertEquals("_1f109152381026b501381028a73d1852", immeuble0.getIdRF());
			assertEquals("CH938391457759", immeuble0.getEgrid());
			assertEquals(veilleImport, immeuble0.getDateRadiation());
			return null;
		});

		// postcondition : les événements fiscaux a bien été envoyés
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			assertEquals(1, events.size());
			events.sort(Comparator.comparing(EvenementFiscal::getId));

			final EvenementFiscalImmeuble event0 = (EvenementFiscalImmeuble) events.get(0);
			assertEquals(EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.RADIATION, event0.getType());
			assertEquals(RegDate.get(2016, 10, 1), event0.getDateValeur());
			assertEquals("_1f109152381026b501381028a73d1852", event0.getImmeuble().getIdRF());

			return null;
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

	/**
	 * [SIFISC-24968] Ce test vérifie que le processing de suppression d'un immeuble ferme bien les droits rattachés à cet immeuble.
	 * [SIFISC-26635] Ce test vérifie que le processing de suppression d'un immeuble ferme bien les servitudes rattachés à cet immeuble.
	 */
	@Test
	public void testProcessMutationSuppressionImmeuble() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);
		final RegDate veilleImport = dateImport.getOneDayBefore();

		// précondition : il y a déjà un immeuble dans la base avec des droits et des servitudes qui pointent vers lui
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				CommuneRF commune = new CommuneRF();
				commune.setNoRf(13);
				commune.setNomRf("Pétahouchnok");
				commune.setNoOfs(66666);
				commune = communeRFDAO.save(commune);

				ProprieteParEtageRF ppe = new ProprieteParEtageRF();
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

				ppe = (ProprieteParEtageRF) immeubleRFDAO.save(ppe);
				assertEquals(1, immeubleRFDAO.getAll().size());

				PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
				pp.setIdRF("3893728273382823");
				pp.setNom("Schulz");
				pp.setPrenom("Alodie");
				pp.setDateNaissance(RegDate.get(1900, 1, 1));
				pp = (PersonnePhysiqueRF) ayantDroitRFDAO.save(pp);

				PersonneMoraleRF pm = new PersonneMoraleRF();
				pm.setIdRF("48349384890202");
				pm.setNoRF(3727);
				pm.setNoContribuable(827288022L);
				pm.setRaisonSociale("Raison sociale");
				pm = (PersonneMoraleRF) ayantDroitRFDAO.save(pm);

				PersonnePhysiqueRF beneficiaire = new PersonnePhysiqueRF();
				beneficiaire.setIdRF("9292871781");
				beneficiaire.setNom("Schulz");
				beneficiaire.setPrenom("Jean-Marc");
				beneficiaire.setDateNaissance(RegDate.get(1900, 1, 1));
				beneficiaire = (PersonnePhysiqueRF) ayantDroitRFDAO.save(beneficiaire);

				DroitProprietePersonnePhysiqueRF droitPP = new DroitProprietePersonnePhysiqueRF();
				droitPP.setMasterIdRF("1f109152381009be0138100c87276e68");
				droitPP.setVersionIdRF("1f109152381009be0138100e4c7c00e5");
				droitPP.setDateDebut(RegDate.get(2005, 2, 12));
				droitPP.setDateDebutMetier(RegDate.get(2005, 1, 1));
				droitPP.setMotifDebut("Achat");
				droitPP.setAyantDroit(pp);
				droitPP.setImmeuble(ppe);
				droitPP.setPart(new Fraction(1, 2));
				droitPP.setRegime(GenrePropriete.COPROPRIETE);
				droitPP.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 1, 1), "Achat", new IdentifiantAffaireRF(13, 2005, 173, 0)));
				droitRFDAO.save(droitPP);

				DroitProprietePersonneMoraleRF droitPM = new DroitProprietePersonneMoraleRF();
				droitPM.setMasterIdRF("9a9c9e94923");
				droitPM.setVersionIdRF("1");
				droitPM.setAyantDroit(pm);
				droitPM.setImmeuble(ppe);
				droitPM.setCommunaute(null);
				droitPM.setDateDebut(RegDate.get(2010, 6, 1));
				droitPM.setDateFin(null);
				droitPM.setDateDebutMetier(RegDate.get(2010, 4, 23));
				droitPM.setMotifDebut("Achat");
				droitPM.setMotifFin(null);
				droitPM.setPart(new Fraction(1, 2));
				droitPM.setRegime(GenrePropriete.COPROPRIETE);
				droitPM.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 23), "Achat", new IdentifiantAffaireRF(6, 2010, 120, 3)));
				droitRFDAO.save(droitPM);

				UsufruitRF usufruit = new UsufruitRF();
				usufruit.setMasterIdRF("38388232");
				usufruit.setVersionIdRF("1");
				usufruit.addAyantDroit(beneficiaire);
				usufruit.addImmeuble(ppe);
				usufruit.setDateDebut(RegDate.get(2010, 6, 1));
				usufruit.setDateFin(null);
				usufruit.setDateDebutMetier(RegDate.get(2010, 4, 23));
				usufruit.setMotifDebut("Convention");
				usufruit.setMotifFin(null);
				usufruit.setIdentifiantDroit(new IdentifiantDroitRF(6, 2010, 22));
				usufruit.setNumeroAffaire(new IdentifiantAffaireRF(6, 2010, 232, 0));
				droitRFDAO.save(usufruit);
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

		// postcondition : la mutation est traitée, l'immeuble est radié et les droits sont fermés
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

				// les droits sont fermés
				final Set<DroitProprieteRF> droits = ppe.getDroitsPropriete();
				assertEquals(2, droits.size());
				final Iterator<DroitProprieteRF> iterator = droits.iterator();

				final DroitProprieteRF droit0 = iterator.next();
				assertEquals(veilleImport, droit0.getDateFinMetier());
				assertEquals("Radiation", droit0.getMotifFin());

				final DroitProprieteRF droit1 = iterator.next();
				assertEquals(veilleImport, droit1.getDateFinMetier());
				assertEquals("Radiation", droit1.getMotifFin());

				// l'usufruit est fermé
				final Set<ServitudeRF> servitudes = ppe.getServitudes();
				assertEquals(1, servitudes.size());

				final ServitudeRF servitude0 = servitudes.iterator().next();
				assertEquals(veilleImport, servitude0.getDateFinMetier());
				assertEquals("Radiation", servitude0.getMotifFin());
			}
		});
	}

	/**
	 * [SIFISC-26635] Ce test vérifie que le processing de suppression d'un immeuble ne ferme pas la servitude rattaché à cet immeuble si la servitude possède d'autres immeubles non-radiés.
	 */
	@Test
	public void testProcessMutationSuppressionImmeubleSurServitudeAvecPlusieursImmeubles() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);
		final RegDate veilleImport = dateImport.getOneDayBefore();

		// précondition : il y a déjà deux immeubles dans la base avec un servitude qui pointe vers eux
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				CommuneRF commune = new CommuneRF();
				commune.setNoRf(13);
				commune.setNomRf("Pétahouchnok");
				commune.setNoOfs(66666);
				commune = communeRFDAO.save(commune);

				ProprieteParEtageRF ppe1 = new ProprieteParEtageRF();
				ppe1.setIdRF("_8af80e62567f816f01571d91f3e56a38");
				ppe1.setEgrid("CH776584246539");
				ppe1.addQuotePart(new QuotePartRF(RegDate.get(1988, 1, 1), null, new Fraction(8, 1000)));

				final SituationRF situation1 = new SituationRF();
				situation1.setDateDebut(RegDate.get(1988, 1, 1));
				situation1.setCommune(commune);
				situation1.setNoParcelle(917);
				situation1.setIndex1(106);
				ppe1.addSituation(situation1);

				final EstimationRF estimation1 = new EstimationRF();
				estimation1.setDateDebut(RegDate.get(1988, 1, 1));
				estimation1.setMontant(240000L);
				estimation1.setReference("RG88");
				estimation1.setAnneeReference(1988);
				estimation1.setDateDebutMetier(RegDate.get(1988, 1, 1));
				estimation1.setEnRevision(false);
				ppe1.addEstimation(estimation1);
				ppe1 = (ProprieteParEtageRF) immeubleRFDAO.save(ppe1);

				ProprieteParEtageRF ppe2 = new ProprieteParEtageRF();
				ppe2.setIdRF("_9498438932489");
				ppe2.setEgrid("CH776584246540");
				ppe2.addQuotePart(new QuotePartRF(RegDate.get(1988, 1, 1), null, new Fraction(8, 1000)));

				final SituationRF situation2 = new SituationRF();
				situation2.setDateDebut(RegDate.get(1988, 1, 1));
				situation2.setCommune(commune);
				situation2.setNoParcelle(917);
				situation2.setIndex1(107);
				ppe2.addSituation(situation2);

				final EstimationRF estimation2 = new EstimationRF();
				estimation2.setDateDebut(RegDate.get(1988, 1, 1));
				estimation2.setMontant(240000L);
				estimation2.setReference("RG88");
				estimation2.setAnneeReference(1988);
				estimation2.setDateDebutMetier(RegDate.get(1988, 1, 1));
				estimation2.setEnRevision(false);
				ppe2.addEstimation(estimation2);
				ppe2 = (ProprieteParEtageRF) immeubleRFDAO.save(ppe2);

				assertEquals(2, immeubleRFDAO.getAll().size());

				PersonnePhysiqueRF beneficiaire = new PersonnePhysiqueRF();
				beneficiaire.setIdRF("9292871781");
				beneficiaire.setNom("Schulz");
				beneficiaire.setPrenom("Jean-Marc");
				beneficiaire.setDateNaissance(RegDate.get(1900, 1, 1));
				beneficiaire = (PersonnePhysiqueRF) ayantDroitRFDAO.save(beneficiaire);

				UsufruitRF usufruit = new UsufruitRF();
				usufruit.setMasterIdRF("38388232");
				usufruit.setVersionIdRF("1");
				usufruit.addAyantDroit(beneficiaire);
				usufruit.addImmeuble(ppe1);
				usufruit.addImmeuble(ppe2);
				usufruit.setDateDebut(RegDate.get(2010, 6, 1));
				usufruit.setDateFin(null);
				usufruit.setDateDebutMetier(RegDate.get(2010, 4, 23));
				usufruit.setMotifDebut("Convention");
				usufruit.setMotifFin(null);
				usufruit.setIdentifiantDroit(new IdentifiantDroitRF(6, 2010, 22));
				usufruit.setNumeroAffaire(new IdentifiantAffaireRF(6, 2010, 232, 0));
				droitRFDAO.save(usufruit);
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

		// postcondition : la mutation est traitée, l'immeuble est radié mais la servitude n'est pas radiée car l'autre immeuble n'est pas radié
		doInNewTransaction(new TxCallbackWithoutResult() {

			@Override
			public void execute(TransactionStatus status) throws Exception {

				final List<ImmeubleRF> immeubles = immeubleRFDAO.getAll();
				assertEquals(2, immeubles.size());
				immeubles.sort(Comparator.comparing(ImmeubleRF::getIdRF));

				// le premier immeuble est radié
				final ProprieteParEtageRF ppe1 = (ProprieteParEtageRF) immeubles.get(0);
				assertEquals("_8af80e62567f816f01571d91f3e56a38", ppe1.getIdRF());
				assertEquals("CH776584246539", ppe1.getEgrid());
				assertEquals(veilleImport, ppe1.getDateRadiation());

				// le second immeuble n'est pas radié
				final ProprieteParEtageRF ppe2 = (ProprieteParEtageRF) immeubles.get(1);
				assertEquals("_9498438932489", ppe2.getIdRF());
				assertEquals("CH776584246540", ppe2.getEgrid());
				assertNull(ppe2.getDateRadiation());

				// l'usufruit n'est pas fermé
				final Set<ServitudeRF> servitudes = ppe1.getServitudes();
				assertEquals(1, servitudes.size());

				final ServitudeRF servitude0 = servitudes.iterator().next();
				assertNull(servitude0.getDateFinMetier());
				assertNull(servitude0.getMotifFin());
			}
		});
	}

	/**
	 * [SIFISC-24595] Ce test vérifie que le processing de suppression d'un immeuble recalcule bien les communautés associées.
	 */
	@Test
	public void testProcessMutationSuppressionImmeubleAvecCommunaute() throws Exception {

		final RegDate dateImport = RegDate.get(2016, 10, 1);
		final RegDate veilleImport = dateImport.getOneDayBefore();
		final String idRFImmeuble = "_8af80e62567f816f01571d91f3e56a38";
		final RegDate dateDebutCommunaute = date(2016, 5, 2);

		final class Ids {
			long idCommunaute;
			long idImmeuble;
		}
		final Ids ids = new Ids();

		// précondition : il y a un immeuble avec une communauté de deux personnes
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
				final BienFondsRF immeuble = addBienFondsRF(idRFImmeuble, "EGRID", commune, 4514, 4, 2, 1);

				// la personne 1
				final PersonnePhysique ctb1 = addNonHabitant("Francis", "Rouge", date(1975, 4, 2), Sexe.MASCULIN);
				final PersonnePhysiqueRF pp1 = addPersonnePhysiqueRF("6784t6gfsbnc", "Francis", "Rouge", date(1975, 4, 2));
				pp1.setNoRF(223L);
				addRapprochementRF(ctb1, pp1, null, null, TypeRapprochementRF.AUTO);

				// la personne 2
				final PersonnePhysique ctb2 = addNonHabitant("Albertine", "Zorro", date(1979, 6, 1), Sexe.FEMININ);
				final PersonnePhysiqueRF pp2 = addPersonnePhysiqueRF("5w47tgtflbsfg", "Albertine", "Zorro", date(1979, 6, 1));
				pp2.setNoRF(554L);
				addRapprochementRF(ctb2, pp2, null, null, TypeRapprochementRF.AUTO);

				// la communauté
				final CommunauteRF communaute = addCommunauteRF("285t378og43t", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
				final IdentifiantAffaireRF affaire = new IdentifiantAffaireRF(213, "5823g");
				addDroitPersonnePhysiqueRF(null, dateDebutCommunaute, null, null, "Achat", null, "3458wgfs", "3458wgfr", affaire, new Fraction(1, 1), GenrePropriete.COMMUNE, pp1, immeuble, communaute);
				addDroitPersonnePhysiqueRF(null, dateDebutCommunaute, null, null, "Un motif, quoi...", null, "5378tgzufbs", "5378tgzufbr", affaire, new Fraction(1, 1), GenrePropriete.COMMUNE, pp2, immeuble, communaute);
				addDroitCommunauteRF(null, dateDebutCommunaute, null, null, "Succession", null, "478tgsbFB", "478tgsbFA", affaire, new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, communaute, immeuble);

				final ModeleCommunauteRF modele = addModeleCommunauteRF(pp1, pp2);
				addRegroupementRF(communaute, modele, dateDebutCommunaute, null);

				ids.idCommunaute = communaute.getId();
				ids.idImmeuble = immeuble.getId();
			}
		});

		// on insère la mutation dans la base
		final Long mutationId = insertMutation(null, dateImport, TypeEntiteRF.IMMEUBLE, TypeMutationRF.SUPPRESSION, idRFImmeuble, null);

		// on process la mutation
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final EvenementRFMutation mutation = evenementRFMutationDAO.get(mutationId);
				processor.process(mutation, false, null);
			}
		});

		// postcondition : la mutation est traitée, l'immeuble est radié et les regroupements de la communauté sont aussi fermés
		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				// l'immeuble est radié
				final BienFondsRF bienFonds = (BienFondsRF) immeubleRFDAO.get(ids.idImmeuble);
				assertNotNull(bienFonds);
				assertEquals(idRFImmeuble, bienFonds.getIdRF());
				assertEquals("EGRID", bienFonds.getEgrid());
				assertEquals(veilleImport, bienFonds.getDateRadiation());

				// la communauté est fermée
				final CommunauteRF communaute = (CommunauteRF) ayantDroitRFDAO.get(ids.idCommunaute);
				assertNotNull(communaute);
				final Set<RegroupementCommunauteRF> regroupements = communaute.getRegroupements();
				assertEquals(1, regroupements.size());
				final RegroupementCommunauteRF regroupement0 = regroupements.iterator().next();
				assertEquals(dateDebutCommunaute, regroupement0.getDateDebut());
				assertEquals(veilleImport, regroupement0.getDateFin());
			}
		});
	}
}