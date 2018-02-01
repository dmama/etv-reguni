package ch.vd.unireg.wsclient.bvrplus;

import org.apache.commons.lang3.StringUtils;

/**
 * Extrait de https://www.credit-suisse.com/media/production/pb/docs/unternehmen/kmugrossunternehmen/besr_technische_dokumentation_fr.pdf, page 7
 */
public abstract class CalculateurChiffreCle {

	private static final short[] BASE = {0, 9, 4, 6, 8, 2, 7, 1, 3, 5};

	private static short nextReport(short report, short nextNumber) {
		return BASE[(report + nextNumber) % BASE.length];
	}

	private static short key(short lastReport) {
		return (short) ((10 - lastReport) % 10);
	}

	public static short getKey(String input) {
		if (StringUtils.isBlank(input) || StringUtils.isNotBlank(input.replaceAll("[0-9]+", ""))) {
			throw new IllegalArgumentException(input);
		}
		short report = 0;
		for (int i = 0 ; i < input.length() ; ++ i) {
			final char c = input.charAt(i);
			final short n = (short) (c - '0');
			report = nextReport(report, n);
		}
		return key(report);
	}
}
