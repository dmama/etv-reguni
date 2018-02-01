package ch.vd.unireg.rattrapage.piunjour;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.ws.parties.v7.Entry;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.xml.common.v2.Cancelable;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.common.v2.DateHelper;
import ch.vd.unireg.xml.common.v2.DateRange;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclaration;
import ch.vd.unireg.xml.party.taxpayer.v5.Taxpayer;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxationPeriod;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.utils.WebServiceV7Helper;
import ch.vd.unireg.xml.DataHelper;

public class Job {

	// PRE-PRODUCTION
	private static final String urlWebService = "http://unireg-pp.etat-de-vaud.ch/fiscalite/unireg/ws/v7";
	private static final String userWebService = "web-it";
	private static final String pwdWebService = "unireg_1014";

	// PRODUCTION
//	private static final String urlWebService = "http://unireg-pr.etat-de-vaud.ch/fiscalite/unireg/ws/v7";
//	private static final String userWebService = "se renseigner...";
//	private static final String pwdWebService = "se renseigner...";

	private static final String userId = "usrreg06";
	private static final int oid = 22;

	private static final int TAILLE_LOT = 100;
	private static final String nomFichier = "input.csv";
	private static final String fichierDestination = "/tmp/correction-pi-un-jour-issues-migration-simpa.sql";

	private static final StringRenderer<Date> DATE_INDEX_RENDERER = d -> String.format("%04d%02d%02d", d.getYear(), d.getMonth(), d.getDay());
	private static final StringRenderer<Date> DATE_DISPLAY_RENDERER = d -> String.format("%02d.%02d.%04d", d.getDay(), d.getMonth(), d.getYear());
	private static final StringRenderer<RegDate> REGDATE_INDEX_RENDERER = RegDateHelper::toIndexString;
	private static final String SQL_USER = "[SQL-SIFISC-22041]";

	public static void main(String[] args) throws Exception {

		// on lit le contenu du fichier
		final List<Integer> ctbs = new ArrayList<>();
		try (InputStream in = Job.class.getResourceAsStream(nomFichier);
		     InputStreamReader fis = new InputStreamReader(in);
		     BufferedReader reader = new BufferedReader(fis)) {

			String line = reader.readLine();
			while (line != null) {
				if (!line.startsWith("#")) {
					final Integer ctb = Integer.valueOf(line);
					ctbs.add(ctb);
				}
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

		final Set<PartyPart> parts = EnumSet.of(PartyPart.CORPORATION_FLAGS,
		                                        PartyPart.TAX_LIGHTENINGS,
		                                        PartyPart.TAX_RESIDENCES,
		                                        PartyPart.TAX_SYSTEMS,
		                                        PartyPart.TAXATION_PERIODS,
		                                        PartyPart.TAX_DECLARATIONS);

		try {
			for (List<Integer> lot : lots) {
				try {
					final Parties parties = WebServiceV7Helper.getParties(urlWebService, userWebService, pwdWebService, userId, oid, lot, parts);
					for (Entry entry : parties.getEntries()) {
						doJob(entry.getParty(), entry.getPartyNo(), entry.getError(), ps);
					}
				}
				catch (Exception e) {
					// problème, il faut essayer un par un...
					for (Integer id : lot) {
						try {
							final Party indivResult = WebServiceV7Helper.getParty(urlWebService, userWebService, pwdWebService, userId, oid, id, parts);
							doJob(indivResult, id, null, ps);
						}
						catch (Exception e1) {
							doJob(null, id, e1.getMessage(), ps);
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

	private static void doJob(@Nullable Party party, int tiersNumber, @Nullable Object erreur, PrintStream ps) {
		if (party == null) {
			System.err.println(String.format("%d : tiers non-trouvé (%s)", tiersNumber, erreur));
		}
		else if (!(party instanceof Taxpayer)) {
			System.err.println(String.format("%d : pas un contribuable, il est donc exclu d'office (%s)", tiersNumber, party.getClass().getSimpleName()));
		}
		else {
			// il faut d'abord vérifier qu'il y a bien une période d'imposition d'un jour en premier
			final Taxpayer taxpayer = (Taxpayer) party;
			final Date datePeriodeUnJour = extractPeriodeImpositionUnJour(tiersNumber, taxpayer.getTaxationPeriods());
			if (datePeriodeUnJour != null) {

				// le premier for fiscal commence-t-il à cette date ?
				final Date dateDebutPremierForFiscal = extractDateDebutPremierForFiscal(taxpayer.getMainTaxResidences(), taxpayer.getOtherTaxResidences());
				if (dateDebutPremierForFiscal == null) {
					System.err.println(String.format("%d : pas de 'premier for fiscal' vaudois...", tiersNumber));
				}
				else {
					// y a-t-il une DI non-annulée sur cette période ?
					final List<TaxDeclaration> dis = getNonCanceledRangesAt(taxpayer.getTaxDeclarations(), dateDebutPremierForFiscal);
					if (!dis.isEmpty()) {
						System.err.println(String.format("%d : une déclaration non-annulée est présente sur la période d'imposition d'un jour...", tiersNumber));
					}
					else {
						final RegDate nouveauDebut = DataHelper.xmlToCore(dateDebutPremierForFiscal).getOneDayAfter();
						final String strAncienDebut = DATE_INDEX_RENDERER.toString(dateDebutPremierForFiscal);
						final String strNouveauDebut = REGDATE_INDEX_RENDERER.toString(nouveauDebut);

						// il faut faire quelque chose sur ce contribuable, notons-le...
						ps.println("--");
						ps.println("-- Contribuable " + FormatNumeroHelper.numeroCTBToDisplay((long) tiersNumber));
						ps.println("--");
						ps.println();

						// décalage de l'ouverture du for fiscal (ou annulation du for si celui-ci ne dure qu'un jour)
						ps.println("-- Annulation du for fiscal s'il ne dure qu'un jour");
						ps.println("UPDATE FOR_FISCAL SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='" + SQL_USER + "', ANNULATION_DATE=CURRENT_DATE, ANNULATION_USER='" + SQL_USER + "'");
						ps.println("WHERE ANNULATION_DATE IS NULL AND TIERS_ID=" + tiersNumber + " AND DATE_OUVERTURE=" + strAncienDebut + " AND DATE_FERMETURE=" + strAncienDebut + " AND TYPE_AUT_FISC='COMMUNE_OU_FRACTION_VD';");
						ps.println();

						ps.println("-- Décalage de l'ouverture du for fiscal");
						ps.println("UPDATE FOR_FISCAL SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='" + SQL_USER + "', DATE_OUVERTURE=" + strNouveauDebut);
						ps.println("WHERE ANNULATION_DATE IS NULL AND TIERS_ID=" + tiersNumber + " AND DATE_OUVERTURE=" + strAncienDebut + " AND TYPE_AUT_FISC='COMMUNE_OU_FRACTION_VD';");
						ps.println();

						// il faut changer la date de début du premier exercice commercial
						ps.println("-- Date de premier exercice commercial");
						ps.println("UPDATE TIERS SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='" + SQL_USER + "', DATE_DEBUT_PREMIER_EXERCICE=" + strNouveauDebut);
						ps.println("WHERE NUMERO=" + tiersNumber + " AND DATE_DEBUT_PREMIER_EXERCICE=" + strAncienDebut + ";");
						ps.println();

						// il faut changer la date des régimes fiscaux
						ps.println("-- Régimes fiscaux");
						ps.println("UPDATE REGIME_FISCAL SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='" + SQL_USER + "', DATE_DEBUT=" + strNouveauDebut);
						ps.println("WHERE ANNULATION_DATE IS NULL AND ENTREPRISE_ID=" + tiersNumber + " AND DATE_DEBUT=" + strAncienDebut + ";");
						ps.println();

						// il faut changer la date des allègements fiscaux
						ps.println("-- Allègements fiscaux");
						ps.println("UPDATE ALLEGEMENT_FISCAL SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='" + SQL_USER + "', DATE_DEBUT=" + strNouveauDebut);
						ps.println("WHERE ANNULATION_DATE IS NULL AND ENTREPRISE_ID=" + tiersNumber + " AND DATE_DEBUT=" + strAncienDebut + ";");
						ps.println();

						// il faut changer la date des spécificités fiscales
						ps.println("-- Spécificités fiscales");
						ps.println("UPDATE CORPORATION_FLAGS SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='" + SQL_USER + "', DATE_DEBUT=" + strNouveauDebut);
						ps.println("WHERE ANNULATION_DATE IS NULL AND ENTREPRISE_ID=" + tiersNumber + " AND DATE_DEBUT=" + strAncienDebut + ";");
						ps.println();
					}
				}
			}
		}
	}

	@Nullable
	private static Date extractPeriodeImpositionUnJour(int tiersNumber, List<TaxationPeriod> periodesImposition) {
		if (periodesImposition == null || periodesImposition.isEmpty()) {
			System.err.println(String.format("%d : aucune période d'imposition", tiersNumber));
			return null;
		}

		final TaxationPeriod pi = periodesImposition.get(0);
		if (pi == null) {
			System.err.println(String.format("%d : première période d'imposition nulle...", tiersNumber));
			return null;
		}
		if (!pi.getDateFrom().equals(pi.getDateTo())) {
			System.err.println(String.format("%d : la première période d'imposition ne dure pas un jour (%s -> %s)",
			                                 tiersNumber,
			                                 DATE_DISPLAY_RENDERER.toString(pi.getDateFrom()),
			                                 DATE_DISPLAY_RENDERER.toString(pi.getDateTo())));
			return null;
		}

		return pi.getDateFrom();
	}

	@Nullable
	private static Date extractDateDebutPremierForFiscal(List<TaxResidence> forsPrincipaux, List<TaxResidence> forsSecondaires) {
		return Stream.of(forsPrincipaux, forsSecondaires)
				.filter(Objects::nonNull)
				.filter(l -> !l.isEmpty())
				.map(l -> l.get(0))
				.filter(f -> f.getTaxationAuthorityType() == TaxationAuthorityType.VAUD_MUNICIPALITY)
				.map(TaxResidence::getDateFrom)
				.min(Comparator.naturalOrder())
				.orElse(null);
	}

	@NotNull
	private static <T extends DateRange & Cancelable> List<T> getNonCanceledRangesAt(List<T> source, Date date) {
		if (source == null) {
			return Collections.emptyList();
		}

		return source.stream()
				.filter(elt -> elt.getCancellationDate() == null)
				.filter(elt -> DateHelper.isDateInRange(elt, date))
				.collect(Collectors.toList());
	}
}
