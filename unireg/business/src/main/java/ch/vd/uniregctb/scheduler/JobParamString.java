package ch.vd.uniregctb.scheduler;

public class JobParamString extends JobParamType {

	public JobParamString() {
		super(String.class);
	}

	@Override
	public Object stringToValue(String s) throws IllegalArgumentException {
		return s;
	}

	@Override
	public String valueToString(Object o) throws IllegalArgumentException {
		if (o == null) {
			return null;
		}
		if (!(o instanceof String)) {
			throw new IllegalArgumentException("L'objet n'est pas de classe String");
		}
		return (String)o;
	}

}
