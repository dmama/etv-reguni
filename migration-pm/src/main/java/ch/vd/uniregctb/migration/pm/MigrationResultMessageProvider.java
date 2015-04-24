package ch.vd.uniregctb.migration.pm;

import java.util.List;

import org.jetbrains.annotations.NotNull;

public interface MigrationResultMessageProvider {

	/**
	 * @param cat catégorie de messages
	 * @return tous les messages enregistrés pour cette catégorie
	 */
	@NotNull
	List<MigrationResultMessage> getMessages(MigrationResultMessage.CategorieListe cat);
}
