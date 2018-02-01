package ch.vd.unireg.rattrapage.simpa.mandataires;

import java.text.ParseException;

public enum TypeTiers {
	INDIVIDU,
	ENTREPRISE,
	ETABLISSEMENT;

	public static TypeTiers fromTypeMandataire(String str) throws ParseException {
		switch (str) {
		case "IND":
			return TypeTiers.INDIVIDU;
		case "ENT":
			return TypeTiers.ENTREPRISE;
		case "ETA":
			return TypeTiers.ETABLISSEMENT;
		default:
			throw new ParseException("Valeur non-supportée pour un type de mandataire : '" + str + "'", 0);
		}
	}
}
