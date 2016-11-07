package ch.vd.uniregctb.evenement.ide.service;

import java.util.ArrayList;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEData;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.ProtoAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.StatutAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.uniregctb.evenement.ide.ServiceIDEException;
import ch.vd.uniregctb.evenement.ide.SingleShotMockAnnonceIDESender;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

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

				final ReferenceAnnonceIDE refAnnonce = addReferenceAnnonceIDE("fake_business_id", etablissement);

				return refAnnonce.getId();
			}
		});

		// Mise en place annonce à l'IDE RCEnt
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {

				// Annonce existante

				// Validation
				ProtoAnnonceIDE proto =
						RCEntAnnonceIDEHelper.createProtoAnnonceIDE(TypeAnnonce.CREATION, DateHelper.getDateTime(2016, 9, 5, 11, 0, 0), null, null, TypeDeSite.ETABLISSEMENT_PRINCIPAL, null, null,
						                                            null, null, null, null, null, null,
						                                            "Syntruc Asso", null, FormeLegale.N_0109_ASSOCIATION, "Fabrication d'objets synthétiques",
						                                            RCEntAnnonceIDEHelper
								                                            .createAdresseAnnonceIDERCEnt(MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), "1", null, 1020, MockLocalite.Renens.getNom(), MockPays.Suisse.getNoOfsEtatSouverain(),
								                                                                          MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(), null,
								                                                                          null, null), null, RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);

				AnnonceIDEEnvoyee.Statut statut = new AnnonceIDEData.StatutImpl(StatutAnnonce.VALIDATION_SANS_ERREUR, DateHelper.getDateTime(2016, 9, 5, 11, 0, 1), new ArrayList<Pair<String, String>>());
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
					serviceIDE.synchroniseIDE((Entreprise) tiersDAO.get(noEntreprise));
				}
				catch (ServiceIDEException e) {
					assertEquals(String.format("Entreprise n°%s: une annonce est en attente de reception par le registre civil des entreprises (RCEnt). " +
							             "Ce traitement doit avoir lieu avant de pouvoir déterminer s'il faut annoncer de nouveaux changements.", FormatNumeroHelper.numeroCTBToDisplay(noEntreprise)),
					             e.getMessage());
					return null;
				} catch (Exception e) {
					fail(String.format("Le service IDE a rencontré un problème lors de la synchronisation IDE: %s", e.getMessage()));
				}
				fail("L'exception attendue n'est pas survenue.");
				return null;
			}
		});
	}

	@Test
	public void testEntreprisePasConcernee() throws Exception {
		/*
			Une nouvelle entreprise a été crée. De part son type elle ne doit pas être annoncé.
		 */

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseInconnueAuCivil();

				addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 5), null, "Synchrotec SA");
				addFormeJuridique(entreprise, date(2016, 9, 5), null, FormeJuridiqueEntreprise.SA);

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

		// Mise en place annonce à l'IDE RCEnt
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {

				// Annonce existante
				// Validation
			}
		});

		// Exécute la synchronisation IDE
		final SingleShotMockAnnonceIDESender annonceIDESender = new SingleShotMockAnnonceIDESender();
		annonceIDEService.setAnnonceIDESender(annonceIDESender);
		final BaseAnnonceIDE baseAnnonceIDE = doInNewTransactionAndSession(new TransactionCallback<BaseAnnonceIDE>() {
			@Override
			public BaseAnnonceIDE doInTransaction(TransactionStatus transactionStatus) {
				try {
					return serviceIDE.synchroniseIDE((Entreprise) tiersDAO.get(noEntreprise));
				} catch (Exception e) {
					fail(String.format("Le service IDE a rencontré un problème lors de la synchronisation IDE: %s", e.getMessage()));
				}
				return null;
			}
		});
		assertNull(baseAnnonceIDE);
	}


	@Test
	public void testAssociationInscriteAuRCPasConcernee() throws Exception {
		/*
			Une nouvelle association a été crée par inscripton au RC. De part son type elle ne doit pas être annoncé.
		 */

		final Long noOrganisation = 100L;
		final Long noSite = noOrganisation + 100;

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 5), date(2016, 9, 7), "Synergy Assoc");
				addFormeJuridique(entreprise, date(2016, 9, 5), date(2016, 9, 7), FormeJuridiqueEntreprise.ASSOCIATION);

				entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

				final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.DOMICILE, date(2016, 9, 5), null, MockRue.Renens.QuatorzeAvril);
				adresseSuisse.setNumeroMaison("1"); // On laisse l'adresse ouverte, pas envie de configurer une adresse RCEnt propre car le test ne porte pas dessus.

				return entreprise.getNumero();
			}
		});

		// Création de l'établissement
		final Long noEtablissement = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);
				addDomicileEtablissement(etablissement, date(2016, 9, 5), date(2016, 9, 7), MockCommune.Renens);
				addActiviteEconomique(entreprise, etablissement, date(2016, 9, 5), null, true);

				return etablissement.getNumero();
			}
		});

		// Mise en place annonce à l'IDE RCEnt
		final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy Assoc", date(2016, 9, 8), null, FormeLegale.N_0109_ASSOCIATION,
		                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), StatusInscriptionRC.ACTIF, date(2016, 9, 5),
		                                                                        StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.ASSOCIATION, "CHE999999996");
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {

				addOrganisation(org);

				// Annonce existante
				// Validation
			}
		});

		// Exécute la synchronisation IDE
		final SingleShotMockAnnonceIDESender annonceIDESender = new SingleShotMockAnnonceIDESender();
		annonceIDEService.setAnnonceIDESender(annonceIDESender);
		final BaseAnnonceIDE baseAnnonceIDE = doInNewTransactionAndSession(new TransactionCallback<BaseAnnonceIDE>() {
			@Override
			public BaseAnnonceIDE doInTransaction(TransactionStatus transactionStatus) {
				try {
					return serviceIDE.synchroniseIDE((Entreprise) tiersDAO.get(noEntreprise));
				} catch (Exception e) {
					fail(String.format("Le service IDE a rencontré un problème lors de la synchronisation IDE: %s", e.getMessage()));
				}
				return null;
			}
		});
		assertNull(baseAnnonceIDE);
	}
}