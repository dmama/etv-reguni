package ch.vd.unireg.rattrapage.rcent.migration;

import java.text.ParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.type.DayMonthHelper;

/**
 * INFO;73391;Active;CHE468929885;101595912;;;;;;;;;;;;;Cycle de bouclements créé, applicable dès le 01.12.2016 : tous les 12 mois, à partir du premier 31.12.
 */
public class SuiviHandler implements CategoryHandler {

	/**
	 * Group(1) -> numéro d'entreprise
	 * Group(2) -> date JJ.MM.AAAA de début du cycle de bouclement
	 * Group(3) -> période en mois
	 * Group(4) -> ancrage (JJ.DD)
	 */
	private static final Pattern BOUCLEMENT_PATTERN = Pattern.compile("(?:INFO|WARN);([0-9]+);[A-Za-z]+;[A-Z0-9]*;[0-9]*;;;;;;;;;;;;;Cycle de bouclements créé, applicable dès le ([0-9]{2}\\.[0-9]{2}\\.[0-9]{4}) : tous les ([0-9]+) mois, à partir du premier ([0-9]{2}\\.[0-9]{2})\\.");

	@Override
	public void buildSql(StringBuilder buffer, List<String> input) throws ParseException {
		for (String line : input) {
			final Matcher matcher = BOUCLEMENT_PATTERN.matcher(line);
			if (matcher.matches()) {
				buffer.append("INSERT INTO BOUCLEMENT (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_DEBUT, ANCRAGE, PERIODE_MOIS, ENTREPRISE_ID)").append(System.lineSeparator());
				buffer.append("SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, '").append(Constants.VISA).append("', CURRENT_DATE, '").append(Constants.VISA).append("', ");
				buffer.append(RegDateHelper.displayStringToRegDate(matcher.group(2), false).index());
				buffer.append(", ");
				buffer.append(DayMonthHelper.fromDisplayString(matcher.group(4)).index());
				buffer.append(", ");
				buffer.append(matcher.group(3));
				buffer.append(", ");
				buffer.append(matcher.group(1));
				buffer.append(" FROM DUAL;").append(System.lineSeparator());
				buffer.append(System.lineSeparator());
			}
			else {
				throw new ParseException("Invalid line : " + line, 0);
			}
		}
	}
}
