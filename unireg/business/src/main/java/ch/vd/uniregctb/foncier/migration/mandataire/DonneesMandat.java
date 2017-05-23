package ch.vd.uniregctb.foncier.migration.mandataire;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * La donnée extraite d'une ligne du fichier source
 */
public class DonneesMandat {

	private static final Pattern PATTERN = Pattern.compile("^(\\d{1,8});(?:[^;]*;)*(Oui|Non);([^;]*);([^;]*);([^;]*);([^;]*);([^;]*);(\\d{4});([^;]*);([^;]*)$");

	private final long noContribuable;
	private final boolean avecCourrier;
	private final String formulePolitesse;
	private final String nom1;
	private final String nom2;
	private final String attentionDe;
	private final String rue;
	private final int npa;
	private final String localite;
	private final String noTelephone;

	private final String ligneSource;

	private DonneesMandat(long noContribuable, boolean avecCourrier, String formulePolitesse, String nom1, String nom2, String attentionDe, String rue, Integer npa, String localite, String noTelephone, String ligneSource) {
		this.noContribuable = noContribuable;
		this.avecCourrier = avecCourrier;
		this.formulePolitesse = formulePolitesse;
		this.nom1 = nom1;
		this.nom2 = nom2;
		this.attentionDe = attentionDe;
		this.rue = rue;
		this.npa = npa;
		this.localite = localite;
		this.noTelephone = noTelephone;
		this.ligneSource = ligneSource;
	}

	@NotNull
	public static DonneesMandat valueOf(String line) throws ParseException {
		final Matcher matcher = PATTERN.matcher(line == null ? StringUtils.EMPTY : line);
		if (!matcher.matches()) {
			throw new ParseException(line, 0);
		}

		final long noContribuable = Long.parseLong(matcher.group(1));
		final boolean avecCourrier = "oui".equalsIgnoreCase(matcher.group(2));
		final String formulePolitesse = StringUtils.trimToNull(matcher.group(3));
		final String nom1 = StringUtils.trimToNull(matcher.group(4));
		final String nom2 = StringUtils.trimToNull(matcher.group(5));
		final String attentionDe = StringUtils.trimToNull(matcher.group(6));
		final String rue = StringUtils.trimToNull(matcher.group(7));
		final int npa = Integer.parseInt(matcher.group(8));
		final String localite = StringUtils.trimToEmpty(matcher.group(9));
		final String noTelephone = StringUtils.trimToEmpty(matcher.group(10));
		return new DonneesMandat(noContribuable, avecCourrier, formulePolitesse, nom1, nom2, attentionDe, rue, npa, localite, noTelephone, line);
	}

	public long getNoContribuable() {
		return noContribuable;
	}

	public boolean isAvecCourrier() {
		return avecCourrier;
	}

	public String getFormulePolitesse() {
		return formulePolitesse;
	}

	public String getNom1() {
		return nom1;
	}

	public String getNom2() {
		return nom2;
	}

	public String getAttentionDe() {
		return attentionDe;
	}

	public String getRue() {
		return rue;
	}

	public Integer getNpa() {
		return npa;
	}

	public String getLocalite() {
		return localite;
	}

	public String getNoTelephone() {
		return noTelephone;
	}

	public String getLigneSource() {
		return ligneSource;
	}
}
