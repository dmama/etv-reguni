package ch.vd.unireg.avs;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.ws.search.party.v1.SearchResult;
import ch.vd.unireg.xml.party.v3.PartyInfo;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.utils.WebServiceV5Helper;

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

		// on lit le contenu du fichier, ligne par ligne
		final List<Pair<String, String>> data = new LinkedList<>();
		try (InputStream in = AvsToCtb.class.getResourceAsStream(nomFichier);
		     InputStreamReader isr = new InputStreamReader(in);
			 BufferedReader reader = new BufferedReader(isr)) {

			final Pattern pattern = Pattern.compile("((?:[0-9.])*)(?:[^0-9.].*)?");
			String line = reader.readLine();
			while (line != null) {
				final Matcher m = pattern.matcher(line);
				if (!m.matches()) {
					System.err.println("Line ignorée : '" + line + "'");
				}
				else {
					final String noAvs = FormatNumeroHelper.removeSpaceAndDash(m.group(1));
					data.add(Pair.of(StringUtils.trimToEmpty(noAvs), line));
				}
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
			final Set<String> partyTypes = new HashSet<>(Collections.singletonList("NATURAL_PERSON"));
			for (Pair<String, String> line : data) {
				try {
					if (StringUtils.isNotBlank(line.getLeft())) {
						final SearchResult result = WebServiceV5Helper.searchParty(urlWebService, userWebService, pwdWebService, userId, oid, null, null, line.getLeft(), null, partyTypes);
						if (result.getError() != null) {
							throw new Exception(result.getError().getErrorMessage());
						}
						final List<PartyInfo> items = result.getParty();
						dumpTiers(line, items, null, ps);
					}
					else {
						// rien trouvé car pas de numéro AVS en entrée
						dumpTiers(line, null, null, ps);
					}
				}
				catch (Exception e) {
					dumpTiers(line, null, e, ps);
				}
			}
		}
		finally {
			if (closeStream) {
				ps.close();
			}
		}
	}

	private static void dumpTiers(Pair<String, String> line, @Nullable List<PartyInfo> items, @Nullable Exception e, PrintStream ps) {
		final StringBuilder b = new StringBuilder(line.getRight());
		b.append(";");
		if (e != null) {
			b.append(e.getClass().getName());
			b.append(';');
			b.append(e.getMessage());
		}
		else if (items != null && !items.isEmpty()) {
			if (items.size() == 1) {
				b.append(items.get(0).getNumber());
			}
			else {
				boolean first = true;
				for (PartyInfo item : items) {
					if (!first) {
						b.append("/");
					}
					b.append(item.getNumber());
					first = false;
				}
			}
		}
		ps.println(b.toString());
	}
}
