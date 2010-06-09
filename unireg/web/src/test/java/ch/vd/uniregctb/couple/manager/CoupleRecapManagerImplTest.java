package ch.vd.uniregctb.couple.manager;

import java.util.Date;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.couple.view.CoupleRecapView;
import ch.vd.uniregctb.couple.view.TypeUnion;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@ContextConfiguration(locations = {
		"classpath:ch/vd/uniregctb/couple/manager/config.xml"
})
public class CoupleRecapManagerImplTest extends BusinessTest {

	private CoupleRecapManager mngr;
	private Validator validator;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tiersService = getBean(TiersService.class, "tiersService");
		validator = getBean(Validator.class, "coupleRecapValidator");
		mngr = getBean(CoupleRecapManager.class, "coupleRecapManager");
	}

	// [UNIREG-1521]
	@Test
	public void testTranformationNHEnMenageCommun() throws Exception {

		// Création de trois personnes physiques dont une représente en fait un ménage commun
		final int noIndArnold = 829837;
		final int noIndJanine = 829838;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu arnold = addIndividu(noIndArnold, date(1970, 1, 1), "Arnold", "Simon", true);
				addNationalite(arnold, MockPays.Suisse, date(1970, 1, 1), null, 1);
				MockIndividu janine = addIndividu(noIndJanine, date(1970, 1, 1), "Janine", "Simon", false);
				addNationalite(janine, MockPays.Suisse, date(1970, 1, 1), null, 1);
			}
		});

		class Ids {
			long arnold;
			long janine;
			long menage;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback(){
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique arnold = addHabitant(noIndArnold);
				ids.arnold = arnold.getNumero();
				PersonnePhysique janine = addHabitant(noIndJanine);
				ids.janine = janine.getNumero();
				PersonnePhysique menage = addNonHabitant("Arnold", "Simon", date(1970, 1, 1), Sexe.MASCULIN);
				ids.menage = menage.getNumero();
				return null;
			}
		});

		// Regroupement des trois personnes physiques en un ménage, avec transformation en ménage commun d'une des personnes physiques
		doInNewTransactionAndSession(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				TiersGeneralView viewTiers1 = new TiersGeneralView();
				viewTiers1.setNumero(ids.arnold);
				TiersGeneralView viewTiers2 = new TiersGeneralView();
				viewTiers2.setNumero(ids.janine);
				TiersGeneralView viewMC = new TiersGeneralView();
				viewMC.setNumero(ids.menage);

				CoupleRecapView view = new CoupleRecapView();

				view.setDateCoupleExistant(RegDate.get());
				view.setDateDebut(new Date());
				view.setNouveauCtb(false);
				view.setPremierePersonne(viewTiers1);
				view.setSecondePersonne(viewTiers2);
				view.setTroisiemeTiers(viewMC);
				view.setTypeUnion(TypeUnion.COUPLE);

				tiersService.getTiers(viewMC.getNumero());
				mngr.save(view);
				return null;
			}
		});

		// On s'assure que le ménage commun est bien de la bonne classe et qu'il est bien composé
		final Tiers mc = (Tiers) hibernateTemplate.get(Tiers.class, ids.menage);
		assertNotNull(mc);
		assertEquals(MenageCommun.class, mc.getClass());
		assertEquals(2, mc.getRapportsObjet().size()); // possède bien deux parties

		final Tiers arnold = (Tiers) hibernateTemplate.get(Tiers.class, ids.arnold);
		assertNotNull(arnold);
		assertEquals(PersonnePhysique.class, arnold.getClass());
		assertEquals(1, arnold.getRapportsSujet().size()); // fait partie du ménage commun

		final Tiers janine = (Tiers) hibernateTemplate.get(Tiers.class, ids.janine);
		assertNotNull(janine);
		assertEquals(PersonnePhysique.class, janine.getClass());
		assertEquals(1, janine.getRapportsSujet().size()); // fait partie du ménage commun
	}

	// [UNIREG-1521] Teste que toutes les opérations sont bien rollées-back en cas d'erreur de validation
	@Test
	@NotTransactional
	public void testTranformationNHEnMenageCommunEtRollback() throws Exception {

		// Création de trois personnes physiques dont une représente en fait un ménage commun
		final int noIndArnold = 829837;
		final int noIndJanine = 829838;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu arnold = addIndividu(noIndArnold, date(1970, 1, 1), "Arnold", "Simon", true);
				addNationalite(arnold, MockPays.Suisse, date(1970, 1, 1), null, 1);
				MockIndividu janine = addIndividu(noIndJanine, date(1970, 1, 1), "Janine", "Simon", false);
				addNationalite(janine, MockPays.Suisse, date(1970, 1, 1), null, 1);
			}
		});
		
		class Ids {
			long arnold;
			long janine;
			long menage;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback(){
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique arnold = addHabitant(noIndArnold);
				ids.arnold = arnold.getNumero();
				PersonnePhysique janine = addHabitant(noIndJanine);
				ids.janine = janine.getNumero();
				PersonnePhysique menage = addNonHabitant("Arnold", "Simon", date(1970, 1, 1), Sexe.MASCULIN);
				addForPrincipal(menage, date(1988,1,1), MotifFor.MAJORITE, MockCommune.LAbbaye);
				ids.menage = menage.getNumero();
				return null;
			}
		});

		// Essai de regroupement des trois personnes physiques en un ménage, avec transformation
		// en ménage commun d'une des personnes physiques + erreur de validation sur le ménage-commun résultant
		try {
			doInNewTransactionAndSession(new TxCallback() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {

					TiersGeneralView viewTiers1 = new TiersGeneralView();
					viewTiers1.setNumero(ids.arnold);
					TiersGeneralView viewTiers2 = new TiersGeneralView();
					viewTiers2.setNumero(ids.janine);
					TiersGeneralView viewMC = new TiersGeneralView();
					viewMC.setNumero(ids.menage);

					CoupleRecapView view = new CoupleRecapView();

					view.setDateCoupleExistant(RegDate.get());
					view.setDateDebut(new Date());
					view.setNouveauCtb(false);
					view.setPremierePersonne(viewTiers1);
					view.setSecondePersonne(viewTiers2);
					view.setTroisiemeTiers(viewMC);
					view.setTypeUnion(TypeUnion.COUPLE);

					tiersService.getTiers(viewMC.getNumero());
					mngr.save(view);
					return null;
				}
			});
			fail("Le ménage-commun ne devrait pas valider à cause de son for principal qui débute avant la date du jour.");
		}
		catch (Exception e) {
			assertEquals("ch.vd.registre.base.validation.ValidationException: MenageCommun #" + ids.menage + " - 1 erreur(s) - 0 warning(s):\n" +
					" [E] Le for fiscal [ForFiscalPrincipal (01.01.1988 - ?)] ne peut pas exister en dehors de la période de validité du ménage-commun numéro [" + ids.menage + "]\n", e.getMessage());
		}

		// On s'assure que la transaction a bien été rollée-back, c'est-à-dire que le trois tiers sont toujours des personnes physiques.
		doInNewTransactionAndSession(new TxCallback(){
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Tiers arnold = (Tiers) hibernateTemplate.get(Tiers.class, ids.arnold);
				assertNotNull(arnold);
				assertEquals(PersonnePhysique.class, arnold.getClass());
				assertEmpty(arnold.getRapportsSujet()); // pas associé à un ménage
				assertEmpty(arnold.getRapportsObjet());

				final Tiers janine = (Tiers) hibernateTemplate.get(Tiers.class, ids.janine);
				assertNotNull(janine);
				assertEquals(PersonnePhysique.class, janine.getClass());
				assertEmpty(janine.getRapportsSujet()); // pas associé à un ménage
				assertEmpty(janine.getRapportsObjet());

				final Tiers mc = (Tiers) hibernateTemplate.get(Tiers.class, ids.menage);
				assertNotNull(mc);
				assertEquals(PersonnePhysique.class, mc.getClass());
				assertEmpty(mc.getRapportsSujet()); // pas associé à un ménage
				assertEmpty(mc.getRapportsObjet());
				return null;
			}
		});
	}

	@Test
	public void testReconciliationDeSeparesAuCivilMariageInconnuAuFiscal() throws Exception {

		final long noMr = 1234567L;
		final long noMme = 1234568L;
		final RegDate dateMariage = date(1971, 4, 17);
		final RegDate dateSeparation = date(2005, 10, 13);

		// civil
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu mr = addIndividu(noMr, date(1948, 1, 26), "Tartempion", "Robert", true);
				final MockIndividu mme = addIndividu(noMme, date(1948, 9, 4), "Tartempion", "Martine", false);
				marieIndividus(mr, mme, dateMariage);
				separeIndividus(mr, mme, dateSeparation);
			}
		});

		final class Ids {
			long noHabMr;
			long noHabMme;
		};
		final Ids ids = new Ids();

		// fiscal de départ
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique mr = addHabitant(noMr);
				final PersonnePhysique mme = addHabitant(noMme);
				addForPrincipal(mr, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
				addForPrincipal(mme, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Bussigny);
				ids.noHabMr = mr.getNumero();
				ids.noHabMme = mme.getNumero();
				return null;
			}
		});

		// re-création du couple
		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final TiersGeneralView viewTiers1 = new TiersGeneralView();
				viewTiers1.setNumero(ids.noHabMr);
				final TiersGeneralView viewTiers2 = new TiersGeneralView();
				viewTiers2.setNumero(ids.noHabMme);

				final CoupleRecapView view = new CoupleRecapView();
				view.setDateCoupleExistant(RegDate.get());
				view.setDateDebut(new Date());
				view.setNouveauCtb(true);
				view.setPremierePersonne(viewTiers1);
				view.setSecondePersonne(viewTiers2);
				view.setTypeUnion(TypeUnion.COUPLE);

				final Errors errors = new BeanPropertyBindingResult(view, "view");
				validator.validate(view, errors);
				assertEmpty(errors.getAllErrors());

				mngr.save(view);
				return null;
			}
		});
	}
}
