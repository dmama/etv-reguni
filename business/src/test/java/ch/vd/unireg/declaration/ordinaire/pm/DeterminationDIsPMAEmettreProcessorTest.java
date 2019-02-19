package ch.vd.unireg.declaration.ordinaire.pm;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.FlushMode;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpotPM;
import ch.vd.unireg.type.CategorieEntreprise;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatTache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class DeterminationDIsPMAEmettreProcessorTest extends BusinessTest {

	private DeterminationDIsPMAEmettreProcessor service;
	private PeriodeImpositionService periodeImpositionService;
	private AdresseService adresseService;
	private TacheDAO tacheDAO;

	private ParametreAppService parametreAppService;
	private Integer oldPremierePeriodeFiscaleDeclarationsPersonnesMorales;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		final PeriodeFiscaleDAO periodeDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		final ValidationService validationService = getBean(ValidationService.class, "validationService");
		periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");
		adresseService = getBean(AdresseService.class, "adresseService");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		service = new DeterminationDIsPMAEmettreProcessor(hibernateTemplate, periodeDAO, tacheDAO, parametreAppService, tiersService,
		                                                  transactionManager, validationService, periodeImpositionService, adresseService);

		// pour permettre la gestion de tâches avant 2016 tant qu'on n'est pas encore en 2016
		oldPremierePeriodeFiscaleDeclarationsPersonnesMorales = parametreAppService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
		parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(2013);
	}

	@Override
	public void onTearDown() throws Exception {
		// restauration de la valeur en base une fois les tests terminés
		if (oldPremierePeriodeFiscaleDeclarationsPersonnesMorales != null) {
			parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(oldPremierePeriodeFiscaleDeclarationsPersonnesMorales);
		}
		super.onTearDown();
	}

	/**
	 * Pour les personnes morales, on va chercher toutes celles qui ont au moins un for vaudois IBC non-annulé, quelle que soit
	 * sa période de validité
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreateListeIdsContribuables() {

		/*
		 * Contribuables devant être pris en compte
		 */

		// Une entreprise avec un for ouvert à droite
		final Entreprise levage = addEntrepriseInconnueAuCivil();
		addRaisonSociale(levage, date(1999, 4, 2), null, "Levage SA");
		addFormeJuridique(levage, date(1999, 4, 2), null, FormeJuridiqueEntreprise.SA);
		addBouclement(levage, date(1999, 7, 1), DayMonth.get(6, 30), 12);
		addRegimeFiscalVD(levage, RegDate.get(1999, 4, 2), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(levage, RegDate.get(1999, 4, 2), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(levage, date(1999, 4, 2), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

		// Une entreprise avec un for principal actif sur VD il y a quelques temps
		final Entreprise bricolage = addEntrepriseInconnueAuCivil();
		addRaisonSociale(bricolage, date(2000, 4, 5), null, "Bricolage SARL");
		addFormeJuridique(bricolage, date(2000, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(bricolage, date(2000, 4, 5), DayMonth.get(12, 31), 12);
		addRegimeFiscalVD(bricolage, RegDate.get(2000, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(bricolage, RegDate.get(2000, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(bricolage, date(2000, 4, 5), MotifFor.DEBUT_EXPLOITATION, date(2012, 5, 2), MotifFor.DEPART_HC, MockCommune.Bex);
		addForPrincipal(bricolage, date(2012, 5, 3), MotifFor.DEPART_HC, MockCommune.Sierre);

		// Une entreprise avec un for secondaire actif sur VD
		final Entreprise renardLico = addEntrepriseInconnueAuCivil();
		addRaisonSociale(renardLico, date(1997, 4, 5), null, "Renard Lico SARL");
		addFormeJuridique(renardLico, date(1997, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(renardLico, date(1997, 4, 5), DayMonth.get(12, 31), 12);
		addRegimeFiscalVD(renardLico, RegDate.get(1997, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(renardLico, RegDate.get(1997, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(renardLico, date(1997, 4, 5), null, MockCommune.Chur);
		addForSecondaire(renardLico, date(1997, 4, 5), MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);

		// Une entreprise avec un for secondaire actif sur VD il y a quelques temps
		final Entreprise tralalaTsointsoin = addEntrepriseInconnueAuCivil();
		addRaisonSociale(tralalaTsointsoin, date(2003, 4, 5), null, "Tralala Tsoin-Tsoin SARL");
		addFormeJuridique(tralalaTsointsoin, date(2003, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(tralalaTsointsoin, date(2003, 4, 5), DayMonth.get(12, 31), 12);
		addRegimeFiscalVD(tralalaTsointsoin, RegDate.get(2003, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(tralalaTsointsoin, RegDate.get(2003, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(tralalaTsointsoin, date(2003, 4, 5), null, MockCommune.Chur);
		addForSecondaire(tralalaTsointsoin, date(2003, 4, 5), MotifFor.ACHAT_IMMOBILIER, date(2008, 12, 2), MotifFor.VENTE_IMMOBILIER, MockCommune.Echallens, MotifRattachement.IMMEUBLE_PRIVE, GenreImpot.BENEFICE_CAPITAL);

		/*
		 * Contribuables devant être ignorés
		 */

		// Une entreprise sans for vaudois non-annulé du tout
		final Entreprise onCroyaitEtPuisNon = addEntrepriseInconnueAuCivil();
		addRaisonSociale(onCroyaitEtPuisNon, date(2000, 4, 5), null, "Bricolage SARL");
		addFormeJuridique(onCroyaitEtPuisNon, date(2000, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(onCroyaitEtPuisNon, date(2000, 4, 5), DayMonth.get(12, 31), 12);
		addRegimeFiscalVD(onCroyaitEtPuisNon, RegDate.get(2000, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(onCroyaitEtPuisNon, RegDate.get(2000, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(onCroyaitEtPuisNon, date(2000, 4, 5), MotifFor.DEBUT_EXPLOITATION, date(2012, 5, 2), MotifFor.DEPART_HC, MockCommune.Bex).setAnnule(true);

		//
		// Contrôle
		//

		final List<Long> list = service.createListeIdsContribuables();
		assertEquals(Arrays.asList(levage.getNumero(), bricolage.getNumero(), renardLico.getNumero(), tralalaTsointsoin.getNumero()), list);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDeterminePeriodesImpositionsConcernees() throws Exception {

		final Entreprise entreprise = addEntrepriseInconnueAuCivil();
		addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
		addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);     // bouclements tous les ans depuis le 31.12.2012
		addBouclement(entreprise, date(2012, 12, 31), DayMonth.get(3, 31), 5);      // bouclements tous les cinq mois depuis le 31.3.2013
		addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, date(2015, 5, 2), MotifFor.DEPART_HC, MockCommune.Bex);
		addForPrincipal(entreprise, date(2015, 5, 3), MotifFor.DEPART_HC, MockCommune.Bern);

		// vérification du paramètre (assigné dans le setup)
		assertEquals((Integer) 2013, parametreAppService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales());

		{
			// il y a deux bouclements en 2013 (31.03, 31.08)

			final int annee = 2013;
			for (int nbJours = -100; nbJours < 500; ++nbJours) {
				final RegDate dateTraitement = date(2013, 1, 1).addDays(nbJours);
				final DeterminationDIsPMResults rapport = new DeterminationDIsPMResults(annee, dateTraitement, 1, tiersService, adresseService);
				final List<PeriodeImpositionPersonnesMorales> pis = service.extrairePeriodesImpositionATraiter(entreprise, annee, dateTraitement, rapport).aTraiter;

				// depuis le 01.04.2014, il y a au moins une période d'imposition terminée -> elle doit être présente en premier
				if (dateTraitement.isAfterOrEqual(date(2013, 4, 1))) {
					assertTrue(RegDateHelper.dateToDisplayString(dateTraitement), pis.size() >= 1);

					final PeriodeImpositionPersonnesMorales pi = pis.get(0);
					assertNotNull(RegDateHelper.dateToDisplayString(dateTraitement), pi);
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), date(2013, 1, 1), pi.getDateDebut());
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), date(2013, 3, 31), pi.getDateFin());
				}
				else {
					// avant le 01.04, aucune période d'imposition n'est terminée -> on ne doit rien recevoir
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), 0, pis.size());
					continue;
				}

				// depuis le 01.09.2013, il doit y en avoir deux...
				if (dateTraitement.isAfterOrEqual(date(2013, 9, 1))) {
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), 2, pis.size());

					final PeriodeImpositionPersonnesMorales pi = pis.get(1);
					assertNotNull(RegDateHelper.dateToDisplayString(dateTraitement), pi);
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), date(2013, 4, 1), pi.getDateDebut());
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), date(2013, 8, 31), pi.getDateFin());
				}
				else {
					// entre le 01.04 et le 31.08.2013, une seule période doit être vue
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), 1, pis.size());
				}
			}
		}
		{
			// il y a trois bouclements en 2014 (31.01, 30.06 et 30.11)

			final int annee = 2014;
			for (int nbJours = -100; nbJours < 500; ++nbJours) {
				final RegDate dateTraitement = date(2014, 1, 1).addDays(nbJours);
				final DeterminationDIsPMResults rapport = new DeterminationDIsPMResults(annee, dateTraitement, 1, tiersService, adresseService);
				final List<PeriodeImpositionPersonnesMorales> pis = service.extrairePeriodesImpositionATraiter(entreprise, annee, dateTraitement, rapport).aTraiter;

				// depuis le 01.02.2014, il y a au moins une période d'imposition terminée -> elle doit être présente en premier
				if (dateTraitement.isAfterOrEqual(date(2014, 2, 1))) {
					assertTrue(RegDateHelper.dateToDisplayString(dateTraitement), pis.size() >= 1);

					final PeriodeImpositionPersonnesMorales pi = pis.get(0);
					assertNotNull(RegDateHelper.dateToDisplayString(dateTraitement), pi);
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), date(2013, 9, 1), pi.getDateDebut());
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), date(2014, 1, 31), pi.getDateFin());
				}
				else {
					// avant le 01.02, aucune période d'imposition n'est terminée -> on ne doit rien recevoir
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), 0, pis.size());
					continue;
				}

				// depuis le 01.07.2014, il doit y en avoir au moins deux...
				if (dateTraitement.isAfterOrEqual(date(2014, 7, 1))) {
					assertTrue(RegDateHelper.dateToDisplayString(dateTraitement), pis.size() >= 2);

					final PeriodeImpositionPersonnesMorales pi = pis.get(1);
					assertNotNull(RegDateHelper.dateToDisplayString(dateTraitement), pi);
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), date(2014, 2, 1), pi.getDateDebut());
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), date(2014, 6, 30), pi.getDateFin());
				}
				else {
					// entre le 01.02 et le 30.06.2014, une seule période doit être vue
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), 1, pis.size());
					continue;
				}

				// depuis le 01.12.2014, il doit y en avoir trois...
				if (dateTraitement.isAfterOrEqual(date(2014, 12, 1))) {
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), 3, pis.size());

					final PeriodeImpositionPersonnesMorales pi = pis.get(2);
					assertNotNull(RegDateHelper.dateToDisplayString(dateTraitement), pi);
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), date(2014, 7, 1), pi.getDateDebut());
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), date(2014, 11, 30), pi.getDateFin());
				}
				else {
					// entre le 01.07 et le 30.11.2014, deux périodes doivent être vues
					assertEquals(RegDateHelper.dateToDisplayString(dateTraitement), 2, pis.size());
				}
			}
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiterPeriodeImposition() throws Exception {

		final PeriodeFiscale periode = addPeriodeFiscale(2014);
		addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);

		final Entreprise entreprise = addEntrepriseInconnueAuCivil();
		addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
		addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
		addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, date(2015, 5, 2), MotifFor.DEPART_HC, MockCommune.Bex);
		addForPrincipal(entreprise, date(2015, 5, 3), MotifFor.DEPART_HC, MockCommune.Bern);

		final List<PeriodeImposition> periodes = periodeImpositionService.determine(entreprise, 2014);
		assertEquals(1, periodes.size());
		final PeriodeImposition periodeImposition = periodes.get(0);
		assertInstanceOf(PeriodeImpositionPersonnesMorales.class, periodeImposition);

		final RegDate dateTraitement = RegDate.get();
		final DeterminationDIsPMResults r = new DeterminationDIsPMResults(2014, dateTraitement, 1, tiersService, adresseService);
		final TacheEnvoiDeclarationImpotPM tacheEnvoi = service.traiterPeriodeImposition(entreprise, (PeriodeImpositionPersonnesMorales) periodeImposition, dateTraitement, r);
		assertNotNull(tacheEnvoi);
		assertTache(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(dateTraitement), date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
		            TypeDocument.DECLARATION_IMPOT_PM_BATCH, tacheEnvoi);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiterPeriodeImpositionTacheDejaTraitee() throws Exception {

		final PeriodeFiscale periode = addPeriodeFiscale(2014);
		addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);

		final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

		// création d'une entreprise
		final Entreprise entreprise = addEntrepriseInconnueAuCivil();
		addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
		addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
		addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, date(2015, 5, 2), MotifFor.DEPART_HC, MockCommune.Bex);
		addForPrincipal(entreprise, date(2015, 5, 3), MotifFor.DEPART_HC, MockCommune.Bern);

		// génération d'une tâche existante déjà traitée (= cela ne fait pas de différence avec l'absence de tâche)
		final RegDate dateTraitement = RegDate.get();
		final RegDate dateEcheanceExistante = dateTraitement.addDays(-50);
		addTacheEnvoiDIPM(TypeEtatTache.TRAITE, dateEcheanceExistante, date(2014, 1, 1), date(2014, 12, 31),
		                  date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
		                  TypeDocument.DECLARATION_IMPOT_PM_BATCH, entreprise, CategorieEntreprise.PM, oipm);
		hibernateTemplate.flush();

		final List<PeriodeImposition> periodes = periodeImpositionService.determine(entreprise, 2014);
		assertEquals(1, periodes.size());
		final PeriodeImposition periodeImposition = periodes.get(0);
		assertInstanceOf(PeriodeImpositionPersonnesMorales.class, periodeImposition);

		final DeterminationDIsPMResults r = new DeterminationDIsPMResults(2014, dateTraitement, 1, tiersService, adresseService);
		final TacheEnvoiDeclarationImpotPM tacheEnvoi = service.traiterPeriodeImposition(entreprise, (PeriodeImpositionPersonnesMorales) periodeImposition, dateTraitement, r);
		assertNotNull(tacheEnvoi);
		assertTache(TypeEtatTache.EN_INSTANCE, Tache.getDefaultEcheance(dateTraitement), date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
		            TypeDocument.DECLARATION_IMPOT_PM_BATCH, tacheEnvoi);
	}

	/**
	 * Essai de duplication de tâches (qui ne doit pas être possible)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testTraiterPeriodeImpositionDuplication() throws Exception {

		final PeriodeFiscale periode = addPeriodeFiscale(2014);
		addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode);

		final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

		// création d'une entreprise
		final Entreprise entreprise = addEntrepriseInconnueAuCivil();
		addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
		addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
		addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, date(2015, 5, 2), MotifFor.DEPART_HC, MockCommune.Bex);
		addForPrincipal(entreprise, date(2015, 5, 3), MotifFor.DEPART_HC, MockCommune.Bern);

		// génération d'une tâche existante encore en instance
		final RegDate dateTraitement = RegDate.get();
		final RegDate dateEcheanceExistante = dateTraitement.addDays(-50);
		addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, dateEcheanceExistante, date(2014, 1, 1), date(2014, 12, 31),
		                  date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
		                  TypeDocument.DECLARATION_IMPOT_PM_BATCH, entreprise, CategorieEntreprise.PM, oipm);
		hibernateTemplate.flush();

		final List<PeriodeImposition> periodes = periodeImpositionService.determine(entreprise, 2014);
		assertEquals(1, periodes.size());
		final PeriodeImposition periodeImposition = periodes.get(0);
		assertInstanceOf(PeriodeImpositionPersonnesMorales.class, periodeImposition);

		final DeterminationDIsPMResults r = new DeterminationDIsPMResults(2014, dateTraitement, 1, tiersService, adresseService);
		final TacheEnvoiDeclarationImpotPM tacheEnvoi = service.traiterPeriodeImposition(entreprise, (PeriodeImpositionPersonnesMorales) periodeImposition, dateTraitement, r);
		assertNull(tacheEnvoi);
		assertEquals(1, r.ignores.size());

		final DeterminationDIsPMResults.Ignore ignore = r.ignores.get(0);
		assertNotNull(ignore);
		assertEquals(DeterminationDIsPMResults.IgnoreType.TACHE_ENVOI_DEJA_EXISTANTE, ignore.raison);
		assertEquals(entreprise.getNumero().longValue(), ignore.noCtb);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCheckTacheExistence() {

		final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

		// Aucune tâche
		{
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
			addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
			addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
			addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, date(2015, 5, 2), MotifFor.DEPART_HC, MockCommune.Bex);
			addForPrincipal(entreprise, date(2015, 5, 3), MotifFor.DEPART_HC, MockCommune.Bern);

			final DateRange range = new DateRangeHelper.Range(date(2014, 1, 1), date(2014, 12, 31));
			assertNull(service.checkExistenceTache(entreprise, range));
		}

		// Une tâche qui existe déjà avec un range qui ne correspond pas du tout
		{
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
			addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
			addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
			addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, date(2015, 5, 2), MotifFor.DEPART_HC, MockCommune.Bex);
			addForPrincipal(entreprise, date(2015, 5, 3), MotifFor.DEPART_HC, MockCommune.Bern);

			addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, date(2015, 1, 5), date(2013, 1, 1), date(2013, 12, 31),
			                  date(2013, 1, 1), date(2013, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
			                  TypeDocument.DECLARATION_IMPOT_PM_BATCH, entreprise, CategorieEntreprise.PM, oipm);

			final DateRange range = new DateRangeHelper.Range(date(2014, 1, 1), date(2014, 12, 31));        // ne correspond pas du tout
			assertNull(service.checkExistenceTache(entreprise, range));
		}

		// Une tâche qui existe déjà avec un range qui correspond parfaitement
		{
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
			addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
			addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
			addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, date(2015, 5, 2), MotifFor.DEPART_HC, MockCommune.Bex);
			addForPrincipal(entreprise, date(2015, 5, 3), MotifFor.DEPART_HC, MockCommune.Bern);

			addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, date(2015, 1, 5), date(2014, 1, 1), date(2014, 12, 31),
			                  date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
			                  TypeDocument.DECLARATION_IMPOT_PM_BATCH, entreprise, CategorieEntreprise.PM, oipm);
			hibernateTemplate.flush();

			final DateRange range = new DateRangeHelper.Range(date(2014, 1, 1), date(2014, 12, 31));
			final DeterminationDIsPMAEmettreProcessor.ExistenceResults<TacheEnvoiDeclarationImpotPM> results = service.checkExistenceTache(entreprise, range);
			assertNotNull(results);
			assertEquals(DeterminationDIsPMAEmettreProcessor.ExistenceResults.TacheStatus.EXISTE_DEJA, results.status);
			assertEquals(date(2014, 1, 1), results.object.getDateDebut());
			assertEquals(date(2014, 12, 31), results.object.getDateFin());
		}

		// Une tâche qui existe déjà avec un range qui recouvre partiellement le range spécifié
		{
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
			addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
			addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
			addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, date(2015, 5, 2), MotifFor.DEPART_HC, MockCommune.Bex);
			addForPrincipal(entreprise, date(2015, 5, 3), MotifFor.DEPART_HC, MockCommune.Bern);

			addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, date(2015, 1, 5), date(2014, 5, 1), date(2014, 12, 31),
			                  date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
			                  TypeDocument.DECLARATION_IMPOT_PM_BATCH, entreprise, CategorieEntreprise.PM, oipm);
			hibernateTemplate.flush();

			final DateRange range = new DateRangeHelper.Range(date(2014, 1, 1), date(2014, 12, 31));
			final DeterminationDIsPMAEmettreProcessor.ExistenceResults<TacheEnvoiDeclarationImpotPM> results = service.checkExistenceTache(entreprise, range);
			assertNotNull(results);
			assertEquals(DeterminationDIsPMAEmettreProcessor.ExistenceResults.TacheStatus.INTERSECTE, results.status);
			assertEquals(date(2014, 5, 1), results.object.getDateDebut());
			assertEquals(date(2014, 12, 31), results.object.getDateFin());
		}

		// Une tâche déjà traitée avec un range qui recouvre partiellement le range spécifié
		{
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
			addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
			addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
			addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, date(2015, 5, 2), MotifFor.DEPART_HC, MockCommune.Bex);
			addForPrincipal(entreprise, date(2015, 5, 3), MotifFor.DEPART_HC, MockCommune.Bern);

			addTacheEnvoiDIPM(TypeEtatTache.TRAITE, date(2015, 1, 5), date(2014, 5, 1), date(2014, 12, 31),
			                  date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
			                  TypeDocument.DECLARATION_IMPOT_PM_BATCH, entreprise, CategorieEntreprise.PM, oipm);
			hibernateTemplate.flush();

			final DateRange range = new DateRangeHelper.Range(date(2014, 1, 1), date(2014, 12, 31));
			final DeterminationDIsPMAEmettreProcessor.ExistenceResults<TacheEnvoiDeclarationImpotPM> results = service.checkExistenceTache(entreprise, range);
			assertNull(results);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationDeclarationSansPeriodeCorrespondante() throws Exception {

		final PeriodeFiscale periode2014 = addPeriodeFiscale(2014);
		final ModeleDocument modele2014 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode2014);
		final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

		// Un contribuable non-assujetti, mais avec une déclaration d'impôt (invalide) pré-existante
		final Entreprise entreprise = addEntrepriseInconnueAuCivil();
		addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
		addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
		addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bern);

		addDeclarationImpot(entreprise, periode2014, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele2014);
		hibernateTemplate.flush();

		final RegDate dateTraitement = RegDate.get();
		final DeterminationDIsPMResults rapport = new DeterminationDIsPMResults(2014, dateTraitement, 1, tiersService, adresseService);
		service.traiterEntreprise(entreprise, periode2014, dateTraitement, rapport);

		assertEmpty(rapport.erreurs);

		// ignoré parce que pas assujetti
		assertEquals(1, rapport.ignores.size());
		final DeterminationDIsPMResults.Ignore ignore = rapport.ignores.get(0);
		assertNotNull(ignore);
		assertEquals(DeterminationDIsPMResults.IgnoreType.PAS_ASSUJETTI, ignore.raison);

		// une nouvelle tâche d'annulation de la déclaration doit avoir été créée
		assertEquals(1, rapport.traites.size());
		final DeterminationDIsPMResults.Traite traite = rapport.traites.get(0);
		assertNotNull(traite);
		assertEquals(DeterminationDIsPMResults.TraiteType.TACHE_ANNULATION_CREE, traite.raison);
	}

	/**
	 * [UNIREG-1981] vérifie qu'on ne génère pas une tâche d'annulation de déclaration d'impôt s'il en existe déjà une non-traitée.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationDeclarationSansPeriodeMaisAvecTacheAnnulationPreexistante() throws Exception {

		final PeriodeFiscale periode2014 = addPeriodeFiscale(2014);
		final ModeleDocument modele2014 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode2014);
		final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

		// Un contribuable non-assujetti, mais avec une déclaration d'impôt (invalide) pré-existante
		final Entreprise entreprise = addEntrepriseInconnueAuCivil();
		addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
		addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
		addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bern);

		final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, periode2014, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele2014);
		addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, RegDate.get(), di, entreprise, oipm);
		hibernateTemplate.flush();

		final RegDate dateTraitement = RegDate.get();
		final DeterminationDIsPMResults rapport = new DeterminationDIsPMResults(2014, dateTraitement, 1, tiersService, adresseService);
		service.traiterEntreprise(entreprise, periode2014, dateTraitement, rapport);

		assertEmpty(rapport.erreurs);
		assertEmpty(rapport.traites);

		// ignoré 2 fois parce que pas assujetti + parce que la tâche d'annulation de la DI existe déjà
		assertEquals(2, rapport.ignores.size());
		assertEquals(DeterminationDIsPMResults.IgnoreType.PAS_ASSUJETTI, rapport.ignores.get(0).raison);
		assertEquals(DeterminationDIsPMResults.IgnoreType.TACHE_ANNULATION_DEJA_EXISTANTE, rapport.ignores.get(1).raison);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationTacheSansPeriodeCorrespondante() throws Exception {

		final PeriodeFiscale periode2014 = addPeriodeFiscale(2014);
		final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

		// Un contribuable non-assujetti, mais avec une déclaration d'impôt (invalide) pré-existante
		final Entreprise entreprise = addEntrepriseInconnueAuCivil();
		addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
		addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
		addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bern);

		final RegDate dateTraitement = RegDate.get();
		addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, dateTraitement.addDays(-41), date(2014, 1, 1), date(2014, 12, 31),
		                  date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
		                  TypeDocument.DECLARATION_IMPOT_PM_BATCH, entreprise, CategorieEntreprise.PM, oipm);
		hibernateTemplate.flush();

		final DeterminationDIsPMResults rapport = new DeterminationDIsPMResults(2014, dateTraitement, 1, tiersService, adresseService);
		service.traiterEntreprise(entreprise, periode2014, dateTraitement, rapport);

		assertEmpty(rapport.erreurs);

		// ignoré parce que pas assujetti
		assertEquals(1, rapport.ignores.size());
		final DeterminationDIsPMResults.Ignore ignore = rapport.ignores.get(0);
		assertNotNull(ignore);
		assertEquals(DeterminationDIsPMResults.IgnoreType.PAS_ASSUJETTI, ignore.raison);

		// une nouvelle tâche d'annulation de la déclaration doit avoir été créée
		assertEquals(1, rapport.traites.size());
		final DeterminationDIsPMResults.Traite traite = rapport.traites.get(0);
		assertNotNull(traite);
		assertEquals(DeterminationDIsPMResults.TraiteType.TACHE_ENVOI_ANNULEE, traite.raison);
	}

	/**
	 * [UNIREG-1981] Vérifie que les tâches déjà traitées ne sont pas annulées
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationTacheDejaTraitee() throws Exception {

		final PeriodeFiscale periode2014 = addPeriodeFiscale(2014);
		final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

		// Un contribuable non-assujetti, mais avec une déclaration d'impôt (invalide) pré-existante
		final Entreprise entreprise = addEntrepriseInconnueAuCivil();
		addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
		addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
		addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bern);

		final RegDate dateTraitement = RegDate.get();
		final TacheEnvoiDeclarationImpotPM tache = addTacheEnvoiDIPM(TypeEtatTache.TRAITE, dateTraitement.addDays(-41), date(2014, 1, 1), date(2014, 12, 31),
		                                                             date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
		                                                             TypeDocument.DECLARATION_IMPOT_PM_BATCH, entreprise, CategorieEntreprise.PM, oipm);
		hibernateTemplate.flush();

		final DeterminationDIsPMResults rapport = new DeterminationDIsPMResults(2014, dateTraitement, 1, tiersService, adresseService);
		service.traiterEntreprise(entreprise, periode2014, dateTraitement, rapport);

		assertEmpty(rapport.erreurs);

		// ignoré parce que pas assujetti
		assertEquals(1, rapport.ignores.size());
		final DeterminationDIsPMResults.Ignore ignore = rapport.ignores.get(0);
		assertNotNull(ignore);
		assertEquals(DeterminationDIsPMResults.IgnoreType.PAS_ASSUJETTI, ignore.raison);

		// la tâche ne doit pas avoir été annulée
		assertNull(tache.getAnnulationDate());
		assertEmpty(rapport.traites);
	}

	/**
	 * [UNIREG-1984] Vérifie que les tâches qui chevauchent la période d'imposition théorique sont bien annulées
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAnnulationTacheCollisionPeriode() throws Exception {

		final PeriodeFiscale periode2014 = addPeriodeFiscale(2014);
		final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

		// Un contribuable non-assujetti, mais avec une déclaration d'impôt (invalide) pré-existante
		final Entreprise entreprise = addEntrepriseInconnueAuCivil();
		addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
		addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
		addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

		// la tâche a été générée à un moment où on croyait avoir un bouclement fin septembre 2014, mais en fait non...
		final RegDate dateTraitement = RegDate.get();
		final TacheEnvoiDeclarationImpotPM tache = addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, dateTraitement.addDays(-41), date(2014, 1, 1), date(2014, 9, 30),
		                                                             date(2014, 1, 1), date(2014, 9, 30), TypeContribuable.VAUDOIS_ORDINAIRE,
		                                                             TypeDocument.DECLARATION_IMPOT_PM_BATCH, entreprise, CategorieEntreprise.PM, oipm);
		hibernateTemplate.flush();

		final DeterminationDIsPMResults rapport = new DeterminationDIsPMResults(2014, dateTraitement, 1, tiersService, adresseService);
		service.traiterEntreprise(entreprise, periode2014, dateTraitement, rapport);

		assertEmpty(rapport.erreurs);
		assertEmpty(rapport.ignores);

		// une nouvelle tâche créée + une tâche annulée
		assertEquals(2, rapport.traites.size());
		assertEquals(DeterminationDIsPMResults.TraiteType.TACHE_ENVOI_CREEE, rapport.traites.get(0).raison);
		assertEquals(DeterminationDIsPMResults.TraiteType.TACHE_ENVOI_ANNULEE, rapport.traites.get(1).raison);

		// la tâche doit avoir été annulée
		assertNotNull(tache.getAnnulationDate());
	}

	/**
	 * [UNIREG-1984] Vérifie que les tâches déjà traitées sont ignorées et qu'une nouvelle tâche d'envoi de DI est bien générée
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIgnoreTacheTraitee() throws Exception {

		final PeriodeFiscale periode2014 = addPeriodeFiscale(2014);
		final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

		// Un contribuable non-assujetti, mais avec une déclaration d'impôt (invalide) pré-existante
		final Entreprise entreprise = addEntrepriseInconnueAuCivil();
		addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
		addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
		addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

		// tâche déjà traitée mais pas de DI correspondante...
		final RegDate dateTraitement = RegDate.get();
		final TacheEnvoiDeclarationImpotPM tache = addTacheEnvoiDIPM(TypeEtatTache.TRAITE, dateTraitement.addDays(-41), date(2014, 1, 1), date(2014, 12, 31),
		                                                             date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
		                                                             TypeDocument.DECLARATION_IMPOT_PM_BATCH, entreprise, CategorieEntreprise.PM, oipm);
		hibernateTemplate.flush();

		final DeterminationDIsPMResults rapport = new DeterminationDIsPMResults(2014, dateTraitement, 1, tiersService, adresseService);
		service.traiterEntreprise(entreprise, periode2014, dateTraitement, rapport);

		assertEmpty(rapport.erreurs);
		assertEmpty(rapport.ignores);

		// une nouvelle tâche d'envoi de DI doit avoir été créée (ce n'est pas parce qu'une tâche d'envoi de DI déjà traitée préexiste que la DI a été bien envoyée:
		// la DI a pu être annulée. Il est donc nécessaire de créer cette nouvelle tâche).
		assertEquals(1, rapport.traites.size());
		assertEquals(DeterminationDIsPMResults.TraiteType.TACHE_ENVOI_CREEE, rapport.traites.get(0).raison);

		// la tâche ne doit pas avoir été annulée
		assertNull(tache.getAnnulationDate());
	}

	/**
	 * [UNIREG-1417] Test qu'une déclaration annulée est bien ignorée
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIgnoreDeclarationAnnulee() throws Exception {

		final PeriodeFiscale periode2014 = addPeriodeFiscale(2014);
		final ModeleDocument modele2014 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, periode2014);
		final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

		// Un contribuable assujetti, mais avec une déclaration d'impôt (annulée) pré-existante
		final Entreprise entreprise = addEntrepriseInconnueAuCivil();
		addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
		addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
		addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
		addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

		// DI annulée
		addDeclarationImpot(entreprise, periode2014, date(2014, 1, 1), date(2014, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele2014).setAnnule(true);
		hibernateTemplate.flush();

		final RegDate dateTraitement = RegDate.get();
		final DeterminationDIsPMResults rapport = new DeterminationDIsPMResults(2014, dateTraitement, 1, tiersService, adresseService);
		service.traiterEntreprise(entreprise, periode2014, dateTraitement, rapport);

		assertEmpty(rapport.erreurs);
		assertEmpty(rapport.ignores);

		// une nouvelle tâche d'envoi de DI doit avoir été créée (la DI pré-existante a été annulée).
		assertEquals(1, rapport.traites.size());
		assertEquals(DeterminationDIsPMResults.TraiteType.TACHE_ENVOI_CREEE, rapport.traites.get(0).raison);
	}

	/**
	 * [SIFISC-17232] quand on lance le job de détermination des tâches d'envoi de DI, la spécification indique qu'en cas de mauvais
	 * type de document (PM vs. APM), la tâche existante doit être annulée puis recréée
	 */
	@Test
	public void testChangementDeTypeDocument() throws Exception {

		final long idpm = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				addPeriodeFiscale(2014);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

				// Un contribuable PM assujetti
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
				addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
				addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
				addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

				// tâche pour laquelle le type de document n'est pas le bon...
				addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, null, date(2014, 1, 1), date(2014, 12, 31),
				                  date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				                  TypeDocument.DECLARATION_IMPOT_APM_BATCH, entreprise, CategorieEntreprise.APM, oipm);
				return entreprise.getNumero();
			}
		});

		final DeterminationDIsPMResults rapport = service.run(2014, RegDate.get(), 1, null);

		assertEmpty(rapport.erreurs);
		assertEmpty(rapport.ignores);

		// la tâche existante doit avoir été annulée et une nouvelle tâche créée
		assertEquals(1, rapport.traites.size());
		assertEquals(DeterminationDIsPMResults.TraiteType.TACHE_ENVOI_CREEE, rapport.traites.get(0).raison);

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> taches = hibernateTemplate.find("select t from Tache t where t.contribuable.id=:idpm order by t.id", Collections.singletonMap("idpm", idpm), FlushMode.AUTO);
				assertNotNull(taches);
				assertEquals(2, taches.size());
				{
					final Tache tache = taches.get(0);
					assertNotNull(tache);
					assertInstanceOf(TacheEnvoiDeclarationImpotPM.class, tache);
					assertTrue(tache.isAnnule());

					final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
					assertEquals(date(2014, 1, 1), tacheEnvoi.getDateDebut());
					assertEquals(date(2014, 12, 31), tacheEnvoi.getDateFin());
					assertEquals(TypeDocument.DECLARATION_IMPOT_APM_BATCH, tacheEnvoi.getTypeDocument());
					assertEquals(CategorieEntreprise.APM, tacheEnvoi.getCategorieEntreprise());
				}
				{
					final Tache tache = taches.get(1);
					assertNotNull(tache);
					assertInstanceOf(TacheEnvoiDeclarationImpotPM.class, tache);
					assertFalse(tache.isAnnule());

					final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
					assertEquals(date(2014, 1, 1), tacheEnvoi.getDateDebut());
					assertEquals(date(2014, 12, 31), tacheEnvoi.getDateFin());
					assertEquals(TypeDocument.DECLARATION_IMPOT_PM_BATCH, tacheEnvoi.getTypeDocument());
					assertEquals(CategorieEntreprise.PM, tacheEnvoi.getCategorieEntreprise());
				}
			}
		});
	}

	/**
	 * [SIFISC-17232] quand on lance le job de détermination des tâches d'envoi de DI, il faut éventuellement recalculer
	 * la catégorie d'entreprise stockée dans la tâche (la spécification ne le dit pas, mais c'est assez évident, à mon sens)
	 */
	@Test
	public void testChangementDeCategorieEntreprise() throws Exception {

		final long idpm = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				addPeriodeFiscale(2014);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

				// Un contribuable PM assujetti
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
				addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
				addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
				addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

				// tâche pour laquelle les dates et le type de document sont bons, mais pas la catégorie d'entreprise
				addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, null, date(2014, 1, 1), date(2014, 12, 31),
				                  date(2014, 1, 1), date(2014, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				                  TypeDocument.DECLARATION_IMPOT_PM_BATCH, entreprise, CategorieEntreprise.APM, oipm);
				return entreprise.getNumero();
			}
		});

		final DeterminationDIsPMResults rapport = service.run(2014, RegDate.get(), 1, null);

		assertEmpty(rapport.erreurs);
		assertEmpty(rapport.traites);

		// la tâche existante doit avoir été marquée comme ignorée
		assertEquals(1, rapport.ignores.size());

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(idpm);
				assertNotNull(taches);
				assertEquals(1, taches.size());
				{
					final Tache tache = taches.get(0);
					assertNotNull(tache);
					assertInstanceOf(TacheEnvoiDeclarationImpotPM.class, tache);
					assertFalse(tache.isAnnule());

					final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
					assertEquals(date(2014, 1, 1), tacheEnvoi.getDateDebut());
					assertEquals(date(2014, 12, 31), tacheEnvoi.getDateFin());
					assertEquals(TypeDocument.DECLARATION_IMPOT_PM_BATCH, tacheEnvoi.getTypeDocument());
					assertEquals(CategorieEntreprise.PM, tacheEnvoi.getCategorieEntreprise());          // donnée corrigée
				}
			}
		});
	}

	/**
	 * [SIFISC-17363] Cas d'une DI sur l'année 2015 (émise avant la fin de l'année car le contribuable avait initialement une fin
	 * d'exercice commercial avant la fin de l'année, qu'il a décalée à fin 2015)
	 */
	@Test
	public void testDeclarationImpotExistanteMaisFinPosterieureADateLimiteBouclement() throws Exception {

		// mise en place fiscale
		final long idpm = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, false, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf = addPeriodeFiscale(2015);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

				// Un contribuable PM assujetti
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
				addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
				addBouclement(entreprise, date(2012, 12, 1), DayMonth.get(12, 31), 12);      // bouclements tous les ans depuis le 31.12.2012
				addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

				// DI migrée (= sans modèle de document) sur 2015
				addDeclarationImpot(entreprise, pf, date(2015, 1, 1), date(2015, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, null);
				return entreprise.getNumero();
			}
		});

		// vérification qu'aucune tâche n'est présente pour le moment sur ce contribuable (c'est le but du switch à "false"),,,
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(idpm);
				assertNotNull(taches);
				assertEquals(0, taches.size());
			}
		});

		// lancement du job avec une date de traitement avant la fin de la période de la DI existante...
		final DeterminationDIsPMResults results = service.run(2015, date(2015, 9, 30), 1, null);
		assertNotNull(results);
		assertEquals(1, results.ignores.size());
		assertEquals(0, results.erreurs.size());
		assertEquals(0, results.traites.size());            // en particulier pas de tâche d'annulation de la DI existante...

		final DeterminationDIsPMResults.Ignore ignore = results.ignores.get(0);
		assertNotNull(ignore);
		assertEquals(idpm, ignore.noCtb);
		assertEquals(DeterminationDIsPMResults.IgnoreType.AUCUN_BOUCLEMENT, ignore.raison);
	}

	/**
	 * [SIFISC-17413] Plusieurs exécution du job -> plusieurs tâches d'envoi identiques générées...
	 */
	@Test
	public void testPlusieursDeterminations() throws Exception {

		// mise en place fiscale
		final long idpm = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, false, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf = addPeriodeFiscale(2015);
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());

				// Un contribuable PM assujetti
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, date(2012, 4, 5), null, "Bricolage SARL");
				addFormeJuridique(entreprise, date(2012, 4, 5), null, FormeJuridiqueEntreprise.SARL);
				addBouclement(entreprise, date(2012, 10, 1), DayMonth.get(10, 31), 12);      // bouclements tous les ans depuis le 31.10.2012
				addRegimeFiscalVD(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, RegDate.get(2012, 4, 5), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2012, 4, 5), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);
				return entreprise.getNumero();
			}
		});

		// vérification qu'aucune tâche n'est présente pour le moment sur ce contribuable (c'est le but du switch à "false"),,,
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(idpm);
				assertNotNull(taches);
				assertEquals(0, taches.size());
			}
		});

		// lancement du job avec une date de traitement après la date de bouclement correspondante
		final DeterminationDIsPMResults results1 = service.run(2015, date(2016, 1, 1), 1, null);
		assertNotNull(results1);
		assertEquals(0, results1.ignores.size());
		assertEquals(0, results1.erreurs.size());
		assertEquals(1, results1.traites.size());

		final DeterminationDIsPMResults.Traite traite = results1.traites.get(0);
		assertNotNull(traite);
		assertEquals(date(2014, 11, 1), traite.dateDebut);
		assertEquals(date(2015, 10, 31), traite.dateFin);
		assertEquals(DeterminationDIsPMResults.TraiteType.TACHE_ENVOI_CREEE, traite.raison);
		assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, traite.typeContribuable);
		assertEquals(TypeDocument.DECLARATION_IMPOT_PM_BATCH, traite.typeDocument);

		// vérification en base -> une tâche d'envoi de DI
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(idpm);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertInstanceOf(TacheEnvoiDeclarationImpotPM.class, tache);
				assertFalse(tache.isAnnule());
			}
		});

		// nouveau lancement du job avec une date de traitement après la date de bouclement correspondante
		final DeterminationDIsPMResults results2 = service.run(2015, date(2016, 1, 1), 1, null);
		assertNotNull(results2);
		assertEquals(1, results2.ignores.size());       // car tâche déjà présente
		assertEquals(0, results2.erreurs.size());
		assertEquals(0, results2.traites.size());

		final DeterminationDIsPMResults.Ignore ignore = results2.ignores.get(0);
		assertNotNull(ignore);
		assertEquals(idpm, ignore.noCtb);
		assertEquals(DeterminationDIsPMResults.IgnoreType.TACHE_ENVOI_DEJA_EXISTANTE, ignore.raison);

		// vérification en base -> toujours une seule tâche d'envoi de DI
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(idpm);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertInstanceOf(TacheEnvoiDeclarationImpotPM.class, tache);
				assertFalse(tache.isAnnule());
			}
		});
	}
}
