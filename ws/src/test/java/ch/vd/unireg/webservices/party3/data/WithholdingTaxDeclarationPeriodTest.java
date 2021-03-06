package ch.vd.unireg.webservices.party3.data;

import org.junit.Test;

import ch.vd.unireg.webservices.party3.EnumTest;
import ch.vd.unireg.webservices.party3.impl.EnumHelper;
import ch.vd.unireg.xml.party.debtortype.v1.WithholdingTaxDeclarationPeriod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class WithholdingTaxDeclarationPeriodTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(WithholdingTaxDeclarationPeriod.class, ch.vd.unireg.type.PeriodeDecompte.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.unireg.type.PeriodeDecompte) null));
		assertEquals(WithholdingTaxDeclarationPeriod.M_01, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.M01));
		assertEquals(WithholdingTaxDeclarationPeriod.M_02, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.M02));
		assertEquals(WithholdingTaxDeclarationPeriod.M_03, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.M03));
		assertEquals(WithholdingTaxDeclarationPeriod.M_04, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.M04));
		assertEquals(WithholdingTaxDeclarationPeriod.M_05, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.M05));
		assertEquals(WithholdingTaxDeclarationPeriod.M_06, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.M06));
		assertEquals(WithholdingTaxDeclarationPeriod.M_07, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.M07));
		assertEquals(WithholdingTaxDeclarationPeriod.M_08, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.M08));
		assertEquals(WithholdingTaxDeclarationPeriod.M_09, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.M09));
		assertEquals(WithholdingTaxDeclarationPeriod.M_10, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.M10));
		assertEquals(WithholdingTaxDeclarationPeriod.M_11, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.M11));
		assertEquals(WithholdingTaxDeclarationPeriod.M_12, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.M12));
		assertEquals(WithholdingTaxDeclarationPeriod.Q_1, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.T1));
		assertEquals(WithholdingTaxDeclarationPeriod.Q_2, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.T2));
		assertEquals(WithholdingTaxDeclarationPeriod.Q_3, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.T3));
		assertEquals(WithholdingTaxDeclarationPeriod.Q_4, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.T4));
		assertEquals(WithholdingTaxDeclarationPeriod.H_1, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.S1));
		assertEquals(WithholdingTaxDeclarationPeriod.H_2, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.S2));
		assertEquals(WithholdingTaxDeclarationPeriod.Y, EnumHelper.coreToWeb(ch.vd.unireg.type.PeriodeDecompte.A));
	}

}
