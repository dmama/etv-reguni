package ch.vd.uniregctb.evenement.civil.interne.ignore;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class EvenementCivilIgnoreTest extends AbstractEvenementCivilInterneTest {

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandle() throws EvenementCivilException {
		final EvenementCivilInterne evt = new EvenementCivilIgnore(TypeEvenementCivil.ETAT_COMPLET, context);

		final List<EvenementCivilExterneErreur> warnings = new ArrayList<EvenementCivilExterneErreur>();
		evt.handle(warnings);

		assertEmpty(warnings);
	}
}
