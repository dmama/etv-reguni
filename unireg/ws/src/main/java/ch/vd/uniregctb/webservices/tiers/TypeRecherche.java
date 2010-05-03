
package ch.vd.uniregctb.webservices.tiers;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


@XmlType(name = "TypeRecherche")
@XmlEnum(String.class)
public enum TypeRecherche {

    CONTIENT,
    PHONETIQUE,
    EST_EXACTEMENT;

    public String value() {
        return name();
    }

    public static TypeRecherche fromValue(String v) {
        return valueOf(v);
    }
}
