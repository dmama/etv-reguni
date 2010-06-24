package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Le type d'affranchissement demandé par la poste pour envoyer un courrier à une adresse.
 */
@XmlType(name = "TypeAffranchissement")
@XmlEnum(String.class)
public enum TypeAffranchissement {
	SUISSE,
	EUROPE,
	MONDE;

	public static TypeAffranchissement fromValue(String v) {
		return valueOf(v);
	}
}