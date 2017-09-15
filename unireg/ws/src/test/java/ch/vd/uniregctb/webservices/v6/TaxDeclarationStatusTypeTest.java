package ch.vd.uniregctb.webservices.v6;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v4.TaxDeclarationStatusType;
import ch.vd.uniregctb.declaration.EtatDeclarationEchue;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;
import ch.vd.uniregctb.xml.party.v4.TaxDeclarationBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


@SuppressWarnings({"JavaDoc"})
public class TaxDeclarationStatusTypeTest extends EnumTest {

	@Test
	public void testTypeCoherence() {
		assertEnumLengthEquals(TaxDeclarationStatusType.class, TypeEtatDocumentFiscal.class);

		// vérification que toutes les valeurs sont mappées sur quelque chose
		for (TypeEtatDocumentFiscal type : TypeEtatDocumentFiscal.values()) {
			assertNotNull(type.name(), EnumHelper.coreToWeb(type));
		}
	}

	@Test
	public void testTypeFromValue() {
		assertNull(EnumHelper.coreToWeb((TypeEtatDocumentFiscal) null));
		assertEquals(TaxDeclarationStatusType.SENT, EnumHelper.coreToWeb(TypeEtatDocumentFiscal.EMISE));
		assertEquals(TaxDeclarationStatusType.SUMMONS_SENT, EnumHelper.coreToWeb(TypeEtatDocumentFiscal.SOMMEE));
		assertEquals(TaxDeclarationStatusType.EXPIRED, EnumHelper.coreToWeb(TypeEtatDocumentFiscal.ECHUE));
		assertEquals(TaxDeclarationStatusType.RETURNED, EnumHelper.coreToWeb(TypeEtatDocumentFiscal.RETOURNEE));
	}

	/**
	 * [UNIREG-3407] Pour les états de sommation, c'est la date de l'envoi du courrier qu'il faut renvoyer
	 */
	@Test
	public void testDateObtention() throws Exception {

		// Etat "SOMMEE"
		{
			final RegDate dateObtention = RegDate.get().addDays(-10);
			final RegDate dateEnvoiCourrier = RegDate.get().addDays(-5);

			final EtatDeclarationSommee sommee = new EtatDeclarationSommee(dateObtention, dateEnvoiCourrier, null);
			final TaxDeclarationStatus etatWeb = TaxDeclarationBuilder.newTaxDeclarationStatus(sommee);

			assertEquals(ch.vd.uniregctb.xml.DataHelper.coreToXMLv2(dateEnvoiCourrier), etatWeb.getDateFrom());
		}

		// Etat "EMISE"
		{
			final RegDate dateObtention = RegDate.get().addDays(-10);

			final EtatDeclarationEmise emise = new EtatDeclarationEmise(dateObtention);
			final TaxDeclarationStatus etatWeb = TaxDeclarationBuilder.newTaxDeclarationStatus(emise);

			assertEquals(ch.vd.uniregctb.xml.DataHelper.coreToXMLv2(dateObtention), etatWeb.getDateFrom());
		}

		// Etat "RETOURNEE"
		{
			final RegDate dateObtention = RegDate.get().addDays(-10);

			final EtatDeclarationRetournee retournee = new EtatDeclarationRetournee(dateObtention, "TEST");
			final TaxDeclarationStatus etatWeb = TaxDeclarationBuilder.newTaxDeclarationStatus(retournee);

			assertEquals(ch.vd.uniregctb.xml.DataHelper.coreToXMLv2(dateObtention), etatWeb.getDateFrom());
		}

		// Etat "ECHUE"
		{
			final RegDate dateObtention = RegDate.get().addDays(-10);

			final EtatDeclarationEchue echue = new EtatDeclarationEchue(dateObtention);
			final TaxDeclarationStatus etatWeb = TaxDeclarationBuilder.newTaxDeclarationStatus(echue);

			assertEquals(ch.vd.uniregctb.xml.DataHelper.coreToXMLv2(dateObtention), etatWeb.getDateFrom());
		}
	}
}
