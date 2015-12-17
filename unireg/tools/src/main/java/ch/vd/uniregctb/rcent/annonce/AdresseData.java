package ch.vd.uniregctb.rcent.annonce;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdresseData implements WithEntrepriseId {

	private static final Pattern PATTERN = Pattern.compile("(\\d+);([^;]*);([^;]*);([^;]*)");

	private final long noEntreprise;
	private final String rue;
	private final String npa;
	private final String lieu;

	private AdresseData(long noEntreprise, String rue, String npa, String lieu) {
		this.noEntreprise = noEntreprise;
		this.rue = rue;
		this.npa = npa;
		this.lieu = lieu;
	}

	public static AdresseData valueOf(String line) throws UnreckognizedLineException {
		final Matcher matcher = PATTERN.matcher(line);
		if (!matcher.matches()) {
			throw new UnreckognizedLineException(line);
		}

		final long noEntreprise = Long.parseLong(matcher.group(1));
		final String rue = matcher.group(2);
		final String npa = matcher.group(3);
		final String lieu = matcher.group(4);
		return new AdresseData(noEntreprise, rue, npa, lieu);
	}

	public long getNoEntreprise() {
		return noEntreprise;
	}

	public String getRue() {
		return rue;
	}

	public String getNpa() {
		return npa;
	}

	public String getLieu() {
		return lieu;
	}
}
