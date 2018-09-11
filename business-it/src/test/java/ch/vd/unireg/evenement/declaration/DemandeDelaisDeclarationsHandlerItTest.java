package ch.vd.unireg.evenement.declaration;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscal;
import ch.vd.unireg.evenement.EvenementHelper;
import ch.vd.unireg.jms.EsbBusinessErrorHandler;
import ch.vd.unireg.jms.GentilEsbMessageListenerContainer;
import ch.vd.unireg.mandataire.DemandeDelaisMandataireDAO;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DemandeDelaisDeclarationsHandlerItTest extends BusinessItTest {

	private String INPUT_QUEUE;
	private EsbJmsTemplate esbTemplate;
	private DemandeDelaisDeclarationsHandler handler;
	private GentilEsbMessageListenerContainer listener;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");

		INPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtDeclaration.delais");
		EvenementHelper.clearQueue(esbTemplate, INPUT_QUEUE, transactionManager);

		handler = new DemandeDelaisDeclarationsHandler();
		handler.setDeclarationImpotService(getBean(DeclarationImpotService.class, "diService"));
		handler.setDemandeDelaisMandataireDAO(getBean(DemandeDelaisMandataireDAO.class, "demandeDelaisMandataireDAO"));
		handler.setHibernateTemplate(hibernateTemplate);
		handler.setTiersDAO(tiersDAO);
		handler.afterPropertiesSet();

		listener = new GentilEsbMessageListenerContainer();
		listener.setEsbTemplate(esbTemplate);
		listener.setEsbErrorHandler(getBean(EsbBusinessErrorHandler.class, "esbErrorHandler"));
		listener.setTransactionManager(transactionManager);
		listener.setConnectionFactory(getBean(ConnectionFactory.class, "jmsConnectionFactory"));
		listener.setHandler(handler);
		listener.setDestinationName(INPUT_QUEUE);
		listener.afterPropertiesSet();
		listener.start();
	}

	@Override
	public void onTearDown() throws Exception {
		if (listener != null) {
			listener.stop();
			listener.destroy();
		}
		super.onTearDown();
	}

	/**
	 * Teste qu'une demande de délais unitaire est bien traitée dans le cas passant.
	 */
	@Test(timeout = 20000)
	public void testDemandeDelaisUnitaireOK() throws Exception {

		final long idPP1 = 10000010L;

		// mise en place, 1 pp avec une déclaration
		doInNewTransaction(status -> {
			final PeriodeFiscale periode2016 = addPeriodeFiscale(2016);
			final ModeleDocument modele2016 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2016);

			final PersonnePhysique pp1 = addNonHabitant(idPP1, "Rodolf", "Frigo", date(1970, 1, 1), Sexe.MASCULIN);
			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp1, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
			addEtatDeclarationEmise(di, date(2017, 1, 15));
			addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			return null;
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/evenement/declaration/demandeDelaisUnitaire.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		EvenementHelper.sendTextMessage(esbTemplate, INPUT_QUEUE, texte, "businessId", null, transactionManager);

		// On attend que le message soit traité
		DelaiDocumentFiscal d;
		do {
			d = doInNewTransaction(status -> {
				final Tiers pp1 = tiersDAO.get(idPP1);
				assertNotNull(pp1);
				final List<DeclarationImpotOrdinairePP> list = pp1.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, 2016, false);
				assertNotNull(list);
				assertEquals(1, list.size());
				return list.get(0).getDernierDelaiAccorde();
			});
			Thread.sleep(100);
		}
		while (d.getDelaiAccordeAu() == date(2017, 3, 15));

		// On vérifie que les délais sont bien ajoutés sur les déclarations
		doInNewTransaction(status -> {
			final Tiers pp = tiersDAO.get(idPP1);
			assertNotNull(pp);
			final List<DeclarationImpotOrdinairePP> list = pp.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, 2016, false);
			assertNotNull(list);
			assertEquals(1, list.size());
			final DeclarationImpotOrdinairePP di = list.get(0);
			final DelaiDocumentFiscal delai = di.getDernierDelaiAccorde();
			assertEquals(date(2017, 5, 29), delai.getDateDemande());
			assertEquals(date(2025, 6, 30), delai.getDelaiAccordeAu());
			return null;
		});
	}

	/**
	 * Teste qu'une demande de délais groupée est bien traitée dans le cas passant.
	 */
	@Test(timeout = 20000)
	public void testDemandeDelaisGroupeeOK() throws Exception {

		final long idPP1 = 10000010L;
		final long idPP2 = 10000011L;
		final long idPP3 = 10000012L;

		// mise en place, 3 pp avec chacun une déclaration
		doInNewTransaction(status -> {
			final PeriodeFiscale periode2016 = addPeriodeFiscale(2016);
			final ModeleDocument modele2016 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2016);

			final PersonnePhysique pp1 = addNonHabitant(idPP1, "Rodolf", "Frigo", date(1970, 1, 1), Sexe.MASCULIN);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp1, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}

			final PersonnePhysique pp2 = addNonHabitant(idPP2, "Fleurette", "Bijoux", date(1970, 1, 1), Sexe.FEMININ);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp2, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}

			final PersonnePhysique pp3 = addNonHabitant(idPP3, "Melania", "Trompette", date(1970, 1, 1), Sexe.FEMININ);
			{
				final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp3, periode2016, date(2016, 1, 1), date(2016, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2016);
				addEtatDeclarationEmise(di, date(2017, 1, 15));
				addDelaiDeclaration(di, date(2017, 1, 15), date(2017, 3, 15), EtatDelaiDocumentFiscal.ACCORDE);
			}
			return null;
		});

		// Lit le message sous format texte
		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/evenement/declaration/demandeDelaisGroupee.xml");
		final String texte = FileUtils.readFileToString(file);

		// Envoie le message
		EvenementHelper.sendTextMessage(esbTemplate, INPUT_QUEUE, texte, "businessId", null, transactionManager);

		// On attend que le message soit traité
		DelaiDocumentFiscal d;
		do {
			d = doInNewTransaction(status -> {
				final Tiers pp1 = tiersDAO.get(idPP1);
				assertNotNull(pp1);
				final List<DeclarationImpotOrdinairePP> list = pp1.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, 2016, false);
				assertNotNull(list);
				assertEquals(1, list.size());
				return list.get(0).getDernierDelaiAccorde();
			});
			Thread.sleep(100);
		}
		while (d.getDelaiAccordeAu() == date(2017, 3, 15));

		// On vérifie que les délais sont bien ajoutés sur les déclarations
		doInNewTransaction(status -> {
			{
				final Tiers pp = tiersDAO.get(idPP1);
				assertNotNull(pp);
				final List<DeclarationImpotOrdinairePP> list = pp.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, 2016, false);
				assertNotNull(list);
				assertEquals(1, list.size());
				final DeclarationImpotOrdinairePP di = list.get(0);
				final DelaiDocumentFiscal delai = di.getDernierDelaiAccorde();
				assertEquals(date(2017, 5, 29), delai.getDateDemande());
				assertEquals(date(2025, 6, 30), delai.getDelaiAccordeAu());
			}
			{
				final Tiers pp = tiersDAO.get(idPP2);
				assertNotNull(pp);
				final List<DeclarationImpotOrdinairePP> list = pp.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, 2016, false);
				assertNotNull(list);
				assertEquals(1, list.size());
				final DeclarationImpotOrdinairePP di = list.get(0);
				final DelaiDocumentFiscal delai = di.getDernierDelaiAccorde();
				assertEquals(date(2017, 5, 29), delai.getDateDemande());
				assertEquals(date(2025, 6, 30), delai.getDelaiAccordeAu());
			}
			{
				final Tiers pp = tiersDAO.get(idPP3);
				assertNotNull(pp);
				final List<DeclarationImpotOrdinairePP> list = pp.getDeclarationsDansPeriode(DeclarationImpotOrdinairePP.class, 2016, false);
				assertNotNull(list);
				assertEquals(1, list.size());
				final DeclarationImpotOrdinairePP di = list.get(0);
				final DelaiDocumentFiscal delai = di.getDernierDelaiAccorde();
				assertEquals(date(2017, 5, 29), delai.getDateDemande());
				assertEquals(date(2025, 6, 30), delai.getDelaiAccordeAu());
			}
			return null;
		});
	}
}