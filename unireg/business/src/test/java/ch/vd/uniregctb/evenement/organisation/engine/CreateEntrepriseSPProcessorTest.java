package ch.vd.uniregctb.evenement.organisation.engine;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static ch.vd.uniregctb.type.EmetteurEvenementOrganisation.FOSC;
import static ch.vd.uniregctb.type.EmetteurEvenementOrganisation.IDE;
import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-09-03
 */
public class CreateEntrepriseSPProcessorTest extends AbstractEvenementOrganisationProcessorTest {

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Test(timeout = 10000L)
	public void testCreationSP() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(
						MockOrganisationFactory.createSimpleEntrepriseRC(noOrganisation, noOrganisation + 1000000, "Robert Alkan et autres", RegDate.get(2015, 6, 24), null, FormeLegale.N_0103_SOCIETE_NOM_COLLECIF,
						                                                 MockCommune.Lausanne));
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 24), A_TRAITER, FOSC, "abcdefgh");
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
				                             Assert.assertEquals(2, entreprise.getRegimesFiscaux().size());

				                             Assert.assertTrue(entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 25)).size() == 0);

				                             final List<DateRanged<Etablissement>> etablissements = tiersService.getEtablissementsForEntreprise(entreprise);
				                             Assert.assertEquals(1, etablissements.size());
				                             Assert.assertEquals(RegDate.get(2015, 6, 25), etablissements.get(0).getDateDebut());

				                             final Etablissement etablissement = etablissements.get(0).getPayload();
				                             Assert.assertEquals(RegDate.get(2015, 6, 25), etablissement.getDomiciles().iterator().next().getDateDebut());

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testCreationAPMNonRC() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				addOrganisation(
						MockOrganisationFactory.createOrganisation(noOrganisation, noOrganisation + 1000000, "Robert Alkan et autres", RegDate.get(2015, 6, 24), null, FormeLegale.N_0103_SOCIETE_NOM_COLLECIF,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), null, null, StatusRegistreIDE.DEFINITIF,
						                                           TypeOrganisationRegistreIDE.ASSOCIATION));
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event = createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 24), A_TRAITER, IDE, "abcdefgh");
				return hibernateTemplate.merge(event).getId();
			}
		});

		// Traitement synchrone de l'événement
		traiterEvenements(noOrganisation);

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.TRAITE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());
				                             Assert.assertEquals(2, entreprise.getRegimesFiscaux().size());

				                             Assert.assertTrue(entreprise.getForsFiscauxValidAt(RegDate.get(2015, 6, 25)).size() == 0);

				                             final List<DateRanged<Etablissement>> etablissements = tiersService.getEtablissementsForEntreprise(entreprise);
				                             Assert.assertEquals(1, etablissements.size());
				                             Assert.assertEquals(RegDate.get(2015, 6, 25), etablissements.get(0).getDateDebut());

				                             final Etablissement etablissement = etablissements.get(0).getPayload();
				                             Assert.assertEquals(RegDate.get(2015, 6, 25), etablissement.getDomiciles().iterator().next().getDateDebut());

				                             return null;
			                             }
		                             }
		);
	}
}
