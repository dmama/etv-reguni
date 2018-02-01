package ch.vd.unireg.remarque;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.WithoutSpringTest;

public class RemarqueViewTest extends WithoutSpringTest{

	@Test
	public void testTrimLines() throws Exception {
		// vu dans le contribuable 108.228.48 -> partait en boucle infinie...
		{
			final String texte = "Pour les besoins de l'impôt Source - arrivée HC au 01.01.2010\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nPour les besoins de l'impôt Source - arrivée HC au 01.01.2010\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nPour les besoins de l'impôt Source - arrivée HC au 01.01.2010\n\n";
			final String trimmed = RemarqueView.trimLines(texte);
			Assert.assertEquals("Pour les besoins de l'impôt Source - arrivée HC au 01.01.2010\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nPour les besoins de l'impôt Source - arrivée HC au 01.01.2010\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nPour les besoins de l'impôt Source - arrivée HC au 01.01.2010", trimmed);
		}

		// lignes à la fin
		{
			final String texte = "Pour les besoins de l'impôt Source - arrivée HC au 01.01.2010\n\n\n\n";
			final String trimmed = RemarqueView.trimLines(texte);
			Assert.assertEquals("Pour les besoins de l'impôt Source - arrivée HC au 01.01.2010", trimmed);
		}

		// lignes au début
		{
			final String texte = "\n\n\nPour les besoins de l'impôt Source - arrivée HC au 01.01.2010";
			final String trimmed = RemarqueView.trimLines(texte);
			Assert.assertEquals("Pour les besoins de l'impôt Source - arrivée HC au 01.01.2010", trimmed);
		}

		// lignes aux début et à la fin
		{
			final String texte = "\n\n\nPour les besoins de l'impôt Source - arrivée HC au 01.01.2010\n\n\n";
			final String trimmed = RemarqueView.trimLines(texte);
			Assert.assertEquals("Pour les besoins de l'impôt Source - arrivée HC au 01.01.2010", trimmed);
		}
	}
}
