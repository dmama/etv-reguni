package ch.vd.uniregctb.evenement.di;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
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
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testQuittancerDI() throws Exception {

		// Création d'un contribuable ordinaire et de sa DI
		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				addCollAdm(ServiceInfrastructureService.noCEDI);

				final PeriodeFiscale periode2011 = addPeriodeFiscale(2011);
				final ModeleDocument declarationComplete2011 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2011);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2011);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2011);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2011);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2011);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2011);


				// Un tiers tout ce quil y a de plus ordinaire
				final PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addDeclarationImpot(eric, periode2011, date(2011, 1, 1), date(2011, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2011);

				return eric.getNumero();
			}
		});

		// Simule la réception d'un événement de quittancement de DI
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final QuittancementDI quittance = new QuittancementDI();
				quittance.setNumeroContribuable(id.intValue());
				quittance.setSource("ADDI");
				quittance.setDate(RegDate.get(2012, 5, 26));
				quittance.setPeriodeFiscale(2011);
				quittance.setBusinessId("1245633");

				service.onEvent(quittance);
				return null;
			}
		});

		// Vérifie que les informations personnelles ainsi que le type de DI ont bien été mis-à-jour
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique eric = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, id);

				final List<Declaration> list = eric.getDeclarationsForPeriode(2011);
				assertNotNull(list);
				assertEquals(1, list.size());
				
				final DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) list.get(0);
				EtatDeclaration etat  = declaration.getDernierEtat();
				assertTrue(etat instanceof EtatDeclarationRetournee);
				assertEquals(date(2012,5,26),etat.getDateObtention());

				return null;
			}
		});
	}


}
