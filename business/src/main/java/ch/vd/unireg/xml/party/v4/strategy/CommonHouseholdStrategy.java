package ch.vd.unireg.xml.party.v4.strategy;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.person.v4.CommonHousehold;
import ch.vd.unireg.xml.party.person.v4.CommonHouseholdStatus;
import ch.vd.unireg.xml.party.v4.PartyPart;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.EnumHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.party.v4.PartyBuilder;

public class CommonHouseholdStrategy extends TaxPayerStrategy<CommonHousehold> {

	@Override
	public CommonHousehold newFrom(ch.vd.unireg.tiers.Tiers right, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		final CommonHousehold menage = new CommonHousehold();
		initBase(menage, right, context);
		initParts(menage, right, parts, context);
		return menage;
	}

	@Override
	public CommonHousehold clone(CommonHousehold right, @Nullable Set<PartyPart> parts) {
		final CommonHousehold menage = new CommonHousehold();
		copyBase(menage, right);
		copyParts(menage, right, parts, ch.vd.unireg.xml.party.v4.strategy.CopyMode.EXCLUSIVE);
		return menage;
	}

	@Override
	protected void initParts(CommonHousehold to, ch.vd.unireg.tiers.Tiers from, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		super.initParts(to, from, parts, context);

		final ch.vd.unireg.tiers.MenageCommun menage = (ch.vd.unireg.tiers.MenageCommun) from;
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
			to.setStatus(from.getStatus());
		}
	}

	private static void initMembers(CommonHousehold left, ch.vd.unireg.tiers.MenageCommun menageCommun, Context context) throws ServiceException {
		EnsembleTiersCouple ensemble = context.tiersService.getEnsembleTiersCouple(menageCommun, null);
		final ch.vd.unireg.tiers.PersonnePhysique principal = ensemble.getPrincipal();
		if (principal != null) {
			left.setMainTaxpayer(PartyBuilder.newNaturalPerson(principal, null, context));
		}

		final ch.vd.unireg.tiers.PersonnePhysique conjoint = ensemble.getConjoint();
		if (conjoint != null) {
			left.setSecondaryTaxpayer(PartyBuilder.newNaturalPerson(conjoint, null, context));
		}

		left.setStatus(initStatus(ensemble, context)); // [SIFISC-6028]
	}

	/**
	 * [SIFISC-6028] Détermine le status du ménage commun.
	 *
	 * @param ensemble l'ensemble tiers-couple du ménage-commun.
	 * @param context  le context d'exécution
	 * @return le statut du ménage
	 */
	private static CommonHouseholdStatus initStatus(EnsembleTiersCouple ensemble, Context context) {
   		return EnumHelper.coreToXMLv4(context.tiersService.getStatutMenageCommun(ensemble.getMenage()));
	}

}
