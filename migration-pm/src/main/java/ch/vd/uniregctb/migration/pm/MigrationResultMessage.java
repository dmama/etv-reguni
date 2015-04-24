package ch.vd.uniregctb.migration.pm;

public class MigrationResultMessage {

	private final Niveau niveau;
	private final String texte;

	public MigrationResultMessage(Niveau niveau, String texte) {
		this.niveau = niveau;
		this.texte = texte;
	}

	public Niveau getNiveau() {
		return niveau;
	}

	public String getTexte() {
		return texte;
	}

	/**
	 * Enumération des différentes listes de contrôle
	 * // TODO il y en a sûrement d'autres...
	 */
	public enum CategorieListe {

		/**
		 * Cas ok qui indique qu'une PM est migrée
		 */
		PM_MIGREE,

		/**
		 * Erreurs inattendues, par exemple, messages génériques en général (??)...
		 */
		GENERIQUE,

		/**
		 * Erreurs/messages liés à la migration des adresses
		 */
		ADRESSES,

		/**
		 * Erreurs/messages liés à la migration des individus PM
		 */
		INDIVIDUS_PM,

		/**
		 * Erreurs/messages liés à la migration des établissements
		 */
		ETABLISSEMENTS,

		/**
		 * Erreurs/messages liés à la migration des fors
		 */
		FORS
	}

	public enum Niveau {
		DEBUG,
		INFO,
		WARN,
		ERROR
	}
}
