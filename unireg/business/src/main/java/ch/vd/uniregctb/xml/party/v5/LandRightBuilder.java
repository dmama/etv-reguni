package ch.vd.uniregctb.xml.party.v5;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.landregistry.v1.CaseIdentifier;
import ch.vd.unireg.xml.party.landregistry.v1.HousingRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.Share;
import ch.vd.unireg.xml.party.landregistry.v1.UsufructRight;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

@SuppressWarnings("Duplicates")
public abstract class LandRightBuilder {

	/**
	 * Stratégie de création d'un droit web à part d'un droit core.
	 */
	public interface Strategy<L extends LandRight> extends BiFunction<DroitRF, RightHolderBuilder.ContribuableIdProvider, L> {
	}

	private static final Map<Class, Strategy<?>> strategies = new HashMap<>();

	static {
		strategies.put(DroitProprieteCommunauteRF.class, (d, p) -> newLandOwnershipRight((DroitProprieteCommunauteRF) d));
		strategies.put(DroitProprietePersonneMoraleRF.class, (d, p) -> newLandOwnershipRight((DroitProprietePersonneMoraleRF) d, p));
		strategies.put(DroitProprietePersonnePhysiqueRF.class, (d, p) -> newLandOwnershipRight((DroitProprietePersonnePhysiqueRF) d, p));
		strategies.put(UsufruitRF.class, (d, p) -> newUsufructRight((UsufruitRF) d, p));
		strategies.put(DroitHabitationRF.class, (d, p) -> newHousingRight((DroitHabitationRF) d, p));
	}

	private LandRightBuilder() {
	}

	@NotNull
	public static LandRight newLandRight(@NotNull DroitRF droitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {
		final Strategy<?> strategy = strategies.get(droitRF.getClass());
		if (strategy == null) {
			throw new IllegalArgumentException("Le type de droit [" + droitRF.getClass() + "] est inconnu");
		}
		return strategy.apply(droitRF, ctbIdProvider);
	}

	@NotNull
	public static LandOwnershipRight newLandOwnershipRight(@NotNull DroitProprieteCommunauteRF droitRF) {
		throw new ProgrammingException("Les droits sur les communautés ne doivent pas être exposés, par design.");
	}

	@NotNull
	public static LandOwnershipRight newLandOwnershipRight(@NotNull DroitProprietePersonneMoraleRF droitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {
		final LandOwnershipRight right = new LandOwnershipRight();
		right.setDateFrom(DataHelper.coreToXMLv2(droitRF.getDateDebutMetier()));
		right.setDateTo(DataHelper.coreToXMLv2(droitRF.getDateFinMetier()));
		right.setType(EnumHelper.coreToXMLv5(droitRF.getRegime()));
		right.setStartReason(droitRF.getMotifDebut());
		right.setEndReason(droitRF.getMotifFin());
		right.setCommunityId(getCommunityId(droitRF.getCommunaute()));
		right.setShare(getShare(droitRF.getPart()));
		right.setCaseIdentifier(getLastCaseIdentifier(droitRF));
		right.setRightHolder(RightHolderBuilder.getRightHolder(droitRF.getAyantDroit(), ctbIdProvider));
		right.setImmovablePropertyId(droitRF.getImmeuble().getId());
		return right;
	}

	// Pour garder une compatiblité ascendante, on expose le dernier numéro d'affaire sur le droit lui-même
	private static CaseIdentifier getLastCaseIdentifier(@NotNull DroitProprieteRF droitRF) {
		return droitRF.getRaisonsAcquisition().stream()
				.max(Comparator.naturalOrder())
				.map(RaisonAcquisitionRF::getNumeroAffaire)
				.map(LandRightBuilder::getCaseIdentifier)
				.orElse(null);
	}

	@NotNull
	public static LandOwnershipRight newLandOwnershipRight(@NotNull DroitProprietePersonnePhysiqueRF droitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {
		final LandOwnershipRight right = new LandOwnershipRight();
		right.setDateFrom(DataHelper.coreToXMLv2(droitRF.getDateDebutMetier()));
		right.setDateTo(DataHelper.coreToXMLv2(droitRF.getDateFinMetier()));
		right.setType(EnumHelper.coreToXMLv5(droitRF.getRegime()));
		right.setStartReason(droitRF.getMotifDebut());
		right.setEndReason(droitRF.getMotifFin());
		right.setCommunityId(getCommunityId(droitRF.getCommunaute()));
		right.setShare(getShare(droitRF.getPart()));
		right.setCaseIdentifier(getLastCaseIdentifier(droitRF));
		right.setRightHolder(RightHolderBuilder.getRightHolder(droitRF.getAyantDroit(), ctbIdProvider));
		right.setImmovablePropertyId(droitRF.getImmeuble().getId());
		return right;
	}

	@NotNull
	public static UsufructRight newUsufructRight(@NotNull UsufruitRF usufruitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {
		final UsufructRight right = new UsufructRight();
		right.setDateFrom(DataHelper.coreToXMLv2(usufruitRF.getDateDebutMetier()));
		right.setDateTo(DataHelper.coreToXMLv2(usufruitRF.getDateFinMetier()));
		right.setStartReason(usufruitRF.getMotifDebut());
		right.setEndReason(usufruitRF.getMotifFin());
		right.setCommunityId(getCommunityId(usufruitRF.getCommunaute()));
		right.setCaseIdentifier(getCaseIdentifier(usufruitRF.getNumeroAffaire()));
		right.setRightHolder(RightHolderBuilder.getRightHolder(usufruitRF.getAyantDroit(), ctbIdProvider));
		right.setImmovablePropertyId(usufruitRF.getImmeuble().getId());
		return right;
	}

	@NotNull
	public static HousingRight newHousingRight(@NotNull DroitHabitationRF droitHabitationRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {
		final HousingRight right = new HousingRight();
		right.setDateFrom(DataHelper.coreToXMLv2(droitHabitationRF.getDateDebutMetier()));
		right.setDateTo(DataHelper.coreToXMLv2(droitHabitationRF.getDateFinMetier()));
		right.setStartReason(droitHabitationRF.getMotifDebut());
		right.setEndReason(droitHabitationRF.getMotifFin());
		right.setCommunityId(getCommunityId(droitHabitationRF.getCommunaute()));
		right.setCaseIdentifier(getCaseIdentifier(droitHabitationRF.getNumeroAffaire()));
		right.setRightHolder(RightHolderBuilder.getRightHolder(droitHabitationRF.getAyantDroit(), ctbIdProvider));
		right.setImmovablePropertyId(droitHabitationRF.getImmeuble().getId());
		return right;
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
		                          null,
		                          null,
		                          null,
		                          0,
		                          numeroAffaire.getNumeroAffaire(),
		                          0,
		                          null);
	}

}
