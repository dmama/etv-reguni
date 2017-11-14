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
import ch.vd.unireg.xml.party.landregistry.v1.OwnershipType;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.unireg.xml.party.landregistry.v1.Share;
import ch.vd.unireg.xml.party.landregistry.v1.UsufructRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualInheritedLandRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualLandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualUsufructRight;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteVirtuelRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.DroitVirtuelHeriteRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;
import ch.vd.uniregctb.registrefoncier.ServitudeRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.registrefoncier.UsufruitVirtuelRF;
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
		strategies.put(DroitProprieteVirtuelRF.class, (d, p, c) -> newVirtualLandOwnershipRight((DroitProprieteVirtuelRF) d, p, c));
		strategies.put(UsufruitRF.class, (d, p, c) -> newUsufructRight((UsufruitRF) d, p, c));
		strategies.put(DroitHabitationRF.class, (d, p, c) -> newHousingRight((DroitHabitationRF) d, p, c));
		strategies.put(UsufruitVirtuelRF.class, (d, p, c) -> newVirtualUsufructRight((UsufruitVirtuelRF) d, p, c));
		strategies.put(DroitVirtuelHeriteRF.class, (d, p, c) -> newVirtualInheritedLandRight((DroitVirtuelHeriteRF) d, p, c));
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

	private static void fillLandRight(@NotNull DroitRF droitRF, LandRight right) {
		right.setDateFrom(DataHelper.coreToXMLv2(droitRF.getDateDebutMetier()));
		right.setDateTo(DataHelper.coreToXMLv2(droitRF.getDateFinMetier()));
		right.setStartReason(droitRF.getMotifDebut());
		right.setEndReason(droitRF.getMotifFin());
	}

	private static void fillLandOwnershipRight(@NotNull DroitProprieteRF droitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider, LandOwnershipRight right) {
		fillLandRight(droitRF, right);
		right.setId(droitRF.getId());
		right.setType(EnumHelper.coreToXMLv5(droitRF.getRegime()));
		right.setShare(getShare(droitRF.getPart()));
		right.setCaseIdentifier(getFirstCaseIdentifier(droitRF));
		right.setRightHolder(RightHolderBuilder.getRightHolder(droitRF.getAyantDroit(), ctbIdProvider));
		right.setImmovablePropertyId(droitRF.getImmeuble().getId());
		right.getAcquisitionReasons().addAll(droitRF.getRaisonsAcquisition().stream()
				                                     .filter(AnnulableHelper::nonAnnule)
				                                     .sorted()
				                                     .map(AcquisitionReasonBuilder::get)
				                                     .collect(Collectors.toList()));
	}

	private static VirtualLandOwnershipRight newVirtualLandOwnershipRight(@NotNull DroitProprieteVirtuelRF droitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider, @NotNull EasementRightHolderComparator rightHolderComparator) {
		final VirtualLandOwnershipRight right = new VirtualLandOwnershipRight();
		fillLandRight(droitRF, right);
		right.setRightHolder(RightHolderBuilder.getRightHolder(droitRF.getAyantDroit(), ctbIdProvider));
		right.setImmovablePropertyId(droitRF.getImmeuble().getId());
		right.getPath().addAll(droitRF.getChemin().stream()
				                        .map(d -> LandRightBuilder.newLandRight(d, ctbIdProvider, rightHolderComparator))
				                        .map(LandOwnershipRight.class::cast)
				                        .collect(Collectors.toList()));
		return right;
	}

	public static UsufructRight newUsufructRight(@NotNull UsufruitRF usufruitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider, @NotNull EasementRightHolderComparator rightHolderComparator) {
		final UsufructRight right = new UsufructRight();
		right.setId(usufruitRF.getId());
		fillEasementRight(usufruitRF, right, ctbIdProvider, rightHolderComparator);
		return right;
	}

	public static VirtualUsufructRight newVirtualUsufructRight(@NotNull UsufruitVirtuelRF usufruitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider, @NotNull EasementRightHolderComparator rightHolderComparator) {
		final VirtualUsufructRight right = new VirtualUsufructRight();
		fillLandRight(usufruitRF, right);
		right.setRightHolder(RightHolderBuilder.getRightHolder(usufruitRF.getAyantDroit(), ctbIdProvider));
		right.setImmovablePropertyId(usufruitRF.getImmeuble().getId());
		right.getPath().addAll(usufruitRF.getChemin().stream()
				                       .map(d -> LandRightBuilder.newLandRight(d, ctbIdProvider, rightHolderComparator))
				                       .collect(Collectors.toList()));
		return right;
	}

	public static HousingRight newHousingRight(@NotNull DroitHabitationRF droitHabitationRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider, @NotNull EasementRightHolderComparator rightHolderComparator) {
		final HousingRight right = new HousingRight();
		right.setId(droitHabitationRF.getId());
		fillEasementRight(droitHabitationRF, right, ctbIdProvider, rightHolderComparator);
		return right;
	}

	private static VirtualInheritedLandRight newVirtualInheritedLandRight(@NotNull DroitVirtuelHeriteRF droitVirtuel, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider, @NotNull EasementRightHolderComparator rightHolderComparator) {

		final LandRight reference = newLandRight(droitVirtuel.getReference(), ctbIdProvider, rightHolderComparator);
		final boolean communauteImplicit = droitVirtuel.getNombreHeritiers() > 1;
		final OwnershipType ownershipTypeOverride = (communauteImplicit ? OwnershipType.COLLECTIVE_OWNERSHIP : null);   // SIFISC-24999 (voir remarque de Carbo du 24.10.2017)

		final VirtualInheritedLandRight right = new VirtualInheritedLandRight();
		fillLandRight(droitVirtuel, right);
		right.setRightHolder(RightHolderBuilder.getRightHolder(droitVirtuel.getHeritierId()));
		right.setImmovablePropertyId(reference.getImmovablePropertyId());
		right.setImplicitCommunity(communauteImplicit);
		right.setInheritedFromId(droitVirtuel.getDecedeId());
		right.setOwnershipTypeOverride(ownershipTypeOverride);
		right.setReference(reference);
		return right;
	}

	private static void fillEasementRight(@NotNull ServitudeRF servitude, EasementRight right, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider, @NotNull EasementRightHolderComparator rightHolderComparator) {

		fillLandRight(servitude, right);
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
		right.setRightHolder(rightHolders.isEmpty() ? null : rightHolders.get(0)); // SIFISC-26200
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
