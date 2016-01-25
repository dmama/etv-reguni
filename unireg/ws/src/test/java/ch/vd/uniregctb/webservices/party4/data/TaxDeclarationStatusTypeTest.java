package ch.vd.uniregctb.webservices.party4.data;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.taxdeclaration.v2.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v2.TaxDeclarationStatusType;
import ch.vd.uniregctb.declaration.EtatDeclarationEchue;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.webservices.party4.EnumTest;
import ch.vd.uniregctb.webservices.party4.impl.EnumHelper;
import ch.vd.uniregctb.xml.party.v2.TaxDeclarationBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


@SuppressWarnings({"JavaDoc"})
public class TaxDeclarationStatusTypeTest extends EnumTest {

	@Test
	public void testTypeCoherence() {

		// deux types ne sont pas envoyés par cette version du WS : RAPPELEE et SUSPENDUE
		final Set<TypeEtatDeclaration> ignored = EnumSet.of(TypeEtatDeclaration.RAPPELEE, TypeEtatDeclaration.SUSPENDUE);
		assertEquals(TaxDeclarationStatusType.values().length + ignored.size(), TypeEtatDeclaration.values().length);

		// vérification que toutes les valeurs officiellement renvoyées sont mappées sur quelque chose
		for (TypeEtatDeclaration type : TypeEtatDeclaration.values()) {
			if (!ignored.contains(type)) {
				assertNotNull(type.name(), EnumHelper.coreToWeb(type));
			}
		}
	}

	@Test
	public void testTypeFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeEtatDeclaration) null));
		assertEquals(TaxDeclarationStatusType.SENT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration.EMISE));
		assertEquals(TaxDeclarationStatusType.SUMMONS_SENT, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration.SOMMEE));
		assertEquals(TaxDeclarationStatusType.EXPIRED, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration.ECHUE));
		assertEquals(TaxDeclarationStatusType.RETURNED, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration.RETOURNEE));
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

			final EtatDeclarationSommee sommee = new EtatDeclarationSommee(dateObtention, dateEnvoiCourrier);
			final TaxDeclarationStatus etatWeb = TaxDeclarationBuilder.newTaxDeclarationStatus(sommee);

			assertEquals(ch.vd.uniregctb.xml.DataHelper.coreToXMLv1(dateEnvoiCourrier), etatWeb.getDateFrom());
		}

		// Etat "EMISE"
		{
			final RegDate dateObtention = RegDate.get().addDays(-10);

			final EtatDeclarationEmise emise = new EtatDeclarationEmise(dateObtention);
			final TaxDeclarationStatus etatWeb = TaxDeclarationBuilder.newTaxDeclarationStatus(emise);

			assertEquals(ch.vd.uniregctb.xml.DataHelper.coreToXMLv1(dateObtention), etatWeb.getDateFrom());
		}

		// Etat "RETOURNEE"
		{
			final RegDate dateObtention = RegDate.get().addDays(-10);

			final EtatDeclarationRetournee retournee = new EtatDeclarationRetournee(dateObtention, "TEST");
			final TaxDeclarationStatus etatWeb = TaxDeclarationBuilder.newTaxDeclarationStatus(retournee);

			assertEquals(ch.vd.uniregctb.xml.DataHelper.coreToXMLv1(dateObtention), etatWeb.getDateFrom());
		}

		// Etat "ECHUE"
		{
			final RegDate dateObtention = RegDate.get().addDays(-10);

			final EtatDeclarationEchue echue = new EtatDeclarationEchue(dateObtention);
			final TaxDeclarationStatus etatWeb = TaxDeclarationBuilder.newTaxDeclarationStatus(echue);

			assertEquals(ch.vd.uniregctb.xml.DataHelper.coreToXMLv1(dateObtention), etatWeb.getDateFrom());
		}
	}
}
