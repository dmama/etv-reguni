package ch.vd.uniregctb.evenement.civil.interne.ignore;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

public class DeclarationEtatCompletIndividuECH99HandlerTest extends AbstractEvenementHandlerTest {

	@Test
	public void testCompleteness() {
		final EvenementCivilInterne evt = new MockDeclarationEtatCompletIndividuECH99();
		final DeclarationEtatCompletIndividuECH99Handler handler = new DeclarationEtatCompletIndividuECH99Handler();

		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		handler.checkCompleteness(evt, erreurs, warnings);

		assertEmpty(erreurs);
		assertEmpty(warnings);
	}

	@Test
	public void testHandle() {
		final EvenementCivilInterne evt = new MockDeclarationEtatCompletIndividuECH99();
		final DeclarationEtatCompletIndividuECH99Handler handler = new DeclarationEtatCompletIndividuECH99Handler();

		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		handler.handle(evt, warnings);

		assertEmpty(warnings);
	}
}
