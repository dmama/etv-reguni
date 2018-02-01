package ch.vd.unireg.xml.party.v5;

import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.landregistry.v1.Building;
import ch.vd.unireg.xml.party.landregistry.v1.BuildingDescription;
import ch.vd.unireg.xml.party.landregistry.v1.BuildingSetting;
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.DescriptionBatimentRF;
import ch.vd.unireg.registrefoncier.ImplantationRF;
import ch.vd.unireg.xml.DataHelper;

import static ch.vd.unireg.common.AbstractSpringTest.assertEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BuildingBuilderTest {

	@Test
	public void testBuildingWithDescription() throws Exception {

		final DescriptionBatimentRF desc0 = new DescriptionBatimentRF();
		desc0.setType("Pâturage");
		desc0.setSurface(1222);
		desc0.setDateDebut(RegDate.get(1955, 2, 22));
		desc0.setDateFin(RegDate.get(2008, 7, 12));

		final DescriptionBatimentRF desc1 = new DescriptionBatimentRF();
		desc1.setType("Pâturage");
		desc1.setSurface(1304);
		desc1.setDateDebut(RegDate.get(2008, 7, 13));
		desc1.setDateFin(null);

		final BatimentRF batiment = new BatimentRF();
		batiment.setId(12343L);
		batiment.setMasterIdRF("92929e9e9");
		batiment.addDescription(desc0);
		batiment.addDescription(desc1);
		batiment.setImplantations(new HashSet<>());

		final Building building = BuildingBuilder.newBuilding(batiment);
		assertEquals(12343L, building.getId());
		assertEmpty(building.getSettings());

		final List<BuildingDescription> descriptions = building.getDescriptions();
		assertEquals(2, descriptions.size());

		final BuildingDescription description0 = descriptions.get(0);
		assertEquals("Pâturage", description0.getType());
		assertEquals(Integer.valueOf(1222), description0.getArea());
		assertEquals(RegDate.get(1955, 2, 22), DataHelper.xmlToCore(description0.getDateFrom()));
		assertEquals(RegDate.get(2008, 7, 12), DataHelper.xmlToCore(description0.getDateTo()));

		final BuildingDescription description1 = descriptions.get(1);
		assertEquals("Pâturage", description1.getType());
		assertEquals(Integer.valueOf(1304), description1.getArea());
		assertEquals(RegDate.get(2008, 7, 13), DataHelper.xmlToCore(description1.getDateFrom()));
		assertNull(description1.getDateTo());
	}

	@Test
	public void testBuildingWithSetting() throws Exception {

		final BienFondsRF bienFonds0 = new BienFondsRF();
		bienFonds0.setId(303L);

		final BienFondsRF bienFonds1 = new BienFondsRF();
		bienFonds1.setId(404L);

		final ImplantationRF impl0 = new ImplantationRF();
		impl0.setSurface(234);
		impl0.setImmeuble(bienFonds0);
		impl0.setDateDebut(RegDate.get(2004, 5, 13));
		impl0.setDateFin(null);

		final ImplantationRF impl1 = new ImplantationRF();
		impl1.setSurface(null);
		impl1.setImmeuble(bienFonds1);
		impl1.setDateDebut(RegDate.get(1950, 11, 1));
		impl1.setDateFin(null);

		final BatimentRF batiment = new BatimentRF();
		batiment.setId(12343L);
		batiment.setMasterIdRF("92929e9e9");
		batiment.setDescriptions(new HashSet<>());
		batiment.addImplantation(impl0);
		batiment.addImplantation(impl1);

		final Building building = BuildingBuilder.newBuilding(batiment);
		assertEquals(12343L, building.getId());
		assertEmpty(building.getDescriptions());

		final List<BuildingSetting> settings = building.getSettings();
		assertEquals(2, settings.size());

		final BuildingSetting setting0 = settings.get(0);
		assertNull(setting0.getArea());
		assertEquals(404L, setting0.getImmovablePropertyId());
		assertEquals(12343L, setting0.getBuildingId());
		assertEquals(RegDate.get(1950, 11, 1), DataHelper.xmlToCore(setting0.getDateFrom()));
		assertNull(setting0.getDateTo());

		final BuildingSetting setting1 = settings.get(1);
		assertEquals(Integer.valueOf(234), setting1.getArea());
		assertEquals(303L, setting1.getImmovablePropertyId());
		assertEquals(12343L, setting1.getBuildingId());
		assertEquals(RegDate.get(2004, 5, 13), DataHelper.xmlToCore(setting1.getDateFrom()));
		assertNull(setting1.getDateTo());
	}
}