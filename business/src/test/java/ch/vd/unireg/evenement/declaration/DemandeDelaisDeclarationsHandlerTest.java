package ch.vd.unireg.evenement.declaration;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscal;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.mandataire.DemandeDelaisMandataire;
import ch.vd.unireg.mandataire.DemandeDelaisMandataireDAO;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.Delai;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.DemandeDelai;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.DemandeGroupee;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.DemandeUnitaire;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.DonneesMetier;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.Mandataire;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.Population;
import ch.vd.unireg.xml.event.di.cyber.demandedelai.v1.Supervision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class DemandeDelaisDeclarationsHandlerTest extends BusinessTest {

	private DemandeDelaisDeclarationsHandler handler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		handler = new DemandeDelaisDeclarationsHandler();
		handler.setTiersDAO(tiersDAO);
		handler.setHibernateTemplate(hibernateTemplate);
		handler.setDeclarationImpotService(getBean(DeclarationImpotService.class, "diService"));
		handler.setDemandeDelaisMandataireDAO(getBean(DemandeDelaisMandataireDAO.class, "demandeDelaisMandataireDAO"));
	}

	/**
	 * Teste le cas passant d'ajout d'un délai accepté sur une déclaration.
	 */
	@Test
	public void testHandleDemandeUnitaireDelaiAccepte() throws Exception {

		// mise en place
		final Long ctbId = doInNewTransaction(status -> {
			final PeriodeFiscale periode2016 = addPeriodeFiscale(2016);
			final ModeleDocument modele2016 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2016);

			final PersonnePhysique pp = addNonHabitant("Rodolf", "Frigo", date(1970, 1, 1), Sexe.MASCULIN);
			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
			addEtatDeclarationEmise(di, date(2017, 1, 15));
			addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			return pp.getNumero();
		});

		final RegDate nouveauDelai = RegDate.get().addMonths(1);
		final RegDate dateObtention = RegDate.get();

		// ajout du délai
		doInNewTransaction(status -> {
			final DemandeDelai demandeDelai = newDemandeUnitaire(ctbId, nouveauDelai, dateObtention, null, 2016, "businessId", "referenceId");

			try {
				handler.handle(demandeDelai);
			}
			catch (EsbBusinessException e) {
				throw new RuntimeException(e);
			}
			return null;
		});

		// vérification que le délai accepté a bien été ajouté
		doInNewTransaction(status -> {
			final Tiers tiers = tiersDAO.get(ctbId);
			assertNotNull(tiers);

			final List<DeclarationImpotOrdinairePP> declarations = tiers.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, 2016, false);
			assertEquals(1, declarations.size());

			final DeclarationImpotOrdinairePP declaration2016 = declarations.get(0);
			assertNotNull(declaration2016);

			final List<DelaiDocumentFiscal> delais = declaration2016.getDelaisSorted();
			assertEquals(2, delais.size());
			assertDelai(date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE, delais.get(0));
			assertDelai(dateObtention, nouveauDelai, EtatDelaiDocumentFiscal.ACCORDE, delais.get(1));
			return null;
		});
	}

	/**
	 * Teste le cas passant d'ajout d'un délai refusé sur une déclaration.
	 */
	@Test
	public void testHandleDemandeUnitaireDelaiRefuse() throws Exception {

		// mise en place
		final Long ctbId = doInNewTransaction(status -> {
			final PeriodeFiscale periode2016 = addPeriodeFiscale(2016);
			final ModeleDocument modele2016 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2016);

			final PersonnePhysique pp = addNonHabitant("Rodolf", "Frigo", date(1970, 1, 1), Sexe.MASCULIN);
			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
			addEtatDeclarationEmise(di, date(2017, 1, 15));
			addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			return pp.getNumero();
		});

		final RegDate nouveauDelai = RegDate.get().addMonths(1);
		final RegDate dateObtention = RegDate.get();

		// ajout du délai
		doInNewTransaction(status -> {
			final DemandeDelai demandeDelai = newDemandeUnitaire(ctbId, nouveauDelai, dateObtention, 1, 2016, "businessId", "referenceId");

			try {
				handler.handle(demandeDelai);
			}
			catch (EsbBusinessException e) {
				throw new RuntimeException(e);
			}
			return null;
		});

		// vérification que le délai refusé a bien été ajouté
		doInNewTransaction(status -> {
			final Tiers tiers = tiersDAO.get(ctbId);
			assertNotNull(tiers);

			final List<DeclarationImpotOrdinairePP> declarations = tiers.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, 2016, false);
			assertEquals(1, declarations.size());

			final DeclarationImpotOrdinairePP declaration2016 = declarations.get(0);
			assertNotNull(declaration2016);

			final List<DelaiDocumentFiscal> delais = declaration2016.getDelaisSorted();
			assertEquals(2, delais.size());
			assertDelai(date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE, delais.get(0));
			assertDelai(dateObtention, null, EtatDelaiDocumentFiscal.REFUSE, delais.get(1));
			return null;
		});
	}

	/**
	 * Teste un des cas d'erreur de l'appel de la méthode declarationImpotService.ajouterDelaiDI()
	 */
	@Test
	public void testHandleDemandeUnitaireDeclarationDejaRetournee() throws Exception {

		// mise en place
		final Long ctbId = doInNewTransaction(status -> {
			final PeriodeFiscale periode2016 = addPeriodeFiscale(2016);
			final ModeleDocument modele2016 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2016);

			final PersonnePhysique pp = addNonHabitant("Rodolf", "Frigo", date(1970, 1, 1), Sexe.MASCULIN);
			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
			addEtatDeclarationEmise(di, date(2017, 1, 15));
			addEtatDeclarationRetournee(di, date(2017, 5, 9));
			addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			return pp.getNumero();
		});

		final RegDate nouveauDelai = RegDate.get().addMonths(1);
		final RegDate dateObtention = RegDate.get();

		// ajout du délai
		doInNewTransaction(status -> {
			final DemandeDelai demandeDelai = newDemandeUnitaire(ctbId, nouveauDelai, dateObtention, null, 2016, "businessId", "referenceId");

			try {
				handler.handle(demandeDelai);
				fail();
			}
			catch (EsbBusinessException e) {
				assertEquals("La déclaration n'est pas dans l'état 'EMIS'.", e.getMessage());
				assertEquals(EsbBusinessCode.MAUVAIS_ETAT_DECLARATION, e.getCode());
			}
			return null;
		});
	}

	/**
	 * Teste le cas passant d'ajout de plusieurs délais accepté sur des déclarations
	 */
	@Test
	public void testHandleDemandeGroupee() throws Exception {

		class Ids {
			Long pp1;
			Long pp2;
			Long pp3;

			public Ids(Long pp1, Long pp2, Long pp3) {
				this.pp1 = pp1;
				this.pp2 = pp2;
				this.pp3 = pp3;
			}
		}

		// mise en place, 3 pp avec chacun une déclaration
		final Ids ids = doInNewTransaction(status -> {
			final PeriodeFiscale periode2016 = addPeriodeFiscale(2016);
			final ModeleDocument modele2016 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2016);

			final PersonnePhysique pp1 = addNonHabitant("Rodolf", "Frigo", date(1970, 1, 1), Sexe.MASCULIN);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp1, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}

			final PersonnePhysique pp2 = addNonHabitant("Fleurette", "Bijoux", date(1970, 1, 1), Sexe.FEMININ);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp2, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}

			final PersonnePhysique pp3 = addNonHabitant("Melania", "Trompette", date(1970, 1, 1), Sexe.FEMININ);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp3, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}
			return new Ids(pp1.getNumero(), pp2.getNumero(), pp3.getNumero());
		});

		final RegDate dateObtention = RegDate.get();

		// traitement de la demande
		doInNewTransaction(status -> {

			final DemandeDelai demandeDelai = newDemandeGroupee(2016, dateObtention, "CHE1", "Fiduciaire pas futée", 1234567, "businessId", "referenceId",
			                                                    new DelaiData(ids.pp1, RegDate.get().addMonths(1), null),
			                                                    new DelaiData(ids.pp2, RegDate.get().addMonths(2), null),
			                                                    new DelaiData(ids.pp3, RegDate.get().addMonths(3), null));
			try {
				handler.handle(demandeDelai);
			}
			catch (EsbBusinessException e) {
				throw new RuntimeException(e);
			}
			return null;
		});

		// vérification que les délais ont bien été ajoutés
		doInNewTransaction(status -> {

			final Tiers tiers1 = tiersDAO.get(ids.pp1);
			assertNotNull(tiers1);
			{
				final List<DeclarationImpotOrdinairePP> declarations = tiers1.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, 2016, false);
				assertEquals(1, declarations.size());

				final DeclarationImpotOrdinairePP declaration2016 = declarations.get(0);
				assertNotNull(declaration2016);

				final List<DelaiDocumentFiscal> delais = declaration2016.getDelaisSorted();
				assertEquals(2, delais.size());
				assertDelai(date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE, delais.get(0));
				assertDelai(dateObtention, RegDate.get().addMonths(1), EtatDelaiDocumentFiscal.ACCORDE, delais.get(1));
				assertDemandeMandataire(1234567L, "CHE1", "Fiduciaire pas futée", "businessId", "referenceId", delais.get(1).getDemandeMandataire());
			}

			final Tiers tiers2 = tiersDAO.get(ids.pp2);
			assertNotNull(tiers2);
			{
				final List<DeclarationImpotOrdinairePP> declarations = tiers2.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, 2016, false);
				assertEquals(1, declarations.size());

				final DeclarationImpotOrdinairePP declaration2016 = declarations.get(0);
				assertNotNull(declaration2016);

				final List<DelaiDocumentFiscal> delais = declaration2016.getDelaisSorted();
				assertEquals(2, delais.size());
				assertDelai(date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE, delais.get(0));
				assertDelai(dateObtention, RegDate.get().addMonths(2), EtatDelaiDocumentFiscal.ACCORDE, delais.get(1));
				assertDemandeMandataire(1234567L, "CHE1", "Fiduciaire pas futée", "businessId", "referenceId", delais.get(1).getDemandeMandataire());
			}

			final Tiers tiers3 = tiersDAO.get(ids.pp3);
			assertNotNull(tiers3);
			{
				final List<DeclarationImpotOrdinairePP> declarations = tiers3.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, 2016, false);
				assertEquals(1, declarations.size());

				final DeclarationImpotOrdinairePP declaration2016 = declarations.get(0);
				assertNotNull(declaration2016);

				final List<DelaiDocumentFiscal> delais = declaration2016.getDelaisSorted();
				assertEquals(2, delais.size());
				assertDelai(date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE, delais.get(0));
				assertDelai(dateObtention, RegDate.get().addMonths(3), EtatDelaiDocumentFiscal.ACCORDE, delais.get(1));
				assertDemandeMandataire(1234567L, "CHE1", "Fiduciaire pas futée", "businessId", "referenceId", delais.get(1).getDemandeMandataire());
			}
			return null;
		});
	}

	/**
	 * Teste le traitement d'une demande groupée avec un des statuts de délai pas OK.
	 */
	@Test
	public void testHandleDemandeGroupeeStatutNOK() throws Exception {

		class Ids {
			Long pp1;
			Long pp2;
			Long pp3;

			public Ids(Long pp1, Long pp2, Long pp3) {
				this.pp1 = pp1;
				this.pp2 = pp2;
				this.pp3 = pp3;
			}
		}

		// mise en place, 3 pp avec chacun une déclaration
		final Ids ids = doInNewTransaction(status -> {
			final PeriodeFiscale periode2016 = addPeriodeFiscale(2016);
			final ModeleDocument modele2016 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2016);

			final PersonnePhysique pp1 = addNonHabitant("Rodolf", "Frigo", date(1970, 1, 1), Sexe.MASCULIN);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp1, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}

			final PersonnePhysique pp2 = addNonHabitant("Fleurette", "Bijoux", date(1970, 1, 1), Sexe.FEMININ);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp2, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}

			final PersonnePhysique pp3 = addNonHabitant("Melania", "Trompette", date(1970, 1, 1), Sexe.FEMININ);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp3, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}
			return new Ids(pp1.getNumero(), pp2.getNumero(), pp3.getNumero());
		});

		final RegDate dateObtention = RegDate.get();

		// traitement de la demande
		doInNewTransaction(status -> {

			final DemandeDelai demandeDelai = newDemandeGroupee(2016, dateObtention, "CHE1", "Fiduciaire pas futée", 1234567, "businessId", "referenceId",
			                                                    new DelaiData(ids.pp1, RegDate.get().addMonths(1), null),
			                                                    new DelaiData(ids.pp2, RegDate.get().addMonths(2), 12),
			                                                    new DelaiData(ids.pp3, RegDate.get().addMonths(3), null));
			try {
				handler.handle(demandeDelai);
				fail();
			}
			catch (EsbBusinessException e) {
				assertEquals("Le statut du délai n'est pas valide (12/null) sur le contribuable n°" + ids.pp2, e.getMessage());
				assertEquals(EsbBusinessCode.DELAI_INVALIDE, e.getCode());
			}
			return null;
		});
	}

	/**
	 * Teste un des cas d'erreur de l'appel de la méthode declarationImpotService.ajouterDelaiDI()
	 */
	@Test
	public void testHandleDemandeGroupeeDeclarationDejaRetourne() throws Exception {

		class Ids {
			Long pp1;
			Long pp2;
			Long pp3;

			public Ids(Long pp1, Long pp2, Long pp3) {
				this.pp1 = pp1;
				this.pp2 = pp2;
				this.pp3 = pp3;
			}
		}

		// mise en place, 3 pp avec chacun une déclaration
		final Ids ids = doInNewTransaction(status -> {
			final PeriodeFiscale periode2016 = addPeriodeFiscale(2016);
			final ModeleDocument modele2016 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2016);

			final PersonnePhysique pp1 = addNonHabitant("Rodolf", "Frigo", date(1970, 1, 1), Sexe.MASCULIN);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp1, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addEtatDeclarationRetournee(di, date(2017, 5, 9));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}

			final PersonnePhysique pp2 = addNonHabitant("Fleurette", "Bijoux", date(1970, 1, 1), Sexe.FEMININ);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp2, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}

			final PersonnePhysique pp3 = addNonHabitant("Melania", "Trompette", date(1970, 1, 1), Sexe.FEMININ);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp3, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}
			return new Ids(pp1.getNumero(), pp2.getNumero(), pp3.getNumero());
		});

		final RegDate dateObtention = RegDate.get();

		// traitement de la demande
		doInNewTransaction(status -> {

			final DemandeDelai demandeDelai = newDemandeGroupee(2016, dateObtention, "CHE1", "Fiduciaire pas futée", 1234567, "businessId", "referenceId",
			                                                    new DelaiData(ids.pp1, RegDate.get().addMonths(1), null),
			                                                    new DelaiData(ids.pp2, RegDate.get().addMonths(2), null),
			                                                    new DelaiData(ids.pp3, RegDate.get().addMonths(3), null));
			try {
				handler.handle(demandeDelai);
				fail();
			}
			catch (EsbBusinessException e) {
				assertEquals("Contribuable n°" + ids.pp1 + " : La déclaration n'est pas dans l'état 'EMIS'.", e.getMessage());
				assertEquals(EsbBusinessCode.MAUVAIS_ETAT_DECLARATION, e.getCode());
			}
			return null;
		});
	}

	@Test
	public void testFindDeclaration() throws Exception {

		class Ids {
			Long pp1;
			Long pp2;
			Long pp3;
			Long pp4;
			Long pp5;
			Long dpi;
		}

		// mise en place, 3 pp avec chacun une déclaration
		final Ids ids = doInNewTransaction(status -> {
			final PeriodeFiscale periode2016 = addPeriodeFiscale(2016);
			final ModeleDocument modele2016 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2016);

			// une déclaration retournée
			final PersonnePhysique pp1 = addNonHabitant("Rodolf", "Frigo", date(1970, 1, 1), Sexe.MASCULIN);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp1, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addEtatDeclarationRetournee(di, date(2017, 5, 9));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}

			// une déclaration émise
			final PersonnePhysique pp2 = addNonHabitant("Fleurette", "Bijoux", date(1970, 1, 1), Sexe.FEMININ);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp2, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}

			// pas de déclaration
			final PersonnePhysique pp3 = addNonHabitant("Melania", "Trompette", date(1970, 1, 1), Sexe.FEMININ);

			// une déclaration annulée
			final PersonnePhysique pp4 = addNonHabitant("Lucielle", "Carosse", date(1970, 1, 1), Sexe.FEMININ);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp4, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
				di.setAnnule(true);
			}

			// deux déclarations émises
			final PersonnePhysique pp5 = addNonHabitant("Fleurette", "Bijoux", date(1970, 1, 1), Sexe.FEMININ);
			{
				final DeclarationImpotOrdinairePP di1 = addDeclarationImpot(pp5, periode2016, date(2016, 1, 1), date(2016, 3, 27), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di1, date(2016, 4, 1));
				addDelaiDeclaration(di1, date(2016, 4, 1), date(2016, 5, 31), EtatDelaiDocumentFiscal.ACCORDE);

				final DeclarationImpotOrdinairePP di2 = addDeclarationImpot(pp5, periode2016, date(2016, 10, 21), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di2, date(2017, 1, 15));
				addDelaiDeclaration(di2, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}

			final DebiteurPrestationImposable dpi = addDebiteur();

			final Ids i = new Ids();
			i.pp1 = pp1.getNumero();
			i.pp2 = pp2.getNumero();
			i.pp3 = pp3.getNumero();
			i.pp4 = pp4.getNumero();
			i.pp5 = pp5.getNumero();
			i.dpi = dpi.getNumero();
			return i;
		});

		doInNewTransaction(status -> {

			// contribuable inexistant
			try {
				handler.findDeclaration(1010101L, 2016, null);
				fail();
			}
			catch (EsbBusinessException e) {
				assertEquals("Le tiers n°1010101 n'existe pas.", e.getMessage());
				assertEquals(EsbBusinessCode.CTB_INEXISTANT, e.getCode());
			}

			// pas un contribuable
			try {
				handler.findDeclaration(ids.dpi, 2016, null);
				fail();
			}
			catch (EsbBusinessException e) {
				assertEquals("Le tiers n°" + ids.dpi + " n'est pas un contribuable.", e.getMessage());
				assertEquals(EsbBusinessCode.CTB_INEXISTANT, e.getCode());
			}

			// pas du tout de déclaration
			try {
				handler.findDeclaration(ids.pp3, 2016, null);
				fail();
			}
			catch (EsbBusinessException e) {
				assertEquals("Le contribuable n°" + ids.pp3 + " ne possède pas de déclaration d'impôt valide en 2016", e.getMessage());
				assertEquals(EsbBusinessCode.DECLARATION_ABSENTE, e.getCode());
			}

			// pas de déclaration dans la période
			try {
				handler.findDeclaration(ids.pp2, 2007, null);
				fail();
			}
			catch (EsbBusinessException e) {
				assertEquals("Le contribuable n°" + ids.pp2 + " ne possède pas de déclaration d'impôt valide en 2007", e.getMessage());
				assertEquals(EsbBusinessCode.DECLARATION_ABSENTE, e.getCode());
			}

			// pas de déclaration valide
			try {
				handler.findDeclaration(ids.pp4, 2016, null);
				fail();
			}
			catch (EsbBusinessException e) {
				assertEquals("Le contribuable n°" + ids.pp4 + " ne possède pas de déclaration d'impôt valide en 2016", e.getMessage());
				assertEquals(EsbBusinessCode.DECLARATION_ABSENTE, e.getCode());
			}

			// une seul déclaration émise (sans numéro de séquence)
			try {
				final DeclarationImpotOrdinaire declaration = handler.findDeclaration(ids.pp2, 2016, null);
				assertNotNull(declaration);
				assertEquals(date(2016, 1, 1), declaration.getDateDebut());
				assertEquals(date(2016, 12, 31), declaration.getDateFin());
			}
			catch (EsbBusinessException e) {
				throw new RuntimeException(e);
			}

			// une seul déclaration émise (avec numéro de séquence)
			try {
				final DeclarationImpotOrdinaire declaration = handler.findDeclaration(ids.pp2, 2016, 1);
				assertNotNull(declaration);
				assertEquals(date(2016, 1, 1), declaration.getDateDebut());
				assertEquals(date(2016, 12, 31), declaration.getDateFin());
			}
			catch (EsbBusinessException e) {
				throw new RuntimeException(e);
			}

			// deux déclaration émises (sans numéro de séquence)
			try {
				handler.findDeclaration(ids.pp5, 2016, null);
				fail();
			}
			catch (EsbBusinessException e) {
				assertEquals("Le contribuable n°" + ids.pp5 + " possède plusieurs déclarations d'impôt valides en 2016", e.getMessage());
				assertEquals(EsbBusinessCode.PLUSIEURS_DECLARATIONS, e.getCode());
			}

			// deux déclaration émises (avec numéro de séquence)
			try {
				final DeclarationImpotOrdinaire declaration = handler.findDeclaration(ids.pp5, 2016, 1);
				assertNotNull(declaration);
				assertEquals(date(2016, 1, 1), declaration.getDateDebut());
				assertEquals(date(2016, 3, 27), declaration.getDateFin());
			}
			catch (EsbBusinessException e) {
				throw new RuntimeException(e);
			}

			return null;
		});

	}

	static class DelaiData {
		int ctbId;
		RegDate delai;
		Integer codeRefus;

		public DelaiData(Long ctbId, RegDate delai, Integer codeRefus) {
			this.ctbId = ctbId.intValue();
			this.delai = delai;
			this.codeRefus = codeRefus;
		}
	}

	private static DemandeDelai newDemandeGroupee(int periodeFiscale, RegDate dateObtention, String numeroIde, String raisonSociale, Integer mandataireId, String businessId, String referenceId, DelaiData... delais) {

		final XMLGregorianCalendar calObtention = XmlUtils.regdate2xmlcal(dateObtention);

		final DemandeGroupee demandeGroupee = new DemandeGroupee();
		demandeGroupee.setMandataire(new Mandataire(numeroIde, raisonSociale, mandataireId));
		Arrays.stream(delais)
				.forEach(d -> {
					final Population population = d.ctbId > Entreprise.LAST_ID ? Population.PP : Population.PM;
					demandeGroupee.getDelais().add(new Delai(d.ctbId, population, null, XmlUtils.regdate2xmlcal(d.delai), d.codeRefus, null));
				});

		final DonneesMetier donneesMetier = new DonneesMetier();
		donneesMetier.setPeriodeFiscale(periodeFiscale);
		donneesMetier.setDemandeGroupee(demandeGroupee);

		final DemandeDelai demandeDelai1 = new DemandeDelai();
		demandeDelai1.setSupervision(new Supervision(calObtention, calObtention, businessId, referenceId));
		demandeDelai1.setDonneesMetier(donneesMetier);
		return demandeDelai1;
	}

	@NotNull
	private static DemandeDelai newDemandeUnitaire(Long ctbId, RegDate nouveauDelai, RegDate dateObtention, Integer codeRefus, int periodeFiscale, String businessId, String referenceId) {

		final XMLGregorianCalendar calObtention = XmlUtils.regdate2xmlcal(dateObtention);
		final XMLGregorianCalendar calDelai = XmlUtils.regdate2xmlcal(nouveauDelai);
		final Population population = ctbId > Entreprise.LAST_ID ? Population.PP : Population.PM;

		final DemandeUnitaire demandeUnitaire = new DemandeUnitaire();
		demandeUnitaire.setDelai(new Delai(ctbId.intValue(), population, null, calDelai, codeRefus, null));

		final DonneesMetier donneesMetier = new DonneesMetier();
		donneesMetier.setPeriodeFiscale(periodeFiscale);
		donneesMetier.setDemandeUnitaire(demandeUnitaire);

		final DemandeDelai demandeDelai = new DemandeDelai();
		demandeDelai.setSupervision(new Supervision(calObtention, calObtention, businessId, referenceId));
		demandeDelai.setDonneesMetier(donneesMetier);
		return demandeDelai;
	}

	private static void assertDemandeMandataire(Long mandataireId, String ide, String raisonSociale, String businessId, String referenceId, DemandeDelaisMandataire demande) {
		assertNotNull(demande);
		assertEquals(businessId, demande.getBusinessId());
		assertEquals(referenceId, demande.getReferenceId());
		assertEquals(mandataireId, demande.getNumeroCtbMandataire());
		assertEquals(ide, demande.getNumeroIDE());
		assertEquals(raisonSociale, demande.getRaisonSociale());
	}

	private static void assertDelai(RegDate dateDemande, RegDate delaiAccordeAu, EtatDelaiDocumentFiscal etatDelai, DelaiDocumentFiscal delai) {
		assertEquals(dateDemande, delai.getDateDemande());
		assertEquals(delaiAccordeAu, delai.getDelaiAccordeAu());
		assertEquals(etatDelai, delai.getEtat());
	}
}