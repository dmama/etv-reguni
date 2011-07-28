package ch.vd.uniregctb.tiers.rattrapage.flaghabitant;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

public class CorrectionFlagHabitantProcessorTest extends BusinessTest {

	public static final Logger LOGGER = Logger.getLogger(CorrectionFlagHabitantProcessorTest.class);

	private static final long INDIVIDU = 1234;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(INDIVIDU, RegDate.get(1980, 10, 25), "Tartempion", "Toto-Individu", true);
			}
		});
	}

	private static enum TypePersonnePhysique {
		HABITANT,
		NON_HABITANT_AVEC_NO_INDIVIDU,
		NON_HABITANT_SANS_NO_INDIVIDU
	}

	private Long createPersonnePhysique(final TypePersonnePhysique typePP, final boolean forVaudois) throws Exception {
		return doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp;
				if (typePP == TypePersonnePhysique.HABITANT) {
					pp = addHabitant(INDIVIDU);
				}
				else {
					pp = addNonHabitant("Toto-nonIndividu", "Tartempion", RegDate.get(1980, 4, 3), Sexe.MASCULIN);
					if (typePP == TypePersonnePhysique.NON_HABITANT_AVEC_NO_INDIVIDU) {
						pp.setNumeroIndividu(INDIVIDU);
					}
				}
				if (forVaudois) {
					addForPrincipal(pp, RegDate.get(2008, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Bex);
				}
				else {
					addForPrincipal(pp, RegDate.get(2008, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
				}
				return pp.getNumero();
			}
		});
	}

	private CorrectionFlagHabitantSurPersonnesPhysiquesResults runProcessorPersonnesPhysiques(int nbThreads) {
		final StatusManager statusManager = new LoggingStatusManager(LOGGER);
		final CorrectionFlagHabitantProcessor processor = new CorrectionFlagHabitantProcessor(hibernateTemplate, tiersService, transactionManager, statusManager);
		return processor.corrigeFlagSurPersonnesPhysiques(nbThreads);
	}

	private void checkResultsPersonnesPhysiques(CorrectionFlagHabitantSurPersonnesPhysiquesResults results, long id, boolean nouvelHabitant, boolean nouveauNonHabitant) throws Exception {
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.getErreurs());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertNotNull(results.getNouveauxHabitants());
		Assert.assertNotNull(results.getNouveauxNonHabitants());
		if (nouvelHabitant || nouveauNonHabitant) {
			if (nouvelHabitant) {
				Assert.assertEquals(1, results.getNouveauxHabitants().size());

				final CorrectionFlagHabitantAbstractResults.ContribuableInfo info = results.getNouveauxHabitants().get(0);
				Assert.assertEquals("Mauvais contribuable ?", id, info.getNoCtb());
			}
			else {
				Assert.assertEquals(1, results.getNouveauxNonHabitants().size());

				final CorrectionFlagHabitantAbstractResults.ContribuableInfo info = results.getNouveauxNonHabitants().get(0);
				Assert.assertEquals("Mauvais contribuable ?", id, info.getNoCtb());
			}
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(id);
			Assert.assertEquals(nouvelHabitant, pp.isHabitantVD());
		}
		else {
			// ignoré !
			Assert.assertEquals(0, results.getNouveauxHabitants().size());
			Assert.assertEquals(0, results.getNouveauxNonHabitants().size());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHabitantForVaudois() throws Exception {
		final long id = createPersonnePhysique(TypePersonnePhysique.HABITANT, true);
		final CorrectionFlagHabitantSurPersonnesPhysiquesResults results = runProcessorPersonnesPhysiques(1);

		// ne doit pas avoir bougé!
		checkResultsPersonnesPhysiques(results, id, false, false);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHabitantForNonVaudois() throws Exception {
		final long id = createPersonnePhysique(TypePersonnePhysique.HABITANT, false);
		final CorrectionFlagHabitantSurPersonnesPhysiquesResults results = runProcessorPersonnesPhysiques(1);

		// devenu non-habitant !
		checkResultsPersonnesPhysiques(results, id, false, true);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNonHabitantAncienHabitantForVaudois() throws Exception {
		final long id = createPersonnePhysique(TypePersonnePhysique.NON_HABITANT_AVEC_NO_INDIVIDU, true);
		final CorrectionFlagHabitantSurPersonnesPhysiquesResults results = runProcessorPersonnesPhysiques(1);

		// devenu habitant !
		checkResultsPersonnesPhysiques(results, id, true, false);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNonHabitantInconnuForVaudois() throws Exception {
		final long id = createPersonnePhysique(TypePersonnePhysique.NON_HABITANT_SANS_NO_INDIVIDU, true);
		final CorrectionFlagHabitantSurPersonnesPhysiquesResults results = runProcessorPersonnesPhysiques(1);

		// pas devenu individu car numéro d'individu inconnu -> erreur
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.getErreurs());
		Assert.assertNotNull(results.getNouveauxHabitants());
		Assert.assertNotNull(results.getNouveauxNonHabitants());

		Assert.assertEquals(1, results.getErreurs().size());
		Assert.assertEquals(0, results.getNouveauxHabitants().size());
		Assert.assertEquals(0, results.getNouveauxNonHabitants().size());

		final CorrectionFlagHabitantAbstractResults.ContribuableErreur erreur = results.getErreurs().get(0);
		Assert.assertEquals("Mauvais contribuable ?", id, erreur.getNoCtb());
		Assert.assertEquals(CorrectionFlagHabitantAbstractResults.Message.PP_NON_HABITANT_SANS_NUMERO_INDIVIDU, erreur.getMessage());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNonHabitantAncienHabitantForNonVaudois() throws Exception {
		final long id = createPersonnePhysique(TypePersonnePhysique.NON_HABITANT_SANS_NO_INDIVIDU, false);
		final CorrectionFlagHabitantSurPersonnesPhysiquesResults results = runProcessorPersonnesPhysiques(1);

		// pas changé !
		checkResultsPersonnesPhysiques(results, id, false, false);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNonHabitantInconnuForNonVaudois() throws Exception {
		final long id = createPersonnePhysique(TypePersonnePhysique.NON_HABITANT_AVEC_NO_INDIVIDU, false);
		final CorrectionFlagHabitantSurPersonnesPhysiquesResults results = runProcessorPersonnesPhysiques(1);

		// pas changé !
		checkResultsPersonnesPhysiques(results, id, false, false);
	}

}
