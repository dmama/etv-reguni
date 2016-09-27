package ch.vd.uniregctb.metier;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.validation.ValidationService;

import static ch.vd.uniregctb.metier.PassageNouveauxRentiersSourciersEnMixteProcessor.SourcierData;


public class PassageNouveauxRentiersSourciersEnMixteProcessorTest extends BusinessTest {

	private PassageNouveauxRentiersSourciersEnMixteProcessor processor;
	private ParametreAppService paramAppService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		final ServiceCivilCacheWarmer cacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");
		final ValidationService validationService = getBean(ValidationService.class, "validationService");
		paramAppService = getBean(ParametreAppService.class, "parametreAppService");
		processor = new PassageNouveauxRentiersSourciersEnMixteProcessor(transactionManager, hibernateTemplate, tiersService,
		                                                                 adresseService, serviceInfra, cacheWarmer, validationService, paramAppService);
	}

	private static SourcierData buildSourcierData(ParametreAppService paramAppService) {
		return new SourcierData(getAgeRentierHomme(paramAppService), getAgeRentierFemme(paramAppService));
	}

	private static int getAgeRentierHomme(ParametreAppService paramAppService) {
		return paramAppService.getAgeRentierHomme();
	}

	private static int getAgeRentierFemme(ParametreAppService paramAppService) {
		return paramAppService.getAgeRentierFemme();
	}

	@Test(expected = IllegalStateException.class)
	public void TestSourcierDataIllegalStateException1() {
		final SourcierData data = buildSourcierData(paramAppService);
		data.getDateRentier();
		Assert.fail("Il devrait être impossible d'appeler getDateRentier sans setter la date de naissance et le sexe");
	}

	@Test(expected = IllegalStateException.class)
	public void TestSourcierDataIllegalStateException2() {
		final SourcierData data = buildSourcierData(paramAppService);
		data.setSexe(Sexe.FEMININ);
		data.getDateRentier();
		Assert.fail("Il devrait être impossible d'appeler getDateRentier sans setter la date de naissance");
	}

	@Test(expected = IllegalStateException.class)
	public void TestSourcierDataIllegalStateException3() {
		final SourcierData data = buildSourcierData(paramAppService);
		data.setDateNaissance(date(1990,1,1));
		data.getDateRentier();
		Assert.fail("Il devrait être impossible d'appeler getDateRentier sans setter le sexe");
	}

	@Test
	public void TestSourcierDataGetDateRentier() {
		final SourcierData data = buildSourcierData(paramAppService);
		final RegDate dateNaissance = date(1990, 1, 1);
		data.setSexe(Sexe.FEMININ);
		data.setDateNaissance(dateNaissance);
		RegDate date = data.getDateRentier();
		Assert.assertNotNull(date);
		Assert.assertEquals(dateNaissance.addYears(getAgeRentierFemme(paramAppService)), date);
		data.setSexe(Sexe.MASCULIN);
		date = data.getDateRentier();
		Assert.assertNotNull(date);
		Assert.assertEquals(dateNaissance.addYears(getAgeRentierHomme(paramAppService)), date);
	}

	@Test
	public void TestSourcierDataGetDateRentierMenage() {
		final SourcierData data = buildSourcierData(paramAppService);
		final RegDate dateNaissance = date(1990, 1, 1);
		final RegDate dateNaissanceConjoint = date(1970, 1, 1);
		data.setMenage(true);
		data.setSexe(Sexe.MASCULIN);
		data.setDateNaissance(dateNaissance);
		data.setDateNaissanceConjoint(dateNaissanceConjoint);
		RegDate date = data.getDateRentier();
		Assert.assertNotNull(date);
		Assert.assertEquals("Le sexe du conjoint n'est pas renseigné, on ne tient pas compte de sa date de naissance", dateNaissance.addYears(getAgeRentierHomme(paramAppService)), date);
		data.setSexeConjoint(Sexe.FEMININ);
		date = data.getDateRentier();
		Assert.assertNotNull(date);
		Assert.assertEquals("Le conjoint est plus agé, on doit calculer la date de rentier avec ses données", dateNaissanceConjoint.addYears(getAgeRentierFemme(paramAppService)), date);
	}

	@Test
	public void testListeCandidatsPotentiels() throws Exception {

		final long noIndividuRentierOrdinaire = 1L;
		final long noIndividuRentierSource = 2L;
		final long noIndividuActifOrdinaire = 3L;
		final long noIndividuActifSource = 4L;
		final long noIndividuMarieRentierSource = 5L;
		final long noIndividuMarieActifSource = 6L;
		final long noIndividuRentierSourceDejaTraite = 7L;

		final RegDate dateReference = RegDate.get().addDays(-1);
		final RegDate dateNaissanceRentierM = dateReference.addYears(-getAgeRentierHomme(paramAppService)).addMonths(-1);
		final RegDate dateNaissanceActifM = dateReference.addYears(-getAgeRentierHomme(paramAppService)).addMonths(6);
		final RegDate dateDebutPermis = dateReference.addYears(-5);
		final RegDate dateMariage = dateReference.addYears(-3);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu rentierOrdinaire = addIndividu(noIndividuRentierOrdinaire, dateNaissanceRentierM, "Rentier", "Ordinaire", Sexe.MASCULIN);
				addNationalite(rentierOrdinaire, MockPays.Suisse, dateNaissanceRentierM, null);

				final MockIndividu rentierSource = addIndividu(noIndividuRentierSource, dateNaissanceRentierM, "Rentier", "Source", Sexe.MASCULIN);
				addNationalite(rentierSource, MockPays.Espagne, dateNaissanceRentierM, null);
				addPermis(rentierSource, TypePermis.SEJOUR, dateDebutPermis, null, false);

				final MockIndividu rentierSourceDejaTraite = addIndividu(noIndividuRentierSourceDejaTraite, dateNaissanceRentierM, "Rentier des Jatraités", "Source", Sexe.MASCULIN);
				addNationalite(rentierSourceDejaTraite, MockPays.Espagne, dateNaissanceRentierM, null);
				addPermis(rentierSourceDejaTraite, TypePermis.SEJOUR, dateDebutPermis, null, false);

				final MockIndividu actifOrdinaire = addIndividu(noIndividuActifOrdinaire, dateNaissanceActifM, "Actif", "Ordinaire", Sexe.MASCULIN);
				addNationalite(actifOrdinaire, MockPays.Suisse, dateNaissanceActifM, null);

				final MockIndividu actifSource = addIndividu(noIndividuActifSource, dateNaissanceActifM, "Actif", "Source", Sexe.MASCULIN);
				addNationalite(actifSource, MockPays.Espagne, dateNaissanceActifM, null);
				addPermis(actifSource, TypePermis.SEJOUR, dateDebutPermis, null, false);

				final MockIndividu marieRentierSource = addIndividu(noIndividuMarieRentierSource, dateNaissanceRentierM, "Rentier marié", "Source", Sexe.MASCULIN);
				addNationalite(marieRentierSource, MockPays.Espagne, dateNaissanceRentierM, null);
				addPermis(marieRentierSource, TypePermis.SEJOUR, dateDebutPermis, null, false);
				marieIndividu(marieRentierSource, dateMariage);

				final MockIndividu marieActifSource = addIndividu(noIndividuMarieActifSource, dateNaissanceActifM, "Actif marié", "Source", Sexe.MASCULIN);
				addNationalite(marieActifSource, MockPays.Espagne, dateNaissanceActifM, null);
				addPermis(marieActifSource, TypePermis.SEJOUR, dateDebutPermis, null, false);
				marieIndividu(marieActifSource, dateMariage);
			}
		});

		class Ids {
			long ppRentierOrdinaire;
			long ppRentierSource;
			long ppRentierSourceDejaTraite;
			long ppActifOrdinaire;
			long ppActifSource;
			long ppMarieRentierSource;
			long ppMarieActifSource;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique ppRentierOrdinaire = addHabitant(noIndividuRentierOrdinaire);
				addForPrincipal(ppRentierOrdinaire, dateNaissanceRentierM.addYears(18), MotifFor.MAJORITE, MockCommune.Lausanne);

				final PersonnePhysique ppRentierSource = addHabitant(noIndividuRentierSource);
				addForPrincipal(ppRentierSource, dateNaissanceRentierM.addYears(18), MotifFor.MAJORITE, MockCommune.Lausanne, ModeImposition.SOURCE);
				ppRentierSource.setDateNaissance(dateNaissanceRentierM);

				final PersonnePhysique ppRentierSourceDejaTraite = addHabitant(noIndividuRentierSourceDejaTraite);
				addForPrincipal(ppRentierSourceDejaTraite, dateNaissanceRentierM.addYears(18), MotifFor.MAJORITE, MockCommune.Lausanne, ModeImposition.SOURCE);
				ppRentierSourceDejaTraite.setRentierSourcierPasseAuRole(Boolean.TRUE);
				ppRentierSourceDejaTraite.setDateNaissance(dateNaissanceRentierM);

				final PersonnePhysique ppActifOrdinaire = addHabitant(noIndividuActifOrdinaire);
				addForPrincipal(ppActifOrdinaire, dateNaissanceActifM.addYears(18), MotifFor.MAJORITE, MockCommune.Lausanne);

				final PersonnePhysique ppActifSource = addHabitant(noIndividuActifSource);
				addForPrincipal(ppActifSource, dateNaissanceActifM.addYears(18), MotifFor.MAJORITE, MockCommune.Lausanne, ModeImposition.SOURCE);
				ppActifSource.setDateNaissance(dateNaissanceActifM);

				final PersonnePhysique ppMarieRentierSource = addHabitant(noIndividuMarieRentierSource);
				ppMarieRentierSource.setDateNaissance(dateNaissanceRentierM);
				final EnsembleTiersCouple coupleRentierSource = addEnsembleTiersCouple(ppMarieRentierSource, null, dateMariage, null);
				addForPrincipal(coupleRentierSource.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne, ModeImposition.SOURCE);

				final PersonnePhysique ppMarieActifSource = addHabitant(noIndividuMarieActifSource);
				ppMarieActifSource.setDateNaissance(dateNaissanceActifM);
				final EnsembleTiersCouple coupleActifSource = addEnsembleTiersCouple(ppMarieActifSource, null, dateMariage, null);
				addForPrincipal(coupleActifSource.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne, ModeImposition.SOURCE);

				final Ids ids = new Ids();
				ids.ppRentierOrdinaire = ppRentierOrdinaire.getNumero();
				ids.ppRentierSource = ppRentierSource.getNumero();
				ids.ppRentierSourceDejaTraite = ppRentierSourceDejaTraite.getNumero();
				ids.ppActifOrdinaire = ppActifOrdinaire.getNumero();
				ids.ppActifSource = ppActifSource.getNumero();
				ids.ppMarieRentierSource = ppMarieRentierSource.getNumero();
				ids.ppMarieActifSource = ppMarieActifSource.getNumero();
				return ids;
			}
		});

		final List<Long> candidats = processor.getListPotentielsNouveauxRentiersSourciers(dateReference);
		Assert.assertNotNull(candidats);
		Assert.assertEquals(2, candidats.size());
		Assert.assertEquals((Long) ids.ppRentierSource, candidats.get(0));
		Assert.assertEquals((Long) ids.ppMarieRentierSource, candidats.get(1));
	}

	/**
	 * SIFISC-8177
	 */
	@Test
	public void testFlagPassageAnterieur() throws Exception {

		final long noIndividu = 32672315L;
		final RegDate dateReference = RegDate.get();
		final RegDate dateNaissance = dateReference.addYears(-getAgeRentierFemme(paramAppService)).addMonths(-7);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu mme = addIndividu(noIndividu, dateNaissance, "McGonagall", "Minerva", Sexe.FEMININ);
				addNationalite(mme, MockPays.RoyaumeUni, dateNaissance, null);
				addPermis(mme, TypePermis.SEJOUR, dateNaissance, null, false);
				addAdresse(mme, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, dateNaissance, null);
			}
		});

		// mise en place fiscale avant le premier passage
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Echallens, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		// première passe : on verifie qu'un nouveau for a bien été créé en mixte
		{
			final PassageNouveauxRentiersSourciersEnMixteResults res = processor.run(dateReference, null);
			Assert.assertNotNull(res);
			Assert.assertEquals(1, res.getNbSourciersTotal());
			Assert.assertEquals(0, res.nbSourciersConjointsIgnores);
			Assert.assertEquals(0, res.nbSourciersHorsSuisse);
			Assert.assertEquals(0, res.nbSourciersTropJeunes);
			Assert.assertEquals(0, res.sourciersEnErreurs.size());
			Assert.assertEquals(1, res.sourciersConvertis.size());

			final PassageNouveauxRentiersSourciersEnMixteResults.Traite converti = res.sourciersConvertis.get(0);
			Assert.assertNotNull(converti);
			Assert.assertEquals((Long) ppId, converti.noCtb);
			Assert.assertEquals(dateNaissance.addYears(getAgeRentierFemme(paramAppService)), converti.dateOuverture);

			// vérification des fors
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
					Assert.assertNotNull(pp);
					Assert.assertTrue(pp.getRentierSourcierPasseAuRole());

					final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
					Assert.assertNotNull(ffp);
					Assert.assertEquals(dateNaissance.addYears(getAgeRentierFemme(paramAppService)), ffp.getDateDebut());
					Assert.assertEquals(MotifFor.CHGT_MODE_IMPOSITION, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(ModeImposition.MIXTE_137_1, ffp.getModeImposition());
					return null;
				}
			});
		}

		// maintenant, on va annuler le for créé (dans l'idée qu'un second passage du batch ne devrait pas le re-créer)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);

				final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
				tiersService.annuleForFiscal(ffp);
				return null;
			}
		});

		// vérification du for actif
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);

				final ForFiscalPrincipalPP newFfp = pp.getDernierForFiscalPrincipal();
				Assert.assertNotNull(newFfp);
				Assert.assertEquals(dateNaissance.addYears(18), newFfp.getDateDebut());
				Assert.assertEquals(MotifFor.MAJORITE, newFfp.getMotifOuverture());
				Assert.assertNull(newFfp.getDateFin());
				Assert.assertNull(newFfp.getMotifFermeture());
				Assert.assertEquals(ModeImposition.SOURCE, newFfp.getModeImposition());
				return null;
			}
		});

		{
			final PassageNouveauxRentiersSourciersEnMixteResults res = processor.run(dateReference, null);
			Assert.assertNotNull(res);
			Assert.assertEquals(0, res.getNbSourciersTotal());
			Assert.assertEquals(0, res.nbSourciersConjointsIgnores);
			Assert.assertEquals(0, res.nbSourciersHorsSuisse);
			Assert.assertEquals(0, res.nbSourciersTropJeunes);
			Assert.assertEquals(0, res.sourciersEnErreurs.size());
			Assert.assertEquals(0, res.sourciersConvertis.size());

			// vérification des fors
			doInNewTransactionAndSession(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
					Assert.assertNotNull(pp);
					Assert.assertTrue(pp.getRentierSourcierPasseAuRole());

					final ForFiscalPrincipalPP ffp = pp.getDernierForFiscalPrincipal();
					Assert.assertNotNull(ffp);
					Assert.assertEquals(dateNaissance.addYears(18), ffp.getDateDebut());
					Assert.assertEquals(MotifFor.MAJORITE, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(ModeImposition.SOURCE, ffp.getModeImposition());
					return null;
				}
			});
		}
	}

}


