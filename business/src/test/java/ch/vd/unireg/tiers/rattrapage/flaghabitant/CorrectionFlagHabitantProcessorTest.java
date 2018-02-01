package ch.vd.uniregctb.tiers.rattrapage.flaghabitant;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;

/**
 * Test du processeur de correction des flag habitant.
 * <p/>
 * <b>Note:</b> les tests <i>métier</i> de la fonctionnalité de correction du flag sont faits dans la classe {@link ch.vd.uniregctb.tiers.TiersServiceTest2}.
 */
public class CorrectionFlagHabitantProcessorTest extends BusinessTest {

	public static final Logger LOGGER = LoggerFactory.getLogger(CorrectionFlagHabitantProcessorTest.class);

	private AdresseService adresseService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		adresseService = getBean(AdresseService.class, "adresseService");
	}

	private CorrectionFlagHabitantResults runProcessorPersonnesPhysiques(int nbThreads) {
		final StatusManager statusManager = new LoggingStatusManager(LOGGER);
		final CorrectionFlagHabitantProcessor processor = new CorrectionFlagHabitantProcessor(hibernateTemplate, tiersService, transactionManager, statusManager, adresseService);
		return processor.corrigeFlagSurPersonnesPhysiques(nbThreads);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNonHabitantSansAdresseResidence() throws Exception {

		final long noIndividu = 1234L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1970, 1, 1), "Marcel", "Dubouchelard", Sexe.MASCULIN);
			}
		});

		doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noIndividu);
				pp.setNom("Dubouchelard");
				pp.setHabitant(false);
				return pp.getNumero();
			}
		});

		assertAucunChangement(runProcessorPersonnesPhysiques(1));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHabitantSansAdresseResidence() throws Exception {

		final long noIndividu = 1234L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, date(1970, 1, 1), "Marcel", "Dubouchelard", Sexe.MASCULIN);
			}
		});

		final long noPP = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				return addHabitant(noIndividu).getNumero();
			}
		});

		assertUnNouveauNonHabitant(runProcessorPersonnesPhysiques(1), noPP);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNonHabitantAvecAdresseResidence() throws Exception {

		final long noIndividu = 1234L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu ind = addIndividu(noIndividu, date(1970, 1, 1), "Marcel", "Dubouchelard", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, date(1970, 1, 1), null);
			}
		});

		final long noPP = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noIndividu);
				pp.setHabitant(false);
				pp.setNom("Dubouchelard");
				return pp.getNumero();
			}
		});

		assertUnNouveauHabitant(runProcessorPersonnesPhysiques(1), noPP);
	}

	private static void assertAucunChangement(CorrectionFlagHabitantResults results) {
		assertEquals(1, results.getNombrePPInspectees());
		assertEquals(0, results.getNombrePersonnesPhysiquesModifiees());
		assertEmpty(results.getErreurs());
		assertEmpty(results.getNouveauxHabitants());
		assertEmpty(results.getNouveauxNonHabitants());
	}

	private static void assertUnNouveauHabitant(CorrectionFlagHabitantResults results, long noPP) {
		assertEquals(1, results.getNombrePPInspectees());
		assertEquals(1, results.getNombrePersonnesPhysiquesModifiees());
		assertEmpty(results.getErreurs());
		assertEmpty(results.getNouveauxNonHabitants());

		final List<CorrectionFlagHabitantResults.ContribuableInfo> nouveaux = results.getNouveauxHabitants();
		assertEquals(1, nouveaux.size());
		assertEquals(noPP, nouveaux.get(0).getNoCtb());
	}

	private static void assertUnNouveauNonHabitant(CorrectionFlagHabitantResults results, long noPP) {
		assertEquals(1, results.getNombrePPInspectees());
		assertEquals(1, results.getNombrePersonnesPhysiquesModifiees());
		assertEmpty(results.getErreurs());
		assertEmpty(results.getNouveauxHabitants());

		final List<CorrectionFlagHabitantResults.ContribuableInfo> nouveaux = results.getNouveauxNonHabitants();
		assertEquals(1, nouveaux.size());
		assertEquals(noPP, nouveaux.get(0).getNoCtb());
	}
}
