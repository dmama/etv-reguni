package ch.vd.uniregctb.webservices.tiers;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "TypeDocument")
@XmlEnum(String.class)
public enum TypeDocument {

	DECLARATION_IMPOT_COMPLETE,
	DECLARATION_IMPOT_VAUDTAX,
	DECLARATION_IMPOT_DEPENSE,
	DECLARATION_IMPOT_HC_IMMEUBLE;
	// LISTE_RECAPITULATIVE : pas nécessaire dans le web-service pour le moment, car TypeDocument est rattaché aux DIs uniquement.

	public static TypeDocument fromValue(String v) {
		return valueOf(v);
	}
}
