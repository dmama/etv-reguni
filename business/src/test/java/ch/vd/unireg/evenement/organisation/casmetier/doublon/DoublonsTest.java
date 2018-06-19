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
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-11-09
 */
public class DoublonsTest extends AbstractEvenementEntrepriseCivileProcessorTest {

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
	public void testEvenementDoublonEntrepriseRemplacee() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 2100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final long noEntrepriseCivileRemplacante = 4096L;
		final Long noEtablissementRemplacant = noEntrepriseCivileRemplacante + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
				MockEtablissementCivil etablissement = (MockEtablissementCivil) entreprise.getEtablissements().iterator().next();
				addEntreprise(entreprise);

				MockEntrepriseCivile entrepriseRemplacante =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivileRemplacante, noEtablissementRemplacant, "Synergy Remplacante SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                       StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissementRemplacant = (MockEtablissementCivil) entrepriseRemplacante.getEtablissements().iterator().next();
				addEntreprise(entrepriseRemplacante);

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
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

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
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivileRemplacante);

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
				final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementEntreprise evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             Assert.assertEquals(String.format("Traitement manuel requis: Doublon d’entreprise civile à l'IDE. Cette entreprise n°%s (civil: %d) est remplacée par l'entreprise n°%s (civil: %d).",
				                                                               FormatNumeroHelper.numeroCTBToDisplay(entrepriseId),
				                                                               noEntrepriseCivile,
				                                                               FormatNumeroHelper.numeroCTBToDisplay(entrepriseRemplaçanteId),
				                                                               noEntrepriseCivileRemplacante),
				                                                 evt.getErreurs().get(1).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testEvenementDoublonEntrepriseVuDuRemplacant() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivileRemplaceePar = 2100L;
		final Long noEtablissementRemplaceePar = noEntrepriseCivileRemplaceePar + 1000000;
		final long noEntrepriseCivile = 4096L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile entrepriseRemplaceePar =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivileRemplaceePar, noEtablissementRemplaceePar, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
				MockEtablissementCivil etablissementRemplaceePar = (MockEtablissementCivil) entrepriseRemplaceePar.getEtablissements().iterator().next();
				addEntreprise(entrepriseRemplaceePar);

				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy Remplacante SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
						                                       StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissement = (MockEtablissementCivil) entreprise.getEtablissements().iterator().next();
				addEntreprise(entreprise);

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
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivileRemplaceePar);

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
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

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
				final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementEntreprise evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             Assert.assertEquals(String.format("Traitement manuel requis: Doublon d’entreprise civile à l'IDE. Cette entreprise n°%s (civil: %d) remplace l'entreprise n°%s (civil: %d).",
				                                                               FormatNumeroHelper.numeroCTBToDisplay(entrepriseId),
				                                                               noEntrepriseCivile,
				                                                               FormatNumeroHelper.numeroCTBToDisplay(entrepriseRemplaceeParId),
				                                                               noEntrepriseCivileRemplaceePar),
				                                                 evt.getErreurs().get(1).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testEvenementDoublonEtablissement() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final long noEtablissementRemplacant = 4096321L;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissement = (MockEtablissementCivil) entreprise.getEtablissements().iterator().next();
				etablissement.addIdeRemplacePar(RegDate.get(2015, 7, 5), null, noEtablissementRemplacant);
				addEntreprise(entreprise);
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
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

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
				final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementEntreprise evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

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
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final long noEtablissementRemplacant = 4096321L;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissement = (MockEtablissementCivil) entreprise.getEtablissements().iterator().next();
				etablissement.addIdeEnRemplacementDe(RegDate.get(2015, 7, 5), null, noEtablissementRemplacant);
				addEntreprise(entreprise);
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
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);

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
				final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementEntreprise evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

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
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;
		final long noEtablissementRemplacant = 4096321L;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissement = (MockEtablissementCivil) entreprise.getEtablissements().iterator().next();
				etablissement.addIdeRemplacePar(RegDate.get(2015, 7, 5), null, noEtablissementRemplacant);
				addEntreprise(entreprise);
			}
		});

		// Création de l'entreprise

		final Long tiersId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {

				return addEntrepriseConnueAuCivil(noEntrepriseCivile).getNumero();
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), A_TRAITER);
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementEntreprise evt = getUniqueEvent(noEvenement);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());

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
