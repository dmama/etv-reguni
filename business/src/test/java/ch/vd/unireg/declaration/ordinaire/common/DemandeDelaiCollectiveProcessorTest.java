package ch.vd.unireg.declaration.ordinaire.common;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DemandeDelaiCollectiveProcessorTest extends BusinessTest {

	private DemandeDelaiCollectiveProcessor processor;
	private AdresseService adresseService;

	private final RegDate dateTraitement = RegDate.get();

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		adresseService = getBean(AdresseService.class, "adresseService");
		final PeriodeFiscaleDAO periodeFiscaleDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new DemandeDelaiCollectiveProcessor(periodeFiscaleDAO, hibernateTemplate, transactionManager, tiersService, adresseService);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAccorderDelaiTiersPersonnePhysique() throws Exception {

		final RegDate dateDelai = RegDate.get(2010, 9, 1);

		final int annee = 2009;
		final PeriodeFiscale periode = addPeriodeFiscale(annee);
		final ModeleDocument modeleDocument = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);

		final List<Long> ids = Collections.emptyList();
		final PersonnePhysique mrKong = addNonHabitant("King", "Kong", date(1965, 4, 13), Sexe.MASCULIN);

		{
			// TEST : un tiers sans déclaration pour 2009.
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, dateDelai, dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(DemandeDelaiCollectiveResults.ErreurType.CONTRIBUABLE_SANS_DI, rapport.errors.get(0).raison);
		}

		final Declaration d = addDeclarationImpot(mrKong, periode, RegDate.get(2009, 1, 1), RegDate.get(2009, 12, 31), TypeContribuable.HORS_CANTON, modeleDocument);
		d.setDelais(new HashSet<>());
		assertNull(d.getDelaiAccordeAu());
		addEtatDeclarationEmise(d, date(2010, 1, 7));

		{
			// TEST : On lui ajoute 1 declaration pour 2009 à l'état émise :
			// - La déclaration n'a pas de délai
			// - La déclaration n'est pas retournée
			// - La déclaration n'est pas annulée
			// - On souhaite accordé un délai au 01.09.2010
			//
			// Resultats attendus :
			// - Le délai est d'abord null
			// - une fois le délai accordé, le délai est au au 01.09.2010

			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, dateDelai, dateTraitement, rapport);
			assertEquals(1, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(0, rapport.errors.size());
			assertEquals(dateDelai, d.getDelaiAccordeAu());
		}

		{
			// TEST : On essaye de rajouter un délai antérieur (au 31.08.2010)
			// Resultat attendu :
			// - le délai ne doit pas etre ajouté
			// - le délai est toujours au 01.09.2010

			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, date(2010, 8, 31), dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(1, rapport.ignores.size());
			assertEquals(0, rapport.errors.size());
			assertEquals(1, d.getDelaisDeclaration().size());
			assertEquals(dateDelai, d.getDelaiAccordeAu());
		}

		{
			// TEST : On essaye de rajouter le même délai (au 01.09.2010)
			// Resultat attendu :
			// - le délai ne doit pas etre ajouté
			// - le délai est toujours au 01.09.2010
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, dateDelai, dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(1, rapport.ignores.size());
			assertEquals(0, rapport.errors.size());
			assertEquals(1, d.getDelaisDeclaration().size());
			assertEquals(dateDelai, d.getDelaiAccordeAu());
		}

		{
			// TEST : On essaye de rajouter un délai posterieur (au 02.09.2010)
			// Resultat attendu :
			// - le délai doit etre ajouté
			// - le délai est maintenant au 02.09.2010
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, date(2010, 9, 2), dateTraitement, rapport);
			assertEquals(1, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(0, rapport.errors.size());
			assertEquals(2, d.getDelaisDeclaration().size());
			assertEquals(RegDate.get(2010, 9, 2), d.getDelaiAccordeAu());
		}


		{
			// TEST : La déclaration passe à l'état reçu :
			// Resultat attendu :
			// - aucun accord de délai ne doit passer
			addEtatDeclarationSommee(d, date(2010, 7, 18), date(2010, 7, 18), null);

			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, date(2010, 12, 4), dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(DemandeDelaiCollectiveResults.ErreurType.DECL_SOMMEE, rapport.errors.get(0).raison);
		}

		{
			// TEST : La déclaration passe à l'état reçu :
			// Resultat attendu :
			// - aucun accord de délai ne doit passer
			addEtatDeclarationEchue(d, date(2010, 8, 17));

			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, date(2010, 12, 4), dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(DemandeDelaiCollectiveResults.ErreurType.DECL_ECHUE, rapport.errors.get(0).raison);
		}

		{
			// TEST : La déclaration passe à l'état reçu :
			// Resultat attendu :
			// - aucun accord de délai ne doit passer
			addEtatDeclarationRetournee(d, date(2010, 10, 1));

			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, date(2010, 12, 4), dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(DemandeDelaiCollectiveResults.ErreurType.DECL_RETOURNEE, rapport.errors.get(0).raison);
		}
	}

	@Test
	public void testPersonneMoraleSansDeclaration() throws Exception {

		final RegDate dateDebut = date(2000, 1, 1);
		final RegDate dateDelai = date(2015, 9, 1);
		final int annee = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			addPeriodeFiscale(annee);
			return e.getNumero();
		});

		// contribuable sans déclaration à laquelle ajouter un délai
		doInNewTransactionAndSession(status -> {
			// TEST : un tiers sans déclaration
			final Entreprise e = (Entreprise) tiersDAO.get(pmId);
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(annee, dateDelai, Collections.<Long>emptyList(), dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(e, annee, dateDelai, dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(DemandeDelaiCollectiveResults.ErreurType.CONTRIBUABLE_SANS_DI, rapport.errors.get(0).raison);
			return null;
		});
	}

	@Test
	public void testPersonneMoraleAvecDeclarationEmise() throws Exception {

		final RegDate dateDebut = date(2000, 1, 1);
		final RegDate dateDelai = date(2015, 9, 1);
		final int annee = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(annee);
			final ModeleDocument modeleDocument = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final Declaration d = addDeclarationImpot(e, periode, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modeleDocument);
			d.setDelais(new HashSet<>());
			assertNull(d.getDelaiAccordeAu());

			addEtatDeclarationEmise(d, date(annee + 1, 1, 7));
			return e.getNumero();
		});

		// processing
		doInNewTransactionAndSession(status -> {
			// - La déclaration n'a pas de délai
			// - La déclaration n'est pas retournée
			// - La déclaration n'est pas annulée
			// - On souhaite accorder un délai au 01.09.2015
			//
			// Resultats attendus :
			// - Le délai est d'abord null
			// - une fois le délai accordé, le délai est au au 01.09.2015
			final Entreprise e = (Entreprise) tiersDAO.get(pmId);
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(annee, dateDelai, Collections.<Long>emptyList(), dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(e, annee, dateDelai, dateTraitement, rapport);
			assertEquals(1, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(0, rapport.errors.size());
			return null;
		});

		// contrôle
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersDAO.get(pmId);
			final Declaration d = e.getDerniereDeclaration(Declaration.class);
			assertNotNull(d);
			assertEquals((Integer) annee, d.getPeriode().getAnnee());
			assertEquals(dateDelai, d.getDelaiAccordeAu());
			return null;
		});
	}

	@Test
	public void testPersonneMoraleDeclarationEmiseDemandeDelaiAvecDelaiExistant() throws Exception {

		final RegDate dateDebut = date(2000, 1, 1);
		final RegDate dateDelaiInitial = date(2015, 9, 1);
		final int annee = 2014;

		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(annee);
			final ModeleDocument modeleDocument = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final Declaration d = addDeclarationImpot(e, periode, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modeleDocument);

			addDelaiDeclaration(d, dateTraitement, dateDelaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			addEtatDeclarationEmise(d, date(annee + 1, 1, 7));
			return e.getNumero();
		});

		// tentative d'ajour d'un délai antérieur au délai existant
		for (int decalage = -20; decalage <= 0; ++decalage) {
			// TEST : On essaye de rajouter un délai antérieur
			// Resultat attendu :
			// - le délai ne doit pas etre ajouté
			// - le délai est toujours au délai initial

			final RegDate dateDelaiDemande = dateDelaiInitial.addDays(decalage);        // au plus tard au délai initial, car décalage est négatif ou nul
			doInNewTransactionAndSession(status -> {
				final Entreprise e = (Entreprise) tiersDAO.get(pmId);
				final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(annee, dateDelaiDemande, Collections.<Long>emptyList(), dateTraitement, tiersService, adresseService);
				processor.accorderDelaiDeclaration(e, annee, dateDelaiDemande, dateTraitement, rapport);
				assertEquals(RegDateHelper.dateToDisplayString(dateDelaiDemande), 0, rapport.traites.size());
				assertEquals(RegDateHelper.dateToDisplayString(dateDelaiDemande), 1, rapport.ignores.size());
				assertEquals(RegDateHelper.dateToDisplayString(dateDelaiDemande), 0, rapport.errors.size());
				return null;
			});

			// contrôle
			doInNewTransactionAndSession(status -> {
				final Entreprise e = (Entreprise) tiersDAO.get(pmId);
				final Declaration d = e.getDerniereDeclaration(Declaration.class);
				assertNotNull(d);
				assertEquals((Integer) annee, d.getPeriode().getAnnee());
				assertEquals(RegDateHelper.dateToDisplayString(dateDelaiDemande), 1, d.getDelaisDeclaration().size());
				assertEquals(RegDateHelper.dateToDisplayString(dateDelaiDemande), dateDelaiInitial, d.getDelaiAccordeAu());
				return null;
			});
		}

		// demandes de délais postérieurs au délai existant
		for (int decalage = 1; decalage < 20; ++decalage) {

			// TEST : On essaye de rajouter un délai posterieur au délai initial
			// Resultat attendu :
			// - le délai doit etre ajouté
			// - le délai est maintenant celui nouvellement demandé

			final RegDate dateDelaiDemande = dateDelaiInitial.addDays(decalage);        // après le délai initial donc, de manière croissante
			doInNewTransactionAndSession(status -> {
				final Entreprise e = (Entreprise) tiersDAO.get(pmId);
				final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(annee, dateDelaiDemande, Collections.<Long>emptyList(), dateTraitement, tiersService, adresseService);
				processor.accorderDelaiDeclaration(e, annee, dateDelaiDemande, dateTraitement, rapport);
				assertEquals(1, rapport.traites.size());
				assertEquals(0, rapport.ignores.size());
				assertEquals(0, rapport.errors.size());
				return null;
			});

			// contrôle du nouveau délai
			final int nbDelaisAttendus = 1 + decalage;      // le délai initial + les délais demandés et accordés jusqu'ici
			doInNewTransactionAndSession(status -> {
				final Entreprise e = (Entreprise) tiersDAO.get(pmId);
				final Declaration d = e.getDerniereDeclaration(Declaration.class);
				assertNotNull(d);
				assertEquals((Integer) annee, d.getPeriode().getAnnee());
				assertEquals(RegDateHelper.dateToDisplayString(dateDelaiDemande), nbDelaisAttendus, d.getDelaisDeclaration().size());
				assertEquals(RegDateHelper.dateToDisplayString(dateDelaiDemande), dateDelaiDemande, d.getDelaiAccordeAu());
				return null;
			});
		}
	}

	@Test
	public void testPersonneMoraleDeclarationRecue() throws Exception {

		final RegDate dateDebut = date(2000, 1, 1);
		final RegDate dateDelaiInitial = date(2015, 9, 1);
		final int annee = 2014;

		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(annee);
			final ModeleDocument modeleDocument = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final Declaration d = addDeclarationImpot(e, periode, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modeleDocument);

			addDelaiDeclaration(d, dateTraitement, dateDelaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			addEtatDeclarationEmise(d, date(annee + 1, 1, 7));

			// TEST : La déclaration passe à l'état reçu :
			// Resultat attendu :
			// - aucun accord de délai ne doit passer
			addEtatDeclarationRetournee(d, date(annee + 1, 7, 18));
			return e.getNumero();
		});

		// tentative d'ajout d'un nouveau délai
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersDAO.get(pmId);
			final RegDate nouveauDelai = date(annee + 1, 12, 4);
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(annee, nouveauDelai, Collections.<Long>emptyList(), dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(e, annee, nouveauDelai, dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(DemandeDelaiCollectiveResults.ErreurType.DECL_RETOURNEE, rapport.errors.get(0).raison);
			return null;
		});
	}

	@Test
	public void testPersonneMoraleDeclarationSommee() throws Exception {

		final RegDate dateDebut = date(2000, 1, 1);
		final RegDate dateDelaiInitial = date(2015, 4, 1);
		final int annee = 2014;

		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(annee);
			final ModeleDocument modeleDocument = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final Declaration d = addDeclarationImpot(e, periode, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modeleDocument);

			addDelaiDeclaration(d, dateTraitement, dateDelaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			addEtatDeclarationEmise(d, date(annee + 1, 1, 7));

			// TEST : La déclaration passe à l'état sommée :
			// Resultat attendu :
			// - aucun accord de délai ne doit passer
			addEtatDeclarationSommee(d, date(annee + 1, 7, 18), date(annee + 1, 7, 19), null);
			return e.getNumero();
		});

		// tentative d'ajout d'un nouveau délai
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersDAO.get(pmId);
			final RegDate nouveauDelai = date(annee + 1, 12, 4);
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(annee, nouveauDelai, Collections.<Long>emptyList(), dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(e, annee, nouveauDelai, dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(DemandeDelaiCollectiveResults.ErreurType.DECL_SOMMEE, rapport.errors.get(0).raison);
			return null;
		});
	}

	@Test
	public void testPersonneMoraleDeclarationEchue() throws Exception {

		final RegDate dateDebut = date(2000, 1, 1);
		final RegDate dateDelaiInitial = date(2015, 4, 1);
		final int annee = 2014;

		final long pmId = doInNewTransactionAndSession(status -> {
			final Entreprise e = addEntrepriseInconnueAuCivil();
			addRaisonSociale(e, dateDebut, null, "Truc machin SA");
			addFormeJuridique(e, dateDebut, null, FormeJuridiqueEntreprise.SA);
			addBouclement(e, dateDebut, DayMonth.get(12, 31), 12);
			addRegimeFiscalVD(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(e, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(e, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Aubonne);

			final PeriodeFiscale periode = addPeriodeFiscale(annee);
			final ModeleDocument modeleDocument = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final Declaration d = addDeclarationImpot(e, periode, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modeleDocument);

			addDelaiDeclaration(d, dateTraitement, dateDelaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
			addEtatDeclarationEmise(d, date(annee + 1, 7, 1));

			// TEST : La déclaration passe à l'état échue :
			// Resultat attendu :
			// - aucun accord de délai ne doit passer
			addEtatDeclarationEchue(d, date(annee + 1, 7, 18));
			return e.getNumero();
		});

		// tentative d'ajout d'un nouveau délai
		doInNewTransactionAndSession(status -> {
			final Entreprise e = (Entreprise) tiersDAO.get(pmId);
			final RegDate nouveauDelai = date(annee + 1, 12, 4);
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(annee, nouveauDelai, Collections.<Long>emptyList(), dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(e, annee, nouveauDelai, dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(DemandeDelaiCollectiveResults.ErreurType.DECL_ECHUE, rapport.errors.get(0).raison);
			return null;
		});
	}
}
