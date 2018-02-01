package ch.vd.unireg.scheduler;

/**
 * Paramètre de batch qui supporte une sélection de 0 ou n valeurs d'une énumération.
 */
public class JobParamMultiSelectEnum extends JobParamType {

	public JobParamMultiSelectEnum(Class<? extends Enum<?>> clazz) {
		super(clazz);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object stringToValue(String s) throws IllegalArgumentException {
		return Enum.valueOf((Class<? extends Enum>) getConcreteClass(), s);
	}

	@Override
	public String valueToString(Object o) throws IllegalArgumentException {
		if (o == null) {
			return null;
		}
		if (!o.getClass().isEnum()) {
			throw new IllegalArgumentException("L'objet n'est pas de classe Enum");
		}
		return ((Enum<?>)o).name();
	}

}
