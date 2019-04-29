package ch.vd.unireg.foncier;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalServiceImpl;
import ch.vd.unireg.documentfiscal.EtatAutreDocumentFiscalRappele;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypeRapprochementRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RappelFormulairesDemandeDegrevementICIProcessorTest extends BusinessTest {

	private RappelFormulairesDemandeDegrevementICIProcessor processor;
	private DelaisService delaisService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final ParametreAppService paramAppService = getBean(ParametreAppService.class, "parametreAppService");
		final AutreDocumentFiscalServiceImpl autreDocFiscalService = getBean(AutreDocumentFiscalServiceImpl.class, "autreDocumentFiscalService");
		final RegistreFoncierService registreFoncierService = getBean(RegistreFoncierService.class, "serviceRF");
		delaisService = getBean(DelaisService.class, "delaisService");
		processor = new RappelFormulairesDemandeDegrevementICIProcessor(paramAppService, transactionManager, autreDocFiscalService, hibernateTemplate, registreFoncierService, delaisService);
	}

	/**
	 * Simple vérification syntaxique de la requête...
	 */
	@Test
	public void testPopulationInspecteeSurBaseVide() throws Exception {

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// personne
			}
		});

		// mise en place fiscale -> rien...

		// lancement de la requête (simple vérification syntaxique, du coup)
		final List<Long> ids = processor.fetchIdsFormulaires(RegDate.get());
		Assert.assertNotNull(ids);
		Assert.assertEquals(0, ids.size());
	}

	@Test
	public void testPopulationInspectee() throws Exception {

		final RegDate dateDebut = date(2009, 4, 1);
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// rien
			}
		});

		final class Ids {
			long idDemandeEmiseNonEchue;
			long idDemandeEmiseEchueSaufDelaiAdministratif;
			long idDemandeEmiseEchueAvecDelaiAdministratif;
			long idDemandeDejaRappelee;
			long idDemandeRetournee;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateDebut, null, "Punkt GmbH");
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aigle);

			final PersonneMoraleRF pmrf = addPersonneMoraleRF("Punkt GmbH", null, "7w457gfhfgfgwre", 845151564684L, null);
			addRapprochementRF(entreprise, pmrf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(12321, MockCommune.Lausanne.getNomOfficiel(), MockCommune.Lausanne.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("dfgszbsl3342", null, commune, 84165);
			addDroitPersonneMoraleRF(dateDebut, dateDebut, null, null, "Achat", null, "4637210tre", "4637210trd", new IdentifiantAffaireRF(784, "SSP"), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmrf, immeuble, null);

			final DemandeDegrevementICI retournee = addDemandeDegrevementICI(entreprise, 2010, immeuble);
			addDelaiAutreDocumentFiscal(retournee, date(2009, 4, 1), date(2009, 4, 30), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(retournee, date(2009, 4, 1));
			addEtatAutreDocumentFiscalRetourne(retournee, date(2009, 4, 24));

			final DemandeDegrevementICI echueSaufDelaiAdministratif = addDemandeDegrevementICI(entreprise, 2011, immeuble);
			addDelaiAutreDocumentFiscal(echueSaufDelaiAdministratif, dateTraitement.addDays(-33), dateTraitement.addDays(-3), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(echueSaufDelaiAdministratif, dateTraitement.addDays(-33));

			final DemandeDegrevementICI echueAvecDelaiAdministratif = addDemandeDegrevementICI(entreprise, 2012, immeuble);
			addDelaiAutreDocumentFiscal(echueAvecDelaiAdministratif, dateTraitement.addDays(-50), dateTraitement.addDays(-20), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(echueAvecDelaiAdministratif, dateTraitement.addDays(-50));
			final DemandeDegrevementICI dejaRappelee = addDemandeDegrevementICI(entreprise, 2013, immeuble);
			addDelaiAutreDocumentFiscal(dejaRappelee, dateTraitement.addDays(-60), dateTraitement.addDays(-30), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(dejaRappelee, dateTraitement.addDays(-60));
			addEtatAutreDocumentFiscalRappele(dejaRappelee, dateTraitement.addDays(-10));

			final DemandeDegrevementICI emiseNonEchue = addDemandeDegrevementICI(entreprise, 2014, immeuble);
			addDelaiAutreDocumentFiscal(emiseNonEchue, dateTraitement.addDays(-15), dateTraitement.addDays(+15), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(emiseNonEchue, dateTraitement.addDays(-15));

			final Ids res = new Ids();
			res.idDemandeDejaRappelee = dejaRappelee.getId();
			res.idDemandeEmiseEchueAvecDelaiAdministratif = echueAvecDelaiAdministratif.getId();
			res.idDemandeEmiseEchueSaufDelaiAdministratif = echueSaufDelaiAdministratif.getId();
			res.idDemandeEmiseNonEchue = emiseNonEchue.getId();
			res.idDemandeRetournee = retournee.getId();
			return res;
		});

		// requête de récupération des identidfiants de demandes
		final List<Long> idsDemandes = processor.fetchIdsFormulaires(dateTraitement);
		Assert.assertEquals(Arrays.asList(ids.idDemandeEmiseEchueSaufDelaiAdministratif, ids.idDemandeEmiseEchueAvecDelaiAdministratif), idsDemandes);
	}

	@Test
	public void testRun() throws Exception {

		final RegDate dateDebut = date(2009, 4, 1);
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// rien
			}
		});

		final class Ids {
			long idEntreprise;
			long idDemandeEmiseNonEchue;
			long idDemandeEmiseEchueSaufDelaiAdministratif;
			long idDemandeEmiseEchueAvecDelaiAdministratif;
			long idDemandeDejaRappelee;
			long idDemandeRetournee;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateDebut, null, "Punkt GmbH");
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aigle);

			final PersonneMoraleRF pmrf = addPersonneMoraleRF("Punkt GmbH", null, "7w457gfhfgfgwre", 845151564684L, null);
			addRapprochementRF(entreprise, pmrf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(12321, MockCommune.Lausanne.getNomOfficiel(), MockCommune.Lausanne.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("dfgszbsl3342", null, commune, 84165);
			addDroitPersonneMoraleRF(dateDebut, dateDebut, null, null, "Achat", null, "4637210tre", "4637210trd", new IdentifiantAffaireRF(784, "SSP"), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmrf, immeuble, null);

			final DemandeDegrevementICI retournee = addDemandeDegrevementICI(entreprise, 2010, immeuble);
			addDelaiAutreDocumentFiscal(retournee, date(2009, 4, 1), date(2009, 4, 30), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(retournee, date(2009, 4, 1));
			addEtatAutreDocumentFiscalRetourne(retournee, date(2009, 4, 24));

			final DemandeDegrevementICI echueSaufDelaiAdministratif = addDemandeDegrevementICI(entreprise, 2011, immeuble);
			addDelaiAutreDocumentFiscal(echueSaufDelaiAdministratif, dateTraitement.addDays(-33), dateTraitement.addDays(-3), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(echueSaufDelaiAdministratif, dateTraitement.addDays(-33));

			final DemandeDegrevementICI echueAvecDelaiAdministratif = addDemandeDegrevementICI(entreprise, 2012, immeuble);
			addDelaiAutreDocumentFiscal(echueAvecDelaiAdministratif, dateTraitement.addDays(-50), dateTraitement.addDays(-20), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(echueAvecDelaiAdministratif, dateTraitement.addDays(-50));

			final DemandeDegrevementICI dejaRappelee = addDemandeDegrevementICI(entreprise, 2013, immeuble);
			addDelaiAutreDocumentFiscal(dejaRappelee, dateTraitement.addDays(-60), dateTraitement.addDays(-30), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(dejaRappelee, dateTraitement.addDays(-60));
			addEtatAutreDocumentFiscalRappele(dejaRappelee, dateTraitement.addDays(-10));

			final DemandeDegrevementICI emiseNonEchue = addDemandeDegrevementICI(entreprise, 2014, immeuble);
			addDelaiAutreDocumentFiscal(emiseNonEchue, dateTraitement.addDays(-15), dateTraitement.addDays(+15), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(emiseNonEchue, dateTraitement.addDays(-15));

			final Ids res = new Ids();
			res.idEntreprise = entreprise.getNumero();
			res.idDemandeDejaRappelee = dejaRappelee.getId();
			res.idDemandeEmiseEchueAvecDelaiAdministratif = echueAvecDelaiAdministratif.getId();
			res.idDemandeEmiseEchueSaufDelaiAdministratif = echueSaufDelaiAdministratif.getId();
			res.idDemandeEmiseNonEchue = emiseNonEchue.getId();
			res.idDemandeRetournee = retournee.getId();
			return res;
		});

		// lancement du job
		final RappelFormulairesDemandeDegrevementICIResults results = processor.run(dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getIgnores().size());
		Assert.assertEquals(1, results.getTraites().size());

		// ignorée car délai administratif non-échu
		{
			final RappelFormulairesDemandeDegrevementICIResults.Ignore ignoree = results.getIgnores().get(0);
			Assert.assertNotNull(ignoree);
			Assert.assertEquals((Long) ids.idEntreprise, ignoree.noCtb);
			Assert.assertEquals("Lausanne", ignoree.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ignoree.ofsCommune);
			Assert.assertEquals(ids.idDemandeEmiseEchueSaufDelaiAdministratif, ignoree.idFormulaire);
			Assert.assertEquals(dateTraitement.addDays(-33), ignoree.dateEnvoi);
			Assert.assertEquals((Integer) 84165, ignoree.noParcelle);
			Assert.assertNull(ignoree.index1);
			Assert.assertNull(ignoree.index2);
			Assert.assertNull(ignoree.index3);
			Assert.assertEquals((Integer) 2011, ignoree.periodeFiscale);
			Assert.assertEquals(RappelFormulairesDemandeDegrevementICIResults.RaisonIgnorement.DELAI_ADMINISTRATIF_NON_ECHU, ignoree.raison);
		}
		// rappelée car délai administratif échu
		{
			final RappelFormulairesDemandeDegrevementICIResults.Traite traitee = results.getTraites().get(0);
			Assert.assertNotNull(traitee);
			Assert.assertEquals((Long) ids.idEntreprise, traitee.noCtb);
			Assert.assertEquals("Lausanne", traitee.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), traitee.ofsCommune);
			Assert.assertEquals(ids.idDemandeEmiseEchueAvecDelaiAdministratif, traitee.idFormulaire);
			Assert.assertEquals(dateTraitement.addDays(-50), traitee.dateEnvoiFormulaire);
			Assert.assertEquals((Integer) 84165, traitee.noParcelle);
			Assert.assertNull(traitee.index1);
			Assert.assertNull(traitee.index2);
			Assert.assertNull(traitee.index3);
			Assert.assertEquals((Integer) 2012, traitee.periodeFiscale);
		}

		// et en base
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
			Assert.assertNotNull(entreprise);

			final List<DemandeDegrevementICI> demandes = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true).stream()
					.sorted(Comparator.comparingLong(DemandeDegrevementICI::getId))
					.collect(Collectors.toList());
			Assert.assertEquals(5, demandes.size());
			{
				// la retournée
				final DemandeDegrevementICI demande = demandes.get(0);
				Assert.assertNotNull(demande);
				Assert.assertEquals(date(2009, 4, 1), demande.getDateEnvoi());
				Assert.assertEquals(date(2009, 4, 30), demande.getDelaiRetour());
				Assert.assertEquals(date(2009, 4, 24), demande.getDateRetour());
				Assert.assertNull(demande.getDateRappel());
				Assert.assertNull(demande.getCleArchivageRappel());
				Assert.assertEquals((Integer) 2010, demande.getPeriodeFiscale());
			}
			{
				// la échue sauf délai administratif
				final DemandeDegrevementICI demande = demandes.get(1);
				Assert.assertNotNull(demande);
				Assert.assertEquals(dateTraitement.addDays(-33), demande.getDateEnvoi());
				Assert.assertEquals(dateTraitement.addDays(-3), demande.getDelaiRetour());
				Assert.assertNull(demande.getDateRetour());
				Assert.assertNull(demande.getDateRappel());
				Assert.assertNull(demande.getCleArchivageRappel());
				Assert.assertEquals((Integer) 2011, demande.getPeriodeFiscale());
			}
			{
				// la échue avec délai admimistratif : celle-ci doit être modifiée !!!
				final DemandeDegrevementICI demande = demandes.get(2);
				Assert.assertNotNull(demande);
				Assert.assertEquals(dateTraitement.addDays(-50), demande.getDateEnvoi());
				Assert.assertEquals(dateTraitement.addDays(-20), demande.getDelaiRetour());
				Assert.assertNull(demande.getDateRetour());
				Assert.assertEquals(dateTraitement, demande.getDateRappel());
				Assert.assertNull(demande.getCleArchivageRappel());                 // c'est la partie Editique qui assigne cette valeur, mais elle est mockée dans les tests UT
				Assert.assertEquals((Integer) 2012, demande.getPeriodeFiscale());

				// [SIFISC-28193] l'état rappelé doit bien posséder les deux dates de traitement et d'envoi
				final EtatAutreDocumentFiscalRappele rappel = (EtatAutreDocumentFiscalRappele) demande.getDernierEtatOfType(TypeEtatDocumentFiscal.RAPPELE);
				assertNotNull(rappel);
				assertEquals(dateTraitement, rappel.getDateObtention());
				assertEquals(delaisService.getDateFinDelaiCadevImpressionDemandeDegrevementICI(dateTraitement), rappel.getDateEnvoiCourrier());

			}
			{
				// la déjà rappelée
				final DemandeDegrevementICI demande = demandes.get(3);
				Assert.assertNotNull(demande);
				Assert.assertEquals(dateTraitement.addDays(-60), demande.getDateEnvoi());
				Assert.assertEquals(dateTraitement.addDays(-30), demande.getDelaiRetour());
				Assert.assertNull(demande.getDateRetour());
				Assert.assertEquals(dateTraitement.addDays(-10), demande.getDateRappel());
				Assert.assertNull(demande.getCleArchivageRappel());
				Assert.assertEquals((Integer) 2013, demande.getPeriodeFiscale());
			}
			{
				// la non-échue
				final DemandeDegrevementICI demande = demandes.get(4);
				Assert.assertNotNull(demande);
				Assert.assertEquals(dateTraitement.addDays(-15), demande.getDateEnvoi());
				Assert.assertEquals(dateTraitement.addDays(15), demande.getDelaiRetour());
				Assert.assertNull(demande.getDateRetour());
				Assert.assertNull(demande.getDateRappel());
				Assert.assertNull(demande.getCleArchivageRappel());
				Assert.assertEquals((Integer) 2014, demande.getPeriodeFiscale());
			}
			return null;
		});
	}

	/**
	 * [SIFISC-25066] Demande exprimée de ne pas envoyer de rappel sur un document bien envoyé mais pour lequel on se rend compte plus tard (= fermeture du droit entre deux)
	 * qu'il n'aurait pas dû être envoyé
	 */
	@Test
	public void testNonEnvoiRappelSurEnvoiFinalementFaitATort() throws Exception {

		final RegDate dateDebut = date(2009, 4, 1);
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// rien
			}
		});

		final class Ids {
			long idEntreprise;
			long idDemandeEmise;
			long idDroit;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateDebut, null, "Punkt GmbH");
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aigle);

			final PersonneMoraleRF pmrf = addPersonneMoraleRF("Punkt GmbH", null, "7w457gfhfgfgwre", 845151564684L, null);
			addRapprochementRF(entreprise, pmrf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(12321, MockCommune.Lausanne.getNomOfficiel(), MockCommune.Lausanne.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("dfgszbsl3342", null, commune, 84165);
			final DroitProprietePersonneMoraleRF droit = addDroitPersonneMoraleRF(dateDebut, dateDebut, null, null, "Achat", null, "4637210tre", "4637210trd",
			                                                                      new IdentifiantAffaireRF(784, "SSP"), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmrf, immeuble, null);

			final DemandeDegrevementICI formulaire = addDemandeDegrevementICI(entreprise, 2017, immeuble);
			addDelaiAutreDocumentFiscal(formulaire, date(2009, 4, 1), date(2009, 4, 30), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(formulaire, date(2009, 4, 1));

			// mais en fait, le droit était déjà clôturé...
			droit.setDateFin(date(2017, 5, 21));
			droit.setDateFinMetier(date(2016, 12, 12));

			final Ids res = new Ids();
			res.idEntreprise = entreprise.getNumero();
			res.idDemandeEmise = formulaire.getId();
			res.idDroit = droit.getId();
			return res;
		});

		// lancement du job
		final RappelFormulairesDemandeDegrevementICIResults results = processor.run(dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(1, results.getIgnores().size());
		Assert.assertEquals(0, results.getTraites().size());

		// ignorée car droit clôturé avant le début de la PF du formulaire
		{
			final RappelFormulairesDemandeDegrevementICIResults.Ignore ignoree = results.getIgnores().get(0);
			Assert.assertNotNull(ignoree);
			Assert.assertEquals((Long) ids.idEntreprise, ignoree.noCtb);
			Assert.assertEquals("Lausanne", ignoree.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ignoree.ofsCommune);
			Assert.assertEquals(ids.idDemandeEmise, ignoree.idFormulaire);
			Assert.assertEquals(date(2009, 4, 1), ignoree.dateEnvoi);
			Assert.assertEquals((Integer) 84165, ignoree.noParcelle);
			Assert.assertNull(ignoree.index1);
			Assert.assertNull(ignoree.index2);
			Assert.assertNull(ignoree.index3);
			Assert.assertEquals((Integer) 2017, ignoree.periodeFiscale);
			Assert.assertEquals(RappelFormulairesDemandeDegrevementICIResults.RaisonIgnorement.DROIT_CLOTURE, ignoree.raison);
		}

		// et en base
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
			Assert.assertNotNull(entreprise);

			final List<DemandeDegrevementICI> demandes = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true).stream()
					.sorted(Comparator.comparingLong(DemandeDegrevementICI::getId))
					.collect(Collectors.toList());
			Assert.assertEquals(1, demandes.size());
			{
				// la demande émise mais toujours pas rappelée
				final DemandeDegrevementICI demande = demandes.get(0);
				Assert.assertNotNull(demande);
				Assert.assertEquals(date(2009, 4, 1), demande.getDateEnvoi());
				Assert.assertEquals(date(2009, 4, 30), demande.getDelaiRetour());
				Assert.assertNull(demande.getDateRetour());
				Assert.assertNull(demande.getDateRappel());
				Assert.assertNull(demande.getCleArchivageRappel());
				Assert.assertEquals((Integer) 2017, demande.getPeriodeFiscale());
			}
			return null;
		});
	}

	/**
	 * [SIFISC-25066] Demande exprimée de ne pas envoyer de rappel sur un document bien envoyé mais pour lequel on se rend compte plus tard (= fermeture du droit entre deux)
	 * qu'il n'aurait pas dû être envoyé (ici on traite le cas du droit clôturé _après_ le début de la période du formulaire...
	 */
	@Test
	public void testEnvoiRappelSurDroitClotureApres1erJanvierPF() throws Exception {

		final RegDate dateDebut = date(2009, 4, 1);
		final RegDate dateTraitement = RegDate.get();

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// rien
			}
		});

		final class Ids {
			long idEntreprise;
			long idDemandeEmise;
			long idDroit;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateDebut, null, "Punkt GmbH");
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aigle);

			final PersonneMoraleRF pmrf = addPersonneMoraleRF("Punkt GmbH", null, "7w457gfhfgfgwre", 845151564684L, null);
			addRapprochementRF(entreprise, pmrf, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(12321, MockCommune.Lausanne.getNomOfficiel(), MockCommune.Lausanne.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("dfgszbsl3342", null, commune, 84165);
			final DroitProprietePersonneMoraleRF droit = addDroitPersonneMoraleRF(dateDebut, dateDebut, null, null, "Achat", null, "4637210tre", "4637210trd",
			                                                                      new IdentifiantAffaireRF(784, "SSP"), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmrf, immeuble, null);

			final DemandeDegrevementICI formulaire = addDemandeDegrevementICI(entreprise, 2017, immeuble);
			addDelaiAutreDocumentFiscal(formulaire, date(2009, 4, 1), date(2009, 4, 30), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(formulaire, date(2009, 4, 1));

			// mais en fait, le droit était déjà clôturé... (mais reste valable au tout début de la période du formulaire)
			droit.setDateFin(date(2017, 5, 21));
			droit.setDateFinMetier(date(2017, 1, 12));

			final Ids res = new Ids();
			res.idEntreprise = entreprise.getNumero();
			res.idDemandeEmise = formulaire.getId();
			res.idDroit = droit.getId();
			return res;
		});

		// lancement du job
		final RappelFormulairesDemandeDegrevementICIResults results = processor.run(dateTraitement, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getIgnores().size());
		Assert.assertEquals(1, results.getTraites().size());

		// rappelée car droit clôturé après le début de la PF du formulaire
		{
			final RappelFormulairesDemandeDegrevementICIResults.Traite traitee = results.getTraites().get(0);
			Assert.assertNotNull(traitee);
			Assert.assertEquals((Long) ids.idEntreprise, traitee.noCtb);
			Assert.assertEquals("Lausanne", traitee.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), traitee.ofsCommune);
			Assert.assertEquals(ids.idDemandeEmise, traitee.idFormulaire);
			Assert.assertEquals(date(2009, 4, 1), traitee.dateEnvoiFormulaire);
			Assert.assertEquals((Integer) 84165, traitee.noParcelle);
			Assert.assertNull(traitee.index1);
			Assert.assertNull(traitee.index2);
			Assert.assertNull(traitee.index3);
			Assert.assertEquals((Integer) 2017, traitee.periodeFiscale);
		}

		// et en base
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idEntreprise);
			Assert.assertNotNull(entreprise);

			final List<DemandeDegrevementICI> demandes = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true).stream()
					.sorted(Comparator.comparingLong(DemandeDegrevementICI::getId))
					.collect(Collectors.toList());
			Assert.assertEquals(1, demandes.size());
			{
				// la demande émise est maintenant rappelée
				final DemandeDegrevementICI demande = demandes.get(0);
				Assert.assertNotNull(demande);
				Assert.assertEquals(date(2009, 4, 1), demande.getDateEnvoi());
				Assert.assertEquals(date(2009, 4, 30), demande.getDelaiRetour());
				Assert.assertNull(demande.getDateRetour());
				Assert.assertEquals(dateTraitement, demande.getDateRappel());
				Assert.assertNull(demande.getCleArchivageRappel());
				Assert.assertEquals((Integer) 2017, demande.getPeriodeFiscale());

				// [SIFISC-28193] l'état rappelé doit bien posséder les deux dates de traitement et d'envoi
				final EtatAutreDocumentFiscalRappele rappel = (EtatAutreDocumentFiscalRappele) demande.getDernierEtatOfType(TypeEtatDocumentFiscal.RAPPELE);
				assertNotNull(rappel);
				assertEquals(dateTraitement, rappel.getDateObtention());
				assertEquals(delaisService.getDateFinDelaiCadevImpressionDemandeDegrevementICI(dateTraitement), rappel.getDateEnvoiCourrier());
			}
			return null;
		});
	}
}
