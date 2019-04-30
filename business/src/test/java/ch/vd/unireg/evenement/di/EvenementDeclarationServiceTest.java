package ch.vd.unireg.evenement.di;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.jms.BamMessageSender;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.ModeleFeuille;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EvenementDeclarationServiceTest extends BusinessTest {

	private EvenementDeclarationServiceImpl service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		service = new EvenementDeclarationServiceImpl();
		service.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		service.setDiService(getBean(DeclarationImpotService.class,"diService"));
		service.setValidationService(getBean(ValidationService.class, "validationService"));
		service.setBamMessageSender(getBean(BamMessageSender.class, "bamMessageSender"));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testQuittancerDI() throws Exception {

		// Création d'un contribuable ordinaire et de sa DI
		final Long id = doInNewTransaction(status -> {
			final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
			final ModeleDocument declarationComplete2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2011);
			addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_210, declarationComplete2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_220, declarationComplete2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_230, declarationComplete2011);
			addModeleFeuilleDocument(ModeleFeuille.ANNEXE_240, declarationComplete2011);


			// Un tiers tout ce quil y a de plus ordinaire
			final PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
			addDeclarationImpot(eric, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
			                    declarationComplete2011);
			return eric.getNumero();
		});

		// Simule la réception d'un événement de quittancement de DI
		doInNewTransaction(status -> {
			final QuittancementDI quittance = new QuittancementDI();
			quittance.setNumeroContribuable(id.intValue());
			quittance.setSource("ADDI");
			quittance.setDate(RegDate.get(2012, 5, 26));
			quittance.setPeriodeFiscale(2011);
			quittance.setBusinessId("1245633");

			service.onEvent(quittance, Collections.<String, String>emptyMap());
			return null;
		});

		// Vérifie que les informations personnelles ainsi que le type de DI ont bien été mis-à-jour
		doInNewTransaction(status -> {
			final PersonnePhysique eric = hibernateTemplate.get(PersonnePhysique.class, id);

			final List<Declaration> list = eric.getDeclarationsDansPeriode(Declaration.class, 2011, false);
			assertNotNull(list);
			assertEquals(1, list.size());

			final DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) list.get(0);
			final EtatDeclaration etat = declaration.getDernierEtatDeclaration();
			assertTrue(etat instanceof EtatDeclarationRetournee);

			final EtatDeclarationRetournee retour = (EtatDeclarationRetournee) etat;
			assertEquals(date(2012, 5, 26), retour.getDateObtention());
			assertEquals("ADDI", retour.getSource());
			return null;
		});
	}


}
