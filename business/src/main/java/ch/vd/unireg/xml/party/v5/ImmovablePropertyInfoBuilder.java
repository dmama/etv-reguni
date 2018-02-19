package ch.vd.unireg.xml.party.v5;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.MineRF;
import ch.vd.unireg.registrefoncier.PartCoproprieteRF;
import ch.vd.unireg.registrefoncier.ProprieteParEtageRF;
import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovablePropertyInfo;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovablePropertyType;

public class ImmovablePropertyInfoBuilder {

	@NotNull
	public static ImmovablePropertyInfo newInfo(@NotNull SituationRF situation) {
		final ImmeubleRF immeuble = situation.getImmeuble();
		final ImmovablePropertyInfo info = new ImmovablePropertyInfo();
		info.setId(immeuble.getId());
		info.setImmovablePropertyType(getType(immeuble));
		info.setEgrid(immeuble.getEgrid());
		info.setLocation(ImmovablePropertyBuilder.newLocation(situation));
		info.setCancellationDate(DataHelper.coreToXMLv2(immeuble.getDateRadiation()));
		return info;
	}

	private static ImmovablePropertyType getType(@NotNull ImmeubleRF immeuble) {
		if (immeuble instanceof BienFondsRF) {
			return ImmovablePropertyType.REAL_ESTATE;
		}
		else if (immeuble instanceof DroitDistinctEtPermanentRF) {
			return ImmovablePropertyType.DISTINCT_AND_PERMANENT_RIGHT;
		}
		else if (immeuble instanceof PartCoproprieteRF) {
			return ImmovablePropertyType.CO_OWNERSHIP_SHARE;
		}
		else if (immeuble instanceof ProprieteParEtageRF) {
			return ImmovablePropertyType.CONDOMINIUM_OWNERSHIP;
		}
		else if (immeuble instanceof MineRF) {
			return ImmovablePropertyType.MINE;
		}
		else {
			throw new IllegalArgumentException("Type d'immeuble inconnu = [" + immeuble.getClass() + "]");
		}
	}
}
