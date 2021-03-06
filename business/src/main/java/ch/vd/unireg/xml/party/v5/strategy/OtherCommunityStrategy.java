package ch.vd.unireg.xml.party.v5.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.IdentificationEntreprise;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.EnumHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.party.othercomm.v3.OtherCommunity;
import ch.vd.unireg.xml.party.v5.InternalPartyPart;
import ch.vd.unireg.xml.party.v5.UidNumberList;

public class OtherCommunityStrategy extends TaxPayerStrategy<OtherCommunity> {

	@Override
	public OtherCommunity newFrom(Tiers right, @Nullable Set<InternalPartyPart> parts, Context context) throws ServiceException {
		final OtherCommunity communaute = new OtherCommunity();
		initBase(communaute, right, context);
		initParts(communaute, right, parts, context);
		return communaute;
	}

	@Override
	public OtherCommunity clone(OtherCommunity right, @Nullable Set<InternalPartyPart> parts) {
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
		to.setLegalForm(EnumHelper.coreToXMLv5(communaute.getFormeJuridique()));

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
