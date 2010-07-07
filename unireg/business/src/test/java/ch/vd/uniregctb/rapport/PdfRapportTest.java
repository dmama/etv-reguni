package ch.vd.uniregctb.rapport;

import ch.vd.uniregctb.common.WithoutSpringTest;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PdfRapportTest extends WithoutSpringTest {

	@Test
	public void testPluralize() {
		assertEquals("heure", PdfRapport.pluralize(0, "heure"));
		assertEquals("heure", PdfRapport.pluralize(1, "heure"));
		assertEquals("heures", PdfRapport.pluralize(2, "heure"));
	}

	@Test
	public void testFormatDureeExecution() {
		assertEquals("0 seconde", PdfRapport.formatDureeExecution(0, 0, 0, 0));
		assertEquals("1 seconde", PdfRapport.formatDureeExecution(0, 0, 0, 1));
		assertEquals("2 secondes", PdfRapport.formatDureeExecution(0, 0, 0, 2));

		assertEquals("1 minute et 0 seconde", PdfRapport.formatDureeExecution(0, 0, 1, 0));
		assertEquals("2 minutes et 0 seconde", PdfRapport.formatDureeExecution(0, 0, 2, 0));

		assertEquals("1 heure, 2 minutes et 0 seconde", PdfRapport.formatDureeExecution(0, 1, 2, 0));
		assertEquals("2 heures, 0 minute et 10 secondes", PdfRapport.formatDureeExecution(0, 2, 0, 10));

		assertEquals("1 jour, 0 heure, 23 minutes et 1 seconde", PdfRapport.formatDureeExecution(1, 0, 23, 1));
		assertEquals("2 jours, 10 heures, 1 minute et 45 secondes", PdfRapport.formatDureeExecution(2, 10, 1, 45));
	}
}
