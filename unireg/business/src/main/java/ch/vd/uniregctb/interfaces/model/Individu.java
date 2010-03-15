package ch.vd.uniregctb.interfaces.model;

import java.util.Collection;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumAttributeIndividu;

public interface Individu extends EntiteCivile {

    /**
     * Retourne la liste des adoptions et reconnaissances de l'individu.
     *
     * @return la liste des adoptions et reconnaissances de l'individu.
     */
    Collection<AdoptionReconnaissance> getAdoptionsReconnaissances();

    /**
     * Retourne le conjoint de l'individu.
     *
     * @return le conjoint de l'individu.
     */
    Individu getConjoint();

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
     * Retourne le dernier historique de l'individu.
     *
     * @return le dernier historique de l'individu.
     */
    HistoriqueIndividu getDernierHistoriqueIndividu();

    /**
     * Retourne la liste des enfants de l'individu. Cette liste contient des objets de type
     * <code>ch.vd.registre.civil.model.Individu</code>.
     *
     * @return la liste des enfants de l'individu.
     */
    Collection<Individu> getEnfants();

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
     * Retourne la liste l'historique de l'individu.
     *
     * @return la liste l'historique de l'individu.
     */
    Collection<HistoriqueIndividu> getHistoriqueIndividu();

    /**
     * Retourne le numéro technique de l'individu.
     *
     * @return le numéro technique de l'individu.
     */
    long getNoTechnique();

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
     * Retourne la liste des permis de l'individu.
     *
     * @return la liste des permis de l'individu.
     */
    Collection<Permis> getPermis();

    /**
     * Retourne la liste des nationalités de l'individu.
     *
     * @return la liste des nationalites de l'individu.
     */
    Collection<Nationalite> getNationalites();

    /**
     * Indique si l'individu est de sexe masculin.
     *
     * @return <code>true</code> si l'individu est de sexe masculin.
     */
    boolean isSexeMasculin();

    /**
     * Retourne la mère de l'individu étendu.
     *
     * @return la mère de l'individu étendu.
     */
    Individu getMere();

    /**
     * Retourne l'origine de l'individu étendu.
     *
     * @return l'origine de l'individu étendu.
     */
    Origine getOrigine();

    /**
     * Retourne le père de l'individu étendu.
     *
     * @return le père de l'individu étendu.
     */
    Individu getPere();

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
	void copyPartsFrom(Individu individu, Set<EnumAttributeIndividu> parts);

	/**
	 * Clone l'individu en restreignant la liste des parties renseignées.
	 *
	 * @param parts les parties à renseigner.
	 * @return un nouvel individu.
	 */
	Individu clone(Set<EnumAttributeIndividu> parts);
}
