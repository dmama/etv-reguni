package ch.vd.unireg.declaration.snc;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.unireg.metier.periodeexploitation.PeriodeExploitationServiceImpl;
import ch.vd.unireg.parametrage.MockParameterAppService;
import ch.vd.unireg.regimefiscal.RegimeFiscalServiceImpl;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static ch.vd.unireg.common.WithoutSpringTest.assertEmpty;
import static ch.vd.unireg.common.WithoutSpringTest.date;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QuestionnaireSNCServiceTest {

	private QuestionnaireSNCServiceImpl service;

	@Before
	public void setUp() throws Exception {

		final ProxyServiceInfrastructureService serviceInfra = new ProxyServiceInfrastructureService();
		serviceInfra.setUpDefault();

		final RegimeFiscalServiceImpl regimeFiscalService = new RegimeFiscalServiceImpl();
		regimeFiscalService.setServiceInfra(serviceInfra);

		PeriodeExploitationServiceImpl periodeExploitationService = new PeriodeExploitationServiceImpl();
		periodeExploitationService.setParametreAppService(new MockParameterAppService());
		periodeExploitationService.setRegimeFiscalService(regimeFiscalService);

		service = new QuestionnaireSNCServiceImpl();
		service.setPeriodeExploitationService(periodeExploitationService);
	}


	/**
	 * Vérifie qu'une entreprise sans régime fiscal n'a pas de période d'exploitation.
	 */
	@Test
	public void testGetPeriodesFiscalesEntrepriseSansRegimeFiscal() {

		final Entreprise entreprise = new Entreprise();

		final Set<Integer> periodes = service.getPeriodesFiscalesTheoriquementCouvertes(entreprise, false);
		assertEmpty(periodes);
	}

	/**
	 * Vérifie qu'une entreprise avec un régime fiscal autre que <i>société de personne</i> n'a pas de période d'exploitation.
	 */
	@Test
	public void testGetPeriodesFiscalesEntrepriseAvecRegimeFiscalOrdinaire() {

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2009, 1, 1), null, RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.ORDINAIRE_PM.getCode()));

		final Set<Integer> periodes = service.getPeriodesFiscalesTheoriquementCouvertes(entreprise, false);
		assertEmpty(periodes);
	}

	/**
	 * Vérifie qu'une entreprise avec un régime fiscal <i>société de personne</i> actif avant 2009 n'a pas de période d'exploitation.
	 */
	@Test
	public void testGetPeriodesFiscalesEntrepriseAvecRegimeFiscalSPAvant2009() {

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2000, 1, 1), date(2004, 12, 31), RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.SOCIETE_PERS.getCode()));

		final Set<Integer> periodes = service.getPeriodesFiscalesTheoriquementCouvertes(entreprise, false);
		assertEmpty(periodes);
	}

	/**
	 * Vérifie qu'une entreprise sans for fiscal n'a pas de période d'exploitation.
	 */
	@Test
	public void testGetPeriodesFiscalesEntrepriseSansForFiscal() {

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2009, 1, 1), null, RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.SOCIETE_PERS.getCode()));
		entreprise.setForsFiscaux(Collections.emptySet());

		final Set<Integer> periodes = service.getPeriodesFiscalesTheoriquementCouvertes(entreprise, false);
		assertEmpty(periodes);
	}

	/**
	 * Vérifie qu'une entreprise sans for fiscal vaudois n'a pas de période d'exploitation.
	 */
	@Test
	public void testGetPeriodesFiscalesEntrepriseSansForFiscalVaudois() {

		final ForFiscalPrincipalPM ffp = new ForFiscalPrincipalPM();
		ffp.setDateDebut(date(2000, 1, 1));
		ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
		ffp.setNumeroOfsAutoriteFiscale(MockCommune.Geneve.getNoOFS());

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2009, 1, 1), null, RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.SOCIETE_PERS.getCode()));
		entreprise.addForFiscal(ffp);

		final Set<Integer> periodes = service.getPeriodesFiscalesTheoriquementCouvertes(entreprise, false);
		assertEmpty(periodes);
	}

	/**
	 * Vérifie qu'une entreprise avec un for fiscal vaudois actif avant 2009 n'a pas de période d'exploitation.
	 */
	@Test
	public void testGetPeriodesFiscalesEntrepriseAvecForFiscalVaudoisAvant2009() {

		final ForFiscalPrincipalPM ffp = new ForFiscalPrincipalPM();
		ffp.setDateDebut(date(2000, 1, 1));
		ffp.setDateFin(date(2004, 12, 31));
		ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ffp.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2009, 1, 1), null, RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.SOCIETE_PERS.getCode()));
		entreprise.addForFiscal(ffp);

		final Set<Integer> periodes = service.getPeriodesFiscalesTheoriquementCouvertes(entreprise, false);
		assertEmpty(periodes);
	}

	/**
	 * Vérifie qu'une entreprise SNC avec un régime fiscal <i>société de personne</i> et un for fiscal vaudois possède les bonnes périodes d'exploitation théoriques.
	 */
	@Test
	public void testGetPeriodesFiscalesPeriodesTheoriques() {

		final ForFiscalPrincipalPM ffp = new ForFiscalPrincipalPM();
		ffp.setDateDebut(date(2000, 1, 1));
		ffp.setDateFin(date(2018, 12, 31));
		ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ffp.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2009, 1, 1), null, RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.SOCIETE_PERS.getCode()));
		entreprise.addForFiscal(ffp);

		// théroque : à partir de 2016
		final Set<Integer> periodes = service.getPeriodesFiscalesTheoriquementCouvertes(entreprise, false);
		assertEquals(10, periodes.size());
		assertTrue(periodes.contains(2009));
		assertTrue(periodes.contains(2010));
		assertTrue(periodes.contains(2011));
		assertTrue(periodes.contains(2012));
		assertTrue(periodes.contains(2013));
		assertTrue(periodes.contains(2014));
		assertTrue(periodes.contains(2015));
		assertTrue(periodes.contains(2016));
		assertTrue(periodes.contains(2017));
		assertTrue(periodes.contains(2018));
	}

	/**
	 * Vérifie qu'une entreprise SNC avec un régime fiscal <i>société de personne</i> et un for fiscal vaudois possède les bonnes périodes d'exploitation pour l'envoi automatique.
	 */
	@Test
	public void testGetPeriodesFiscalesPeriodesEnvoiAutomatique() {

		final ForFiscalPrincipalPM ffp = new ForFiscalPrincipalPM();
		ffp.setDateDebut(date(2000, 1, 1));
		ffp.setDateFin(date(2018, 12, 31));
		ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ffp.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());

		final Entreprise entreprise = new Entreprise();
		entreprise.addRegimeFiscal(new RegimeFiscal(date(2009, 1, 1), null, RegimeFiscal.Portee.VD, MockTypeRegimeFiscal.SOCIETE_PERS.getCode()));
		entreprise.addForFiscal(ffp);

		// envoi automatique : à partir de 2016
		final Set<Integer> periodes = service.getPeriodesFiscalesTheoriquementCouvertes(entreprise, true);
		assertEquals(3, periodes.size());
		assertTrue(periodes.contains(2016));
		assertTrue(periodes.contains(2017));
		assertTrue(periodes.contains(2018));
	}
}