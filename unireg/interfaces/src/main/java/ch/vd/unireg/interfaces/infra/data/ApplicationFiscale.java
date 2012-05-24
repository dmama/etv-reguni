package ch.vd.unireg.interfaces.infra.data;

/**
 * Les applications fiscales connues de Unireg.
 */
public enum ApplicationFiscale {
	TAO_PP("label.TAOPP"),
	TAO_BA("label.TAOBA"),
	TAO_IS("label.TAOIS"),
	SIPF("label.SIPF");

	private String messageKey;

	private ApplicationFiscale(String messageKey) {
		this.messageKey = messageKey;
	}

	public String getMessageKey() {
		return messageKey;
	}
}
