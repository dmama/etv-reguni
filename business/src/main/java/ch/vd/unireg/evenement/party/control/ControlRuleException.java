package ch.vd.uniregctb.evenement.party.control;

public class ControlRuleException extends Exception {

	private static final long serialVersionUID = -720263702768091438L;

	public ControlRuleException(String message, Throwable t) {
		super(message, t);
	}

	public ControlRuleException(String message) {
		super(message);
	}
}
