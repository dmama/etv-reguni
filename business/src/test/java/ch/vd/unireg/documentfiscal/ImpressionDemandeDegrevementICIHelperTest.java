package ch.vd.unireg.documentfiscal;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.unireg.registrefoncier.MineRF;
import ch.vd.unireg.registrefoncier.PartCoproprieteRF;
import ch.vd.unireg.registrefoncier.ProprieteParEtageRF;

public class ImpressionDemandeDegrevementICIHelperTest extends WithoutSpringTest {

	@Test
	public void testGetTypeImmeuble() throws Exception {
		Assert.assertEquals("PPE", ImpressionDemandeDegrevementICIHelperImpl.getTypeImmeuble(new ProprieteParEtageRF()));
		Assert.assertEquals("DDP", ImpressionDemandeDegrevementICIHelperImpl.getTypeImmeuble(new DroitDistinctEtPermanentRF()));
		Assert.assertEquals("Mine", ImpressionDemandeDegrevementICIHelperImpl.getTypeImmeuble(new MineRF()));
		Assert.assertEquals("Bien-fonds", ImpressionDemandeDegrevementICIHelperImpl.getTypeImmeuble(new BienFondsRF()));
		Assert.assertEquals("Copropriété", ImpressionDemandeDegrevementICIHelperImpl.getTypeImmeuble(new PartCoproprieteRF()));
	}
}
