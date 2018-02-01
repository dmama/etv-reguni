package ch.vd.uniregctb.documentfiscal;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.MineRF;
import ch.vd.uniregctb.registrefoncier.PartCoproprieteRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;

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
