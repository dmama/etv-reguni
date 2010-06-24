package ch.vd.uniregctb.metier.modeimposition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Résolution du nouveau mode d'imposition pour le cas décès.
 *
 * @author Pavel BLANCO
 *
 */
public class DecesModeImpositionResolver extends TiersModeImpositionResolver {

	final private DivorceModeImpositionResolver resolver;

	public DecesModeImpositionResolver(TiersService tiersService, Long numeroEvenement) {
		super(tiersService);
		resolver = new DivorceModeImpositionResolver(tiersService, numeroEvenement);
	}

	public Imposition resolve(Contribuable survivant, RegDate date, ModeImposition impositionCouple) throws ModeImpositionResolverException {
		// la spec dit "comme séparation/divorce"
		return resolver.resolve(survivant, date, impositionCouple);
	}
}