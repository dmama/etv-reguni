/**
 *
 */
package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "Sexe")
@XmlEnum(String.class)
public enum Sexe {
	FEMININ,
	MASCULIN;

	public static Sexe fromValue(String v) {
		return valueOf(v);
	}
}