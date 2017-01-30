package ch.vd.uniregctb.foncier.migration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

/**
 * Classe utilitaire pour le parsing des fichiers d'export de SIMPA
 */
public abstract class ParsingHelper {

	private static final Pattern PERCENT_PATTERN = Pattern.compile("([0-9]{1,3})\\.?([0-9])?([0-9])?");
	private static final SimpleDateFormat DATE_FORMAT_SLASH = new SimpleDateFormat("dd/MM/yyyy");
	private static final SimpleDateFormat DATE_FORMAT_DOT = new SimpleDateFormat("dd.MM.yyyy");

	/**
	 * Converti un pourcent avec deux décimales en pour-dix-millièmes ("4.03" -> 403).
	 *
	 * @param token un pourcent sous forme de string
	 * @return le pour-dix-millième correspondant
	 */
	public static int parsePourdixmille(String token) throws ParseException {

		final Matcher matcher = PERCENT_PATTERN.matcher(token);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Le pourcent [" + token + "] est invalide");
		}

		final String units = matcher.group(1);
		final String dec = matcher.group(2);
		final String cent = matcher.group(3);

		int val = Integer.parseInt(units) * 100;
		if (dec != null) {
			val += Integer.parseInt(dec) * 10;
		}
		if (cent != null) {
			val += Integer.parseInt(cent);
		}

		return val;
	}

	/**
	 * Parse une date qui peut être au format dd/MM/yyyy ou dd.MM.yyyy
	 * @param token chaîne de caractères à parser
	 * @return une date, ou <code>null</code> si la chaîne de caractères est vide
	 * @throws ParseException en cas de format non-reconnu
	 */
	@Nullable
	public static RegDate parseDate(@Nullable String token) throws ParseException {
		if (StringUtils.isBlank(token)) {
			return null;
		}
		Date date;
		try {
			// la plupart des dates sont au format dd/MM/yyyy
			date = DATE_FORMAT_SLASH.parse(token);
		}
		catch (ParseException e) {
			// mais certaines sont au format dd.MM.yyyy
			date = DATE_FORMAT_DOT.parse(token);
		}
		final RegDate d = RegDateHelper.get(date);
		if (d == null) {
			throw new IllegalArgumentException("La date [" + token + "] n'est pas valide.");
		}
		return d;
	}
}
