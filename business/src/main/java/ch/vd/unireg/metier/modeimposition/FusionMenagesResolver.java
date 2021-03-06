package ch.vd.unireg.metier.modeimposition;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.ModeImposition;

/**
 * Resolver du mode d'imposition pour le cas de reconstitution d'un ménage commun à partir de deux ménages communs incomplets
 * 
 * @author Pavel BLANCO
 *
 */
public class FusionMenagesResolver extends MariageModeImpositionResolver {

	private final MenageCommun premierMenage;
	private final MenageCommun secondMenage;
	
	public FusionMenagesResolver(TiersService tiersService, MenageCommun premierMenage, MenageCommun secondMenage, AuditManager audit) {
		super(tiersService, null, audit);
		
		this.premierMenage = premierMenage;
		this.secondMenage = secondMenage;
	}

	@Override
	public Imposition resolve(ContribuableImpositionPersonnesPhysiques contribuable, RegDate date) throws ModeImpositionResolverException {
		final ForFiscalPrincipalPP forFPPrincipal = premierMenage.getForFiscalPrincipalAt(null);
		final ForFiscalPrincipalPP forFPConjoint = secondMenage.getForFiscalPrincipalAt(null);
		
		if (forFPPrincipal != null || forFPConjoint != null) {

			final ModeImposition impositionContribuable = (forFPPrincipal == null ? null : forFPPrincipal.getModeImposition());
			final ModeImposition impositionConjoint = (forFPConjoint == null ? null : forFPConjoint.getModeImposition());
			return internalResolveCouple(date, impositionContribuable, impositionConjoint);
		}
		
		return null;
	}

}
