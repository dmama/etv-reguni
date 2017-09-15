package ch.vd.uniregctb.evenement.externe;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDocumentFiscal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractEvenementExterneEsbHandlerTest extends BusinessTest {

	protected EvenementExterneEsbHandler handler;

	private EvenementExterneDAO evenementExterneDAO;
	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementExterneDAO = getBean(EvenementExterneDAO.class, "evenementExterneDAO");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		final EvenementExterneService service = getBean(EvenementExterneService.class, "evenementExterneService");

		handler = new EvenementExterneEsbHandler();
		handler.setHandler(service);

		final List<EvenementExterneConnector> connectors = Collections.<EvenementExterneConnector>singletonList(getTestedConnector());
		handler.setConnectors(connectors);
		handler.afterPropertiesSet();
	}

	protected abstract EvenementExterneConnector<?> getTestedConnector();

	protected interface MessageCreator {
		EsbMessage createNewMessage(long dpiId, RegDate dateDebut, RegDate dateFin, RegDate dateQuittancement) throws Exception;
	}

	protected void doTestNewEventQuittancement(final MessageCreator msgCreator) throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = msgCreator.createNewMessage(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				final EvenementExterne evt = evts.get(0);
				assertNotNull(evt);
				assertEquals(EtatEvenementExterne.TRAITE, evt.getEtat());
				final String xml = evt.getMessage();
				assertTrue(xml, xml.startsWith("<?xml version=\"1.0\""));
				assertTrue(xml, xml.endsWith("evtListe>"));


				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final Set<EtatDeclaration> etats = lr.getEtatsDeclaration();
				assertNotNull(etats);
				assertEquals(2, etats.size());      // l'état "EMISE" et l'état "RETOURNEE"

				final EtatDeclaration etatEmission = lr.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.EMISE);
				assertNotNull(etatEmission);
				assertTrue(etats.contains(etatEmission));
				assertEquals(TypeEtatDocumentFiscal.EMISE, etatEmission.getEtat());
				assertEquals(dateFin, etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatRetour = lr.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNEE);
				assertNotNull(etatRetour);
				assertTrue(etats.contains(etatRetour));
				assertEquals(TypeEtatDocumentFiscal.RETOURNEE, etatRetour.getEtat());
				assertEquals(dateQuittancement, etatRetour.getDateObtention());
				assertFalse(etatRetour.isAnnule());

				return null;
			}
		});
	}

	protected void doTestNewEventLC(final MessageCreator msgCreator) throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = msgCreator.createNewMessage(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(0, evts.size());
				return null;
			}
		});
	}

	protected void doTestNewEventAnnulationEtatRetourInexistant(final MessageCreator msgCreator) throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = msgCreator.createNewMessage(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.ERREUR, evts.get(0).getEtat());
				final String erreurAttendue = String.format("La déclaration impôt source sélectionnée (tiers=%d, période=%s) ne contient pas de retour à annuler.",
				                                            dpiId, DateRangeHelper.toDisplayString(new DateRangeHelper.Range(dateDebut, dateFin)));
				assertEquals(erreurAttendue, evts.get(0).getErrorMessage());

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final Set<EtatDeclaration> etats = lr.getEtatsDeclaration();
				assertNotNull(etats);
				assertEquals(1, etats.size());      // l'état "EMISE"

				final EtatDeclaration etatEmission = etats.iterator().next();
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDocumentFiscal.EMISE, etatEmission.getEtat());
				assertEquals(dateFin, etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				return null;
			}
		});
	}

	protected void doTestNewEventDoubleAnnulation(final MessageCreator msgCreator) throws Exception{

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationEmise(dateFin.addDays(-10)));

				final EtatDeclaration etatRetourne = new EtatDeclarationRetournee(dateQuittancement, "TEST");
				etatRetourne.setAnnule(true);
				lr.addEtat(etatRetourne);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = msgCreator.createNewMessage(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.ERREUR, evts.get(0).getEtat());
				final String erreurAttendue = String.format("La déclaration impôt source sélectionnée (tiers=%s, période=%s) ne contient pas de retour à annuler.",
				                                            dpiId, DateRangeHelper.toDisplayString(new DateRangeHelper.Range(dateDebut, dateFin)));
				assertEquals(erreurAttendue, evts.get(0).getErrorMessage());
				return null;
			}
		});
	}

	protected void doTestNewEventAnnulation(final MessageCreator msgCreator) throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement, "TEST"));

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = msgCreator.createNewMessage(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.TRAITE, evts.get(0).getEtat());

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final List<EtatDeclaration> etats = lr.getEtatsDeclarationSorted();
				assertNotNull(etats);
				assertEquals(2, etats.size());      // états "EMISE" et "RETOURNEE" (ce dernier annulé)

				final EtatDeclaration etatEmission = etats.get(0);
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDocumentFiscal.EMISE, etatEmission.getEtat());
				assertEquals(dateFin,  etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatRetour = etats.get(1);
				assertNotNull(etatRetour);
				assertEquals(TypeEtatDocumentFiscal.RETOURNEE, etatRetour.getEtat());
				assertEquals(dateQuittancement, etatRetour.getDateObtention());
				assertTrue(etatRetour.isAnnule());

				return null;
			}
		});
	}

	protected void doTestNewEvenementDoubleQuittancement(final MessageCreator msgCreator) throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement, "TEST"));

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = msgCreator.createNewMessage(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.TRAITE, evts.get(0).getEtat());

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final List<EtatDeclaration> etats = lr.getEtatsDeclarationSorted();
				assertNotNull(etats);
				assertEquals(3, etats.size());      // l'état "EMISE", puis les deux états "RETOURNEE", dont l'un est annulé

				final EtatDeclaration etatEmission = etats.get(0);
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDocumentFiscal.EMISE, etatEmission.getEtat());
				assertEquals(dateFin,  etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatAnnule = etats.get(1);
				assertNotNull(etatAnnule);
				assertEquals(TypeEtatDocumentFiscal.RETOURNEE, etatAnnule.getEtat());
				assertEquals(dateQuittancement, etatAnnule.getDateObtention());
				assertTrue(etatAnnule.isAnnule());

				final EtatDeclaration etatValide = etats.get(2);
				assertNotNull(etatValide);
				assertEquals(TypeEtatDocumentFiscal.RETOURNEE, etatValide.getEtat());
				assertEquals(dateQuittancement, etatValide.getDateObtention());
				assertFalse(etatValide.isAnnule());

				final EtatDeclaration dernierEtat = lr.getDernierEtatDeclaration();
				assertNotNull(dernierEtat);
				assertEquals(TypeEtatDocumentFiscal.RETOURNEE, dernierEtat.getEtat());
				assertFalse(dernierEtat.isAnnule());
				return null;
			}
		});
	}

	protected void doTestNewEvenementAnnulationDoubleQuittancement(final MessageCreator msgCreator) throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = PeriodiciteDecompte.TRIMESTRIEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement, "TEST"));
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement, "TEST"));

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = msgCreator.createNewMessage(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.TRAITE, evts.get(0).getEtat());

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final List<EtatDeclaration> etats = lr.getEtatsDeclarationSorted();
				assertNotNull(etats);
				assertEquals(3, etats.size());      // "EMISE", et deux "RETOURNEE", dont un est annulé

				final EtatDeclaration etatEmission = etats.get(0);
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDocumentFiscal.EMISE, etatEmission.getEtat());
				assertEquals(dateFin,  etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());

				final EtatDeclaration etatAnnule = etats.get(1);
				assertNotNull(etatAnnule);
				assertEquals(TypeEtatDocumentFiscal.RETOURNEE, etatAnnule.getEtat());
				assertEquals(dateQuittancement, etatAnnule.getDateObtention());
				assertTrue(etatAnnule.isAnnule());

				final EtatDeclaration etatValide = etats.get(2);
				assertNotNull(etatValide);
				assertEquals(TypeEtatDocumentFiscal.RETOURNEE, etatValide.getEtat());
				assertEquals(dateQuittancement, etatValide.getDateObtention());
				assertFalse(etatValide.isAnnule());

				final EtatDeclaration dernierEtat = lr.getDernierEtatDeclaration();
				assertNotNull(dernierEtat);
				assertEquals(TypeEtatDocumentFiscal.RETOURNEE, dernierEtat.getEtat());
				assertFalse(dernierEtat.isAnnule());
				return null;
			}
		});
	}
	//SIFISC-15259
	//Permet de tester qu'un message avec une date devenement dans le futur de quelques minutes mais sur la même journée ne provoque pas d'exception
	protected void doTestNewEvenementQuittancementDesynchroHeure(final MessageCreator msgCreator) throws Exception {

		final RegDate dateDebut = date(2015, 1, 1);
		final RegDate dateFin = PeriodiciteDecompte.MENSUEL.getFinPeriode(dateDebut);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2015);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, PeriodiciteDecompte.MENSUEL, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final EsbMessage message = msgCreator.createNewMessage(dpiId, dateDebut, dateFin, dateQuittancement);
				handler.onMessage(message, "TEST-" + System.currentTimeMillis());
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.TRAITE, evts.get(0).getEtat());

				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final List<EtatDeclaration> etats = lr.getEtatsDeclarationSorted();
				assertNotNull(etats);
				assertEquals(2, etats.size());      // l'état "EMISE", puis les deux états "RETOURNEE", dont l'un est annulé

				final EtatDeclaration etatEmission = etats.get(0);
				assertNotNull(etatEmission);
				assertEquals(TypeEtatDocumentFiscal.EMISE, etatEmission.getEtat());
				assertEquals(dateFin,  etatEmission.getDateObtention());
				assertFalse(etatEmission.isAnnule());


				final EtatDeclaration dernierEtat = lr.getDernierEtatDeclaration();
				assertNotNull(dernierEtat);
				assertEquals(TypeEtatDocumentFiscal.RETOURNEE, dernierEtat.getEtat());
				assertFalse(dernierEtat.isAnnule());
				return null;
			}
		});
	}
}
