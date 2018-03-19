package ch.vd.unireg.interfaces.service;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.LogicielMetier;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.adresse.AdresseGenerique;

public interface ServiceInfrastructureService {

	String SERVICE_NAME = ServiceInfrastructureRaw.SERVICE_NAME;

	int noOIPM = ServiceInfrastructureRaw.noOIPM;
	int noACI = ServiceInfrastructureRaw.noACI;
	int noACIImpotSource = ServiceInfrastructureRaw.noACIImpotSource;
	int noCEDI = ServiceInfrastructureRaw.noCEDI;
	int noTuteurGeneral = ServiceInfrastructureRaw.noTuteurGeneral;
	int noCAT = ServiceInfrastructureRaw.noCAT;
	int noRC = ServiceInfrastructureRaw.noRC;

	int noOfsSuisse = ServiceInfrastructureRaw.noOfsSuisse;
	int noPaysApatride = ServiceInfrastructureRaw.noPaysApatride;
	int noPaysInconnu = ServiceInfrastructureRaw.noPaysInconnu;

	/**
	 * Constante sigle du canton de Vaud
	 */
	String SIGLE_CANTON_VD = ServiceInfrastructureRaw.SIGLE_CANTON_VD;

	/**
	 * Constante sigle du pays Suisse
	 */
	String SIGLE_SUISSE = ServiceInfrastructureRaw.SIGLE_SUISSE;

	/**
	 * @return la liste des pays.
	 * @throws ch.vd.unireg.interfaces.infra.ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Pays> getPays() throws ServiceInfrastructureException;

	/**
	 * @param numeroOFS un numéro Ofs de pays
	 * @return les version du pays ayant possédé ce numéro OFS
	 * @throws ServiceInfrastructureException
	 */
	List<Pays> getPaysHisto(int numeroOFS) throws ServiceInfrastructureException;

	/**
	 * @param numeroOFS un numéro Ofs de pays.
	 * @param date      date de référence pour la validité du pays
	 * @return le pays avec le numéro Ofs spécifié; ou <b>null</b> si aucun pays ne corresponds.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Pays getPays(int numeroOFS, @Nullable RegDate date) throws ServiceInfrastructureException;

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
	 * @return La liste des communes vaudoise (en incluant les fractions mais pas leur commune faîtière)
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getCommunesVD() throws ServiceInfrastructureException;

	/**
	 * @return La liste des communes vaudoise faîtières (excluant les fractions et les communes non-fractionnées)
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getListeCommunesFaitieres() throws ServiceInfrastructureException;

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
	 * @param onrp          le numéro technique de la localité
	 * @param dateReference on cherche une localité valide à la date donnée, ou en tout cas la plus proche
	 * @return la localité qui corresponds à numéro technique spécifié
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Localite getLocaliteByONRP(int onrp, RegDate dateReference) throws ServiceInfrastructureException;

	/**
	 * @param localite une localité
	 * @return les rues de la localité spécifiée
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Rue> getRues(Localite localite) throws ServiceInfrastructureException;

	/**
	 * @param numero le numéro technique d'une rue
	 * @return la rue qui correspond au numéro technique spécifié.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Rue getRueByNumero(int numero) throws ServiceInfrastructureException;

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
	 * @param egid un numéro de bâtiment
	 * @param date la date à laquelle on se place pour faire la recherche (en cas de fusion de communes, un bâtiment peut être sur une commune un jour donné, et sur une autre le lendemain).
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
	 * Recherche une commune par nom officiel.
	 *
	 * @param nomOfficiel      le nom officiel de la commune
	 * @param includeFaitieres vrai s'il faut inclure les communes faîtières
	 * @param includeFractions vrai s'il faut inclure les fractions de commune
	 * @param date             la date de valeur de la commune (null = recherche dans tous l'historique)
	 * @return la commune trouvée ou <b>null</b> si aucune commune n'a été trouvée.
	 */
	@Nullable
	Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws ServiceInfrastructureException;

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
	List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws ServiceInfrastructureException;

	/**
	 * Permet de retourner une ou plusieurs localités à partir d'un npa
	 *
	 * @param npa           le npa
	 * @param dateReference la date de référence
	 * @return la liste des localités ciblées
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws ServiceInfrastructureException;

	/**
	 * Construit et retourne l'url vers la page de visualisation d'un tiers dans un application fiscale connectée à Unireg.
	 *
	 * @param application l'application considérée
	 * @param tiersId     le numéro de tiers
	 * @return une chaîne de caractère qui contient l'url demandée
	 */
	String getUrlInteroperabilite(ApplicationFiscale application, Long tiersId);

	/**
	 * Construit et retourne l'URL d'accès à la page de visualisation du document dans RepElec
	 * @param tiersId   le numéro du tiers
	 * @param pf        la période fiscale du document, si applicable
	 * @param cleDocument la clé d'indexation du document dans RepElec
	 * @return une chaîne de caractères qui contient l'URL demandée
	 */
	String getUrlVisualisationDocument(Long tiersId, @Nullable Integer pf, String cleDocument);

	/**
	 * Retourne l'URL brutte (= sans résolution de paramètre) de l'application fiscale
	 * @param application application concernée
	 * @return l'URL brutte correspondante
	 */
	String getUrlBrutte(ApplicationFiscale application);

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
	 * Recherche un pays à partir de son code ('CH', 'FR', 'BE', ...). Voir la documentation de la méthode {@link ch.vd.infrastructure.model.Pays#getCodePays()}.
	 *
	 * @param codePays un code de pays ('CH', 'FR', 'BE', ...)
	 * @param date     une date de référence pour la validité du pays
	 * @return le pays avec le code pays spécifié; ou <b>null</b> si aucun pays ne corresponds.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Pays getPays(String codePays, @Nullable RegDate date) throws ServiceInfrastructureException;

	/**
	 * @return la collectivite administrative de l'ACI
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	CollectiviteAdministrative getACI() throws ServiceInfrastructureException;

	/**
	 * @return la collectivite administrative  correspondant à l'IMPOT à la source, service de l'ACI
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	CollectiviteAdministrative getACIImpotSource() throws ServiceInfrastructureException;

	/**
	 * @return la collectivite administrative OIPM, service de l'ACI
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	CollectiviteAdministrative getACIOIPM() throws ServiceInfrastructureException;

	/**
	 * @return la collectivite administrative du CEDI
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	CollectiviteAdministrative getCEDI() throws ServiceInfrastructureException;

	/**
	 * @return la collectivite administrative du CAT
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	CollectiviteAdministrative getCAT() throws ServiceInfrastructureException;

	/**
	 * @return la collectivite administrative du RC (= Registre du Commerce du canton de Vaud)
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	CollectiviteAdministrative getRC() throws ServiceInfrastructureException;

	/**
	 * @param oid l'id d'un office d'impôt
	 * @return la liste des communes pour l'office d'impôt spécifié
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getListeCommunesByOID(int oid) throws ServiceInfrastructureException;

	/**
	 * Charge les communes et fractions de commune du canton
	 *
	 * @return les communes du canton de Vaud
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getCommunesDeVaud() throws ServiceInfrastructureException;

	/**
	 * Charge les communes hors canton
	 *
	 * @return toutes les communes de Suisse, sauf celles dans le canton de Vaud
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getCommunesHorsCanton() throws ServiceInfrastructureException;

	/**
	 * @param commune le numéro Ofs d'une commune
	 * @return une localité
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Localite> getLocaliteByCommune(int commune) throws ServiceInfrastructureException;

	/**
	 * @param localites une collection de localités
	 * @return les rues des localités spécifiées
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Rue> getRues(Collection<Localite> localites) throws ServiceInfrastructureException;

	/**
	 * @return la Suisse.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Pays getSuisse() throws ServiceInfrastructureException;

	/**
	 * @return la canton de Vaud
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Canton getVaud() throws ServiceInfrastructureException;

	/**
	 * @param cantonOFS un numéro Ofs de canton
	 * @return un canton
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Canton getCanton(int cantonOFS) throws ServiceInfrastructureException;

	/**
	 * @param sigle le sigle d'un canton ('VD', 'FR', ...)
	 * @return un canton
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Canton getCantonBySigle(String sigle) throws ServiceInfrastructureException;

	/**
	 * @param noOfsCommune le numéro Ofs d'une commune
	 * @return le canton dans lequel la commune existe
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Canton getCantonByCommune(int noOfsCommune) throws ServiceInfrastructureException;

	/**
	 * Retrouve la commune avec le numéro OFS donné ; si plusieurs communes correspondent, renvoie celle qui est valide à la date donnée
	 *
	 * @param noCommune numéro OFS de la commune (ou technique de la fraction de commune vaudoise)
	 * @param date      date de référence (<code>null</code> pour la date du jour)
	 * @return Commune
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Commune getCommuneByNumeroOfs(int noCommune, @Nullable RegDate date) throws ServiceInfrastructureException;

	/**
	 * Résoud la commune d'une adresse civile (s'il existe une commune directement attachée, on la prend, sinon on prend la commune correspondant à la localité de l'adresse - en Suisse)
	 *
	 * @param adresse adresse civile dont on cherche la commune
	 * @param date    la date de référence (<code>null</code> pour la date du jour)
	 * @return une commune, ou <code>null</code> si l'adresse est hors-Suisse
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	Commune getCommuneByAdresse(Adresse adresse, RegDate date) throws ServiceInfrastructureException;

	/**
	 * Résoud la commune d'une adresse générique (s'il existe une commune directement attachée, on la prend, sinon on prend la commune correspondant à la localité de l'adresse - en Suisse)
	 *
	 * @param adresse adresse générique (civile, fiscale, transférée d'un autre tiers...) dont on cherche la commune
	 * @param date    la date de référence (<code>null</code> pour la date du jour)
	 * @return une commune, ou <code>null</code> si l'adresse est hors-Suisse
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	Commune getCommuneByAdresse(AdresseGenerique adresse, RegDate date) throws ServiceInfrastructureException;

	/**
	 * Retourne la commune sur laquelle un bâtiment est construit.
	 *
	 * @param egid un numéro de bâtiment
	 * @param date la date à laquelle on se place pour faire la recherche (en cas de fusion de communes, un bâtiment peut être sur une commune un jour donné, et sur une autre le lendemain).
	 * @return une commune, ou <code>null</code> si le bâtiment est inconnu.
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	Commune getCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException;

	/**
	 * Résoud la commune faîtière d'une fraction de commune (renvoie la commune elle-même si ce n'est pas une fraction)
	 *
	 * @param commune       fraction de commune
	 * @param dateReference date de référence (au cas où...)
	 * @return la commune faîtière de la fraction donnée à la date donnée
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	Commune getCommuneFaitiere(Commune commune, RegDate dateReference) throws ServiceInfrastructureException;

	/**
	 * @param rue une rue
	 * @return <b>true</b> si la rue spécifiée est dans le canton de Vaud
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	boolean estDansLeCanton(final Rue rue) throws ServiceInfrastructureException;

	/**
	 * @param commune une commune
	 * @return <b>true</b> si la commune spécifiée est dans le canton de Vaud
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	boolean estDansLeCanton(final Commune commune) throws ServiceInfrastructureException;

	/**
	 * @param adresse une adresse générique
	 * @return <b>true</b> si l'adresse spécifiée est dans le canton de Vaud
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	boolean estDansLeCanton(AdresseGenerique adresse) throws ServiceInfrastructureException;

	/**
	 * @param adresse une adresse
	 * @return <b>true</b> si l'adresse spécifiée est dans le canton de Vaud
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	boolean estDansLeCanton(Adresse adresse) throws ServiceInfrastructureException;

	/**
	 * @param adresse une adresse générique
	 * @return <b>true</b> si l'adresse spécifiée est en Suisse
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	boolean estEnSuisse(AdresseGenerique adresse) throws ServiceInfrastructureException;

	/**
	 * @param adresse une adresse
	 * @return <b>true</b> si l'adresse spécifiée est en Suisse
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	boolean estEnSuisse(Adresse adresse) throws ServiceInfrastructureException;

	enum Zone {
		VAUD,
		HORS_CANTON,
		HORS_SUISSE
	}

	/**
	 * @param adresse une adresse
	 * @return la zone géographique de l'adresse spécifiée.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Zone getZone(AdresseGenerique adresse) throws ServiceInfrastructureException;

	/**
	 * Retourne l'entité representant un pays inconnu
	 *
	 * @return Pays
	 * @throws ServiceInfrastructureException en cas de problème
	 */
	Pays getPaysInconnu() throws ServiceInfrastructureException;

	/**
	 * @param metier un domaine métier
	 * @return la liste de tous les logiciels associés au domaine métier spécifié.
	 */
	List<Logiciel> getLogicielsPour(LogicielMetier metier);

	/**
	 * @param metier un domaine métier
	 * @return la liste de tous les logiciels certifiés associés au domaine métier spécifié.
	 */
	List<Logiciel> getLogicielsCertifiesPour(LogicielMetier metier);

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
	 * @return l'ensemble des différents régimes fiscaux disponibles
	 * @throws ServiceInfrastructureException en cas de souci
	 */
	List<TypeRegimeFiscal> getRegimesFiscaux() throws ServiceInfrastructureException;

	/**
	 * @param code le code d'un régime fiscal
	 * @return le régime fiscal qui correspond au code spécifié; ou <b>null</b> si le régime n'existe pas.
	 * @throws ServiceInfrastructureException en cas de souci
	 */
	@Nullable
	TypeRegimeFiscal getRegimeFiscal(@NotNull String code) throws ServiceInfrastructureException;

	/**
	 * @return l'ensemble des différents genres d'impôt utilisables dans les mandats spéciaux
	 * @throws ServiceInfrastructureException en cas de souci
	 */
	List<GenreImpotMandataire> getGenresImpotMandataires() throws ServiceInfrastructureException;
}