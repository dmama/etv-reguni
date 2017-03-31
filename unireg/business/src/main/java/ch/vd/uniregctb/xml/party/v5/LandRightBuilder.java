package ch.vd.uniregctb.xml.party.v5;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
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
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
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
	public static LandOwnershipRight newLandOwnershipRight(@NotNull DroitProprietePersonneRF droitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {
		final LandOwnershipRight right = new LandOwnershipRight();
		right.setDateFrom(DataHelper.coreToXMLv2(droitRF.getDateDebutMetier()));
		right.setDateTo(DataHelper.coreToXMLv2(droitRF.getDateFinMetier()));
		right.setType(EnumHelper.coreToXMLv5(droitRF.getRegime()));
		right.setStartReason(droitRF.getMotifDebut());
		right.setEndReason(droitRF.getMotifFin());
		right.setCommunityId(getCommunityId(droitRF.getCommunaute()));
		right.setShare(getShare(droitRF.getPart()));
		right.setCaseIdentifier(getFirstCaseIdentifier(droitRF));
		right.setRightHolder(RightHolderBuilder.getRightHolder(droitRF.getAyantDroit(), ctbIdProvider));
		right.setImmovablePropertyId(droitRF.getImmeuble().getId());
		right.getAcquisitionReasons().addAll(droitRF.getRaisonsAcquisition().stream()
				                                     .sorted()
				                                     .map(AcquisitionReasonBuilder::get)
				                                     .collect(Collectors.toList()));
		return right;
	}

	@NotNull
	public static UsufructRight newUsufructRight(@NotNull UsufruitRF usufruitRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {
		final UsufructRight right = new UsufructRight();
		fillEasementRight(usufruitRF, right, ctbIdProvider);
		return right;
	}

	@NotNull
	public static HousingRight newHousingRight(@NotNull DroitHabitationRF droitHabitationRF, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {
		final HousingRight right = new HousingRight();
		fillEasementRight(droitHabitationRF, right, ctbIdProvider);
		return right;
	}

	private static void fillEasementRight(@NotNull ServitudeRF servitude, EasementRight right, @NotNull RightHolderBuilder.ContribuableIdProvider ctbIdProvider) {

		right.setDateFrom(DataHelper.coreToXMLv2(servitude.getDateDebutMetier()));
		right.setDateTo(DataHelper.coreToXMLv2(servitude.getDateFinMetier()));
		right.setStartReason(servitude.getMotifDebut());
		right.setEndReason(servitude.getMotifFin());
		right.setCaseIdentifier(getCaseIdentifier(servitude.getNumeroAffaire()));

		final List<RightHolder> rightHolders = servitude.getAyantDroits().stream()
				.sorted(Comparator.comparing(AyantDroitRF::getId))
				.map(r -> RightHolderBuilder.getRightHolder(r, ctbIdProvider))
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
