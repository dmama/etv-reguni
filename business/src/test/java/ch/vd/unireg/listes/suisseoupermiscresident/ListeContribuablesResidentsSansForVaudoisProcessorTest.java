package ch.vd.unireg.listes.suisseoupermiscresident;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.CategorieEtranger;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAdresseTiers;

public class ListeContribuablesResidentsSansForVaudoisProcessorTest extends BusinessTest {

	private ListeContribuablesResidentsSansForVaudoisProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		final ServiceCivilCacheWarmer serviceCivilCacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");
		processor = new ListeContribuablesResidentsSansForVaudoisProcessor(hibernateTemplate, tiersService, adresseService, transactionManager, tiersDAO, serviceInfra, serviceCivilCacheWarmer);
	}

	@Test
	public void testPresentEnSecondaireSansForVaudois() throws Exception {

		final long noIndividu = 12352326L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1980, 10, 25), "Malfoy", "Draco", true);
				addNationalite(individu, MockPays.Suisse, date(1980, 10, 25), null);
				addAdresse(individu, TypeAdresseCivil.SECONDAIRE, MockRue.Bex.CheminDeLaForet, null, date(2010, 12, 12), null);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.Bex.CheminDeLaForet, null, date(2010, 12, 12), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero();
		});

		// un individu présent en secondaire seulement ne doit pas être listé par le batch (dans les ignorés, si, bien-sûr)
		final ListeContribuablesResidentsSansForVaudoisResults result = processor.run(date(2011, 1, 1), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.getContribuablesIdentifies().size());
		Assert.assertEquals(0, result.getListeErreurs().size());
		Assert.assertEquals(1, result.getContribuablesIgnores().size());

		final ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore ignore = result.getContribuablesIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(ppId, ignore.ctbId);
		Assert.assertEquals(ListeContribuablesResidentsSansForVaudoisResults.CauseIgnorance.DOMICILE_NON_VAUDOIS, ignore.cause);
	}

	@Test
	public void testMineurSansForAucun() throws Exception {

		final long noIndividu = 12352326L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(2000, 10, 25), "Malfoy", "Draco", true);
				addNationalite(individu, MockPays.Suisse, date(1980, 10, 25), null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, date(2010, 12, 12), null);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.Bex.CheminDeLaForet, null, date(2010, 12, 12), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero();
		});

		// un individu mineur doit être ignoré
		final ListeContribuablesResidentsSansForVaudoisResults result = processor.run(date(2011, 1, 1), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.getContribuablesIdentifies().size());
		Assert.assertEquals(0, result.getListeErreurs().size());
		Assert.assertEquals(1, result.getContribuablesIgnores().size());

		final ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore ignore = result.getContribuablesIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(ppId, ignore.ctbId);
		Assert.assertEquals(ListeContribuablesResidentsSansForVaudoisResults.CauseIgnorance.MINEUR, ignore.cause);
	}

	@Test
	public void testDecedeSansForAucun() throws Exception {

		final long noIndividu = 12352326L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1980, 10, 25), "Malfoy", "Draco", true);
				addNationalite(individu, MockPays.Suisse, date(1980, 10, 25), null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, date(2010, 12, 12), null);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.Bex.CheminDeLaForet, null, date(2010, 12, 12), null);
				individu.setDateDeces(date(2010, 12, 31));
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero();
		});

		// un individu décédé doit être ignoré
		final ListeContribuablesResidentsSansForVaudoisResults result = processor.run(date(2011, 1, 1), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.getContribuablesIdentifies().size());
		Assert.assertEquals(0, result.getListeErreurs().size());
		Assert.assertEquals(1, result.getContribuablesIgnores().size());

		final ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore ignore = result.getContribuablesIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(ppId, ignore.ctbId);
		Assert.assertEquals(ListeContribuablesResidentsSansForVaudoisResults.CauseIgnorance.DECEDE, ignore.cause);
	}

	@Test
	public void testPresentEnPrincipalSansForAucun() throws Exception {

		final long noIndividu = 12352326L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1980, 10, 25), "Malfoy", "Draco", true);
				addNationalite(individu, MockPays.Suisse, date(1980, 10, 25), null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, date(2010, 12, 12), null);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.Bex.CheminDeLaForet, null, date(2010, 12, 12), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero();
		});

		// un individu présent en principal sans for vaudois doit être listé par le batch
		final ListeContribuablesResidentsSansForVaudoisResults result = processor.run(date(2011, 1, 1), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getContribuablesIdentifies().size());
		Assert.assertEquals(0, result.getListeErreurs().size());
		Assert.assertEquals(0, result.getContribuablesIgnores().size());

		Assert.assertEquals((Long) ppId, result.getContribuablesIdentifies().get(0));
	}

	@Test
	public void testPresentEnPrincipalAvecForHorsCanton() throws Exception {

		final long noIndividu = 12352326L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1980, 10, 25), "Malfoy", "Draco", true);
				addNationalite(individu, MockPays.Suisse, date(1980, 10, 25), null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, date(2010, 12, 12), null);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.Bex.CheminDeLaForet, null, date(2010, 12, 12), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, date(2010, 12, 12), MotifFor.INDETERMINE, MockCommune.Bern);
			return pp.getNumero();
		});

		// un individu présent en principal avec un for hors-canton seul doit être listé par le batch
		final ListeContribuablesResidentsSansForVaudoisResults result = processor.run(date(2011, 1, 1), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getContribuablesIdentifies().size());
		Assert.assertEquals(0, result.getListeErreurs().size());
		Assert.assertEquals(0, result.getContribuablesIgnores().size());

		Assert.assertEquals((Long) ppId, result.getContribuablesIdentifies().get(0));
	}

	@Test
	public void testPresentEnPrincipalAvecForHorsCantonEtForSecondaireVaudois() throws Exception {

		final long noIndividu = 12352326L;

		// mise en place civile
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1980, 10, 25), "Malfoy", "Draco", true);
				addNationalite(individu, MockPays.Suisse, date(1980, 10, 25), null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, date(2010, 12, 12), null);
				addAdresse(individu, TypeAdresseCivil.COURRIER, MockRue.Bex.CheminDeLaForet, null, date(2010, 12, 12), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, date(2010, 12, 12), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bern);
			addForSecondaire(pp, date(2010, 12, 12), MotifFor.ACHAT_IMMOBILIER, MockCommune.Aigle, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero();
		});

		// un individu présent en principal avec un for hors-canton seul et for secondaire vaudois doit être listé par le batch
		final ListeContribuablesResidentsSansForVaudoisResults result = processor.run(date(2011, 1, 1), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getContribuablesIdentifies().size());
		Assert.assertEquals(0, result.getListeErreurs().size());
		Assert.assertEquals(0, result.getContribuablesIgnores().size());

		Assert.assertEquals((Long) ppId, result.getContribuablesIdentifies().get(0));
	}

	@Test
	public void testNonHabitantAvecAdresseCourrierVaudoise() throws Exception {

		// mise en place civile (vide, où sont-ils tous partis ???)
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Ronald", "Weasley", null, Sexe.MASCULIN);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2000, 1, 1), null, MockRue.Bussigny.RueDeLIndustrie);
			pp.setNumeroOfsNationalite(MockPays.RoyaumeUni.getNoOFS());
			pp.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);
			return pp.getNumero();
		});

		// un non-habitant dont l'adresse courrier est dans le canton ne doit pas être listé
		final ListeContribuablesResidentsSansForVaudoisResults result = processor.run(date(2011, 1, 1), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.getContribuablesIdentifies().size());
		Assert.assertEquals(0, result.getListeErreurs().size());
		Assert.assertEquals(1, result.getContribuablesIgnores().size());

		final ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore ignore = result.getContribuablesIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(ppId, ignore.ctbId);
		Assert.assertEquals(ListeContribuablesResidentsSansForVaudoisResults.CauseIgnorance.DOMICILE_NON_VAUDOIS, ignore.cause);
	}

	@Test
	public void testTiersAnnule() throws Exception {

		// mise en place civile (vide, où sont-ils tous partis ???)
		serviceCivil.setUp(new DefaultMockIndividuConnector(false) {
			@Override
			protected void init() {
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Ronald", "Weasley", null, Sexe.MASCULIN);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2000, 1, 1), null, MockRue.Bussigny.RueDeLIndustrie);
			pp.setNumeroOfsNationalite(MockPays.RoyaumeUni.getNoOFS());
			pp.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);
			pp.setAnnule(true);
			return pp.getNumero();
		});

		// un tiers annulé ne doit pas être vu du tout...
		final ListeContribuablesResidentsSansForVaudoisResults result = processor.run(date(2011, 1, 1), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.getContribuablesIdentifies().size());
		Assert.assertEquals(0, result.getListeErreurs().size());
		Assert.assertEquals(0, result.getContribuablesIgnores().size());
	}
}
