package ch.vd.uniregctb.scheduler;

public class JobParamEnum extends JobParamType {

	public JobParamEnum(Class<? extends Enum<?>> clazz) {
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
