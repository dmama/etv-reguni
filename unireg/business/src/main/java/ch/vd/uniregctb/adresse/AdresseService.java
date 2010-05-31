package ch.vd.uniregctb.adresse;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.FormulePolitesse;

public interface AdresseService {

	/**
	 * Extrait les adresses civiles définies pour une date donnée.
	 * <p/>
	 * Les adresses civiles sont extraites: <ul> <li>du registre civile (contrôle des habitants) pour les individus</li> <li>du registre infrastructure pour les collectivités administratives</li> </ul>
	 *
	 * @param tiers  le tiers dont on recherche les adresses
	 * @param date   la date de référence, ou null pour obtenir les adresses existantes jusqu'à ce jour.
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return les adresses civiles du tiers, ou <b>null</b> si le tiers ne possède pas d'adresse.
	 * @throws AdresseException en cas d'erreur sur les adresses récupérées du registre civil.
	 */
	public AdressesCiviles getAdressesCiviles(Tiers tiers, RegDate date, boolean strict) throws AdresseException;

	/**
	 * Extrait les adresses fiscales (= adresses civiles + adresses tiers) définies pour une date donnée.
	 *
	 * @param tiers  le tiers dont on recherche les adresses
	 * @param date   la date de référence, ou null pour obtenir les adresses existantes jusqu'à ce jour.
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return les adresses fiscales du tiers à la date donnée.
	 * @throws AdresseException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public abstract AdressesFiscales getAdressesFiscales(Tiers tiers, RegDate date, boolean strict) throws AdresseException;

	/**
	 * Extrait l'adresse fiscale (= adresse civile + adresse tiers) définie pour une date et un type d'adresse donné.
	 *
	 * @param tiers  le tiers dont on recherche les adresses
	 * @param type   le type d'adresse voulu
	 * @param date   la date de référence, ou null pour obtenir l'adresse courante.
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return l'adresse fiscale du tiers pour le type et la date spécifiés.
	 * @throws AdresseException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public abstract AdresseGenerique getAdresseFiscale(Tiers tiers, TypeAdresseFiscale type, RegDate date, boolean strict) throws AdresseException;

	/**
	 * Extrait l'historique des adresses fiscales (= adresses civils + adresse tiers) pour le tiers spécifié.
	 * <p/>
	 * <b>Attention !</b> Les adresses retournées peuvent contenir des adresses annulées.
	 *
	 * @param tiers  le tiers dont on recherche les adresses
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return l'historique des adresses fiscales du tiers
	 * @throws AdresseException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public abstract AdressesFiscalesHisto getAdressesFiscalHisto(Tiers tiers, boolean strict) throws AdresseException;

	/**
	 * Retourne l'adresse 'représentation' du représentant du tiers spécifié.
	 *
	 * @param tiers  le tiers dont on recherche l'adresse du représentant
	 * @param type   le type de représentation voulu.
	 * @param date   la date de référence, ou null pour obtenir l'adresse courante.
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return les adresses demandée, ou <b>null</b> si le tiers ne possède pas de représentant.
	 * @throws AdresseException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public AdresseGenerique getAdresseRepresentant(Tiers tiers, TypeAdresseRepresentant type, RegDate date, boolean strict)
			throws AdresseException;

	/**
	 * Crée et retourne l'adresse d'envoi formattée (six lignes) pour un tiers donné.
	 *
	 * @param tiers  le tiers dont on recherche l'adresse.
	 * @param date   la date de référence, ou null pour obtenir les adresses existantes jusqu'à ce jour.
	 * @param type   le type d'adresse voulu
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return l'adresse d'envoi déjà formattée.
	 * @throws AdresseException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public abstract AdresseEnvoiDetaillee getAdresseEnvoi(Tiers tiers, RegDate date, TypeAdresseFiscale type, boolean strict) throws AdresseException;

	/**
	 * Crée et retourne l'adresse d'envoi pour un individu donné.
	 *
	 * @param individu l'individu dont on recherche l'adresse.
	 * @param date     la date de référence, ou null pour obtenir les adresses existantes jusqu'à ce jour.
	 * @param strict   si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *                 (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return l'adresse d'envoi déjà formattée
	 * @throws AdresseException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public abstract AdresseEnvoi getAdresseEnvoi(Individu individu, RegDate date, boolean strict) throws AdresseException;

	/**
	 * Créé et retourne l'adresse de courrier à fournir au registre foncier
	 *
	 * @param ctb  le contribuable dont on recherche l'adresse
	 * @param date la date de référence, ou null pour obtenir l'adresse existante à ce jour
	 * @return l'adresse structurée pour le RF
	 * @throws AdressesResolutionException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public AdresseCourrierPourRF getAdressePourRF(Contribuable ctb, RegDate date) throws AdresseException;

	/**
	 * Ajoute une adresse fiscale sur un tiers. Cette méthode s'assure que la cohérence des plages d'adresse est respectée.
	 *
	 * @param tiers   le tiers sur lequel on veut ajouter une adresse
	 * @param adresse l'adresse à ajouter sur le tiers
	 * @return le tiers avec sa nouvelle adresse
	 */
	public Tiers addAdresse(Tiers tiers, AdresseTiers adresse);

	/**
	 * Annule l'adresse spécifiée. Cette méthode s'assure que la cohérence des plages d'adresse est respectée.
	 *
	 * @param adresse l'adresse à annuler
	 */
	public void annulerAdresse(AdresseTiers adresse);

	/**
	 * Retourne le nom utilisé dans l'adresse courrier du tiers spécifié. En fonction du type de tiers, ce nom peut prendre 1 ou deux lignes.
	 *
	 * @param tiers  un tiers
	 * @param date   la date de référence, ou <b>null</b> pour obtenir les valeurs valides à ce jour.
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return le nom courrier du tiers.
	 * @throws AdresseException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public List<String> getNomCourrier(Tiers tiers, RegDate date, boolean strict) throws AdresseException;

	/**
	 * Calcul le nom courrier
	 *
	 * @param numeroIndividu un numéro d'individu
	 * @return le nom courrier de l'individu spécifié.
	 */
	public String getNomCourrier(long numeroIndividu);

	/**
	 * Calcule et retourne la formule de politesse pour le tiers spécifié.
	 *
	 * @param tiers le tiers dont on veut connaître la formule de politesse.
	 * @return une formule de politesse; ou <b>null</b> si aucune formule ne s'applique sur le tiers.
	 */
	public FormulePolitesse getFormulePolitesse(Tiers tiers);

	/**
	 * Recherche la dernière adresse vaudoise d'un tiers
	 *
	 * @param tiers un tiers
	 * @param type  le type d'adresse considéré
	 * @return la dernière adresse vaudoise; ou <b>null</b> si le tiers n'a jamais eu d'adresse sur le canton de Vaud.
	 * @throws AdresseException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	public AdresseGenerique getDerniereAdresseVaudoise(Tiers tiers, TypeAdresseFiscale type) throws AdresseException;
}
