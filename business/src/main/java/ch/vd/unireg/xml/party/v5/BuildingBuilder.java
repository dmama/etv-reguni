package ch.vd.unireg.xml.party.v5;

import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.unireg.xml.party.landregistry.v1.Building;
import ch.vd.unireg.xml.party.landregistry.v1.BuildingDescription;
import ch.vd.unireg.xml.party.landregistry.v1.BuildingSetting;
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.DescriptionBatimentRF;
import ch.vd.unireg.registrefoncier.ImplantationRF;
import ch.vd.unireg.xml.DataHelper;

public class BuildingBuilder {

	@NotNull
	public static Building newBuilding(@NotNull BatimentRF batiment) {

		final Building building = new Building();
		building.setId(batiment.getId());
		building.getDescriptions().addAll(batiment.getDescriptions().stream()
				                                  .sorted(new DateRangeComparator<>())
				                                  .map(BuildingBuilder::newDescription)
				                                  .collect(Collectors.toList()));
		building.getSettings().addAll(batiment.getImplantations().stream()
				                              .sorted(new DateRangeComparator<>())
				                              .map(BuildingBuilder::newBuildSetting)
				                              .collect(Collectors.toList()));
		return building;
	}

	@NotNull
	private static BuildingDescription newDescription(@NotNull DescriptionBatimentRF description) {
		final BuildingDescription bd = new BuildingDescription();
		bd.setType(description.getType());
		bd.setArea(description.getSurface());
		bd.setDateFrom(DataHelper.coreToXMLv2(description.getDateDebut()));
		bd.setDateTo(DataHelper.coreToXMLv2(description.getDateFin()));
		return bd;
	}

	@NotNull
	public static BuildingSetting newBuildSetting(@NotNull ImplantationRF implantation) {
		final BuildingSetting setting = new BuildingSetting();
		setting.setArea(implantation.getSurface());
		setting.setBuildingId(implantation.getBatiment().getId());
		setting.setImmovablePropertyId(implantation.getImmeuble().getId());
		setting.setDateFrom(DataHelper.coreToXMLv2(implantation.getDateDebut()));
		setting.setDateTo(DataHelper.coreToXMLv2(implantation.getDateFin()));
		return setting;
	}
}
