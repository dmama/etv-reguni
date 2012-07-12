package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Le type d'affranchissement demandé par la poste pour envoyer un courrier à une adresse.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>tariffZoneType</i> (xml) / <i>TariffZone</i> (client java)
 */
@XmlType(name = "TypeAffranchissement")
@XmlEnum(String.class)
public enum TypeAffranchissement {
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>SWITZERLAND</i>.
	 */
	SUISSE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>EUROPE</i>.
	 */
	EUROPE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>OTHER_COUNTRIES</i>.
	 */
	MONDE;

	public static TypeAffranchissement fromValue(String v) {
		return valueOf(v);
	}
}