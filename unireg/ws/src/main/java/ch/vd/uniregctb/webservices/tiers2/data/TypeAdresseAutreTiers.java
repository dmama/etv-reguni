package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <b>Dans la version 3 du web-service :</b> <i>otherPartyAddressTypeType</i> (xml) / <i>OtherPartyAddressType</i> (client java)
 */
@XmlType(name = "TypeAdressePoursuiteAutreTiers")
@XmlEnum(String.class)
public enum TypeAdresseAutreTiers {

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>SPECIFIC</i>.
	 */
	SPECIFIQUE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>REPRESENTATIVE</i>.
	 */
	MANDATAIRE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>WELFARE_ADVOCATE</i>.
	 */
	CURATELLE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>LEGAL_ADVISER</i>.
	 */
	CONSEIL_LEGAL,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>GUARDIAN</i>.
	 */
	TUTELLE;

	public static TypeAdresseAutreTiers fromValue(String v) {
		return valueOf(v);
	}
}