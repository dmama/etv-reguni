package ch.vd.uniregctb.documentfiscal;

import org.junit.Test;

import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.tiers.Entreprise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AutreDocumentFiscalServiceTest {

	/**
	 * Vérifie qu'un nouveau code de contrôle est attribué sur une première demande de dégrèvement.
	 */
	@Test
	public void testGetCodeControleDemandeDegrevementICI() {

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.setId(1212L);

		// une entreprise sans demande de dégrèvement
		final Entreprise entreprise = new Entreprise();

		final String codeControl = AutreDocumentFiscalServiceImpl.getCodeControleDemandeDegrevementICI(entreprise, immeuble, 2017);
		assertNotNull(codeControl);
		assertTrue(codeControl.matches("[A-Z][0-9]{5}"));   // exemple : J57738
	}

	/**
	 * [SIFISC-27973] Vérifie qu'un code de contrôle existant est réutilisé lorsqu'il existe une demande de dégrèvement pour la même période fiscale et le même immeuble.
	 */
	@Test
	public void testGetCodeControleDemandeDegrevementICIMemePeriodeFiscale() {

		final String code2015 = "X00000";
		final String code2017 = "Z71717";

		final ImmeubleRF immeuble1 = new BienFondsRF();
		immeuble1.setId(1212L);
		final ImmeubleRF immeuble2 = new BienFondsRF();
		immeuble2.setId(388389L);

		// une avec une demande de dégrèvement en 2015 sur l'immeuble 1
		final Entreprise entreprise = new Entreprise();
		final DemandeDegrevementICI demande2015Immeuble1 = new DemandeDegrevementICI();
		demande2015Immeuble1.setPeriodeFiscale(2015);
		demande2015Immeuble1.setImmeuble(immeuble1);
		demande2015Immeuble1.setCodeControle(code2015);
		entreprise.addAutreDocumentFiscal(demande2015Immeuble1);

		// le code de contrôle doit être nouvellement généré (car la période fiscale ne correspond pas)
		{
			final String codeControl = AutreDocumentFiscalServiceImpl.getCodeControleDemandeDegrevementICI(entreprise, immeuble1, 2017);
			assertNotNull(codeControl);
			assertFalse(code2015.equals(codeControl));
			assertTrue(codeControl.matches("[A-Z][0-9]{5}"));   // exemple : J57738
		}

		// on ajoute une demande de dégrèvement en 2017 sur l'immeuble 2
		final DemandeDegrevementICI demande2017Immeuble2 = new DemandeDegrevementICI();
		demande2017Immeuble2.setPeriodeFiscale(2017);
		demande2017Immeuble2.setImmeuble(immeuble2);
		demande2017Immeuble2.setCodeControle(code2017);
		entreprise.addAutreDocumentFiscal(demande2017Immeuble2);

		// le code de contrôle doit être nouvellement généré (car l'immeuble ne correspond pas)
		{
			final String codeControl = AutreDocumentFiscalServiceImpl.getCodeControleDemandeDegrevementICI(entreprise, immeuble1, 2017);
			assertNotNull(codeControl);
			assertFalse(code2015.equals(codeControl));
			assertTrue(codeControl.matches("[A-Z][0-9]{5}"));   // exemple : J57738
		}

		// on ajoute une demande de dégrèvement en 2017 sur l'immeuble 1
		final DemandeDegrevementICI demande2017Immeuble1 = new DemandeDegrevementICI();
		demande2017Immeuble1.setPeriodeFiscale(2017);
		demande2017Immeuble1.setImmeuble(immeuble1);
		demande2017Immeuble1.setCodeControle(code2017);
		entreprise.addAutreDocumentFiscal(demande2017Immeuble1);

		// le code de contrôle doit être repris de la demande de dégrèvement 2017
		{
			final String codeControl = AutreDocumentFiscalServiceImpl.getCodeControleDemandeDegrevementICI(entreprise, immeuble1, 2017);
			assertNotNull(codeControl);
			assertEquals(code2017, codeControl);
		}

		// on annule la demande de dégrèvement de 2017
		demande2017Immeuble1.setAnnule(true);

		// le code de contrôle doit quand même être repris de la demande de dégrèvement 2017
		{
			final String codeControl = AutreDocumentFiscalServiceImpl.getCodeControleDemandeDegrevementICI(entreprise, immeuble1, 2017);
			assertNotNull(codeControl);
			assertEquals(code2017, codeControl);
		}
	}
}