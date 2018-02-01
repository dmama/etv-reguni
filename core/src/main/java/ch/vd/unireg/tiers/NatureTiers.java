package ch.vd.unireg.tiers;

/**
 * Les différentes natures des tiers. La nature d'un tiers correspondant à son type ({@link ch.vd.unireg.tiers.TypeTiers}) sauf pour les personnes physiques qui sont spécialisées en
 * <i>habitants</i> et <i>non-habitants</i>.
 */
public enum NatureTiers {
	Habitant,
	NonHabitant,
	Entreprise,
	Etablissement,
	MenageCommun,
	AutreCommunaute,
	DebiteurPrestationImposable,
	CollectiviteAdministrative
}
