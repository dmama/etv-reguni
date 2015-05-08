package ch.vd.uniregctb.migration.pm.mapping;

import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public interface IdMapping {

	class NonExistentMappingException extends RuntimeException {
		public NonExistentMappingException(String message) {
			super(message);
		}
	}

	/**
	 * Ajoute une entreprise dans le mapping
	 * @param regpm entreprise de RegPM
	 * @param unireg entreprise dans Unireg
	 * @throws NullPointerException si l'une des deux entités n'a pas d'identifiant
	 * @throws IllegalArgumentException si l'identifiant de RegPM donné était déjà mappé à une entreprise avec un autre identifiant
	 */
	void addEntreprise(RegpmEntreprise regpm, Entreprise unireg);

	/**
	 * Ajoute un établissement dans le mapping
	 * @param regpm établissement de RegPM
	 * @param unireg établissement dans Unireg
	 * @throws NullPointerException si l'une des deux entités n'a pas d'identifiant
	 * @throws IllegalArgumentException si l'identifiant de RegPM donné était déjà mappé à un établissement avec un autre identifiant
	 */
	void addEtablissement(RegpmEtablissement regpm, Etablissement unireg);

	/**
	 * Ajoute un individu dans le mapping
	 * @param regpm individu de RegPM
	 * @param unireg personne physique dans Unireg
	 * @throws NullPointerException si l'une des deux entités n'a pas d'identifiant
	 * @throws IllegalArgumentException si l'identifiant de RegPM donné était déjà mappé à une personne physique avec un autre identifiant
	 */
	void addIndividu(RegpmIndividu regpm, PersonnePhysique unireg);

	/**
	 * Récupération de l'identifiant Unireg à partir de l'identifiant RegPM d'une entreprise
	 * @param idRegpm identifiant RegPM
	 * @return l'identifiant de l'entreprise correspondante dans Unireg
	 * @throws NonExistentMappingException en cas de mapping inconnu
	 */
	long getIdUniregEntreprise(long idRegpm) throws NonExistentMappingException;

	/**
	 * Récupération de l'identifiant Unireg à partir de l'identifiant RegPM d'un établissement
	 * @param idRegpm identifiant RegPM
	 * @return l'identifiant de l'établissement correspondant dans Unireg
	 * @throws NonExistentMappingException en cas de mapping inconnu
	 */
	long getIdUniregEtablissement(long idRegpm) throws NonExistentMappingException;

	/**
	 * Récupération de l'identifiant Unireg à partir de l'identifiant RegPM d'un individu
	 * @param idRegpm identifiant RegPM
	 * @return l'identifiant de la personne physique correspondante dans Unireg
	 * @throws NonExistentMappingException en cas de mapping inconnu
	 */
	long getIdUniregIndividu(long idRegpm) throws NonExistentMappingException;

	/**
	 * @param idRegpm identifiant d'entreprise RegPM
	 * @return <code>true</code> si un mapping est connu pour l'entreprise RegPM identifiée
	 */
	boolean hasMappingForEntreprise(long idRegpm);

	/**
	 * @param idRegpm identifiant d'un établissement RegPM
	 * @return <code>true</code> si un mapping est connu pour l'établissement RegPM identifié
	 */
	boolean hasMappingForEtablissement(long idRegpm);

	/**
	 * @param idRegpm identifiant d'individu RegPM
	 * @return <code>true</code> si un mapping est connu pour l'individu RegPM identifié
	 */
	boolean hasMappingForIndividu(long idRegpm);
}
