package ch.vd.uniregctb.tiers.rattrapage.origine;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.OriginePersonnePhysique;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;

public class RecuperationOriginesNonHabitantsProcessorTest extends BusinessTest {

	private RecuperationOriginesNonHabitantsProcessor processor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		processor = new RecuperationOriginesNonHabitantsProcessor(hibernateTemplate, transactionManager, serviceCivil, serviceInfra);
	}

	@Test
	public void testHabitant() throws Exception {

		final long noIndividu = 4267242L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Duschmol", "Patrick", Sexe.MASCULIN);
				addOrigine(ind, MockCommune.Bale);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero();
			}
		});

		// lancement du job
		final RecuperationOriginesNonHabitantsResults results = processor.run(1, false, null);
		Assert.assertNotNull(results);

		// rien vu, aucun non-habitant présent...
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getTraites().size());
	}

	@Test
	public void testNonHabitantConnuAuCivilSansOrigine() throws Exception {

		final long noIndividu = 5738953687L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, null, "Duschmol", "Patrick", Sexe.MASCULIN);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				return pp.getNumero();
			}
		});

		// lancement du job
		final RecuperationOriginesNonHabitantsResults results = processor.run(1, false, null);
		Assert.assertNotNull(results);

		// ignoré car pas d'origine civile connue
		Assert.assertEquals(1, results.getIgnores().size());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getTraites().size());

		// validation de la raison exacte
		final RecuperationOriginesNonHabitantsResults.InfoIgnore ignore = results.getIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(ppId, ignore.noCtb);
		Assert.assertEquals(RecuperationOriginesNonHabitantsResults.RaisonIgnorement.AUCUNE_ORIGINE_CIVILE_CONNUE.getLibelle(), ignore.getMessage());
	}

	@Test
	public void testNonHabitantInconnuAuCivilSansLibelleAssigne() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Patricia", "Duschmol", null, Sexe.FEMININ);
				return pp.getNumero();
			}
		});

		// lancement du job
		final RecuperationOriginesNonHabitantsResults results = processor.run(1, false, null);
		Assert.assertNotNull(results);

		// ignoré car pas d'origine civile connue
		Assert.assertEquals(1, results.getIgnores().size());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getTraites().size());

		// validation de la raison exacte
		final RecuperationOriginesNonHabitantsResults.InfoIgnore ignore = results.getIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(ppId, ignore.noCtb);
		Assert.assertEquals(RecuperationOriginesNonHabitantsResults.RaisonIgnorement.NON_HABITANT_SANS_LIBELLE_ORIGINE.getLibelle(), ignore.getMessage());
	}

	@Test
	public void testAncienHabitantAvecOrigineCivileConnue() throws Exception {

		final long noIndividu = 5738953687L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Duschmol", "Patrick", Sexe.MASCULIN);
				addOrigine(ind, MockCommune.Bern);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				pp.setLibelleCommuneOrigine(null);
				pp.setOrigine(null);
				return pp.getNumero();
			}
		});

		// lancement du job
		final RecuperationOriginesNonHabitantsResults results = processor.run(1, false, null);
		Assert.assertNotNull(results);

		// traité car le civil savait...
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getTraites().size());

		// validation du traitement
		final RecuperationOriginesNonHabitantsResults.InfoTraitement traite = results.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(ppId, traite.noCtb);
		Assert.assertEquals(MockCommune.Bern.getNomOfficiel(), traite.getLibelle());
		Assert.assertEquals(MockCommune.Bern.getSigleCanton(), traite.getSigleCanton());

		// il faut encore vérifier le contenu de la base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);

				final OriginePersonnePhysique origine = pp.getOrigine();
				Assert.assertNotNull(origine);
				Assert.assertEquals(MockCommune.Bern.getNomOfficiel(), origine.getLibelle());
				Assert.assertEquals(MockCommune.Bern.getSigleCanton(), origine.getSigleCanton());
			}
		});
	}

	@Test
	public void testAncienHabitantAvecOrigineCivileConnueAussiFiscalement() throws Exception {

		final long noIndividu = 5738953687L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Duschmol", "Patrick", Sexe.MASCULIN);
				addOrigine(ind, MockCommune.Bern);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				pp.setLibelleCommuneOrigine(null);

				Assert.assertNotNull(pp.getOrigine());
				Assert.assertEquals(MockCommune.Bern.getNomOfficiel(), pp.getOrigine().getLibelle());
				Assert.assertEquals(MockCommune.Bern.getSigleCanton(), pp.getOrigine().getSigleCanton());

				return pp.getNumero();
			}
		});

		// lancement du job
		final RecuperationOriginesNonHabitantsResults results = processor.run(1, false, null);
		Assert.assertNotNull(results);

		// ignoré, car le fiscal savait déjà
		Assert.assertEquals(1, results.getIgnores().size());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getTraites().size());

		// validation de la raison exacte
		final RecuperationOriginesNonHabitantsResults.InfoIgnore ignore = results.getIgnores().get(0);
		Assert.assertNotNull(ignore);
		Assert.assertEquals(ppId, ignore.noCtb);
		Assert.assertEquals(RecuperationOriginesNonHabitantsResults.RaisonIgnorement.VALEUR_DEJA_PRESENTE.getLibelle(), ignore.getMessage());

		// il faut encore vérifier le contenu de la base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);

				final OriginePersonnePhysique origine = pp.getOrigine();
				Assert.assertNotNull(origine);
				Assert.assertEquals(MockCommune.Bern.getNomOfficiel(), origine.getLibelle());
				Assert.assertEquals(MockCommune.Bern.getSigleCanton(), origine.getSigleCanton());
			}
		});
	}

	@Test
	public void testAncienHabitantAvecOrigineCivileConnueDryRun() throws Exception {

		final long noIndividu = 5738953687L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Duschmol", "Patrick", Sexe.MASCULIN);
				addOrigine(ind, MockCommune.Bern);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
				pp.setLibelleCommuneOrigine(null);
				pp.setOrigine(null);
				return pp.getNumero();
			}
		});

		// lancement du job
		final RecuperationOriginesNonHabitantsResults results = processor.run(1, true, null);
		Assert.assertNotNull(results);

		// traité car le civil savait...
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getTraites().size());

		// validation du traitement
		final RecuperationOriginesNonHabitantsResults.InfoTraitement traite = results.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(ppId, traite.noCtb);
		Assert.assertEquals(MockCommune.Bern.getNomOfficiel(), traite.getLibelle());
		Assert.assertEquals(MockCommune.Bern.getSigleCanton(), traite.getSigleCanton());

		// il faut encore vérifier le contenu de la base (qui ne doit pas avoir bougé, nous sommes en simulation)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertNull(pp.getOrigine());
			}
		});
	}

	@Test
	public void testNonHabitantAvecLibelleReconnu() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alicia", "Raquette", null, Sexe.FEMININ);
				pp.setLibelleCommuneOrigine("geNeVE");      // décidément, il y en a qui écrivent n'importe comment...
				return pp.getNumero();
			}
		});

		// lancement du job
		final RecuperationOriginesNonHabitantsResults results = processor.run(1, false, null);
		Assert.assertNotNull(results);

		// traité car le libellé nous dit quelque chose...
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getTraites().size());

		// validation du traitement
		final RecuperationOriginesNonHabitantsResults.InfoTraitement traite = results.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(ppId, traite.noCtb);
		Assert.assertEquals(MockCommune.Geneve.getNomOfficiel(), traite.getLibelle());
		Assert.assertEquals(MockCommune.Geneve.getSigleCanton(), traite.getSigleCanton());

		// il faut encore vérifier le contenu de la base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);

				final OriginePersonnePhysique origine = pp.getOrigine();
				Assert.assertNotNull(origine);
				Assert.assertEquals(MockCommune.Geneve.getNomOfficiel(), origine.getLibelle());
				Assert.assertEquals(MockCommune.Geneve.getSigleCanton(), origine.getSigleCanton());
			}
		});
	}

	@Test
	public void testAncienHabitantAvecLibelleReconnuDryRun() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alicia", "Raquette", null, Sexe.FEMININ);
				pp.setLibelleCommuneOrigine(MockCommune.Echallens.getNomOfficiel());
				return pp.getNumero();
			}
		});

		// lancement du job
		final RecuperationOriginesNonHabitantsResults results = processor.run(1, true, null);
		Assert.assertNotNull(results);

		// traité car le libellé nous dit quelque chose...
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getTraites().size());

		// validation du traitement
		final RecuperationOriginesNonHabitantsResults.InfoTraitement traite = results.getTraites().get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(ppId, traite.noCtb);
		Assert.assertEquals(MockCommune.Echallens.getNomOfficiel(), traite.getLibelle());
		Assert.assertEquals(MockCommune.Echallens.getSigleCanton(), traite.getSigleCanton());

		// il faut encore vérifier le contenu de la base (qui ne doit pas avoir bougé, nous sommes en simulation)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertNull(pp.getOrigine());
			}
		});
	}

	@Test
	public void testNonHabitantAvecLibelleNonReconnu() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alicia", "Raquette", null, Sexe.FEMININ);
				pp.setLibelleCommuneOrigine("Là-bas derrière");
				return pp.getNumero();
			}
		});

		// lancement du job
		final RecuperationOriginesNonHabitantsResults results = processor.run(1, false, null);
		Assert.assertNotNull(results);

		// traité car le libellé nous dit quelque chose...
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getErreurs().size());
		Assert.assertEquals(0, results.getTraites().size());

		// exactitude de l'erreur
		final RecuperationOriginesNonHabitantsResults.InfoErreur erreur = results.getErreurs().get(0);
		Assert.assertNotNull(erreur);
		Assert.assertEquals(ppId, erreur.noCtb);
		Assert.assertEquals("Pas de commune trouvée avec le libellé 'Là-bas derrière'", erreur.getMessage());

		// il faut encore vérifier le contenu de la base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertNull(pp.getOrigine());
			}
		});
	}

	@Test
	public void testAncienHabitantAvecLibelleNonReconnuDryRun() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Alicia", "Raquette", null, Sexe.FEMININ);
				pp.setLibelleCommuneOrigine("Là-bas derrière");
				return pp.getNumero();
			}
		});

		// lancement du job
		final RecuperationOriginesNonHabitantsResults results = processor.run(1, true, null);
		Assert.assertNotNull(results);

		// traité car le libellé nous dit quelque chose...
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getErreurs().size());
		Assert.assertEquals(0, results.getTraites().size());

		// exactitude de l'erreur
		final RecuperationOriginesNonHabitantsResults.InfoErreur erreur = results.getErreurs().get(0);
		Assert.assertNotNull(erreur);
		Assert.assertEquals(ppId, erreur.noCtb);
		Assert.assertEquals("Pas de commune trouvée avec le libellé 'Là-bas derrière'", erreur.getMessage());

		// il faut encore vérifier le contenu de la base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertNull(pp.getOrigine());
			}
		});
	}
}
