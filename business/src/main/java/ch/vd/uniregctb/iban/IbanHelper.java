package ch.vd.uniregctb.iban;

import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.common.FormatNumeroHelper;

public class IbanHelper {

	private static final IbanValidator VALIDATOR = new IbanValidator();

	public static String normalize(String iban) {
		if (iban == null) {
			return null;
		}
		return StringUtils.trimToNull(FormatNumeroHelper.removeSpaceAndDash(iban).toUpperCase());
	}

	public static String toDisplayString(String iban) {
		if (VALIDATOR.isValidIban(iban)) {
			final int length = iban.length();
			final StringBuilder b = new StringBuilder(length + length / 4 + 1);
			for (int i = 0 ; i < length; i += 4) {
				final int endIndex = Math.min(i + 4, length);
				b.append(iban.substring(i, endIndex));
				if (endIndex < length) {
					b.append(' ');
				}
			}
			return b.toString();
		}
		else {
			// on ne sait pas mieux faire pour un truc invalide...
			return iban;
		}
	}

	public static boolean areSame(String iban1, String iban2) {
		final String norm1 = normalize(iban1);
		final String norm2 = normalize(iban2);
		return (norm1 == null && norm2 == null) || (norm1 != null && norm1.equals(norm2));
	}
}
