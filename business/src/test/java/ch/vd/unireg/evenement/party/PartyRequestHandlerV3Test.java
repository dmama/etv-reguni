package ch.vd.unireg.evenement.party;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.metier.piis.PeriodeImpositionImpotSourceService;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.situationfamille.SituationFamilleService;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.event.party.party.v3.PartyRequest;
import ch.vd.unireg.xml.event.party.party.v3.PartyResponse;
import ch.vd.unireg.xml.party.address.v2.Address;
import ch.vd.unireg.xml.party.address.v2.AddressInformation;
import ch.vd.unireg.xml.party.address.v2.AddressType;
import ch.vd.unireg.xml.party.address.v2.FormattedAddress;
import ch.vd.unireg.xml.party.address.v2.PersonMailAddressInfo;
import ch.vd.unireg.xml.party.address.v2.TariffZone;
import ch.vd.unireg.xml.party.debtor.v3.Debtor;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class PartyRequestHandlerV3Test extends BusinessTest {

	private PartyRequestHandlerV3 handler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		handler = new PartyRequestHandlerV3();
		handler.setAdresseService(getBean(AdresseService.class, "adresseService"));
		handler.setAssujettissementService(getBean(AssujettissementService.class, "assujettissementService"));
		handler.setPeriodeImpositionService(getBean(PeriodeImpositionService.class, "periodeImpositionService"));
		handler.setPeriodeImpositionImpotSourceService(getBean(PeriodeImpositionImpotSourceService.class, "periodeImpositionImpotSourceService"));
		handler.setDiService(getBean(DeclarationImpotService.class, "diService"));
		handler.setHibernateTemplate(hibernateTemplate);
		handler.setIbanValidator(getBean(IbanValidator.class, "ibanValidator"));
		handler.setInfraService(serviceInfra);
		handler.setServiceCivil(serviceCivil);
		handler.setServiceEntreprise(serviceEntreprise);
		handler.setSituationService(getBean(SituationFamilleService.class, "situationFamilleService"));
		handler.setTiersDAO(tiersDAO);
		handler.setTiersService(tiersService);
		handler.setTransactionManager(transactionManager);
		handler.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleUtilisateurSansDroit() throws Exception {

		handler.setSecurityProvider(new MockSecurityProvider());
		try {

			final PartyRequest request = new PartyRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);

			try {
				handler.handle(request);
				fail();
			}
			catch (EsbBusinessException e) {
				assertEquals(EsbBusinessCode.DROITS_INSUFFISANTS, e.getCode());
				assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture complète sur l'application.", e.getMessage());
			}

		}
		finally {
			handler.setSecurityProvider(null);
		}
	}

	@Test
	public void testHandleSurDossierProtege() throws Exception {

		final long tiersId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alfred", "Margoulin", null, Sexe.MASCULIN);
			return pp.getNumero();
		});

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		provider.setDossiersProteges(tiersId);

		handler.setSecurityProvider(provider);
		try {

			final PartyRequest request = new PartyRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.setPartyNumber((int) tiersId);

			doInNewTransactionAndSession(status -> {
				try {
					handler.handle(request);
					fail();
				}
				catch (EsbBusinessException e) {
					assertEquals(EsbBusinessCode.DROITS_INSUFFISANTS, e.getCode());
					assertEquals("L'utilisateur spécifié (xxxxx/22) n'a pas les droits d'accès en lecture sur le tiers n° " + tiersId + ".", e.getMessage());
				}
				return null;
			});
		}
		finally {
			handler.setSecurityProvider(null);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandleSurTiersInconnu() throws Exception {

		final int partyNo = 4224;

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles) {
			@Override
			public Niveau getDroitAcces(String visaOperateur, long tiersId) throws ObjectNotFoundException {
				// en cas de tiers inexistant, le comportement du vrai service est de lancer une exception...
				if (tiersId == partyNo) {
					throw new TiersNotFoundException(tiersId);
				}
				return super.getDroitAcces(visaOperateur, tiersId);
			}
		};

		handler.setSecurityProvider(provider);
		try {
			final PartyRequest request = new PartyRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.setPartyNumber(partyNo);

			try {
				handler.handle(request);
				fail();
			}
			catch (EsbBusinessException e) {
				assertEquals(EsbBusinessCode.CTB_INEXISTANT, e.getCode());
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
			final PartyRequest request = new PartyRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.setPartyNumber(id.intValue());
			request.getParts().add(PartyPart.ADDRESSES);

			final PartyResponse response = (PartyResponse) handler.handle(request).getResponse();
			assertNotNull(response);

			final Party party = response.getParty();
			assertNotNull(party);

			final List<Address> addresses = party.getResidenceAddresses();
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

	/**
	 * Type de débiteur non-supporté par les vieilles versions du service
	 */
	@Test
	public void testGetDebiteurParticipationsHorsSuisse() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);

		final long dpiId = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.PARTICIPATIONS_HORS_SUISSE, PeriodiciteDecompte.MENSUEL, date(2013, 1, 1));
			return dpi.getNumero();
		});

		handler.setSecurityProvider(provider);
		try {
			final PartyRequest request = new PartyRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.setPartyNumber((int) dpiId);
			request.getParts().add(PartyPart.ADDRESSES);

			// web-service call
			final PartyResponse response = doInNewTransactionAndSession(status -> (PartyResponse) handler.handle(request).getResponse());

			assertNotNull(response);
			assertNotNull(response.getParty());
			assertInstanceOf(Debtor.class, response.getParty());
			assertEquals(DebtorCategory.PROFIT_SHARING_FOREIGN_COUNTRY_TAXPAYERS, ((Debtor) response.getParty()).getCategory());
		}
		finally {
			handler.setSecurityProvider(null);
		}
	}

	/**
	 * Type de débiteur non-supporté par les vieilles versions du service
	 */
	@Test
	public void testGetDebiteurEffeuilleuses() throws Exception {

		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);

		final long dpiId = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.EFFEUILLEUSES, PeriodiciteDecompte.MENSUEL, date(2013, 1, 1));
			return dpi.getNumero();
		});

		handler.setSecurityProvider(provider);
		try {
			final PartyRequest request = new PartyRequest();
			final UserLogin login = new UserLogin("xxxxx", 22);
			request.setLogin(login);
			request.setPartyNumber((int) dpiId);
			request.getParts().add(PartyPart.ADDRESSES);

			// web-service call
			final PartyResponse response = doInNewTransactionAndSession(status -> (PartyResponse) handler.handle(request).getResponse());

			assertNotNull(response);
			assertNotNull(response.getParty());
			assertInstanceOf(Debtor.class, response.getParty());
			assertEquals(DebtorCategory.WINE_FARM_SEASONAL_WORKERS, ((Debtor) response.getParty()).getCategory());
		}
		finally {
			handler.setSecurityProvider(null);
		}
	}
}
