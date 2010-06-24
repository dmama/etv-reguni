package ch.vd.uniregctb.webservices.tiers;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "PeriodeDecompte")
@XmlEnum(String.class)
public enum PeriodeDecompte {
	M01,
	M02,
	M03,
	M04,
	M05,
	M06,
	M07,
	M08,
	M09,
	M10,
	M11,
	M12,
	T1,
	T2,
	T3,
	T4,
	S1,
	S2,
	A;
	
	public static PeriodeDecompte fromValue(String v) {
		return valueOf(v);
	}	
}
