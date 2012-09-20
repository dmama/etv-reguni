package ch.vd.uniregctb.declaration.ordinaire;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.IdentifiantDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.ordinaire.EchoirDIsResults.Echue;
import ch.vd.uniregctb.declaration.ordinaire.EchoirDIsResults.Erreur;
import ch.vd.uniregctb.declaration.ordinaire.EchoirDIsResults.ErreurType;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class EchoirDIsProcessorTest extends BusinessTest {

	private EchoirDIsProcessor processor;

	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/declaration/ordinaire/echoirDiTiersInvalide.xml";

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final DelaisService delaisService = getBean(DelaisService.class, "delaisService");
		final DeclarationImpotService diService = getBean(DeclarationImpotService.class, "diService");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new EchoirDIsProcessor(hibernateTemplate, delaisService, diService, transactionManager);

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				addCollAdm(MockCollectiviteAdministrative.CEDI);
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiterDINull() {
		try {
			processor.traiterDI(null, new EchoirDIsResults(date(2000, 1, 1)));
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
			processor.traiterDI(new IdentifiantDeclaration(12345L,12L,0), new EchoirDIsResults(date(2000, 1, 1)));
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

		// Crée une déclaration sans état
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique marco = addNonHabitant("Marco", "Polorose", date(1953, 3, 27), Sexe.MASCULIN);
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(marco, periode, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				return declaration.getId();
			}
		});

		try {
			DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, id);
			IdentifiantDeclaration ident = new IdentifiantDeclaration(di.getId(),di.getTiers().getNumero(),0);
			processor.traiterDI(ident, new EchoirDIsResults(date(2000, 1, 1)));
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

		final RegDate dateTraitement = date(2009, 1, 1);

		// Crée une déclaration à l'état émise
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique marco = addNonHabitant("Marco", "Polorose", date(1953, 3, 27), Sexe.MASCULIN);
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(marco, periode, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, date(2008, 1, 15));
				return declaration.getId();
			}
		});

		final EchoirDIsResults rapport = new EchoirDIsResults(dateTraitement);
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

	@Test
	public void testRunAvecDINonSommee() throws Exception {

		final RegDate dateTraitement = date(2009, 1, 1);

		// Crée une déclaration à l'état émise
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique marco = addNonHabitant("Marco", "Polorose", date(1953, 3, 27), Sexe.MASCULIN);
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(marco, periode, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, date(2008, 1, 15));
				return declaration.getId();
			}
		});

		final EchoirDIsResults rapport = processor.run(dateTraitement, null);

		assertEquals(0, rapport.nbDIsTotal);
		assertEmpty(rapport.disEchues);
		assertEmpty(rapport.disEnErrors);
	}

	@Test
	public void testDIDelaiNonDepasse() throws Exception {

		final RegDate dateTraitement = date(2008, 8, 1);
		final RegDate dateSommation = date(2008, 6, 30); // [UNIREG-1468] le délai s'applique à partir de la date de sommation

		// Crée une déclaration à l'état sommé mais avec un délai non dépassé
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique marco = addNonHabitant("Marco", "Polorose", date(1953, 3, 27), Sexe.MASCULIN);
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(marco, periode, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, date(2008, 1, 15));
				addDelaiDeclaration(declaration, date(2008, 1, 15), date(2008, 3, 15));
				addEtatDeclarationSommee(declaration, dateSommation,dateSommation.addDays(3));
				return declaration.getId();
			}
		});

		// la date de traitement (1er août 2008) est avant le délai (dateSommation + 30 jours + 15 jours = 15 août 2008)
		final EchoirDIsResults rapport = processor.run(dateTraitement, null);

		assertEquals(0, rapport.nbDIsTotal);
		assertEmpty(rapport.disEchues);
		assertEmpty(rapport.disEnErrors);
	}

	@Test
	public void testDIDelaiDepasse() throws Exception {

		final RegDate dateTraitement = date(2008, 9, 1);
		final RegDate dateSommation = date(2008, 6, 30);

		// Crée une déclaration à l'état sommé et avec un délai dépassé
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique marco = addNonHabitant("Marco", "Polorose", date(1953, 3, 27), Sexe.MASCULIN);
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(marco, periode, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, date(2008, 1, 15));
				addDelaiDeclaration(declaration, date(2008, 1, 15), date(2008, 3, 15));
				addEtatDeclarationSommee(declaration, dateSommation,dateSommation.addDays(3));
				return declaration.getId();
			}
		});

		// la date de traitement (1er septembre 2008) est après le délai (dateSommation + 30 jours + 15 jours = 15 août 2008)
		final EchoirDIsResults rapport = processor.run(dateTraitement, null);

		assertEquals(1, rapport.nbDIsTotal);
		assertEquals(1, rapport.disEchues.size());
		assertEmpty(rapport.disEnErrors);

		final Echue echue = rapport.disEchues.get(0);
		assertNotNull(echue);
		assertEquals(id.longValue(), echue.diId);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) hibernateTemplate.get(DeclarationImpotOrdinaire.class, id);
				assertNotNull(di);
				assertEquals(TypeEtatDeclaration.ECHUE, di.getDernierEtat().getEtat());
				return null;
			}
		});
	}

	@Test
	public void testDITiersInvalide() throws Exception {

		final RegDate dateTraitement = date(2008, 9, 1);
		final RegDate dateSommation = date(2008, 6, 30);
		 final long numeroMenageCommun = 12600004L;
		loadDatabase(DB_UNIT_DATA_FILE);

		// la date de traitement (1er septembre 2008) est après le délai (dateSommation + 30 jours + 15 jours = 15 août 2008)
		final EchoirDIsResults rapport = processor.run(dateTraitement, null);

		assertEquals(1, rapport.nbDIsTotal);
		assertEmpty(rapport.disEchues);
		assertEquals(1, rapport.disEnErrors.size());

		final Erreur erreur = rapport.disEnErrors.get(0);
		assertNotNull(erreur);
		assertEquals(ErreurType.EXCEPTION, erreur.raison);



	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiterDIDejaEchue() throws Exception {

		final RegDate dateTraitement = date(2009, 1, 1);

		// Crée une déclaration à l'état échue
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique marco = addNonHabitant("Marco", "Polorose", date(1953, 3, 27), Sexe.MASCULIN);
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(marco, periode, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclarationEmise(declaration, date(2008, 1, 15));
				addEtatDeclarationSommee(declaration, date(2008, 6, 30), date(2008, 6, 30));
				addEtatDeclarationEchue(declaration, date(2008, 10, 15));
				return declaration.getId();
			}
		});

		final EchoirDIsResults rapport = new EchoirDIsResults(dateTraitement);
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
