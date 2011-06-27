package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <b>Dans la version 3 du web-service :</b> <i>withholdingTaxTariffType</i> (xml) / <i>WithholdingTaxTariff</i> (client java)
 */
@XmlType(name = "TarifImpotSource")
@XmlEnum(String.class)
public enum TarifImpotSource {

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>NORMAL</i>.
	 */
	NORMAL,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>DOUBLE_REVENUE</i>.
	 */
	DOUBLE_GAIN;

	public static TarifImpotSource fromValue(String v) {
		return valueOf(v);
	}
}
