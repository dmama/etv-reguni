/**
 *
 */
package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "PeriodiciteDecompte")
@XmlEnum(String.class)
public enum PeriodiciteDecompte {
	MENSUEL,
	ANNUEL,
	TRIMESTRIEL,
	SEMESTRIEL,
	UNIQUE;

	public static PeriodiciteDecompte fromValue(String v) {
		return valueOf(v);
	}
}