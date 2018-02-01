package ch.vd.uniregctb.xml.party.v4.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.othercomm.v2.OtherCommunity;
import ch.vd.unireg.xml.party.v4.PartyPart;
import ch.vd.unireg.xml.party.v4.UidNumberList;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.EnumHelper;
import ch.vd.uniregctb.xml.ServiceException;

public class OtherCommunityStrategy extends TaxPayerStrategy<OtherCommunity> {

	@Override
	public OtherCommunity newFrom(Tiers right, @Nullable Set<PartyPart> parts, Context context) throws ServiceException {
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
		to.setLegalForm(EnumHelper.coreToXMLv2(communaute.getFormeJuridique()));

		// [SIFISC-11689] Exposition des numéros IDE
		final Set<IdentificationEntreprise> ides = communaute.getIdentificationsEntreprise();
		if (ides != null && !ides.isEmpty()) {
			final List<String> ideList = new ArrayList<>(ides.size());
			for (IdentificationEntreprise ide : ides) {
				ideList.add(ide.getNumeroIde());
			}
			to.setUidNumbers(new UidNumberList(ideList));
		}
	}

	@Override
	protected void copyBase(OtherCommunity to, OtherCommunity from) {
		super.copyBase(to, from);

		to.setName(from.getName());
		to.setLegalForm(from.getLegalForm());
		to.setUidNumbers(from.getUidNumbers());
	}
}
