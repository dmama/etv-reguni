package ch.vd.uniregctb.evenement.ide.service;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.organisation.data.AdresseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEData;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.NumeroIDE;
import ch.vd.unireg.interfaces.organisation.data.ProtoAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.StatutAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.uniregctb.evenement.ide.SingleShotMockAnnonceIDESender;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

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

		final Long noOrganisation = 1111L;
		final Long noSite = noOrganisation + 1000000;

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				addActiviteEconomique(entreprise, etablissement, date(2016, 9, 5), null, true);

				addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 5), null, "Syntruc Asso");
				addDomicileEtablissement(etablissement, date(2016, 9, 5), null, MockCommune.Renens);
				addFormeJuridique(entreprise, date(2016, 9, 5), null, FormeJuridiqueEntreprise.ASSOCIATION);

				//entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

				final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.DOMICILE, date(2016, 9, 5), null, MockRue.Renens.QuatorzeAvril);
				adresseSuisse.setNumeroMaison("1");

				return entreprise.getNumero();
			}
		});

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {

				// l'association existante
				addOrganisation(
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Association bidule", date(2016, 9, 5), null, FormeLegale.N_0109_ASSOCIATION,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), null, null, StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.ASSOCIATION, "CHE999999996"));
				// Annonce existante

				// Validation
				ProtoAnnonceIDE proto =
						RCEntAnnonceIDEHelper.createProtoAnnonceIDE(TypeAnnonce.MUTATION, DateHelper.getDateTime(2016, 9, 5, 11, 0, 0), null, null, TypeDeSite.ETABLISSEMENT_PRINCIPAL, null, null,
						                                            new NumeroIDE("CHE999999996"), null, null, noSite, noOrganisation, null,
						                                            "Syntruc Asso", null, FormeLegale.N_0109_ASSOCIATION, null,
						                                            RCEntAnnonceIDEHelper
								                                                  .createAdresseAnnonceIDERCEnt(MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), "1", null, MockLocalite.Renens.getNPA(), MockLocalite.Renens.getNoOrdre(), MockLocalite.Renens.getNom(), MockPays.Suisse.getNoOfsEtatSouverain(),
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

				assertEquals(TypeAnnonce.MUTATION, annonceIDE.getType());
				assertEquals(TypeDeSite.ETABLISSEMENT_PRINCIPAL, annonceIDE.getTypeDeSite());

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

				final BaseAnnonceIDE.InformationOrganisation informationOrganisation = annonceIDE.getInformationOrganisation();
				assertNotNull(informationOrganisation);
				assertNotNull(informationOrganisation.getNumeroOrganisation());
				assertEquals(noOrganisation, informationOrganisation.getNumeroOrganisation());
				assertNotNull(informationOrganisation.getNumeroSite());
				assertEquals(noSite, informationOrganisation.getNumeroSite());
				assertNull(informationOrganisation.getNumeroSiteRemplacant());

				final BaseAnnonceIDE.Utilisateur utilisateur = annonceIDE.getUtilisateur();
				assertNotNull(utilisateur);
				assertEquals("unireg", utilisateur.getUserId());
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
			}
		});
	}
}