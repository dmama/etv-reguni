package ch.vd.unireg.evenement.party;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.fiscact.periodic.v1.PeriodicFiscalActivityRequest;
import ch.vd.unireg.xml.event.party.fiscact.v1.FiscalActivityResponse;

public class PeriodicFiscalActivityRequestHandlerV1Test extends BusinessTest {

	private PeriodicFiscalActivityRequestHandlerV1 handler;
	private static final UserLogin USER_LOGIN = new UserLogin("USER", 22);

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		handler = new PeriodicFiscalActivityRequestHandlerV1();
		handler.setSecurityProvider(new MockSecurityProvider(Role.VISU_ALL));
		handler.setTiersDAO(tiersDAO);
	}

	@NotNull
	private static PeriodicFiscalActivityRequest createRequest(int noTiers, int annee) {
		return new PeriodicFiscalActivityRequest(USER_LOGIN, noTiers, annee);
	}

	@Test
	public void testContribuableSansFor() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Frontignac", null, Sexe.MASCULIN);
				return pp.getNumero();
			}
		});
		Assert.assertTrue(Long.toString(ppId), ppId >= Integer.MIN_VALUE && ppId <= Integer.MAX_VALUE);

		// on essaie sur plusieurs années
		for (int annee = 1950 ; annee < 2050 ; ++ annee) {

			// génération de la requête
			final PeriodicFiscalActivityRequest request = createRequest((int) ppId, annee);

			// lancement de la requête et obtention de la réponse
			final FiscalActivityResponse response = doInNewTransactionAndSession(new TxCallback<FiscalActivityResponse>() {
				@Override
				public FiscalActivityResponse execute(TransactionStatus status) throws Exception {
					return (FiscalActivityResponse) handler.handle(request).getResponse();
				}
			});

			// contrôle de la réponse
			Assert.assertNotNull(Integer.toString(annee), response);
			Assert.assertFalse(Integer.toString(annee), response.isActive());
			Assert.assertEquals("Le contribuable n'a aucun for vaudois ouvert sur la période demandée.", response.getMessage());
		}
	}

	@Test
	public void testContribuableAvecForVaudoisAnnule() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Frontignac", null, Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 1, 5), MotifFor.DEPART_HS, MockCommune.Cossonay);
				ffp.setAnnule(true);
				return pp.getNumero();
			}
		});
		Assert.assertTrue(Long.toString(ppId), ppId >= Integer.MIN_VALUE && ppId <= Integer.MAX_VALUE);

		// on essaie sur plusieurs années
		for (int annee = 1950 ; annee < 2050 ; ++ annee) {

			// génération de la requête
			final PeriodicFiscalActivityRequest request = createRequest((int) ppId, annee);

			// lancement de la requête et obtention de la réponse
			final FiscalActivityResponse response = doInNewTransactionAndSession(new TxCallback<FiscalActivityResponse>() {
				@Override
				public FiscalActivityResponse execute(TransactionStatus status) throws Exception {
					return (FiscalActivityResponse) handler.handle(request).getResponse();
				}
			});

			// contrôle de la réponse
			Assert.assertNotNull(Integer.toString(annee), response);
			Assert.assertFalse(Integer.toString(annee), response.isActive());
			Assert.assertEquals("Le contribuable n'a aucun for vaudois ouvert sur la période demandée.", response.getMessage());
		}
	}

	@Test
	public void testContribuableAvecForPrincipalVaudois() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Frontignac", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 12, 31), MotifFor.ARRIVEE_HS, date(2010, 1, 1), MotifFor.DEPART_HS, MockCommune.Cossonay);
				return pp.getNumero();
			}
		});
		Assert.assertTrue(Long.toString(ppId), ppId >= Integer.MIN_VALUE && ppId <= Integer.MAX_VALUE);

		// on essaie sur plusieurs années
		for (int annee = 1950 ; annee < 2050 ; ++ annee) {

			// génération de la requête
			final PeriodicFiscalActivityRequest request = createRequest((int) ppId, annee);

			// lancement de la requête et obtention de la réponse
			final FiscalActivityResponse response = doInNewTransactionAndSession(new TxCallback<FiscalActivityResponse>() {
				@Override
				public FiscalActivityResponse execute(TransactionStatus status) throws Exception {
					return (FiscalActivityResponse) handler.handle(request).getResponse();
				}
			});

			// contrôle de la réponse
			Assert.assertNotNull(Integer.toString(annee), response);
			Assert.assertEquals(Integer.toString(annee), annee >= 2000 && annee <= 2010, response.isActive());
			if (response.isActive()) {
				Assert.assertEquals("Le contribuable a un for vaudois ouvert sur la période demandée.", response.getMessage());
			}
			else {
				Assert.assertEquals("Le contribuable n'a aucun for vaudois ouvert sur la période demandée.", response.getMessage());
			}
		}
	}

	@Test
	public void testContribuableAvecForPrincipalHorsSuisseSansForSecondaire() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Frontignac", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 12, 31), MotifFor.MAJORITE, date(2010, 1, 1), MotifFor.VEUVAGE_DECES, MockPays.Allemagne);
				return pp.getNumero();
			}
		});
		Assert.assertTrue(Long.toString(ppId), ppId >= Integer.MIN_VALUE && ppId <= Integer.MAX_VALUE);

		// on essaie sur plusieurs années
		for (int annee = 1950 ; annee < 2050 ; ++ annee) {

			// génération de la requête
			final PeriodicFiscalActivityRequest request = createRequest((int) ppId, annee);

			// lancement de la requête et obtention de la réponse
			final FiscalActivityResponse response = doInNewTransactionAndSession(new TxCallback<FiscalActivityResponse>() {
				@Override
				public FiscalActivityResponse execute(TransactionStatus status) throws Exception {
					return (FiscalActivityResponse) handler.handle(request).getResponse();
				}
			});

			// contrôle de la réponse
			Assert.assertNotNull(Integer.toString(annee), response);
			Assert.assertFalse(Integer.toString(annee), response.isActive());
		}
	}

	@Test
	public void testContribuableAvecForPrincipalHorsCantonSansForSecondaire() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Frontignac", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 12, 31), MotifFor.MAJORITE, date(2010, 1, 1), MotifFor.VEUVAGE_DECES, MockCommune.Chur);
				return pp.getNumero();
			}
		});
		Assert.assertTrue(Long.toString(ppId), ppId >= Integer.MIN_VALUE && ppId <= Integer.MAX_VALUE);

		// on essaie sur plusieurs années
		for (int annee = 1950 ; annee < 2050 ; ++ annee) {

			// génération de la requête
			final PeriodicFiscalActivityRequest request = createRequest((int) ppId, annee);

			// lancement de la requête et obtention de la réponse
			final FiscalActivityResponse response = doInNewTransactionAndSession(new TxCallback<FiscalActivityResponse>() {
				@Override
				public FiscalActivityResponse execute(TransactionStatus status) throws Exception {
					return (FiscalActivityResponse) handler.handle(request).getResponse();
				}
			});

			// contrôle de la réponse
			Assert.assertNotNull(Integer.toString(annee), response);
			Assert.assertFalse(Integer.toString(annee), response.isActive());
			Assert.assertEquals("Le contribuable n'a aucun for vaudois ouvert sur la période demandée.", response.getMessage());
		}
	}

	@Test
	public void testContribuableAvecForPrincipalHorsSuisseAvecForSecondaire() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Frontignac", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 12, 31), MotifFor.MAJORITE, date(2010, 1, 1), MotifFor.VEUVAGE_DECES, MockPays.Allemagne);
				addForSecondaire(pp, date(2003, 5, 31), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 12), MotifFor.VEUVAGE_DECES, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});
		Assert.assertTrue(Long.toString(ppId), ppId >= Integer.MIN_VALUE && ppId <= Integer.MAX_VALUE);

		// on essaie sur plusieurs années
		for (int annee = 1950 ; annee < 2050 ; ++ annee) {

			// génération de la requête
			final PeriodicFiscalActivityRequest request = createRequest((int) ppId, annee);

			// lancement de la requête et obtention de la réponse
			final FiscalActivityResponse response = doInNewTransactionAndSession(new TxCallback<FiscalActivityResponse>() {
				@Override
				public FiscalActivityResponse execute(TransactionStatus status) throws Exception {
					return (FiscalActivityResponse) handler.handle(request).getResponse();
				}
			});

			// contrôle de la réponse
			Assert.assertNotNull(Integer.toString(annee), response);
			Assert.assertEquals(Integer.toString(annee), annee >= 2003 && annee <= 2008, response.isActive());
			if (response.isActive()) {
				Assert.assertEquals("Le contribuable a un for vaudois ouvert sur la période demandée.", response.getMessage());
			}
			else {
				Assert.assertEquals("Le contribuable n'a aucun for vaudois ouvert sur la période demandée.", response.getMessage());
			}
		}
	}

	@Test
	public void testContribuableAvecForPrincipalHorsCantonAvecForSecondaire() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Frontignac", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 12, 31), MotifFor.MAJORITE, date(2010, 1, 1), MotifFor.VEUVAGE_DECES, MockCommune.Chur);
				addForSecondaire(pp, date(2003, 5, 31), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 12), MotifFor.VEUVAGE_DECES, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});
		Assert.assertTrue(Long.toString(ppId), ppId >= Integer.MIN_VALUE && ppId <= Integer.MAX_VALUE);

		// on essaie sur plusieurs années
		for (int annee = 1950 ; annee < 2050 ; ++ annee) {

			// génération de la requête
			final PeriodicFiscalActivityRequest request = createRequest((int) ppId, annee);

			// lancement de la requête et obtention de la réponse
			final FiscalActivityResponse response = doInNewTransactionAndSession(new TxCallback<FiscalActivityResponse>() {
				@Override
				public FiscalActivityResponse execute(TransactionStatus status) throws Exception {
					return (FiscalActivityResponse) handler.handle(request).getResponse();
				}
			});

			// contrôle de la réponse
			Assert.assertNotNull(Integer.toString(annee), response);
			Assert.assertEquals(Integer.toString(annee), annee >= 2003 && annee <= 2008, response.isActive());
			if (response.isActive()) {
				Assert.assertEquals("Le contribuable a un for vaudois ouvert sur la période demandée.", response.getMessage());
			}
			else {
				Assert.assertEquals("Le contribuable n'a aucun for vaudois ouvert sur la période demandée.", response.getMessage());
			}
		}
	}

	@Test
	public void testContribuableAvecForVaudoisOuvert() throws Exception {

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Frontignac", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 12, 31), MotifFor.MAJORITE, MockCommune.Echallens);
				return pp.getNumero();
			}
		});
		Assert.assertTrue(Long.toString(ppId), ppId >= Integer.MIN_VALUE && ppId <= Integer.MAX_VALUE);

		// on essaie sur plusieurs années
		for (int annee = 1950 ; annee < 2050 ; ++ annee) {

			// génération de la requête
			final PeriodicFiscalActivityRequest request = createRequest((int) ppId, annee);

			// lancement de la requête et obtention de la réponse
			final FiscalActivityResponse response = doInNewTransactionAndSession(new TxCallback<FiscalActivityResponse>() {
				@Override
				public FiscalActivityResponse execute(TransactionStatus status) throws Exception {
					return (FiscalActivityResponse) handler.handle(request).getResponse();
				}
			});

			// contrôle de la réponse
			Assert.assertNotNull(Integer.toString(annee), response);
			Assert.assertEquals(Integer.toString(annee), annee >= 2000, response.isActive());
			if (response.isActive()) {
				Assert.assertEquals("Le contribuable a un for vaudois ouvert sur la période demandée.", response.getMessage());
			}
			else {
				Assert.assertEquals("Le contribuable n'a aucun for vaudois ouvert sur la période demandée.", response.getMessage());
			}
		}
	}
}