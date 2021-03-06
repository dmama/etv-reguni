package ch.vd.unireg.evenement.entreprise.casmetier.raisonsociale;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEtablissementCivilFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;

/**
 * @author Raphaël Marmier, 2016-05-18
 */
public class RaisonSocialeTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public RaisonSocialeTest() {
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
	public void testSimpleChgmtRaisonSociale() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 1012021L;
		final Long noEtablissement = noEntrepriseCivile + 100;
		final Long noEtablissement2 = noEntrepriseCivile + 200;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				final MockEtablissementCivil etablissementSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                                      FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                                      MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                                      StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997");

				final MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) ent.getEtablissementPrincipal(date(2010, 6, 26)).getPayload();

				etablissementPrincipal.changeNom(date(2015, 7, 5), "Energol SA");
				etablissementSecondaire.changeNom(date(2015, 7, 5), "Energol creation Aubonne SA");
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		final Map<Long, Long> idMap = doInNewTransactionAndSession(status -> {
			Map<Long, Long> map = new HashMap<>();

			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			map.put(noEtablissement, etablissement.getNumero());

			Etablissement etablissementSecondaire = addEtablissement();
			etablissementSecondaire.setNumeroEtablissement(noEtablissement2);

			addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);

			map.put(noEtablissement2, etablissementSecondaire.getNumero());
			return map;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			Assert.assertEquals(String.format("Changement de raison sociale de l'établissement principal n°%s (civil: %d). Synergy SA devient Energol SA.",
			                                  FormatNumeroHelper.numeroCTBToDisplay(idMap.get(noEtablissement)),
			                                  noEtablissement),
			                    evt.getErreurs().get(2).getMessage());

			Assert.assertEquals(String.format("Changement de raison sociale de l'établissement secondaire n°%s (civil: %d). Synergy Conception Aubonne SA devient Energol creation Aubonne SA.",
			                                  FormatNumeroHelper.numeroCTBToDisplay(idMap.get(noEtablissement2)),
			                                  noEtablissement2),
			                    evt.getErreurs().get(3).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testChgmtRaisonSocialeAvecEnseigne() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 1012021L;
		final Long noEtablissement = noEntrepriseCivile + 100;
		final Long noEtablissement2 = noEntrepriseCivile + 200;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				final MockEtablissementCivil etablissementSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                                      FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                                      MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                                      StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997");

				final MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) ent.getEtablissementPrincipal(date(2010, 6, 26)).getPayload();

				etablissementPrincipal.changeNom(date(2015, 7, 5), "Energol SA");
				etablissementSecondaire.changeNom(date(2015, 7, 5), "Energol creation Aubonne SA");
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		final Map<Long, Long> idMap = doInNewTransactionAndSession(status -> {
			Map<Long, Long> map = new HashMap<>();

			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			map.put(noEtablissement, etablissement.getNumero());

			Etablissement etablissementSecondaire = addEtablissement();
			etablissementSecondaire.setNumeroEtablissement(noEtablissement2);
			etablissementSecondaire.setEnseigne("Synergy Conception");

			addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);

			map.put(noEtablissement2, etablissementSecondaire.getNumero());
			return map;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			Assert.assertEquals(String.format("Changement de raison sociale de l'établissement principal n°%s (civil: %d). Synergy SA devient Energol SA.",
			                                  FormatNumeroHelper.numeroCTBToDisplay(idMap.get(noEtablissement)),
			                                  noEtablissement),
			                    evt.getErreurs().get(2).getMessage());

			Assert.assertEquals(String.format("Changement de raison sociale de l'établissement secondaire n°%s (civil: %d). Synergy Conception Aubonne SA devient Energol creation Aubonne SA.",
			                                  FormatNumeroHelper.numeroCTBToDisplay(idMap.get(noEtablissement2)),
			                                  noEtablissement2),
			                    evt.getErreurs().get(3).getMessage());

			Assert.assertEquals(String.format("Avertissement: l'enseigne Synergy Conception de l'établissement secondaire n°%s ne correspond pas à la nouvelle raison sociale Energol creation Aubonne SA. Veuillez corriger à la main si nécessaire.",
			                                  FormatNumeroHelper.numeroCTBToDisplay(idMap.get(noEtablissement2))),
			                    evt.getErreurs().get(4).getMessage());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testChgmtRaisonSocialeEnseigneCorrespond() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 1012021L;
		final Long noEtablissement = noEntrepriseCivile + 100;
		final Long noEtablissement2 = noEntrepriseCivile + 200;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999995");

				final MockEtablissementCivil etablissementSecondaire = MockEtablissementCivilFactory.addEtablissement(noEtablissement2, ent, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                                      FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                                      MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                                      StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				final MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) ent.getEtablissementPrincipal(date(2010, 6, 26)).getPayload();

				etablissementPrincipal.changeNom(date(2015, 7, 5), "Energol SA");
				addEntreprise(ent);

			}
		});

		// Création de l'entreprise

		final Map<Long, Long> idMap = doInNewTransactionAndSession(status -> {
			Map<Long, Long> map = new HashMap<>();

			Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

			Etablissement etablissement = addEtablissement();
			etablissement.setNumeroEtablissement(noEtablissement);
			etablissement.setEnseigne("Energol SA");

			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

			map.put(noEtablissement, etablissement.getNumero());

			Etablissement etablissementSecondaire = addEtablissement();
			etablissementSecondaire.setNumeroEtablissement(noEtablissement2);

			addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

			addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
			                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
			addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);

			map.put(noEtablissement2, etablissementSecondaire.getNumero());
			return map;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

			Assert.assertEquals(3, evt.getErreurs().size());
			Assert.assertEquals(String.format("Changement de raison sociale de l'établissement principal n°%s (civil: %d). Synergy SA devient Energol SA.",
			                                  FormatNumeroHelper.numeroCTBToDisplay(idMap.get(noEtablissement)),
			                                  noEtablissement),
			                    evt.getErreurs().get(2).getMessage());
			return null;
		});
	}


}
