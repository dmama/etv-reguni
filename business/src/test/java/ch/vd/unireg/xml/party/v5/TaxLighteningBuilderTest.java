package ch.vd.unireg.xml.party.v5;

import java.math.BigDecimal;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.corporation.v5.LighteningType;
import ch.vd.unireg.xml.party.corporation.v5.MunicipalityLighteningTarget;
import ch.vd.unireg.xml.party.corporation.v5.TaxLightening;
import ch.vd.unireg.xml.party.corporation.v5.TaxType;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.AllegementFiscalCanton;
import ch.vd.unireg.tiers.AllegementFiscalCantonCommune;
import ch.vd.unireg.tiers.AllegementFiscalCommune;
import ch.vd.unireg.tiers.AllegementFiscalConfederation;
import ch.vd.unireg.xml.DataHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TaxLighteningBuilderTest {

	@Test
	public void testNewTaxLighteningCH() throws Exception {

		final AllegementFiscalConfederation ch = new AllegementFiscalConfederation();
		ch.setId(12444L);
		ch.setDateDebut(RegDate.get(2000,1,1));
		ch.setDateFin(RegDate.get(2000,12,31));
		ch.setPourcentageAllegement(BigDecimal.TEN);
		ch.setTypeImpot(AllegementFiscal.TypeImpot.BENEFICE);
		ch.setType(AllegementFiscalConfederation.Type.IMMEUBLE_SI_SUBVENTIONNEE);

		final TaxLightening lightening = TaxLighteningBuilder.newTaxLightening(ch);
		assertNotNull(lightening);
		assertEquals(RegDate.get(2000,1,1), DataHelper.xmlToCore(lightening.getDateFrom()));
		assertEquals(RegDate.get(2000,12,31), DataHelper.xmlToCore(lightening.getDateTo()));
		assertNull(lightening.getAmountBased());
		assertEquals(BigDecimal.TEN, lightening.getLighteningPercentage());
		assertEquals(TaxType.PROFIT, lightening.getTaxType());
		assertNotNull(lightening.getTargetCollectivity().getSwissConfederation());
		assertEquals(LighteningType.SUBSIDIZED_BUILDING, lightening.getLighteningType());
	}

	@Test
	public void testNewTaxLighteningVD() throws Exception {

		final AllegementFiscalCanton vd = new AllegementFiscalCanton();
		vd.setId(12444L);
		vd.setDateDebut(RegDate.get(2000,1,1));
		vd.setDateFin(RegDate.get(2000,12,31));
		vd.setPourcentageAllegement(BigDecimal.TEN);
		vd.setTypeImpot(AllegementFiscal.TypeImpot.BENEFICE);
		vd.setType(AllegementFiscalCantonCommune.Type.SOCIETE_SERVICE);

		final TaxLightening lightening = TaxLighteningBuilder.newTaxLightening(vd);
		assertNotNull(lightening);
		assertEquals(RegDate.get(2000,1,1), DataHelper.xmlToCore(lightening.getDateFrom()));
		assertEquals(RegDate.get(2000,12,31), DataHelper.xmlToCore(lightening.getDateTo()));
		assertNull(lightening.getAmountBased());
		assertEquals(BigDecimal.TEN, lightening.getLighteningPercentage());
		assertEquals(TaxType.PROFIT, lightening.getTaxType());
		assertNotNull(lightening.getTargetCollectivity().getCanton());
		assertEquals(LighteningType.SERVICES_COMPANY, lightening.getLighteningType());
	}

	@Test
	public void testNewTaxLighteningCommune() throws Exception {

		final AllegementFiscalCommune commune = new AllegementFiscalCommune();
		commune.setId(12444L);
		commune.setDateDebut(RegDate.get(2000,1,1));
		commune.setDateFin(RegDate.get(2000,12,31));
		commune.setPourcentageAllegement(null);
		commune.setTypeImpot(AllegementFiscal.TypeImpot.BENEFICE);
		commune.setType(AllegementFiscalCantonCommune.Type.TRANSPORTS_CONCESSIONNES);
		commune.setNoOfsCommune(5586);

		final TaxLightening lightening = TaxLighteningBuilder.newTaxLightening(commune);
		assertNotNull(lightening);
		assertEquals(RegDate.get(2000,1,1), DataHelper.xmlToCore(lightening.getDateFrom()));
		assertEquals(RegDate.get(2000,12,31), DataHelper.xmlToCore(lightening.getDateTo()));
		assertNotNull(lightening.getAmountBased());
		assertNull(lightening.getLighteningPercentage());
		assertEquals(TaxType.PROFIT, lightening.getTaxType());
		final MunicipalityLighteningTarget municipality = lightening.getTargetCollectivity().getMunicipality();
		assertNotNull(municipality);
		assertEquals(Integer.valueOf(5586), municipality.getMunicipalityFSOId());
		assertEquals(LighteningType.LICENSED_PUBLIC_TRANSPORT, lightening.getLighteningType());
	}
}