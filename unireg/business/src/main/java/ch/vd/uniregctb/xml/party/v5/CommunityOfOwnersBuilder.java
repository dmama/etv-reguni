package ch.vd.uniregctb.xml.party.v5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.xml.party.landregistry.v1.AdministrativeAuthorityIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.CorporationIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.NaturalPersonIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.Owner;
import ch.vd.unireg.xml.party.landregistry.v1.OwnerIdentity;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRFMembreInfo;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public abstract class CommunityOfOwnersBuilder {

	private CommunityOfOwnersBuilder() {
	}

	public static CommunityOfOwners newCommunity(CommunauteRF communaute, Function<Long, CommunauteRFMembreInfo> membreInfoProvider) {

		final CommunauteRFMembreInfo membreInfo = membreInfoProvider.apply(communaute.getId());

		final CommunityOfOwners community = new CommunityOfOwners();
		community.setId(communaute.getId());
		community.setType(EnumHelper.coreToXMLv5(communaute.getType()));
		community.getMembers().addAll(buildMembers(membreInfo));
		return community;
	}

	@NotNull
	private static Collection<? extends Owner> buildMembers(@NotNull CommunauteRFMembreInfo membreInfo) {
		final List<Owner> membres = new ArrayList<>(membreInfo.getCount());
		membreInfo.getCtbIds().forEach(i -> membres.add(new Owner(i.intValue(), null, 0, null)));
		membreInfo.getTiersRF().forEach(t -> membres.add(new Owner(null, buildOwnerIdentity(t), 0, null)));
		return membres;
	}

	@NotNull
	private static OwnerIdentity buildOwnerIdentity(@NotNull TiersRF tiersRF) {
		final OwnerIdentity ownerIdentity;
		if (tiersRF instanceof PersonnePhysiqueRF) {
			final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) tiersRF;
			final NaturalPersonIdentity identity = new NaturalPersonIdentity();
			identity.setFirstName(pp.getPrenom());
			identity.setLastName(pp.getNom());
			identity.setDateOfBirth(DataHelper.coreToPartialDateXmlv2(pp.getDateNaissance()));
			ownerIdentity = identity;
		}
		else if (tiersRF instanceof PersonneMoraleRF) {
			final PersonneMoraleRF pm = (PersonneMoraleRF) tiersRF;
			final CorporationIdentity identity = new CorporationIdentity();
			identity.setName(pm.getRaisonSociale());
			identity.setCommercialRegisterNumber(pm.getNumeroRC());
			ownerIdentity = identity;
		}
		else if (tiersRF instanceof CollectivitePubliqueRF) {
			final CollectivitePubliqueRF coll = (CollectivitePubliqueRF) tiersRF;
			final AdministrativeAuthorityIdentity identity = new AdministrativeAuthorityIdentity();
			identity.setName(coll.getRaisonSociale());
			ownerIdentity = identity;
		}
		else {
			throw new IllegalArgumentException("Type de tiers RF inconnu = [" + tiersRF + "]");
		}
		return ownerIdentity;
	}
}
