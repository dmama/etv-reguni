package ch.vd.uniregctb.foncier.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identifie le numéro de parcelle (+ les indexes en cas de PPE) d'un immeuble issu de SIMPA-PM.
 */
public class MigrationParcelle {

	/**
	 * Les séparateurs peuvent être des tirets...
	 */
	private static final Pattern SEPARATORS = Pattern.compile("[-]");

	private static final List<Pair<BiFunction<Integer, String, Matcher>, Function<Matcher, Integer>>> MANIPULATORS = buildManipulators();

	private int noParcelle;
	@Nullable
	private Integer index1;
	@Nullable
	private Integer index2;
	@Nullable
	private Integer index3;

	private static List<Pair<BiFunction<Integer, String, Matcher>, Function<Matcher, Integer>>> buildManipulators() {
		final List<Pair<BiFunction<Integer, String, Matcher>, Function<Matcher, Integer>>> list = new ArrayList<>();

		//
		// Pour retrouver les cas "CFA" ou "CFAx" avec x un chiffre
		//
		{
			final Pattern pattern = Pattern.compile("CFA\\d?");
			list.add(Pair.of((index, string) -> index == 1 ? pattern.matcher(string) : null, matcher -> null));
		}

		//
		// on enlève les lettres finales (séparées ou non du numéro de parcelle par des espaces
		//
		{
			final Pattern pattern = Pattern.compile("(\\d+)\\s*[A-Z]+");
			list.add(Pair.of((index, string) -> pattern.matcher(string), matcher -> Integer.parseInt(matcher.group(1))));
		}

		//
		// extracteur par défaut
		//
		{
			final Pattern pattern = Pattern.compile(".*");      // tout !!
			list.add(Pair.of((index, string) -> pattern.matcher(string), matcher -> Integer.parseInt(matcher.group(0))));
		}

		return list;
	}

	public MigrationParcelle(@NotNull String baseParcelle, @Nullable String parcelle, @Nullable String lotPPE) {
		// [SIFISC-23111] nouvelle règle de transcription
		// - si le numéro de parcelle est vide, on prend le numéro de parcelle de base et on découpe pour extraire les indexes
		// - si le numéro de parcelle n'est pas vide, il est pris tel quel (sans indexes)
		// - le numéro de lot PPE n'est donc jamais utilisé !

		// [SIFISC-23187] les mentions CFA et CFA2 doivent être ignorées...

		if (StringUtils.isBlank(parcelle)) {
			// si le numéro de parcelle est renseigné, c'est toujours lui qui prime sur le numéro de base
			parcelle = baseParcelle;
		}

		final String[] tokens = SEPARATORS.split(parcelle);
		noParcelle = parseToken(0, tokens[0]);
		index1 = tokens.length > 1 ? parseToken(1, tokens[1]) : null;
		index2 = tokens.length > 2 ? parseToken(2, tokens[2]) : null;
		index3 = tokens.length > 3 ? parseToken(3, tokens[3]) : null;
	}

	@Nullable
	private static Integer parseToken(int index, String token) {
		return MANIPULATORS.stream()
				.map(pair -> Pair.of(pair.getLeft().apply(index, token), pair.getRight()))
				.filter(pair -> pair.getLeft() != null && pair.getLeft().matches())
				.findFirst()
				.map(pair -> pair.getRight().apply(pair.getLeft()))
				.orElse(null);
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
