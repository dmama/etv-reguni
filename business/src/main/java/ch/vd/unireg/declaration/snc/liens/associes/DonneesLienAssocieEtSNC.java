package ch.vd.unireg.declaration.snc.liens.associes;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;

/**
 * La donnée extraite d'une ligne du fichier source
 */
public class DonneesLienAssocieEtSNC {
	/**
	 * Hearder du fichier CSV
	 */
	public static final String SUJET = "Tiers sujet ID";
	public static final String OBJECT = "Tiers object ID";
	public static final List<String> HEADERS = Arrays.asList(SUJET, OBJECT);
	/**
	 * Date début du liens pour la relation initiale, cette date sera celle du for de gestion de la SNC
	 */
	static final String DATE_DEBUT_LIEN = "01.01.2018";
	private static final RegDateHelper.StringFormat format = RegDateHelper.StringFormat.DISPLAY;


	//Pattern de validation des lignes du csv
	private static final Pattern PATTERN_NUM_CTB = Pattern.compile("^(\\d{1,8})$");

	private final long noContribuableSNC;
	private final long noContribuableAssocie;
	private RegDate dateDebut;
	private final String ligneSource;

	public DonneesLienAssocieEtSNC(long noContribuableSNC, Long noContribuableAssocie, RegDate dateDebut, String ligneSource) {
		this.noContribuableSNC = noContribuableSNC;
		this.noContribuableAssocie = noContribuableAssocie;
		this.dateDebut = dateDebut;
		this.ligneSource = ligneSource;
	}


	@NotNull
	public static DonneesLienAssocieEtSNC valueOf(CSVRecord csvRecord) throws ParseException {

		final String sujet = csvRecord.get(SUJET);
		//on supprime les points du format d'affichage si présent.
		final Matcher matcherSujet = PATTERN_NUM_CTB.matcher(sujet == null ? StringUtils.EMPTY : sujet.replaceAll(Pattern.quote("."), ""));
		if (!matcherSujet.matches()) {
			throw new ParseException(sujet, 0);
		}

		final String object = csvRecord.get(OBJECT);
		final Matcher matcherObject = PATTERN_NUM_CTB.matcher(object == null ? StringUtils.EMPTY : object.replaceAll(Pattern.quote("."), ""));
		if (!matcherObject.matches()) {
			throw new ParseException(object, 1);
		}

		final long noContribuableSNC = Long.parseLong(matcherSujet.group(1));
		final long noContribuableAssocie = Long.parseLong(matcherObject.group(1));
		final RegDate dateDebut = format.fromString(DATE_DEBUT_LIEN, false);

		return new DonneesLienAssocieEtSNC(noContribuableSNC, noContribuableAssocie, dateDebut, DonneesLienAssocieEtSNC.parseToCsvString(csvRecord));
	}


	public long getNoContribuableSNC() {
		return noContribuableSNC;
	}

	public long getNoContribuableAssocie() {
		return noContribuableAssocie;
	}

	public String getLigneSource() {
		return ligneSource;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}


	public static String parseToCsvString(CSVRecord csvRecord) {
		return csvRecord.get(SUJET) + CsvHelper.COMMA + csvRecord.get(OBJECT) + CsvHelper.COMMA;
	}
}
