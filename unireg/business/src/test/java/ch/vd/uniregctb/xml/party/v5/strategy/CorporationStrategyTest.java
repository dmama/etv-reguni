package ch.vd.uniregctb.xml.party.v5.strategy;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatement;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IfoncExemption;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.ExonerationIFONC;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CorporationStrategyTest {

	private CorporationStrategy strategy;

	@Before
	public void setUp() throws Exception {
		strategy = new CorporationStrategy();
	}

	/**
	 * Ce test vérifie que les collections des allègements fonciers sont bien ordonnées correctement.
	 */
	@Test
	public void testInitLandTaxLightenings() throws Exception {

		final BienFondRF immeuble0 = new BienFondRF();
		immeuble0.setId(0L);

		final BienFondRF immeuble1 = new BienFondRF();
		immeuble1.setId(1L);

		final BienFondRF immeuble2 = new BienFondRF();
		immeuble2.setId(2L);


		final Entreprise entreprise = new Entreprise();
		entreprise.addAllegementFoncier(newExoneration(RegDate.get(2005, 1, 1), null, immeuble2));
		entreprise.addAllegementFoncier(newExoneration(RegDate.get(2005, 1, 1), null, immeuble1));
		entreprise.addAllegementFoncier(newExoneration(RegDate.get(2005, 1, 1), null, immeuble0));
		entreprise.addAllegementFoncier(newExoneration(RegDate.get(2000, 1, 1), RegDate.get(2009, 12, 31), immeuble0));

		entreprise.addAllegementFoncier(newDegrevement(RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 13), immeuble2));
		entreprise.addAllegementFoncier(newDegrevement(RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 13), immeuble1));
		entreprise.addAllegementFoncier(newDegrevement(RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 13), immeuble0));
		entreprise.addAllegementFoncier(newDegrevement(RegDate.get(1990, 1, 1), null, immeuble2));

		final Corporation corporation = new Corporation();
		strategy.initLandTaxLightenings(corporation, entreprise);

		// l'ordre doit être :
		// - chronologique croissant
		// - id d'immeuble croissant
		final List<IfoncExemption> exemptions = corporation.getIfoncExemptions();
		assertNotNull(exemptions);
		assertEquals(4, exemptions.size());
		assertExemption(RegDate.get(2000, 1, 1), RegDate.get(2009, 12, 31), 0L, exemptions.get(0));
		assertExemption(RegDate.get(2005, 1, 1), null, 0L, exemptions.get(1));
		assertExemption(RegDate.get(2005, 1, 1), null, 1L, exemptions.get(2));
		assertExemption(RegDate.get(2005, 1, 1), null, 2L, exemptions.get(3));

		// l'ordre doit être :
		// - chronologique croissant
		// - id d'immeuble croissant
		final List<IciAbatement> abatements = corporation.getIciAbatements();
		assertNotNull(abatements);
		assertEquals(4, abatements.size());
		assertAbatement(RegDate.get(1990, 1, 1), null, 2L, abatements.get(0));
		assertAbatement(RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 13), 0L, abatements.get(1));
		assertAbatement(RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 13), 1L, abatements.get(2));
		assertAbatement(RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 13), 2L, abatements.get(3));
	}

	private static void assertExemption(RegDate dateFrom, RegDate dateTo, long immovablePropId, IfoncExemption exemption) {
		assertNotNull(exemption);
		assertEquals(dateFrom, DataHelper.xmlToCore(exemption.getDateFrom()));
		assertEquals(dateTo, DataHelper.xmlToCore(exemption.getDateTo()));
		assertEquals(immovablePropId, exemption.getImmovablePropertyId());
	}

	private static void assertAbatement(RegDate dateFrom, RegDate dateTo, long immovablePropId, IciAbatement abatement) {
		assertNotNull(abatement);
		assertEquals(dateFrom, DataHelper.xmlToCore(abatement.getDateFrom()));
		assertEquals(dateTo, DataHelper.xmlToCore(abatement.getDateTo()));
		assertEquals(immovablePropId, abatement.getImmovablePropertyId());
	}

	private static DegrevementICI newDegrevement(RegDate dateDebut, RegDate dateFin, BienFondRF immeuble) {
		final DegrevementICI deg = new DegrevementICI();
		deg.setDateDebut(dateDebut);
		deg.setDateFin(dateFin);
		deg.setImmeuble(immeuble);
		return deg;
	}

	private static ExonerationIFONC newExoneration(RegDate dateDebut, RegDate dateFin, BienFondRF immeuble) {
		final ExonerationIFONC exo0 = new ExonerationIFONC();
		exo0.setDateDebut(dateDebut);
		exo0.setDateFin(dateFin);
		exo0.setImmeuble(immeuble);
		return exo0;
	}
}