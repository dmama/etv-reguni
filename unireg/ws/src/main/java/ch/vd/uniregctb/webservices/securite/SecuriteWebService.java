package ch.vd.uniregctb.webservices.securite;

import java.util.Set;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import ch.vd.uniregctb.webservices.common.WebServiceException;

/**
 * Interface du web-service <i>sécurité</i> du registre fiscal Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@WebService(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security", name = "SecuritePort", serviceName = "SecuriteService")
public interface SecuriteWebService {

	/**
	 * Retourne le niveau d'autorisation que possède un opérateur sur un dossier d'un tiers particulier.
	 *
	 * @param login
	 *            le visa + le numéro de collectivité de l'opérateur
	 * @param numeroTiers
	 *            le numéro du tiers correspondant au dossier
	 * @return <code>null</code> si l'opérateur ne possède aucun droit, ni lecture ni écriture; <code>LECTURE</code> si l'opérateur possède
	 *         le droit de consultation uniquement; ou <code>ECRITURE</code> si l'opérateur possède le droit d'accè complet (lecture et
	 *         écriture).
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security")
	public NiveauAutorisation getAutorisationSurDossier(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security", partName = "params", name = "GetAutorisationSurDossier") GetAutorisationSurDossier params)
			throws WebServiceException;

	/**
	 * Retourne l'ensemble des ids des contribuables (= les dossiers) sur lesquelles des autorisations ou des restrictions sont appliquées
	 * actuellement.
	 * <p>
	 * <b>Note:</b> cette méthode ne doit être appelée que par Host-Interface. Il s'agit d'une méthode transitoire pour assurer
	 * l'iso-fonctionnalité du service de sécurité lors de la mise en production d'Unireg.
	 *
	 * @param authenticationToken
	 *            le token d'autentification de l'application appelante
	 * @return l'ensemble d'ids de contribuables
	 * @throws WebServiceException
	 *             si le token d'autentification n'est pas reconnu.
	 * @deprecated cette méthode ne doit pas être appelée par une autre application que Host-Interface.
	 */
	@Deprecated
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security")
	public Set<Long> getDossiersControles(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security", partName = "params", name = "GetDossiersControles") GetDossiersControles params)
			throws WebServiceException;
}
