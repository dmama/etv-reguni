package ch.vd.uniregctb.xml.party.v5;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.xml.party.landregistry.v1.CoOwnershipShare;
import ch.vd.unireg.xml.party.landregistry.v1.CondominiumOwnership;
import ch.vd.unireg.xml.party.landregistry.v1.DistinctAndPermanentRight;
import ch.vd.unireg.xml.party.landregistry.v1.GroundArea;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.landregistry.v1.Location;
import ch.vd.unireg.xml.party.landregistry.v1.Mine;
import ch.vd.unireg.xml.party.landregistry.v1.RealEstate;
import ch.vd.unireg.xml.party.landregistry.v1.TaxEstimate;
import ch.vd.unireg.xml.party.landregistry.v1.TotalArea;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.MineRF;
import ch.vd.uniregctb.registrefoncier.PartCoproprieteRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.registrefoncier.SurfaceTotaleRF;
import ch.vd.uniregctb.rf.Immeuble;
import ch.vd.uniregctb.rf.PartPropriete;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public abstract class ImmovablePropertyBuilder {

	private ImmovablePropertyBuilder() {
	}

	/**
	 * Permet de récupérer l'URL vers Capitastra à partir de l'id d'un immeuble.
	 */
	public interface CapitastraURLProvider extends Function<Long, String> {
	}

	@FunctionalInterface
	public interface Strategy<I extends ImmovableProperty>  {
		/**
		 * Stratégie de création d'un immeuble web à part d'un immeuble core.
		 */
		I apply(ImmeubleRF i, @NotNull CapitastraURLProvider capitastraURLProvider,  @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider);
	}

	private static final Map<Class, Strategy<?>> strategies = new HashMap<>();

	static {
		strategies.put(ProprieteParEtageRF.class, (i, p, c) -> newCondominiumOwnership((ProprieteParEtageRF) i, p, c));
		strategies.put(PartCoproprieteRF.class, (i, p, c) -> newCoOwnershipShare((PartCoproprieteRF) i, p, c));
		strategies.put(DroitDistinctEtPermanentRF.class, (i, p, c) -> newDistinctAndPermanentRight((DroitDistinctEtPermanentRF) i, p, c));
		strategies.put(MineRF.class, (i, p, c) -> newMine((MineRF) i, p, c));
		strategies.put(BienFondRF.class, (i, p, c) -> newRealEstate((BienFondRF) i, p, c));
	}

	@NotNull
	public static ImmovableProperty newImmovableProperty(@NotNull ImmeubleRF immeuble, @NotNull CapitastraURLProvider capitastraUrlProvider, RightHolderBuilder.ContribuableIdProvider contribuableIdProvider) {
		final Strategy<?> strategy = strategies.get(immeuble.getClass());
		if (strategy == null) {
			throw new IllegalArgumentException("Le type d'immeuble [" + immeuble.getClass() + "] est inconnu");
		}
		return strategy.apply(immeuble, capitastraUrlProvider, contribuableIdProvider);
	}

	@NotNull
	private static CondominiumOwnership newCondominiumOwnership(@NotNull ProprieteParEtageRF ppe, @NotNull CapitastraURLProvider capitastraUrlProvider,  @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider) {
		final CondominiumOwnership condo = new CondominiumOwnership();
		fillBase(condo, ppe, capitastraUrlProvider, contribuableIdProvider);
		condo.setShare(LandRightBuilder.getShare(ppe.getQuotePart()));
		return condo;
	}

	@NotNull
	private static CoOwnershipShare newCoOwnershipShare(@NotNull PartCoproprieteRF part, @NotNull CapitastraURLProvider capitastraUrlProvider,  @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider) {
		final CoOwnershipShare coown = new CoOwnershipShare();
		fillBase(coown, part, capitastraUrlProvider, contribuableIdProvider);
		coown.setShare(LandRightBuilder.getShare(part.getQuotePart()));
		return coown;
	}

	@NotNull
	private static DistinctAndPermanentRight newDistinctAndPermanentRight(@NotNull DroitDistinctEtPermanentRF ddp, @NotNull CapitastraURLProvider capitastraUrlProvider,  @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider) {
		final DistinctAndPermanentRight dpr = new DistinctAndPermanentRight();
		fillBase(dpr, ddp, capitastraUrlProvider, contribuableIdProvider);
		return dpr;
	}

	@NotNull
	private static Mine newMine(@NotNull MineRF mine, @NotNull CapitastraURLProvider capitastraUrlProvider,  @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider) {
		final Mine m = new Mine();
		fillBase(m, mine, capitastraUrlProvider, contribuableIdProvider);
		return m;
	}

	@NotNull
	private static RealEstate newRealEstate(@NotNull BienFondRF bienFond, @NotNull CapitastraURLProvider capitastraUrlProvider,  @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider) {
		final RealEstate estate = new RealEstate();
		fillBase(estate, bienFond, capitastraUrlProvider, contribuableIdProvider);
		estate.setCfa(bienFond.isCfa());
		return estate;
	}

	private static void fillBase(ImmovableProperty property, @NotNull ImmeubleRF immeuble, @NotNull CapitastraURLProvider capitastraUrlProvider,  @NotNull RightHolderBuilder.ContribuableIdProvider contribuableIdProvider) {
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
		property.getLandRights().addAll(immeuble.getDroits().stream()
				                                .filter(AnnulableHelper::nonAnnule)
				                                // on n'expose pas les droits des communautés (c'est les droits des personnes membres des communautés qui portent l'information)
				                                .filter(d -> !(d instanceof DroitProprieteCommunauteRF))
				                                .sorted()
				                                .map(d -> LandRightBuilder.newLandRight(d, contribuableIdProvider))
				                                .collect(Collectors.toList()));
	}

	@NotNull
	private static Location newLocation(@NotNull SituationRF situation) {
		final Location loc = new Location();
		loc.setDateFrom(DataHelper.coreToXMLv2(situation.getDateDebut()));
		loc.setDateTo(DataHelper.coreToXMLv2(situation.getDateFin()));
		loc.setParcelNumber(situation.getNoParcelle());
		loc.setIndex1(situation.getIndex1());
		loc.setIndex2(situation.getIndex2());
		loc.setIndex3(situation.getIndex3());
		loc.setMunicipalityFsoId(situation.getCommune().getNoOfs());
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

	public static ch.vd.unireg.xml.party.immovableproperty.v2.ImmovableProperty newImmovableProperty(Immeuble immeuble) {
		final ch.vd.unireg.xml.party.immovableproperty.v2.ImmovableProperty immo = new ch.vd.unireg.xml.party.immovableproperty.v2.ImmovableProperty();
		immo.setNumber(immeuble.getNumero());
		immo.setDateFrom(DataHelper.coreToXMLv2(immeuble.getDateDebut()));
		immo.setDateTo(DataHelper.coreToXMLv2(immeuble.getDateFin()));
		immo.setEntryDate(DataHelper.coreToXMLv2(immeuble.getDateValidRF()));
		immo.setMunicipalityName(immeuble.getNomCommune());
		immo.setEstimatedTaxValue(immeuble.getEstimationFiscale());
		immo.setEstimatedTaxValueReference(immeuble.getReferenceEstimationFiscale());
		immo.setNature(immeuble.getNature());
		immo.setOwnershipType(EnumHelper.coreToXMLv2(immeuble.getGenrePropriete()));
		immo.setShare(coreToXML(immeuble.getPartPropriete()));
		immo.setType(EnumHelper.coreToXMLv2(immeuble.getTypeImmeuble()));
		immo.setLastMutationDate(DataHelper.coreToXMLv2(immeuble.getDateDerniereMutation()));
		immo.setLastMutationType(EnumHelper.coreToXMLv2(immeuble.getDerniereMutation()));
		return immo;
	}

	private static ch.vd.unireg.xml.party.immovableproperty.v2.PropertyShare coreToXML(PartPropriete part) {
		if (part == null) {
			return null;
		}
		return new ch.vd.unireg.xml.party.immovableproperty.v2.PropertyShare(part.getNumerateur(), part.getDenominateur());
	}
}
