package ch.vd.uniregctb.webservices.securite;

import javax.xml.bind.annotation.XmlElement;

import ch.vd.uniregctb.webservices.common.UserLogin;

/**
 * Paramètres de la méthode {@link SecuriteWebService#getAutorisationSurDossier(GetAutorisationSurDossier)}.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class GetAutorisationSurDossier {

	/** Les informations de login de l'utilisateur de l'application */
	@XmlElement(required = true)
	public UserLogin login;

	/**
	 * Le numéro de tiers sur le dossier duquel on veut connaître le niveau d'autorisation.
	 */
	@XmlElement(required = true)
	public long numeroTiers;
}
