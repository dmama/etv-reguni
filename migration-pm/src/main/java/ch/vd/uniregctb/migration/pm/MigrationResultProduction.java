package ch.vd.uniregctb.migration.pm;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;

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

	/**
	 * Récupère la donnée préalablement enregistrée lors d'un précédent appel pour la même classe et la même entité. Si aucune donnée
	 * n'a été préalablement enregistrée (= premier appel), alors l'extracteur correspondant (préalablement enregistré par un
	 * appel à {@link MigrationResultInitialization#registerDataExtractor(Class, Function, Function, Function)}) est sollicité
	 * @param clazz classe discriminante pour la donnée à extraire (une donnée par classe et entité)
	 * @param key clé de l'entité concernée par la donnée à extraire (une donnée par classe et entité)
	 * @param <T> le type de la donnée extraite
	 * @return la donnée extraite
	 */
	<T> T getExtractedData(Class<T> clazz, EntityKey key);
}
