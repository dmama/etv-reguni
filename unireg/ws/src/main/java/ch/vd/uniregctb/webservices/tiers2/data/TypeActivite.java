package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "TypeActivite")
@XmlEnum(String.class)
public enum TypeActivite {

	PRINCIPALE,
	ACCESSOIRE,
	COMPLEMENTAIRE;

	public static TypeActivite fromValue(String v) {
		return valueOf(v);
	}
}
