package ch.vd.uniregctb.migration.pm.engine;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.migration.pm.MigrationResultCollector;
import ch.vd.uniregctb.migration.pm.communes.FractionsCommuneProvider;
import ch.vd.uniregctb.migration.pm.communes.FusionCommunesProvider;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.mapping.IdMapper;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.DatesParticulieres;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TypeTiers;
import ch.vd.uniregctb.type.Sexe;

public class IndividuMigratorTest extends AbstractEntityMigratorTest {

	private IndividuMigrator migrator;
	private NonHabitantIndex nonHabitantIndex;

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();
		nonHabitantIndex = getBean(NonHabitantIndex.class, "nonHabitantIndex");
		nonHabitantIndex.overwriteIndex();

		final ActivityManager activityManager = entreprise -> true;     // tout le monde est actif dans ces tests!

		migrator = new IndividuMigrator(
				getBean(UniregStore.class, "uniregStore"),
				activityManager,
				getBean(ServiceInfrastructureService.class, "serviceInfrastructureService"),
				getBean(TiersDAO.class, "tiersDAO"),
				getBean(RcPersClient.class, "rcpersClient"),
				nonHabitantIndex,
				getBean(FusionCommunesProvider.class, "fusionCommunesProvider"),
				getBean(FractionsCommuneProvider.class, "fractionsCommuneProvider"),
				getBean(DatesParticulieres.class, "datesParticulieres"));
	}

	static RegpmIndividu buildBaseIndividu(long id, String nom, String prenom, RegDate dateNaissance, Sexe sexe) {
		final RegpmIndividu individu = new RegpmIndividu();
		assignMutationVisa(individu, REGPM_VISA, REGPM_MODIF);
		individu.setId(id);
		individu.setDateNaissance(dateNaissance);
		individu.setNom(nom);
		individu.setPrenom(prenom);
		individu.setSexe(sexe);

		// initialisation des collections à des collections vides tout comme on les trouverait avec une entité
		// extraite de la base de données
		individu.setMandants(new HashSet<>());

		return individu;
	}

	@Test
	public void testIndividuSansRoleMandataire() throws Exception {

		// on construit un individu simple (qui n'existe pas dans Unireg, ni dans RCPers, avec un numéro comme ça...), et on le migre
		final long noIndividuRegpm = 7484841141411857L;
		final RegpmIndividu individu = buildBaseIndividu(noIndividuRegpm, "Dantès", "Edmond Alexandre", RegDate.get(1978, 5, 12), Sexe.MASCULIN);

		final MockGraphe graphe = new MockGraphe(null,
		                                         null,
		                                         Collections.singletonList(individu));
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(individu, migrator, mr, linkCollector, idMapper);

		// vérification de ce que l'on a créé en base (= rien !!)
		doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.PERSONNE_PHYSIQUE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(0, ids.size());
		});

		Assert.assertNotNull(linkCollector.getCollectedLinks());
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());

		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.INDIVIDUS_PM);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bIndividu PM ignoré car n'a pas le rôle de mandataire\\.$");
	}

	@Test
	public void testNouvelIndividu() throws Exception {

		// on construit un individu simple (qui n'existe pas dans Unireg, ni dans RCPers, avec un numéro comme ça...), et on le migre
		final long noIndividuRegpm = 7484841141411857L;
		final RegpmIndividu individu = buildBaseIndividu(noIndividuRegpm, "Dantès", "Edmond Alexandre", RegDate.get(1978, 5, 12), Sexe.MASCULIN);

		// rôle mandataire pour faire fonctionner la migration
		final long idEntreprise = 4521L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addMandat(entreprise, individu, RegpmTypeMandat.GENERAL, null, RegDate.get(2000, 1, 1), RegDate.get(2014, 12, 4));

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         null,
		                                         Collections.singletonList(individu));
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(individu, migrator, mr, linkCollector, idMapper);

		// vérification de ce que l'on a créé en base
		final long ppId = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.PERSONNE_PHYSIQUE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());

			final PersonnePhysique pp = (PersonnePhysique) getTiersDAO().get(ids.get(0));
			Assert.assertNotNull(pp);
			Assert.assertEquals("Edmond", pp.getPrenomUsuel());
			Assert.assertEquals("Edmond Alexandre", pp.getTousPrenoms());
			Assert.assertEquals("Dantès", pp.getNom());
			Assert.assertEquals(RegDate.get(1978, 5, 12), pp.getDateNaissance());
			Assert.assertEquals(Sexe.MASCULIN, pp.getSexe());
			Assert.assertFalse(pp.isAnnule());
			Assert.assertFalse(pp.isConnuAuCivil());
			return ids.get(0);
		});

		Assert.assertEquals(ppId, idMapper.getIdUniregIndividu(noIndividuRegpm));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());

		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.INDIVIDUS_PM, LogCategory.SUIVI);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bAucun résultat dans RCPers pour le nom (.*), prénom (.*), sexe (.*) et date de naissance (.*)\\.$");
		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bAucun non-habitant trouvé dans Unireg avec ces nom (.*), prénom (.*), sexe (.*) et date de naissance (.*)\\.$");
		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bCréation de la personne physique [0-9.]+ pour correspondre à l'individu RegPM\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "Individu migré : [0-9.]+\\.$");
	}

	@Test
	public void testIndividuMigreCivil() throws Exception {

		final long noIndividu = 33153;
		final RegpmIndividu individu = buildBaseIndividu(noIndividu, "Dusonchet", "Thérèse", RegDate.get(1953, 3, 12), Sexe.FEMININ);

		// rôle mandataire pour faire fonctionner la migration
		final long idEntreprise = 4521L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addMandat(entreprise, individu, RegpmTypeMandat.GENERAL, null, RegDate.get(2000, 1, 1), RegDate.get(2014, 12, 4));

		// création de l'habitant correspondant dans Unireg
		final long ppId = doInUniregTransaction(status -> {
			final PersonnePhysique pp = new PersonnePhysique(noIndividu);
			return (Long) getUniregSessionFactory().getCurrentSession().save(pp);
		});

		// migration de l'individu RepPM -> la personne physique ne doit pas être nouvellement créée
		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         null,
		                                         Collections.singletonList(individu));
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(individu, migrator, mr, linkCollector, idMapper);

		// vérification de ce qui s'est passé (ou pas) en base
		doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.PERSONNE_PHYSIQUE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			Assert.assertEquals((Long) ppId, ids.get(0));

			final PersonnePhysique pp = (PersonnePhysique) getTiersDAO().get(ids.get(0));
			Assert.assertNotNull(pp);
			Assert.assertNull(pp.getPrenomUsuel());
			Assert.assertNull(pp.getTousPrenoms());
			Assert.assertNull(pp.getNom());
			Assert.assertNull(pp.getDateNaissance());
			Assert.assertNull(pp.getSexe());
			Assert.assertFalse(pp.isAnnule());
			Assert.assertTrue(pp.isConnuAuCivil());
			Assert.assertEquals((Long) noIndividu, pp.getNumeroIndividu());
			return null;
		});

		Assert.assertEquals(ppId, idMapper.getIdUniregIndividu(noIndividu));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());

		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.INDIVIDUS_PM, LogCategory.SUIVI);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bIndividu trouvé avec le même identifiant et la même identité dans RCPers\\.$");
		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bTrouvé personne physique existante [0-9.]+\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bIndividu migré : [0-9.]+\\.$");
	}

	@Test
	public void testIndividuMigreCivilMaisAbsentFiscal() throws Exception {

		final long noIndividu = 33153;
		final RegpmIndividu individu = buildBaseIndividu(noIndividu, "Dusonchet", "Thérèse", RegDate.get(1953, 3, 12), Sexe.FEMININ);

		// rôle mandataire pour faire fonctionner la migration
		final long idEntreprise = 4521L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addMandat(entreprise, individu, RegpmTypeMandat.GENERAL, null, RegDate.get(2000, 1, 1), RegDate.get(2014, 12, 4));

		// migration de l'individu RepPM -> la personne physique doit être nouvellement créée et liée au civil
		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         null,
		                                         Collections.singletonList(individu));
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(individu, migrator, mr, linkCollector, idMapper);

		// vérification de ce qui s'est passé (ou pas) en base
		final long ppId = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.PERSONNE_PHYSIQUE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());

			final PersonnePhysique pp = (PersonnePhysique) getTiersDAO().get(ids.get(0));
			Assert.assertNotNull(pp);
			Assert.assertNull(pp.getPrenomUsuel());
			Assert.assertNull(pp.getTousPrenoms());
			Assert.assertNull(pp.getNom());
			Assert.assertNull(pp.getDateNaissance());
			Assert.assertNull(pp.getSexe());
			Assert.assertFalse(pp.isAnnule());
			Assert.assertTrue(pp.isConnuAuCivil());
			Assert.assertEquals((Long) noIndividu, pp.getNumeroIndividu());
			return ids.get(0);
		});

		Assert.assertEquals(ppId, idMapper.getIdUniregIndividu(noIndividu));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());

		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.INDIVIDUS_PM, LogCategory.SUIVI);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bIndividu trouvé avec le même identifiant et la même identité dans RCPers\\.$");
		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bIndividu 33153 trouvé dans RCPers sans équivalent dans Unireg\\.\\.\\.$");
		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bCréation de la personne physique [0-9.]+ pour correspondre à l'individu RegPM\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bIndividu migré : [0-9.]+\\.$");
	}

	@Test
	public void testIndividuNonMigreCivilMaisArriveDepuisAvecAutreIdentifiant() throws Exception {

		final long noIndividuRegpm = 12;
		final long noIndividuRcpers = 33153;

		final RegpmIndividu individu = buildBaseIndividu(noIndividuRegpm, "Dusonchet", "Thérèse", RegDate.get(1953, 3, 12), Sexe.FEMININ);

		// rôle mandataire pour faire fonctionner la migration
		final long idEntreprise = 4521L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addMandat(entreprise, individu, RegpmTypeMandat.GENERAL, null, RegDate.get(2000, 1, 1), RegDate.get(2014, 12, 4));

		// création de l'habitant correspondant dans Unireg
		final long ppId = doInUniregTransaction(status -> {
			final PersonnePhysique pp = new PersonnePhysique(noIndividuRcpers);
			return (Long) getUniregSessionFactory().getCurrentSession().save(pp);
		});

		// migration de l'individu RepPM -> la personne physique ne doit pas être nouvellement créée
		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         null,
		                                         Collections.singletonList(individu));
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(individu, migrator, mr, linkCollector, idMapper);

		// vérification de ce qui s'est passé (ou pas) en base
		doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.PERSONNE_PHYSIQUE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			Assert.assertEquals((Long) ppId, ids.get(0));

			final PersonnePhysique pp = (PersonnePhysique) getTiersDAO().get(ids.get(0));
			Assert.assertNotNull(pp);
			Assert.assertNull(pp.getPrenomUsuel());
			Assert.assertNull(pp.getTousPrenoms());
			Assert.assertNull(pp.getNom());
			Assert.assertNull(pp.getDateNaissance());
			Assert.assertNull(pp.getSexe());
			Assert.assertFalse(pp.isAnnule());
			Assert.assertTrue(pp.isConnuAuCivil());
			Assert.assertEquals((Long) noIndividuRcpers, pp.getNumeroIndividu());
			return null;
		});

		Assert.assertEquals(ppId, idMapper.getIdUniregIndividu(noIndividuRegpm));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());

		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.INDIVIDUS_PM, LogCategory.SUIVI);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bTrouvé un individu \\([0-9]+\\) de RCPers pour le nom (.*), prénom (.*), sexe (.*) et date de naissance (.*)\\.$");
		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bTrouvé personne physique existante [0-9.]+\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bIndividu migré : [0-9.]+\\.$");
	}

	@Test
	public void testIndividuNonMigreCivilEtExistantNonHabitantFiscal() throws Exception {

		// on construit un individu simple (qui n'existe pas dans Unireg, ni dans RCPers, avec un numéro comme ça...), et on le migre
		final long noIndividuRegpm = 7484841141411857L;
		final RegpmIndividu individu = buildBaseIndividu(noIndividuRegpm, "Dantès", "Edmond Alexandre", RegDate.get(1978, 5, 12), Sexe.MASCULIN);

		// rôle mandataire pour faire fonctionner la migration
		final long idEntreprise = 4521L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addMandat(entreprise, individu, RegpmTypeMandat.GENERAL, null, RegDate.get(2000, 1, 1), RegDate.get(2014, 12, 4));

		// création du non-habitant avec les mêmes données en base (il sera indexé et retrouvé lors de la migration)
		final long ppId = doInUniregTransaction(status -> {
			final PersonnePhysique pp = new PersonnePhysique(Boolean.FALSE);
			pp.setNom("Dantès");
			pp.setPrenomUsuel("Edmond");
			pp.setTousPrenoms("Edmond Alexandre Hervé");
			pp.setDateNaissance(RegDate.get(1978, 5, 12));
			pp.setSexe(Sexe.MASCULIN);
			return (Long) getUniregSessionFactory().getCurrentSession().save(pp);
		});

		// indexation du non-habitant...
		doInUniregTransaction(true, status -> {
			final PersonnePhysique pp = (PersonnePhysique) getTiersDAO().get(ppId);
			nonHabitantIndex.index(pp);
			return null;
		});

		// migration de l'individu RepPM -> la personne physique ne doit pas être nouvellement créée
		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         null,
		                                         Collections.singletonList(individu));
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(individu, migrator, mr, linkCollector, idMapper);

		// vérification de ce que l'on a créé en base
		doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.PERSONNE_PHYSIQUE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			Assert.assertEquals((Long) ppId, ids.get(0));

			final PersonnePhysique pp = (PersonnePhysique) getTiersDAO().get(ids.get(0));
			Assert.assertNotNull(pp);
			Assert.assertEquals("Edmond", pp.getPrenomUsuel());
			Assert.assertEquals("Edmond Alexandre Hervé", pp.getTousPrenoms());
			Assert.assertEquals("Dantès", pp.getNom());
			Assert.assertEquals(RegDate.get(1978, 5, 12), pp.getDateNaissance());
			Assert.assertEquals(Sexe.MASCULIN, pp.getSexe());
			Assert.assertFalse(pp.isAnnule());
			Assert.assertFalse(pp.isConnuAuCivil());
			return null;
		});

		Assert.assertEquals(ppId, idMapper.getIdUniregIndividu(noIndividuRegpm));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());

		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.INDIVIDUS_PM, LogCategory.SUIVI);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bL'individu RCPers [0-9]+ ne peut être renvoyé \\(Personne .* introuvable\\)\\.$");
		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bAucun résultat dans RCPers pour le nom (.*), prénom (.*), sexe (.*) et date de naissance (.*)\\.$");
		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bTrouvé personne physique existante [0-9.]+\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bIndividu migré : [0-9.]+\\.$");
	}
}
