package ch.vd.unireg.webservices.party4.data;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxpayer.v2.MaritalStatus;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.webservices.party4.EnumTest;
import ch.vd.unireg.webservices.party4.impl.EnumHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class MaritalStatusTest extends EnumTest {

// [SIFISC-6042] Les deux enums ne sont plus égaux depuis l'ajout de la constante PARTENARIAT_SEPARE
//	@Test
//	public void testCoherence() {
//		assertEnumLengthEquals(MaritalStatus.class, ch.vd.unireg.type.EtatCivil.class);
//	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.unireg.type.EtatCivil) null));
		assertEquals(MaritalStatus.SINGLE, EnumHelper.coreToWeb(ch.vd.unireg.type.EtatCivil.CELIBATAIRE));
		assertEquals(MaritalStatus.MARRIED, EnumHelper.coreToWeb(ch.vd.unireg.type.EtatCivil.MARIE));
		assertEquals(MaritalStatus.WIDOWED, EnumHelper.coreToWeb(ch.vd.unireg.type.EtatCivil.VEUF));
		assertEquals(MaritalStatus.REGISTERED_PARTNER, EnumHelper.coreToWeb(ch.vd.unireg.type.EtatCivil.LIE_PARTENARIAT_ENREGISTRE));
		assertEquals(MaritalStatus.NOT_MARRIED, EnumHelper.coreToWeb(ch.vd.unireg.type.EtatCivil.NON_MARIE));
		assertEquals(MaritalStatus.PARTNERSHIP_ABOLISHED_BY_DEATH, EnumHelper.coreToWeb(ch.vd.unireg.type.EtatCivil.PARTENARIAT_DISSOUS_DECES));
		assertEquals(MaritalStatus.PARTNERSHIP_ABOLISHED_BY_LAW, EnumHelper.coreToWeb(ch.vd.unireg.type.EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT));
		assertEquals(MaritalStatus.DIVORCED, EnumHelper.coreToWeb(ch.vd.unireg.type.EtatCivil.DIVORCE));
		assertEquals(MaritalStatus.SEPARATED, EnumHelper.coreToWeb(ch.vd.unireg.type.EtatCivil.SEPARE));
		assertEquals(MaritalStatus.SEPARATED, EnumHelper.coreToWeb(EtatCivil.PARTENARIAT_SEPARE));
	}
}
