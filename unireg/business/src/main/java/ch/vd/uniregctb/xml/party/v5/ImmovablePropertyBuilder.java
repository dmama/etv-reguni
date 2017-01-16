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
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
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

	private static final Map<Class, Function<ImmeubleRF, ? extends ImmovableProperty>> strategies = new HashMap<>();

	static {
		strategies.put(ProprieteParEtageRF.class, i -> newCondominiumOwnership((ProprieteParEtageRF) i));
		strategies.put(PartCoproprieteRF.class, i -> newCoOwnershipShare((PartCoproprieteRF) i));
		strategies.put(DroitDistinctEtPermanentRF.class, i -> newDistinctAndPermanentRight((DroitDistinctEtPermanentRF) i));
		strategies.put(MineRF.class, i -> newMine((MineRF) i));
		strategies.put(BienFondRF.class, i -> newRealEstate((BienFondRF) i));
	}

	@NotNull
	public static ImmovableProperty newImmovableProperty(@NotNull ImmeubleRF immeuble) {
		final Function<ImmeubleRF, ? extends ImmovableProperty> strategy = strategies.get(immeuble.getClass());
		if (strategy == null) {
			throw new IllegalArgumentException("Le type d'immeuble [" + immeuble.getClass() + "] est inconnu");
		}
		return strategy.apply(immeuble);
	}

	@NotNull
	private static CondominiumOwnership newCondominiumOwnership(@NotNull ProprieteParEtageRF ppe) {
		final CondominiumOwnership condo = new CondominiumOwnership();
		fillBase(condo, ppe);
		condo.setShare(LandRightBuilder.getShare(ppe.getQuotePart()));
		return condo;
	}

	@NotNull
	private static CoOwnershipShare newCoOwnershipShare(@NotNull PartCoproprieteRF part) {
		final CoOwnershipShare coown = new CoOwnershipShare();
		fillBase(coown, part);
		coown.setShare(LandRightBuilder.getShare(part.getQuotePart()));
		return coown;
	}

	@NotNull
	private static DistinctAndPermanentRight newDistinctAndPermanentRight(@NotNull DroitDistinctEtPermanentRF ddp) {
		final DistinctAndPermanentRight dpr = new DistinctAndPermanentRight();
		fillBase(dpr, ddp);
		return dpr;
	}

	@NotNull
	private static Mine newMine(@NotNull MineRF mine) {
		final Mine m = new Mine();
		fillBase(m, mine);
		return m;
	}

	@NotNull
	private static RealEstate newRealEstate(@NotNull BienFondRF bienFond) {
		final RealEstate estate = new RealEstate();
		fillBase(estate, bienFond);
		estate.setCfa(bienFond.isCfa());
		return estate;
	}

	private static void fillBase(ImmovableProperty property, @NotNull ImmeubleRF immeuble) {
		property.setId(immeuble.getId());
		property.setEgrid(immeuble.getEgrid());
		property.setUrlIntercapi("todo");   // TODO (msi)
		property.setCancellationDate(DataHelper.coreToXMLv2(immeuble.getDateRadiation()));

		property.getLocations().addAll(immeuble.getSituations().stream()
				                               .filter(Annulable::isNotAnnule)
				                               .sorted(SituationRF::compareTo)
				                               .map(ImmovablePropertyBuilder::newLocation)
				                               .collect(Collectors.toList()));
		property.getTaxEstimates().addAll(immeuble.getEstimations().stream()
				                                  .filter(Annulable::isNotAnnule)
				                                  .sorted(EstimationRF::compareTo)
				                                  .map(ImmovablePropertyBuilder::newTaxEstimate)
				                                  .collect(Collectors.toList()));
		property.getTotalAreas().addAll(immeuble.getSurfacesTotales().stream()
				                                .filter(Annulable::isNotAnnule)
				                                .sorted(SurfaceTotaleRF::compareTo)
				                                .map(ImmovablePropertyBuilder::newTotalArea)
				                                .collect(Collectors.toList()));
		property.getGroundAreas().addAll(immeuble.getSurfacesAuSol().stream()
				                                 .filter(Annulable::isNotAnnule)
				                                 .sorted(SurfaceAuSolRF::compareTo)
				                                 .map(ImmovablePropertyBuilder::newGroundArea)
				                                 .collect(Collectors.toList()));
		property.getBuildingSettings().addAll(immeuble.getImplantations().stream()
				                                      .filter(Annulable::isNotAnnule)
				                                      .sorted(ImplantationRF::compareTo)
				                                      .map(BuildingBuilder::newBuildSetting)
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
		estimate.setDateFrom(DataHelper.coreToXMLv2(e.getDateDebut()));
		estimate.setDateTo(DataHelper.coreToXMLv2(e.getDateFin()));
		estimate.setAmount(e.getMontant());
		estimate.setReference(e.getReference());
		estimate.setEstimationDate(DataHelper.coreToXMLv2(e.getDateEstimation()));
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
