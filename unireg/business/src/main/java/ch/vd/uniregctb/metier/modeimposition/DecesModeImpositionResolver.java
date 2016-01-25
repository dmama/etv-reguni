package ch.vd.uniregctb.metier.modeimposition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Résolution du nouveau mode d'imposition pour le cas décès.
 *
 * @author Pavel BLANCO
 *
 */
public class DecesModeImpositionResolver extends TerminaisonCoupleModeImpositionResolver {

	private final DivorceModeImpositionResolver resolver;

	public DecesModeImpositionResolver(TiersService tiersService, Long numeroEvenement) {
		super(tiersService);
		resolver = new DivorceModeImpositionResolver(tiersService, numeroEvenement);
	}

	@Override
	public Imposition resolve(Contribuable survivant, RegDate date, ModeImposition impositionCouple, TypeAutoriteFiscale futurTypeAutoriteFiscale, boolean hadForSecondaire) throws ModeImpositionResolverException {
		// la spec dit "comme séparation/divorce"
		return resolver.resolve(survivant, date, impositionCouple, futurTypeAutoriteFiscale, hadForSecondaire);
	}
}