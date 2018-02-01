package ch.vd.unireg.remarque;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.common.HtmlHelper;
import ch.vd.unireg.tiers.Remarque;

@SuppressWarnings("UnusedDeclaration")
public class RemarqueView implements Annulable {

	private static final Pattern EOL_PATTERN = Pattern.compile("\n|\r|\r\n");
	private static final Pattern HEAD_LINE_TRIMING_PATTERN = Pattern.compile("^\\s*");
	private static final Pattern TAIL_LINE_TRIMING_PATTERN = Pattern.compile("\\s*$");

	private final Long id;
	private final String date;
	private final String user;
	private final boolean annule;
	private final int nbLines;
	private final String htmlText;
	private final String shortHtmlText;

	public RemarqueView(Remarque remarque) {
		this.id = remarque.getId();
		this.date = DateHelper.dateTimeToDisplayString(remarque.getLogCreationDate());
		this.user = remarque.getLogCreationUser();
		this.annule = remarque.isAnnule();

		final String text = trimLines(remarque.getTexte());
		this.htmlText = HtmlHelper.renderMultilines(text);
		this.nbLines = countLines(text);
		this.shortHtmlText = HtmlHelper.renderMultilines(forgetExtraLines(text, getThresholdNbLines() - 1));
	}

	public Long getId() {
		return id;
	}

	public String getDate() {
		return date;
	}

	public String getUser() {
		return user;
	}

	public boolean isAnnule() {
		return annule;
	}

	public String getHtmlText() {
		return htmlText;
	}

	public String getShortHtmlText() {
		return shortHtmlText;
	}

	public int getNbLines() {
		return nbLines;
	}

	/**
	 * A partir de 6 lignes, on coupe !
	 * @return le nombre de lignes minimal à partir duquel on coupe l'affichage
	 */
	public int getThresholdNbLines() {
		return 6;
	}

	private static int countLines(String text) {
		final Matcher matcher = EOL_PATTERN.matcher(text);
		int nbLines = 1;
		while (matcher.find()) {
			++ nbLines;
		}
		return nbLines;
	}

	/**
	 * On enlève les lignes vides au début et à la fin de la chaîne de texte
	 * @param text le texte à épurer
	 * @return le texte épuré
	 */
	protected static String trimLines(String text) {
		if (text == null) {
			return StringUtils.EMPTY;
		}
		final String noHead = HEAD_LINE_TRIMING_PATTERN.matcher(text).replaceAll(StringUtils.EMPTY);
		return TAIL_LINE_TRIMING_PATTERN.matcher(noHead).replaceAll(StringUtils.EMPTY);
	}

	/**
	 * Ne conserve que les <i>nbLinesMax</i> première lignes du texte donné
	 * @param text texte source
	 * @param nbLinesMax nombre de lignes maximum à conserver
	 * @return Résultat de la troncature
	 */
	private static String forgetExtraLines(String text, int nbLinesMax) {
		final Pattern pattern = Pattern.compile("^(.*)$", Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(text);
		final StringBuilder b = new StringBuilder();
		int nbKeptLines = 0;
		while (matcher.find() && nbKeptLines < nbLinesMax) {
			if (b.length() > 0) {
				b.append(System.lineSeparator());
			}
			b.append(matcher.group());
			++ nbKeptLines;
		}
		return b.toString();
	}
}
