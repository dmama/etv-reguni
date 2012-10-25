package ch.vd.unireg.interfaces.infra;

import java.util.List;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeEtatPM;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;

public interface ServiceInfrastructureRaw {

	static final String SERVICE_NAME = "ServiceInfra";

	final static int noACI = 22;
	final static int noACIImpotSource = 47;
	final static int noACISuccessions = 1344;
	final static int noCEDI = 1012;
	final static int noTuteurGeneral = 1013;
	final static int noCAT = 1341;

	final static int noOfsSuisse = 8100;
	final static int noPaysApatride = 8998;
	final static int noPaysInconnu = 8999;

	/**
	 * Constante sigle du canton de Vaud
	 */
	final static String SIGLE_CANTON_VD = "VD";

	/**
	 * Constante sigle du pays Suisse
	 */
	final static String SIGLE_SUISSE = "CH";

	/**
	 * @return la liste des pays.
	 * @throws ch.vd.unireg.interfaces.infra.ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Pays> getPays() throws ServiceInfrastructureException;

	/**
	 * @param numeroOFS un numéro Ofs de pays.
	 * @return le pays avec le numéro Ofs spécifié; ou <b>null</b> si aucun pays ne corresponds.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Pays getPays(int numeroOFS) throws ServiceInfrastructureException;

	/**
	 * Recherche un pays à partir de son code ('CH', 'FR', 'BE', ...). Voir la documentation de la méthode {@link ch.vd.infrastructure.model.Pays#getCodePays()}.
	 *
	 * @param codePays un code de pays ('CH', 'FR', 'BE', ...)
	 * @return le pays avec le code pays spécifié; ou <b>null</b> si aucun pays ne corresponds.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Pays getPays(String codePays) throws ServiceInfrastructureException;

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
	 * Retourne l'historique d'une commune à partir de son numéro OFS étendu donné. Cette méthode permet de gérer les 28 exceptions où deux communes se partagent le même numéro Ofs.
	 *
	 * @param noOfsCommune numéro OFS de la commune (ou technique de la fraction de commune vaudoise)
	 * @return une liste avec 0, 1 ou 2 (cas exceptionnel) communes.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException;

	/**
	 * Retourne le numéro Ofs de la commune sur laquelle un bâtiment est construit.
	 *
	 * @param egid             un numéro de bâtiment
	 * @param date             la date à laquelle on se place pour faire la recherche (en cas de fusion de communes, un bâtiment peut être sur une commune un jour donné, et sur une autre le lendemain).
	 * @return le numéro Ofs de la commune, ou <code>null</code> si le bâtiment est inconnu.
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException;

	/**
	 * @param localite une localité
	 * @return la commune correspondant à la localité EN GERANT LES FRACTIONS de commune
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException;

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
	 * @param oid
	 * @return une chaîne de caractère qui contient l'url demandée
	 */
	String getUrlVers(ApplicationFiscale application, Long tiersId, Integer oid);

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
