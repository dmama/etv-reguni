package ch.vd.uniregctb.interfaces.model;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;

public interface Individu extends EntiteCivile {

	/**
	 * @return le prénom de l'individu
	 */
	String getPrenom();

	/**
	 * @return les autres prénom de l'individu.
	 */
	String getAutresPrenoms();

	/**
	 * @return le nom de famille de l'individu
	 */
	String getNom();

	/**
	 * @return le nom de naissance de l'individu.
	 */
	String getNomNaissance();

    /**
     * Retourne la liste des adoptions et reconnaissances de l'individu.
     *
     * @return la liste des adoptions et reconnaissances de l'individu.
     */
    Collection<AdoptionReconnaissance> getAdoptionsReconnaissances();

    /**
     * Retourne la date de décès de l'individu.
     *
     * @return la date de décès de l'individu.
     */
    RegDate getDateDeces();

    /**
     * Retourne la date de naissance de l'individu.
     *
     * @return la date de naissance de l'individu.
     */
    RegDate getDateNaissance();

	/**
	 * Renvoie <code>true</code> si la date de naissance de l'individu est connue
	 * et si elle est moins de 18 ans avant la date passée en paramètre
	 * @param date date pour laquelle la réponse est valable
	 * @return <code>true</code> si l'individu est encore mineur à la date donnée, <code>false</code> s'il est déjà majeur (ou si sa date de naissance est inconnue)
	 */
	boolean isMineur(RegDate date);

	/**
     * @return la liste des enfants de l'individu.
     */
    Collection<RelationVersIndividu> getEnfants();

    /**
     * Retourne la liste des états civils de l'individu.
     *
     * @return la liste des états civils de l'individu.
     */
    EtatCivilList getEtatsCivils();

	/**
	 * Récupère l'état civil courant d'un individu.
	 *
	 * @return l'état civil courant de l'individu
	 */
	EtatCivil getEtatCivilCourant();

	/**
	 * Récupère l'état civil à une date donnée d'un individu.
	 * Si aucune date n'est donnée, l'état civil courant est retourné.
	 *
	 * @return l'état civil de l'individu
	 */
	EtatCivil getEtatCivil(RegDate date);

	/**
     * Retourne le numéro technique de l'individu.
     *
     * @return le numéro technique de l'individu.
     */
    long getNoTechnique();

	/**
	 * @return l'ancien numéro AVS sur 11 position.
	 */
	String getNoAVS11();

    /**
     * Retourne le numéro AVS formule 2008 de l'individu.
     *
     * @return le numéro AVS formule 2008 de l'individu.
     */
    String getNouveauNoAVS();

    /**
     * Retourne le numéro du registre des étrangers de l'individu.
     *
     * @return le numéro du registre des étrangers de l'individu.
     */
    String getNumeroRCE();

    /**
     * @return la liste des permis de l'individu, triée par ordre croissant d'obtention.
     */
    List<Permis> getPermis();

    /**
     * Retourne la liste des nationalités de l'individu.
     *
     * @return la liste des nationalites de l'individu.
     */
    List<Nationalite> getNationalites();

    /**
     * Indique si l'individu est de sexe masculin.
     *
     * @return <code>true</code> si l'individu est de sexe masculin.
     */
    boolean isSexeMasculin();

    /**
     * Retourne les origines de l'individu étendu.
     *
     * @return les origines de l'individu étendu.
     */
    Collection<Origine> getOrigines();

	/**
	 * @return les parents de l'individu courant.
	 */
	List<RelationVersIndividu> getParents();

    /**
     * Retourne la tutelle à laquelle l'individu étendu est soumis.
     *
     * @return la tutelle à laquelle l'individu étendu est soumis.
     */
    Tutelle getTutelle();

	/**
	 * Copie les parties spécifiées à partir de l'individu spécifié.
	 *
	 * @param individu l'individu sur lequel il faut copier les parties.
	 * @param parts    les parties à copier.
	 */
	void copyPartsFrom(Individu individu, Set<AttributeIndividu> parts);

	/**
	 * Clone l'individu en restreignant la liste des parties renseignées.
	 *
	 * @param parts les parties à renseigner.
	 * @return un nouvel individu.
	 */
	Individu clone(Set<AttributeIndividu> parts);

	/**
	 * Détermine le permis actif d'individu à une date donnée.
	 * <p/>
	 * <b>Note:</b> l'individu doit avoir sa collection de permis renseignée pour que cette méthode puisse retourner un résultat correct.
	 *
	 * @param date la date de validité du permis, ou <b>null</b> pour obtenir le dernis permis valide.
	 * @return le permis actif d'un individu à une date donnée.
	 */
	Permis getPermisActif(@Nullable RegDate date);
}
