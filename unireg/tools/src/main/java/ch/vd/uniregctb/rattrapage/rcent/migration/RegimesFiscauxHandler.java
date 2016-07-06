package ch.vd.uniregctb.rattrapage.rcent.migration;

import java.text.ParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lignes de la forme "INFO;55076;Active;CHE112228970;101464771;2004-07-01;CH;70;70;"
 */
public class RegimesFiscauxHandler implements CategoryHandler {

	/**
	 * Group(1) -> numéro d'entreprise
	 * Group(2) -> année de début
	 * Group(3) -> mois de début
	 * Group(4) -> jour de début
	 * Group(5) -> portée (CH/VD)
	 * Group(6) -> code du régime fiscal
	 */
	private static final Pattern LINE_PATTERN = Pattern.compile("[A-Z]+;([0-9]+);[A-Za-z]+;[A-Z0-9]*;[0-9]*;([0-9]{4})-([0-9]{2})-([0-9]{2});(CH|VD);[A-Za-z0-9]*;([A-Za-z0-9]+);");

	@Override
	public void buildSql(StringBuilder buffer, List<String> input) throws ParseException {
		for (String line : input) {
			final Matcher matcher = LINE_PATTERN.matcher(line);
			if (matcher.matches()) {
				buffer.append("INSERT INTO REGIME_FISCAL (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_DEBUT, ENTREPRISE_ID, PORTEE, CODE)").append(System.lineSeparator());
				buffer.append("SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, '").append(Constants.VISA).append("', CURRENT_DATE, '").append(Constants.VISA).append("', ");
				buffer.append(matcher.group(2)).append(matcher.group(3)).append(matcher.group(4));
				buffer.append(", ");
				buffer.append(matcher.group(1));
				buffer.append(", '");
				buffer.append(matcher.group(5));
				buffer.append("', '");
				buffer.append(matcher.group(6));
				buffer.append("' FROM DUAL;").append(System.lineSeparator());
				buffer.append(System.lineSeparator());
			}
			else {
				throw new ParseException("Invalid line : " + line, 0);
			}
		}
	}
}
