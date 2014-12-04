package ch.vd.uniregctb.avs;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.ws.search.party.v1.SearchResult;
import ch.vd.unireg.xml.party.v3.PartyInfo;
import ch.vd.uniregctb.utils.WebServiceV5Helper;

public class AvsToCtb {

	// INTEGRATION
//	private static final String urlWebService = "http://unireg-in.etat-de-vaud.ch/fiscalite/int-unireg/ws/v5";
//	private static final String userWebService = "unireg";
//	private static final String pwdWebService = "unireg_1014";

	// PRE-PRODUCTION
	private static final String urlWebService = "http://unireg-pp.etat-de-vaud.ch/fiscalite/unireg/ws/v5";
	private static final String userWebService = "web-it";
	private static final String pwdWebService = "unireg_1014";

	// PRODUCTION
//	private static final String urlWebService = "http://unireg-pr.etat-de-vaud.ch/fiscalite/unireg/ws/v5";
//	private static final String userWebService = "se renseigner...";
//	private static final String pwdWebService = "se renseigner...";

	private static final String userId = "usrfis06";
	private static final int oid = 22;

	private static final String nomFichier = "input-avs.csv";
	private static final String fichierDestination = "/tmp/tiers-from-avs.csv";

	public static void main(String[] args) throws Exception {

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
			final Set<String> partyTypes = new HashSet<>(Arrays.asList("NATURAL_PERSON"));
			for (String avs : avsList) {
				try {
					final SearchResult result = WebServiceV5Helper.searchParty(urlWebService, userWebService, pwdWebService, userId, oid, null, null, avs, null, partyTypes);
					if (result.getError() != null) {
						throw new Exception(result.getError().getErrorMessage());
					}
					final List<PartyInfo> items = result.getParty();
					dumpTiers(avs, items, null, ps);
				}
				catch (Exception e) {
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
}
