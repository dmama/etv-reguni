package ch.vd.unireg.evenement.entreprise.engine;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-09-03
 */
public class TraitementManuelProcessorTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public TraitementManuelProcessorTest() {
		setWantIndexationTiers(true);
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Test(timeout = 10000L)
	public void testDeclenchementTraitementManuel() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEntrepriseCivile + 1000000, "Synergy SA", RegDate.get(2015, 6, 24), null,
						                                               FormeLegale.N_0113_FORME_JURIDIQUE_PARTICULIERE,
						                                               MockCommune.Lausanne));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 24), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());
			Assert.assertEquals("L'entreprise civile n°101202100, nom: 'Synergy SA', possède dans RCEnt une forme juridique non-acceptée par Unireg. Elle ne peut aboutir à la création d'un contribuable.", evt.getErreurs().get(1).getMessage());
			return null;
		});
	}
}
