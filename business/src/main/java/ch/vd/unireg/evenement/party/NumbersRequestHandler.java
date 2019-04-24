package ch.vd.unireg.evenement.party;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TypeTiers;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.numbers.v1.NumbersRequest;
import ch.vd.unireg.xml.event.party.numbers.v1.NumbersResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.party.v1.PartyType;

public class NumbersRequestHandler implements RequestHandlerV1<NumbersRequest> {

	private TiersDAO tiersDAO;
	private SecurityProviderInterface securityProvider;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@Override
	public RequestHandlerResult<NumbersResponse> handle(NumbersRequest request) throws ServiceException {

		// Vérification des droits d'accès
		final UserLogin login = request.getLogin();
		if (!securityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid())) {
			throw new ServiceException(
					new AccessDeniedExceptionInfo("L'utilisateur spécifié (" + login.getUserId() + '/' + login.getOid() + ") n'a pas les droits d'accès en lecture complète sur l'application.", null));
		}

		// on récupère les ids demandés
		final List<PartyType> types = request.getTypes();
		final Set<TypeTiers> tiersTypes = party2tiers(types);
		final Date time = DateHelper.getCurrentDate();
		final List<Long> ids = tiersDAO.getAllIdsFor(request.isIncludeCancelled(), tiersTypes);

		// construction de la réponse
		final NumbersResponse response = new NumbersResponse();
		if (types != null) {
			response.getTypes().addAll(types);
		}
		response.setTimestamp(XmlUtils.date2xmlcal(time));
		response.setIncludeCancelled(request.isIncludeCancelled());
		response.setIdsCount(ids.size());

		final RequestHandlerResult<NumbersResponse> r = new RequestHandlerResult<>(response);
		r.addAttachment("ids", ids2byteArray(ids));
		return r;
	}

	private static byte[] ids2byteArray(List<Long> ids) {
		try {
			return long2String(ids).getBytes("UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private static String long2String(List<Long> ids) {
		final StringBuilder s = new StringBuilder();
		for (Long id : ids) {
			s.append(id).append('\n');
		}
		return s.toString();
	}

	@Nullable
	private static Set<TypeTiers> party2tiers(@Nullable List<PartyType> types) {
		if (types == null || types.isEmpty()) {
			return null;
		}
		final Set<TypeTiers> party = new HashSet<>();
		for (PartyType type : types) {
			party.addAll(party2tiers(type));
		}
		return party;
	}

	private static List<TypeTiers> party2tiers(PartyType type) {
		switch (type) {
		case CORPORATION:
			return Arrays.asList(TypeTiers.ENTREPRISE, TypeTiers.AUTRE_COMMUNAUTE, TypeTiers.COLLECTIVITE_ADMINISTRATIVE);
		case DEBTOR:
			return Collections.singletonList(TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE);
		case HOUSEHOLD:
			return Collections.singletonList(TypeTiers.MENAGE_COMMUN);
		case NATURAL_PERSON:
			return Collections.singletonList(TypeTiers.PERSONNE_PHYSIQUE);
		default:
			throw new IllegalArgumentException("Type de party inconnu = [" + type + "]");
		}
	}

	@Override
	@NotNull
	public List<String> getRequestXSDs() {
		return Arrays.asList("eCH-0010-4-0.xsd",
		                     "eCH-0044-2-0.xsd",
		                     "unireg-common-1.xsd",
		                     "party/unireg-party-address-1.xsd",
		                     "party/unireg-party-relation-1.xsd",
		                     "party/unireg-party-debtor-type-1.xsd",
		                     "party/unireg-party-taxdeclaration-1.xsd",
		                     "party/unireg-party-taxresidence-1.xsd",
		                     "party/unireg-party-1.xsd",
		                     "party/unireg-party-debtor-1.xsd",
		                     "event/party/request-1.xsd",
		                     "event/party/numbers-request-1.xsd");
	}

	@Override
	@NotNull
	public List<String> getResponseXSDs() {
		return Arrays.asList("eCH-0010-4-0.xsd",
		                     "eCH-0044-2-0.xsd",
		                     "unireg-common-1.xsd",
		                     "unireg-exception-1.xsd",
		                     "party/unireg-party-address-1.xsd",
		                     "party/unireg-party-relation-1.xsd",
		                     "party/unireg-party-debtor-type-1.xsd",
		                     "party/unireg-party-taxdeclaration-1.xsd",
		                     "party/unireg-party-taxresidence-1.xsd",
		                     "party/unireg-party-1.xsd",
		                     "party/unireg-party-debtor-1.xsd",
		                     "event/party/response-1.xsd",
		                     "event/party/numbers-response-1.xsd");
	}
}
