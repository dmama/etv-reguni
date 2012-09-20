package ch.vd.uniregctb.webservices.securite;

import javax.xml.bind.annotation.XmlElement;

/**
 * Paramètres de la méthode {@link SecuriteWebService#getDossiersControles(GetDossiersControles)}.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class GetDossiersControles {

	/** Le token d'autentification de l'applicaiton appelante */
	@XmlElement(required = true)
	public String authenticationToken;
}
