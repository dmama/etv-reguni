package ch.vd.uniregctb.evenement.party;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.taxliab.aperiodic.v1.AperiodicTaxLiabilityRequest;
import ch.vd.unireg.xml.event.party.taxliab.aperiodic.v1.AperiodicTaxLiabilityResponse;
import ch.vd.unireg.xml.event.party.taxliab.common.v1.ResponseType;
import ch.vd.unireg.xml.event.party.taxliab.common.v1.Scope;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxationAuthorityType;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.service.mock.MockServicePM;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.xml.ServiceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AperiodicTaxLiabilityRequestHandlerTest extends BusinessTest {

	private AperiodicTaxLiabilityRequestHandler handler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		handler = new AperiodicTaxLiabilityRequestHandler();
		handler.setTiersDAO(tiersDAO);
		handler.setTiersService(tiersService);
	}

	@Override
	public void onTearDown() throws Exception {
		handler.setSecurityProvider(null);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleUtilisateurSansDroit() throws Exception {

		handler.setSecurityProvider(new MockSecurityProvider());

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(12345678);
		request.setDate(new Date(2013, 2, 8));
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

		try {
			handler.handle(request);
			fail();
		}
		catch (ServiceException e) {
			assertTrue(e.getInfo() instanceof AccessDeniedExceptionInfo);
			assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture complète sur l'application.", e.getMessage());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleTiersInconnu() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		handler.setSecurityProvider(provider);

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(12345678);
		request.setDate(new Date(2013, 2, 8));
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

		try {
			handler.handle(request);
			fail();
		}
		catch (ServiceException e) {
			assertTrue(e.getInfo() instanceof BusinessExceptionInfo);
			assertEquals("Le tiers n°12345678 n'existe pas.", e.getMessage());
		}
	}

	@Test
	public void testHandleSurEntreprise() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		handler.setSecurityProvider(provider);

		final long noPM = 1234;

		servicePM.setUp(new MockServicePM() {
			@Override
			protected void init() {
				addPM(noPM, "Boucherie Truc", "SARL", date(1978, 1, 1), null);
			}
		});

		final Long idPM = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Entreprise pm = addEntreprise(noPM);
				return pm.getNumero();
			}
		});

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(idPM.intValue());
		request.setDate(new Date(2013, 2, 8));
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

		final RequestHandlerResult result = doInNewTransaction(new TxCallback<RequestHandlerResult>() {
			@Override
			public RequestHandlerResult execute(TransactionStatus status) throws Exception {
				return handler.handle(request);
			}
		});
		assertNotNull(result);

		final AperiodicTaxLiabilityResponse response = (AperiodicTaxLiabilityResponse) result.getResponse();
		assertNotNull(response);
		assertEquals(idPM.intValue(), response.getPartyNumber().intValue());
		assertEquals(ResponseType.TAXPAYER_WITHOUT_TAX_LIABILITY, response.getType());
		assertNull(response.getMainTaxResidence());
	}

	@Test
	public void testHandleSurHabitantVD() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		handler.setSecurityProvider(provider);

		final long noInd = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1965, 3, 12), "Ramaldadji", "Jacques", Sexe.MASCULIN);
			}
		});

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(1986, 3, 12), MotifFor.MAJORITE, MockCommune.Vevey);
				return pp.getNumero();
			}
		});

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(idPP.intValue());
		request.setDate(new Date(2013, 2, 8));
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

		final RequestHandlerResult result = doInNewTransaction(new TxCallback<RequestHandlerResult>() {
			@Override
			public RequestHandlerResult execute(TransactionStatus status) throws Exception {
				return handler.handle(request);
			}
		});
		assertNotNull(result);

		// on s'assure que la réponse est bien positive
		final AperiodicTaxLiabilityResponse response = (AperiodicTaxLiabilityResponse) result.getResponse();
		assertNotNull(response);
		assertEquals(idPP.intValue(), response.getPartyNumber().intValue());
		assertEquals(ResponseType.OK_TAXPAYER_FOUND, response.getType());

		final TaxResidence mainTaxResidence = response.getMainTaxResidence();
		assertNotNull(mainTaxResidence);
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, mainTaxResidence.getTaxationAuthorityType());
		assertEquals(MockCommune.Vevey.getNoOFS(), mainTaxResidence.getTaxationAuthorityFSOId());
	}

	@Test
	public void testHandleSurSourcierHC() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		handler.setSecurityProvider(provider);

		// on crée un sourcier hors-canton qui travaille dans le canton de Vaud
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Rebetez", "Ulysse", date(1945, 3, 22), Sexe.MASCULIN);
				addForPrincipal(pp, date(1986, 3, 12), MotifFor.MAJORITE, MockCommune.Neuchatel, ModeImposition.SOURCE);
				return pp.getNumero();
			}
		});

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(idPP.intValue());
		request.setDate(new Date(2013, 2, 8));
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

		final RequestHandlerResult result = doInNewTransaction(new TxCallback<RequestHandlerResult>() {
			@Override
			public RequestHandlerResult execute(TransactionStatus status) throws Exception {
				return handler.handle(request);
			}
		});
		assertNotNull(result);

		// on s'assure que la réponse est bien positive
		final AperiodicTaxLiabilityResponse response = (AperiodicTaxLiabilityResponse) result.getResponse();
		assertNotNull(response);
		assertEquals(idPP.intValue(), response.getPartyNumber().intValue());
		assertEquals(ResponseType.OK_TAXPAYER_FOUND, response.getType());

		final TaxResidence mainTaxResidence = response.getMainTaxResidence();
		assertNotNull(mainTaxResidence);
		assertEquals(TaxationAuthorityType.OTHER_CANTON_MUNICIPALITY, mainTaxResidence.getTaxationAuthorityType());
		assertEquals(MockCommune.Neuchatel.getNoOFS(), mainTaxResidence.getTaxationAuthorityFSOId());
	}

	@Test
	public void testHandleSurDiplomateSuisse() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		handler.setSecurityProvider(provider);

		final long noInd = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1965, 3, 12), "Muraz", "Jacques", Sexe.MASCULIN);
			}
		});

		// on crée un diplomate Suisse basé à l'étranger (dans ce cas, son for principal est posé sur sa commune d'origine)
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(1986, 3, 12), MotifFor.MAJORITE, MockCommune.Vevey, MotifRattachement.DIPLOMATE_SUISSE);
				return pp.getNumero();
			}
		});

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(idPP.intValue());
		request.setDate(new Date(2013, 2, 8));
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

		final RequestHandlerResult result = doInNewTransaction(new TxCallback<RequestHandlerResult>() {
			@Override
			public RequestHandlerResult execute(TransactionStatus status) throws Exception {
				return handler.handle(request);
			}
		});
		assertNotNull(result);

		// on s'assure que le diplomate n'est pas assujetti
		final AperiodicTaxLiabilityResponse response = (AperiodicTaxLiabilityResponse) result.getResponse();
		assertNotNull(response);
		assertEquals(idPP.intValue(), response.getPartyNumber().intValue());
		assertEquals(ResponseType.TAXPAYER_WITHOUT_TAX_LIABILITY, response.getType());
		assertNull(response.getMainTaxResidence());
	}

	@Test
	public void testHandleSurHabitantVDEnMenage() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		handler.setSecurityProvider(provider);

		final long noInd = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1965, 3, 12), "Ramaldadji", "Jacques", Sexe.MASCULIN);
			}
		});

		class Ids {
			long pp;
			long mc;
		}
		final Ids ids = new Ids();

		// on crée un habitant qui appartient à un ménage vaudois ordinaire
		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(1985, 3, 12), MotifFor.MAJORITE, date(1994, 3, 11), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pp, null, date(1994, 3, 12), null);
				final MenageCommun menage = ensemble.getMenage();
				ids.pp = pp.getId();
				ids.mc = menage.getId();
				addForPrincipal(menage, date(1994, 3, 12), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Vevey);
			}
		});

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		request.setLogin(new UserLogin("xxxxx", 22));
		request.setPartyNumber((int) ids.pp);
		request.setDate(new Date(2013, 2, 8));
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

		final RequestHandlerResult result = doInNewTransaction(new TxCallback<RequestHandlerResult>() {
			@Override
			public RequestHandlerResult execute(TransactionStatus status) throws Exception {
				return handler.handle(request);
			}
		});
		assertNotNull(result);

		// on s'assure que la réponse est bien positive et que c'est le ménage qui est retourné
		final AperiodicTaxLiabilityResponse response = (AperiodicTaxLiabilityResponse) result.getResponse();
		assertNotNull(response);
		assertEquals(ids.mc, response.getPartyNumber().intValue());
		assertEquals(ResponseType.OK_TAXPAYER_FOUND, response.getType());

		final TaxResidence mainTaxResidence = response.getMainTaxResidence();
		assertNotNull(mainTaxResidence);
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, mainTaxResidence.getTaxationAuthorityType());
		assertEquals(MockCommune.Vevey.getNoOFS(), mainTaxResidence.getTaxationAuthorityFSOId());
	}

	@Test
	public void testHandleSurSourcierHCEnMenage() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		handler.setSecurityProvider(provider);

		class Ids {
			long pp;
			long mc;
		}
		final Ids ids = new Ids();

		// on crée un sourcier hors-canton en ménage qui travaille dans le canton de Vaud
		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Rebetez", "Ulysse", date(1945, 3, 22), Sexe.MASCULIN);
				addForPrincipal(pp, date(1985, 3, 12), MotifFor.MAJORITE, date(1994, 3, 11), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Peseux, ModeImposition.SOURCE);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pp, null, date(1994, 3, 12), null);
				final MenageCommun menage = ensemble.getMenage();
				ids.pp = pp.getId();
				ids.mc = menage.getId();
				addForPrincipal(menage, date(1994, 3, 12), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Neuchatel, ModeImposition.SOURCE);
			}
		});

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber((int) ids.pp);
		request.setDate(new Date(2013, 2, 8));
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

		final RequestHandlerResult result = doInNewTransaction(new TxCallback<RequestHandlerResult>() {
			@Override
			public RequestHandlerResult execute(TransactionStatus status) throws Exception {
				return handler.handle(request);
			}
		});
		assertNotNull(result);

		// on s'assure que la réponse est bien positive et que c'est le ménage qui est retourné
		final AperiodicTaxLiabilityResponse response = (AperiodicTaxLiabilityResponse) result.getResponse();
		assertNotNull(response);
		assertEquals(ids.mc, response.getPartyNumber().intValue());
		assertEquals(ResponseType.OK_TAXPAYER_FOUND, response.getType());

		final TaxResidence mainTaxResidence = response.getMainTaxResidence();
		assertNotNull(mainTaxResidence);
		assertEquals(TaxationAuthorityType.OTHER_CANTON_MUNICIPALITY, mainTaxResidence.getTaxationAuthorityType());
		assertEquals(MockCommune.Neuchatel.getNoOFS(), mainTaxResidence.getTaxationAuthorityFSOId());
	}

	@Test
	public void testHandleSurEnfantVDAvecParentsEnMenage() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		handler.setSecurityProvider(provider);

		final long noIndEnfant = 1234;
		final long noIndPere = 1235;
		final long noIndMere = 1236;
		final RegDate dateNaissanceEnfant = RegDate.get().addYears(-10);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu enfant = addIndividu(noIndEnfant, dateNaissanceEnfant, "Ramaldadji", "Jacques", Sexe.MASCULIN);
				final MockIndividu pere = addIndividu(noIndPere, date(1973, 5, 9), "Ramaldadji", "Robert", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndMere, date(1976, 9, 12), "Ramaldadji", "Mireille", Sexe.FEMININ);
				addLiensFiliation(enfant, pere, mere, dateNaissanceEnfant, null);
			}
		});

		class Ids {
			long pp;
			long mc;
		}
		final Ids ids = new Ids();

		// on crée un habitant qui appartient à un ménage vaudois ordinaire
		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique enfant = addHabitant(noIndEnfant);
				final PersonnePhysique pere = addHabitant(noIndPere);
				final PersonnePhysique mere = addHabitant(noIndMere);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(pere, mere, date(1994, 3, 12), null);
				final MenageCommun menage = ensemble.getMenage();
				addForPrincipal(menage, date(1994, 3, 12), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Vevey);
				addParente(enfant, pere, dateNaissanceEnfant, null);
				addParente(enfant, mere, dateNaissanceEnfant, null);

				ids.pp = enfant.getId();
				ids.mc = menage.getId();
			}
		});

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		request.setLogin(new UserLogin("xxxxx", 22));
		request.setPartyNumber((int) ids.pp);
		request.setDate(new Date(2013, 2, 8));
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

		final RequestHandlerResult result = doInNewTransaction(new TxCallback<RequestHandlerResult>() {
			@Override
			public RequestHandlerResult execute(TransactionStatus status) throws Exception {
				return handler.handle(request);
			}
		});
		assertNotNull(result);

		// on s'assure que la réponse est bien positive et que c'est le ménage des parents qui est retourné
		final AperiodicTaxLiabilityResponse response = (AperiodicTaxLiabilityResponse) result.getResponse();
		assertNotNull(response);
		assertEquals(ids.mc, response.getPartyNumber().intValue());
		assertEquals(ResponseType.OK_TAXPAYER_FOUND, response.getType());

		final TaxResidence mainTaxResidence = response.getMainTaxResidence();
		assertNotNull(mainTaxResidence);
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, mainTaxResidence.getTaxationAuthorityType());
		assertEquals(MockCommune.Vevey.getNoOFS(), mainTaxResidence.getTaxationAuthorityFSOId());
	}

	@Test
	public void testHandleSurEnfantVDAvecParentsUnionLibre() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		handler.setSecurityProvider(provider);

		final long noIndEnfant = 1234;
		final long noIndPere = 1235;
		final long noIndMere = 1236;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu enfant = addIndividu(noIndEnfant, RegDate.get().addYears(-10), "Ramaldadji", "Jacques", Sexe.MASCULIN);
				final MockIndividu pere = addIndividu(noIndPere, date(1973, 5, 9), "Ramaldadji", "Robert", Sexe.MASCULIN);
				final MockIndividu mere = addIndividu(noIndMere, date(1976, 9, 12), "Ramaldadji", "Mireille", Sexe.FEMININ);
				addLiensFiliation(enfant, pere, mere, enfant.getDateNaissance(), null);
			}
		});

		class Ids {
			long pp;
		}
		final Ids ids = new Ids();

		// on crée un habitant qui appartient à un ménage vaudois ordinaire
		doInNewTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique enfant = addHabitant(noIndEnfant);
				final PersonnePhysique pere = addHabitant(noIndPere);
				addForPrincipal(pere, date(1994, 3, 12), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);
				final PersonnePhysique mere = addHabitant(noIndMere);
				addForPrincipal(mere, date(1994, 3, 12), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);

				ids.pp = enfant.getId();
			}
		});

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		request.setLogin(new UserLogin("xxxxx", 22));
		request.setPartyNumber((int) ids.pp);
		request.setDate(new Date(2013, 2, 8));
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

		final RequestHandlerResult result = doInNewTransaction(new TxCallback<RequestHandlerResult>() {
			@Override
			public RequestHandlerResult execute(TransactionStatus status) throws Exception {
				return handler.handle(request);
			}
		});
		assertNotNull(result);

		// on s'assure que la réponse est bien négative car les deux parents ne font pas partie du même ménage
		final AperiodicTaxLiabilityResponse response = (AperiodicTaxLiabilityResponse) result.getResponse();
		assertNotNull(response);
		assertEquals(ids.pp, response.getPartyNumber().intValue());
		assertEquals(ResponseType.TAXPAYER_WITHOUT_TAX_LIABILITY, response.getType());
		assertNull(response.getMainTaxResidence());
	}

	@Test
	public void testHandleSurAncienHabitantVD() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		handler.setSecurityProvider(provider);

		final long noInd = 1234;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noInd, date(1965, 3, 12), "Ramaldadji", "Jacques", Sexe.MASCULIN);
			}
		});

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addHabitant(noInd);
				addForPrincipal(pp, date(1986, 3, 12), MotifFor.MAJORITE, date(1999, 7, 23), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(pp, date(1999, 7, 24), MotifFor.DEMENAGEMENT_VD, date(2004, 7, 23), MotifFor.DEPART_HC, MockCommune.Vevey);
				addForPrincipal(pp, date(2004, 7, 24), MotifFor.DEPART_HC, MockCommune.Neuchatel);
				return pp.getNumero();
			}
		});

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(idPP.intValue());
		request.setDate(new Date(2013, 2, 8));
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

		final RequestHandlerResult result = doInNewTransaction(new TxCallback<RequestHandlerResult>() {
			@Override
			public RequestHandlerResult execute(TransactionStatus status) throws Exception {
				return handler.handle(request);
			}
		});
		assertNotNull(result);

		// on s'assure que la réponse est bien négative, et que l'assujettissement le plus récent est bien retourné
		final AperiodicTaxLiabilityResponse response = (AperiodicTaxLiabilityResponse) result.getResponse();
		assertNotNull(response);
		assertEquals(idPP.intValue(), response.getPartyNumber().intValue());
		assertEquals(ResponseType.FORMER_TAXPAYER, response.getType());

		final TaxResidence mainTaxResidence = response.getMainTaxResidence();
		assertNotNull(mainTaxResidence);
		assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, mainTaxResidence.getTaxationAuthorityType());
		assertEquals(MockCommune.Vevey.getNoOFS(), mainTaxResidence.getTaxationAuthorityFSOId());
		assertEquals(new Date(1999, 7, 24), mainTaxResidence.getDateFrom());
		assertEquals(new Date(2004, 7, 23), mainTaxResidence.getDateTo());
	}

	@Test
	public void testHandleSurHorsCantonAvecImmeubleVD() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		handler.setSecurityProvider(provider);

		// on crée un habitant vaudois ordinaire
		final Long idPP = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Rebetez", "Ulysse", date(1945, 3, 22), Sexe.MASCULIN);
				addForPrincipal(pp, date(1986, 3, 12), MotifFor.DEPART_HC, MockCommune.Neuchatel);
				addForSecondaire(pp, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero();
			}
		});

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		final UserLogin login = new UserLogin("xxxxx", 22);
		request.setLogin(login);
		request.setPartyNumber(idPP.intValue());
		request.setDate(new Date(2013, 2, 8));
		request.setScope(Scope.VD_RESIDENT_AND_WITHHOLDING);

		final RequestHandlerResult result = doInNewTransaction(new TxCallback<RequestHandlerResult>() {
			@Override
			public RequestHandlerResult execute(TransactionStatus status) throws Exception {
				return handler.handle(request);
			}
		});
		assertNotNull(result);

		// on s'assure que la réponse est bien négative, et que le for principal du contribuable est retourné (parce qu'il possède un immeuble dans le canton, selon la spéc)
		final AperiodicTaxLiabilityResponse response = (AperiodicTaxLiabilityResponse) result.getResponse();
		assertNotNull(response);
		assertEquals(idPP.intValue(), response.getPartyNumber().intValue());
		assertEquals(ResponseType.OUT_OF_SCOPE_TAXPAYER, response.getType());

		final TaxResidence mainTaxResidence = response.getMainTaxResidence();
		assertNotNull(mainTaxResidence);
		assertEquals(TaxationAuthorityType.OTHER_CANTON_MUNICIPALITY, mainTaxResidence.getTaxationAuthorityType());
		assertEquals(MockCommune.Neuchatel.getNoOFS(), mainTaxResidence.getTaxationAuthorityFSOId());
	}
}
