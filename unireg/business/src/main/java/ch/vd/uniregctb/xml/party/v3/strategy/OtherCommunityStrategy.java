package ch.vd.uniregctb.xml.party.v3.strategy;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.othercomm.v1.OtherCommunity;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.EnumHelper;
import ch.vd.uniregctb.xml.ServiceException;

public class OtherCommunityStrategy extends TaxPayerStrategy<OtherCommunity> {

	@Override
	public OtherCommunity newFrom(ch.vd.uniregctb.tiers.Tiers right, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
		final OtherCommunity communaute = new OtherCommunity();
		initBase(communaute, right, context);
		initParts(communaute, right, parts, context);
		return communaute;
	}

	@Override
	public OtherCommunity clone(OtherCommunity right, @Nullable Set<PartyPart> parts) {
		final OtherCommunity communaute = new OtherCommunity();
		copyBase(communaute, right);
		copyParts(communaute, right, parts, CopyMode.EXCLUSIVE);
		return communaute;
	}

	@Override
	protected void initBase(OtherCommunity to, Tiers from, Context context) throws ServiceException {
		super.initBase(to, from, context);

		final AutreCommunaute communaute = (AutreCommunaute) from;
		to.setName(communaute.getNom());
		to.setLegalForm(EnumHelper.coreToXMLv1(communaute.getFormeJuridique()));
	}
}
