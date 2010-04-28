package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ModeCommunication")
@XmlEnum(String.class)
public enum ModeCommunication {

	SITE_WEB,
	PAPIER,
	ELECTRONIQUE;

	public static ModeCommunication fromValue(String v) {
		return valueOf(v);
	}
}
