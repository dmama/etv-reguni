package ch.vd.uniregctb.tiers;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.DataHolder;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
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

		final DataHolder<MockIndividu> holder = new DataHolder<MockIndividu>();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				holder.set(addIndividu(noIndividu, date(1970, 1, 1), "Marcel", "Dubouchelard", Sexe.MASCULIN));
			}
		});
		final PersonnePhysique pp = addHabitant(noIndividu);
		final MockIndividu individu = holder.get();

		// un individu sans adresse => non-habitant
		assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null, null));
		assertFalse(pp.isHabitantVD());

		// un individu à Lausanne en résidence principale => habitant
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(1970, 1, 1), null));
		assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_HABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null, null));
		assertTrue(pp.isHabitantVD());

		// un individu à Lausanne en résidence secondaire => habitant
		individu.getAdresses().clear();
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueDeMarcelin, null, date(1970, 1, 1), null));
		assertEquals(TiersService.UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT, tiersService.updateHabitantFlag(pp, noIndividu, null, null));
		assertTrue(pp.isHabitantVD());

		// un individu à Genève en résidence principale => non-habitant
		individu.getAdresses().clear();
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1970, 1, 1), null));
		assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null, null));
		assertFalse(pp.isHabitantVD());

		// un individu à Genève en résidence principale et à Lausanne en résidence secondaire => habitant
		individu.getAdresses().clear();
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1970, 1, 1), null));
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueDeMarcelin, null, date(1970, 1, 1), null));
		assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_HABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null, null));
		assertTrue(pp.isHabitantVD());

		// un individu parti de Lausanne à destination vaudoise => habitant
		{
			individu.getAdresses().clear();
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Morges.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT, tiersService.updateHabitantFlag(pp, noIndividu, null, null));
			assertTrue(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination hors-canton => nonhabitant
		{
			individu.getAdresses().clear();
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Neuchatel.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null, null));
			assertFalse(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination hors-Suisse => nonhabitant
		{
			individu.getAdresses().clear();
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT, tiersService.updateHabitantFlag(pp, noIndividu, null, null));
			assertFalse(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination vaudoise (variante en résidence secondaire) => habitant
		{
			individu.getAdresses().clear();
			individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1970, 1, 1), null));
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueDeMarcelin, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Morges.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_HABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null, null));
			assertTrue(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination hors-canton (variante en résidence secondaire) => nonhabitant
		{
			individu.getAdresses().clear();
			individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1970, 1, 1), null));
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueDeMarcelin, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Neuchatel.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null, null));
			assertFalse(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination hors-Suisse (variante en résidence secondaire) => nonhabitant
		{
			individu.getAdresses().clear();
			individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1970, 1, 1), null));
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueDeMarcelin, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT, tiersService.updateHabitantFlag(pp, noIndividu, null, null));
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
				final List<ForFiscalPrincipal> fors = pp.getForsFiscauxPrincipauxActifsSorted();
				assertNotNull(fors);
				assertEquals(1, fors.size());

				final ForFiscalPrincipal f0 = fors.get(0);
				assertForPrincipal(date(1976, 3, 2), MotifFor.MAJORITE, date(2003, 5, 31), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, f0);
				return null;
			}
		});
	}
}
