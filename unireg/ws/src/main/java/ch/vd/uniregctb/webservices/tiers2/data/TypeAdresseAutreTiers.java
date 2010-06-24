package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "TypeAdressePoursuiteAutreTiers")
@XmlEnum(String.class)
public enum TypeAdresseAutreTiers {

	SPECIFIQUE,
	MANDATAIRE,
	CURATELLE,
	CONSEIL_LEGAL,
	TUTELLE;

	public static TypeAdresseAutreTiers fromValue(String v) {
		return valueOf(v);
	}
}