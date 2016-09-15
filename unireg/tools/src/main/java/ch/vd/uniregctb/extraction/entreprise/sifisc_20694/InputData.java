package ch.vd.uniregctb.extraction.entreprise.sifisc_20694;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class InputData {

	final long idEntreprise;
	final Long noCantonalEntreprise;
	final long idEtablissementPrincipal;
	final Long noCantonalEtablissementPrincipal;

	private static final Pattern PATTERN = Pattern.compile("^([0-9]+);([0-9]+)?;([0-9]+);([0-9]+)?$");

	private InputData(long idEntreprise, @Nullable Long noCantonalEntreprise, long idEtablissementPrincipal, @Nullable Long noCantonalEtablissementPrincipal) {
		this.idEntreprise = idEntreprise;
		this.noCantonalEntreprise = noCantonalEntreprise;
		this.idEtablissementPrincipal = idEtablissementPrincipal;
		this.noCantonalEtablissementPrincipal = noCantonalEtablissementPrincipal;
	}

	public static InputData of(String line) throws ParseException {
		final Matcher matcher = PATTERN.matcher(line);
		if (!matcher.matches()) {
			throw new ParseException(line, 0);
		}

		final long idEntreprise = Long.parseLong(matcher.group(1));
		final Long noCantonalEntreprise = StringUtils.isNotBlank(matcher.group(2)) ? Long.parseLong(matcher.group(2)) : null;
		final long idEtablissementPrincipal = Long.parseLong(matcher.group(3));
		final Long noCantonalEtablissementPrincipal = StringUtils.isNotBlank(matcher.group(4)) ? Long.parseLong(matcher.group(4)) : null;
		return new InputData(idEntreprise, noCantonalEntreprise, idEtablissementPrincipal, noCantonalEtablissementPrincipal);
	}
}
