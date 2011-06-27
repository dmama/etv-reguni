package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <b>Dans la version 3 du web-service :</b> <i>communicationModeType</i> (xml) / <i>CommunicationMode</i> (client java)
 */
@XmlType(name = "ModeCommunication")
@XmlEnum(String.class)
public enum ModeCommunication {

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>WEB_SITE</i>.
	 */
	SITE_WEB,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>PAPER</i>.
	 */
	PAPIER,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>UPLOAD</i>.
	 */
	ELECTRONIQUE;

	public static ModeCommunication fromValue(String v) {
		return valueOf(v);
	}
}
