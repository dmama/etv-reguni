package ch.vd.uniregctb.security;

import java.util.List;

import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeDroitAcces;

/**
 * Service qui permet de gérer les droits d'accès (ajout, suppression, copie, transert) entre des opérateurs et des contribuables.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface DroitAccesService {

	/**
	 * Ajoute un droit d'accès sur un tiers pour un opérateur donné.
	 * @param operateurId le numéro d'individu de l'opérateur qui reçoit le droit d'accès.
	 * @param tiersId le tiers sur lequel le droit d'accès s'applique.
	 * @param type le type d'accès
	 * @param niveau le niveau d'accès
	 * @throws DroitAccesException si l'ajout de cet accès provoquerait un problème cohérence (p.a. interdiction + autorisation sur le même contribuable)
	 */
	DroitAcces ajouteDroitAcces(long operateurId, long tiersId, TypeDroitAcces type, Niveau niveau) throws DroitAccesException;

	/**
	 * Copie les droits d'accès ouverts du dossier source vers le dossier destination
	 * @param source dossier source des droits d'accès
	 * @param destination dossier auquel les droits d'accès doivent être ajoutés
	 * @throws DroitAccesException en cas de conflit entre les droits existant sur le dossier de destination et ceux sur le dossier source
	 */
	void copieDroitsAcces(Contribuable source, Contribuable destination) throws DroitAccesException;

	/**
	 * Copie les droits d'accès d'un opérateur à un autre.
	 * @param operateurSourceId le numéro d'individu de l'opérateur source.
	 * @param operateurTargetId le numéro d'individu de l'opérateur destination.
	 * @return les éventuels conflit rencontrés lors de la copie (les accès sans conflit ont été copiés)
	 */
	List<DroitAccesConflit> copieDroitsAcces(long operateurSourceId, long operateurTargetId);

	/**
	 * Transfère les droits d'accès d'un opérateur à un autre. Les droits de l'opérateur source seront donc annulés.
	 * @param operateurSourceId le numéro d'individu de l'opérateur source.
	 * @param operateurTargetId le numéro d'individu de l'opérateur destination.
	 * @return les éventuels conflit rencontrés lors du transfert (les accès sans conflit ont été transférés, et les accès en conflit ont simplement été fermés sur l'opérateur source)
	 */
	List<DroitAccesConflit> transfereDroitsAcces(long operateurSourceId, long operateurTargetId);

	/**
	 * Annule un droit d'accès.
	 * @param id l'id du droit d'accès à annuler.
	 */
	void annuleDroitAcces(long id) throws DroitAccesException;

	/**
	 * Annule tous les droits d'accès pour un opérateur donné.
	 * @param noIndividuOperateur le numero d'individu de l'operateur dont les droits seront supprimés
	 */
	void annuleToutLesDroitAcces(long noIndividuOperateur);
}
