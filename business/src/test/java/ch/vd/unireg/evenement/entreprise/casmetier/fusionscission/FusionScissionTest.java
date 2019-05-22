package ch.vd.unireg.evenement.entreprise.casmetier.fusionscission;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.PublicationBusiness;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeDeFusion;
import ch.vd.unireg.interfaces.entreprise.data.TypeDePublicationBusiness;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;

/**
 * @author Raphaël Marmier, 2016-02-19
 */
public class FusionScissionTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public FusionScissionTest() {
		setWantIndexationTiers(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Test(timeout = 10000L)
	public void testFusionEntreprise() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) entreprise.getEtablissements().get(0);
				etablissementPrincipal.addPublicationBusiness(new PublicationBusiness(date(2015, 7, 3), TypeDePublicationBusiness.FOSC_APPEL_AUX_CREANCIERS_SUITE_FUSION_OU_SCISSION, "123456", date(2015, 7, 5),
				                                                                      "Blah blah publication FOSC", TypeDeFusion.FUSION_INTERNATIONALE, null, null, null));
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> addEntrepriseConnueAuCivil(noEntrepriseCivile));

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});


		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			Assert.assertEquals("Une vérification, pouvant aboutir à un traitement manuel (processus complexe), est requise pour cause de Fusion d'entreprises.",
			                    evt.getErreurs().get(2).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testPasDeFusionEntreprise() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) entreprise.getEtablissements().get(0);
				etablissementPrincipal.addPublicationBusiness(new PublicationBusiness(date(2015, 7, 3), TypeDePublicationBusiness.FOSC_COMMANDEMENT_DE_PAYER, "1233634231", date(2015, 7, 5),
				                                                                      "blah blah publication.", null, null, null, null));
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> addEntrepriseConnueAuCivil(noEntrepriseCivile));

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});


		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testScissionEntreprise() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) entreprise.getEtablissements().get(0);
				etablissementPrincipal.addPublicationBusiness(new PublicationBusiness(date(2015, 7, 3), TypeDePublicationBusiness.FOSC_APPEL_AUX_CREANCIERS_SUITE_FUSION_OU_SCISSION, "123456", date(2015, 7, 5),
				                                                                      "Blah blah publication FOSC", TypeDeFusion.SCISSION_ART_45_LFUS, null, null, null));
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> addEntrepriseConnueAuCivil(noEntrepriseCivile));

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});


		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			Assert.assertEquals("Une vérification, pouvant aboutir à un traitement manuel (processus complexe), est requise pour cause de Scission de l'entreprise.",
			                    evt.getErreurs().get(2).getMessage());
			return null;
		});
	}
}
