package ch.vd.uniregctb.migration.pm.engine;

import java.io.File;
import java.util.Arrays;
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
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.MigrationResultMessageProvider;
import ch.vd.uniregctb.migration.pm.SerializationIntermediary;
import ch.vd.uniregctb.migration.pm.communes.FractionsCommuneProvider;
import ch.vd.uniregctb.migration.pm.communes.FusionCommunesProvider;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.log.LoggedElementRenderer;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmImmeuble;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;
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
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersDAO;
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

		final ActivityManager activityManager = entreprise -> INACTIVE_ENTREPRISE_ID != entreprise.getId();         // tout le monde est actif dans ces tests, sauf la 1832
		final RegDate seuilActivite = RegDate.get(2015, 1, 1);

		grapheMigrator = new GrapheMigrator();
		grapheMigrator.setEntrepriseMigrator(new EntrepriseMigrator(uniregStore, activityManager, infraService, bouclementService, assujettissementService, seuilActivite, rcEntAdapter, adresseHelper, fusionCommunesProvider, fractionsCommuneProvider));
		grapheMigrator.setEtablissementMigrator(new EtablissementMigrator(uniregStore, activityManager, infraService, rcEntAdapter, adresseHelper, fusionCommunesProvider, fractionsCommuneProvider));
		grapheMigrator.setIndividuMigrator(new IndividuMigrator(uniregStore, activityManager, infraService, tiersDAO, rcpersClient, nonHabitantIndex, fusionCommunesProvider, fractionsCommuneProvider));
		grapheMigrator.setUniregStore(uniregStore);
		grapheMigrator.setUniregTransactionManager(getUniregTransactionManager());
		grapheMigrator.setValidationInterceptor(validationInterceptor);
		grapheMigrator.afterPropertiesSet();

		validationService = getBean(ValidationService.class, "validationService");
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
					Assert.assertEquals("Sujet : Mandat (01.01.2000 - 31.12.2006)", msg.getMessage());
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
			final List<Etablissement> etablissementsTries = etablissements.stream().sorted(Comparator.comparing(Etablissement::isPrincipal)).collect(Collectors.toList());

			// établissement secondaire du mandataire
			{
				final Etablissement etb = etablissementsTries.get(0);
				Assert.assertEquals(0, etb.getRapportsSujet().size());
				Assert.assertFalse(etb.isPrincipal());

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
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.ADRESSES, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.ETABLISSEMENTS), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(10, msgs.size());
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;" + idEtablissementMandataire + ";;;" + idEntrepriseMandataire + ";;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaireMandataire.longValue()) + ".", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;Pas de commune ni de for principal associé, pas d'établissement principal créé.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntrepriseMandante) + ".", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(5));
			Assert.assertEquals("WARN;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(6));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalMandataire.longValue()) + ".", msgs.get(7));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalMandataire.longValue()) + " : [01.01.1990 -> ?] sur PAYS_HS/8215.", msgs.get(8));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntrepriseMandataire) + ".", msgs.get(9));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("WARN;" + idEntrepriseMandataire + ";Active;;;" + idEtablissementMandataire + ";;;" + idEntrepriseMandataire + ";;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(0));
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
			Assert.assertEquals("WARN;" + idEntrepriseMandataire + ";Active;;;Nouvelle période d'assujettissement apparue : [01.01.1995 -> ?].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ETABLISSEMENTS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + idEtablissementMandataire + ";;;" + idEntrepriseMandataire + ";;Domicile : [01.01.1995 -> ?] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(0));
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
				Assert.assertFalse(etb.isPrincipal());

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
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.ADRESSES, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.ETABLISSEMENTS), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement1 + ";;;" + idEntreprise + ";;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire1.longValue()) + ".", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement2 + ";;;" + idEntreprise + ";;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire2.longValue()) + ".", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.01.1990 -> ?] sur COMMUNE_HC/2701.", msgs.get(5));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(6));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;" + idEtablissement1 + ";;;" + idEntreprise + ";;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;" + idEtablissement2 + ";;;" + idEntreprise + ";;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(1));
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

			// on a la date du jour car il s'agit d'un assujetti hors-Canton qui ferme tous ses fors vaudois,
			// donc l'assujettissement va jusqu'à la fin de l'exercice commercial actif au moment de la fermeture du dernier for vaudois
			// (et en l'absence de données d'exercices commerciaux, on suppose l'existance d'un seul exercice commercial qui court depuis
			// l'ouverture du premier for de l'entreprise jusqu'à la date du jour)
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Nouvelle période d'assujettissement apparue : [01.01.1990 -> " + RegDateHelper.dateToDisplayString(RegDate.get()) + "].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ETABLISSEMENTS);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("INFO;" + idEtablissement1 + ";;;" + idEntreprise + ";;Domicile : [12.05.1999 -> 26.01.2003] sur COMMUNE_OU_FRACTION_VD/5642.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEtablissement1 + ";;;" + idEntreprise + ";;Domicile : [27.01.2003 -> 31.10.2006] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEtablissement2 + ";;;" + idEntreprise + ";;Domicile : [14.07.2002 -> 21.03.2004] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEtablissement2 + ";;;" + idEntreprise + ";;Domicile : [22.03.2004 -> 25.11.2010] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(3));
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
		final MutableLong noContribuableEtablissementSecondaire1 = new MutableLong();
		final MutableLong noContribuableEtablissementSecondaire2 = new MutableLong();

		// vérification du résultat
		doInUniregTransaction(true, status -> {

			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);       // c'est le même identifiant dans RegPM et dans Unireg
			Assert.assertNotNull(e);

			// pour tester la cohérence avec le message de suivi par la suite
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			noContribuableEtablissementPrincipalCree.setValue(etablissements.stream()
					                                                  .filter(Etablissement::isPrincipal)
					                                                  .findAny()
					                                                  .map(Etablissement::getNumero)
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
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.ADRESSES, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.ETABLISSEMENTS), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement1 + ";;;" + idEntreprise + ";;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire1.longValue()) + ".", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement2 + ";;;" + idEntreprise + ";;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire2.longValue()) + ".", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.01.1990 -> ?] sur COMMUNE_HC/2701.", msgs.get(5));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(6));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;" + idEtablissement1 + ";;;" + idEntreprise + ";;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;" + idEtablissement2 + ";;;" + idEntreprise + ";;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(1));
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

			// on a la date du jour car il s'agit d'un assujetti hors-Canton qui ferme tous ses fors vaudois,
			// donc l'assujettissement va jusqu'à la fin de l'exercice commercial actif au moment de la fermeture du dernier for vaudois
			// (et en l'absence de données d'exercices commerciaux, on suppose l'existance d'un seul exercice commercial qui court depuis
			// l'ouverture du premier for de l'entreprise jusqu'à la date du jour)
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Nouvelle période d'assujettissement apparue : [01.01.1990 -> " + RegDateHelper.dateToDisplayString(RegDate.get()) + "].", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ETABLISSEMENTS);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("INFO;" + idEtablissement1 + ";;;" + idEntreprise + ";;Domicile : [12.05.1999 -> 26.01.2003] sur COMMUNE_OU_FRACTION_VD/5642.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEtablissement1 + ";;;" + idEntreprise + ";;Domicile : [27.01.2003 -> 31.10.2006] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEtablissement2 + ";;;" + idEntreprise + ";;Domicile : [14.07.2002 -> 21.03.2004] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEtablissement2 + ";;;" + idEntreprise + ";;Domicile : [22.03.2004 -> 25.11.2010] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(3));
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
					Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
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
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;Pas de commune ni de for principal associé, pas d'établissement principal créé.", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(3));
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
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Nouvelle période d'assujettissement apparue : [01.05.1982 -> ?].", msgs.get(0));
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
			Assert.assertTrue(etbPrincipal.isPrincipal());
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
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.05.1987 -> ?] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(4));
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
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Nouvelle période d'assujettissement apparue : [01.05.1982 -> ?].", msgs.get(0));
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
			Assert.assertTrue(etbPrincipal.isPrincipal());
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
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + noEntreprise+ ";Active;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.05.1987 -> ?] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(4));
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
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Nouvelle période d'assujettissement apparue : [01.05.1982 -> ?].", msgs.get(0));
		}
	}

	@Test
	public void testAucunForSecondaire() throws Exception {

		final long noEntreprise = 43782L;
		final RegDate dateDebutForPrincipal = RegDate.get(1987, 5, 1);

		final RegpmEntreprise regpm = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(regpm, dateDebutForPrincipal, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);

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
			Assert.assertTrue(etbPrincipal.isPrincipal());
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
				Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
				Assert.assertFalse(ffp.isAnnule());
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(mr);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.05.1987 -> ?] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(4));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5518 [01.05.1987 -> ?] généré.", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Nouvelle période d'assujettissement apparue : [01.05.1987 -> ?].", msgs.get(0));
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
	public void testImmeubleSurEtablissement() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BALE);       // un for principal de base

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
					                                                  .filter(Etablissement::isPrincipal)
					                                                  .findAny()
					                                                  .map(Etablissement::getNumero)
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
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.ADRESSES, LogCategory.FORS, LogCategory.ETABLISSEMENTS, LogCategory.ASSUJETTISSEMENTS), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(6, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement + ";;;" + idEntreprise + ";;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire.longValue()) + ".", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.01.1990 -> ?] sur COMMUNE_HC/2701.", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(5));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;" + idEtablissement + ";;;" + idEntreprise + ";;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(0));
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
			Assert.assertEquals("WARN;" + idEtablissement + ";;;" + idEntreprise + ";;Etablissement avec rattachement propriétaire direct sur la commune Echallens/5518.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEtablissement + ";;;" + idEntreprise + ";;Etablissement avec rattachement propriétaire (via groupe) sur la commune Echallens/5518.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEtablissement + ";;;" + idEntreprise + ";;Domicile : [12.05.1999 -> 26.01.2003] sur COMMUNE_OU_FRACTION_VD/5642.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEtablissement + ";;;" + idEntreprise + ";;Domicile : [27.01.2003 -> 31.10.2006] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(3));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertEquals(1, msgs.size());

			// on a la date du jour car il s'agit d'un assujetti hors-Canton qui ferme tous ses fors vaudois,
			// donc l'assujettissement va jusqu'à la fin de l'exercice commercial actif au moment de la fermeture du dernier for vaudois
			// (et en l'absence de données d'exercices commerciaux, on suppose l'existance d'un seul exercice commercial qui court depuis
			// l'ouverture du premier for de l'entreprise jusqu'à la date du jour)
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Nouvelle période d'assujettissement apparue : [01.01.1990 -> " + RegDateHelper.dateToDisplayString(RegDate.get()) + "].", msgs.get(0));
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
		Assert.assertEquals("WARN;" + idEntreprise + ";Inactive;;;Nouvelle période d'assujettissement apparue : [14.03.1991 -> ?].", msg.get(0));
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
		Assert.assertEquals("WARN;" + idEntreprise + ";Inactive;;;Nouvelle période d'assujettissement apparue : [12.05.2015 -> ?].", msg.get(0));
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
		Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Entité ForFiscalPrincipalPM [04.01.1988 -> ?] sur COMMUNE_HC/261 au moins partiellement remplacée par ForFiscalPrincipalPM [04.01.1988 -> 31.12.1989] sur COMMUNE_HC/253 pour suivre les fusions de communes.", msg.get(4));
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
				Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
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
		Assert.assertEquals(4, msg.size());
		Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Entité ForFiscalPrincipalPM [12.04.1998 -> ?] sur COMMUNE_HC/2029 au moins partiellement remplacée par ForFiscalPrincipalPM [12.04.1998 -> 31.12.1999] sur COMMUNE_HC/2029 pour suivre les fusions de communes.", msg.get(0));
		Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;Entité ForFiscalPrincipalPM [12.04.1998 -> ?] sur COMMUNE_HC/2029 au moins partiellement remplacée par ForFiscalPrincipalPM [01.01.2000 -> ?] sur COMMUNE_HC/2029 pour suivre les fusions de communes.", msg.get(1));
		Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_HC/2029 [12.04.1998 -> 31.12.1999] généré.", msg.get(2));
		Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_HC/2029 [01.01.2000 -> ?] généré.", msg.get(3));

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
				Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
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

		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(2000, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BERN);
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
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissement = etablissements.get(0);
			Assert.assertNotNull(etablissement);
			Assert.assertTrue(etablissement.isPrincipal());
			noEtablissementPrincipal.setValue(etablissement.getNumero());

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
			Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
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
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.INDIVIDUS_PM, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(6, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + ".", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [01.01.2000 -> ?] sur COMMUNE_HC/351.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(4));
			Assert.assertEquals("INFO;;;;;;;;;;" + idIndividu + ";Deker;Jacob;MASCULIN;1971-10-31;Individu migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableIndividu.longValue()) + ".", msgs.get(5));
		}
		{
			final List<String> msgs = messages.get(LogCategory.INDIVIDUS_PM);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("WARN;" + idIndividu + ";Deker;Jacob;MASCULIN;1971-10-31;L'individu RCPers " + idIndividu + " ne peut être renvoyé (Personne 'CT.VD.RCPERS/" + idIndividu + "' introuvable).", msgs.get(0));
			Assert.assertEquals("INFO;" + idIndividu + ";Deker;Jacob;MASCULIN;1971-10-31;Trouvé un individu (165501) de RCPers pour le nom (Deker), prénom (Jacob), sexe (MASCULIN) et date de naissance (31.10.1971).", msgs.get(1));
			Assert.assertEquals("WARN;" + idIndividu + ";Deker;Jacob;MASCULIN;1971-10-31;Individu 165501 trouvé dans RCPers sans équivalent dans Unireg...", msgs.get(2));
			Assert.assertEquals("INFO;" + idIndividu + ";Deker;Jacob;MASCULIN;1971-10-31;Création de la personne physique " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableIndividu.longValue()) + " pour correspondre à l'individu RegPM.", msgs.get(3));
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
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Nouvelle période d'assujettissement apparue : [12.05.1986 -> ?].", msgs.get(0));
		}
	}

	/**
	 * Ceci est un test utile au debugging, on charge un graphe depuis un fichier sur disque (identique à ce que
	 * l'on peut envoyer dans la vraie migration) et on tente la migration du graphe en question
	 */
	@Ignore
	@Test
	public void testMigrationGrapheSerialise() throws Exception {

		final String grapheFilename = "/home/jacob/migration-pm/dump-regpm/00004600.data";
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
