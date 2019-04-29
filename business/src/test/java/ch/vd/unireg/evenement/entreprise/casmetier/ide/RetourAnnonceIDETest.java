package ch.vd.unireg.evenement.entreprise.casmetier.ide;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AdresseAnnonceIDERCEnt;
import ch.vd.unireg.interfaces.entreprise.data.AdresseEffectiveRCEnt;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.NumeroIDE;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeAnnonce;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.entreprise.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;
import static ch.vd.unireg.type.EtatEvenementEntreprise.FORCE;

/**
 * @author Raphaël Marmier, 2016-09-27, <raphael.marmier@vd.ch>
 */
public class RetourAnnonceIDETest extends AbstractEvenementEntrepriseCivileProcessorTest {

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
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissementCivil = noEntrepriseCivile + 1000000;

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseInconnueAuCivil();

			addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 1), null, "Syntruc Asso");
			addFormeJuridique(entreprise, date(2016, 9, 1), null, FormeJuridiqueEntreprise.ASSOCIATION);

			addIdentificationEntreprise(entreprise, "CHE999999996");

			entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

			final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.DOMICILE, date(2016, 9, 1), null, MockRue.Renens.QuatorzeAvril);
			adresseSuisse.setNumeroMaison("1");
			return entreprise.getNumero();
		});

		// Création de l'établissement
		final Long noEtablissement = doInNewTransactionAndSession(status -> {
			Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

			Etablissement etablissement = addEtablissement();
			addDomicileEtablissement(etablissement, date(2016, 9, 1), null, MockCommune.Renens);
			addActiviteEconomique(entreprise, etablissement, date(2016, 9, 1), null, true);
			return etablissement.getNumero();
		});

		// Ajout de la référence d'annonce
		final Long idReferenceAnnonce = doInNewTransactionAndSession(status -> {
			Etablissement etablissement = (Etablissement) tiersDAO.get(noEtablissement);

			final ReferenceAnnonceIDE refAnnonce = addReferenceAnnonceIDE("test_business_id", etablissement);
			return refAnnonce.getId();
		});

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final AdresseAnnonceIDERCEnt adresse = RCEntAnnonceIDEHelper
						.createAdresseAnnonceIDERCEnt(MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), "1", null, MockLocalite.Renens.getNPA(), null, MockLocalite.Renens.getNoOrdre(), MockLocalite.Renens.getNom(),
						                              MockPays.Suisse.getNoOfsEtatSouverain(), MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(),
						                              null, null, null);
				// Annonce existante
				AnnonceIDE annonce =
						RCEntAnnonceIDEHelper
								.createAnnonceIDE(idReferenceAnnonce, TypeAnnonce.CREATION, DateHelper.getDateTime(2016, 9, 1, 11, 0, 0), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, null, null,
								                  new NumeroIDE("CHE999999996"), null, null, null, null, null,
								                  "Syntruc Asso", null, FormeLegale.N_0109_ASSOCIATION, "Fabrication d'objets synthétiques",
								                  adresse, null, RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);
				this.addAnnonceIDE(annonce, RCEntAnnonceIDEHelper.UNIREG_USER);

				// L'entreprise
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Syntruc Asso", date(2016, 9, 5), null, FormeLegale.N_0109_ASSOCIATION,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(),
				                                                                        null, null,
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996");
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) ent.getEtablissements().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.addAdresseEffective(new AdresseEffectiveRCEnt(date(2016, 9, 5), null, MockLocalite.Renens.getNom(), "1", null, MockLocalite.Renens.getNoOrdre(),
				                                                                 MockLocalite.Renens.getNPA().toString(), null, MockPays.Suisse.getNoOfsEtatSouverain(),
				                                                                 MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), null, null, null));
				addEntreprise(ent);
			}
		});

		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2016, 9, 5), A_TRAITER);
			event.setReferenceAnnonceIDE(referenceAnnonceIDEDAO.get(idReferenceAnnonce));
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

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
							"EntrepriseCivile civile n°%d rattachée à l'entreprise n°%s.",
							noEntrepriseCivile,
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
		});
	}

	@Test(timeout = 1000000L)
	public void testNouveauRCEntAnnonceeIDEDejaRecuUneFois() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissementCivil = noEntrepriseCivile + 1000000;

		// Création de l'entreprise
		final Long noEntreprise = doInNewTransactionAndSession(status -> {
			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			addRaisonSocialeFiscaleEntreprise(entreprise, date(2016, 9, 1), date(2016, 9, 4), "Syntruc Asso");
			addFormeJuridique(entreprise, date(2016, 9, 1), date(2016, 9, 4), FormeJuridiqueEntreprise.ASSOCIATION);

			addIdentificationEntreprise(entreprise, "CHE999999996");

			entreprise.changeSecteurActivite("Fabrication d'objets synthétiques");

			final AdresseSuisse adresseSuisse = addAdresseSuisse(entreprise, TypeAdresseTiers.DOMICILE, date(2016, 9, 1), date(2016, 9, 4), MockRue.Renens.QuatorzeAvril);
			adresseSuisse.setNumeroMaison("1");
			return entreprise.getNumero();
		});

		// Création de l'établissement
		final Long noEtablissement = doInNewTransactionAndSession(status -> {
			Entreprise entreprise = (Entreprise) tiersDAO.get(noEntreprise);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissementCivil);
			addDomicileEtablissement(etablissement, date(2016, 9, 1), date(2016, 9, 4), MockCommune.Renens);
			addActiviteEconomique(entreprise, etablissement, date(2016, 9, 1), null, true);
			return etablissement.getNumero();
		});

		// Ajout de la référence d'annonce
		final Long idReferenceAnnonce = doInNewTransactionAndSession(status -> {
			Etablissement etablissement = (Etablissement) tiersDAO.get(noEtablissement);

			final ReferenceAnnonceIDE refAnnonce = addReferenceAnnonceIDE("test_business_id", etablissement);
			return refAnnonce.getId();
		});

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final AdresseAnnonceIDERCEnt adresse = RCEntAnnonceIDEHelper
						.createAdresseAnnonceIDERCEnt(MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), "1", null, MockLocalite.Renens.getNPA(), null, MockLocalite.Renens.getNoOrdre(), MockLocalite.Renens.getNom(),
						                              MockPays.Suisse.getNoOfsEtatSouverain(), MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(),
						                              null, null, null);
				// Annonce existante
				AnnonceIDE annonce =
						RCEntAnnonceIDEHelper
								.createAnnonceIDE(idReferenceAnnonce, TypeAnnonce.CREATION, DateHelper.getDateTime(2016, 9, 1, 11, 0, 0), RCEntAnnonceIDEHelper.UNIREG_USER, null, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, null, null,
								                  new NumeroIDE("CHE999999996"), null, null, null, null, null,
								                  "Syntruc Asso", null, FormeLegale.N_0109_ASSOCIATION, "Fabrication d'objets synthétiques",
								                  adresse, null, RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);
				this.addAnnonceIDE(annonce, RCEntAnnonceIDEHelper.UNIREG_USER);

				// L'entreprise
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Syntruc Asso", date(2016, 9, 5), null, FormeLegale.N_0109_ASSOCIATION,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(),
				                                                                        null, null,
				                                                                        StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.ASSOCIATION, "CHE999999996");
				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) ent.getEtablissements().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.addAdresseEffective(new AdresseEffectiveRCEnt(date(2016, 9, 5), null, MockLocalite.Renens.getNom(), "1", null, MockLocalite.Renens.getNoOrdre(),
				                                                                 MockLocalite.Renens.getNPA().toString(), null, MockPays.Suisse.getNoOfsEtatSouverain(),
				                                                                 MockRue.Renens.QuatorzeAvril.getDesignationCourrier(), null, null, null));
				addEntreprise(ent);
			}
		});

		globalTiersIndexer.sync();

		// Création de l'événement
		final Long noPremierEvenement = 11111L;
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise pastEvent = createEvent(noPremierEvenement, noEntrepriseCivile, TypeEvenementEntreprise.IDE_NOUVELLE_INSCRIPTION, RegDate.get(2016, 9, 5), FORCE);
			pastEvent.setReferenceAnnonceIDE(referenceAnnonceIDEDAO.get(idReferenceAnnonce));
			hibernateTemplate.merge(pastEvent);
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.IDE_MUTATION, RegDate.get(2016, 9, 10), A_TRAITER);
			event.setReferenceAnnonceIDE(referenceAnnonceIDEDAO.get(idReferenceAnnonce));
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());

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
		});
	}
}