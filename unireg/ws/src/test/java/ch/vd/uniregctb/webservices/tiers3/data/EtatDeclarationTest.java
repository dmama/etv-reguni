package ch.vd.uniregctb.webservices.tiers3.data;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.EtatDeclarationEchue;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.webservices.tiers3.EnumTest;
import ch.vd.uniregctb.webservices.tiers3.EtatDeclaration;
import ch.vd.uniregctb.webservices.tiers3.TypeEtatDeclaration;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;


@SuppressWarnings({"JavaDoc"})
public class EtatDeclarationTest extends EnumTest {

	@Test
	public void testTypeCoherence() {
		assertEnumLengthEquals(TypeEtatDeclaration.class, ch.vd.uniregctb.type.TypeEtatDeclaration.class);
		assertEnumConstantsEqual(TypeEtatDeclaration.class, ch.vd.uniregctb.type.TypeEtatDeclaration.class);
	}

	@Test
	public void testTypeFromValue() {
		assertNull(EnumHelper.coreToWeb((ch.vd.uniregctb.type.TypeEtatDeclaration) null));
		assertEquals(TypeEtatDeclaration.EMISE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration.EMISE));
		assertEquals(TypeEtatDeclaration.SOMMEE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration.SOMMEE));
		assertEquals(TypeEtatDeclaration.ECHUE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration.ECHUE));
		assertEquals(TypeEtatDeclaration.RETOURNEE, EnumHelper.coreToWeb(ch.vd.uniregctb.type.TypeEtatDeclaration.RETOURNEE));
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
			final EtatDeclaration etatWeb = DeclarationBuilder.newEtatDeclaration(sommee);

			assertEquals(DataHelper.coreToWeb(dateEnvoiCourrier), etatWeb.getDateObtention());
		}

		// Etat "EMISE"
		{
			final RegDate dateObtention = RegDate.get().addDays(-10);

			final EtatDeclarationEmise emise = new EtatDeclarationEmise(dateObtention);
			final EtatDeclaration etatWeb = DeclarationBuilder.newEtatDeclaration(emise);

			assertEquals(DataHelper.coreToWeb(dateObtention), etatWeb.getDateObtention());
		}

		// Etat "RETOURNEE"
		{
			final RegDate dateObtention = RegDate.get().addDays(-10);

			final EtatDeclarationRetournee retournee = new EtatDeclarationRetournee(dateObtention);
			final EtatDeclaration etatWeb = DeclarationBuilder.newEtatDeclaration(retournee);

			assertEquals(DataHelper.coreToWeb(dateObtention), etatWeb.getDateObtention());
		}

		// Etat "ECHUE"
		{
			final RegDate dateObtention = RegDate.get().addDays(-10);

			final EtatDeclarationEchue echue = new EtatDeclarationEchue(dateObtention);
			final EtatDeclaration etatWeb = DeclarationBuilder.newEtatDeclaration(echue);

			assertEquals(DataHelper.coreToWeb(dateObtention), etatWeb.getDateObtention());
		}
	}
}
