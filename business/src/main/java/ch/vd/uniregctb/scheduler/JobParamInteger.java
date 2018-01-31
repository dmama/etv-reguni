package ch.vd.uniregctb.scheduler;

public class JobParamInteger extends JobParamNumber {

	public JobParamInteger() {
		super(Integer.class);
	}

	@Override
	public Object stringToValue(String s) throws IllegalArgumentException {
		return Integer.parseInt(s);
	}

}
