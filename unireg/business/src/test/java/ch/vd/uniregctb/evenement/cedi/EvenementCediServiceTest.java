package ch.vd.uniregctb.evenement.cedi;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.validation.ValidationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EvenementCediServiceTest extends BusinessTest {

	private EvenementCediServiceImpl service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = new EvenementCediServiceImpl();
		service.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		service.setModeleDocumentDAO(getBean(ModeleDocumentDAO.class, "modeleDocumentDAO"));
		service.setPeriodeFiscaleDAO(getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO"));
		service.setValidationService(getBean(ValidationService.class, "validationService"));
	}

	@Test
	public void testModifierInformationsPersonnelles() throws Exception {

		// Création d'un contribuable ordinaire et de sa DI
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				addCollAdm(ServiceInfrastructureService.noCEDI);

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2008);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2008);


				// Un tiers tout ce quil y a de plus ordinaire
				final PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addDeclarationImpot(eric, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2008);

				return eric.getNumero();
			}
		});

		// Simule la réception d'un événement de scan de DI
		doInNewTransaction(new TxCallback() {
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
				service.onRetourDI(scan);
				return null;
			}
		});

		// Vérifie que les informations personnelles ainsi que le type de DI ont bien été mis-à-jour
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique eric = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, id);
				assertEquals("zuzu@gmail.com", eric.getAdresseCourrierElectronique());
				assertEquals("CFE2145000321457", eric.getNumeroCompteBancaire());
				assertEquals("Famille devel", eric.getTitulaireCompteBancaire());
				assertEquals("0215478936", eric.getNumeroTelephonePrive());
				assertEquals("0789651243", eric.getNumeroTelephonePortable());

				final List<Declaration> list = eric.getDeclarationsForPeriode(2008);
				assertNotNull(list);
				assertEquals(1, list.size());
				
				final DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) list.get(0);
				assertEquals(TypeDocument.DECLARATION_IMPOT_VAUDTAX, declaration.getModeleDocument().getTypeDocument());

				return null;
			}
		});
	}

	@Test
	public void testModifierInformationsPersonnellesAvecStreamlining() throws Exception {

		assertEquals("La valeur de la constante a changé, le test doit être modifié", 35, LengthConstants.TIERS_NUMTEL);

		// Création d'un contribuable ordinaire et de sa DI
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				addCollAdm(ServiceInfrastructureService.noCEDI);

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2008);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2008);


				// Un tiers tout ce quil y a de plus ordinaire
				final PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addDeclarationImpot(eric, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2008);

				return eric.getNumero();
			}
		});

		// Simule la réception d'un événement de scan de DI
		doInNewTransaction(new TxCallback() {
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
				service.onRetourDI(scan);
				return null;
			}
		});

		// Vérifie que les informations personnelles ainsi que le type de DI ont bien été mis-à-jour
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique eric = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, id);
				assertEquals("Maison: zuzu@gmail.com Boulot: toto@monentreprise.ch", eric.getAdresseCourrierElectronique());
				assertEquals("CFE2145000321457", eric.getNumeroCompteBancaire());
				assertEquals("Famille devel", eric.getTitulaireCompteBancaire());
				assertEquals("Rez: 0215478936 A l'étage tout en h", eric.getNumeroTelephonePrive());
				assertEquals("Moi 0789651243 Ma copine 0791234567", eric.getNumeroTelephonePortable());

				final List<Declaration> list = eric.getDeclarationsForPeriode(2008);
				assertNotNull(list);
				assertEquals(1, list.size());

				final DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) list.get(0);
				assertEquals(TypeDocument.DECLARATION_IMPOT_VAUDTAX, declaration.getModeleDocument().getTypeDocument());

				return null;
			}
		});
	}
}
