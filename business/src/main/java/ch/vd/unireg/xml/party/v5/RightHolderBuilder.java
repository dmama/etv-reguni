package ch.vd.unireg.xml.party.v5;

import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.CollectivitePubliqueRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.CommunauteRFAppartenanceInfo;
import ch.vd.unireg.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.TiersRF;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.party.landregistry.v1.AdministrativeAuthorityIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.CorporationIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.NaturalPersonIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolderIdentity;

public abstract class RightHolderBuilder {
	private RightHolderBuilder() {
	}

	/**
	 * Interface fonctionnelle pour retourner le numéro de contibuable qui correspond à un tiers RF.
	 */
	@FunctionalInterface
	public interface ContribuableIdProvider extends Function<TiersRF, Long> {

	}

	public static RightHolder getRightHolder(@NotNull AyantDroitRF ayantDroit, @NotNull ContribuableIdProvider ctbIdProvider) {
		if (ayantDroit instanceof TiersRF) {
			final TiersRF tiersRF = (TiersRF) ayantDroit;
			return getRightHolder(tiersRF, ctbIdProvider);
		}
		else if (ayantDroit instanceof ImmeubleBeneficiaireRF) {
			final ImmeubleBeneficiaireRF beneficiaire = (ImmeubleBeneficiaireRF) ayantDroit;
			return new RightHolder(null, beneficiaire.getImmeuble().getId(), null, null, 0, null);
		}
		else if (ayantDroit instanceof CommunauteRF) {
			final CommunauteRF communaute = (CommunauteRF) ayantDroit;
			return new RightHolder(null, null, communaute.getId(), null, 0, null);
		}
		else {
			throw new IllegalArgumentException("Type d'ayant-droit illégal=[" + ayantDroit.getClass().getSimpleName() + "]");
		}
	}

	@NotNull
	public static RightHolder getRightHolder(@NotNull TiersRF tiersRF, @NotNull ContribuableIdProvider ctbIdProvider) {
		final Long ctbId = ctbIdProvider.apply(tiersRF);
		if (ctbId == null) {
			// le tiers n'est pas rapproché
			return new RightHolder(null, null, null, buildRightHolderIdentity(tiersRF), 0, null);
		}
		else {
			return new RightHolder(ctbId.intValue(), null, null, null, 0, null);
		}
	}

	@NotNull
	public static RightHolder getRightHolder(@NotNull Long ctbId) {
		return new RightHolder(ctbId.intValue(), null, null, null, 0, null);
	}

	@NotNull
	public static RightHolder getRightHolder(@NotNull CommunauteRFAppartenanceInfo appartenance) {
		final Long ctbId = appartenance.getCtbId();
		final TiersRF ayantDroit = appartenance.getAyantDroit();
		if (ctbId == null) {
			if (ayantDroit == null) {
				throw new IllegalArgumentException("L'id de CTB et l'ayant-droit sont tous les deux nuls.");
			}
			// le tiers n'est pas rapproché
			return new RightHolder(null, null, null, buildRightHolderIdentity(ayantDroit), 0, null);
		}
		else {
			return new RightHolder(ctbId.intValue(), null, null, null, 0, null);
		}
	}

	@NotNull
	public static RightHolderIdentity buildRightHolderIdentity(@NotNull TiersRF tiersRF) {
		final RightHolderIdentity holderIdentity;
		if (tiersRF instanceof PersonnePhysiqueRF) {
			final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) tiersRF;
			final NaturalPersonIdentity identity = new NaturalPersonIdentity();
			identity.setFirstName(pp.getPrenom());
			identity.setLastName(pp.getNom());
			identity.setDateOfBirth(DataHelper.coreToPartialDateXmlv2(pp.getDateNaissance()));
			holderIdentity = identity;
		}
		else if (tiersRF instanceof PersonneMoraleRF) {
			final PersonneMoraleRF pm = (PersonneMoraleRF) tiersRF;
			final CorporationIdentity identity = new CorporationIdentity();
			identity.setName(pm.getRaisonSociale());
			identity.setCommercialRegisterNumber(StringUtils.trimToEmpty(pm.getNumeroRC())); // le numéro RC est obligatoire sous peine de non-validation XML
			holderIdentity = identity;
		}
		else if (tiersRF instanceof CollectivitePubliqueRF) {
			final CollectivitePubliqueRF coll = (CollectivitePubliqueRF) tiersRF;
			final AdministrativeAuthorityIdentity identity = new AdministrativeAuthorityIdentity();
			identity.setName(coll.getRaisonSociale());
			holderIdentity = identity;
		}
		else {
			throw new IllegalArgumentException("Type de tiers RF inconnu = [" + tiersRF + "]");
		}
		return holderIdentity;
	}
}
