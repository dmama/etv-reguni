package ch.vd.uniregctb.tiers;

import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TiersServiceTest2 extends BusinessTest {

	private TiersService tiersService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tiersService = getBean(TiersService.class, "tiersService");
	}

	@Test
	@Transactional
	public void testUpdateHabitantFlag() throws Exception {

		final long noIndividu = 1234L;

		final Mutable<MockIndividu> holder = new MutableObject<>();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				holder.setValue(addIndividu(noIndividu, date(1970, 1, 1), "Marcel", "Dubouchelard", Sexe.MASCULIN));
			}
		});
		final PersonnePhysique pp = addHabitant(noIndividu);
		final MockIndividu individu = holder.getValue();

		// un individu sans adresse => non-habitant
		assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null));
		assertFalse(pp.isHabitantVD());

		// un individu à Lausanne en résidence principale => habitant
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), null));
		assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_HABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null));
		assertTrue(pp.isHabitantVD());

		// un individu à Lausanne en résidence secondaire => habitant
		individu.getAdresses().clear();
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), null));
		assertEquals(TiersService.UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT, tiersService.updateHabitantFlag(pp, noIndividu, null));
		assertTrue(pp.isHabitantVD());

		// un individu à Genève en résidence principale => non-habitant
		individu.getAdresses().clear();
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1970, 1, 1), null));
		assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null));
		assertFalse(pp.isHabitantVD());

		// un individu à Genève en résidence principale et à Lausanne en résidence secondaire => habitant
		individu.getAdresses().clear();
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1970, 1, 1), null));
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), null));
		assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_HABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null));
		assertTrue(pp.isHabitantVD());

		// un individu parti de Lausanne à destination vaudoise => non-habitant, car on ignore maintenant ([SIFISC-13741]) les destinations des adresses (qui ne sont plus sensées
		// être vaudoises pour la dernière adresse valide...)
		{
			individu.getAdresses().clear();
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Morges.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null));
			assertFalse(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination hors-canton => nonhabitant
		{
			pp.setHabitant(true);
			individu.getAdresses().clear();
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Neuchatel.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null));
			assertFalse(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination hors-Suisse => nonhabitant
		{
			individu.getAdresses().clear();
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT, tiersService.updateHabitantFlag(pp, noIndividu, null));
			assertFalse(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination vaudoise (variante en résidence secondaire) => non-habitant car les destinations des résidences secondaires sont maintenant ignorées
		{
			individu.getAdresses().clear();
			individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1970, 1, 1), null));
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Morges.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT, tiersService.updateHabitantFlag(pp, noIndividu, null));
			assertFalse(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination hors-canton (variante en résidence secondaire) => nonhabitant
		{
			individu.getAdresses().clear();
			individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1970, 1, 1), null));
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Neuchatel.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT, tiersService.updateHabitantFlag(pp, noIndividu, null));
			assertFalse(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination hors-Suisse (variante en résidence secondaire) => nonhabitant
		{
			individu.getAdresses().clear();
			individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1970, 1, 1), null));
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT, tiersService.updateHabitantFlag(pp, noIndividu, null));
			assertFalse(pp.isHabitantVD());
		}
	}

	/**
	 * [SIFISC-7954] Vérifie que la mise-à-jour simultanée du motif de fermeture et de l'autorité fiscale d'un for fiscal principale est bien prise en compte.
	 */
	@Test
	public void testUpdateForPrincipalMotifEtAutoriteFiscaleEnMemeTemps() throws Exception {

		final Long ppId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Jean", "Dufoot", date(1956, 3, 2), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(1976, 3, 2), MotifFor.MAJORITE, date(2003, 5, 31), MotifFor.FUSION_COMMUNES, MockCommune.Vevey);

				// on met-à-jour à la fois le motif de fermeture et l'autorité fiscale
				tiersService.updateForPrincipal(ffp, date(2003, 5, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne.getNoOFS());
				return pp.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final List<ForFiscalPrincipalPP> fors = pp.getForsFiscauxPrincipauxActifsSorted();
				assertNotNull(fors);
				assertEquals(1, fors.size());

				final ForFiscalPrincipalPP f0 = fors.get(0);
				assertForPrincipal(date(1976, 3, 2), MotifFor.MAJORITE, date(2003, 5, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, f0);
				return null;
			}
		});
	}

	/**
	 * [SIFISC-13741] Non-résident transformé en habitant suite à un passage par la vallée de Joux
	 */
	@Test
	public void testPassageParLaValleeEtFlagHabitant() throws Exception {

		final long noIndividu = 45120321L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1968, 5, 12), "Li", "Kim", Sexe.MASCULIN);
				{
					final MockAdresse adr = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.LeSentier.GrandRue, null, date(2000, 1, 1), date(2006, 7, 31));
					adr.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, ServiceInfrastructureRaw.noPaysInconnu, null));
					adr.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lonay.getNoOFS(), null));
				}
				{
					final MockAdresse adr = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lonay.CheminDuRechoz, null, date(2006, 8, 1), date(2007, 5, 31));
					adr.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.LeChenit.getNoOFS(), null));
					adr.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Echallens.getNoOFS(), null));
				}
				{
					final MockAdresse adr = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(2007, 6, 1), date(2011, 9, 29));
					adr.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lonay.getNoOFS(), null));
					adr.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.LeChenit.getNoOFS(), null));
				}
				{
					final MockAdresse adr = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.LeSentier.GrandRue, null, date(2011, 9, 30), date(2012, 6, 18));
					adr.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Echallens.getNoOFS(), null));
					adr.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));
				}
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				assertFalse(pp.isHabitantVD());
				return pp.getNumero();
			}
		});

		// recalcul du flag habitant sur cet individu
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				tiersService.updateHabitantFlag(pp, noIndividu, null);
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertFalse("Redevenu habitant ?", pp.isHabitantVD());
			}
		});
	}

	/**
	 * [SIFISC-13741] Non-résident transformé en habitant... suite à une déclaration de départ vers une commune pas encore existante au moment du départ
	 */
	@Test
	public void testDepartVersCommuneNonEncoreExistanteEtFlagHabitant() throws Exception {

		final long noIndividu = 45120321L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1968, 5, 12), "Li", "Kim", Sexe.MASCULIN);
				{
					final MockAdresse adr = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lonay.CheminDuRechoz, null, date(2006, 8, 1), date(2007, 5, 31));
					adr.setLocalisationPrecedente(new Localisation(LocalisationType.HORS_SUISSE, ServiceInfrastructureRaw.noPaysInconnu, null));
					adr.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.BourgEnLavaux.getNoOFS(), null));          // n'existe pas en 2007 !!
				}
				{
					final MockAdresse adr = addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Epesses.RueDeLaMottaz, null, date(2007, 6, 1), date(2011, 9, 29));
					adr.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lonay.getNoOFS(), null));
					adr.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));
				}
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				assertFalse(pp.isHabitantVD());
				return pp.getNumero();
			}
		});

		// recalcul du flag habitant sur cet individu
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				tiersService.updateHabitantFlag(pp, noIndividu, null);
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertFalse("Redevenu habitant ?", pp.isHabitantVD());
			}
		});
	}
}
