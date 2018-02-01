package ch.vd.uniregctb.extraction.autrecommunaute;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.ws.parties.v1.Entry;
import ch.vd.unireg.ws.parties.v1.Parties;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.party.address.v2.Address;
import ch.vd.unireg.xml.party.address.v2.FormattedAddress;
import ch.vd.unireg.xml.party.othercomm.v1.OtherCommunity;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.utils.WebServiceV5Helper;

public class AutreCommunauteDataExtractor {

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

	private static final String COMMA = ";";
	private static final int TAILLE_LOT = 100;
	private static final String nomFichier = "autres-communautes.csv";
	private static final String fichierDestination = "/tmp/data.csv";

	public static void main(String[] args) throws Exception {

		// on lit le contenu du fichier
		final List<Integer> ctbs = new ArrayList<>();
		try (InputStream in = AutreCommunauteDataExtractor.class.getResourceAsStream(nomFichier);
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

		ps.println("NO_TIERS;FORME_JURIDIQUE;NOM;ANNULATION;ADR_LIGNE_1;ADR_LIGNE_2;ADR_LIGNE_3;ADR_LIGNE_4;ADR_LIGNE_5;ADR_LIGNE_6;FIN_ADRESSE;IDE");

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
		else if (!(party instanceof OtherCommunity)) {
			System.err.println(String.format("%d n'est pas un tiers de type 'AutreCommunaute'", tiersNumber));
		}
		else {
			// il faut maintenant exporter les données
			final OtherCommunity ac = (OtherCommunity) party;

			final Address adresseEnvoi = findAdresseEnvoi(ac);

			final StringBuilder b = new StringBuilder();
			b.append(tiersNumber).append(COMMA);
			b.append(ac.getLegalForm()).append(COMMA);
			b.append(ac.getName()).append(COMMA);
			b.append(ac.getCancellationDate() != null ? "O" : "N").append(COMMA);
			if (adresseEnvoi != null) {
				final FormattedAddress adr = adresseEnvoi.getFormattedAddress();
				b.append(StringUtils.trimToEmpty(adr.getLine1())).append(COMMA);
				b.append(StringUtils.trimToEmpty(adr.getLine2())).append(COMMA);
				b.append(StringUtils.trimToEmpty(adr.getLine3())).append(COMMA);
				b.append(StringUtils.trimToEmpty(adr.getLine4())).append(COMMA);
				b.append(StringUtils.trimToEmpty(adr.getLine5())).append(COMMA);
				b.append(StringUtils.trimToEmpty(adr.getLine6())).append(COMMA);

				final Date dateFin = adresseEnvoi.getDateTo();
				if (dateFin != null) {
					b.append(RegDate.get(dateFin.getYear(), dateFin.getMonth(), dateFin.getDay())).append(COMMA);
				}
				else {
					b.append(COMMA);
				}
			}
			else {
				b.append(COMMA);
				b.append(COMMA);
				b.append(COMMA);
				b.append(COMMA);
				b.append(COMMA);
				b.append(COMMA);
				b.append(COMMA);
			}

			// Numéros IDE
			b.append(CollectionsUtils.toString(findNumerosIDE(ac), StringRenderer.DEFAULT, ":"));

			ps.println(b.toString());
		}
	}

	private static Address findAdresseEnvoi(Party party) {
		// adresse courante
		for (Address adresse : party.getMailAddresses()) {
			if (adresse.getDateTo() == null) {
				return adresse;
			}
		}

		// si pas d'adresse courante, on prend la dernière adresse fermée
		Address chosen = null;
		for (Address candidate : party.getMailAddresses()) {
			if (chosen == null || candidate.getDateTo().compareTo(chosen.getDateTo()) > 0) {
				chosen = candidate;
			}
		}

		// finalement, c'est le mieux que l'on puisse faire
		return chosen;
	}

	private static List<String> findNumerosIDE(OtherCommunity ac) {
		final List<String> list = new ArrayList<>();
		if (ac.getUidNumbers() != null) {
			list.addAll(ac.getUidNumbers().getUidNumber());
		}
		Collections.sort(list);
		return list.isEmpty() ? Collections.emptyList() : list;
	}
}

