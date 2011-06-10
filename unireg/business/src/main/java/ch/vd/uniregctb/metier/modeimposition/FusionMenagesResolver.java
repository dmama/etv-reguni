package ch.vd.uniregctb.metier.modeimposition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Resolver du mode d'imposition pour le cas de reconstitution d'un ménage commun à partir de deux ménages communs incomplets
 * 
 * @author Pavel BLANCO
 *
 */
public class FusionMenagesResolver extends MariageModeImpositionResolver implements ModeImpositionResolver {

	private final MenageCommun premierMenage;
	private final MenageCommun secondMenage;
	
	public FusionMenagesResolver(TiersService tiersService, MenageCommun premierMenage, MenageCommun secondMenage) {
		super(tiersService, null);
		
		this.premierMenage = premierMenage;
		this.secondMenage = secondMenage;
	}

	@Override
	public Imposition resolve(Contribuable contribuable, RegDate date, ModeImposition imposition) throws ModeImpositionResolverException {
		final ForFiscalPrincipal forFPPrincipal = premierMenage.getForFiscalPrincipalAt(null);
		final ForFiscalPrincipal forFPConjoint = secondMenage.getForFiscalPrincipalAt(null);
		
		if (forFPPrincipal != null || forFPConjoint != null) {

			final ModeImposition impositionContribuable = (forFPPrincipal == null ? null : forFPPrincipal.getModeImposition());
			final ModeImposition impositionConjoint = (forFPConjoint == null ? null : forFPConjoint.getModeImposition());
			return internalResolveCouple(date, impositionContribuable, impositionConjoint);
		}
		
		return null;
	}

}
