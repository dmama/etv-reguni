package ch.vd.uniregctb.utils;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;

import ch.vd.unireg.ws.parties.v1.Parties;
import ch.vd.unireg.ws.search.party.v1.SearchResult;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyPart;

public abstract class WebServiceV5Helper {

	public static Party getParty(String urlWebService, String username, String password, String businessUser, int oid, int partyNo, Set<PartyPart> parts) {
		final WebClient client = WebClient.create(urlWebService, username, password, null);
		client.path("party");
		client.path(String.valueOf(partyNo));
		client.query("user", String.format("%s/%d", businessUser, oid));
		if (parts != null && !parts.isEmpty()) {
			client.query("part", parts.toArray(new PartyPart[parts.size()]));
		}
		return client.get(Party.class);
	}

	public static Parties getParties(String urlWebService, String username, String password, String businessUser, int oid, Collection<Integer> partyNos, Set<PartyPart> parts) {
		final WebClient client = WebClient.create(urlWebService, username, password, null);
		client.path("parties");
		client.query("user", String.format("%s/%d", businessUser, oid));
		client.query("partyNo", partyNos.toArray(new Integer[partyNos.size()]));
		if (parts != null && !parts.isEmpty()) {
			client.query("part", parts.toArray(new PartyPart[parts.size()]));
		}
		return client.get(Parties.class);
	}

	public static SearchResult searchParty(String urlWebService, String username, String password, String businessUser, int oid,
	                                 String name, String nameSearchMode, String avs, String ide, Set<String> partyTypes) {
		final WebClient client = WebClient.create(urlWebService, username, password, null);
		client.path("searchParty");
		client.query("user", String.format("%s/%d", businessUser, oid));
		if (StringUtils.isNotBlank(name)) {
			client.query("name", name);
			if (StringUtils.isNotBlank(nameSearchMode)) {
				client.query("nameSearchMode", nameSearchMode);
			}
		}
		if (StringUtils.isNotBlank(avs)) {
			client.query("vn", avs);
		}
		if (StringUtils.isNotBlank(ide)) {
			client.query("uid", ide);
		}
		if (partyTypes != null && !partyTypes.isEmpty()) {
			client.query("partyType", partyTypes.toArray(new String[partyTypes.size()]));
		}
		return client.get(SearchResult.class);
	}
}
