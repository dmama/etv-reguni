package ch.vd.unireg.xml.party.v5;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CapitastraURLProvider;
import ch.vd.unireg.registrefoncier.ChargeServitudeRF;
import ch.vd.unireg.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.DroitRFRangeMetierComparator;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.MineRF;
import ch.vd.unireg.registrefoncier.PartCoproprieteRF;
import ch.vd.unireg.registrefoncier.ProprieteParEtageRF;
import ch.vd.unireg.registrefoncier.QuotePartRF;
import ch.vd.unireg.registrefoncier.ServitudeHelper;
import ch.vd.unireg.registrefoncier.ServitudeRF;
import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.registrefoncier.SurfaceAuSolRF;
import ch.vd.unireg.registrefoncier.SurfaceTotaleRF;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.party.landregistry.v1.CoOwnershipShare;
import ch.vd.unireg.xml.party.landregistry.v1.CondominiumOwnership;
import ch.vd.unireg.xml.party.landregistry.v1.DatedShare;
import ch.vd.unireg.xml.party.landregistry.v1.DistinctAndPermanentRight;
import ch.vd.unireg.xml.party.landregistry.v1.GroundArea;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.Location;
import ch.vd.unireg.xml.party.landregistry.v1.Mine;
import ch.vd.unireg.xml.party.landregistry.v1.RealEstate;
import ch.vd.unireg.xml.party.landregistry.v1.TaxEstimate;
import ch.vd.unireg.xml.party.landregistry.v1.TotalArea;

public abstract class ImmovablePropertyBuilder {

	private ImmovablePropertyBuilder() {
	}

	@FunctionalInterface
	public interface Strategy<I extends ImmovableProperty> {
		/**
		 * Stratégie de création d'un immeuble web à part d'un immeuble core.
		 */
		I apply(@NotNull ImmeubleRF i,
		        @NotNull CapitastraURLProvider capitastraURLProvider,
		        @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider,
		        @NotNull EasementRightHolderComparator rightHolderComparator);
	}

	private static final Map<Class, Strategy<?>> strategies = new HashMap<>();

	static {
		strategies.put(ProprieteParEtageRF.class, (i, u, p, c) -> newCondominiumOwnership((ProprieteParEtageRF) i, u, p, c));
		strategies.put(PartCoproprieteRF.class, (i, u, p, c) -> newCoOwnershipShare((PartCoproprieteRF) i, u, p, c));
		strategies.put(DroitDistinctEtPermanentRF.class, (i, u, p, c) -> newDistinctAndPermanentRight((DroitDistinctEtPermanentRF) i, u, p, c));
		strategies.put(MineRF.class, (i, u, p, c) -> newMine((MineRF) i, u, p, c));
		strategies.put(BienFondsRF.class, (i, u, p, c) -> newRealEstate((BienFondsRF) i, u, p, c));
	}

	public static ImmovableProperty newImmovableProperty(@NotNull ImmeubleRF immeuble, @NotNull CapitastraURLProvider capitastraUrlProvider, RightHolderBuilder.ContribuableIdProvider contribuableIdProvider,
	                                                     @NotNull EasementRightHolderComparator rightHolderComparator) {
		final Strategy<?> strategy = strategies.get(immeuble.getClass());
		if (strategy == null) {
			throw new IllegalArgumentException("Le type d'immeuble [" + immeuble.getClass() + "] est inconnu");
		}
		return strategy.apply(immeuble, capitastraUrlProvider, contribuableIdProvider, rightHolderComparator);
	}

	private static CondominiumOwnership newCondominiumOwnership(@NotNull ProprieteParEtageRF ppe, @NotNull CapitastraURLProvider capitastraUrlProvider, @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider,
	                                                            @NotNull EasementRightHolderComparator rightHolderComparator) {
		final CondominiumOwnership condo = new CondominiumOwnership();
		fillBase(condo, ppe, capitastraUrlProvider, contribuableIdProvider, rightHolderComparator);
		// [SIFISC-24715] on expose la quote-part courante (pour des raisons de compatibilité ascendante du WS)
		ppe.getQuotesParts().stream()
				.filter(q -> q.isValidAt(null))
				.findFirst()
				.map(QuotePartRF::getQuotePart)
				.map(LandRightBuilder::getShare)
				.ifPresent(condo::setShare);
		// [SIFISC-24715] on expose l'historique des quotes-parts
		condo.getShares().addAll(ppe.getQuotesParts().stream()
				                         .filter(AnnulableHelper::nonAnnule)
				                         .sorted(new DateRangeComparator<>())
				                         .map(ImmovablePropertyBuilder::newShare)
				                         .collect(Collectors.toList()));
		return condo;
	}

	private static CoOwnershipShare newCoOwnershipShare(@NotNull PartCoproprieteRF part, @NotNull CapitastraURLProvider capitastraUrlProvider, @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider,
	                                                    @NotNull EasementRightHolderComparator rightHolderComparator) {
		final CoOwnershipShare coown = new CoOwnershipShare();
		fillBase(coown, part, capitastraUrlProvider, contribuableIdProvider, rightHolderComparator);
		// [SIFISC-24715] on expose la quote-part courante (pour des raisons de compatibilité ascendante du WS)
		part.getQuotesParts().stream()
				.filter(q -> q.isValidAt(null))
				.findFirst()
				.map(QuotePartRF::getQuotePart)
				.map(LandRightBuilder::getShare)
				.ifPresent(coown::setShare);
		// [SIFISC-24715] on expose l'historique des quotes-parts
		coown.getShares().addAll(part.getQuotesParts().stream()
				                         .filter(AnnulableHelper::nonAnnule)
				                         .sorted(new DateRangeComparator<>())
				                         .map(ImmovablePropertyBuilder::newShare)
				                         .collect(Collectors.toList()));
		return coown;
	}

	private static DistinctAndPermanentRight newDistinctAndPermanentRight(@NotNull DroitDistinctEtPermanentRF ddp, @NotNull CapitastraURLProvider capitastraUrlProvider, @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider,
	                                                                      @NotNull EasementRightHolderComparator rightHolderComparator) {
		final DistinctAndPermanentRight dpr = new DistinctAndPermanentRight();
		fillBase(dpr, ddp, capitastraUrlProvider, contribuableIdProvider, rightHolderComparator);
		return dpr;
	}

	private static Mine newMine(@NotNull MineRF mine, @NotNull CapitastraURLProvider capitastraUrlProvider, @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider,
	                            @NotNull EasementRightHolderComparator rightHolderComparator) {
		final Mine m = new Mine();
		fillBase(m, mine, capitastraUrlProvider, contribuableIdProvider, rightHolderComparator);
		return m;
	}

	private static RealEstate newRealEstate(@NotNull BienFondsRF bienFonds, @NotNull CapitastraURLProvider capitastraUrlProvider, @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider,
	                                        @NotNull EasementRightHolderComparator rightHolderComparator) {
		final RealEstate estate = new RealEstate();
		fillBase(estate, bienFonds, capitastraUrlProvider, contribuableIdProvider, rightHolderComparator);
		estate.setCfa(bienFonds.isCfa());
		return estate;
	}

	private static void fillBase(ImmovableProperty property, @NotNull ImmeubleRF immeuble, @NotNull CapitastraURLProvider capitastraUrlProvider, @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider,
	                             @NotNull EasementRightHolderComparator rightHolderComparator) {
		property.setId(immeuble.getId());
		property.setEgrid(immeuble.getEgrid());
		property.setUrlIntercapi(capitastraUrlProvider.apply(immeuble.getId()));
		property.setCancellationDate(DataHelper.coreToXMLv2(immeuble.getDateRadiation()));

		property.getLocations().addAll(immeuble.getSituations().stream()
				                               .filter(AnnulableHelper::nonAnnule)
				                               .sorted()
				                               .map(ImmovablePropertyBuilder::newLocation)
				                               .collect(Collectors.toList()));
		property.getTaxEstimates().addAll(immeuble.getEstimations().stream()
				                                  .filter(AnnulableHelper::nonAnnule)
				                                  .sorted()
				                                  .map(ImmovablePropertyBuilder::newTaxEstimate)
				                                  .collect(Collectors.toList()));
		property.getTotalAreas().addAll(immeuble.getSurfacesTotales().stream()
				                                .filter(AnnulableHelper::nonAnnule)
				                                .sorted()
				                                .map(ImmovablePropertyBuilder::newTotalArea)
				                                .collect(Collectors.toList()));
		property.getGroundAreas().addAll(immeuble.getSurfacesAuSol().stream()
				                                 .filter(AnnulableHelper::nonAnnule)
				                                 .sorted()
				                                 .map(ImmovablePropertyBuilder::newGroundArea)
				                                 .collect(Collectors.toList()));
		property.getBuildingSettings().addAll(immeuble.getImplantations().stream()
				                                      .filter(AnnulableHelper::nonAnnule)
				                                      .sorted()
				                                      .map(BuildingBuilder::newBuildSetting)
				                                      .collect(Collectors.toList()));

		final Stream<DroitRF> droits = Stream.concat(immeuble.getDroitsPropriete().stream(),
		                                             immeuble.getChargesServitudes().stream()
				                                             .map(ImmovablePropertyBuilder::getAdaptedServitude));
		property.getLandRights().addAll(buildLandRights(droits, contribuableIdProvider, rightHolderComparator));

		final ImmeubleBeneficiaireRF beneficiaire = immeuble.getEquivalentBeneficiaire();
		if (beneficiaire != null) {
			property.getLandRightsFrom().addAll(buildLandRights(beneficiaire.getDroitsPropriete().stream(), contribuableIdProvider, rightHolderComparator));
		}
	}

	/**
	 * [IMM-795] Adapte la validité de la servitude à celle de la charge de l'immeuble
	 *
	 * @param charge une charge de servitude
	 * @return la servitude la charge adaptée à la durée de validité de la charge
	 */
	private static ServitudeRF getAdaptedServitude(@NotNull ChargeServitudeRF charge) {
		final ServitudeRF servitude = charge.getServitude();
		return ServitudeHelper.adapteServitude(servitude, charge);
	}

	private static List<LandRight> buildLandRights(Stream<? extends DroitRF> droits, @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider, @NotNull EasementRightHolderComparator rightHolderComparator) {
		return droits
				.filter(AnnulableHelper::nonAnnule)
				// on n'expose pas les droits des communautés (c'est les droits des personnes membres des communautés qui portent l'information)
				// [SIFISC-24457] on expose bien les droits sur les communautés dorénavant
				// .filter(d -> !(d instanceof DroitProprieteCommunauteRF))
				.sorted(new DroitRFRangeMetierComparator())
				.map(d -> LandRightBuilder.newLandRight(d, contribuableIdProvider, rightHolderComparator))
				.collect(Collectors.toList());
	}

	@NotNull
	public static Location newLocation(@NotNull SituationRF situation) {
		final Location loc = new Location();
		loc.setDateFrom(DataHelper.coreToXMLv2(situation.getDateDebut()));
		loc.setDateTo(DataHelper.coreToXMLv2(situation.getDateFin()));
		loc.setParcelNumber(situation.getNoParcelle());
		loc.setIndex1(situation.getIndex1());
		loc.setIndex2(situation.getIndex2());
		loc.setIndex3(situation.getIndex3());
		loc.setMunicipalityFsoId(situation.getNoOfsCommune());
		return loc;
	}

	@NotNull
	private static TaxEstimate newTaxEstimate(@NotNull EstimationRF e) {
		final TaxEstimate estimate = new TaxEstimate();
		estimate.setDateFrom(DataHelper.coreToXMLv2(e.getDateDebutMetier()));   // SIFISC-22995 : on expose les dates métier
		estimate.setDateTo(DataHelper.coreToXMLv2(e.getDateFinMetier()));
		estimate.setAmount(e.getMontant());
		estimate.setReference(e.getReference());  // SIFISC-23612 : on expose bien la référence fiscale originale (et non pas celle nettoyée)
		estimate.setRegistrationDate(DataHelper.coreToXMLv2(e.getDateInscription()));
		estimate.setInReview(e.isEnRevision());
		return estimate;
	}

	@NotNull
	private static TotalArea newTotalArea(@NotNull SurfaceTotaleRF surface) {
		final TotalArea area = new TotalArea();
		area.setDateFrom(DataHelper.coreToXMLv2(surface.getDateDebut()));
		area.setDateTo(DataHelper.coreToXMLv2(surface.getDateFin()));
		area.setArea(surface.getSurface());
		return area;
	}

	@NotNull
	private static GroundArea newGroundArea(@NotNull SurfaceAuSolRF surface) {
		final GroundArea area = new GroundArea();
		area.setDateFrom(DataHelper.coreToXMLv2(surface.getDateDebut()));
		area.setDateTo(DataHelper.coreToXMLv2(surface.getDateFin()));
		area.setArea(surface.getSurface());
		area.setType(surface.getType());
		return area;
	}

	@NotNull
	private static DatedShare newShare(@NotNull QuotePartRF quotePart) {
		final DatedShare share = new DatedShare();
		share.setNumerator(quotePart.getQuotePart().getNumerateur());
		share.setDenominator(quotePart.getQuotePart().getDenominateur());
		share.setDateFrom(DataHelper.coreToXMLv2(quotePart.getDateDebut()));
		share.setDateTo(DataHelper.coreToXMLv2(quotePart.getDateFin()));
		return share;
	}

}
