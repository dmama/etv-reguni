package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;

public interface ServiceInfrastructureService {

	public static final String SERVICE_NAME = "ServiceInfra";

	public final static int noACI = 22;
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
	 * @throws Exception
	 */
	public List<Pays> getPays() throws InfrastructureException;

	/**
	 * @return le pays avec le numéro Ofs spécifié; ou <b>null</b> si aucun pays ne corresponds.
	 */
	public Pays getPays(int numeroOFS) throws InfrastructureException;

	/**
	 * Recherche un pays à partir de son code ('CH', 'FR', 'BE', ...). Voir la documentation de la méthode
	 * {@link ch.vd.infrastructure.model.Pays#getCodePays()}.
	 *
	 * @return le pays avec le code pays spécifié; ou <b>null</b> si aucun pays ne corresponds.
	 */
	public Pays getPays(String codePays) throws InfrastructureException;

	/**
	 * @return la collectivite administrative.
	 * @throws Exception
	 */
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException;

	/**
	 * @return la collectivite administrative de l'ACI
	 * @throws Exception
	 */
	public CollectiviteAdministrative getACI() throws InfrastructureException;

	/**
	 * @return la collectivite administrative du CEDI
	 * @throws Exception
	 */
	public CollectiviteAdministrative getCEDI() throws InfrastructureException;

	/**
	 * @return la collectivite administrative du CAT
	 * @throws Exception
	 */
	public CollectiviteAdministrative getCAT() throws InfrastructureException;

	/**
	 * @return
	 * @throws Exception
	 */
	public List<Canton> getAllCantons() throws InfrastructureException;

	/**
	 * @param canton
	 * @return
	 * @throws Exception
	 */
	public List<Commune> getListeCommunes(Canton canton) throws InfrastructureException;

	/**
	 * @return
	 * @throws Exception
	 */
	public List<Commune> getListeFractionsCommunes() throws InfrastructureException;

	/**
	 * @param cantonAsString
	 * @return
	 * @throws Exception
	 */
	public List<Commune> getListeCommunes(int cantonOFS) throws InfrastructureException;

	/**
	 * @return la liste des communes pour l'office d'impôt spécifié
	 */
	public List<Commune> getListeCommunesByOID(int oid) throws InfrastructureException;

	/**
	 * Charge les communes et fractions de commune du canton
	 *
	 * @return
	 * @throws Exception
	 */
	public List<Commune> getCommunesDeVaud() throws InfrastructureException;

	/**
	 * Charge les communes hors canton
	 *
	 * @return
	 * @throws Exception
	 */
	public List<Commune> getCommunesHorsCanton() throws InfrastructureException;

	/**
	 * Charge les communes
	 *
	 * @return
	 * @throws Exception
	 */
	public List<Commune> getCommunes() throws InfrastructureException;

	/**
	 * @return
	 * @throws Exception
	 */
	public List<Localite> getLocalites() throws InfrastructureException;

	/**
	 * @return
	 * @throws Exception
	 */
	public Localite getLocaliteByONRP(int onrp) throws InfrastructureException;

	/**
	 * @return
	 * @throws Exception
	 */
	public List<Localite>  getLocaliteByCommune(int commune) throws InfrastructureException;

	/**
	 * @return
	 * @throws Exception
	 */
	public List<Rue> getAllRues() throws InfrastructureException;

	/**
	 * @return
	 * @throws Exception
	 */
	public List<Rue> getRues(Localite localite) throws InfrastructureException;

	/**
	 * @return
	 * @throws Exception
	 */
	public List<Rue> getRues(Collection<Localite> localites) throws InfrastructureException;

	/**
	 * Renvoie les rues de ce canton
	 *
	 * @param canton
	 * @return une liste de rues
	 * @throws InfrastructureException
	 */
	public List<Rue> getRues(Canton canton) throws InfrastructureException;

	/**
	 * @return la rue qui correspond au numéro technique spécifié.
	 */
	public Rue getRueByNumero(int numero) throws InfrastructureException;

	/**
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws Exception
	 */
	public Pays getSuisse() throws ServiceInfrastructureException;

	/**
	 * @return
	 * @throws Exception
	 */
	public Canton getVaud() throws InfrastructureException;

	/**
	 * @param cantonOFS
	 * @return
	 * @throws Exception
	 */
	public Canton getCanton(int cantonOFS) throws InfrastructureException;

	/**
	 * @param cantonOFS
	 * @return
	 * @throws Exception
	 */
	public Canton getCantonBySigle(String sigle) throws InfrastructureException;

	/**
	 * @return le canton dans lequel la commune existe
	 * @throws InfrastructureException
	 */
	public Canton getCantonByCommune(int noOfsCommune) throws InfrastructureException;

	/**
	 * @param numeroACI
	 * @return Commune
	 * @throws Exception
	 */
	public Commune getCommuneVaudByNumACI(Integer numeroACI) throws InfrastructureException;

	/**
	 * Retrouve la commune avec le numéro OFS étendu donné ; si plusieurs communes correspondent,
	 * renvoie celle qui est valide à la date donnée
	 * @param noCommune numéro OFS de la commune (ou technique de la fraction de commune vaudoise)
	 * @param date date de référence (<code>null</code> pour la date du jour)
	 * @return Commune
	 * @throws InfrastructureException
	 */
	public Commune getCommuneByNumeroOfsEtendu(int noCommune, RegDate date) throws InfrastructureException;

	/**
	 *
	 * @param localite
	 * @return la commune correspondant à la localité EN GERANT LES FRACTIONS de commune
	 * @throws InfrastructureException
	 */
	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException;

	/**
	 * @return la commune correspondant à l'adresse civile, ou <b>null</b> si l'adresse n'est pas en Suisse.
	 */
	public Commune getCommuneByAdresse(Adresse adresse) throws InfrastructureException;

	/**
	 * @return la commune correspondant à l'adresse spécifiée, ou <b>null</b> si l'adresse est hors-Suisse.
	 */
	public Commune getCommuneByAdresse(AdresseGenerique adresse) throws InfrastructureException;

	/**
	 * @return l'office d'impôt à partir de son numéro de collectivité.
	 */
	public OfficeImpot getOfficeImpot(int noColAdm) throws InfrastructureException;

	/**
	 * @return l'office d'impôt responsable de la commune spécifiée par son numéro OFS.
	 */
	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws InfrastructureException ;

	/**
	 * @return tous les offices d'impôt de district du canton de Vaud
	 */
	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException;

	/**
	 * @return <b>true</b> si la rue spécifiée est dans le canton de Vaud
	 */
	public boolean estDansLeCanton(final Rue rue) throws InfrastructureException;

	/**
	 * @return <b>true</b> si la commune spécifiée est dans le canton de Vaud
	 */
	public boolean estDansLeCanton(final CommuneSimple commune) throws InfrastructureException;

	/**
	 * @return <b>true</b> si la commune spécifiée est dans le canton de Vaud
	 */
	public boolean estDansLeCanton(final Commune commune) throws InfrastructureException;

	/**
	 * @return <b>true</b> si l'adresse spécifiée est dans le canton de Vaud
	 */
	public boolean estDansLeCanton(AdresseGenerique adresse) throws InfrastructureException;

	/**
	 * @return <b>true</b> si l'adresse spécifiée est dans le canton de Vaud
	 */
	public boolean estDansLeCanton(Adresse adresse) throws InfrastructureException;

	/**
	 * @return <b>true</b> si l'adresse spécifiée est en Suisse
	 */
	public boolean estEnSuisse(AdresseGenerique adresse) throws InfrastructureException;

	/**
	 * @return <b>true</b> si l'adresse spécifiée est en Suisse
	 */
	public boolean estEnSuisse(Adresse adresse) throws InfrastructureException;

	public static enum Zone {
		VAUD,
		HORS_CANTON,
		HORS_SUISSE
	}

	/**
	 * @return la zone géographique de l'adresse spécifiée.
	 */
	public Zone getZone(AdresseGenerique adresse) throws InfrastructureException;

	/**
	 * Retourne la liste des collectivites administratives du canton
	 *
	 * @return
	 * @throws InfrastructureException
	 */
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException ;

	/**
	 * Retourne la liste des collectivites administratives du canton
	 * @param typesCollectivite
	 * @return
	 * @throws InfrastructureException
	 */
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite) throws InfrastructureException ;


	/**
	 * Retourne l'entité representant un pays inconnu
	 *
	 * @return Pays
	 * @throws InfrastructureException
	 */
	public Pays getPaysInconnu() throws InfrastructureException ;

    /**
	 * Retourne l'institution financière spécifiée par son id technique.
	 *
	 * @param id
	 *            l'id de l'institution financière
	 * @return une institution financière ou <code>null</code> si aucune institution ne correspond à l'id spécifié.
	 */
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws InfrastructureException;

    /**
	 * La ou les institutions financière enregistrées sous le numéro de clearing spécifié.
	 * <p>
	 * <b>Note:</b> logiquement, on ne devrait retourner qu'une institution financière pour un clearing donné, mais il se trouve que cette
	 * contrainte n'est pas respectée dans la base.
	 *
	 * @param noClearing
	 *            un numéro de clearing
	 * @return 0, 1 ou plusieurs institutions financières.
	 */
    public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws InfrastructureException;

    /** Permet de retourner une localite a partir d'un npa
     *
     * @param le npa
     * @return la localite
     * @throws InfrastructureException
     */
	public Localite getLocaliteByNPA(int npa) throws InfrastructureException;
}
