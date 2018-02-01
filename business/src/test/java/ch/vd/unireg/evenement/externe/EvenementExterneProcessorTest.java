package ch.vd.unireg.evenement.externe;

import java.util.Date;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

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
		final RegDate obtentionRetour = RegDateHelper.get(quittancement);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				addForDebiteur(dpi, dateDebut, MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Lausanne);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final DeclarationImpotSource lr = addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);
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
				addForDebiteur(dpi, dateDebut, MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Lausanne);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);

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
				final DeclarationImpotSource lr = dpi.getDerniereDeclaration(DeclarationImpotSource.class);
				Assert.assertNotNull(lr);
				Assert.assertEquals(dateDebut, lr.getDateDebut());
				final EtatDeclaration etat = lr.getDernierEtatDeclaration();
				Assert.assertNotNull(etat);
				Assert.assertEquals(RegDateHelper.get(quittancement), etat.getDateObtention());
				Assert.assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat.getEtat());

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
				addForDebiteur(dpi, dateDebut, MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Lausanne);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);

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
				final DeclarationImpotSource lr = dpi.getDerniereDeclaration(DeclarationImpotSource.class);
				Assert.assertNotNull(lr);
				Assert.assertEquals(dateDebut, lr.getDateDebut());
				final EtatDeclaration etat = lr.getDernierEtatDeclaration();
				Assert.assertNotNull(etat);
				Assert.assertEquals(RegDateHelper.get(quittancement), etat.getDateObtention());
				Assert.assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat.getEtat());

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
				addForDebiteur(dpi, dateDebut, MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Lausanne);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final DeclarationImpotSource lr = addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);
				lr.addEtat(new EtatDeclarationRetournee(RegDateHelper.get(premierQuittancement), "TEST"));      // premier quittancement

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
				final DeclarationImpotSource lr = dpi.getDerniereDeclaration(DeclarationImpotSource.class);
				Assert.assertNotNull(lr);
				Assert.assertEquals(dateDebut, lr.getDateDebut());
				final Set<EtatDeclaration> etats = lr.getEtatsDeclaration();
				Assert.assertNotNull(etats);
				Assert.assertEquals(3, etats.size());       // "EMIS", 2x "RETOURNE", dont un annulé

				// vérification que l'état "RETOURNE" pré-existant a bien été annulé
				boolean etatRetourneAnnuleTrouve = false;
				for (EtatDeclaration etat : etats) {
					if (etat.isAnnule() && etat.getEtat() == TypeEtatDocumentFiscal.RETOURNE) {
						etatRetourneAnnuleTrouve = true;
						Assert.assertEquals(RegDateHelper.get(premierQuittancement), etat.getDateObtention());
					}
				}
				Assert.assertTrue(etatRetourneAnnuleTrouve);

				// test de l'état final de la déclaration après traitement de l'événement
				final EtatDeclaration etat = lr.getDernierEtatDeclaration();
				Assert.assertNotNull(etat);
				Assert.assertEquals(RegDateHelper.get(quittancement), etat.getDateObtention());       // la date de quittancement a été changée
				Assert.assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat.getEtat());

				return null;
			}
		});
	}

	//SIFISC-15261
	@Test
	public void testRelance2Quittancements2AnnulationsEt1QuittancementFinal() throws Exception {

		final int annee = 2015;
		final RegDate dateDebut = date(annee, 1, 1);
		final RegDate dateFin = date(annee, 3, 31);
		final Date premierQuittancement = RegDate.get().addDays(-10).asJavaDate();
		final Date quittancement = RegDate.get().addDays(-3).asJavaDate();

		final class Ids {
			final long dpiId;
			final long evtQ1Id;
			final long evtQ2Id;
			final long evtQ3Id;
			final long evtA1Id;
			final long evtA2Id;

			Ids(long dpiId, long evtQ1Id,long evtQ2Id,long evtQ3Id,long evtA1Id,long evtA2Id) {
				this.dpiId = dpiId;
				this.evtQ1Id = evtQ1Id;
				this.evtQ2Id = evtQ2Id;
				this.evtQ3Id = evtQ3Id;
				this.evtA1Id = evtA1Id;
				this.evtA2Id = evtA2Id;
			}
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.TRIMESTRIEL, dateDebut);
				addForDebiteur(dpi, dateDebut, MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Lausanne);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final DeclarationImpotSource lr = addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);

					//Première quittance/annulation
					final QuittanceLR quittance1 = new QuittanceLR();
					quittance1.setTiers(dpi);
					quittance1.setBusinessId("Evénement de test");
					quittance1.setType(TypeQuittance.QUITTANCEMENT);
					quittance1.setDateEvenement(quittancement);
					quittance1.setDateDebut(dateDebut);
					quittance1.setDateFin(dateFin);
					quittance1.setEtat(EtatEvenementExterne.ERREUR);
					quittance1.setErrorMessage("Pas pu...");
					final QuittanceLR quittanceSauvee1 = hibernateTemplate.merge(quittance1);

					final QuittanceLR annulation1 = new QuittanceLR();
					annulation1.setTiers(dpi);
					annulation1.setBusinessId("Evénement de test");
					annulation1.setType(TypeQuittance.ANNULATION);
					annulation1.setDateEvenement(quittancement);
					annulation1.setDateDebut(dateDebut);
					annulation1.setDateFin(dateFin);
					annulation1.setEtat(EtatEvenementExterne.ERREUR);
					annulation1.setErrorMessage("Pas pu...");
					final QuittanceLR annulationSauvee1 = hibernateTemplate.merge(annulation1);


					//Seconde quittance/annulation
					final QuittanceLR quittance2 = new QuittanceLR();
					quittance2.setTiers(dpi);
					quittance2.setBusinessId("Evénement de test");
					quittance2.setType(TypeQuittance.QUITTANCEMENT);
					quittance2.setDateEvenement(quittancement);
					quittance2.setDateDebut(dateDebut);
					quittance2.setDateFin(dateFin);
					quittance2.setEtat(EtatEvenementExterne.ERREUR);
					quittance2.setErrorMessage("Pas pu...");
					final QuittanceLR quittanceSauvee2 = hibernateTemplate.merge(quittance2);

					final QuittanceLR annulation2 = new QuittanceLR();
					annulation2.setTiers(dpi);
					annulation2.setBusinessId("Evénement de test");
					annulation2.setType(TypeQuittance.ANNULATION);
					annulation2.setDateEvenement(quittancement);
					annulation2.setDateDebut(dateDebut);
					annulation2.setDateFin(dateFin);
					annulation2.setEtat(EtatEvenementExterne.ERREUR);
					annulation2.setErrorMessage("Pas pu...");
					final QuittanceLR annulationSauvee2 = hibernateTemplate.merge(annulation2);


					//Dernière quittance
					final QuittanceLR quittance3 = new QuittanceLR();
					quittance3.setTiers(dpi);
					quittance3.setBusinessId("Evénement de test");
					quittance3.setType(TypeQuittance.QUITTANCEMENT);
					quittance3.setDateEvenement(quittancement);
					quittance3.setDateDebut(dateDebut);
					quittance3.setDateFin(dateFin);
					quittance3.setEtat(EtatEvenementExterne.ERREUR);
					quittance3.setErrorMessage("Pas pu...");
					final QuittanceLR quittanceSauvee3 = hibernateTemplate.merge(quittance3);

				return new Ids(dpi.getNumero(), quittanceSauvee1.getId(),quittanceSauvee2.getId(),quittanceSauvee3.getId(),annulationSauvee1.getId(),annulationSauvee2.getId());
			}
		});

		final TraiterEvenementExterneResult result = processor.traiteEvenementsExternes(RegDate.get(), 1, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(5, result.nbEvenementTotal);
		//Assert.assertNotNull(result.traites);
		//Assert.assertEquals(5, result.traites.size());
		//Assert.assertNotNull(result.erreurs);
		//Assert.assertEquals(0, result.erreurs.size());

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {


				// la LR doit être quittancée
				final DebiteurPrestationImposable dpi = tiersDAO.getDebiteurPrestationImposableByNumero(ids.dpiId);
				final DeclarationImpotSource lr = dpi.getDerniereDeclaration(DeclarationImpotSource.class);
				Assert.assertNotNull(lr);
				Assert.assertEquals(dateDebut, lr.getDateDebut());
				final Set<EtatDeclaration> etats = lr.getEtatsDeclaration();
				Assert.assertNotNull(etats);
				Assert.assertEquals(4, etats.size());       // "EMIS", 2x "RETOURNE et annulée", 1 RETOURNE

				// vérification que l'état "RETOURNE" pré-existant a bien été annulé
				boolean etatRetourneAnnuleTrouve = false;
				int nombreEmise = 0;
				int nombreRetourneeAnnule = 0;
				int nombreRetournee = 0;
				for (EtatDeclaration etat : etats) {
					if (etat.isAnnule() && etat.getEtat() == TypeEtatDocumentFiscal.RETOURNE) {
						nombreRetourneeAnnule++;
					}
					if (!etat.isAnnule() && etat.getEtat() == TypeEtatDocumentFiscal.EMIS) {
						nombreEmise++;
					}
					if (!etat.isAnnule() && etat.getEtat() == TypeEtatDocumentFiscal.RETOURNE) {
						nombreRetournee++;
					}

				}

				Assert.assertEquals(1,nombreEmise);
				Assert.assertEquals(2, nombreRetourneeAnnule);
				Assert.assertEquals(1,nombreRetournee);

				// test de l'état final de la déclaration après traitement de tous ces événements
				final EtatDeclaration etat = lr.getDernierEtatDeclaration();
				Assert.assertNotNull(etat);
				Assert.assertEquals(RegDateHelper.get(quittancement), etat.getDateObtention());       // la date de quittancement a été changée
				Assert.assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat.getEtat());

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
				addForDebiteur(dpi, dateDebut, MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Lausanne);
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				addLR(dpi, dateDebut, PeriodiciteDecompte.TRIMESTRIEL, pf);

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
				final DeclarationImpotSource lr = dpi.getDerniereDeclaration(DeclarationImpotSource.class);
				Assert.assertNotNull(lr);
				Assert.assertEquals(dateDebut, lr.getDateDebut());
				final Set<EtatDeclaration> etats = lr.getEtatsDeclaration();
				Assert.assertNotNull(etats);
				Assert.assertEquals(1, etats.size());       // "EMIS"

				// test de l'état final de la déclaration après non-traitement de l'événement
				final EtatDeclaration etat = lr.getDernierEtatDeclaration();
				Assert.assertNotNull(etat);
				Assert.assertEquals(dateFin, etat.getDateObtention());
				Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, etat.getEtat());

				return null;
			}
		});
	}
}
