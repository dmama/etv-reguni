package ch.vd.uniregctb.xml.party.v5;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.landregistry.v1.CaseIdentifier;
import ch.vd.unireg.xml.party.landregistry.v1.EasementRight;
import ch.vd.unireg.xml.party.landregistry.v1.HousingRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.unireg.xml.party.landregistry.v1.Share;
import ch.vd.unireg.xml.party.landregistry.v1.UsufructRight;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.ServitudeRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

@SuppressWarnings("Duplicates")
public abstract class LandRightBuilder {

	/**
	 * Stratégie de création d'un droit web à part d'un droit core.
	 */
	public interface Strategy<L extends LandRight> {
		L apply(@NotNull DroitRF droitRF,
		        @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider,
		        @NotNull EasementRightHolderComparator rightHolderComparator);
	}

	private static final Map<Class, Strategy<?>> strategies = new HashMap<>();

	static {
		strategies.put(DroitProprieteCommunauteRF.class, (d, p, c) -> newLandOwnershipRight((DroitProprieteCommunauteRF) d, p));
		strategies.put(DroitProprietePersonneMoraleRF.class, (d, p, c) -> newLandOwnershipRight((DroitProprietePersonneMoraleRF) d, p));
		strategies.put(DroitProprietePersonnePhysiqueRF.class, (d, p, c) -> newLandOwnershipRight((DroitProprietePersonnePhysiqueRF) d, p));
		strategies.put(DroitProprieteImmeubleRF.class, (d, p, c) -> newLandOwnershipRight((DroitProprieteImmeubleRF) d, p));
		strategies.put(UsufruitRF.class, (d, p, c) -> newUsufructRight((UsufruitRF) d, p, c));
		strategies.put(DroitHabitationRF.class, (d, p, c) -> newHousingRight((DroitHabitationRF) d, p, c));
	}

	private LandRightBuilder() {
	}

	public static LandRight newLandRight(@NotNull DroitRF droitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider, @NotNull EasementRightHolderComparator rightHolderComparator) {
		final Strategy<?> strategy = strategies.get(droitRF.getClass());
		if (strategy == null) {
			throw new IllegalArgumentException("Le type de droit [" + droitRF.getClass() + "] est inconnu");
		}
		return strategy.apply(droitRF, ctbIdProvider, rightHolderComparator);
	}

	@NotNull
	public static LandOwnershipRight newLandOwnershipRight(@NotNull DroitProprieteCommunauteRF droitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {
		final LandOwnershipRight right = new LandOwnershipRight();
		fillLandOwnershipRight(droitRF, ctbIdProvider, right);
		right.setCommunityId(null); // par définition, le droit de la communauté elle-même n'a pas cette valeur renseignée
		return right;
	}

	@NotNull
	public static LandOwnershipRight newLandOwnershipRight(@NotNull DroitProprietePersonneRF droitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {
		final LandOwnershipRight right = new LandOwnershipRight();
		fillLandOwnershipRight(droitRF, ctbIdProvider, right);
		right.setCommunityId(getCommunityId(droitRF.getCommunaute()));
		return right;
	}

	@NotNull
	public static LandOwnershipRight newLandOwnershipRight(@NotNull DroitProprieteImmeubleRF droitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {
		final LandOwnershipRight right = new LandOwnershipRight();
		fillLandOwnershipRight(droitRF, ctbIdProvider, right);
		return right;
	}

	private static void fillLandOwnershipRight(@NotNull DroitProprieteRF droitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider, LandOwnershipRight right) {
		right.setDateFrom(DataHelper.coreToXMLv2(droitRF.getDateDebutMetier()));
		right.setDateTo(DataHelper.coreToXMLv2(droitRF.getDateFinMetier()));
		right.setType(EnumHelper.coreToXMLv5(droitRF.getRegime()));
		right.setStartReason(droitRF.getMotifDebut());
		right.setEndReason(droitRF.getMotifFin());
		right.setShare(getShare(droitRF.getPart()));
		right.setCaseIdentifier(getFirstCaseIdentifier(droitRF));
		right.setRightHolder(RightHolderBuilder.getRightHolder(droitRF.getAyantDroit(), ctbIdProvider));
		right.setImmovablePropertyId(droitRF.getImmeuble().getId());
		right.getAcquisitionReasons().addAll(droitRF.getRaisonsAcquisition().stream()
				                                     .sorted()
				                                     .map(AcquisitionReasonBuilder::get)
				                                     .collect(Collectors.toList()));
	}

	public static UsufructRight newUsufructRight(@NotNull UsufruitRF usufruitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider, @NotNull EasementRightHolderComparator rightHolderComparator) {
		final UsufructRight right = new UsufructRight();
		fillEasementRight(usufruitRF, right, ctbIdProvider, rightHolderComparator);
		return right;
	}

	public static HousingRight newHousingRight(@NotNull DroitHabitationRF droitHabitationRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider, @NotNull EasementRightHolderComparator rightHolderComparator) {
		final HousingRight right = new HousingRight();
		fillEasementRight(droitHabitationRF, right, ctbIdProvider, rightHolderComparator);
		return right;
	}

	private static void fillEasementRight(@NotNull ServitudeRF servitude, EasementRight right, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider, @NotNull EasementRightHolderComparator rightHolderComparator) {

		right.setDateFrom(DataHelper.coreToXMLv2(servitude.getDateDebutMetier()));
		right.setDateTo(DataHelper.coreToXMLv2(servitude.getDateFinMetier()));
		right.setStartReason(servitude.getMotifDebut());
		right.setEndReason(servitude.getMotifFin());
		right.setCaseIdentifier(getCaseIdentifier(servitude.getNumeroAffaire()));

		final List<RightHolder> rightHolders = servitude.getAyantDroits().stream()
				.map(r -> RightHolderBuilder.getRightHolder(r, ctbIdProvider))
				.sorted(rightHolderComparator)
				.collect(Collectors.toList());
		right.getRightHolders().addAll(rightHolders);

		final List<Long> immovablePropIds = servitude.getImmeubles().stream()
				.sorted(Comparator.comparing(ImmeubleRF::getId))
				.map(ImmeubleRF::getId)
				.collect(Collectors.toList());
		right.getImmovablePropertyIds().addAll(immovablePropIds);

		// pour des raisons de compatibilité ascendante, on renseigne encore ces deux propriétés
		right.setRightHolder(rightHolders.get(0));
		right.setImmovablePropertyId(immovablePropIds.get(0));
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

	// Pour garder une compatiblité ascendante (et être cohérent avec la date de début), on expose le premier numéro d'affaire sur le droit lui-même
	private static CaseIdentifier getFirstCaseIdentifier(@NotNull DroitProprieteRF droitRF) {
		return droitRF.getRaisonsAcquisition().stream()
				.min(Comparator.naturalOrder())
				.map(RaisonAcquisitionRF::getNumeroAffaire)
				.map(LandRightBuilder::getCaseIdentifier)
				.orElse(null);
	}

	@Nullable
	public static CaseIdentifier getCaseIdentifier(@Nullable IdentifiantAffaireRF numeroAffaire) {
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
