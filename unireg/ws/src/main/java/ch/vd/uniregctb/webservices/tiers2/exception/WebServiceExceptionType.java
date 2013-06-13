package ch.vd.uniregctb.webservices.tiers2.exception;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Représente le type précis d'une exception levée par le web-service tiers.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlType(name = "WebServiceExceptionType")
@XmlEnum(String.class)
public enum WebServiceExceptionType {

	/**
	 * Type d'exception levée par le web-service lorsqu'une erreur métier est levée.
	 * <p>
	 * Une erreur métier peut être le résultat de paramètres d'entrée incorrects, de données incohérentes dans la base, de problèmes de
	 * connectivités avec d'autres registres, etc...
	 */
	BUSINESS,
	/**
	 * Type d'exception levée par le web-service lorsque l'accès à une ressource n'est pas autorisé.
	 */
	ACCESS_DENIED,
	/**
	 * Type d'exception levée par le web-service lorsqu'une erreur interne au web-service est levée. Dans ce cas, il s'agit généralement
	 * d'un bug du web-service.
	 */
	TECHNICAL;

	public static WebServiceExceptionType fromValue(String v) {
		return valueOf(v);
	}
}
