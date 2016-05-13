package ch.vd.uniregctb.evenement.organisation.engine;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationErreur;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.TypeTiers;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2016-04-15
 */
public class AutresCommunautesProcessingTest extends AbstractEvenementOrganisationProcessorTest {

	public AutresCommunautesProcessingTest() {
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
	public void testAutreCommunauteTraitementManuel() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Correia Pinto, Jardinage et Paysagisme", date(2010, 6, 26), null, FormeLegale.N_0302_SOCIETE_SIMPLE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                           StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.ENTREPRISE_INDIVIDUELLE, null, null);
				addOrganisation(organisation);

			}
		});

		// L'autre communauté
		final Long tiersId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final AutreCommunaute autreCommunaute = addAutreCommunaute("Correia Pinto, Jardinage et Paysagisme");
				return autreCommunaute.getNumero();
			}
		});

		globalTiersIndexer.sync();

		Thread.sleep(1000);
		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(noEvenement, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2010, 6, 26), A_TRAITER);
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

				                             final EvenementOrganisationErreur evtErreur0 = evt.getErreurs().get(0);
				                             Assert.assertEquals(String.format("Attention: le tiers n°%s identifié grâce aux attributs civils [Correia Pinto, Jardinage et Paysagisme] n'est pas une entreprise (%s) et sera ignoré. " +
						                                                               "Si nécessaire, un tiers Entreprise sera créé pour l'organisation civile n°%d, en doublon du tiers n°%s (%s).",
				                                                               FormatNumeroHelper.numeroCTBToDisplay(tiersId),
				                                                               TypeTiers.AUTRE_COMMUNAUTE.getDescription(),
				                                                               noOrganisation,
				                                                               FormatNumeroHelper.numeroCTBToDisplay(tiersId),
				                                                               TypeTiers.AUTRE_COMMUNAUTE.getDescription()),
				                                                 evtErreur0.getMessage());
				                             final EvenementOrganisationErreur evtErreur1 = evt.getErreurs().get(1);
				                             Assert.assertEquals("Création automatique non prise en charge.",
				                                                 evtErreur1.getMessage());
				                             return null;
			                             }
		                             }
		);
	}
}
