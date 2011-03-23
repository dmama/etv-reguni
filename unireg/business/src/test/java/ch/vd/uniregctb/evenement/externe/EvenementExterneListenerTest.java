package ch.vd.uniregctb.evenement.externe;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;


import ch.vd.fiscalite.taxation.evtQuittanceListeV1.EvtQuittanceListeDocument;
import ch.vd.fiscalite.taxation.evtQuittanceListeV1.ListeType;
import ch.vd.fiscalite.taxation.evtQuittanceListeV1.QuittanceType;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EvenementExterneListenerTest extends BusinessTest {

	private EvenementExterneListenerImpl listener;

	private EvenementExterneDAO evenementExterneDAO;
	private TiersDAO tiersDAO;
	private EvenementExterneService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementExterneDAO = getBean(EvenementExterneDAO.class, "evenementExterneDAO");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		service = getBean(EvenementExterneService.class, "evenementExterneService");

		listener = new EvenementExterneListenerImpl();
		listener.setHandler(service);
	}

	@Test
	public void testEventImpotSource() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, dateFin, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final String message = createMessageQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				try {
					listener.onMessage(message, "TEST-" + System.currentTimeMillis());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
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

				final Set<EtatDeclaration> etats = lr.getEtats();
				assertNotNull(etats);
				assertEquals(1, etats.size());

				final EtatDeclaration etat = etats.iterator().next();
				assertNotNull(etat);
				assertEquals(TypeEtatDeclaration.RETOURNEE, etat.getEtat());
				assertEquals(dateQuittancement, etat.getDateObtention());
				assertFalse(etat.isAnnule());

				return null;
			}
		});
	}

	@Test
	public void testEventLC() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, dateFin, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final String message = createMessageLC(dpiId, dateDebut, dateFin, dateQuittancement);
				try {
					listener.onMessage(message, "TEST-" + System.currentTimeMillis());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(0, evts.size());
				

				return null;
			}
		});
	}

	@Test
	public void testEventAnnulationEtatRetourNonExiste() throws Exception{

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();


		final long dpiId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);
				addLR(dpi, dateDebut, dateFin, pf);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final String message = createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				try {
					listener.onMessage(message, "TEST-" + System.currentTimeMillis());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.ERREUR, evts.get(0).getEtat());
				assertEquals("La déclaration impôt source sélectionnée ne contient pas de retour à annuler.", evts.get(0).getErrorMessage());


				final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(dpiId);
				final Set<Declaration> lrs = dpi.getDeclarations();
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = (DeclarationImpotSource) lrs.iterator().next();
				assertNotNull(lr);

				final Set<EtatDeclaration> etats = lr.getEtats();
				assertNotNull(etats);
				assertEquals(0, etats.size());

				return null;
			}
		});
	}

	@Test
	public void testEventDoubleAnnulation() throws Exception{

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationEmise(dateFin.addDays(-10)));

				final EtatDeclaration etatRetourne = new EtatDeclarationRetournee(dateQuittancement);
				etatRetourne.setAnnule(true);
				lr.addEtat(etatRetourne);

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final String message = createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				try {
					listener.onMessage(message, "TEST-" + System.currentTimeMillis());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final List<EvenementExterne> evts = evenementExterneDAO.getAll();
				assertEquals(1, evts.size());
				assertNotNull(evts.get(0));
				assertEquals(EtatEvenementExterne.ERREUR, evts.get(0).getEtat());
				assertEquals("La déclaration impôt source sélectionnée ne contient pas de retour à annuler.", evts.get(0).getErrorMessage());
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testEventAnnulation() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement));

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final String message = createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				try {
					listener.onMessage(message, "TEST-" + System.currentTimeMillis());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
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

				final Set<EtatDeclaration> etats = lr.getEtats();
				assertNotNull(etats);
				assertEquals(1, etats.size());

				final EtatDeclaration etat = etats.iterator().next();
				assertNotNull(etat);
				assertEquals(TypeEtatDeclaration.RETOURNEE, etat.getEtat());
				assertEquals(dateQuittancement, etat.getDateObtention());
				assertTrue(etat.isAnnule());

				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testEvenementDoubleQuittancement() throws Exception {
		
		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement));

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final String message = createMessageQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				try {
					listener.onMessage(message, "TEST-" + System.currentTimeMillis());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
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

				final Set<EtatDeclaration> etats = lr.getEtats();
				assertNotNull(etats);
				assertEquals(2, etats.size());

				for (EtatDeclaration etat : etats) {
					assertNotNull(etat);
					assertEquals(TypeEtatDeclaration.RETOURNEE, etat.getEtat());
					assertEquals(dateQuittancement, etat.getDateObtention());
					assertFalse(etat.isAnnule());
				}

				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void testEvenementAnnulationDoubleQuittancement() throws Exception {

		final RegDate dateDebut = date(2008, 1, 1);
		final RegDate dateFin = date(2008, 3, 31);
		final RegDate dateQuittancement = RegDate.get();

		final long dpiId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				dpi.setNom1("DebiteurTest");
				addForDebiteur(dpi, dateDebut, null, MockCommune.Lausanne);

				final PeriodeFiscale pf = addPeriodeFiscale(2008);

				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement));
				lr.addEtat(new EtatDeclarationRetournee(dateQuittancement));

				return dpi.getNumero();
			}
		});

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final String message = createMessageAnnulationQuittancement(dpiId, dateDebut, dateFin, dateQuittancement);
				try {
					listener.onMessage(message, "TEST-" + System.currentTimeMillis());
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
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

				final Set<EtatDeclaration> etats = lr.getEtats();
				assertNotNull(etats);
				assertEquals(2, etats.size());

				boolean trouveAnnule = false;
				for (EtatDeclaration etat : etats) {
					assertNotNull(etat);
					assertEquals(TypeEtatDeclaration.RETOURNEE, etat.getEtat());
					assertEquals(dateQuittancement, etat.getDateObtention());

					if (etat.isAnnule()) {
						assertFalse(trouveAnnule);
						trouveAnnule = true;
					}
				}
				assertTrue(trouveAnnule);

				final EtatDeclaration dernierEtat = lr.getDernierEtat();
				assertNotNull(dernierEtat);
				assertEquals(TypeEtatDeclaration.RETOURNEE, dernierEtat.getEtat());
				assertFalse(dernierEtat.isAnnule());

				return null;
			}
		});
	}

	private String createMessageQuittancement(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) {
		final EvtQuittanceListeDocument doc = service.createEvenementQuittancement(QuittanceType.QUITTANCEMENT, noCtb, ListeType.LR, debutPeriode, finPeriode, dateEvenement);
		return doc.xmlText();
	}
	
	private String createMessageAnnulationQuittancement(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) {
		final EvtQuittanceListeDocument doc = service.createEvenementQuittancement(QuittanceType.ANNULATION, noCtb, ListeType.LR, debutPeriode, finPeriode, dateEvenement);
		return doc.xmlText();
	}

	private String createMessageLC(long noCtb, RegDate debutPeriode, RegDate finPeriode, RegDate dateEvenement) {
		final EvtQuittanceListeDocument doc = service.createEvenementQuittancement(QuittanceType.QUITTANCEMENT, noCtb, ListeType.LC, debutPeriode, finPeriode, dateEvenement);
		return doc.xmlText();
	}
}
