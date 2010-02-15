package ch.vd.uniregctb.fiscal.service;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.ejb.EJBObject;

import ch.vd.ifosdi.metier.exceptions.BusinessException;
import ch.vd.ifosdi.metier.registre.ContribuableSDI;
import ch.vd.registre.common.model.CoordonneesFinancieres;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.registre.fiscal.model.Assujettissement;
import ch.vd.registre.fiscal.model.Contribuable;
import ch.vd.registre.fiscal.model.ContribuableRetourInfoDi;
import ch.vd.registre.fiscal.model.DeclarationQuittance;
import ch.vd.registre.fiscal.model.EnumCritereRechercheContribuable;
import ch.vd.registre.fiscal.model.For;
import ch.vd.registre.fiscal.model.RechercherComplementInformationContribuable;
import ch.vd.registre.fiscal.model.RechercherNoContribuable;
import ch.vd.registre.fiscal.model.ResultatRechercheContribuable;
import ch.vd.registre.fiscal.model.ResultatRechercherNoContribuable;

/**
 * Interface du service EJB Unireg-Interfaces
 *
 * @author Baba NGOM xsibnm
 *@version $Revision: 1.0 $
 */
public interface ServiceFiscal extends EJBObject{

	/** Nom JNDI du service EJB. */
	static final String JNDI_NAME = "ejb/" + ServiceFiscal.class.getName();

	/**
     * Retourne la liste des assujettissements, valides durant l'annÃ©e en paramÃªtre, pour un contribuable identifiÃ© par
     * le numÃ©ro en paramÃªtre.
     * <p>
     * Cette liste contient des objets de type {@link Assujettissement}.
     * <p>
     * Ce service renseigne, pour chaque objet du graphe retournÃ©, l'ensemble des attributs mono-valuÃ©s. De plus, ce
     * service charge Ã©galement les dÃ©clarations d'impÃ´t de chaque assujettissement retournÃ© dans la liste.
     * <p>
     * La liste retournÃ©e par ce service peut Ãªtre vide, signifiant l'absence de donnÃ©es d'un point de vue mÃ©tier pour
     * les paramÃªtres donnÃ©s.
     *
     * @param noContribuable le numÃ©ro du contribuable.
     * @param annee l'annÃ©e de validitÃ©.
     * @return la liste des assujettissements, valides durant l'annÃ©e, du contribuable.
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si aucun contribuable, pour l'annÃ©e donnÃ©e, n'est identifiÃ© par le numÃ©ro en paramÃªtre
     *             ou si un problÃ¨me mÃ©tier survient lors de l'invocation du service.
     */
    Collection getAssujettissements(long noContribuable, int annee) throws RemoteException, RegistreException;

    /**
     * Retourne le contribuable, valide durant l'annÃ©e en paramÃªtre, identifiÃ© par le numÃ©ro en paramÃªtre.
     * <p>
     * Ce service renseigne, pour chaque objet du graphe retournÃ©, l'ensemble des attributs mono-valuÃ©s ainsi que les
     * attributs muti-valuÃ©s suivants :
     * <li>Si il s'agit d'un contribuable foyer, la liste des membres du contribuable foyer.</li>
     * <li>La liste des enfants de l'individu principal.</li>
     * <li>La liste des Ã©tats civil de l'individu principal et de l'individu conjoint.</li>
     * <p>
     * L'objet retournÃ© par ce service peut Ãªtre <code>null</code>, signifiant l'absence de donnÃ©es d'un point de vue
     * mÃ©tier pour les paramÃªtres donnÃ©s.
     *
     * @param noContribuable le numÃ©ro du contribuable.
     * @param annee l'annÃ©e de validitÃ©.
     * @return le contribuable populÃ© avec les donnÃ©es de l'annÃ©e de validitÃ©.
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si un problÃ¨me mÃ©tier survient lors de l'invocation du service.
     */
    Contribuable getContribuable(long noContribuable, int annee) throws RemoteException, RegistreException;

    /**
     * Retourne le contribuable, valide durant l'annÃ©e en paramÃªtre, selon les rÃªgle dÃ©crites ci-dessous..
     * <p>
     * Si le paramÃªtre <code>recupererContribuableFoyer</code> contient la valeur <code>true</code>, alors le
     * service retourne, si le paramÃªtre <code>noContribuable</code> contient un numÃ©ro de contribuable individu, le
     * contribuable foyer correspondant.
     * <p>
     * Si le paramÃªtre <code>recupererContribuableFoyer</code> contient la valeur <code>true</code>, alors le
     * service retourne, si le paramÃªtre <code>noContribuable</code> contient un numÃ©ro de contribuable foyer, le
     * contribuable foyer identifiÃ© par le numÃ©ro en paramÃªtre.
     * <p>
     * Si le paramÃªtre <code>recupererContribuableFoyer</code> contient la valeur <code>false</code>, le service
     * retourne toujours le contribuable correspondant strictement au numÃ©ro de contribuable en paramÃªtre.
     * <p>
     * Ce service renseigne, pour chaque objet du graphe retournÃ©, l'ensemble des attributs mono-valuï¿½s ainsi que les
     * attributs muti-valuÃ©s suivants :
     * <li>Si il s'agit d'un contribuable foyer, la liste des membres du contribuable foyer.</li>
     * <li>La liste des enfants de l'individu principal.</li>
     * <li>La liste des Ã©tats civil de l'individu principal et de l'individu conjoint.</li>
     * <p>
     * L'objet retournÃ© par ce service peut Ãªtre <code>null</code>, signifiant l'absence de donnÃ©es d'un point de vue
     * mÃ©tier pour les paramÃªtres donnÃ©s.
     *
     * @param noContribuable le numÃ©ro du contribuable.
     * @param annee l'annÃ©e de validitÃ©.
     * @param recupererContribuableFoyer indique si le contribuable foyer correspondant au
     * @return le contribuable populÃ© avec les donnÃ©es de l'annÃ©e de validitÃ©.
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si un problÃ¨me mÃ©tier survient lors de l'invocation du service.
     */
    Contribuable getContribuable(long noContribuable, int annee, boolean recupererContribuableFoyer) throws RemoteException, RegistreException;

    /**
     * Retourne le contribuable, valide durant l'annÃ©e en paramÃªtre, selon les rÃªgle dÃ©crites ci-dessous (ATTENTION: <em>Lire attentivement les informations qui ne sont pas renseignÃ©es</em>).
     * <p>
     * Si le paramÃªtre <code>recupererContribuableFoyer</code> contient la valeur <code>true</code>, alors le
     * service retourne, si le paramÃªtre <code>noContribuable</code> contient un numÃ©ro de contribuable individu, le
     * contribuable foyer correspondant.
     * <p>
     * Si le paramÃªtre <code>recupererContribuableFoyer</code> contient la valeur <code>true</code>, alors le
     * service retourne, si le paramÃªtre <code>noContribuable</code> contient un numÃ©ro de contribuable foyer, le
     * contribuable foyer identifiÃ© par le numÃ©ro en paramÃªtre.
     * <p>
     * Si le paramÃªtre <code>recupererContribuableFoyer</code> contient la valeur <code>false</code>, le service
     * retourne toujours le contribuable correspondant strictement au numÃ©ro de contribuable en paramÃªtre.
     * <p>
     * Ce service renseigne, pour chaque objet du graphe retournÃ©, l'ensemble des attributs mono-valuÃ©s.
     * <p>
     * <em>L'information suivante n'est pas rÃ©cupÃ©rÃ©e</em> de la persistence (elle n'est pas renseignÃ©e).
     * <li>la liste des membres du contribuable foyer. (<em>pas renseignÃ©e</em>)</li>
     * <li>La liste des enfants de l'individu principal. (<em>pas renseignÃ©e</em>)</li>
     * <li>La liste des Ã©tats civil de l'individu principal et de l'individu conjoint. (<em>pas renseignÃ©e</em>)</li>
     * <p>
     * Pour obtenir une information complÃ¨te utiliser la mÃ©thode <em>getContribuable</em>.
     *
     * <p>
     * L'objet retournÃ© par ce service peut Ãªtre <code>null</code>, signifiant l'absence de donnÃ©es d'un point de vue
     * mÃ©tier pour les paramÃªtres donnÃ©s.
     *
     * <p>
     * NOTE: Appelle seulement le proxy REG_CONTRIBUABLE_INFO_GENERAL_V1 - IL2D
     * <br>
     * NOTE: N'appelle pas le proxy REG_CONTRIBUABLE_INFO_FAMILLE - IL1E. Utiliser pour cela la mÃ©thode <em>getContribuable</em>.
     *
     * @see #getContribuable(long, int, boolean)
     *
     * @param noContribuable
     * @param annee
     * @param recupererContribuableFoyer
     * @return le contribuable populÃ© avec les donnÃ©es de l'annÃ©e de validitÃ© (<em>donnÃ©es pas renseignÃ©es: principal, membres, enfants, conjoint</em>).
     * @throws RemoteException
     * @throws RegistreException
     */
    Contribuable getContribuableInfoGenerale(long noContribuable, int annee, boolean recupererContribuableFoyer) throws RemoteException, RegistreException;

    /**
     * Retourne les coordonnÃ©es financiÃ¨res d'un contribuable identifiÃ© par le numÃ©ro en paramÃªtre.
     * <p>
     * Ce service renseigne, pour chaque objet du graphe retournÃ©, l'ensemble des attributs mono-valuï¿½s.
     * <p>
     * L'objet retournÃ© par ce service peut Ãªtre <code>null</code>, signifiant l'absence de donnÃ©es d'un point de vue
     * mÃ©tier pour les paramÃªtres donnÃ©s.
     *
     * @param noContribuable le numÃ©ro du contribuable.
     * @return les coordonnÃ©es financiÃ¨res du contribuable.
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si aucun contribuable, pour l'annÃ©e donnÃ©e, n'est identifiÃ© par le numÃ©ro en paramÃªtre
     *             ou si un problÃ¨me mÃ©tier survient lors de l'invocation du service.
     */
    CoordonneesFinancieres getCoordonneesFinancieres(long noContribuable) throws RemoteException, RegistreException;

    /**
     * Retourne les coordonnÃ©es financiÃ¨res, valides durant l'annÃ©e en paramÃªtre, pour un contribuable identifiÃ© par le
     * numÃ©ro en paramÃªtre.
     * <p>
     * Ce service renseigne, pour chaque objet du graphe retournÃ©, l'ensemble des attributs mono-valuÃ©s.
     * <p>
     * L'objet retournÃ© par ce service peut Ãªtre <code>null</code>, signifiant l'absence de donnÃ©es d'un point de vue
     * mÃ©tier pour les paramÃªtres donnÃ©s.
     *
     * @param noContribuable le numÃ©ro du contribuable.
     * @param annee l'annÃ©e de validitÃ©.
     * @return les coordonnÃ©es financiÃ¨res, valides durant l'annÃ©e, du contribuable.
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si aucun contribuable, pour l'annÃ©e donnÃ©e, n'est identifiÃ© par le numÃ©ro en paramÃªtre
     *             ou si un problÃ¨me mÃ©tier survient lors de l'invocation du service.
     * @deprecated cette mï¿½thode est remplacÃ©e par {@link #getCoordonneesFinancieres(long)}.
     */
    @Deprecated
	CoordonneesFinancieres getCoordonneesFinancieres(long noContribuable, int annee) throws RemoteException, RegistreException;

    /**
     * Retourne la liste des fors, valides durant l'annÃ©e en paramÃªtre, pour un contribuable identifiÃ© par le numÃ©ro en
     * paramÃªtre.
     * <p>
     * Cette liste contient des objets de type {@link For}.
     * <p>
     * Ce service renseigne, pour chaque objet du graphe retournÃ©, l'ensemble des attributs mono-valuÃ©s.
     * <p>
     * La liste retournÃ©e par ce service peut Ãªtre vide, signifiant l'absence de donnÃ©es d'un point de vue mÃ©tier pour
     * les paramÃªtres donnÃ©s.
     *
     * @param noContribuable le numÃ©ro du contribuable.
     * @param annee l'annÃ©e de validitÃ©.
     * @return la liste des fors, valides durant l'annÃ©e, du contribuable.
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si aucun contribuable, pour l'annÃ©e donnÃ©e, n'est identifiÃ© par le numÃ©ro en paramÃªtre
     *             ou si un problÃ¨me mÃ©tier survient lors de l'invocation du service.
     */
    Collection getFors(long noContribuable, int annee) throws RemoteException, RegistreException;

    /**
     * Retourne la liste des numÃ©ros des contribuables ayant subi une mutation (modification au niveau des fors, de
     * l'assujettissement, etc.) au sein du Registre dans la pÃ©riode passÃ©e en paramÃªtre. Il est possible de spÃ©cifier
     * un numÃ©ro de contribuable Ã  partir duquel s'effectue la recherche.
     *
     * <p>
     * Cette liste contient des objets de type {@link Integer}.
     *
     * @param dateDebutRech la date de dÃ©but de la pÃ©riode de recherche.
     * @param dateFinRech la date de fin de la pÃ©riode de recherche.
     * @param numeroCtbDepart le numÃ©ro du contribuable Ã  partir duquel s'effectue la recherche.
     * @return la liste des numÃ©ros des contribuables ayant subi une mutation Registre.
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si un problÃ¨me mÃ©tier survient lors de l'invocation du service.
     */
    Collection getListeCtbModifies(Date dateDebutRech, Date dateFinRech, int numeroCtbDepart) throws RemoteException, RegistreException;

    /**
     * Retourne la liste des numÃ©ros de contribuables qui n'ont pas reÃ§u de DI (DÃ©claration d'impÃ´t) pour la pÃ©riode
     * fiscale spÃ©cifiÃ©e. Il est possible de spÃ©cifier un numÃ©ro de contribuable Ã  partir duquel s'effectue la
     * recherche.
     *
     * <p>
     * Cette liste contient des objets de type {@link Integer}.
     *
     * @param periodeFiscale la periode fiscale.
     * @param numeroCtbDepart numÃ©ro du contribuable Ã  partir duquel s'effectue la recherche.
     * @return la liste des numÃ©ros de contribuables qui n'ont pas reÃ§u de DI pour la pÃ©riode fiscale spÃ©cifiÃ©e.
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si un problÃ¨me mÃ©tier survient lors de l'invocation du service.
     */
    Collection getListeCtbSansDIPeriode(int periodeFiscale, int numeroCtbDepart) throws RemoteException, RegistreException;

    /**
     * Retourne la liste des numÃ©ros de contribuable foyer auxquels le contribuable individu, identifiÃ© par le numÃ©ro en
     * paramÃªtre, a participÃ©s. Aussi, les paramÃªtres <code>anneeDebut</code> et <code>anneeFin</code> permettent de
     * dÃ©limiter la pÃ©riode de recherche du service.
     * <p>
     * Cette liste contient des objets de type {@link Long}.
     *
     * @param noContribuableIndividu le numÃ©ro de contribuable individu.
     * @param anneeDebut l'annÃ©e dÃ©but de recherche. Si la valeur de ce paramÃªtre est Ã©gale Ã  <code>0</code>, aucune
     *            filtre n'est appliquÃ©e sur l'annÃ©e de dÃ©but de recherche.
     * @param anneeFin l'annÃ©e fin de recherche.Si la valeur de ce paramÃªtre est Ã©gale Ã  <code>0</code>, aucune
     *            filtre n'est appliquÃ©e sur l'annÃ©e de fin de recherche.
     * @return la liste des numÃ©ros de contribuable foyer auxquels le contribuable individu Ã  participÃ©.
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si un problÃ¨me mÃ©tier survient lors de l'invocation du service.
     */
    Collection getNoContribuableFoyer(long noContribuableIndividu, int anneeDebut, int anneeFin) throws RemoteException, RegistreException;

    /**
     * Service permettant de quittancer une liste de dï¿½claration d'impÃ´t en paramÃªtre.
     *
     * <p>
     * Cette liste contient des objets de type {@link DeclarationQuittance} et doit contenir un maximum de 50 ï¿½lï¿½ments.
     *
     * @param declarationQuittances la liste des dï¿½clarations d'impÃ´t Ã  quittancer.
     * @return la liste des dï¿½clarations d'impÃ´t quittancï¿½es.
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si un problÃ¨me mÃ©tier survient lors de l'invocation du service.
     */
    List quittanceDeclarations(List declarationQuittances) throws RemoteException, RegistreException;

    /**
     * Retourne la liste des contribuables, valides durant l'annÃ©e en paramÃªtre, correspondant aux critÃ¨res passï¿½s en
     * paramÃªtre.
     * <p>
     * Cette liste contient des objets de type {@link ResultatRechercheContribuable}.
     * <p>
     * Ce service renseigne, pour chaque objet du graphe retournÃ©, l'ensemble des attributs mono-valuï¿½s.
     * <p>
     * La liste retournÃ©e par ce service peut Ãªtre vide, signifiant l'absence de donnÃ©es d'un point de vue mÃ©tier pour
     * les paramÃªtres donnÃ©s.
     *
     * @param criteresRecherche les critÃ¨res de recherche de contribuables. Cet objet contient un ensemble de couples
     *            clÃ© et valeur, les diffÃ©rentes clÃ©s Ã©tant dÃ©finies par la classe
     *            {@link EnumCritereRechercheContribuable}.
     * @param nbResultat le nombre maximum de contribuables retournÃ©s par cette recherche. La valeur de ce paramÃªtre ne
     *            peut excï¿½der 49. Si la recherche renvoie plus de resultats que ce paramÃªtre, le nombre de rÃ©sultat
     *            sera limitÃ© Ã  <code>nbResultat</code> + 1.
     * @return la liste des contribuables, valides durant l'annÃ©e en paramÃªtre, correspondant aux critÃ¨res passÃ©s en
     *         paramÃªtre.
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si un problÃ¨me mÃ©tier survient lors de l'invocation du service.
     */
    Collection rechercherContribuables(HashMap criteresRecherche, int nbResultat) throws RemoteException, RegistreException;

    /**
     * Met a jour le code blocage remboursement automatique du contribuable
     *
     * @param noContribuable le numÃ©ro de contribuable .
     * @param code code blocage remboursement automatique.
     * @param user le User de l'opÃ©rateur qui met la valeur Ã  jour.
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si un problÃ¨me mÃ©tier survient lors de l'invocation du service.
     */
    void modifierCodeBlocageRmbtAuto(long noContribuable, boolean code, String user) throws RemoteException,  RegistreException;

    /**
     * Obtient l'indication si le contribuable avec le numÃ©ro <code>noContribuable</code> est un I107.
     * @param noContribuable le numÃ©ro de contribuable .
     * @return Retourne <b>true</b>, si le contribuable est un I107, autrement <b>false</b>
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si un problï¿½me mï¿½tier survient lors de l'invocation du service.
     */
    boolean isContribuableI107( long noContribuable)throws RemoteException, RegistreException;

    /**
     * Met a jour des informations pour un contribuable
     * @param contribuableRetourInfoDi ContribuableRetourInfoDi contenant les informations a modifier et le numÃ©ro du ctb Ã  modifier.
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si un problÃ¨me mÃ©tier survient lors de l'invocation du service.
     */
    void modifierInformationsPersonnelles(ContribuableRetourInfoDi contribuableRetourInfoDi) throws RemoteException, RegistreException;

    /**
     * Recherche du numéro de contribuable par rapport au no AVC, nom, prénom, années NCS
     * @param rechercherNoContribuable RechercherNoContribuable contenant les informations pour la recherche.
     * @return ResultatRechercherNoContribuable contient les informations trouvées
     * @throws RemoteException si un problème technique survient durant l'invocation du service.
     * @throws RegistreException si un problème métier survient lors de l'invocation du service.
     */

    ResultatRechercherNoContribuable RechercherNoContribuable(RechercherNoContribuable rechercherNoContribuable) throws RemoteException, RegistreException;


    /**
     * interroge le registre pour recueillir les informations sur le contribuable
     *
     * @param dateRef : date de référence pour l'accès au registre - doit ne contenir que des données
     *        année, mois, jour. si des heures, minutes, seconds ou milliseconds sont passées, le résultat est imprévisible
     * @param numeroCtb : numéro du contribuable dans le registre
     * @return ch.vd.ifosdi.metier.registre.Contribuable
     * @throws BusinessException si le contribuable n'existe pas
     * @throws RemoteException une exception dans la communication
     */
    ContribuableSDI getCtrlContribuable(Date dateRef, int numeroCtb) throws RemoteException, BusinessException;

    /**
     * Obtient des informations complÃ©mentaires du contribuable.
     * @param noContribuable le numÃ©ro de contribuable .
     * @return Retourne les informations complÃ©mentaires du contribuable.
     * @throws RemoteException si un problÃ¨me technique survient durant l'invocation du service.
     * @throws RegistreException si un problÃ¨me mÃ©tier survient lors de l'invocation du service.
     * @deprecated le complÃ©ment d'information est directement accessible dans le contribuable.
     */
    @Deprecated
	RechercherComplementInformationContribuable getComplementInformationContribuable( long noContribuable) throws RemoteException, RegistreException;

}
