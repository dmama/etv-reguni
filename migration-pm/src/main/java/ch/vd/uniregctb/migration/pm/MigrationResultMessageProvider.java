package ch.vd.uniregctb.migration.pm;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LoggedElement;

public interface MigrationResultMessageProvider {

	/**
	 * @param cat catégorie de messages
	 * @return tous les messages enregistrés pour cette catégorie
	 */
	@NotNull
	List<LoggedElement> getMessages(LogCategory cat);
}
