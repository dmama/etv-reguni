package ch.vd.uniregctb.couple.manager;

import java.util.Iterator;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.Test;

import ch.vd.uniregctb.common.WebitTest;

import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class CoupleRecapManagerTest extends WebitTest {

	private static final String DBUNIT_FILENAME = "UNIREG-1521.xml";

	private static boolean alreadySetUp = false;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			alreadySetUp = true;

			loadDatabase(DBUNIT_FILENAME);
		}
	}

	/**
	 * [UNIREG-1521] on s'assure que la transaction est bien rollée-back lorsque le ménage commun résultant d'un regroupement ne valide pas.
	 */
	@Test
	public void testTranformationNHEnMenageCommunEtRollback() throws Exception {

		final Long jeremy = 10074118L;
		final Long eun = 10533052L;
		final Long menage = 64415701L;

		// On s'assure que les données d'entrée sont correctes, c'est-à-dire que le trois tiers sont des personnes physiques.
		assertNatureTiers("Habitant", jeremy);
		assertNatureTiers("Habitant", eun);
		assertNatureTiers("NonHabitant", menage);

		final HtmlPage page = getHtmlPage("/couple/recap.do?numeroPP1=" + jeremy + "&numeroPP2=" + eun + "&numeroCTB=" + menage);
		assertNotNull(page);

		// recherche le bouton qui permet de soumettre la forme
		HtmlInput sauver = null;
		for (HtmlElement element : page.getHtmlElementDescendants()) {
			if (element instanceof HtmlInput) {
				HtmlInput input = (HtmlInput) element;
				if (input.getValueAttribute().equals("Sauver")) {
					sauver = input;
					break;
				}
			}
		}
		assertNotNull(sauver);

		// soumet la forme
		final HtmlPage pageResultat = (HtmlPage) sauver.click();
		assertNotNull(pageResultat);

		// on devrait avoir un message d'erreur
		assertContains("Le for fiscal [ForFiscalPrincipal (2001.03.05 - 2003.01.06)] ne peut pas exister alors que le tiers [10074118] appartient à un ménage-commun", pageResultat);

		// On s'assure que la transaction a bien été rollée-back, c'est-à-dire que le trois tiers sont toujours des personnes physiques.
		assertNatureTiers("Habitant", jeremy);
		assertNatureTiers("Habitant", eun);
		assertNatureTiers("NonHabitant", menage);
	}
}
