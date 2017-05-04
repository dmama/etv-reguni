package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.MotifFor;

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
		service.setTiersService(tiersService);
		service.setParametreAppService(pas);
		service.setRegimeFiscalService(regimeFiscalService);
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

	/**
	 * C'est juste pour vérifier que le calcul est fait (= demande transmise au {@link AssujettissementPersonnesMoralesCalculator}), qui est testé
	 * beaucoup plus profondément par ailleurs...
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testPeriodesImpositionPersonnesMorales() throws Exception {
		final Entreprise ctb = addEntrepriseInconnueAuCivil();
		addRaisonSociale(ctb, date(1984, 1, 1), null, "Toto SA");
		addFormeJuridique(ctb, date(1984, 1, 1), null, FormeJuridiqueEntreprise.SA);
		addRegimeFiscalVD(ctb, date(2009, 1, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(ctb, date(2009, 1, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(ctb, date(1984, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addBouclement(ctb, date(1984, 1, 1), DayMonth.get(12, 31), 12);
		final List<PeriodeImposition> periodesImposition = service.determine(ctb);
		Assert.assertNotNull(periodesImposition);
		Assert.assertEquals(RegDate.get().year() - 2009 + 1, periodesImposition.size());
	}
}
