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
	TAO_ICI_IFONC("label.TAOICIIFONC"),
	SIPF("label.SIPF"),
	DPERM("label.DPERM"),
	DPERM_DOCUMENT("label.DPERM"),
	CAPITASTRA("label.CAPITASTRA");

	private final String messageKey;

	ApplicationFiscale(String messageKey) {
		this.messageKey = messageKey;
	}

	public String getMessageKey() {
		return messageKey;
	}
}
