package ch.vd.uniregctb.webservices.tiers2.stats;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.utils.Assert;

/**
 * Statistiques d'un appel au web-service.
 */
class Call {
	private String method;
	private long milliseconds;
	private int tiersCount;
	private HourMinutes timestamp;
	private List<String> parts;

	Call(String method, long milliseconds, int tiersCount, HourMinutes timestamp, List<String> parts) {
		this.method = method;
		this.milliseconds = milliseconds;
		this.tiersCount = tiersCount;
		this.timestamp = timestamp;
		this.parts = parts;
	}

	public String getMethod() {
		return method;
	}

	public long getMilliseconds() {
		return milliseconds;
	}

	public int getTiersCount() {
		return tiersCount;
	}

	public HourMinutes getTimestamp() {
		return timestamp;
	}

	public List<String> getParts() {
		return parts;
	}

	// exemple de ligne de log : [tiers2.read] INFO  [2010-11-11 10:48:38.464] [web-it] (15 ms) GetTiersHisto{login=UserLogin{userId='zsimsn', oid=22}, tiersNumber=10010169, parts=[ADRESSES]} charge=1
	public static Call parse(String line) throws java.text.ParseException {
		if (StringUtils.isBlank(line)) {
			return null;
		}

		// on saute le premier groupe []
		int next = line.indexOf(']');

		// on récupère le timestamp
		final String timestampAsString;
		{
			int left = line.indexOf('[', next + 1);
			int right = line.indexOf(']', next + 1);
			timestampAsString = line.substring(left + 12, right - 7);
			next = right;
		}

		// on saute le groupe [] suivant
		next = line.indexOf(']', next + 1);

		// on récupère les millisecondes
		final String milliAsString;
		{
			int left = line.indexOf('(', next + 1);
			int right = line.indexOf(')', next + 1);
			milliAsString = line.substring(left + 1, right - 3);
			next = right;
		}

		// on récupère le nom de la méthode
		final String method;
		{
			int left = line.indexOf(' ', next + 1);
			int right = line.indexOf('{', next + 1);
			method = line.substring(left + 1, right);
		}

		final int tiersCount = extractTiersCount(line, method);
		final HourMinutes timestamp = HourMinutes.parse(timestampAsString);
		final long milliseconds = Long.parseLong(milliAsString);
		final List<String> parts = extractParts(line);

		return new Call(method, milliseconds, tiersCount, timestamp, parts);
	}

	private static final Pattern TIERS_NUMBERS = Pattern.compile(".*tiersNumbers=\\[([0-9, ]*)\\].*");

	private static int extractTiersCount(String line, String method) {
		final int tiersCount;
		if (method.startsWith("GetBatch")) {
			// en cas de méthode batch, on calcul le temps moyen de réponse par tiers demandé
			final Matcher m = TIERS_NUMBERS.matcher(line);
			Assert.isTrue(m.matches());
			final String tiersNumer = m.group(1);
			tiersCount = StringUtils.countMatches(tiersNumer, ",") + 1;
		}
		else {
			tiersCount = 1;
		}
		return tiersCount;
	}

	// exemple de ligne de log : [tiers2.read] INFO  [2010-11-11 10:48:38.464] [web-it] (15 ms) GetTiersHisto{login=UserLogin{userId='zsimsn', oid=22}, tiersNumber=10010169, parts=[ADRESSES]} charge=1

	private static List<String> extractParts(String line) {
		List<String> parts = null;
		int start = line.indexOf("parts=[");
		if (start >= 0) {
			start += 7;
			int end = line.indexOf("]", start);
			String partsAsString = line.substring(start, end);
			parts = Arrays.asList(partsAsString.split(", "));
		}
		return parts;
	}
}
