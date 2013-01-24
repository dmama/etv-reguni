package ch.vd.uniregctb.metier.modeimposition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Classe abstraite de base des resolvers de mode d'imposition lors de la terminaison d'un couple
 * (divorce, séparation, décès...)
 */
public abstract class TerminaisonCoupleModeImpositionResolver extends ModeImpositionResolver {

	protected TerminaisonCoupleModeImpositionResolver(TiersService tiersService) {
		super(tiersService);
	}

	/**
	 * Calcule du nouveau mode d'imposition pour le contribuable à la date et en prenant en compte son ancien mode d'imposition.
	 *
	 *
	 * @param contribuable le contribuable
	 * @param date date à partir de laquelle le mode d'imposition sera appliqué
	 * @param ancienModeImposition l'ancien mode d'imposition
	 * @param futurTypeAutoriteFiscale le type d'autorité fiscale du for qui sera ouvert
	 * @return le nouveau mode d'imposition
	 *
	 * @throws ModeImpositionResolverException en cas de souci
	 */
	public abstract Imposition resolve(Contribuable contribuable, RegDate date, ModeImposition ancienModeImposition, TypeAutoriteFiscale futurTypeAutoriteFiscale) throws ModeImpositionResolverException;

}
