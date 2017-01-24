package ch.vd.uniregctb.xml.party.v5;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.xml.party.landregistry.v1.AdministrativeAuthorityIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.CorporationIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.NaturalPersonIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.Owner;
import ch.vd.unireg.xml.party.landregistry.v1.OwnerIdentity;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.xml.DataHelper;

public abstract class OwnerBuilder {
	private OwnerBuilder() {
	}

	public interface ContribuableIdProvider extends Function<TiersRF, Long> {

	}
	public static Owner getOwner(@NotNull AyantDroitRF ayantDroit, @NotNull ContribuableIdProvider ctbIdProvider) {
		if (ayantDroit instanceof CommunauteRF) {
			throw new IllegalArgumentException("On ne devrait pas recevoir de communauté");
		}
		final TiersRF tiersRF = (TiersRF) ayantDroit;
		return getOwner(tiersRF, ctbIdProvider);
	}

	@NotNull
	public static Owner getOwner(@NotNull TiersRF tiersRF, @NotNull ContribuableIdProvider ctbIdProvider) {
		final Long ctbId = ctbIdProvider.apply(tiersRF);
		if (ctbId == null) {
			// le tiers n'est pas rapproché
			return new Owner(null, buildOwnerIdentity(tiersRF), 0, null);
		}
		else {
			return new Owner(ctbId.intValue(), null, 0, null);
		}
	}

	@NotNull
	public static OwnerIdentity buildOwnerIdentity(@NotNull TiersRF tiersRF) {
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
