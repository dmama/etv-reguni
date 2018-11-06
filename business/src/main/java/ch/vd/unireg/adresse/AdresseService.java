package ch.vd.unireg.adresse;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.model.AdressesCiviles;
import ch.vd.unireg.interfaces.model.AdressesCivilesHisto;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.FormulePolitesse;

public interface AdresseService {

	/**
	 * Extrait les adresses civiles définies pour une date donnée.
	 * <p/>
	 * Les adresses civiles sont extraites: <ul> <li>du registre civil (contrôle des habitants) pour les individus</li> <li>du registre infrastructure pour les collectivités administratives</li> </ul>
	 *
	 * @param tiers  le tiers dont on recherche les adresses
	 * @param date   la date de référence, ou null pour obtenir les adresses existantes jusqu'à ce jour.
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return les adresses civiles du tiers, ou <b>null</b> si le tiers ne possède pas d'adresse.
	 * @throws AdresseException en cas d'erreur sur les adresses récupérées du registre civil.
	 */
	AdressesCiviles getAdressesCiviles(Tiers tiers, RegDate date, boolean strict) throws AdresseException;

	/**
	 * Extrait les adresses civiles définies pour une date donnée.
	 * <p/>
	 * Les adresses civiles sont extraites du registre civil (contrôle des habitants)
	 *
	 * @param numeroIndividu l'identifiant de l'individu civil dont on cherche les adresses
	 * @param date   la date de référence, ou null pour obtenir les adresses existantes jusqu'à ce jour.
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return les adresses civiles du tiers, ou <b>null</b> si l'individu ne possède pas d'adresse.
	 * @throws AdresseException en cas d'erreur sur les adresses récupérées du registre civil.
	 */
	AdressesCiviles getAdressesCiviles(long numeroIndividu, RegDate date, boolean strict) throws AdresseException;

	/**
	 * Extrait les adresses fiscales (= adresses civiles + adresses tiers) définies pour une date donnée. Les adresses annulées sont ignorées et ne sont pas retournées.
	 *
	 * @param tiers  le tiers dont on recherche les adresses
	 * @param date   la date de référence, ou null pour obtenir les adresses existantes jusqu'à ce jour.
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return les adresses fiscales du tiers à la date donnée.
	 * @throws AdresseException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	AdressesFiscales getAdressesFiscales(@Nullable Tiers tiers, @Nullable RegDate date, boolean strict) throws AdresseException;

	/**
	 * Extrait l'adresse fiscale (= adresse civile + adresse tiers) définie pour une date et un type d'adresse donné. Les adresses annulées sont ignorées et ne sont pas retournées.
	 *
	 * @param tiers  le tiers dont on recherche les adresses
	 * @param type   le type d'adresse voulu
	 * @param date   la date de référence, ou null pour obtenir l'adresse courante.
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return l'adresse fiscale du tiers pour le type et la date spécifiés.
	 * @throws AdresseException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	AdresseGenerique getAdresseFiscale(@Nullable Tiers tiers, TypeAdresseFiscale type, @Nullable RegDate date, boolean strict) throws AdresseException;

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
	AdressesFiscalesHisto getAdressesFiscalHisto(Tiers tiers, boolean strict) throws AdresseException;

	/**
	 * Calcul et retourne l'historique des adresses fiscales avec le détail complet des couches qui le composent.
	 *
	 * @param tiers  le tiers dont on recherche les adresses
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return l'historique des adresses fiscales sous forme de sandwich de couches d'adresses.
	 * @throws AdresseException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	AdressesFiscalesSandwich getAdressesFiscalesSandwich(Tiers tiers, boolean strict) throws AdresseException;

	/**
	 * Retourne l'historique des adresses civiles du tiers spécifié. Ou <b>null</b> si le tiers n'en possède pas.
	 *
	 * @param tiers  un tiers dont on veut extraite l'historique des adresses civiles.
	 * @param strict si <b>faux</b> essaie de résoudre silencieusement les problèmes détectés durant le traitement; autrement lève une exception.
	 * @return l'historique des adresses civiles du tiers spécifié.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	AdressesCivilesHisto getAdressesCivilesHisto(Tiers tiers, boolean strict) throws AdresseException;

	/**
	 * Retourne l'historique des adresses civiles de l'individu spécifié. Ou <b>null</b> s'il n'en possède pas. Elle sont extraites du registre civil.
	 *
	 * @param numeroIndividu l'identifiant de l'individu civil dont on cherche les adresses
	 * @param strict si <b>faux</b> essaie de résoudre silencieusement les problèmes détectés durant le traitement; autrement lève une exception.
	 * @return l'historique des adresses civiles de l'individu spécifié.
	 * @throws AdresseException en cas de problème dans le traitement
	 */
	AdressesCivilesHisto getAdressesCivilesHisto(long numeroIndividu, boolean strict) throws AdresseException;

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
	AdresseGenerique getAdresseRepresentant(Tiers tiers, TypeAdresseRepresentant type, RegDate date, boolean strict) throws AdresseException;

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
	AdresseEnvoiDetaillee getAdresseEnvoi(Tiers tiers, @Nullable RegDate date, TypeAdresseFiscale type, boolean strict) throws AdresseException;

	/**
	 * Crée et retourne une adresse d'envoi vide pour un tiers donné à utiliser pour les cas en erreur où on a besoin de passer
	 * le message d'erreur dans les champs (pour reporting).
	 *
	 * @param tiers  le tiers dont on "recherche" l'adresse.
	 * @return       une adresse d'envoi vide
	 */
	AdresseEnvoiDetaillee getDummyAdresseEnvoi(Tiers tiers);

	/**
	 * Crée et retourne l'historique complet des adresses d'envoi formattée (six lignes) pour un tiers donné.
	 *
	 * @param tiers  le tiers dont on recherche l'adresse.
	 * @param strict si <i>vrai</i>, la cohérence des données est vérifiée de manière stricte et en cas d'incohérence, une exception est levée. Si <i>faux</i>, la méthode essaie de corriger les données
	 *               (dans la mesure du possible) pour ne pas lever d'exception.
	 * @return l'adresse d'envoi déjà formattée.
	 * @throws AdresseException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	AdressesEnvoiHisto getAdressesEnvoiHisto(Tiers tiers, boolean strict) throws AdresseException;

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
	AdresseEnvoi getAdresseEnvoi(Individu individu, RegDate date, boolean strict) throws AdresseException;

	/**
	 * Créé et retourne l'adresse de courrier à fournir au registre foncier
	 *
	 * @param ctb  le contribuable dont on recherche l'adresse
	 * @param date la date de référence, ou null pour obtenir l'adresse existante à ce jour
	 * @return l'adresse structurée pour le RF
	 * @throws AdressesResolutionException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	AdresseCourrierPourRF getAdressePourRF(Contribuable ctb, @Nullable RegDate date) throws AdresseException;

	/**
	 * Ajoute une adresse fiscale sur un tiers. Cette méthode s'assure que la cohérence des plages d'adresse est respectée.
	 *
	 * @param tiers   le tiers sur lequel on veut ajouter une adresse
	 * @param adresse l'adresse à ajouter sur le tiers
	 * @return le tiers avec sa nouvelle adresse
	 */
	Tiers addAdresse(Tiers tiers, AdresseTiers adresse);

	/**
	 * Annule l'adresse spécifiée. Cette méthode s'assure que la cohérence des plages d'adresse est respectée.
	 *
	 * @param adresse l'adresse à annuler
	 */
	void annulerAdresse(AdresseTiers adresse);


	/**Ferme une adresse fiscale a une date donnée
	 *
	 * @param adresse l'adresse à fermer
	 *	 * @param dateFin date de fermeture de l'adresse.

	 */
	void fermerAdresse(AdresseTiers adresse,RegDate dateFin);

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
	List<String> getNomCourrier(Tiers tiers, @Nullable RegDate date, boolean strict) throws AdresseException;

	/**
	 * Calcul le nom courrier
	 *
	 * @param numeroIndividu un numéro d'individu
	 * @return le nom courrier de l'individu spécifié.
	 */
	String getNomCourrier(long numeroIndividu);

	/**
	 * Calcule et retourne la formule de politesse pour le tiers spécifié.
	 *
	 * @param tiers le tiers dont on veut connaître la formule de politesse.
	 * @param date  la date de valeur de la formule de politesse.
	 * @return une formule de politesse; ou <b>null</b> si aucune formule ne s'applique sur le tiers.
	 */
	@Nullable
	FormulePolitesse getFormulePolitesse(Tiers tiers, @Nullable RegDate date);

	/**
	 * Recherche la dernière adresse vaudoise d'un tiers
	 *
	 * @param tiers un tiers
	 * @param type  le type d'adresse considéré
	 * @return la dernière adresse vaudoise; ou <b>null</b> si le tiers n'a jamais eu d'adresse sur le canton de Vaud.
	 * @throws AdresseException en cas d'erreur dans les adresses (plages se recoupant, cycle infini détecté, ...).
	 */
	AdresseGenerique getDerniereAdresseVaudoise(Tiers tiers, TypeAdresseFiscale type) throws AdresseException;

	/**Appel au processeur du batch de résolution des adresses connues
	 *
	 * @param dateTraitement
	 * @param nbThreads
	 * @param status
	 * @return
	 */
	ResolutionAdresseResults resoudreAdresse(RegDate dateTraitement, int nbThreads, StatusManager status);

	/**
	 * Retourne uniquement les adresses de source purement fiscale stocké en base
	 * @param tiers  le Tiers concerné
	 * @return listes des adresses du tiers
	 */
	AdressesFiscalesHisto getAdressesTiers(Tiers tiers) throws AdresseException;

	/**
	 * Méthode de conversion entre une adresse générique et une adresse d'envoi
	 * @param tiers tiers pour lequel l'adresse est calculée
	 * @param adresse adresse à convertir
	 * @param date date de référence
	 * @return une adresse d'envoi détaillée
	 */
	AdresseEnvoiDetaillee buildAdresseEnvoi(Tiers tiers, AdresseGenerique adresse, RegDate date) throws AdresseException;
}
