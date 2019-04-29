package ch.vd.unireg.tiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.regimefiscal.FormeJuridiqueVersTypeRegimeFiscalMapping;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TiersService2Test extends BusinessTest {

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
		individu.clearAddresses();
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), null));
		assertEquals(TiersService.UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT, tiersService.updateHabitantFlag(pp, noIndividu, null));
		assertTrue(pp.isHabitantVD());

		// un individu à Genève en résidence principale => non-habitant
		individu.clearAddresses();
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1970, 1, 1), null));
		assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null));
		assertFalse(pp.isHabitantVD());

		// un individu à Genève en résidence principale et à Lausanne en résidence secondaire => habitant
		individu.clearAddresses();
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1970, 1, 1), null));
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), null));
		assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_HABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null));
		assertTrue(pp.isHabitantVD());

		// un individu parti de Lausanne à destination vaudoise => non-habitant, car on ignore maintenant ([SIFISC-13741]) les destinations des adresses (qui ne sont plus sensées
		// être vaudoises pour la dernière adresse valide...)
		{
			individu.clearAddresses();
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Morges.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null));
			assertFalse(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination hors-canton => nonhabitant
		{
			pp.setHabitant(true);
			individu.clearAddresses();
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Neuchatel.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT, tiersService.updateHabitantFlag(pp, noIndividu, null));
			assertFalse(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination hors-Suisse => nonhabitant
		{
			individu.clearAddresses();
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT, tiersService.updateHabitantFlag(pp, noIndividu, null));
			assertFalse(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination vaudoise (variante en résidence secondaire) => non-habitant car les destinations des résidences secondaires sont maintenant ignorées
		{
			individu.clearAddresses();
			individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1970, 1, 1), null));
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Morges.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT, tiersService.updateHabitantFlag(pp, noIndividu, null));
			assertFalse(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination hors-canton (variante en résidence secondaire) => nonhabitant
		{
			individu.clearAddresses();
			individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(1970, 1, 1), null));
			final MockAdresse lausanne = new MockAdresse(TypeAdresseCivil.SECONDAIRE, MockRue.Lausanne.AvenueJolimont, null, date(1970, 1, 1), date(1999, 12, 31));
			lausanne.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Neuchatel.getNoOFS(), null));
			individu.addAdresse(lausanne);
			assertEquals(TiersService.UpdateHabitantFlagResultat.PAS_DE_CHANGEMENT, tiersService.updateHabitantFlag(pp, noIndividu, null));
			assertFalse(pp.isHabitantVD());
		}

		// un individu parti de Lausanne à destination hors-Suisse (variante en résidence secondaire) => nonhabitant
		{
			individu.clearAddresses();
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
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			assertFalse(pp.isHabitantVD());
			return pp.getNumero();
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
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			assertFalse(pp.isHabitantVD());
			return pp.getNumero();
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

	@Test
	public void testGetCommunautesHeritiers() throws Exception {

		class Ids {
			Long heinrich;
			Long hans;
			Long lieselotte;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final PersonnePhysique heinrich = addNonHabitant("Heinrich", "Schaudi", RegDate.get(1950, 2, 3), Sexe.MASCULIN);
			final PersonnePhysique hans = addNonHabitant("Hans", "Schaudi", RegDate.get(1977, 11, 6), Sexe.MASCULIN);
			final PersonnePhysique lieselotte = addNonHabitant("Lieselotte", "Mayer", RegDate.get(1979, 5, 22), Sexe.FEMININ);

			// Au réveillon de l'an 2000, Heinrich abuse des Schnitzels et décède peu après d'une
			// occlusion intestinale (paix à son âme). Hans et Lieselotte sont désignés comme ses héritiers.
			addHeritage(hans, heinrich, RegDate.get(2000, 1, 1), RegDate.get(2009, 12, 31), true);
			addHeritage(lieselotte, heinrich, RegDate.get(2000, 1, 1), RegDate.get(2009, 12, 31), false);

			// En 2010, Lieselotte reprend la pharmacie de son oncle et fait fortune grâce à la vente de Zahnpasta,
			// elle devient donc naturellement le principal de la communauté d'héritiers de Heinrich.
			addHeritage(lieselotte, heinrich, RegDate.get(2010, 1, 1), null, true);
			addHeritage(hans, heinrich, RegDate.get(2010, 1, 1), null, false);

			ids.heinrich = heinrich.getId();
			ids.hans = hans.getId();
			ids.lieselotte = lieselotte.getId();
			return null;
		});

		doInNewTransaction(status -> {
			final Map<Long, CommunauteHeritiers> communautes = tiersService.getCommunautesHeritiers(Arrays.asList(ids.heinrich, ids.hans, ids.lieselotte));
			assertNotNull(communautes);
			assertEquals(1, communautes.size());

			final CommunauteHeritiers communaute = communautes.get(ids.heinrich);
			assertNotNull(communaute);
			assertEquals(ids.heinrich.longValue(), communaute.getDefuntId());
			assertEquals(RegDate.get(2000, 1, 1), communaute.getDateDebut());
			assertNull(communaute.getDateFin());

			final List<Heritage> heritages = communaute.getLiensHeritage();
			assertEquals(4, heritages.size());
			heritages.sort(new DateRangeComparator<Heritage>().thenComparing(Heritage::getSujetId));
			assertHeritage(ids.heinrich, ids.hans, RegDate.get(2000, 1, 1), RegDate.get(2009, 12, 31), true, heritages.get(0));
			assertHeritage(ids.heinrich, ids.lieselotte, RegDate.get(2000, 1, 1), RegDate.get(2009, 12, 31), false, heritages.get(1));
			assertHeritage(ids.heinrich, ids.hans, RegDate.get(2010, 1, 1), null, false, heritages.get(2));
			assertHeritage(ids.heinrich, ids.lieselotte, RegDate.get(2010, 1, 1), null, true, heritages.get(3));
			return null;
		});
	}

	/**
	 * [FISCPROJ-155] Vérifie que la méthode openRegimesFiscauxParDefautCHVD ouvre bien plusieurs régimes sur les associations créées avant le 1er janvier 2018 (date de changement de mapping)
	 */
	@Test
	public void testOpenRegimesFiscauxParDefautCHVDPourAssociation() throws Exception {

		final RegDate dateFondation = date(2004, 1, 23);

		doInNewTransaction(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil("Ma petite association", dateFondation);

			final List<FormeJuridiqueVersTypeRegimeFiscalMapping> list = new ArrayList<>();
			tiersService.openRegimesFiscauxParDefautCHVD(entreprise, FormeJuridiqueEntreprise.ASSOCIATION, dateFondation, list::add);

			// on vérifie que deux mappings forme juridique -> type de régime fiscal ont été trouvés pour la période de validité de l'association
			assertEquals(2, list.size());
			assertMapping(null, date(2017, 12, 31), FormeJuridiqueEntreprise.ASSOCIATION, "70", list.get(0));
			assertMapping(date(2018, 1, 1), null, FormeJuridiqueEntreprise.ASSOCIATION, "703", list.get(1));

			// on vérifie que les régimes fiscaux correspondants ont été créés
			final List<RegimeFiscal> regimesFiscaux = new ArrayList<>(entreprise.getRegimesFiscaux());
			assertEquals(4, regimesFiscaux.size());
			regimesFiscaux.sort(new DateRangeComparator<RegimeFiscal>().thenComparing(RegimeFiscal::getPortee));
			assertRegimeFiscal(dateFondation, date(2017, 12, 31), RegimeFiscal.Portee.VD, "70", regimesFiscaux.get(0));
			assertRegimeFiscal(dateFondation, date(2017, 12, 31), RegimeFiscal.Portee.CH, "70", regimesFiscaux.get(1));
			assertRegimeFiscal(date(2018, 1, 1), null, RegimeFiscal.Portee.VD, "703", regimesFiscaux.get(2));
			assertRegimeFiscal(date(2018, 1, 1), null, RegimeFiscal.Portee.CH, "703", regimesFiscaux.get(3));

			return null;
		});

	}

	/**
	 * [FISCPROJ-155] Vérifie que la méthode openRegimesFiscauxParDefautCHVD ouvre un seul régime sur les SA quelque soit la date de création (car il n'y a un qu'un seul mapping sans limite de temps)
	 */
	@Test
	public void testOpenRegimesFiscauxParDefautCHVDPourSA() throws Exception {

		final RegDate dateFondation = date(2004, 1, 23);

		doInNewTransaction(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil("Ma petite entreprise", dateFondation);

			final List<FormeJuridiqueVersTypeRegimeFiscalMapping> list = new ArrayList<>();
			tiersService.openRegimesFiscauxParDefautCHVD(entreprise, FormeJuridiqueEntreprise.SA, dateFondation, list::add);

			// on vérifie qu'un seul mapping forme juridique -> type de régime fiscal ont été trouvés pour la période de validité de la société
			assertEquals(1, list.size());
			assertMapping(null, null, FormeJuridiqueEntreprise.SA, "01", list.get(0));

			// on vérifie que les régimes fiscaux correspondants ont été créés
			final List<RegimeFiscal> regimesFiscaux = new ArrayList<>(entreprise.getRegimesFiscaux());
			assertEquals(2, regimesFiscaux.size());
			regimesFiscaux.sort(new DateRangeComparator<RegimeFiscal>().thenComparing(RegimeFiscal::getPortee));
			assertRegimeFiscal(dateFondation, null, RegimeFiscal.Portee.VD, "01", regimesFiscaux.get(0));
			assertRegimeFiscal(dateFondation, null, RegimeFiscal.Portee.CH, "01", regimesFiscaux.get(1));

			return null;
		});

	}

	/**
	 * [FISCPROJ-155] Vérifie que la méthode changeRegimesFiscauxParDefautCHVD ouvre bien plusieurs régimes sur les associations créées avant le 1er janvier 2018 (date de changement de mapping)
	 */
	@Test
	public void testChangeRegimesFiscauxParDefautCHVDPourAssociation() throws Exception {

		final RegDate dateFondation = date(2004, 1, 23);
		final RegDate dateChangement = date(2006, 5, 17);

		final Long id = doInNewTransaction(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil("Ma petite association", dateFondation);
			entreprise.addRegimeFiscal(new RegimeFiscal(dateFondation, null, RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.INDETERMINE.getCode()));
			entreprise.addRegimeFiscal(new RegimeFiscal(dateFondation, null, RegimeFiscal.Portee.CH, MockTypeRegimeFiscal.INDETERMINE.getCode()));
			return entreprise.getId();
		});

		doInNewTransaction(status -> {
			final Entreprise entreprise = hibernateTemplate.get(Entreprise.class, id);
			assertNotNull(entreprise);

			final List<FormeJuridiqueVersTypeRegimeFiscalMapping> list = new ArrayList<>();
			tiersService.changeRegimesFiscauxParDefautCHVD(entreprise, FormeJuridiqueEntreprise.ASSOCIATION, dateChangement, list::add);

			// on vérifie que deux mappings forme juridique -> type de régime fiscal ont été trouvés pour la période de validité après la date de changement
			assertEquals(2, list.size());
			assertMapping(null, date(2017, 12, 31), FormeJuridiqueEntreprise.ASSOCIATION, "70", list.get(0));
			assertMapping(date(2018, 1, 1), null, FormeJuridiqueEntreprise.ASSOCIATION, "703", list.get(1));

			// on vérifie que les nouveaux régimes fiscaux correspondants ont été créés
			final List<RegimeFiscal> regimesFiscaux = new ArrayList<>(entreprise.getRegimesFiscaux());
			assertEquals(6, regimesFiscaux.size());
			regimesFiscaux.sort(new DateRangeComparator<RegimeFiscal>().thenComparing(RegimeFiscal::getPortee));
			assertRegimeFiscal(dateFondation, dateChangement.getOneDayBefore(), RegimeFiscal.Portee.VD, "00", regimesFiscaux.get(0));
			assertRegimeFiscal(dateFondation, dateChangement.getOneDayBefore(), RegimeFiscal.Portee.CH, "00", regimesFiscaux.get(1));
			assertRegimeFiscal(dateChangement, date(2017, 12, 31), RegimeFiscal.Portee.VD, "70", regimesFiscaux.get(2));
			assertRegimeFiscal(dateChangement, date(2017, 12, 31), RegimeFiscal.Portee.CH, "70", regimesFiscaux.get(3));
			assertRegimeFiscal(date(2018, 1, 1), null, RegimeFiscal.Portee.VD, "703", regimesFiscaux.get(4));
			assertRegimeFiscal(date(2018, 1, 1), null, RegimeFiscal.Portee.CH, "703", regimesFiscaux.get(5));

			return null;
		});
	}

	/**
	 * [FISCPROJ-155] Vérifie que la méthode changeRegimesFiscauxParDefautCHVD ouvre un seul régime sur les SA quelque soit la date de création (car il n'y a un qu'un seul mapping sans limite de temps)
	 */
	@Test
	public void testChangeRegimesFiscauxParDefautCHVDPourSA() throws Exception {

		final RegDate dateFondation = date(2004, 1, 23);
		final RegDate dateChangement = date(2006, 5, 17);

		final Long id = doInNewTransaction(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil("Ma petite entreprise", dateFondation);
			entreprise.addRegimeFiscal(new RegimeFiscal(dateFondation, null, RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.INDETERMINE.getCode()));
			entreprise.addRegimeFiscal(new RegimeFiscal(dateFondation, null, RegimeFiscal.Portee.CH, MockTypeRegimeFiscal.INDETERMINE.getCode()));
			return entreprise.getId();
		});

		doInNewTransaction(status -> {
			final Entreprise entreprise = hibernateTemplate.get(Entreprise.class, id);
			assertNotNull(entreprise);

			final List<FormeJuridiqueVersTypeRegimeFiscalMapping> list = new ArrayList<>();
			tiersService.changeRegimesFiscauxParDefautCHVD(entreprise, FormeJuridiqueEntreprise.SA, dateChangement, list::add);

			// on vérifie qu'un seul mapping forme juridique -> type de régime fiscal ont été trouvés pour la période de validité de la société
			assertEquals(1, list.size());
			assertMapping(null, null, FormeJuridiqueEntreprise.SA, "01", list.get(0));

			// on vérifie que les nouveaux régimes fiscaux correspondants ont été créés
			final List<RegimeFiscal> regimesFiscaux = new ArrayList<>(entreprise.getRegimesFiscaux());
			assertEquals(4, regimesFiscaux.size());
			regimesFiscaux.sort(new DateRangeComparator<RegimeFiscal>().thenComparing(RegimeFiscal::getPortee));
			assertRegimeFiscal(dateFondation, dateChangement.getOneDayBefore(), RegimeFiscal.Portee.VD, "00", regimesFiscaux.get(0));
			assertRegimeFiscal(dateFondation, dateChangement.getOneDayBefore(), RegimeFiscal.Portee.CH, "00", regimesFiscaux.get(1));
			assertRegimeFiscal(dateChangement, null, RegimeFiscal.Portee.VD, "01", regimesFiscaux.get(2));
			assertRegimeFiscal(dateChangement, null, RegimeFiscal.Portee.CH, "01", regimesFiscaux.get(3));

			return null;
		});
	}

	private static void assertMapping(RegDate dateDebut, RegDate dateFin, FormeJuridiqueEntreprise formeJuridiqueEntreprise, String codeRegimeFiscal, FormeJuridiqueVersTypeRegimeFiscalMapping mapping) {
		assertEquals(dateDebut, mapping.getDateDebut());
		assertEquals(dateFin, mapping.getDateFin());
		assertEquals(formeJuridiqueEntreprise, mapping.getFormeJuridique());
		assertEquals(codeRegimeFiscal, mapping.getTypeRegimeFiscal().getCode());
	}

	private static void assertHeritage(Long defuntId, Long heritierId, RegDate dateDebut, RegDate dateFin, Boolean principalCommunaute, Heritage heritage) {
		assertNotNull(heritage);
		assertEquals(defuntId, heritage.getObjetId());
		assertEquals(heritierId, heritage.getSujetId());
		assertEquals(dateDebut, heritage.getDateDebut());
		assertEquals(dateFin, heritage.getDateFin());
		assertEquals(principalCommunaute, heritage.getPrincipalCommunaute());
	}
}
