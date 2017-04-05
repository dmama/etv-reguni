package ch.vd.unireg.interfaces.civil.data;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.uniregctb.type.Sexe;

public interface Individu {

	/**
	 * @return le statut (actif, inactif, remplacé par...) de l'individu
	 */
	StatutIndividu getStatut();

	/**
	 * @return le prénom usuel de l'individu
	 */
	String getPrenomUsuel();

	/**
	 * @return tous les prénoms de l'individu.(comprend normalement également le prénom usuel)
	 */
	String getTousPrenoms();

	/**
	 * @return le nom de famille de l'individu
	 */
	String getNom();

	/**
	 * @return le nom de naissance de l'individu.
	 */
	String getNomNaissance();

	/**
	 * @return la date de décès de l'individu.
	 */
	RegDate getDateDeces();

	/**
	 * @return la date de naissance de l'individu.
	 */
	RegDate getDateNaissance();

	/**
	 * @return la date d'arrivée dans le canton.
	 */
	RegDate getDateArriveeVD();

	/**
	 * Renvoie <code>true</code> si la date de naissance de l'individu est connue et si elle est moins de 18 ans avant la date passée en paramètre
	 *
	 * @param date date pour laquelle la réponse est valable
	 * @return <code>true</code> si l'individu est encore mineur à la date donnée, <code>false</code> s'il est déjà majeur (ou si sa date de naissance est inconnue)
	 */
	boolean isMineur(RegDate date);

	/**
	 * @return la liste des états civils de l'individu.
	 */
	EtatCivilList getEtatsCivils();

	/**
	 * @return l'état civil courant de l'individu
	 */
	EtatCivil getEtatCivilCourant();

	/**
	 * Récupère l'état civil à une date donnée d'un individu. Si aucune date n'est donnée, l'état civil courant est retourné.
	 *
	 * @param date la date de valeur de l'état-civil
	 * @return l'état civil de l'individu
	 */
	EtatCivil getEtatCivil(RegDate date);

	/**
	 * @return le numéro technique de l'individu.
	 */
	long getNoTechnique();

	/**
	 * @return l'ancien numéro AVS sur 11 positions.
	 */
	String getNoAVS11();

	/**
	 * @return le numéro AVS sur 13 positions de l'individu.
	 */
	String getNouveauNoAVS();

	/**
	 * @return le numéro du registre des étrangers de l'individu.
	 */
	String getNumeroRCE();

	/**
	 * @return la liste des permis de l'individu, triée par ordre croissant d'obtention.
	 */
	PermisList getPermis();

	/**
	 * @return la liste des nationalités connues de l'individu
	 */
	Collection<Nationalite> getNationalites();

	/**
	 * @return le sexe de l'individu
	 */
	Sexe getSexe();

	/**
	 * @return les origines de l'individu .
	 */
	Collection<Origine> getOrigines();

	/**
	 * @return les parents de l'individu courant.
	 */
	List<RelationVersIndividu> getParents();

	/**
	 * @return le nom officiel de la mère
	 */
	NomPrenom getNomOfficielMere();

	/**
	 * @return le nom officiel du père
	 */
	NomPrenom getNomOfficielPere();

	/**
	 * @return l'historique des conjoints de l'individu.
	 */
	List<RelationVersIndividu> getConjoints();

	/**
	 * @return la liste des adresses de l'individu
	 */
	Collection<Adresse> getAdresses();

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
	 * Clone l'individu en gardant uniquement l'historique des données jusqu'à une certaine date.
	 *
	 * @param date la limite jusqu'à laquelle les données doivent être gardées.
	 * @return un nouvel individu.
	 */
	Individu cloneUpTo(@NotNull RegDate date);

	/**
	 * @return l'ensemble des parts effectivement renseignées sur l'individu. Ces parts peuvent différer des parts explicitement demandées, car certaines implémentations renseignent systématiquement
	 *         certaines parts.
	 */
	Set<AttributeIndividu> getAvailableParts();
}
