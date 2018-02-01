package ch.vd.unireg.avs;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.ws.parties.v1.Entry;
import ch.vd.unireg.ws.parties.v1.Parties;
import ch.vd.unireg.xml.party.person.v3.NaturalPerson;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.utils.WebServiceV5Helper;

public class CtbToAvs {

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

	private static final int TAILLE_LOT = 100;
	private static final String nomFichier = "input-ctb.csv";
	private static final String fichierDestination = "/tmp/tiers-avec-avs.csv";

	public static void main(String[] args) throws Exception {

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
				try {
					final Parties parties = WebServiceV5Helper.getParties(urlWebService, userWebService, pwdWebService, userId, oid, lot, null);
					for (Entry entry : parties.getEntries()) {
						final Party party = entry.getParty();
						dumpTiers(party, entry.getPartyNo(), entry.getError(), data.get(party.getNumber()), ps);
					}

				}
				catch (Exception e) {
					// problème... on essaie un par un
					for (Integer id : lot) {
						try {
							final Party indivResult = WebServiceV5Helper.getParty(urlWebService, userWebService, pwdWebService, userId, oid, id, null);
							dumpTiers(indivResult, id, null, data.get(id), ps);
						}
						catch (Exception e1) {
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
		if (tail == null) {
			tail = StringUtils.EMPTY;
		}
		if (party == null) {
			System.err.println(String.format("%d n'a pas été trouvé (%s)", tiersNumber, exceptionInfo));
			ps.println(String.format("%d%s;", tiersNumber, tail));
		}
		else if (party instanceof NaturalPerson) {
			final Long avs = ((NaturalPerson) party).getVn();
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
}
