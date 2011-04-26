package ch.vd.uniregctb.metier.modeimposition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Resolver du mode d'imposition pour le cas de reconstitution d'un ménage commun incomplet.
 * 
 * @author Pavel BLANCO
 *
 */
public class ReconstitutionMenageResolver extends MariageModeImpositionResolver implements ModeImpositionResolver {

	private MenageCommun menage;

	public ReconstitutionMenageResolver(TiersService tiersService, MenageCommun menage) {
		super(tiersService, null);
		
		this.menage = menage;
	}

	public Imposition resolve(Contribuable contribuable, RegDate date, ModeImposition imposition) throws ModeImpositionResolverException {
		final ForFiscalPrincipal forFPPrincipal = menage.getForFiscalPrincipalAt(null);
		final ForFiscalPrincipal forFPConjoint = contribuable.getForFiscalPrincipalAt(null);
		
		if (forFPPrincipal != null || forFPConjoint != null) {
			final ModeImposition impositionContribuable = (forFPPrincipal == null ? null : forFPPrincipal.getModeImposition());
			final ModeImposition impositionConjoint = (forFPConjoint == null ? null : forFPConjoint.getModeImposition());
			return internalResolveCouple(date, impositionContribuable, impositionConjoint);
		}
		return null;
	}

}
