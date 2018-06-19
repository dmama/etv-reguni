package ch.vd.unireg.evenement.organisation.casmetier.liquidation;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.PublicationBusiness;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeDeLiquidation;
import ch.vd.unireg.interfaces.organisation.data.TypeDePublicationBusiness;
import ch.vd.unireg.interfaces.organisation.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.organisation.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;

/**
 * @author Raphaël Marmier, 2016-02-19
 */
public class LiquidationTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public LiquidationTest() {
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
	public void testLiquidationEntreprise() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.EN_LIQUIDATION, date(2010, 6, 24),
						                                       StatusRegistreIDE.RADIE,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) entreprise.getEtablissements().get(0);
				etablissementPrincipal.addPublicationBusiness(new PublicationBusiness(date(2015, 7, 3), TypeDePublicationBusiness.FOSC_COMMUNICATION_DANS_FAILLITE, "123456", date(2015, 7, 5),
				                                                             "Blah blah publication FOSC",
				                                                             null, null, null, null));
				etablissementPrincipal.addPublicationBusiness(new PublicationBusiness(date(2015, 7, 3), TypeDePublicationBusiness.FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS, "123457", date(2015, 7, 5),
				                                                             "Blah blah publication FOSC",
				                                                             null, null, null, TypeDeLiquidation.SOCIETE_RESPONSABILITE_LIMITE));
				etablissementPrincipal.addPublicationBusiness(new PublicationBusiness(date(2015, 7, 3), TypeDePublicationBusiness.FOSC_COMMUNICATION_DANS_FAILLITE, "123458", date(2015, 7, 5),
				                                                             "Blah blah publication FOSC",
				                                                             null, null, null, null));
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {

				return addEntrepriseConnueAuCivil(noEntrepriseCivile);
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
				                             Assert.assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

				                             Assert.assertEquals("Une vérification, pouvant aboutir à un traitement manuel (processus complexe), est requise pour cause de Liquidation de l'entreprise.",
				                                                 evt.getErreurs().get(2).getMessage());
				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testPasDeLiquidationEntreprise() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.EN_LIQUIDATION, date(2010, 6, 24),
						                                       StatusRegistreIDE.RADIE,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) entreprise.getEtablissements().get(0);
				etablissementPrincipal.addPublicationBusiness(new PublicationBusiness(date(2015, 7, 3), TypeDePublicationBusiness.FOSC_COMMANDEMENT_DE_PAYER, "123456", date(2015, 7, 5),
				                                                             "Blah blah publication FOSC",null, null, null, null));
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {

				return addEntrepriseConnueAuCivil(noEntrepriseCivile);
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
				                             Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testPasNonPlusDeLiquidationEntreprise() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.EN_LIQUIDATION, date(2010, 6, 24),
						                                       StatusRegistreIDE.DEFINITIF,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				MockEtablissementCivil etablissementPrincipal = (MockEtablissementCivil) entreprise.getEtablissements().get(0);
				etablissementPrincipal.addPublicationBusiness(new PublicationBusiness(date(2015, 7, 5), TypeDePublicationBusiness.FOSC_PUBLICATION_FAILLITE_ET_APPEL_AUX_CREANCIERS, null, null, null,
				                                                             null, null, null, TypeDeLiquidation.SOCIETE_RESPONSABILITE_LIMITE));
				addEntreprise(entreprise);

			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {

				return addEntrepriseConnueAuCivil(noEntrepriseCivile);
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
				                             Assert.assertEquals(EtatEvenementEntreprise.TRAITE, evt.getEtat());

				                             return null;
			                             }
		                             }
		);
	}
}
