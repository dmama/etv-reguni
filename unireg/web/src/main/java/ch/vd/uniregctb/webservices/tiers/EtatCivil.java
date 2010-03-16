/**
 *
 */
package ch.vd.uniregctb.webservices.tiers;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;
@XmlType(name = "EtatCivil")
@XmlEnum(String.class)
public enum EtatCivil {

	CELIBATAIRE,
	MARIE,
	VEUF,
	LIE_PARTENARIAT_ENREGISTRE,
	NON_MARIE,
	PARTENARIAT_DISSOUS_JUDICIAIREMENT,
	DIVORCE,
	SEPARE,
	PARTENARIAT_DISSOUS_DECES;

	public static EtatCivil fromValue(String v) {
		return valueOf(v);
	}
}
