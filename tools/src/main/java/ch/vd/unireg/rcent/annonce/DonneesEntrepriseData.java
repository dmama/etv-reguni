package ch.vd.unireg.rcent.annonce;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DonneesEntrepriseData implements WithEntrepriseId {

	private static final Pattern PATTERN = Pattern.compile("([0-9]+);([^;]+);([^;]+);([^;]+);([^;]*);([^;]*)");

	private final long noEntreprise;
	private final String raisonSociale;
	private final String formeJuridique;
	private final String siege;
	private final String cantonSiege;
	private final String paysSiege;

	private DonneesEntrepriseData(long noEntreprise, String raisonSociale, String formeJuridique, String siege, String cantonSiege, String paysSiege) {
		this.noEntreprise = noEntreprise;
		this.raisonSociale = raisonSociale;
		this.formeJuridique = formeJuridique;
		this.siege = siege;
		this.cantonSiege = cantonSiege;
		this.paysSiege = paysSiege;
	}

	public static DonneesEntrepriseData valueOf(String line) throws UnreckognizedLineException {
		final Matcher matcher = PATTERN.matcher(line);
		if (!matcher.matches()) {
			throw new UnreckognizedLineException(line);
		}

		final long noEntreprise = Long.parseLong(matcher.group(1));
		final String raisonSociale = matcher.group(2);
		final String formeJuridique = matcher.group(3);
		final String siege = matcher.group(4);
		final String cantonSiege = matcher.group(5);
		final String paysSiege = matcher.group(6);
		return new DonneesEntrepriseData(noEntreprise, raisonSociale, formeJuridique, siege, cantonSiege, paysSiege);
	}

	public long getNoEntreprise() {
		return noEntreprise;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public String getFormeJuridique() {
		return formeJuridique;
	}

	public String getSiege() {
		return siege;
	}

	public String getCantonSiege() {
		return cantonSiege;
	}

	public String getPaysSiege() {
		return paysSiege;
	}
}
