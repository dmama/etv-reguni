package ch.vd.uniregctb.evenement.adresses;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.unireg.xml.address.Address;
import ch.vd.unireg.xml.address.AddressInformation;
import ch.vd.unireg.xml.address.AddressType;
import ch.vd.unireg.xml.address.FormattedAddress;
import ch.vd.unireg.xml.address.PersonMailAddressInfo;
import ch.vd.unireg.xml.address.TariffZone;
import ch.vd.unireg.xml.common.Date;
import ch.vd.unireg.xml.common.UserLogin;
import ch.vd.unireg.xml.event.address.GetAddressRequest;
import ch.vd.unireg.xml.event.address.GetAddressResponse;
import ch.vd.unireg.xml.exception.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.BusinessExceptionInfo;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.security.MockSecurityProvider;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.xml.ServiceException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GetAdressesRequestListenerTest extends BusinessTest {

	private GetAdressesRequestListener listener;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		listener = new GetAdressesRequestListener();
		listener.setAdresseService(getBean(AdresseService.class, "adresseService"));
		listener.setTiersDAO(tiersDAO);
	}

	@Test
	public void testHandleUtilisateurSansDroit() throws Exception {

		pushSecurityProvider(new MockSecurityProvider());
		try {

			final GetAddressRequest request = new GetAddressRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);

			try {
				listener.handle(request);
				fail();
			}
			catch (ServiceException e) {
				assertTrue(e.getInfo() instanceof AccessDeniedExceptionInfo);
				assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture complète sur l'application.", e.getMessage());
			}

		}
		finally {
			popSecurityProvider();
		}
	}

	@Test
	public void testHandleSurDossierProtege() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		provider.setDossiersProteges(4224L);

		pushSecurityProvider(provider);
		try {

			final GetAddressRequest request = new GetAddressRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.setPartyNumber(4224);

			try {
				listener.handle(request);
				fail();
			}
			catch (ServiceException e) {
				assertTrue(e.getInfo() instanceof AccessDeniedExceptionInfo);
				assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture sur le tiers n° 4224.", e.getMessage());
			}

		}
		finally {
			popSecurityProvider();
		}
	}

	@Test
	public void testHandleSurTiersInconnu() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);

		pushSecurityProvider(provider);
		try {
			final GetAddressRequest request = new GetAddressRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.setPartyNumber(4224);

			try {
				listener.handle(request);
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
			popSecurityProvider();
		}
	}

	@Test
	public void testHandleRequeteOK() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Michel", "Mabelle", date(1950, 3, 14), Sexe.MASCULIN);
				addAdresseSuisse(pp, TypeAdresseTiers.DOMICILE, date(1950, 3, 14), null, MockRue.Chamblon.RueDesUttins);
				return pp.getNumero();
			}
		});

		pushSecurityProvider(provider);
		try {
			final GetAddressRequest request = new GetAddressRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.setPartyNumber(id.intValue());
			request.getTypes().add(AddressType.RESIDENCE);

			final GetAddressResponse response = listener.handle(request);
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
			assertEquals(Integer.valueOf(198539), info.getStreetId());
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
			popSecurityProvider();
		}
	}
}
