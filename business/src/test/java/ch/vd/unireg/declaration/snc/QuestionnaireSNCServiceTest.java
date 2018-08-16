package ch.vd.unireg.declaration.snc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
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
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
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
	@Test
	public void testMonoQuittancements() throws Exception {

		class Ids {
			public long nonoId;
		}
		final Ids ids = new Ids();

		// les quittancements avec cette source ne doivent pas être multiples
		final String SOURCE = "E-DIPM";
		service.setSourcesMonoQuittancement(new HashSet<>(Collections.singletonList(SOURCE)));

		// Création d'une SNC et de son questionnaire
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PeriodeFiscale periode2017 = addPeriodeFiscale(2017);

				// Une SNC quelconque
				Entreprise nono = addEntrepriseInconnueAuCivil();
				ids.nonoId = nono.getNumero();
				final ForFiscalPrincipalPM ffp = new ForFiscalPrincipalPM();
				ffp.setDateDebut(date(2003, 1, 1));
				ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
				ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				ffp.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
				addQuestionnaireSNC(nono,periode2017);

				return null;
			}
		});

		// 1er quittance du questionnaire
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Entreprise nono = hibernateTemplate.get(Entreprise.class, ids.nonoId);
				final List<QuestionnaireSNC> qsncs = nono.getDeclarationsDansPeriode(QuestionnaireSNC.class, 2017, false);
				assertEquals(1, qsncs.size());
				final QuestionnaireSNC qsnc = qsncs.get(0);
				assertNotNull(qsnc);
				service.quittancerQuestionnaire(qsnc, date(2018, 5, 12), SOURCE);
				return null;
			}
		});

		// On s'assure que l'état retourné est bien enregistré
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Entreprise nono = hibernateTemplate.get(Entreprise.class, ids.nonoId);
				final List<QuestionnaireSNC> qsncs = nono.getDeclarationsDansPeriode(QuestionnaireSNC.class, 2017, false);
				assertEquals(1, qsncs.size());

				final QuestionnaireSNC qsnc = qsncs.get(0);
				assertNotNull(qsnc);
				assertEquals(date(2018, 5, 12), qsnc.getDateRetour());

				final Set<EtatDeclaration> etats = qsnc.getEtatsDeclaration();
				assertNotNull(etats);
				assertEquals(1, etats.size());

				final Iterator<EtatDeclaration> iterator = etats.iterator();

				final EtatDeclarationRetournee etat0 = (EtatDeclarationRetournee) iterator.next();
				assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat0.getEtat());
				assertEquals(date(2018, 5, 12), etat0.getDateObtention());
				assertEquals(SOURCE, etat0.getSource());
				assertFalse(etat0.isAnnule());

				// l'état retourné est le dernier, comme il se doit
				assertSame(etat0, qsnc.getDernierEtatDeclaration());
				assertSame(etat0, qsnc.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE));
				return null;
			}
		});

		// 2ème quittance du questionnaire, source différence
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Entreprise nono = hibernateTemplate.get(Entreprise.class, ids.nonoId);
				final List<QuestionnaireSNC> qsncs = nono.getDeclarationsDansPeriode(QuestionnaireSNC.class, 2017, false);
				assertEquals(1, qsncs.size());
				final QuestionnaireSNC qsnc = qsncs.get(0);
				assertNotNull(qsnc);
				service.quittancerQuestionnaire(qsnc, date(2018, 6, 12), "MANUEL");
				return null;
			}
		});


		// 2ème quittance du questionnaire, source initiale qui ne supporte pas le multi-quittancement
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Entreprise nono = hibernateTemplate.get(Entreprise.class, ids.nonoId);
				final List<QuestionnaireSNC> qsncs = nono.getDeclarationsDansPeriode(QuestionnaireSNC.class, 2017, false);
				assertEquals(1, qsncs.size());
				final QuestionnaireSNC qsnc = qsncs.get(0);
				assertNotNull(qsnc);
				service.quittancerQuestionnaire(qsnc, date(2018, 6, 23), SOURCE);
				return null;
			}
		});


		// on s'assure maintenant que premier quittancement de la source est bien annulé au profit du second, mais que les quittancements de sources annexes n'ont pas été touchés
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Entreprise nono = hibernateTemplate.get(Entreprise.class, ids.nonoId);
				final List<QuestionnaireSNC> qsncs = nono.getDeclarationsDansPeriode(QuestionnaireSNC.class, 2017, false);
				assertEquals(1, qsncs.size());

				final QuestionnaireSNC qsnc = qsncs.get(0);
				assertNotNull(qsnc);
				assertEquals(date(2018, 6, 23), qsnc.getDateRetour());

				final List<EtatDeclaration> etats = qsnc.getEtatsDeclarationSorted();
				assertNotNull(etats);
				assertEquals(3, etats.size());

				final EtatDeclarationRetournee etat0 = (EtatDeclarationRetournee) etats.get(0);
				assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat0.getEtat());
				assertEquals(date(2018, 5, 12), etat0.getDateObtention());
				assertEquals(SOURCE, etat0.getSource());
				assertTrue(etat0.isAnnule());

				final EtatDeclarationRetournee etat1 = (EtatDeclarationRetournee) etats.get(1);
				assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat1.getEtat());
				assertEquals(date(2018, 6, 12), etat1.getDateObtention());
				assertEquals("MANUEL", etat1.getSource());
				assertFalse(etat1.isAnnule());

				final EtatDeclarationRetournee etat2 = (EtatDeclarationRetournee) etats.get(2);
				assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat2.getEtat());
				assertEquals(date(2018, 6, 23), etat2.getDateObtention());
				assertEquals(SOURCE, etat2.getSource());
				assertFalse(etat2.isAnnule());

				// le nouvel état retourné doit être le dernier
				assertSame(etat2, qsnc.getDernierEtatDeclaration());
				assertSame(etat2, qsnc.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE));
				return null;
			}
		});
	}
}