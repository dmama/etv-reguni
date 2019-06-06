package ch.vd.unireg.interfaces.infra;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;

public interface InfrastructureConnector {

	String SERVICE_NAME = "InfraConnector";

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
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Pays> getPays() throws InfrastructureException;

	/**
	 * @param numeroOFS un numéro Ofs de pays.
	 * @param date      la date de référence, ou <b>null</b> pour la date du jour
	 * @return le pays avec le numéro Ofs spécifié; ou <b>null</b> si aucun pays ne corresponds.
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Pays getPays(int numeroOFS, @Nullable RegDate date) throws InfrastructureException;

	/**
	 * @param numeroOFS un numéro Ofs de pays
	 * @return les version du pays ayant possédé ce numéro OFS
	 * @throws InfrastructureException
	 */
	List<Pays> getPaysHisto(int numeroOFS) throws InfrastructureException;

	/**
	 * Recherche un pays à partir de son code ('CH', 'FR', 'BE', ...). Voir la documentation de la méthode {@link ch.vd.infrastructure.model.Pays#getCodePays()}.
	 *
	 * @param codePays un code de pays ('CH', 'FR', 'BE', ...)
	 * @param date     la date de référence, ou <b>null</b> pour la date du jour
	 * @return le pays avec le code pays spécifié; ou <b>null</b> si aucun pays ne corresponds.
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws InfrastructureException;

	/**
	 * @param noColAdm le numéro technique de la collectivité
	 * @return la collectivite administrative.
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException;

	/**
	 * @return tous les cantons de la Suisse
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Canton> getAllCantons() throws InfrastructureException;

	/**
	 * @param canton un canton
	 * @return les communes du canton spécifié
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getListeCommunes(Canton canton) throws InfrastructureException;

	/**
	 * @return La liste des communes vaudoise (en incluant les fractions mais pas leur commune faîtière)
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getCommunesVD() throws InfrastructureException;

	/**
	 * @return La liste des communes vaudoise faîtières (excluant les fractions et les communes non-fractionnées)
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getListeCommunesFaitieres() throws InfrastructureException;

	/**
	 * Charge les communes
	 *
	 * @return toutes les communes de Suisse
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getCommunes() throws InfrastructureException;

	/**
	 * @return toutes les localités de Suisse
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Localite> getLocalites() throws InfrastructureException;

	/**
	 * @param onrp le numéro technique de la localité
	 * @return les localités qui ont correspondu au numéro technique spécifié au cours du temps (il ne doit pas y avoir de chevauchement des périodes de validité), triées par ordre chronologique
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Localite> getLocalitesByONRP(int onrp) throws InfrastructureException;

	/**
	 * @param onrp          le numéro technique de la localité
	 * @param dateReference on cherche une localité valide à la date donnée, ou en tout cas la plus proche
	 * @return la localité qui corresponds à numéro technique spécifié
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Localite getLocaliteByONRP(int onrp, RegDate dateReference) throws InfrastructureException;

	/**
	 * @param localite une localité
	 * @return les rues de la localité spécifiée
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Rue> getRues(Localite localite) throws InfrastructureException;

	/**
	 * @param numero le numéro technique d'une rue (= estrid)
	 * @return l'historique des rues qui ont porté ce numéro à travers les âges
	 * @throws InfrastructureException
	 */
	List<Rue> getRuesHisto(int numero) throws InfrastructureException;

	/**
	 * @param numero le numéro technique d'une rue (= estrid)
	 * @param date   la date de référence
	 * @return la rue qui correspond au numéro technique spécifié.
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Rue getRueByNumero(int numero, RegDate date) throws InfrastructureException;

	/**
	 * Retourne l'historique d'une commune à partir de son numéro OFS donné. Cette méthode permet de gérer les 28 exceptions où deux communes se partagent le même numéro Ofs.
	 *
	 * @param noOfsCommune numéro OFS de la commune (ou technique de la fraction de commune vaudoise)
	 * @return une liste avec 0, 1 ou 2 (cas exceptionnel) communes.
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws InfrastructureException;

	/**
	 * Retourne le numéro Ofs de la commune (non-faîtières) sur laquelle un bâtiment est construit.
	 *
	 * @param egid un numéro de bâtiment
	 * @param date la date à laquelle on se place pour faire la recherche (en cas de fusion de communes, un bâtiment peut être sur une commune un jour donné, et sur une autre le lendemain).
	 * @return le numéro Ofs de la commune, ou <code>null</code> si le bâtiment est inconnu.
	 * @throws InfrastructureException en cas de problème
	 */
	Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws InfrastructureException;

	/**
	 * @param localite une localité
	 * @return la commune correspondant à la localité EN GERANT LES FRACTIONS de commune
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Commune getCommuneByLocalite(Localite localite) throws InfrastructureException;

	/**
	 * Recherche une commune par nom officiel.
	 *
	 * @param nomOfficiel      le nom officiel de la commune
	 * @param includeFaitieres vrai s'il faut inclure les communes faîtières
	 * @param includeFractions vrai s'il faut inclure les fractions de commune
	 * @param date             la date de valeur de la commune (null = recherche dans tous l'historique)
	 * @return la commune trouvée ou <b>null</b> si aucune commune n'a été trouvée.
	 */
	@Nullable
	Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws InfrastructureException;

	/**
	 * @return tous les offices d'impôt *actifs* de district du canton de Vaud
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<OfficeImpot> getOfficesImpot() throws InfrastructureException;

	/**
	 * @return la liste des collectivites administratives *actives* du canton de Vaud
	 * @throws InfrastructureException en cas de problème
	 */
	List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException;

	/**
	 * @param typesCollectivite le type de collectivité administrative
	 * @return la liste des collectivites administratives *actives* du canton de Vaud du type spécifié
	 * @throws InfrastructureException en cas de problème
	 */
	List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws InfrastructureException;

	/**
	 * Permet de retourner une plusieurs localités à partir d'un npa
	 *
	 * @param npa           le npa
	 * @param dateReference date de référence
	 * @return les localités trouvées pour ce NPA
	 * @throws InfrastructureException en cas de problème
	 */
	List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws InfrastructureException;

	/**
	 * Fonctionalité de base de résolution d'une URL en provenance de FIDOR
	 * @param application application fiscale visée
	 * @param parametres paramètres de substitution (si <code>null</code>, aucune substitution n'est opérée, mais sinon, tous les paramètres sont substitués (au pire avec une chaîne vide))
	 * @return l'URL éventuellement résolue au niveau de ses paramètres
	 */
	String getUrl(ApplicationFiscale application, @Nullable Map<String, String> parametres);

	/**
	 * Retourne un logiciel déterminé par son id.
	 *
	 * @param id l'id d'un logiciel
	 * @return un logiciel; ou <b>null</b> si aucun logiciel ne possède l'id spécifié.
	 * @throws InfrastructureException en cas de problème
	 */
	Logiciel getLogiciel(Long id);

	/**
	 * @return la liste de tous les logiciels connus.
	 * @throws InfrastructureException en cas de problème
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
	 * @throws InfrastructureException en cas de problème
	 */
	List<TypeRegimeFiscal> getTousLesRegimesFiscaux();

	/**
	 * @return la liste des tous les genres d'impôt utilisables pour les mandats spéciaux
	 * @throws InfrastructureException en cas de problème
	 */
	List<GenreImpotMandataire> getTousLesGenresImpotMandataires();

	/**
	 * @param codeCollectivites liste des identifiants de collectivités.
	 * @param inactif           filtre sur les collectivités à remontés.
	 * @return la liste des collectivités correspondantes aux identofiants
	 */
	List<CollectiviteAdministrative> findCollectivitesAdministratives(@NotNull Collection<Integer> codeCollectivites, boolean inactif);

	/**
	 * Méthode qui permet de tester que le connecteur d'infrastructure répond bien. Cette méthode est insensible aux caches.
	 *
	 * @throws InfrastructureException en cas de non-fonctionnement du connecteur d'infrastructure
	 */
	void ping() throws InfrastructureException;
}
