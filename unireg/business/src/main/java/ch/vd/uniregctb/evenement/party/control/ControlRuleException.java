package ch.vd.uniregctb.evenement.party.control;

public class ControlRuleException extends Exception{
	/**
	 *
	 */
	private static final long serialVersionUID = -4809515869211116599L;

	/**
	 * @param message
	 * @param t
	 */
	public ControlRuleException(String message, Throwable t) {
		super(message, t);
	}

	/**
	 *
	 * @param message
	 */
	public ControlRuleException(String message) {
		super(message);
	}

}
