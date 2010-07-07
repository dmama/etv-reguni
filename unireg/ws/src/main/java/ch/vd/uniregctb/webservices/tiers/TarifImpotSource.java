package ch.vd.uniregctb.webservices.tiers;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "TarifImpotSource")
@XmlEnum(String.class)
public enum TarifImpotSource {

	NORMAL,
	DOUBLE_GAIN;

	public static TarifImpotSource fromValue(String v) {
		return valueOf(v);
	}
}
