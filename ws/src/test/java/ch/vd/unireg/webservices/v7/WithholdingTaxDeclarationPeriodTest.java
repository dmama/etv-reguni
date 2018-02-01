package ch.vd.uniregctb.webservices.v7;

import org.junit.Test;

import ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriod;
import ch.vd.uniregctb.type.PeriodeDecompte;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class WithholdingTaxDeclarationPeriodTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(WithholdingTaxDeclarationPeriod.class, PeriodeDecompte.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (PeriodeDecompte pd : PeriodeDecompte.values()) {
			assertNotNull(pd.name(), EnumHelper.coreToWeb(pd));
		}
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((PeriodeDecompte) null));
		assertEquals(WithholdingTaxDeclarationPeriod.M_01, EnumHelper.coreToWeb(PeriodeDecompte.M01));
		assertEquals(WithholdingTaxDeclarationPeriod.M_02, EnumHelper.coreToWeb(PeriodeDecompte.M02));
		assertEquals(WithholdingTaxDeclarationPeriod.M_03, EnumHelper.coreToWeb(PeriodeDecompte.M03));
		assertEquals(WithholdingTaxDeclarationPeriod.M_04, EnumHelper.coreToWeb(PeriodeDecompte.M04));
		assertEquals(WithholdingTaxDeclarationPeriod.M_05, EnumHelper.coreToWeb(PeriodeDecompte.M05));
		assertEquals(WithholdingTaxDeclarationPeriod.M_06, EnumHelper.coreToWeb(PeriodeDecompte.M06));
		assertEquals(WithholdingTaxDeclarationPeriod.M_07, EnumHelper.coreToWeb(PeriodeDecompte.M07));
		assertEquals(WithholdingTaxDeclarationPeriod.M_08, EnumHelper.coreToWeb(PeriodeDecompte.M08));
		assertEquals(WithholdingTaxDeclarationPeriod.M_09, EnumHelper.coreToWeb(PeriodeDecompte.M09));
		assertEquals(WithholdingTaxDeclarationPeriod.M_10, EnumHelper.coreToWeb(PeriodeDecompte.M10));
		assertEquals(WithholdingTaxDeclarationPeriod.M_11, EnumHelper.coreToWeb(PeriodeDecompte.M11));
		assertEquals(WithholdingTaxDeclarationPeriod.M_12, EnumHelper.coreToWeb(PeriodeDecompte.M12));
		assertEquals(WithholdingTaxDeclarationPeriod.Q_1, EnumHelper.coreToWeb(PeriodeDecompte.T1));
		assertEquals(WithholdingTaxDeclarationPeriod.Q_2, EnumHelper.coreToWeb(PeriodeDecompte.T2));
		assertEquals(WithholdingTaxDeclarationPeriod.Q_3, EnumHelper.coreToWeb(PeriodeDecompte.T3));
		assertEquals(WithholdingTaxDeclarationPeriod.Q_4, EnumHelper.coreToWeb(PeriodeDecompte.T4));
		assertEquals(WithholdingTaxDeclarationPeriod.H_1, EnumHelper.coreToWeb(PeriodeDecompte.S1));
		assertEquals(WithholdingTaxDeclarationPeriod.H_2, EnumHelper.coreToWeb(PeriodeDecompte.S2));
		assertEquals(WithholdingTaxDeclarationPeriod.Y, EnumHelper.coreToWeb(PeriodeDecompte.A));
	}

}
