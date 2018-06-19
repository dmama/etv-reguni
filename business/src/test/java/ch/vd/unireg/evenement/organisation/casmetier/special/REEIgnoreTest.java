package ch.vd.unireg.evenement.organisation.casmetier.special;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;

/**
 * @author Raphaël Marmier, 2016-07-04
 */
public class REEIgnoreTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public REEIgnoreTest() {
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
	public void testREEPureIgnoree() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Société de Jeunesse de Ballens", date(2016, 5, 20), null, FormeLegale.N_0109_ASSOCIATION,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
						                                       null, null, null, null, null);

				addEntreprise(entreprise);

			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.REE_NOUVELLE_INSCRIPTION, RegDate.get(2016, 5, 20), A_TRAITER);
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

				                             Assert.assertEquals(String.format("L'entreprise Société de Jeunesse de Ballens (civil: n°%d), domiciliée à Lausanne (VD), n'existe pas à l'IDE ni au RC. Pas de création automatique.", noEntrepriseCivile),
				                                                 evt.getErreurs().get(1).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testErreurManqueIDE() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Metafun SA", date(2016, 5, 20), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2016, 5, 15),
						                                       StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE,
						                                       null, null, null);

				addEntreprise(entreprise);

			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.REE_MUTATION, RegDate.get(2016, 5, 20), A_TRAITER);
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

				                             Assert.assertEquals(String.format("Numéro IDE manquant pour l'entreprise Metafun SA (civil: n°%d), domiciliée à Lausanne (VD), pourtant inscrite au RC. Impossible de continuer.", noEntrepriseCivile),
				                                                 evt.getErreurs().get(1).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	// SIFISC-19766 - Une forme juridique null est possible, dans le cas ou le RC et l'IDE ne sont pas impliqués.
	@Test(timeout = 10000L)
	public void testREEPureIgnoreePasDeFormeJuridique() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Société de Jeunesse de Ballens", date(2016, 5, 20), null, null,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
						                                       null, null, null, null, null);

				addEntreprise(entreprise);

			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.REE_NOUVELLE_INSCRIPTION, RegDate.get(2016, 5, 20), A_TRAITER);
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

				                             Assert.assertEquals(String.format("L'entreprise Société de Jeunesse de Ballens (civil: n°%d), domiciliée à Lausanne (VD), n'existe pas à l'IDE ni au RC. Pas de création automatique.", noEntrepriseCivile),
				                                                 evt.getErreurs().get(2).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

}
