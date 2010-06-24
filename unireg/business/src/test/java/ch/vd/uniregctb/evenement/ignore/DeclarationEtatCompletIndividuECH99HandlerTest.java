package ch.vd.uniregctb.evenement.ignore;

import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DeclarationEtatCompletIndividuECH99HandlerTest extends AbstractEvenementHandlerTest {

	@Test
	public void testCompleteness() {
		final EvenementCivil evt = new MockDeclarationEtatCompletIndividuECH99();
		final DeclarationEtatCompletIndividuECH99Handler handler = new DeclarationEtatCompletIndividuECH99Handler();

		final List<EvenementCivilErreur> erreurs = new ArrayList<EvenementCivilErreur>();
		final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		handler.checkCompleteness(evt, erreurs, warnings);

		assertEmpty(erreurs);
		assertEmpty(warnings);
	}

	@Test
	public void testHandle() {
		final EvenementCivil evt = new MockDeclarationEtatCompletIndividuECH99();
		final DeclarationEtatCompletIndividuECH99Handler handler = new DeclarationEtatCompletIndividuECH99Handler();

		final List<EvenementCivilErreur> warnings = new ArrayList<EvenementCivilErreur>();
		handler.handle(evt, warnings);

		assertEmpty(warnings);
	}
}
