package ch.vd.uniregctb.migration.pm;

import org.jetbrains.annotations.NotNull;

public interface MigrationInitializationRegistrar {

	/**
	 * Enregistre un callback qui sera lancé (en mode synchrone) avant que la véritable migration puisse commencer
	 * @param callback callback à lancer avant la véritable phase de migration
	 */
	void registerInitializationCallback(@NotNull Runnable callback);
}
