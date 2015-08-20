package ch.vd.uniregctb.migration.pm.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.adapter.rcent.service.RCEntAdapter;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.migration.pm.MigrationResultCollector;
import ch.vd.uniregctb.migration.pm.communes.FractionsCommuneProvider;
import ch.vd.uniregctb.migration.pm.communes.FusionCommunesProvider;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.log.EtablissementLoggedElement;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LoggedElementAttribute;
import ch.vd.uniregctb.migration.pm.mapping.IdMapper;
import ch.vd.uniregctb.migration.pm.regpm.AdresseAvecRue;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAppartenanceGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDomicileEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissementStable;
import ch.vd.uniregctb.migration.pm.regpm.RegpmGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmImmeuble;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRattachementProprietaire;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.TypeTiers;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class EtablissementMigratorTest extends AbstractEntityMigratorTest {

	private EtablissementMigrator migrator;

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();

		final ActivityManager activityManager = entreprise -> true;         // tout le monde est actif dans ces tests

		migrator = new EtablissementMigrator(
				getBean(UniregStore.class, "uniregStore"),
				activityManager,
				getBean(ServiceInfrastructureService.class, "serviceInfrastructureService"),
				getBean(RCEntAdapter.class, "rcEntAdapter"),
				getBean(AdresseHelper.class, "adresseHelper"),
				getBean(FusionCommunesProvider.class, "fusionCommunesProvider"),
				getBean(FractionsCommuneProvider.class, "fractionsCommuneProvider"));
	}

	static RegpmEtablissement buildEtablissement(long id, RegpmEntreprise entreprise) {
		final RegpmEtablissement etablissement = new RegpmEtablissement();
		etablissement.setId(id);
		etablissement.setEntreprise(entreprise);
		etablissement.setLastMutationOperator(REGPM_VISA);
		etablissement.setLastMutationTimestamp(REGPM_MODIF);

		// initialisation des collections à des collections vides tout comme on les trouverait avec une entité
		// extraite de la base de données
		etablissement.setAppartenancesGroupeProprietaire(new HashSet<>());
		etablissement.setDomicilesEtablissements(new TreeSet<>());
		etablissement.setEtablissementsStables(new HashSet<>());
		etablissement.setInscriptionsRC(new TreeSet<>());
		etablissement.setRadiationsRC(new TreeSet<>());
		etablissement.setRattachementsProprietaires(new HashSet<>());
		etablissement.setSuccursales(new HashSet<>());

		// rattachement à l'entreprise depuis l'entreprise également
		entreprise.getEtablissements().add(etablissement);

		return etablissement;
	}

	static RegpmEtablissement buildEtablissement(long id, RegpmIndividu individu) {
		final RegpmEtablissement etablissement = new RegpmEtablissement();
		etablissement.setId(id);
		etablissement.setIndividu(individu);
		etablissement.setLastMutationOperator(REGPM_VISA);
		etablissement.setLastMutationTimestamp(REGPM_MODIF);

		// initialisation des collections à des collections vides tout comme on les trouverait avec une entité
		// extraite de la base de données
		etablissement.setAppartenancesGroupeProprietaire(new HashSet<>());
		etablissement.setDomicilesEtablissements(new TreeSet<>());
		etablissement.setEtablissementsStables(new HashSet<>());
		etablissement.setInscriptionsRC(new TreeSet<>());
		etablissement.setRadiationsRC(new TreeSet<>());
		etablissement.setRattachementsProprietaires(new HashSet<>());
		etablissement.setSuccursales(new HashSet<>());

		return etablissement;
	}

	static void addEtablissementStable(RegpmEtablissement etablissement, RegDate dateDebut, RegDate dateFin) {
		// initialisation de la collection, au cas où...
		// ... et attribution d'un nouveau numéro de séquence pour l'identifiant
		final Set<RegpmEtablissementStable> stables = etablissement.getEtablissementsStables();
		final int newSeqNo = computeNewSeqNo(stables, stable -> stable.getId().getSeqNo());

		// création d'un établissement stable
		final RegpmEtablissementStable stable = new RegpmEtablissementStable();
		stable.setLastMutationOperator(REGPM_VISA);
		stable.setLastMutationTimestamp(REGPM_MODIF);
		stable.setId(new RegpmEtablissementStable.PK(newSeqNo, etablissement.getId()));
		stable.setDateDebut(dateDebut);
		stable.setDateFin(dateFin);
		stables.add(stable);
	}

	static void addDomicileEtablissement(RegpmEtablissement etablissement, RegDate dateDebut, RegpmCommune commune, boolean annule) {
		// initialization de la collection, au cas où...
		// ... et attribution d'un nouveau numéro de séquence pour l'identifiant
		final SortedSet<RegpmDomicileEtablissement> domiciles = etablissement.getDomicilesEtablissements();
		final int newSeqNo = computeNewSeqNo(domiciles, domicile -> domicile.getId().getSeqNo());

		// création d'une entrée de domicile
		final RegpmDomicileEtablissement domicile = new RegpmDomicileEtablissement();
		domicile.setLastMutationOperator(REGPM_VISA);
		domicile.setLastMutationTimestamp(REGPM_MODIF);
		domicile.setId(new RegpmDomicileEtablissement.PK(newSeqNo, etablissement.getId()));
		domicile.setDateValidite(dateDebut);
		domicile.setCommune(commune);
		domicile.setRectifiee(annule);
		domiciles.add(domicile);
	}

	static RegpmRattachementProprietaire addRattachementProprietaire(RegpmEtablissement etb, RegDate dateDebut, RegDate dateFin, RegpmImmeuble immeuble) {
		final RegpmRattachementProprietaire rrp = new RegpmRattachementProprietaire();
		rrp.setId(ID_GENERATOR.next());
		assignMutationVisa(rrp, REGPM_VISA, REGPM_MODIF);
		rrp.setDateDebut(dateDebut);
		rrp.setDateFin(dateFin);
		rrp.setImmeuble(immeuble);
		etb.getRattachementsProprietaires().add(rrp);
		return rrp;
	}

	static RegpmAppartenanceGroupeProprietaire addAppartenanceGroupeProprietaire(RegpmEtablissement etb, RegpmGroupeProprietaire groupe, RegDate dateDebut, RegDate dateFin, boolean leader) {
		final RegpmAppartenanceGroupeProprietaire ragp = new RegpmAppartenanceGroupeProprietaire();
		ragp.setId(new RegpmAppartenanceGroupeProprietaire.PK(NO_SEQUENCE_GENERATOR.next(), groupe.getId()));
		ragp.setDateDebut(dateDebut);
		ragp.setDateFin(dateFin);
		ragp.setGroupeProprietaire(groupe);
		ragp.setLeader(leader);
		etb.getAppartenancesGroupeProprietaire().add(ragp);
		return ragp;
	}

	@Test
	public void testSansEtablissementStable() throws Exception {

		final long noEntreprise = 4346375L;
		final long noEtablissement = 353645427L;

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> un nouvel établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertTrue(Long.toString(idEtablissement), Etablissement.ETB_GEN_FIRST_ID <= idEtablissement && idEtablissement <= Etablissement.ETB_GEN_LAST_ID);
		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());

		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.ETABLISSEMENTS, LogCategory.SUIVI);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.ETABLISSEMENTS, "\\bEtablissement sans aucune période de validité d'un établissement stable \\(aucun lien créé\\)\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bEtablissement migré : [0-9.]+\\.$");

		Assert.assertEquals(0, mr.getPreTransactionCommitData().size());
	}

	@Test
	public void testAvecUnEtablissementStableSansDomicile() throws Exception {

		final long noEntreprise = 4346375L;
		final long noEtablissement = 353645427L;
		final RegDate dateDebut = RegDate.get(2000, 1, 13);
		final RegDate dateFin = RegDate.get(2006, 5, 4);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		addEtablissementStable(etablissement, dateDebut, dateFin);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> un nouvel établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertTrue(Long.toString(idEtablissement), Etablissement.ETB_GEN_FIRST_ID <= idEtablissement && idEtablissement <= Etablissement.ETB_GEN_LAST_ID);
		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));
		Assert.assertEquals(1, linkCollector.getCollectedLinks().size());

		// lien vers l'entité juridique avec les dates des établissements stables
		{
			final EntityLinkCollector.EntityLink link = linkCollector.getCollectedLinks().get(0);
			Assert.assertEquals(EntityLinkCollector.LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, link.getType());
			Assert.assertEquals(dateDebut, link.getDateDebut());
			Assert.assertEquals(dateFin, link.getDateFin());
		}

		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.ETABLISSEMENTS, LogCategory.ADRESSES, LogCategory.SUIVI);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.ETABLISSEMENTS, "\\bL'établissement stable .* n'intersecte aucun domicile\\.$");
		assertExistMessageWithContent(mr, LogCategory.ADRESSES, "\\bAdresse trouvée sans rue ni localité postale\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bEtablissement migré : [0-9.]+\\.$");

		Assert.assertEquals(0, mr.getPreTransactionCommitData().size());
	}

	@Test
	public void testAvecUnEtablissementStableAvecDomicileAnnule() throws Exception {

		final long noEntreprise = 4346375L;
		final long noEtablissement = 353645427L;
		final RegDate dateDebut = RegDate.get(2000, 1, 13);
		final RegDate dateFin = RegDate.get(2006, 5, 4);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		addEtablissementStable(etablissement, dateDebut, dateFin);
		addDomicileEtablissement(etablissement, dateDebut, Commune.LAUSANNE, true);     // il y a intersection, mais le domicile est annulé, donc ignoré...

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> un nouvel établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertTrue(Long.toString(idEtablissement), Etablissement.ETB_GEN_FIRST_ID <= idEtablissement && idEtablissement <= Etablissement.ETB_GEN_LAST_ID);
		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));
		Assert.assertEquals(1, linkCollector.getCollectedLinks().size());

		// lien vers l'entité juridique avec les dates des établissements stables
		{
			final EntityLinkCollector.EntityLink link = linkCollector.getCollectedLinks().get(0);
			Assert.assertEquals(EntityLinkCollector.LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, link.getType());
			Assert.assertEquals(dateDebut, link.getDateDebut());
			Assert.assertEquals(dateFin, link.getDateFin());
		}

		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.ETABLISSEMENTS, LogCategory.ADRESSES, LogCategory.SUIVI);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.ETABLISSEMENTS, "\\bL'établissement stable .* n'intersecte aucun domicile\\.$");
		assertExistMessageWithContent(mr, LogCategory.ADRESSES, "\\bAdresse trouvée sans rue ni localité postale\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bEtablissement migré : [0-9.]+\\.$");

		Assert.assertEquals(0, mr.getPreTransactionCommitData().size());
	}

	@Test
	public void testAvecUnEtablissementStableAvecDomicileCouvertureComplete() throws Exception {

		final long noEntreprise = 4346375L;
		final long noEtablissement = 353645427L;
		final RegDate dateDebut = RegDate.get(2000, 1, 13);
		final RegDate dateFin = RegDate.get(2006, 5, 4);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		addEtablissementStable(etablissement, dateDebut, dateFin);
		addDomicileEtablissement(etablissement, dateDebut, Commune.LAUSANNE, false);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> un nouvel établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertTrue(Long.toString(idEtablissement), Etablissement.ETB_GEN_FIRST_ID <= idEtablissement && idEtablissement <= Etablissement.ETB_GEN_LAST_ID);
		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));
		Assert.assertEquals(1, linkCollector.getCollectedLinks().size());

		// lien vers l'entité juridique avec les dates des établissements stables
		{
			final EntityLinkCollector.EntityLink link = linkCollector.getCollectedLinks().get(0);
			Assert.assertEquals(EntityLinkCollector.LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, link.getType());
			Assert.assertEquals(dateDebut, link.getDateDebut());
			Assert.assertEquals(dateFin, link.getDateFin());
		}

		// vérification des messages collectés
		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.ETABLISSEMENTS, LogCategory.ADRESSES, LogCategory.SUIVI);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.ADRESSES, "\\bAdresse trouvée sans rue ni localité postale\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bEtablissement migré : [0-9.]+\\.$");

		// vérification des demandes de fors secondaires enregistrées
		final Map<Class<?>, List<?>> preCommitData = mr.getPreTransactionCommitData();
		Assert.assertEquals(1, preCommitData.size());
		Assert.assertTrue(preCommitData.containsKey(ForsSecondairesData.Activite.class));
		{
			//noinspection unchecked
			final List<ForsSecondairesData.Activite> data = (List<ForsSecondairesData.Activite>) preCommitData.get(ForsSecondairesData.Activite.class);
			Assert.assertEquals(1, data.size());

			{
				final ForsSecondairesData.Activite fsData = data.get(0);
				Assert.assertNotNull(fsData);
				Assert.assertEquals(EntityKey.Type.ENTREPRISE, fsData.entiteJuridiqueSupplier.getKey().getType());
				Assert.assertEquals(noEntreprise, fsData.entiteJuridiqueSupplier.getKey().getId());

				final Map<RegpmCommune, List<DateRange>> communes = fsData.communes;
				Assert.assertNotNull(communes);
				Assert.assertEquals(1, communes.size());

				final List<DateRange> lsneData = communes.get(Commune.LAUSANNE);
				Assert.assertNotNull(lsneData);
				Assert.assertEquals(Collections.<DateRange>singletonList(new DateRangeHelper.Range(dateDebut, dateFin)), lsneData);
			}
		}
	}

	@Test
	public void testAvecUnEtablissementStableAvecDomicileCouverturePartielle() throws Exception {

		final long noEntreprise = 4346375L;
		final long noEtablissement = 353645427L;
		final RegDate dateDebutEtablissementStable = RegDate.get(2000, 1, 13);
		final RegDate dateFinEtablissementStable = RegDate.get(2006, 5, 4);
		final RegDate dateDebutDomicile = dateDebutEtablissementStable.addMonths(6);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		addEtablissementStable(etablissement, dateDebutEtablissementStable, dateFinEtablissementStable);
		addDomicileEtablissement(etablissement, dateDebutDomicile, Commune.MORGES, false);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> un nouvel établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertTrue(Long.toString(idEtablissement), Etablissement.ETB_GEN_FIRST_ID <= idEtablissement && idEtablissement <= Etablissement.ETB_GEN_LAST_ID);
		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));
		Assert.assertEquals(1, linkCollector.getCollectedLinks().size());

		// lien vers l'entité juridique avec les dates des établissements stables
		{
			final EntityLinkCollector.EntityLink link = linkCollector.getCollectedLinks().get(0);
			Assert.assertEquals(EntityLinkCollector.LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, link.getType());
			Assert.assertEquals(dateDebutEtablissementStable, link.getDateDebut());
			Assert.assertEquals(dateFinEtablissementStable, link.getDateFin());
		}

		// vérification des messages collectés
		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.ETABLISSEMENTS, LogCategory.ADRESSES, LogCategory.SUIVI);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.ETABLISSEMENTS, "\\bL'établissement stable .* n'est couvert par les domiciles qu'à partir du [0-9.]+\\.$");
		assertExistMessageWithContent(mr, LogCategory.ADRESSES, "\\bAdresse trouvée sans rue ni localité postale\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bEtablissement migré : [0-9.]+\\.$");

		// vérification des demandes de fors secondaires enregistrées
		final Map<Class<?>, List<?>> preCommitData = mr.getPreTransactionCommitData();
		Assert.assertEquals(1, preCommitData.size());
		Assert.assertTrue(preCommitData.containsKey(ForsSecondairesData.Activite.class));
		{
			//noinspection unchecked
			final List<ForsSecondairesData.Activite> data = (List<ForsSecondairesData.Activite>) preCommitData.get(ForsSecondairesData.Activite.class);
			Assert.assertEquals(1, data.size());

			{
				final ForsSecondairesData.Activite fsData = data.get(0);
				Assert.assertNotNull(fsData);
				Assert.assertEquals(EntityKey.Type.ENTREPRISE, fsData.entiteJuridiqueSupplier.getKey().getType());
				Assert.assertEquals(noEntreprise, fsData.entiteJuridiqueSupplier.getKey().getId());

				final Map<RegpmCommune, List<DateRange>> communes = fsData.communes;
				Assert.assertNotNull(communes);
				Assert.assertEquals(1, communes.size());

				final List<DateRange> morgesData = communes.get(Commune.MORGES);
				Assert.assertNotNull(morgesData);
				Assert.assertEquals(Collections.<DateRange>singletonList(new DateRangeHelper.Range(dateDebutDomicile, dateFinEtablissementStable)), morgesData);
			}
		}
	}

	@Test
	public void testAvecUnEtablissementStableEtPlusieursDomiciles() throws Exception {

		final long noEntreprise = 4346375L;
		final long noEtablissement = 353645427L;
		final RegDate dateDebut = RegDate.get(2000, 1, 13);
		final RegDate dateFin = RegDate.get(2006, 5, 4);
		final RegDate dateDebutSecondDomicile = RegDate.get(2003, 7, 25);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		addEtablissementStable(etablissement, dateDebut, dateFin);
		addDomicileEtablissement(etablissement, dateDebut, Commune.LAUSANNE, false);
		addDomicileEtablissement(etablissement, dateDebutSecondDomicile, Commune.ECHALLENS, false);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> un nouvel établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertTrue(Long.toString(idEtablissement), Etablissement.ETB_GEN_FIRST_ID <= idEtablissement && idEtablissement <= Etablissement.ETB_GEN_LAST_ID);
		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));
		Assert.assertEquals(1, linkCollector.getCollectedLinks().size());

		// lien vers l'entité juridique avec les dates des établissements stables
		{
			final EntityLinkCollector.EntityLink link = linkCollector.getCollectedLinks().get(0);
			Assert.assertEquals(EntityLinkCollector.LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, link.getType());
			Assert.assertEquals(dateDebut, link.getDateDebut());
			Assert.assertEquals(dateFin, link.getDateFin());
		}

		// vérification des messages collectés
		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.ETABLISSEMENTS, LogCategory.ADRESSES, LogCategory.SUIVI);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.ADRESSES, "\\bAdresse trouvée sans rue ni localité postale\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bEtablissement migré : [0-9.]+\\.$");

		// vérification des demandes de fors secondaires enregistrées
		final Map<Class<?>, List<?>> preCommitData = mr.getPreTransactionCommitData();
		Assert.assertEquals(1, preCommitData.size());
		Assert.assertTrue(preCommitData.containsKey(ForsSecondairesData.Activite.class));
		{
			//noinspection unchecked
			final List<ForsSecondairesData.Activite> data = (List<ForsSecondairesData.Activite>) preCommitData.get(ForsSecondairesData.Activite.class);
			Assert.assertEquals(1, data.size());

			{
				final ForsSecondairesData.Activite fsData = data.get(0);
				Assert.assertNotNull(fsData);
				Assert.assertEquals(EntityKey.Type.ENTREPRISE, fsData.entiteJuridiqueSupplier.getKey().getType());
				Assert.assertEquals(noEntreprise, fsData.entiteJuridiqueSupplier.getKey().getId());

				final Map<RegpmCommune, List<DateRange>> communes = fsData.communes;
				Assert.assertNotNull(communes);
				Assert.assertEquals(2, communes.size());

				final List<DateRange> lausanne = communes.get(Commune.LAUSANNE);
				Assert.assertNotNull(lausanne);
				Assert.assertEquals(Collections.<DateRange>singletonList(new DateRangeHelper.Range(dateDebut, dateDebutSecondDomicile.getOneDayBefore())), lausanne);

				final List<DateRange> echallens = communes.get(Commune.ECHALLENS);
				Assert.assertNotNull(echallens);
				Assert.assertEquals(Collections.<DateRange>singletonList(new DateRangeHelper.Range(dateDebutSecondDomicile, dateFin)), echallens);
			}
		}
	}

	@Test
	public void testAvecUnEtablissementStableEtPlusieursDomicilesCouverturePartielle() throws Exception {

		final long noEntreprise = 4346375L;
		final long noEtablissement = 353645427L;
		final RegDate dateDebutEtablissementStable = RegDate.get(2000, 1, 13);
		final RegDate dateFinEtablissementStable = RegDate.get(2006, 5, 4);
		final RegDate dateDebutDomicile = dateDebutEtablissementStable.addMonths(8);
		final RegDate dateDebutSecondDomicile = RegDate.get(2003, 7, 25);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		addEtablissementStable(etablissement, dateDebutEtablissementStable, dateFinEtablissementStable);
		addDomicileEtablissement(etablissement, dateDebutDomicile, Commune.LAUSANNE, false);
		addDomicileEtablissement(etablissement, dateDebutSecondDomicile, Commune.ECHALLENS, false);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> un nouvel établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertTrue(Long.toString(idEtablissement), Etablissement.ETB_GEN_FIRST_ID <= idEtablissement && idEtablissement <= Etablissement.ETB_GEN_LAST_ID);
		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));
		Assert.assertEquals(1, linkCollector.getCollectedLinks().size());

		// lien vers l'entité juridique avec les dates des établissements stables
		{
			final EntityLinkCollector.EntityLink link = linkCollector.getCollectedLinks().get(0);
			Assert.assertEquals(EntityLinkCollector.LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, link.getType());
			Assert.assertEquals(dateDebutEtablissementStable, link.getDateDebut());
			Assert.assertEquals(dateFinEtablissementStable, link.getDateFin());
		}

		// vérification des messages collectés
		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.ETABLISSEMENTS, LogCategory.ADRESSES, LogCategory.SUIVI);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));
		mr.getMessages().values().stream()
				.flatMap(Collection::stream)
				.map(msg -> Pair.of(msg, msg.context.get(EtablissementLoggedElement.class)))
				.filter(pair -> pair.getRight() != null)
				.filter(pair -> noEtablissement != (Long) pair.getRight().getItemValues().get(LoggedElementAttribute.ETABLISSEMENT_ID))
				.findAny()
				.map(Pair::getLeft)
				.ifPresent(msg -> Assert.fail(String.format("Tous les messages devraient être dans le contexte de l'établissement (trouvé '%s')", msg.text)));

		assertExistMessageWithContent(mr, LogCategory.ETABLISSEMENTS, "\\bL'établissement stable .* n'est couvert par les domiciles qu'à partir du [0-9.]+\\.$");
		assertExistMessageWithContent(mr, LogCategory.ADRESSES, "\\bAdresse trouvée sans rue ni localité postale\\.$");

		// vérification des demandes de fors secondaires enregistrées
		final Map<Class<?>, List<?>> preCommitData = mr.getPreTransactionCommitData();
		Assert.assertEquals(1, preCommitData.size());
		Assert.assertTrue(preCommitData.containsKey(ForsSecondairesData.Activite.class));
		{
			//noinspection unchecked
			final List<ForsSecondairesData.Activite> data = (List<ForsSecondairesData.Activite>) preCommitData.get(ForsSecondairesData.Activite.class);
			Assert.assertEquals(1, data.size());

			{
				final ForsSecondairesData.Activite fsData = data.get(0);
				Assert.assertNotNull(fsData);
				Assert.assertEquals(EntityKey.Type.ENTREPRISE, fsData.entiteJuridiqueSupplier.getKey().getType());
				Assert.assertEquals(noEntreprise, fsData.entiteJuridiqueSupplier.getKey().getId());

				final Map<RegpmCommune, List<DateRange>> communes = fsData.communes;
				Assert.assertNotNull(communes);
				Assert.assertEquals(2, communes.size());

				final List<DateRange> lausanne = communes.get(Commune.LAUSANNE);
				Assert.assertNotNull(lausanne);
				Assert.assertEquals(Collections.<DateRange>singletonList(new DateRangeHelper.Range(dateDebutDomicile, dateDebutSecondDomicile.getOneDayBefore())), lausanne);

				final List<DateRange> echallens = communes.get(Commune.ECHALLENS);
				Assert.assertNotNull(echallens);
				Assert.assertEquals(Collections.<DateRange>singletonList(new DateRangeHelper.Range(dateDebutSecondDomicile, dateFinEtablissementStable)), echallens);
			}
		}
	}

	@Test
	public void testCoordonneesFinancieresAvecRaisonSocialeEtEnseigne() throws Exception {

		final long noEntreprise = 1234L;
		final long noEtablissement = 43256475L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		entreprise.setRaisonSociale1("Ma petite");
		entreprise.setRaisonSociale2("entreprise à moi");
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		etablissement.setCoordonneesFinancieres(createCoordonneesFinancieres(null, "UBSWCHZH80A", "23050422318T", null, "UBS SA", "230"));
		etablissement.setEnseigne("Mon enseigne");

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> une nouvelle entreprise avec un établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertTrue(Long.toString(idEtablissement), Etablissement.ETB_GEN_FIRST_ID <= idEtablissement && idEtablissement <= Etablissement.ETB_GEN_LAST_ID);
		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());       // pas d'établissement stable -> pas de lien

		// avec les coordonnées financières qui vont bien
		doInUniregTransaction(true, status -> {
			final Etablissement etab = (Etablissement) getUniregSessionFactory().getCurrentSession().get(Etablissement.class, idEtablissement);
			Assert.assertEquals("CH350023023050422318T", etab.getNumeroCompteBancaire());
			Assert.assertEquals("UBSWCHZH80A", etab.getAdresseBicSwift());
			Assert.assertEquals("Ma petite entreprise à moi", etab.getTitulaireCompteBancaire());     // utilisation de la raison sociale de l'entreprise liée

			Assert.assertNull(etab.getNumeroEtablissement());
			Assert.assertFalse(etab.isPrincipal());
			Assert.assertEquals("Mon enseigne", etab.getEnseigne());
			Assert.assertEquals(0, etab.getDomiciles().size());

			return null;
		});
	}

	@Test
	public void testCoordonneesFinancieresAvecPersonnePhysique() throws Exception {

		final long noIndividu = 1234L;
		final long noEtablissement = 43256475L;
		final RegpmIndividu individu = IndividuMigratorTest.buildBaseIndividu(noIndividu, "Duplancher", "Philibert", null, Sexe.MASCULIN);

		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, individu);
		etablissement.setCoordonneesFinancieres(createCoordonneesFinancieres(null, "UBSWCHZH80A", "23050422318T", null, "UBS SA", "230"));
		etablissement.setEnseigne("Mon enseigne");

		final MockGraphe graphe = new MockGraphe(null,
		                                         Collections.singletonList(etablissement),
		                                         Collections.singletonList(individu));
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> une nouvelle entreprise avec un établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertTrue(Long.toString(idEtablissement), Etablissement.ETB_GEN_FIRST_ID <= idEtablissement && idEtablissement <= Etablissement.ETB_GEN_LAST_ID);
		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());       // pas d'établissement stable -> pas de lien

		// avec les coordonnées financières qui vont bien
		doInUniregTransaction(true, status -> {
			final Etablissement etab = (Etablissement) getUniregSessionFactory().getCurrentSession().get(Etablissement.class, idEtablissement);
			Assert.assertEquals("CH350023023050422318T", etab.getNumeroCompteBancaire());
			Assert.assertEquals("UBSWCHZH80A", etab.getAdresseBicSwift());
			Assert.assertEquals("Philibert Duplancher", etab.getTitulaireCompteBancaire());     // utilisation des noms et prénoms de la personne liée

			Assert.assertNull(etab.getNumeroEtablissement());
			Assert.assertFalse(etab.isPrincipal());
			Assert.assertEquals("Mon enseigne", etab.getEnseigne());
			Assert.assertEquals(0, etab.getDomiciles().size());

			return null;
		});
	}

	@Test
	public void testCoordonneesFinancieresSansRaisonSocialeMaisEnseigne() throws Exception {

		final long noEntreprise = 1234L;
		final long noEtablissement = 43256475L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		etablissement.setCoordonneesFinancieres(createCoordonneesFinancieres(null, "UBSWCHZH80A", "23050422318T", null, "UBS SA", "230"));
		etablissement.setEnseigne("Mon enseigne");

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> une nouvelle entreprise avec un établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertTrue(Long.toString(idEtablissement), Etablissement.ETB_GEN_FIRST_ID <= idEtablissement && idEtablissement <= Etablissement.ETB_GEN_LAST_ID);
		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());       // pas d'établissement stable -> pas de lien

		// avec les coordonnées financières qui vont bien
		doInUniregTransaction(true, status -> {
			final Etablissement etab = (Etablissement) getUniregSessionFactory().getCurrentSession().get(Etablissement.class, idEtablissement);
			Assert.assertEquals("CH350023023050422318T", etab.getNumeroCompteBancaire());
			Assert.assertEquals("UBSWCHZH80A", etab.getAdresseBicSwift());
			Assert.assertEquals("Mon enseigne", etab.getTitulaireCompteBancaire());     // utilisation l'enseigne en l'absence de raison sociale sur l'entreprise parente

			Assert.assertNull(etab.getNumeroEtablissement());
			Assert.assertFalse(etab.isPrincipal());
			Assert.assertEquals("Mon enseigne", etab.getEnseigne());
			Assert.assertEquals(0, etab.getDomiciles().size());

			return null;
		});
	}

	@Test
	public void testEnseigne() throws Exception {

		final long noEntreprise = 1234L;
		final long noEtablissement = 43256475L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		etablissement.setEnseigne("La rouge musaraigne");

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> une nouvelle entreprise avec un établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertTrue(Long.toString(idEtablissement), Etablissement.ETB_GEN_FIRST_ID <= idEtablissement && idEtablissement <= Etablissement.ETB_GEN_LAST_ID);
		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());       // pas d'établissement stable -> pas de lien

		// vérification des messages collectés
		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.ETABLISSEMENTS, LogCategory.SUIVI);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.ETABLISSEMENTS, "\\bEtablissement sans domicile\\.$");
		assertExistMessageWithContent(mr, LogCategory.ETABLISSEMENTS, "\\bEtablissement sans aucune période de validité d'un établissement stable \\(aucun lien créé\\)\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bEtablissement migré : [0-9.]+\\.$");

		// avec les coordonnées financières qui vont bien
		doInUniregTransaction(true, status -> {
			final Etablissement etab = (Etablissement) getUniregSessionFactory().getCurrentSession().get(Etablissement.class, idEtablissement);
			Assert.assertNull(etab.getNumeroCompteBancaire());
			Assert.assertNull(etab.getAdresseBicSwift());
			Assert.assertNull(etab.getTitulaireCompteBancaire());

			Assert.assertNull(etab.getNumeroEtablissement());
			Assert.assertFalse(etab.isPrincipal());
			Assert.assertEquals("La rouge musaraigne", etab.getEnseigne());
			Assert.assertEquals(0, etab.getDomiciles().size());

			return null;
		});
	}

	@Test
	public void testCommuneUniqueVaudoise() throws Exception {

		final long noEntreprise = 1234L;
		final long noEtablissement = 43256475L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		etablissement.setEnseigne("La verte mangouste");
		addDomicileEtablissement(etablissement, RegDate.get(2000, 1, 1), Commune.ECHALLENS, true);      // domicile annulé -> pas pris en compte
		addDomicileEtablissement(etablissement, RegDate.get(2000, 1, 1), Commune.MORGES, false);
		addEtablissementStable(etablissement, RegDate.get(2000, 1, 1), null);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> une nouvelle entreprise avec un établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertTrue(Long.toString(idEtablissement), Etablissement.ETB_GEN_FIRST_ID <= idEtablissement && idEtablissement <= Etablissement.ETB_GEN_LAST_ID);
		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));

		Assert.assertEquals(1, linkCollector.getCollectedLinks().size());
		{
			final EntityLinkCollector.EntityLink collectedLink = linkCollector.getCollectedLinks().get(0);
			Assert.assertNotNull(collectedLink);
			Assert.assertEquals(RegDate.get(2000, 1, 1), collectedLink.getDateDebut());
			Assert.assertNull(collectedLink.getDateFin());
			Assert.assertEquals(EntityLinkCollector.LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, collectedLink.getType());
		}

		// vérification des messages collectés
		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.ADRESSES, LogCategory.SUIVI, LogCategory.ETABLISSEMENTS);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.ADRESSES, "\\bAdresse trouvée sans rue ni localité postale\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bEtablissement migré : [0-9.]+\\.$");
		assertExistMessageWithContent(mr, LogCategory.ETABLISSEMENTS, "\\bDomicile .* sur COMMUNE_OU_FRACTION_VD/5642\\.$");

		// avec les coordonnées financières qui vont bien
		doInUniregTransaction(true, status -> {
			final Etablissement etab = (Etablissement) getUniregSessionFactory().getCurrentSession().get(Etablissement.class, idEtablissement);
			Assert.assertNull(etab.getNumeroCompteBancaire());
			Assert.assertNull(etab.getAdresseBicSwift());
			Assert.assertNull(etab.getTitulaireCompteBancaire());

			Assert.assertNull(etab.getNumeroEtablissement());
			Assert.assertFalse(etab.isPrincipal());
			Assert.assertEquals("La verte mangouste", etab.getEnseigne());

			final List<DomicileEtablissement> domiciles = etab.getSortedDomiciles(true);
			Assert.assertEquals(1, domiciles.size());
			{
				final DomicileEtablissement domicile = domiciles.get(0);
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(RegDate.get(2000, 1, 1), domicile.getDateDebut());
				Assert.assertNull(domicile.getDateFin());
			}

			return null;
		});
	}

	@Test
	public void testCommuneUniqueHorsCanton() throws Exception {

		final long noEntreprise = 1234L;
		final long noEtablissement = 43256475L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		etablissement.setEnseigne("Le jaune éléphant");
		addDomicileEtablissement(etablissement, RegDate.get(1998, 1, 1), Commune.ECHALLENS, true);      // domicile annulé -> pas pris en compte
		addDomicileEtablissement(etablissement, RegDate.get(2000, 1, 1), Commune.BALE, false);
		addEtablissementStable(etablissement, RegDate.get(2000, 1, 1), null);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> une nouvelle entreprise avec un établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertTrue(Long.toString(idEtablissement), Etablissement.ETB_GEN_FIRST_ID <= idEtablissement && idEtablissement <= Etablissement.ETB_GEN_LAST_ID);
		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));

		Assert.assertEquals(1, linkCollector.getCollectedLinks().size());
		{
			final EntityLinkCollector.EntityLink collectedLink = linkCollector.getCollectedLinks().get(0);
			Assert.assertNotNull(collectedLink);
			Assert.assertEquals(RegDate.get(2000, 1, 1), collectedLink.getDateDebut());
			Assert.assertNull(collectedLink.getDateFin());
			Assert.assertEquals(EntityLinkCollector.LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, collectedLink.getType());
		}

		// vérification des messages collectés
		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.ADRESSES, LogCategory.SUIVI, LogCategory.ETABLISSEMENTS);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.ADRESSES, "\\bAdresse trouvée sans rue ni localité postale\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bEtablissement migré : [0-9.]+\\.$");
		assertExistMessageWithContent(mr, LogCategory.ETABLISSEMENTS, "\\bDomicile .* sur COMMUNE_HC/2701\\.$");

		// avec les coordonnées financières qui vont bien
		doInUniregTransaction(true, status -> {
			final Etablissement etab = (Etablissement) getUniregSessionFactory().getCurrentSession().get(Etablissement.class, idEtablissement);
			Assert.assertNull(etab.getNumeroCompteBancaire());
			Assert.assertNull(etab.getAdresseBicSwift());
			Assert.assertNull(etab.getTitulaireCompteBancaire());

			Assert.assertNull(etab.getNumeroEtablissement());
			Assert.assertFalse(etab.isPrincipal());
			Assert.assertEquals("Le jaune éléphant", etab.getEnseigne());

			final List<DomicileEtablissement> domiciles = etab.getSortedDomiciles(true);
			Assert.assertEquals(1, domiciles.size());
			{
				final DomicileEtablissement domicile = domiciles.get(0);
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Bale.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(RegDate.get(2000, 1, 1), domicile.getDateDebut());
				Assert.assertNull(domicile.getDateFin());
			}

			return null;
		});
	}

	@Test
	public void testCommunesMultiples() throws Exception {

		final long noEntreprise = 1234L;
		final long noEtablissement = 43256475L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		etablissement.setEnseigne("L'orange pie");
		addDomicileEtablissement(etablissement, RegDate.get(1998, 1, 1), Commune.ECHALLENS, false);
		addDomicileEtablissement(etablissement, RegDate.get(2000, 1, 1), Commune.MORGES, false);
		addEtablissementStable(etablissement, RegDate.get(1995, 1, 1), RegDate.get(2005, 12, 31));

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> une nouvelle entreprise avec un établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertTrue(Long.toString(idEtablissement), Etablissement.ETB_GEN_FIRST_ID <= idEtablissement && idEtablissement <= Etablissement.ETB_GEN_LAST_ID);
		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));

		Assert.assertEquals(1, linkCollector.getCollectedLinks().size());
		{
			final EntityLinkCollector.EntityLink collectedLink = linkCollector.getCollectedLinks().get(0);
			Assert.assertNotNull(collectedLink);
			Assert.assertEquals(RegDate.get(1995, 1, 1), collectedLink.getDateDebut());
			Assert.assertEquals(RegDate.get(2005, 12, 31), collectedLink.getDateFin());
			Assert.assertEquals(EntityLinkCollector.LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, collectedLink.getType());
		}

		// vérification des messages collectés
		final Set<LogCategory> expectedCategories = EnumSet.of(LogCategory.ETABLISSEMENTS, LogCategory.ADRESSES, LogCategory.SUIVI);
		mr.getMessages().keySet().stream()
				.filter(cat -> !expectedCategories.contains(cat))
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		assertExistMessageWithContent(mr, LogCategory.ETABLISSEMENTS, "\\bL'établissement stable \\[01\\.01\\.1995 -> 31\\.12\\.2005\\] n'est couvert par les domiciles qu'à partir du 01\\.01\\.1998\\.$");
		assertExistMessageWithContent(mr, LogCategory.ADRESSES, "\\bAdresse trouvée sans rue ni localité postale\\.$");
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bEtablissement migré : [0-9.]+\\.$");

		// avec les coordonnées financières qui vont bien
		doInUniregTransaction(true, status -> {
			final Etablissement etab = (Etablissement) getUniregSessionFactory().getCurrentSession().get(Etablissement.class, idEtablissement);
			Assert.assertNull(etab.getNumeroCompteBancaire());
			Assert.assertNull(etab.getAdresseBicSwift());
			Assert.assertNull(etab.getTitulaireCompteBancaire());

			Assert.assertNull(etab.getNumeroEtablissement());
			Assert.assertFalse(etab.isPrincipal());
			Assert.assertEquals("L'orange pie", etab.getEnseigne());

			final List<DomicileEtablissement> domiciles = etab.getSortedDomiciles(true);
			Assert.assertEquals(2, domiciles.size());
			{
				final DomicileEtablissement domicile = domiciles.get(0);
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(RegDate.get(1998, 1, 1), domicile.getDateDebut());
				Assert.assertEquals(RegDate.get(1999, 12, 31), domicile.getDateFin());
			}
			{
				final DomicileEtablissement domicile = domiciles.get(1);
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(RegDate.get(2000, 1, 1), domicile.getDateDebut());
				Assert.assertEquals(RegDate.get(2005, 12, 31), domicile.getDateFin());
			}

			return null;
		});
	}

	@Test
	public void testAdresseEnAbsenceDEtablissementStable() throws Exception {
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(12442L);
		final RegpmEtablissement etablissement = buildEtablissement(5435L, entreprise);
		etablissement.setNomRue("Avenue de Longemalle");
		etablissement.setLocalitePostale(LocalitePostale.RENENS);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// il y a eu un temps où la migration échouait sur un appel à RegpmEtablissement.Adresse.getDateFin()
		// en absence d'établissement stable

		final AdresseAvecRue adresse = etablissement.getAdresse();
		Assert.assertNull(adresse);
	}

	@Test
	public void testEtablissementStableAvecDateFinDansLeFutur() throws Exception {

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(12442L);
		final RegpmEtablissement etablissement = buildEtablissement(5435L, entreprise);
		addEtablissementStable(etablissement, RegDate.get(2005, 3, 12), RegDate.get().addDays(2));       // date de fin toujours dans le futur
		addDomicileEtablissement(etablissement, RegDate.get(2005, 3, 12), Commune.LAUSANNE, false);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// extraction de l'identifiant de l'établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		// validation des dates de domiciles
		doInUniregTransaction(true, status -> {
			final Etablissement etb = (Etablissement) getTiersDAO().get(idEtablissement);
			Assert.assertNotNull(etb);

			final Set<DomicileEtablissement> domiciles = etb.getDomiciles();
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(1, domiciles.size());

			final DomicileEtablissement domicile = domiciles.iterator().next();
			Assert.assertNotNull(domicile);
			Assert.assertFalse(domicile.isAnnule());
			Assert.assertEquals(RegDate.get(2005, 3, 12), domicile.getDateDebut());
			Assert.assertNull(domicile.getDateFin());               // -> date nulle car ignorée dans le futur
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), domicile.getNumeroOfsAutoriteFiscale());
		});

		// validation des liens (en particulier dates)
		Assert.assertEquals(1, linkCollector.getCollectedLinks().size());
		{
			final EntityLinkCollector.EntityLink collectedLink = linkCollector.getCollectedLinks().get(0);
			Assert.assertNotNull(collectedLink);
			Assert.assertEquals(RegDate.get(2005, 3, 12), collectedLink.getDateDebut());
			Assert.assertNull(collectedLink.getDateFin());          // -> date null car ignorée dans le futur
			Assert.assertEquals(EntityLinkCollector.LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, collectedLink.getType());
		}

		assertExistMessageWithContent(mr, LogCategory.ETABLISSEMENTS, "\\bEtablissement stable avec date de fin dans le futur [0-9.]+ : la migration ignore cette date\\.$");
	}
}
