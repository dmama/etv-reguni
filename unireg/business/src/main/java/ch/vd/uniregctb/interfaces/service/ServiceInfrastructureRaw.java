package ch.vd.uniregctb.interfaces.service;

import java.util.List;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.ApplicationFiscale;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.TypeEtatPM;
import ch.vd.uniregctb.interfaces.model.TypeRegimeFiscal;

public interface ServiceInfrastructureRaw {

	/**
	 * @return la liste des pays.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Pays> getPays() throws ServiceInfrastructureException;

	/**
	 * @param noColAdm le numéro technique de la collectivité
	 * @return la collectivite administrative.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException;

	/**
	 * @return tous les cantons de la Suisse
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Canton> getAllCantons() throws ServiceInfrastructureException;

	/**
	 * @param canton un canton
	 * @return les communes du canton spécifié
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException;

	/**
	 * @return La liste des communes vaudoise (en incluant les fractions mais pas leur commune faîtière)
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException;

	/**
	 * Charge les communes
	 *
	 * @return toutes les communes de Suisse
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getCommunes() throws ServiceInfrastructureException;

	/**
	 * @return toutes les localités de Suisse
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Localite> getLocalites() throws ServiceInfrastructureException;

	/**
	 * @param onrp le numéro technique de la localité
	 * @return la localité qui corresponds à numéro technique spécifié
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Localite getLocaliteByONRP(int onrp) throws ServiceInfrastructureException;

	/**
	 * @param localite une localité
	 * @return les rues de la localité spécifiée
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Rue> getRues(Localite localite) throws ServiceInfrastructureException;

	/**
	 * Renvoie les rues de ce canton
	 *
	 * @param canton un canton
	 * @return une liste de rues
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Rue> getRues(Canton canton) throws ServiceInfrastructureException;

	/**
	 * @param numero le numéro technique d'une rue
	 * @return la rue qui correspond au numéro technique spécifié.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Rue getRueByNumero(int numero) throws ServiceInfrastructureException;

	/**
	 * Retrouve la commune avec le numéro OFS étendu donné ; si plusieurs communes correspondent, renvoie celle qui est valide à la date donnée
	 *
	 * @param noCommune numéro OFS de la commune (ou technique de la fraction de commune vaudoise)
	 * @param date      date de référence (<code>null</code> pour la date du jour)
	 * @return Commune
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Commune getCommuneByNumeroOfsEtendu(int noCommune, RegDate date) throws ServiceInfrastructureException;

	/**
	 * Retourne le numéro Ofs de la commune sur laquelle un bâtiment est construit.
	 *
	 * @param egid             un numéro de bâtiment
	 * @param date             la date à laquelle on se place pour faire la recherche (en cas de fusion de communes, un bâtiment peut être sur une commune un jour donné, et sur une autre le lendemain).
	 * @param hintNoOfsCommune le numéro Ofs de la commune d'annonce qui sera utilisé comme indice pour accélérer la recherche (par commune d'annonce, il faut entendre la commune associée à l'adresse
	 *                         telle que retournée par le service civil. La signification métier de cette commune est encore sujette à interprétations)
	 * @return le numéro Ofs de la commune, ou <code>null</code> si le bâtiment est inconnu.
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	Integer getNoOfsCommuneByEgid(int egid, RegDate date, int hintNoOfsCommune) throws ServiceInfrastructureException;

	/**
	 * @param localite une localité
	 * @return la commune correspondant à la localité EN GERANT LES FRACTIONS de commune
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException;

	/**
	 * @param noColAdm le numéro de collectivité administrative de l'office d'impôt
	 * @return l'office d'impôt à partir de son numéro de collectivité.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	OfficeImpot getOfficeImpot(int noColAdm) throws ServiceInfrastructureException;

	/**
	 * @param noCommune le numéro Ofs d'un commune
	 * @return l'office d'impôt responsable de la commune spécifiée par son numéro OFS.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	OfficeImpot getOfficeImpotDeCommune(int noCommune) throws ServiceInfrastructureException;

	/**
	 * @return tous les offices d'impôt de district du canton de Vaud
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException;

	/**
	 * @return la liste des collectivites administratives du canton de Vaud
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException;

	/**
	 * @param typesCollectivite le type de collectivité administrative
	 * @return la liste des collectivites administratives du canton de Vaud du type spécifié
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite) throws ServiceInfrastructureException;

	/**
	 * Retourne l'institution financière spécifiée par son id technique.
	 *
	 * @param id l'id de l'institution financière
	 * @return une institution financière ou <code>null</code> si aucune institution ne correspond à l'id spécifié.
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException;

	/**
	 * La ou les institutions financière enregistrées sous le numéro de clearing spécifié.
	 * <p/>
	 * <b>Note:</b> logiquement, on ne devrait retourner qu'une institution financière pour un clearing donné, mais il se trouve que cette contrainte n'est pas respectée dans la base.
	 *
	 * @param noClearing un numéro de clearing
	 * @return 0, 1 ou plusieurs institutions financières.
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException;

	/**
	 * Permet de retourner une localite a partir d'un npa
	 *
	 * @param npa le npa
	 * @return la localite
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	Localite getLocaliteByNPA(int npa) throws ServiceInfrastructureException;

	/**
	 * @return la liste des types de régimes fiscaux qui existent pour les personnes morales.
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws ServiceInfrastructureException;

	/**
	 * @param code un code de régime fiscal
	 * @return le régime fiscal pour le code demandé; ou <null> si le code ne correspond à aucun régime fiscal connu,
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	TypeRegimeFiscal getTypeRegimeFiscal(String code) throws ServiceInfrastructureException;

	/**
	 * @return la liste des types d'états qui existent pour les personnes morales.
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	List<TypeEtatPM> getTypesEtatsPM() throws ServiceInfrastructureException;

	/**
	 * @param code un code de type d'état PM
	 * @return le type d'état PM pour le code demandé; ou <null> si le code ne correspond à aucun type d'état connu,
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	TypeEtatPM getTypeEtatPM(String code) throws ServiceInfrastructureException;

	/**
	 * Construit et retourne l'url vers la page de visualisation d'un tiers dans un application fiscale connectée à Unireg.
	 *
	 * @param application l'application considérée
	 * @param tiersId     le numéro de tiers
	 * @return une chaîne de caractère qui contient l'url demandée
	 */
	String getUrlVers(ApplicationFiscale application, Long tiersId);

	/**
	 * Retourne un logiciel déterminé par son id.
	 *
	 * @param id l'id d'un logiciel
	 * @return un logiciel; ou <b>null</b> si aucun logiciel ne possède l'id spécifié.
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	Logiciel getLogiciel(Long id);

	/**
	 * @return la liste de tous les logiciels connus.
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	List<Logiciel> getTousLesLogiciels();
}
