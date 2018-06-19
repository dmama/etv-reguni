package ch.vd.unireg.evenement.organisation.casmetier.special;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseErreur;
import ch.vd.unireg.evenement.organisation.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.organisation.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.TypeTiers;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;

/**
 * @author Raphaël Marmier, 2016-04-15
 */
public class AutresCommunautesTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public AutresCommunautesTest() {
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
	public void testAutreCommunauteTraitementManuel() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Correia Pinto, Jardinage et Paysagisme", date(2010, 6, 26), null, FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.ENTREPRISE_INDIVIDUELLE, "CHE999999996" , null, null);
				addEntreprise(entreprise);

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
				final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2010, 6, 26), A_TRAITER);
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
				                             Assert.assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

				                             final List<EvenementEntrepriseErreur> erreurs = evt.getErreurs();
				                             Assert.assertNotNull(erreurs);
				                             Assert.assertEquals(2, erreurs.size());

				                             final EvenementEntrepriseErreur evtErreur0 = erreurs.get(0);
				                             Assert.assertEquals(String.format("Attention: le tiers n°%s identifié grâce aux attributs civils [Correia Pinto, Jardinage et Paysagisme, IDE: CHE-999.999.996] n'est pas une entreprise (%s) et sera ignoré. " +
						                                                               "Si nécessaire, un tiers Entreprise sera créé pour l'entreprise civile n°%d, en doublon du tiers n°%s (%s).",
				                                                               FormatNumeroHelper.numeroCTBToDisplay(tiersId),
				                                                               TypeTiers.AUTRE_COMMUNAUTE.getDescription(),
				                                                               noEntrepriseCivile,
				                                                               FormatNumeroHelper.numeroCTBToDisplay(tiersId),
				                                                               TypeTiers.AUTRE_COMMUNAUTE.getDescription()),
				                                                 evtErreur0.getMessage());

				                             final EvenementEntrepriseErreur evtErreur1 = evt.getErreurs().get(1);
				                             Assert.assertEquals(String.format("L'entreprise civile n°%d est une entreprise individuelle vaudoise. Pas de création.", noEntrepriseCivile),
				                                                 evtErreur1.getMessage());
				                             return null;
			                             }
		                             }
		);
	}
}
