package ch.vd.uniregctb.migration.pm;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LoggedElement;
import ch.vd.uniregctb.migration.pm.log.LoggedElementRenderer;

public interface MigrationResultMessageProvider {

	/**
	 * @param cat catégorie de messages
	 * @return tous les messages enregistrés pour cette catégorie
	 */
	@NotNull
	List<LoggedElement> getMessages(LogCategory cat);

	/**
	 * @return une chaîne de caractères qui contient, par catégorie de log, les messages
	 */
	default String summary() {
		return Stream.of(LogCategory.values())
				.map(cat -> Pair.of(cat, getMessages(cat)))
				.filter(pair -> pair.getRight() != null && !pair.getRight().isEmpty())
				.map(pair -> Pair.of(pair.getLeft(), pair.getRight().stream().map(LoggedElementRenderer.INSTANCE::toString).collect(Collectors.joining("\n- ", "- ", StringUtils.EMPTY))))
				.map(pair -> String.format("Catégorie %s :\n%s", pair.getLeft(), pair.getRight()))
				.collect(Collectors.joining("\n"));
	}
}
