package ch.vd.unireg.evenement.entreprise.engine;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseErreur;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseService;
import ch.vd.unireg.evenement.entreprise.engine.translator.EvenementEntrepriseTranslatorImpl;
import ch.vd.unireg.evenement.entreprise.engine.translator.NiveauCappingEtat;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.identification.contribuable.IdentificationContribuableService;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.unireg.metier.MetierServicePM;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.rattrapage.appariement.AppariementService;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeEtatEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;
import ch.vd.unireg.type.TypeEvenementErreur;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test de quelques cas de capping en erreur
 */
public class CappingEnErrorTest extends AbstractEvenementEntrepriseCivileProcessorTest {

	private EvenementFiscalDAO evtFiscalDAO;

	public CappingEnErrorTest() {
		setWantIndexationTiers(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	@Override
	protected boolean buildProcessorOnSetup() {
		return false;
	}

	@Test(timeout = 10000L)
	public void testCreationPM() throws Exception {

		// Mise en place service mock
		final long noEntrepriseCivile = 101202100L;
		final long noEtablissementPrincipal = noEntrepriseCivile + 1000000L;

		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				addEntreprise(
						MockEntrepriseFactory.createSimpleEntrepriseRC(noEntrepriseCivile, noEtablissementPrincipal, "Synergy SA", RegDate.get(2015, 6, 27), null, FormeLegale.N_0106_SOCIETE_ANONYME,
						                                               MockCommune.Lausanne));
			}
		});

		// Création de l'événement
		final Long noEvenement = 12344321L;

		// Persistence événement
		doInNewTransactionAndSession(transactionStatus -> {
			final EvenementEntreprise event = createEvent(noEvenement, noEntrepriseCivile, TypeEvenementEntreprise.FOSC_NOUVELLE_ENTREPRISE, RegDate.get(2015, 6, 27), A_TRAITER);
			return hibernateTemplate.merge(event).getId();
		});

		// construction d'un translator cappé
		final EvenementEntrepriseTranslatorImpl translator = new EvenementEntrepriseTranslatorImpl();
		translator.setServiceEntreprise(serviceEntreprise);
		translator.setServiceInfrastructureService(getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService"));
		translator.setRegimeFiscalService(getBean(RegimeFiscalService.class, "regimeFiscalService"));
		translator.setTiersDAO(getBean(TiersDAO.class, "tiersDAO"));
		translator.setDataEventService(getBean(DataEventService.class, "dataEventService"));
		translator.setTiersService(getBean(TiersService.class, "tiersService"));
		translator.setMetierServicePM(getBean(MetierServicePM.class, "metierServicePM"));
		translator.setAdresseService(getBean(AdresseService.class, "adresseService"));
		translator.setIndexer(getBean(GlobalTiersIndexer.class, "globalTiersIndexer"));
		translator.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		translator.setEvenementFiscalService(getBean(EvenementFiscalService.class, "evenementFiscalService"));
		translator.setAppariementService(getBean(AppariementService.class, "appariementService"));
		translator.setEvenementEntrepriseService(getBean(EvenementEntrepriseService.class, "evtEntrepriseService"));
		translator.setParametreAppService(getBean(ParametreAppService.class, "parametreAppService"));
		translator.setUseOrganisationsOfNotice(false);
		translator.setCappingLevelProvider(() -> NiveauCappingEtat.EN_ERREUR);
		translator.setAudit(audit);
		translator.afterPropertiesSet();
		buildProcessor(translator);

		// Traitement synchrone de l'événement
		traiterEvenements(noEntrepriseCivile);

		// Vérification du presque-traitement de l'événement (= tous les messages sont enregistrés, mais au final rien n'est fait !)
		doInNewTransactionAndSession(status -> {

			final EvenementEntreprise evt = getUniqueEvent(noEvenement);
			assertNotNull(evt);
			assertEquals(EtatEvenementEntreprise.EN_ERREUR, evt.getEtat());
			assertEquals(11, evt.getErreurs().size());

			{
				final EvenementEntrepriseErreur erreur = evt.getErreurs().get(0);
				assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
				assertEquals("Aucune entreprise identifiée pour le numéro civil " + noEntrepriseCivile + " ou les attributs civils [Synergy SA, IDE: CHE-999.999.996].", erreur.getMessage());
			}
			{
				final EvenementEntrepriseErreur erreur = evt.getErreurs().get(1);
				assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
				assertEquals("Mutation : Création d'une entreprise vaudoise", erreur.getMessage());
			}

			final long idEntreprise;
			{
				final EvenementEntrepriseErreur erreur = evt.getErreurs().get(2);
				assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());

				final Pattern pattern = Pattern.compile("Entreprise créée avec le numéro de contribuable ([0-9.]+) pour l'entreprise civile n°" + noEntrepriseCivile);
				final Matcher matcher = pattern.matcher(erreur.getMessage());
				assertTrue(matcher.matches());

				idEntreprise = Long.parseLong(matcher.group(1).replaceAll("\\.", StringUtils.EMPTY));
			}
			{
				final EvenementEntrepriseErreur erreur = evt.getErreurs().get(3);
				assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
				assertEquals(String.format("Réglage de l'état: %s pour l'entreprise n°%s (civil: %d).", TypeEtatEntreprise.INSCRITE_RC.getLibelle(), FormatNumeroHelper.numeroCTBToDisplay(idEntreprise), noEntrepriseCivile), erreur.getMessage());
			}

			final long idEtablissementPrincipal;
			{
				final EvenementEntrepriseErreur erreur = evt.getErreurs().get(4);
				assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());

				final Pattern pattern = Pattern.compile("Etablissement principal créé avec le numéro ([0-9.]+) pour l'établissement civil " + noEtablissementPrincipal + Pattern.quote(", domicile Lausanne (VD) (ofs: 5586), à partir du 24.06.2015"));
				final Matcher matcher = pattern.matcher(erreur.getMessage());
				assertTrue(matcher.matches());

				idEtablissementPrincipal = Long.parseLong(matcher.group(1).replaceAll("\\.", StringUtils.EMPTY));
			}
			{
				final EvenementEntrepriseErreur erreur = evt.getErreurs().get(5);
				assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
				assertEquals("Application de la surcharge civile entre le 24.06.2015 et le 26.06.2015 avec les valeurs du 27.06.2015", erreur.getMessage());
			}
			{
				final EvenementEntrepriseErreur erreur = evt.getErreurs().get(6);
				assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
				assertEquals("Régimes fiscaux par défaut [01 - Ordinaire] VD et CH ouverts pour l'entreprise n°" + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + " (civil: " + noEntrepriseCivile + ")", erreur.getMessage());
			}
			{
				final EvenementEntrepriseErreur erreur = evt.getErreurs().get(7);
				assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
				assertEquals(
						"Ouverture d'un for fiscal principal à Lausanne (VD) à partir du 25.06.2015, motif ouverture Début d'activité, rattachement DOMICILE, pour l'entreprise n°" + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + " (civil: " +
								noEntrepriseCivile + ").", erreur.getMessage());
			}
			{
				final EvenementEntrepriseErreur erreur = evt.getErreurs().get(8);
				assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
				assertEquals("Réglage de la date de début du premier exercice commercial au 01.01.2015", erreur.getMessage());
			}
			{
				final EvenementEntrepriseErreur erreur = evt.getErreurs().get(9);
				assertEquals(TypeEvenementErreur.SUIVI, erreur.getType());
				assertEquals("Bouclement créé avec une périodicité de 12 mois à partir du 31.12.2015", erreur.getMessage());
			}
			{
				final EvenementEntrepriseErreur erreur = evt.getErreurs().get(10);
				assertEquals(TypeEvenementErreur.ERROR, erreur.getType());
				assertEquals("Evénement explicitement placé 'en erreur' par configuration applicative. Toutes les modifications apportées pendant le traitement sont abandonnées.", erreur.getMessage());
				assertNull(erreur.getCallstack());
			}

			// mais en fait, rien n'a été créé...

			final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(evt.getNoEntrepriseCivile());
			assertNull(entreprise);

			final Entreprise entrepriseFiscale = (Entreprise) tiersDAO.get(idEntreprise);
			assertNull(entrepriseFiscale);
			final Etablissement etablissementFiscal = (Etablissement) tiersDAO.get(idEtablissementPrincipal);
			assertNull(etablissementFiscal);

			final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
			assertNotNull(evtsFiscaux);
			assertEquals(0, evtsFiscaux.size());

			return null;
		});
	}

}
