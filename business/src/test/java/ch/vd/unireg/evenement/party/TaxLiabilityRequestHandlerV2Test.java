package ch.vd.unireg.evenement.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.evenement.party.control.TaxLiabilityControlService;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.taxliab.aperiodic.v2.AperiodicTaxLiabilityRequest;
import ch.vd.unireg.xml.event.party.taxliab.v2.Failure;
import ch.vd.unireg.xml.event.party.taxliab.v2.MinorInfo;
import ch.vd.unireg.xml.event.party.taxliab.v2.TaxLiabilityResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TaxLiabilityRequestHandlerV2Test extends BusinessTest {

	private AperiodicTaxLiabilityRequestHandlerV2 handler;

	private TaxLiabilityControlService taxLiabilityControlService;


	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		taxLiabilityControlService = getBean(TaxLiabilityControlService.class, "taxLiabilityControlService");
		handler = new AperiodicTaxLiabilityRequestHandlerV2();
		handler.setTiersDAO(tiersDAO);
		handler.setTaxliabilityControlService(taxLiabilityControlService);
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
		request.setSearchCommonHouseHolds(false);
		request.setSearchParents(false);

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
		request.setSearchCommonHouseHolds(false);
		request.setSearchParents(false);


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
	public void testHandleSurEnfantVDAvecParentsUnionLibre() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		handler.setSecurityProvider(provider);

		final long noIndEnfant = 1234;
		final long noIndPere = 1235;
		final long noIndMere = 1236;
		final RegDate dateNaissance = RegDate.get().addYears(-10);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu enfant = addIndividu(noIndEnfant, dateNaissance, "Ramaldadji", "Jacques", Sexe.MASCULIN);
				MockIndividu pere = addIndividu(noIndPere, date(1973, 5, 9), "Ramaldadji", "Robert", Sexe.MASCULIN);
				MockIndividu mere = addIndividu(noIndMere, date(1976, 9, 12), "Ramaldadji", "Mireille", Sexe.FEMININ);
				addLiensFiliation(enfant, pere, mere, dateNaissance, null);
			}
		});

		class Ids {
			Long idEnfant;
			Long idPere;
			Long idMere;
		}
		final Ids ids = new Ids();

		// on crée un habitant qui appartient à un ménage vaudois ordinaire
		doInNewTransaction(status -> {
			final PersonnePhysique enfant = addHabitant(noIndEnfant);
			final PersonnePhysique pere = addHabitant(noIndPere);
			addForPrincipal(pere, date(1994, 3, 12), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);
			final PersonnePhysique mere = addHabitant(noIndMere);
			addForPrincipal(mere, date(1994, 3, 12), MotifFor.DEMENAGEMENT_VD, MockCommune.Vevey);

			addParente(enfant, pere, dateNaissance, null);
			addParente(enfant, mere, dateNaissance, null);

			ids.idEnfant = enfant.getId();
			ids.idPere = pere.getId();
			ids.idMere = mere.getId();
			return null;
		});

		final AperiodicTaxLiabilityRequest request = new AperiodicTaxLiabilityRequest();
		request.setLogin(new UserLogin("xxxxx", 22));
		request.setPartyNumber(ids.idEnfant.intValue());
		request.setDate(new Date(2013, 2, 8));
		request.setSearchCommonHouseHolds(false);
		request.setSearchParents(true);


		final RequestHandlerResult result = doInNewTransaction(new TxCallback<RequestHandlerResult>() {
			@Override
			public RequestHandlerResult execute(TransactionStatus status) throws Exception {
				return handler.handle(request);
			}
		});
		assertNotNull(result);

		// on s'assure que la réponse est bien négative car les deux parents ne font pas partie du même ménage
		final TaxLiabilityResponse response = (TaxLiabilityResponse) result.getResponse();
		assertNotNull(response);
		final Failure failure = response.getFailure();
		assertNotNull(failure);
		final MinorInfo noTaxLiableMinorTaxPayer = failure.getNoTaxLiableMinorTaxPayer();
		assertNotNull(noTaxLiableMinorTaxPayer);
		List<Integer> expected = new ArrayList<>();
		expected.add(ids.idPere.intValue());
		expected.add(ids.idMere.intValue());
		final List<Integer> parentsIds = noTaxLiableMinorTaxPayer.getParentPartyNumber();
		assertEquals(2, parentsIds.size());
		Collections.sort(parentsIds);
		Collections.sort(expected);
		for (int i = 0; i < expected.size() ; i++) {
			assertEquals(expected.get(i),parentsIds.get(i));
		}
	}


}
