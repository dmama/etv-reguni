package ch.vd.uniregctb.supergra;

/**
 * Les types d'entité pouvant être édités en mode SuperGra.
 */
public enum EntityType {
	Tiers(ch.vd.uniregctb.tiers.Tiers.class, "tiers"),
	ForFiscal(ch.vd.uniregctb.tiers.ForFiscal.class, "for fiscal"),
	AdresseTiers(ch.vd.uniregctb.adresse.AdresseTiers.class, "adresse"),
	Declaration(ch.vd.uniregctb.declaration.Declaration.class, "déclaration"),
	EtatDeclaration(ch.vd.uniregctb.declaration.EtatDeclaration.class, "état de déclaration"),
	DelaiDeclaration(ch.vd.uniregctb.declaration.DelaiDeclaration.class, "délai de déclaration"),
	RapportEntreTiers(ch.vd.uniregctb.tiers.RapportEntreTiers.class, "rapport entre tiers"),
	IdentificationPersonne(ch.vd.uniregctb.tiers.IdentificationPersonne.class, "identification de personne"),
	Periodicite(ch.vd.uniregctb.declaration.Periodicite.class, "périodicité"),
	SituationFamille(ch.vd.uniregctb.tiers.SituationFamille.class, "situation de famille"),
	ModeleDocument(ch.vd.uniregctb.declaration.ModeleDocument.class, "modèle de document"),
	PeriodeFiscale(ch.vd.uniregctb.declaration.PeriodeFiscale.class, "période fiscale");

	private final Class<?> hibernateClass;
	private final String displayName;

	EntityType(Class<?> hibernateClass, String displayName) {
		this.hibernateClass = hibernateClass;
		this.displayName = displayName;
	}

	public Class<?> getHibernateClass() {
		return hibernateClass;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static EntityType fromHibernateClass(Class<?> clazz) {
		for (EntityType e : EntityType.values()) {
			if (e.getHibernateClass() == clazz || e.getHibernateClass().isAssignableFrom(clazz)) {
				return e;
			}
		}
		return null;
	}
}
