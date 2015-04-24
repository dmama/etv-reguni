package ch.vd.uniregctb.migration.pm;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public interface MigrationResultProduction {

	/**
	 * Appelé lors de la migration dès qu'un message doit sortir dans une liste de contrôle
	 * @param cat la catégorie du message (= la liste de contrôle concernée)
	 * @param niveau le niveau du message
	 * @param msg le message
	 */
	void addMessage(MigrationResultMessage.CategorieListe cat, MigrationResultMessage.Niveau niveau, String msg);

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

	/**
	 * @param prefix le préfixe à rajouter
	 * @return une façade vers l'implémentation courante qui ajoute ce préfixe à tous les messages loggués
	 */
	@NotNull
	default MigrationResultProduction withMessagePrefix(String prefix) {

		// pas de préfixe, pas la peine de faire une nouvelle instance
		if (StringUtils.isBlank(prefix)) {
			return MigrationResultProduction.this;
		}

		// nouvelle instance en façade pour le préfixe non-vide
		return new MigrationResultProduction() {
			@Override
			public void addMessage(MigrationResultMessage.CategorieListe cat, MigrationResultMessage.Niveau niveau, String msg) {
				MigrationResultProduction.this.addMessage(cat, niveau, String.format("%s : %s", prefix, msg));
			}

			@Override
			public void addPostTransactionCallback(@NotNull Runnable callback) {
				MigrationResultProduction.this.addPostTransactionCallback(callback);
			}

			@Override
			public <D> void addPreTransactionCommitData(@NotNull D data) {
				MigrationResultProduction.this.addPreTransactionCommitData(data);
			}
		};
	}
}
