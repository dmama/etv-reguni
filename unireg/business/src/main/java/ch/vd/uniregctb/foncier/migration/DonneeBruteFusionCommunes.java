package ch.vd.uniregctb.foncier.migration;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class DonneeBruteFusionCommunes {

	private static final Pattern PARSING_PATTERN = Pattern.compile("(\\d{4});(\\d{4});(\\d{2}\\.\\d{2}\\.\\d{4});(\\d+)");

	public final int ofsNouvelleCommune;
	public final int ofsAncienneCommune;
	public final RegDate dateFusion;
	public final int offsetParcelle;

	private DonneeBruteFusionCommunes(int ofsNouvelleCommune, int ofsAncienneCommune, RegDate dateFusion, int offsetParcelle) {
		this.ofsNouvelleCommune = ofsNouvelleCommune;
		this.ofsAncienneCommune = ofsAncienneCommune;
		this.dateFusion = dateFusion;
		this.offsetParcelle = offsetParcelle;
	}

	public static DonneeBruteFusionCommunes valueOf(String csv) throws ParseException {
		final Matcher matcher = PARSING_PATTERN.matcher(csv);
		if (matcher.matches()) {
			final int ofsNouvelleCommune = Integer.valueOf(matcher.group(1));
			final int ofsAncienneCommune = Integer.valueOf(matcher.group(2));
			final String strDateFusion = matcher.group(3);
			final int offsetParcelle = Integer.valueOf(matcher.group(4));
			final RegDate dateFusion = RegDateHelper.displayStringToRegDate(strDateFusion, false);
			return new DonneeBruteFusionCommunes(ofsNouvelleCommune, ofsAncienneCommune, dateFusion, offsetParcelle);
		}
		throw new ParseException(csv, 0);
	}
}
