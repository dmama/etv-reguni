package ch.vd.uniregctb.webservices.tiers3.data;

import org.junit.Test;

import ch.vd.unireg.webservices.tiers3.MaritalStatus;
import ch.vd.uniregctb.webservices.tiers3.EnumTest;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class MaritalStatusTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(MaritalStatus.class, ch.vd.uniregctb.type.EtatCivil.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.EtatCivil) null));
		assertEquals(MaritalStatus.SINGLE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.CELIBATAIRE));
		assertEquals(MaritalStatus.MARRIED, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.MARIE));
		assertEquals(MaritalStatus.WIDOWED, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.VEUF));
		assertEquals(MaritalStatus.REGISTERED_PARTNER, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.LIE_PARTENARIAT_ENREGISTRE));
		assertEquals(MaritalStatus.NOT_MARRIED, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.NON_MARIE));
		assertEquals(MaritalStatus.PARTNERSHIP_ABOLISHED_BY_DEATH, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.PARTENARIAT_DISSOUS_DECES));
		assertEquals(MaritalStatus.PARTNERSHIP_ABOLISHED_BY_LAW, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT));
		assertEquals(MaritalStatus.DIVORCED, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.DIVORCE));
		assertEquals(MaritalStatus.SEPARATED, EnumHelper.coreToWeb(ch.vd.uniregctb.type.EtatCivil.SEPARE));
	}
}
