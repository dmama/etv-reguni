package ch.vd.unireg.scheduler;

public abstract class JobParamNumber extends JobParamType {

	public JobParamNumber(Class<?> clazz) {
		super(clazz);
	}

	@Override
	public String valueToString(Object o) throws IllegalArgumentException {
		if (o == null) {
			return null;
		}
		if (!(o instanceof Number)) {
			throw new IllegalArgumentException("L'objet n'est pas de classe Number");
		}
		return o.toString();
	}

}
