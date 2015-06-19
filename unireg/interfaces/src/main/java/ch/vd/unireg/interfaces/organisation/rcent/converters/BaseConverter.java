package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Classe de base pour l'implémentation des fonctions de conversion.
 *
 * Une valeur null en entrée est gérée par cette classe de base, et une valeur
 * null est retournée dans ce cas.
 *
 * La véritable conversion n'est appellée qu'en présence d'une valeur en entrée.
 *
 * Les classes concrètes dérivant cette classe doivent implémenter la conversion de
 * manière à ne jamais renvoyer null, mais lancer une exception si elles se trouvent
 * incapable de convertir la valeur reçue.
 *
 * @param <T> La valeur entrante
 * @param <R> La valeur sortante
 */
public abstract class BaseConverter<T, R> implements Converter<T, R> {

	/**
	 * Applique la fonction de conversion. Si la valeur en entrée est null,
	 * la valeur en retour sera null.
	 * @param t La donnée à transformer
	 * @return La valeur transformée, ou null
	 */
	@Nullable
	public final R apply(T t) {
		if (t == null) return null;
		R converted = convert(t);
		if (converted == null) { // Garde-fou pour éviter tout risque en cas de valeur non prise en charge. En effet, les annotations
								 // @NotNull n'empêchent pas de définir un convertisseur renvoyant null au lieu d'une exception. Dans ce
								 // cas, les tests seraient impuissant à faire ressortir le cas.
			throw new IllegalStateException("La conversion de la valeur [" + t.toString() + "] " +
					                                "a renvoyé une valeur nulle. Contrôler l'implémentation de convert().");
		}
		return converted;
	}

	/**
	 * Méthode exécutant la conversion elle-même.
	 *
	 * @param t La donnée à transformer. Ne peut être null.
	 * @return La valeur transformée
	 */
	@NotNull
	protected abstract R convert(@NotNull T t);
}
