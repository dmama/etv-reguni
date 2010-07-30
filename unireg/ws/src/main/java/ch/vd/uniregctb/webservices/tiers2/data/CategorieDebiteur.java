package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "CategorieDebiteur")
@XmlEnum(String.class)
public enum CategorieDebiteur {
	ADMINISTRATEURS,
	CONFERENCIERS_ARTISTES_SPORTIFS,
	CREANCIERS_HYPOTHECAIRES,
	PRESTATIONS_PREVOYANCE,
	REGULIERS,
	LOI_TRAVAIL_AU_NOIR;

	public static CategorieDebiteur fromValue(String v) {
		return valueOf(v);
	}
}