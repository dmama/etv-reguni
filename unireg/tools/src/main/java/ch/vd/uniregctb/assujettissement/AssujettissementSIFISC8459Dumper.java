package ch.vd.uniregctb.assujettissement;

import javax.xml.ws.BindingProvider;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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

public class AssujettissementSIFISC8459Dumper {

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

	private static final String userId = "xsijde";
	private static final int oid = 22;

	private static final int TAILLE_LOT = 100;
	private static final int pfMin = 2011;
	private static final int pfMax = 2013;
	private static final String nomFichier = "input.csv";
	private static final String fichierDestination = "/tmp/assujettissement.csv";

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
		final List<List<Integer>> ctbs = new ArrayList<>();
		try (InputStream in = AssujettissementSIFISC8459Dumper.class.getResourceAsStream(nomFichier);
		     InputStreamReader isr = new InputStreamReader(in);
			 BufferedReader reader = new BufferedReader(isr)) {

			String line = reader.readLine();
			while (line != null) {
				final String[] split = line.split(";");
				final List<Integer> localCtbs = new ArrayList<>(split.length);
				for (String noCtbStr : split) {
					localCtbs.add(Integer.parseInt(noCtbStr));
				}
				ctbs.add(localCtbs);
				line = reader.readLine();
			}
		}

		// ensuite, on fait des groupes de TAILLE_LOT
		final int nbLots = ctbs.size() / TAILLE_LOT + 1;
		final List<List<Integer>> lots = new ArrayList<>(nbLots);
		for (int i = 0 ; i < nbLots ; ++ i) {
			final List<Integer> lot = new LinkedList<>();
			for (List<Integer> line : ctbs.subList(i * TAILLE_LOT, Math.min((i + 1) * TAILLE_LOT, ctbs.size()))) {
				lot.addAll(line.subList(1, line.size()));
			}
			if (!lot.isEmpty()) {
				lots.add(lot);
			}
		}

		// et on boucle sur les lots
		@SuppressWarnings("unchecked") final Map<Integer, String>[] assujettissements = new Map[pfMax - pfMin + 1];
		for (int i = 0 ; i < pfMax - pfMin + 1 ; ++ i) {
			assujettissements[i] = new HashMap<>();
		}
		for (List<Integer> lot : lots) {
			batchRequest.getPartyNumbers().clear();
			batchRequest.getPartyNumbers().addAll(lot);
			try {
				final BatchParty response = service.getBatchParty(batchRequest);
				for (BatchPartyEntry entry : response.getEntries()) {
					final Party party = entry.getParty();
					fillMaps(party, entry.getNumber(), entry.getExceptionInfo(), assujettissements);
				}
			}
			catch (WebServiceException e) {
				// problème... on essaie un par un
				for (Integer id : lot) {
					soloRequest.setPartyNumber(id);
					try {
						final Party party = service.getParty(soloRequest);
						fillMaps(party, id, null, assujettissements);
					}
					catch (WebServiceException e1) {
						fillMaps(null, id, e1.getMessage(), assujettissements);
					}
				}
			}
		}

		try (FileOutputStream stream = new FileOutputStream(fichierDestination); PrintStream ps = new PrintStream(stream)) {
			ps.print("IND;PP_ID;MC_ID");
			for (int pf = pfMin ; pf <= pfMax ; ++ pf) {
				ps.print(";PP_ASS_"+pf+";MC_ASS_"+pf);
			}
			ps.println();

			for (List<Integer> ctbGroup : ctbs) {
				if (ctbGroup.size() > 3) {
					throw new IllegalArgumentException("Pas prévu plus de 2 ctbs en entrée");
				}

				final int ind = ctbGroup.get(0);
				final int pp = ctbGroup.get(1);
				final Integer mc = ctbGroup.size() > 2 ? ctbGroup.get(2) : null;

				ps.print(ind);
				ps.print(';');
				ps.print(pp);
				ps.print(';');
				if (ctbGroup.size() > 2) {
					ps.print(display(mc));
				}
				for (int pf = pfMin ; pf <= pfMax ; ++ pf) {
					final Map<Integer, String> assPf = assujettissements[pf - pfMin];
					final String assPpStr = assPf.get(pp);
					ps.print(';');
					ps.print(display(assPpStr));
					ps.print(';');
					if (mc != null) {
						final String assMcStr = assPf.get(mc);
						ps.print(display(assMcStr));
					}
				}
				ps.println();
			}
		}
	}

	private static void fillMaps(Party party, int tiersId, @Nullable Object exceptionInfo, Map<Integer, String>[] maps) {
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
			for (int pf = pfMin ; pf <= pfMax ; ++ pf) {
				final DateRange range = new Range(new Date(pf, 1, 1), new Date(pf, 12, 31));
				final StringBuilder b = new StringBuilder();
				for (TaxLiability tl : assujettissements) {
					if (DateHelper.intersect(tl, range)) {
						if (b.length() > 0) {
							b.append(" / ");
						}
						b.append(tl.getClass().getSimpleName());
					}
				}
				maps[pf - pfMin].put(tiersId, b.toString());
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
