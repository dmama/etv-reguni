package ch.vd.unireg.declaration.snc;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscal;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class QuestionnaireSNCServiceTest extends BusinessTest {

	private QuestionnaireSNCServiceImpl service;

	@Before
	public void setUp() throws Exception {
		service = getBean(QuestionnaireSNCServiceImpl.class, "qsncService");
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

	/**
	 * Ce test vérifie que la méthode 'ajouterDelai' fonctionne bien dans le cas passant.
	 */
	@Test
	public void testAjouterDelai() throws Exception {

		// on crée un questionnaire SNC
		final Long qsncId = doInNewTransaction(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			final PeriodeFiscale periode = addPeriodeFiscale(2017);
			final QuestionnaireSNC qsnc = addQuestionnaireSNC(entreprise, periode);
			return qsnc.getId();
		});

		final RegDate delaiAccordeAu = RegDate.get().addMonths(3);
		final RegDate dateDemande = RegDate.get(2018, 3, 14);

		// on ajoute un délai
		final Long idDelai = doInNewTransaction(status -> {
			//noinspection CodeBlock2Expr
			return service.ajouterDelai(qsncId, dateDemande, delaiAccordeAu, EtatDelaiDocumentFiscal.ACCORDE);
		});

		// on vérifie que le délai est bien ajouté
		doInNewTransaction(status -> {
			final QuestionnaireSNC qsnc = hibernateTemplate.get(QuestionnaireSNC.class, qsncId);
			assertNotNull(qsnc);
			final Set<DelaiDocumentFiscal> delais = qsnc.getDelais();
			assertEquals(1, delais.size());

			final DelaiDocumentFiscal delai0 = delais.iterator().next();
			assertNotNull(delai0);
			assertEquals(dateDemande, delai0.getDateDemande());
			assertEquals(RegDate.get(), delai0.getDateTraitement());
			assertEquals(delaiAccordeAu, delai0.getDelaiAccordeAu());
			assertEquals(idDelai, delai0.getId());
			return null;
		});
	}
}