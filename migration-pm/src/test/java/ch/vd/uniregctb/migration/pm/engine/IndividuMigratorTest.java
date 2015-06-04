package ch.vd.uniregctb.migration.pm.engine;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.migration.pm.MigrationResultCollector;
import ch.vd.uniregctb.migration.pm.MigrationResultMessage;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.migration.pm.mapping.IdMapper;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TypeTiers;
import ch.vd.uniregctb.type.Sexe;

public class IndividuMigratorTest extends AbstractEntityMigratorTest {

	private static final String REGPM_VISA = "REGPM";
	private static final Timestamp REGPM_MODIF = new Timestamp(DateHelper.getCurrentDate().getTime() - TimeUnit.DAYS.toMillis(2000));   // 2000 jours ~ 5.5 années

	private IndividuMigrator migrator;
	private NonHabitantIndex nonHabitantIndex;

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();
		nonHabitantIndex = getBean(NonHabitantIndex.class, "nonHabitantIndex");
		nonHabitantIndex.overwriteIndex();

		migrator = new IndividuMigrator(
				getBean(UniregStore.class, "uniregStore"),
				getBean(TiersDAO.class, "tiersDAO"),
				getBean(RcPersClient.class, "rcpersClient"),
				nonHabitantIndex);

	}

	private static RegpmIndividu buildBaseIndividu(long id, String nom, String prenom, RegDate dateNaissance, Sexe sexe) {
		final RegpmIndividu individu = new RegpmIndividu();
		individu.setLastMutationOperator(REGPM_VISA);
		individu.setLastMutationTimestamp(REGPM_MODIF);
		individu.setId(id);
		individu.setDateNaissance(dateNaissance);
		individu.setNom(nom);
		individu.setPrenom(prenom);
		individu.setSexe(sexe);
		return individu;
	}

	@Test
	public void testNouvelIndividu() throws Exception {

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();

		// on construit un individu simple (qui n'existe pas dans Unireg, ni dans RCPers, avec un numéro comme ça...), et on le migre
		final long noIndividuRegpm = 7484841141411857L;
		final RegpmIndividu individu = buildBaseIndividu(noIndividuRegpm, "Dantès", "Edmond Alexandre", RegDate.get(1978, 5, 12), Sexe.MASCULIN);
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

		mr.getMessages().keySet().stream()
				.filter(cat -> cat != MigrationResultMessage.CategorieListe.INDIVIDUS_PM)
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));
		mr.getMessages().get(MigrationResultMessage.CategorieListe.INDIVIDUS_PM).stream()
				.filter(msg -> !msg.getTexte().startsWith("Individu " + noIndividuRegpm + " : "))
				.findAny()
				.ifPresent(msg -> Assert.fail(String.format("Tous les messages devraient être dans le contexte de l'individu (trouvé '%s')", msg.getTexte())));

		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.INDIVIDUS_PM, "\\bAucun résultat dans RCPers pour le nom (.*), prénom (.*), sexe (.*) et date de naissance (.*)\\.$");
		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.INDIVIDUS_PM,
		                              "\\bAucun non-habitant trouvé dans Unireg avec ces nom (.*), prénom (.*), sexe (.*) et date de naissance (.*)\\.$");
		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.INDIVIDUS_PM, "\\bCréation de la personne physique [0-9.]+ pour correspondre à l'individu RegPM\\.$");
	}

	@Test
	public void testIndividuMigreCivil() throws Exception {

		final long noIndividu = 33153;
		final RegpmIndividu individu = buildBaseIndividu(noIndividu, "Dusonchet", "Thérèse", RegDate.get(1953, 3, 12), Sexe.FEMININ);

		// création de l'habitant correspondant dans Unireg
		final long ppId = doInUniregTransaction(status -> {
			final PersonnePhysique pp = new PersonnePhysique(noIndividu);
			return (Long) getUniregSessionFactory().getCurrentSession().save(pp);
		});

		// migration de l'individu RepPM -> la personne physique ne doit pas être nouvellement créée
		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
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

		mr.getMessages().keySet().stream()
				.filter(cat -> cat != MigrationResultMessage.CategorieListe.INDIVIDUS_PM)
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));
		mr.getMessages().get(MigrationResultMessage.CategorieListe.INDIVIDUS_PM).stream()
				.filter(msg -> !msg.getTexte().startsWith("Individu " + noIndividu + " : "))
				.findAny()
				.ifPresent(msg -> Assert.fail(String.format("Tous les messages devraient être dans le contexte de l'individu (trouvé '%s')", msg.getTexte())));

		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.INDIVIDUS_PM, "\\bIndividu trouvé avec le même identifiant et la même identité dans RCPers\\.$");
		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.INDIVIDUS_PM, "\\bTrouvé personne physique existante [0-9.]+\\.$");
	}

	@Test
	public void testIndividuMigreCivilMaisAbsentFiscal() throws Exception {

		final long noIndividu = 33153;
		final RegpmIndividu individu = buildBaseIndividu(noIndividu, "Dusonchet", "Thérèse", RegDate.get(1953, 3, 12), Sexe.FEMININ);

		// migration de l'individu RepPM -> la personne physique doit être nouvellement créée et liée au civil
		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
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

		mr.getMessages().keySet().stream()
				.filter(cat -> cat != MigrationResultMessage.CategorieListe.INDIVIDUS_PM)
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));
		mr.getMessages().get(MigrationResultMessage.CategorieListe.INDIVIDUS_PM).stream()
				.filter(msg -> !msg.getTexte().startsWith("Individu " + noIndividu + " : "))
				.findAny()
				.ifPresent(msg -> Assert.fail(String.format("Tous les messages devraient être dans le contexte de l'individu (trouvé '%s')", msg.getTexte())));

		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.INDIVIDUS_PM, "\\bIndividu trouvé avec le même identifiant et la même identité dans RCPers\\.$");
		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.INDIVIDUS_PM, "\\bIndividu 33153 trouvé dans RCPers sans équivalent dans Unireg\\.\\.\\.$");
		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.INDIVIDUS_PM, "\\bCréation de la personne physique [0-9.]+ pour correspondre à l'individu RegPM\\.$");
	}

	@Test
	public void testIndividuNonMigreCivilMaisArriveDepuisAvecAutreIdentifiant() throws Exception {

		final long noIndividuRegpm = 12;
		final long noIndividuRcpers = 33153;

		final RegpmIndividu individu = buildBaseIndividu(noIndividuRegpm, "Dusonchet", "Thérèse", RegDate.get(1953, 3, 12), Sexe.FEMININ);

		// création de l'habitant correspondant dans Unireg
		final long ppId = doInUniregTransaction(status -> {
			final PersonnePhysique pp = new PersonnePhysique(noIndividuRcpers);
			return (Long) getUniregSessionFactory().getCurrentSession().save(pp);
		});

		// migration de l'individu RepPM -> la personne physique ne doit pas être nouvellement créée
		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
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

		mr.getMessages().keySet().stream()
				.filter(cat -> cat != MigrationResultMessage.CategorieListe.INDIVIDUS_PM)
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));
		mr.getMessages().get(MigrationResultMessage.CategorieListe.INDIVIDUS_PM).stream()
				.filter(msg -> !msg.getTexte().startsWith("Individu " + noIndividuRegpm + " : "))
				.findAny()
				.ifPresent(msg -> Assert.fail(String.format("Tous les messages devraient être dans le contexte de l'individu (trouvé '%s')", msg.getTexte())));

		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.INDIVIDUS_PM, "\\bTrouvé un individu \\([0-9]+\\) de RCPers pour le nom (.*), prénom (.*), sexe (.*) et date de naissance (.*)\\.$");
		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.INDIVIDUS_PM, "\\bTrouvé personne physique existante [0-9.]+\\.$");
	}

	@Test
	public void testIndividuNonMigreCivilEtExistantNonHabitantFiscal() throws Exception {

		// on construit un individu simple (qui n'existe pas dans Unireg, ni dans RCPers, avec un numéro comme ça...), et on le migre
		final long noIndividuRegpm = 7484841141411857L;
		final RegpmIndividu individu = buildBaseIndividu(noIndividuRegpm, "Dantès", "Edmond Alexandre", RegDate.get(1978, 5, 12), Sexe.MASCULIN);

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
			nonHabitantIndex.index(pp, null);
			return null;
		});

		// migration de l'individu RepPM -> la personne physique ne doit pas être nouvellement créée
		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
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

		mr.getMessages().keySet().stream()
				.filter(cat -> cat != MigrationResultMessage.CategorieListe.INDIVIDUS_PM)
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));
		mr.getMessages().get(MigrationResultMessage.CategorieListe.INDIVIDUS_PM).stream()
				.filter(msg -> !msg.getTexte().startsWith("Individu " + noIndividuRegpm + " : "))
				.findAny()
				.ifPresent(msg -> Assert.fail(String.format("Tous les messages devraient être dans le contexte de l'individu (trouvé '%s')", msg.getTexte())));

		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.INDIVIDUS_PM, "\\bL'individu RCPers [0-9]+ ne peut être renvoyé \\(Personne .* introuvable\\)\\.$");
		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.INDIVIDUS_PM, "\\bAucun résultat dans RCPers pour le nom (.*), prénom (.*), sexe (.*) et date de naissance (.*)\\.$");
		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.INDIVIDUS_PM, "\\bTrouvé personne physique existante [0-9.]+\\.$");
	}
}
