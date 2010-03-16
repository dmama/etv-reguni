package ch.vd.uniregctb.webservices.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import org.junit.Test;

import ch.vd.uniregctb.webservices.tiers.impl.EnumHelper;


public class TypeDocumentTest extends EnumTest {

	/**
	 * msi (2009.03.03) : les enums TypeDocument entre core et web ne sont plus synchronisés. L'enum de core possède tous les types
	 * pour les LRs et les DIs, alors que celui de web se limite aux types valables pour les DIs.
	 * <p>
	 * Pour rendre de nouveau synchrone les deux enums, il faudrait changer la définition du wsdl.
	 */
	@Test
	public void testCoherence() {
		// assertEnumLengthEquals(TypeDocument.class, ch.vd.uniregctb.type.TypeDocument.class);
		// assertEnumConstantsEqual(TypeDocument.class, ch.vd.uniregctb.type.TypeDocument.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeDocument) null));
		assertEquals(TypeDocument.DECLARATION_IMPOT_COMPLETE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH));
		assertEquals(TypeDocument.DECLARATION_IMPOT_VAUDTAX, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeDocument.DECLARATION_IMPOT_VAUDTAX));
		assertEquals(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE));
		assertEquals(TypeDocument.DECLARATION_IMPOT_DEPENSE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeDocument.DECLARATION_IMPOT_DEPENSE));
	}

}
