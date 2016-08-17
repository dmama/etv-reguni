package ch.vd.uniregctb.nationalite;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.ws.parties.v1.Entry;
import ch.vd.unireg.ws.parties.v1.Parties;
import ch.vd.unireg.xml.party.person.v3.CommonHousehold;
import ch.vd.unireg.xml.party.person.v3.Nationality;
import ch.vd.unireg.xml.party.person.v3.NaturalPerson;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.utils.WebServiceV5Helper;

/**
 * Outil pour aller chercher les nationalités des tiers dont les numéros sont passés dans un fichier donné
 */
public class NationaliteExtractor {

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
	private static final String nomFichier = "tiers.csv";
	private static final String fichierDestination = "/tmp/tiers-avec-nationalités.csv";

	public static void main(String[] args) throws Exception {

		// on lit le contenu du fichier
		final List<Integer> ctbs = new ArrayList<>();
		try (InputStream in = NationaliteExtractor.class.getResourceAsStream(nomFichier);
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

		ps.println("NO_CTB;NO_CTB1;NATS_CTB1;NO_CTB2;NATS_CTB2");

		final Set<PartyPart> parts = EnumSet.of(PartyPart.HOUSEHOLD_MEMBERS);

		// et on boucle sur les lots
		try {
			for (List<Integer> lot : lots) {
				try {
					final Parties parties = WebServiceV5Helper.getParties(urlWebService, userWebService, pwdWebService, userId, oid, lot, parts);
					for (Entry entry : parties.getEntries()) {
						final Party party = entry.getParty();
						dumpTiers(party, entry.getPartyNo(), entry.getError(), ps);
					}
				}
				catch (Exception e) {
					// problème... on essaie un par un
					for (Integer id : lot) {
						try {
							final Party indivResult = WebServiceV5Helper.getParty(urlWebService, userWebService, pwdWebService, userId, oid, id, parts);
							dumpTiers(indivResult, id, null, ps);
						}
						catch (Exception e1) {
							e1.printStackTrace(System.err);
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

	private static int extractNoOfs(Nationality nationality) {
		if (nationality.getSwiss() != null) {
			return ServiceInfrastructureService.noOfsSuisse;
		}
		else if (nationality.getStateless() != null) {
			return ServiceInfrastructureService.noPaysApatride;
		}
		else if (nationality.getForeignCountry() != null) {
			return nationality.getForeignCountry();
		}
		else {
			throw new IllegalArgumentException("Quelle est cette nationalité sans aucune indication ??? : " + nationality);
		}
	}

	private static class InfoNationalite {
		final long noCtb;
		final Set<Integer> ofsPays = new TreeSet<>();

		InfoNationalite(long noCtb) {
			this.noCtb = noCtb;
		}

		@Override
		public String toString() {
			return String.format("%d;%s", noCtb, CollectionsUtils.toString(ofsPays, StringRenderer.DEFAULT, "-", StringUtils.EMPTY));
		}
	}

	private static InfoNationalite buildInfoNationalite(NaturalPerson np) {
		final InfoNationalite in = new InfoNationalite(np.getNumber());
		for (Nationality nationality : np.getNationalities()) {
			if (nationality.getDateTo() == null) {
				in.ofsPays.add(extractNoOfs(nationality));
			}
		}
		return in;
	}

	private static String toString(InfoNationalite infoNationalite) {
		if (infoNationalite == null) {
			return ";";
		}
		else {
			return infoNationalite.toString();
		}
	}

	private static void dumpTiers(@Nullable Party party, long tiersNumber, @Nullable Object exceptionInfo, PrintStream ps) {
		if (party == null) {
			System.err.println(String.format("%d n'a pas été trouvé (%s)", tiersNumber, exceptionInfo));
		}
		else {

			final InfoNationalite infoCtb1;
			final InfoNationalite infoCtb2;

			// il faut maintenant trouver la bonne nationalité (= celle qui n'a pas de date de fin...)
			if (party instanceof NaturalPerson) {
				infoCtb1 = buildInfoNationalite((NaturalPerson) party);
				infoCtb2 = null;
			}
			else if (party instanceof CommonHousehold) {
				final CommonHousehold mc = (CommonHousehold) party;
				infoCtb1 = mc.getMainTaxpayer() != null ? buildInfoNationalite(mc.getMainTaxpayer()) : null;
				infoCtb2 = mc.getSecondaryTaxpayer() != null ? buildInfoNationalite(mc.getSecondaryTaxpayer()) : null;
			}
			else {
				System.err.println(String.format("%d n'est ni une personne physique ni un ménage commun", tiersNumber));
				return;
			}

			ps.println(String.format("%d;%s;%s", tiersNumber, toString(infoCtb1), toString(infoCtb2)));
		}
	}
}
