package ch.vd.uniregctb.migration.pm.log;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.MigrationResultMessageProvider;

/**
 * Un container des messages de log sous leur forme résolue (la résolution se fait dans le constructeur de la structure)
 */
public class LoggedMessages {

	private final Map<LogCategory, List<LoggedMessage>> map;

	private LoggedMessages(MigrationResultMessageProvider mrp) {
		this.map = Stream.of(LogCategory.values())
				.map(cat -> Pair.of(cat, mrp.getMessages(cat)))
				.filter(pair -> pair.getRight() != null && !pair.getRight().isEmpty())
				.map(pair -> Pair.of(pair.getLeft(), pair.getRight().stream().map(LoggedElement::resolve).collect(Collectors.toList())))
				.collect(Collectors.toMap(Pair::getLeft,
				                          Pair::getRight,
				                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()),
				                          () -> new EnumMap<>(LogCategory.class)));

	}

	private LoggedMessages(Map<LogCategory, List<LoggedMessage>> map) {
		this.map = map;
	}

	@NotNull
	public static LoggedMessages resolutionOf(MigrationResultMessageProvider mrp) {
		return new LoggedMessages(mrp);
	}

	@NotNull
	public static LoggedMessages singleton(LogCategory cat, LoggedMessage msg) {
		return new LoggedMessages(Collections.singletonMap(cat, Collections.singletonList(msg)));
	}

	/**
	 * @return les données sous leur forme résolue
	 */
	public Map<LogCategory, List<LoggedMessage>> asMap() {
		return map;
	}

	/**
	 * @return un petit résumé du contenu des messages
	 */
	@Override
	public String toString() {
		return map.entrySet().stream()
				.map(entry -> Pair.of(entry.getKey(), entry.getValue().stream().map(LoggedMessage::getMessage).collect(Collectors.joining("\n- ", "- ", StringUtils.EMPTY))))
				.map(pair -> String.format("Catégorie %s :\n%s", pair.getLeft(), pair.getRight()))
				.collect(Collectors.joining("\n"));
	}
}
