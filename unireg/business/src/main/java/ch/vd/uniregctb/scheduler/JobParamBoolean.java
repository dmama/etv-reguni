package ch.vd.uniregctb.scheduler;

public class JobParamBoolean extends JobParamType {

	public JobParamBoolean() {
		super(Boolean.class);
	}

	@Override
	public Object stringToValue(String s) throws IllegalArgumentException {
		return Boolean.parseBoolean(s);
	}

	@Override
	public String valueToString(Object o) throws IllegalArgumentException {
		if (o == null) {
			return null;
		}
		if (!(o instanceof Boolean)) {
			throw new IllegalArgumentException("L'objet n'est pas de classe Boolean");
		}
		return ((Boolean)o).toString();
	}

}
