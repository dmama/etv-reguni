package ch.vd.uniregctb.migration.pm.engine;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
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
import org.junit.Ignore;
import org.junit.Test;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationMessage;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.adapter.rcent.service.RCEntAdapter;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.MigrationResultMessageProvider;
import ch.vd.uniregctb.migration.pm.SerializationIntermediary;
import ch.vd.uniregctb.migration.pm.communes.FractionsCommuneProvider;
import ch.vd.uniregctb.migration.pm.communes.FusionCommunesProvider;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.engine.helpers.DoublonProvider;
import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.log.LoggedElementRenderer;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCategoriePersonneMorale;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmImmeuble;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmModeImposition;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.DatesParticulieres;
import ch.vd.uniregctb.migration.pm.utils.ValidationInterceptor;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.CapitalEntreprise;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DonneesRegistreCommerce;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.validation.EntityValidator;
import ch.vd.uniregctb.validation.ValidationService;

@SuppressWarnings("deprecation")
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

	private static final long INACTIVE_ENTREPRISE_ID = 1832L;

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();

		uniregStore = getBean(UniregStore.class, "uniregStore");

		final BouclementService bouclementService = getBean(BouclementService.class, "bouclementService");
		final ServiceInfrastructureService infraService = getBean(ServiceInfrastructureService.class, "serviceInfrastructureService");
		final RCEntAdapter rcEntAdapter = getBean(RCEntAdapter.class, "rcEntAdapter");
		final AdresseHelper adresseHelper = getBean(AdresseHelper.class, "adresseHelper");
		final TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		final RcPersClient rcpersClient = getBean(RcPersClient.class, "rcpersClient");
		final NonHabitantIndex nonHabitantIndex = getBean(NonHabitantIndex.class, "nonHabitantIndex");
		final ValidationInterceptor validationInterceptor = getBean(ValidationInterceptor.class, "validationInterceptor");
		final AssujettissementService assujettissementService = getBean(AssujettissementService.class, "assujettissementService");
		final FusionCommunesProvider fusionCommunesProvider = getBean(FusionCommunesProvider.class, "fusionCommunesProvider");
		final FractionsCommuneProvider fractionsCommuneProvider = getBean(FractionsCommuneProvider.class, "fractionsCommuneProvider");
		final DatesParticulieres datesParticulieres = getBean(DatesParticulieres.class, "datesParticulieres");
		final PeriodeImpositionService periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");
		final ParametreAppService parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		final DoublonProvider doublonProvider = getBean(DoublonProvider.class, "doublonProvider");

		final ActivityManager activityManager = entreprise -> INACTIVE_ENTREPRISE_ID != entreprise.getId();         // tout le monde est actif dans ces tests, sauf la 1832

		grapheMigrator = new GrapheMigrator();
		grapheMigrator.setEntrepriseMigrator(new EntrepriseMigrator(uniregStore, activityManager, infraService, bouclementService, assujettissementService, rcEntAdapter, adresseHelper,
		                                                            fusionCommunesProvider, fractionsCommuneProvider, datesParticulieres, periodeImpositionService, parametreAppService, doublonProvider));
		grapheMigrator.setEtablissementMigrator(new EtablissementMigrator(uniregStore, activityManager, infraService, rcEntAdapter, adresseHelper, fusionCommunesProvider, fractionsCommuneProvider, datesParticulieres));
		grapheMigrator.setIndividuMigrator(new IndividuMigrator(uniregStore, activityManager, infraService, tiersDAO, rcpersClient, nonHabitantIndex, adresseHelper, fusionCommunesProvider, fractionsCommuneProvider, datesParticulieres));
		grapheMigrator.setUniregStore(uniregStore);
		grapheMigrator.setUniregTransactionManager(getUniregTransactionManager());
		grapheMigrator.setValidationInterceptor(validationInterceptor);
		grapheMigrator.afterPropertiesSet();

		validationService = getBean(ValidationService.class, "validationService");
	}

	/**
	 * Utile dans les cas où aucune donnée d'exercice commercial n'existe dans RegPM, l'exercice commercial
	 * courant est alors supposé aller jusqu'à la fin de l'année...
	 * @return la date de fin de l'année en cours
	 */
	private static RegDate getFinAnneeEnCours() {
		return RegDate.get(RegDate.get().year(), 12, 31);
	}

	/**
	 * Ce remplissage est particulièrement important dès qu'on parle de validation (si le rapport n'est pas mis dans
	 * la collection ad'hoc des rapports entre tiers, les validations de cette collection seront forcément biaisés)
	 */
	@Test
	public void testRemplissageRapportsEntreTiersDansTransaction() throws Throwable {

		final long idEntrepriseMandante = 42L;
		final long idEntrepriseMandataire = 131L;
		final long idEtablissementMandataire = 3562L;
		final RegpmEntreprise mandant = EntrepriseMigratorTest.buildEntreprise(idEntrepriseMandante);

		final RegpmEntreprise entrepriseMandataire = EntrepriseMigratorTest.buildEntreprise(idEntrepriseMandataire);
		final RegpmEtablissement mandataire = EtablissementMigratorTest.buildEtablissement(idEtablissementMandataire, entrepriseMandataire);
		EntrepriseMigratorTest.addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, null, RegDate.get(2000, 1, 1), RegDate.get(2008, 12, 31));
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
			grapheMigrator.migrate(graphe);
			Assert.fail("On a lancé une erreur dès que l'entreprise mandante a des liens... aucun n'a donc été créé ?");
		}
		catch (MigrationException me) {
			try {
				throw me.getCause();
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
					Assert.assertEquals("Sujet : Mandat (01.01.2000 - 31.12.2008)", msg.getMessage());
				}
				{
					final ValidationMessage msg = errors.get(2);
					Assert.assertEquals("Sujet : Mandat (01.01.2010 - ?)", msg.getMessage());
				}
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
		EntrepriseMigratorTest.addRaisonSociale(mandant, RegDate.get(1995, 1, 1), "Je suis", "le mandant", null, true);
		EntrepriseMigratorTest.addFormeJuridique(mandant, RegDate.get(1995, 1, 1), EntrepriseMigratorTest.createTypeFormeJuridique("ASS", RegpmCategoriePersonneMorale.APM));

		final RegpmEntreprise entrepriseMandataire = EntrepriseMigratorTest.buildEntreprise(idEntrepriseMandataire);
		EntrepriseMigratorTest.addRaisonSociale(entrepriseMandataire, RegDate.get(1990, 1, 1), "Je suis", "le mandataire", null, true);
		EntrepriseMigratorTest.addFormeJuridique(entrepriseMandataire, RegDate.get(1990, 1, 1), EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addForPrincipalEtranger(entrepriseMandataire, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, MockPays.RoyaumeUni.getNoOFS());
		EntrepriseMigratorTest.addSiegeEtranger(entrepriseMandataire, RegDate.get(1990, 2, 2), MockPays.RoyaumeUni.getNoOFS());

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
		final MutableLong noContribuableEtablissementSecondaireMandataire = new MutableLong();

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
			final List<Pair<Etablissement, ActiviteEconomique>> etablissementsTries = etablissements.stream()
					.map(etb -> Pair.of(etb, AbstractEntityMigratorTest.getRapportsObjets(etb, ActiviteEconomique.class)))
					.peek(pair -> Assert.assertEquals(1, pair.getRight().size()))
					.map(pair -> Pair.of(pair.getLeft(), pair.getRight().iterator().next()))
					.sorted(Comparator.comparing(pair -> pair.getRight().isPrincipal()))
					.collect(Collectors.toList());

			// établissement secondaire du mandataire
			{
				final Pair<Etablissement, ActiviteEconomique> pair = etablissementsTries.get(0);
				final Etablissement etb = pair.getLeft();
				Assert.assertEquals(0, etb.getRapportsSujet().size());

				final ActiviteEconomique activite = pair.getRight();
				Assert.assertNotNull(activite);
				Assert.assertFalse(activite.isPrincipal());

				noContribuableEtablissementSecondaireMandataire.setValue(etb.getNumero());

				final Map<TypeRapportEntreTiers, List<RapportEntreTiers>> rapportsObjetMap = etb.getRapportsObjet().stream()
						.collect(Collectors.toMap(RapportEntreTiers::getType,
						                          Collections::singletonList,
						                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).sorted(Comparator.comparing(RapportEntreTiers::getDateDebut)).collect(Collectors.toList())));
				Assert.assertEquals(2, rapportsObjetMap.size());

				// mandats
				{
					final List<RapportEntreTiers> rapports = rapportsObjetMap.get(TypeRapportEntreTiers.MANDAT);
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());        // le mandat fermé en 2006 a été ignoré car... fermé en 2006

					{
						final RapportEntreTiers ret = rapports.get(0);
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
				final Pair<Etablissement, ActiviteEconomique> pair = etablissementsTries.get(1);
				final Etablissement etb = pair.getLeft();
				Assert.assertEquals(0, etb.getRapportsSujet().size());

				final ActiviteEconomique activite = pair.getRight();
				Assert.assertNotNull(activite);
				Assert.assertTrue(activite.isPrincipal());

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
						Assert.assertEquals(RegDate.get(1990, 2, 2), ret.getDateDebut());       // le lien d'activité économique est basé sur les dates du siège
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
				Assert.assertEquals(RegDate.get(1990, 2, 2), domicile.getDateDebut());      // l'établissement principal est basé sur les dates du siège
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
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ff.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());

					final ForFiscalPrincipalPM ffp = (ForFiscalPrincipalPM) ff;
					Assert.assertNull(ffp.getMotifOuverture());         // ouverture du premier for qui se trouve être à l'étranger
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
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), ff.getNumeroOfsAutoriteFiscale());

					final ForFiscalSecondaire ffs = (ForFiscalSecondaire) ff;
					Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffs.getMotifOuverture());
					Assert.assertNull(ffs.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, ffs.getMotifRattachement());
				}
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.ADRESSES,
		                               LogCategory.FORS,
		                               LogCategory.ASSUJETTISSEMENTS,
		                               LogCategory.ETABLISSEMENTS,
		                               LogCategory.RAPPORTS_ENTRE_TIERS,
		                               LogCategory.DONNEES_CIVILES_REGPM),
		                    messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(12, msgs.size());
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;" + idEtablissementMandataire + ";" + noContribuableEtablissementSecondaireMandataire.longValue() + ";;;" + idEntrepriseMandataire + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaireMandataire.longValue()) + ".", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni for principal.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Pas de siège associé, pas d'établissement principal créé.", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Le mandat 1 de l'entreprise mandante " + idEntrepriseMandante + " vers l'entité mandataire " + idEtablissementMandataire + " de type ETABLISSEMENT est ignoré car sa date de résiliation est antérieure au 01.01.2008 (31.12.2006).", msgs.get(5));
			Assert.assertEquals("INFO;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntrepriseMandante) + ".", msgs.get(6));
			Assert.assertEquals("WARN;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(7));
			Assert.assertEquals("WARN;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(8));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalMandataire.longValue()) + " d'après le siège 1.", msgs.get(9));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalMandataire.longValue()) + " : [02.02.1990 -> ?] sur PAYS_HS/8215.", msgs.get(10));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntrepriseMandataire) + ".", msgs.get(11));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("WARN;" + idEntrepriseMandataire + ";Active;;;" + idEtablissementMandataire + ";" + noContribuableEtablissementSecondaireMandataire.longValue() + ";;;" + idEntrepriseMandataire + ";;;;;;;;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(3, msgs.size());
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;For principal PAYS_HS/8215 [01.01.1990 -> ?] généré.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;For secondaire 'activité' [01.01.1995 -> ?] ajouté sur la commune 5518.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntrepriseMandataire + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5518 (maintenant : [01.01.1995 -> ?]).", msgs.get(2));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + idEntrepriseMandataire + ";Active;;;Apparition d'assujettissement sur une entreprise auparavant complètement non-assujettie : [01.01.1995 -> ?].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ETABLISSEMENTS);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("WARN;" + idEtablissementMandataire + ";" + noContribuableEtablissementSecondaireMandataire.longValue() + ";;;" + idEntrepriseMandataire + ";;Le mandat 1 de l'entreprise mandante " + idEntrepriseMandante + " vers l'entité mandataire " + idEtablissementMandataire + " de type ETABLISSEMENT est ignoré car sa date de résiliation est antérieure au 01.01.2008 (31.12.2006).", msgs.get(0));
			Assert.assertEquals("INFO;" + idEtablissementMandataire + ";" + noContribuableEtablissementSecondaireMandataire.longValue() + ";;;" + idEntrepriseMandataire + ";;Domicile : [01.01.1995 -> ?] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntrepriseMandante + ";Active;;;;;;;;;Données 'civiles' migrées : sur la période [01.01.1995 -> ?], raison sociale (Je suis le mandant) et forme juridique (ASSOCIATION).", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;Données 'civiles' migrées : sur la période [01.01.1990 -> ?], raison sociale (Je suis le mandataire) et forme juridique (SA).", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertEquals(3, msgs.size());
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1995-01-01;;;" + idEtablissementMandataire + ";;" + noContribuableEtablissementSecondaireMandataire.longValue() + ";" + idEntrepriseMandataire + ";;;" + idEntrepriseMandataire + ";", msgs.get(0));
			Assert.assertEquals("INFO;MANDANT_MANDATAIRE;2010-01-01;;" + idEntrepriseMandante + ";;;" + idEntrepriseMandante + ";;" + idEtablissementMandataire + ";;" + noContribuableEtablissementSecondaireMandataire.longValue() + ";", msgs.get(1));
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1990-02-02;;;;;" + noContribuableEtablissementPrincipalMandataire.longValue() + ";" + idEntrepriseMandataire + ";;;" + idEntrepriseMandataire + ";", msgs.get(2));
		}
	}

	@Test
	public void testMigrationEtablissementsMultiples() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		entreprise.setEnseigne("Smart zoo");
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BALE);       // un for principal de base
		EntrepriseMigratorTest.addSiegeSuisse(entreprise, RegDate.get(1990, 1, 1), Commune.BALE);       // un siège

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
		final MutableLong noContribuableEtablissementSecondaire1 = new MutableLong();
		final MutableLong noContribuableEtablissementSecondaire2 = new MutableLong();

		// vérification du résultat
		doInUniregTransaction(true, status -> {

			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);       // c'est le même identifiant dans RegPM et dans Unireg
			Assert.assertNotNull(e);

			// on vérifie d'abord qu'il y a bien trois établissements liés (2 secondaires + 1 principal)
			final Set<RapportEntreTiers> rapports = e.getRapportsSujet();
			Assert.assertNotNull(rapports);
			Assert.assertEquals(3, rapports.size());
			rapports.stream().filter(r -> !(r instanceof ActiviteEconomique)).findAny().ifPresent(
					r -> Assert.fail("Rapport " + r + " trouvé là où seuls des rapports d'activité économiques étaient attendus"));
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

				final Collection<ActiviteEconomique> rapportsActiviteEconomique = AbstractEntityMigratorTest.getRapportsObjets(etb, ActiviteEconomique.class);
				Assert.assertNotNull(rapportsActiviteEconomique);
				Assert.assertEquals(1, rapportsActiviteEconomique.size());
				Assert.assertTrue(rapportsActiviteEconomique.iterator().next().isPrincipal());

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

				final Collection<ActiviteEconomique> rapportsActiviteEconomique = AbstractEntityMigratorTest.getRapportsObjets(etb, ActiviteEconomique.class);
				Assert.assertNotNull(rapportsActiviteEconomique);
				Assert.assertEquals(1, rapportsActiviteEconomique.size());
				Assert.assertFalse(rapportsActiviteEconomique.iterator().next().isPrincipal());

				// mémorisation de la valeur
				noContribuableEtablissementSecondaire1.setValue(etb.getNumero());

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

				final Collection<ActiviteEconomique> rapportsActiviteEconomique = AbstractEntityMigratorTest.getRapportsObjets(etb, ActiviteEconomique.class);
				Assert.assertNotNull(rapportsActiviteEconomique);
				Assert.assertEquals(1, rapportsActiviteEconomique.size());
				Assert.assertFalse(rapportsActiviteEconomique.iterator().next().isPrincipal());

				// mémorisation de la valeur
				noContribuableEtablissementSecondaire2.setValue(etb.getNumero());

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
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.ADRESSES, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.ETABLISSEMENTS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement1 + ";" + noContribuableEtablissementSecondaire1.longValue() + ";;;" + idEntreprise + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire1.longValue()) + ".", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement2 + ";" + noContribuableEtablissementSecondaire2.longValue() + ";;;" + idEntreprise + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire2.longValue()) + ".", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " d'après le siège 1.", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.01.1990 -> ?] sur COMMUNE_HC/2701.", msgs.get(
					5));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(6));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;" + idEtablissement1 + ";" + noContribuableEtablissementSecondaire1.longValue() + ";;;" + idEntreprise + ";;;;;;;;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;" + idEtablissement2 + ";" + noContribuableEtablissementSecondaire2.longValue() + ";;;" + idEntreprise + ";;;;;;;;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_HC/2701 [01.01.1990 -> ?] généré.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [22.03.2004 -> 25.11.2010] ajouté sur la commune 5518.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [14.07.2002 -> 31.10.2006] ajouté sur la commune 5586.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [12.05.1999 -> 26.01.2003] ajouté sur la commune 5642.", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5518 (maintenant : [22.03.2004 -> 25.11.2010]).", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5586 (maintenant : [14.07.2002 -> 31.10.2006]).", msgs.get(5));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5642 (maintenant : [12.05.1999 -> 26.01.2003]).", msgs.get(6));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertEquals(1, msgs.size());

			// on a la date de la fin d'année courante car il s'agit d'un assujetti hors-Canton qui ferme tous ses fors vaudois,
			// donc l'assujettissement va jusqu'à la fin de l'exercice commercial actif au moment de la fermeture du dernier for vaudois
			// (et en l'absence de données d'exercices commerciaux, on suppose l'existance d'un seul exercice commercial qui court depuis
			// l'ouverture du premier for de l'entreprise jusqu'à la fin l'année civile en cours)
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;Apparition d'assujettissement sur une entreprise auparavant complètement non-assujettie : [01.01.1990 -> " + RegDateHelper.dateToDisplayString(getFinAnneeEnCours()) + "].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ETABLISSEMENTS);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("INFO;" + idEtablissement1 + ";" + noContribuableEtablissementSecondaire1.longValue() + ";;;" + idEntreprise + ";;Domicile : [12.05.1999 -> 26.01.2003] sur COMMUNE_OU_FRACTION_VD/5642.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEtablissement1 + ";" + noContribuableEtablissementSecondaire1.longValue() + ";;;" + idEntreprise + ";;Domicile : [27.01.2003 -> 31.10.2006] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEtablissement2 + ";" + noContribuableEtablissementSecondaire2.longValue() + ";;;" + idEntreprise + ";;Domicile : [14.07.2002 -> 21.03.2004] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEtablissement2 + ";" + noContribuableEtablissementSecondaire2.longValue() + ";;;" + idEntreprise + ";;Domicile : [22.03.2004 -> 25.11.2010] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(3));
		}
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertEquals(3, msgs.size());
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1999-05-12;2006-10-31;;" + idEtablissement1 + ";;" + noContribuableEtablissementSecondaire1.longValue() + ";" + idEntreprise + ";;;" + idEntreprise + ";", msgs.get(0));
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;2002-07-14;2010-11-25;;" + idEtablissement2 + ";;" + noContribuableEtablissementSecondaire2.longValue() + ";" + idEntreprise + ";;;" + idEntreprise + ";", msgs.get(1));
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1990-01-01;;;;;" + noContribuableEtablissementPrincipalCree.longValue() + ";" + idEntreprise+ ";;;" + idEntreprise + ";", msgs.get(2));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msgs.get(0));
		}
	}

	@Test
	public void testCalculForsSecondairesActiviteEtImmeuble() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BALE);       // un for principal de base
		EntrepriseMigratorTest.addSiegeSuisse(entreprise, RegDate.get(1990, 1, 1), Commune.BALE);       // un siège

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
		final MutableLong noContribuableEtablissementSecondaire1 = new MutableLong();
		final MutableLong noContribuableEtablissementSecondaire2 = new MutableLong();

		// vérification du résultat
		doInUniregTransaction(true, status -> {

			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);       // c'est le même identifiant dans RegPM et dans Unireg
			Assert.assertNotNull(e);

			// pour tester la cohérence avec le message de suivi par la suite
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			noContribuableEtablissementPrincipalCree.setValue(etablissements.stream()
					                                                  .map(etb -> AbstractEntityMigratorTest.getRapportsObjets(etb, ActiviteEconomique.class))
					                                                  .flatMap(Collection::stream)
					                                                  .filter(ActiviteEconomique::isPrincipal)
					                                                  .findAny()
					                                                  .map(ActiviteEconomique::getObjetId)
					                                                  .orElseThrow(() -> new IllegalStateException("Aucun établissement principal trouvé!")));
			noContribuableEtablissementSecondaire1.setValue(etablissements.stream()
					                                                .filter(etb -> etb.getEnseigne().equals("Le chat qui fume"))
					                                                .findAny()
					                                                .map(Etablissement::getNumero)
					                                                .orElseThrow(() -> new IllegalStateException("Pas d'établissement avec la bonne enseigne créé ?")));
			noContribuableEtablissementSecondaire2.setValue(etablissements.stream()
					                                                .filter(etb -> etb.getEnseigne().equals("Le chien qui pête"))
					                                                .findAny()
					                                                .map(Etablissement::getNumero)
					                                                .orElseThrow(() -> new IllegalStateException("Pas d'établissement avec la bonne enseigne créé ?")));

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
				Assert.assertNull(ffp.getMotifOuverture());     // ouverture du premier for qui se trouve être hors canton
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
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.ADRESSES, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.ETABLISSEMENTS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement1 + ";" + noContribuableEtablissementSecondaire1.longValue() + ";;;" + idEntreprise + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire1.longValue()) + ".", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement2 + ";" + noContribuableEtablissementSecondaire2.longValue() + ";;;" + idEntreprise + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire2.longValue()) + ".", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " d'après le siège 1.", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.01.1990 -> ?] sur COMMUNE_HC/2701.", msgs.get(5));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(6));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;" + idEtablissement1 + ";" + noContribuableEtablissementSecondaire1.longValue() + ";;;" + idEntreprise + ";;;;;;;;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;" + idEtablissement2 + ";" + noContribuableEtablissementSecondaire2.longValue() + ";;;" + idEntreprise + ";;;;;;;;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(8, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_HC/2701 [01.01.1990 -> ?] généré.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [22.03.2004 -> 25.11.2010] ajouté sur la commune 5518.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [14.07.2002 -> 31.10.2006] ajouté sur la commune 5586.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [12.05.1999 -> 26.01.2003] ajouté sur la commune 5642.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'immeuble' [01.07.2004 -> 31.12.2010] ajouté sur la commune 5518.", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5518 (maintenant : [22.03.2004 -> 31.12.2010]).", msgs.get(5));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5586 (maintenant : [14.07.2002 -> 31.10.2006]).", msgs.get(6));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5642 (maintenant : [12.05.1999 -> 26.01.2003]).", msgs.get(7));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertEquals(1, msgs.size());

			// on a la fin de l'année en cours car il s'agit d'un assujetti hors-Canton qui ferme tous ses fors vaudois,
			// donc l'assujettissement va jusqu'à la fin de l'exercice commercial actif au moment de la fermeture du dernier for vaudois
			// (et en l'absence de données d'exercices commerciaux, on suppose l'existance d'un seul exercice commercial qui court depuis
			// l'ouverture du premier for de l'entreprise jusqu'à la fin de l'année en cours)
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;Apparition d'assujettissement sur une entreprise auparavant complètement non-assujettie : [01.01.1990 -> " + RegDateHelper.dateToDisplayString(getFinAnneeEnCours()) + "].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ETABLISSEMENTS);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("INFO;" + idEtablissement1 + ";" + noContribuableEtablissementSecondaire1.longValue() + ";;;" + idEntreprise + ";;Domicile : [12.05.1999 -> 26.01.2003] sur COMMUNE_OU_FRACTION_VD/5642.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEtablissement1 + ";" + noContribuableEtablissementSecondaire1.longValue() + ";;;" + idEntreprise + ";;Domicile : [27.01.2003 -> 31.10.2006] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEtablissement2 + ";" + noContribuableEtablissementSecondaire2.longValue() + ";;;" + idEntreprise + ";;Domicile : [14.07.2002 -> 21.03.2004] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEtablissement2 + ";" + noContribuableEtablissementSecondaire2.longValue() + ";;;" + idEntreprise + ";;Domicile : [22.03.2004 -> 25.11.2010] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(3));
		}
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertEquals(3, msgs.size());
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1999-05-12;2006-10-31;;" + idEtablissement1 + ";;" + noContribuableEtablissementSecondaire1.longValue() + ";" + idEntreprise + ";;;" + idEntreprise + ";", msgs.get(0));
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;2002-07-14;2010-11-25;;" + idEtablissement2 + ";;" + noContribuableEtablissementSecondaire2.longValue() + ";" + idEntreprise + ";;;" + idEntreprise + ";", msgs.get(1));
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1990-01-01;;;;;" + noContribuableEtablissementPrincipalCree.longValue() + ";" + idEntreprise + ";;;" + idEntreprise + ";", msgs.get(2));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msgs.get(0));
		}
	}

	@Test
	public void testForSecondaireImmeubleSurFraction() throws Exception {
		final long noEntreprise = 1234L;
		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegDate debut = RegDate.get(2005, 5, 7);
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
		final RegDate debut = RegDate.get(2005, 5, 7);
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

	@Test
	public void testCouvertureForsSecondairesParForsPrincipauxAucunForPrincipalAuDepart() throws Exception {

		final long noEntreprise = 43782L;
		final RegDate dateDebutForPrincipal = RegDate.get(1987, 5, 1);
		final RegDate dateAchatImmeuble1 = dateDebutForPrincipal.addYears(-5);   // avant le for principal !!
		final RegDate dateAchatImmeuble2 = dateDebutForPrincipal.addMonths(-23); // avant le for principal !!

		// pas de for principal...
		final RegpmEntreprise regpm = EntrepriseMigratorTest.buildEntreprise(noEntreprise);

		final RegpmImmeuble immeuble1 = EntrepriseMigratorTest.createImmeuble(Commune.MORGES);
		EntrepriseMigratorTest.addRattachementProprietaire(regpm, dateAchatImmeuble1, null, immeuble1);

		final RegpmImmeuble immeuble2 = EntrepriseMigratorTest.createImmeuble(Commune.LAUSANNE);
		EntrepriseMigratorTest.addRattachementProprietaire(regpm, dateAchatImmeuble2, null, immeuble2);

		final Graphe graphe = new MockGraphe(Collections.singletonList(regpm),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// vérification du contenu de la base -> une nouvelle regpm
		doInUniregTransaction(true, status -> {
			final List<Entreprise> entreprises = uniregStore.getEntitiesFromDb(Entreprise.class, null);
			Assert.assertNotNull(entreprises);
			Assert.assertEquals(1, entreprises.size());

			final Entreprise entreprise = entreprises.get(0);
			Assert.assertNotNull(entreprise);

			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(0, etablissements.size());      // pas de for principal -> pas d'établissement principal

			final ForsParType fpt = entreprise.getForsParType(true);

			// fors secondaires
			{
				Assert.assertEquals(2, fpt.secondaires.size());

				// le plus ancien d'abord
				{
					final ForFiscalSecondaire ffs = fpt.secondaires.get(0);
					Assert.assertNotNull(ffs);
					Assert.assertEquals(dateAchatImmeuble1, ffs.getDateDebut());
					Assert.assertNull(ffs.getDateFin());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
					Assert.assertEquals(Commune.MORGES.getNoOfs(), ffs.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffs.getMotifRattachement());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffs.getGenreImpot());
					Assert.assertFalse(ffs.isAnnule());
				}
				// l'autre ensuite
				{
					final ForFiscalSecondaire ffs = fpt.secondaires.get(1);
					Assert.assertNotNull(ffs);
					Assert.assertEquals(dateAchatImmeuble2, ffs.getDateDebut());
					Assert.assertNull(ffs.getDateFin());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
					Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffs.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffs.getMotifRattachement());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffs.getGenreImpot());
					Assert.assertFalse(ffs.isAnnule());
				}
			}

			// fors principaux
			{
				Assert.assertEquals(1, fpt.principauxPM.size());

				// un seul for -> le bouche-trou en pays inconnu
				{
					final ForFiscalPrincipalPM ffp = fpt.principauxPM.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertEquals(dateAchatImmeuble1, ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertNull(ffp.getMotifOuverture());         // le premier for principal qui se trouve être hors Suisse
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) ServiceInfrastructureService.noPaysInconnu, ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
					Assert.assertFalse(ffp.isAnnule());
				}
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni for principal.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé, pas d'établissement principal créé.", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(4));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For secondaire 'immeuble' [01.06.1985 -> ?] ajouté sur la commune 5586.", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For secondaire 'immeuble' [01.05.1982 -> ?] ajouté sur la commune 5642.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5586 (maintenant : [01.06.1985 -> ?]).", msgs.get(2));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5642 (maintenant : [01.05.1982 -> ?]).", msgs.get(3));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Création d'un for principal 'bouche-trou' [01.05.1982 -> ?] pour couvrir les fors secondaires.", msgs.get(4));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;Apparition d'assujettissement sur une entreprise auparavant complètement non-assujettie : [01.05.1982 -> ?].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msgs.get(0));
		}
	}

	@Test
	public void testCouvertureForsSecondairesParForsPrincipaux() throws Exception {

		final long noEntreprise = 43782L;
		final RegDate dateDebutForPrincipal = RegDate.get(1987, 5, 1);
		final RegDate dateAchatImmeuble1 = dateDebutForPrincipal.addYears(-5);   // avant le for principal !!
		final RegDate dateAchatImmeuble2 = dateDebutForPrincipal.addMonths(-23); // avant le for principal !!

		final RegpmEntreprise regpm = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(regpm, dateDebutForPrincipal, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addSiegeSuisse(regpm, dateDebutForPrincipal, Commune.ECHALLENS);

		final RegpmImmeuble immeuble1 = EntrepriseMigratorTest.createImmeuble(Commune.MORGES);
		EntrepriseMigratorTest.addRattachementProprietaire(regpm, dateAchatImmeuble1, null, immeuble1);

		final RegpmImmeuble immeuble2 = EntrepriseMigratorTest.createImmeuble(Commune.LAUSANNE);
		EntrepriseMigratorTest.addRattachementProprietaire(regpm, dateAchatImmeuble2, null, immeuble2);

		final Graphe graphe = new MockGraphe(Collections.singletonList(regpm),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// récupération du numéro de contribuable de l'établissement principal (pour le contrôle des logs)
		final MutableLong noContribuableEtablissementPrincipalCree = new MutableLong();

		// vérification du contenu de la base -> une nouvelle regpm
		doInUniregTransaction(true, status -> {
			final List<Entreprise> entreprises = uniregStore.getEntitiesFromDb(Entreprise.class, null);
			Assert.assertNotNull(entreprises);
			Assert.assertEquals(1, entreprises.size());

			final Entreprise entreprise = entreprises.get(0);
			Assert.assertNotNull(entreprise);

			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());
			final Etablissement etbPrincipal = etablissements.get(0);
			Assert.assertNotNull(etbPrincipal);

			final Collection<ActiviteEconomique> rapportsActiviteEconomique = AbstractEntityMigratorTest.getRapportsObjets(etbPrincipal, ActiviteEconomique.class);
			Assert.assertNotNull(rapportsActiviteEconomique);
			Assert.assertEquals(1, rapportsActiviteEconomique.size());
			Assert.assertTrue(rapportsActiviteEconomique.iterator().next().isPrincipal());

			noContribuableEtablissementPrincipalCree.setValue(etbPrincipal.getNumero());

			final ForsParType fpt = entreprise.getForsParType(true);

			// fors secondaires
			{
				Assert.assertEquals(2, fpt.secondaires.size());

				// le plus ancien d'abord
				{
					final ForFiscalSecondaire ffs = fpt.secondaires.get(0);
					Assert.assertNotNull(ffs);
					Assert.assertEquals(dateAchatImmeuble1, ffs.getDateDebut());
					Assert.assertNull(ffs.getDateFin());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
					Assert.assertEquals(Commune.MORGES.getNoOfs(), ffs.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffs.getMotifRattachement());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffs.getGenreImpot());
					Assert.assertFalse(ffs.isAnnule());
				}
				// l'autre ensuite
				{
					final ForFiscalSecondaire ffs = fpt.secondaires.get(1);
					Assert.assertNotNull(ffs);
					Assert.assertEquals(dateAchatImmeuble2, ffs.getDateDebut());
					Assert.assertNull(ffs.getDateFin());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
					Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffs.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffs.getMotifRattachement());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffs.getGenreImpot());
					Assert.assertFalse(ffs.isAnnule());
				}
			}

			// fors principaux
			{
				Assert.assertEquals(1, fpt.principauxPM.size());

				// le pré-existant dans Regpm, dont la date de début a cependant été modifiée
				{
					final ForFiscalPrincipalPM ffp = fpt.principauxPM.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertEquals(RegDateHelper.minimum(dateAchatImmeuble1, dateAchatImmeuble2, NullDateBehavior.LATEST), ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
					Assert.assertFalse(ffp.isAnnule());
				}
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " d'après le siège 1.", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.05.1987 -> ?] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(4));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(6, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5518 [01.05.1987 -> ?] généré.", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For secondaire 'immeuble' [01.06.1985 -> ?] ajouté sur la commune 5586.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For secondaire 'immeuble' [01.05.1982 -> ?] ajouté sur la commune 5642.", msgs.get(2));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5586 (maintenant : [01.06.1985 -> ?]).", msgs.get(3));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5642 (maintenant : [01.05.1982 -> ?]).", msgs.get(4));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;La date de début du for fiscal principal [01.05.1987 -> ?] est adaptée (-> 01.05.1982) pour couvrir les fors secondaires.", msgs.get(5));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;Apparition d'assujettissement sur une entreprise auparavant complètement non-assujettie : [01.05.1982 -> ?].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1987-05-01;;;;;" + noContribuableEtablissementPrincipalCree.longValue() + ";" + noEntreprise + ";;;" + noEntreprise + ";", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msgs.get(0));
		}
	}

	/**
	 * Cas vu dans la base de RegPM où le for secondaire non-couvert n'est pas juste en bordure des fors principaux
	 * (= par exemple, revente de l'immeuble alors que le for principal n'existe toujours pas avant potentiellement plusieurs années)
	 */
	@Test
	public void testCouvertureForsSecondairesParForsPrincipauxLoinDeForsPrincipaux() throws Exception {

		final long noEntreprise = 43782L;
		final RegDate dateDebutForPrincipal = RegDate.get(1987, 5, 1);
		final RegDate dateAchatImmeuble = dateDebutForPrincipal.addYears(-5);      // avant le for principal !!
		final RegDate dateVenteImmeuble = dateDebutForPrincipal.addYears(-1);       // ça aussi, avant le for principal !!

		final RegpmEntreprise regpm = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(regpm, dateDebutForPrincipal, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addSiegeSuisse(regpm, dateDebutForPrincipal, Commune.ECHALLENS);

		final RegpmImmeuble immeuble1 = EntrepriseMigratorTest.createImmeuble(Commune.MORGES);
		EntrepriseMigratorTest.addRattachementProprietaire(regpm, dateAchatImmeuble, dateVenteImmeuble, immeuble1);

		final Graphe graphe = new MockGraphe(Collections.singletonList(regpm),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// récupération du numéro de contribuable de l'établissement principal (pour le contrôle des logs)
		final MutableLong noContribuableEtablissementPrincipalCree = new MutableLong();

		// vérification du contenu de la base -> une nouvelle regpm
		doInUniregTransaction(true, status -> {
			final List<Entreprise> entreprises = uniregStore.getEntitiesFromDb(Entreprise.class, null);
			Assert.assertNotNull(entreprises);
			Assert.assertEquals(1, entreprises.size());

			final Entreprise entreprise = entreprises.get(0);
			Assert.assertNotNull(entreprise);

			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());
			final Etablissement etbPrincipal = etablissements.get(0);
			Assert.assertNotNull(etbPrincipal);

			final Collection<ActiviteEconomique> rapportsActiviteEconomique = AbstractEntityMigratorTest.getRapportsObjets(etbPrincipal, ActiviteEconomique.class);
			Assert.assertNotNull(rapportsActiviteEconomique);
			Assert.assertEquals(1, rapportsActiviteEconomique.size());
			Assert.assertTrue(rapportsActiviteEconomique.iterator().next().isPrincipal());

			noContribuableEtablissementPrincipalCree.setValue(etbPrincipal.getNumero());

			final ForsParType fpt = entreprise.getForsParType(true);

			// fors secondaires
			{
				Assert.assertEquals(1, fpt.secondaires.size());
				{
					final ForFiscalSecondaire ffs = fpt.secondaires.get(0);
					Assert.assertNotNull(ffs);
					Assert.assertEquals(dateAchatImmeuble, ffs.getDateDebut());
					Assert.assertEquals(dateVenteImmeuble, ffs.getDateFin());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
					Assert.assertEquals(Commune.MORGES.getNoOfs(), ffs.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffs.getMotifRattachement());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffs.getGenreImpot());
					Assert.assertFalse(ffs.isAnnule());
				}
			}

			// fors principaux
			{
				Assert.assertEquals(1, fpt.principauxPM.size());

				// le pré-existant dans Regpm, dont la date de début a cependant été modifiée
				{
					final ForFiscalPrincipalPM ffp = fpt.principauxPM.get(0);
					Assert.assertNotNull(ffp);
					Assert.assertEquals(dateAchatImmeuble, ffp.getDateDebut());
					Assert.assertNull(ffp.getDateFin());
					Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
					Assert.assertFalse(ffp.isAnnule());
				}
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " d'après le siège 1.", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.05.1987 -> ?] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(4));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5518 [01.05.1987 -> ?] généré.", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For secondaire 'immeuble' [01.05.1982 -> 01.05.1986] ajouté sur la commune 5642.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5642 (maintenant : [01.05.1982 -> 01.05.1986]).", msgs.get(2));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;La date de début du for fiscal principal [01.05.1987 -> ?] est adaptée (-> 01.05.1982) pour couvrir les fors secondaires.", msgs.get(3));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;Apparition d'assujettissement sur une entreprise auparavant complètement non-assujettie : [01.05.1982 -> ?].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1987-05-01;;;;;" + noContribuableEtablissementPrincipalCree.longValue() + ";" + noEntreprise + ";;;" + noEntreprise + ";", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msgs.get(0));
		}
	}

	@Test
	public void testAucunForSecondaire() throws Exception {

		final long noEntreprise = 43782L;
		final RegDate dateDebutForPrincipal = RegDate.get(1987, 5, 1);

		final RegpmEntreprise regpm = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		regpm.setDateInscriptionRC(dateDebutForPrincipal);
		EntrepriseMigratorTest.addForPrincipalSuisse(regpm, dateDebutForPrincipal, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addSiegeSuisse(regpm, dateDebutForPrincipal, Commune.ECHALLENS);

		final Graphe graphe = new MockGraphe(Collections.singletonList(regpm),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// récupération du numéro de contribuable de l'établissement principal (pour le contrôle des logs)
		final MutableLong noContribuableEtablissementPrincipalCree = new MutableLong();

		// vérification du contenu de la base -> une nouvelle regpm
		doInUniregTransaction(true, status -> {
			final List<Entreprise> entreprises = uniregStore.getEntitiesFromDb(Entreprise.class, null);
			Assert.assertNotNull(entreprises);
			Assert.assertEquals(1, entreprises.size());

			final Entreprise entreprise = entreprises.get(0);
			Assert.assertNotNull(entreprise);

			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());
			final Etablissement etbPrincipal = etablissements.get(0);
			Assert.assertNotNull(etbPrincipal);

			final Collection<ActiviteEconomique> rapportsActiviteEconomique = AbstractEntityMigratorTest.getRapportsObjets(etbPrincipal, ActiviteEconomique.class);
			Assert.assertNotNull(rapportsActiviteEconomique);
			Assert.assertEquals(1, rapportsActiviteEconomique.size());
			Assert.assertTrue(rapportsActiviteEconomique.iterator().next().isPrincipal());

			noContribuableEtablissementPrincipalCree.setValue(etbPrincipal.getNumero());

			final ForsParType fpt = entreprise.getForsParType(true);
			Assert.assertEquals(0, fpt.secondaires.size());
			Assert.assertEquals(0, fpt.principauxPP.size());
			Assert.assertEquals(0, fpt.dpis.size());
			Assert.assertEquals(0, fpt.autresImpots.size());
			Assert.assertEquals(0, fpt.autreElementImpot.size());
			Assert.assertEquals(1, fpt.principauxPM.size());

			// for principal
			{
				final ForFiscalPrincipalPM ffp = fpt.principauxPM.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateDebutForPrincipal, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
				Assert.assertFalse(ffp.isAnnule());
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " d'après le siège 1.", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.05.1987 -> ?] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(4));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5518 [01.05.1987 -> ?] généré.", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;Apparition d'assujettissement sur une entreprise auparavant complètement non-assujettie : [01.05.1987 -> ?].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1987-05-01;;;;;" + noContribuableEtablissementPrincipalCree.longValue() + ";" + noEntreprise + ";;;" + noEntreprise + ";", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msgs.get(0));
		}
	}

	@Test
	public void testEtablissementsStablesAvecChevauchement() throws Exception {

		final long idEntreprise = 12442L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		final RegpmEtablissement etablissement = EtablissementMigratorTest.buildEtablissement(5435L, entreprise);
		EtablissementMigratorTest.addEtablissementStable(etablissement, RegDate.get(2005, 3, 12), RegDate.get(2010, 5, 3));       // les 1, 2 et 3 mai 2005 sont en chevauchement entre les deux établissements stables
		EtablissementMigratorTest.addEtablissementStable(etablissement, RegDate.get(2010, 5, 1), null);
		EtablissementMigratorTest.addDomicileEtablissement(etablissement, RegDate.get(2005, 3, 12), Commune.LAUSANNE, false);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// extraction de l'établissement et vérification des données
		doInUniregTransaction(true, status -> {
			final List<Etablissement> etbs = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etbs);
			Assert.assertEquals(1, etbs.size());

			final Etablissement etb = etbs.get(0);
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

			final Set<RapportEntreTiers> rets = etb.getRapportsObjet();
			Assert.assertNotNull(rets);
			Assert.assertEquals(1, rets.size());

			final RapportEntreTiers ret = rets.iterator().next();
			Assert.assertNotNull(ret);
			Assert.assertEquals(RegDate.get(2005, 3, 12), ret.getDateDebut());      // un seul rapport entre tiers "activité" qui reprend toute la période
			Assert.assertNull(ret.getDateFin());
			Assert.assertEquals(TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE, ret.getType());
			Assert.assertFalse(ret.isAnnule());
			Assert.assertEquals((Long) idEntreprise, ret.getSujetId());
		});
	}

	@Test
	public void testImmeubleSurEtablissementStable() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BALE);       // un for principal de base
		EntrepriseMigratorTest.addSiegeSuisse(entreprise, RegDate.get(1990, 1, 1), Commune.BALE);       // un siège

		// un établissement
		final long idEtablissement = 235612L;
		final RegpmEtablissement etablissement = EtablissementMigratorTest.buildEtablissement(idEtablissement, entreprise);
		etablissement.setEnseigne("Le chat qui fume");
		EtablissementMigratorTest.addDomicileEtablissement(etablissement, RegDate.get(1999, 5, 12), Commune.MORGES, false);
		EtablissementMigratorTest.addDomicileEtablissement(etablissement, RegDate.get(2003, 1, 27), Commune.LAUSANNE, false);
		EtablissementMigratorTest.addEtablissementStable(etablissement, RegDate.get(1999, 5, 12), RegDate.get(2006, 10, 31));

		// les immeubles
		final RegpmImmeuble immeuble1 = createImmeuble(Commune.ECHALLENS);
		EtablissementMigratorTest.addRattachementProprietaire(etablissement, RegDate.get(2006, 5, 1), RegDate.get(2010, 12, 31), immeuble1);

		final RegpmImmeuble immeuble2 = createImmeuble(Commune.ECHALLENS);
		final RegpmGroupeProprietaire groupe2 = createGroupeProprietaire("Zoo", RegpmTypeGroupeProprietaire.CONSORTIUM_SOCIETE_SIMPLE, RegDate.get(2000, 1, 1), null);
		EtablissementMigratorTest.addAppartenanceGroupeProprietaire(etablissement, groupe2, RegDate.get(2004, 5, 29), RegDate.get(2009, 12, 21), false);
		EtablissementMigratorTest.addRattachementProprietaire(groupe2, RegDate.get(2004, 7, 1), null, immeuble2);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     Collections.singletonList(etablissement),
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// pour tester la cohérence avec le message de suivi par la suite
		final MutableLong noContribuableEtablissementPrincipalCree = new MutableLong();
		final MutableLong noContribuableEtablissementSecondaire = new MutableLong();

		// vérification du résultat
		doInUniregTransaction(true, status -> {

			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);       // c'est le même identifiant dans RegPM et dans Unireg
			Assert.assertNotNull(e);

			// pour tester la cohérence avec le message de suivi par la suite
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			noContribuableEtablissementPrincipalCree.setValue(etablissements.stream()
					                                                  .map(etb -> AbstractEntityMigratorTest.getRapportsObjets(etb, ActiviteEconomique.class))
					                                                  .flatMap(Collection::stream)
					                                                  .filter(ActiviteEconomique::isPrincipal)
					                                                  .findAny()
					                                                  .map(ActiviteEconomique::getObjetId)
					                                                  .orElseThrow(() -> new IllegalStateException("Aucun établissement principal trouvé!")));
			noContribuableEtablissementSecondaire.setValue(etablissements.stream()
					                                               .filter(etb -> etb.getEnseigne().equals("Le chat qui fume"))
					                                               .findAny()
					                                               .map(Etablissement::getNumero)
					                                               .orElseThrow(() -> new IllegalStateException("Pas d'établissement avec la bonne enseigne créé ?")));

			final List<ForFiscal> fors = e.getForsFiscauxSorted();
			Assert.assertEquals(4, fors.size());

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
				Assert.assertNull(ffp.getMotifOuverture());         // le premier for principal qui se trouve être hors canton
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
				Assert.assertEquals(RegDate.get(2003, 1, 27), ff.getDateDebut());
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

			// for fiscal secondaire (immeuble) à Echallens (transmis depuis l'établissement)
			{
				final ForFiscal ff = fors.get(3);
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
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.ADRESSES, LogCategory.FORS, LogCategory.ETABLISSEMENTS, LogCategory.ASSUJETTISSEMENTS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(6, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire.longValue()) + ".", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " d'après le siège 1.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.01.1990 -> ?] sur COMMUNE_HC/2701.", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(5));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;;;;;;;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_HC/2701 [01.01.1990 -> ?] généré.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [27.01.2003 -> 31.10.2006] ajouté sur la commune 5586.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [12.05.1999 -> 26.01.2003] ajouté sur la commune 5642.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'immeuble' [01.07.2004 -> 31.12.2010] ajouté sur la commune 5518.", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5518 (maintenant : [01.07.2004 -> 31.12.2010]).", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5586 (maintenant : [27.01.2003 -> 31.10.2006]).", msgs.get(5));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5642 (maintenant : [12.05.1999 -> 26.01.2003]).", msgs.get(6));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ETABLISSEMENTS);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("WARN;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;Etablissement avec rattachement propriétaire direct sur la commune Echallens/5518.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;Etablissement avec rattachement propriétaire (via groupe) sur la commune Echallens/5518.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;Domicile : [12.05.1999 -> 26.01.2003] sur COMMUNE_OU_FRACTION_VD/5642.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;Domicile : [27.01.2003 -> 31.10.2006] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(3));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertEquals(1, msgs.size());

			// on a la date de fin d'année courante car il s'agit d'un assujetti hors-Canton qui ferme tous ses fors vaudois,
			// donc l'assujettissement va jusqu'à la fin de l'exercice commercial actif au moment de la fermeture du dernier for vaudois
			// (et en l'absence de données d'exercices commerciaux, on suppose l'existance d'un seul exercice commercial qui court depuis
			// l'ouverture du premier for de l'entreprise jusqu'à la fin de l'année civile courante)
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;Apparition d'assujettissement sur une entreprise auparavant complètement non-assujettie : [01.01.1990 -> " + RegDateHelper.dateToDisplayString(getFinAnneeEnCours()) + "].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1999-05-12;2006-10-31;;" + idEtablissement + ";;" + noContribuableEtablissementSecondaire.longValue() + ";" + idEntreprise + ";;;" + idEntreprise + ";", msgs.get(0));
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1990-01-01;;;;;" + noContribuableEtablissementPrincipalCree.longValue() + ";" + idEntreprise + ";;;" + idEntreprise + ";", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msgs.get(0));
		}
	}

	@Test
	public void testImmeubleSurEtablissementNonStableNiMandataire() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BALE);       // un for principal de base
		EntrepriseMigratorTest.addSiegeSuisse(entreprise, RegDate.get(1990, 1, 1), Commune.BALE);       // un siège

		// un établissement
		final long idEtablissement = 235612L;
		final RegpmEtablissement etablissement = EtablissementMigratorTest.buildEtablissement(idEtablissement, entreprise);
		etablissement.setEnseigne("Le chat qui fume");

		// les immeubles
		final RegpmImmeuble immeuble1 = createImmeuble(Commune.ECHALLENS);
		EtablissementMigratorTest.addRattachementProprietaire(etablissement, RegDate.get(2006, 5, 1), RegDate.get(2010, 12, 31), immeuble1);

		final RegpmImmeuble immeuble2 = createImmeuble(Commune.ECHALLENS);
		final RegpmGroupeProprietaire groupe2 = createGroupeProprietaire("Zoo", RegpmTypeGroupeProprietaire.CONSORTIUM_SOCIETE_SIMPLE, RegDate.get(2000, 1, 1), null);
		EtablissementMigratorTest.addAppartenanceGroupeProprietaire(etablissement, groupe2, RegDate.get(2004, 5, 29), RegDate.get(2009, 12, 21), false);
		EtablissementMigratorTest.addRattachementProprietaire(groupe2, RegDate.get(2004, 7, 1), null, immeuble2);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     Collections.singletonList(etablissement),
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
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());      // seulement l'établissement principal car l'établissement de regpm n'est ni stable ni mandataire

			final Etablissement etbPrincipal = etablissements.get(0);
			Assert.assertNotNull(etbPrincipal);

			final Collection<ActiviteEconomique> rapportsActiviteEconomique = AbstractEntityMigratorTest.getRapportsObjets(etbPrincipal, ActiviteEconomique.class);
			Assert.assertNotNull(rapportsActiviteEconomique);
			Assert.assertEquals(1, rapportsActiviteEconomique.size());
			Assert.assertTrue(rapportsActiviteEconomique.iterator().next().isPrincipal());

			noContribuableEtablissementPrincipalCree.setValue(etbPrincipal.getNumero());

			final List<ForFiscal> fors = e.getForsFiscauxSorted();
			Assert.assertEquals(2, fors.size());

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
				Assert.assertNull(ffp.getMotifOuverture());         // premier for principal qui se trouve être hors canton
				Assert.assertNull(ffp.getMotifFermeture());
				Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
			}

			// for fiscal secondaire (immeuble) à Echallens (transmis depuis l'établissement)
			{
				final ForFiscal ff = fors.get(1);
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
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.ETABLISSEMENTS, LogCategory.ASSUJETTISSEMENTS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " d'après le siège 1.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.01.1990 -> ?] sur COMMUNE_HC/2701.", msgs.get(
					3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(4));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(3, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_HC/2701 [01.01.1990 -> ?] généré.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'immeuble' [01.07.2004 -> 31.12.2010] ajouté sur la commune 5518.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5518 (maintenant : [01.07.2004 -> 31.12.2010]).", msgs.get(2));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ETABLISSEMENTS);
			Assert.assertEquals(3, msgs.size());
			Assert.assertEquals("WARN;" + idEtablissement + ";;;;" + idEntreprise + ";;Etablissement avec rattachement propriétaire direct sur la commune Echallens/5518.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEtablissement + ";;;;" + idEntreprise + ";;Etablissement avec rattachement propriétaire (via groupe) sur la commune Echallens/5518.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEtablissement + ";;;;" + idEntreprise + ";;Etablissement ignoré car sans établissement stable ni rôle de mandataire.", msgs.get(2));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertEquals(1, msgs.size());

			// on a la date de fin d'année car il s'agit d'un assujetti hors-Canton qui ferme tous ses fors vaudois,
			// donc l'assujettissement va jusqu'à la fin de l'exercice commercial actif au moment de la fermeture du dernier for vaudois
			// (et en l'absence de données d'exercices commerciaux, on suppose l'existance d'un seul exercice commercial qui court depuis
			// l'ouverture du premier for de l'entreprise jusqu'à la fin de l'année en cours)
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;Apparition d'assujettissement sur une entreprise auparavant complètement non-assujettie : [01.01.1990 -> " + RegDateHelper.dateToDisplayString(getFinAnneeEnCours()) + "].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1990-01-01;;;;;" + noContribuableEtablissementPrincipalCree.longValue() + ";" + idEntreprise + ";;;" + idEntreprise + ";", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msgs.get(0));
		}
	}

	@Test
	public void testComparaisonAssujettissementsIdentiques() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeAssujettissement.LILIC);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		final List<String> msg = messages.get(LogCategory.ASSUJETTISSEMENTS);
		Assert.assertNull(msg);     // -> aucun message : pas de différence trouvée
	}

	@Test
	public void testComparaisonAssujettissementsIdentiquesSiFusionnes() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addForPrincipalEtranger(entreprise, RegDate.get(2000, 1, 1), RegpmTypeForPrincipal.SIEGE, MockPays.France.getNoOFS());
		EntrepriseMigratorTest.addRattachementProprietaire(entreprise, RegDate.get(1995, 5, 12), null, createImmeuble(Commune.LAUSANNE));
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeAssujettissement.LILIC);

		// avec le calcul Unireg, cela donne un assujettissement VD puis un assujettissement HS (qui donnent tous les deux lieu à de l'ICC)
		// alors que dans RegPM, il n'y avait qu'une seule période... si seuls les périodes couvertes nous intéressent, alors les deux doivent
		// être vus comme équivalents

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		final List<String> msg = messages.get(LogCategory.ASSUJETTISSEMENTS);
		Assert.assertNull(msg);     // -> aucun message : pas de différence trouvée
	}

	@Test
	public void testComparaisonAssujettissementDisparition() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(1980, 1, 1), RegDate.get(1985, 12, 31), RegpmTypeAssujettissement.LILIC);        // que fait-il donc là ?
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeAssujettissement.LILIC);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		final List<String> msg = messages.get(LogCategory.ASSUJETTISSEMENTS);
		Assert.assertNotNull(msg);
		Assert.assertEquals(1, msg.size());
		final String texte = msg.get(0);
		Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Ancienne période d'assujettissement disparue : [01.01.1980 -> 31.12.1985].", texte);
	}

	@Test
	public void testComparaisonAssujettissementModification() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(1990, 1, 1), RegDate.get(2014, 12, 31), RegpmTypeAssujettissement.LILIC);    // et après 2014 ???

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		final List<String> msg = messages.get(LogCategory.ASSUJETTISSEMENTS);
		Assert.assertNotNull(msg);
		Assert.assertEquals(1, msg.size());
		final String texte = msg.get(0);
		Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Période(s) d'assujettissement modifiée(s) : avant ([01.01.1990 -> 31.12.2014]) et après ([01.01.1990 -> ?]).", texte);
	}

	@Test
	public void testAssujettissementApparuValideApresSeuilSurEntrepriseInactive() throws Exception {

		final long idEntreprise = INACTIVE_ENTREPRISE_ID;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1991, 3, 14), RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);

		// cette entreprise est inactive (à cause de cet identifiant "magique"), mais a un for principal vaudois ouvert après 2015... -> ERREUR

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		final List<String> msg = messages.get(LogCategory.ASSUJETTISSEMENTS);
		Assert.assertNotNull(msg);
		Assert.assertEquals(2, msg.size());
		Assert.assertEquals("ERROR;" + idEntreprise + ";Inactive;;;Apparition d'assujettissement sur une entreprise auparavant complètement non-assujettie : [14.03.1991 -> ?].", msg.get(0));
		Assert.assertEquals("ERROR;" + idEntreprise + ";Inactive;;;Assujettissement calculé après le 01.01.2015 sur une entreprise considérée comme inactive.", msg.get(1));
	}

	@Test
	public void testAssujettissementApparuDebutApresSeuilSurEntrepriseInactive() throws Exception {

		final long idEntreprise = INACTIVE_ENTREPRISE_ID;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(2015, 5, 12), RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);

		// cette entreprise est inactive (à cause de cet identifiant "magique"), mais a un for principal vaudois ouvert après 2015... -> ERREUR

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		final List<String> msg = messages.get(LogCategory.ASSUJETTISSEMENTS);
		Assert.assertNotNull(msg);
		Assert.assertEquals(2, msg.size());
		Assert.assertEquals("ERROR;" + idEntreprise + ";Inactive;;;Apparition d'assujettissement sur une entreprise auparavant complètement non-assujettie : [12.05.2015 -> ?].", msg.get(0));
		Assert.assertEquals("ERROR;" + idEntreprise + ";Inactive;;;Assujettissement calculé après le 01.01.2015 sur une entreprise considérée comme inactive.", msg.get(1));
	}

	/**
	 * C'est le cas de l'entreprise 15486 : son for principal est à ZH (261) depuis 1991, mais en raison
	 * de fors secondaires antérieurs, on doit ramener cette date à avant 1990 (= période où ZH n'a pas le même numéro OFS)
	 */
	@Test
	public void testAdaptationCouvertureForsSecondairesAvecFusionCommunes() throws Exception {

		final long idEntreprise = 15486;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1991, 3, 14), RegpmTypeForPrincipal.SIEGE, Commune.ZURICH);
		EntrepriseMigratorTest.addRattachementProprietaire(entreprise, RegDate.get(1988, 1, 4), null, createImmeuble(Commune.ECHALLENS));

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		final List<String> msg = messages.get(LogCategory.FORS);
		Assert.assertNotNull(msg);
		Assert.assertEquals(6, msg.size());
		Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_HC/261 [14.03.1991 -> ?] généré.", msg.get(0));
		Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'immeuble' [04.01.1988 -> ?] ajouté sur la commune 5518.", msg.get(1));
		Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5518 (maintenant : [04.01.1988 -> ?]).", msg.get(2));
		Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;La date de début du for fiscal principal [14.03.1991 -> ?] est adaptée (-> 04.01.1988) pour couvrir les fors secondaires.", msg.get(3));
		Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Entité ForFiscalPrincipalPM [04.01.1988 -> ?] sur COMMUNE_HC/261 au moins partiellement remplacée par ForFiscalPrincipalPM [04.01.1988 -> 31.12.1989] sur COMMUNE_HC/253 pour suivre les fusions de communes.", msg.get(
				4));
		Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Entité ForFiscalPrincipalPM [04.01.1988 -> ?] sur COMMUNE_HC/261 au moins partiellement remplacée par ForFiscalPrincipalPM [01.01.1990 -> ?] sur COMMUNE_HC/261 pour suivre les fusions de communes.", msg.get(5));

		// on va regarder en base quand-même pour vérifier que les fors sont les bons (et qu'il n'y a qu'eux!!)
		doInUniregTransaction(true, status -> {

			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);       // c'est le même identifiant dans RegPM et dans Unireg
			Assert.assertNotNull(e);

			final List<ForFiscalPrincipalPM> ffps = e.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(ffps);
			Assert.assertEquals(2, ffps.size());

			{
				final ForFiscalPrincipalPM ffp = ffps.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(RegDate.get(1988, 1, 4), ffp.getDateDebut());
				Assert.assertEquals(RegDate.get(1989, 12, 31), ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) 253, ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertNull(ffp.getMotifOuverture());             // le premier for principal qui se trouve être hors-canton
				Assert.assertEquals(MotifFor.FUSION_COMMUNES, ffp.getMotifFermeture());
			}
			{
				final ForFiscalPrincipalPM ffp = ffps.get(1);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(RegDate.get(1990, 1, 1), ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) 261, ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(MotifFor.FUSION_COMMUNES, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getMotifFermeture());
			}
		});
	}

	@Test
	public void testForSurCommuneQuiFusionneEnGardantLeMemeNumeroOFS() throws Exception {

		final long idEntreprise = 15781L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1998, 4, 12), RegpmTypeForPrincipal.SIEGE, Commune.MONTAGNY);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		final List<String> msg = messages.get(LogCategory.FORS);
		Assert.assertNotNull(msg);
		Assert.assertEquals(5, msg.size());
		Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Entité ForFiscalPrincipalPM [12.04.1998 -> ?] sur COMMUNE_HC/2029 au moins partiellement remplacée par ForFiscalPrincipalPM [12.04.1998 -> 31.12.1999] sur COMMUNE_HC/2029 pour suivre les fusions de communes.", msg.get(0));
		Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Entité ForFiscalPrincipalPM [12.04.1998 -> ?] sur COMMUNE_HC/2029 au moins partiellement remplacée par ForFiscalPrincipalPM [01.01.2000 -> ?] sur COMMUNE_HC/2029 pour suivre les fusions de communes.", msg.get(1));
		Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Fusion des entités ForFiscalPrincipalPM [12.04.1998 -> 31.12.1999] sur COMMUNE_HC/2029 et ForFiscalPrincipalPM [01.01.2000 -> ?] sur COMMUNE_HC/2029 empêchée par le changement de la commune 2029 au 31.12.1999.", msg.get(2));
		Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_HC/2029 [12.04.1998 -> 31.12.1999] généré.", msg.get(3));
		Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_HC/2029 [01.01.2000 -> ?] généré.", msg.get(4));

		// on va regarder en base quand-même pour vérifier que les fors sont les bons (et qu'il n'y a qu'eux!!)
		doInUniregTransaction(true, status -> {

			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);       // c'est le même identifiant dans RegPM et dans Unireg
			Assert.assertNotNull(e);

			final List<ForFiscalPrincipalPM> ffps = e.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(ffps);
			Assert.assertEquals(2, ffps.size());

			{
				final ForFiscalPrincipalPM ffp = ffps.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(RegDate.get(1998, 4, 12), ffp.getDateDebut());
				Assert.assertEquals(RegDate.get(1999, 12, 31), ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.MONTAGNY.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertNull(ffp.getMotifOuverture());             // premier for principal qui se trouve être hors canton
				Assert.assertEquals(MotifFor.FUSION_COMMUNES, ffp.getMotifFermeture());
			}
			{
				final ForFiscalPrincipalPM ffp = ffps.get(1);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(RegDate.get(2000, 1, 1), ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.MONTAGNY.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(MotifFor.FUSION_COMMUNES, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getMotifFermeture());
			}
		});
	}

	/**
	 * Cas particulier d'entreprise pour laquelle il faut ralonger un for principal pour couvrir les fors secondaires
	 * alors qu'un individu est également dans le graphe (= dans ce cas, un flush - causé par la recherche de l'éventuel
	 * contribuable existant correspondant à l'individu - avait pour conséquence des erreurs de validation sur l'entreprise
	 * car le for fiscal allongé se retrouvait en double)
	 */
	@Test
	public void testMigrationGrapheAvecRalongementForPrincipalPourCouvertureForsSecondairesEtInvididu() throws Exception {

		final long idEntreprise = 423632L;
		final long idIndividu = Long.MAX_VALUE;     // ne devrait pas exister dans RCPers, selon toute vraissemblance
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		final RegpmIndividu individu = IndividuMigratorTest.buildBaseIndividu(idIndividu, "Deker", "Jacob", RegDate.get(1971, 10, 31), Sexe.MASCULIN);
		EntrepriseMigratorTest.addMandat(entreprise, individu, RegpmTypeMandat.GENERAL, null, RegDate.get(2009, 6, 12), RegDate.get(2010, 7, 13));      // mandat ajouté pour déclencher la migration de l'individu

		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(2000, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BERN);
		EntrepriseMigratorTest.addSiegeSuisse(entreprise, RegDate.get(2000, 1, 1), Commune.BERN);
		EntrepriseMigratorTest.addRattachementProprietaire(entreprise, RegDate.get(1986, 5, 12), null, createImmeuble(Commune.ECHALLENS));

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     Collections.singletonList(individu));

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// identifiants des nouvelles entités
		final MutableLong noEtablissementPrincipal = new MutableLong();
		final MutableLong noContribuableIndividu = new MutableLong();
		doInUniregTransaction(true, status -> {

			// établissement principal

			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissement = etablissements.get(0);
			Assert.assertNotNull(etablissement);

			final Collection<ActiviteEconomique> rapportsActiviteEconomique = AbstractEntityMigratorTest.getRapportsObjets(etablissement, ActiviteEconomique.class);
			Assert.assertNotNull(rapportsActiviteEconomique);
			Assert.assertEquals(1, rapportsActiviteEconomique.size());
			Assert.assertTrue(rapportsActiviteEconomique.iterator().next().isPrincipal());

			noEtablissementPrincipal.setValue(etablissement.getNumero());

			// domicile de l'établissement principal

			final Set<DomicileEtablissement> domiciles = etablissement.getDomiciles();
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(1, domiciles.size());

			final DomicileEtablissement domcile = domiciles.iterator().next();
			Assert.assertNotNull(domcile);
			Assert.assertFalse(domcile.isAnnule());
			Assert.assertEquals(RegDate.get(2000, 1, 1), domcile.getDateDebut());
			Assert.assertNull(domcile.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, domcile.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.BERN.getNoOfs(), domcile.getNumeroOfsAutoriteFiscale());

			final List<PersonnePhysique> pps = uniregStore.getEntitiesFromDb(PersonnePhysique.class, null);
			Assert.assertNotNull(pps);
			Assert.assertEquals(1, pps.size());

			final PersonnePhysique pp = pps.get(0);
			Assert.assertNotNull(pp);
			noContribuableIndividu.setValue(pp.getNumero());

			// contrôle des fors principaux sur la PM
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final Set<ForFiscal> fors = e.getForsFiscaux();
			Assert.assertEquals(2, fors.size());

			final Map<Class<?>, List<ForFiscal>> mapFors = fors.stream()
					.collect(Collectors.toMap(ForFiscal::getClass,
					                          Collections::singletonList,
					                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()),
					                          HashMap::new));
			Assert.assertEquals(2, mapFors.size());

			final List<ForFiscal> forsPrincipaux = mapFors.get(ForFiscalPrincipalPM.class);
			Assert.assertNotNull(forsPrincipaux);
			Assert.assertEquals(1, forsPrincipaux.size());
			final ForFiscalPrincipalPM ffp = (ForFiscalPrincipalPM) forsPrincipaux.get(0);
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(RegDate.get(1986, 5, 12), ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertNull(ffp.getMotifOuverture());         // premier for principal qui se trouve être hors canton
			Assert.assertNull(ffp.getMotifFermeture());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.BERN.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());

			final List<ForFiscal> forsSecondaires = mapFors.get(ForFiscalSecondaire.class);
			Assert.assertNotNull(forsSecondaires);
			Assert.assertEquals(1, forsSecondaires.size());
			final ForFiscalSecondaire ffs = (ForFiscalSecondaire) forsSecondaires.get(0);
			Assert.assertNotNull(ffs);
			Assert.assertFalse(ffs.isAnnule());
			Assert.assertEquals(RegDate.get(1986, 5, 12), ffs.getDateDebut());
			Assert.assertNull(ffs.getDateFin());
			Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffs.getMotifOuverture());
			Assert.assertNull(ffs.getMotifFermeture());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), ffs.getNumeroOfsAutoriteFiscale());
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());
		Assert.assertNotNull(noContribuableIndividu.getValue());

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.INDIVIDUS_PM, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(6, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " d'après le siège 1.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [01.01.2000 -> ?] sur COMMUNE_HC/351.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(4));
			Assert.assertEquals("INFO;;;;;;;;;;;" + idIndividu + ";" + noContribuableIndividu.longValue() + ";Deker;Jacob;MASCULIN;1971-10-31;Individu migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableIndividu.longValue()) + ".", msgs.get(5));
		}
		{
			final List<String> msgs = messages.get(LogCategory.INDIVIDUS_PM);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("WARN;" + idIndividu + ";" + noContribuableIndividu.longValue() + ";Deker;Jacob;MASCULIN;1971-10-31;L'individu RCPers " + idIndividu + " ne peut être renvoyé (Personne 'CT.VD.RCPERS/" + idIndividu + "' introuvable).", msgs.get(0));
			Assert.assertEquals("INFO;" + idIndividu + ";" + noContribuableIndividu.longValue() + ";Deker;Jacob;MASCULIN;1971-10-31;Trouvé un individu (165501) de RCPers pour le nom (Deker), prénom (Jacob), sexe (MASCULIN) et date de naissance (31.10.1971).", msgs.get(1));
			Assert.assertEquals("WARN;" + idIndividu + ";" + noContribuableIndividu.longValue() + ";Deker;Jacob;MASCULIN;1971-10-31;Individu 165501 trouvé dans RCPers sans équivalent dans Unireg...", msgs.get(2));
			Assert.assertEquals("INFO;" + idIndividu + ";" + noContribuableIndividu.longValue() + ";Deker;Jacob;MASCULIN;1971-10-31;Création de la personne physique " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableIndividu.longValue()) + " pour correspondre à l'individu RegPM.", msgs.get(3));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_HC/351 [01.01.2000 -> ?] généré.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'immeuble' [12.05.1986 -> ?] ajouté sur la commune 5518.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5518 (maintenant : [12.05.1986 -> ?]).", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;La date de début du for fiscal principal [01.01.2000 -> ?] est adaptée (-> 12.05.1986) pour couvrir les fors secondaires.", msgs.get(3));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;Apparition d'assujettissement sur une entreprise auparavant complètement non-assujettie : [12.05.1986 -> ?].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;2000-01-01;;;;;" + noEtablissementPrincipal.longValue() + ";" + idEntreprise + ";;;" + idEntreprise + ";", msgs.get(0));
			Assert.assertEquals("INFO;MANDANT_MANDATAIRE;2009-06-12;2010-07-13;" + idEntreprise + ";;;" + idEntreprise + ";;;" + idIndividu + ";" + noContribuableIndividu.longValue() + ";", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msgs.get(0));
		}
	}

	/**
	 * Cas qui apparait quelques fois : 5410, 40551...
	 * L'entité est radiée au RC, mettons en 2010, mais possède encore des immeubles en 2011...
	 */
	@Test
	public void testMultiplesZonesForsSecondairesNonCouvertesParForsPrincipaux() throws Exception {

		final long idEntreprise = 5410L;
		final RegDate dateRequisitionRadiation = RegDate.get(2010, 9, 28);
		final RegDate dateBouclementFutur = RegDate.get(2015, 12, 31);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		entreprise.setDateRequisitionRadiation(dateRequisitionRadiation);
		entreprise.setDateBouclementFutur(dateBouclementFutur);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1995, 5, 17), RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);

		// attention, important : il y a un trou entre les deux immeubles (afin qu'on ait plusieurs zones non-couvertes distinctes après la fin du for principal)
		EntrepriseMigratorTest.addRattachementProprietaire(entreprise, RegDate.get(2003, 5, 7), RegDate.get(2010, 12, 30), createImmeuble(Commune.ECHALLENS));
		EntrepriseMigratorTest.addRattachementProprietaire(entreprise, RegDate.get(2011, 1, 1), null, createImmeuble(Commune.MORGES));

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		doInUniregTransaction(true, status -> {

			// contrôle des fors principaux sur la PM
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final Set<ForFiscal> fors = e.getForsFiscaux();
			Assert.assertEquals(3, fors.size());

			final Map<Class<?>, List<ForFiscal>> mapFors = fors.stream()
					.collect(Collectors.toMap(ForFiscal::getClass,
					                          Collections::singletonList,
					                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).sorted(DateRangeComparator::compareRanges).collect(Collectors.toList()),
					                          HashMap::new));
			Assert.assertEquals(2, mapFors.size());

			final List<ForFiscal> forsPrincipaux = mapFors.get(ForFiscalPrincipalPM.class);
			Assert.assertNotNull(forsPrincipaux);
			Assert.assertEquals(1, forsPrincipaux.size());
			final ForFiscalPrincipalPM ffp = (ForFiscalPrincipalPM) forsPrincipaux.get(0);
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(RegDate.get(1995, 5, 17), ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getMotifFermeture());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());

			final List<ForFiscal> forsSecondaires = mapFors.get(ForFiscalSecondaire.class);
			Assert.assertNotNull(forsSecondaires);
			Assert.assertEquals(2, forsSecondaires.size());

			{
				final ForFiscalSecondaire ffs = (ForFiscalSecondaire) forsSecondaires.get(0);
				Assert.assertNotNull(ffs);
				Assert.assertFalse(ffs.isAnnule());
				Assert.assertEquals(RegDate.get(2003, 5, 7), ffs.getDateDebut());
				Assert.assertEquals(RegDate.get(2010, 12, 30), ffs.getDateFin());
				Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffs.getMotifOuverture());
				Assert.assertEquals(MotifFor.VENTE_IMMOBILIER, ffs.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), ffs.getNumeroOfsAutoriteFiscale());
			}
			{
				final ForFiscalSecondaire ffs = (ForFiscalSecondaire) forsSecondaires.get(1);
				Assert.assertNotNull(ffs);
				Assert.assertFalse(ffs.isAnnule());
				Assert.assertEquals(RegDate.get(2011, 1, 1), ffs.getDateDebut());
				Assert.assertNull(ffs.getDateFin());
				Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffs.getMotifOuverture());
				Assert.assertNull(ffs.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.MORGES.getNoOfs(), ffs.getNumeroOfsAutoriteFiscale());
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(25, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Date de fin d'activité proposée (date de réquisition de radiation) : 28.09.2010.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2014 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2013 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2012 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2011 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(5));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2010 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(6));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2009 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(7));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2008 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(8));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2007 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(9));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2006 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(10));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2005 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(11));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2004 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(12));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2003 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(13));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2002 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(14));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2001 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(15));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2000 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(16));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.1999 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(17));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.1998 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(18));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.1997 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(19));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.1996 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(20));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.1995 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(21));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Cycle de bouclements créé, applicable dès le 01.12.1995 : tous les 12 mois, à partir du premier 31.12.", msgs.get(22));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé, pas d'établissement principal créé.", msgs.get(23));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(24));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5586 [17.05.1995 -> 28.09.2010] généré.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'immeuble' [07.05.2003 -> 30.12.2010] ajouté sur la commune 5518.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'immeuble' [01.01.2011 -> ?] ajouté sur la commune 5642.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5518 (maintenant : [07.05.2003 -> 30.12.2010]).", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5642 (maintenant : [01.01.2011 -> ?]).", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;La date de fin du for fiscal principal [17.05.1995 -> 28.09.2010] est adaptée (-> 30.12.2010) pour couvrir les fors secondaires.", msgs.get(5));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;La date de fin du for fiscal principal [17.05.1995 -> 30.12.2010] est adaptée (-> ?) pour couvrir les fors secondaires.", msgs.get(6));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;Apparition d'assujettissement sur une entreprise auparavant complètement non-assujettie : [17.05.1995 -> ?].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msgs.get(0));
		}
	}

	@Test
	public void testMigrationRaisonSocialeEtFormeJuridique() throws Exception {

		final long idEntreprise = 48741L;
		final RegDate dateDebut = RegDate.get(2010, 9, 28);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(entreprise, dateDebut, "Billards", "&", "co", true);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// vérification de la présence de la raison sociale
		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final Set<DonneesRegistreCommerce> donneesRC = e.getDonneesRC();
			Assert.assertNotNull(donneesRC);
			Assert.assertEquals(1, donneesRC.size());

			final DonneesRegistreCommerce rc = donneesRC.iterator().next();
			Assert.assertNotNull(rc);
			Assert.assertEquals(dateDebut, rc.getDateDebut());
			Assert.assertNull(rc.getDateFin());
			Assert.assertEquals("Billards & co", rc.getRaisonSociale());
			Assert.assertEquals(FormeJuridiqueEntreprise.SA, rc.getFormeJuridique());

			final Set<CapitalEntreprise> capitaux = e.getCapitaux();
			Assert.assertNotNull(capitaux);
			Assert.assertEquals(0, capitaux.size());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni for principal.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé, pas d'établissement principal créé.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(4));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Données 'civiles' migrées : sur la période [28.09.2010 -> ?], raison sociale (Billards & co) et forme juridique (SA).", msgs.get(0));
		}
	}

	@Test
	public void testMigrationRaisonSocialeEtFormeJuridiqueAvecDateFin() throws Exception {

		final long idEntreprise = 48741L;
		final RegDate dateDebut = RegDate.get(2010, 9, 28);
		final RegDate dateRequisitionRadiation = RegDate.get(2014, 12, 27);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(entreprise, dateDebut, "Billards", "&", "co", true);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		entreprise.setDateRequisitionRadiation(dateRequisitionRadiation);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// vérification de la présence de la raison sociale
		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final Set<DonneesRegistreCommerce> donneesRC = e.getDonneesRC();
			Assert.assertNotNull(donneesRC);
			Assert.assertEquals(1, donneesRC.size());

			final DonneesRegistreCommerce rc = donneesRC.iterator().next();
			Assert.assertNotNull(rc);
			Assert.assertEquals(dateDebut, rc.getDateDebut());
			Assert.assertEquals(dateRequisitionRadiation, rc.getDateFin());
			Assert.assertEquals("Billards & co", rc.getRaisonSociale());
			Assert.assertEquals(FormeJuridiqueEntreprise.SA, rc.getFormeJuridique());

			final Set<CapitalEntreprise> capitaux = e.getCapitaux();
			Assert.assertNotNull(capitaux);
			Assert.assertEquals(0, capitaux.size());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(6, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Date de fin d'activité proposée (date de réquisition de radiation) : 27.12.2014.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni for principal.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé, pas d'établissement principal créé.", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(5));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Données 'civiles' migrées : sur la période [28.09.2010 -> 27.12.2014], raison sociale (Billards & co) et forme juridique (SA).", msgs.get(0));
		}
	}

	@Test
	public void testMigrationCapital() throws Exception {

		final long idEntreprise = 48741L;
		final RegDate dateDebut = RegDate.get(2010, 9, 28);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(entreprise, dateDebut, "Markus", "und", "Söhne", true);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.R.L.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addCapital(entreprise, dateDebut, 45678134L);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// vérification de la présence de la raison sociale
		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final Set<DonneesRegistreCommerce> donneesRC = e.getDonneesRC();
			Assert.assertNotNull(donneesRC);
			Assert.assertEquals(1, donneesRC.size());

			final DonneesRegistreCommerce rc = donneesRC.iterator().next();
			Assert.assertNotNull(rc);
			Assert.assertEquals(dateDebut, rc.getDateDebut());
			Assert.assertNull(rc.getDateFin());
			Assert.assertEquals("Markus und Söhne", rc.getRaisonSociale());
			Assert.assertEquals(FormeJuridiqueEntreprise.SARL, rc.getFormeJuridique());

			final Set<CapitalEntreprise> capitaux = e.getCapitaux();
			Assert.assertNotNull(capitaux);
			Assert.assertEquals(1, capitaux.size());

			final CapitalEntreprise capital = capitaux.iterator().next();
			Assert.assertNotNull(capital);
			Assert.assertEquals(dateDebut, capital.getDateDebut());
			Assert.assertNull(capital.getDateFin());
			Assert.assertNull(capital.getDateFin());
			Assert.assertNotNull(capital.getMontant());
			Assert.assertEquals((Long) 45678134L, capital.getMontant().getMontant());
			Assert.assertEquals("CHF", capital.getMontant().getMonnaie());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni for principal.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé, pas d'établissement principal créé.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(4));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Données 'civiles' migrées : sur la période [28.09.2010 -> ?], raison sociale (Markus und Söhne) et forme juridique (SARL).", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de capital migrée : sur la période [28.09.2010 -> ?], 45678134 CHF.", msgs.get(1));
		}
	}

	/**
	 * [SIFISC-16164] C'est le cas de l'entreprise 14657 dont un établissement (à Lausanne, identifiant regpm 3764) possède 4 périodes de validité d'établissements stables
	 * mais une seule période (couvrante, pas de problème) de domicile
	 */
	@Test
	public void testEtablissementAvecPlusieursPeriodesStablesEtUnSeulDomicile() throws Exception {

		final long idEntreprise = 14657L;
		final long idEtablissement = 3764L;
		final RegDate dateDebut = RegDate.get(1998, 9, 28);
		final RegDate dateFin1 = RegDate.get(2000, 6, 30);
		final RegDate dateDebut2 = RegDate.get(2001, 1, 1);
		final RegDate dateFin2 = RegDate.get(2005, 12, 31);
		final RegDate dateDebut3 = RegDate.get(2007, 1, 1);
		final RegDate dateFin3 = RegDate.get(2010, 12, 31);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addSiegeSuisse(entreprise, dateDebut, Commune.MORGES);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.MORGES);
		EntrepriseMigratorTest.addRaisonSociale(entreprise, dateDebut, "Markus", "und", "Söhne", true);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.R.L.", RegpmCategoriePersonneMorale.PM));

		final RegpmEtablissement etablissement = EtablissementMigratorTest.buildEtablissement(idEtablissement, entreprise);
		EtablissementMigratorTest.addDomicileEtablissement(etablissement, dateDebut, Commune.LAUSANNE, false);
		EtablissementMigratorTest.addEtablissementStable(etablissement, dateDebut, dateFin1);
		EtablissementMigratorTest.addEtablissementStable(etablissement, dateDebut2, dateFin2);
		EtablissementMigratorTest.addEtablissementStable(etablissement, dateDebut3, dateFin3);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     Collections.singletonList(etablissement),
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		final MutableLong noContribuableEtablissementPrincipal = new MutableLong();
		final MutableLong noContribuableEtablissementSecondaire = new MutableLong();

		// vérification des liens d'activité économique et des fors secondaires sur la PM migrée
		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final List<Etablissement> etbs = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(e);
			Assert.assertEquals(2, etbs.size());
			final Map<Boolean, Long> idsEtablissement = etbs.stream()
					.map(etb -> Pair.of(etb, AbstractEntityMigratorTest.getRapportsObjets(etb, ActiviteEconomique.class)))
					.map(pair -> {
						final List<ActiviteEconomique> activites = new ArrayList<>(pair.getRight());
						Assert.assertNotEquals(0, activites.size());
						Boolean principal = null;
						for (ActiviteEconomique activite : activites) {
							Assert.assertNotNull(activite);
							Assert.assertEquals((Long) idEntreprise, activite.getSujetId());
							Assert.assertEquals(pair.getLeft().getNumero(), activite.getObjetId());
							if (principal == null) {
								principal = activite.isPrincipal();
							}
							else {
								Assert.assertEquals(principal, activite.isPrincipal());     // vérification que tous les liens concernent le même type d'activité (prn vs sec)
							}
						}
						return Pair.of(principal, pair.getLeft().getNumero());
					})
					.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
			noContribuableEtablissementPrincipal.setValue(idsEtablissement.get(Boolean.TRUE));
			noContribuableEtablissementSecondaire.setValue(idsEtablissement.get(Boolean.FALSE));
			Assert.assertNotNull(noContribuableEtablissementPrincipal.getValue());
			Assert.assertNotNull(noContribuableEtablissementSecondaire.getValue());

			//
			// les rapports entre tiers (entre l'entreprise et les établissements)
			//

			final List<ActiviteEconomique> aes = uniregStore.getEntitiesFromDb(ActiviteEconomique.class, null);
			Assert.assertNotNull(aes);
			Assert.assertEquals(4, aes.size());

			final List<ActiviteEconomique> aesTries = new ArrayList<>(aes);
			Collections.sort(aesTries, DateRangeComparator::compareRanges);
			{
				final ActiviteEconomique ae = aesTries.get(0);
				Assert.assertNotNull(ae);
				Assert.assertFalse(ae.isAnnule());
				Assert.assertEquals(dateDebut, ae.getDateDebut());
				Assert.assertEquals(dateFin1, ae.getDateFin());
				Assert.assertEquals((Long) idEntreprise, ae.getSujetId());
				Assert.assertEquals((Long) noContribuableEtablissementSecondaire.longValue(), ae.getObjetId());
			}
			{
				final ActiviteEconomique ae = aesTries.get(1);
				Assert.assertNotNull(ae);
				Assert.assertFalse(ae.isAnnule());
				Assert.assertEquals(dateDebut, ae.getDateDebut());
				Assert.assertNull(ae.getDateFin());
				Assert.assertEquals((Long) idEntreprise, ae.getSujetId());
				Assert.assertEquals((Long) noContribuableEtablissementPrincipal.longValue(), ae.getObjetId());
			}
			{
				final ActiviteEconomique ae = aesTries.get(2);
				Assert.assertNotNull(ae);
				Assert.assertFalse(ae.isAnnule());
				Assert.assertEquals(dateDebut2, ae.getDateDebut());
				Assert.assertEquals(dateFin2, ae.getDateFin());
				Assert.assertEquals((Long) idEntreprise, ae.getSujetId());
				Assert.assertEquals((Long) noContribuableEtablissementSecondaire.longValue(), ae.getObjetId());
			}
			{
				final ActiviteEconomique ae = aesTries.get(3);
				Assert.assertNotNull(ae);
				Assert.assertFalse(ae.isAnnule());
				Assert.assertEquals(dateDebut3, ae.getDateDebut());
				Assert.assertEquals(dateFin3, ae.getDateFin());
				Assert.assertEquals((Long) idEntreprise, ae.getSujetId());
				Assert.assertEquals((Long) noContribuableEtablissementSecondaire.longValue(), ae.getObjetId());
			}

			//
			// les fors secondaires sur la PM
			//

			final ForsParType fpt = e.getForsParType(true);
			Assert.assertEquals(1, fpt.principauxPM.size());
			Assert.assertEquals(3, fpt.secondaires.size());

			{
				final ForFiscalPrincipalPM ffp = fpt.principauxPM.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(dateDebut, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.MORGES.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
			}
			{
				final ForFiscalSecondaire ffs = fpt.secondaires.get(0);
				Assert.assertNotNull(ffs);
				Assert.assertFalse(ffs.isAnnule());
				Assert.assertEquals(dateDebut, ffs.getDateDebut());
				Assert.assertEquals(dateFin1, ffs.getDateFin());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffs.getMotifOuverture());
				Assert.assertEquals(MotifFor.FIN_EXPLOITATION, ffs.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffs.getNumeroOfsAutoriteFiscale());
			}
			{
				final ForFiscalSecondaire ffs = fpt.secondaires.get(1);
				Assert.assertNotNull(ffs);
				Assert.assertFalse(ffs.isAnnule());
				Assert.assertEquals(dateDebut2, ffs.getDateDebut());
				Assert.assertEquals(dateFin2, ffs.getDateFin());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffs.getMotifOuverture());
				Assert.assertEquals(MotifFor.FIN_EXPLOITATION, ffs.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffs.getNumeroOfsAutoriteFiscale());
			}
			{
				final ForFiscalSecondaire ffs = fpt.secondaires.get(2);
				Assert.assertNotNull(ffs);
				Assert.assertFalse(ffs.isAnnule());
				Assert.assertEquals(dateDebut3, ffs.getDateDebut());
				Assert.assertEquals(dateFin3, ffs.getDateFin());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffs.getMotifOuverture());
				Assert.assertEquals(MotifFor.FIN_EXPLOITATION, ffs.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffs.getNumeroOfsAutoriteFiscale());
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.DONNEES_CIVILES_REGPM,
		                               LogCategory.ADRESSES,
		                               LogCategory.FORS,
		                               LogCategory.ASSUJETTISSEMENTS,
		                               LogCategory.ETABLISSEMENTS,
		                               LogCategory.RAPPORTS_ENTRE_TIERS),
		                    messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(6, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire.longValue()) + ".", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipal.longValue()) +  " d'après le siège 1.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipal.longValue()) +  " : [28.09.1998 -> ?] sur COMMUNE_OU_FRACTION_VD/5642.", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(5));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Données 'civiles' migrées : sur la période [28.09.1998 -> ?], raison sociale (Markus und Söhne) et forme juridique (SARL).", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;;;;;;;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5642 [28.09.1998 -> ?] généré.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [28.09.1998 -> 30.06.2000] ajouté sur la commune 5586.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [01.01.2001 -> 31.12.2005] ajouté sur la commune 5586.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For secondaire 'activité' [01.01.2007 -> 31.12.2010] ajouté sur la commune 5586.", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5586 (maintenant : [28.09.1998 -> 30.06.2000], [01.01.2001 -> 31.12.2005], [01.01.2007 -> 31.12.2010]).", msgs.get(4));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;Apparition d'assujettissement sur une entreprise auparavant complètement non-assujettie : [28.09.1998 -> ?].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ETABLISSEMENTS);
			Assert.assertEquals(3, msgs.size());
			Assert.assertEquals("INFO;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;Domicile : [28.09.1998 -> 30.06.2000] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;Domicile : [01.01.2001 -> 31.12.2005] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;Domicile : [01.01.2007 -> 31.12.2010] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(2));
		}
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1998-09-28;2000-06-30;;" + idEtablissement + ";;" + noContribuableEtablissementSecondaire.longValue() + ";" + idEntreprise + ";;;" + idEntreprise + ";", msgs.get(0));
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;2001-01-01;2005-12-31;;" + idEtablissement + ";;" + noContribuableEtablissementSecondaire.longValue() + ";" + idEntreprise + ";;;" + idEntreprise + ";", msgs.get(1));
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;2007-01-01;2010-12-31;;" + idEtablissement + ";;" + noContribuableEtablissementSecondaire.longValue() + ";" + idEntreprise + ";;;" + idEntreprise + ";", msgs.get(2));
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1998-09-28;;;;;" + noContribuableEtablissementPrincipal.longValue() + ";" + idEntreprise + ";;;" + idEntreprise + ";", msgs.get(3));
		}
	}

	@Test
	public void testFusionEntreprises() throws Exception {

		final long idEntrepriseAvantFusion1 = 43262L;
		final long idEntrepriseAvantFusion2 = 54415L;
		final long idEntrepriseApresFusion = 67233L;
		final RegDate dateBilanFusion = RegDate.get(2012, 6, 23);

		final RegpmEntreprise avant1 = EntrepriseMigratorTest.buildEntreprise(idEntrepriseAvantFusion1);
		EntrepriseMigratorTest.addForPrincipalSuisse(avant1, RegDate.get(2000, 5, 7), RegpmTypeForPrincipal.SIEGE, Commune.MORGES);
		avant1.setDateInscriptionRC(RegDate.get(2000, 5, 7));

		final RegpmEntreprise avant2 = EntrepriseMigratorTest.buildEntreprise(idEntrepriseAvantFusion2);
		EntrepriseMigratorTest.addForPrincipalSuisse(avant2, RegDate.get(2003, 9, 24), RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		avant2.setDateConstitution(RegDate.get(2003, 9, 24));

		final RegpmEntreprise apres = EntrepriseMigratorTest.buildEntreprise(idEntrepriseApresFusion);
		EntrepriseMigratorTest.addForPrincipalSuisse(apres, dateBilanFusion, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);   // dans RegPM, la commune résultante de la fusion a son for qui s'ouvre à la date du bilan de fusion

		EntrepriseMigratorTest.addFusion(avant1, apres, dateBilanFusion);
		EntrepriseMigratorTest.addFusion(avant2, apres, dateBilanFusion);

		final Graphe graphe = new MockGraphe(Arrays.asList(avant1, avant2, apres),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// vérification de ce qui a été mis en base
		doInUniregTransaction(true, status -> {

			// récupération des entreprises en base
			final Map<Long, Entreprise> entreprisesCrees = uniregStore.getEntitiesFromDb(Entreprise.class, null).stream()
					.collect(Collectors.toMap(Entreprise::getNumero, Function.identity()));

			final Entreprise entrepriseAvant1 = entreprisesCrees.get(idEntrepriseAvantFusion1);
			final Entreprise entrepriseAvant2 = entreprisesCrees.get(idEntrepriseAvantFusion2);
			final Entreprise entrepriseApres = entreprisesCrees.get(idEntrepriseApresFusion);
			Assert.assertNotNull(entrepriseAvant1);
			Assert.assertNotNull(entrepriseAvant2);
			Assert.assertNotNull(entrepriseApres);
			Assert.assertEquals(3, entreprisesCrees.size());

			// fors
			{
				final ForsParType fpt = entrepriseAvant1.getForsParType(true);
				Assert.assertNotNull(fpt);
				Assert.assertEquals(1, fpt.principauxPM.size());
				Assert.assertEquals(0, fpt.secondaires.size());

				final ForFiscalPrincipalPM ffp = fpt.principauxPM.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(RegDate.get(2000, 5, 7), ffp.getDateDebut());
				Assert.assertEquals(dateBilanFusion, ffp.getDateFin());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffp.getMotifOuverture());
				Assert.assertEquals(MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, ffp.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.MORGES.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
			}
			{
				final ForsParType fpt = entrepriseAvant2.getForsParType(true);
				Assert.assertNotNull(fpt);
				Assert.assertEquals(1, fpt.principauxPM.size());
				Assert.assertEquals(0, fpt.secondaires.size());

				final ForFiscalPrincipalPM ffp = fpt.principauxPM.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(RegDate.get(2003, 9, 24), ffp.getDateDebut());
				Assert.assertEquals(dateBilanFusion, ffp.getDateFin());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffp.getMotifOuverture());
				Assert.assertEquals(MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, ffp.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
			}
			{
				final ForsParType fpt = entrepriseApres.getForsParType(true);
				Assert.assertNotNull(fpt);
				Assert.assertEquals(1, fpt.principauxPM.size());
				Assert.assertEquals(0, fpt.secondaires.size());

				final ForFiscalPrincipalPM ffp = fpt.principauxPM.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(dateBilanFusion, ffp.getDateDebut());           // date reprise du for du mainframe, même si je pense que l'on devrait trouver le lendemain...
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(MotifFor.CESSATION_ACTIVITE_FUSION_FAILLITE, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
			}

			// rapports entre tiers
			final Map<TypeRapportEntreTiers, List<RapportEntreTiers>> rapports = uniregStore.getEntitiesFromDb(RapportEntreTiers.class, null).stream()
					.collect(Collectors.toMap(RapportEntreTiers::getType,
					                          Collections::singletonList,
					                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).sorted(Comparator.comparingLong(RapportEntreTiers::getSujetId)).collect(Collectors.toList()),
					                          () -> new EnumMap<>(TypeRapportEntreTiers.class)));

			final List<RapportEntreTiers> fusions = rapports.get(TypeRapportEntreTiers.FUSION_ENTREPRISES);
			Assert.assertNotNull(fusions);
			Assert.assertEquals(1, rapports.size());

			Assert.assertEquals(2, fusions.size());
			{
				final RapportEntreTiers fusion = fusions.get(0);
				Assert.assertNotNull(fusion);
				Assert.assertEquals(dateBilanFusion.getOneDayAfter(), fusion.getDateDebut());
				Assert.assertNull(fusion.getDateFin());
				Assert.assertEquals((Long) idEntrepriseAvantFusion1, fusion.getSujetId());
				Assert.assertEquals((Long) idEntrepriseApresFusion, fusion.getObjetId());
			}
			{
				final RapportEntreTiers fusion = fusions.get(1);
				Assert.assertNotNull(fusion);
				Assert.assertEquals(dateBilanFusion.getOneDayAfter(), fusion.getDateDebut());
				Assert.assertNull(fusion.getDateFin());
				Assert.assertEquals((Long) idEntrepriseAvantFusion2, fusion.getSujetId());
				Assert.assertEquals((Long) idEntrepriseApresFusion, fusion.getObjetId());
			}
		});
	}

	@Test
	public void testDeclarationEmiseSansLienVersExerciceCommercial() throws Exception {

		final long idEntreprise = 46545L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, RegDate.get(2013, 1, 1), EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addRaisonSociale(entreprise, RegDate.get(2013, 1, 1), "Toto SA", null, null, true);
		EntrepriseMigratorTest.addCapital(entreprise, RegDate.get(2013, 1, 1), 10000L);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(2013, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		final RegpmAssujettissement assujettissement = EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(2013, 1, 1), null, RegpmTypeAssujettissement.LILIC);
		final RegpmDossierFiscal df2013 = EntrepriseMigratorTest.addDossierFiscal(entreprise, assujettissement, 2013, RegDate.get(2014, 1, 3), RegpmModeImposition.POST);
		df2013.setDateRetour(RegDate.get(2014, 5, 12));
		final RegpmDossierFiscal df2014 = EntrepriseMigratorTest.addDossierFiscal(entreprise, assujettissement, 2014, RegDate.get(2015, 1, 1), RegpmModeImposition.POST);           // celui-ci n'a pas d'exercice commercial associé
		EntrepriseMigratorTest.addExerciceCommercial(entreprise, df2013, RegDate.get(2013, 1, 1), RegDate.get(2013, 12, 31));
		entreprise.setDateBouclementFutur(RegDate.get(2015, 12, 31));       // la DI a été envoyée, donc la date décalée d'un cran

		// ajout de quelques périodes fiscales utiles
		doInUniregTransaction(false, status -> {
			addPeriodeFiscale(2013);
			addPeriodeFiscale(2014);
		});

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// vérification du contenu de la base
		doInUniregTransaction(true, status -> {

			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final List<Declaration> declarations = e.getDeclarationsSorted();
			Assert.assertNotNull(declarations);
			Assert.assertEquals(2, declarations.size());        // une en 2013, une en 2014

			{
				// 2013
				final Declaration d = declarations.get(0);
				Assert.assertNotNull(d);
				Assert.assertFalse(d.isAnnule());
				Assert.assertEquals(DeclarationImpotOrdinairePM.class, d.getClass());
				Assert.assertEquals(RegDate.get(2013, 1, 1), d.getDateDebut());
				Assert.assertEquals(RegDate.get(2013, 12, 31), d.getDateFin());
				Assert.assertEquals((Integer) 2013, d.getPeriode().getAnnee());

				final List<EtatDeclaration> etats = d.getEtatsSorted();
				Assert.assertNotNull(etats);
				Assert.assertEquals(2, etats.size());
				{
					final EtatDeclaration etat = etats.get(0);
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(EtatDeclarationEmise.class, etat.getClass());
					Assert.assertEquals(RegDate.get(2014, 1, 3), etat.getDateObtention());
				}
				{
					final EtatDeclaration etat = etats.get(1);
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(EtatDeclarationRetournee.class, etat.getClass());
					Assert.assertEquals(RegDate.get(2014, 5, 12), etat.getDateObtention());
				}
			}
			{
				// 2014
				final Declaration d = declarations.get(1);
				Assert.assertNotNull(d);
				Assert.assertFalse(d.isAnnule());
				Assert.assertEquals(DeclarationImpotOrdinairePM.class, d.getClass());
				Assert.assertEquals(RegDate.get(2014, 1, 1), d.getDateDebut());
				Assert.assertEquals(RegDate.get(2014, 12, 31), d.getDateFin());
				Assert.assertEquals((Integer) 2014, d.getPeriode().getAnnee());

				final List<EtatDeclaration> etats = d.getEtatsSorted();
				Assert.assertNotNull(etats);
				Assert.assertEquals(1, etats.size());
				{
					final EtatDeclaration etat = etats.get(0);
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(EtatDeclarationEmise.class, etat.getClass());
					Assert.assertEquals(RegDate.get(2015, 1, 1), etat.getDateObtention());
				}
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.FORS,
		                               LogCategory.DECLARATIONS,
		                               LogCategory.DONNEES_CIVILES_REGPM),
		                    messages.keySet());

		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2014 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.2014 -> 31.12.2015].", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Cycle de bouclements créé, applicable dès le 01.12.2013 : tous les 12 mois, à partir du premier 31.12.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé, pas d'établissement principal créé.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(4));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5586 [01.01.2013 -> ?] généré.", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DECLARATIONS);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Génération d'une déclaration sur la PF 2013 à partir des dates [01.01.2013 -> 31.12.2013] de l'exercice commercial 1 et du dossier fiscal correspondant.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Délai initial de retour fixé au 16.08.2014.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Etat 'EMISE' migré au 03.01.2014.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Etat 'RETOURNEE' migré au 12.05.2014.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Génération d'une déclaration sur la PF 2014 à partir des dates [01.01.2014 -> 31.12.2014] de la période d'imposition calculée et du dossier fiscal 2014/1 sans exercice commercial lié.", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Délai initial de retour fixé au 14.08.2015.", msgs.get(5));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Etat 'EMISE' migré au 01.01.2015.", msgs.get(6));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Données 'civiles' migrées : sur la période [01.01.2013 -> ?], raison sociale (Toto SA) et forme juridique (SA).", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de capital migrée : sur la période [01.01.2013 -> ?], 10000 CHF.", msgs.get(1));
		}
	}
	@Test
	public void testDeclarationEmiseAnnuleeSansLienVersExerciceCommercial() throws Exception {

		final long idEntreprise = 46545L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, RegDate.get(2013, 1, 1), EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addRaisonSociale(entreprise, RegDate.get(2013, 1, 1), "Toto SA", null, null, true);
		EntrepriseMigratorTest.addCapital(entreprise, RegDate.get(2013, 1, 1), 10000L);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(2013, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		entreprise.setDateRequisitionRadiation(RegDate.get(2013, 12, 31));
		final RegpmAssujettissement assujettissement = EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(2013, 1, 1), RegDate.get(2013, 12, 31), RegpmTypeAssujettissement.LILIC);
		final RegpmDossierFiscal df2013 = EntrepriseMigratorTest.addDossierFiscal(entreprise, assujettissement, 2013, RegDate.get(2014, 1, 3), RegpmModeImposition.POST);
		df2013.setDateRetour(RegDate.get(2014, 5, 12));
		final RegpmDossierFiscal df2014 = EntrepriseMigratorTest.addDossierFiscal(entreprise, assujettissement, 2014, RegDate.get(2015, 1, 1), RegpmModeImposition.POST);           // celui-ci n'a pas d'exercice commercial associé
		df2014.setEtat(RegpmTypeEtatDossierFiscal.ANNULE);                                                                                                                          // et en plus il est annulé
		EntrepriseMigratorTest.addExerciceCommercial(entreprise, df2013, RegDate.get(2013, 1, 1), RegDate.get(2013, 12, 31));
		entreprise.setDateBouclementFutur(RegDate.get(2015, 12, 31));       // la DI a été envoyée, donc la date décalée d'un cran

		// ajout de quelques périodes fiscales utiles
		doInUniregTransaction(false, status -> {
			addPeriodeFiscale(2013);
			addPeriodeFiscale(2014);
		});

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// vérification du contenu de la base
		doInUniregTransaction(true, status -> {

			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final List<Declaration> declarations = e.getDeclarationsSorted();
			Assert.assertNotNull(declarations);
			Assert.assertEquals(2, declarations.size());        // une en 2013, une en 2014

			{
				// 2013
				final Declaration d = declarations.get(0);
				Assert.assertNotNull(d);
				Assert.assertFalse(d.isAnnule());
				Assert.assertEquals(DeclarationImpotOrdinairePM.class, d.getClass());
				Assert.assertEquals(RegDate.get(2013, 1, 1), d.getDateDebut());
				Assert.assertEquals(RegDate.get(2013, 12, 31), d.getDateFin());
				Assert.assertEquals((Integer) 2013, d.getPeriode().getAnnee());

				final List<EtatDeclaration> etats = d.getEtatsSorted();
				Assert.assertNotNull(etats);
				Assert.assertEquals(2, etats.size());
				{
					final EtatDeclaration etat = etats.get(0);
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(EtatDeclarationEmise.class, etat.getClass());
					Assert.assertEquals(RegDate.get(2014, 1, 3), etat.getDateObtention());
				}
				{
					final EtatDeclaration etat = etats.get(1);
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(EtatDeclarationRetournee.class, etat.getClass());
					Assert.assertEquals(RegDate.get(2014, 5, 12), etat.getDateObtention());
				}
			}
			{
				// 2014
				final Declaration d = declarations.get(1);
				Assert.assertNotNull(d);
				Assert.assertTrue(d.isAnnule());
				Assert.assertEquals(DeclarationImpotOrdinairePM.class, d.getClass());
				Assert.assertEquals(RegDate.get(2014, 1, 1), d.getDateDebut());
				Assert.assertEquals(RegDate.get(2014, 12, 31), d.getDateFin());
				Assert.assertEquals((Integer) 2014, d.getPeriode().getAnnee());

				final List<EtatDeclaration> etats = d.getEtatsSorted();
				Assert.assertNotNull(etats);
				Assert.assertEquals(1, etats.size());
				{
					final EtatDeclaration etat = etats.get(0);
					Assert.assertNotNull(etat);
					Assert.assertFalse(etat.isAnnule());
					Assert.assertEquals(EtatDeclarationEmise.class, etat.getClass());
					Assert.assertEquals(RegDate.get(2015, 1, 1), etat.getDateObtention());
				}
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.FORS,
		                               LogCategory.DECLARATIONS,
		                               LogCategory.DONNEES_CIVILES_REGPM),
		                    messages.keySet());

		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(6, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Date de fin d'activité proposée (date de réquisition de radiation) : 31.12.2013.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2014 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.2014 -> 31.12.2015].", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Cycle de bouclements créé, applicable dès le 01.12.2013 : tous les 12 mois, à partir du premier 31.12.", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé, pas d'établissement principal créé.", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(5));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5586 [01.01.2013 -> 31.12.2013] généré.", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DECLARATIONS);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Génération d'une déclaration sur la PF 2013 à partir des dates [01.01.2013 -> 31.12.2013] de l'exercice commercial 1 et du dossier fiscal correspondant.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Délai initial de retour fixé au 16.08.2014.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Etat 'EMISE' migré au 03.01.2014.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Etat 'RETOURNEE' migré au 12.05.2014.", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Déclaration annulée migrée sur la PF 2014 en supposant des exercices de 12 mois suite au dernier exercice non-annulé ([01.01.2013 -> 31.12.2013]) : [01.01.2014 -> 31.12.2014].", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Délai initial de retour fixé au 14.08.2015.", msgs.get(5));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Etat 'EMISE' migré au 01.01.2015.", msgs.get(6));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Données 'civiles' migrées : sur la période [01.01.2013 -> 31.12.2013], raison sociale (Toto SA) et forme juridique (SA).", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de capital migrée : sur la période [01.01.2013 -> 31.12.2013], 10000 CHF.", msgs.get(1));
		}
	}

	@Test
	public void testDoublonAvecFor() throws Exception {

		final long idEntreprise = 2623L;
		final RegDate dateDebut = RegDate.get(2001, 4, 2);
		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(e, dateDebut , "*Chez-moi sàrl", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.R.L.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// en base : le flag débiteur inactif doit avoir été mis, et les fors créés mais annulés
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(entreprise);
			Assert.assertTrue(entreprise.isAnnule());          // une étoile au début de la raison sociale -> tiers annulé

			final Set<ForFiscal> fors = entreprise.getForsFiscaux();
			Assert.assertNotNull(fors);
			Assert.assertEquals(1, fors.size());
			final ForFiscal forFiscal = fors.iterator().next();
			Assert.assertNotNull(forFiscal);
			Assert.assertTrue(forFiscal.isAnnule());        // <-- annulé !!!
			Assert.assertEquals(dateDebut, forFiscal.getDateDebut());
			Assert.assertNull(forFiscal.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forFiscal.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), forFiscal.getNumeroOfsAutoriteFiscale());
		});

		// et dans les messages de suivi ?
		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.FORS,
		                               LogCategory.DONNEES_CIVILES_REGPM),
		                    messages.keySet());


		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise identifiée comme un doublon.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé, pas d'établissement principal créé.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(4));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5586 [02.04.2001 -> ?] généré.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For fiscal ForFiscalPrincipalPM [02.04.2001 -> ?] sur COMMUNE_OU_FRACTION_VD/5586 annulé car l'entreprise a été identifiée comme un débiteur inactif.", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Données 'civiles' migrées : sur la période [02.04.2001 -> ?], raison sociale (*Chez-moi sàrl) et forme juridique (SARL).", msgs.get(0));
		}
	}

	/**
	 * Ceci est un test utile au debugging, on charge un graphe depuis un fichier sur disque (identique à ce que
	 * l'on peut envoyer dans la vraie migration) et on tente la migration du graphe en question
	 */
	@Ignore
	@Test
	public void testMigrationGrapheSerialise() throws Exception {

		final String grapheFilename = "/home/jacob/migration-pm/dump-regpm/00052898.data";
		final File file = new File(grapheFilename);
		final Graphe graphe = SerializationIntermediary.deserialize(file);

		// ajout de toutes les périodes fiscales
		doInUniregTransaction(false, status -> {
			for (int pf = 1995; pf <= RegDate.get().year() ; ++pf) {
				addPeriodeFiscale(pf);
			}
		});

		// lancement de la migration du graphe (de la même façon, en ce qui concerne la gestion des exception, que ce qui est fait dans le MigrationWorker)
		MigrationResultMessageProvider mr;
		try {
			mr = grapheMigrator.migrate(graphe);
			Assert.assertNotNull(mr);
		}
		catch (MigrationException e) {
			final MigrationResult res = new MigrationResult(graphe);
			final Long[] idsEntreprises = graphe.getEntreprises().keySet().toArray(new Long[graphe.getEntreprises().size()]);
			final String msg = String.format("Les entreprises %s n'ont pas pu être migrées : %s", Arrays.toString(idsEntreprises), MigrationWorker.dump(e));
			res.addMessage(LogCategory.EXCEPTIONS, LogLevel.ERROR, msg);
			mr = res;
		}

		// dump sur la sortie standard
		final String summary = mr.summary();
		System.out.println(summary);

		// on ouvre une session hibernate pour vérifier visuellement le contenu
		doInUniregTransaction(true, status ->  {
			final List<Entreprise> entreprises = uniregStore.getEntitiesFromDb(Entreprise.class, null);
			Assert.assertNotNull(entreprises);
		});
	}
}
