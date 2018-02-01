package ch.vd.unireg.type;

/**
 * Type de traitement qui a présidé à la génération d'un état d'entreprise
 * (pourra avoir une influence sur ce qui est éditable à la main ou pas - hors SuperGRA, bien-sûr)
 */
public enum TypeGenerationEtatEntreprise {
	AUTOMATIQUE("Automatique"),
	MANUELLE("Manuelle");

	private final String libelle;

	TypeGenerationEtatEntreprise(String libelle) {
		this.libelle = libelle;
	}

	public String getLibelle() {
		return libelle;
	}
}
