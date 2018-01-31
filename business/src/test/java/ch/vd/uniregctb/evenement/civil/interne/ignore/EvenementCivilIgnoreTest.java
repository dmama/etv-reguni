package ch.vd.uniregctb.evenement.civil.interne.ignore;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.MessageCollector;

public class EvenementCivilIgnoreTest extends AbstractEvenementCivilInterneTest {

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testHandle() throws EvenementCivilException {
		final EvenementCivilInterne evt = new EvenementCivilIgnore(context);

		final MessageCollector collector = buildMessageCollector();
		evt.handle(collector);
		assertEmpty(collector.getWarnings());
	}
}
