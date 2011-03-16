package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.ApplicationFiscale;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.model.LogicielMetier;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.TypeAffranchissement;
import ch.vd.uniregctb.interfaces.model.TypeEtatPM;
import ch.vd.uniregctb.interfaces.model.TypeRegimeFiscal;

public interface ServiceInfrastructureService {

	public static final String SERVICE_NAME = "ServiceInfra";

	public final static int noACI = 22;
	public final static int noACIImpotSource = 47;
	public final static int noACISuccessions = 1344;
	public final static int noCEDI = 1012;
	public final static int noTuteurGeneral = 1013;
	public final static int noCAT = 1341;

	public final static int noOfsSuisse = 8100;

	/**
	 * Constante sigle du canton de Vaud
	 */
	public static final String SIGLE_CANTON_VD = "VD";

	/**
	 * Constante sigle du pays Suisse
	 */
	public static final String SIGLE_SUISSE = "CH";

	/**
	 * @return la liste des pays.
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<Pays> getPays() throws InfrastructureException;

	/**
	 * @param numeroOFS un numéro Ofs de pays.
	 * @return le pays avec le numéro Ofs spécifié; ou <b>null</b> si aucun pays ne corresponds.
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public Pays getPays(int numeroOFS) throws InfrastructureException;

	/**
	 * Recherche un pays à partir de son code ('CH', 'FR', 'BE', ...). Voir la documentation de la méthode {@link ch.vd.infrastructure.model.Pays#getCodePays()}.
	 *
	 * @param codePays un code de pays ('CH', 'FR', 'BE', ...)
	 * @return le pays avec le code pays spécifié; ou <b>null</b> si aucun pays ne corresponds.
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public Pays getPays(String codePays) throws InfrastructureException;

	/**
	 * @param noColAdm le numéro technique de la collectivité
	 * @return la collectivite administrative.
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException;

	/**
	 * @return la collectivite administrative de l'ACI
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public CollectiviteAdministrative getACI() throws InfrastructureException;

	/**
	 * @return la collectivite administrative  correspondant à l'IMPOT à la source, service de l'ACI
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public CollectiviteAdministrative getACIImpotSource() throws InfrastructureException;


	/**
	 * @return la collectivite administrative  SUCCESSION, service de l'ACI
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public CollectiviteAdministrative getACISuccessions() throws InfrastructureException;

	/**
	 * @return la collectivite administrative du CEDI
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public CollectiviteAdministrative getCEDI() throws InfrastructureException;

	/**
	 * @return la collectivite administrative du CAT
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public CollectiviteAdministrative getCAT() throws InfrastructureException;

	/**
	 * @return tous les cantons de la Suisse
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<Canton> getAllCantons() throws InfrastructureException;

	/**
	 * @param canton un canton
	 * @return les communes du canton spécifié
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<Commune> getListeCommunes(Canton canton) throws InfrastructureException;

	/**
	 * @return La liste des communes vaudoise (en incluant les fractions mais pas leur commune faîtière)
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<Commune> getListeFractionsCommunes() throws InfrastructureException;

	/**
	 * @param cantonOFS le numéro Ofs d'un canton
	 * @return les communes du canton spécifié
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<Commune> getListeCommunes(int cantonOFS) throws InfrastructureException;

	/**
	 * @param oid l'id d'un office d'impôt
	 * @return la liste des communes pour l'office d'impôt spécifié
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<Commune> getListeCommunesByOID(int oid) throws InfrastructureException;

	/**
	 * Charge les communes et fractions de commune du canton
	 *
	 * @return les communes du canton de Vaud
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<Commune> getCommunesDeVaud() throws InfrastructureException;

	/**
	 * Charge les communes hors canton
	 *
	 * @return toutes les communes de Suisse, sauf celles dans le canton de Vaud
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<Commune> getCommunesHorsCanton() throws InfrastructureException;

	/**
	 * Charge les communes
	 *
	 * @return toutes les communes de Suisse
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<Commune> getCommunes() throws InfrastructureException;

	/**
	 * @return toutes les localités de Suisse
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<Localite> getLocalites() throws InfrastructureException;

	/**
	 * @param onrp le numéro technique de la localité
	 * @return la localité qui corresponds à numéro technique spécifié
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public Localite getLocaliteByONRP(int onrp) throws InfrastructureException;

	/**
	 * @param commune le numéro Ofs d'une commune
	 * @return une localité
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<Localite> getLocaliteByCommune(int commune) throws InfrastructureException;

	/**
	 * @param localite une localité
	 * @return les rues de la localité spécifiée
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<Rue> getRues(Localite localite) throws InfrastructureException;

	/**
	 * @param localites une collection de localités
	 * @return les rues des localités spécifiées
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<Rue> getRues(Collection<Localite> localites) throws InfrastructureException;

	/**
	 * Renvoie les rues de ce canton
	 *
	 * @param canton un canton
	 * @return une liste de rues
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<Rue> getRues(Canton canton) throws InfrastructureException;

	/**
	 * @param numero le numéro technique d'une rue
	 * @return la rue qui correspond au numéro technique spécifié.
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public Rue getRueByNumero(int numero) throws InfrastructureException;

	/**
	 * @return la Suisse.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public Pays getSuisse() throws ServiceInfrastructureException;

	/**
	 * @return la canton de Vaud
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public Canton getVaud() throws InfrastructureException;

	/**
	 * @param cantonOFS un numéro Ofs de canton
	 * @return un canton
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public Canton getCanton(int cantonOFS) throws InfrastructureException;

	/**
	 * @param sigle le sigle d'un canton ('VD', 'FR', ...)
	 * @return un canton
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public Canton getCantonBySigle(String sigle) throws InfrastructureException;

	/**
	 * @param noOfsCommune le numéro Ofs d'une commune
	 * @return le canton dans lequel la commune existe
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public Canton getCantonByCommune(int noOfsCommune) throws InfrastructureException;

	/**
	 * @param numeroACI le numéro ACI d'une commune
	 * @return une commune
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 * @deprecated TODO (msi)
	 */
	public Commune getCommuneVaudByNumACI(Integer numeroACI) throws InfrastructureException;

	/**
	 * Retrouve la commune avec le numéro OFS étendu donné ; si plusieurs communes correspondent, renvoie celle qui est valide à la date donnée
	 *
	 * @param noCommune numéro OFS de la commune (ou technique de la fraction de commune vaudoise)
	 * @param date      date de référence (<code>null</code> pour la date du jour)
	 * @return Commune
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public Commune getCommuneByNumeroOfsEtendu(int noCommune, RegDate date) throws InfrastructureException;

	/**
	 * @param localite une localité
	 * @return la commune correspondant à la localité EN GERANT LES FRACTIONS de commune
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException;

	/**
	 * Résoud la commune d'une adresse civile (s'il existe une commune directement attachée, on la prend, sinon on prend la commune correspondant à la localité de l'adresse - en Suisse)
	 *
	 * @param adresse adresse civile dont on cherche la commune
	 * @param date    la date de référence (<code>null</code> pour la date du jour)
	 * @return une commune, ou <code>null</code> si l'adresse est hors-Suisse
	 * @throws InfrastructureException en cas de problème
	 */
	public CommuneSimple getCommuneByAdresse(Adresse adresse, RegDate date) throws InfrastructureException;

	/**
	 * Résoud la commune d'une adresse générique (s'il existe une commune directement attachée, on la prend, sinon on prend la commune correspondant à la localité de l'adresse - en Suisse)
	 *
	 * @param adresse adresse générique (civile, fiscale, transférée d'un autre tiers...) dont on cherche la commune
	 * @param date    la date de référence (<code>null</code> pour la date du jour)
	 * @return une commune, ou <code>null</code> si l'adresse est hors-Suisse
	 * @throws InfrastructureException en cas de problème
	 */
	public CommuneSimple getCommuneByAdresse(AdresseGenerique adresse, RegDate date) throws InfrastructureException;

	/**
	 * Retourne le numéro Ofs de la commune sur laquelle un bâtiment est construit.
	 *
	 * @param egid             un numéro de bâtiment
	 * @param date             la date à laquelle on se place pour faire la recherche (en cas de fusion de communes, un bâtiment peut être sur une commune un jour donné, et sur une autre le lendemain).
	 * @param hintNoOfsCommune (optionnel) un numéro Ofs de commune qui sera utilisé comme indice pour accélérer la recherche (par exemple, le numéro de commune faîtière en cas de fractions de commune,
	 *                         ou le numéro de commune avant/après fusion)
	 * @return le numéro Ofs de la commune, ou <code>null</code> si le bâtiment est inconnu.
	 * @throws InfrastructureException en cas de problème
	 */
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date, Integer hintNoOfsCommune) throws InfrastructureException;

	/**
	 * Retourne la commune sur laquelle un bâtiment est construit.
	 *
	 * @param egid             un numéro de bâtiment
	 * @param date             la date à laquelle on se place pour faire la recherche (en cas de fusion de communes, un bâtiment peut être sur une commune un jour donné, et sur une autre le lendemain).
	 * @param hintNoOfsCommune (optionnel) un numéro Ofs de commune qui sera utilisé comme indice pour accélérer la recherche (par exemple, le numéro de commune faîtière en cas de fractions de commune,
	 *                         ou le numéro de commune avant/après fusion)
	 * @return une commune, ou <code>null</code> si le bâtiment est inconnu.
	 * @throws InfrastructureException en cas de problème
	 */
	public CommuneSimple getCommuneByEgid(int egid, RegDate date, Integer hintNoOfsCommune) throws InfrastructureException;

	/**
	 * Résoud la commune faîtière d'une fraction de commune (renvoie la commune elle-même si ce n'est pas une fraction)
	 *
	 * @param commune       fraction de commune
	 * @param dateReference date de référence (au cas où...)
	 * @return la commune faîtière de la fraction donnée à la date donnée
	 * @throws InfrastructureException en cas de problème
	 */
	public CommuneSimple getCommuneFaitiere(CommuneSimple commune, RegDate dateReference) throws InfrastructureException;

	/**
	 * @param noColAdm le numéro de collectivité administrative de l'office d'impôt
	 * @return l'office d'impôt à partir de son numéro de collectivité.
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public OfficeImpot getOfficeImpot(int noColAdm) throws InfrastructureException;

	/**
	 * @param noCommune le numéro Ofs d'un commune
	 * @return l'office d'impôt responsable de la commune spécifiée par son numéro OFS.
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws InfrastructureException;

	/**
	 * @return tous les offices d'impôt de district du canton de Vaud
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException;

	/**
	 * @param rue une rue
	 * @return <b>true</b> si la rue spécifiée est dans le canton de Vaud
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public boolean estDansLeCanton(final Rue rue) throws InfrastructureException;

	/**
	 * @param commune une commune
	 * @return <b>true</b> si la commune spécifiée est dans le canton de Vaud
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public boolean estDansLeCanton(final CommuneSimple commune) throws InfrastructureException;

	/**
	 * @param commune une commune
	 * @return <b>true</b> si la commune spécifiée est dans le canton de Vaud
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public boolean estDansLeCanton(final Commune commune) throws InfrastructureException;

	/**
	 * @param adresse une adresse générique
	 * @return <b>true</b> si l'adresse spécifiée est dans le canton de Vaud
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public boolean estDansLeCanton(AdresseGenerique adresse) throws InfrastructureException;

	/**
	 * @param adresse une adresse
	 * @return <b>true</b> si l'adresse spécifiée est dans le canton de Vaud
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public boolean estDansLeCanton(Adresse adresse) throws InfrastructureException;

	/**
	 * @param adresse une adresse générique
	 * @return <b>true</b> si l'adresse spécifiée est en Suisse
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public boolean estEnSuisse(AdresseGenerique adresse) throws InfrastructureException;

	/**
	 * @param adresse une adresse
	 * @return <b>true</b> si l'adresse spécifiée est en Suisse
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public boolean estEnSuisse(Adresse adresse) throws InfrastructureException;

	public static enum Zone {
		VAUD,
		HORS_CANTON,
		HORS_SUISSE
	}

	/**
	 * @param adresse une adresse
	 * @return la zone géographique de l'adresse spécifiée.
	 * @throws InfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	public Zone getZone(AdresseGenerique adresse) throws InfrastructureException;

	/**
	 * @return la liste des collectivites administratives du canton de Vaud
	 * @throws InfrastructureException en cas de problème
	 */
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException;

	/**
	 * @param typesCollectivite le type de collectivité administrative
	 * @return la liste des collectivites administratives du canton de Vaud du type spécifié
	 * @throws InfrastructureException en cas de problème
	 */
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite) throws InfrastructureException;


	/**
	 * Retourne l'entité representant un pays inconnu
	 *
	 * @return Pays
	 * @throws InfrastructureException en cas de problème
	 */
	public Pays getPaysInconnu() throws InfrastructureException;

	/**
	 * Retourne l'institution financière spécifiée par son id technique.
	 *
	 * @param id l'id de l'institution financière
	 * @return une institution financière ou <code>null</code> si aucune institution ne correspond à l'id spécifié.
	 * @throws InfrastructureException en cas de problème
	 */
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws InfrastructureException;

	/**
	 * La ou les institutions financière enregistrées sous le numéro de clearing spécifié.
	 * <p/>
	 * <b>Note:</b> logiquement, on ne devrait retourner qu'une institution financière pour un clearing donné, mais il se trouve que cette contrainte n'est pas respectée dans la base.
	 *
	 * @param noClearing un numéro de clearing
	 * @return 0, 1 ou plusieurs institutions financières.
	 * @throws InfrastructureException en cas de problème
	 */
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws InfrastructureException;

	/**
	 * Permet de retourner une localite a partir d'un npa
	 *
	 * @param npa le npa
	 * @return la localite
	 * @throws InfrastructureException en cas de problème
	 */
	public Localite getLocaliteByNPA(int npa) throws InfrastructureException;

	/**
	 * Calcule et retourne le type d'affranchissement demandé par la poste pour envoyer un courrier dans un pays particulier.
	 *
	 * @param noOfsPays le numéro Ofs du pays de destination.
	 * @return le type d'affranchissement du courrier.
	 */
	TypeAffranchissement getTypeAffranchissement(int noOfsPays);

	/**
	 * @return la liste des types de régimes fiscaux qui existent pour les personnes morales.
	 * @throws InfrastructureException en cas de problème
	 */
	List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws InfrastructureException;

	/**
	 * @param code un code de régime fiscal
	 * @return le régime fiscal pour le code demandé; ou <null> si le code ne correspond à aucun régime fiscal connu,
	 * @throws InfrastructureException en cas de problème
	 */
	TypeRegimeFiscal getTypeRegimeFiscal(String code) throws InfrastructureException;

	/**
	 * @return la liste des types d'états qui existent pour les personnes morales.
	 * @throws InfrastructureException en cas de problème
	 */
	List<TypeEtatPM> getTypesEtatsPM() throws InfrastructureException;

	/**
	 * @param code un code de type d'état PM
	 * @return le type d'état PM pour le code demandé; ou <null> si le code ne correspond à aucun type d'état connu,
	 * @throws InfrastructureException en cas de problème
	 */
	TypeEtatPM getTypeEtatPM(String code) throws InfrastructureException;

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
	 * @param idLogiciel l'id d'un logiciel
	 * @return un logiciel; ou <b>null</b> si aucun logiciel ne possède l'id spécifié.
	 */
	Logiciel getLogiciel(Long idLogiciel);

	/**
	 * @return la liste de tous les logiciels connus.
	 */
	List<Logiciel> getTousLesLogiciels();

	/**
	 * @param metier un domaine métier
	 * @return la liste de tous les logiciels associés au domaine métier spécifié.
	 */
	List<Logiciel> getLogicielsPour(LogicielMetier metier);
}
