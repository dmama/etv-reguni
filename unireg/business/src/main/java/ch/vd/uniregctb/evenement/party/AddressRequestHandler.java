package ch.vd.uniregctb.evenement.party;

import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.address.v1.AddressRequest;
import ch.vd.unireg.xml.event.party.address.v1.AddressResponse;
import ch.vd.unireg.xml.event.party.v1.Response;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.party.address.v1.AddressType;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.ServiceException;
import ch.vd.uniregctb.xml.address.AddressBuilder;

public class AddressRequestHandler implements PartyRequestHandler<AddressRequest> {

	private TiersDAO tiersDAO;
	private AdresseService adresseService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@Override
	public Response handle(AddressRequest request) throws ServiceException {

		// Vérification des droits d'accès
		final UserLogin login = request.getLogin();
		if (!SecurityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid())) {
			throw new ServiceException(
					new AccessDeniedExceptionInfo("L'utilisateur spécifié (" + login.getUserId() + "/" + login.getOid() + ") n'a pas les droits d'accès en lecture complète sur l'application.", null));
		}

		if (SecurityProvider.getDroitAcces(login.getUserId(), request.getPartyNumber()) == null) {
			throw new ServiceException(new AccessDeniedExceptionInfo(
					"L'utilisateur spécifié (" + login.getUserId() + "/" + login.getOid() + ") n'a pas les droits d'accès en lecture sur le tiers n° " + request.getPartyNumber() + ".", null));
		}

		// Récupération du tiers
		final Tiers tiers = tiersDAO.get(request.getPartyNumber(), true);
		if (tiers == null) {
			throw new ServiceException(new BusinessExceptionInfo("Le tiers n°" + request.getPartyNumber() + " n'existe pas.", BusinessExceptionCode.UNKNOWN_PARTY.name(), null));
		}

		// Calcul de l'adresse
		final AddressResponse response = new AddressResponse();
		try {
			for (AddressType type : request.getTypes()) {
				final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(tiers, DataHelper.xmlToCore(request.getDate()), DataHelper.xmlToCore(type), false);
				response.getAddresses().add(AddressBuilder.newAddress(adresse, type));
			}
		}
		catch (AdresseException e) {
			throw new ServiceException(new BusinessExceptionInfo(e.getMessage(), BusinessExceptionCode.ADDRESSES.name(), null));
		}

		return response;
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/address-request-1.xsd");
	}

	@Override
	public ClassPathResource getResponseXSD() {
		return new ClassPathResource("event/party/address-response-1.xsd");
	}
}
