package ch.vd.uniregctb.migration.pm.engine;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.MigrationResultCollector;
import ch.vd.uniregctb.migration.pm.communes.FractionsCommuneProvider;
import ch.vd.uniregctb.migration.pm.communes.FusionCommunesProvider;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.engine.helpers.DateHelper;
import ch.vd.uniregctb.migration.pm.engine.helpers.DoublonProvider;
import ch.vd.uniregctb.migration.pm.engine.helpers.RegimeFiscalHelper;
import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.mapping.IdMapper;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAdministrateur;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAdresseIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFinMandatAdministrateur;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFonction;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmLocalitePostale;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscalVD;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAdresseIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeRegimeFiscal;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.DatesParticulieres;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TypeTiers;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;
import ch.vd.uniregctb.type.Sexe;

public class IndividuMigratorTest extends AbstractEntityMigratorTest {

	private IndividuMigrator migrator;
	private NonHabitantIndex nonHabitantIndex;

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();
		nonHabitantIndex = getBean(NonHabitantIndex.class, "nonHabitantIndex");
		nonHabitantIndex.overwriteIndex();

		final MigrationContexte contexte = new MigrationContexte(getBean(UniregStore.class, "uniregStore"),
		                                                         entreprise -> true,                // tout le monde est actif dans ces tests!!
		                                                         idCantonal -> Collections.<Long>emptySet(),      // les numéros cantonaux d'organisation ne sont jamais utilisés plusieurs fois dans ces tests
		                                                         getBean(ServiceInfrastructureService.class, "serviceInfrastructureService"),
		                                                         getBean(FusionCommunesProvider.class, "fusionCommunesProvider"),
		                                                         getBean(FractionsCommuneProvider.class, "fractionsCommuneProvider"),
		                                                         getBean(DateHelper.class, "dateHelper"),
		                                                         getBean(DatesParticulieres.class, "datesParticulieres"),
		                                                         getBean(AdresseHelper.class, "adresseHelper"),
		                                                         getBean(BouclementService.class, "bouclementService"),
		                                                         getBean(AssujettissementService.class, "assujettissementService"),
		                                                         getBean(PeriodeImpositionService.class, "periodeImpositionService"),
		                                                         getBean(ParametreAppService.class, "parametreAppService"),
		                                                         null,
		                                                         getBean(DoublonProvider.class, "doublonProvider"),
		                                                         getBean(RegimeFiscalHelper.class, "regimeFiscalHelper"),
		                                                         getBean(TiersDAO.class, "tiersDAO"),
		                                                         getBean(RemarqueDAO.class, "remarqueDAO"),
		                                                         getBean(RcPersClient.class, "rcpersClient"),
		                                                         getBean(NonHabitantIndex.class, "nonHabitantIndex"));

		migrator = new IndividuMigrator(contexte);
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
		individu.setAdresses(new HashSet<>());
		individu.setCaracteristiques(new TreeSet<>());
		individu.setAdministrations(new HashSet<>());

		return individu;
	}

	static RegpmAdresseIndividu addAdresse(RegpmIndividu individu, RegpmTypeAdresseIndividu type, RegDate dateDebut, @Nullable RegDate dateFin, String nomRue, String noPolice, RegpmLocalitePostale localitePostale) {
		final RegpmAdresseIndividu adresse = new RegpmAdresseIndividu();
		assignMutationVisa(adresse, REGPM_VISA, REGPM_MODIF);
		adresse.setId(new RegpmAdresseIndividu.PK(computeNewSeqNo(individu.getAdresses(), x -> x.getId().getNoSequence()), individu.getId()));
		adresse.setDateDebut(dateDebut);
		adresse.setDateFin(dateFin);
		adresse.setType(type);
		adresse.setLocalitePostale(localitePostale);
		adresse.setNomRue(nomRue);
		adresse.setNoPolice(noPolice);
		individu.getAdresses().add(adresse);
		return adresse;
	}

	static RegpmAdministrateur addAdministration(RegpmIndividu individu, RegpmFonction fonction, RegDate dateDebut, @Nullable RegDate dateFin, boolean rectifiee, long noEntreprise) {
		final RegpmAdministrateur admin = new RegpmAdministrateur();
		assignMutationVisa(individu, REGPM_VISA, REGPM_MODIF);
		admin.setId(new RegpmAdministrateur.PK(computeNewSeqNo(individu.getAdministrations(), x -> x.getId().getSeqNo()), individu.getId()));
		admin.setAdministrateur(individu);
		admin.setFonction(fonction);
		admin.setDateEntreeFonction(dateDebut);
		admin.setRectifiee(rectifiee);
		admin.setFins(new HashSet<>());
		admin.initEntrepriseData(new TreeSet<>(), noEntreprise);
		if (dateFin != null) {
			final RegpmFinMandatAdministrateur finMandat = new RegpmFinMandatAdministrateur();
			assignMutationVisa(finMandat, REGPM_VISA, REGPM_MODIF);
			finMandat.setId(new RegpmFinMandatAdministrateur.PK(computeNewSeqNo(admin.getFins(), x -> x.getId().getSeqNo()), individu.getId(), admin.getId().getSeqNo()));
			finMandat.setRectifiee(false);
			finMandat.setDateFinMandat(dateFin);
			admin.getFins().add(finMandat);
		}
		individu.getAdministrations().add(admin);
		return admin;
	}

	static RegpmRegimeFiscalVD addRegimeFiscalVD(RegpmAdministrateur admin, RegDate dateDebut, @Nullable RegDate dateAnnulation, RegpmTypeRegimeFiscal type) {
		final RegpmRegimeFiscalVD regime = new RegpmRegimeFiscalVD();
		assignMutationVisa(regime, REGPM_VISA, REGPM_MODIF);
		regime.setId(new RegpmRegimeFiscalVD.PK(computeNewSeqNo(admin.getRegimesFiscauxVD(), x -> x.getId().getSeqNo()), admin.getEntrepriseId()));
		regime.setDateDebut(dateDebut);
		regime.setDateAnnulation(dateAnnulation);
		regime.setType(type);
		admin.getRegimesFiscauxVD().add(regime);
		return regime;
	}

	@Test
	public void testIndividuSansRoleMandataireNiAdministrateur() throws Exception {

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

		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bIndividu PM ignoré car n'a pas de rôle de mandataire ni d'administrateur\\.$");
	}

	@Test
	public void testIndividuAvecRoleAdministrateur() throws Exception {

		// on construit un individu simple (qui n'existe pas dans Unireg, ni dans RCPers, avec un numéro comme ça...), et on le migre
		final long noIndividuRegpm = 7484841141411857L;
		final RegpmIndividu individu = buildBaseIndividu(noIndividuRegpm, "Dantès", "Edmond Alexandre", RegDate.get(1978, 5, 12), Sexe.MASCULIN);
		final RegpmAdministrateur administration = addAdministration(individu, RegpmFonction.ADMINISTRATEUR, RegDate.get(2000, 1, 1), null, false, 42L);
		addRegimeFiscalVD(administration, RegDate.get(1965, 12, 4), null, RegpmTypeRegimeFiscal._35_SOCIETE_ORDINAIRE_SIAL);

		final MockGraphe graphe = new MockGraphe(null,
		                                         null,
		                                         Collections.singletonList(individu));
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(individu, migrator, mr, linkCollector, idMapper);

		// vérification de ce que l'on a créé en base (= rien!!)
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

		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bIndividu non migré car aucune correspondance univoque n'a pu être trouvée avec une personne physique existante dans Unireg\\.$");
	}

	@Test
	public void testIndividuInexistantDansUnireg() throws Exception {

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

		// vérification de ce que l'on a créé en base : pas de personne physique, en tout cas...
		doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.PERSONNE_PHYSIQUE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(0, ids.size());
		});

		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());
		Assert.assertEquals(Collections.singleton(EntityKey.of(individu)), linkCollector.getNeutralizedKeys());

		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.INDIVIDUS_PM);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bAucun résultat dans RCPers pour le nom (.*), prénom (.*), sexe (.*) et date de naissance (.*)\\.$");
		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bAucun non-habitant trouvé dans Unireg avec ces nom (.*), prénom (.*), sexe (.*) et date de naissance (.*)\\.$");
		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bIndividu non migré car aucune correspondance univoque n'a pu être trouvée avec une personne physique existante dans Unireg\\.$");
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

		// vérification de ce qui s'est passé (ou pas) en base : pour éviter la création de doublons, on ne génère aucun nouvel individu dans Unireg [SIFISC-16858]
		doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.PERSONNE_PHYSIQUE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(0, ids.size());
		});

		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());
		Assert.assertEquals(Collections.singleton(EntityKey.of(individu)), linkCollector.getNeutralizedKeys());

		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.INDIVIDUS_PM);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bIndividu trouvé avec le même identifiant et la même identité dans RCPers\\.$");
		assertExistMessageWithContent(mr, LogCategory.INDIVIDUS_PM, "\\bIndividu non migré car aucune correspondance univoque n'a pu être trouvée avec une personne physique existante dans Unireg\\.$");
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
		final RegpmIndividu individu = buildBaseIndividu(noIndividuRegpm, "Dantès", "Edmond", RegDate.get(1978, 5, 12), Sexe.MASCULIN);

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
