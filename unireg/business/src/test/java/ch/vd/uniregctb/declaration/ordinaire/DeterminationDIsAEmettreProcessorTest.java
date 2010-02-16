package ch.vd.uniregctb.declaration.ordinaire;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeterminationDIsAEmettreProcessor.ExistenceResults;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.TypeContribuableDI;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheCriteria;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseRetour;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

@SuppressWarnings({"JavaDoc"})
public class DeterminationDIsAEmettreProcessorTest extends BusinessTest {

	private HibernateTemplate hibernateTemplate;
	private DeterminationDIsAEmettreProcessor service;
	private TacheDAO tacheDAO;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		final PeriodeFiscaleDAO periodeDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		final ParametreAppService parametres = getBean(ParametreAppService.class, "parametreAppService");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		service = new DeterminationDIsAEmettreProcessor(hibernateTemplate, periodeDAO, tacheDAO, parametres, transactionManager);

		// évite de logger plein d'erreurs pendant qu'on teste le comportement du processor
		final Logger serviceLogger = Logger.getLogger(DeclarationImpotServiceImpl.class);
		serviceLogger.setLevel(Level.FATAL);
	}

	/**
	 * [UNIREG-1742] L'itérateur doit retourner tous les contribuables ayant des fors ouverts dans l'année, et non plus seulement ceux ayant des fors ouverts au 31 décembre.
	 * [UNIREG-1742] Les contribuables possédant des déclarations d'impôt ou avec des tâches d'envoi de déclarations d'impôt en instance doivent aussi être retournés.
	 */
	@Test
	public void testCreateListeIdsContribuables() {

		final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		final ModeleDocument model2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
		addModeleFeuilleDocument("Déclaration", "210", model2007);
		addModeleFeuilleDocument("Annexe 1", "220", model2007);
		addModeleFeuilleDocument("Annexe 2-3", "230", model2007);
		addModeleFeuilleDocument("Annexe 4-5", "240", model2007);

		/*
		 * Contribuable devant être pris en compte
		 */

		// Un tiers avec un for ouvert à droite
		Contribuable paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);

		// Un tiers avec un for actif durant l'interval donné
		Contribuable incognito = addNonHabitant("Incog", "Nito", null, null);
		addForPrincipal(incognito, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2010, 12, 31), MotifFor.DEPART_HC, MockCommune.Lausanne);

		// Un tiers avec un mode d'imposition sourcier-mixte
		Contribuable arnold = addNonHabitant("Arnold", "Duplat", date(1965, 4, 13), Sexe.MASCULIN);
		ForFiscalPrincipal fors = addForPrincipal(arnold, date(1983, 4, 13), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		fors.setModeImposition(ModeImposition.MIXTE_137_2);

		// Un tiers avec un for principal hors canton, et avec un immeuble dans le canton
		Contribuable geo = addNonHabitant("Geo", "Trouverien", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(geo, date(1968, 11, 3), null, null, null, MockCommune.Neuchatel.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC,
				MotifRattachement.DOMICILE);
		addForSecondaire(geo, date(2003, 3, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);

		// [UNIREG-465] Un tiers avec un for principal hors canton, et avec un immeuble dans le canton qui a été vendu en cours d'année
		Contribuable johnny = addNonHabitant("Johnny", "Hallyday", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(johnny, date(2005, 11, 3), null, null, null, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC,
				MotifRattachement.DOMICILE);
		addForSecondaire(johnny, date(2005, 11, 3), MotifFor.ACHAT_IMMOBILIER, date(2007, 8, 30), MotifFor.VENTE_IMMOBILIER,
				MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		// [UNIREG-465][UNIREG-1742] Un tiers avec un for principal hors canton, et avec une activité indépendente dans le canton qui a été stoppé en cours d'année
		Contribuable tyler = createHorsCantonAvecFinActiviteIndependante(date(2007, 8, 30));

		// [UNIREG-1742] Un contribuable avec un for fermé en 2007
		Contribuable ralf = createDepartHorsCanton(date(2007, 7, 1));

		// [UNIREG-1742] Un contribuable hors-Suisse avec une activité indépendente dans le canton qui a été stoppé en cours d'année
		Contribuable armand = createHorsSuisseAvecFinActiviteIndependante(date(2007, 8, 30));

		// [UNIREG-1742] Un contribuable hors-Suisse avec un immeuble dans le canton qui a été vendu en cours d'année
		Contribuable alfred = createHorsSuisseAvecVenteImmeuble(date(2007, 8, 30));

		// [UNIREG-1742] Un non-assujetti avec une déclaration d'impôt
		Contribuable malko = addNonHabitant("Malko", "Totor", date(1955, 2, 11), Sexe.MASCULIN);
		addDeclarationImpot(malko, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, model2007);

		// [UNIREG-1742] Un non-assujetti avec une tâche d'envoi de déclaration d'impôt en instance
		Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
		addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
				eric, Qualification.AUTOMATIQUE);

		/*
		 * Contribuable devant être ignorés
		 */

		// Un tiers avec un for fermé avant 2007
		Contribuable pierre = addNonHabitant("Pierre", "Dubateau", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(pierre, date(1968, 11, 3), MotifFor.ARRIVEE_HC, date(1983, 7, 1), MotifFor.DEPART_HC, MockCommune.Lausanne);

		// Un tiers avec un seul for annulé
		Contribuable jean = addNonHabitant("Jean", "Duchmol", date(1948, 11, 3), Sexe.MASCULIN);
		fors = addForPrincipal(jean, date(1968, 11, 3), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		fors.setAnnulationDate(DateHelper.getDate(1967, 1, 1));

		// Un tiers avec un seul for hors canton
		Contribuable jeans = addNonHabitant("Jean", "Studer", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(jeans, date(1968, 11, 3), null, MockCommune.Neuchatel);

		// Un tiers avec un mode d'imposition sourcier pure
		Contribuable johan = addNonHabitant("Johan", "Smet", date(1965, 4, 13), Sexe.MASCULIN);
		fors = addForPrincipal(johan, date(1983, 4, 13), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		fors.setModeImposition(ModeImposition.SOURCE);

		// [UNIREG-1742] Un non-assujetti avec une déclaration d'impôt annulée
		Contribuable borg = addNonHabitant("Borg", "Totor", date(1955, 2, 11), Sexe.MASCULIN);
		DeclarationImpotOrdinaire di = addDeclarationImpot(borg, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, model2007);
		di.setAnnule(true);

		// [UNIREG-1742] Un non-assujetti avec une tâche d'envoi de déclaration d'impôt traitée
		Contribuable tom = addNonHabitant("Tom", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
		addTacheEnvoiDI(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
				tom, Qualification.AUTOMATIQUE);

		// [UNIREG-1742] Un non-assujetti avec une tâche d'envoi de déclaration d'impôt en instance mais annulée
		Contribuable tommy = addNonHabitant("Tommy", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
		TacheEnvoiDeclarationImpot tache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, tommy, Qualification.AUTOMATIQUE);
		tache.setAnnule(true);

		final List<Long> list = service.createListeIdsContribuables(2007);
		assertEquals(11, list.size());
		assertEquals(paul.getNumero(), list.get(0));
		assertEquals(incognito.getNumero(), list.get(1));
		assertEquals(arnold.getNumero(), list.get(2));
		assertEquals(geo.getNumero(), list.get(3));
		assertEquals(johnny.getNumero(), list.get(4));
		assertEquals(tyler.getNumero(), list.get(5));
		assertEquals(ralf.getNumero(), list.get(6));
		assertEquals(armand.getNumero(), list.get(7));
		assertEquals(alfred.getNumero(), list.get(8));
		assertEquals(malko.getNumero(), list.get(9));
		assertEquals(eric.getNumero(), list.get(10));
	}

	@Test
	public void testCreateListeIdsContribuablesAvecDeclarations() {

		PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
		PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		ModeleDocument declarationComplete2006 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
		addModeleFeuilleDocument("Déclaration", "210", declarationComplete2006);
		addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2006);
		addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2006);
		addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2006);
		ModeleDocument declarationComplete2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
		addModeleFeuilleDocument("Déclaration", "210", declarationComplete2007);
		addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2007);
		addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2007);
		addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2007);

		// Un tiers avec un for ouvert sans déclaration envoyée pour 2007
		Contribuable paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		{
			addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
			addDeclarationImpot(paul, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					declarationComplete2006);
		}

		// Un tiers avec un for ouvert et avec une déclaration déja envoyée pour 2007
		Contribuable pierre = addNonHabitant("Pierre", "Dubateau", date(1948, 11, 3), Sexe.MASCULIN);
		{
			addForPrincipal(pierre, date(1968, 11, 3), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
			addDeclarationImpot(pierre, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					declarationComplete2006);
			addDeclarationImpot(pierre, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					declarationComplete2007);
		}

		// Un contribuable hors-Suisse avec plusieurs achats et ventes d'immeuble dans l'année (= plusieurs périodes d'imposition) et autant de déclarations envoyées
		final Contribuable armand;
		{
			final Range activite1 = new Range(date(2007, 1, 18), date(2007, 3, 31));
			final Range activite2 = new Range(date(2007, 8, 1), date(2007, 11, 15));
			armand = createHorsSuisseAvecPlusieursDebutsEtFinsActivitesIndependantes(activite1, activite2);
			addDeclarationImpot(armand, periode2007, activite1.getDateDebut(), activite1.getDateFin(), TypeContribuable.HORS_SUISSE, declarationComplete2007);
			addDeclarationImpot(armand, periode2007, activite2.getDateDebut(), activite2.getDateFin(), TypeContribuable.HORS_SUISSE, declarationComplete2007);
		}

		final List<Long> list = service.createListeIdsContribuables(2007);
		assertEquals(3, list.size());
		assertEquals(paul.getNumero(), list.get(0));
		assertEquals(pierre.getNumero(), list.get(1)); // [UNIREG-1742] rattapage des déclarations : tous les contribuables avec fors doivent être pris en compte
		assertEquals(armand.getNumero(), list.get(2)); // [UNIREG-1742] rattapage des déclarations : tous les contribuables avec fors doivent être pris en compte
	}

	/**
	 * Les cas ci-dessous reflètent un problème de filtrage au niveau de la requête SQL
	 */
	@Test
	public void testDetermineDetailsEnvoiProblemeRequeteSql() throws Exception {

		// Un tiers sans aucun for
		Contribuable frederic = addNonHabitant("Frédéric", "Ragnar", date(1988, 10, 2), Sexe.MASCULIN);
		assertZeroDeclaration(service.determineDetailsEnvoi(frederic, 2007));

		// Un tiers sans for principal ou ni for secondaire mais avec un autre type de for
		Contribuable samuel = addNonHabitant("Samuel", "Ragnar", date(1988, 10, 2), Sexe.MASCULIN);
		addForAutreImpot(samuel, date(1968, 11, 3), null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				GenreImpot.SUCCESSION);
		assertZeroDeclaration(service.determineDetailsEnvoi(samuel, 2007));

		// Un tiers avec un for principal hors canton
		Contribuable jean = addNonHabitant("Jean", "Studer", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(jean, date(1968, 11, 3), null, null, null, MockCommune.Neuchatel.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC,
				MotifRattachement.DOMICILE);
		assertZeroDeclaration(service.determineDetailsEnvoi(jean, 2007));

		// Un tiers avec un mode d'imposition sourcier pure
		Contribuable johan = addNonHabitant("Johan", "Smet", date(1965, 4, 13), Sexe.MASCULIN);
		ForFiscalPrincipal fors = addForPrincipal(johan, date(1983, 4, 13), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		fors.setModeImposition(ModeImposition.SOURCE);
		assertZeroDeclaration(service.determineDetailsEnvoi(johan, 2007));

		// Un tiers avec un for secondaire bidon
		Contribuable yvette = addNonHabitant("Yvette", "Jolie", date(1965, 4, 13), Sexe.FEMININ);
		{
			ForFiscalSecondaire f = new ForFiscalSecondaire() {

				private static final long serialVersionUID = 2398344278141895238L;
				private MotifRattachement motif;

				/**
				 * Hyper-hack pour permettre de setter un motif de rattachement interdit (c'est dègue, mais on peut imaginer que quelqu'un
				 * fasse la même chose directement en SQL dans la base)
				 */
				@Override
				public void setMotifRattachement(MotifRattachement motif) {
					this.motif = motif;
				}

				@Override
				public MotifRattachement getMotifRattachement() {
					return this.motif;
				}
			};
			f.setDateDebut(date(1968, 11, 3));
			f.setDateFin(null);
			f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			f.setNumeroOfsAutoriteFiscale(MockCommune.Cossonay.getNoOFS());
			f.setMotifRattachement(MotifRattachement.PRESTATION_PREVOYANCE); // <- volontairement faux
			yvette.addForFiscal(f);
		}
		assertZeroDeclaration(service.determineDetailsEnvoi(yvette, 2007));
		yvette.getForsFiscaux().clear();
	}

	@Test
	public void testDetermineDetailsEnvoiCasDesDiplomates() throws Exception {

		// Un diplomate suisse en mission hors Suisse ne recoivent pas de DIs
		{
			Contribuable marc = addNonHabitant("Marc", "Ramatruelle", date(1948, 11, 3), Sexe.MASCULIN);
			addForPrincipal(marc, date(1968, 11, 3), MotifFor.MAJORITE, null, null, MockCommune.Lausanne.getNoOFS(),
					TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DIPLOMATE_SUISSE);
			assertZeroDeclaration(service.determineDetailsEnvoi(marc, 2007));
		}

		/*
		 * Un fonctionnaire international ou un diplomate étranger en mission en Suisse possédant un immeuble dans le canton recoivent sont
		 * assimilés à des contribuables vaudois et recoivent une DI normale.
		 */
		{
			Contribuable ramon = addNonHabitant("Ramon", "Zapapatotoche", date(1948, 11, 3), Sexe.MASCULIN);
			addForPrincipal(ramon, date(1968, 11, 3), null, null, null, MockPays.Espagne.getNoOFS(), TypeAutoriteFiscale.PAYS_HS,
					MotifRattachement.DIPLOMATE_ETRANGER);
			addForSecondaire(ramon, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
					MotifRattachement.IMMEUBLE_PRIVE);
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(
					ramon, 2007));
		}
	}

	/**
	 * Les cas ci-dessous reflètent une incohérence des données de la base
	 */
	@Test
	public void testDetermineDetailsEnvoiProblemeIncoherenceDonnees() throws Exception {

		loadDatabase("TestDetermineDetailsEnvoiProblemeIncoherenceDonnees.xml");

		/*
		 * Un tiers sans for principal mais avec immeuble et une adresse de résidence dans le canton (=> il devrait posséder un for
		 * principal dans ce cas-là)
		 */
		final Contribuable jean = (Contribuable) tiersService.getTiers(10000001);
		assertNull(service.determineDetailsEnvoi(jean, 2007));

		/*
		 * Un tiers avec un for principal et un rattachement différent de DOMICILE ou DIPLOMATE sur le for principal (ce qui est interdit)
		 */
		Contribuable michel = addNonHabitant("Michel", "Studer", date(1948, 11, 3), Sexe.MASCULIN);
		{
			ForFiscalPrincipal f = new ForFiscalPrincipal() {

				private static final long serialVersionUID = 2398344278141895238L;
				private MotifRattachement motif;

				// Hyper-hack pour permettre de setter un motif de rattachement interdit (c'est dègue, mais on peut imaginer que quelqu'un
				// fasse la même chose directement en SQL dans la base)
				@Override
				public void setMotifRattachement(MotifRattachement motif) {
					this.motif = motif;
				}

				@Override
				public MotifRattachement getMotifRattachement() {
					return this.motif;
				}
			};
			f.setDateDebut(date(1968, 11, 3));
			f.setMotifOuverture(MotifFor.ARRIVEE_HC);
			f.setDateFin(null);
			f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			f.setNumeroOfsAutoriteFiscale(MockCommune.Cossonay.getNoOFS());
			f.setMotifRattachement(MotifRattachement.ACTIVITE_INDEPENDANTE); // <- volontairement faux
			f.setModeImposition(ModeImposition.ORDINAIRE);
			michel.addForFiscal(f);
		}
		assertNull(service.determineDetailsEnvoi(michel, 2007));
		michel.getForsFiscaux().clear();
	}

	/**
	 * Teste des cas standards pour les contribuables avec fors principaux
	 */
	@Test
	public void testDetermineDetailsEnvoiPourTiersAvecForPrincipal() throws Exception {

		PeriodeFiscale periode2006 = addPeriodeFiscale(2006);
		ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
		addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
		addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
		addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
		addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);
		ModeleDocument declarationVaudTax = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2006);
		addModeleFeuilleDocument("Déclaration vaud tax", "250", declarationVaudTax);

		// Un tiers imposé à la dépense
		Contribuable paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		ForFiscalPrincipal fors = addForPrincipal(paul, date(1983, 4, 13), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		fors.setModeImposition(ModeImposition.DEPENSE);
		assertDetails(TypeContribuableDI.VAUDOIS_DEPENSE, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(paul,
				2007));

		// Un tiers tout ce quil y a de plus ordinaire
		Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
		addDeclarationImpot(eric, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				declarationComplete);
		assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(eric,
				2007));

		// Un tiers ordinaire, mais sans déclaration d'impôt précédente
		Contribuable olrik = addNonHabitant("Olrick", "Pasgentil", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(olrik, date(1983, 4, 13), MotifFor.MAJORITE, null, null, MockCommune.Lausanne);
		assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(olrik,
				2007));

		// Un tiers ordinaire mais avec VaudTax
		Contribuable guillaume = addNonHabitant("Guillaume", "Portes", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(guillaume, date(1983, 4, 13), MotifFor.MAJORITE, null, null, MockCommune.Lausanne);
		addDeclarationImpot(guillaume, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				declarationVaudTax);
		assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX, date(2007, 1, 1), date(2007, 12, 31), service
				.determineDetailsEnvoi(guillaume, 2007));

	}

	/**
	 * Teste des cas standards pour les contribuables avec fors secondaires
	 */
	@Test
	public void testDetermineDetailsEnvoiPourTiersAvecForsSecondaires() throws Exception {

		// contribuable hors canton ayant une activité indépendante dans le canton
		Contribuable jean = addNonHabitant("Jean", "Glasfich", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(jean, date(1968, 11, 3), null, null, null, MockCommune.Neuchatel);
		addForSecondaire(jean, date(1968, 11, 3), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(),
				MotifRattachement.ACTIVITE_INDEPENDANTE);
		addAdresseSuisse(jean, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, MockRue.Neuchatel.RueDesBeauxArts);
		assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(jean,
				2007));

		// contribuable hors canton ayant une activité indépendante dans le canton, ainsi qu'un autre type de for
		Contribuable jacques = addNonHabitant("Jacques", "Glasfich", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(jacques, date(1968, 11, 3), null, null, null, MockCommune.Neuchatel);
		addForSecondaire(jacques, date(1968, 11, 3), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(),
				MotifRattachement.ACTIVITE_INDEPENDANTE);
		addForAutreImpot(jacques, date(1968, 11, 3), null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				GenreImpot.DONATION);
		addAdresseSuisse(jacques, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, MockRue.Neuchatel.RueDesBeauxArts);
		assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(
				jacques, 2007));

		// contribuable hors Suisse ayant une activité indépendante dans le canton
		Contribuable mitt = addNonHabitant("Mitt", "Romney", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(mitt, date(1968, 11, 3), null, null, null, MockPays.Danemark);
		addForSecondaire(mitt, date(1968, 11, 3), MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(),
				MotifRattachement.ACTIVITE_INDEPENDANTE);
		addAdresseEtrangere(mitt, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, MockPays.Danemark);
		assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(mitt,
				2007));

		// contribuable propriétaire d'immeubles privés sis dans le canton et domiciliée hors canton
		Contribuable georges = addNonHabitant("Georges", "Delatchaux", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(georges, date(1968, 11, 3), null, null, null, MockCommune.Neuchatel);
		addForSecondaire(georges, date(1968, 11, 3), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);
		addAdresseSuisse(georges, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, MockRue.Neuchatel.RueDesBeauxArts);
		assertDetails(TypeContribuableDI.HORS_CANTON, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(georges,
				2007));

		// contribuable propriétaire d'immeubles privés sis dans le canton et domiciliée hors Suisse
		Contribuable jacky = addNonHabitant("Jacky", "Galager", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(jacky, date(1968, 11, 3), null, null, null, MockPays.Danemark);
		addForSecondaire(jacky, date(1968, 11, 3), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE);
		addAdresseEtrangere(jacky, TypeAdresseTiers.DOMICILE, date(1968, 11, 3), null, MockPays.Danemark);
		assertZeroDeclaration(service.determineDetailsEnvoi(jacky, 2007));
	}

	/**
	 * Teste des cas standards pour les contribuables avec fors principaux
	 */
	@Test
	public void testDetermineDetailsEnvoiPourTiersDepartHorsCantonFinPeriode() throws Exception {

		PeriodeFiscale periode2006 = addPeriodeFiscale(2006);


		ModeleDocument declarationComplete = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2006);
		addModeleFeuilleDocument("Déclaration", "210", declarationComplete);
		addModeleFeuilleDocument("Annexe 1", "220", declarationComplete);
		addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete);
		addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete);
		ModeleDocument declarationVaudTax = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2006);
		addModeleFeuilleDocument("Déclaration vaud tax", "250", declarationVaudTax);


		// Un tiers qui part hors canton le 31.12
		Contribuable laurent = addNonHabitant("Laurent", "Maillard", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(laurent, date(1983, 4, 13), MotifFor.MAJORITE,date(2006,12,31),MotifFor.DEPART_HC, MockCommune.Lausanne);
		addDeclarationImpot(laurent, periode2006, date(2006, 1, 1), date(2006, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				declarationComplete);

		assertZeroDeclaration(service.determineDetailsEnvoi(laurent, 2007));
		assertAbsenceTachesAnnulationDI(laurent,2006);
	}

	@Test
	public void testDetermineDetailsEnvoiPourTiersAvecForsSansMotifsOuvertureFermeture() throws Exception {

		// Un tiers avec for ouvert pendant l'année mais sans motif
		Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
		ForFiscalPrincipal f = addForPrincipal(eric, date(2007, 4, 13), MotifFor.ARRIVEE_HC, null, null, MockCommune.Lausanne);
		f.setMotifOuverture(null); // hack pour bypasser la validation
		assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(eric,
				2007));
	}

	@Test
	public void testDetermineDetailsEnvoiPourTiersHorsCantonAyantVenduSonDernierImmeubleEnCoursDAnnee() throws Exception {

		/*
		 * [UNIREG-465] Un tiers hors-canton qui a vendu son dernier immeuble en cours d'année : il ne reçoit pas automatiquement de DI
		 * suite à la vente de son immeuble, et doit recevoir une DI au début de l'année suivante
		 */
		Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(eric, date(1998, 4, 13), null, null, null, MockCommune.Bern);
		addForSecondaire(eric, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2007, 5, 30), MotifFor.VENTE_IMMOBILIER,
				MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		// [UNIREG-1742] pas de déclaration envoyée pour les contribuables domiciliés dans un autre canton dont le rattachement
		// économique (activité indépendante ou immeuble) s’est terminé au cours de la période fiscale
		assertZeroDeclaration(service.determineDetailsEnvoi(eric, 2007));
	}

	@Test
	public void testDetermineDetailsEnvoiPourHorsSuisseAvecImmeuble() throws Exception {

		{
			// [UNIREG-465] Contribuable hors-suisse depuis toujours avec immeuble dans le canton
			Contribuable salvatore = addNonHabitant("Salvatore", "Adamo", date(1965, 4, 13), Sexe.MASCULIN);
			addForPrincipal(salvatore, date(1998, 4, 13), null, null, null, MockPays.Albanie);
			addForSecondaire(salvatore, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2007, 5, 30), MotifFor.VENTE_IMMOBILIER,
					MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
			assertZeroDeclaration(service.determineDetailsEnvoi(salvatore, 2000)); // [UNIREG-1742] pas de DI, même la première année
			assertZeroDeclaration(service.determineDetailsEnvoi(salvatore, 2001)); // pas de DI
			assertZeroDeclaration(service.determineDetailsEnvoi(salvatore, 2002)); // pas de DI
			assertZeroDeclaration(service.determineDetailsEnvoi(salvatore, 2003)); // pas de DI
			assertZeroDeclaration(service.determineDetailsEnvoi(salvatore, 2004)); // pas de DI
			assertZeroDeclaration(service.determineDetailsEnvoi(salvatore, 2005)); // pas de DI
			assertZeroDeclaration(service.determineDetailsEnvoi(salvatore, 2006)); // pas de DI
			assertDetails(TypeContribuableDI.HORS_SUISSE, date(2007, 1, 1), date(2007, 5, 30), service.determineDetailsEnvoi(salvatore, 2007)); // vente de l'immeuble + rattrapage -> DI
		}

		{
			// [UNIREG-465] Contribuable hors-suisse depuis 2007 avec immeuble dans le canton
			Contribuable greg = addNonHabitant("Grégoire", "Wriztjk", date(1965, 4, 13), Sexe.MASCULIN);
			addForPrincipal(greg, date(1998, 4, 13), MotifFor.MAJORITE, date(2007, 7, 1), MotifFor.DEPART_HS, MockCommune.Lausanne);
			addForPrincipal(greg, date(2007, 7, 2), MotifFor.DEPART_HS, MockPays.Espagne);
			addForSecondaire(greg, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2005, 1, 1), date(2005, 12, 31), service.determineDetailsEnvoi(greg, 2005));
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2006, 1, 1), date(2006, 12, 31), service.determineDetailsEnvoi(greg, 2006));
			// [UNIREG-1742] rattrapage de la DI qui aurait dû être émise automatiquement lors du départ
			assertDetails(TypeContribuableDI.HORS_SUISSE, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(greg, 2007));
			assertZeroDeclaration(service.determineDetailsEnvoi(greg, 2008)); // DI optionnelle pour les hors-Suisse avec immeuble
		}
	}

	/**
	 * [UNIREG-1742] Cas des contribuables dont l'assujettissement s'est interrompu en cours d'année, qui aurait dû recevoir une déclaration d'impôt immédiatement mais qui n'en ont pas reçu -> le batch
	 * doit leur envoyer des déclarations.
	 */
	@Test
	public void testDetermineDetailsEnvoiRattrapageContribuables() throws Exception {

		final RegDate dateFin = date(2007, 8, 30);

		final Contribuable tyler = createHorsCantonAvecFinActiviteIndependante(dateFin);
		{
			final DeterminationDIsResults rapport = new DeterminationDIsResults(2007, date(2008, 1, 30));
			service.setRapport(rapport);
			assertZeroDeclaration(service.determineDetailsEnvoi(tyler, 2007)); // déclaration remplacée par une note à l'administration fiscale
			assertEquals(1, rapport.ignores.size());
			final DeterminationDIsResults.Ignore ignore = rapport.ignores.get(0);
			assertEquals(DeterminationDIsResults.IgnoreType.REMPLACEE_PAR_NOTE, ignore.raison);
		}

		final Contribuable ralf = createDepartHorsCanton(date(2007, 7, 1));
		assertZeroDeclaration(service.determineDetailsEnvoi(ralf, 2007));  // pas de déclaration dans ce cas

		final Contribuable armand = createHorsSuisseAvecFinActiviteIndependante(dateFin);
		assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2007, 1, 1), dateFin, service.determineDetailsEnvoi(armand, 2007));

		final Contribuable alfred = createHorsSuisseAvecVenteImmeuble(dateFin);
		assertDetails(TypeContribuableDI.HORS_SUISSE, date(2007, 1, 1), dateFin, service.determineDetailsEnvoi(alfred, 2007));
	}

	@Test
	public void testDetermineDetailsEnvoiRattrapagePlusieursPeriodesImpositions() throws Exception {

		final Range activite1 = new Range(date(2007, 1, 18), date(2007, 3, 31));
		final Range activite2 = new Range(date(2007, 8, 1), date(2007, 11, 15));
		final Contribuable armand = createHorsSuisseAvecPlusieursDebutsEtFinsActivitesIndependantes(activite1, activite2);

		final List<PeriodeImposition> details = service.determineDetailsEnvoi(armand, 2007);
		assertNotNull(details);
		assertEquals(2, details.size());
		assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX, activite1.getDateDebut(), activite1.getDateFin(), details.get(0));
		assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX, activite2.getDateDebut(), activite2.getDateFin(), details.get(1));
	}

	/**
	 * @return contribuable hors-Suisse avec un immeuble dans le canton qui a été vendu en cours d'année
	 */
	private Contribuable createHorsSuisseAvecVenteImmeuble(RegDate dateVente) {
		Contribuable alfred = addNonHabitant("Alfred", "Hallyday", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(alfred, date(2005, 11, 3), null, MockPays.France);
		addForSecondaire(alfred, date(2005, 11, 3), MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		return alfred;
	}

	/**
	 * @return un contribuable hors-Suisse avec une activité indépendente dans le canton qui a été stoppée en cours d'année
	 */
	private Contribuable createHorsSuisseAvecFinActiviteIndependante(RegDate dateFin) {
		Contribuable armand = addNonHabitant("Armand", "Brulé", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(armand, date(2005, 11, 3), null, MockPays.Danemark);
		addForSecondaire(armand, date(2005, 11, 3), MotifFor.DEBUT_EXPLOITATION, dateFin, MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		return armand;
	}

	/**
	 * @return un contribuable hors-Suisse avec plusieurs activités indépendentes dans le canton qui a été démarrées et stoppées
	 */
	private Contribuable createHorsSuisseAvecPlusieursDebutsEtFinsActivitesIndependantes(DateRange... ranges) {
		Contribuable armand = addNonHabitant("Armand", "Brulé", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(armand, date(2005, 11, 3), null, MockPays.Danemark);
		for (DateRange r : ranges) {
			addForSecondaire(armand, r.getDateDebut(), MotifFor.DEBUT_EXPLOITATION, r.getDateFin(), MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne.getNoOFS(),
					MotifRattachement.ACTIVITE_INDEPENDANTE);
		}
		return armand;
	}

	/**
	 * @return un contribuable avec un for fermé en 2007
	 */
	private Contribuable createDepartHorsCanton(RegDate dateDepart) {
		Contribuable ralf = addNonHabitant("Ralf", "Dubateau", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(ralf, date(1968, 11, 3), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HC, MockCommune.Lausanne);
		return ralf;
	}

	/**
	 * @return un contribuable hors-canton avec une activité indépendente dans le canton qui a été stoppé en cours d'année
	 */
	private Contribuable createHorsCantonAvecFinActiviteIndependante(RegDate dateFin) {
		Contribuable tyler = addNonHabitant("Tyler", "Brulé", date(1948, 11, 3), Sexe.MASCULIN);
		addForPrincipal(tyler, date(2005, 11, 3), null, MockCommune.Bern);
		addForSecondaire(tyler, date(2005, 11, 3), MotifFor.DEBUT_EXPLOITATION, dateFin, MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
		return tyler;
	}

	@Test
	public void testTraiterPeriodeImposition() throws Exception {

		final PeriodeFiscale periode = addPeriodeFiscale(2007);
		ModeleDocument declarationComplete2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		addModeleFeuilleDocument("Déclaration", "210", declarationComplete2007);
		addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2007);
		addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2007);
		addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2007);

		// Un contribuable normal
		{
			Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);

			final List<PeriodeImposition> periodes = PeriodeImposition.determine(eric, 2007);
			assertEquals(1, periodes.size());
			final PeriodeImposition periodeImposition = periodes.get(0);

			TacheEnvoiDeclarationImpot tacheEric = service.traiterPeriodeImposition(eric, periode, periodeImposition);
			assertNotNull(tacheEric);
			assertTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, tacheEric);
		}

		// Un autre contribuable normal
		{
			Contribuable john = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
			addForPrincipal(john, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);

			final List<PeriodeImposition> periodes = PeriodeImposition.determine(john, 2007);
			assertEquals(1, periodes.size());
			final PeriodeImposition periodeImposition = periodes.get(0);

			TacheEnvoiDeclarationImpot tacheJohn = service.traiterPeriodeImposition(john, periode, periodeImposition);
			assertNotNull(tacheJohn);
			assertTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, tacheJohn);
		}
	}

	/**
	 * [UNIREG-1417] 
	 */
	@Test
	public void testTraiterPeriodeImpositionTacheDejaTraite() throws Exception {

		final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);
		ModeleDocument declarationComplete2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2009);
		addModeleFeuilleDocument("Déclaration", "210", declarationComplete2009);
		addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2009);
		addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2009);
		addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2009);

		// Un contribuable normal
		Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);

		addTacheEnvoiDI(TypeEtatTache.TRAITE, date(2010, 1, 15), date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, eric, null);
		hibernateTemplate.flush();

		final List<PeriodeImposition> periodes = PeriodeImposition.determine(eric, 2009);
		assertEquals(1, periodes.size());
		final PeriodeImposition periodeImposition = periodes.get(0);

		TacheEnvoiDeclarationImpot tacheEric = service.traiterPeriodeImposition(eric, periode2009, periodeImposition);
		assertNotNull(tacheEric);
		assertTache(TypeEtatTache.EN_INSTANCE, date(2010, 1, 31), date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.CEDI, tacheEric);
	}

	/**
	 * Essai de duplication de tâches (qui ne doit pas être possible)
	 */
	@Test
	public void testTraiterPeriodeImpositionDuplication() throws Exception {

		final PeriodeFiscale periode = addPeriodeFiscale(2007);
		ModeleDocument declarationComplete2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
		addModeleFeuilleDocument("Déclaration", "210", declarationComplete2007);
		addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2007);
		addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2007);
		addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2007);

		// Un contribuable normal avec une tâche d'envoi de DI pré-existante
		Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
		ForFiscalPrincipal ffp = addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
		addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 15), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, eric, null);
		hibernateTemplate.flush();

		// Nouvelle tâches avec assujettissement sur toute l'année 2007
		{
			final List<PeriodeImposition> periodes = PeriodeImposition.determine(eric, 2007);
			assertEquals(1, periodes.size());
			final PeriodeImposition tout2007 = periodes.get(0);
			assertNull(service.traiterPeriodeImposition(eric, periode, tout2007)); // null parce que la tâche d'envoi existe déjà
		}

		// Nouvelle tâches avec assujettissement sur un partie de l'année 2007 seulement
		{
			ffp.setDateFin(date(2007, 8, 10));
			ffp.setMotifFermeture(MotifFor.VEUVAGE_DECES);

			final List<PeriodeImposition> periodes = PeriodeImposition.determine(eric, 2007);
			assertEquals(1, periodes.size());

			final PeriodeImposition partie2007 = periodes.get(0);
			assertEquals(date(2007, 8, 10), partie2007.getDateFin());
			// [UNIREG-1984] la tâche d'envoi pré-xistante devra être annulée (dans un post-processing) et une nouvelle tâche d'envoi de DI doit avoir été déterminée
			assertTache(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 8, 10), TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, TypeAdresseRetour.ACI, service.traiterPeriodeImposition(eric, periode, partie2007));
		}
	}

	@Test
	public void testDetermineDetailsEnvoi() throws Exception {

		// Contribuable vaudois sans aucun for
		{
			Contribuable ctb = addNonHabitant("Junior", "Bolomey", date(2004, 1, 1), Sexe.MASCULIN);
			assertNull(service.determineDetailsEnvoi(ctb, 2007));
		}

		// Contribuable vaudois présent dans le canton depuis plusieurs années
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(1987, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(
					ctb, 2007));
		}

		/*
		 * Contribuable vaudois présent dans le canton depuis plusieurs années, mais qui a déménagé début janvier de l'année suivante dans
		 * un autre canton
		 */
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(1987, 1, 1), MotifFor.ARRIVEE_HC, date(2008, 1, 5), MotifFor.DEPART_HC, MockCommune.Lausanne);
			addForPrincipal(ctb, date(2008, 1, 6), null, null, null, MockCommune.Neuchatel);
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(
					ctb, 2007));
		}

		// Contribuable vaudois présent dans le canton depuis plusieurs années, mais sans motif d'ouverture connu
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			ForFiscalPrincipal f = addForPrincipal(ctb, date(1987, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Lausanne);
			f.setMotifOuverture(null); // hack pour bypasser la validation
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(
					ctb, 2007));
		}

		// Contribuable vaudois arrivé dans le canton en cours d'année depuis un autre canton
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(2007, 3, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(
					ctb, 2007));
		}

		// Contribuable vaudois arrivé dans le canton en cours d'année depuis un autre pays
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(2007, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX, date(2007, 3, 1), date(2007, 12, 31), service.determineDetailsEnvoi(
					ctb, 2007));
		}

		// Contribuable vaudois arrivé dans le canton en cours d'année depuis un autre pays mais possédant un immeuble depuis plusieurs
		// années
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(2005, 1, 1), null, date(2007, 2, 28), null, MockPays.Danemark);
			addForPrincipal(ctb, date(2007, 3, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addForSecondaire(ctb, date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(ctb, 2007));
		}

		// Contribuable vaudois arrivé dans le canton en cours d'année depuis un autre pays et ayant changé de commune entre-deux
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(2007, 3, 1), MotifFor.ARRIVEE_HS, date(2007, 6, 30), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
			addForPrincipal(ctb, date(2007, 7, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Bex);
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX, date(2007, 3, 1), date(2007, 12, 31), service.determineDetailsEnvoi(
					ctb, 2007));
		}

		// Contribuable vaudois arrivé dans le canton depuis un autre pays l'année précédente et ayant changé de commune en cours d'année
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(2006, 3, 1), MotifFor.ARRIVEE_HS, date(2007, 6, 30), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
			addForPrincipal(ctb, date(2007, 7, 1), MotifFor.DEMENAGEMENT_VD, MockCommune.Bex);
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(
					ctb, 2007));
		}

		// Contribuable vaudois dont le conjoint est décéde en cours d'année
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(2007, 3, 22), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX, date(2007, 3, 22), date(2007, 12, 31), service.determineDetailsEnvoi(
					ctb, 2007));
		}

		// Contribuable vaudois parti dans un autre canton en cours d'année
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(1998, 1, 1), MotifFor.ARRIVEE_HS, date(2007, 12, 1), MotifFor.DEPART_HC, MockCommune.Lausanne);
			addForPrincipal(ctb, date(2007, 12, 2), MotifFor.DEPART_HC, MockCommune.Neuchatel);
			assertNull(service.determineDetailsEnvoi(ctb, 2007));
		}

		// Contribuable vaudois parti dans un autre canton en cours d'année, mais possédant un immeuble dans le canton depuis plusieurs
		// années
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(1998, 1, 1), MotifFor.ARRIVEE_HS, date(2007, 12, 1), MotifFor.DEPART_HC, MockCommune.Lausanne);
			addForPrincipal(ctb, date(2007, 12, 2), MotifFor.DEPART_HC, MockCommune.Neuchatel);
			addForSecondaire(ctb, date(2001, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
					MotifRattachement.IMMEUBLE_PRIVE);
			assertDetails(TypeContribuableDI.HORS_CANTON, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(ctb,
					2007));
		}

		// Contribuable vaudois parti dans un autre canton en cours d'année, mais ayant acquis un immeuble dans le canton en cours d'année
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(1998, 1, 1), MotifFor.ARRIVEE_HS, date(2007, 12, 1), MotifFor.DEPART_HC, MockCommune.Lausanne);
			addForPrincipal(ctb, date(2007, 12, 2), MotifFor.DEPART_HC, MockCommune.Neuchatel);
			addForSecondaire(ctb, date(2007, 4, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Lausanne.getNoOFS(),
					MotifRattachement.IMMEUBLE_PRIVE);
			assertDetails(TypeContribuableDI.HORS_CANTON, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(ctb,
					2007));
		}

		// Contribuable vaudois parti dans un autre pays en cours d'année, sans for secondaire
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(1998, 1, 1), MotifFor.ARRIVEE_HS, date(2007, 12, 1), MotifFor.DEPART_HS, MockCommune.Lausanne);
			addForPrincipal(ctb, date(2007, 12, 2), MotifFor.DEPART_HS, MockPays.Danemark);
			// [UNIREG-1742] rattrapage de la DI
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2007, 1, 1), date(2007, 12, 1), service.determineDetailsEnvoi(ctb,	2007));
		}

		// Contribuable vaudois décédé en cours d'année
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(1998, 1, 1), MotifFor.ARRIVEE_HS, date(2007, 12, 1), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
			// [UNIREG-1742] rattrapage de la DI
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2007, 1, 1), date(2007, 12, 1), service.determineDetailsEnvoi(ctb,	2007));
		}

		// Contribuable vaudois marié en cours d'année (non testé ici : le ménage commun)
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(1998, 1, 1), MotifFor.ARRIVEE_HS, date(2007, 12, 1),
					MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
			assertNull(service.determineDetailsEnvoi(ctb, 2007));
		}

		// Contribuable vaudois dont le for principal s'arrête en cours d'année pour une raison inconnu
		{
			Contribuable ctb = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			ForFiscalPrincipal f = addForPrincipal(ctb, date(1998, 1, 1), MotifFor.ARRIVEE_HS, date(2007, 12, 1), MotifFor.DEPART_HS,
					MockCommune.Lausanne);
			f.setMotifFermeture(null); // hack pour bypasser la validation
			assertNull(service.determineDetailsEnvoi(ctb, 2007));
		}

		// Contribuable hors-canton avec un for secondaire immeuble dans le canton
		{
			Contribuable ctb = addNonHabitant("Raoul", "Lavanchy", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(1998, 1, 1), MotifFor.MAJORITE, MockCommune.Bern);
			addForSecondaire(ctb, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Cossonay.getNoOFS(),
					MotifRattachement.IMMEUBLE_PRIVE);
			assertDetails(TypeContribuableDI.HORS_CANTON, date(2007, 1, 1), date(2007, 12, 31), service.determineDetailsEnvoi(ctb,
					2007));
		}

		// Contribuable hors-canton avec un for secondaire activité indépendante dans le canton qui a été stoppée dans l'année
		{
			Contribuable ctb = addNonHabitant("Raoul", "Lavanchy", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(1998, 1, 1), MotifFor.MAJORITE, MockCommune.Bern);
			addForSecondaire(ctb, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, date(2007, 3, 4), MotifFor.FIN_EXPLOITATION,
					MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
			// [UNIREG-1742] pas de déclaration envoyée pour les contribuables domiciliés dans un autre canton dont le rattachement
			// économique (activité indépendante ou immeuble) s’est terminé au cours de la période fiscale
			assertZeroDeclaration(service.determineDetailsEnvoi(ctb, 2007));
		}

		// Contribuable hors-Suisse avec un for secondaire activité indépendante dans le canton qui a été stoppée dans l'année
		{
			Contribuable ctb = addNonHabitant("Raoul", "Lavanchy", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(1998, 1, 1), null, MockPays.Danemark);
			addForSecondaire(ctb, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, date(2007, 3, 4), MotifFor.FIN_EXPLOITATION,
					MockCommune.Cossonay.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);
			// [UNIREG-1742] rattrapage de la DI
			assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE, date(2007, 1, 1), date(2007, 3, 4), service.determineDetailsEnvoi(ctb, 2007));
		}

		// [UNIREG-465] Contribuable hors-canton avec un for secondaire immeuble dans le canton qui a été fermé dans l'année
		{
			Contribuable ctb = addNonHabitant("Raoul", "Lavanchy", date(1963, 1, 1), Sexe.MASCULIN);
			addForPrincipal(ctb, date(1998, 1, 1), MotifFor.MAJORITE, MockCommune.Bern);
			addForSecondaire(ctb, date(2000, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2007, 3, 4), MotifFor.VENTE_IMMOBILIER,
					MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
			// [UNIREG-1742] pas de déclaration envoyée pour les contribuables domiciliés dans un autre canton dont le rattachement
			// économique (activité indépendante ou immeuble) s’est terminé au cours de la période fiscale
			assertZeroDeclaration(service.determineDetailsEnvoi(ctb, 2007));
		}
	}

	@Test
	public void testCheckTacheExistence() {

		// Aucune tâche
		{
			final Contribuable contribuable = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);

			Range range = new Range(date(2007, 1, 1), date(2007, 12, 31));
			assertNull(service.checkExistenceTache(contribuable, range));
		}

		// Une tâche qui existe déjà avec un range qui ne correspond pas du tout
		{
			final Contribuable contribuable = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 5, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, contribuable, null);

			Range range = new Range(date(2007, 7, 1), date(2007, 12, 31));
			assertNull(service.checkExistenceTache(contribuable, range));
		}

		// Une tâche qui existe déjà avec un range qui correspond parfaitement
		{
			final Contribuable contribuable = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31),
					TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, contribuable, null);
			hibernateTemplate.flush();

			Range range = new Range(date(2007, 1, 1), date(2007, 12, 31));
			final ExistenceResults<TacheEnvoiDeclarationImpot> results = service.checkExistenceTache(contribuable, range);
			assertNotNull(results);
			assertEquals(ExistenceResults.TacheStatus.EXISTE_DEJA, results.status);
			assertEquals(date(2007, 1, 1), results.object.getDateDebut());
			assertEquals(date(2007, 12, 31), results.object.getDateFin());
		}

		// Une tâche qui existe déjà avec un range qui recouvre partiellement le range spécifié
		{
			final Contribuable contribuable = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 5, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, contribuable, null);
			hibernateTemplate.flush();

			Range range = new Range(date(2007, 1, 1), date(2007, 12, 31));
			final ExistenceResults<TacheEnvoiDeclarationImpot> results = service.checkExistenceTache(contribuable, range);
			assertNotNull(results);
			assertEquals(ExistenceResults.TacheStatus.INTERSECTE, results.status);
			assertEquals(date(2007, 1, 1), results.object.getDateDebut());
			assertEquals(date(2007, 5, 31), results.object.getDateFin());
		}


		// Une tâche déjà traitée avec un range qui recouvre partiellement le range spécifié
		{
			final Contribuable contribuable = addNonHabitant("Werner", "Karey", date(1963, 1, 1), Sexe.MASCULIN);
			addTacheEnvoiDI(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 5, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
					TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, contribuable, null);
			hibernateTemplate.flush();

			Range range = new Range(date(2007, 1, 1), date(2007, 12, 31));
			assertNull(service.checkExistenceTache(contribuable, range)); // on doit ignorer les tâches déjà traitées
		}
	}

	@Test
	public void testDILibres() throws Exception {

		class Ids {
			Long periodeId;
			Long arnoldId;
			Long malkoId;
			Long salvatoreId;
			Long ericId;
			Long gregId;
		}
		final Ids ids = new Ids();

		final RegDate dateDepart = date(2008, 5, 3);

		doInNewTransaction(new TxCallback(){
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				final ModeleDocument declarationComplete2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2008);
				ids.periodeId = periode2008.getId();

				// cas #1: un tiers avec une DI libre "honorée" (cas simple)
				Contribuable arnold = addNonHabitant("Arnold", "Charbon", date(1965, 4, 13), Sexe.MASCULIN);
				ids.arnoldId = arnold.getNumero();
				addForPrincipal(arnold, date(1983, 4, 13), MotifFor.MAJORITE, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
				addDeclarationImpot(arnold, periode2008, date(2008, 1, 1), dateDepart, TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete2008);

				// cas #2: un tiers avec une DI libre, parti après (cas d'erreur)
				Contribuable malko = addNonHabitant("Malko", "Totor", date(1955, 2, 11), Sexe.MASCULIN);
				ids.malkoId = malko.getNumero();
				addForPrincipal(malko, date(1983, 4, 13), MotifFor.ARRIVEE_HC, date(2008, 6, 15), MotifFor.DEPART_HS, MockCommune.Lausanne);
				addDeclarationImpot(malko, periode2008, date(2008, 1, 1), dateDepart, TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete2008);

				// cas #3: un tiers avec une DI libre, jamais parti (cas d'erreur)
				Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addDeclarationImpot(eric, periode2008, date(2008, 1, 1), dateDepart, TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete2008);

				// cas #4: un tiers avec un DI libre, parti puis revenu (cas de traitement normal)
				Contribuable salvatore = addNonHabitant("Salvatore", "Adamo", date(1965, 4, 13), Sexe.MASCULIN);
				ids.salvatoreId = salvatore.getNumero();
				addForPrincipal(salvatore, date(1983, 4, 13), MotifFor.ARRIVEE_HC, dateDepart, MotifFor.DEPART_HS, MockCommune.Lausanne);
				addForPrincipal(salvatore, dateDepart.addMonths(6), MotifFor.ARRIVEE_HS, MockCommune.Vevey);
				addDeclarationImpot(salvatore, periode2008, date(2008, 1, 1), dateDepart, TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete2008);

				// cas #5: un tiers avec une DI libre, parti avant la date annoncée, puis revenu (cas d'erreur)
				Contribuable greg = addNonHabitant("Grégoire", "Wriztjk", date(1965, 4, 13), Sexe.MASCULIN);
				ids.gregId = greg.getNumero();
				addForPrincipal(greg, date(1998, 4, 13), MotifFor.MAJORITE, date(2008, 4, 25), MotifFor.DEPART_HS, MockCommune.Lausanne); // date de départ effective != date de départ annoncée
				addForPrincipal(greg, dateDepart.addMonths(5), MotifFor.ARRIVEE_HS, MockCommune.Renens);
				addDeclarationImpot(greg, periode2008, date(2008, 1, 1), dateDepart, TypeContribuable.VAUDOIS_ORDINAIRE, declarationComplete2008);

				return null;
			}
		});

		final PeriodeFiscale periode2008 = (PeriodeFiscale) hibernateTemplate.get(PeriodeFiscale.class, ids.periodeId);
		assertNotNull(periode2008);

		assertTraitementContribuable(0, 0, 1, ids.arnoldId, periode2008); // une DI déjà existante (ignorée)
		assertTraitementContribuable(0, 1, 0, ids.malkoId, periode2008); // une DI libre en erreur
		assertTraitementContribuable(0, 1, 0, ids.ericId, periode2008); // une DI libre en erreur
		assertTraitementContribuable(1, 0, 1, ids.salvatoreId, periode2008); // une DI déjà existante (ignorée), une autre créée
		assertTraitementContribuable(1, 1, 0, ids.gregId, periode2008); // la DI libre en erreur, celle correspondante à la deuxième période traitée
	}

	@Test
	public void testAnnulationDeclarationSansPeriodeCorrespondante() throws Exception {

		final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		final ModeleDocument model2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
		addModeleFeuilleDocument("Déclaration", "210", model2007);
		addModeleFeuilleDocument("Annexe 1", "220", model2007);
		addModeleFeuilleDocument("Annexe 2-3", "230", model2007);
		addModeleFeuilleDocument("Annexe 4-5", "240", model2007);

		// Un contribuable non-assujetti, mais avec une déclaration d'impôt (invalide) pré-existante
		Contribuable malko = addNonHabitant("Malko", "Totor", date(1955, 2, 11), Sexe.MASCULIN);
		addDeclarationImpot(malko, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, model2007);
		hibernateTemplate.flush();

		DeterminationDIsResults rapport = new DeterminationDIsResults(2007, date(2008,1,15));
		service.setRapport(rapport);
		service.traiterContribuable(malko, periode2007);

		assertEmpty(rapport.erreurs);

		// ignoré parce que pas assujetti
		assertEquals(1, rapport.ignores.size());
		final DeterminationDIsResults.Ignore ignore = rapport.ignores.get(0);
		assertNotNull(ignore);
		assertEquals(DeterminationDIsResults.IgnoreType.PAS_ASSUJETTI, ignore.raison);

		// une nouvelle tâche d'annulation de la déclaration doit avoir été créée
		assertEquals(1, rapport.traites.size());
		final DeterminationDIsResults.Traite traite = rapport.traites.get(0);
		assertNotNull(traite);
		assertEquals(DeterminationDIsResults.TraiteType.TACHE_ANNULATION_CREE, traite.raison);
	}

	/**
	 * [UNIREG-1981] vérifie qu'on ne génère pas une tâche d'annulation de déclaration d'impôt s'il en existe déjà une non-traitée.
	 */
	@Test
	public void testAnnulationDeclarationSansPeriodeMaisAvecTacheAnnulationPreexistante() throws Exception {

		final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		final ModeleDocument model2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
		addModeleFeuilleDocument("Déclaration", "210", model2007);
		addModeleFeuilleDocument("Annexe 1", "220", model2007);
		addModeleFeuilleDocument("Annexe 2-3", "230", model2007);
		addModeleFeuilleDocument("Annexe 4-5", "240", model2007);

		// Un contribuable non-assujetti, mais avec une déclaration d'impôt (invalide) pré-existante ainsi qu'une tâche d'annulation de cette déclaration non-traitée
		Contribuable malko = addNonHabitant("Malko", "Totor", date(1955, 2, 11), Sexe.MASCULIN);
		DeclarationImpotOrdinaire di = addDeclarationImpot(malko, periode2007, date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, model2007);
		addTacheAnnulDI(TypeEtatTache.EN_INSTANCE, date(2007, 3, 21), di, malko);
		hibernateTemplate.flush();

		DeterminationDIsResults rapport = new DeterminationDIsResults(2007, date(2008, 1, 15));
		service.setRapport(rapport);
		service.traiterContribuable(malko, periode2007);

		assertEmpty(rapport.erreurs);
		assertEmpty(rapport.traites);

		// ignoré 2 fois parce que pas assujetti + parce que la tâche d'annulation de la DI existe déjà
		assertEquals(2, rapport.ignores.size());
		assertEquals(DeterminationDIsResults.IgnoreType.PAS_ASSUJETTI, rapport.ignores.get(0).raison);
		assertEquals(DeterminationDIsResults.IgnoreType.TACHE_ANNULATION_DEJA_EXISTANTE, rapport.ignores.get(1).raison);
	}

	@Test
	public void testAnnulationTacheSansPeriodeCorrespondante() throws Exception {


		final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		final ModeleDocument model2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
		addModeleFeuilleDocument("Déclaration", "210", model2007);
		addModeleFeuilleDocument("Annexe 1", "220", model2007);
		addModeleFeuilleDocument("Annexe 2-3", "230", model2007);
		addModeleFeuilleDocument("Annexe 4-5", "240", model2007);

		// Un contribuable non-assujetti, mais avec une tâche (invalide) d'envoi de déclaration d'impôt pré-existante
		Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
		addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
				eric, Qualification.AUTOMATIQUE);
		hibernateTemplate.flush();

		DeterminationDIsResults rapport = new DeterminationDIsResults(2007, date(2008,1,15));
		service.setRapport(rapport);
		service.traiterContribuable(eric, periode2007);


		assertEmpty(rapport.erreurs);

		// ignoré parce que pas assujetti
		assertEquals(1, rapport.ignores.size());
		final DeterminationDIsResults.Ignore ignore = rapport.ignores.get(0);
		assertNotNull(ignore);
		assertEquals(DeterminationDIsResults.IgnoreType.PAS_ASSUJETTI, ignore.raison);

		// une nouvelle tâche d'annulation de la déclaration doit avoir été créée
		assertEquals(1, rapport.traites.size());
		final DeterminationDIsResults.Traite traite = rapport.traites.get(0);
		assertNotNull(traite);
		assertEquals(DeterminationDIsResults.TraiteType.TACHE_ENVOI_ANNULEE, traite.raison);
	}

	@Test
	public void testAnnulationTacheSansPeriodeCorrespondanteComplexe() throws Exception {


		final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		final ModeleDocument model2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
		addModeleFeuilleDocument("Déclaration", "210", model2007);
		addModeleFeuilleDocument("Annexe 1", "220", model2007);
		addModeleFeuilleDocument("Annexe 2-3", "230", model2007);
		addModeleFeuilleDocument("Annexe 4-5", "240", model2007);

		// Un contribuable parti hors-Suisse en cours d'année avec une tâche (valide) pour sa période en Suisse, et une autre tâche (invalide) pour sa période hors-Suisse.
		Contribuable arnold = addNonHabitant("Arnold", "Charbon", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(arnold, date(1983, 4, 13), MotifFor.MAJORITE, date(2007, 6, 30), MotifFor.DEPART_HS, MockCommune.Lausanne);
		addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 6, 30), TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH,
				arnold, Qualification.AUTOMATIQUE);
		addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 7, 1), date(2007, 12, 31), TypeContribuable.HORS_SUISSE, TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, arnold,
				Qualification.AUTOMATIQUE);
		hibernateTemplate.flush();

		DeterminationDIsResults rapport = new DeterminationDIsResults(2007, date(2008,1,15));
		service.setRapport(rapport);
		service.traiterContribuable(arnold, periode2007);

		assertEmpty(rapport.erreurs);

		// la tâche existante valide est ignorée
		assertEquals(1, rapport.ignores.size());
		final DeterminationDIsResults.Ignore ignore = rapport.ignores.get(0);
		assertNotNull(ignore);
		assertEquals(DeterminationDIsResults.IgnoreType.TACHE_ENVOI_DEJA_EXISTANTE, ignore.raison);

		// la tâche d'envoi doit avoir été annulée
		assertEquals(1, rapport.traites.size());
		final DeterminationDIsResults.Traite traite = rapport.traites.get(0);
		assertNotNull(traite);
		assertEquals(DeterminationDIsResults.TraiteType.TACHE_ENVOI_ANNULEE, traite.raison);
	}

	/**
	 * [UNIREG-1981] Vérifie que les tâches déjà traitées ne sont pas annulées
	 */
	@Test
	public void testAnnulationTacheDejaTraitee() throws Exception {

		final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		final ModeleDocument model2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
		addModeleFeuilleDocument("Déclaration", "210", model2007);
		addModeleFeuilleDocument("Annexe 1", "220", model2007);
		addModeleFeuilleDocument("Annexe 2-3", "230", model2007);
		addModeleFeuilleDocument("Annexe 4-5", "240", model2007);

		// Un contribuable non-assujetti, mais avec une tâche (invalide) d'envoi de déclaration d'impôt pré-existante déjà traitée
		Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
		TacheEnvoiDeclarationImpot tache = addTacheEnvoiDI(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, eric, Qualification.AUTOMATIQUE);
		hibernateTemplate.flush();

		DeterminationDIsResults rapport = new DeterminationDIsResults(2007, date(2008,1,15));
		service.setRapport(rapport);
		service.traiterContribuable(eric, periode2007);

		assertEmpty(rapport.erreurs);

		// ignoré parce que pas assujetti
		assertEquals(1, rapport.ignores.size());
		final DeterminationDIsResults.Ignore ignore = rapport.ignores.get(0);
		assertNotNull(ignore);
		assertEquals(DeterminationDIsResults.IgnoreType.PAS_ASSUJETTI, ignore.raison);

		// la tâche ne doit pas avoir été annulée
		assertNull(tache.getAnnulationDate());
		assertEmpty(rapport.traites);
	}

	/**
	 * [UNIREG-1984] Vérifie que les tâches qui ne chevauche la période d'imposition théorique sont bien annulées
	 */
	@Test
	public void testAnnulationTacheCollisionPeriode() throws Exception {

		final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		final ModeleDocument model2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
		addModeleFeuilleDocument("Déclaration", "210", model2007);
		addModeleFeuilleDocument("Annexe 1", "220", model2007);
		addModeleFeuilleDocument("Annexe 2-3", "230", model2007);
		addModeleFeuilleDocument("Annexe 4-5", "240", model2007);

		// Un contribuable arrivé de hors-Suisse en cours d'année, mais avec une tâche (invalide) d'envoi de déclaration d'impôt pré-existante sur toute l'année
		Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(eric, date(2007, 4,28), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		TacheEnvoiDeclarationImpot tache = addTacheEnvoiDI(TypeEtatTache.EN_INSTANCE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, eric, Qualification.AUTOMATIQUE);
		hibernateTemplate.flush();

		DeterminationDIsResults rapport = new DeterminationDIsResults(2007, date(2008,1,15));
		service.setRapport(rapport);
		service.traiterContribuable(eric, periode2007);

		assertEmpty(rapport.erreurs);
		assertEmpty(rapport.ignores);

		// une nouvelle tâche créée + une tâche annulée
		assertEquals(2, rapport.traites.size());
		assertEquals(DeterminationDIsResults.TraiteType.TACHE_ENVOI_CREEE, rapport.traites.get(0).raison);
		assertEquals(DeterminationDIsResults.TraiteType.TACHE_ENVOI_ANNULEE, rapport.traites.get(1).raison);

		// la tâche doit avoir été annulée
		assertNotNull(tache.getAnnulationDate());
	}

	/**
	 * [UNIREG-1984] Vérifie que les tâches déjà traitées sont ignorées et qu'une nouvelle tâche d'envoi de DI est bien générée
	 */
	@Test
	public void testIgnoreTacheTraitee() throws Exception {

		final PeriodeFiscale periode2007 = addPeriodeFiscale(2007);
		final ModeleDocument model2007 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2007);
		addModeleFeuilleDocument("Déclaration", "210", model2007);
		addModeleFeuilleDocument("Annexe 1", "220", model2007);
		addModeleFeuilleDocument("Annexe 2-3", "230", model2007);
		addModeleFeuilleDocument("Annexe 4-5", "240", model2007);

		// Un contribuable vaudois ordinaire, avec une tâche (valide) d'envoi de déclaration d'impôt déjà traitée
		Contribuable eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(eric, date(2002, 4,28), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
		TacheEnvoiDeclarationImpot tache = addTacheEnvoiDI(TypeEtatTache.TRAITE, date(2008, 1, 31), date(2007, 1, 1), date(2007, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
				TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, eric, Qualification.AUTOMATIQUE);
		hibernateTemplate.flush();

		DeterminationDIsResults rapport = new DeterminationDIsResults(2007, date(2008,1,15));
		service.setRapport(rapport);
		service.traiterContribuable(eric, periode2007);

		assertEmpty(rapport.erreurs);
		assertEmpty(rapport.ignores);

		// une nouvelle tâche d'envoi de DI doit avoir été créée (ce n'est pas parce qu'une tâche d'envoi de DI déjà traitée préexiste que la DI a été bien envoyée:
		// la DI a pu être annulée. Il est donc nécessaire de créer cette nouvelle tâche).
		assertEquals(1, rapport.traites.size());
		assertEquals(DeterminationDIsResults.TraiteType.TACHE_ENVOI_CREEE, rapport.traites.get(0).raison);

		// la tâche ne doit pas avoir été annulée
		assertNull(tache.getAnnulationDate());
	}

	/**
	 * [UNIREG-1980] Teste que le type de document pour un indigent qui a reçu des Vaudtax reste Vaudtax.
	 */
	@SuppressWarnings({"unchecked"})
	@Test
	public void testEnvoiDIContribuableIndigentVaudTax() throws Exception {

		final PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
		final ModeleDocument declarationVaudTax = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2008);

		// Un contribuable indigent habitant à Lausanne
		final Contribuable marc = addNonHabitant("Marc", "Dumont", date(1962, 3, 12), Sexe.MASCULIN);
		marc.setOfficeImpotId(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm());
		final ForFiscalPrincipal ffp = addForPrincipal(marc, date(1990, 1, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
		ffp.setModeImposition(ModeImposition.INDIGENT);

		final DeclarationImpotOrdinaire decl2008 = addDeclarationImpot(marc, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, declarationVaudTax);
		addEtatDeclaration(decl2008, date(2009, 1, 15), TypeEtatDeclaration.EMISE);
		addEtatDeclaration(decl2008, date(2009, 1, 15), TypeEtatDeclaration.RETOURNEE);

		hibernateTemplate.flush();

		assertDetails(TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX, date(2009,1,1), date(2009,12,31), service.determineDetailsEnvoi(marc, 2009));
	}

	private void assertTraitementContribuable(int nbTraites, int nbEnErreur, int nbIgnores, long ctbId, PeriodeFiscale periodeFiscale) throws DeclarationException, AssujettissementException {
		DeterminationDIsResults rapport = new DeterminationDIsResults(2008, date(2009, 1, 15));
		service.setRapport(rapport);
		final Contribuable ctb = (Contribuable) hibernateTemplate.get(Contribuable.class, ctbId);
		assertNotNull(ctb);
		service.traiterContribuable(ctb, periodeFiscale);
		assertEquals(nbTraites, rapport.traites.size());
		assertEquals(nbEnErreur, rapport.erreurs.size());
		assertEquals(nbIgnores, rapport.ignores.size());
	}

	private static void assertDetails(final TypeContribuableDI type, final RegDate dateDebut, final RegDate dateFin, final List<PeriodeImposition> details) {
		assertNotNull(details);
		assertEquals(1, details.size());
		assertDetails(type, dateDebut, dateFin, details.get(0));
	}

	private static void assertDetails(final TypeContribuableDI type, final RegDate dateDebut, final RegDate dateFin, final PeriodeImposition details) {
		assertNotNull(details);
		assertEquals(type, details.getTypeContribuableDI());
		assertEquals(dateDebut, details.getDateDebut());
		assertEquals(dateFin, details.getDateFin());
	}

	private void assertZeroDeclaration(List<PeriodeImposition> periodes) {
		if (periodes != null) {
			for (PeriodeImposition p : periodes) {
				assertFalse(service.needsDeclaration(p));
			}
		}
	}

	private void assertAbsenceTachesAnnulationDI(Contribuable contribuable,int annee) {

		TacheCriteria criterion = new TacheCriteria();
		List<Tache> taches;
		criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
		criterion.setAnnee(annee);
		criterion.setContribuable(contribuable);
		taches = tacheDAO.find(criterion);
		assertEmpty(taches);

	}
}
