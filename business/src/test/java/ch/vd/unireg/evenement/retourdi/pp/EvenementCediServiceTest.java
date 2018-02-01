package ch.vd.unireg.evenement.retourdi.pp;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleDocumentDAO;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.iban.IbanValidator;
import ch.vd.unireg.jms.BamMessageSender;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.ModeleFeuille;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.validation.ValidationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EvenementCediServiceTest extends BusinessTest {

	private EvenementCediServiceImpl service;
	private PeriodeFiscaleDAO pfDao;
	private ModeleDocumentDAO modeleDocumentDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		pfDao = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		modeleDocumentDAO = getBean(ModeleDocumentDAO.class, "modeleDocumentDAO");

		service = new EvenementCediServiceImpl();
		service.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		service.setModeleDocumentDAO(modeleDocumentDAO);
		service.setPeriodeFiscaleDAO(pfDao);
		service.setValidationService(getBean(ValidationService.class, "validationService"));
		service.setBamMessageSender(getBean(BamMessageSender.class, "bamMessageSender"));
		service.setIbanValidator(getBean(IbanValidator.class, "ibanValidator"));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testModifierInformationsPersonnelles() throws Exception {

		// Création d'un contribuable ordinaire et de sa DI
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2008);


				// Un tiers tout ce quil y a de plus ordinaire
				final PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addDeclarationImpot(eric, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2008);

				return eric.getNumero();
			}
		});

		// Simule la réception d'un événement de scan de DI
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final RetourDI scan = new RetourDI();
				scan.setNoContribuable(id.intValue());
				scan.setEmail("zuzu@gmail.com");
				scan.setIban("CFE2145000321457");
				scan.setNoMobile("0789651243");
				scan.setNoTelephone("0215478936");
				scan.setTitulaireCompte("Famille devel");
				scan.setTypeDocument(RetourDI.TypeDocument.VAUDTAX);
				scan.setPeriodeFiscale(2008);
				scan.setNoSequenceDI(1);
				service.onRetourDI(scan, Collections.<String, String>emptyMap());
				return null;
			}
		});

		// Vérifie que les informations personnelles ainsi que le type de DI ont bien été mis-à-jour
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, id);
				assertEquals("zuzu@gmail.com", eric.getAdresseCourrierElectronique());
				assertEquals("CFE2145000321457", eric.getNumeroCompteBancaire());
				assertEquals("Famille devel", eric.getTitulaireCompteBancaire());
				assertEquals("0215478936", eric.getNumeroTelephonePrive());
				assertEquals("0789651243", eric.getNumeroTelephonePortable());

				final List<Declaration> list = eric.getDeclarationsDansPeriode(Declaration.class, 2008, false);
				assertNotNull(list);
				assertEquals(1, list.size());
				
				final DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) list.get(0);
				assertEquals(TypeDocument.DECLARATION_IMPOT_VAUDTAX, declaration.getModeleDocument().getTypeDocument());

				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testModifierInformationsPersonnellesAvecStreamlining() throws Exception {

		assertEquals("La valeur de la constante a changé, le test doit être modifié", 35, LengthConstants.TIERS_NUMTEL);

		// Création d'un contribuable ordinaire et de sa DI
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2008);


				// Un tiers tout ce quil y a de plus ordinaire
				final PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addDeclarationImpot(eric, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2008);

				return eric.getNumero();
			}
		});

		// Simule la réception d'un événement de scan de DI
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final RetourDI scan = new RetourDI();
				scan.setNoContribuable(id.intValue());
				scan.setEmail("Maison: zuzu@gmail.com    Boulot: toto@monentreprise.ch");
				scan.setIban("CFE2145000321457    ");
				scan.setNoMobile    ("Moi 0789651243    Ma copine 0791234567");
				scan.setNoTelephone ("Rez: 0215478936 A l'étage tout en haut: 0213456789");
				scan.setTitulaireCompte("Famille  devel");
				scan.setTypeDocument(RetourDI.TypeDocument.VAUDTAX);
				scan.setPeriodeFiscale(2008);
				scan.setNoSequenceDI(1);
				service.onRetourDI(scan, Collections.<String, String>emptyMap());
				return null;
			}
		});

		// Vérifie que les informations personnelles ainsi que le type de DI ont bien été mis-à-jour
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, id);
				assertEquals("Maison: zuzu@gmail.com Boulot: toto@monentreprise.ch", eric.getAdresseCourrierElectronique());
				assertEquals("CFE2145000321457", eric.getNumeroCompteBancaire());
				assertEquals("Famille devel", eric.getTitulaireCompteBancaire());
				assertEquals("Rez: 0215478936 A l'étage tout en h", eric.getNumeroTelephonePrive());
				assertEquals("Moi 0789651243 Ma copine 0791234567", eric.getNumeroTelephonePortable());

				final List<Declaration> list = eric.getDeclarationsDansPeriode(Declaration.class, 2008, false);
				assertNotNull(list);
				assertEquals(1, list.size());

				final DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) list.get(0);
				assertEquals(TypeDocument.DECLARATION_IMPOT_VAUDTAX, declaration.getModeleDocument().getTypeDocument());

				return null;
			}
		});
	}

	@Test
	public void testModificationsIban() throws Exception {

		// mise en place des données de base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2008);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2008);
				return null;
			}
		});

		doTestModificationIban("CH452365", null, false);                        // null ignoré sur IBAN invalide
		doTestModificationIban("CH9308440717427290198", null, false);           // null ignoré sur IBAN valide

		doTestModificationIban(null, "CH", false);                      // CH ignoré
		doTestModificationIban(null, "CH547458", true);                 // valeur vide remplacée, même par donnée invalide
		doTestModificationIban(null, "CH9308440717427290198", true);    // IBAN valide

		doTestModificationIban("CH443", null, false);                       // null ignoré
		doTestModificationIban("CH443", "CH", false);                       // CH ignoré
		doTestModificationIban("CH443", "CH547458", true);                  // valeur invalide remplacée par autre valeur invalide
		doTestModificationIban("CH443", "CH9308440717427290198", true);     // IBAN valide

		doTestModificationIban("CH690023000123456789A", null, false);                       // null ignoré
		doTestModificationIban("CH690023000123456789A", "CH", false);                       // CH ignoré
		doTestModificationIban("CH690023000123456789A", "CH547458", false);                 // valeur valide non-remplacée par valeur invalide
		doTestModificationIban("CH690023000123456789A", "CH9308440717427290198", true);     // IBAN valide
	}

	private void doTestModificationIban(final String ibanInitial, final String nouvelIban, final boolean replacementExpected) throws Exception {

		// Création d'un contribuable ordinaire et de sa DI
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2008 = pfDao.getPeriodeFiscaleByYear(2008);
				final ModeleDocument declarationComplete2008 = modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(periode2008, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);

				// Un tiers tout ce quil y a de plus ordinaire
				final PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addDeclarationImpot(eric, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				                    declarationComplete2008);

				// le numéro présent en base avant réception du nouveau
				if (StringUtils.isNotBlank(ibanInitial)) {
					eric.setCoordonneesFinancieres(new CoordonneesFinancieres(ibanInitial, null));
				}

				return eric.getNumero();
			}
		});

		// Simule la réception d'un événement de scan de DI
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final RetourDI scan = new RetourDI();
				scan.setNoContribuable(id.intValue());
				scan.setIban(nouvelIban);
				scan.setTypeDocument(RetourDI.TypeDocument.VAUDTAX);
				scan.setPeriodeFiscale(2008);
				scan.setNoSequenceDI(1);
				service.onRetourDI(scan, Collections.<String, String>emptyMap());
				return null;
			}
		});

		// Vérifie que les informations personnelles ainsi que le type de DI ont bien été mis-à-jour
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, id);
				if ((!replacementExpected && ibanInitial == null) || (replacementExpected && nouvelIban == null)) {
					assertNull(eric.getNumeroCompteBancaire());
				}
				else {
					assertEquals(replacementExpected ? nouvelIban : ibanInitial, eric.getNumeroCompteBancaire());
				}
				return null;
			}
		});
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testModifierInformationsPersonnellesPourDiAnnulee() throws Exception {

		// Création d'un contribuable ordinaire et de sa DI
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2014 = addPeriodeFiscale(2014);
				final ModeleDocument declarationComplete2014 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2014);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2014);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2014);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2014);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2014);
				addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2014);

				// Un tiers tout ce quil y a de plus ordinaire
				final PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				final DeclarationImpotOrdinaire d2014_1 = addDeclarationImpot(eric, periode2014, date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2014);
				d2014_1.setAnnule(true);
				final DeclarationImpotOrdinaire d2014_2 = addDeclarationImpot(eric, periode2014, date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2014);


				return eric.getNumero();
			}
		});

		// Simule la réception d'un événement de scan de DI
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final RetourDI scan = new RetourDI();
				scan.setNoContribuable(id.intValue());
				scan.setEmail("zuzu@gmail.com");
				scan.setIban("CFE2145000321457");
				scan.setNoMobile("0789651243");
				scan.setNoTelephone("0215478936");
				scan.setTitulaireCompte("Famille devel");
				scan.setTypeDocument(RetourDI.TypeDocument.VAUDTAX);
				scan.setPeriodeFiscale(2014);
				scan.setNoSequenceDI(1);
				service.onRetourDI(scan, Collections.<String, String>emptyMap());
				return null;
			}
		});

		// Vérifie que les informations personnelles ainsi que le type de DI ont bien été mis-à-jour
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, id);
				assertEquals("zuzu@gmail.com", eric.getAdresseCourrierElectronique());
				assertEquals("CFE2145000321457", eric.getNumeroCompteBancaire());
				assertEquals("Famille devel", eric.getTitulaireCompteBancaire());
				assertEquals("0215478936", eric.getNumeroTelephonePrive());
				assertEquals("0789651243", eric.getNumeroTelephonePortable());
				final List<Declaration> list = eric.getDeclarationsDansPeriode(Declaration.class, 2014, false);
				assertNotNull(list);
				assertEquals(1, list.size());

				final DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) list.get(0);
				assertEquals(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, declaration.getModeleDocument().getTypeDocument());
				return null;
			}
		});
	}

}
