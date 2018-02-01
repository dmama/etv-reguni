package ch.vd.unireg.tiers;

import org.jetbrains.annotations.NotNull;

/**
 * Types de tiers concrets existants.
 */
public enum TypeTiers {
	PERSONNE_PHYSIQUE("Personne physique", PersonnePhysique.class),
	MENAGE_COMMUN("Ménage commun", MenageCommun.class),
	ENTREPRISE("Entreprise", Entreprise.class),
	ETABLISSEMENT("Etablissement", Etablissement.class),
	COLLECTIVITE_ADMINISTRATIVE("Collectivité administrative", CollectiviteAdministrative.class),
	AUTRE_COMMUNAUTE("Autre communauté", AutreCommunaute.class),
	DEBITEUR_PRESTATION_IMPOSABLE("Débiteur prestation imposable", DebiteurPrestationImposable.class);

	@NotNull
	private final String description;
	@NotNull
	private final Class<? extends Tiers> concreteTiersClass;

	TypeTiers(@NotNull String description, @NotNull Class<? extends Tiers> concreteTiersClass) {
		this.description = description;
		this.concreteTiersClass = concreteTiersClass;
	}

	@NotNull
	public String getDescription() {
		return description;
	}

	@NotNull
	public Class<? extends Tiers> getConcreteTiersClass() {
		return concreteTiersClass;
	}
}
