package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <b>Dans la version 3 du web-service :</b> <i>debtorCategoryType</i> (xml) / <i>DebtorCategory</i> (client java)
 */
@XmlType(name = "CategorieDebiteur")
@XmlEnum(String.class)
public enum CategorieDebiteur {
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>ADMINISTRATORS</i>.
	 */
	ADMINISTRATEURS,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>SPEAKERS_ARTISTS_SPORTSMEN</i>.
	 */
	CONFERENCIERS_ARTISTES_SPORTIFS,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>MORTGAGE_CREDITORS</i>.
	 */
	CREANCIERS_HYPOTHECAIRES,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>PENSION_FUND</i>.
	 */
	PRESTATIONS_PREVOYANCE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>REGULAR</i>.
	 */
	REGULIERS,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>LAW_ON_UNDECLARED_WORK</i>.
	 */
	LOI_TRAVAIL_AU_NOIR;

	public static CategorieDebiteur fromValue(String v) {
		return valueOf(v);
	}
}