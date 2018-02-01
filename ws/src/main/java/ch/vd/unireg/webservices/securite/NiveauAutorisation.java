package ch.vd.uniregctb.webservices.securite;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Les différents niveaux d'autorisation que peut obtenir un opérateur sur un certains dossier.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlType(name = "NiveauAutorisation")
@XmlEnum(String.class)
public enum NiveauAutorisation {
	/**
	 * Autorise l'accès en lecture sur un dossier. Aucune modification n'est autorisée.
	 */
	LECTURE,
	/**
	 * Autoriser l'accès total (lecture + écriture) sur un dossier.
	 */
	ECRITURE;

	public static NiveauAutorisation fromValue(String name) {
		return valueOf(name);
	}
}
