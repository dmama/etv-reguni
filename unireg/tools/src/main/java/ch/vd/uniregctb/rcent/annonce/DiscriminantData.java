package ch.vd.uniregctb.rcent.annonce;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscriminantData implements WithEntrepriseId {

	private static final Pattern PATTERN = Pattern.compile("[^;]+;([0-9]+);(.*)");

	/**
	 * Numéro de la colonne (0-based) de la ligne qui contient le discriminant
	 */
	private static final int noColonneTrigger = 21;

	/**
	 * Pattern à faire passer sur la colonne du discriminant pour savoir si la ligne correspond à quelque chose qui doit être exporté
	 */
	private static final Pattern PATTERN_TRIGGER = Pattern.compile("OUI");

	private final long noEntreprise;
	private final String[] otherColumns;

	private DiscriminantData(long noEntreprise, String[] otherColumns) {
		this.noEntreprise = noEntreprise;
		this.otherColumns = otherColumns;
	}

	public static DiscriminantData valueOf(String line) throws UnreckognizedLineException {
		final Matcher matcher = PATTERN.matcher(line);
		if (!matcher.matches()) {
			throw new UnreckognizedLineException(line);
		}

		final long noEntreprise = Long.parseLong(matcher.group(1));
		final String[] otherColumns = matcher.group(2).split(";");
		return new DiscriminantData(noEntreprise, otherColumns);
	}

	public long getNoEntreprise() {
		return noEntreprise;
	}

	public boolean isExported() {
		return PATTERN_TRIGGER.matcher(otherColumns[noColonneTrigger - 1]).matches();
	}
}
