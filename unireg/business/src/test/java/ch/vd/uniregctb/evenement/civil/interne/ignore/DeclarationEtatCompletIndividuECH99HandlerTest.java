package ch.vd.uniregctb.evenement.civil.interne.ignore;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.vd.uniregctb.evenement.AbstractEvenementHandlerTest;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilIgnoreAdapter;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class DeclarationEtatCompletIndividuECH99HandlerTest extends AbstractEvenementHandlerTest {

	@Test
	public void testCompleteness() {
		final EvenementCivilInterne evt = new EvenementCivilIgnoreAdapter(TypeEvenementCivil.ETAT_COMPLET, context);

		final List<EvenementCivilExterneErreur> erreurs = new ArrayList<EvenementCivilExterneErreur>();
		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		evt.checkCompleteness(erreurs, warnings);

		assertEmpty(erreurs);
		assertEmpty(warnings);
	}

	@Test
	public void testHandle() {
		final EvenementCivilInterne evt = new EvenementCivilIgnoreAdapter(TypeEvenementCivil.ETAT_COMPLET, context);

		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		evt.handle(warnings);

		assertEmpty(warnings);
	}
}
