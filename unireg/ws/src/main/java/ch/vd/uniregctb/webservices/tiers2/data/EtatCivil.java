package ch.vd.uniregctb.webservices.tiers2.data;

/**
 * <b>Dans la version 3 du web-service :</b> <i>maritalStatusType</i> (xml) / <i>MaritalStatus</i> (client java)
 */
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;
@XmlType(name = "EtatCivil")
@XmlEnum(String.class)
public enum EtatCivil {

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>SINGLE</i>.
	 */
	CELIBATAIRE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>MARRIED</i>.
	 */
	MARIE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>WIDOWED</i>.
	 */
	VEUF,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>REGISTERED_PARTNER</i>.
	 */
	LIE_PARTENARIAT_ENREGISTRE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>NOT_MARRIED</i>.
	 */
	NON_MARIE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>PARTNERSHIP_ABOLISHED_BY_LAW</i>.
	 */
	PARTENARIAT_DISSOUS_JUDICIAIREMENT,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>DIVORCED</i>.
	 */
	DIVORCE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>SEPARATED</i>.
	 */
	SEPARE,
	/**
	 * <b>Dans la version 3 du web-service :</b> <i>PARTNERSHIP_ABOLISHED_BY_DEATH</i>.
	 */
	PARTENARIAT_DISSOUS_DECES;

	public static EtatCivil fromValue(String v) {
		return valueOf(v);
	}
}
