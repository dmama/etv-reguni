package ch.vd.uniregctb.webservices.tiers3.data.strategy;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.webservices.tiers3.PartyPart;
import ch.vd.unireg.webservices.tiers3.WebServiceException;
import ch.vd.unireg.xml.party.person.v1.CommonHousehold;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.webservices.tiers3.data.PartyBuilder;
import ch.vd.uniregctb.webservices.tiers3.impl.Context;

public class CommonHouseholdStrategy extends TaxPayerStrategy<CommonHousehold> {

	@Override
	public CommonHousehold newFrom(ch.vd.uniregctb.tiers.Tiers right, @Nullable Set<PartyPart> parts, Context context) throws WebServiceException {
		final CommonHousehold menage = new CommonHousehold();
		initBase(menage, right, context);
		initParts(menage, right, parts, context);
		return menage;
	}

	@Override
	public CommonHousehold clone(CommonHousehold right, @Nullable Set<PartyPart> parts) {
		final CommonHousehold menage = new CommonHousehold();
		copyBase(menage, right);
		copyParts(menage, right, parts, CopyMode.EXCLUSIVE);
		return menage;
	}

	@Override
	protected void initParts(CommonHousehold to, ch.vd.uniregctb.tiers.Tiers from, @Nullable Set<PartyPart> parts, Context context) throws WebServiceException {
		super.initParts(to, from, parts, context);

		final ch.vd.uniregctb.tiers.MenageCommun menage = (ch.vd.uniregctb.tiers.MenageCommun) from;
		if (parts != null && parts.contains(PartyPart.HOUSEHOLD_MEMBERS)) {
			initMembers(to, menage, context);
		}
	}

	@Override
	protected void copyParts(CommonHousehold to, CommonHousehold from, @Nullable Set<PartyPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(PartyPart.HOUSEHOLD_MEMBERS)) {
			to.setMainTaxpayer(from.getMainTaxpayer());
			to.setSecondaryTaxpayer(from.getSecondaryTaxpayer());
		}
	}

	private static void initMembers(CommonHousehold left, ch.vd.uniregctb.tiers.MenageCommun menageCommun, Context context) throws WebServiceException {
		EnsembleTiersCouple ensemble = context.tiersService.getEnsembleTiersCouple(menageCommun, null);
		final ch.vd.uniregctb.tiers.PersonnePhysique principal = ensemble.getPrincipal();
		if (principal != null) {
			left.setMainTaxpayer(PartyBuilder.newNaturalPerson(principal, null, context));
		}

		final ch.vd.uniregctb.tiers.PersonnePhysique conjoint = ensemble.getConjoint();
		if (conjoint != null) {
			left.setSecondaryTaxpayer(PartyBuilder.newNaturalPerson(conjoint, null, context));
		}
	}
}
