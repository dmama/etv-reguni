package ch.vd.unireg.evenement.ide.service;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.unireg.evenement.ide.SingleShotMockAnnonceIDESender;
import ch.vd.unireg.interfaces.entreprise.data.AdresseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEData;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.NumeroIDE;
import ch.vd.unireg.interfaces.entreprise.data.ProtoAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.StatutAnnonce;
import ch.vd.unireg.interfaces.entreprise.data.TypeAnnonce;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.entreprise.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Raphaël Marmier, 2016-09-14, <raphael.marmier@vd.ch>
 */
public class ServiceIDEModificationEntrepriseTest extends AbstractServiceIDEServiceTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}

	@Test
	public void testCreationSimple() throws Exception {
		/*
			Une entité existante et appariée vient d'être modifiée dans Unireg. Une annonce de mutation doit partir vers le registre IDE.
		 */

		final Long noEntrepriseCivile = 1111L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2016, 9, 5), null, true);

			addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 5), null, "Syntruc Asso");
			addDomicileEtablissement(etablissement, date(2016, 9, 5), null, MockCommune.Renens);
			addFormeJuridique(entreprise, date(2016, 9, 5), null, FormeJuridiqueEntreprise.ASSOCIATION);

			//entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

			final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, date(2016, 9, 5), null, MockRue.Renens.QuatorzeAvril);
			adresseSuisse.setNumeroMaison("1");
			return entreprise.getNumero();
		});

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {

				// l'association existante
				addEntreprise(
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Association bidule", date(2016, 9, 5), null, FormeLegale.N_0109_ASSOCIATION,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), null, null, StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996"));
				// Annonce existante

				// Validation
				ProtoAnnonceIDE proto =
						RCEntAnnonceIDEHelper.createProtoAnnonceIDE(TypeAnnonce.MUTATION, DateHelper.getDateTime(2016, 9, 5, 11, 0, 0), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, null, null,
						                                            new NumeroIDE("CHE999999996"), null, null, noEtablissement, noEntrepriseCivile, null,
						                                            "Syntruc Asso", null, FormeLegale.N_0109_ASSOCIATION, null,
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
		final AnnonceIDEEnvoyee annonceIDE = doInNewTransactionAndSession(transactionStatus -> {
			try {
				return (AnnonceIDEEnvoyee) serviceIDE.synchroniseIDE((Entreprise) tiersDAO.get(noEntreprise));
			} catch (Exception e) {
				fail(String.format("Le service IDE a rencontré un problème lors de la synchronisation IDE: %s", e.getMessage()));
				return null;
			}
		});

		// Vérification du résultat
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

			assertNotNull(annonceIDE.getNumero());
			final ReferenceAnnonceIDE referenceAnnonceIDE = referenceAnnonceIDEDAO.get(annonceIDE.getNumero());
			Assert.assertNotNull(referenceAnnonceIDE);

			assertNotNull(annonceIDESender.getMsgBusinessIdUtilisee());
			assertTrue(annonceIDESender.getMsgBusinessIdUtilisee().startsWith("unireg-req-" + referenceAnnonceIDE.getId().toString()));

			assertEquals(TypeAnnonce.MUTATION, annonceIDE.getType());
			assertEquals(TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, annonceIDE.getTypeEtablissementCivil());

			assertNotNull(annonceIDE.getNoIde());
			assertEquals("CHE999999996", annonceIDE.getNoIde().getValeur());
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

			final BaseAnnonceIDE.InformationEntreprise informationEntreprise = annonceIDE.getInformationEntreprise();
			assertNotNull(informationEntreprise);
			assertNotNull(informationEntreprise.getNumeroEntreprise());
			assertEquals(noEntrepriseCivile, informationEntreprise.getNumeroEntreprise());
			assertNotNull(informationEntreprise.getNumeroEtablissement());
			assertEquals(noEtablissement, informationEntreprise.getNumeroEtablissement());
			assertNull(informationEntreprise.getNumeroEtablissementRemplacant());

			final BaseAnnonceIDE.Utilisateur utilisateur = annonceIDE.getUtilisateur();
			assertNotNull(utilisateur);
			assertEquals(RCEntAnnonceIDEHelper.UNIREG_USER, utilisateur.getUserId());
			assertNull(utilisateur.getTelephone());

			final BaseAnnonceIDE.Contenu contenu = annonceIDE.getContenu();
			assertNotNull(contenu);
			assertEquals("Syntruc Asso", contenu.getNom());
			assertNull(contenu.getNomAdditionnel());
			assertEquals(FormeLegale.N_0109_ASSOCIATION, contenu.getFormeLegale());
			assertNull(contenu.getSecteurActivite());

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
			return null;
		});
	}

	@Test
	public void testModificationAdresseEtrangere() throws Exception {
		/*
			Une entité existante et appariée vient d'être modifiée dans Unireg. Une annonce de mutation doit partir vers le registre IDE.
			La modification porte sur un changement d'adresse effective, avec une nouvelle adresse à l'étranger.
		 */

		final Long noEntrepriseCivile = 1111L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2016, 9, 5), null, true);

			addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 5), null, "Syntruc Asso");
			addDomicileEtablissement(etablissement, date(2016, 9, 5), null, MockCommune.Renens);
			addFormeJuridique(entreprise, date(2016, 9, 5), null, FormeJuridiqueEntreprise.ASSOCIATION);

			//entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

			addAdresseEtrangere(entreprise, TypeAdresseTiers.COURRIER, date(2016, 9, 5), null, "Shawinigan Lake B.C. V0R 2W1", "2332 Lockspur Road", MockPays.EtatsUnis);
			return entreprise.getNumero();
		});

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {

				// l'association existante
				addEntreprise(
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Association bidule", date(2016, 9, 5), null, FormeLegale.N_0109_ASSOCIATION,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), null, null, StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996"));
				// Annonce existante

				// Validation
				ProtoAnnonceIDE proto =
						RCEntAnnonceIDEHelper.createProtoAnnonceIDE(TypeAnnonce.MUTATION, DateHelper.getDateTime(2016, 9, 5, 11, 0, 0), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, null, null,
						                                            new NumeroIDE("CHE999999996"), null, null, noEtablissement, noEntrepriseCivile, null,
						                                            "Syntruc Asso", null, FormeLegale.N_0109_ASSOCIATION, null,
						                                            RCEntAnnonceIDEHelper
								                                            .createAdresseAnnonceIDERCEnt("Shawinigan Lake B.C. V0R 2W1", null, null, null, null, null, "2332 Lockspur Road", MockPays.EtatsUnis.getNoOfsEtatSouverain(),
								                                                                          MockPays.EtatsUnis.getCodeIso2(), MockPays.EtatsUnis.getNomCourt(), null,
								                                                                          null, null), null, RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);

				AnnonceIDEEnvoyee.Statut statut = new AnnonceIDEData.StatutImpl(StatutAnnonce.VALIDATION_SANS_ERREUR, DateHelper.getDateTime(2016, 9, 5, 11, 0, 1), new ArrayList<>());
				this.addStatutAnnonceIDEAttentu(proto, statut);

			}
		});

		// Exécute la synchronisation IDE
		final SingleShotMockAnnonceIDESender annonceIDESender = new SingleShotMockAnnonceIDESender();
		annonceIDEService.setAnnonceIDESender(annonceIDESender);
		final AnnonceIDEEnvoyee annonceIDE = doInNewTransactionAndSession(transactionStatus -> {
			try {
				return (AnnonceIDEEnvoyee) serviceIDE.synchroniseIDE((Entreprise) tiersDAO.get(noEntreprise));
			} catch (Exception e) {
				throw new RuntimeException(String.format("Le service IDE a rencontré un problème lors de la synchronisation IDE: %s", e.getMessage()), e);
			}
		});

		// Vérification du résultat
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

			assertNotNull(annonceIDE.getNumero());
			final ReferenceAnnonceIDE referenceAnnonceIDE = referenceAnnonceIDEDAO.get(annonceIDE.getNumero());
			Assert.assertNotNull(referenceAnnonceIDE);

			assertNotNull(annonceIDESender.getMsgBusinessIdUtilisee());
			assertTrue(annonceIDESender.getMsgBusinessIdUtilisee().startsWith("unireg-req-" + referenceAnnonceIDE.getId().toString()));

			assertEquals(TypeAnnonce.MUTATION, annonceIDE.getType());
			assertEquals(TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, annonceIDE.getTypeEtablissementCivil());

			assertNotNull(annonceIDE.getNoIde());
			assertEquals("CHE999999996", annonceIDE.getNoIde().getValeur());
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

			final BaseAnnonceIDE.InformationEntreprise informationEntreprise = annonceIDE.getInformationEntreprise();
			assertNotNull(informationEntreprise);
			assertNotNull(informationEntreprise.getNumeroEntreprise());
			assertEquals(noEntrepriseCivile, informationEntreprise.getNumeroEntreprise());
			assertNotNull(informationEntreprise.getNumeroEtablissement());
			assertEquals(noEtablissement, informationEntreprise.getNumeroEtablissement());
			assertNull(informationEntreprise.getNumeroEtablissementRemplacant());

			final BaseAnnonceIDE.Utilisateur utilisateur = annonceIDE.getUtilisateur();
			assertNotNull(utilisateur);
			assertEquals(RCEntAnnonceIDEHelper.UNIREG_USER, utilisateur.getUserId());
			assertNull(utilisateur.getTelephone());

			final BaseAnnonceIDE.Contenu contenu = annonceIDE.getContenu();
			assertNotNull(contenu);
			assertEquals("Syntruc Asso", contenu.getNom());
			assertNull(contenu.getNomAdditionnel());
			assertEquals(FormeLegale.N_0109_ASSOCIATION, contenu.getFormeLegale());
			assertNull(contenu.getSecteurActivite());

			final AdresseAnnonceIDE adresse = contenu.getAdresse();
			assertNotNull(adresse);
			assertEquals("Shawinigan Lake B.C. V0R 2W1", adresse.getRue());
			assertNull(adresse.getNumero());
			assertNull(adresse.getNumeroAppartement());
			assertNull(adresse.getNpa());
			assertNull(adresse.getNumeroOrdrePostal());
			assertEquals("2332 Lockspur Road", adresse.getVille());

			assertNull(adresse.getNumeroCasePostale());
			assertNull(adresse.getTexteCasePostale());

			final AdresseAnnonceIDE.Pays pays = adresse.getPays();
			assertNotNull(pays);
			assertEquals(MockPays.EtatsUnis.getNoOFS(), pays.getNoOfs().intValue());
			assertEquals(MockPays.EtatsUnis.getCodeIso2(), pays.getCodeISO2());
			assertEquals(MockPays.EtatsUnis.getNomCourt(), pays.getNomCourt());
			return null;
		});
	}
}