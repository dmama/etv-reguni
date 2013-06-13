package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <b>Dans la version 3 du web-service :</b> <i>documentTypeType</i> (xml) / <i>DocumentType</i> (client java)
 */
@XmlType(name = "TypeDocument")
@XmlEnum(String.class)
public enum TypeDocument {

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>FULL_TAX_DECLARATION</i>.
	 */
	DECLARATION_IMPOT_COMPLETE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>VAUDTAX_TAX_DECLARATION</i>.
	 */
	DECLARATION_IMPOT_VAUDTAX,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>EXPENDITURE_BASED_TAX_DECLARATION</i>.
	 */
	DECLARATION_IMPOT_DEPENSE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>IMMOVABLE_PROPERTY_OTHER_CANTON_TAX_DECLARATION</i>.
	 */
	DECLARATION_IMPOT_HC_IMMEUBLE;

	public static TypeDocument fromValue(String v) {
		return valueOf(v);
	}
}
