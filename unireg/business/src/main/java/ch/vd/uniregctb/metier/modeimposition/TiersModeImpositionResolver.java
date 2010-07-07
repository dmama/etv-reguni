package ch.vd.uniregctb.metier.modeimposition;

import ch.vd.uniregctb.tiers.TiersService;

/**
 * Implementation du resolver de mode d'imposition utilisant le TiersService.
 * 
 * @author Pavel BLANCO
 *
 */
public abstract class TiersModeImpositionResolver implements ModeImpositionResolver {

	private TiersService tiersService;
	
	public TiersModeImpositionResolver(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	/**
	 * Retourne l'instance de TiersService utilisée par le resolver.
	 * 
	 * @return the tiersService
	 */
	public TiersService getTiersService() {
		return tiersService;
	}
}
