package ch.vd.uniregctb.migration.pm;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;

public interface MigrationResultProduction {

	/**
	 * Appelé lors de la migration dès qu'un message doit sortir dans une liste de contrôle
	 * @param cat la catégorie du message (= la liste de contrôle concernée)
	 * @param niveau le niveau du message
	 * @param msg le message
	 */
	void addMessage(LogCategory cat, LogLevel niveau, String msg);

	/**
	 * Enregistre un callback à appeler une fois la transaction courante committée
	 * @param callback appelé une fois la transaction committée (l'appel se fera hors de tout contexte transactionnel !)
	 */
	void addPostTransactionCallback(@NotNull Runnable callback);

	/**
	 * Enregistre une donnée qui sera intégrée aux autres et traitée en fin de transaction
	 * @param data donnée à intégrer
	 * @param <D> type de la donnée
	 */
	<D> void addPreTransactionCommitData(@NotNull D data);

}
