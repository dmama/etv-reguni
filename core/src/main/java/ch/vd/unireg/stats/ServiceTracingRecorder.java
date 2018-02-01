package ch.vd.unireg.stats;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

public interface ServiceTracingRecorder {

	/**
	 * Signale le début d'un appel d'une méthode. La valeur retournée doit être transmise à la méthode {@link #end(long)}.
	 *
	 * @return un timestamp à transmettre à la méthode end().
	 */
	long start();

	/**
	 * Signale la fin d'un appel d'une méthode
	 *
	 * @param start la valeur retournée par la méthode {@link #start()}.
	 */
	void end(long start);

	/**
	 * Signale la fin d'un appel d'une méthode nommée (le temps de réponse est loggué en niveau INFO)
	 *
	 * @param start la valeur retournée par la méthode {@link #start()}.
	 * @param name  le nom de la méthode
	 * @param params un constructeur de chaîne de caractères pour décrire les paramètres de la méthode
	 */
	void end(long start, String name, @Nullable Supplier<String> params);

	/**
	 * Signale la fin d'un appel d'une méthode nommée (le temps de réponse est loggué en niveau INFO),
	 * en ajoutant, le cas échéant, la classe de l'exception levée par l'appel
	 *
	 * @param start la valeur retournée par la méthode {@link #start()}.
	 * @param thrown l'exception éventuellement reçue dans l'appel
	 * @param name  le nom de la méthode
	 * @param params un constructeur de chaîne de caractères pour décrire les paramètres de la méthode
	 */
	void end(long start, @Nullable Throwable thrown, String name, @Nullable Supplier<String> params);

	/**
	 * Signale la fin d'un appel d'une méthode nommée (le temps de réponse est loggué en niveau INFO),
	 * en ajoutant, le cas échéant, la classe de l'exception levée par l'appel
	 *
	 * @param start la valeur retournée par la méthode {@link #start()}.
	 * @param thrown l'exception éventuellement reçue dans l'appel
	 * @param name  le nom de la méthode
	 * @param items le nombre d'éléments à prendre en compte dans l'appel de la méthode (sera utilisé pour calculer une moyenne du temps de réponse par élément).
	 * @param params un constructeur de chaîne de caractères pour décrire les paramètres de la méthode
	 */
	void end(long start, @Nullable Throwable thrown, String name, int items, @Nullable Supplier<String> params);
}
