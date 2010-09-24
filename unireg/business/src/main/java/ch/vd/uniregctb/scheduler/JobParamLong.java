package ch.vd.uniregctb.scheduler;

public class JobParamLong extends JobParamNumber {

	public JobParamLong() {
		super(Long.class);
	}

	@Override
	public Object stringToValue(String s) throws IllegalArgumentException {
		return Long.parseLong(s);
	}

}
