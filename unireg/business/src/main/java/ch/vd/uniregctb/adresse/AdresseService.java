package ch.vd.uniregctb.adresse;

import java.util.List;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.FormulePolitesse;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public interface AdresseService {

	public TiersService getTiersService();

	public void setTiersService(TiersService tiersService);

	public ServiceInfrastructureService getServiceInfra();

	public void setServiceInfra(ServiceInfrastructureService serviceInfra);

	public ServiceCivilService getServiceCivilService();

	public void setServiceCivilService(ServiceCivilService serviceCivil);

	/**
	 * Extrait les adresses civiles définies pour une date donnée.
	 * <p>
	 * Les adresses civiles sont extraites:
	 * <ul>
	 * <li>du registre civile (contrôle des habitants) pour les individus</li>
	 * <li>du registre infrastructure pour les collectivités administratives</li>
	 * </ul>
	 *
	 * @param tiers
	 *            le tiers dont on recherche les adresses
	 * @param date
	 *            la date de référence, ou null pour obtenir les adresses existantes jusqu'à ce jour.
	 * @return les adresses civiles du tiers, ou <b>null</b> si le tiers ne possède pas d'adresse.
	 */
	public AdressesCiviles getAdressesCiviles(Tiers tiers, RegDate date);

	/**
	 * Extrait les adresses fiscales (= adresses civiles + adresses tiers) définies pour une date donnée.
	 *
	 * @param tiers
	 *            le tiers dont on recherche les adresses
	 * @param date
	 *            la date de référence, ou null pour obtenir les adresses existantes jusqu'à ce jour.
	 * @throws AdressesResolutionException
	 *             en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public abstract AdressesFiscales getAdressesFiscales(Tiers tiers, RegDate date) throws AdressesResolutionException;

	/**
	 * Extrait l'adresse fiscal (= adresse civile + adresse tiers) définie pour une date et un type d'adresse donné.
	 *
	 * @param tiers
	 *            le tiers dont on recherche les adresses
	 * @param type
	 *            le type d'adresse voulu
	 * @param date
	 *            la date de référence, ou null pour obtenir l'adresse courante.
	 * @throws AdressesResolutionException
	 *             en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public abstract AdresseGenerique getAdresseFiscale(Tiers tiers, TypeAdresseTiers type, RegDate date) throws AdressesResolutionException;

	/**
	 * Extrait l'historique des adresses fiscales (= adresses civils + adresse tiers) pour le tiers spécifié.
	 * <p>
	 * <b>Attention !</b> Les adresses retournées peuvent contenir des adresses annulées.
	 *
	 * @throws AdressesResolutionException
	 *             en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public abstract AdressesFiscalesHisto getAdressesFiscalHisto(Tiers tiers) throws AdressesResolutionException;

	/**
	 * Retourne l'adresse 'représentation' du représentant du tiers spécifié.
	 *
	 * @param tiers
	 *            le tiers dont on recherche l'adresse du représentant
	 * @param type
	 *            le type de représentation voulu.
	 * @return les adresses demandée, ou <b>null</b> si le tiers ne possède pas de représentant.
	 * @throws AdressesResolutionException
	 *             en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public AdresseGenerique getAdresseRepresentant(Tiers tiers, TypeAdresseRepresentant type, RegDate date)
			throws AdressesResolutionException;

	/**
	 * Crée et retourne l'adresse d'envoi pour un tiers donné.
	 *
	 * @param tiers
	 *            le tiers dont on recherche l'adresse.
	 * @param date
	 *            la date de référence, ou null pour obtenir les adresses existantes jusqu'à ce jour.
	 *
	 * @return l'adresse d'envoi déjà formattée.
	 * @throws AdressesResolutionException
	 *             en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public abstract AdresseEnvoiDetaillee getAdresseEnvoi(Tiers tiers, RegDate date) throws AdressesResolutionException;

	/**
	 * Crée et retourne l'adresse d'envoi pour un individu donné.
	 *
	 * @param individu
	 *            l'individu dont on recherche l'adresse.
	 * @param date
	 *            la date de référence, ou null pour obtenir les adresses existantes jusqu'à ce jour.
	 *
	 * @return l'adresse d'envoi déjà formattée
	 * @throws AdressesResolutionException
	 *             en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public abstract AdresseEnvoi getAdresseEnvoi(Individu individu, RegDate date) throws AdressesResolutionException;

	/**
	 * Créé et retourne l'adresse de courrier à fournir au registre foncier
	 *
	 * @param contribuable le contribuable dont on recherche l'adresse
	 * @param date la date de référence, ou null pour obtenir l'adresse existante à ce jour
	 *
	 * @return l'adresse structurée pour le RF
	 * @throws AdressesResolutionException
	 *             en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public AdresseCourrierPourRF getAdressePourRF(Contribuable ctb, RegDate date) throws AdressesResolutionException;

	/**
	 * Ajoute une adresse fiscale sur un tiers. Cette méthode s'assure que la cohérence des plages d'adresse est respectée.
	 *
	 * @param tiers
	 *            le tiers sur lequel on veut ajouter une adresse
	 * @param adresse
	 *            l'adresse à ajouter sur le tiers
	 * @return le tiers avec sa nouvelle adresse
	 */
	public Tiers addAdresse(Tiers tiers, AdresseTiers adresse);

	/**
	 * Annule l'adresse spécifiée. Cette méthode s'assure que la cohérence des plages d'adresse est respectée.
	 *
	 * @param adresse
	 *            l'adresse à annuler
	 */
	public void annulerAdresse(AdresseTiers adresse);

	/**
	 * Retourne le nom utilisé dans l'adresse courrier du tiers spécifié. En fonction du type de tiers, ce nom peut prendre 1 ou deux
	 * lignes.
	 *
	 * @param tiers
	 * @param date
	 *            la date de référence, ou <b>null</b> pour obtenir les valeurs valides à ce jour.
	 * @return le nom courrier du tiers.
	 * @throws AdressesResolutionException
	 *             en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public List<String> getNomCourrier(Tiers tiers, RegDate date) throws AdressesResolutionException;

	/**
	 * Calcul le nom courrier
	 *
	 * @param numeroIndividu
	 * @return
	 * @throws AdressesResolutionException
	 */
	public String getNomCourrier(Long numeroIndividu) throws AdressesResolutionException ;

	/**
	 * Calcule et retourne la formule de politesse pour le tiers spécifié.
	 *
	 * @param tiers
	 *            le tiers dont on veut connaître la formule de politesse.
	 * @return une formule de politesse; ou <b>null</b> si aucune formule ne s'applique sur le tiers.
	 */
	public FormulePolitesse getFormulePolitesse(Tiers tiers);

	/**Recherche la dernière adressevaudoise d'un tiers
	 *
	 * @param tiers
	 * @return
	 */

	public AdresseGenerique getDerniereAdresseVaudoise(Tiers tiers, TypeAdresseTiers type) throws AdressesResolutionException, InfrastructureException ;
}
