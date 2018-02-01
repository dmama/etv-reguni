package ch.vd.uniregctb.rattrapage.simpa.mandataires.perception;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.rattrapage.simpa.mandataires.TypeTiers;
import ch.vd.uniregctb.type.TypeMandat;

public class ExtractionSimpa {

	private final long noMandant;
	private final String nomMandant;
	private final long noMandataire;
	private final String nomMandataire;
	private final TypeTiers typeMandataire;
	private final TypeMandat typeMandat;
	private final RegDate dateAttribution;
	private final RegDate dateResiliation;
	private final String motifAttribution;      // ?
	private final String nomContact;
	private final String prenomContact;
	private final String telephoneContact;
	private final String faxContact;
	private final String ccp;
	private final String noCompteBancaire;
	private final String iban;
	private final String bicSwift;
	private final String institutionFinanciere;

	private static final Pattern PATTERN = Pattern.compile("(\\d+);([^;]*);(\\d+);([^;]*);(IND|ENT|ETA);([TG]);(\\d{1,2}[/.]\\d{1,2}[/.]\\d{4});((?:\\d{1,2}[/.]\\d{1,2}[/.]\\d{4})?);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*)(;.*)?");

	public static ExtractionSimpa of(String line) throws ParseException {
		final Matcher matcher = PATTERN.matcher(line);
		if (!matcher.matches()) {
			throw new ParseException(line, 0);
		}
		return new ExtractionSimpa(Long.parseLong(matcher.group(1)),
		                           matcher.group(2),
		                           Long.parseLong(matcher.group(3)),
		                           matcher.group(4),
		                           TypeTiers.fromTypeMandataire(matcher.group(5)),
		                           typeMandatFromString(matcher.group(6)),
		                           date(matcher.group(7)),
		                           date(matcher.group(8)),
		                           matcher.group(9),
		                           matcher.group(10),
		                           matcher.group(11),
		                           matcher.group(12),
		                           matcher.group(13),
		                           matcher.group(14),
		                           matcher.group(15),
		                           matcher.group(16),
		                           matcher.group(17),
		                           matcher.group(18));
	}

	private static TypeMandat typeMandatFromString(String str) throws ParseException {
		switch (str) {
		case "T":
			return TypeMandat.TIERS;
		case "G":
			return TypeMandat.GENERAL;
		default:
			throw new ParseException("Valeur non-supportée pour un type de mandat : '" + str + "'", 0);
		}
	}

	private static RegDate date(String str) throws ParseException {
		if (StringUtils.isBlank(str)) {
			return null;
		}
		final String[] parts = str.split("[/.]");
		return RegDate.get(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
	}

	private ExtractionSimpa(long noMandant, String nomMandant, long noMandataire, String nomMandataire, TypeTiers typeMandataire, TypeMandat typeMandat, RegDate dateAttribution, RegDate dateResiliation, String motifAttribution, String nomContact, String prenomContact,
	                        String telephoneContact, String faxContact, String ccp, String noCompteBancaire, String iban, String bicSwift, String institutionFinanciere) {
		this.noMandant = noMandant;
		this.nomMandant = nomMandant;
		this.noMandataire = noMandataire;
		this.nomMandataire = nomMandataire;
		this.typeMandataire = typeMandataire;
		this.typeMandat = typeMandat;
		this.dateAttribution = dateAttribution;
		this.dateResiliation = dateResiliation;
		this.motifAttribution = motifAttribution;
		this.nomContact = nomContact;
		this.prenomContact = prenomContact;
		this.telephoneContact = telephoneContact;
		this.faxContact = faxContact;
		this.ccp = ccp;
		this.noCompteBancaire = noCompteBancaire;
		this.iban = iban;
		this.bicSwift = bicSwift;
		this.institutionFinanciere = institutionFinanciere;
	}

	public long getNoMandant() {
		return noMandant;
	}

	public String getNomMandant() {
		return nomMandant;
	}

	public long getNoMandataire() {
		return noMandataire;
	}

	public String getNomMandataire() {
		return nomMandataire;
	}

	public TypeTiers getTypeMandataire() {
		return typeMandataire;
	}

	public TypeMandat getTypeMandat() {
		return typeMandat;
	}

	public RegDate getDateAttribution() {
		return dateAttribution;
	}

	public RegDate getDateResiliation() {
		return dateResiliation;
	}

	public String getMotifAttribution() {
		return motifAttribution;
	}

	public String getNomContact() {
		return nomContact;
	}

	public String getPrenomContact() {
		return prenomContact;
	}

	public String getTelephoneContact() {
		return telephoneContact;
	}

	public String getFaxContact() {
		return faxContact;
	}

	public String getCcp() {
		return ccp;
	}

	public String getNoCompteBancaire() {
		return noCompteBancaire;
	}

	public String getIban() {
		return iban;
	}

	public String getBicSwift() {
		return bicSwift;
	}

	public String getInstitutionFinanciere() {
		return institutionFinanciere;
	}
}
