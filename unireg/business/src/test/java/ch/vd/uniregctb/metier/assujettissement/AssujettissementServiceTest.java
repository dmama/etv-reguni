package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

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
		assertOrdinaire(date(1984, 1, 1), null, MotifFor.ARRIVEE_HS, null, assujettissement.get(0));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAssujeHCAvecVenteImmeublesuiviRuptureHS() throws Exception {

		final int annee = 2015;
		final RegDate dateArriveeHorsCanton = date(2013, 1, 22);
		final RegDate dateDepartHS = date(2015, 7, 31);
		final PersonnePhysique pp = createContribuableSansFor();

		final RegDate dateDepartHorsCanton = date(2014, 10, 02);
		addForPrincipal(pp, dateArriveeHorsCanton, MotifFor.ARRIVEE_HC, dateDepartHorsCanton, MotifFor.DEPART_HC, MockCommune.Moudon, ModeImposition.ORDINAIRE);

		addForPrincipal(pp, dateDepartHorsCanton.addDays(1), MotifFor.DEPART_HC, dateDepartHS, MotifFor.DEPART_HS, MockCommune.Geneve);
		addForPrincipal(pp, dateDepartHS.addDays(1), MotifFor.DEPART_HS, MockPays.France);
		addForSecondaire(pp, date(2013,2,7), MotifFor.ACHAT_IMMOBILIER, date(2015,6,23),MotifFor.VENTE_IMMOBILIER, MockCommune.Moudon.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		final List<Assujettissement> ass = service.determine(pp, annee);
		Assert.assertNotNull(ass);
		Assert.assertEquals(1, ass.size());

		assertHorsSuisse(date(annee, 1, 1), dateDepartHS, MotifFor.DEPART_HS, MotifFor.VENTE_IMMOBILIER, ass.get(0));
	}
}
