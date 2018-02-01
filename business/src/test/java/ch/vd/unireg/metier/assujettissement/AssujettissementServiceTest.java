package ch.vd.unireg.metier.assujettissement;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.MotifFor;

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
		assertOrdinaire(date(1983, 1, 1), null, MotifAssujettissement.ARRIVEE_HC, null, assujettissement.get(0));
	}

	/**
	 * C'est juste pour vérifier que le calcul est fait (= demande transmise au {@link AssujettissementPersonnesMoralesCalculator}), qui est testé
	 * beaucoup plus profondément par ailleurs...
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAssujettissementPersonnesMorales() throws Exception {
		final Entreprise ctb = addEntrepriseInconnueAuCivil();
		addRegimeFiscalVD(ctb, date(2009, 1, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(ctb, date(2009, 1, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(ctb, date(1984, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		addBouclement(ctb, date(1984, 1, 1), DayMonth.get(12, 31), 12);
		final List<Assujettissement> assujettissement = service.determine(ctb);
		Assert.assertNotNull(assujettissement);
		Assert.assertEquals(1, assujettissement.size());
		assertOrdinaire(date(1984, 1, 1), null, MotifAssujettissement.ARRIVEE_HS, null, assujettissement.get(0));
	}
}
