package ch.vd.uniregctb.metier.modeimposition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Classe abstraite de base des resolvers de mode d'imposition lors de la création d'un couple
 * (mariage, partenariat, réconciliation, fusion...)
 */
public abstract class CreationCoupleModeImpositionResolver extends ModeImpositionResolver {

	protected CreationCoupleModeImpositionResolver(TiersService tiersService) {
		super(tiersService);
	}

	/**
	 * Calcule du nouveau mode d'imposition pour le contribuable couple
	 *
	 * @param contribuable le contribuable
	 * @param date date à partir de laquelle le mode d'imposition sera appliqué
	 * @return le nouveau mode d'imposition
	 *
	 * @throws ModeImpositionResolverException en cas de problème
	 */
	public abstract Imposition resolve(Contribuable contribuable, RegDate date) throws ModeImpositionResolverException;

}
