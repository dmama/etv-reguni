package ch.vd.unireg.webservices.v7;

import org.junit.Test;

import ch.vd.unireg.xml.party.taxdeclaration.v5.OrdinaryTaxDeclarationType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class TypeDocumentTest extends EnumTest {

	/**
	 * msi (2009.03.03) : les enums TypeDocument entre core et web ne sont plus synchronisés. L'enum de core possède tous les types
	 * pour les LRs et les DIs, alors que celui de web se limite aux types valables pour les DIs.
	 * <p>
	 * Pour rendre de nouveau synchrones les deux enums, il faudrait changer la définition du wsdl.
	 */
	@Test
	public void testCoherence() {
		// assertEnumLengthEquals(TypeDocument.class, ch.vd.unireg.type.TypeDocument.class);
		// assertEnumConstantsEqual(TypeDocument.class, ch.vd.unireg.type.TypeDocument.class);
	}

	@Test
	public void testFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.unireg.type.TypeDocument) null));
		assertEquals(OrdinaryTaxDeclarationType.FULL_TAX_DECLARATION, EnumHelper.coreToWeb(ch.vd.unireg.type.TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH));
		assertEquals(OrdinaryTaxDeclarationType.VAUDTAX_TAX_DECLARATION, EnumHelper.coreToWeb(ch.vd.unireg.type.TypeDocument.DECLARATION_IMPOT_VAUDTAX));
		assertEquals(OrdinaryTaxDeclarationType.IMMOVABLE_PROPERTY_OTHER_CANTON_TAX_DECLARATION, EnumHelper.coreToWeb(ch.vd.unireg.type.TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE));
		assertEquals(OrdinaryTaxDeclarationType.EXPENDITURE_BASED_TAX_DECLARATION, EnumHelper.coreToWeb(ch.vd.unireg.type.TypeDocument.DECLARATION_IMPOT_DEPENSE));
	}

}
