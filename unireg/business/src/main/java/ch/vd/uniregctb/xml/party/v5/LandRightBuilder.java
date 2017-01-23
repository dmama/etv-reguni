package ch.vd.uniregctb.xml.party.v5;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.xml.party.landregistry.v1.CaseIdentifier;
import ch.vd.unireg.xml.party.landregistry.v1.HousingRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.Share;
import ch.vd.unireg.xml.party.landregistry.v1.UsufructRight;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

@SuppressWarnings("Duplicates")
public abstract class LandRightBuilder {

	/**
	 * Stratégie de création d'un droit web à part d'un droit core.
	 */
	public interface Strategy <L extends LandRight> extends Function<DroitRF, L> {
	}

	private static final Map<Class, Strategy<?>> strategies = new HashMap<>();

	static {
		strategies.put(DroitHabitationRF.class, d -> newHousingRight((DroitHabitationRF) d));
		strategies.put(DroitProprieteCommunauteRF.class, d -> newLandOwnershipRight((DroitProprieteCommunauteRF) d));
		strategies.put(DroitProprietePersonneMoraleRF.class, d -> newLandOwnershipRight((DroitProprietePersonneMoraleRF) d));
		strategies.put(DroitProprietePersonnePhysiqueRF.class, d -> newLandOwnershipRight((DroitProprietePersonnePhysiqueRF) d));
		strategies.put(UsufruitRF.class, d -> newUsufructRight((UsufruitRF) d));
	}

	private LandRightBuilder() {
	}

	@NotNull
	public static LandRight newLandRight(@NotNull DroitRF droitRF) {
		final Strategy<?> strategy = strategies.get(droitRF.getClass());
		if (strategy == null) {
			throw new IllegalArgumentException("Le type de droit [" + droitRF.getClass() + "] est inconnu");
		}
		return strategy.apply(droitRF);
	}

	@NotNull
	public static HousingRight newHousingRight(@NotNull DroitHabitationRF droitHabitationRF) {
		throw new NotImplementedException();
	}

	@NotNull
	public static LandOwnershipRight newLandOwnershipRight(@NotNull DroitProprieteCommunauteRF droitRF) {
		// TODO (msi) on ne devrait jamais recevoir de droit de ce type, car les communautés ne sont pas exposées par le WS -> supprimer cette méthode ?
		throw new NotImplementedException();
	}

	@NotNull
	public static LandOwnershipRight newLandOwnershipRight(@NotNull DroitProprietePersonneMoraleRF droitRF) {
		final LandOwnershipRight right = new LandOwnershipRight();
		right.setDateFrom(DataHelper.coreToXMLv2(droitRF.getDateDebutOfficielle()));
		right.setDateTo(null);  // on ne connaît pas la date de fin officielle
		right.setType(EnumHelper.coreToXMLv5(droitRF.getRegime()));
		right.setStartReason(droitRF.getMotifDebut());
		right.setEndReason(droitRF.getMotifFin());
		right.setCommunityId(getCommunityId(droitRF.getCommunaute()));
		right.setShare(getShare(droitRF.getPart()));
		right.setCaseIdentifier(getCaseIdentifier(droitRF.getNumeroAffaire()));
		right.setImmovablePropertyId(droitRF.getImmeuble().getId());
		return right;
	}

	@NotNull
	public static LandOwnershipRight newLandOwnershipRight(@NotNull DroitProprietePersonnePhysiqueRF droitRF) {
		final LandOwnershipRight right = new LandOwnershipRight();
		right.setDateFrom(DataHelper.coreToXMLv2(droitRF.getDateDebutOfficielle()));
		right.setDateTo(null);  // on ne connaît pas la date de fin officielle
		right.setType(EnumHelper.coreToXMLv5(droitRF.getRegime()));
		right.setStartReason(droitRF.getMotifDebut());
		right.setEndReason(droitRF.getMotifFin());
		right.setCommunityId(getCommunityId(droitRF.getCommunaute()));
		right.setShare(getShare(droitRF.getPart()));
		right.setCaseIdentifier(getCaseIdentifier(droitRF.getNumeroAffaire()));
		right.setImmovablePropertyId(droitRF.getImmeuble().getId());
		return right;
	}

	@NotNull
	public static UsufructRight newUsufructRight(@NotNull UsufruitRF usufruitRF) {
		throw new NotImplementedException();
	}

	private static Long getCommunityId(@Nullable CommunauteRF communaute) {
		return communaute == null ? null : communaute.getId();
	}

	@Nullable
	public static Share getShare(@Nullable Fraction part) {
		if (part == null) {
			return null;
		}

		return new Share(part.getNumerateur(), part.getDenominateur());
	}

	@Nullable
	private static CaseIdentifier getCaseIdentifier(@Nullable IdentifiantAffaireRF numeroAffaire) {
		if (numeroAffaire == null) {
			return null;
		}
		return new CaseIdentifier(numeroAffaire.getNumeroOffice(),
		                          numeroAffaire.getAnnee(),
		                          numeroAffaire.getNumero(),
		                          numeroAffaire.getIndex(),
		                          0,
		                          null);
	}

}
