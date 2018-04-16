package ch.vd.unireg.evenement.organisation.casmetier.ide;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.engine.AbstractEvenementOrganisationProcessorTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.organisation.data.AdresseAnnonceIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.AdresseEffectiveRCEnt;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.NumeroIDE;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementOrganisation;

import static ch.vd.unireg.type.EtatEvenementOrganisation.A_TRAITER;
import static ch.vd.unireg.type.EtatEvenementOrganisation.FORCE;

/**
 * @author Raphaël Marmier, 2016-09-27, <raphael.marmier@vd.ch>
 */
public class RetourAnnonceIDETest extends AbstractEvenementOrganisationProcessorTest {

	public RetourAnnonceIDETest() {
		setWantIndexationTiers(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}


	@Test(timeout = 1000000L)
	public void testNouveauRCEntAnnonceeIDERienNeSePasse() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseInconnueAuCivil();

				addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 1), null, "Syntruc Asso");
				addFormeJuridique(entreprise, date(2016, 9, 1), null, FormeJuridiqueEntreprise.ASSOCIATION);

				addIdentificationEntreprise(entreprise, "CHE999999996");

				entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

				final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.DOMICILE, date(2016, 9, 1), null, MockRue.Renens.QuatorzeAvril);
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
				addDomicileEtablissement(etablissement, date(2016, 9, 1), null, MockCommune.Renens);
				addActiviteEconomique(entreprise, etablissement, date(2016, 9, 1), null, true);

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
						RCEntAnnonceIDEHelper
								.createAnnonceIDE(idReferenceAnnonce, TypeAnnonce.CREATION, DateHelper.getDateTime(2016, 9, 1, 11, 0, 0), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeDeSite.ETABLISSEMENT_PRINCIPAL, null, null,
								                  new NumeroIDE("CHE999999996"), null, null, null, null, null,
								                  "Syntruc Asso", null, FormeLegale.N_0109_ASSOCIATION, "Fabrication d'objets synthétiques",
								                  adresse, null, RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);
				this.addAnnonceIDE(annonce, RCEntAnnonceIDEHelper.UNIREG_USER);

				// L'organisation
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Syntruc Asso", date(2016, 9, 5), null, FormeLegale.N_0109_ASSOCIATION,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(),
				                                                                        null, null,
				                                                                        StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.ASSOCIATION, "CHE999999996");
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) org.getDonneesSites().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.addAdresseEffective(new AdresseEffectiveRCEnt(date(2016, 9, 5), null, MockLocalite.Renens.getNom(), "1", null, MockLocalite.Renens.getNoOrdre(),
				                                                                 MockLocalite.Renens.getNPA().toString(), null, MockPays.Suisse.getNoOfsEtatSouverain(),
				                                                                 MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), null, null, null));
				addOrganisation(org);
			}
		});

		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2016, 9, 5), A_TRAITER);
				event.setReferenceAnnonceIDE(referenceAnnonceIDEDAO.get(idReferenceAnnonce));
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissement);

				                             Assert.assertEquals(3, evt.getErreurs().size());
				                             Assert.assertEquals(
						                             String.format(
								                             "Retour de l'annonce à l'IDE n°%s du %s concernant l'entreprise n°%s suite à création ou modification dans Unireg. L'état à l'IDE est maintenant aligné sur celui d'Unireg.",
								                             idReferenceAnnonce,
								                             DateHelper.dateTimeToDisplayString(DateHelper.getDateTime(2016, 9, 1, 11, 0, 0)),
								                             FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()
								                             )
						                             ),
						                             evt.getErreurs().get(1).getMessage()
				                             );
				                             Assert.assertEquals(
						                             String.format(
								                             "Organisation civile n°%d rattachée à l'entreprise n°%s.",
								                             noOrganisation,
								                             FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()
								                             )
						                             ),
						                             evt.getErreurs().get(2).getMessage()
				                             );

				                             // Vérification de la fermeture des surcharges fiscales des données civiles
				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementPrincipal(entreprise, date(2016, 9, 5));
				                             final DomicileEtablissement domicileEtablissement = DateRangeHelper.rangeAt(etablissementPrincipal.getSortedDomiciles(false), date(2016, 9, 4));
				                             Assert.assertNotNull(domicileEtablissement);
				                             Assert.assertNotNull(domicileEtablissement.getDateFin());
				                             Assert.assertEquals(date(2016, 9, 4), domicileEtablissement.getDateFin());

				                             final List<AdresseTiers> adressesTiersSorted = AnnulableHelper.sansElementsAnnules(entreprise.getAdressesTiersSorted(TypeAdresseTiers.COURRIER));
				                             if (adressesTiersSorted.isEmpty()) {
					                             return null;
				                             }
				                             final AdresseTiers adresseTiersCourrier = DateRangeHelper.rangeAt(adressesTiersSorted, date(2016, 9, 4));
				                             Assert.assertNotNull(adresseTiersCourrier);
				                             Assert.assertNotNull(adresseTiersCourrier.getDateFin());
				                             Assert.assertEquals(date(2016, 9, 4), adresseTiersCourrier.getDateFin());


				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 1000000L)
	public void testNouveauRCEntAnnonceeIDEDejaRecuUneFois() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 1), date(2016, 9, 4), "Syntruc Asso");
				addFormeJuridique(entreprise, date(2016, 9, 1), date(2016, 9, 4), FormeJuridiqueEntreprise.ASSOCIATION);

				addIdentificationEntreprise(entreprise, "CHE999999996");

				entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

				final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.DOMICILE, date(2016, 9, 1), date(2016, 9, 4), MockRue.Renens.QuatorzeAvril);
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
				etablissement.setNumeroEtablissement(noSite);
				addDomicileEtablissement(etablissement, date(2016, 9, 1), date(2016, 9, 4), MockCommune.Renens);
				addActiviteEconomique(entreprise, etablissement, date(2016, 9, 1), null, true);

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
						RCEntAnnonceIDEHelper
								.createAnnonceIDE(idReferenceAnnonce, TypeAnnonce.CREATION, DateHelper.getDateTime(2016, 9, 1, 11, 0, 0), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeDeSite.ETABLISSEMENT_PRINCIPAL, null, null,
								                  new NumeroIDE("CHE999999996"), null, null, null, null, null,
								                  "Syntruc Asso", null, FormeLegale.N_0109_ASSOCIATION, "Fabrication d'objets synthétiques",
								                  adresse, null, RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);
				this.addAnnonceIDE(annonce, RCEntAnnonceIDEHelper.UNIREG_USER);

				// L'organisation
				final MockOrganisation org = MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Syntruc Asso", date(2016, 9, 5), null, FormeLegale.N_0109_ASSOCIATION,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(),
				                                                                        null, null,
				                                                                        StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.ASSOCIATION, "CHE999999996");
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) org.getDonneesSites().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.addAdresseEffective(new AdresseEffectiveRCEnt(date(2016, 9, 5), null, MockLocalite.Renens.getNom(), "1", null, MockLocalite.Renens.getNoOrdre(),
				                                                                 MockLocalite.Renens.getNPA().toString(), null, MockPays.Suisse.getNoOfsEtatSouverain(),
				                                                                 MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), null, null, null));
				addOrganisation(org);
			}
		});

		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noPremierEvenement = 11111L;
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation pastEvent = createEvent(noPremierEvenement, noOrganisation, TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2016, 9, 5), FORCE);
				pastEvent.setReferenceAnnonceIDE(referenceAnnonceIDEDAO.get(idReferenceAnnonce));
				hibernateTemplate.merge(pastEvent);
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.IDE_MUTATION, RegDate.get(2016, 9, 10), A_TRAITER);
				event.setReferenceAnnonceIDE(referenceAnnonceIDEDAO.get(idReferenceAnnonce));
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.EN_ERREUR, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             final Etablissement etablissement = tiersService.getEtablissementsPrincipauxEntreprise(entreprise).get(0).getPayload();
				                             Assert.assertNotNull(etablissement);

				                             Assert.assertEquals(1, evt.getErreurs().size());
				                             Assert.assertEquals(
						                             String.format(
								                             "Un ou plusieurs précédant événements RCEnt sont déjà associés à l'annonce à l'IDE n°%d. Le présent événement n°%d ne peut lui aussi provenir de cette annonce à l'IDE. C'est un bug du registre civil. Traitement manuel.",
								                             noEvenement,
								                             idReferenceAnnonce
								                             ),
						                             evt.getErreurs().get(0).getMessage()
				                             );

				                             // Vérification de la fermeture des surcharges fiscales des données civiles
				                             final Etablissement etablissementPrincipal = tiersService.getEtablissementPrincipal(entreprise, date(2016, 9, 5));
				                             final DomicileEtablissement domicileEtablissement = DateRangeHelper.rangeAt(etablissementPrincipal.getSortedDomiciles(false), date(2016, 9, 4));
				                             Assert.assertNotNull(domicileEtablissement);
				                             Assert.assertNotNull(domicileEtablissement.getDateFin());
				                             Assert.assertEquals(date(2016, 9, 4), domicileEtablissement.getDateFin());

				                             final List<AdresseTiers> adressesTiersSorted = AnnulableHelper.sansElementsAnnules(entreprise.getAdressesTiersSorted(TypeAdresseTiers.COURRIER));
				                             if (adressesTiersSorted.isEmpty()) {
					                             return null;
				                             }
				                             final AdresseTiers adresseTiersCourrier = DateRangeHelper.rangeAt(adressesTiersSorted, date(2016, 9, 4));
				                             Assert.assertNotNull(adresseTiersCourrier);
				                             Assert.assertNotNull(adresseTiersCourrier.getDateFin());
				                             Assert.assertEquals(date(2016, 9, 4), adresseTiersCourrier.getDateFin());


				                             return null;
			                             }
		                             }
		);
	}
}