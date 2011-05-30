package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.model.LogicielMetier;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.TypeAffranchissement;

public interface ServiceInfrastructureService extends ServiceInfrastructureRaw {

	static final String SERVICE_NAME = "ServiceInfra";

	final static int noACI = 22;
	final static int noACIImpotSource = 47;
	final static int noACISuccessions = 1344;
	final static int noCEDI = 1012;
	final static int noTuteurGeneral = 1013;
	final static int noCAT = 1341;

	final static int noOfsSuisse = 8100;
	final static int noPaysInconnu = 8999;

	/**
	 * Constante sigle du canton de Vaud
	 */
	static final String SIGLE_CANTON_VD = "VD";

	/**
	 * Constante sigle du pays Suisse
	 */
	static final String SIGLE_SUISSE = "CH";

	/**
	 * Recherche un pays à partir de son code ('CH', 'FR', 'BE', ...). Voir la documentation de la méthode {@link ch.vd.infrastructure.model.Pays#getCodePays()}.
	 *
	 * @param codePays un code de pays ('CH', 'FR', 'BE', ...)
	 * @return le pays avec le code pays spécifié; ou <b>null</b> si aucun pays ne corresponds.
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Pays getPays(String codePays) throws ServiceInfrastructureException;

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
	 * @return la collectivite administrative  SUCCESSION, service de l'ACI
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	CollectiviteAdministrative getACISuccessions() throws ServiceInfrastructureException;

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
	 * @param cantonOFS le numéro Ofs d'un canton
	 * @return les communes du canton spécifié
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	List<Commune> getListeCommunes(int cantonOFS) throws ServiceInfrastructureException;

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
	 * Retrouve la commune avec le numéro OFS étendu donné ; si plusieurs communes correspondent, renvoie celle qui est valide à la date donnée
	 *
	 * @param noCommune numéro OFS de la commune (ou technique de la fraction de commune vaudoise)
	 * @param date      date de référence (<code>null</code> pour la date du jour)
	 * @return Commune
	 * @throws ServiceInfrastructureException en cas de problème d'accès à l'infrastructure
	 */
	Commune getCommuneByNumeroOfsEtendu(int noCommune, @Nullable RegDate date) throws ServiceInfrastructureException;

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
	 * @param egid             un numéro de bâtiment
	 * @param date             la date à laquelle on se place pour faire la recherche (en cas de fusion de communes, un bâtiment peut être sur une commune un jour donné, et sur une autre le lendemain).
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
	 *
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

	static enum Zone {
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
	 * Calcule et retourne le type d'affranchissement demandé par la poste pour envoyer un courrier dans un pays particulier.
	 *
	 * @param noOfsPays le numéro Ofs du pays de destination.
	 * @return le type d'affranchissement du courrier.
	 */
	TypeAffranchissement getTypeAffranchissement(int noOfsPays);

	/**
	 * @param metier un domaine métier
	 * @return la liste de tous les logiciels associés au domaine métier spécifié.
	 */
	List<Logiciel> getLogicielsPour(LogicielMetier metier);
}
