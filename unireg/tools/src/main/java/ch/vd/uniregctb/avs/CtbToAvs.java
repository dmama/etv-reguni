package ch.vd.uniregctb.avs;

import javax.xml.ws.BindingProvider;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.message.Message;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.ResourceUtils;

import ch.vd.unireg.webservices.party4.BatchParty;
import ch.vd.unireg.webservices.party4.BatchPartyEntry;
import ch.vd.unireg.webservices.party4.GetBatchPartyRequest;
import ch.vd.unireg.webservices.party4.GetPartyRequest;
import ch.vd.unireg.webservices.party4.PartyWebService;
import ch.vd.unireg.webservices.party4.PartyWebServiceFactory;
import ch.vd.unireg.webservices.party4.WebServiceException;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.party.person.v2.NaturalPerson;
import ch.vd.unireg.xml.party.v2.Party;

public class CtbToAvs {

	// INTEGRATION
//	private static final String urlWebService = "http://unireg-in.etat-de-vaud.ch/fiscalite/int-unireg/ws/party4";
//	private static final String userWebService = "unireg";
//	private static final String pwdWebService = "unireg_1014";

	// PRE-PRODUCTION
	private static final String urlWebService = "http://unireg-pp.etat-de-vaud.ch/fiscalite/unireg/ws/party4";
	private static final String userWebService = "web-it";
	private static final String pwdWebService = "unireg_1014";

	// PRODUCTION
//	private static final String urlWebService = "http://unireg-pr.etat-de-vaud.ch/fiscalite/unireg/ws/party4";
//	private static final String userWebService = "se renseigner...";
//	private static final String pwdWebService = "se renseigner...";

	private static final String userId = "usrfis06";
	private static final int oid = 22;

	private static final int TAILLE_LOT = 100;
	private static final String nomFichier = "input-ctb.csv";
	private static final String fichierDestination = "/tmp/tiers-avec-avs.csv";

	public static void main(String[] args) throws Exception {

		final PartyWebService service = initWebService(urlWebService, userWebService, pwdWebService);
		final GetBatchPartyRequest batchPartyRequest = new GetBatchPartyRequest();
		final UserLogin login = new UserLogin(userId, oid);
		batchPartyRequest.setLogin(login);

		final GetPartyRequest partyRequest = new GetPartyRequest();
		partyRequest.setLogin(login);

		// on lit le contenu du fichier
		final Map<Integer, String> data = new LinkedHashMap<>();
		try (InputStream in = CtbToAvs.class.getResourceAsStream(nomFichier);
		     InputStreamReader fis = new InputStreamReader(in);
		     BufferedReader reader = new BufferedReader(fis)) {

			final Pattern pattern = Pattern.compile("(\\d+)([^\\d].*)?");
			String line = reader.readLine();
			while (line != null) {
				final Matcher m = pattern.matcher(line);
				if (!m.matches()) {
					System.err.println("Line ignorée : '" + line + "'");
				}
				else {
					final int noCtb = Integer.valueOf(m.group(1));
					final String tail = m.group(2);
					data.put(noCtb, tail);
				}
				line = reader.readLine();
			}
		}

		// ensuite, on fait des groupes de TAILLE_LOT
		final List<Integer> ctbs = new ArrayList<>(data.keySet());
		final int nbLots = ctbs.size() / TAILLE_LOT + 1;
		final List<List<Integer>> lots = new ArrayList<>(nbLots);
		for (int i = 0 ; i < nbLots ; ++ i) {
			final List<Integer> lot = ctbs.subList(i * TAILLE_LOT, Math.min((i + 1) * TAILLE_LOT, data.size()));
			if (!lot.isEmpty()) {
				lots.add(lot);
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
			for (List<Integer> lot : lots) {
				batchPartyRequest.getPartyNumbers().clear();
				batchPartyRequest.getPartyNumbers().addAll(lot);

				try {
					final BatchParty result = service.getBatchParty(batchPartyRequest);
					for (BatchPartyEntry entry : result.getEntries()) {
						final Party tiers = entry.getParty();
						dumpTiers(tiers, entry.getNumber(), entry.getExceptionInfo(), data.get(entry.getNumber()), ps);
					}
				}
				catch (WebServiceException e) {
					// problème... on essaie un par un
					for (Integer id : lot) {
						partyRequest.setPartyNumber(id);
						try {
							final Party indivResult = service.getParty(partyRequest);
							dumpTiers(indivResult, id, null, data.get(id), ps);
						}
						catch (WebServiceException e1) {
							dumpTiers(null, id, e1.getMessage(), data.get(id), ps);
						}
					}
				}
			}
		}
		finally {
			if (closeStream) {
				ps.close();
			}
		}
	}

	private static void dumpTiers(@Nullable Party party, long tiersNumber, @Nullable Object exceptionInfo, String tail, PrintStream ps) {
		if (party == null) {
			System.err.println(String.format("%d n'a pas été trouvé (%s)", tiersNumber, exceptionInfo));
			ps.println(String.format("%d%s;", tiersNumber, tail));
		}
		else if (party instanceof NaturalPerson) {
			final Long avs = ((NaturalPerson) party).getIdentification().getVn();
			if (avs != null) {
				ps.println(String.format("%d%s;%d", tiersNumber, tail, avs));
			}
			else {
				ps.println(String.format("%d%s;", tiersNumber, tail));
			}
		}
		else {
			System.err.println(String.format("%d n'est pas une personne physique (%s)", tiersNumber, party.getClass().getSimpleName()));
			ps.println(String.format("%d%s;", tiersNumber, tail));
		}
	}

	private static PartyWebService initWebService(String serviceUrl, String username, String password) throws Exception {
		final URL wsdlUrl = ResourceUtils.getURL("classpath:PartyService4.wsdl");
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
