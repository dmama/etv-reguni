package ch.vd.uniregctb.evenement.externe;

import java.util.Date;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

@SuppressWarnings({"JavaDoc"})
public class EvenementExterneProcessorTest extends BusinessTest {

	private EvenementExterneProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		processor = getBean(EvenementExterneProcessor.class, "evenementExterneProcessor");
	}

	/**
	 * Pas d'événement dans la base : rien à faire
	 */
	@Test
	public void testAucunTraitementSiAucunEvenementExistant() throws Exception {
		final TraiterEvenementExterneResult result = processor.traiteEvenementsExternes(RegDate.get(), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.nbEvenementTotal);
		Assert.assertNotNull(result.traites);
		Assert.assertEquals(0, result.traites.size());
		Assert.assertNotNull(result.erreurs);
		Assert.assertEquals(0, result.erreurs.size());
	}

	/**
	 * on crée un événement déjà traité... il ne devrait rien se passer au niveau du processeur
	 */
	@Test
	public void testAucunTraitementSiAucunEvenementATraiter() throws Exception {

		final int annee = 2009;
		final RegDate dateDebut = date(annee, 1, 1);
		final RegDate dateFin = date(annee, 3, 31);
		final Date quittancement = DateHelper.getCurrentDate();
		final RegDate obtentionRetour = RegDate.get(quittancement);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationRetournee(obtentionRetour, "TEST"));

				final QuittanceLR quittance = new QuittanceLR();
				quittance.setTiers(dpi);
				quittance.setBusinessId("Evénement de test");
				quittance.setType(TypeQuittance.QUITTANCEMENT);
				quittance.setDateEvenement(quittancement);
				quittance.setDateDebut(dateDebut);
				quittance.setDateFin(dateFin);
				quittance.setEtat(EtatEvenementExterne.TRAITE);
				hibernateTemplate.merge(quittance);
				return null;
			}
		});

		final TraiterEvenementExterneResult result = processor.traiteEvenementsExternes(RegDate.get(), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.nbEvenementTotal);
		Assert.assertNotNull(result.traites);
		Assert.assertEquals(0, result.traites.size());
		Assert.assertNotNull(result.erreurs);
		Assert.assertEquals(0, result.erreurs.size());
	}

	@Test
	public void testTraitementReussiEvenementEnErreur() throws Exception {

		final int annee = 2009;
		final RegDate dateDebut = date(annee, 1, 1);
		final RegDate dateFin = date(annee, 3, 31);
		final Date quittancement = RegDate.get().addDays(-3).asJavaDate();

		final class Ids {
			final long dpiId;
			final long evtId;

			Ids(long dpiId, long evtId) {
				this.dpiId = dpiId;
				this.evtId = evtId;
			}
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				addLR(dpi, dateDebut, dateFin, pf);

				final QuittanceLR quittance = new QuittanceLR();
				quittance.setTiers(dpi);
				quittance.setBusinessId("Evénement de test");
				quittance.setType(TypeQuittance.QUITTANCEMENT);
				quittance.setDateEvenement(quittancement);
				quittance.setDateDebut(dateDebut);
				quittance.setDateFin(dateFin);
				quittance.setEtat(EtatEvenementExterne.ERREUR);
				quittance.setErrorMessage("Pas pu...");
				final QuittanceLR quittanceSauvee = hibernateTemplate.merge(quittance);
				return new Ids(dpi.getNumero(), quittanceSauvee.getId());
			}
		});

		final TraiterEvenementExterneResult result = processor.traiteEvenementsExternes(RegDate.get(), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.nbEvenementTotal);
		Assert.assertNotNull(result.traites);
		Assert.assertEquals(1, result.traites.size());
		Assert.assertNotNull(result.erreurs);
		Assert.assertEquals(0, result.erreurs.size());

		final TraiterEvenementExterneResult.Traite traite = result.traites.get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(ids.dpiId, (long) traite.numeroTiers);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// la LR doit être quittancée...
				final DebiteurPrestationImposable dpi = tiersDAO.getDebiteurPrestationImposableByNumero(ids.dpiId);
				final DeclarationImpotSource lr = (DeclarationImpotSource) dpi.getDeclarationActive(dateDebut);
				final EtatDeclaration etat = lr.getDernierEtat();
				Assert.assertNotNull(etat);
				Assert.assertEquals(RegDate.get(quittancement), etat.getDateObtention());
				Assert.assertEquals(TypeEtatDeclaration.RETOURNEE, etat.getEtat());

				// et l'événement traité
				final QuittanceLR evt = hibernateTemplate.get(QuittanceLR.class, ids.evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(ids.dpiId, (long) evt.getTiersId());
				Assert.assertEquals(EtatEvenementExterne.TRAITE, evt.getEtat());
				Assert.assertNull(evt.getErrorMessage());
				return null;
			}
		});
	}

	@Test
	public void testTraitementReussiEvenementNonTraite() throws Exception {

		final int annee = 2009;
		final RegDate dateDebut = date(annee, 1, 1);
		final RegDate dateFin = date(annee, 3, 31);
		final Date quittancement = RegDate.get().addDays(-3).asJavaDate();

		final class Ids {
			final long dpiId;
			final long evtId;

			Ids(long dpiId, long evtId) {
				this.dpiId = dpiId;
				this.evtId = evtId;
			}
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				addLR(dpi, dateDebut, dateFin, pf);

				final QuittanceLR quittance = new QuittanceLR();
				quittance.setTiers(dpi);
				quittance.setBusinessId("Evénement de test");
				quittance.setType(TypeQuittance.QUITTANCEMENT);
				quittance.setDateEvenement(quittancement);
				quittance.setDateDebut(dateDebut);
				quittance.setDateFin(dateFin);
				quittance.setEtat(EtatEvenementExterne.NON_TRAITE);
				quittance.setErrorMessage("Pas encore eu le temps...");
				final QuittanceLR quittanceSauvee = hibernateTemplate.merge(quittance);
				return new Ids(dpi.getNumero(), quittanceSauvee.getId());
			}
		});

		final TraiterEvenementExterneResult result = processor.traiteEvenementsExternes(RegDate.get(), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.nbEvenementTotal);
		Assert.assertNotNull(result.traites);
		Assert.assertEquals(1, result.traites.size());
		Assert.assertNotNull(result.erreurs);
		Assert.assertEquals(0, result.erreurs.size());

		final TraiterEvenementExterneResult.Traite traite = result.traites.get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(ids.dpiId, (long) traite.numeroTiers);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// la LR doit être quittancée...
				final DebiteurPrestationImposable dpi = tiersDAO.getDebiteurPrestationImposableByNumero(ids.dpiId);
				final DeclarationImpotSource lr = (DeclarationImpotSource) dpi.getDeclarationActive(dateDebut);
				final EtatDeclaration etat = lr.getDernierEtat();
				Assert.assertNotNull(etat);
				Assert.assertEquals(RegDate.get(quittancement), etat.getDateObtention());
				Assert.assertEquals(TypeEtatDeclaration.RETOURNEE, etat.getEtat());

				// et l'événement traité
				final QuittanceLR evt = hibernateTemplate.get(QuittanceLR.class, ids.evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(ids.dpiId, (long) evt.getTiersId());
				Assert.assertEquals(EtatEvenementExterne.TRAITE, evt.getEtat());
				Assert.assertNull(evt.getErrorMessage());
				return null;
			}
		});
	}

	@Test
	public void testRelanceDoubleQuittancement() throws Exception {

		final int annee = 2009;
		final RegDate dateDebut = date(annee, 1, 1);
		final RegDate dateFin = date(annee, 3, 31);
		final Date premierQuittancement = RegDate.get().addDays(-10).asJavaDate();
		final Date quittancement = RegDate.get().addDays(-3).asJavaDate();

		final class Ids {
			final long dpiId;
			final long evtId;

			Ids(long dpiId, long evtId) {
				this.dpiId = dpiId;
				this.evtId = evtId;
			}
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final DeclarationImpotSource lr = addLR(dpi, dateDebut, dateFin, pf);
				lr.addEtat(new EtatDeclarationRetournee(RegDate.get(premierQuittancement), "TEST"));      // premier quittancement

				final QuittanceLR quittance = new QuittanceLR();
				quittance.setTiers(dpi);
				quittance.setBusinessId("Evénement de test");
				quittance.setType(TypeQuittance.QUITTANCEMENT);
				quittance.setDateEvenement(quittancement);
				quittance.setDateDebut(dateDebut);
				quittance.setDateFin(dateFin);
				quittance.setEtat(EtatEvenementExterne.ERREUR);
				quittance.setErrorMessage("Pas pu...");
				final QuittanceLR quittanceSauvee = hibernateTemplate.merge(quittance);
				return new Ids(dpi.getNumero(), quittanceSauvee.getId());
			}
		});

		final TraiterEvenementExterneResult result = processor.traiteEvenementsExternes(RegDate.get(), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.nbEvenementTotal);
		Assert.assertNotNull(result.traites);
		Assert.assertEquals(1, result.traites.size());
		Assert.assertNotNull(result.erreurs);
		Assert.assertEquals(0, result.erreurs.size());

		final TraiterEvenementExterneResult.Traite traite = result.traites.get(0);
		Assert.assertNotNull(traite);
		Assert.assertEquals(ids.dpiId, (long) traite.numeroTiers);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// l'événement doit avoir été traité
				final QuittanceLR evt = hibernateTemplate.get(QuittanceLR.class, ids.evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(ids.dpiId, (long) evt.getTiersId());
				Assert.assertEquals(EtatEvenementExterne.TRAITE, evt.getEtat());
				Assert.assertNull(evt.getErrorMessage());

				// la LR doit être quittancée
				final DebiteurPrestationImposable dpi = tiersDAO.getDebiteurPrestationImposableByNumero(ids.dpiId);
				final DeclarationImpotSource lr = (DeclarationImpotSource) dpi.getDeclarationActive(dateDebut);
				final Set<EtatDeclaration> etats = lr.getEtats();
				Assert.assertNotNull(etats);
				Assert.assertEquals(3, etats.size());       // "EMISE", 2x "RETOURNEE", dont un annulé

				// vérification que l'état "RETOURNEE" pré-existant a bien été annulé
				boolean etatRetourneAnnuleTrouve = false;
				for (EtatDeclaration etat : etats) {
					if (etat.isAnnule() && etat.getEtat() == TypeEtatDeclaration.RETOURNEE) {
						etatRetourneAnnuleTrouve = true;
						Assert.assertEquals(RegDate.get(premierQuittancement), etat.getDateObtention());
					}
				}
				Assert.assertTrue(etatRetourneAnnuleTrouve);

				// test de l'état final de la déclaration après traitement de l'événement
				final EtatDeclaration etat = lr.getDernierEtat();
				Assert.assertNotNull(etat);
				Assert.assertEquals(RegDate.get(quittancement), etat.getDateObtention());       // la date de quittancement a été changée
				Assert.assertEquals(TypeEtatDeclaration.RETOURNEE, etat.getEtat());

				return null;
			}
		});
	}

	@Test
	public void testRelanceAnnulationQuittancementSansRetourPrealable() throws Exception {

		final int annee = 2009;
		final RegDate dateDebut = date(annee, 1, 1);
		final RegDate dateFin = date(annee, 3, 31);

		final class Ids {
			final long dpiId;
			final long evtId;

			Ids(long dpiId, long evtId) {
				this.dpiId = dpiId;
				this.evtId = evtId;
			}
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				addLR(dpi, dateDebut, dateFin, pf);

				final QuittanceLR quittance = new QuittanceLR();
				quittance.setTiers(dpi);
				quittance.setBusinessId("Evénement de test");
				quittance.setType(TypeQuittance.ANNULATION);
				quittance.setDateEvenement(DateHelper.getCurrentDate());
				quittance.setDateDebut(dateDebut);
				quittance.setDateFin(dateFin);
				quittance.setEtat(EtatEvenementExterne.NON_TRAITE);
				quittance.setErrorMessage("Pas eu le temps...");
				final QuittanceLR quittanceSauvee = hibernateTemplate.merge(quittance);
				return new Ids(dpi.getNumero(), quittanceSauvee.getId());
			}
		});

		final TraiterEvenementExterneResult result = processor.traiteEvenementsExternes(RegDate.get(), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.nbEvenementTotal);
		Assert.assertNotNull(result.traites);
		Assert.assertEquals(0, result.traites.size());
		Assert.assertNotNull(result.erreurs);
		Assert.assertEquals(1, result.erreurs.size());

		final TraiterEvenementExterneResult.Erreur erreur = result.erreurs.get(0);
		Assert.assertNotNull(erreur);
		Assert.assertEquals(ids.evtId, (long) erreur.evenementId);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// l'événement ne doit pas avoir été traité
				final QuittanceLR evt = hibernateTemplate.get(QuittanceLR.class, ids.evtId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(ids.dpiId, (long) evt.getTiersId());
				Assert.assertEquals(EtatEvenementExterne.ERREUR, evt.getEtat());

				final String erreurAttendue = String.format("La déclaration impôt source sélectionnée (tiers=%d, période=%s) ne contient pas de retour à annuler.", ids.dpiId, DateRangeHelper.toDisplayString(new DateRangeHelper.Range(dateDebut, dateFin)));
				Assert.assertEquals(erreurAttendue, evt.getErrorMessage());

				// la LR ne doit toujours pas être quittancée
				final DebiteurPrestationImposable dpi = tiersDAO.getDebiteurPrestationImposableByNumero(ids.dpiId);
				final DeclarationImpotSource lr = (DeclarationImpotSource) dpi.getDeclarationActive(dateDebut);
				final Set<EtatDeclaration> etats = lr.getEtats();
				Assert.assertNotNull(etats);
				Assert.assertEquals(1, etats.size());       // "EMISE"

				// test de l'état final de la déclaration après non-traitement de l'événement
				final EtatDeclaration etat = lr.getDernierEtat();
				Assert.assertNotNull(etat);
				Assert.assertEquals(dateFin, etat.getDateObtention());
				Assert.assertEquals(TypeEtatDeclaration.EMISE, etat.getEtat());

				return null;
			}
		});
	}
}
