package ch.vd.unireg.security;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.TypeDroitAcces;

/**
 * Service qui permet de gérer les droits d'accès (ajout, suppression, copie, transert) entre des opérateurs et des contribuables.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface DroitAccesService {

	/**
	 * Ajoute un droit d'accès sur un tiers pour un opérateur donné.
	 *
	 * @param visaOperateur le visa de l'opérateur qui reçoit le droit d'accès.
	 * @param tiersId       le tiers sur lequel le droit d'accès s'applique.
	 * @param type          le type d'accès
	 * @param niveau        le niveau d'accès
	 * @throws DroitAccesException si l'ajout de cet accès provoquerait un problème cohérence (p.a. interdiction + autorisation sur le même contribuable)
	 */
	DroitAcces ajouteDroitAcces(@NotNull String visaOperateur, long tiersId, TypeDroitAcces type, Niveau niveau) throws DroitAccesException;

	/**
	 * Copie les droits d'accès ouverts du dossier source vers le dossier destination
	 *
	 * @param source      dossier source des droits d'accès
	 * @param destination dossier auquel les droits d'accès doivent être ajoutés
	 * @throws DroitAccesException en cas de conflit entre les droits existant sur le dossier de destination et ceux sur le dossier source
	 */
	void copieDroitsAcces(Contribuable source, Contribuable destination) throws DroitAccesException;

	/**
	 * Copie les droits d'accès d'un opérateur à un autre.
	 *
	 * @param visaOperateurSource le visa de l'opérateur source.
	 * @param visaOperateurTarget le visa de l'opérateur destination.
	 * @return les éventuels conflit rencontrés lors de la copie (les accès sans conflit ont été copiés)
	 */
	List<DroitAccesConflit> copieDroitsAcces(@NotNull String visaOperateurSource, @NotNull String visaOperateurTarget);

	/**
	 * Transfère les droits d'accès d'un opérateur à un autre. Les droits de l'opérateur source seront donc annulés.
	 *
	 * @param visaOperateurSource le visa de l'opérateur source.
	 * @param visaOperateurTarget le visa de l'opérateur destination.
	 * @return les éventuels conflit rencontrés lors du transfert (les accès sans conflit ont été transférés, et les accès en conflit ont simplement été fermés sur l'opérateur source)
	 */
	List<DroitAccesConflit> transfereDroitsAcces(@NotNull String visaOperateurSource, @NotNull String visaOperateurTarget);

	/**
	 * Annule un droit d'accès.
	 *
	 * @param id l'id du droit d'accès à annuler.
	 */
	void annuleDroitAcces(long id) throws DroitAccesException;

	/**
	 * Annule tous les droits d'accès pour un opérateur donné.
	 *
	 * @param visaOperateur le numero d'individu de l'operateur dont les droits seront supprimés
	 */
	void annuleToutLesDroitAcces(@NotNull String visaOperateur);
}
