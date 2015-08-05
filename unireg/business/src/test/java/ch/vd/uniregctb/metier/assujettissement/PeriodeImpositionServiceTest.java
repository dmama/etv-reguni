package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.PersonnePhysique;

@SuppressWarnings({"JavaDoc"})
public class PeriodeImpositionServiceTest extends MetierTest {

	private PeriodeImpositionServiceImpl service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final AssujettissementService as = getBean(AssujettissementService.class, "assujettissementService");
		final ParametreAppService pas = getBean(ParametreAppService.class, "parametreAppService");
		service = new PeriodeImpositionServiceImpl();
		service.setAssujettissementService(as);
		service.setParametreAppService(pas);
		service.afterPropertiesSet();
	}

	/**
	 * C'est juste pour vérifier que le calcul est fait (= demande transmise au {@link AssujettissementPersonnesPhysiquesCalculator}), qui est testé
	 * beaucoup plus profondément par ailleurs...
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testPeriodesImpositionPersonnesPhysiques() throws Exception {
		final PersonnePhysique ctb = createUnForSimple();
		final List<PeriodeImposition> periodesImposition = service.determine(ctb);
		Assert.assertNotNull(periodesImposition);
		Assert.assertEquals(RegDate.get().year() - 2003 + 1, periodesImposition.size());
	}

}
