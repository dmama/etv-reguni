package ch.vd.unireg.evenement.ide.service;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.unireg.evenement.ide.ServiceIDEException;
import ch.vd.unireg.evenement.ide.SingleShotMockAnnonceIDESender;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.organisation.data.AdresseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AdresseAnnonceIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEData;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.NumeroIDE;
import ch.vd.unireg.interfaces.organisation.data.ProtoAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.StatutAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeEtablissementCivil;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.TypeAdresseTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Raphaël Marmier, 2016-09-14, <raphael.marmier@vd.ch>
 */
public class ServiceIDECreationEntrepriseTest extends AbstractServiceIDEServiceTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}

	@Test
	public void testCreationSimple() throws Exception {
		/*
			Une nouvelle entité vient d'être créée dans Unireg. Une annonce de création doit partir vers le registre IDE.
		 */

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseInconnueAuCivil();
				Etablissement etablissement = addEtablissement();

				addActiviteEconomique(entreprise, etablissement, date(2016, 9, 5), null, true);

				addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 5), null, "Syntruc Asso");
				addDomicileEtablissement(etablissement, date(2016, 9, 5), null, MockCommune.Renens);
				addFormeJuridique(entreprise, date(2016, 9, 5), null, FormeJuridiqueEntreprise.ASSOCIATION);

				entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

				final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, date(2016, 9, 5), null, MockRue.Renens.QuatorzeAvril);
				adresseSuisse.setNumeroMaison("1");

				return entreprise.getNumero();
			}
		});

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {

				// Annonce existante

				// Validation
				ProtoAnnonceIDE proto =
						RCEntAnnonceIDEHelper.createProtoAnnonceIDE(TypeAnnonce.CREATION, DateHelper.getDateTime(2016, 9, 5, 11, 0, 0), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, null, null,
						                                            null, null, null, null, null, null,
						                                            "Syntruc Asso", null, FormeLegale.N_0109_ASSOCIATION, "Fabrication d'objets synthétiques",
						                                            RCEntAnnonceIDEHelper
								                                                  .createAdresseAnnonceIDERCEnt(MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), "1", null, MockLocalite.Renens.getNPA(), null, MockLocalite.Renens.getNoOrdre(), MockLocalite.Renens.getNom(), MockPays.Suisse.getNoOfsEtatSouverain(),
								                                                                                MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(), null,
								                                                                                null, null), null, RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);

				AnnonceIDEEnvoyee.Statut statut = new AnnonceIDEData.StatutImpl(StatutAnnonce.VALIDATION_SANS_ERREUR, DateHelper.getDateTime(2016, 9, 5, 11, 0, 1), new ArrayList<>());
				this.addStatutAnnonceIDEAttentu(proto, statut);

			}
		});

		// Exécute la synchronisation IDE
		final SingleShotMockAnnonceIDESender annonceIDESender = new SingleShotMockAnnonceIDESender();
		annonceIDEService.setAnnonceIDESender(annonceIDESender);
		final AnnonceIDEEnvoyee annonceIDE = doInNewTransactionAndSession(new TransactionCallback<AnnonceIDEEnvoyee>() {
			@Override
			public AnnonceIDEEnvoyee doInTransaction(TransactionStatus transactionStatus) {
				try {
					return (AnnonceIDEEnvoyee) serviceIDE.synchroniseIDE((Entreprise) tiersDAO.get(noEntreprise));
				} catch (Exception e) {
					fail(String.format("Le service IDE a rencontré un problème lors de la synchronisation IDE: %s", e.getMessage()));
					return null;
				}
			}
		});

		// Vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

				assertNotNull(annonceIDE.getNumero());
				final ReferenceAnnonceIDE referenceAnnonceIDE = referenceAnnonceIDEDAO.get(annonceIDE.getNumero());
				Assert.assertNotNull(referenceAnnonceIDE);

				assertNotNull(annonceIDESender.getMsgBusinessIdUtilisee());
				assertTrue(annonceIDESender.getMsgBusinessIdUtilisee().startsWith("unireg-req-" + referenceAnnonceIDE.getId().toString()));

				assertEquals(TypeAnnonce.CREATION, annonceIDE.getType());
				assertEquals(TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, annonceIDE.getTypeEtablissementCivil());

				assertNull(annonceIDE.getNoIde());
				assertNull(annonceIDE.getNoIdeRemplacant());
				assertNull(annonceIDE.getNoIdeEtablissementPrincipal());

				assertNull(annonceIDE.getRaisonDeRadiation());

				assertEquals("Généré automatiquement suite à la mise à jour des données civiles du contribuable.", annonceIDE.getCommentaire());

				final BaseAnnonceIDE.Statut statut = annonceIDE.getStatut();
				assertNull(statut);

				final BaseAnnonceIDE.InfoServiceIDEObligEtendues infoServiceIDEObligEtendues = annonceIDE.getInfoServiceIDEObligEtendues();
				assertNotNull(infoServiceIDEObligEtendues);
				assertEquals(RCEntAnnonceIDEHelper.NO_IDE_ADMINISTRATION_CANTONALE_DES_IMPOTS, infoServiceIDEObligEtendues.getNoIdeServiceIDEObligEtendues());
				assertEquals(RCEntAnnonceIDEHelper.NO_APPLICATION_UNIREG, infoServiceIDEObligEtendues.getApplicationId());
				assertEquals(RCEntAnnonceIDEHelper.NOM_APPLICATION_UNIREG, infoServiceIDEObligEtendues.getApplicationName());

				final BaseAnnonceIDE.InformationOrganisation informationOrganisation = annonceIDE.getInformationOrganisation();
				assertNotNull(informationOrganisation);
				assertNull(informationOrganisation.getNumeroOrganisation());
				assertNull(informationOrganisation.getNumeroEtablissement());
				assertNull(informationOrganisation.getNumeroEtablissementRemplacant());

				final BaseAnnonceIDE.Utilisateur utilisateur = annonceIDE.getUtilisateur();
				assertNotNull(utilisateur);
				assertEquals(RCEntAnnonceIDEHelper.UNIREG_USER, utilisateur.getUserId());
				assertNull(utilisateur.getTelephone());

				final BaseAnnonceIDE.Contenu contenu = annonceIDE.getContenu();
				assertNotNull(contenu);
				assertEquals("Syntruc Asso", contenu.getNom());
				assertNull(contenu.getNomAdditionnel());
				assertEquals(FormeLegale.N_0109_ASSOCIATION, contenu.getFormeLegale());
				assertEquals("Fabrication d'objets synthétiques", contenu.getSecteurActivite());

				final AdresseAnnonceIDE adresse = contenu.getAdresse();
				assertNotNull(adresse);
				assertEquals(MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), adresse.getRue());
				assertEquals("1", adresse.getNumero());
				assertNull(adresse.getNumeroAppartement());
				assertEquals(MockLocalite.Renens.getNPA().intValue(), adresse.getNpa().intValue());
				assertEquals(MockLocalite.Renens.getNoOrdre().intValue(), adresse.getNumeroOrdrePostal().intValue());
				assertEquals(MockLocalite.Renens.getNom(), adresse.getVille());

				assertNull(adresse.getNumeroCasePostale());
				assertNull(adresse.getTexteCasePostale());

				final AdresseAnnonceIDE.Pays pays = adresse.getPays();
				assertNotNull(pays);
				assertEquals(MockPays.Suisse.getNoOFS(), pays.getNoOfs().intValue());
				assertEquals(MockPays.Suisse.getCodeIso2(), pays.getCodeISO2());
				assertEquals(MockPays.Suisse.getNomCourt(), pays.getNomCourt());
			}
		});
	}

	@Test
	public void testCreationModificationSimple() throws Exception {
		/*
			Une nouvelle entité créée hier dans Unireg vient d'être modifiée alors que sa création est provisoire à l'IDE.
			On envoie une mutation vers le registre IDE. (A voir si RCEnt est d'accord, avec dans le cas contraire une erreur lors de la validation qui sera remontée.)
		 */

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseInconnueAuCivil();

				addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 5), date(2016, 9, 5), "Syntruc Asso");
				addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 6), null, "Rienavoir Asso"); // Changement de nom, il y avait une erreur.
				addFormeJuridique(entreprise, date(2016, 9, 5), null, FormeJuridiqueEntreprise.ASSOCIATION);

				addIdentificationEntreprise(entreprise, "CHE999999996");

				entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

				final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, date(2016, 9, 5), null, MockRue.Renens.QuatorzeAvril);
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

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {

				final AdresseAnnonceIDERCEnt adresse = RCEntAnnonceIDEHelper
						.createAdresseAnnonceIDERCEnt(MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), "1", null, MockLocalite.Renens.getNPA(), null, MockLocalite.Renens.getNoOrdre(), MockLocalite.Renens.getNom(),
						                              MockPays.Suisse.getNoOfsEtatSouverain(), MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(),
						                              null, null, null);

				// Annonce existante
				AnnonceIDE annonce =
						RCEntAnnonceIDEHelper.createAnnonceIDE(idReferenceAnnonce, TypeAnnonce.CREATION, DateHelper.getDateTime(2016, 9, 5, 11, 0, 0), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, null, null,
						                                       new NumeroIDE("CHE999999996"), null, null, null, null, null,
						                                       "Syntruc Asso", null, FormeLegale.N_0109_ASSOCIATION, "Fabrication d'objets synthétiques",
						                                       adresse, null, RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);
				this.addAnnonceIDE(annonce, RCEntAnnonceIDEHelper.UNIREG_USER);

				// Validation
				ProtoAnnonceIDE proto =
						RCEntAnnonceIDEHelper.createProtoAnnonceIDE(TypeAnnonce.MUTATION, DateHelper.getDateTime(2016, 9, 6, 11, 0, 0), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, null, null,
						                                            new NumeroIDE("CHE999999996"), null, null, null, null, null,
						                                            "Rienavoir Asso", null, FormeLegale.N_0109_ASSOCIATION, "Fabrication d'objets synthétiques",
						                                            adresse, null, RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);

				AnnonceIDEEnvoyee.Statut statut = new AnnonceIDEData.StatutImpl(StatutAnnonce.VALIDATION_SANS_ERREUR, DateHelper.getDateTime(2016, 9, 6, 11, 0, 1), new ArrayList<>());
				this.addStatutAnnonceIDEAttentu(proto, statut);
			}
		});

		// Exécute la synchronisation IDE
		final SingleShotMockAnnonceIDESender annonceIDESender = new SingleShotMockAnnonceIDESender();
		annonceIDEService.setAnnonceIDESender(annonceIDESender);
		final AnnonceIDEEnvoyee annonceIDE = doInNewTransactionAndSession(new TransactionCallback<AnnonceIDEEnvoyee>() {
			@Override
			public AnnonceIDEEnvoyee doInTransaction(TransactionStatus transactionStatus) {
				try {
					return (AnnonceIDEEnvoyee) serviceIDE.synchroniseIDE((Entreprise) tiersDAO.get(noEntreprise));
				} catch (Exception e) {
					String expectedMessage = String.format("L'entreprise n°%s non appariée a été annoncée au registre IDE mais cette annonce n'a pas encore été traitée: impossible d'évaluer la situation du tiers pour l'instant.",
					                                       FormatNumeroHelper.numeroCTBToDisplay(noEntreprise));
					if (e instanceof ServiceIDEException) {
						Assert.assertEquals(expectedMessage, e.getMessage());
						return null;
					}
					fail(String.format("Une erreur non attendue est survenue signifiant l'échec du test: %s", e.getMessage()));
					return null;
				}
			}
		});
	}

}