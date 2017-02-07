package ch.vd.uniregctb.foncier.migration;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identifie le numéro de parcelle (+ les indexes en cas de PPE) d'un immeuble issu de SIMPA-PM.
 */
public class MigrationParcelle {

	/**
	 * Les séparateurs peuvent être des tirets ou des slashes...
	 */
	private static final Pattern SEPARATORS = Pattern.compile("[-]");

	/**
	 * Pour retrouver les cas "CFA" ou "CFAx" avec x un chiffre
	 */
	private static final Pattern CFA_PATTERN = Pattern.compile("CFA\\d?");

	/**
	 * Pour retrouver (et éliminer) les numéros de parcelle qui ressemblent à des dates
	 */
	private static final List<Pattern> DATE_PATTERNS = Arrays.asList(Pattern.compile("\\d{1,2}\\.(janv|févr|mars|avr|mai|juin|juil|août|sept|oct|nov|déc)"),
	                                                                 Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}"),
	                                                                 Pattern.compile("(janv|févr|mars|avr|mai|juin|juil|août|sept|oct|nov|déc)\\.\\d{2}"));

	private int noParcelle;
	@Nullable
	private Integer index1;
	@Nullable
	private Integer index2;
	@Nullable
	private Integer index3;

	public MigrationParcelle(@NotNull String baseParcelle, @Nullable String parcelle, @Nullable String lotPPE) {
		// [SIFISC-23111] nouvelle règle de transcription
		// - si le numéro de parcelle est vide, on prend le numéro de parcelle de base et on découpe pour extraire les indexes
		// - si le numéro de parcelle n'est pas vide, il est pris tel quel (sans indexes)
		// - le numéro de lot PPE n'est donc jamais utilisé !

		// [SIFISC-23187] les mentions CFA et CFA2 doivent être ignorées...

		if (StringUtils.isBlank(parcelle) || isPeutEtreDate(parcelle)) {
			// si le numéro de parcelle est renseigné, c'est toujours lui qui prime sur le numéro de base
			// [SIFISC-23189] sauf s'il ressemble à une date...
			parcelle = baseParcelle;
		}

		final String[] tokens = SEPARATORS.split(parcelle);
		noParcelle = Integer.parseInt(tokens[0]);
		index1 = tokens.length > 1 ? parseIndex1(tokens[1]) : null;
		index2 = tokens.length > 2 ? Integer.parseInt(tokens[2]) : null;
		index3 = tokens.length > 3 ? Integer.parseInt(tokens[3]) : null;
	}

	@Nullable
	private static Integer parseIndex1(String value) {
		final Matcher matcher = CFA_PATTERN.matcher(value);
		if (matcher.matches()) {
			return null;
		}
		return Integer.parseInt(value);
	}

	private static boolean isPeutEtreDate(String value) {
		return DATE_PATTERNS.stream()
				.map(pattern -> pattern.matcher(value))
				.anyMatch(Matcher::matches);
	}

	public int getNoParcelle() {
		return noParcelle;
	}

	@Nullable
	public Integer getIndex1() {
		return index1;
	}

	@Nullable
	public Integer getIndex2() {
		return index2;
	}

	@Nullable
	public Integer getIndex3() {
		return index3;
	}

	@Override
	public String toString() {
		return noParcelle + "/" + index1 + "/" + index2 + "/" + index3;
	}
}
