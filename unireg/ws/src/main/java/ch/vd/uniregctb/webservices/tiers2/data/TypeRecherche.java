package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <b>Dans la version 3 du web-service :</b> <i>searchModeType</i> (xml) / <i>SearchMode</i> (client java)
 */
@XmlType(name = "TypeRecherche")
@XmlEnum(String.class)
public enum TypeRecherche {

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>CONTAINS</i>.
	 */
    CONTIENT,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>PHONETIC</i>.
	 */
    PHONETIQUE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>IS_EXACTLY</i>.
	 */
    EST_EXACTEMENT;

    public String value() {
        return name();
    }

    public static TypeRecherche fromValue(String v) {
        return valueOf(v);
    }
}
