package ch.vd.uniregctb.evenement.common;

/**
 * @author Raphaël Marmier, 2016-12-13, <raphael.marmier@vd.ch>
 */
public class AjaxResponseMessage {
	private boolean success;
	private String message;
	private Long id;

	public AjaxResponseMessage(boolean success, String message, Long id) {
		this.success = success;
		this.message = message;
		this.id = id;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}

	public Long getId() {
		return id;
	}
}
