package ch.vd.unireg.interfaces.infra.data;

/**
 * Les applications fiscales connues de Unireg.
 */
public enum ApplicationFiscale {
	TAO_PP("label.TAOPP"),
	TAO_BA("label.TAOBA"),
	TAO_IS("label.TAOIS"),
	TAO_IS_DEBITEUR("label.TAOIS"),
	TAO_PM("label.TAOPM"),
	SIPF("label.SIPF"),
	DPERM("label.DPERM"),
	CAPITASTRA("label.CAPITASTRA");

	private String messageKey;

	ApplicationFiscale(String messageKey) {
		this.messageKey = messageKey;
	}

	public String getMessageKey() {
		return messageKey;
	}
}
