package ch.vd.uniregctb.migration.pm.engine;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.uniregctb.migration.pm.MigrationResultMessage;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDomicileEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissementStable;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.migration.pm.utils.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.utils.IdMapper;
import ch.vd.uniregctb.tiers.TypeTiers;

public class EtablissementMigratorTest extends AbstractEntityMigratorTest {

	private EtablissementMigrator migrator;

	private static final String REGPM_VISA = "REGPM";
	private static final Timestamp REGPM_MODIF = new Timestamp(DateHelper.getCurrentDate().getTime() - TimeUnit.DAYS.toMillis(2000));   // 2000 jours ~ 5.5 années

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();
		final RcEntClient rcentClient = getBean(RcEntClient.class, "rcentClient");
		migrator = new EtablissementMigrator(getUniregSessionFactory(), getStreetDataMigrator(), getTiersDAO(), rcentClient);
	}

	private static RegpmEtablissement buildEtablissement(long id, RegpmEntreprise entreprise) {
		final RegpmEtablissement etablissement = new RegpmEtablissement();
		etablissement.setId(id);
		etablissement.setEntreprise(entreprise);
		etablissement.setLastMutationOperator(REGPM_VISA);
		etablissement.setLastMutationTimestamp(REGPM_MODIF);
		return etablissement;
	}

	private static <T> int computeNewSeqNo(Collection<T> elements, Function<? super T, Integer> seqNoExtractor) {
		final int biggestSoFar = elements.stream()
				.map(seqNoExtractor)
				.max(Comparator.<Integer>naturalOrder())
				.orElse(0);
		return biggestSoFar + 1;
	}

	private static void addEtablissementStable(RegpmEtablissement etablissement, RegDate dateDebut, RegDate dateFin) {
		// initialisation de la collection, au cas où...
		// ... et attribution d'un nouveau numéro de séquence pour l'identifiant
		Set<RegpmEtablissementStable> stables = etablissement.getEtablissementsStables();
		final int newSeqNo;
		if (stables == null) {
			stables = new HashSet<>();
			etablissement.setEtablissementsStables(stables);
			newSeqNo = 1;
		}
		else {
			newSeqNo = computeNewSeqNo(stables, stable -> stable.getId().getSeqNo());
		}

		// création d'un établissement stable
		final RegpmEtablissementStable stable = new RegpmEtablissementStable();
		stable.setLastMutationOperator(REGPM_VISA);
		stable.setLastMutationTimestamp(REGPM_MODIF);
		stable.setId(new RegpmEtablissementStable.PK(newSeqNo, etablissement.getId()));
		stable.setDateDebut(dateDebut);
		stable.setDateFin(dateFin);
		stables.add(stable);
	}

	private static void addDomicileEtablissement(RegpmEtablissement etablissement, RegDate dateDebut, RegpmCommune commune, boolean annule) {
		// initialization de la collection, au cas où...
		// ... et attribution d'un nouveau numéro de séquence pour l'identifiant
		SortedSet<RegpmDomicileEtablissement> domiciles = etablissement.getDomicilesEtablissements();
		final int newSeqNo;
		if (domiciles == null) {
			domiciles = new TreeSet<>();
			etablissement.setDomicilesEtablissements(domiciles);
			newSeqNo = 1;
		}
		else {
			newSeqNo = computeNewSeqNo(domiciles, domicile -> domicile.getId().getSeqNo());
		}

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

	private static RegpmEntreprise buildEntreprise(long id) {
		final RegpmEntreprise entreprise = new RegpmEntreprise();
		entreprise.setId(id);
		entreprise.setLastMutationOperator(REGPM_VISA);
		entreprise.setLastMutationTimestamp(REGPM_MODIF);
		return entreprise;
	}

	@Test
	public void testSansEtablissementStable() throws Exception {

		final long noEntreprise = 4346375L;
		final long noEtablissement = 353645427L;

		final RegpmEntreprise entreprise = buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> un nouvel établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());

		mr.getMessages().keySet().stream()
				.filter(cat -> cat != MigrationResultMessage.CategorieListe.ETABLISSEMENTS)
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));
		mr.getMessages().get(MigrationResultMessage.CategorieListe.ETABLISSEMENTS).stream()
				.filter(msg -> !msg.getTexte().startsWith("Etablissement " + noEtablissement + " de l'entreprise " + noEntreprise + " : "))
				.findAny()
				.ifPresent(msg -> Assert.fail(String.format("Tous les messages devraient être dans le contexte de l'établissement (trouvé '%s')", msg.getTexte())));

		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.ETABLISSEMENTS, "\\bEtablissement sans aucune période de validité d'un établissement stable\\.$");

		Assert.assertEquals(0, mr.getPreTransactionCommitData().size());
	}

	@Test
	public void testAvecUnEtablissementStableSansDomicile() throws Exception {

		final long noEntreprise = 4346375L;
		final long noEtablissement = 353645427L;
		final RegDate dateDebut = RegDate.get(2000, 1, 13);
		final RegDate dateFin = RegDate.get(2006, 5, 4);

		final RegpmEntreprise entreprise = buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		addEtablissementStable(etablissement, dateDebut, dateFin);

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> un nouvel établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));
		Assert.assertEquals(1, linkCollector.getCollectedLinks().size());

		// lien vers l'entité juridique avec les dates des établissements stables
		{
			final EntityLinkCollector.EntityLink link = linkCollector.getCollectedLinks().get(0);
			Assert.assertEquals(EntityLinkCollector.LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, link.getType());
			Assert.assertEquals(dateDebut, link.getDateDebut());
			Assert.assertEquals(dateFin, link.getDateFin());
		}

		mr.getMessages().keySet().stream()
				.filter(cat -> cat != MigrationResultMessage.CategorieListe.ETABLISSEMENTS)
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));
		mr.getMessages().get(MigrationResultMessage.CategorieListe.ETABLISSEMENTS).stream()
				.filter(msg -> !msg.getTexte().startsWith("Etablissement " + noEtablissement + " de l'entreprise " + noEntreprise + " : "))
				.findAny()
				.ifPresent(msg -> Assert.fail(String.format("Tous les messages devraient être dans le contexte de l'établissement (trouvé '%s')", msg.getTexte())));

		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.ETABLISSEMENTS, "\\bL'établissement stable .* n'intersecte aucun domicile\\.$");

		Assert.assertEquals(0, mr.getPreTransactionCommitData().size());
	}

	@Test
	public void testAvecUnEtablissementStableAvecDomicileAnnule() throws Exception {

		final long noEntreprise = 4346375L;
		final long noEtablissement = 353645427L;
		final RegDate dateDebut = RegDate.get(2000, 1, 13);
		final RegDate dateFin = RegDate.get(2006, 5, 4);

		final RegpmEntreprise entreprise = buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		addEtablissementStable(etablissement, dateDebut, dateFin);
		addDomicileEtablissement(etablissement, dateDebut, LAUSANNE, true);     // il y a intersection, mais le domicile est annulé, donc ignoré...

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> un nouvel établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertEquals(idEtablissement, idMapper.getIdUniregEtablissement(noEtablissement));
		Assert.assertEquals(1, linkCollector.getCollectedLinks().size());

		// lien vers l'entité juridique avec les dates des établissements stables
		{
			final EntityLinkCollector.EntityLink link = linkCollector.getCollectedLinks().get(0);
			Assert.assertEquals(EntityLinkCollector.LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, link.getType());
			Assert.assertEquals(dateDebut, link.getDateDebut());
			Assert.assertEquals(dateFin, link.getDateFin());
		}

		mr.getMessages().keySet().stream()
				.filter(cat -> cat != MigrationResultMessage.CategorieListe.ETABLISSEMENTS)
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));
		mr.getMessages().get(MigrationResultMessage.CategorieListe.ETABLISSEMENTS).stream()
				.filter(msg -> !msg.getTexte().startsWith("Etablissement " + noEtablissement + " de l'entreprise " + noEntreprise + " : "))
				.findAny()
				.ifPresent(msg -> Assert.fail(String.format("Tous les messages devraient être dans le contexte de l'établissement (trouvé '%s')", msg.getTexte())));

		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.ETABLISSEMENTS, "\\bL'établissement stable .* n'intersecte aucun domicile\\.$");

		Assert.assertEquals(0, mr.getPreTransactionCommitData().size());
	}

	@Test
	public void testAvecUnEtablissementStableAvecDomicileCouvertureComplete() throws Exception {

		final long noEntreprise = 4346375L;
		final long noEtablissement = 353645427L;
		final RegDate dateDebut = RegDate.get(2000, 1, 13);
		final RegDate dateFin = RegDate.get(2006, 5, 4);

		final RegpmEntreprise entreprise = buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		addEtablissementStable(etablissement, dateDebut, dateFin);
		addDomicileEtablissement(etablissement, dateDebut, LAUSANNE, false);

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> un nouvel établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

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
		mr.getMessages().keySet().stream()
				.filter(cat -> cat != MigrationResultMessage.CategorieListe.ETABLISSEMENTS)
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		final List<MigrationResultMessage> msgsEtablissements = mr.getMessages().get(MigrationResultMessage.CategorieListe.ETABLISSEMENTS);
		if (msgsEtablissements != null) {
			msgsEtablissements.stream()
					.filter(msg -> !msg.getTexte().startsWith("Etablissement " + noEtablissement + " de l'entreprise " + noEntreprise + " : "))
					.findAny()
					.ifPresent(msg -> Assert.fail(String.format("Tous les messages devraient être dans le contexte de l'établissement (trouvé '%s')", msg.getTexte())));
		}

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

				final List<DateRange> lsneData = communes.get(LAUSANNE);
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

		final RegpmEntreprise entreprise = buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		addEtablissementStable(etablissement, dateDebutEtablissementStable, dateFinEtablissementStable);
		addDomicileEtablissement(etablissement, dateDebutDomicile, MORGES, false);

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> un nouvel établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

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
		mr.getMessages().keySet().stream()
				.filter(cat -> cat != MigrationResultMessage.CategorieListe.ETABLISSEMENTS)
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));
		mr.getMessages().get(MigrationResultMessage.CategorieListe.ETABLISSEMENTS).stream()
				.filter(msg -> !msg.getTexte().startsWith("Etablissement " + noEtablissement + " de l'entreprise " + noEntreprise + " : "))
				.findAny()
				.ifPresent(msg -> Assert.fail(String.format("Tous les messages devraient être dans le contexte de l'établissement (trouvé '%s')", msg.getTexte())));

		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.ETABLISSEMENTS, "\\bL'établissement stable .* n'est couvert par les domiciles qu'à partir du [0-9.]+\\.$");

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

				final List<DateRange> morgesData = communes.get(MORGES);
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

		final RegpmEntreprise entreprise = buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		addEtablissementStable(etablissement, dateDebut, dateFin);
		addDomicileEtablissement(etablissement, dateDebut, LAUSANNE, false);
		addDomicileEtablissement(etablissement, dateDebutSecondDomicile, ECHALLENS, false);

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> un nouvel établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

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
		mr.getMessages().keySet().stream()
				.filter(cat -> cat != MigrationResultMessage.CategorieListe.ETABLISSEMENTS)
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		final List<MigrationResultMessage> msgsEtablissements = mr.getMessages().get(MigrationResultMessage.CategorieListe.ETABLISSEMENTS);
		if (msgsEtablissements != null) {
			msgsEtablissements.stream()
					.filter(msg -> !msg.getTexte().startsWith("Etablissement " + noEtablissement + " de l'entreprise " + noEntreprise + " : "))
					.findAny()
					.ifPresent(msg -> Assert.fail(String.format("Tous les messages devraient être dans le contexte de l'établissement (trouvé '%s')", msg.getTexte())));
		}

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

				final List<DateRange> lausanne = communes.get(LAUSANNE);
				Assert.assertNotNull(lausanne);
				Assert.assertEquals(Collections.<DateRange>singletonList(new DateRangeHelper.Range(dateDebut, dateDebutSecondDomicile.getOneDayBefore())), lausanne);

				final List<DateRange> echallens = communes.get(ECHALLENS);
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

		final RegpmEntreprise entreprise = buildEntreprise(noEntreprise);
		final RegpmEtablissement etablissement = buildEtablissement(noEtablissement, entreprise);
		addEtablissementStable(etablissement, dateDebutEtablissementStable, dateFinEtablissementStable);
		addDomicileEtablissement(etablissement, dateDebutDomicile, LAUSANNE, false);
		addDomicileEtablissement(etablissement, dateDebutSecondDomicile, ECHALLENS, false);

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(etablissement, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> un nouvel établissement
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

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
		mr.getMessages().keySet().stream()
				.filter(cat -> cat != MigrationResultMessage.CategorieListe.ETABLISSEMENTS)
				.findAny()
				.ifPresent(cat -> Assert.fail(String.format("Il ne devrait pas y avoir de message dans la catégorie %s", cat)));

		final List<MigrationResultMessage> msgsEtablissements = mr.getMessages().get(MigrationResultMessage.CategorieListe.ETABLISSEMENTS);
		if (msgsEtablissements != null) {
			msgsEtablissements.stream()
					.filter(msg -> !msg.getTexte().startsWith("Etablissement " + noEtablissement + " de l'entreprise " + noEntreprise + " : "))
					.findAny()
					.ifPresent(msg -> Assert.fail(String.format("Tous les messages devraient être dans le contexte de l'établissement (trouvé '%s')", msg.getTexte())));
		}

		assertExistMessageWithContent(mr, MigrationResultMessage.CategorieListe.ETABLISSEMENTS, "\\bL'établissement stable .* n'est couvert par les domiciles qu'à partir du [0-9.]+\\.$");

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

				final List<DateRange> lausanne = communes.get(LAUSANNE);
				Assert.assertNotNull(lausanne);
				Assert.assertEquals(Collections.<DateRange>singletonList(new DateRangeHelper.Range(dateDebutDomicile, dateDebutSecondDomicile.getOneDayBefore())), lausanne);

				final List<DateRange> echallens = communes.get(ECHALLENS);
				Assert.assertNotNull(echallens);
				Assert.assertEquals(Collections.<DateRange>singletonList(new DateRangeHelper.Range(dateDebutSecondDomicile, dateFinEtablissementStable)), echallens);
			}
		}
	}
}
