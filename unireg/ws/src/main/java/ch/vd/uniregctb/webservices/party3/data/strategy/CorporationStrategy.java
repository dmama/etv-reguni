package ch.vd.uniregctb.webservices.party3.data.strategy;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.webservices.party3.PartyPart;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.party.corporation.v1.Corporation;
import ch.vd.uniregctb.webservices.party3.impl.Context;

public class CorporationStrategy extends TaxPayerStrategy<Corporation> {

	@Override
	public Corporation newFrom(ch.vd.uniregctb.tiers.Tiers right, @Nullable Set<PartyPart> parts, Context context) throws WebServiceException {
		throw new NotImplementedException(); // TODO (msi)
//		final PersonneMorale pm = new PersonneMorale();
//		initBase(pm, right, context);
//		initParts(pm, right, parts, context);
//		return pm;
	}

	@Override
	public Corporation clone(Corporation right, @Nullable Set<PartyPart> parts) {
		final Corporation pm = new Corporation();
		copyBase(pm, right);
		copyParts(pm, right, parts, CopyMode.EXCLUSIVE);
		return pm;
	}

	@Override
	protected void copyBase(Corporation to, Corporation from) {
		super.copyBase(to, from);
		to.setShortName(from.getShortName());
		to.setName1(from.getName1());
		to.setName2(from.getName2());
		to.setName3(from.getName3());
		to.setEndDateOfLastBusinessYear(from.getEndDateOfLastBusinessYear());
		to.setEndDateOfNextBusinessYear(from.getEndDateOfNextBusinessYear());
		to.setIpmroNumber(from.getIpmroNumber());
	}

	@Override
	protected void copyParts(Corporation to, Corporation from, @Nullable Set<PartyPart> parts, CopyMode mode) {
		super.copyParts(to, from, parts, mode);

		if (parts != null && parts.contains(PartyPart.LEGAL_SEATS)) {
			copyColl(to.getLegalSeats(), from.getLegalSeats());
		}

		if (parts != null && parts.contains(PartyPart.LEGAL_FORMS)) {
			copyColl(to.getLegalForms(), from.getLegalForms());
		}

		if (parts != null && parts.contains(PartyPart.CAPITALS)) {
			copyColl(to.getCapitals(), from.getCapitals());
		}

		if (parts != null && parts.contains(PartyPart.TAX_SYSTEMS)) {
			copyColl(to.getTaxSystemsVD(), from.getTaxSystemsVD());
			copyColl(to.getTaxSystemsCH(), from.getTaxSystemsCH());
		}

		if (parts != null && parts.contains(PartyPart.CORPORATION_STATUSES)) {
			copyColl(to.getStatuses(), from.getStatuses());
		}
	}
}
