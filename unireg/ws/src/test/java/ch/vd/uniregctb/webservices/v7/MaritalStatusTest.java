package ch.vd.uniregctb.webservices.v7;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus;
import ch.vd.uniregctb.type.EtatCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class MaritalStatusTest extends EnumTest {

	@Test
	public void testCoherence() {
		assertEnumLengthEquals(MaritalStatus.class, EtatCivil.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (EtatCivil ec : EtatCivil.values()) {
			assertNotNull(ec.name(), EnumHelper.coreToWeb(ec));
		}
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
