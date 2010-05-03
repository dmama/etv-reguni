package ch.vd.uniregctb.webservices.tiers2;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers2.data.EtatDeclaration;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;


public class EtatDeclarationTest extends EnumTest {

	@Test
	public void testTypeCoherence() {
		assertEnumLengthEquals(EtatDeclaration.Type.class, ch.vd.uniregctb.type.TypeEtatDeclaration.class);
		assertEnumConstantsEqual(EtatDeclaration.Type.class, ch.vd.uniregctb.type.TypeEtatDeclaration.class);
	}

	@Test
	public void testTypeFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeEtatDeclaration) null));
		assertEquals(EtatDeclaration.Type.EMISE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration.EMISE));
		assertEquals(EtatDeclaration.Type.SOMMEE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration.SOMMEE));
		assertEquals(EtatDeclaration.Type.ECHUE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration.ECHUE));
		assertEquals(EtatDeclaration.Type.RETOURNEE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration.RETOURNEE));
	}
}
