package ch.vd.uniregctb.couple.manager;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import org.junit.Test;

import ch.vd.uniregctb.common.WebitTest;

import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class CoupleControllerTest extends WebitTest {

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

		final HtmlPage page = getHtmlPage("/couple/create.do");
		assertNotNull(page);

		// recherche le bouton qui permet de soumettre la forme
		HtmlInput pp1Id = null;
		HtmlInput pp2Id = null;
		HtmlInput mcId = null;
		HtmlInput sauver = null;
		HtmlRadioButtonInput nouveauMC = null;
		for (HtmlElement element : page.getHtmlElementDescendants()) {
			if (element instanceof HtmlInput) {
				HtmlInput input = (HtmlInput) element;
				if (input.getValueAttribute().equals("Sauver")) {
					sauver = input;
				}
				else if (input.getId().equals("pp1Id")) {
					pp1Id = input;
				}
				else if (input.getId().equals("pp2Id")) {
					pp2Id = input;
				}
				else if (input.getId().equals("mcId")) {
					mcId = input;
				}
				else if (element instanceof HtmlRadioButtonInput) {
					HtmlRadioButtonInput e =(HtmlRadioButtonInput) element;
					if (e.getId().equals("nouveauMC")) {
						nouveauMC = e;
					}
				}
			}
		}
		assertNotNull(pp1Id);
		assertNotNull(pp2Id);
		assertNotNull(mcId);
		assertNotNull(nouveauMC);
		assertNotNull(sauver);

		// remplir le formulaire
		pp1Id.setValueAttribute(jeremy.toString());
		pp2Id.setValueAttribute(eun.toString());
		mcId.setValueAttribute(menage.toString());
		nouveauMC.setValueAttribute("false");

		// soumet le formulaire
		final HtmlPage pageResultat = (HtmlPage) sauver.click();
		assertNotNull(pageResultat);

		// on devrait avoir un message d'erreur
		assertContains("L'action n'a pas pu être effectuée à cause des erreurs suivantes", pageResultat);

		// On s'assure que la transaction a bien été rollée-back, c'est-à-dire que le trois tiers sont toujours des personnes physiques.
		assertNatureTiers("Habitant", jeremy);
		assertNatureTiers("Habitant", eun);
		assertNatureTiers("NonHabitant", menage);
	}
}
