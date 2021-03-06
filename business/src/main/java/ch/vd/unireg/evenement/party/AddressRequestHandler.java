package ch.vd.unireg.evenement.party;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.address.AddressBuilder;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.address.v1.AddressRequest;
import ch.vd.unireg.xml.event.party.address.v1.AddressResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.party.address.v1.AddressType;

public class AddressRequestHandler implements RequestHandlerV1<AddressRequest> {

	private TiersDAO tiersDAO;
	private AdresseService adresseService;
	private SecurityProviderInterface securityProvider;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@Override
	public RequestHandlerResult<AddressResponse> handle(AddressRequest request) throws ServiceException {

		// Vérification des droits d'accès
		final UserLogin login = request.getLogin();
		if (!securityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid())) {
			throw new ServiceException(
					new AccessDeniedExceptionInfo("L'utilisateur spécifié (" + login.getUserId() + '/' + login.getOid() + ") n'a pas les droits d'accès en lecture complète sur l'application.", null));
		}

		if (securityProvider.getDroitAcces(login.getUserId(), request.getPartyNumber()) == null) {
			throw new ServiceException(new AccessDeniedExceptionInfo(
					"L'utilisateur spécifié (" + login.getUserId() + '/' + login.getOid() + ") n'a pas les droits d'accès en lecture sur le tiers n° " + request.getPartyNumber() + '.', null));
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

		return new RequestHandlerResult<>(response);
	}

	@Override
	@NotNull
	public List<String> getRequestXSDs() {
		return Arrays.asList("eCH-0010-4-0.xsd",
		                     "unireg-common-1.xsd",
		                     "party/unireg-party-address-1.xsd",
		                     "event/party/request-1.xsd",
		                     "event/party/address-request-1.xsd");
	}

	@Override
	@NotNull
	public List<String> getResponseXSDs() {
		return Arrays.asList("eCH-0010-4-0.xsd",
		                     "unireg-common-1.xsd",
		                     "unireg-exception-1.xsd",
		                     "party/unireg-party-address-1.xsd",
		                     "event/party/response-1.xsd",
		                     "event/party/address-response-1.xsd");
	}
}
