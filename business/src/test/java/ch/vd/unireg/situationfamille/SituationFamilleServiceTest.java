package ch.vd.unireg.situationfamille;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.situationfamille.VueSituationFamille.Source;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.SituationFamille;
import ch.vd.unireg.tiers.SituationFamilleDAO;
import ch.vd.unireg.tiers.SituationFamilleMenageCommun;
import ch.vd.unireg.tiers.SituationFamillePersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TarifImpotSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SituationFamilleServiceTest extends BusinessTest {

	private SituationFamilleServiceImpl service;
	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");

		service = new SituationFamilleServiceImpl();
		service.setHibernateTemplate(hibernateTemplate);
		service.setServiceCivil(serviceCivil);
		service.setTiersService(getBean(TiersService.class, "tiersService"));
		service.setSituationFamilleDAO(getBean(SituationFamilleDAO.class, "situationFamilleDAO"));
		service.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));

	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetVueNonHabitantSansSituationFamille() {

		PersonnePhysique nonhabitant = new PersonnePhysique(false);
		nonhabitant.setNom("Khan");
		nonhabitant.setPrenomUsuel("Gengis");
		nonhabitant = (PersonnePhysique) tiersDAO.save(nonhabitant);

		final List<VueSituationFamille> vueHisto = service.getVueHisto(nonhabitant);
		assertNotNull(vueHisto);
		assertEmpty(vueHisto);

		final VueSituationFamille vue19700101 = service.getVue(nonhabitant, date(1970, 1, 1), true);
		final VueSituationFamille vue20000101 = service.getVue(nonhabitant, date(2000, 1, 1), true);
		final VueSituationFamille vue20200101 = service.getVue(nonhabitant, date(2020, 1, 1), true);
		assertNull(vue19700101);
		assertNull(vue20000101);
		assertNull(vue20200101);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetVueNonHabitantAvecSituationsFamille() {

		PersonnePhysique nonhabitant = new PersonnePhysique(false);
		nonhabitant.setNom("Khan");
		nonhabitant.setPrenomUsuel("Gengis");

		{
			SituationFamille situation = new SituationFamillePersonnePhysique();
			situation.setDateDebut(date(1902, 1, 1));
			situation.setDateFin(date(1924, 12, 31));
			situation.setEtatCivil(EtatCivil.CELIBATAIRE);
			situation.setNombreEnfants(Integer.valueOf(0));
			nonhabitant.addSituationFamille(situation);
		}
		{
			SituationFamille situation = new SituationFamillePersonnePhysique();
			situation.setDateDebut(date(1925, 1, 1));
			situation.setDateFin(date(1949, 12, 31));
			situation.setEtatCivil(EtatCivil.MARIE);
			situation.setNombreEnfants(Integer.valueOf(23));
			nonhabitant.addSituationFamille(situation);
		}
		{
			SituationFamille situation = new SituationFamillePersonnePhysique();
			situation.setDateDebut(date(1950, 1, 1));
			situation.setEtatCivil(EtatCivil.VEUF);
			situation.setNombreEnfants(Integer.valueOf(23));
			nonhabitant.addSituationFamille(situation);
		}

		nonhabitant = (PersonnePhysique) tiersDAO.save(nonhabitant);

		final List<VueSituationFamille> vueHisto = service.getVueHisto(nonhabitant);
		assertNotNull(vueHisto);
		assertEquals(3, vueHisto.size());

		final VueSituationFamillePersonnePhysique vueHisto0 = (VueSituationFamillePersonnePhysique) vueHisto.get(0);
		final VueSituationFamillePersonnePhysique vueHisto1 = (VueSituationFamillePersonnePhysique) vueHisto.get(1);
		final VueSituationFamillePersonnePhysique vueHisto2 = (VueSituationFamillePersonnePhysique) vueHisto.get(2);
		assertVue(date(1902, 1, 1), date(1924, 12, 31), EtatCivil.CELIBATAIRE, 0, Source.FISCALE_TIERS, vueHisto0);
		assertVue(date(1925, 1, 1), date(1949, 12, 31), EtatCivil.MARIE, 23, Source.FISCALE_TIERS, vueHisto1);
		assertVue(date(1950, 1, 1), null, EtatCivil.VEUF, 23, Source.FISCALE_TIERS, vueHisto2);

		final VueSituationFamillePersonnePhysique vue19100101 = (VueSituationFamillePersonnePhysique) service.getVue(nonhabitant, date(
				1910, 1, 1), true);
		final VueSituationFamillePersonnePhysique vue19300101 = (VueSituationFamillePersonnePhysique) service.getVue(nonhabitant, date(
				1930, 1, 1), true);
		final VueSituationFamillePersonnePhysique vue19800101 = (VueSituationFamillePersonnePhysique) service.getVue(nonhabitant, date(
				1980, 1, 1), true);
		assertVue(date(1902, 1, 1), date(1924, 12, 31), EtatCivil.CELIBATAIRE, 0, Source.FISCALE_TIERS, vue19100101);
		assertVue(date(1925, 1, 1), date(1949, 12, 31), EtatCivil.MARIE, 23, Source.FISCALE_TIERS, vue19300101);
		assertVue(date(1950, 1, 1), null, EtatCivil.VEUF, 23, Source.FISCALE_TIERS, vue19800101);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetVueHabitantSansEtatCivil() {

		final long noIndividu = 1;
		final RegDate dateNaissance = date(1953, 11, 2);

		// Crée un habitant (qui possédera un état-civil célibataire par défaut)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Dupont", "Pierre", true);
				individu.getEtatsCivils().clear();
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		habitant = (PersonnePhysique) tiersDAO.save(habitant);

		final List<VueSituationFamille> vueHisto = service.getVueHisto(habitant);
		assertNotNull(vueHisto);
		assertEquals(0, vueHisto.size());

		final VueSituationFamille vue19300101 = service.getVue(habitant, date(1930, 1, 1), true);
		final VueSituationFamille vue19700101 = service.getVue(habitant, date(1970, 1, 1), true);
		final VueSituationFamille vue20200101 = service.getVue(habitant, date(2020, 1, 1), true);
		assertNull(vue19300101);
		assertNull(vue19700101);
		assertNull(vue20200101);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetVueHabitantAvecPlusieursEtatsCivils() {

		final long noIndividu = 1;
		final RegDate dateNaissance = date(1953, 11, 2);
		final RegDate dateMariage = date(1972, 5, 1);
		final RegDate dateDivorce = date(1991, 5, 31);

		// Crée un habitant (qui possédera un état-civil célibataire par défaut)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Dupont", "Pierre", true);

				// Ajoute des état-civil à celui par défaut
				addEtatCivil(individu, dateMariage, TypeEtatCivil.MARIE);
				addEtatCivil(individu, dateDivorce, TypeEtatCivil.DIVORCE);
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		habitant = (PersonnePhysique) tiersDAO.save(habitant);

		final List<VueSituationFamille> vueHisto = service.getVueHisto(habitant);
		assertNotNull(vueHisto);
		assertEquals(3, vueHisto.size());

		final VueSituationFamillePersonnePhysique vueHisto0 = (VueSituationFamillePersonnePhysique) vueHisto.get(0);
		final VueSituationFamillePersonnePhysique vueHisto1 = (VueSituationFamillePersonnePhysique) vueHisto.get(1);
		final VueSituationFamillePersonnePhysique vueHisto2 = (VueSituationFamillePersonnePhysique) vueHisto.get(2);
		assertVue(dateNaissance, dateMariage.getOneDayBefore(), EtatCivil.CELIBATAIRE, null, Source.CIVILE, vueHisto0);
		assertVue(dateMariage, dateDivorce.getOneDayBefore(), EtatCivil.MARIE, null, Source.CIVILE, vueHisto1);
		assertVue(dateDivorce, null, EtatCivil.DIVORCE, null, Source.CIVILE, vueHisto2);

		final VueSituationFamillePersonnePhysique vue19300101 = (VueSituationFamillePersonnePhysique) service.getVue(habitant, date(1930,
				1, 1), true);
		final VueSituationFamillePersonnePhysique vue19700101 = (VueSituationFamillePersonnePhysique) service.getVue(habitant, date(1970,
				1, 1), true);
		final VueSituationFamillePersonnePhysique vue19800101 = (VueSituationFamillePersonnePhysique) service.getVue(habitant, date(1980,
				1, 1), true);
		final VueSituationFamillePersonnePhysique vue20200101 = (VueSituationFamillePersonnePhysique) service.getVue(habitant, date(2020,
				1, 1), true);
		assertNull(vue19300101);
		assertVue(dateNaissance, dateMariage.getOneDayBefore(), EtatCivil.CELIBATAIRE, null, Source.CIVILE, vue19700101);
		assertVue(dateMariage, dateDivorce.getOneDayBefore(), EtatCivil.MARIE, null, Source.CIVILE, vue19800101);
		assertVue(dateDivorce, null, EtatCivil.DIVORCE, null, Source.CIVILE, vue20200101);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetVueHabitantAvecEtatsCivilsEtSituationsFamille() {

		final long noIndividu = 1;
		final RegDate dateNaissance = date(1953, 11, 2);
		final RegDate dateMariage = date(1972, 5, 1);
		final RegDate dateSeparationFiscale = date(1990, 3, 1);
		final RegDate dateDivorce = date(1991, 5, 31);

		// Crée un habitant (qui possédera un état-civil célibataire par défaut)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Dupont", "Pierre", true);

				// Ajoute des état-civil à celui par défaut
				addEtatCivil(individu, dateMariage, TypeEtatCivil.MARIE);
				addEtatCivil(individu, dateDivorce, TypeEtatCivil.DIVORCE);
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);

		// ajout d'une séparation fiscale (décidée par l'ACI) qui surcharge l'état civil
		{
			SituationFamille situation = new SituationFamillePersonnePhysique();
			situation.setDateDebut(dateSeparationFiscale);
			situation.setDateFin(dateDivorce.getOneDayBefore());
			situation.setEtatCivil(EtatCivil.SEPARE);
			situation.setNombreEnfants(Integer.valueOf(0));
			habitant.addSituationFamille(situation);
		}

		habitant = (PersonnePhysique) tiersDAO.save(habitant);

		final List<VueSituationFamille> vueHisto = service.getVueHisto(habitant);
		assertNotNull(vueHisto);
		assertEquals(4, vueHisto.size());

		final VueSituationFamillePersonnePhysique vueHisto0 = (VueSituationFamillePersonnePhysique) vueHisto.get(0);
		final VueSituationFamillePersonnePhysique vueHisto1 = (VueSituationFamillePersonnePhysique) vueHisto.get(1);
		final VueSituationFamillePersonnePhysique vueHisto2 = (VueSituationFamillePersonnePhysique) vueHisto.get(2);
		final VueSituationFamillePersonnePhysique vueHisto3 = (VueSituationFamillePersonnePhysique) vueHisto.get(3);
		assertVue(dateNaissance, dateMariage.getOneDayBefore(), EtatCivil.CELIBATAIRE, null, Source.CIVILE, vueHisto0);
		assertVue(dateMariage, dateSeparationFiscale.getOneDayBefore(), EtatCivil.MARIE, null, Source.CIVILE, vueHisto1);
		assertVue(dateSeparationFiscale, dateDivorce.getOneDayBefore(), EtatCivil.SEPARE, 0, Source.FISCALE_TIERS, vueHisto2);
		assertVue(dateDivorce, null, EtatCivil.DIVORCE, null, Source.CIVILE, vueHisto3);

		final VueSituationFamillePersonnePhysique vue19300101 = (VueSituationFamillePersonnePhysique) service.getVue(habitant, date(1930,
				1, 1), true);
		final VueSituationFamillePersonnePhysique vue19700101 = (VueSituationFamillePersonnePhysique) service.getVue(habitant, date(1970,
				1, 1), true);
		final VueSituationFamillePersonnePhysique vue19800101 = (VueSituationFamillePersonnePhysique) service.getVue(habitant, date(1980,
				1, 1), true);
		final VueSituationFamillePersonnePhysique vue19900501 = (VueSituationFamillePersonnePhysique) service.getVue(habitant, date(1990,
				5, 1), true);
		final VueSituationFamillePersonnePhysique vue20200101 = (VueSituationFamillePersonnePhysique) service.getVue(habitant, date(2020,
				1, 1), true);
		assertNull(vue19300101);
		assertVue(dateNaissance, dateMariage.getOneDayBefore(), EtatCivil.CELIBATAIRE, null, Source.CIVILE, vue19700101);
		assertVue(dateMariage, dateSeparationFiscale.getOneDayBefore(), EtatCivil.MARIE, null, Source.CIVILE, vue19800101);
		assertVue(dateSeparationFiscale, dateDivorce.getOneDayBefore(), EtatCivil.SEPARE, 0, Source.FISCALE_TIERS, vue19900501);
		assertVue(dateDivorce, null, EtatCivil.DIVORCE, null, Source.CIVILE, vue20200101);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHabitantAnnulerSituationsFamille() {

		final long noIndividu = 1;
		final RegDate dateNaissance = date(1953, 11, 2);
		final RegDate dateMariage = date(1972, 5, 1);
		final RegDate dateSeparationFiscale = date(1990, 3, 1);
		final RegDate dateDivorce = date(1991, 5, 31);
		final RegDate datePacs = date(2005, 7, 12);

		// Crée un habitant (qui possédera un état-civil célibataire par défaut)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Dupont", "Pierre", true);

				// Ajoute des état-civil à celui par défaut
				addEtatCivil(individu, dateMariage, TypeEtatCivil.MARIE);
				addEtatCivil(individu, dateDivorce, TypeEtatCivil.DIVORCE);

			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);

		// ajout d'une séparation fiscale (décidée par l'ACI) qui surcharge l'état civil
		{
			SituationFamille situation = new SituationFamillePersonnePhysique();
			situation.setDateDebut(dateSeparationFiscale);
			situation.setDateFin(dateDivorce.getOneDayBefore());
			situation.setEtatCivil(EtatCivil.SEPARE);
			situation.setNombreEnfants(Integer.valueOf(0));
			habitant.addSituationFamille(situation);
			situation.setContribuable(habitant);
		}

		// ajout d'un PACS (décidée par l'ACI) que nous allons annuler par la suite gnark gnark !!!
		{
			SituationFamille situation = new SituationFamillePersonnePhysique();
			situation.setDateDebut(datePacs);
			situation.setEtatCivil(EtatCivil.LIE_PARTENARIAT_ENREGISTRE);
			situation.setNombreEnfants(Integer.valueOf(0));
			habitant.addSituationFamille(situation);
			situation.setContribuable(habitant);
		}

		habitant = (PersonnePhysique) tiersDAO.save(habitant);

		long pacsId;
		{
			final List<VueSituationFamille> vueHisto = service.getVueHisto(habitant);
			assertNotNull(vueHisto);
			assertEquals(5, vueHisto.size());

			final VueSituationFamillePersonnePhysique vue0 = (VueSituationFamillePersonnePhysique) vueHisto.get(0);
			final VueSituationFamillePersonnePhysique vue1 = (VueSituationFamillePersonnePhysique) vueHisto.get(1);
			final VueSituationFamillePersonnePhysique vue2 = (VueSituationFamillePersonnePhysique) vueHisto.get(2);
			final VueSituationFamillePersonnePhysique vue3 = (VueSituationFamillePersonnePhysique) vueHisto.get(3);
			final VueSituationFamillePersonnePhysique vue4 = (VueSituationFamillePersonnePhysique) vueHisto.get(4);
			assertVue(dateNaissance, dateMariage.getOneDayBefore(), EtatCivil.CELIBATAIRE, null, Source.CIVILE, vue0);
			assertVue(dateMariage, dateSeparationFiscale.getOneDayBefore(), EtatCivil.MARIE, null, Source.CIVILE, vue1);
			assertVue(dateSeparationFiscale, dateDivorce.getOneDayBefore(), EtatCivil.SEPARE, 0, Source.FISCALE_TIERS, vue2);
			assertVue(dateDivorce, datePacs.getOneDayBefore(), EtatCivil.DIVORCE, null, Source.CIVILE, vue3);
			assertVue(datePacs, null, EtatCivil.LIE_PARTENARIAT_ENREGISTRE, 0, Source.FISCALE_TIERS, vue4);

			pacsId = vue4.getId();
		}

		// annulation du pacs
		service.annulerSituationFamille(pacsId);
		{
			final List<VueSituationFamille> vueHisto = service.getVueHisto(habitant);
			assertNotNull(vueHisto);
			assertEquals(5, vueHisto.size());

			final VueSituationFamillePersonnePhysique vue0 = (VueSituationFamillePersonnePhysique) vueHisto.get(0);
			final VueSituationFamillePersonnePhysique vue1 = (VueSituationFamillePersonnePhysique) vueHisto.get(1);
			final VueSituationFamillePersonnePhysique vue2 = (VueSituationFamillePersonnePhysique) vueHisto.get(2);
			final VueSituationFamillePersonnePhysique vue3 = (VueSituationFamillePersonnePhysique) vueHisto.get(3);
			final VueSituationFamillePersonnePhysique vue4 = (VueSituationFamillePersonnePhysique) vueHisto.get(4);
			assertVue(dateNaissance, dateMariage.getOneDayBefore(), EtatCivil.CELIBATAIRE, null, Source.CIVILE, vue0);
			assertVue(dateMariage, dateSeparationFiscale.getOneDayBefore(), EtatCivil.MARIE, null, Source.CIVILE, vue1);
			assertVue(dateSeparationFiscale, dateDivorce.getOneDayBefore(), EtatCivil.SEPARE, 0, Source.FISCALE_TIERS, vue2);
			assertVue(dateDivorce, null, EtatCivil.DIVORCE, null, Source.CIVILE, vue3);
			assertTrue(vue4.isAnnule());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetVueMenageCommunSansSituationFamille() {

		MenageCommun menage = new MenageCommun();
		menage = (MenageCommun) tiersDAO.save(menage);

		final List<VueSituationFamille> vueHisto = service.getVueHisto(menage);
		assertNotNull(vueHisto);
		assertEmpty(vueHisto);

		final VueSituationFamille vue19700101 = service.getVue(menage, date(1970, 1, 1), true);
		final VueSituationFamille vue20000101 = service.getVue(menage, date(2000, 1, 1), true);
		final VueSituationFamille vue20200101 = service.getVue(menage, date(2020, 1, 1), true);
		assertNull(vue19700101);
		assertNull(vue20000101);
		assertNull(vue20200101);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetVueMenageCommunAvecSituationsFamille() {

		PersonnePhysique madame = new PersonnePhysique(true);
		madame.setNumeroIndividu(1234L);
		PersonnePhysique monsieur = new PersonnePhysique(true);
		monsieur.setNumeroIndividu(1235L);
		madame = (PersonnePhysique) tiersDAO.save(madame);
		monsieur = (PersonnePhysique) tiersDAO.save(monsieur);

		MenageCommun menage = new MenageCommun();

		final RegDate dateMariage = date(1988, 1, 1);
		final RegDate dateNaissance1erEnfant = date(1992, 4, 24);
		final RegDate dateNaissance2emeEnfant = date(1996, 12, 4);
		final RegDate dateRegimeOrdinaire = date(2000, 1, 1);

		// ménage de sourcier où les deux travaillent et madame gagne plus que monsieur
		{
			SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
			situation.setDateDebut(dateMariage);
			situation.setDateFin(dateNaissance1erEnfant.getOneDayBefore());
			situation.setEtatCivil(EtatCivil.MARIE);
			situation.setNombreEnfants(Integer.valueOf(0));
			situation.setTarifApplicable(TarifImpotSource.DOUBLE_GAIN);
			situation.setContribuablePrincipalId(madame.getId());
			menage.addSituationFamille(situation);
		}

		// naissance d'un enfant, arrêt de travail de madame
		{
			SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
			situation.setDateDebut(dateNaissance1erEnfant);
			situation.setDateFin(dateNaissance2emeEnfant.getOneDayBefore());
			situation.setEtatCivil(EtatCivil.MARIE);
			situation.setNombreEnfants(Integer.valueOf(1));
			situation.setTarifApplicable(TarifImpotSource.NORMAL);
			situation.setContribuablePrincipalId(monsieur.getId());
			menage.addSituationFamille(situation);
		}

		// naissance d'un second enfant
		{
			SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
			situation.setDateDebut(dateNaissance2emeEnfant);
			situation.setDateFin(dateRegimeOrdinaire.getOneDayBefore());
			situation.setEtatCivil(EtatCivil.MARIE);
			situation.setNombreEnfants(Integer.valueOf(2));
			situation.setTarifApplicable(TarifImpotSource.NORMAL);
			situation.setContribuablePrincipalId(monsieur.getId());
			menage.addSituationFamille(situation);
		}

		// passage au régime ordinaire
		{
			SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
			situation.setDateDebut(dateRegimeOrdinaire);
			situation.setDateFin(null);
			situation.setEtatCivil(EtatCivil.MARIE);
			situation.setNombreEnfants(Integer.valueOf(2));
			situation.setTarifApplicable(null);
			situation.setContribuablePrincipalId(null);
			menage.addSituationFamille(situation);
		}
		menage = (MenageCommun) tiersDAO.save(menage);

		final List<VueSituationFamille> vueHisto = service.getVueHisto(menage);
		assertNotNull(vueHisto);
		assertEquals(4, vueHisto.size());

		final VueSituationFamilleMenageCommun vueHisto0 = (VueSituationFamilleMenageCommun) vueHisto.get(0);
		final VueSituationFamilleMenageCommun vueHisto1 = (VueSituationFamilleMenageCommun) vueHisto.get(1);
		final VueSituationFamilleMenageCommun vueHisto2 = (VueSituationFamilleMenageCommun) vueHisto.get(2);
		final VueSituationFamilleMenageCommun vueHisto3 = (VueSituationFamilleMenageCommun) vueHisto.get(3);

		assertVue(dateMariage, dateNaissance1erEnfant.getOneDayBefore(), 0, TarifImpotSource.DOUBLE_GAIN, madame.getNumero(),
				Source.FISCALE_TIERS, vueHisto0);
		assertVue(dateNaissance1erEnfant, dateNaissance2emeEnfant.getOneDayBefore(), 1, TarifImpotSource.NORMAL, monsieur.getNumero(),
				Source.FISCALE_TIERS, vueHisto1);
		assertVue(dateNaissance2emeEnfant, dateRegimeOrdinaire.getOneDayBefore(), 2, TarifImpotSource.NORMAL, monsieur.getNumero(),
				Source.FISCALE_TIERS, vueHisto2);
		assertVue(dateRegimeOrdinaire, null, 2, null, null, Source.FISCALE_TIERS, vueHisto3);

		final VueSituationFamilleMenageCommun vue19700101 = (VueSituationFamilleMenageCommun) service.getVue(menage, date(1970, 1, 1), true);
		final VueSituationFamilleMenageCommun vue19900101 = (VueSituationFamilleMenageCommun) service.getVue(menage, date(1990, 1, 1), true);
		final VueSituationFamilleMenageCommun vue19930101 = (VueSituationFamilleMenageCommun) service.getVue(menage, date(1993, 1, 1), true);
		final VueSituationFamilleMenageCommun vue19980101 = (VueSituationFamilleMenageCommun) service.getVue(menage, date(1998, 1, 1), true);
		final VueSituationFamilleMenageCommun vue20020101 = (VueSituationFamilleMenageCommun) service.getVue(menage, date(2002, 1, 1), true);
		assertNull(vue19700101);
		assertVue(dateMariage, dateNaissance1erEnfant.getOneDayBefore(), 0, TarifImpotSource.DOUBLE_GAIN, madame.getNumero(),
				Source.FISCALE_TIERS, vue19900101);
		assertVue(dateNaissance1erEnfant, dateNaissance2emeEnfant.getOneDayBefore(), 1, TarifImpotSource.NORMAL, monsieur.getNumero(),
				Source.FISCALE_TIERS, vue19930101);
		assertVue(dateNaissance2emeEnfant, dateRegimeOrdinaire.getOneDayBefore(), 2, TarifImpotSource.NORMAL, monsieur.getNumero(),
				Source.FISCALE_TIERS, vue19980101);
		assertVue(dateRegimeOrdinaire, null, 2, null, null, Source.FISCALE_TIERS, vue20020101);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetVueEntreprise() {

		Entreprise entreprise = new Entreprise();
		entreprise.setNumero(123456L);
		entreprise = (Entreprise) tiersDAO.save(entreprise);

		// par définition, une entreprise ne possède pas de situation de famille
		final List<VueSituationFamille> vueHisto = service.getVueHisto(entreprise);
		assertNotNull(vueHisto);
		assertEmpty(vueHisto);

		final VueSituationFamille vue19700101 = service.getVue(entreprise, date(1970, 1, 1), true);
		final VueSituationFamille vue20000101 = service.getVue(entreprise, date(2000, 1, 1), true);
		final VueSituationFamille vue20200101 = service.getVue(entreprise, date(2020, 1, 1), true);
		assertNull(vue19700101);
		assertNull(vue20000101);
		assertNull(vue20200101);
	}

	private static void assertVue(RegDate debut, RegDate fin, EtatCivil etat, Integer enfants, Source source,
			VueSituationFamillePersonnePhysique vue) {
		assertNotNull(vue);
		assertEquals(debut, vue.getDateDebut());
		assertEquals(fin, vue.getDateFin());
		assertEquals(etat, vue.getEtatCivil());
		assertEquals(enfants, vue.getNombreEnfants());
		assertEquals(source, vue.getSource());
	}

	private static void assertVue(RegDate debut, RegDate fin, Integer enfants, TarifImpotSource tarif, Long idCtbPrincipal, Source source,
			VueSituationFamilleMenageCommun vue) {
		assertNotNull(vue);
		assertEquals(debut, vue.getDateDebut());
		assertEquals(fin, vue.getDateFin());
		assertEquals(enfants, vue.getNombreEnfants());
		assertEquals(tarif, vue.getTarifApplicable());
		assertEquals(idCtbPrincipal, vue.getNumeroContribuablePrincipal());
		assertEquals(source, vue.getSource());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetEtatCivil() {

		final long NO_PIERRE = 1;
		final long NO_MOMO = 2;

		/*
		 * Mise en place des données
		 */

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne seul
				addIndividu(NO_PIERRE, RegDate.get(1953, 11, 2), "Dupont", "Pierre", true);

				// mariée seul
				MockIndividu momo = addIndividu(NO_MOMO, RegDate.get(1953, 11, 2), "Dupneu", "Monique", false);
				marieIndividu(momo, RegDate.get(1970, 1, 1));
			}
		});

		final long NO_CTB_PIERRE;
		final long NO_CTB_MOMO;
		final long NO_CTB_ARNOLD;
		final long NO_CTB_NOLWEN;
		final long NO_CTB_ALF;
		{
			PersonnePhysique pierre = new PersonnePhysique(true);
			pierre.setNumeroIndividu(NO_PIERRE);
			pierre = (PersonnePhysique) tiersDAO.save(pierre);
			NO_CTB_PIERRE = pierre.getNumero();

			PersonnePhysique momo = new PersonnePhysique(true);
			momo.setNumeroIndividu(NO_MOMO);
			momo = (PersonnePhysique) tiersDAO.save(momo);
			NO_CTB_MOMO = momo.getNumero();

			// non-habitant marié
			PersonnePhysique arnold = new PersonnePhysique(false);
			arnold.setPrenomUsuel("Arnold");
			arnold.setNom("Schwarzie");
			arnold.setSexe(Sexe.MASCULIN);
			SituationFamille situation = new SituationFamillePersonnePhysique();
			situation.setDateDebut(RegDate.get(1990, 1, 1));
			situation.setNombreEnfants(0);
			situation.setEtatCivil(EtatCivil.MARIE);
			arnold.addSituationFamille(situation);
			arnold = (PersonnePhysique) tiersDAO.save(arnold);
			NO_CTB_ARNOLD = arnold.getNumero();

			// non-habitant célibataire
			PersonnePhysique nolwen = new PersonnePhysique(false);
			nolwen.setPrenomUsuel("Nowlen");
			nolwen.setNom("Raflss");
			nolwen.setSexe(Sexe.FEMININ);
			situation = new SituationFamillePersonnePhysique();
			situation.setDateDebut(RegDate.get(1990, 1, 1));
			situation.setNombreEnfants(0);
			situation.setEtatCivil(EtatCivil.CELIBATAIRE);
			nolwen.addSituationFamille(situation);
			nolwen = (PersonnePhysique) tiersDAO.save(nolwen);
			NO_CTB_NOLWEN = nolwen.getNumero();

			// non-habitant sans situation de famille
			PersonnePhysique alf = new PersonnePhysique(false);
			alf.setPrenomUsuel("Alf");
			alf.setNom("Alf");
			alf.setSexe(null);
			alf = (PersonnePhysique) tiersDAO.save(alf);
			NO_CTB_ALF = alf.getNumero();
		}

		{
			final PersonnePhysique pierre = (PersonnePhysique) tiersDAO.get(NO_CTB_PIERRE);
			assertNotNull(pierre);
			assertEquals(EtatCivil.CELIBATAIRE, service.getEtatCivil(pierre, null, true));

			final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(NO_CTB_MOMO);
			assertNotNull(momo);
			assertEquals(EtatCivil.MARIE, service.getEtatCivil(momo, null, true));

			final PersonnePhysique arnold = (PersonnePhysique) tiersDAO.get(NO_CTB_ARNOLD);
			assertNotNull(arnold);
			assertEquals(EtatCivil.MARIE, service.getEtatCivil(arnold, null, true));

			final PersonnePhysique nolwen = (PersonnePhysique) tiersDAO.get(NO_CTB_NOLWEN);
			assertNotNull(nolwen);
			assertEquals(EtatCivil.CELIBATAIRE, service.getEtatCivil(nolwen, null, true));

			final PersonnePhysique alf = (PersonnePhysique) tiersDAO.get(NO_CTB_ALF);
			assertNotNull(alf);
			assertNull(service.getEtatCivil(alf, null, true));
		}
	}

	// [UNIREG-823] le dernier état-civil doit être fermé à la date de décès si l'individu est décédé.
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHabitantDecede() {

		final long noIndividu = 1;

		// Crée un habitant qui possède plus état-civil et qui est décédé
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, date(1945, 1, 4), "Dupont", "Pierre", true);

				addEtatCivil(individu, date(1965, 6, 3), TypeEtatCivil.MARIE);
				addEtatCivil(individu, date(1976, 12, 24), TypeEtatCivil.DIVORCE);

				individu.setDateDeces(date(2004, 5, 1));
			}
		});

		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		habitant = (PersonnePhysique) tiersDAO.save(habitant);

		final List<VueSituationFamille> vueHisto = service.getVueHisto(habitant);
		assertNotNull(vueHisto);
		assertEquals(3, vueHisto.size());

		final VueSituationFamillePersonnePhysique vue0 = (VueSituationFamillePersonnePhysique) vueHisto.get(0);
		final VueSituationFamillePersonnePhysique vue1 = (VueSituationFamillePersonnePhysique) vueHisto.get(1);
		final VueSituationFamillePersonnePhysique vue2 = (VueSituationFamillePersonnePhysique) vueHisto.get(2);
		assertVue(date(1945, 1, 4), date(1965, 6, 2), EtatCivil.CELIBATAIRE, null, Source.CIVILE, vue0);
		assertVue(date(1965, 6, 3), date(1976, 12, 23), EtatCivil.MARIE, null, Source.CIVILE, vue1);
		assertVue(date(1976, 12, 24), date(2004, 5, 1), EtatCivil.DIVORCE, null, Source.CIVILE, vue2);
	}

	/**
	 * [UNIREG-1673] Vérifie qu'une NullPointerException ne survient pas dans le cas tordu ci-dessous.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetVueHabitantAvecEtatsCivilsDatesDebutNulles() {

		final long noIndividu = 1;
		final RegDate dateNaissanceCivile = null;
		final RegDate dateMariageCivile = null;
		final RegDate dateMariageFiscale = date(1972, 5, 1);

		// Crée un habitant avec deux états civils sans dates de début connues
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// Etat-civil célibataire implicite
				MockIndividu individu = addIndividu(noIndividu, dateNaissanceCivile, "Dupont", "Pierre", true);

				// Ajoute l'état-civil marié
				addEtatCivil(individu, dateMariageCivile, TypeEtatCivil.MARIE);
			}
		});

		// Crée un habitant avec un ménage commun (donc avec une date de mariage connue au niveau fiscal)
		PersonnePhysique habitant = new PersonnePhysique(true);
		habitant.setNumeroIndividu(noIndividu);
		habitant = (PersonnePhysique) tiersDAO.save(habitant);

		MenageCommun menage = new MenageCommun();
		menage = (MenageCommun) tiersDAO.save(menage);

		AppartenanceMenage rapport = new AppartenanceMenage(dateMariageFiscale, null, habitant, menage);
		rapport = (AppartenanceMenage) tiersDAO.save(rapport);
		habitant.addRapportSujet(rapport);
		menage.addRapportObjet(rapport);

		// Vérifie la vue historique
		final List<VueSituationFamille> vueHisto = service.getVueHisto(habitant);
		assertNotNull(vueHisto);
		assertEquals(2, vueHisto.size());

		final VueSituationFamillePersonnePhysique vueHisto0 = (VueSituationFamillePersonnePhysique) vueHisto.get(0);
		final VueSituationFamillePersonnePhysique vueHisto1 = (VueSituationFamillePersonnePhysique) vueHisto.get(1);
		assertVue(dateNaissanceCivile, dateMariageFiscale.getOneDayBefore(), EtatCivil.CELIBATAIRE, null, Source.CIVILE, vueHisto0);
		assertVue(dateMariageFiscale, null, EtatCivil.MARIE, null, Source.CIVILE, vueHisto1);

		// Vérifie la vue ponctuelle
		final VueSituationFamillePersonnePhysique vue19700101 = (VueSituationFamillePersonnePhysique) service.getVue(habitant, date(1970, 1, 1), true);
		final VueSituationFamillePersonnePhysique vue19800101 = (VueSituationFamillePersonnePhysique) service.getVue(habitant, date(1980, 1, 1), true);
		assertVue(dateNaissanceCivile, dateMariageFiscale.getOneDayBefore(), EtatCivil.CELIBATAIRE, null, Source.CIVILE, vue19700101);
		assertVue(dateMariageFiscale, null, EtatCivil.MARIE, null, Source.CIVILE, vue19800101);
	}

	/**
	 * [SIFISC-10825] l'état civil "marié" sans date de début n'était pas pris en compte
	 */
	@Test
	public void testEtatCivilMarieSansDateDebutPuisSeparation() throws Exception {

		final long noIndividu = 478437L;
		final RegDate dateNaissance = date(1960, 1, 1);
		final RegDate dateSeparation = date(2012, 6, 21);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Nanini", "Bernard", Sexe.MASCULIN);
				addEtatCivil(ind, null, TypeEtatCivil.MARIE);
				addEtatCivil(ind, dateSeparation, TypeEtatCivil.SEPARE);
			}
		});

		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateSeparation.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Aigle);
			return pp.getNumero();
		});

		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			final List<VueSituationFamille> histo = service.getVueHisto(pp);
			assertNotNull(histo);
			assertEquals(2, histo.size());
			assertEquals(EtatCivil.MARIE, histo.get(0).getEtatCivil());
			assertEquals(EtatCivil.SEPARE, histo.get(1).getEtatCivil());
			return null;
		});
	}

	@Test
	public void testRattrapageDatesEtatsCivilsAvecAppartenancesMenages() throws Exception {

		final long noIndividu = 478437L;
		final RegDate dateNaissance = date(1960, 1, 1);
		final RegDate dateMariage1 = date(1985, 7, 23);
		final RegDate dateDivorce = date(1996, 2, 12);
		final RegDate dateMariage2 = date(2000, 7, 5);
		final RegDate dateDecesConjoint = date(2010, 9, 15);

		// mise en place civile (on ne connait aucune date)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Lehéro", "Toto", Sexe.MASCULIN);
				addEtatCivil(ind, null, TypeEtatCivil.MARIE);
				addEtatCivil(ind, null, TypeEtatCivil.DIVORCE);
				addEtatCivil(ind, null, TypeEtatCivil.MARIE);
				addEtatCivil(ind, null, TypeEtatCivil.VEUF);
			}
		});

		// mise ne place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			pp.setDateNaissance(dateNaissance);
			addEnsembleTiersCouple(pp, null, dateMariage1, dateDivorce.getOneDayBefore());
			addEnsembleTiersCouple(pp, null, dateMariage2, dateDecesConjoint);
			return pp.getNumero();
		});

		// construction de la vue historique
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			final List<VueSituationFamille> histo = service.getVueHisto(pp);
			assertNotNull(histo);
			assertEquals(5, histo.size());
			{
				final VueSituationFamille vue = histo.get(0);
				assertNotNull(vue);
				assertEquals(dateNaissance, vue.getDateDebut());
				assertEquals(dateMariage1.getOneDayBefore(), vue.getDateFin());
				assertEquals(EtatCivil.CELIBATAIRE, vue.getEtatCivil());
			}
			{
				final VueSituationFamille vue = histo.get(1);
				assertNotNull(vue);
				assertEquals(dateMariage1, vue.getDateDebut());
				assertEquals(dateDivorce.getOneDayBefore(), vue.getDateFin());
				assertEquals(EtatCivil.MARIE, vue.getEtatCivil());
			}
			{
				final VueSituationFamille vue = histo.get(2);
				assertNotNull(vue);
				assertEquals(dateDivorce, vue.getDateDebut());
				assertEquals(dateMariage2.getOneDayBefore(), vue.getDateFin());
				assertEquals(EtatCivil.DIVORCE, vue.getEtatCivil());
			}
			{
				final VueSituationFamille vue = histo.get(3);
				assertNotNull(vue);
				assertEquals(dateMariage2, vue.getDateDebut());
				assertEquals(dateDecesConjoint, vue.getDateFin());
				assertEquals(EtatCivil.MARIE, vue.getEtatCivil());
			}
			{
				final VueSituationFamille vue = histo.get(4);
				assertNotNull(vue);
				assertEquals(dateDecesConjoint.getOneDayAfter(), vue.getDateDebut());
				assertNull(vue.getDateFin());
				assertEquals(EtatCivil.VEUF, vue.getEtatCivil());
			}
			;
			return null;
		});
	}

	@Test
	public void testRattrapageDatesEtatsCivilsAvecForsFiscaux() throws Exception {

		final long noIndividu = 478437L;
		final RegDate dateNaissance = date(1960, 1, 1);
		final RegDate dateDivorce = date(1996, 2, 12);

		// mise en place civile (on ne connait aucune date)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Lehéro", "Toto", Sexe.MASCULIN);
				addEtatCivil(ind, null, TypeEtatCivil.DIVORCE);
				ind.setDateNaissance(dateNaissance);
			}
		});

		// mise ne place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateDivorce, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Bussigny);
			return pp.getNumero();
		});

		// construction de la vue historique
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			final List<VueSituationFamille> histo = service.getVueHisto(pp);
			assertNotNull(histo);
			assertEquals(2, histo.size());
			{
				final VueSituationFamille vue = histo.get(0);
				assertNotNull(vue);
				assertEquals(dateNaissance, vue.getDateDebut());
				assertEquals(dateDivorce.getOneDayBefore(), vue.getDateFin());
				assertEquals(EtatCivil.CELIBATAIRE, vue.getEtatCivil());
			}
			{
				final VueSituationFamille vue = histo.get(1);
				assertNotNull(vue);
				assertEquals(dateDivorce, vue.getDateDebut());      // cette date vient du for fiscal
				assertNull(vue.getDateFin());
				assertEquals(EtatCivil.DIVORCE, vue.getEtatCivil());
			}
			;
			return null;
		});
	}

	@Test
	public void testRattrapageDatesEtatsCivilsAvecDateNaissancePartielle() throws Exception {

		final long noIndividu = 478437L;
		final RegDate dateNaissance = RegDate.get(1960, 5);

		// mise en place civile (on ne connait aucune date)
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Lehéro", "Toto", Sexe.MASCULIN);
			}
		});

		// mise ne place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			pp.setDateNaissance(dateNaissance);
			return pp.getNumero();
		});

		// construction de la vue historique
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			final List<VueSituationFamille> histo = service.getVueHisto(pp);
			assertNotNull(histo);
			assertEquals(1, histo.size());
			{
				final VueSituationFamille vue = histo.get(0);
				assertNotNull(vue);
				assertEquals(date(1960, 5, 1), vue.getDateDebut());
				assertNull(vue.getDateFin());
				assertEquals(EtatCivil.CELIBATAIRE, vue.getEtatCivil());
			}
			;
			return null;
		});
	}

	/**
	 * [SIFISC-11460] la "zone continue" de fors n'est pas correctement prise en compte
	 */
	@Test
	public void testRattrapageDatesEtatsCivilsAvecDemenagements() throws Exception {

		final long noIndividu = 234678245273L;
		final RegDate dateNaissance = RegDate.get(1978, 6, 3);
		final RegDate dateArrivee1 = RegDate.get(2012, 6, 11);
		final RegDate dateDepartHS = RegDate.get(2012, 12, 31);
		final RegDate dateArrivee2 = RegDate.get(2013, 3, 1);
		final RegDate dateDemenagement = RegDate.get(2013, 4, 1);

		// mise en place du service civil
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Pranlaporte", "Philibert", Sexe.MASCULIN);
				addEtatCivil(ind, null, TypeEtatCivil.MARIE);
				addEtatCivil(ind, null, TypeEtatCivil.SEPARE);
				addEtatCivil(ind, null, TypeEtatCivil.SEPARE);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee1, MotifFor.ARRIVEE_HS, dateDepartHS, MotifFor.DEPART_HS, MockCommune.Fraction.LeSentier, ModeImposition.SOURCE);
			addForPrincipal(pp, dateDepartHS.getOneDayAfter(), MotifFor.DEPART_HS, dateArrivee2.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockPays.Espagne, ModeImposition.SOURCE);
			addForPrincipal(pp, dateArrivee2, MotifFor.ARRIVEE_HS, dateDemenagement.getOneDayBefore(), MotifFor.ARRIVEE_HS, MockCommune.Fraction.LeSentier, ModeImposition.SOURCE);
			addForPrincipal(pp, dateDemenagement, MotifFor.ARRIVEE_HS, MockCommune.Fraction.LeSolliat, ModeImposition.SOURCE);
			return pp.getNumero();
		});

		// calcul des situations de famille
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			final List<VueSituationFamille> histo = service.getVueHisto(pp);
			assertNotNull(histo);
			assertEquals(2, histo.size());
			{
				final VueSituationFamille vue = histo.get(0);
				assertNotNull(vue);
				assertNull(vue.getDateDebut());
				assertEquals(dateArrivee1.getOneDayBefore(), vue.getDateFin());
				assertEquals(EtatCivil.SEPARE, vue.getEtatCivil());
			}
			{
				final VueSituationFamille vue = histo.get(1);
				assertNotNull(vue);
				assertEquals(dateArrivee1, vue.getDateDebut());
				assertNull(vue.getDateFin());
				assertEquals(EtatCivil.SEPARE, vue.getEtatCivil());
			}
			;
			return null;
		});
	}
}
