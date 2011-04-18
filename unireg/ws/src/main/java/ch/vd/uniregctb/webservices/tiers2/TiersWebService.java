package ch.vd.uniregctb.webservices.tiers2;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.util.List;

import ch.vd.uniregctb.webservices.tiers2.data.BatchTiers;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.DebiteurInfo;
import ch.vd.uniregctb.webservices.tiers2.data.EvenementPM;
import ch.vd.uniregctb.webservices.tiers2.data.ReponseQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers2.data.Tiers;
import ch.vd.uniregctb.webservices.tiers2.data.TiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.TiersInfo;
import ch.vd.uniregctb.webservices.tiers2.exception.AccessDeniedException;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.exception.TechnicalException;
import ch.vd.uniregctb.webservices.tiers2.params.AllConcreteTiersClasses;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetDebiteurInfo;
import ch.vd.uniregctb.webservices.tiers2.params.GetListeCtbModifies;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersPeriode;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersType;
import ch.vd.uniregctb.webservices.tiers2.params.QuittancerDeclarations;
import ch.vd.uniregctb.webservices.tiers2.params.SearchEvenementsPM;
import ch.vd.uniregctb.webservices.tiers2.params.SearchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.SetTiersBlocRembAuto;

/**
 * Interface du web-service <i>tiers</i> <b>version 2</b> du registre fiscal Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"UnusedDeclaration"})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@WebService(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", name = "TiersPort", serviceName = "TiersService")
public interface TiersWebService {

	/**
	 * Recherche un ou plusieurs tiers en fonction de paramètres.
	 *
	 * @param params les paramètres de recherche.
	 * @return une liste contenant 0 ou n informations sur les tiers correspondants aux critères de recherche.
	 * @throws BusinessException     en cas d'erreur métier
	 * @throws AccessDeniedException en cas d'accès interdit à la ressource demandée
	 * @throws TechnicalException    en cas d'erreur inattendue (bug)
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public List<TiersInfo> searchTiers(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "SearchTiers") SearchTiers params)
			throws BusinessException, AccessDeniedException, TechnicalException;

	/**
	 * Retourne le type d'un tiers en fonction de son numéro de tiers.
	 *
	 * @param tiersNumber le numéro de tiers
	 * @return le type de tiers, ou null si le tiers n'existe pas
	 * @throws BusinessException     en cas d'erreur métier
	 * @throws AccessDeniedException en cas d'accès interdit à la ressource demandée
	 * @throws TechnicalException    en cas d'erreur inattendue (bug)
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public Tiers.Type getTiersType(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetTiersType") GetTiersType params)
			throws BusinessException, AccessDeniedException, TechnicalException;

	/**
	 * Retourne les informations du tiers correspondant au numéro de contribuable spécifié.
	 *
	 * @param tiersNumber le numéro de contribuable du tiers.
	 * @param date        la date de validité des informations à retourner, ou null pour obtenir les valeurs courantes.
	 * @param parts       la liste des parties à renseigner.
	 * @return les informations du tiers spécifiée, ou null si ce tiers n'existe pas.
	 * @throws BusinessException     en cas d'erreur métier
	 * @throws AccessDeniedException en cas d'accès interdit à la ressource demandée
	 * @throws TechnicalException    en cas d'erreur inattendue (bug)
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public Tiers getTiers(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetTiers") GetTiers params)
			throws BusinessException, AccessDeniedException, TechnicalException;

	/**
	 * Retourne les informations du tiers correspondant au numéro de contribuable spécifié pour la période spécifiée.
	 *
	 * @param tiersNumber le numéro de contribuable du tiers.
	 * @param periode     la période fiscale des informations à retourner, ou null pour obtenir la période courante.
	 * @param parts       la liste des parties à renseigner.
	 * @return les informations du tiers spécifiée, ou null si ce tiers n'existe pas.
	 * @throws BusinessException     en cas d'erreur métier
	 * @throws AccessDeniedException en cas d'accès interdit à la ressource demandée
	 * @throws TechnicalException    en cas d'erreur inattendue (bug)
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public TiersHisto getTiersPeriode(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetTiersPeriode") GetTiersPeriode params)
			throws BusinessException, AccessDeniedException, TechnicalException;

	/**
	 * Retourne l'historique des informations du tiers correspondant au numéro de contribuable spécifié.
	 *
	 * @param tiersNumber le numéro de contribuable du tiers.
	 * @param parts       la liste des parties à renseigner.
	 * @return les informations du tiers spécifié, ou null si le tiers n'existe pas.
	 * @throws BusinessException     en cas d'erreur métier
	 * @throws AccessDeniedException en cas d'accès interdit à la ressource demandée
	 * @throws TechnicalException    en cas d'erreur inattendue (bug)
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public TiersHisto getTiersHisto(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetTiersHisto") GetTiersHisto params)
			throws BusinessException, AccessDeniedException, TechnicalException;

	/**
	 * Retourne les tiers correspondant aux numéros de tiers spécifiés.
	 * <p/>
	 * <b>Attention !</b> Le nombre maximal d'ids supporté est de 500.
	 *
	 * @param tiersNumbers les numéros des tiers.
	 * @param date         la date de validité des informations à retourner, ou null pour obtenir les valeurs courantes.
	 * @param parts        la liste des parties à renseigner.
	 * @return les tiers indexés par leurs ids.
	 * @throws BusinessException     en cas d'erreur métier
	 * @throws AccessDeniedException en cas d'accès interdit à la ressource demandée
	 * @throws TechnicalException    en cas d'erreur inattendue (bug)
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public BatchTiers getBatchTiers(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetBatchTiers") GetBatchTiers params)
			throws BusinessException, AccessDeniedException, TechnicalException;

	/**
	 * Retourne les tiers historiques correspondant aux numéros de tiers spécifiés.
	 * <p/>
	 * <b>Attention !</b> Le nombre maximal d'ids supporté est de 500.
	 *
	 * @param tiersNumbers les numéros des tiers.
	 * @param parts        la liste des parties à renseigner.
	 * @return les tiers indexés par leurs ids.
	 * @throws BusinessException     en cas d'erreur métier
	 * @throws AccessDeniedException en cas d'accès interdit à la ressource demandée
	 * @throws TechnicalException    en cas d'erreur inattendue (bug)
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public BatchTiersHisto getBatchTiersHisto(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetBatchTiersHisto") GetBatchTiersHisto params)
			throws BusinessException, AccessDeniedException, TechnicalException;

	/**
	 * Change le code de blocage du remboursement automatique sur le tiers spécifié.
	 *
	 * @param params les paramètres permettant d'identifier l'utilisateur, le tiers et le code blocage.
	 * @throws BusinessException     en cas d'erreur métier
	 * @throws AccessDeniedException en cas d'accès interdit à la ressource demandée
	 * @throws TechnicalException    en cas d'erreur inattendue (bug)
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public void setTiersBlocRembAuto(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "SetTiersBlocRembAuto") SetTiersBlocRembAuto params)
			throws BusinessException, AccessDeniedException, TechnicalException;

	/**
	 * Recherche un ou plusieurs événements PM en fonction de certains critères.
	 *
	 * @param params les critères de sélection des événements
	 * @return une liste contenant 0 ou plusieurs événements
	 * @throws BusinessException     en cas d'erreur métier
	 * @throws AccessDeniedException en cas d'accès interdit à la ressource demandée
	 * @throws TechnicalException    en cas d'erreur inattendue (bug)
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public List<EvenementPM> searchEvenementsPM(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "SearchEvenementsPM") SearchEvenementsPM params)
			throws BusinessException, AccessDeniedException, TechnicalException;

	/**
	 * Calcule et retourne des informations métier supplémentaires (notamment sur les déclarations impôt source (LRs)) d'un débiteur de prestations imposables.
	 *
	 * @param params le numéro du débiteur et la période fiscale considérée
	 * @return le nombre de déclarations théoriques et réellement émises.
	 * @throws BusinessException     en cas d'erreur métier
	 * @throws AccessDeniedException en cas d'accès interdit à la ressource demandée
	 * @throws TechnicalException    en cas d'erreur inattendue (bug)
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public DebiteurInfo getDebiteurInfo(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetDebiteurInfo") GetDebiteurInfo params)
			throws BusinessException, AccessDeniedException, TechnicalException;

	/**
	 * Cette méthode permet de quittancer les déclarations d'impôt ordinaires aux dates spécifiées.
	 *
	 * @param params les demandes de quittancement
	 * @return le résultat détaillé du processing des quittancements
	 * @throws BusinessException     en cas d'erreur métier
	 * @throws AccessDeniedException en cas d'accès interdit à la ressource demandée
	 * @throws TechnicalException    en cas d'erreur inattendue (bug)
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public List<ReponseQuittancementDeclaration> quittancerDeclarations(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "QuittancerDeclarations") QuittancerDeclarations params) throws BusinessException,
			AccessDeniedException, TechnicalException;

	/**
	 * Cette méthode permet de retourner les numéros des contribuables modifiés entre un intervalle de temps passé en paramètre
	 *
	 * @param params les dates de début et de fin de recherche
	 * @return  la liste des ids des contribuables modifies
	 * @throws BusinessException en cas d'erreur métier
	 * @throws AccessDeniedException en cas d'accès interdit à la ressource demandée
	 * @throws TechnicalException en cas d'erreur inattendue (bug)
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public List<Long> getListeCtbModifies(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetListeCtbModifies") GetListeCtbModifies params) throws BusinessException,
			AccessDeniedException, TechnicalException;

	/**
	 * Cette méthode s'assure que les classes concrètes dérivant de Tiers sont exposées dans le WSDL. Elle ne fait rien proprement dit.
	 *
	 * @param dummy paramètre bidon.
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public void doNothing(AllConcreteTiersClasses dummy);


}
