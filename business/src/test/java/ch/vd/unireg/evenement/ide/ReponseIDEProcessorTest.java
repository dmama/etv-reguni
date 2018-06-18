package ch.vd.unireg.evenement.ide;

import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEData;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.NumeroIDE;
import ch.vd.unireg.interfaces.organisation.data.StatutAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeEtablissementCivil;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.IdentificationEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Raphaël Marmier, 2016-10-07, <raphael.marmier@vd.ch>
 */
public class ReponseIDEProcessorTest extends BusinessTest {

	private ReponseIDEProcessor reponseIDEProcessor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		reponseIDEProcessor  = getBean(ReponseIDEProcessor.class, "reponseIDEProcessor");

	}

	@Test
	public void testNouvelleEntrepriseTransmisIDE() throws Exception {
		/*
			Une nouvelle entité vient d'être créée dans Unireg. Une annonce de création est partie vers le registre IDE, et on reçoit la quittance de transmission avec
			un identifiant IDE provisoire.
		 */

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseInconnueAuCivil();

				addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 5), null, "Syntruc Asso");
				addFormeJuridique(entreprise, date(2016, 9, 5), null, FormeJuridiqueEntreprise.ASSOCIATION);

				entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

				final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.DOMICILE, date(2016, 9, 5), null, MockRue.Renens.QuatorzeAvril);
				adresseSuisse.setNumeroMaison("1");

				return entreprise.getNumero();
			}
		});

		// Création de l'établissement
		final Long noEtablissement = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

				Etablissement etablissement = addEtablissement();
				addDomicileEtablissement(etablissement, date(2016, 9, 5), null, MockCommune.Renens);
				addActiviteEconomique(entreprise, etablissement, date(2016, 9, 5), null, true);

				return etablissement.getNumero();
			}
		});

		// Ajout de la référence d'annonce
		final Long idReferenceAnnonce = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				Etablissement etablissement = (Etablissement) tiersDAO.get(noEtablissement);

				final ReferenceAnnonceIDE refAnnonce = addReferenceAnnonceIDE("test_business_id", etablissement);

				return refAnnonce.getId();
			}
		});

		// Validation
		final AnnonceIDE annonce =
				RCEntAnnonceIDEHelper.createAnnonceIDE(idReferenceAnnonce, TypeAnnonce.CREATION, DateHelper.getDateTime(2016, 9, 5, 11, 0, 0), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, null, null,
				                                       new NumeroIDE("CHE111111114"), null, null, null, null, null,
				                                       "Syntruc Asso", null, FormeLegale.N_0109_ASSOCIATION, "Fabrication d'objets synthétiques",
				                                       RCEntAnnonceIDEHelper
						                                       .createAdresseAnnonceIDERCEnt(MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), "1", null, MockLocalite.Renens.getNPA(), null, MockLocalite.Renens.getNoOrdre(), MockLocalite.Renens.getNom(), MockPays.Suisse.getNoOfsEtatSouverain(),
						                                                                     MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(), null,
						                                                                     null, null),
				                                       new AnnonceIDEData.StatutImpl(StatutAnnonce.TRANSMIS, DateHelper.getDateTime(2016, 9, 5, 12, 0, 0), null), RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);

		// Traiter la quittance en réponse
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				try {
					reponseIDEProcessor.traiterReponseAnnonceIDE(annonce);
				}
				catch (ReponseIDEProcessorException e) {
					fail(String.format("Le traitement de l'annonce a rencontré un problème inattendu: %s", e.getMessage()));
				}
			}

		});

		// Vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);
				final Set<IdentificationEntreprise> listIdentEntreprise = entreprise.getIdentificationsEntreprise();
				assertNotNull(listIdentEntreprise);
				assertEquals("CHE111111114", listIdentEntreprise.iterator().next().getNumeroIde());

				final Etablissement etablissementPrincipal = tiersService.getEtablissementPrincipal(entreprise, date(2016, 9, 5));
				final Set<IdentificationEntreprise> listIdentEtablissementPrincipal = etablissementPrincipal.getIdentificationsEntreprise();
				assertNotNull(listIdentEtablissementPrincipal);
				assertEquals("CHE111111114", listIdentEtablissementPrincipal.iterator().next().getNumeroIde());
			}
		});
	}

	@Test
	public void testNouvelleEntrepriseAccepteeIDE() throws Exception {
		/*
			Une nouvelle entité vient d'être créée dans Unireg. Une annonce de création est partie vers le registre IDE, et on reçoit la quittance, positive.
		 */

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseInconnueAuCivil();

				addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 5), null, "Syntruc Asso");
				addFormeJuridique(entreprise, date(2016, 9, 5), null, FormeJuridiqueEntreprise.ASSOCIATION);

				entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

				final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.DOMICILE, date(2016, 9, 5), null, MockRue.Renens.QuatorzeAvril);
				adresseSuisse.setNumeroMaison("1");

				return entreprise.getNumero();
			}
		});

		// Création de l'établissement
		final Long noEtablissement = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

				Etablissement etablissement = addEtablissement();
				addDomicileEtablissement(etablissement, date(2016, 9, 5), null, MockCommune.Renens);
				addActiviteEconomique(entreprise, etablissement, date(2016, 9, 5), null, true);

				return etablissement.getNumero();
			}
		});

		// Ajout de la référence d'annonce
		final Long idReferenceAnnonce = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				Etablissement etablissement = (Etablissement) tiersDAO.get(noEtablissement);

				final ReferenceAnnonceIDE refAnnonce = addReferenceAnnonceIDE("test_business_id", etablissement);

				return refAnnonce.getId();
			}
		});

		// Validation
		final AnnonceIDE annonce =
				RCEntAnnonceIDEHelper.createAnnonceIDE(idReferenceAnnonce, TypeAnnonce.CREATION, DateHelper.getDateTime(2016, 9, 5, 11, 0, 0), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, null, null,
				                                       new NumeroIDE("CHE111111114"), null, null, null, null, null,
				                                       "Syntruc Asso", null, FormeLegale.N_0109_ASSOCIATION, "Fabrication d'objets synthétiques",
				                                       RCEntAnnonceIDEHelper
						                                       .createAdresseAnnonceIDERCEnt(MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), "1", null, MockLocalite.Renens.getNPA(), null, MockLocalite.Renens.getNoOrdre(), MockLocalite.Renens.getNom(), MockPays.Suisse.getNoOfsEtatSouverain(),
						                                                                     MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(), null,
						                                                                     null, null),
				                                       new AnnonceIDEData.StatutImpl(StatutAnnonce.ACCEPTE_IDE, DateHelper.getDateTime(2016, 9, 5, 12, 0, 0), null), RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);

		// Traiter la quittance en réponse
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				try {
					reponseIDEProcessor.traiterReponseAnnonceIDE(annonce);
				}
				catch (ReponseIDEProcessorException e) {
					fail(String.format("Le traitement de l'annonce a rencontré un problème inattendu: %s", e.getMessage()));
				}
			}

		});

		// Vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);
				final Set<IdentificationEntreprise> listIdentEntreprise = entreprise.getIdentificationsEntreprise();
				assertNotNull(listIdentEntreprise);
				assertEquals("CHE111111114", listIdentEntreprise.iterator().next().getNumeroIde());

				final Etablissement etablissementPrincipal = tiersService.getEtablissementPrincipal(entreprise, date(2016, 9, 5));
				final Set<IdentificationEntreprise> listIdentEtablissementPrincipal = etablissementPrincipal.getIdentificationsEntreprise();
				assertNotNull(listIdentEtablissementPrincipal);
				assertEquals("CHE111111114", listIdentEtablissementPrincipal.iterator().next().getNumeroIde());
			}
		});
	}

	@Test
	public void testNouvelleEntrepriseRefuseeIDEDoublon() throws Exception {
		/*
			Une nouvelle entité vient d'être créée dans Unireg. Une annonce de création est partie vers le registre IDE, et on reçoit la quittance, négative avec doublon.
		 */

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseInconnueAuCivil();

				addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 5), null, "Syntruc Asso");
				addFormeJuridique(entreprise, date(2016, 9, 5), null, FormeJuridiqueEntreprise.ASSOCIATION);

				entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

				final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.DOMICILE, date(2016, 9, 5), null, MockRue.Renens.QuatorzeAvril);
				adresseSuisse.setNumeroMaison("1");

				return entreprise.getNumero();
			}
		});

		// Création de l'établissement
		final Long noEtablissement = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

				Etablissement etablissement = addEtablissement();
				addDomicileEtablissement(etablissement, date(2016, 9, 5), null, MockCommune.Renens);
				addActiviteEconomique(entreprise, etablissement, date(2016, 9, 5), null, true);

				return etablissement.getNumero();
			}
		});

		// Ajout de la référence d'annonce
		final Long idReferenceAnnonce = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				Etablissement etablissement = (Etablissement) tiersDAO.get(noEtablissement);

				final ReferenceAnnonceIDE refAnnonce = addReferenceAnnonceIDE("test_business_id", etablissement);

				return refAnnonce.getId();
			}
		});

		// Validation
		final AnnonceIDE annonce =
				RCEntAnnonceIDEHelper.createAnnonceIDE(idReferenceAnnonce, TypeAnnonce.CREATION, DateHelper.getDateTime(2016, 9, 5, 11, 0, 0), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, null, null,
				                                       new NumeroIDE("CHE111111114"), new NumeroIDE("CHE222222224"), null, null, null, null,
				                                       "Syntruc Asso", null, FormeLegale.N_0109_ASSOCIATION, "Fabrication d'objets synthétiques",
				                                       RCEntAnnonceIDEHelper
						                                       .createAdresseAnnonceIDERCEnt(MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), "1", null, MockLocalite.Renens.getNPA(), null, MockLocalite.Renens.getNoOrdre(), MockLocalite.Renens.getNom(), MockPays.Suisse.getNoOfsEtatSouverain(),
						                                                                     MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(), null,
						                                                                     null, null),
				                                       new AnnonceIDEData.StatutImpl(StatutAnnonce.REFUSE_IDE, DateHelper.getDateTime(2016, 9, 5, 12, 0, 0), null), RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);

		// Traiter la quittance en réponse
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				try {
					reponseIDEProcessor.traiterReponseAnnonceIDE(annonce);
				}
				catch (ReponseIDEProcessorException e) {
					fail(String.format("Le traitement de l'annonce a rencontré un problème inattendu: %s", e.getMessage()));
				}
			}

		});

		// Vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);
				final Set<IdentificationEntreprise> listIdentEntreprise = entreprise.getIdentificationsEntreprise();
				assertNotNull(listIdentEntreprise);
				assertFalse(listIdentEntreprise.iterator().hasNext());

				final Etablissement etablissementPrincipal = tiersService.getEtablissementPrincipal(entreprise, date(2016, 9, 5));
				final Set<IdentificationEntreprise> listIdentEtablissementPrincipal = etablissementPrincipal.getIdentificationsEntreprise();
				assertNotNull(listIdentEtablissementPrincipal);
				assertFalse(listIdentEtablissementPrincipal.iterator().hasNext());
			}
		});

	}
}