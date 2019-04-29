package ch.vd.unireg.tiers.manager;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.common.WebTest;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.service.mock.MockServiceSecuriteService;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class AutorisationTest  extends WebTest {

	private AutorisationManager autorisationManager;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		autorisationManager = getBean(AutorisationManagerImpl.class, "autorisationManager");
	}

	@Test
	public void testAutorisationModifIdeVaudoisOrdinaire() throws Exception {

		final String visaOperateur = "xsizai";
		final long noIndividuFederico = 452120L;
		final long noIndividuAlbert = 4564121L;

		// extrait du profile OID
		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {

				addOperateur(visaOperateur, 42L, Role.MODIF_VD_ORD);
			}
		});


		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu indFederico = addIndividu(noIndividuFederico, null, "jurencon", "Federico", Sexe.MASCULIN);
				final MockIndividu indAlbert = addIndividu(noIndividuAlbert, null, "rodrigue", "Albert", Sexe.MASCULIN);
				addAdresse(indFederico, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, date(2000, 11, 3), null);
				addAdresse(indAlbert, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(2000, 11, 4), null);
				addNationalite(indFederico, MockPays.Suisse, date(1978, 11, 3), null);
				addNationalite(indAlbert, MockPays.Suisse, date(1979, 11, 3), null);
			}
		});

		final class Ids {
			long ppFederico;
			long ppAlbert;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique federico = addHabitant(noIndividuFederico);
			final PersonnePhysique albert = addNonHabitant("Gregoire", "albert", null, Sexe.MASCULIN);
			addForPrincipal(federico, date(2000, 11, 3), MotifFor.DEMENAGEMENT_VD, MockCommune.Cossonay);

			final Ids ids1 = new Ids();
			ids1.ppFederico = federico.getNumero();
			ids1.ppAlbert = albert.getNumero();
			return ids1;
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique federico = (PersonnePhysique) tiersDAO.get(ids.ppFederico);
				//Modification autorisée sur vaudois ordinaire
				Autorisations autorisationsFederico = autorisationManager.getAutorisations(federico, visaOperateur, 1);
				Assert.assertTrue(autorisationsFederico.isIdentificationEntreprise());

				final PersonnePhysique albert = (PersonnePhysique) tiersDAO.get(ids.ppAlbert);
				Autorisations autorisationsAlbert = autorisationManager.getAutorisations(albert, visaOperateur, 1);
				Assert.assertFalse(autorisationsAlbert.isIdentificationEntreprise());


			}
		});

	}


	@Test
	public void testAutorisationModifDiAvecDecisionAci() throws Exception {

		final String visaOperateurDi = "xsizij";
		final String visaOperateurSimple = "xsizap";
		final long noIndividuFederico = 452120L;
		final long noIndividuAlbert = 4564121L;

		// extrait du profile OID
		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {

				addOperateur(visaOperateurDi, 43L, Role.DI_DELAI_PP,
				             Role.DI_DESANNUL_PP, Role.DI_DUPLIC_PP,
				             Role.DI_EMIS_PP, Role.DI_QUIT_PP,
				             Role.DI_SOM_PP, Role.FOR_PRINC_ORDDEP_HAB, Role.FOR_PRINC_ORDDEP_HCHS);
				addOperateur(visaOperateurSimple, 44L, Role.FOR_PRINC_ORDDEP_HAB, Role.FOR_PRINC_ORDDEP_HCHS);
			}
		});


		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu indFederico = addIndividu(noIndividuFederico, null, "jurencon", "Federico", Sexe.MASCULIN);
				final MockIndividu indAlbert = addIndividu(noIndividuAlbert, null, "rodrigue", "Albert", Sexe.MASCULIN);
				addAdresse(indFederico, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, date(2000, 11, 3), null);
				addAdresse(indAlbert, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(2000, 11, 4), null);
				addNationalite(indFederico, MockPays.Suisse, date(1978, 11, 3), null);
				addNationalite(indAlbert, MockPays.Suisse, date(1979, 11, 3), null);
			}
		});

		final class Ids {
			long ppFederico;
			long ppAlbert;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique federico = addHabitant(noIndividuFederico);
			final PersonnePhysique albert = addNonHabitant("Gregoire", "albert", null, Sexe.MASCULIN);
			addForPrincipal(federico, date(2000, 11, 3), MotifFor.DEMENAGEMENT_VD, MockCommune.Cossonay);
			addDecisionAci(albert, date(2014, 1, 1), null, MockCommune.Aubonne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);

			final Ids ids1 = new Ids();
			ids1.ppFederico = federico.getNumero();
			ids1.ppAlbert = albert.getNumero();
			return ids1;
		});


		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final PersonnePhysique albert = (PersonnePhysique) tiersDAO.get(ids.ppAlbert);
				//Modification autorisée sur DI
				Autorisations autorisationsDiAlbert = autorisationManager.getAutorisations(albert, visaOperateurDi, 1);
				Assert.assertTrue(autorisationsDiAlbert.isDeclarationImpots());
				//Modif fiscales interdites car présences d'une décisions
				Assert.assertFalse(autorisationsDiAlbert.isDonneesFiscales());

				//Modification non autorisée sur DI
				Autorisations autorisationsSimpleAlbert = autorisationManager.getAutorisations(albert, visaOperateurSimple, 1);
				Assert.assertFalse(autorisationsSimpleAlbert.isDeclarationImpots());
				//Modif fiscales interdites car présences d'une décisions
				Assert.assertFalse(autorisationsSimpleAlbert.isDonneesFiscales());


				final PersonnePhysique federico = (PersonnePhysique) tiersDAO.get(ids.ppFederico);
				//Modification sur DI
				Autorisations autorisationsDiFederico = autorisationManager.getAutorisations(federico, visaOperateurDi, 1);
				Assert.assertTrue(autorisationsDiFederico.isDeclarationImpots());
				//Modif fisales autorisées
				Assert.assertTrue(autorisationsDiFederico.isDonneesFiscales());
				//Modification non autorisée sur DI
				Autorisations autorisationsSimpleFederico = autorisationManager.getAutorisations(federico, visaOperateurSimple, 1);
				Assert.assertFalse(autorisationsSimpleFederico.isDeclarationImpots());
				//Modif fisales autorisées
				Assert.assertTrue(autorisationsSimpleFederico.isDonneesFiscales());

			}
		});

	}

	@Test
	public void testAutorisationModifDiSansDecisionAci() throws Exception {

		final String visaOperateurDi = "xsizij";
		final String visaOperateurSimple = "xsizap";
		final long noIndividuFederico = 452120L;
		final long noIndividuAlbert = 4564121L;

		// extrait du profile OID
		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {

				addOperateur(visaOperateurDi, 43L, Role.DI_DELAI_PP,
				             Role.DI_DESANNUL_PP, Role.DI_DUPLIC_PP,
				             Role.DI_EMIS_PP, Role.DI_QUIT_PP,
				             Role.DI_SOM_PP, Role.FOR_PRINC_ORDDEP_HAB, Role.FOR_PRINC_ORDDEP_HCHS);
				addOperateur(visaOperateurSimple, 44L, Role.FOR_PRINC_ORDDEP_HAB, Role.FOR_PRINC_ORDDEP_HCHS);
			}
		});


		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu indFederico = addIndividu(noIndividuFederico, null, "jurencon", "Federico", Sexe.MASCULIN);
				final MockIndividu indAlbert = addIndividu(noIndividuAlbert, null, "rodrigue", "Albert", Sexe.MASCULIN);
				addAdresse(indFederico, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, date(2000, 11, 3), null);
				addAdresse(indAlbert, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(2000, 11, 4), null);
				addNationalite(indFederico, MockPays.Suisse, date(1978, 11, 3), null);
				addNationalite(indAlbert, MockPays.Suisse, date(1979, 11, 3), null);
			}
		});

		final class Ids {
			long ppFederico;
			long ppAlbert;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique federico = addHabitant(noIndividuFederico);
			final PersonnePhysique albert = addNonHabitant("Gregoire", "albert", null, Sexe.MASCULIN);
			addForPrincipal(federico, date(2000, 11, 3), MotifFor.DEMENAGEMENT_VD, MockCommune.Cossonay);

			final Ids ids1 = new Ids();
			ids1.ppFederico = federico.getNumero();
			ids1.ppAlbert = albert.getNumero();
			return ids1;
		});


		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final PersonnePhysique albert = (PersonnePhysique) tiersDAO.get(ids.ppAlbert);
				//Modification autorisée sur DI
				Autorisations autorisationsDiAlbert = autorisationManager.getAutorisations(albert, visaOperateurDi, 1);
				Assert.assertTrue(autorisationsDiAlbert.isDeclarationImpots());
				//Modif fiscales autorisees
				Assert.assertTrue(autorisationsDiAlbert.isDonneesFiscales());

				//Modification non autorisée sur DI
				Autorisations autorisationsSimpleAlbert = autorisationManager.getAutorisations(albert, visaOperateurSimple, 1);
				Assert.assertFalse(autorisationsSimpleAlbert.isDeclarationImpots());
				//Modif fiscales autorisees
				Assert.assertTrue(autorisationsSimpleAlbert.isDonneesFiscales());


				final PersonnePhysique federico = (PersonnePhysique) tiersDAO.get(ids.ppFederico);
				//Modification sur DI
				Autorisations autorisationsDiFederico = autorisationManager.getAutorisations(federico, visaOperateurDi, 1);
				Assert.assertTrue(autorisationsDiFederico.isDeclarationImpots());
				//Modif fisales autorisées
				Assert.assertTrue(autorisationsDiFederico.isDonneesFiscales());
				//Modification non autorisée sur DI
				Autorisations autorisationsSimpleFederico = autorisationManager.getAutorisations(federico, visaOperateurSimple, 1);
				Assert.assertFalse(autorisationsSimpleFederico.isDeclarationImpots());
				//Modif fisales autorisées
				Assert.assertTrue(autorisationsSimpleFederico.isDonneesFiscales());

			}
		});

	}



	@Test
	public void testAutorisationModifIdeSourcier() throws Exception {

		final String visaOperateur = "xsizbi";
		final long noIndividuFederico = 452120L;
		final long noIndividuAlbert = 4564121L;

		// extrait du profile OID
		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur(visaOperateur, 42L, Role.MODIF_VD_SOURC);
			}

		});

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu indFederico = addIndividu(noIndividuFederico, null, "jurencon", "Federico", Sexe.MASCULIN);
				final MockIndividu indAlbert = addIndividu(noIndividuAlbert, null, "rodrigue", "Albert", Sexe.MASCULIN);
				addAdresse(indFederico, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, date(2000, 11, 3), null);
				addAdresse(indAlbert, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(2000, 11, 4), null);
				addNationalite(indFederico, MockPays.Suisse, date(1978, 11, 3), null);
				addNationalite(indAlbert, MockPays.France, date(1979, 11, 3), null);
			}
		});

		final class Ids {
			long ppFederico;
			long ppAlbert;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique federico = addHabitant(noIndividuFederico);
			final PersonnePhysique albert = addNonHabitant("Gregoire", "albert", null, Sexe.MASCULIN);
			addForPrincipalSource(albert, date(2000, 3, 2), MotifFor.ARRIVEE_HC, null, null, MockCommune.Aubonne.getNoOFS());
			addForPrincipal(federico, date(2000, 11, 3), MotifFor.DEMENAGEMENT_VD, MockCommune.Cossonay);

			final Ids ids1 = new Ids();
			ids1.ppFederico = federico.getNumero();
			ids1.ppAlbert = albert.getNumero();
			return ids1;
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final PersonnePhysique albert = (PersonnePhysique) tiersDAO.get(ids.ppAlbert);
				//Modification autorisée sur sourcier
				Autorisations autorisationsAlbert = autorisationManager.getAutorisations(albert, visaOperateur, 1);
				Assert.assertTrue(autorisationsAlbert.isIdentificationEntreprise());

				final PersonnePhysique federico = (PersonnePhysique) tiersDAO.get(ids.ppFederico);
				Autorisations autorisationsFederico = autorisationManager.getAutorisations(federico, visaOperateur, 1);
				Assert.assertFalse(autorisationsFederico.isIdentificationEntreprise());

			}
		});

	}

	@Test
	public void testAutorisationModifIdeHorsCantonHorsSuisse() throws Exception {

		final String visaOperateur = "xsizci";
		final long noIndividuFederico = 452120L;
		final long noIndividuAlbert = 4564121L;

		// extrait du profile OID
		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur(visaOperateur, 42L, Role.MODIF_HC_HS);
			}
		});

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu indFederico = addIndividu(noIndividuFederico, null, "jurencon", "Federico", Sexe.MASCULIN);
				final MockIndividu indAlbert = addIndividu(noIndividuAlbert, null, "rodrigue", "Albert", Sexe.MASCULIN);
				addAdresse(indFederico, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, date(2000, 11, 3), null);
				addAdresse(indAlbert, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(2000, 11, 4), null);
				addNationalite(indFederico, MockPays.Suisse, date(1978, 11, 3), null);
				addNationalite(indAlbert, MockPays.Suisse, date(1979, 11, 3), null);
			}
		});

		final class Ids {
			long ppFederico;
			long ppAlbert;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique federico = addHabitant(noIndividuFederico);
			final PersonnePhysique albert = addNonHabitant("Gregoire", "albert", null, Sexe.MASCULIN);
			addForPrincipalSource(albert, date(2000, 3, 2), MotifFor.ARRIVEE_HC, null, null, MockCommune.Aubonne.getNoOFS());
			addForPrincipal(federico, date(2000, 11, 3), MotifFor.DEPART_HC, MockCommune.Zurich);

			final Ids ids1 = new Ids();
			ids1.ppFederico = federico.getNumero();
			ids1.ppAlbert = albert.getNumero();
			return ids1;
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique federico = (PersonnePhysique) tiersDAO.get(ids.ppFederico);
				//Modification autorisée sur hors canton
				Autorisations autorisationsFederico = autorisationManager.getAutorisations(federico, visaOperateur, 1);
				Assert.assertTrue(autorisationsFederico.isIdentificationEntreprise());

				final PersonnePhysique albert = (PersonnePhysique) tiersDAO.get(ids.ppAlbert);
				Autorisations autorisationsAlbert = autorisationManager.getAutorisations(albert, visaOperateur, 1);
				Assert.assertFalse(autorisationsAlbert.isIdentificationEntreprise());


			}
		});

	}


	//SIFISC-13191 Autorisation de modifier un I107
	@Test
	public void testAutorisationModifIdeCtbI107() throws Exception {

		final String visaOperateur = "usrreg01";
		final long noIndividuFederico = 452120L;

		// extrait du profile OID
		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur(visaOperateur, 42L, Role.MODIF_NONHAB_INACTIF);
			}
		});


		final class Ids {
			long ppFederico;
			long ppAlbert;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique albert = addNonHabitant("Gregoire", "albert", null, Sexe.MASCULIN);
			albert.setDebiteurInactif(true);
			final Ids ids1 = new Ids();
			ids1.ppAlbert = albert.getNumero();
			return ids1;
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final PersonnePhysique albert = (PersonnePhysique) tiersDAO.get(ids.ppAlbert);
				Autorisations autorisationsAlbert = autorisationManager.getAutorisations(albert, visaOperateur, 1);
				Assert.assertTrue(autorisationsAlbert.isIdentificationEntreprise());


			}
		});

	}

	//SIFISC-13192 / SIFISC-13193 Autorisations dépassent les droits attribués.
	@Test
	public void testAutorisationDepasseDroitsAttribues() throws Exception {

		final String visaOperateur = "usrreg03";
		final long noIndividuFederico = 452120L;

		// extrait du profile OID
		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur(visaOperateur, 42L, Role.MODIF_HC_HS, Role.MODIF_NONHAB_DEBPUR);
			}
		});


		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu indFederico = addIndividu(noIndividuFederico, null, "jurencon", "Federico", Sexe.MASCULIN);

			}
		});

		final class Ids {
			long ppAlbert;
			long ppRegis;
			long ppFederico;
			long ppJustin;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique albert = addNonHabitant("Gregoire", "albert", null, Sexe.MASCULIN);
			addForPrincipal(albert, date(2000, 11, 3), MotifFor.DEPART_HC, MockCommune.Zurich);

			final PersonnePhysique justin = addNonHabitant("Gregoire", "justin", null, Sexe.MASCULIN);

			final PersonnePhysique federico = addHabitant(noIndividuFederico);
			addForPrincipal(federico, date(2000, 11, 3), MotifFor.ARRIVEE_HC, MockCommune.Echallens);

			final PersonnePhysique regis = addNonHabitant("Gregoire", "regis", null, Sexe.MASCULIN);

			final Ids ids1 = new Ids();
			ids1.ppAlbert = albert.getNumero();
			ids1.ppRegis = regis.getNumero();
			ids1.ppFederico = federico.getNumero();
			ids1.ppJustin = justin.getNumero();
			return ids1;
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final PersonnePhysique albert = (PersonnePhysique) tiersDAO.get(ids.ppAlbert);
				Autorisations autorisationsAlbert = autorisationManager.getAutorisations(albert, visaOperateur, 1);
				Assert.assertTrue(autorisationsAlbert.isIdentificationEntreprise());

				final PersonnePhysique justin = (PersonnePhysique) tiersDAO.get(ids.ppJustin);
				Autorisations autorisationsJustin = autorisationManager.getAutorisations(justin, visaOperateur, 1);
				Assert.assertTrue(autorisationsJustin.isIdentificationEntreprise());

				final PersonnePhysique regis = (PersonnePhysique) tiersDAO.get(ids.ppRegis);
				Autorisations autorisationsRegis = autorisationManager.getAutorisations(regis, visaOperateur, 1);
				Assert.assertTrue(autorisationsRegis.isIdentificationEntreprise());

				final PersonnePhysique federico = (PersonnePhysique) tiersDAO.get(ids.ppFederico);
				Autorisations autorisationsFederico = autorisationManager.getAutorisations(federico, visaOperateur, 1);
				Assert.assertFalse(autorisationsFederico.isIdentificationEntreprise());


			}
		});

	}
	//SIFISC-13194
	@Test
	public void testAutorisationMajNonHabitantDepasseDroitsAttribues() throws Exception {

		final String visaOperateur = "usrreg08";
		final long noIndividuFederico = 452120L;
		final long noIndividuAlbert = 452121L;
		final long noIndividuRegis = 452122L;
		final long noIndividuJustin = 452123L;

		// extrait du profile OID
		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur(visaOperateur, 42L, Role.MODIF_NONHAB_DEBPUR);
			}
		});


		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu indFederico = addIndividu(noIndividuFederico, null, "jurencon", "Federico", Sexe.MASCULIN);
				final MockIndividu indAbert = addIndividu(noIndividuAlbert, null, "jurencon", "Albert", Sexe.MASCULIN);
				final MockIndividu indRegis = addIndividu(noIndividuRegis, null, "jurencon", "Regis", Sexe.MASCULIN);
				final MockIndividu indJustin = addIndividu(noIndividuJustin, null, "jurencon", "Justin", Sexe.MASCULIN);

			}
		});

		final class Ids {
			//HC
			long ppAlbert;
			//NON ASSUJETTI
			long ppRegis;
			// VD ORDINAIRE
			long ppFederico;
			//SOURCIER
			long ppJustin;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique albert = addHabitant(noIndividuAlbert);
			addForPrincipal(albert, date(2000, 11, 3), MotifFor.DEPART_HC, MockCommune.Zurich);

			final PersonnePhysique justin = addHabitant(noIndividuJustin);
			addForPrincipalSource(justin, date(2000, 3, 2), MotifFor.ARRIVEE_HC, null, null, MockCommune.Aubonne.getNoOFS());


			final PersonnePhysique federico = addHabitant(noIndividuFederico);
			addForPrincipal(federico, date(2000, 11, 3), MotifFor.ARRIVEE_HC, MockCommune.Echallens);

			final PersonnePhysique regis = addHabitant(noIndividuRegis);

			final Ids ids1 = new Ids();
			ids1.ppAlbert = albert.getNumero();
			ids1.ppRegis = regis.getNumero();
			ids1.ppFederico = federico.getNumero();
			ids1.ppJustin = justin.getNumero();
			return ids1;
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				//Hors canton
				final PersonnePhysique albert = (PersonnePhysique) tiersDAO.get(ids.ppAlbert);
				Autorisations autorisationsAlbert = autorisationManager.getAutorisations(albert, visaOperateur, 1);
				Assert.assertFalse(autorisationsAlbert.isIdentificationEntreprise());

				//SOURCIER
				final PersonnePhysique justin = (PersonnePhysique) tiersDAO.get(ids.ppJustin);
				Autorisations autorisationsJustin = autorisationManager.getAutorisations(justin, visaOperateur, 1);
				Assert.assertFalse(autorisationsJustin.isIdentificationEntreprise());

				//Non assujetti
				final PersonnePhysique regis = (PersonnePhysique) tiersDAO.get(ids.ppRegis);
				Autorisations autorisationsRegis = autorisationManager.getAutorisations(regis, visaOperateur, 1);
				Assert.assertFalse(autorisationsRegis.isIdentificationEntreprise());

				//Vaudois ordinaire
				final PersonnePhysique federico = (PersonnePhysique) tiersDAO.get(ids.ppFederico);
				Autorisations autorisationsFederico = autorisationManager.getAutorisations(federico, visaOperateur, 1);
				Assert.assertFalse(autorisationsFederico.isIdentificationEntreprise());

			}
		});

	}

}
