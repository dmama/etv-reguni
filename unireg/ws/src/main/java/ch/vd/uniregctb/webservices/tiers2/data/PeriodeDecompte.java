package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * <b>Dans la version 3 du web-service :</b> <i>withholdingTaxDeclarationPeriodType</i> (xml) / <i>WithholdingTaxDeclarationPeriod</i> (client java)
 */
@XmlType(name = "PeriodeDecompte")
@XmlEnum(String.class)
public enum PeriodeDecompte {
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>M01</i>.
	 */
	M01,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>M02</i>.
	 */
	M02,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>M03</i>.
	 */
	M03,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>M04</i>.
	 */
	M04,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>M05</i>.
	 */
	M05,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>M06</i>.
	 */
	M06,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>M07</i>.
	 */
	M07,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>M08</i>.
	 */
	M08,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>M09</i>.
	 */
	M09,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>M10</i>.
	 */
	M10,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>M11</i>.
	 */
	M11,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>M12</i>.
	 */
	M12,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>Q1</i>.
	 */
	T1,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>Q2</i>.
	 */
	T2,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>Q3</i>.
	 */
	T3,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>Q4</i>.
	 */
	T4,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>H1</i>.
	 */
	S1,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>H2</i>.
	 */
	S2,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>Y</i>.
	 */
	A;
	
	public static PeriodeDecompte fromValue(String v) {
		return valueOf(v);
	}	
}
