package ch.vd.uniregctb.avs;

import javax.xml.ws.BindingProvider;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.message.Message;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.ResourceUtils;

import ch.vd.unireg.webservices.party3.PartyWebService;
import ch.vd.unireg.webservices.party3.PartyWebServiceFactory;
import ch.vd.unireg.webservices.party3.SearchPartyRequest;
import ch.vd.unireg.webservices.party3.SearchPartyResponse;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.party.v1.PartyInfo;
import ch.vd.unireg.xml.party.v1.PartyType;

public class AvsToCtb {

	// INTEGRATION
//	private static final String urlWebService = "http://unireg-in.etat-de-vaud.ch/fiscalite/int-unireg/ws/party3";
//	private static final String userWebService = "unireg";
//	private static final String pwdWebService = "unireg_1014";

	// PRE-PRODUCTION
	private static final String urlWebService = "http://unireg-pp.etat-de-vaud.ch/fiscalite/unireg/ws/party3";
	private static final String userWebService = "web-it";
	private static final String pwdWebService = "unireg_1014";

	// PRODUCTION
//	private static final String urlWebService = "http://unireg-pr.etat-de-vaud.ch/fiscalite/unireg/ws/party3";
//	private static final String userWebService = "se renseigner...";
//	private static final String pwdWebService = "se renseigner...";

	private static final String userId = "usrfis06";
	private static final int oid = 22;

	private static final String nomFichier = "input-avs.csv";
	private static final String fichierDestination = "/tmp/tiers-from-avs.csv";

	public static void main(String[] args) throws Exception {

		final PartyWebService service = initWebService(urlWebService, userWebService, pwdWebService);
		final SearchPartyRequest searchRequest = new SearchPartyRequest();
		final UserLogin login = new UserLogin(userId, oid);
		searchRequest.setLogin(login);
		searchRequest.getPartyTypes().add(PartyType.NATURAL_PERSON);

		// on lit le contenu du fichier
		final List<String> avsList = new ArrayList<>();
		try (InputStream in = AvsToCtb.class.getResourceAsStream(nomFichier);
		     InputStreamReader isr = new InputStreamReader(in);
			 BufferedReader reader = new BufferedReader(isr)) {

			String line = reader.readLine();
			while (line != null) {
				avsList.add(line);
				line = reader.readLine();
			}
		}

		final boolean closeStream;
		final PrintStream ps;
		if (fichierDestination == null) {
			ps = System.out;
			closeStream = false;
		}
		else {
			final FileOutputStream stream = new FileOutputStream(fichierDestination);
			ps = new PrintStream(stream);
			closeStream = true;
		}

		// et on boucle sur les lots
		try {
			for (String avs : avsList) {
				searchRequest.setSocialInsuranceNumber(avs);
				try {
					final SearchPartyResponse result = service.searchParty(searchRequest);
					final List<PartyInfo> items = result.getItems();
					dumpTiers(avs, items, null, ps);
				}
				catch (WebServiceException e) {
					dumpTiers(avs, null, e, ps);
				}
			}
		}
		finally {
			if (closeStream) {
				ps.close();
			}
		}
	}

	private static void dumpTiers(String avs, @Nullable List<PartyInfo> items, @Nullable Exception e, PrintStream ps) {
		final StringBuilder b = new StringBuilder(avs);
		b.append(';');
		if (e != null) {
			b.append(e.getClass().getName());
			b.append(';');
			b.append(e.getMessage());
		}
		else if (items != null && !items.isEmpty()) {
			for (PartyInfo item : items) {
				b.append(item.getNumber());
				b.append(';');
			}
		}
		ps.println(b.toString());
	}

	private static PartyWebService initWebService(String serviceUrl, String username, String password) throws Exception {
		final URL wsdlUrl = ResourceUtils.getURL("classpath:PartyService3.wsdl");
		final PartyWebServiceFactory ts = new PartyWebServiceFactory(wsdlUrl);
		final PartyWebService service = ts.getService();
		final Map<String, Object> context = ((BindingProvider) service).getRequestContext();
		if (StringUtils.isNotBlank(username)) {
			context.put(BindingProvider.USERNAME_PROPERTY, username);
			context.put(BindingProvider.PASSWORD_PROPERTY, password);
		}
		context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, serviceUrl);

		// Désactive la validation du schéma (= ignore silencieusement les éléments inconnus), de manière à permettre l'évolution ascendante-compatible du WSDL.
		context.put(Message.SCHEMA_VALIDATION_ENABLED, false);
		context.put("set-jaxb-validation-event-handler", false);

		return service;
	}

}
