package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Classe de base pour les fonctions de conversion tolérant les valeurs nulles.
 * @param <T> La valeur entrante
 * @param <R> La valeur sortante
 */
public abstract class BaseConverter<T, R> implements Convert<T, R> {

	/**
	 * Applique la fonction de conversion. Si la valeur en entrée est null,
	 * la valeur en retour sera null.
	 * @param t La donnée à transformer
	 * @return La valeur transformée, ou null
	 */
	@Nullable
	public final R apply(T t) {
		if (t == null) return null;
		return convert(t);
	}

	/**
	 * Méthode exécutant la conversion elle-même.
	 * @param t La donnée à transformer
	 * @return La valeur transformée, ou null
	 */
	protected abstract R convert(@NotNull T t);
}
