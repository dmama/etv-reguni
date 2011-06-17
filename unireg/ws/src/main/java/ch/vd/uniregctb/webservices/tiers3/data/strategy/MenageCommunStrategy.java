package ch.vd.uniregctb.webservices.tiers3.data.strategy;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.webservices.tiers3.MenageCommun;
import ch.vd.unireg.webservices.tiers3.TiersPart;
import ch.vd.unireg.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.webservices.tiers3.data.TiersBuilder;
import ch.vd.uniregctb.webservices.tiers3.impl.Context;

public class MenageCommunStrategy extends ContribuableStrategy<MenageCommun> {

	@Override
	public MenageCommun newFrom(ch.vd.uniregctb.tiers.Tiers right, @Nullable Set<TiersPart> parts, Context context) throws WebServiceException {
		final MenageCommun menage = new MenageCommun();
		initBase(menage, right, context);
		initParts(menage, right, parts, context);
		return menage;
	}

	@Override
	public MenageCommun clone(MenageCommun right, @Nullable Set<TiersPart> parts) {
		final MenageCommun menage = new MenageCommun();
		copyBase(menage, right);
		copyParts(menage, right, parts, CopyMode.EXCLUSIF);
		return menage;
	}

	@Override
	protected void initParts(MenageCommun to, ch.vd.uniregctb.tiers.Tiers from, @Nullable Set<TiersPart> parts, Context context) throws WebServiceException {
		super.initParts(to, from, parts, context);

		final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) from;
		if (parts != null && parts.contains(TiersPart.COMPOSANTS_MENAGE)) {
			initComposants(to, menage, context);
		}
	}

	@Override
	protected void copyParts(MenageCommun to, MenageCommun from, @Nullable Set<TiersPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(TiersPart.COMPOSANTS_MENAGE)) {
			to.setContribuablePrincipal(from.getContribuablePrincipal());
			to.setContribuableSecondaire(from.getContribuableSecondaire());
		}
	}

	private static void initComposants(MenageCommun left, ch.vd.uniregctb.tiers.MenageCommun menageCommun, Context context) throws WebServiceException {
		EnsembleTiersCouple ensemble = context.tiersService.getEnsembleTiersCouple(menageCommun, null);
		final ch.vd.uniregctb.tiers.PersonnePhysique principal = ensemble.getPrincipal();
		if (principal != null) {
			left.setContribuablePrincipal(TiersBuilder.newPersonnePhysique(principal, null, context));
		}

		final ch.vd.uniregctb.tiers.PersonnePhysique conjoint = ensemble.getConjoint();
		if (conjoint != null) {
			left.setContribuableSecondaire(TiersBuilder.newPersonnePhysique(conjoint, null, context));
		}
	}
}
