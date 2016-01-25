package ch.vd.uniregctb.supergra;

/**
 * Les types d'entité pouvant être édités en mode SuperGra.
 */
@SuppressWarnings({"UnusedDeclaration"})
public enum EntityType {
	Tiers(ch.vd.uniregctb.tiers.Tiers.class, "tiers", "le tiers", "du tiers"),
	ForFiscal(ch.vd.uniregctb.tiers.ForFiscal.class, "for fiscal", "le for fiscal", "du for fiscal"),
	AdresseTiers(ch.vd.uniregctb.adresse.AdresseTiers.class, "adresse", "l'adresse", "de l'adresse"),
	Declaration(ch.vd.uniregctb.declaration.Declaration.class, "déclaration", "la déclaration", "de la déclaration"),
	EtatDeclaration(ch.vd.uniregctb.declaration.EtatDeclaration.class, "état de déclaration", "l'état de la déclaration", "de l'état de la déclaration"),
	DelaiDeclaration(ch.vd.uniregctb.declaration.DelaiDeclaration.class, "délai de déclaration", "le délai de la déclaration", "du délai de la déclaration"),
	RapportEntreTiers(ch.vd.uniregctb.tiers.RapportEntreTiers.class, "rapport entre tiers", "le rapport entre tiers", "du rapport en tiers"),
	IdentificationPersonne(ch.vd.uniregctb.tiers.IdentificationPersonne.class, "identification de personne", "l'identification de personne", "de l'identification de personne"),
	IdentificationEntreprise(ch.vd.uniregctb.tiers.IdentificationEntreprise.class, "identification d'entreprise", "l'identification d'entreprise", "de l'identification d'entreprise"),
	Periodicite(ch.vd.uniregctb.declaration.Periodicite.class, "périodicité", "la périodicité", "de la périodicité"),
	SituationFamille(ch.vd.uniregctb.tiers.SituationFamille.class, "situation de famille", "la situation de famille", "de la situation de famille"),
	ModeleDocument(ch.vd.uniregctb.declaration.ModeleDocument.class, "modèle de document", "le modèle de document", "du modèle de document"),
	PeriodeFiscale(ch.vd.uniregctb.declaration.PeriodeFiscale.class, "période fiscale", "la période fiscale", "de la période fiscale"),
	Immeuble(ch.vd.uniregctb.rf.Immeuble.class, "immeuble", "l'immeuble", "de l'immeuble"),
	Remarque(ch.vd.uniregctb.tiers.Remarque.class, "remarque", "la remarque", "de la remarque"),
	MouvementDossier(ch.vd.uniregctb.mouvement.MouvementDossier.class, "mouvement de dossier", "le mouvement de dossier", "du mouvement de dossier"),
	DecisionAci(ch.vd.uniregctb.tiers.DecisionAci.class,"décision ACI", "la décision ACI","de la décision ACI");

	private final Class<?> hibernateClass;
	private final String displayName;   // "chat"
	private final String displayArticleName; // "le chat"
	private final String displayPrepositionName; // "du chat"

	EntityType(Class<?> hibernateClass, String displayName, String displayArticleName, String displayPrepositionName) {
		this.hibernateClass = hibernateClass;
		this.displayName = displayName;
		this.displayArticleName = displayArticleName;
		this.displayPrepositionName = displayPrepositionName;
	}

	public Class<?> getHibernateClass() {
		return hibernateClass;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDisplayArticleName() {
		return displayArticleName;
	}

	public String getDisplayPrepositionName() {
		return displayPrepositionName;
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
