package ch.vd.unireg.declaration.ordinaire.pm;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.IdentifiantDeclaration;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.declaration.ordinaire.pm.EchoirDIsPMResults.Echue;
import ch.vd.unireg.declaration.ordinaire.pm.EchoirDIsPMResults.Erreur;
import ch.vd.unireg.declaration.ordinaire.pm.EchoirDIsPMResults.ErreurType;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class EchoirDIsPMProcessorTest extends BusinessTest {

	private EchoirDIsPMProcessor processor;
	private AdresseService adresseService;

	private ParametreAppService parametreAppService;
	private Integer delaiAdministratifPM = null;
	private Integer oldPremierePeriodeFiscaleDeclarationPM;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final DelaisService delaisService = getBean(DelaisService.class, "delaisService");
		final DeclarationImpotService diService = getBean(DeclarationImpotService.class, "diService");
		adresseService = getBean(AdresseService.class, "adresseService");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new EchoirDIsPMProcessor(hibernateTemplate, delaisService, diService, transactionManager, tiersService, adresseService);

		// tant qu'on n'est pas en 2016, les tests devront utiliser cet artifice...
		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		oldPremierePeriodeFiscaleDeclarationPM = parametreAppService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
		delaiAdministratifPM = parametreAppService.getDelaiEnvoiSommationDeclarationImpotPM();
		parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(2014);
		parametreAppService.setDelaiEnvoiSommationDeclarationImpotPM(15);
	}

	@Override
	public void onTearDown() throws Exception {
		if (oldPremierePeriodeFiscaleDeclarationPM != null) {
			parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(oldPremierePeriodeFiscaleDeclarationPM);
			oldPremierePeriodeFiscaleDeclarationPM = null;
		}
		if (delaiAdministratifPM != null) {
			parametreAppService.setDelaiEnvoiSommationDeclarationImpotPM(delaiAdministratifPM);
			delaiAdministratifPM = null;
		}
		super.onTearDown();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiterDINull() {
		try {
			processor.traiterDI(null, new EchoirDIsPMResults(date(2000, 1, 1), tiersService, adresseService));
			fail();
		}
		catch (IllegalArgumentException e) {
			// ok
			assertEquals("L'id doit être spécifié.", e.getMessage());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiterDIInexistante() {
		try {
			processor.traiterDI(new IdentifiantDeclaration(12345L, 12L), new EchoirDIsPMResults(date(2000, 1, 1), tiersService, adresseService));
			fail();
		}
		catch (IllegalArgumentException e) {
			// ok
			assertEquals("La déclaration n'existe pas.", e.getMessage());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiterDISansEtat() throws Exception {

		final RegDate dateDebut = RegDate.get(2000, 1, 1);

		// Crée une déclaration sans état
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, dateDebut, null, "Truc machin SA");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(2014);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				return declaration.getId();
			}
		});

		try {
			final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, id);
			final IdentifiantDeclaration ident = new IdentifiantDeclaration(di.getId(),di.getTiers().getNumero(),0);
			processor.traiterDI(ident, new EchoirDIsPMResults(RegDate.get(), tiersService, adresseService));
			fail();
		}
		catch (IllegalArgumentException e) {
			// ok
			assertEquals("La déclaration ne possède pas d'état.", e.getMessage());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiterDINonSommee() throws Exception {

		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateTraitement = date(2015, 10, 1);

		// Crée une déclaration à l'état émise
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, dateDebut, null, "Truc machin SA");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(2014);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, date(2015, 1, 15));
				return declaration.getId();
			}
		});

		final EchoirDIsPMResults rapport = new EchoirDIsPMResults(dateTraitement, tiersService, adresseService);
		final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, id);
		final IdentifiantDeclaration ident = new IdentifiantDeclaration(di.getId(), di.getTiers().getNumero());
		processor.traiterDI(ident, rapport);

		assertEquals(1, rapport.nbDIsTotal);
		assertEmpty(rapport.disEchues);
		assertEquals(1, rapport.disEnErrors.size());

		final Erreur erreur = rapport.disEnErrors.get(0);
		assertNotNull(erreur);
		assertEquals(id.longValue(), erreur.diId);
		assertEquals(ErreurType.ETAT_DECLARATION_INCOHERENT, erreur.raison);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiterDISommeeMaisSuspendue() throws Exception {

		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateTraitement = date(2015, 10, 1);

		// Crée une déclaration à l'état émise
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, dateDebut, null, "Truc machin SA");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(2014);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, date(2015, 1, 15));
				addEtatDeclarationSommee(declaration, date(2015, 8, 15), date(2015, 8, 17), null);
				addEtatDeclarationSuspendue(declaration, date(2015, 8, 20));
				return declaration.getId();
			}
		});

		final EchoirDIsPMResults rapport = new EchoirDIsPMResults(dateTraitement, tiersService, adresseService);
		final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, id);
		final IdentifiantDeclaration ident = new IdentifiantDeclaration(di.getId(), di.getTiers().getNumero());
		processor.traiterDI(ident, rapport);

		assertEquals(1, rapport.nbDIsTotal);
		assertEmpty(rapport.disEchues);
		assertEmpty(rapport.disEnErrors);
		assertEquals(1, rapport.disIgnorees.size());

		final EchoirDIsPMResults.Ignoree ignoree = rapport.disIgnorees.get(0);
		assertNotNull(ignoree);
		assertEquals(id.longValue(), ignoree.diId);
		assertEquals(EchoirDIsPMResults.MotifIgnorance.DECLARATION_SUSPENDUE, ignoree.motif);
	}

	@Test
	public void testRunAvecDINonSommee() throws Exception {

		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateTraitement = date(2015, 11, 1);

		// Crée une déclaration à l'état émise
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, dateDebut, null, "Truc machin SA");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(2014);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, date(2015, 1, 15));
				return declaration.getId();
			}
		});

		final EchoirDIsPMResults rapport = processor.run(dateTraitement, null);

		assertEquals(0, rapport.nbDIsTotal);
		assertEmpty(rapport.disEchues);
		assertEmpty(rapport.disEnErrors);
		assertEmpty(rapport.disIgnorees);
	}

	@Test
	public void testRunAvecDISommeeMaisSuspendue() throws Exception {

		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateTraitement = date(2015, 11, 1);

		// Crée une déclaration à l'état émise
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, dateDebut, null, "Truc machin SA");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(2014);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, date(2015, 1, 15));
				addEtatDeclarationSommee(declaration, date(2015, 8, 15), date(2015, 8, 17), null);
				addEtatDeclarationSuspendue(declaration, date(2015, 8, 20));
				return declaration.getId();
			}
		});

		final EchoirDIsPMResults rapport = processor.run(dateTraitement, null);

		assertEquals(1, rapport.nbDIsTotal);
		assertEmpty(rapport.disEchues);
		assertEmpty(rapport.disEnErrors);
		assertEquals(1, rapport.disIgnorees.size());

		final EchoirDIsPMResults.Ignoree ignoree = rapport.disIgnorees.get(0);
		assertNotNull(ignoree);
		assertEquals(id.longValue(), ignoree.diId);
		assertEquals(EchoirDIsPMResults.MotifIgnorance.DECLARATION_SUSPENDUE, ignoree.motif);
	}

	@Test
	public void testDIDelaiNonDepasse() throws Exception {

		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateTraitement = date(2015, 8, 1);
		final RegDate dateSommation = date(2015, 6, 30); // [UNIREG-1468] le délai s'applique à partir de la date de sommation

		// Crée une déclaration à l'état sommé mais avec un délai non dépassé
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, dateDebut, null, "Truc machin SA");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(2014);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, date(2015, 1, 15));
				addDelaiDeclaration(declaration, date(2015, 1, 15), date(2015, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
				addEtatDeclarationSommee(declaration, dateSommation, dateSommation.addDays(3), null);
				return declaration.getId();
			}
		});

		// la date de traitement (1er août 2015) est avant le délai (dateSommation + 30 jours + 15 jours = 15 août 2015)
		final EchoirDIsPMResults rapport = processor.run(dateTraitement, null);

		assertEquals(0, rapport.nbDIsTotal);
		assertEmpty(rapport.disEchues);
		assertEmpty(rapport.disEnErrors);
	}

	@Test
	public void testDISursisNonDepasse() throws Exception {

		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateTraitement = date(2015, 9, 1);
		final RegDate dateSommation = date(2015, 6, 30); // [UNIREG-1468] le délai s'applique à partir de la date de sommation

		// Crée une déclaration à l'état sommé mais avec un délai non dépassé
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, dateDebut, null, "Truc machin SA");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(2014);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, date(2015, 1, 15));
				addDelaiDeclaration(declaration, date(2015, 1, 15), date(2015, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
				addEtatDeclarationSommee(declaration, dateSommation, dateSommation.addDays(3), null);

				// on met un sursis à la veille de la date de traitement, pour bien montrer que le délai
				// administratif de 15 jours est bien pris en compte (= le sursis est toujours actif même au lendemain de sa date officielle)
				final DelaiDeclaration sursis = addDelaiDeclaration(declaration, dateSommation.addDays(10), dateTraitement.getOneDayBefore(), EtatDelaiDocumentFiscal.ACCORDE);
				sursis.setSursis(true);

				return declaration.getId();
			}
		});

		// la date de traitement (1er août 2015) est avant le délai (dateSommation + 30 jours + 15 jours = 15 août 2015)
		final EchoirDIsPMResults rapport = processor.run(dateTraitement, null);

		assertEquals(1, rapport.nbDIsTotal);
		assertEmpty(rapport.disEchues);
		assertEmpty(rapport.disEnErrors);
		assertEquals(1, rapport.disIgnorees.size());

		final EchoirDIsPMResults.Ignoree diIgnoree = rapport.disIgnorees.get(0);
		assertNotNull(diIgnoree);
		assertEquals(EchoirDIsPMResults.MotifIgnorance.SURSIS_ACCORDE, diIgnoree.motif);
	}

	@Test
	public void testDIDelaiDepasse() throws Exception {

		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateTraitement = date(2015, 9, 1);
		final RegDate dateSommation = date(2015, 6, 30);

		// Crée une déclaration à l'état sommé et avec un délai dépassé
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, dateDebut, null, "Truc machin SA");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(2014);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, date(2015, 1, 15));
				addDelaiDeclaration(declaration, date(2015, 1, 15), date(2015, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
				addEtatDeclarationSommee(declaration, dateSommation, dateSommation.addDays(3), null);
				return declaration.getId();
			}
		});

		// la date de traitement (1er septembre 2015) est après le délai (dateSommation + 30 jours + 15 jours = 15 août 2015)
		final EchoirDIsPMResults rapport = processor.run(dateTraitement, null);

		assertEquals(1, rapport.nbDIsTotal);
		assertEquals(1, rapport.disEchues.size());
		assertEmpty(rapport.disEnErrors);

		final Echue echue = rapport.disEchues.get(0);
		assertNotNull(echue);
		assertEquals(id.longValue(), echue.diId);

		doInNewTransactionAndSession(status -> {
			final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, id);
			assertNotNull(di);
			assertEquals(TypeEtatDocumentFiscal.ECHU, di.getDernierEtatDeclaration().getEtat());
			return null;
		});
	}

	@Test
	public void testDISursisDepasse() throws Exception {

		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateTraitement = date(2015, 9, 1);
		final RegDate dateSommation = date(2015, 6, 30); // [UNIREG-1468] le délai s'applique à partir de la date de sommation

		// Crée une déclaration à l'état sommé mais avec un délai non dépassé
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, dateDebut, null, "Truc machin SA");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(2014);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, date(2015, 1, 15));
				addDelaiDeclaration(declaration, date(2015, 1, 15), date(2015, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
				addEtatDeclarationSommee(declaration, dateSommation, dateSommation.addDays(3), null);

				// on est obligé de revenir en arrière un gros bout pour coutourner le délai administratif de 15 jours
				final DelaiDeclaration sursis = addDelaiDeclaration(declaration, dateSommation.addDays(10), dateTraitement.addDays(-20), EtatDelaiDocumentFiscal.ACCORDE);
				sursis.setSursis(true);

				return declaration.getId();
			}
		});

		// la date de traitement (1er août 2015) est avant le délai (dateSommation + 30 jours + 15 jours = 15 août 2015)
		final EchoirDIsPMResults rapport = processor.run(dateTraitement, null);

		assertEquals(1, rapport.nbDIsTotal);
		assertEquals(1, rapport.disEchues.size());
		assertEmpty(rapport.disEnErrors);
		assertEmpty(rapport.disIgnorees);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiterDIDejaEchue() throws Exception {

		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegDate dateTraitement = date(2015, 11, 1);

		// Crée une déclaration à l'état échue
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final Entreprise e = addEntrepriseInconnueAuCivil();
				addRaisonSociale(e, dateDebut, null, "Truc machin SA");
				addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
				addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

				final PeriodeFiscale periode = addPeriodeFiscale(2014);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(e, periode, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, date(2015, 1, 15));
				addEtatDeclarationSommee(declaration, date(2015, 6, 30), date(2015, 6, 30), null);
				addEtatDeclarationEchue(declaration, date(2015, 10, 15));
				return declaration.getId();
			}
		});

		final EchoirDIsPMResults rapport = new EchoirDIsPMResults(dateTraitement, tiersService, adresseService);
		DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, id);
		IdentifiantDeclaration ident = new IdentifiantDeclaration(di.getId(),di.getTiers().getNumero(),0);
		processor.traiterDI(ident, rapport);

		assertEquals(1, rapport.nbDIsTotal);
		assertEmpty(rapport.disEchues);
		assertEquals(1, rapport.disEnErrors.size());

		final Erreur erreur = rapport.disEnErrors.get(0);
		assertNotNull(erreur);
		assertEquals(id.longValue(), erreur.diId);
		assertEquals(ErreurType.ETAT_DECLARATION_INCOHERENT, erreur.raison);
	}
}
