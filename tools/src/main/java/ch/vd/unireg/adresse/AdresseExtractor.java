package ch.vd.unireg.adresse;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.ws.parties.v7.Entry;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.xml.party.address.v3.Address;
import ch.vd.unireg.xml.party.address.v3.FormattedAddress;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.unireg.utils.WebServiceV7Helper;

/**
 * Outil pour aller chercher les adresses d'envoi (sur six lignes, donc) des tiers
 * dont les numéros sont passés dans un fichier donné
 */
public class AdresseExtractor {

	// INTEGRATION
//	private static final String urlWebService = "http://unireg-in.etat-de-vaud.ch/fiscalite/int-unireg/ws/v7";
//	private static final String userWebService = "unireg";
//	private static final String pwdWebService = "unireg_1014";

	// PRE-PRODUCTION
	private static final String urlWebService = "http://unireg-pp.etat-de-vaud.ch/fiscalite/unireg/ws/v7";
	private static final String userWebService = "web-it";
	private static final String pwdWebService = "unireg_1014";

	// PRODUCTION
//	private static final String urlWebService = "http://unireg-pr.etat-de-vaud.ch/fiscalite/unireg/ws/v7";
//	private static final String userWebService = "se renseigner...";
//	private static final String pwdWebService = "se renseigner...";

	private static final String userId = "usrfis06";
	private static final int oid = 22;

	private static final int TAILLE_LOT = 100;
	private static final String nomFichier = "tiers.csv";
	private static final String fichierDestination = "/tmp/tiers-avec-adresse.csv";

	public static void main(String[] args) throws Exception {

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

		final Set<PartyPart> parts = EnumSet.of(PartyPart.ADDRESSES);

		// et on boucle sur les lots
		try {
			for (List<Integer> lot : lots) {
				try {
					final Parties parties = WebServiceV7Helper.getParties(urlWebService, userWebService, pwdWebService, userId, oid, lot, parts);
					for (Entry entry : parties.getEntries()) {
						final Party party = entry.getParty();
						dumpTiers(party, entry.getPartyNo(), entry.getError(), ps);
					}
				}
				catch (Exception e) {
					// problème... on essaie un par un
					for (Integer id : lot) {
						try {
							final Party indivResult = WebServiceV7Helper.getParty(urlWebService, userWebService, pwdWebService, userId, oid, id, parts);
							dumpTiers(indivResult, id, null, ps);
						}
						catch (Exception e1) {
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

			if (adrEnvoi == null || adrEnvoi.getPostAddress() == null) {
				System.err.println(String.format("%d n'a pas d'adresse d'envoi", tiersNumber));
			}
			else {
				final FormattedAddress adr = adrEnvoi.getPostAddress().getFormattedAddress();
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
}
