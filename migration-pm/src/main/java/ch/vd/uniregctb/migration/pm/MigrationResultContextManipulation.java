package ch.vd.uniregctb.migration.pm;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.log.LoggedElement;

/**
 * Spécialisation de l'interface de production des résultat autour de la manipulation
 * du contexte
 */
public interface MigrationResultContextManipulation extends MigrationResultProduction {

	/**
	 * Ajoute une valeur au contexte, en mémorisant la valeur précédente
	 * @param clazz classe du contexte
	 * @param value valeur à ajouter
	 * @param <E> type de la classe de contexte
	 */
	<E extends LoggedElement> void pushContextValue(Class<E> clazz, @NotNull E value);

	/**
	 * Ejecte la valeur précédemment associée au contexte pour la classe donnée (et revient à la valeur précédemment mémorisée)
	 * @param clazz classe de contexte
	 * @param <E> type de la classe de contexte
	 */
	<E extends LoggedElement> void popContexteValue(Class<E> clazz);

	/**
	 * @return le graphe en cours de migration
	 */
	Graphe getCurrentGraphe();

}
