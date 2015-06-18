package ch.vd.unireg.interfaces.organisation.rcent.converters;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Classe de base pour les fonctions de conversion tolérant les valeurs nulles.
 * @param <T> La valeur entrante
 * @param <R> La valeur sortante
 */
public abstract class BaseConverter<T, R> implements Function<T, R> {

	/**
	 * Applique la fonction de conversion, si la valeur n'est pas nulle.
	 * @param t
	 * @return La valeur transformée, ou null
	 */
	@Override
	@Nullable
	public final R apply(T t) {
		if (t == null) return null;
		return convert(t);
	}

	/**
	 * Méthode exécutant la conversion elle-même.
	 * @param t
	 * @return
	 */
	protected abstract R convert(@NotNull T t);
}
