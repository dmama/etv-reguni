package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <b>Dans la version 3 du web-service :</b> <i>sexType</i> (xml) / <i>Sex</i> (client java)
 */
@XmlType(name = "Sexe")
@XmlEnum(String.class)
public enum Sexe {
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>2</i>.
	 */
	FEMININ,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>1</i>.
	 */
	MASCULIN;

	public static Sexe fromValue(String v) {
		return valueOf(v);
	}
}