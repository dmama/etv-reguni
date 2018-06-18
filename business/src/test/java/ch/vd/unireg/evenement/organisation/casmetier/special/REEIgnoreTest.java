package ch.vd.unireg.evenement.organisation.casmetier.special;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.engine.AbstractEvenementOrganisationProcessorTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementOrganisation;

import static ch.vd.unireg.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2016-07-04
 */
public class REEIgnoreTest extends AbstractEvenementOrganisationProcessorTest {

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
		final Long noOrganisation = 101202100L;
		final Long noEtablissement = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noEtablissement, "Société de Jeunesse de Ballens", date(2016, 5, 20), null, FormeLegale.N_0109_ASSOCIATION,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
						                                           null, null, null, null, null);

				addOrganisation(organisation);

			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.REE_NOUVELLE_INSCRIPTION, RegDate.get(2016, 5, 20), A_TRAITER);
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

				                             Assert.assertEquals(String.format("L'organisation Société de Jeunesse de Ballens (civil: n°%d), domiciliée à Lausanne (VD), n'existe pas à l'IDE ni au RC. Pas de création automatique.", noOrganisation),
				                                                 evt.getErreurs().get(1).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testErreurManqueIDE() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noEtablissement = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noEtablissement, "Metafun SA", date(2016, 5, 20), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2016, 5, 15),
						                                           StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE,
						                                           null, null, null);

				addOrganisation(organisation);

			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.REE_MUTATION, RegDate.get(2016, 5, 20), A_TRAITER);
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

				                             Assert.assertEquals(String.format("Numéro IDE manquant pour l'organisation Metafun SA (civil: n°%d), domiciliée à Lausanne (VD), pourtant inscrite au RC. Impossible de continuer.", noOrganisation),
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
		final Long noOrganisation = 101202100L;
		final Long noEtablissement = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noEtablissement, "Société de Jeunesse de Ballens", date(2016, 5, 20), null, null,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null,
						                                           null, null, null, null, null);

				addOrganisation(organisation);

			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.REE_NOUVELLE_INSCRIPTION, RegDate.get(2016, 5, 20), A_TRAITER);
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

				                             Assert.assertEquals(String.format("L'organisation Société de Jeunesse de Ballens (civil: n°%d), domiciliée à Lausanne (VD), n'existe pas à l'IDE ni au RC. Pas de création automatique.", noOrganisation),
				                                                 evt.getErreurs().get(2).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

}
