package ch.vd.unireg.interfaces.infra;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;

public interface ServiceInfrastructureRaw {

	String SERVICE_NAME = "ServiceInfra";

	int noOIPM = 21;
	int noACI = 22;
	int noACIImpotSource = 47;
	int noCEDI = 1012;
	int noTuteurGeneral = 1013;
	int noCAT = 1341;
	int noRC = 999;

	int noOfsSuisse = 8100;
	int noPaysApatride = 8998;
	int noPaysInconnu = 8999;

	/**
	 * Constante sigle du canton de Vaud
	 */
	String SIGLE_CANTON_VD = "VD";

	/**
	 * Constante sigle du pays Suisse
	 */
	String SIGLE_SUISSE = "CH";

	/**
	 * @return la liste des pays.
	 * @throws ch.vd.unireg.interfaces.infra.ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Pays> getPays() throws ServiceInfrastructureException;

	/**
	 * @param numeroOFS un numéro Ofs de pays.
	 * @param date la date de référence, ou <b>null</b> pour la date du jour
	 * @return le pays avec le numéro Ofs spécifié; ou <b>null</b> si aucun pays ne corresponds.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Pays getPays(int numeroOFS, @Nullable RegDate date) throws ServiceInfrastructureException;

	/**
	 * @param numeroOFS un numéro Ofs de pays
	 * @return les version du pays ayant possédé ce numéro OFS
	 * @throws ServiceInfrastructureException
	 */
	List<Pays> getPaysHisto(int numeroOFS) throws ServiceInfrastructureException;

	/**
	 * Recherche un pays à partir de son code ('CH', 'FR', 'BE', ...). Voir la documentation de la méthode {@link ch.vd.infrastructure.model.Pays#getCodePays()}.
	 *
	 * @param codePays un code de pays ('CH', 'FR', 'BE', ...)
	 * @param date la date de référence, ou <b>null</b> pour la date du jour
	 * @return le pays avec le code pays spécifié; ou <b>null</b> si aucun pays ne corresponds.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws ServiceInfrastructureException;

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
	 * @return les localités qui ont correspondu au numéro technique spécifié au cours du temps (il ne doit pas y avoir de chevauchement des périodes de validité), triées par ordre chronologique
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Localite> getLocalitesByONRP(int onrp) throws ServiceInfrastructureException;

	/**
	 * @param localite une localité
	 * @return les rues de la localité spécifiée
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Rue> getRues(Localite localite) throws ServiceInfrastructureException;

	/**
	 *
	 * @param numero le numéro technique d'une rue (= estrid)
	 * @return l'historique des rues qui ont porté ce numéro à travers les âges
	 * @throws ServiceInfrastructureException
	 */
	List<Rue> getRuesHisto(int numero) throws ServiceInfrastructureException;

	/**
	 * @param numero le numéro technique d'une rue (= estrid)
	 * @param date la date de référence
	 * @return la rue qui correspond au numéro technique spécifié.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Rue getRueByNumero(int numero, RegDate date) throws ServiceInfrastructureException;

	/**
	 * Retourne l'historique d'une commune à partir de son numéro OFS donné. Cette méthode permet de gérer les 28 exceptions où deux communes se partagent le même numéro Ofs.
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
	List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws ServiceInfrastructureException;

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
	 * Permet de retourner une plusieurs localités à partir d'un npa
	 * @param npa le npa
	 * @param dateReference date de référence
	 * @return les localités trouvées pour ce NPA
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws ServiceInfrastructureException;

	/**
	 * Construit et retourne l'url vers la page de visualisation d'un tiers dans un application fiscale connectée à Unireg.
	 *
	 * @param application l'application considérée
	 * @param tiersId     le numéro de tiers
	 * @param oid         l'office d'impôt
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

	/**
	 * @param code un code de district
	 * @return un district ou <b>null</b> si le code passé ne correspond à aucun district.
	 */
	District getDistrict(int code);

	/**
	 * @param code un code de région
	 * @return une région ou <b>null</b> si le code passé ne correspond à aucune région.
	 */
	Region getRegion(int code);

	/**
	 * @return la liste des tous les régimes fiscaux (actifs ou non) connus.
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	List<TypeRegimeFiscal> getTousLesRegimesFiscaux();

	/**
	 * @return la liste des tous les genres d'impôt utilisables pour les mandats spéciaux
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	List<GenreImpotMandataire> getTousLesGenresImpotMandataires();

	/**
	 * Méthode qui permet de tester que le service infrastructure répond bien. Cette méthode est insensible aux caches.
	 *
	 * @throws ServiceInfrastructureException en cas de non-fonctionnement du service infrastructure
	 */
	void ping() throws ServiceInfrastructureException;
}
