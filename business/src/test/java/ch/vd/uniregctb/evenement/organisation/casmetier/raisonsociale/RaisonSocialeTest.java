package ch.vd.uniregctb.evenement.organisation.casmetier.raisonsociale;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockSiteOrganisationFactory;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.engine.AbstractEvenementOrganisationProcessorTest;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2016-05-18
 */
public class RaisonSocialeTest extends AbstractEvenementOrganisationProcessorTest {

	public RaisonSocialeTest() {
		setWantIndexationTiers(true);
	}

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Test(timeout = 10000L)
	public void testSimpleChgmtRaisonSociale() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 1012021L;
		final Long noSite = noOrganisation + 100;
		final Long noSite2 = noOrganisation + 200;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				final MockSiteOrganisation siteSecondaire = MockSiteOrganisationFactory.addSite(noSite2, org, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                 FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                 MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997");

				final MockSiteOrganisation sitePrincipal = (MockSiteOrganisation) org.getSitePrincipal(date(2010, 6, 26)).getPayload();

				sitePrincipal.changeNom(date(2015, 7, 5), "Energol SA");
				siteSecondaire.changeNom(date(2015, 7, 5), "Energol creation Aubonne SA");
				addOrganisation(org);

			}
		});

		// Création de l'entreprise

		final Map<Long, Long> idMap = doInNewTransactionAndSession(new TransactionCallback<Map<Long, Long>>() {
			@Override
			public Map<Long, Long> doInTransaction(TransactionStatus transactionStatus) {
				Map<Long, Long> idMap = new HashMap<>();

				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				idMap.put(noSite, etablissement.getNumero());

				Etablissement etablissementSecondaire = addEtablissement();
				etablissementSecondaire.setNumeroEtablissement(noSite2);

				addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne.getNoOFS(), MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);

				idMap.put(noSite2, etablissementSecondaire.getNumero());
				return idMap;
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
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

				                             Assert.assertEquals(String.format("Changement de raison sociale de l'établissement principal n°%s (civil: %d). Synergy SA devient Energol SA.",
				                                                               FormatNumeroHelper.numeroCTBToDisplay(idMap.get(noSite)),
				                                                               noSite),
				                                                 evt.getErreurs().get(2).getMessage());

				                             Assert.assertEquals(String.format("Changement de raison sociale de l'établissement secondaire n°%s (civil: %d). Synergy Conception Aubonne SA devient Energol creation Aubonne SA.",
				                                                 FormatNumeroHelper.numeroCTBToDisplay(idMap.get(noSite2)),
				                                                 noSite2),
				                                                 evt.getErreurs().get(3).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testChgmtRaisonSocialeAvecEnseigne() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 1012021L;
		final Long noSite = noOrganisation + 100;
		final Long noSite2 = noOrganisation + 200;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				final MockSiteOrganisation siteSecondaire = MockSiteOrganisationFactory.addSite(noSite2, org, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                 FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                 MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999997");

				final MockSiteOrganisation sitePrincipal = (MockSiteOrganisation) org.getSitePrincipal(date(2010, 6, 26)).getPayload();

				sitePrincipal.changeNom(date(2015, 7, 5), "Energol SA");
				siteSecondaire.changeNom(date(2015, 7, 5), "Energol creation Aubonne SA");
				addOrganisation(org);

			}
		});

		// Création de l'entreprise

		final Map<Long, Long> idMap = doInNewTransactionAndSession(new TransactionCallback<Map<Long, Long>>() {
			@Override
			public Map<Long, Long> doInTransaction(TransactionStatus transactionStatus) {
				Map<Long, Long> idMap = new HashMap<>();

				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				idMap.put(noSite, etablissement.getNumero());

				Etablissement etablissementSecondaire = addEtablissement();
				etablissementSecondaire.setNumeroEtablissement(noSite2);
				etablissementSecondaire.setEnseigne("Synergy Conception");

				addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne.getNoOFS(), MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);

				idMap.put(noSite2, etablissementSecondaire.getNumero());
				return idMap;
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
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
				                             Assert.assertEquals(EtatEvenementOrganisation.A_VERIFIER, evt.getEtat());

				                             Assert.assertEquals(String.format("Changement de raison sociale de l'établissement principal n°%s (civil: %d). Synergy SA devient Energol SA.",
				                                                               FormatNumeroHelper.numeroCTBToDisplay(idMap.get(noSite)),
				                                                               noSite),
				                                                 evt.getErreurs().get(2).getMessage());

				                             Assert.assertEquals(String.format("Changement de raison sociale de l'établissement secondaire n°%s (civil: %d). Synergy Conception Aubonne SA devient Energol creation Aubonne SA.",
				                                                 FormatNumeroHelper.numeroCTBToDisplay(idMap.get(noSite2)),
				                                                 noSite2),
				                                                 evt.getErreurs().get(3).getMessage());

				                             Assert.assertEquals(String.format("Avertissement: l'enseigne Synergy Conception de l'établissement secondaire n°%s ne correspond pas à la nouvelle raison sociale Energol creation Aubonne SA. Veuillez corriger à la main si nécessaire.",
				                                                 FormatNumeroHelper.numeroCTBToDisplay(idMap.get(noSite2))),
				                                                 evt.getErreurs().get(4).getMessage());

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testChgmtRaisonSocialeEnseigneCorrespond() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 1012021L;
		final Long noSite = noOrganisation + 100;
		final Long noSite2 = noOrganisation + 200;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999995");

				final MockSiteOrganisation siteSecondaire = MockSiteOrganisationFactory.addSite(noSite2, org, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
				                                                                                 FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				                                                                                 MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 23),
				                                                                                 StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");

				final MockSiteOrganisation sitePrincipal = (MockSiteOrganisation) org.getSitePrincipal(date(2010, 6, 26)).getPayload();

				sitePrincipal.changeNom(date(2015, 7, 5), "Energol SA");
				addOrganisation(org);

			}
		});

		// Création de l'entreprise

		final Map<Long, Long> idMap = doInNewTransactionAndSession(new TransactionCallback<Map<Long, Long>>() {
			@Override
			public Map<Long, Long> doInTransaction(TransactionStatus transactionStatus) {
				Map<Long, Long> idMap = new HashMap<>();

				Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noSite);
				etablissement.setEnseigne("Energol SA");

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 24), null, true);

				idMap.put(noSite, etablissement.getNumero());

				Etablissement etablissementSecondaire = addEtablissement();
				etablissementSecondaire.setNumeroEtablissement(noSite2);

				addActiviteEconomique(entreprise, etablissementSecondaire, date(2010, 6, 24), null, false);

				addRegimeFiscalVD(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2010, 6, 24), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, null, null,
				                MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, GenreImpot.BENEFICE_CAPITAL);
				addForSecondaire(entreprise, date(2010, 6, 24), MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne.getNoOFS(), MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);

				idMap.put(noSite2, etablissementSecondaire.getNumero());
				return idMap;
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 5), A_TRAITER);
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

				                             Assert.assertEquals(3, evt.getErreurs().size());
				                             Assert.assertEquals(String.format("Changement de raison sociale de l'établissement principal n°%s (civil: %d). Synergy SA devient Energol SA.",
				                                                               FormatNumeroHelper.numeroCTBToDisplay(idMap.get(noSite)),
				                                                               noSite),
				                                                 evt.getErreurs().get(2).getMessage());

				                             return null;
			                             }
		                             }
		);
	}


}
