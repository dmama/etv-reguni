package ch.vd.uniregctb.evenement.party;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.numbers.v1.NumbersRequest;
import ch.vd.unireg.xml.event.party.numbers.v1.NumbersResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.party.v1.PartyType;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TypeTiers;
import ch.vd.uniregctb.xml.ServiceException;

public class NumbersRequestHandler implements RequestHandler<NumbersRequest> {

	private TiersDAO tiersDAO;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public RequestHandlerResult handle(NumbersRequest request) throws ServiceException {

		// Vérification des droits d'accès
		final UserLogin login = request.getLogin();
		if (!SecurityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid())) {
			throw new ServiceException(
					new AccessDeniedExceptionInfo("L'utilisateur spécifié (" + login.getUserId() + '/' + login.getOid() + ") n'a pas les droits d'accès en lecture complète sur l'application.", null));
		}

		// on récupère les ids demandés
		final List<PartyType> types = request.getTypes();
		final TypeTiers[] tiersTypes = party2tiers(types);
		final Date time = new Date();
		final List<Long> ids = tiersDAO.getAllIdsFor(request.isIncludeCancelled(), tiersTypes);

		// construction de la réponse
		final NumbersResponse response = new NumbersResponse();
		if (types != null) {
			response.getTypes().addAll(types);
		}
		response.setTimestamp(XmlUtils.date2xmlcal(time));
		response.setIncludeCancelled(request.isIncludeCancelled());
		response.setIdsCount(ids.size());

		final RequestHandlerResult r = new RequestHandlerResult(response);
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
	private static TypeTiers[] party2tiers(@Nullable List<PartyType> types) {
		if (types == null || types.isEmpty()) {
			return null;
		}
		final List<TypeTiers> party = new ArrayList<TypeTiers>();
		for (PartyType type : types) {
			party.addAll(party2tiers(type));
		}
		return party.toArray(new TypeTiers[party.size()]);
	}

	private static List<TypeTiers> party2tiers(PartyType type) {
		switch (type) {
		case CORPORATION:
			return Arrays.asList(TypeTiers.ENTREPRISE, TypeTiers.ETABLISSEMENT, TypeTiers.AUTRE_COMMUNAUTE, TypeTiers.COLLECTIVITE_ADMINISTRATIVE);
		case DEBTOR:
			return Arrays.asList(TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE);
		case HOUSEHOLD:
			return Arrays.asList(TypeTiers.MENAGE_COMMUN);
		case NATURAL_PERSON:
			return Arrays.asList(TypeTiers.PERSONNE_PHYSIQUE);
		default:
			throw new IllegalArgumentException("Type de party inconnu = [" + type + "]");
		}
	}

	private static List<Integer> long2Int(List<Long> ids) {
		final List<Integer> list = new ArrayList<Integer>(ids.size());
		for (Long id : ids) {
			list.add(id.intValue());
		}
		return list;
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/numbers-request-1.xsd");
	}

	@Override
	public List<ClassPathResource> getResponseXSD() {
		return Arrays.asList(new ClassPathResource("event/party/numbers-response-1.xsd"));
	}
}
