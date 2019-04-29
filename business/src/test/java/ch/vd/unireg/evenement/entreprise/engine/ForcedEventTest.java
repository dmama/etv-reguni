package ch.vd.unireg.evenement.entreprise.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseBasicInfo;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.InscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_VERIFIER;
import static ch.vd.unireg.type.EtatEvenementEntreprise.EN_ERREUR;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class ForcedEventTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	public ForcedEventTest() {
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
	public void testForcerNeCreePasForEnvoieEvtFiscaux() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final RegDate dateInscriptionRC = date(2010, 6, 24);
				final RegDate dateRadiationRC = date(2012, 1, 26);
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.RADIE, dateInscriptionRC,
						                                       StatusRegistreIDE.RADIE,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2012, 1, 26), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                          dateInscriptionRC, null,
				                                                          dateInscriptionRC, dateRadiationRC));
				rc.changeInscription(date(2015, 5, 7), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
				                                                         dateInscriptionRC, null,
				                                                         dateInscriptionRC, null));

				final MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) entreprise.getEtablissements().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(RegDate.get(2012, 1, 26), StatusRegistreIDE.RADIE);
				donneesRegistreIDE.changeStatus(RegDate.get(2015, 7, 5), StatusRegistreIDE.DEFINITIF);
				addEntreprise(entreprise);
			}
		});

		// Création de l'entreprise

		final Entreprise entreprise = doInNewTransactionAndSession(status -> {
			final Entreprise ent = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addRegimeFiscalVD(ent, RegDate.get(2010, 6, 25), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(ent, RegDate.get(2010, 6, 25), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(ent, RegDate.get(2010, 6, 25), MotifFor.DEBUT_EXPLOITATION, RegDate.get(2012, 1, 1), MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.DOMICILE);
			return ent;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		EvenementEntreprise evt = doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event =
					createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT, RegDate.get(2015, 7, 5), EN_ERREUR);
			return hibernateTemplate.merge(event);
		});

		// Traitement synchrone de l'événement
		processor.forceEvenement(new EvenementEntrepriseBasicInfo(evt, evt.getNoEntrepriseCivile()));

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise eve = getUniqueEvent(noEvenement);
			Assert.assertNotNull(eve);
			Assert.assertEquals(EtatEvenementEntreprise.FORCE, eve.getEtat());

			final Entreprise ent = tiersDAO.getEntrepriseByNoEntrepriseCivile(eve.getNoEntrepriseCivile());

			// Si la réinscription devait être traitée et non forcée, on trouverait un for.
			Assert.assertEquals(0, ent.getForsFiscauxValidAt(RegDate.get(2015, 7, 5)).size());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			// Si la réinscription devait être traitée et non forcée, on trouverait deux événements de for en plus de
			// celui d'information.
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
				Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.APPEL_CREANCIERS_CONCORDAT, efrf.getType());
			}
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testForcerNeCreePasForAucunEvtFiscauxAEnvoyer() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2012, 1, 26);
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.RADIE, dateInscription,
						                                       StatusRegistreIDE.RADIE,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");

				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2012, 1, 26), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                          dateInscription, null,
				                                                          dateInscription, dateRadiation));
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
				                                                         dateInscription, null,
				                                                         dateInscription, null));
				MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) entreprise.getEtablissements().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(RegDate.get(2012, 1, 26), StatusRegistreIDE.RADIE);
				donneesRegistreIDE.changeStatus(RegDate.get(2015, 7, 5), StatusRegistreIDE.DEFINITIF);
				addEntreprise(entreprise);
			}
		});

		// Création de l'entreprise

		final Entreprise entreprise = doInNewTransactionAndSession(status -> {
			final Entreprise ent = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addRegimeFiscalVD(ent, RegDate.get(2010, 6, 25), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(ent, RegDate.get(2010, 6, 25), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(ent, RegDate.get(2010, 6, 25), MotifFor.DEBUT_EXPLOITATION, RegDate.get(2012, 1, 1), MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.DOMICILE);
			return ent;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		EvenementEntreprise evt = doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event =
					createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_AUTRE_MUTATION, RegDate.get(2015, 7, 5), EN_ERREUR);
			return hibernateTemplate.merge(event);
		});

		// Traitement synchrone de l'événement
		processor.forceEvenement(new EvenementEntrepriseBasicInfo(evt, evt.getNoEntrepriseCivile()));

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise eve = getUniqueEvent(noEvenement);
			Assert.assertNotNull(eve);
			Assert.assertEquals(EtatEvenementEntreprise.FORCE, eve.getEtat());

			final Entreprise ent = tiersDAO.getEntrepriseByNoEntrepriseCivile(eve.getNoEntrepriseCivile());

			// Si la réinscription devait être traitée et non forcée, on trouverait un for.
			Assert.assertEquals(0, ent.getForsFiscauxValidAt(RegDate.get(2015, 7, 5)).size());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			// Si la réinscription devait être traitée et non forcée, on trouverait deux événements de for.
			Assert.assertEquals(0, evtsFiscaux.size());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testForcerAVERIFIERNeRienFaire() throws Exception {

		// Mise en place service mock
		final Long noEntrepriseCivile = 101202100L;
		final Long noEtablissement = noEntrepriseCivile + 1000000;

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final RegDate dateInscription = date(2010, 6, 24);
				final RegDate dateRadiation = date(2012, 1, 26);
				final MockEntrepriseCivile entreprise =
						MockEntrepriseFactory.createEntreprise(noEntrepriseCivile, noEtablissement, "Synergy SA", date(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
						                                       TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.RADIE, dateInscription,
						                                       StatusRegistreIDE.RADIE,
						                                       TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", BigDecimal.valueOf(50000), "CHF");
				final MockDonneesRC rc = (MockDonneesRC) entreprise.getEtablissements().get(0).getDonneesRC();
				rc.changeInscription(date(2012, 1, 26), new InscriptionRC(StatusInscriptionRC.RADIE, null,
				                                                          dateInscription, null,
				                                                          dateInscription, dateRadiation));
				rc.changeInscription(date(2015, 7, 5), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
				                                                         dateInscription, null,
				                                                         dateInscription, null));
				MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) entreprise.getEtablissements().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(RegDate.get(2012, 1, 26), StatusRegistreIDE.RADIE);
				donneesRegistreIDE.changeStatus(RegDate.get(2015, 7, 5), StatusRegistreIDE.DEFINITIF);
				addEntreprise(entreprise);
			}
		});

		// Création de l'entreprise

		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntrepriseCivile);
			addRegimeFiscalVD(entreprise, RegDate.get(2010, 6, 25), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, RegDate.get(2010, 6, 25), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, RegDate.get(2010, 6, 25), MotifFor.DEBUT_EXPLOITATION, RegDate.get(2012, 1, 1), MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.DOMICILE);
			return null;
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		final EvenementEntreprise evt = doInNewTransactionAndSession(status -> {
			final EvenementEntreprise event =
					createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT, RegDate.get(2015, 7, 5), A_VERIFIER);
			return hibernateTemplate.merge(event);
		});

		processor.forceEvenement(new EvenementEntrepriseBasicInfo(evt, evt.getNoEntrepriseCivile()));


		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(status -> {
			final EvenementEntreprise eve = getUniqueEvent(noEvenement);
			Assert.assertNotNull(eve);
			Assert.assertEquals(EtatEvenementEntreprise.FORCE, eve.getEtat());

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(eve.getNoEntrepriseCivile());

			// Si la réinscription devait être traitée et non forcée, on trouverait un for.
			Assert.assertEquals(0, entreprise.getForsFiscauxValidAt(RegDate.get(2015, 7, 5)).size());

			// vérification des événements fiscaux
			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			Assert.assertNotNull(evtsFiscaux);
			// Si la réinscription devait être traitée et non forcée, on trouverait deux événements de for en plus de
			// celui d'information.
			Assert.assertEquals(0, evtsFiscaux.size());
			return null;
		});
	}
}
