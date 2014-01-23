package ch.vd.uniregctb.webservices.v5;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus;
import ch.vd.uniregctb.type.EtatCivil;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


public class MaritalStatusTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(MaritalStatus.class, ch.vd.uniregctb.type.EtatCivil.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((EtatCivil) null));
		assertEquals(MaritalStatus.SINGLE, EnumHelper.coreToWeb(EtatCivil.CELIBATAIRE));
		assertEquals(MaritalStatus.MARRIED, EnumHelper.coreToWeb(EtatCivil.MARIE));
		assertEquals(MaritalStatus.WIDOWED, EnumHelper.coreToWeb(EtatCivil.VEUF));
		assertEquals(MaritalStatus.REGISTERED_PARTNER, EnumHelper.coreToWeb(EtatCivil.LIE_PARTENARIAT_ENREGISTRE));
		assertEquals(MaritalStatus.NOT_MARRIED, EnumHelper.coreToWeb(EtatCivil.NON_MARIE));
		assertEquals(MaritalStatus.PARTNERSHIP_ABOLISHED_BY_DEATH, EnumHelper.coreToWeb(EtatCivil.PARTENARIAT_DISSOUS_DECES));
		assertEquals(MaritalStatus.PARTNERSHIP_ABOLISHED_BY_LAW, EnumHelper.coreToWeb(EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT));
		assertEquals(MaritalStatus.DIVORCED, EnumHelper.coreToWeb(EtatCivil.DIVORCE));
		assertEquals(MaritalStatus.SEPARATED, EnumHelper.coreToWeb(EtatCivil.SEPARE));
		assertEquals(MaritalStatus.PARTNERSHIP_SEPARATED, EnumHelper.coreToWeb(EtatCivil.PARTENARIAT_SEPARE));
	}
}
