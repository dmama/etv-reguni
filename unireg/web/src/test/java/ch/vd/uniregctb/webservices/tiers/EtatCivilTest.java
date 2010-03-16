package ch.vd.uniregctb.webservices.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers.impl.EnumHelper;


public class EtatCivilTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(EtatCivil.class, ch.vd.uniregctb.type.EtatCivil.class);
		assertEnumConstantsEqual(EtatCivil.class, ch.vd.uniregctb.type.EtatCivil.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.EtatCivil) null));
		assertEquals(EtatCivil.CELIBATAIRE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.CELIBATAIRE));
		assertEquals(EtatCivil.MARIE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.MARIE));
		assertEquals(EtatCivil.VEUF, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.VEUF));
		assertEquals(EtatCivil.LIE_PARTENARIAT_ENREGISTRE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.LIE_PARTENARIAT_ENREGISTRE));
		assertEquals(EtatCivil.NON_MARIE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.NON_MARIE));
		assertEquals(EtatCivil.PARTENARIAT_DISSOUS_DECES, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.PARTENARIAT_DISSOUS_DECES));
		assertEquals(EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT));
		assertEquals(EtatCivil.DIVORCE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.DIVORCE));
		assertEquals(EtatCivil.SEPARE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.SEPARE));
	}
}
