package ch.vd.unireg.evenement.ide.service;

import java.util.ArrayList;

import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.unireg.evenement.ide.ServiceIDEException;
import ch.vd.unireg.evenement.ide.SingleShotMockAnnonceIDESender;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEData;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.ProtoAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.StatutAnnonce;
import ch.vd.unireg.interfaces.entreprise.data.TypeAnnonce;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Raphaël Marmier, 2016-09-14, <raphael.marmier@vd.ch>
 */
public class ServiceIDECasParticuliersTest extends AbstractServiceIDEServiceTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}

	@Test
	public void testUneAnnonceEstEncoreDansLeTuyau() throws Exception {
		/*
			Une nouvelle entité vient d'être créée dans Unireg. Une annonce de création doit partir vers le registre IDE.
		 */

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseInconnueAuCivil();

			addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 5), null, "Syntruc Asso");
			addFormeJuridique(entreprise, date(2016, 9, 5), null, FormeJuridiqueEntreprise.ASSOCIATION);

			entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

			final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, date(2016, 9, 5), null, MockRue.Renens.QuatorzeAvril);
			adresseSuisse.setNumeroMaison("1");
			return entreprise.getNumero();
		});

		// Création de l'établissement
		final Long noEtablissement = doInNewTransactionAndSession(status -> {
			Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

			Etablissement etablissement = addEtablissement();
			addDomicileEtablissement(etablissement, date(2016, 9, 5), null, MockCommune.Renens);
			addActiviteEconomique(entreprise, etablissement, date(2016, 9, 5), null, true);
			return etablissement.getNumero();
		});

		// Ajout de la référence d'annonce
		final Long idReferenceAnnonce = doInNewTransactionAndSession(status -> {
			Etablissement etablissement = (Etablissement) tiersDAO.get(noEtablissement);

			final ReferenceAnnonceIDE refAnnonce = addReferenceAnnonceIDE("fake_business_id", etablissement);
			return refAnnonce.getId();
		});

		// Mise en place annonce à l'IDE RCEnt
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
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
		final AnnonceIDEEnvoyee annonceIDE = doInNewTransactionAndSession(status -> {
			try {
				serviceIDE.synchroniseIDE((Entreprise) tiersDAO.get(noEntreprise));
			}
			catch (ServiceIDEException e) {
				assertEquals(String.format("Entreprise n°%s: une annonce est en attente de reception par le registre civil des entreprises (RCEnt). " +
						                           "Ce traitement doit avoir lieu avant de pouvoir déterminer s'il faut annoncer de nouveaux changements.", FormatNumeroHelper.numeroCTBToDisplay(noEntreprise)),
				             e.getMessage());
				return null;
			}
			catch (Exception e) {
				fail(String.format("Le service IDE a rencontré un problème lors de la synchronisation IDE: %s", e.getMessage()));
			}
			fail("L'exception attendue n'est pas survenue.");
			return null;
		});
	}

	@Test
	public void testEntreprisePasConcernee() throws Exception {
		/*
			Une nouvelle entreprise a été crée. De part son type elle ne doit pas être annoncé.
		 */

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseInconnueAuCivil();

			addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 5), null, "Synchrotec SA");
			addFormeJuridique(entreprise, date(2016, 9, 5), null, FormeJuridiqueEntreprise.SA);

			entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

			final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, date(2016, 9, 5), null, MockRue.Renens.QuatorzeAvril);
			adresseSuisse.setNumeroMaison("1");
			return entreprise.getNumero();
		});

		// Création de l'établissement
		final Long noEtablissement = doInNewTransactionAndSession(status -> {
			Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

			Etablissement etablissement = addEtablissement();
			addDomicileEtablissement(etablissement, date(2016, 9, 5), null, MockCommune.Renens);
			addActiviteEconomique(entreprise, etablissement, date(2016, 9, 5), null, true);
			return etablissement.getNumero();
		});

		// Mise en place annonce à l'IDE RCEnt
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {

				// Annonce existante
				// Validation
			}
		});

		// Exécute la synchronisation IDE
		final SingleShotMockAnnonceIDESender annonceIDESender = new SingleShotMockAnnonceIDESender();
		annonceIDEService.setAnnonceIDESender(annonceIDESender);
		final BaseAnnonceIDE baseAnnonceIDE = doInNewTransactionAndSession(status -> {
			try {
				return serviceIDE.synchroniseIDE((Entreprise) tiersDAO.get(noEntreprise));
			}
			catch (Exception e) {
				fail(String.format("Le service IDE a rencontré un problème lors de la synchronisation IDE: %s", e.getMessage()));
			}
			return null;
		});
		assertNull(baseAnnonceIDE);
	}


	@Test
	public void testAssociationInscriteAuRCPasConcernee() throws Exception {
		/*
			Une nouvelle association a été crée par inscripton au RC. De part son type elle ne doit pas être annoncé.
		 */

		final Long noEntrepriseCivile = 100L;
		final Long noEtablissementCivil = noEntrepriseCivile + 100;

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 5), date(2016, 9, 7), "Synergy Assoc");
			addFormeJuridique(entreprise, date(2016, 9, 5), date(2016, 9, 7), FormeJuridiqueEntreprise.ASSOCIATION);

			entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

			final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, date(2016, 9, 5), null, MockRue.Renens.QuatorzeAvril);
			adresseSuisse.setNumeroMaison("1"); // On laisse l'adresse ouverte, pas envie de configurer une adresse RCEnt propre car le test ne porte pas dessus.;
			return entreprise.getNumero();
		});

		// Création de l'établissement
		final Long noEtablissement = doInNewTransactionAndSession(status -> {
			Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissementCivil);
			addDomicileEtablissement(etablissement, date(2016, 9, 5), date(2016, 9, 7), MockCommune.Renens);
			addActiviteEconomique(entreprise, etablissement, date(2016, 9, 5), null, true);
			return etablissement.getNumero();
		});

		// Mise en place annonce à l'IDE RCEnt
		final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy Assoc", date(2016, 9, 8), null, FormeLegale.N_0109_ASSOCIATION,
		                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), StatusInscriptionRC.ACTIF, date(2016, 9, 5),
		                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996");
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {

				addEntreprise(ent);

				// Annonce existante
				// Validation
			}
		});

		// Exécute la synchronisation IDE
		final SingleShotMockAnnonceIDESender annonceIDESender = new SingleShotMockAnnonceIDESender();
		annonceIDEService.setAnnonceIDESender(annonceIDESender);
		final BaseAnnonceIDE baseAnnonceIDE = doInNewTransactionAndSession(status -> {
			try {
				return serviceIDE.synchroniseIDE((Entreprise) tiersDAO.get(noEntreprise));
			}
			catch (Exception e) {
				fail(String.format("Le service IDE a rencontré un problème lors de la synchronisation IDE: %s", e.getMessage()));
			}
			return null;
		});
		assertNull(baseAnnonceIDE);
	}
}