package ch.vd.uniregctb.migration.pm.engine;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationMessage;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.adapter.rcent.service.RCEntAdapter;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.MigrationResultMessageProvider;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LoggedElementRenderer;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmImmeuble;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.ValidationInterceptor;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.validation.EntityValidator;
import ch.vd.uniregctb.validation.ValidationService;

public class GrapheMigratorTest extends AbstractMigrationEngineTest {

	private GrapheMigrator grapheMigrator;
	private ValidationService validationService;
	private UniregStore uniregStore;

	private static Map<LogCategory, List<String>> buildTextualMessages(MigrationResultMessageProvider mr) {
		return Stream.of(LogCategory.values())
				.map(cat -> mr.getMessages(cat).stream().map(msg -> Pair.of(cat, LoggedElementRenderer.INSTANCE.toString(msg))))
				.flatMap(Function.identity())
				.collect(Collectors.toMap(Pair::getKey,
				                          pair -> Collections.singletonList(pair.getValue()),
				                          (v1, v2) -> Stream.concat(v1.stream(), v2.stream()).collect(Collectors.toList()),
				                          () -> new EnumMap<>(LogCategory.class)));
	}

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();

		uniregStore = getBean(UniregStore.class, "uniregStore");

		final BouclementService bouclementService = getBean(BouclementService.class, "bouclementService");
		final RCEntAdapter rcEntAdapter = getBean(RCEntAdapter.class, "rcEntAdapter");
		final AdresseHelper adresseHelper = getBean(AdresseHelper.class, "adresseHelper");
		final TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		final RcPersClient rcpersClient = getBean(RcPersClient.class, "rcpersClient");
		final NonHabitantIndex nonHabitantIndex = getBean(NonHabitantIndex.class, "nonHabitantIndex");
		final ValidationInterceptor validationInterceptor = getBean(ValidationInterceptor.class, "validationInterceptor");
		final ActivityManager activityManager = entreprise -> true;         // tout le monde est actif dans ces tests

		grapheMigrator = new GrapheMigrator();
		grapheMigrator.setEntrepriseMigrator(new EntrepriseMigrator(uniregStore, activityManager, bouclementService, rcEntAdapter, adresseHelper));
		grapheMigrator.setEtablissementMigrator(new EtablissementMigrator(uniregStore, activityManager, rcEntAdapter, adresseHelper));
		grapheMigrator.setIndividuMigrator(new IndividuMigrator(uniregStore, activityManager, tiersDAO, rcpersClient, nonHabitantIndex));
		grapheMigrator.setUniregStore(uniregStore);
		grapheMigrator.setUniregTransactionManager(getUniregTransactionManager());
		grapheMigrator.setValidationInterceptor(validationInterceptor);
		grapheMigrator.setActivityManager(activityManager);
		grapheMigrator.afterPropertiesSet();

		validationService = getBean(ValidationService.class, "validationService");
	}

	/**
	 * Ce remplissage est particulièrement important dès qu'on parle de validation (si le rapport n'est pas mis dans
	 * la collection ad'hoc des rapports entre tiers, les validations de cette collection seront forcément biaisés)
	 */
	@Test
	public void testRemplissageRapportsEntreTiersDansTransaction() throws Exception {

		final long idEntrepriseMandante = 42L;
		final long idEntrepriseMandataire = 131L;
		final long idEtablissementMandataire = 3562L;
		final RegpmEntreprise mandant = EntrepriseMigratorTest.buildEntreprise(idEntrepriseMandante);

		final RegpmEntreprise entrepriseMandataire = EntrepriseMigratorTest.buildEntreprise(idEntrepriseMandataire);
		final RegpmEtablissement mandataire = EtablissementMigratorTest.buildEtablissement(idEtablissementMandataire, entrepriseMandataire);
		EntrepriseMigratorTest.addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, null, RegDate.get(2000, 1, 1), RegDate.get(2006, 12, 31));
		EntrepriseMigratorTest.addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, null, RegDate.get(2010, 1, 1), null);

		final Graphe graphe = new MockGraphe(Arrays.asList(mandant, entrepriseMandataire),
		                                     Collections.singletonList(mandataire),
		                                     null);

		// Bidouille : on ajoute temporairement un validateur sur les entreprises
		// Ce validateur va causer une erreur sur l'entreprise mandante si celle-ci a des liens (elle doit en avoir, c'est bien, c'est juste pour pouvoir s'envoyer un message qui décrit ces liens...)
		// Donc :
		// -> s'il n'y a pas d'erreur de validation, c'est que les liens n'ont pas été créés correctement sur l'entreprise mandante (en tout cas, ils n'ont pas été ajoutés correctement dans la collection ad'hoc)
		// -> même s'il y a une erreur de validation, il faut vérifier que c'est le bon lien qui a été créé

		final EntityValidator<Entreprise> additionalValidator = e -> {
			final ValidationResults vr = new ValidationResults();
			if (e.getId() == idEntrepriseMandante) {
				final Set<RapportEntreTiers> rapportsObjet = e.getRapportsObjet();
				final Set<RapportEntreTiers> rapportsSujet = e.getRapportsSujet();
				if (rapportsObjet != null || rapportsSujet != null) {
					vr.addError((rapportsObjet != null ? rapportsObjet.size() : 0) + " objets, " + (rapportsSujet != null ? rapportsSujet.size() : 0) + " sujets");
					if (rapportsObjet != null) {
						rapportsObjet.stream()
								.sorted(Comparator.comparing(RapportEntreTiers::getDateDebut))
								.map(obj -> String.format("Object : %s", obj))
								.forEach(vr::addError);
					}
					if (rapportsSujet != null) {
						rapportsSujet.stream()
								.sorted(Comparator.comparing(RapportEntreTiers::getDateDebut))
								.map(suj -> String.format("Sujet : %s", suj))
								.forEach(vr::addError);
					}
				}
			}
			return vr;
		};
		validationService.registerValidator(Entreprise.class, additionalValidator);
		try {
			final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
			Assert.fail("On a lancé une erreur dès que l'entreprise mandante a des liens... aucun n'a donc été créé ?");
		}
		catch (ValidationException e) {
			final List<ValidationMessage> errors = e.getErrors();
			Assert.assertEquals(3, errors.size());

			{
				final ValidationMessage msg = errors.get(0);
				Assert.assertEquals("0 objets, 2 sujets", msg.getMessage());        // le mandant est le sujet
			}
			{
				final ValidationMessage msg = errors.get(1);
				Assert.assertEquals("Sujet : Mandat (01.01.2000 - 31.12.2006)", msg.getMessage());
			}
			{
				final ValidationMessage msg = errors.get(2);
				Assert.assertEquals("Sujet : Mandat (01.01.2010 - ?)", msg.getMessage());
			}
		}
		finally {
			validationService.unregisterValidator(Entreprise.class, additionalValidator);
		}

		// validation échouée -> rien en base !
		doInUniregTransaction(true, status -> {
			// pas d'entreprise
			final Iterator<Entreprise> entrepriseIterator = uniregStore.iterateOnAllEntities(Entreprise.class);
			Assert.assertFalse(entrepriseIterator.hasNext());

			// pas d'établissement
			final Iterator<Etablissement> etablissementIterator = uniregStore.iterateOnAllEntities(Etablissement.class);
			Assert.assertFalse(etablissementIterator.hasNext());
		});
	}

	@Test
	public void testMigrationBasiqueAvecLiens() throws Exception {

		final long idEntrepriseMandante = 42L;
		final long idEntrepriseMandataire = 131L;
		final long idEtablissementMandataire = 3562L;
		final RegpmEntreprise mandant = EntrepriseMigratorTest.buildEntreprise(idEntrepriseMandante);

		final RegpmEntreprise entrepriseMandataire = EntrepriseMigratorTest.buildEntreprise(idEntrepriseMandataire);
		EntrepriseMigratorTest.addForPrincipalEtranger(entrepriseMandataire, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, MockPays.RoyaumeUni.getNoOFS());

		final RegpmEtablissement mandataire = EtablissementMigratorTest.buildEtablissement(idEtablissementMandataire, entrepriseMandataire);
		EtablissementMigratorTest.addDomicileEtablissement(mandataire, RegDate.get(1995, 1, 1), Commune.ECHALLENS, false);
		EtablissementMigratorTest.addEtablissementStable(mandataire, RegDate.get(1995, 1, 1), null);

		EntrepriseMigratorTest.addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, null, RegDate.get(2000, 1, 1), RegDate.get(2006, 12, 31));
		EntrepriseMigratorTest.addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, null, RegDate.get(2010, 1, 1), null);

		final Graphe graphe = new MockGraphe(Arrays.asList(mandant, entrepriseMandataire),
		                                     Collections.singletonList(mandataire),
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		final MutableLong noContribuableEtablissementPrincipalMandataire = new MutableLong();

		// migration terminée -> vérification en base !
		doInUniregTransaction(true, status -> {

			// entreprises : 2
			final List<Entreprise> entreprises = uniregStore.getEntitiesFromDb(Entreprise.class, null);
			Assert.assertNotNull(entreprises);
			Assert.assertEquals(2, entreprises.size());
			final Map<Long, Entreprise> entrepriseMap = entreprises.stream().collect(Collectors.toMap(Entreprise::getId, Function.identity()));
			Assert.assertTrue(entrepriseMap.containsKey(idEntrepriseMandante));
			Assert.assertTrue(entrepriseMap.containsKey(idEntrepriseMandataire));

			// établissements : 2 (le principal et le secondaire de l'entreprise mandataire - comme l'entreprise mandante n'a pas de for ni de commune, elle n'a pas d'établissement principal)
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(2, etablissements.size());

			// tri en mettant l'établissement secondaire d'abord...
			final List<Etablissement> etablissementsTries = etablissements.stream().sorted(Comparator.comparing(Etablissement::isPrincipal)).collect(Collectors.toList());

			// établissement secondaire du mandataire
			{
				final Etablissement etb = etablissementsTries.get(0);
				Assert.assertEquals(0, etb.getRapportsSujet().size());
				Assert.assertFalse(etb.isPrincipal());

				final Map<TypeRapportEntreTiers, List<RapportEntreTiers>> rapportsObjetMap = etb.getRapportsObjet().stream()
						.collect(Collectors.toMap(RapportEntreTiers::getType,
						                          Collections::singletonList,
						                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).sorted(Comparator.comparing(RapportEntreTiers::getDateDebut)).collect(Collectors.toList())));
				Assert.assertEquals(2, rapportsObjetMap.size());

				// mandats
				{
					final List<RapportEntreTiers> rapports = rapportsObjetMap.get(TypeRapportEntreTiers.MANDAT);
					Assert.assertNotNull(rapports);
					Assert.assertEquals(2, rapports.size());

					// ils sont triés par construction (voir plus haut...)
					{
						final RapportEntreTiers ret = rapports.get(0);
						Assert.assertNotNull(ret);
						Assert.assertTrue(ret instanceof Mandat);
						Assert.assertEquals(RegDate.get(2000, 1, 1), ret.getDateDebut());
						Assert.assertEquals(RegDate.get(2006, 12, 31), ret.getDateFin());
						Assert.assertEquals(etb.getId(), ret.getObjetId());
						Assert.assertEquals((Long) idEntrepriseMandante, ret.getSujetId());
						Assert.assertFalse(ret.isAnnule());
					}
					{
						final RapportEntreTiers ret = rapports.get(1);
						Assert.assertNotNull(ret);
						Assert.assertTrue(ret instanceof Mandat);
						Assert.assertEquals(RegDate.get(2010, 1, 1), ret.getDateDebut());
						Assert.assertNull(ret.getDateFin());
						Assert.assertEquals(etb.getId(), ret.getObjetId());
						Assert.assertEquals((Long) idEntrepriseMandante, ret.getSujetId());
						Assert.assertFalse(ret.isAnnule());
					}
				}

				// lien d'activité économique (établissement <-> entreprise)
				{
					final List<RapportEntreTiers> rapports = rapportsObjetMap.get(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE);
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					{
						final RapportEntreTiers ret = rapports.get(0);
						Assert.assertNotNull(ret);
						Assert.assertTrue(ret instanceof ActiviteEconomique);
						Assert.assertEquals(RegDate.get(1995, 1, 1), ret.getDateDebut());
						Assert.assertNull(ret.getDateFin());
						Assert.assertEquals(etb.getId(), ret.getObjetId());
						Assert.assertEquals((Long) idEntrepriseMandataire, ret.getSujetId());
						Assert.assertFalse(ret.isAnnule());
					}
				}

				// domiciles -> 1, à Echallens
				final List<DomicileEtablissement> domiciles = etb.getSortedDomiciles(true);
				Assert.assertNotNull(domiciles);
				Assert.assertEquals(1, domiciles.size());

				final DomicileEtablissement domicile = domiciles.get(0);
				Assert.assertNotNull(domicile);
				Assert.assertFalse(domicile.isAnnule());
				Assert.assertEquals(RegDate.get(1995, 1, 1), domicile.getDateDebut());
				Assert.assertNull(domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
			}

			// établissement principal de l'entreprise mandataire
			{
				final Etablissement etb = etablissementsTries.get(1);
				Assert.assertEquals(0, etb.getRapportsSujet().size());
				Assert.assertTrue(etb.isPrincipal());

				// récupération du numéro de contribuable de l'établissement principal pour le test des messages plus bas
				noContribuableEtablissementPrincipalMandataire.setValue(etb.getNumero());

				final Map<TypeRapportEntreTiers, List<RapportEntreTiers>> rapportsObjetMap = etb.getRapportsObjet().stream()
						.collect(Collectors.toMap(RapportEntreTiers::getType,
						                          Collections::singletonList,
						                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).sorted(Comparator.comparing(RapportEntreTiers::getDateDebut)).collect(Collectors.toList())));
				Assert.assertEquals(1, rapportsObjetMap.size());

				// lien d'activité économique (établissement <-> entreprise)
				{
					final List<RapportEntreTiers> rapports = rapportsObjetMap.get(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE);
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					{
						final RapportEntreTiers ret = rapports.get(0);
						Assert.assertNotNull(ret);
						Assert.assertTrue(ret instanceof ActiviteEconomique);
						Assert.assertEquals(RegDate.get(1990, 1, 1), ret.getDateDebut());
						Assert.assertNull(ret.getDateFin());
						Assert.assertEquals(etb.getId(), ret.getObjetId());
						Assert.assertEquals((Long) idEntrepriseMandataire, ret.getSujetId());
						Assert.assertFalse(ret.isAnnule());
					}
				}

				// domiciles -> 1, à l'étranger
				final List<DomicileEtablissement> domiciles = etb.getSortedDomiciles(true);
				Assert.assertNotNull(domiciles);
				Assert.assertEquals(1, domiciles.size());

				final DomicileEtablissement domicile = domiciles.get(0);
				Assert.assertNotNull(domicile);
				Assert.assertFalse(domicile.isAnnule());
				Assert.assertEquals(RegDate.get(1990, 1, 1), domicile.getDateDebut());
				Assert.assertNull(domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
			}

			// fors sur l'entreprise mandante -> aucun
			{
				final Entreprise entreprise = entrepriseMap.get(idEntrepriseMandante);
				Assert.assertNotNull(entreprise);
				Assert.assertEquals(0, entreprise.getForsFiscaux().size());
			}

			// fors sur l'entreprise mandataire -> 2 (= un for principal migré directement + un for secondaire établissement calculé)
			{
				final Entreprise entreprise = entrepriseMap.get(idEntrepriseMandataire);
				Assert.assertNotNull(entreprise);

				final Set<ForFiscal> fors = entreprise.getForsFiscaux();
				Assert.assertNotNull(fors);
				Assert.assertEquals(2, fors.size());

				final List<ForFiscal> forsTries = fors.stream().sorted(Comparator.comparing(ForFiscal::getDateDebut)).collect(Collectors.toList());
				{
					final ForFiscal ff = forsTries.get(0);
					Assert.assertNotNull(ff);
					Assert.assertTrue(ff instanceof ForFiscalPrincipalPM);
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ff.getGenreImpot());
					Assert.assertEquals(RegDate.get(1990, 1, 1), ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());

					final ForFiscalPrincipalPM ffp = (ForFiscalPrincipalPM) ff;
					Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
				}
				{
					final ForFiscal ff = forsTries.get(1);
					Assert.assertNotNull(ff);
					Assert.assertTrue(ff instanceof ForFiscalSecondaire);
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ff.getGenreImpot());
					Assert.assertEquals(RegDate.get(1995, 1, 1), ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());

					final ForFiscalSecondaire ffs = (ForFiscalSecondaire) ff;
					Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffs.getMotifOuverture());
					Assert.assertNull(ffs.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, ffs.getMotifRattachement());
				}
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.ADRESSES, LogCategory.FORS), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(9, msgs.size());
			Assert.assertEquals("INFO;;;;;" + idEtablissementMandataire + ";;;" + idEntrepriseMandataire + ";;;;;;;Domicile : [01.01.1995 -> ?] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(0));
			Assert.assertEquals("INFO;;;;;" + idEtablissementMandataire + ";;;" + idEntrepriseMandataire + ";;;;;;;Etablissement migré.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;Pas de commune ni de for principal associé, pas d'établissement principal créé.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;Entreprise migrée.", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(5));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalMandataire.longValue()) + ".", msgs.get(6));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalMandataire.longValue()) + " : [01.01.1990 -> ?] sur PAYS_HS/8215.", msgs.get(7));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;Entreprise migrée.", msgs.get(8));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("WARN;;;;;" + idEtablissementMandataire + ";;;" + idEntrepriseMandataire + ";;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;For secondaire 'activité' [01.01.1995 -> ?] ajouté sur la commune 5518.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntrepriseMandataire + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5518 (maintenant : [01.01.1995 -> ?]).", msgs.get(1));
		}
	}

	@Test
	public void testMigrationEtablissementsMultiples() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		entreprise.setEnseigne("Smart zoo");
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BALE);       // un for principal de base

		// les établissements
		final long idEtablissement1 = 235612L;
		final RegpmEtablissement etablissement1 = EtablissementMigratorTest.buildEtablissement(idEtablissement1, entreprise);
		etablissement1.setEnseigne("Le chat qui fume");
		EtablissementMigratorTest.addDomicileEtablissement(etablissement1, RegDate.get(1999, 5, 12), Commune.MORGES, false);
		EtablissementMigratorTest.addDomicileEtablissement(etablissement1, RegDate.get(2003, 1, 27), Commune.LAUSANNE, false);
		EtablissementMigratorTest.addEtablissementStable(etablissement1, RegDate.get(1999, 5, 12), RegDate.get(2006, 10, 31));

		final long idEtablissement2 = 4367324L;
		final RegpmEtablissement etablissement2 = EtablissementMigratorTest.buildEtablissement(idEtablissement2, entreprise);
		etablissement2.setEnseigne("Le chien qui pête");
		EtablissementMigratorTest.addDomicileEtablissement(etablissement2, RegDate.get(2002, 7, 14), Commune.LAUSANNE, false);
		EtablissementMigratorTest.addDomicileEtablissement(etablissement2, RegDate.get(2004, 3, 22), Commune.ECHALLENS, false);
		EtablissementMigratorTest.addEtablissementStable(etablissement2, RegDate.get(2002, 7, 14), RegDate.get(2010, 11, 25));

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     Arrays.asList(etablissement1, etablissement2),
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// pour tester la cohérence avec le message de suivi par la suite
		final MutableLong noContribuableEtablissementPrincipalCree = new MutableLong();

		// vérification du résultat
		doInUniregTransaction(true, status -> {

			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);       // c'est le même identifiant dans RegPM et dans Unireg
			Assert.assertNotNull(e);

			// on vérifie d'abord qu'il y a bien trois établissements liés (2 secondaires + 1 principal)
			final Set<RapportEntreTiers> rapports = e.getRapportsSujet();
			Assert.assertNotNull(rapports);
			Assert.assertEquals(3, rapports.size());
			rapports.stream().filter(r -> !(r instanceof ActiviteEconomique)).findAny().ifPresent(r -> Assert.fail("Rapport " + r + " trouvé là où seuls des rapports d'activité économiques étaient attendus"));
			final List<ActiviteEconomique> activitesEconomiques = rapports.stream()
					.map(r -> (ActiviteEconomique) r)
					.sorted(Comparator.comparing(ActiviteEconomique::getDateDebut))
					.collect(Collectors.toList());

			final Long idEtbPrn;
			{
				final ActiviteEconomique ae = activitesEconomiques.get(0);
				Assert.assertNotNull(ae);
				Assert.assertEquals(RegDate.get(1990, 1, 1), ae.getDateDebut());
				Assert.assertNull(ae.getDateFin());
				Assert.assertEquals((Long) idEntreprise, ae.getSujetId());

				idEtbPrn = ae.getObjetId();
				Assert.assertNotNull(idEtbPrn);
			}
			final Long idEtbSec1;
			{
				final ActiviteEconomique ae = activitesEconomiques.get(1);
				Assert.assertNotNull(ae);
				Assert.assertEquals(RegDate.get(1999, 5, 12), ae.getDateDebut());
				Assert.assertEquals(RegDate.get(2006, 10, 31), ae.getDateFin());
				Assert.assertEquals((Long) idEntreprise, ae.getSujetId());

				idEtbSec1 = ae.getObjetId();
				Assert.assertNotNull(idEtbSec1);
			}
			final Long idEtbSec2;
			{
				final ActiviteEconomique ae = activitesEconomiques.get(2);
				Assert.assertNotNull(ae);
				Assert.assertEquals(RegDate.get(2002, 7, 14), ae.getDateDebut());
				Assert.assertEquals(RegDate.get(2010, 11, 25), ae.getDateFin());
				Assert.assertEquals((Long) idEntreprise, ae.getSujetId());

				idEtbSec2 = ae.getObjetId();
				Assert.assertNotNull(idEtbSec2);
			}
			Assert.assertNotEquals(idEtbSec1, idEtbSec2);
			Assert.assertNotEquals(idEtbSec1, idEtbPrn);
			Assert.assertNotEquals(idEtbSec2, idEtbPrn);

			// vérification qu'il n'y a bien que ces trois établissements-là en base
			final List<Etablissement> allEtablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(allEtablissements);
			Assert.assertEquals(3, allEtablissements.size());

			// vérification des domiciles de ces établissements
			{
				final Etablissement etb = uniregStore.getEntityFromDb(Etablissement.class, idEtbPrn);
				Assert.assertNotNull(etb);
				Assert.assertEquals("Smart zoo", etb.getEnseigne());
				Assert.assertTrue(etb.isPrincipal());

				// mémorisation de la valeur...
				noContribuableEtablissementPrincipalCree.setValue(idEtbPrn);

				final Set<DomicileEtablissement> domiciles = etb.getDomiciles();
				Assert.assertNotNull(domiciles);
				Assert.assertEquals(1, domiciles.size());

				final DomicileEtablissement domicile = domiciles.iterator().next();
				Assert.assertNotNull(domicile);
				Assert.assertEquals(RegDate.get(1990, 1, 1), domicile.getDateDebut());
				Assert.assertNull(domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Bale.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
				Assert.assertFalse(domicile.isAnnule());
			}
			{
				final Etablissement etb = uniregStore.getEntityFromDb(Etablissement.class, idEtbSec1);
				Assert.assertNotNull(etb);
				Assert.assertEquals("Le chat qui fume", etb.getEnseigne());
				Assert.assertFalse(etb.isPrincipal());

				final Set<DomicileEtablissement> domiciles = etb.getDomiciles();
				Assert.assertNotNull(domiciles);
				Assert.assertEquals(2, domiciles.size());

				final Map<Integer, DomicileEtablissement> domicileParCommune = domiciles.stream().collect(Collectors.toMap(DomicileEtablissement::getNumeroOfsAutoriteFiscale, Function.identity()));
				{
					final DomicileEtablissement domicile = domicileParCommune.get(MockCommune.Morges.getNoOFS());
					Assert.assertNotNull("Pas de domicile à Morges ?", domicile);
					Assert.assertEquals(RegDate.get(1999, 5, 12), domicile.getDateDebut());
					Assert.assertEquals(RegDate.get(2003, 1, 26), domicile.getDateFin());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
					Assert.assertFalse(domicile.isAnnule());
				}
				{
					final DomicileEtablissement domicile = domicileParCommune.get(MockCommune.Lausanne.getNoOFS());
					Assert.assertNotNull("Pas de domicile à Lausanne ?", domicile);
					Assert.assertEquals(RegDate.get(2003, 1, 27), domicile.getDateDebut());
					Assert.assertEquals(RegDate.get(2006, 10, 31), domicile.getDateFin());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
					Assert.assertFalse(domicile.isAnnule());
				}
			}
			{
				final Etablissement etb = uniregStore.getEntityFromDb(Etablissement.class, idEtbSec2);
				Assert.assertNotNull(etb);
				Assert.assertEquals("Le chien qui pête", etb.getEnseigne());
				Assert.assertFalse(etb.isPrincipal());

				final Set<DomicileEtablissement> domiciles = etb.getDomiciles();
				Assert.assertNotNull(domiciles);
				Assert.assertEquals(2, domiciles.size());

				final Map<Integer, DomicileEtablissement> domicileParCommune = domiciles.stream().collect(Collectors.toMap(DomicileEtablissement::getNumeroOfsAutoriteFiscale, Function.identity()));
				{
					final DomicileEtablissement domicile = domicileParCommune.get(MockCommune.Lausanne.getNoOFS());
					Assert.assertNotNull("Pas de domicile à Lausanne ?", domicile);
					Assert.assertEquals(RegDate.get(2002, 7, 14), domicile.getDateDebut());
					Assert.assertEquals(RegDate.get(2004, 3, 21), domicile.getDateFin());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
					Assert.assertFalse(domicile.isAnnule());
				}
				{
					final DomicileEtablissement domicile = domicileParCommune.get(MockCommune.Echallens.getNoOFS());
					Assert.assertNotNull("Pas de domicile à Echallens ?", domicile);
					Assert.assertEquals(RegDate.get(2004, 3, 22), domicile.getDateDebut());
					Assert.assertEquals(RegDate.get(2010, 11, 25), domicile.getDateFin());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
					Assert.assertFalse(domicile.isAnnule());
				}
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.ADRESSES, LogCategory.FORS), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(10, msgs.size());
			Assert.assertEquals("INFO;;;;;" + idEtablissement1 + ";;;" + idEntreprise + ";;;;;;;Domicile : [12.05.1999 -> 26.01.2003] sur COMMUNE_OU_FRACTION_VD/5642.", msgs.get(0));
			Assert.assertEquals("INFO;;;;;" + idEtablissement1 + ";;;" + idEntreprise + ";;;;;;;Domicile : [27.01.2003 -> 31.10.2006] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(1));
			Assert.assertEquals("INFO;;;;;" + idEtablissement1 + ";;;" + idEntreprise + ";;;;;;;Etablissement migré.", msgs.get(2));
			Assert.assertEquals("INFO;;;;;" + idEtablissement2 + ";;;" + idEntreprise + ";;;;;;;Domicile : [14.07.2002 -> 21.03.2004] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(3));
			Assert.assertEquals("INFO;;;;;" + idEtablissement2 + ";;;" + idEntreprise + ";;;;;;;Domicile : [22.03.2004 -> 25.11.2010] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(4));
			Assert.assertEquals("INFO;;;;;" + idEtablissement2 + ";;;" + idEntreprise + ";;;;;;;Etablissement migré.", msgs.get(5));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(6));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(7));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.01.1990 -> ?] sur COMMUNE_HC/2701.", msgs.get(8));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Entreprise migrée.", msgs.get(9));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("WARN;;;;;" + idEtablissement1 + ";;;" + idEntreprise + ";;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(0));
			Assert.assertEquals("WARN;;;;;" + idEtablissement2 + ";;;" + idEntreprise + ";;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(6, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [22.03.2004 -> 25.11.2010] ajouté sur la commune 5518.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [14.07.2002 -> 31.10.2006] ajouté sur la commune 5586.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [12.05.1999 -> 26.01.2003] ajouté sur la commune 5642.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5518 (maintenant : [22.03.2004 -> 25.11.2010]).", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5586 (maintenant : [14.07.2002 -> 31.10.2006]).", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5642 (maintenant : [12.05.1999 -> 26.01.2003]).", msgs.get(5));
		}
	}

	@Test
	public void testCalculForsSecondairesActiviteEtImmeuble() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BALE);       // un for principal de base

		// les établissements
		final long idEtablissement1 = 235612L;
		final RegpmEtablissement etablissement1 = EtablissementMigratorTest.buildEtablissement(idEtablissement1, entreprise);
		etablissement1.setEnseigne("Le chat qui fume");
		EtablissementMigratorTest.addDomicileEtablissement(etablissement1, RegDate.get(1999, 5, 12), Commune.MORGES, false);
		EtablissementMigratorTest.addDomicileEtablissement(etablissement1, RegDate.get(2003, 1, 27), Commune.LAUSANNE, false);
		EtablissementMigratorTest.addEtablissementStable(etablissement1, RegDate.get(1999, 5, 12), RegDate.get(2006, 10, 31));

		final long idEtablissement2 = 4367324L;
		final RegpmEtablissement etablissement2 = EtablissementMigratorTest.buildEtablissement(idEtablissement2, entreprise);
		etablissement2.setEnseigne("Le chien qui pête");
		EtablissementMigratorTest.addDomicileEtablissement(etablissement2, RegDate.get(2002, 7, 14), Commune.LAUSANNE, false);
		EtablissementMigratorTest.addDomicileEtablissement(etablissement2, RegDate.get(2004, 3, 22), Commune.ECHALLENS, false);
		EtablissementMigratorTest.addEtablissementStable(etablissement2, RegDate.get(2002, 7, 14), RegDate.get(2010, 11, 25));

		// les immeubles
		final RegpmImmeuble immeuble1 = createImmeuble(Commune.ECHALLENS);
		EntrepriseMigratorTest.addRattachementProprietaire(entreprise, RegDate.get(2006, 5, 1), RegDate.get(2010, 12, 31), immeuble1);

		final RegpmImmeuble immeuble2 = createImmeuble(Commune.ECHALLENS);
		final RegpmGroupeProprietaire groupe2 = createGroupeProprietaire("Zoo", RegpmTypeGroupeProprietaire.CONSORTIUM_SOCIETE_SIMPLE, RegDate.get(2000, 1, 1), null);
		EntrepriseMigratorTest.addAppartenanceGroupeProprietaire(entreprise, groupe2, RegDate.get(2004, 5, 29), RegDate.get(2009, 12, 21), false);
		EntrepriseMigratorTest.addRattachementProprietaire(groupe2, RegDate.get(2004, 7, 1), null, immeuble2);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     Arrays.asList(etablissement1, etablissement2),
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// pour tester la cohérence avec le message de suivi par la suite
		final MutableLong noContribuableEtablissementPrincipalCree = new MutableLong();

		// vérification du résultat
		doInUniregTransaction(true, status -> {

			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);       // c'est le même identifiant dans RegPM et dans Unireg
			Assert.assertNotNull(e);

			// pour tester la cohérence avec le message de suivi par la suite
			noContribuableEtablissementPrincipalCree.setValue(uniregStore.getEntitiesFromDb(Etablissement.class, null).stream()
					                                                  .filter(Etablissement::isPrincipal)
					                                                  .findAny()
					                                                  .map(Etablissement::getId)
					                                                  .orElseThrow(() -> new IllegalStateException("Aucun établissement principal trouvé!")));

			final List<ForFiscal> fors = e.getForsFiscauxSorted();
			Assert.assertEquals(5, fors.size());

			// for fiscal principal à Bâle (migré directement)
			{
				final ForFiscal ff = fors.get(0);
				Assert.assertNotNull(ff);
				Assert.assertEquals(RegDate.get(1990, 1, 1), ff.getDateDebut());
				Assert.assertNull(ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ff.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Bale.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ff.getGenreImpot());
				Assert.assertFalse(ff.isAnnule());
				Assert.assertTrue(ff instanceof ForFiscalPrincipalPM);

				final ForFiscalPrincipalPM ffp = (ForFiscalPrincipalPM) ff;
				Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getMotifFermeture());
				Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
			}

			// for fiscal secondaire (activité économique) à Morges
			{
				final ForFiscal ff = fors.get(1);
				Assert.assertNotNull(ff);
				Assert.assertEquals(RegDate.get(1999, 5, 12), ff.getDateDebut());
				Assert.assertEquals(RegDate.get(2003, 1, 26), ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ff.getGenreImpot());
				Assert.assertFalse(ff.isAnnule());
				Assert.assertTrue(ff instanceof ForFiscalSecondaire);

				final ForFiscalSecondaire ffs = (ForFiscalSecondaire) ff;
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffs.getMotifOuverture());
				Assert.assertEquals(MotifFor.FIN_EXPLOITATION, ffs.getMotifFermeture());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, ffs.getMotifRattachement());
			}

			// for fiscal secondaire (activité économique) à Lausanne
			{
				final ForFiscal ff = fors.get(2);
				Assert.assertNotNull(ff);
				Assert.assertEquals(RegDate.get(2002, 7, 14), ff.getDateDebut());
				Assert.assertEquals(RegDate.get(2006, 10, 31), ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ff.getGenreImpot());
				Assert.assertFalse(ff.isAnnule());
				Assert.assertTrue(ff instanceof ForFiscalSecondaire);

				final ForFiscalSecondaire ffs = (ForFiscalSecondaire) ff;
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffs.getMotifOuverture());
				Assert.assertEquals(MotifFor.FIN_EXPLOITATION, ffs.getMotifFermeture());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, ffs.getMotifRattachement());
			}

			// for fiscal secondaire (activité économique) à Echallens
			{
				final ForFiscal ff = fors.get(3);
				Assert.assertNotNull(ff);
				Assert.assertEquals(RegDate.get(2004, 3, 22), ff.getDateDebut());
				Assert.assertEquals(RegDate.get(2010, 11, 25), ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ff.getGenreImpot());
				Assert.assertFalse(ff.isAnnule());
				Assert.assertTrue(ff instanceof ForFiscalSecondaire);

				final ForFiscalSecondaire ffs = (ForFiscalSecondaire) ff;
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffs.getMotifOuverture());
				Assert.assertEquals(MotifFor.FIN_EXPLOITATION, ffs.getMotifFermeture());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, ffs.getMotifRattachement());
			}

			// for fiscal secondaire (immeuble) à Echallens
			{
				final ForFiscal ff = fors.get(4);
				Assert.assertNotNull(ff);
				Assert.assertEquals(RegDate.get(2004, 7, 1), ff.getDateDebut());
				Assert.assertEquals(RegDate.get(2010, 12, 31), ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ff.getGenreImpot());
				Assert.assertFalse(ff.isAnnule());
				Assert.assertTrue(ff instanceof ForFiscalSecondaire);

				final ForFiscalSecondaire ffs = (ForFiscalSecondaire) ff;
				Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffs.getMotifOuverture());
				Assert.assertEquals(MotifFor.VENTE_IMMOBILIER, ffs.getMotifFermeture());
				Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffs.getMotifRattachement());
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.ADRESSES, LogCategory.FORS), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(10, msgs.size());
			Assert.assertEquals("INFO;;;;;" + idEtablissement1 + ";;;" + idEntreprise + ";;;;;;;Domicile : [12.05.1999 -> 26.01.2003] sur COMMUNE_OU_FRACTION_VD/5642.", msgs.get(0));
			Assert.assertEquals("INFO;;;;;" + idEtablissement1 + ";;;" + idEntreprise + ";;;;;;;Domicile : [27.01.2003 -> 31.10.2006] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(1));
			Assert.assertEquals("INFO;;;;;" + idEtablissement1 + ";;;" + idEntreprise + ";;;;;;;Etablissement migré.", msgs.get(2));
			Assert.assertEquals("INFO;;;;;" + idEtablissement2 + ";;;" + idEntreprise + ";;;;;;;Domicile : [14.07.2002 -> 21.03.2004] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(3));
			Assert.assertEquals("INFO;;;;;" + idEtablissement2 + ";;;" + idEntreprise + ";;;;;;;Domicile : [22.03.2004 -> 25.11.2010] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(4));
			Assert.assertEquals("INFO;;;;;" + idEtablissement2 + ";;;" + idEntreprise + ";;;;;;;Etablissement migré.", msgs.get(5));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(6));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(7));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.01.1990 -> ?] sur COMMUNE_HC/2701.", msgs.get(8));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Entreprise migrée.", msgs.get(9));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("WARN;;;;;" + idEtablissement1 + ";;;" + idEntreprise + ";;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(0));
			Assert.assertEquals("WARN;;;;;" + idEtablissement2 + ";;;" + idEntreprise + ";;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [22.03.2004 -> 25.11.2010] ajouté sur la commune 5518.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [14.07.2002 -> 31.10.2006] ajouté sur la commune 5586.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [12.05.1999 -> 26.01.2003] ajouté sur la commune 5642.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'immeuble' [01.07.2004 -> 31.12.2010] ajouté sur la commune 5518.", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5518 (maintenant : [22.03.2004 -> 31.12.2010]).", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5586 (maintenant : [14.07.2002 -> 31.10.2006]).", msgs.get(5));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5642 (maintenant : [12.05.1999 -> 26.01.2003]).", msgs.get(6));
		}
	}

	@Test
	public void testForSecondaireImmeubleSurFraction() throws Exception {
		final long noEntreprise = 1234L;
		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegDate debut = RegDate.get(2005, 5 , 7);
		EntrepriseMigratorTest.addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.BERN);

		final RegpmImmeuble immeuble = createImmeuble(Commune.Fraction.LE_SENTIER);
		EntrepriseMigratorTest.addRattachementProprietaire(e, debut, null, immeuble);

		final Graphe graphe = new MockGraphe(Collections.singletonList(e),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// vérification du contenu de la base -> une nouvelle entreprise
		final long idEntreprise = doInUniregTransaction(true, status -> {
			final List<Entreprise> entreprises = uniregStore.getEntitiesFromDb(Entreprise.class, null);
			Assert.assertNotNull(entreprises);
			Assert.assertEquals(1, entreprises.size());
			return entreprises.get(0).getNumero();
		});

		// vérification de la commune du for principal créé
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(entreprise);

			final ForsParType fpt = entreprise.getForsParType(true);
			Assert.assertNotNull(fpt);
			Assert.assertEquals(1, fpt.principauxPM.size());
			Assert.assertEquals(0, fpt.principauxPP.size());
			Assert.assertEquals(1, fpt.secondaires.size());
			Assert.assertEquals(0, fpt.dpis.size());
			Assert.assertEquals(0, fpt.autreElementImpot.size());
			Assert.assertEquals(0, fpt.autresImpots.size());

			final ForFiscalSecondaire ff = fpt.secondaires.get(0);
			Assert.assertNotNull(ff);
			Assert.assertFalse(ff.isAnnule());
			Assert.assertEquals(debut, ff.getDateDebut());
			Assert.assertNull(ff.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.Fraction.LE_SENTIER.getId().intValue(), ff.getNumeroOfsAutoriteFiscale().intValue());
			Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ff.getMotifOuverture());
			Assert.assertNull(ff.getMotifFermeture());
			Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ff.getMotifRattachement());
			Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ff.getGenreImpot());
		});
	}

	@Test
	public void testForSecondaireActiviteSurFraction() throws Exception {
		final long noEntreprise = 1234L;
		final long idEtablissement = 3267382L;

		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegDate debut = RegDate.get(2005, 5 , 7);
		EntrepriseMigratorTest.addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.BERN);

		final RegpmEtablissement etb = EtablissementMigratorTest.buildEtablissement(idEtablissement, e);
		EtablissementMigratorTest.addEtablissementStable(etb, debut, null);
		EtablissementMigratorTest.addDomicileEtablissement(etb, debut, Commune.Fraction.LE_BRASSUS, false);

		final Graphe graphe = new MockGraphe(Collections.singletonList(e),
		                                     Collections.singletonList(etb),
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// vérification du contenu de la base -> une nouvelle entreprise
		final long idEntreprise = doInUniregTransaction(true, status -> {
			final List<Entreprise> entreprises = uniregStore.getEntitiesFromDb(Entreprise.class, null);
			Assert.assertNotNull(entreprises);
			Assert.assertEquals(1, entreprises.size());
			return entreprises.get(0).getNumero();
		});

		// vérification de la commune du for principal créé
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(entreprise);

			final ForsParType fpt = entreprise.getForsParType(true);
			Assert.assertNotNull(fpt);
			Assert.assertEquals(1, fpt.principauxPM.size());
			Assert.assertEquals(0, fpt.principauxPP.size());
			Assert.assertEquals(1, fpt.secondaires.size());
			Assert.assertEquals(0, fpt.dpis.size());
			Assert.assertEquals(0, fpt.autreElementImpot.size());
			Assert.assertEquals(0, fpt.autresImpots.size());

			final ForFiscalSecondaire ff = fpt.secondaires.get(0);
			Assert.assertNotNull(ff);
			Assert.assertFalse(ff.isAnnule());
			Assert.assertEquals(debut, ff.getDateDebut());
			Assert.assertNull(ff.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.Fraction.LE_BRASSUS.getId().intValue(), ff.getNumeroOfsAutoriteFiscale().intValue());
			Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ff.getMotifOuverture());
			Assert.assertNull(ff.getMotifFermeture());
			Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, ff.getMotifRattachement());
			Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ff.getGenreImpot());
		});
	}
}
