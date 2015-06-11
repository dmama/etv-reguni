package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;

@SuppressWarnings({"JavaDoc", "deprecation"})
public class AssujettissementServiceTest extends MetierTest {

	private AssujettissementService service;
	
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(AssujettissementService.class, "assujettissementService");
	}

	/**
	 * C'est juste pour vérifier que le calcul est fait (= demande transmise au {@link AssujettissementPersonnesPhysiquesCalculator}), qui est testé
	 * beaucoup plus profondément par ailleurs...
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAssujettissementPersonnesPhysiques() throws Exception {
		final PersonnePhysique ctb = createUnForSimple();
		final List<Assujettissement> assujettissement = service.determine(ctb);
		Assert.assertNotNull(assujettissement);
		Assert.assertEquals(1, assujettissement.size());
		assertOrdinaire(date(1983, 1, 1), null, MotifFor.ARRIVEE_HC, null, assujettissement.get(0));
	}
}
