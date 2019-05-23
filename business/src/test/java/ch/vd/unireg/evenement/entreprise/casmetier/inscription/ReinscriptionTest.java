package ch.vd.unireg.evenement.entreprise.casmetier.inscription;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.engine.AbstractEvenementEntrepriseCivileProcessorTest;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;
import ch.vd.unireg.type.TypeGenerationEtatEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class ReinscriptionTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public ReinscriptionTest() {
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
	public void testReinscriptionPM() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2012, 1, 26);

				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", RegDate.get(2010, 6, 27), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.RADIE, dateInscription,
						                                       StatusRegistreIDE.RADIE,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2012, 1, 26), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                          dateInscription, dateRadiation,
				                                                          dateInscription, dateRadiation));
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
				                                                         dateInscription, dateRadiation,            // dateRadiation, et pas null, car en fait la date persiste dans RCEnt après la réinscription...
				                                                         dateInscription, dateRadiation));          // dateRadiation, et pas null, car en fait la date persiste dans RCEnt après la réinscription...

				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) entreprise.getEtablissements().get(0).getDonneesRegistreIDE();
				// La réinscription n'a pas d'effet immédiat à l'IDE.
				//donneesRegistreIDE.changeStatus(RegDate.get(2012, 1, 26), StatusRegistreIDE.RADIE);
				//donneesRegistreIDE.changeStatus(RegDate.get(2015, 7, 5), StatusRegistreIDE.DEFINITIF);
				addEntreprise(entreprise);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addEtatEntreprise(entreprise, date(2010, 6, 24), TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(entreprise, date(2012, 1, 1), TypeEtatEntreprise.EN_LIQUIDATION, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addEtatEntreprise(entreprise, date(2013, 2, 1), TypeEtatEntreprise.RADIEE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);
			addRegimeFiscalVD(entreprise, RegDate.get(2010, 6, 25), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, RegDate.get(2010, 6, 25), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, RegDate.get(2010, 6, 25), MotifFor.DEBUT_EXPLOITATION, RegDate.get(2012, 1, 26), MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.DOMICILE);

			final Etablissement etablissement = addEtablissement(3000001L);
			addActiviteEconomique(entreprise, etablissement, date(2010, 6, 25), null, true);
			return entreprise;
		});

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
			Assert.assertEquals(EtatEvenementEntreprise.A_VERIFIER, evt.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			Assert.assertEquals(TypeEtatEntreprise.RADIEE_RC, entreprise.getEtatActuel().getType());

			Assert.assertEquals(0, entreprise.getForsFiscauxValidAt(RegDate.get(2015, 7, 5)).size());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			Assert.assertEquals(0, evtsFiscaux.size());

			Assert.assertEquals("Réinscription de l'entreprise au RC. Veuillez vérifier et faire le nécessaire à la main.",
			                    evt.getErreurs().get(2).getMessage());
			return null;
		});
	}
}
