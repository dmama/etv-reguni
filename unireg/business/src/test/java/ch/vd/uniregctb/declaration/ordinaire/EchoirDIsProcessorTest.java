package ch.vd.uniregctb.declaration.ordinaire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.ordinaire.EchoirDIsResults.Echue;
import ch.vd.uniregctb.declaration.ordinaire.EchoirDIsResults.Erreur;
import ch.vd.uniregctb.declaration.ordinaire.EchoirDIsResults.ErreurType;
import ch.vd.uniregctb.declaration.ordinaire.EchoirDIsResults.Ignore;
import ch.vd.uniregctb.declaration.ordinaire.EchoirDIsResults.IgnoreType;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class EchoirDIsProcessorTest extends BusinessTest {

	private EchoirDIsProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		DelaisService delaisService = getBean(DelaisService.class, "delaisService");
		DeclarationImpotService diService = getBean(DeclarationImpotService.class, "diService");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new EchoirDIsProcessor(hibernateTemplate, delaisService, diService, transactionManager);
	}

	@Test
	public void testTraiterDINull() {
		try {
			processor.traiterDI(null, date(2000, 1, 1));
			fail();
		}
		catch (IllegalArgumentException e) {
			// ok
			assertEquals("L'id doit être spécifié.", e.getMessage());
		}
	}

	@Test
	public void testTraiterDIInexistante() {
		try {
			processor.traiterDI(12345L, date(2000, 1, 1));
			fail();
		}
		catch (IllegalArgumentException e) {
			// ok
			assertEquals("La déclaration n'existe pas.", e.getMessage());
		}
	}

	@Test
	public void testTraiterDISansEtat() throws Exception {

		// Crée une déclaration sans état
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique marco = addNonHabitant("Marco", "Polorose", date(1953, 3, 27), Sexe.MASCULIN);
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(marco, periode, date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				return declaration.getId();
			}
		});

		try {
			processor.traiterDI(id, date(2000, 1, 1));
			fail();
		}
		catch (IllegalArgumentException e) {
			// ok
			assertEquals("La déclaration ne possède pas d'état.", e.getMessage());
		}
	}

	@Test
	public void testTraiterDINonSommee() throws Exception {

		final RegDate dateTraitement = date(2009, 1, 1);

		// Crée une déclaration à l'état émise
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique marco = addNonHabitant("Marco", "Polorose", date(1953, 3, 27), Sexe.MASCULIN);
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(marco, periode, date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclaration(declaration, date(2008, 1, 15), TypeEtatDeclaration.EMISE);
				return declaration.getId();
			}
		});

		final EchoirDIsResults rapport = new EchoirDIsResults(dateTraitement);
		processor.setRapport(rapport);
		processor.traiterDI(id, dateTraitement);

		assertEquals(1, rapport.nbDIsTotal);
		assertEmpty(rapport.disEchues);
		assertEmpty(rapport.disIgnorees);
		assertEquals(1, rapport.disEnErrors.size());

		final Erreur erreur = rapport.disEnErrors.get(0);
		assertNotNull(erreur);
		assertEquals(id.longValue(), erreur.diId);
		assertEquals(ErreurType.ETAT_DECLARATION_INCOHERENT, erreur.raison);
	}

	@Test
	public void testTraiterDIDelaiNonDepasse() throws Exception {

		final RegDate dateTraitement = date(2008, 8, 1);
		final RegDate dateSommation = date(2008, 6, 30); // [UNIREG-1468] le délai s'applique à partir de la date de sommation

		// Crée une déclaration à l'état sommé mais avec un délai non dépassé
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique marco = addNonHabitant("Marco", "Polorose", date(1953, 3, 27), Sexe.MASCULIN);
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(marco, periode, date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclaration(declaration, date(2008, 1, 15), TypeEtatDeclaration.EMISE);
				addDelaiDeclaration(declaration, date(2008, 1, 15), date(2008, 3, 15));
				addEtatDeclaration(declaration, dateSommation, TypeEtatDeclaration.SOMMEE);
				return declaration.getId();
			}
		});

		// la date de traitement (1er août 2008) est avant le délai (dateSommation + 30 jours + 15 jours = 15 août 2008)
		final EchoirDIsResults rapport = new EchoirDIsResults(dateTraitement);
		processor.setRapport(rapport);
		processor.traiterDI(id, dateTraitement);

		assertEquals(1, rapport.nbDIsTotal);
		assertEmpty(rapport.disEchues);
		assertEquals(1, rapport.disIgnorees.size());
		assertEmpty(rapport.disEnErrors);

		final Ignore ignore = rapport.disIgnorees.get(0);
		assertNotNull(ignore);
		assertEquals(id.longValue(), ignore.diId);
		assertEquals(IgnoreType.DELAI_NON_ECHU, ignore.raison);
	}

	@Test
	public void testTraiterDIDelaiDepasse() throws Exception {

		final RegDate dateTraitement = date(2008, 9, 1);
		final RegDate dateSommation = date(2008, 6, 30);

		// Crée une déclaration à l'état sommé et avec un délai dépassé
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique marco = addNonHabitant("Marco", "Polorose", date(1953, 3, 27), Sexe.MASCULIN);
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(marco, periode, date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclaration(declaration, date(2008, 1, 15), TypeEtatDeclaration.EMISE);
				addDelaiDeclaration(declaration, date(2008, 1, 15), date(2008, 3, 15));
				addEtatDeclaration(declaration, dateSommation, TypeEtatDeclaration.SOMMEE);
				return declaration.getId();
			}
		});

		// la date de traitement (1er septembre 2008) est après le délai (dateSommation + 30 jours + 15 jours = 15 août 2008)
		final EchoirDIsResults rapport = new EchoirDIsResults(dateTraitement);
		processor.setRapport(rapport);
		processor.traiterDI(id, dateTraitement);

		assertEquals(1, rapport.nbDIsTotal);
		assertEquals(1, rapport.disEchues.size());
		assertEmpty(rapport.disIgnorees);
		assertEmpty(rapport.disEnErrors);

		final Echue echue = rapport.disEchues.get(0);
		assertNotNull(echue);
		assertEquals(id.longValue(), echue.diId);

		final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) hibernateTemplate.get(DeclarationImpotOrdinaire.class, id);
		assertNotNull(di);
		assertEquals(TypeEtatDeclaration.ECHUE, di.getDernierEtat().getEtat());
	}

	@Test
	public void testTraiterDIDejaEchue() throws Exception {

		final RegDate dateTraitement = date(2009, 1, 1);

		// Crée une déclaration à l'état échue
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique marco = addNonHabitant("Marco", "Polorose", date(1953, 3, 27), Sexe.MASCULIN);
				final PeriodeFiscale periode = addPeriodeFiscale(2007);
				final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				final DeclarationImpotOrdinaire declaration = addDeclarationImpot(marco, periode, date(2007, 1, 1), date(2007, 12, 31),
						TypeContribuable.VAUDOIS_ORDINAIRE, modele);
				addEtatDeclaration(declaration, date(2008, 1, 15), TypeEtatDeclaration.EMISE);
				addEtatDeclaration(declaration, date(2008, 6, 30), TypeEtatDeclaration.SOMMEE);
				addEtatDeclaration(declaration, date(2008, 10, 15), TypeEtatDeclaration.ECHUE);
				return declaration.getId();
			}
		});

		final EchoirDIsResults rapport = new EchoirDIsResults(dateTraitement);
		processor.setRapport(rapport);
		processor.traiterDI(id, dateTraitement);

		assertEquals(1, rapport.nbDIsTotal);
		assertEmpty(rapport.disEchues);
		assertEmpty(rapport.disIgnorees);
		assertEquals(1, rapport.disEnErrors.size());

		final Erreur erreur = rapport.disEnErrors.get(0);
		assertNotNull(erreur);
		assertEquals(id.longValue(), erreur.diId);
		assertEquals(ErreurType.ETAT_DECLARATION_INCOHERENT, erreur.raison);
	}
}
