package ch.vd.uniregctb.tiers.rattrapage.ancienshabitants;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class RecuperationDonneesAnciensHabitantsProcessorTest extends BusinessTest {

	private RecuperationDonneesAnciensHabitantsProcessor processor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		processor = new RecuperationDonneesAnciensHabitantsProcessor(hibernateTemplate, transactionManager, tiersDAO, serviceCivil);
	}

	@Test
	public void testCasIgnores() throws Exception {

		final long noIndividuEncoreHabitant = 1L;
		final long noIndividuSansDonneesCivilesParents = 2L;
		final long noIndividuDejaToutConnu = 3L;
		final long noIndividuMereSeuleConnueFiscalement = 4L;
		final long noIndividuPereSeulConnuFiscalement = 5L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				{
					final MockIndividu ind = addIndividu(noIndividuEncoreHabitant, null, "Habitant", "Encore", Sexe.MASCULIN);
					addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, date(2009, 1, 1), null);
				}
				{
					addIndividu(noIndividuSansDonneesCivilesParents, null, "Lin", "Orphe", Sexe.MASCULIN);
				}
				{
					final MockIndividu ind = addIndividu(noIndividuDejaToutConnu, null, "Connu", "Touté", Sexe.FEMININ);
					ind.setNomOfficielMere(new NomPrenom("Petibois", "Alphonsine Gilberte"));
					ind.setNomOfficielPere(new NomPrenom("Boiserie", "Gérard"));
				}
				{
					final MockIndividu ind = addIndividu(noIndividuMereSeuleConnueFiscalement, null, "Connu", "Seulemaire", Sexe.FEMININ);
					ind.setNomOfficielMere(new NomPrenom("Petibois", "Alphonsine Gilberte"));
				}
				{
					final MockIndividu ind = addIndividu(noIndividuPereSeulConnuFiscalement, null, "Connu", "Seulpaire", Sexe.MASCULIN);
					ind.setNomOfficielPere(new NomPrenom("Boiserie", "Gérard"));
				}
			}
		});

		final class Ids {
			long idEncoreHabitant;
			long idJamaisHabitant;
			long idSansDonneesCiviles;
			long idToutConnu;
			long idMereSeuleConnue;
			long idPereSeulConnu;
			long idMenage;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique encoreHabitant = addHabitant(noIndividuEncoreHabitant);
				final PersonnePhysique jamaisHabitant = addNonHabitant("Jamais", "Vu", null, Sexe.MASCULIN);
				final PersonnePhysique sansDonneesCiviles = tiersService.createNonHabitantFromIndividu(noIndividuSansDonneesCivilesParents);
				final PersonnePhysique dejaToutConnu = tiersService.createNonHabitantFromIndividu(noIndividuDejaToutConnu);
				final PersonnePhysique mereSeuleConnue = tiersService.createNonHabitantFromIndividu(noIndividuMereSeuleConnueFiscalement);
				final PersonnePhysique pereSeulConnu = tiersService.createNonHabitantFromIndividu(noIndividuPereSeulConnuFiscalement);

				final PersonnePhysique marie = addNonHabitant("Avoir", "Rien", null, Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(marie, null, date(2010, 6, 3), null);
				final MenageCommun mc = couple.getMenage();

				final Ids ids = new Ids();
				ids.idEncoreHabitant = encoreHabitant.getNumero();
				ids.idJamaisHabitant = jamaisHabitant.getNumero();
				ids.idSansDonneesCiviles = sansDonneesCiviles.getNumero();
				ids.idToutConnu = dejaToutConnu.getNumero();
				ids.idMereSeuleConnue = mereSeuleConnue.getNumero();
				ids.idPereSeulConnu = pereSeulConnu.getNumero();
				ids.idMenage = mc.getNumero();
				return ids;
			}
		});

		// lancement du processus
		final RecuperationDonneesAnciensHabitantsResults res = processor.run(1, false, true, false, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(4, res.getIgnores().size());        // les habitant, jamais habitant et ménage commun ne sont même pas pris en compte
		Assert.assertEquals(0, res.getTraites().size());

		// vérification des résultats du rapport
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoIgnore info = res.getIgnores().get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals(ids.idSansDonneesCiviles, info.noCtb);
			Assert.assertEquals(RecuperationDonneesAnciensHabitantsResults.RaisonIgnorement.RIEN_DANS_CIVIL, info.raison);
		}
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoIgnore info = res.getIgnores().get(1);
			Assert.assertNotNull(info);
			Assert.assertEquals(ids.idToutConnu, info.noCtb);
			Assert.assertEquals(RecuperationDonneesAnciensHabitantsResults.RaisonIgnorement.VALEUR_DEJA_PRESENTE, info.raison);
		}
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoIgnore info = res.getIgnores().get(2);
			Assert.assertNotNull(info);
			Assert.assertEquals(ids.idMereSeuleConnue, info.noCtb);
			Assert.assertEquals(RecuperationDonneesAnciensHabitantsResults.RaisonIgnorement.VALEUR_DEJA_PRESENTE, info.raison);
		}
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoIgnore info = res.getIgnores().get(3);
			Assert.assertNotNull(info);
			Assert.assertEquals(ids.idPereSeulConnu, info.noCtb);
			Assert.assertEquals(RecuperationDonneesAnciensHabitantsResults.RaisonIgnorement.VALEUR_DEJA_PRESENTE, info.raison);
		}

		// vérification des résultats en base (= rien ne doit avoir bougé)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idEncoreHabitant);
					Assert.assertNotNull(pp);
					Assert.assertNull(pp.getPrenomsMere());
					Assert.assertNull(pp.getNomMere());
					Assert.assertNull(pp.getPrenomsPere());
					Assert.assertNull(pp.getNomPere());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idJamaisHabitant);
					Assert.assertNotNull(pp);
					Assert.assertNull(pp.getPrenomsMere());
					Assert.assertNull(pp.getNomMere());
					Assert.assertNull(pp.getPrenomsPere());
					Assert.assertNull(pp.getNomPere());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idSansDonneesCiviles);
					Assert.assertNotNull(pp);
					Assert.assertNull(pp.getPrenomsMere());
					Assert.assertNull(pp.getNomMere());
					Assert.assertNull(pp.getPrenomsPere());
					Assert.assertNull(pp.getNomPere());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idToutConnu);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Alphonsine Gilberte", pp.getPrenomsMere());
					Assert.assertEquals("Petibois", pp.getNomMere());
					Assert.assertEquals("Gérard", pp.getPrenomsPere());
					Assert.assertEquals("Boiserie", pp.getNomPere());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idMereSeuleConnue);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Alphonsine Gilberte", pp.getPrenomsMere());
					Assert.assertEquals("Petibois", pp.getNomMere());
					Assert.assertNull(pp.getPrenomsPere());
					Assert.assertNull(pp.getNomPere());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idPereSeulConnu);
					Assert.assertNotNull(pp);
					Assert.assertNull(pp.getPrenomsMere());
					Assert.assertNull(pp.getNomMere());
					Assert.assertEquals("Gérard", pp.getPrenomsPere());
					Assert.assertEquals("Boiserie", pp.getNomPere());
				}
			}
		});
	}

	private static void resetDataNomsParents(PersonnePhysique pp, boolean resetMere, boolean resetPere) {
		if (resetMere) {
			initDataNomsMere(pp, null, null);
		}
		if (resetPere) {
			initDataNomsPere(pp, null, null);
		}
	}

	private static void initDataNomsMere(PersonnePhysique pp, String prenomsMere, String nomMere) {
		pp.setPrenomsMere(prenomsMere);
		pp.setNomMere(nomMere);
	}

	private static void initDataNomsPere(PersonnePhysique pp, String prenomsPere, String nomPere) {
		pp.setPrenomsPere(prenomsPere);
		pp.setNomPere(nomPere);
	}

	private static void initDataNomsParents(PersonnePhysique pp, String prenomsMere, String nomMere, String prenomsPere, String nomPere) {
		initDataNomsMere(pp, prenomsMere, nomMere);
		initDataNomsPere(pp, prenomsPere, nomPere);
	}

	@Test
	public void testCasTraitesSansForcage() throws Exception {

		final long noIndividuMereSeule = 1L;
		final long noIndividuPereSeul = 2L;
		final long noIndividuTout = 3L;
		final long noIndividuToutMereRafraichie = 4L;
		final long noIndividuToutPereRafraichi = 5L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				{
					final MockIndividu ind = addIndividu(noIndividuMereSeule, null, "Connu", "Seulemaire", Sexe.FEMININ);
					ind.setNomOfficielMere(new NomPrenom("Dubois", "Alphonsine Gilberte"));
				}
				{
					final MockIndividu ind = addIndividu(noIndividuPereSeul, null, "Connu", "Seulpaire", Sexe.MASCULIN);
					ind.setNomOfficielPere(new NomPrenom("Deschamps", "Gérard"));
				}
				{
					final MockIndividu ind = addIndividu(noIndividuTout, null, "Connu", "Touté", Sexe.FEMININ);
					ind.setNomOfficielMere(new NomPrenom("Dubois", "Alphonsine Gilberte"));
					ind.setNomOfficielPere(new NomPrenom("Deschamps", "Gérard"));
				}
				{
					final MockIndividu ind = addIndividu(noIndividuToutMereRafraichie, null, "Rafraîchi", "Maire", Sexe.FEMININ);
					ind.setNomOfficielMere(new NomPrenom("Dubois", "Alphonsine Gilberte"));
					ind.setNomOfficielPere(new NomPrenom("Deschamps", "Gérard"));
				}
				{
					final MockIndividu ind = addIndividu(noIndividuToutPereRafraichi, null, "Rafraîchi", "Paire", Sexe.FEMININ);
					ind.setNomOfficielMere(new NomPrenom("Dubois", "Alphonsine Gilberte"));
					ind.setNomOfficielPere(new NomPrenom("Deschamps", "Gérard"));
				}
			}
		});

		final class Ids {
			long idMereSeule;
			long idPereSeul;
			long idTout;
			long idToutMereRafraichie;
			long idToutPereRafraichi;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique mereSeule = tiersService.createNonHabitantFromIndividu(noIndividuMereSeule);
				resetDataNomsParents(mereSeule, true, true);

				final PersonnePhysique pereSeul = tiersService.createNonHabitantFromIndividu(noIndividuPereSeul);
				resetDataNomsParents(pereSeul, true, true);

				final PersonnePhysique tout = tiersService.createNonHabitantFromIndividu(noIndividuTout);
				resetDataNomsParents(tout, true, true);

				final PersonnePhysique toutMereConnue = tiersService.createNonHabitantFromIndividu(noIndividuToutMereRafraichie);
				initDataNomsParents(toutMereConnue, null, null, "Papa", "Duchesne");

				final PersonnePhysique toutPereConnu = tiersService.createNonHabitantFromIndividu(noIndividuToutPereRafraichi);
				initDataNomsParents(toutPereConnu, "Maman", "Delaforêt", null, null);

				final Ids ids = new Ids();
				ids.idMereSeule = mereSeule.getNumero();
				ids.idPereSeul = pereSeul.getNumero();
				ids.idTout = tout.getNumero();
				ids.idToutMereRafraichie = toutMereConnue.getNumero();
				ids.idToutPereRafraichi = toutPereConnu.getNumero();
				return ids;
			}
		});

		// lancement du processus
		final RecuperationDonneesAnciensHabitantsResults res = processor.run(1, false, true, false, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(5, res.getTraites().size());

		// vérification des résultats dans le rapport
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoTraite info = res.getTraites().get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals(ids.idMereSeule, info.noCtb);
			Assert.assertTrue(info.majMere);
			Assert.assertEquals("Alphonsine Gilberte", info.prenomsMere);
			Assert.assertEquals("Dubois", info.nomMere);
			Assert.assertFalse(info.majPere);
			Assert.assertNull(info.prenomsPere);
			Assert.assertNull(info.nomPere);
		}
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoTraite info = res.getTraites().get(1);
			Assert.assertNotNull(info);
			Assert.assertEquals(ids.idPereSeul, info.noCtb);
			Assert.assertFalse(info.majMere);
			Assert.assertNull(info.prenomsMere);
			Assert.assertNull(info.nomMere);
			Assert.assertTrue(info.majPere);
			Assert.assertEquals("Gérard", info.prenomsPere);
			Assert.assertEquals("Deschamps", info.nomPere);
		}
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoTraite info = res.getTraites().get(2);
			Assert.assertNotNull(info);
			Assert.assertEquals(ids.idTout, info.noCtb);
			Assert.assertTrue(info.majMere);
			Assert.assertEquals("Alphonsine Gilberte", info.prenomsMere);
			Assert.assertEquals("Dubois", info.nomMere);
			Assert.assertTrue(info.majPere);
			Assert.assertEquals("Gérard", info.prenomsPere);
			Assert.assertEquals("Deschamps", info.nomPere);
		}
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoTraite info = res.getTraites().get(3);
			Assert.assertNotNull(info);
			Assert.assertEquals(ids.idToutMereRafraichie, info.noCtb);
			Assert.assertTrue(info.majMere);
			Assert.assertEquals("Alphonsine Gilberte", info.prenomsMere);
			Assert.assertEquals("Dubois", info.nomMere);
			Assert.assertFalse(info.majPere);
			Assert.assertEquals("Papa", info.prenomsPere);
			Assert.assertEquals("Duchesne", info.nomPere);
		}
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoTraite info = res.getTraites().get(4);
			Assert.assertNotNull(info);
			Assert.assertEquals(ids.idToutPereRafraichi, info.noCtb);
			Assert.assertFalse(info.majMere);
			Assert.assertEquals("Maman", info.prenomsMere);
			Assert.assertEquals("Delaforêt", info.nomMere);
			Assert.assertTrue(info.majPere);
			Assert.assertEquals("Gérard", info.prenomsPere);
			Assert.assertEquals("Deschamps", info.nomPere);
		}

		// vérification des données en base (= les mises-à-jour doivent avoir eu lieu)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idMereSeule);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Alphonsine Gilberte", pp.getPrenomsMere());
					Assert.assertEquals("Dubois", pp.getNomMere());
					Assert.assertNull(pp.getPrenomsPere());
					Assert.assertNull(pp.getNomPere());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idPereSeul);
					Assert.assertNotNull(pp);
					Assert.assertNull(pp.getPrenomsMere());
					Assert.assertNull(pp.getNomMere());
					Assert.assertEquals("Gérard", pp.getPrenomsPere());
					Assert.assertEquals("Deschamps", pp.getNomPere());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idTout);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Alphonsine Gilberte", pp.getPrenomsMere());
					Assert.assertEquals("Dubois", pp.getNomMere());
					Assert.assertEquals("Gérard", pp.getPrenomsPere());
					Assert.assertEquals("Deschamps", pp.getNomPere());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idToutMereRafraichie);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Alphonsine Gilberte", pp.getPrenomsMere());
					Assert.assertEquals("Dubois", pp.getNomMere());
					Assert.assertEquals("Papa", pp.getPrenomsPere());
					Assert.assertEquals("Duchesne", pp.getNomPere());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idToutPereRafraichi);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Maman", pp.getPrenomsMere());
					Assert.assertEquals("Delaforêt", pp.getNomMere());
					Assert.assertEquals("Gérard", pp.getPrenomsPere());
					Assert.assertEquals("Deschamps", pp.getNomPere());
				}
			}
		});
	}

	@Test
	public void casTraitesAvecForcage() throws Exception {

		final long noIndividuMereSeule = 1L;
		final long noIndividuPereSeul = 2L;
		final long noIndividuTout = 3L;
		final long noIndividuToutMereRafraichie = 4L;
		final long noIndividuToutPereRafraichi = 5L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				{
					final MockIndividu ind = addIndividu(noIndividuMereSeule, null, "Connu", "Seulemaire", Sexe.FEMININ);
					ind.setNomOfficielMere(new NomPrenom("Dubois", "Alphonsine Gilberte"));
				}
				{
					final MockIndividu ind = addIndividu(noIndividuPereSeul, null, "Connu", "Seulpaire", Sexe.MASCULIN);
					ind.setNomOfficielPere(new NomPrenom("Deschamps", "Gérard"));
				}
				{
					final MockIndividu ind = addIndividu(noIndividuTout, null, "Connu", "Touté", Sexe.FEMININ);
					ind.setNomOfficielMere(new NomPrenom("Dubois", "Alphonsine Gilberte"));
					ind.setNomOfficielPere(new NomPrenom("Deschamps", "Gérard"));
				}
				{
					final MockIndividu ind = addIndividu(noIndividuToutMereRafraichie, null, "Rafraîchi", "Maire", Sexe.FEMININ);
					ind.setNomOfficielMere(new NomPrenom("Dubois", "Alphonsine Gilberte"));
					ind.setNomOfficielPere(new NomPrenom("Deschamps", "Gérard"));
				}
				{
					final MockIndividu ind = addIndividu(noIndividuToutPereRafraichi, null, "Rafraîchi", "Paire", Sexe.FEMININ);
					ind.setNomOfficielMere(new NomPrenom("Dubois", "Alphonsine Gilberte"));
					ind.setNomOfficielPere(new NomPrenom("Deschamps", "Gérard"));
				}
			}
		});

		final class Ids {
			long idMereSeule;
			long idPereSeul;
			long idTout;
			long idToutMereRafraichie;
			long idToutPereRafraichi;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique mereSeule = tiersService.createNonHabitantFromIndividu(noIndividuMereSeule);
				resetDataNomsParents(mereSeule, true, true);

				final PersonnePhysique pereSeul = tiersService.createNonHabitantFromIndividu(noIndividuPereSeul);
				resetDataNomsParents(pereSeul, true, true);

				final PersonnePhysique tout = tiersService.createNonHabitantFromIndividu(noIndividuTout);
				resetDataNomsParents(tout, true, true);

				final PersonnePhysique toutMereConnue = tiersService.createNonHabitantFromIndividu(noIndividuToutMereRafraichie);
				initDataNomsParents(toutMereConnue, null, null, "Papa", "Duchesne");

				final PersonnePhysique toutPereConnu = tiersService.createNonHabitantFromIndividu(noIndividuToutPereRafraichi);
				initDataNomsParents(toutPereConnu, "Maman", "Delaforêt", null, null);

				final Ids ids = new Ids();
				ids.idMereSeule = mereSeule.getNumero();
				ids.idPereSeul = pereSeul.getNumero();
				ids.idTout = tout.getNumero();
				ids.idToutMereRafraichie = toutMereConnue.getNumero();
				ids.idToutPereRafraichi = toutPereConnu.getNumero();
				return ids;
			}
		});

		// lancement du processus
		final RecuperationDonneesAnciensHabitantsResults res = processor.run(1, true, true, false, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(5, res.getTraites().size());

		// vérification des résultats dans le rapport
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoTraite info = res.getTraites().get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals(ids.idMereSeule, info.noCtb);
			Assert.assertTrue(info.majMere);
			Assert.assertEquals("Alphonsine Gilberte", info.prenomsMere);
			Assert.assertEquals("Dubois", info.nomMere);
			Assert.assertFalse(info.majPere);
			Assert.assertNull(info.prenomsPere);
			Assert.assertNull(info.nomPere);
		}
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoTraite info = res.getTraites().get(1);
			Assert.assertNotNull(info);
			Assert.assertEquals(ids.idPereSeul, info.noCtb);
			Assert.assertFalse(info.majMere);
			Assert.assertNull(info.prenomsMere);
			Assert.assertNull(info.nomMere);
			Assert.assertTrue(info.majPere);
			Assert.assertEquals("Gérard", info.prenomsPere);
			Assert.assertEquals("Deschamps", info.nomPere);
		}
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoTraite info = res.getTraites().get(2);
			Assert.assertNotNull(info);
			Assert.assertEquals(ids.idTout, info.noCtb);
			Assert.assertTrue(info.majMere);
			Assert.assertEquals("Alphonsine Gilberte", info.prenomsMere);
			Assert.assertEquals("Dubois", info.nomMere);
			Assert.assertTrue(info.majPere);
			Assert.assertEquals("Gérard", info.prenomsPere);
			Assert.assertEquals("Deschamps", info.nomPere);
		}
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoTraite info = res.getTraites().get(3);
			Assert.assertNotNull(info);
			Assert.assertEquals(ids.idToutMereRafraichie, info.noCtb);
			Assert.assertTrue(info.majMere);
			Assert.assertEquals("Alphonsine Gilberte", info.prenomsMere);
			Assert.assertEquals("Dubois", info.nomMere);
			Assert.assertTrue(info.majPere);
			Assert.assertEquals("Gérard", info.prenomsPere);
			Assert.assertEquals("Deschamps", info.nomPere);
		}
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoTraite info = res.getTraites().get(4);
			Assert.assertNotNull(info);
			Assert.assertEquals(ids.idToutPereRafraichi, info.noCtb);
			Assert.assertTrue(info.majMere);
			Assert.assertEquals("Alphonsine Gilberte", info.prenomsMere);
			Assert.assertEquals("Dubois", info.nomMere);
			Assert.assertTrue(info.majPere);
			Assert.assertEquals("Gérard", info.prenomsPere);
			Assert.assertEquals("Deschamps", info.nomPere);
		}

		// vérification des données en base (= les mises-à-jour doivent avoir eu lieu)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idMereSeule);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Alphonsine Gilberte", pp.getPrenomsMere());
					Assert.assertEquals("Dubois", pp.getNomMere());
					Assert.assertNull(pp.getPrenomsPere());
					Assert.assertNull(pp.getNomPere());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idPereSeul);
					Assert.assertNotNull(pp);
					Assert.assertNull(pp.getPrenomsMere());
					Assert.assertNull(pp.getNomMere());
					Assert.assertEquals("Gérard", pp.getPrenomsPere());
					Assert.assertEquals("Deschamps", pp.getNomPere());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idTout);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Alphonsine Gilberte", pp.getPrenomsMere());
					Assert.assertEquals("Dubois", pp.getNomMere());
					Assert.assertEquals("Gérard", pp.getPrenomsPere());
					Assert.assertEquals("Deschamps", pp.getNomPere());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idToutMereRafraichie);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Alphonsine Gilberte", pp.getPrenomsMere());
					Assert.assertEquals("Dubois", pp.getNomMere());
					Assert.assertEquals("Gérard", pp.getPrenomsPere());
					Assert.assertEquals("Deschamps", pp.getNomPere());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idToutPereRafraichi);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Alphonsine Gilberte", pp.getPrenomsMere());
					Assert.assertEquals("Dubois", pp.getNomMere());
					Assert.assertEquals("Gérard", pp.getPrenomsPere());
					Assert.assertEquals("Deschamps", pp.getNomPere());
				}
			}
		});
	}

	@Test
	public void testIgnorePrenomsRienDansCivil() throws Exception {

		final long noIndividu = 2367342L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "De Saint André", "Mercédes", Sexe.FEMININ);
			}
		});

		// mise en place fisccale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				return pp.getNumero();
			}
		});

		// lancement du rattrapage
		final RecuperationDonneesAnciensHabitantsResults res = processor.run(1, false, false, true, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(1, res.getIgnores().size());
		Assert.assertEquals(0, res.getTraites().size());

		// vérification des résultats dans le rapport
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoIgnore info = res.getIgnores().get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals(ppId, info.noCtb);
			Assert.assertEquals(RecuperationDonneesAnciensHabitantsResults.RaisonIgnorement.RIEN_DANS_CIVIL, info.raison);
		}

		// vérification en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNull(pp.getTousPrenoms());
			}
		});
	}

	@Test
	public void testIgnorePrenomsDejaConnus() throws Exception {

		final long noIndividu = 2367342L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "De Saint André", "Mercédes", Sexe.FEMININ);
				individu.setTousPrenoms("Mercédes Anne Marie");
			}
		});

		// mise en place fisccale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				pp.setTousPrenoms("Mercédes");
				return pp.getNumero();
			}
		});

		// lancement du rattrapage
		final RecuperationDonneesAnciensHabitantsResults res = processor.run(1, false, false, true, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(1, res.getIgnores().size());
		Assert.assertEquals(0, res.getTraites().size());

		// vérification des résultats dans le rapport
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoIgnore info = res.getIgnores().get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals(ppId, info.noCtb);
			Assert.assertEquals(RecuperationDonneesAnciensHabitantsResults.RaisonIgnorement.VALEUR_DEJA_PRESENTE, info.raison);
		}

		// vérification en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertEquals("Mercédes", pp.getTousPrenoms());
			}
		});
	}

	@Test
	public void testReprisePrenomsVides() throws Exception {

		final long noIndividu = 2367342L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "De Saint André", "Mercédes", Sexe.FEMININ);
				individu.setTousPrenoms("Mercédes Anne Marie");
				individu.setNomOfficielMere(new NomPrenom("De Saint André", "Alix"));
				individu.setNomOfficielPere(new NomPrenom("De Saint André", "Godefroy"));
			}
		});

		// mise en place fisccale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				pp.setTousPrenoms(null);
				resetDataNomsParents(pp, true, true);
				return pp.getNumero();
			}
		});

		// lancement du rattrapage
		final RecuperationDonneesAnciensHabitantsResults res = processor.run(1, false, false, true, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(1, res.getTraites().size());

		// vérification des résultats dans le rapport
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoTraite info = res.getTraites().get(0);
			Assert.assertNotNull(info);
			Assert.assertFalse(info.majMere);
			Assert.assertNull(info.prenomsMere);
			Assert.assertNull(info.nomMere);
			Assert.assertFalse(info.majPere);
			Assert.assertNull(info.prenomsPere);
			Assert.assertNull(info.nomPere);
			Assert.assertEquals(ppId, info.noCtb);
			Assert.assertTrue(info.majPrenoms);
			Assert.assertEquals("Mercédes Anne Marie", info.tousPrenoms);
		}

		// vérification en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertEquals("Mercédes Anne Marie", pp.getTousPrenoms());
				Assert.assertNull(pp.getPrenomsMere());
				Assert.assertNull(pp.getNomMere());
				Assert.assertNull(pp.getPrenomsPere());
				Assert.assertNull(pp.getNomPere());
			}
		});
	}

	@Test
	public void testReprisePrenomsForcage() throws Exception {

		final long noIndividu = 2367342L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "De Saint André", "Mercédes", Sexe.FEMININ);
				individu.setTousPrenoms("Mercédes Anne Marie");
				individu.setNomOfficielMere(new NomPrenom("De Saint André", "Alix"));
				individu.setNomOfficielPere(new NomPrenom("De Saint André", "Godefroy"));
			}
		});

		// mise en place fisccale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				pp.setTousPrenoms("Mercédes");
				resetDataNomsParents(pp, true, true);
				return pp.getNumero();
			}
		});

		// lancement du rattrapage
		final RecuperationDonneesAnciensHabitantsResults res = processor.run(1, true, false, true, null);
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.getErreurs().size());
		Assert.assertEquals(0, res.getIgnores().size());
		Assert.assertEquals(1, res.getTraites().size());

		// vérification des résultats dans le rapport
		{
			final RecuperationDonneesAnciensHabitantsResults.InfoTraite info = res.getTraites().get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals(ppId, info.noCtb);
			Assert.assertFalse(info.majMere);
			Assert.assertNull(info.prenomsMere);
			Assert.assertNull(info.nomMere);
			Assert.assertFalse(info.majPere);
			Assert.assertNull(info.prenomsPere);
			Assert.assertNull(info.nomPere);
			Assert.assertTrue(info.majPrenoms);
			Assert.assertEquals("Mercédes Anne Marie", info.tousPrenoms);
		}

		// vérification en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertEquals("Mercédes Anne Marie", pp.getTousPrenoms());
				Assert.assertNull(pp.getPrenomsMere());
				Assert.assertNull(pp.getNomMere());
				Assert.assertNull(pp.getPrenomsPere());
				Assert.assertNull(pp.getNomPere());
			}
		});
	}
}
