package ch.vd.uniregctb.scheduler;

/**
 * Type de paramètre utilisé dans les job.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class JobParamType {

	private final Class<?> clazz;

	public JobParamType(Class<?> clazz) {
		this.clazz = clazz;
	}

	public Class<?> getConcreteClass() {
		return clazz;
	}

	/**
	 * Converti la string spécifiée en un objet du bon type.
	 *
	 * @throws IllegalArgumentException
	 *             en cas d'erreur de conversion
	 */
	public abstract Object stringToValue(String s) throws IllegalArgumentException;

	/**
	 * Converti l'objet spécifié en sa représentation string.
	 *
	 * @throws IllegalArgumentException
	 *             en cas d'erreur de conversion
	 */
	public abstract String valueToString(Object o) throws IllegalArgumentException;
}
