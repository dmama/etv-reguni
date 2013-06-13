package ch.vd.uniregctb.assujettissement;

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
import ch.vd.unireg.xml.common.v1.Date;
import ch.vd.unireg.xml.common.v1.DateHelper;
import ch.vd.unireg.xml.common.v1.DateRange;
import ch.vd.unireg.xml.common.v1.Range;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.party.taxpayer.v1.Taxpayer;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxLiability;
import ch.vd.unireg.xml.party.v1.Party;

public class AssujettissementDumper {

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
	private static final int pf = 2012;
	private static final String nomFichier = "input.csv";
	private static final String fichierDestination = "/tmp/assujettissement.csv";

	private static final DateRange pfRange = new Range(new Date(pf, 1, 1), new Date(pf, 12, 31));

	public static void main(String[] args) throws Exception {

		final PartyWebService service = initWebService(urlWebService, userWebService, pwdWebService);
		final GetBatchPartyRequest batchRequest = new GetBatchPartyRequest();
		final UserLogin login = new UserLogin(userId, oid);
		batchRequest.setLogin(login);
		batchRequest.getParts().add(PartyPart.TAX_LIABILITIES);

		final GetPartyRequest soloRequest = new GetPartyRequest();
		soloRequest.setLogin(login);
		soloRequest.getParts().add(PartyPart.TAX_LIABILITIES);

		// on lit le contenu du fichier
		final List<Integer> ctbs = new ArrayList<>();
		try (InputStream in = AssujettissementDumper.class.getResourceAsStream(nomFichier);
		     InputStreamReader isr = new InputStreamReader(in);
			 BufferedReader reader = new BufferedReader(isr)) {

			String line = reader.readLine();
			while (line != null) {
				ctbs.add(Integer.parseInt(line));
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
				batchRequest.getPartyNumbers().clear();
				batchRequest.getPartyNumbers().addAll(lot);
				try {
					final BatchParty response = service.getBatchParty(batchRequest);
					for (BatchPartyEntry entry : response.getEntries()) {
						final Party party = entry.getParty();
						dumpTiers(party, entry.getNumber(), entry.getExceptionInfo(), ps);
					}
				}
				catch (WebServiceException e) {
					// problème... on essaie un par un
					for (Integer id : lot) {
						soloRequest.setPartyNumber(id);
						try {
							final Party party = service.getParty(soloRequest);
							dumpTiers(party, id, null, ps);
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

	private static void dumpTiers(Party party, int tiersId, @Nullable Object exceptionInfo, PrintStream ps) {
		if (party == null) {
			System.err.println(String.format("%d n'a pas été trouvé (%s)", tiersId, exceptionInfo));
		}
		else if (!(party instanceof Taxpayer)) {
			// quel est l'assujettissement sur la PF demandée ?
			System.err.println(String.format("%d n'est pas un contribuable", tiersId));
		}
		else {
			final Taxpayer taxpayer = (Taxpayer) party;
			final List<TaxLiability> assujettissements = taxpayer.getTaxLiabilities();
			boolean found = false;
			for (TaxLiability tl : assujettissements) {
				if (DateHelper.intersect(tl, pfRange)) {
					final StringBuilder b = new StringBuilder();
					b.append(tiersId).append(";");
					b.append(display(tl.getDateFrom())).append(";");
					b.append(display(tl.getStartReason())).append(";");
					b.append(display(tl.getDateTo())).append(";");
					b.append(display(tl.getEndReason())).append(";");
					b.append(display(tl.getClass().getSimpleName()));
					ps.println(b.toString());
					found = true;
				}
			}
			if (!found) {
				System.err.println(String.format("%d n'est pas assujetti sur la PF %d", tiersId, pf));
			}
		}
	}

	private static String display(Date date) {
		if (date == null) {
			return StringUtils.EMPTY;
		}
		else {
			return String.format("%04d%02d%02d", date.getYear(), date.getMonth(), date.getDay());
		}
	}

	private static String display(Object obj) {
		if (obj == null) {
			return StringUtils.EMPTY;
		}
		else {
			return obj.toString();
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
