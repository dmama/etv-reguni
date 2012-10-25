package ch.vd.uniregctb.tiers;

/**
 * Les différentes natures des tiers. La nature d'un tiers correspondant à son type ({@link ch.vd.uniregctb.tiers.TypeTiers}) sauf pour les personnes physiques qui sont spécialisées en
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
