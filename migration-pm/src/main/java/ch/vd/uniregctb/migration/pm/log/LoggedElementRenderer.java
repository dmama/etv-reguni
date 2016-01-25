package ch.vd.uniregctb.migration.pm.log;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.StringRenderer;

public final class LoggedElementRenderer implements StringRenderer<LoggedElement> {

	public static final StringRenderer<LoggedElement> INSTANCE = new LoggedElementRenderer();

	private static final String SEPARATOR = ";";

	@Override
	public String toString(LoggedElement element) {
		// toutes les valeurs, dans l'ordre de la liste
		final Map<LoggedElementAttribute, Object> values = element.getItemValues();
		return element.getItems().stream()
				.map(item -> Pair.of(item, values.get(item)))
				.map(LoggedElementRenderer::render)
				.collect(Collectors.joining(SEPARATOR));
	}

	@NotNull
	private static <T> String render(@NotNull Pair<LoggedElementAttribute, T> entry) {
		if (entry.getValue() == null) {
			return StringUtils.EMPTY;
		}

		final StringRenderer<? super T> renderer = entry.getKey().getValueRenderer();
		return renderer.toString(entry.getValue()).replaceAll(Pattern.quote(SEPARATOR), ",");
	}

	/**
	 * @param columns une liste d'attributs (= les colonnes, en termes CSV)
	 * @return la représentation CSV de cette liste (utilisation du même séparateur que pour la liste des valeurs gérées par ce renderer)
	 */
	public static String renderColumns(List<LoggedElementAttribute> columns) {
		return columns.stream().map(LoggedElementAttribute::name).collect(Collectors.joining(SEPARATOR));
	}
}

