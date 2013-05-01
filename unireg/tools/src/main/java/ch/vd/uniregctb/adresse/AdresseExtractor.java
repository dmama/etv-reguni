package ch.vd.uniregctb.adresse;

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

import ch.vd.unireg.webservices.party3.BatchParty;
import ch.vd.unireg.webservices.party3.BatchPartyEntry;
import ch.vd.unireg.webservices.party3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.PartyWebService;
import ch.vd.unireg.webservices.party3.PartyWebServiceFactory;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.party.address.v1.Address;
import ch.vd.unireg.xml.party.address.v1.FormattedAddress;
import ch.vd.unireg.xml.party.v1.Party;

/**
 * Outil pour aller chercher les adresses d'envoi (sur six lignes, donc) des tiers
 * dont les numéros sont passés dans un fichier donné
 */
public class AdresseExtractor {

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

	private static final int TAILLE_LOT = 100;
	private static final String nomFichier = "tiers.csv";
	private static final String fichierDestination = "/tmp/tiers-avec-adresse.csv";

	public static void main(String[] args) throws Exception {

		final PartyWebService service = initWebService(urlWebService, userWebService, pwdWebService);
		final GetBatchPartyRequest batchPartyRequest = new GetBatchPartyRequest();
		final UserLogin login = new UserLogin(userId, oid);
		batchPartyRequest.setLogin(login);
		batchPartyRequest.getParts().add(PartyPart.ADDRESSES);

		final GetPartyRequest partyRequest = new GetPartyRequest();
		partyRequest.setLogin(login);
		partyRequest.getParts().add(PartyPart.ADDRESSES);

		// on lit le contenu du fichier
		final List<Integer> ctbs = new ArrayList<>();
		try (InputStream in = AdresseExtractor.class.getResourceAsStream(nomFichier);
		     InputStreamReader fis = new InputStreamReader(in);
		     BufferedReader reader = new BufferedReader(fis)) {

			String line = reader.readLine();
			while (line != null) {
				final Integer ctb = Integer.valueOf(line);
			    ctbs.add(ctb);
				line = reader.readLine();
			}
		}

		// ensuite, on fait des groupes de TAILLE_LOT
		final int nbLots = ctbs.size() / TAILLE_LOT + 1;
		final List<List<Integer>> lots = new ArrayList<>(nbLots);
		for (int i = 0 ; i < nbLots ; ++ i) {
			final List<Integer> lot = ctbs.subList(i * TAILLE_LOT, Math.min((i + 1) * TAILLE_LOT, ctbs.size()));
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
						dumpTiers(tiers, entry.getNumber(), entry.getExceptionInfo(), ps);
					}
				}
				catch (WebServiceException e) {
					// problème... on essaie un par un
					for (Integer id : lot) {
						partyRequest.setPartyNumber(id);
						try {
							final Party indivResult = service.getParty(partyRequest);
							dumpTiers(indivResult, id, null, ps);
						}
						catch (WebServiceException e1) {
							dumpTiers(null, id, e1.getMessage(), ps);
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

	private static void dumpTiers(@Nullable Party party, long tiersNumber, @Nullable Object exceptionInfo, PrintStream ps) {
		if (party == null) {
			System.err.println(String.format("%d n'a pas été trouvé (%s)", tiersNumber, exceptionInfo));
		}
		else {
			// il faut maintenant trouver la bonne adresse (= celle qui n'a pas de date de fin...)
			Address adrEnvoi = null;
			for (Address adresse : party.getMailAddresses()) {
				if (adresse.getDateTo() == null) {
					adrEnvoi = adresse;
					break;
				}
			}

			if (adrEnvoi == null) {
				System.err.println(String.format("%d n'a pas d'adresse d'envoi", tiersNumber));
			}
			else {
				final FormattedAddress adr = adrEnvoi.getFormattedAddress();
				ps.println(String.format("%d;%s;%s;%s;%s;%s;%s", tiersNumber,
				                         StringUtils.trimToEmpty(adr.getLine1()),
				                         StringUtils.trimToEmpty(adr.getLine2()),
				                         StringUtils.trimToEmpty(adr.getLine3()),
				                         StringUtils.trimToEmpty(adr.getLine4()),
				                         StringUtils.trimToEmpty(adr.getLine5()),
				                         StringUtils.trimToEmpty(adr.getLine6())));
			}
		}
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
