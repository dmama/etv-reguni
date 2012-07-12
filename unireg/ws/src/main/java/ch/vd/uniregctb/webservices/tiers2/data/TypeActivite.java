package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <b>Dans la version 3 du web-service :</b> <i>activityTypeType</i> (xml) / <i>ActivityType</i> (client java)
 */
@XmlType(name = "TypeActivite")
@XmlEnum(String.class)
public enum TypeActivite {

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>MAIN</i>.
	 */
	PRINCIPALE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>ACCESSORY</i>.
	 */
	ACCESSOIRE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>COMPLEMENTARY</i>.
	 */
	COMPLEMENTAIRE;

	public static TypeActivite fromValue(String v) {
		return valueOf(v);
	}
}
