package ch.vd.unireg.evenement.organisation.casmetier.information;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.engine.AbstractEvenementOrganisationProcessorTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeDeCapital;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementOrganisation;

import static ch.vd.unireg.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-10-16
 */
public class InformationComplementaireTest extends AbstractEvenementOrganisationProcessorTest {

	public InformationComplementaireTest() {
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
	public void testEvenementFailliteEtConcordatSansImpact() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996"));

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {

				return addEntrepriseConnueAuCivil(noOrganisation);
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, RegDate.get(2015, 6, 24), A_TRAITER);
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

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(1, evtsFiscaux.size());

				                             final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
				                             Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
					                             @Override
					                             public int compare(EvenementFiscal o1, EvenementFiscal o2) {
						                             return Long.compare(o1.getId(), o2.getId());
					                             }
				                             });

				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(0);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalInformationComplementaire efrf = (EvenementFiscalInformationComplementaire) ef;
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.AVIS_PREALABLE_OUVERTURE_FAILLITE, efrf.getType());
				                             }
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testEvenementFailliteEtConcordatAvecImpact() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                                        StatusRegistreIDE.DEFINITIF,
				                                                                        TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996"));

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {

				return addEntrepriseConnueAuCivil(noOrganisation);
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS, RegDate.get(2015, 6, 24), A_TRAITER);
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

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(1, evtsFiscaux.size());

				                             final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
				                             Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
					                             @Override
					                             public int compare(EvenementFiscal o1, EvenementFiscal o2) {
						                             return Long.compare(o1.getId(), o2.getId());
					                             }
				                             });

				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(0);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 6, 24), ef.getDateValeur());

					                             final EvenementFiscalInformationComplementaire efrf = (EvenementFiscalInformationComplementaire) ef;
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.PUBLICATION_FAILLITE_APPEL_CREANCIERS, efrf.getType());
				                             }
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testEvenementModificationDeCapital() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.addCapital(RegDate.get(2015, 7, 5), null, new Capital(RegDate.get(2015, 7, 5), null, TypeDeCapital.CAPITAL_ACTIONS, "CHF", BigDecimal.valueOf(100000), ""));
				addOrganisation(organisation);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {

				return addEntrepriseConnueAuCivil(noOrganisation);
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
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(1, evtsFiscaux.size());

				                             final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
				                             Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
					                             @Override
					                             public int compare(EvenementFiscal o1, EvenementFiscal o2) {
						                             return Long.compare(o1.getId(), o2.getId());
					                             }
				                             });

				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(0);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 7, 5), ef.getDateValeur());

					                             final EvenementFiscalInformationComplementaire efrf = (EvenementFiscalInformationComplementaire) ef;
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.MODIFICATION_CAPITAL, efrf.getType());
				                             }
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testEvenementModificationDesButs() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.addButs(RegDate.get(2015, 7, 5), null, "Nouveau but!");
				addOrganisation(organisation);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {

				return addEntrepriseConnueAuCivil(noOrganisation);
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
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(1, evtsFiscaux.size());

				                             final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
				                             Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
					                             @Override
					                             public int compare(EvenementFiscal o1, EvenementFiscal o2) {
						                             return Long.compare(o1.getId(), o2.getId());
					                             }
				                             });

				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(0);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 7, 5), ef.getDateValeur());

					                             final EvenementFiscalInformationComplementaire efrf = (EvenementFiscalInformationComplementaire) ef;
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.MODIFICATION_BUT, efrf.getType());
				                             }
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testEvenementModificationDesStatuts() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.addDateStatus(RegDate.get(2015, 7, 5), null, RegDate.get(2015, 7, 1));
				addOrganisation(organisation);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {

				return addEntrepriseConnueAuCivil(noOrganisation);
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
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             Assert.assertEquals(1, evtsFiscaux.size());

				                             final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
				                             Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
					                             @Override
					                             public int compare(EvenementFiscal o1, EvenementFiscal o2) {
						                             return Long.compare(o1.getId(), o2.getId());
					                             }
				                             });

				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(0);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 7, 5), ef.getDateValeur());

					                             final EvenementFiscalInformationComplementaire efrf = (EvenementFiscalInformationComplementaire) ef;
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.MODIFICATION_STATUTS, efrf.getType());
				                             }
				                             return null;
			                             }
		                             }
		);
	}
}
