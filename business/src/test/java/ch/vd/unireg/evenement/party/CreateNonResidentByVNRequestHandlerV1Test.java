package ch.vd.unireg.evenement.party;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.upi.mock.DefaultMockServiceUpi;
import ch.vd.unireg.interfaces.upi.mock.ServiceUpiProxy;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TypeTiers;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.common.v2.UserLogin;
import ch.vd.unireg.xml.event.party.nonresident.vn.v1.CreateNonresidentByVNRequest;
import ch.vd.unireg.xml.event.party.nonresident.vn.v1.CreateNonresidentByVNResponse;
import ch.vd.unireg.xml.event.party.v2.ExceptionResponse;
import ch.vd.unireg.xml.event.party.v2.Response;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.ServiceExceptionInfo;

public class CreateNonResidentByVNRequestHandlerV1Test extends BusinessTest {

	private static final UserLogin USER_LOGIN = new UserLogin("USER/22");

	private CreateNonresidentByVNRequestHandlerV1 handler;
	private ServiceUpiProxy serviceUpi;

	@Override
	public void onSetUp() throws Exception {
		// petit passage par l'indexation des tiers activée pour vider l'indexeur en entrée
		setWantIndexationTiers(true);
		try {

			super.onSetUp();

			serviceUpi = new ServiceUpiProxy();

			handler = new CreateNonresidentByVNRequestHandlerV1();
			handler.setHibernateTemplate(hibernateTemplate);
			handler.setServiceUpi(serviceUpi);
			handler.setTiersSearcher(globalTiersSearcher);
		}
		finally {
			setWantIndexationTiers(false);
		}
	}

	@Test
	public void testSansDroit() throws Exception {
		handler.setSecurityProvider(new MockSecurityProvider());
		try {
			final CreateNonresidentByVNRequest request = new CreateNonresidentByVNRequest(USER_LOGIN, 7561111111111L);
			handler.handle(request);
			Assert.fail("Le demandeur n'a pas les droits de créer un nouveau contribuable");
		}
		catch (ServiceException e) {
			Assert.assertTrue(e.getInfo().getClass().getName(), e.getInfo() instanceof AccessDeniedExceptionInfo);
		}
	}

	@Test
	public void testCreationSansProbleme() throws Exception {
		handler.setSecurityProvider(new MockSecurityProvider(Role.CREATE_NONHAB));
		serviceUpi.setUp(new DefaultMockServiceUpi());

		final String noAvs = "7565115001333";
		Assert.assertNotNull(serviceUpi.getPersonInfo(noAvs));

		final RequestHandlerResult<Response> result = doInNewTransactionAndSession(new TxCallback<RequestHandlerResult<Response>>() {
			@Override
			public RequestHandlerResult<Response> execute(TransactionStatus status) throws Exception {
				final CreateNonresidentByVNRequest request = new CreateNonresidentByVNRequest(USER_LOGIN, Long.parseLong(noAvs));
				final RequestHandlerResult<Response> result = handler.handle(request);
				Assert.assertNotNull(result);
				return result;
			}
		});
		final Response response = result.getResponse();
		Assert.assertNotNull(response);
		Assert.assertEquals(CreateNonresidentByVNResponse.class, response.getClass());

		final long id = ((CreateNonresidentByVNResponse) response).getNumber();
		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersDAO.get(id);
			Assert.assertNotNull(tiers);
			Assert.assertEquals(PersonnePhysique.class, tiers.getClass());

			// toutes les données viennent du DefaultMockServiceUpi...
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			Assert.assertFalse(pp.isHabitantVD());
			Assert.assertEquals(noAvs, pp.getNumeroAssureSocial());
			Assert.assertEquals("Susan", pp.getPrenomUsuel());
			Assert.assertEquals("Susan Alexandra", pp.getTousPrenoms());
			Assert.assertEquals("Weaver", pp.getNom());
			Assert.assertEquals(date(1949, 10, 8), pp.getDateNaissance());
			Assert.assertEquals(Sexe.FEMININ, pp.getSexe());
			Assert.assertEquals((Integer) MockPays.EtatsUnis.getNoOFS(), pp.getNumeroOfsNationalite());
			Assert.assertEquals("Inglis", pp.getNomMere());
			Assert.assertEquals("Elisabeth", pp.getPrenomsMere());
			Assert.assertEquals("Weaver", pp.getNomPere());
			Assert.assertEquals("Sylvester", pp.getPrenomsPere());

			Assert.assertEquals(0, pp.getForsFiscaux().size());
			return null;
		});
	}

	@Test
	public void testCreationSansProblemeAvecRemplacementNumeroAVS() throws Exception {
		handler.setSecurityProvider(new MockSecurityProvider(Role.CREATE_NONHAB));
		serviceUpi.setUp(new DefaultMockServiceUpi());

		final String noAvs = "7567986294906";
		final String noAvsRemplacant = "7565115001333";
		Assert.assertNotNull(serviceUpi.getPersonInfo(noAvs));

		final RequestHandlerResult<Response> result = doInNewTransactionAndSession(new TxCallback<RequestHandlerResult<Response>>() {
			@Override
			public RequestHandlerResult<Response> execute(TransactionStatus status) throws Exception {
				final CreateNonresidentByVNRequest request = new CreateNonresidentByVNRequest(USER_LOGIN, Long.parseLong(noAvs));
				final RequestHandlerResult<Response> result = handler.handle(request);
				Assert.assertNotNull(result);
				return result;
			}
		});
		final Response response = result.getResponse();
		Assert.assertNotNull(response);
		Assert.assertEquals(CreateNonresidentByVNResponse.class, response.getClass());

		final long id = ((CreateNonresidentByVNResponse) response).getNumber();
		doInNewTransactionAndSession(status -> {
			final Tiers tiers = tiersDAO.get(id);
			Assert.assertNotNull(tiers);
			Assert.assertEquals(PersonnePhysique.class, tiers.getClass());

			// toutes les données viennent du DefaultMockServiceUpi...
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			Assert.assertFalse(pp.isHabitantVD());
			Assert.assertEquals(noAvsRemplacant, pp.getNumeroAssureSocial());
			Assert.assertEquals("Susan", pp.getPrenomUsuel());
			Assert.assertEquals("Susan Alexandra", pp.getTousPrenoms());
			Assert.assertEquals("Weaver", pp.getNom());
			Assert.assertEquals(date(1949, 10, 8), pp.getDateNaissance());
			Assert.assertEquals(Sexe.FEMININ, pp.getSexe());
			Assert.assertEquals((Integer) MockPays.EtatsUnis.getNoOFS(), pp.getNumeroOfsNationalite());
			Assert.assertEquals("Inglis", pp.getNomMere());
			Assert.assertEquals("Elisabeth", pp.getPrenomsMere());
			Assert.assertEquals("Weaver", pp.getNomPere());
			Assert.assertEquals("Sylvester", pp.getPrenomsPere());

			Assert.assertEquals(0, pp.getForsFiscaux().size());
			return null;
		});
	}

	@Test
	public void testCreationNAVSTotalementInconnu() throws Exception {
		handler.setSecurityProvider(new MockSecurityProvider(Role.CREATE_NONHAB));
		serviceUpi.setUp(new DefaultMockServiceUpi());

		final String noAvs = "7569999999991";
		Assert.assertNull(serviceUpi.getPersonInfo(noAvs));

		final RequestHandlerResult<Response> result = doInNewTransactionAndSession(new TxCallback<RequestHandlerResult<Response>>() {
			@Override
			public RequestHandlerResult<Response> execute(TransactionStatus status) throws Exception {
				final CreateNonresidentByVNRequest request = new CreateNonresidentByVNRequest(USER_LOGIN, Long.parseLong(noAvs));
				final RequestHandlerResult<Response> result = handler.handle(request);
				Assert.assertNotNull(result);
				return result;
			}
		});
		final Response response = result.getResponse();
		Assert.assertNotNull(response);
		Assert.assertEquals(ExceptionResponse.class, response.getClass());

		final ExceptionResponse exceptResponse = (ExceptionResponse) response;
		final ServiceExceptionInfo exceptionInfo = exceptResponse.getExceptionInfo();
		Assert.assertNotNull(exceptionInfo);
		Assert.assertEquals(BusinessExceptionInfo.class, exceptionInfo.getClass());
		final BusinessExceptionInfo bei = (BusinessExceptionInfo) exceptionInfo;
		Assert.assertEquals("Numéro AVS inconnu à l'UPI.", bei.getMessage());
		Assert.assertEquals(BusinessExceptionCode.UNKNOWN_PARTY.name(), bei.getCode());

		doInNewTransactionAndSession(status -> {
			final List<Long> idsPP = tiersDAO.getAllIdsFor(true, TypeTiers.PERSONNE_PHYSIQUE);
			Assert.assertNotNull(idsPP);
			Assert.assertEquals(0, idsPP.size());
			return null;
		});
	}

	@Test
	public void testCreationAvecContribuableConnuAvecNAVSFourni() throws Exception {
		handler.setSecurityProvider(new MockSecurityProvider(Role.CREATE_NONHAB));
		serviceUpi.setUp(new DefaultMockServiceUpi());

		final String noAvs = "7569050304498";
		Assert.assertNotNull(serviceUpi.getPersonInfo(noAvs));

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		// création d'un tiers PP avec ce même numéro
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Viersteine", date(1908, 6, 14), Sexe.MASCULIN);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// attente de fin d'indexation
		globalTiersIndexer.sync();

		final RequestHandlerResult<Response> result = doInNewTransactionAndSession(new TxCallback<RequestHandlerResult<Response>>() {
			@Override
			public RequestHandlerResult<Response> execute(TransactionStatus status) throws Exception {
				final CreateNonresidentByVNRequest request = new CreateNonresidentByVNRequest(USER_LOGIN, Long.parseLong(noAvs));
				final RequestHandlerResult<Response> result = handler.handle(request);
				Assert.assertNotNull(result);
				return result;
			}
		});
		final Response response = result.getResponse();
		Assert.assertNotNull(response);
		Assert.assertEquals(ExceptionResponse.class, response.getClass());

		final ExceptionResponse exceptResponse = (ExceptionResponse) response;
		final ServiceExceptionInfo exceptionInfo = exceptResponse.getExceptionInfo();
		Assert.assertNotNull(exceptionInfo);
		Assert.assertEquals(BusinessExceptionInfo.class, exceptionInfo.getClass());
		final BusinessExceptionInfo bei = (BusinessExceptionInfo) exceptionInfo;
		Assert.assertEquals("Un contribuable existe déjà avec ce numéro AVS.", bei.getMessage());
		Assert.assertEquals(BusinessExceptionCode.ALREADY_EXISTING_PARTY.name(), bei.getCode());

		doInNewTransactionAndSession(status -> {
			final List<Long> idsPP = tiersDAO.getAllIdsFor(true, TypeTiers.PERSONNE_PHYSIQUE);
			Assert.assertNotNull(idsPP);
			Assert.assertEquals(1, idsPP.size());

			final Long foundId = idsPP.get(0);
			Assert.assertNotNull(foundId);
			Assert.assertEquals((Long) ppId, foundId);
			return null;
		});
	}

	@Test
	public void testCreationAvecContribuableConnuAvecNAVSRemplacantCeluiFourni() throws Exception {
		handler.setSecurityProvider(new MockSecurityProvider(Role.CREATE_NONHAB));
		serviceUpi.setUp(new DefaultMockServiceUpi());

		final String noAvs = "7560142399040";
		final String noAvsRemplacant = "7569050304498";
		Assert.assertNotNull(serviceUpi.getPersonInfo(noAvs));
		Assert.assertEquals(noAvsRemplacant, serviceUpi.getPersonInfo(noAvs).getNoAvs13());

		// besoin d'indexation des tiers manipulés ici
		setWantIndexationTiers(true);

		// création d'un tiers PP avec ce même numéro
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Viersteine", date(1908, 6, 14), Sexe.MASCULIN);
			pp.setNumeroAssureSocial(noAvsRemplacant);
			return pp.getNumero();
		});

		// attente de fin d'indexation
		globalTiersIndexer.sync();

		final RequestHandlerResult<Response> result = doInNewTransactionAndSession(new TxCallback<RequestHandlerResult<Response>>() {
			@Override
			public RequestHandlerResult<Response> execute(TransactionStatus status) throws Exception {
				final CreateNonresidentByVNRequest request = new CreateNonresidentByVNRequest(USER_LOGIN, Long.parseLong(noAvs));
				final RequestHandlerResult<Response> result = handler.handle(request);
				Assert.assertNotNull(result);
				return result;
			}
		});
		final Response response = result.getResponse();
		Assert.assertNotNull(response);
		Assert.assertEquals(ExceptionResponse.class, response.getClass());

		final ExceptionResponse exceptResponse = (ExceptionResponse) response;
		final ServiceExceptionInfo exceptionInfo = exceptResponse.getExceptionInfo();
		Assert.assertNotNull(exceptionInfo);
		Assert.assertEquals(BusinessExceptionInfo.class, exceptionInfo.getClass());
		final BusinessExceptionInfo bei = (BusinessExceptionInfo) exceptionInfo;
		Assert.assertEquals("Un contribuable existe déjà avec ce numéro AVS.", bei.getMessage());
		Assert.assertEquals(BusinessExceptionCode.ALREADY_EXISTING_PARTY.name(), bei.getCode());

		doInNewTransactionAndSession(status -> {
			final List<Long> idsPP = tiersDAO.getAllIdsFor(true, TypeTiers.PERSONNE_PHYSIQUE);
			Assert.assertNotNull(idsPP);
			Assert.assertEquals(1, idsPP.size());

			final Long foundId = idsPP.get(0);
			Assert.assertNotNull(foundId);
			Assert.assertEquals((Long) ppId, foundId);
			return null;
		});
	}
}
