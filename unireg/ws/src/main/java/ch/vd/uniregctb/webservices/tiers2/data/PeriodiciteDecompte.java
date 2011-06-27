package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <b>Dans la version 3 du web-service :</b> <i>withholdingTaxDeclarationPeriodicityType</i> (xml) / <i>WithholdingTaxDeclarationPeriodicity</i> (client java)
 */
@XmlType(name = "PeriodiciteDecompte")
@XmlEnum(String.class)
public enum PeriodiciteDecompte {
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>MONTHLY</i>.
	 */
	MENSUEL,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>YEARLY</i>.
	 */
	ANNUEL,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>QUARTERLY</i>.
	 */
	TRIMESTRIEL,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>HALF_YEARLY</i>.
	 */
	SEMESTRIEL,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>ONCE</i>.
	 */
	UNIQUE;

	public static PeriodiciteDecompte fromValue(String v) {
		return valueOf(v);
	}
}