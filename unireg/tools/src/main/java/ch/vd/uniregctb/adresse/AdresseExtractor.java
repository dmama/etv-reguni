package ch.vd.uniregctb.adresse;

import javax.xml.ws.BindingProvider;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.message.Message;
import org.springframework.util.ResourceUtils;

import ch.vd.uniregctb.webservices.tiers2.AdresseEnvoi;
import ch.vd.uniregctb.webservices.tiers2.BatchTiers;
import ch.vd.uniregctb.webservices.tiers2.BatchTiersEntry;
import ch.vd.uniregctb.webservices.tiers2.GetBatchTiers;
import ch.vd.uniregctb.webservices.tiers2.Tiers;
import ch.vd.uniregctb.webservices.tiers2.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.TiersPort;
import ch.vd.uniregctb.webservices.tiers2.TiersService;
import ch.vd.uniregctb.webservices.tiers2.UserLogin;

/**
 * Outil pour aller chercher les adresses d'envoi (sur six lignes, donc) des contribuables
 * dont les numéros sont passés dans un fichier donné
 */
public class AdresseExtractor {

	// INTEGRATION
	private static final String urlWebServiceTiers2 = "http://ssv0309v.etat-de-vaud.ch:41060/fiscalite/int-unireg/ws/tiers2";
	private static final String userWebService = "web-it";
	private static final String pwdWebService = "unireg_1014";

	// PRODUCTION
//	private static final String urlWebServiceTiers2 = "http://unireg-pr.etat-de-vaud.ch:80/fiscalite/unireg/ws/tiers2";
//	private static final String userWebService = "se renseigner...";
//	private static final String pwdWebService = "se renseigner...";

	private static final String userId = "usrfis06";
	private static final int oid = 22;

	private static final int TAILLE_LOT = 100;
	private static final String nomFichier = "ctbs.csv";
	
	public static void main(String[] args) throws Exception {

		final TiersPort tiersPort = initWebService(urlWebServiceTiers2, userWebService, pwdWebService);
		final GetBatchTiers batchTiers = new GetBatchTiers();
		final UserLogin login = new UserLogin();
		login.setOid(oid);
		login.setUserId(userId);
		batchTiers.setLogin(login);
		batchTiers.getParts().add(TiersPart.ADRESSES_ENVOI);

		final InputStream in = AdresseExtractor.class.getResourceAsStream(nomFichier);
		final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		// on lit le contenu du fichier
		final List<Long> ctbs = new ArrayList<Long>();
		String line = reader.readLine();
		while (line != null) {
			final Long ctb = Long.valueOf(line);
		    ctbs.add(ctb);
			line = reader.readLine();
		}
		reader.close();

		// ensuite, on fait des groupes de TAILLE_LOT
		final int nbLots = ctbs.size() / TAILLE_LOT + 1;
		final List<List<Long>> lots = new ArrayList<List<Long>>(nbLots);
		for (int i = 0 ; i < nbLots ; ++ i) {
			final List<Long> lot = ctbs.subList(i * TAILLE_LOT, Math.min((i + 1) * TAILLE_LOT, ctbs.size()));
			if (lot.size() > 0) {
				lots.add(lot);
			}
		}

		// et on boucle sur les lots
		for (List<Long> lot : lots) {
			batchTiers.getTiersNumbers().clear();
			batchTiers.getTiersNumbers().addAll(lot);

			final BatchTiers result = tiersPort.getBatchTiers(batchTiers);
			for (BatchTiersEntry entry : result.getEntries()) {
				final Tiers tiers = entry.getTiers();
				if (tiers == null) {
					System.err.println(String.format("%d n'a pas été trouvé (%s)", entry.getNumber(), entry.getExceptionMessage()));
				}
				else if (tiers.getAdresseEnvoi() == null) {
					System.err.println(String.format("%d n'a pas d'adresse d'envoi", entry.getNumber()));
				}
				else {
					final AdresseEnvoi adr = tiers.getAdresseEnvoi();
					System.out.println(String.format("%d;%s;%s;%s;%s;%s;%s", entry.getNumber(),
							StringUtils.trimToEmpty(adr.getLigne1()),
							StringUtils.trimToEmpty(adr.getLigne2()),
							StringUtils.trimToEmpty(adr.getLigne3()),
							StringUtils.trimToEmpty(adr.getLigne4()),
							StringUtils.trimToEmpty(adr.getLigne5()),
							StringUtils.trimToEmpty(adr.getLigne6())));
				}
			}
		}
	}

	private static TiersPort initWebService(String serviceUrl, String username, String password) throws Exception {
		URL wsdlUrl = ResourceUtils.getURL("classpath:TiersService2.wsdl");
		final TiersService ts = new TiersService(wsdlUrl);
		final TiersPort service = ts.getTiersPortPort();
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
