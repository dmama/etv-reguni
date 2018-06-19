package ch.vd.unireg.scheduler;

/**
 * Différentes catégories dans les jobs
 */
public enum JobCategory {

	TEST("Test"),
	DEBUG("Debug"),

	INDEXEUR("Indexeur"),
	OID("OID"),
	RF("Registre foncier"),
	TIERS("Tiers"),
	TACHE("Tâches"),
	IDENTIFICATION("Identification"),
	EVENTS("Evénements"),
	LR("Listes récapitulatives"),
	DI_PP("Déclarations d'impôt (PP)"),
	DI_PM("Déclarations d'impôt (PM)"),
	DI("Déclarations d'impôt (PP & PM)"),
	DD("Demandes de dégrèvement"),
	QSNC("Questionnaires SNC"),
	IMPORT_LIENS_TIERS_SNC("Import les rapports entre associés-SNC"),
	AUTRES_DOCUMENTS("Autres documents fiscaux"),
	FORS("Fors fiscaux"),
	STATS("Statistiques / Extractions"),
	DB("Base de données"),
	CACHE("Cache");

	private final String displayName;

	JobCategory(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}
