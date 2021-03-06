package ch.vd.unireg.evenement.party;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.address.v1.AddressRequest;
import ch.vd.unireg.xml.event.party.address.v1.AddressResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.AddressInformation;
import ch.vd.unireg.xml.party.address.v1.AddressType;
import ch.vd.unireg.xml.party.address.v1.FormattedAddress;
import ch.vd.unireg.xml.party.address.v1.PersonMailAddressInfo;
import ch.vd.unireg.xml.party.address.v1.TariffZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AddressRequestHandlerTest extends BusinessTest {

	private AddressRequestHandler handler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		handler = new AddressRequestHandler();
		handler.setAdresseService(getBean(AdresseService.class, "adresseService"));
		handler.setTiersDAO(tiersDAO);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleUtilisateurSansDroit() throws Exception {

		handler.setSecurityProvider(new MockSecurityProvider());
		try {

			final AddressRequest request = new AddressRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);

			try {
				handler.handle(request);
				fail();
			}
			catch (ServiceException e) {
				assertTrue(e.getInfo() instanceof AccessDeniedExceptionInfo);
				assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture complète sur l'application.", e.getMessage());
			}

		}
		finally {
			handler.setSecurityProvider(null);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleSurDossierProtege() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		provider.setDossiersProteges(4224L);

		handler.setSecurityProvider(provider);
		try {

			final AddressRequest request = new AddressRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.setPartyNumber(4224);

			try {
				handler.handle(request);
				fail();
			}
			catch (ServiceException e) {
				assertTrue(e.getInfo() instanceof AccessDeniedExceptionInfo);
				assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture sur le tiers n° 4224.", e.getMessage());
			}

		}
		finally {
			handler.setSecurityProvider(null);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleSurTiersInconnu() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);

		handler.setSecurityProvider(provider);
		try {
			final AddressRequest request = new AddressRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.setPartyNumber(4224);

			try {
				handler.handle(request);
				fail();
			}
			catch (ServiceException e) {
				assertTrue(e.getInfo() instanceof BusinessExceptionInfo);
				final BusinessExceptionInfo info = (BusinessExceptionInfo) e.getInfo();
				assertEquals(BusinessExceptionCode.UNKNOWN_PARTY.name(), info.getCode());
				assertEquals("Le tiers n°4224 n'existe pas.", e.getMessage());
			}
		}
		finally {
			handler.setSecurityProvider(null);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleRequeteOK() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);

		final Long id = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Michel", "Mabelle", date(1950, 3, 14), Sexe.MASCULIN);
			addAdresseSuisse(pp, TypeAdresseTiers.DOMICILE, date(1950, 3, 14), null, MockRue.Chamblon.RueDesUttins);
			return pp.getNumero();
		});

		handler.setSecurityProvider(provider);
		try {
			final AddressRequest request = new AddressRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.setPartyNumber(id.intValue());
			request.getTypes().add(AddressType.RESIDENCE);

			final AddressResponse response = (AddressResponse) handler.handle(request).getResponse();
			assertNotNull(response);

			final List<Address> addresses = response.getAddresses();
			assertNotNull(addresses);
			assertEquals(1, addresses.size());

			final Address address = addresses.get(0);
			assertNotNull(address);
			assertEquals(AddressType.RESIDENCE, address.getType());
			assertEquals(new Date(1950, 3, 14), address.getDateFrom());
			assertNull(address.getDateTo());

			final AddressInformation info = address.getAddressInformation();
			assertNotNull(info);
			assertNull(info.getAddressLine1());
			assertNull(info.getAddressLine2());
			assertNull(info.getCareOf());
			assertNull(info.getComplementaryInformation());
			assertEquals("CH", info.getCountry());
			assertEquals(Integer.valueOf(8100), info.getCountryId());
			assertEquals("Suisse", info.getCountryName());
			assertNull(info.getDwellingNumber());
			assertNull(info.getForeignZipCode());
			assertNull(info.getHouseNumber());
			assertNull(info.getLocality());
			assertNull(info.getPostOfficeBoxNumber());
			assertNull(info.getPostOfficeBoxText());
			assertEquals("Rue des Uttins", info.getStreet());
			assertEquals(MockRue.Chamblon.RueDesUttins.getNoRue(), info.getStreetId());
			assertEquals(Long.valueOf(1436), info.getSwissZipCode());
			assertEquals(Integer.valueOf(5876), info.getSwissZipCodeId());
			assertNull(info.getSwissZipCodeAddOn());
			assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());
			assertEquals("Chamblon", info.getTown());

			assertNull(address.getCouple());
			assertNull(address.getOrganisation());

			final PersonMailAddressInfo person = address.getPerson();
			assertNotNull(person);
			assertEquals("Monsieur", person.getFormalGreeting());
			assertEquals("Monsieur", person.getSalutation());
			assertEquals("Michel", person.getFirstName());
			assertEquals("Mabelle", person.getLastName());
			assertEquals("2", person.getMrMrs());
			assertNull(person.getTitle());

			final FormattedAddress formatted = address.getFormattedAddress();
			assertNotNull(formatted);
			assertEquals("Monsieur", formatted.getLine1());
			assertEquals("Michel Mabelle", formatted.getLine2());
			assertEquals("Rue des Uttins", formatted.getLine3());
			assertEquals("1436 Chamblon", formatted.getLine4());
			assertNull(formatted.getLine5());
		}
		finally {
			handler.setSecurityProvider(null);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleRequetePaysInconnue() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);

		final Long id = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Michel", "Mabelle", date(1950, 3, 14), Sexe.MASCULIN);
			addAdresseEtrangere(pp, TypeAdresseTiers.DOMICILE, date(1950, 3, 14), null, null, "1234 Glasgow", MockPays.PaysInconnu);
			return pp.getNumero();
		});

		handler.setSecurityProvider(provider);
		try {
			final AddressRequest request = new AddressRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.setPartyNumber(id.intValue());
			request.getTypes().add(AddressType.RESIDENCE);

			final AddressResponse response = (AddressResponse) handler.handle(request).getResponse();
			assertNotNull(response);

			final List<Address> addresses = response.getAddresses();
			assertNotNull(addresses);
			assertEquals(1, addresses.size());

			final Address address = addresses.get(0);
			assertNotNull(address);
			assertTrue(address.isIncomplete());
			assertEquals(AddressType.RESIDENCE, address.getType());
			assertEquals(new Date(1950, 3, 14), address.getDateFrom());
			assertNull(address.getDateTo());

			final AddressInformation info = address.getAddressInformation();
			assertNotNull(info);
			assertEquals(TariffZone.OTHER_COUNTRIES, info.getTariffZone());

		}
		finally {
			handler.setSecurityProvider(null);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleRequeteAdresseIncomplete() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);

		final Long id = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Michel", "Mabelle", date(1950, 3, 14), Sexe.MASCULIN);
			addAdresseEtrangere(pp, TypeAdresseTiers.DOMICILE, date(1950, 3, 14), null, null, "xxx", MockPays.France);
			return pp.getNumero();
		});

		handler.setSecurityProvider(provider);
		try {
			final AddressRequest request = new AddressRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.setPartyNumber(id.intValue());
			request.getTypes().add(AddressType.RESIDENCE);

			final AddressResponse response = (AddressResponse) handler.handle(request).getResponse();
			assertNotNull(response);

			final List<Address> addresses = response.getAddresses();
			assertNotNull(addresses);
			assertEquals(1, addresses.size());

			final Address address = addresses.get(0);
			assertNotNull(address);
			assertEquals(AddressType.RESIDENCE, address.getType());
			assertEquals(new Date(1950, 3, 14), address.getDateFrom());
			assertNull(address.getDateTo());
			assertTrue(address.isIncomplete());

			final AddressInformation info = address.getAddressInformation();
			assertNotNull(info);
			assertEquals(TariffZone.EUROPE, info.getTariffZone());

		}
		finally {
			handler.setSecurityProvider(null);
		}
	}



	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleNomTropLong() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);

		final Long id = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Joana", "Marques Gama Caldas Pimenta Duarte", date(1950, 3, 14), Sexe.FEMININ);
			addAdresseSuisse(pp, TypeAdresseTiers.DOMICILE, date(1950, 3, 14), null, MockRue.Chamblon.RueDesUttins);
			return pp.getNumero();
		});

		handler.setSecurityProvider(provider);
		try {
			final AddressRequest request = new AddressRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.setPartyNumber(id.intValue());
			request.getTypes().add(AddressType.RESIDENCE);

			final AddressResponse response = (AddressResponse) handler.handle(request).getResponse();
			assertNotNull(response);

			final List<Address> addresses = response.getAddresses();
			assertNotNull(addresses);
			assertEquals(1, addresses.size());

			final Address address = addresses.get(0);

			assertNull(address.getCouple());
			assertNull(address.getOrganisation());

			final PersonMailAddressInfo person = address.getPerson();
			assertNotNull(person);
			assertEquals("Madame", person.getFormalGreeting());
			assertEquals("Madame", person.getSalutation());
			assertEquals("Joana", person.getFirstName());
			assertEquals("Marques Gama Caldas Pimenta Du", person.getLastName());
			assertNull(person.getTitle());

			final FormattedAddress formatted = address.getFormattedAddress();
			assertNotNull(formatted);
			assertEquals("Madame", formatted.getLine1());
			assertEquals("Joana Marques Gama Caldas Pimenta Duarte", formatted.getLine2());
			assertEquals("Rue des Uttins", formatted.getLine3());
			assertEquals("1436 Chamblon", formatted.getLine4());
			assertNull(formatted.getLine5());
		}
		finally {
			handler.setSecurityProvider(null);
		}
	}
}
