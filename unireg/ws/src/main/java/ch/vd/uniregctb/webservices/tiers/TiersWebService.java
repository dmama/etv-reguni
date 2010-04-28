package ch.vd.uniregctb.webservices.tiers;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import ch.vd.uniregctb.webservices.common.WebServiceException;
import ch.vd.uniregctb.webservices.tiers.params.AllConcreteTiersClasses;
import ch.vd.uniregctb.webservices.tiers.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers.params.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers.params.GetTiersPeriode;
import ch.vd.uniregctb.webservices.tiers.params.GetTiersType;
import ch.vd.uniregctb.webservices.tiers.params.SearchTiers;
import ch.vd.uniregctb.webservices.tiers.params.SetTiersBlocRembAuto;

/**
 * Interface du web-service <i>tiers</i> <b>version 1</b> du registre fiscal Unireg.
 * <p>
 * <b>Note:</b> l'interface de ce web-service est freezée et ne doit plus être modifiée !
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@WebService(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers", name = "TiersPort", serviceName = "TiersService")
public interface TiersWebService {

	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers")
	public List<TiersInfo> searchTiers(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers", partName = "params", name = "SearchTiers") SearchTiers params)
			throws WebServiceException;

	/**
	 * Retourne le type d'un tiers en fonction de son numéro de tiers.
	 *
	 * @param tiersNumber
	 *            le numéro de tiers
	 * @return le type de tiers, ou null si le tiers n'existe pas
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers")
	public Tiers.Type getTiersType(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers", partName = "params", name = "GetTiersType") GetTiersType params)
			throws WebServiceException;

	/**
	 * Retourne les informations du tiers correspondant au numéro de contribuable spécifié.
	 *
	 * @param tiersNumber
	 *            le numéro de contribuable du tiers.
	 * @param date
	 *            la date de validité des informations à retourner, ou null pour obtenir les valeurs courantes.
	 * @param parts
	 *            la liste des parties à renseigner.
	 * @return les informations du tiers spécifiée, ou null si ce tiers n'existe pas.
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers")
	public Tiers getTiers(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers", partName = "params", name = "GetTiers") GetTiers params)
			throws WebServiceException;

	/**
	 * Retourne les informations du tiers correspondant au numéro de contribuable spécifié pour la période spécifiée.
	 *
	 * @param tiersNumber
	 *            le numéro de contribuable du tiers.
	 * @param periode
	 *            la période fiscale des informations à retourner, ou null pour obtenir la période courante.
	 * @param parts
	 *            la liste des parties à renseigner.
	 * @return les informations du tiers spécifiée, ou null si ce tiers n'existe pas.
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers")
	public TiersHisto getTiersPeriode(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers", partName = "params", name = "GetTiersPeriode") GetTiersPeriode params)
			throws WebServiceException;

	/**
	 * Retourne l'historique des informations du tiers correspondant au numéro de contribuable spécifié.
	 *
	 * @param tiersNumber
	 *            le numéro de contribuable du tiers.
	 * @param parts
	 *            la liste des parties à renseigner.
	 * @return les informations du tiers spécifié, ou null si le tiers n'existe pas.
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers")
	public TiersHisto getTiersHisto(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers", partName = "params", name = "GetTiersHisto") GetTiersHisto params)
			throws WebServiceException;

	/**
	 * Change le code de blocage du remboursement automatique sur le tiers spécifié.
	 *
	 * @param params
	 *            les paramètres permettant d'identifier l'utilisateur, le tiers et le code blocage.
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers")
	public void setTiersBlocRembAuto(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers", partName = "params", name = "SetTiersBlocRembAuto") SetTiersBlocRembAuto params)
			throws WebServiceException;

	/**
	 * Cette méthode s'assure que les classes concrètes dérivant de Tiers sont exposées dans le WSDL. Elle ne fait rien proprement dit.
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers")
	public void doNothing(AllConcreteTiersClasses dummy);
}
