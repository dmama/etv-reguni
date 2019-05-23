package ch.vd.unireg.evenement.entreprise.casmetier.information;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.unireg.interfaces.entreprise.data.Capital;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeDeCapital;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-10-16
 */
public class InformationComplementaireTest extends AbstractEvenementEntrepriseCivileProcessorTest {

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
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                     TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                     StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996"));

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> addEntrepriseConnueAuCivil(noEntrepriseCivile));

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AVIS_PREALABLE_OUVERTURE_FAILLITE, RegDate.get(2015, 6, 24), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

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
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementFailliteEtConcordatAvecImpact() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
				                                                     TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
				                                                     StatusRegistreIDE.DEFINITIF,
				                                                     TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996"));

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> addEntrepriseConnueAuCivil(noEntrepriseCivile));

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS, RegDate.get(2015, 6, 24), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

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
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementModificationDeCapital() throws Exception {

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
				MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.addCapital(RegDate.get(2015, 7, 5), null, new Capital(RegDate.get(2015, 7, 5), null, TypeDeCapital.CAPITAL_ACTIONS, "CHF", BigDecimal.valueOf(100000), ""));
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
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementModificationDesButs() throws Exception {

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
				MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.addButs(RegDate.get(2015, 7, 5), null, "Nouveau but!");
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
		});
	}

	@Test(timeout = 10000L)
	public void testEvenementModificationDesStatuts() throws Exception {

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
				MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.addDateStatus(RegDate.get(2015, 7, 5), null, RegDate.get(2015, 7, 1));
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
		});
	}
}
