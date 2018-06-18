package ch.vd.unireg.evenement.organisation.casmetier.doublon;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.engine.AbstractEvenementOrganisationProcessorTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementOrganisation;

import static ch.vd.unireg.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-11-09
 */
public class DoublonsTest extends AbstractEvenementOrganisationProcessorTest {

	public DoublonsTest() {
		setWantIndexationTiers(true);
	}

	private EvenementFiscalDAO evtFiscalDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Test(timeout = 10000L)
	public void testEvenementDoublonOrganisationRemplacee() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 2100L;
		final Long noEtablissement = noOrganisation + 1000000;
		final long noOrganisationRemplacante = 4096L;
		final Long noEtablissementRemplacant = noOrganisationRemplacante + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
				MockEtablissementCivil etablissement = (MockEtablissementCivil) organisation.getEtablissements().iterator().next();
				addOrganisation(organisation);

				MockOrganisation organisationRemplacante =
						MockOrganisationFactory.createOrganisation(noOrganisationRemplacante, noEtablissementRemplacant, "Synergy Remplacante SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                           StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissementRemplacant = (MockEtablissementCivil) organisationRemplacante.getEtablissements().iterator().next();
				addOrganisation(organisationRemplacante);

				etablissement.addIdeRemplacePar(date(2015, 7, 5), null, noEtablissementRemplacant);
				//etablissementRemplacant.addIdeEnRemplacementDe(date(2015, 7, 5), null, noEtablissement);
			}
		});

		// Création de l'entreprise remplacée

		final Long etablissementId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noEtablissement);

				return etablissement.getNumero();
			}
		});
		final Long entrepriseId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				final Etablissement etablissement = (Etablissement) tiersDAO.get(etablissementId);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 26), null, true);
				return entreprise.getNumero();
			}
		});

		// Création de l'entreprise remplaçante

		final Long etablissementRemplaçantId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noEtablissementRemplacant);

				return etablissement.getNumero();
			}
		});
		final Long entrepriseRemplaçanteId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisationRemplacante);

				final Etablissement etablissement = (Etablissement) tiersDAO.get(etablissementRemplaçantId);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 26), null, true);
				return entreprise.getNumero();
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER);
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

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             Assert.assertEquals(String.format("Traitement manuel requis: Doublon d’organisation à l'IDE. Cette entreprise n°%s (civil: %d) est remplacée par l'entreprise n°%s (civil: %d).",
				                                                               FormatNumeroHelper.numeroCTBToDisplay(entrepriseId),
				                                                               noOrganisation,
				                                                               FormatNumeroHelper.numeroCTBToDisplay(entrepriseRemplaçanteId),
				                                                               noOrganisationRemplacante),
				                                                 evt.getErreurs().get(1).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testEvenementDoublonOrganisationVuDuRemplacant() throws Exception {

		// Mise en place service mock
		final Long noOrganisationRemplaceePar = 2100L;
		final Long noEtablissementRemplaceePar = noOrganisationRemplaceePar + 1000000;
		final long noOrganisation = 4096L;
		final Long noEtablissement = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisationRemplaceePar =
						MockOrganisationFactory.createOrganisation(noOrganisationRemplaceePar, noEtablissementRemplaceePar, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
				MockEtablissementCivil etablissementRemplaceePar = (MockEtablissementCivil) organisationRemplaceePar.getEtablissements().iterator().next();
				addOrganisation(organisationRemplaceePar);

				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noEtablissement, "Synergy Remplacante SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                           StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissement = (MockEtablissementCivil) organisation.getEtablissements().iterator().next();
				addOrganisation(organisation);

				//etablissement.addIdeRemplacePar(date(2015, 7, 5), null, noEtablissementRemplacant);
				etablissement.addIdeEnRemplacementDe(date(2015, 7, 5), null, noEtablissementRemplaceePar);
			}
		});

		// Création de l'entreprise remplacée

		final Long etablissementRemplaceeParId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noEtablissementRemplaceePar);

				return etablissement.getNumero();
			}
		});
		final Long entrepriseRemplaceeParId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisationRemplaceePar);

				final Etablissement etablissement = (Etablissement) tiersDAO.get(etablissementRemplaceeParId);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 26), null, true);
				return entreprise.getNumero();
			}
		});

		// Création de l'entreprise remplaçante

		final Long etablissementId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noEtablissement);

				return etablissement.getNumero();
			}
		});
		final Long entrepriseId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				final Etablissement etablissement = (Etablissement) tiersDAO.get(etablissementId);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 26), null, true);
				return entreprise.getNumero();
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER);
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

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             Assert.assertEquals(String.format("Traitement manuel requis: Doublon d’organisation à l'IDE. Cette entreprise n°%s (civil: %d) remplace l'entreprise n°%s (civil: %d).",
				                                                               FormatNumeroHelper.numeroCTBToDisplay(entrepriseId),
				                                                               noOrganisation,
				                                                               FormatNumeroHelper.numeroCTBToDisplay(entrepriseRemplaceeParId),
				                                                               noOrganisationRemplaceePar),
				                                                 evt.getErreurs().get(1).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testEvenementDoublonEtablissement() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noEtablissement = noOrganisation + 1000000;
		final long noEtablissementRemplacant = 4096321L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissement = (MockEtablissementCivil) organisation.getEtablissements().iterator().next();
				etablissement.addIdeRemplacePar(RegDate.get(2015, 7, 5), null, noEtablissementRemplacant);
				addOrganisation(organisation);
			}
		});

		// Création de l'entreprise

		final Long etablissementId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noEtablissement);

				return etablissement.getNumero();
			}
		});
		final Long entrepriseId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				final Etablissement etablissement = (Etablissement) tiersDAO.get(etablissementId);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 26), null, true);
				return entreprise.getNumero();
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER);
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

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             Assert.assertEquals(String.format("Traitement manuel requis: Doublon d'établissement civil à l'IDE. L'établissement n°%s (civil: %d) est remplacé par l'établissement non encore connue d'Unireg (civil: %d).",
				                                                               FormatNumeroHelper.numeroCTBToDisplay(etablissementId),
				                                                               noEtablissement, noEtablissementRemplacant),
				                                                 evt.getErreurs().get(2).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testEvenementDoublonEtablissementVuDuRemplacant() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noEtablissement = noOrganisation + 1000000;
		final long noEtablissementRemplacant = 4096321L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissement = (MockEtablissementCivil) organisation.getEtablissements().iterator().next();
				etablissement.addIdeEnRemplacementDe(RegDate.get(2015, 7, 5), null, noEtablissementRemplacant);
				addOrganisation(organisation);
			}
		});

		// Création de l'entreprise

		final Long etablissementId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final Etablissement etablissement = addEtablissement();
				etablissement.setNumeroEtablissement(noEtablissement);

				return etablissement.getNumero();
			}
		});
		final Long entrepriseId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);

				final Etablissement etablissement = (Etablissement) tiersDAO.get(etablissementId);

				addActiviteEconomique(entreprise, etablissement, date(2010, 6, 26), null, true);
				return entreprise.getNumero();
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER);
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

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             Assert.assertEquals(String.format("Traitement manuel requis: Doublon d'établissement civil à l'IDE. L'établissement n°%s (civil: %d) remplace l'établissement non encore connue d'Unireg (civil: %d).",
				                                                               FormatNumeroHelper.numeroCTBToDisplay(etablissementId),
				                                                               noEtablissement, noEtablissementRemplacant),
				                                                 evt.getErreurs().get(2).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testEvenementDoublonEtablissementInconnu() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noEtablissement = noOrganisation + 1000000;
		final long noEtablissementRemplacant = 4096321L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissement = (MockEtablissementCivil) organisation.getEtablissements().iterator().next();
				etablissement.addIdeRemplacePar(RegDate.get(2015, 7, 5), null, noEtablissementRemplacant);
				addOrganisation(organisation);
			}
		});

		// Création de l'entreprise

		final Long tiersId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				return addEntrepriseConnueAuCivil(noOrganisation).getNumero();
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER);
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

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             Assert.assertEquals(String.format("Traitement manuel requis: Doublon d'établissement civil à l'IDE. L'établissement non encore connue d'Unireg (civil: %d) est remplacé par l'établissement non encore connue d'Unireg (civil: %d).", noEtablissement, noEtablissementRemplacant),
				                                                 evt.getErreurs().get(2).getMessage());
				                             return null;
			                             }
		                             }
		);
	}
}
