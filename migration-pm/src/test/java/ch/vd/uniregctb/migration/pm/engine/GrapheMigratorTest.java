package ch.vd.uniregctb.migration.pm.engine;

import java.io.File;
import java.math.BigDecimal;
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

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.mutable.MutableObject;
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
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.Capital;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.TypeDeCapital;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRCBuilder;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.adresse.AdresseMandataireSuisse;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.ProxyServiceOrganisation;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.SerializationIntermediary;
import ch.vd.uniregctb.migration.pm.communes.FractionsCommuneProvider;
import ch.vd.uniregctb.migration.pm.communes.FusionCommunesProvider;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.engine.helpers.DateHelper;
import ch.vd.uniregctb.migration.pm.engine.helpers.DoublonProvider;
import ch.vd.uniregctb.migration.pm.engine.helpers.OrganisationServiceAccessor;
import ch.vd.uniregctb.migration.pm.engine.helpers.RegimeFiscalHelper;
import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.log.LoggedMessage;
import ch.vd.uniregctb.migration.pm.log.LoggedMessages;
import ch.vd.uniregctb.migration.pm.log.MessageLoggedElement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCategoriePersonneMorale;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFonction;
import ch.vd.uniregctb.migration.pm.regpm.RegpmGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmImmeuble;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmModeImposition;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAdresseEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAdresseIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeRegimeFiscal;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.DatesParticulieres;
import ch.vd.uniregctb.migration.pm.utils.ValidationInterceptor;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.AdministrationEntreprise;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.CapitalFiscalEntreprise;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DonneeCivileEntreprise;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.FormeJuridiqueFiscaleEntreprise;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.MontantMonetaire;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.SocieteDirection;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.validation.EntityValidator;
import ch.vd.uniregctb.validation.ValidationService;

@SuppressWarnings("deprecation")
public class GrapheMigratorTest extends AbstractMigrationEngineTest {

	private static final long INACTIVE_ENTREPRISE_ID = 1832L;

	private static final ActivityManager ALL_ACTIVE = entreprise -> true;
	private static final ActivityManager ALL_BUT_ONE_ACTIVE = entreprise -> INACTIVE_ENTREPRISE_ID != entreprise.getId();

	private GrapheMigrator grapheMigrator;
	private ValidationService validationService;
	private UniregStore uniregStore;
	private ActivityManagerProxy activityManager;
	private ProxyServiceOrganisation organisationService;
	private MigrationContexte migrationContexte;

	private static Map<LogCategory, List<String>> buildTextualMessages(LoggedMessages lms) {
		final Map<LogCategory, List<LoggedMessage>> map = lms.asMap();
		final Map<LogCategory, List<String>> messages = new EnumMap<>(LogCategory.class);
		for (Map.Entry<LogCategory, List<LoggedMessage>> entry : map.entrySet()) {
			messages.put(entry.getKey(), entry.getValue().stream().map(LoggedMessage::getMessage).collect(Collectors.toList()));
		}
		return messages;
	}

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();

		uniregStore = getBean(UniregStore.class, "uniregStore");
		organisationService = getBean(ProxyServiceOrganisation.class, "serviceOrganisationService");

		final OrganisationServiceAccessor organisationServiceAccessor = new OrganisationServiceAccessor(organisationService, true, 1);
		final ValidationInterceptor validationInterceptor = getBean(ValidationInterceptor.class, "validationInterceptor");

		final NonHabitantIndex nonHabitantIndex = getBean(NonHabitantIndex.class, "nonHabitantIndex");
		nonHabitantIndex.overwriteIndex();

		activityManager = new ActivityManagerProxy();
		migrationContexte = new MigrationContexte(uniregStore,
		                                          activityManager,
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
		                                          organisationServiceAccessor,
		                                          getBean(DoublonProvider.class, "doublonProvider"),
		                                          getBean(RegimeFiscalHelper.class, "regimeFiscalHelper"),
		                                          getBean(TiersDAO.class, "tiersDAO"),
		                                          getBean(RcPersClient.class, "rcpersClient"),
		                                          nonHabitantIndex);

		grapheMigrator = new GrapheMigrator();
		grapheMigrator.setEntrepriseMigrator(new EntrepriseMigrator(migrationContexte));
		grapheMigrator.setEtablissementMigrator(new EtablissementMigrator(migrationContexte));
		grapheMigrator.setIndividuMigrator(new IndividuMigrator(migrationContexte));
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

		activityManager.setup(ALL_ACTIVE);

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
		EntrepriseMigratorTest.addRegimeFiscalVD(entrepriseMandataire, RegDate.get(2009, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entrepriseMandataire, RegDate.get(2009, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		final RegpmEtablissement mandataire = EtablissementMigratorTest.buildEtablissement(idEtablissementMandataire, entrepriseMandataire);
		EtablissementMigratorTest.addDomicileEtablissement(mandataire, RegDate.get(1995, 1, 1), Commune.ECHALLENS, false);
		EtablissementMigratorTest.addEtablissementStable(mandataire, RegDate.get(1995, 1, 1), null);

		EntrepriseMigratorTest.addAssujettissement(entrepriseMandataire, RegDate.get(1995, 1, 1), null, RegpmTypeAssujettissement.LILIC);

		EntrepriseMigratorTest.addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, null, RegDate.get(2000, 1, 1), RegDate.get(2006, 12, 31));
		EntrepriseMigratorTest.addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, null, RegDate.get(2010, 1, 1), null);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Arrays.asList(mandant, entrepriseMandataire),
		                                     Collections.singletonList(mandataire),
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.ADRESSES,
		                               LogCategory.FORS,
		                               LogCategory.ETABLISSEMENTS,
		                               LogCategory.RAPPORTS_ENTRE_TIERS,
		                               LogCategory.DONNEES_CIVILES_REGPM,
		                               LogCategory.MAPPINGS_REGIMES_FISCAUX),
		                    messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(16, msgs.size());
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;" + idEtablissementMandataire + ";" + noContribuableEtablissementSecondaireMandataire.longValue() + ";;;" + idEntrepriseMandataire + ";;;;;;;;Pas de numéro cantonal assigné sur l'établissement, pas de lien vers le civil.", msgs.get(0));
			Assert.assertEquals("ERROR;" + idEntrepriseMandataire + ";Active;;;" + idEtablissementMandataire + ";" + noContribuableEtablissementSecondaireMandataire.longValue() + ";;;" + idEntrepriseMandataire + ";;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;" + idEtablissementMandataire + ";" + noContribuableEtablissementSecondaireMandataire.longValue() + ";;;" + idEntrepriseMandataire + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaireMandataire.longValue()) + ".", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(3));
			Assert.assertEquals("ERROR;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni for principal.", msgs.get(5));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(6));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(7));
			Assert.assertEquals("WARN;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Le mandat 1 de l'entreprise mandante " + idEntrepriseMandante + " vers l'entité mandataire " + idEtablissementMandataire + " de type ETABLISSEMENT est ignoré car sa date de résiliation est antérieure au 01.01.2008 (31.12.2006).", msgs.get(8));
			Assert.assertEquals("INFO;" + idEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntrepriseMandante) + ".", msgs.get(9));
			Assert.assertEquals("WARN;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(10));
			Assert.assertEquals("WARN;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(11));
			Assert.assertEquals("ERROR;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(12));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalMandataire.longValue()) + ".", msgs.get(13));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalMandataire.longValue()) + " : [02.02.1990 -> ?] sur PAYS_HS/8215.", msgs.get(14));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntrepriseMandataire) + ".", msgs.get(15));
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
			final List<String> msgs = messages.get(LogCategory.ETABLISSEMENTS);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("WARN;" + idEtablissementMandataire + ";" + noContribuableEtablissementSecondaireMandataire.longValue() + ";;;" + idEntrepriseMandataire + ";;Le mandat 1 de l'entreprise mandante " + idEntrepriseMandante + " vers l'entité mandataire " + idEtablissementMandataire + " de type ETABLISSEMENT est ignoré car sa date de résiliation est antérieure au 01.01.2008 (31.12.2006).", msgs.get(0));
			Assert.assertEquals("INFO;" + idEtablissementMandataire + ";" + noContribuableEtablissementSecondaireMandataire.longValue() + ";;;" + idEntrepriseMandataire + ";;Domicile : [01.01.1995 -> ?] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("INFO;" + idEntrepriseMandante + ";Active;;;;;;;;;Donnée de raison sociale migrée : sur la période [01.01.1995 -> ?], 'Je suis le mandant'.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntrepriseMandante + ";Active;;;;;;;;;Donnée de forme juridique migrée : sur la période [01.01.1995 -> ?], ASSOCIATION.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;Donnée de raison sociale migrée : sur la période [01.01.1990 -> ?], 'Je suis le mandataire'.", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;;;;;;;Donnée de forme juridique migrée : sur la période [01.01.1990 -> ?], SA.", msgs.get(3));
		}
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertEquals(3, msgs.size());
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1995-01-01;;;" + idEtablissementMandataire + ";;" + noContribuableEtablissementSecondaireMandataire.longValue() + ";" + idEntrepriseMandataire + ";;;" + idEntrepriseMandataire + ";", msgs.get(0));
			Assert.assertEquals("INFO;MANDANT_MANDATAIRE;2010-01-01;;" + idEntrepriseMandante + ";;;" + idEntrepriseMandante + ";;" + idEtablissementMandataire + ";;" + noContribuableEtablissementSecondaireMandataire.longValue() + ";", msgs.get(1));
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1990-02-02;;;;;" + noContribuableEtablissementPrincipalMandataire.longValue() + ";" + idEntrepriseMandataire + ";;;" + idEntrepriseMandataire + ";", msgs.get(2));
		}
		{
			final List<String> msgs = messages.get(LogCategory.MAPPINGS_REGIMES_FISCAUX);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;2009-01-01;CH;01;01;", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntrepriseMandataire + ";Active;;;2009-01-01;VD;01;01;", msgs.get(1));
		}
	}

	@Test
	public void testMigrationEtablissementsMultiples() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		entreprise.setEnseigne("Smart zoo");
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BALE);       // un for principal de base
		EntrepriseMigratorTest.addSiegeSuisse(entreprise, RegDate.get(1990, 1, 1), Commune.BALE);       // un siège
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(1999, 5, 12), RegDate.get(2010, 11, 25), RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(1995, 5, 12), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(1995, 5, 12), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

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

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     Arrays.asList(etablissement1, etablissement2),
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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
				Assert.assertEquals(RegDate.get(2010, 11, 25), ae.getDateFin());
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
				Assert.assertEquals(RegDate.get(2010, 11, 25), domicile.getDateFin());
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

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.ADRESSES, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.ETABLISSEMENTS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM, LogCategory.MAPPINGS_REGIMES_FISCAUX), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(12, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement1 + ";" + noContribuableEtablissementSecondaire1.longValue() + ";;;" + idEntreprise + ";;;;;;;;Pas de numéro cantonal assigné sur l'établissement, pas de lien vers le civil.", msgs.get(0));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;" + idEtablissement1 + ";" + noContribuableEtablissementSecondaire1.longValue() + ";;;" + idEntreprise + ";;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement1 + ";" + noContribuableEtablissementSecondaire1.longValue() + ";;;" + idEntreprise + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire1.longValue()) + ".", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement2 + ";" + noContribuableEtablissementSecondaire2.longValue() + ";;;" + idEntreprise + ";;;;;;;;Pas de numéro cantonal assigné sur l'établissement, pas de lien vers le civil.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement2 + ";" + noContribuableEtablissementSecondaire2.longValue() + ";;;" + idEntreprise + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire2.longValue()) + ".", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(5));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Date de fin d'activité proposée (date de fin d'assujettissement ICC) : 25.11.2010.", msgs.get(6));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(7));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(8));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(9));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.01.1990 -> 25.11.2010] sur COMMUNE_HC/2701.", msgs.get(10));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(11));
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
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_HC/2701 [01.01.1990 -> 25.11.2010] généré.", msgs.get(0));
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
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Période(s) d'assujettissement modifiée(s) : avant ([12.05.1999 -> 25.11.2010]) et après ([01.01.1990 -> 25.11.2010]).", msgs.get(0));
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
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;1990-01-01;2010-11-25;;;;" + noContribuableEtablissementPrincipalCree.longValue() + ";" + idEntreprise+ ";;;" + idEntreprise + ";", msgs.get(2));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.MAPPINGS_REGIMES_FISCAUX);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;1995-05-12;CH;01;01;", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;1995-05-12;VD;01;01;", msgs.get(1));
		}
	}

	@Test
	public void testCalculForsSecondairesActiviteEtImmeuble() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BALE);       // un for principal de base
		EntrepriseMigratorTest.addSiegeSuisse(entreprise, RegDate.get(1990, 1, 1), Commune.BALE);       // un siège
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

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

		// l'assujettissement
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(1999, 5, 12), null, RegpmTypeAssujettissement.LILIC);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     Arrays.asList(etablissement1, etablissement2),
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.ADRESSES,
		                               LogCategory.FORS,
		                               LogCategory.ASSUJETTISSEMENTS,
		                               LogCategory.ETABLISSEMENTS,
		                               LogCategory.RAPPORTS_ENTRE_TIERS,
		                               LogCategory.DONNEES_CIVILES_REGPM,
		                               LogCategory.MAPPINGS_REGIMES_FISCAUX),
		                    messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(11, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement1 + ";" + noContribuableEtablissementSecondaire1.longValue() + ";;;" + idEntreprise + ";;;;;;;;Pas de numéro cantonal assigné sur l'établissement, pas de lien vers le civil.", msgs.get(0));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;" + idEtablissement1 + ";" + noContribuableEtablissementSecondaire1.longValue() + ";;;" + idEntreprise + ";;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement1 + ";" + noContribuableEtablissementSecondaire1.longValue() + ";;;" + idEntreprise + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire1.longValue()) + ".", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement2 + ";" + noContribuableEtablissementSecondaire2.longValue() + ";;;" + idEntreprise + ";;;;;;;;Pas de numéro cantonal assigné sur l'établissement, pas de lien vers le civil.", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement2 + ";" + noContribuableEtablissementSecondaire2.longValue() + ";;;" + idEntreprise + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire2.longValue()) + ".", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(5));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(6));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(7));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(8));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.01.1990 -> ?] sur COMMUNE_HC/2701.", msgs.get(9));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(10));
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
			// (et en l'absence de données d'exercices commerciaux, on suppose l'existence d'un seul exercice commercial qui court depuis
			// l'ouverture du premier for de l'entreprise jusqu'à la fin de l'année en cours)
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Période(s) d'assujettissement modifiée(s) : avant ([12.05.1999 -> ?]) et après ([01.01.1990 -> " + RegDateHelper.dateToDisplayString(getFinAnneeEnCours()) + "]).", msgs.get(0));
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
		{
			final List<String> msgs = messages.get(LogCategory.MAPPINGS_REGIMES_FISCAUX);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;1990-01-01;CH;01;01;", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;1990-01-01;VD;01;01;", msgs.get(1));
		}
	}

	@Test
	public void testForSecondaireImmeubleSurFraction() throws Exception {
		final long noEntreprise = 1234L;
		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegDate debut = RegDate.get(2005, 5, 7);
		EntrepriseMigratorTest.addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.BERN);
		EntrepriseMigratorTest.addRegimeFiscalVD(e, debut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(e, debut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		final RegpmImmeuble immeuble = createImmeuble(Commune.Fraction.LE_SENTIER);
		EntrepriseMigratorTest.addRattachementProprietaire(e, debut, null, immeuble);
		EntrepriseMigratorTest.addAssujettissement(e, debut, null, RegpmTypeAssujettissement.LILIC);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(e),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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
		EntrepriseMigratorTest.addAssujettissement(e, debut, null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(e, debut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(e, debut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		final RegpmEtablissement etb = EtablissementMigratorTest.buildEtablissement(idEtablissement, e);
		EtablissementMigratorTest.addEtablissementStable(etb, debut, null);
		EtablissementMigratorTest.addDomicileEtablissement(etb, debut, Commune.Fraction.LE_BRASSUS, false);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(e),
		                                     Collections.singletonList(etb),
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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
		EntrepriseMigratorTest.addRegimeFiscalVD(regpm, dateDebutForPrincipal, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(regpm, dateDebutForPrincipal, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		// mais un assujettissement quand-même (sinon le test ne représente plus le cas...)
		EntrepriseMigratorTest.addAssujettissement(regpm, dateAchatImmeuble1, null, RegpmTypeAssujettissement.LILIC);

		final RegpmImmeuble immeuble1 = EntrepriseMigratorTest.createImmeuble(Commune.MORGES);
		EntrepriseMigratorTest.addRattachementProprietaire(regpm, dateAchatImmeuble1, null, immeuble1);

		final RegpmImmeuble immeuble2 = EntrepriseMigratorTest.createImmeuble(Commune.LAUSANNE);
		EntrepriseMigratorTest.addRattachementProprietaire(regpm, dateAchatImmeuble2, null, immeuble2);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(regpm),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.DONNEES_CIVILES_REGPM, LogCategory.MAPPINGS_REGIMES_FISCAUX), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni for principal.", msgs.get(2));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(3));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(4));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(5));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(6));
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
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.MAPPINGS_REGIMES_FISCAUX);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;1987-05-01;CH;01;01;", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;1987-05-01;VD;01;01;", msgs.get(1));
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
		EntrepriseMigratorTest.addAssujettissement(regpm, dateDebutForPrincipal, null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(regpm, dateDebutForPrincipal, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(regpm, dateDebutForPrincipal, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		final RegpmImmeuble immeuble1 = EntrepriseMigratorTest.createImmeuble(Commune.MORGES);
		EntrepriseMigratorTest.addRattachementProprietaire(regpm, dateAchatImmeuble1, null, immeuble1);

		final RegpmImmeuble immeuble2 = EntrepriseMigratorTest.createImmeuble(Commune.LAUSANNE);
		EntrepriseMigratorTest.addRattachementProprietaire(regpm, dateAchatImmeuble2, null, immeuble2);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(regpm),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM, LogCategory.MAPPINGS_REGIMES_FISCAUX), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(4));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.05.1987 -> ?] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(5));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(6));
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
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Période(s) d'assujettissement modifiée(s) : avant ([01.05.1987 -> ?]) et après ([01.05.1982 -> ?]).", msgs.get(0));
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
		{
			final List<String> msgs = messages.get(LogCategory.MAPPINGS_REGIMES_FISCAUX);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;1987-05-01;CH;01;01;", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;1987-05-01;VD;01;01;", msgs.get(1));
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
		EntrepriseMigratorTest.addAssujettissement(regpm, dateDebutForPrincipal, null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(regpm, dateDebutForPrincipal, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(regpm, dateDebutForPrincipal, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		final RegpmImmeuble immeuble1 = EntrepriseMigratorTest.createImmeuble(Commune.MORGES);
		EntrepriseMigratorTest.addRattachementProprietaire(regpm, dateAchatImmeuble, dateVenteImmeuble, immeuble1);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(regpm),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM, LogCategory.MAPPINGS_REGIMES_FISCAUX), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(4));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.05.1987 -> ?] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(5));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(6));
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
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Période(s) d'assujettissement modifiée(s) : avant ([01.05.1987 -> ?]) et après ([01.05.1982 -> ?]).", msgs.get(0));
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
		{
			final List<String> msgs = messages.get(LogCategory.MAPPINGS_REGIMES_FISCAUX);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;1987-05-01;CH;01;01;", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;1987-05-01;VD;01;01;", msgs.get(1));
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
		EntrepriseMigratorTest.addAssujettissement(regpm, dateDebutForPrincipal, null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(regpm, dateDebutForPrincipal, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(regpm, dateDebutForPrincipal, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(regpm),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM, LogCategory.MAPPINGS_REGIMES_FISCAUX), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(4));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.05.1987 -> ?] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(5));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(6));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5518 [01.05.1987 -> ?] généré.", msgs.get(0));
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
		{
			final List<String> msgs = messages.get(LogCategory.MAPPINGS_REGIMES_FISCAUX);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;1987-05-01;CH;01;01;", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;1987-05-01;VD;01;01;", msgs.get(1));
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

		activityManager.setup(ALL_ACTIVE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);
		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(1999, 5, 12), null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

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

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     Collections.singletonList(etablissement),
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.ADRESSES, LogCategory.FORS, LogCategory.ETABLISSEMENTS, LogCategory.ASSUJETTISSEMENTS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM, LogCategory.MAPPINGS_REGIMES_FISCAUX), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(9, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;;;;;;;Pas de numéro cantonal assigné sur l'établissement, pas de lien vers le civil.", msgs.get(0));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire.longValue()) + ".", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(4));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(5));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(6));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.01.1990 -> ?] sur COMMUNE_HC/2701.", msgs.get(7));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(8));
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
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Période(s) d'assujettissement modifiée(s) : avant ([12.05.1999 -> ?]) et après ([01.01.1990 -> " + RegDateHelper.dateToDisplayString(getFinAnneeEnCours()) + "]).", msgs.get(0));
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
		{
			final List<String> msgs = messages.get(LogCategory.MAPPINGS_REGIMES_FISCAUX);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;1990-01-01;CH;01;01;", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;1990-01-01;VD;01;01;", msgs.get(1));
		}
	}

	@Test
	public void testImmeubleSurEtablissementNonStableNiMandataire() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BALE);       // un for principal de base
		EntrepriseMigratorTest.addSiegeSuisse(entreprise, RegDate.get(1990, 1, 1), Commune.BALE);       // un siège
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(2004, 7, 1), null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

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

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     Collections.singletonList(etablissement),
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.ETABLISSEMENTS, LogCategory.ASSUJETTISSEMENTS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM, LogCategory.MAPPINGS_REGIMES_FISCAUX), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + ".", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipalCree.longValue()) + " : [01.01.1990 -> ?] sur COMMUNE_HC/2701.", msgs.get(5));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(6));
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
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Période(s) d'assujettissement modifiée(s) : avant ([01.07.2004 -> ?]) et après ([01.01.1990 -> " + RegDateHelper.dateToDisplayString(getFinAnneeEnCours()) + "]).", msgs.get(0));
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
		{
			final List<String> msgs = messages.get(LogCategory.MAPPINGS_REGIMES_FISCAUX);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;1990-01-01;CH;01;01;", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;1990-01-01;VD;01;01;", msgs.get(1));
		}
	}

	@Test
	public void testComparaisonAssujettissementsIdentiques() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
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
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		activityManager.setup(ALL_ACTIVE);

		// avec le calcul Unireg, cela donne un assujettissement VD puis un assujettissement HS (qui donnent tous les deux lieu à de l'ICC)
		// alors que dans RegPM, il n'y avait qu'une seule période... si seuls les périodes couvertes nous intéressent, alors les deux doivent
		// être vus comme équivalents

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
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
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
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
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(1990, 1, 2), RegDate.get(2014, 12, 31), RegpmTypeAssujettissement.LILIC);    // décalage d'un jour au début
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(1990, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		final List<String> msg = messages.get(LogCategory.ASSUJETTISSEMENTS);
		Assert.assertNotNull(msg);
		Assert.assertEquals(1, msg.size());
		final String texte = msg.get(0);
		Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Période d'assujettissement décalée d'un jour : avant ([02.01.1990 -> 31.12.2014]) et après ([01.01.1990 -> 31.12.2014]).", texte);
	}

	@Test
	public void testAssujettissementApparuDebutApresSeuilSurEntrepriseInactive() throws Exception {

		final long idEntreprise = INACTIVE_ENTREPRISE_ID;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(2015, 5, 12), RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(2000, 1, 1), null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(2015, 5, 12), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(2015, 5, 12), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		// cette entreprise est inactive (à cause de cet identifiant "magique"), mais a un for principal vaudois ouvert après 2015... -> ERREUR
		activityManager.setup(ALL_BUT_ONE_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		final List<String> msg = messages.get(LogCategory.ASSUJETTISSEMENTS);
		Assert.assertNotNull(msg);
		Assert.assertEquals(2, msg.size());
		Assert.assertEquals("WARN;" + idEntreprise + ";Inactive;;;Période(s) d'assujettissement modifiée(s) : avant ([01.01.2000 -> ?]) et après ([12.05.2015 -> ?]).", msg.get(0));
		Assert.assertEquals("ERROR;" + idEntreprise + ";Inactive;;;Assujettissement calculé après le 01.01.2015 sur une entreprise considérée comme inactive.", msg.get(1));
	}

	@Test
	public void testForDebuteApresFinAssujettissementICC() throws Exception {

		final long idEntreprise = INACTIVE_ENTREPRISE_ID;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(2011, 7, 1), RegpmTypeForPrincipal.SIEGE, Commune.MORGES);
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(2000, 1, 1), RegDate.get(2011, 6, 30), RegpmTypeAssujettissement.LILIC);

		// cette entreprise est inactive (à cause de cet identifiant "magique"), mais a un for principal vaudois ouvert après 2015... -> ERREUR
		activityManager.setup(ALL_BUT_ONE_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.FORS,
		                               LogCategory.ASSUJETTISSEMENTS,
		                               LogCategory.DONNEES_CIVILES_REGPM,
		                               LogCategory.FORS_OUVERTS_APRES_FIN_ASSUJETTISSEMENT),
		                    messages.keySet());

		{
			final List<String> msg = messages.get(LogCategory.SUIVI);
			Assert.assertNotNull(msg);
			Assert.assertEquals(8, msg.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Inactive;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msg.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Inactive;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msg.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Inactive;;;;;;;;;;;;;;;Date de fin d'activité proposée (date de fin d'assujettissement ICC) : 30.06.2011.", msg.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Inactive;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni for principal.", msg.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Inactive;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msg.get(4));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Inactive;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msg.get(5));
			Assert.assertEquals("WARN;" + idEntreprise + ";Inactive;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msg.get(6));
			Assert.assertEquals("INFO;" + idEntreprise + ";Inactive;;;;;;;;;;;;;;;Entreprise migrée : 18.32.", msg.get(7));
		}
		{
			final List<String> msg = messages.get(LogCategory.FORS);
			Assert.assertNotNull(msg);
			Assert.assertEquals(1, msg.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Inactive;;;For fiscal principal vaudois 1 ignoré car sa date de début de validité (01.07.2011) est postérieure à la date de fin d'assujettissement ICC de l'entreprise (30.06.2011).", msg.get(0));
		}
		{
			final List<String> msg = messages.get(LogCategory.ASSUJETTISSEMENTS);
			Assert.assertNotNull(msg);
			Assert.assertEquals(1, msg.size());
			Assert.assertEquals("ERROR;" + idEntreprise + ";Inactive;;;Disparition totale de l'assujettissement précédent : [01.01.2000 -> 30.06.2011].", msg.get(0));
		}
		{
			final List<String> msg = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertNotNull(msg);
			Assert.assertEquals(1, msg.size());
			Assert.assertEquals("ERROR;" + idEntreprise + ";Inactive;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msg.get(0));
		}
		{
			final List<String> msg = messages.get(LogCategory.FORS_OUVERTS_APRES_FIN_ASSUJETTISSEMENT);
			Assert.assertNotNull(msg);
			Assert.assertEquals(1, msg.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";;Morges;2011-07-01;2011-06-30;", msg.get(0));
		}
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
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(1991, 3, 14), null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(1991, 3, 14), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(1991, 3, 14), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
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
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(1998, 4, 12), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(1998, 4, 12), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
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
		final RegpmIndividu individu = IndividuMigratorTest.buildBaseIndividu(idIndividu, "Porchet", "Philibert", RegDate.get(1971, 8, 31), Sexe.MASCULIN);
		EntrepriseMigratorTest.addMandat(entreprise, individu, RegpmTypeMandat.GENERAL, null, RegDate.get(2009, 6, 12), RegDate.get(2010, 7, 13));      // mandat ajouté pour déclencher la migration de l'individu

		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(2000, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BERN);
		EntrepriseMigratorTest.addSiegeSuisse(entreprise, RegDate.get(2000, 1, 1), Commune.BERN);
		EntrepriseMigratorTest.addRattachementProprietaire(entreprise, RegDate.get(1986, 5, 12), null, createImmeuble(Commune.ECHALLENS));
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(2000, 1, 1), null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(2000, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(2000, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     Collections.singletonList(individu));

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// identifiants des nouvelles entités
		final MutableLong noEtablissementPrincipal = new MutableLong();
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
			Assert.assertEquals(0, pps.size());

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

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.INDIVIDUS_PM, LogCategory.FORS, LogCategory.ASSUJETTISSEMENTS, LogCategory.RAPPORTS_ENTRE_TIERS, LogCategory.DONNEES_CIVILES_REGPM, LogCategory.MAPPINGS_REGIMES_FISCAUX), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + ".", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [01.01.2000 -> ?] sur COMMUNE_HC/351.", msgs.get(5));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(6));
		}
		{
			final List<String> msgs = messages.get(LogCategory.INDIVIDUS_PM);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("WARN;" + idIndividu + ";;Porchet;Philibert;MASCULIN;1971-08-31;L'individu RCPers " + idIndividu + " ne peut être renvoyé (Personne 'CT.VD.RCPERS/" + idIndividu + "' introuvable).", msgs.get(0));
			Assert.assertEquals("INFO;" + idIndividu + ";;Porchet;Philibert;MASCULIN;1971-08-31;Aucun résultat dans RCPers pour le nom (Porchet), prénom (Philibert), sexe (MASCULIN) et date de naissance (31.08.1971).", msgs.get(1));
			Assert.assertEquals("INFO;" + idIndividu + ";;Porchet;Philibert;MASCULIN;1971-08-31;Aucun non-habitant trouvé dans Unireg avec ces nom (Porchet), prénom (Philibert), sexe (MASCULIN) et date de naissance (31.08.1971).", msgs.get(2));
			Assert.assertEquals("ERROR;" + idIndividu + ";;Porchet;Philibert;MASCULIN;1971-08-31;Individu non migré car aucune correspondance univoque n'a pu être trouvée avec une personne physique existante dans Unireg.", msgs.get(3));
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
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Période(s) d'assujettissement modifiée(s) : avant ([01.01.2000 -> ?]) et après ([12.05.1986 -> ?]).", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;2000-01-01;;;;;" + noEtablissementPrincipal.longValue() + ";" + idEntreprise + ";;;" + idEntreprise + ";", msgs.get(0));
			Assert.assertEquals("ERROR;MANDANT_MANDATAIRE;2009-06-12;2010-07-13;" + idEntreprise + ";;;" + idEntreprise + ";;;" + idIndividu + ";;Lien non généré car l'une des parties au moins a finalement été exclue de la migration.", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.MAPPINGS_REGIMES_FISCAUX);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;2000-01-01;CH;01;01;", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;2000-01-01;VD;01;01;", msgs.get(1));
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
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(1995, 5, 17), null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(1995, 5, 17), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(1995, 5, 17), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		// attention, important : il y a un trou entre les deux immeubles (afin qu'on ait plusieurs zones non-couvertes distinctes après la fin du for principal)
		EntrepriseMigratorTest.addRattachementProprietaire(entreprise, RegDate.get(2003, 5, 7), RegDate.get(2010, 12, 30), createImmeuble(Commune.ECHALLENS));
		EntrepriseMigratorTest.addRattachementProprietaire(entreprise, RegDate.get(2011, 1, 1), null, createImmeuble(Commune.MORGES));

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.FORS, LogCategory.DONNEES_CIVILES_REGPM, LogCategory.MAPPINGS_REGIMES_FISCAUX), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(27, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Date de fin d'activité proposée (date de réquisition de radiation) : 28.09.2010.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2014 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2013 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2012 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(5));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2011 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(6));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2010 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(7));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2009 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(8));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2008 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(9));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2007 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(10));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2006 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(11));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2005 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(12));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2004 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(13));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2003 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(14));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2002 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(15));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2001 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(16));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2000 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(17));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.1999 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(18));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.1998 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(19));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.1997 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(20));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.1996 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(21));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.1995 pour combler l'absence d'exercice commercial dans RegPM sur la période [17.05.1995 -> 31.12.2015].", msgs.get(22));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Cycle de bouclements créé, applicable dès le 01.12.1995 : tous les 12 mois, à partir du premier 31.12.", msgs.get(23));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(24));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(25));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(26));
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
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;Impossible de déterminer la date de début des données du registre du commerce (aucune donnée de raison sociale et/ou de forme juridique).", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.MAPPINGS_REGIMES_FISCAUX);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;1995-05-17;CH;01;01;", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;1995-05-17;VD;01;01;", msgs.get(1));
		}
	}

	@Test
	public void testMigrationRaisonSocialeEtFormeJuridique() throws Exception {

		final long idEntreprise = 48741L;
		final RegDate dateDebut = RegDate.get(2010, 9, 28);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(entreprise, dateDebut, "Billards", "&", "co", true);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification de la présence de la raison sociale et de la forme juridique
		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final Map<Class, DonneeCivileEntreprise> map = e.getDonneesCiviles().stream()
					.collect(Collectors.toMap(Object::getClass, Function.identity()));
			Assert.assertNotNull(map);
			Assert.assertEquals(2, map.size());
			Assert.assertTrue(map.containsKey(RaisonSocialeFiscaleEntreprise.class));
			Assert.assertTrue(map.containsKey(FormeJuridiqueFiscaleEntreprise.class));
			Assert.assertFalse(map.containsKey(CapitalFiscalEntreprise.class));

			final RaisonSocialeFiscaleEntreprise rs = (RaisonSocialeFiscaleEntreprise) map.get(RaisonSocialeFiscaleEntreprise.class);
			Assert.assertNotNull(rs);
			Assert.assertEquals(dateDebut, rs.getDateDebut());
			Assert.assertNull(rs.getDateFin());
			Assert.assertEquals("Billards & co", rs.getRaisonSociale());

			final FormeJuridiqueFiscaleEntreprise fj = (FormeJuridiqueFiscaleEntreprise) map.get(FormeJuridiqueFiscaleEntreprise.class);
			Assert.assertNotNull(fj);
			Assert.assertEquals(dateDebut, fj.getDateDebut());
			Assert.assertNull(fj.getDateFin());
			Assert.assertEquals(FormeJuridiqueEntreprise.SA, fj.getFormeJuridique());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(6, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni for principal.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(5));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de raison sociale migrée : sur la période [28.09.2010 -> ?], 'Billards & co'.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de forme juridique migrée : sur la période [28.09.2010 -> ?], SA.", msgs.get(1));
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

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification de la présence de la raison sociale
		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final Map<Class, DonneeCivileEntreprise> map = e.getDonneesCiviles().stream()
					.collect(Collectors.toMap(Object::getClass, Function.identity()));
			Assert.assertNotNull(map);
			Assert.assertEquals(2, map.size());
			Assert.assertTrue(map.containsKey(RaisonSocialeFiscaleEntreprise.class));
			Assert.assertTrue(map.containsKey(FormeJuridiqueFiscaleEntreprise.class));
			Assert.assertFalse(map.containsKey(CapitalFiscalEntreprise.class));

			final RaisonSocialeFiscaleEntreprise rs = (RaisonSocialeFiscaleEntreprise) map.get(RaisonSocialeFiscaleEntreprise.class);
			Assert.assertNotNull(rs);
			Assert.assertEquals(dateDebut, rs.getDateDebut());
			Assert.assertEquals(dateRequisitionRadiation, rs.getDateFin());
			Assert.assertEquals("Billards & co", rs.getRaisonSociale());

			final FormeJuridiqueFiscaleEntreprise fj = (FormeJuridiqueFiscaleEntreprise) map.get(FormeJuridiqueFiscaleEntreprise.class);
			Assert.assertNotNull(fj);
			Assert.assertEquals(dateDebut, fj.getDateDebut());
			Assert.assertEquals(dateRequisitionRadiation, fj.getDateFin());
			Assert.assertEquals(FormeJuridiqueEntreprise.SA, fj.getFormeJuridique());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Date de fin d'activité proposée (date de réquisition de radiation) : 27.12.2014.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni for principal.", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(5));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(6));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de raison sociale migrée : sur la période [28.09.2010 -> 27.12.2014], 'Billards & co'.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de forme juridique migrée : sur la période [28.09.2010 -> 27.12.2014], SA.", msgs.get(1));
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

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification de la présence de la raison sociale
		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final Map<Class, DonneeCivileEntreprise> map = e.getDonneesCiviles().stream()
					.collect(Collectors.toMap(Object::getClass, Function.identity()));
			Assert.assertNotNull(map);
			Assert.assertEquals(3, map.size());
			Assert.assertTrue(map.containsKey(RaisonSocialeFiscaleEntreprise.class));
			Assert.assertTrue(map.containsKey(FormeJuridiqueFiscaleEntreprise.class));
			Assert.assertTrue(map.containsKey(CapitalFiscalEntreprise.class));

			final RaisonSocialeFiscaleEntreprise rs = (RaisonSocialeFiscaleEntreprise) map.get(RaisonSocialeFiscaleEntreprise.class);
			Assert.assertNotNull(rs);
			Assert.assertEquals(dateDebut, rs.getDateDebut());
			Assert.assertNull(rs.getDateFin());
			Assert.assertEquals("Markus und Söhne", rs.getRaisonSociale());

			final FormeJuridiqueFiscaleEntreprise fj = (FormeJuridiqueFiscaleEntreprise) map.get(FormeJuridiqueFiscaleEntreprise.class);
			Assert.assertNotNull(fj);
			Assert.assertEquals(dateDebut, fj.getDateDebut());
			Assert.assertNull(fj.getDateFin());
			Assert.assertEquals(FormeJuridiqueEntreprise.SARL, fj.getFormeJuridique());

			final CapitalFiscalEntreprise capital = (CapitalFiscalEntreprise) map.get(CapitalFiscalEntreprise.class);
			Assert.assertNotNull(capital);
			Assert.assertEquals(dateDebut, capital.getDateDebut());
			Assert.assertNull(capital.getDateFin());
			Assert.assertNotNull(capital.getMontant());
			Assert.assertEquals((Long) 45678134L, capital.getMontant().getMontant());
			Assert.assertEquals("CHF", capital.getMontant().getMonnaie());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI, LogCategory.DONNEES_CIVILES_REGPM), messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(6, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni for principal.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(4));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(5));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(3, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de raison sociale migrée : sur la période [28.09.2010 -> ?], 'Markus und Söhne'.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de forme juridique migrée : sur la période [28.09.2010 -> ?], SARL.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de capital migrée : sur la période [28.09.2010 -> ?], 45678134 CHF.", msgs.get(2));
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
		EntrepriseMigratorTest.addAssujettissement(entreprise, dateDebut, null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		final RegpmEtablissement etablissement = EtablissementMigratorTest.buildEtablissement(idEtablissement, entreprise);
		EtablissementMigratorTest.addDomicileEtablissement(etablissement, dateDebut, Commune.LAUSANNE, false);
		EtablissementMigratorTest.addEtablissementStable(etablissement, dateDebut, dateFin1);
		EtablissementMigratorTest.addEtablissementStable(etablissement, dateDebut2, dateFin2);
		EtablissementMigratorTest.addEtablissementStable(etablissement, dateDebut3, dateFin3);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     Collections.singletonList(etablissement),
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.DONNEES_CIVILES_REGPM,
		                               LogCategory.ADRESSES,
		                               LogCategory.FORS,
		                               LogCategory.ETABLISSEMENTS,
		                               LogCategory.RAPPORTS_ENTRE_TIERS,
		                               LogCategory.MAPPINGS_REGIMES_FISCAUX),
		                    messages.keySet());
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(9, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;;;;;;;Pas de numéro cantonal assigné sur l'établissement, pas de lien vers le civil.", msgs.get(0));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;" + idEtablissement + ";" + noContribuableEtablissementSecondaire.longValue() + ";;;" + idEntreprise + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementSecondaire.longValue()) + ".", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(4));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(5));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipal.longValue()) +  ".", msgs.get(6));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noContribuableEtablissementPrincipal.longValue()) +  " : [28.09.1998 -> ?] sur COMMUNE_OU_FRACTION_VD/5642.", msgs.get(7));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(8));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de raison sociale migrée : sur la période [28.09.1998 -> ?], 'Markus und Söhne'.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de forme juridique migrée : sur la période [28.09.1998 -> ?], SARL.", msgs.get(1));
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
		{
			final List<String> msgs = messages.get(LogCategory.MAPPINGS_REGIMES_FISCAUX);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;1998-09-28;CH;01;01;", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;1998-09-28;VD;01;01;", msgs.get(1));
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
		EntrepriseMigratorTest.addAssujettissement(avant1, RegDate.get(2000, 5, 7), dateBilanFusion, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(avant1, RegDate.get(2000, 5, 7), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(avant1, RegDate.get(2000, 5, 7), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		final RegpmEntreprise avant2 = EntrepriseMigratorTest.buildEntreprise(idEntrepriseAvantFusion2);
		EntrepriseMigratorTest.addForPrincipalSuisse(avant2, RegDate.get(2003, 9, 24), RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		avant2.setDateConstitution(RegDate.get(2003, 9, 24));
		EntrepriseMigratorTest.addAssujettissement(avant2, RegDate.get(2003, 9, 24), dateBilanFusion, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(avant2, RegDate.get(2003, 9, 24), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(avant2, RegDate.get(2003, 9, 24), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		final RegpmEntreprise apres = EntrepriseMigratorTest.buildEntreprise(idEntrepriseApresFusion);
		EntrepriseMigratorTest.addForPrincipalSuisse(apres, dateBilanFusion, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);   // dans RegPM, la commune résultante de la fusion a son for qui s'ouvre à la date du bilan de fusion
		EntrepriseMigratorTest.addAssujettissement(apres, dateBilanFusion, null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(apres, dateBilanFusion, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(apres, dateBilanFusion, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		EntrepriseMigratorTest.addFusion(avant1, apres, dateBilanFusion);
		EntrepriseMigratorTest.addFusion(avant2, apres, dateBilanFusion);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Arrays.asList(avant1, avant2, apres),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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
				Assert.assertEquals(MotifFor.FUSION_ENTREPRISES, ffp.getMotifFermeture());
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
				Assert.assertEquals(MotifFor.FUSION_ENTREPRISES, ffp.getMotifFermeture());
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
				Assert.assertEquals(MotifFor.FUSION_ENTREPRISES, ffp.getMotifOuverture());
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
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(2013, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(2013, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		final RegpmAssujettissement assujettissement = EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(2013, 1, 1), null, RegpmTypeAssujettissement.LILIC);
		final RegpmDossierFiscal df2013 = EntrepriseMigratorTest.addDossierFiscal(entreprise, assujettissement, 2013, RegDate.get(2014, 1, 3), RegpmModeImposition.POST);
		df2013.setDateRetour(RegDate.get(2014, 5, 12));
		EntrepriseMigratorTest.addDossierFiscal(entreprise, assujettissement, 2014, RegDate.get(2015, 1, 1), RegpmModeImposition.POST);           // celui-ci n'a pas d'exercice commercial associé
		EntrepriseMigratorTest.addExerciceCommercial(entreprise, df2013, RegDate.get(2013, 1, 1), RegDate.get(2013, 12, 31));
		entreprise.setDateBouclementFutur(RegDate.get(2015, 12, 31));       // la DI a été envoyée, donc la date décalée d'un cran

		// ajout de quelques périodes fiscales utiles
		doInUniregTransaction(false, status -> {
			addPeriodeFiscale(2013);
			addPeriodeFiscale(2014);
		});

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification du contenu de la base
		doInUniregTransaction(true, status -> {

			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final List<Declaration> declarations = e.getDeclarationsTriees();
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

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.FORS,
		                               LogCategory.DECLARATIONS,
		                               LogCategory.DONNEES_CIVILES_REGPM,
		                               LogCategory.MAPPINGS_REGIMES_FISCAUX),
		                    messages.keySet());

		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2014 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.2014 -> 31.12.2015].", msgs.get(2));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Cycle de bouclements créé, applicable dès le 01.12.2013 : tous les 12 mois, à partir du premier 31.12.", msgs.get(3));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(5));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(6));
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
			Assert.assertEquals(3, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de raison sociale migrée : sur la période [01.01.2013 -> ?], 'Toto SA'.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de forme juridique migrée : sur la période [01.01.2013 -> ?], SA.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de capital migrée : sur la période [01.01.2013 -> ?], 10000 CHF.", msgs.get(2));
		}
		{
			final List<String> msgs = messages.get(LogCategory.MAPPINGS_REGIMES_FISCAUX);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;2013-01-01;CH;01;01;", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;2013-01-01;VD;01;01;", msgs.get(1));
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
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(2013, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(2013, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
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

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification du contenu de la base
		doInUniregTransaction(true, status -> {

			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final List<Declaration> declarations = e.getDeclarationsTriees();
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

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.FORS,
		                               LogCategory.DECLARATIONS,
		                               LogCategory.DONNEES_CIVILES_REGPM,
		                               LogCategory.MAPPINGS_REGIMES_FISCAUX),
		                    messages.keySet());

		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(8, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Date de fin d'activité proposée (date de réquisition de radiation) : 31.12.2013.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2014 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.2014 -> 31.12.2015].", msgs.get(3));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Cycle de bouclements créé, applicable dès le 01.12.2013 : tous les 12 mois, à partir du premier 31.12.", msgs.get(4));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(5));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(6));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(7));
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
			Assert.assertEquals(3, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de raison sociale migrée : sur la période [01.01.2013 -> 31.12.2013], 'Toto SA'.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de forme juridique migrée : sur la période [01.01.2013 -> 31.12.2013], SA.", msgs.get(1));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de capital migrée : sur la période [01.01.2013 -> 31.12.2013], 10000 CHF.", msgs.get(2));
		}
		{
			final List<String> msgs = messages.get(LogCategory.MAPPINGS_REGIMES_FISCAUX);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;2013-01-01;CH;01;01;", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;2013-01-01;VD;01;01;", msgs.get(1));
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

		activityManager.setup(ALL_ACTIVE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

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
		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.FORS,
		                               LogCategory.DONNEES_CIVILES_REGPM),
		                    messages.keySet());


		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(8, msgs.size());
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise identifiée comme un doublon.", msgs.get(2));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(3));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'un régime fiscal VD de type '01' sur la période [01.01.2009 -> ?] pour couvrir les fors de l'entreprise.", msgs.get(4));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'un régime fiscal CH de type '01' sur la période [01.01.2009 -> ?] pour couvrir les fors de l'entreprise.", msgs.get(5));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(6));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(idEntreprise) + ".", msgs.get(7));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5586 [02.04.2001 -> ?] généré.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For fiscal ForFiscalPrincipalPM [02.04.2001 -> ?] sur COMMUNE_OU_FRACTION_VD/5586 annulé car l'entreprise a été identifiée comme un débiteur inactif.", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de raison sociale migrée : sur la période [02.04.2001 -> ?], '*Chez-moi sàrl'.", msgs.get(0));
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;;;;;;;Donnée de forme juridique migrée : sur la période [02.04.2001 -> ?], SARL.", msgs.get(1));
		}
	}

	@Test
	public void testFinAssujettissementAvantDebutForPrincipal() throws Exception {

		final long noEntreprise = 2623L;
		final RegDate dateCreationFor = RegDate.get(2005, 2, 1);
		final RegDate dateFinAssujettissement = RegDate.get(2006, 6, 12);
		final RegDate dateCreationDeuxiemeFor = dateFinAssujettissement.addDays(5);

		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(e, dateCreationFor, "Ma", "Petite", "Entreprise", true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateCreationFor, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateCreationFor, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateCreationDeuxiemeFor, RegpmTypeForPrincipal.SIEGE, Commune.MORGES);
		EntrepriseMigratorTest.addAssujettissement(e, dateCreationFor, dateFinAssujettissement, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addSiegeSuisse(e, dateCreationFor, Commune.LAUSANNE);
		e.setDateDissolution(dateFinAssujettissement);

		activityManager.setup(ALL_ACTIVE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// pour récupérer le numéro de tiers de l'établissement principal créé
		final MutableLong noEtablissementPrincipal = new MutableLong();

		// en base : la date de fin doit avoir été ignorée car incohérente avec un for principal qui commence après...
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final List<ForFiscalPrincipalPM> ffps = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(ffps);
			Assert.assertEquals(1, ffps.size());

			{
				final ForFiscalPrincipalPM ffp = ffps.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateCreationFor, ffp.getDateDebut());
				Assert.assertEquals(dateFinAssujettissement, ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
				Assert.assertEquals(MotifFor.CESSATION_ACTIVITE, ffp.getMotifFermeture());
			}

			// récupération du numéro fiscal de l'établissement principal généré
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissementPrincipal = etablissements.get(0);
			Assert.assertNotNull(etablissementPrincipal);
			noEtablissementPrincipal.setValue(etablissementPrincipal.getNumero());

			// le domicile de l'établissement
			final Set<DomicileEtablissement> domiciles = etablissementPrincipal.getDomiciles();
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(1, domiciles.size());

			final DomicileEtablissement domicile = domiciles.iterator().next();
			Assert.assertNotNull(domicile);
			Assert.assertEquals(dateCreationFor, domicile.getDateDebut());
			Assert.assertEquals(dateFinAssujettissement, domicile.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), domicile.getNumeroOfsAutoriteFiscale());
			Assert.assertFalse(domicile.isAnnule());
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());

		// et dans les messages de suivi ?
		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.FORS,
		                               LogCategory.DONNEES_CIVILES_REGPM,
		                               LogCategory.RAPPORTS_ENTRE_TIERS,
		                               LogCategory.FORS_OUVERTS_APRES_FIN_ASSUJETTISSEMENT),
		                    messages.keySet());

		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(8, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Date de fin d'activité proposée (date de dissolution) : 12.06.2006.", msgs.get(2));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(3));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(4));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + ".", msgs.get(5));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [01.02.2005 -> 12.06.2006] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(6));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : 26.23.", msgs.get(7));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;For fiscal principal vaudois 2 ignoré car sa date de début de validité (17.06.2006) est postérieure à la date de fin d'assujettissement ICC de l'entreprise (12.06.2006).", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5586 [01.02.2005 -> 12.06.2006] généré.", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;Donnée de raison sociale migrée : sur la période [01.02.2005 -> 12.06.2006], 'Ma Petite Entreprise'.", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;Donnée de forme juridique migrée : sur la période [01.02.2005 -> 12.06.2006], SA.", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;2005-02-01;2006-06-12;;;;" + noEtablissementPrincipal.longValue() + ";" + noEntreprise + ";;;" + noEntreprise + ";", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS_OUVERTS_APRES_FIN_ASSUJETTISSEMENT);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Ma Petite Entreprise;Morges;2006-06-17;2006-06-12;", msgs.get(0));
		}
	}

	@Test
	public void testEntrepriseAvecForMaisSansAssujettissementDansRegPM() throws Exception {

		final long noEntreprise = 2623L;
		final RegDate dateCreationFor = RegDate.get(2005, 2, 1);
		final RegDate dateDissolution = RegDate.get(2011, 6, 12);

		// une entreprise
		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(e, dateCreationFor, "Ma", "Petite", "Entreprise", true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateCreationFor, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateCreationFor, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		EntrepriseMigratorTest.addSiegeSuisse(e, dateCreationFor, Commune.LAUSANNE);
		e.setDateDissolution(dateDissolution);

		// un établissement
		final long idEtablissement = 4367324L;
		final RegpmEtablissement etablissement = EtablissementMigratorTest.buildEtablissement(idEtablissement, e);
		etablissement.setEnseigne("Le chien qui pête");
		EtablissementMigratorTest.addDomicileEtablissement(etablissement, dateCreationFor, Commune.RENENS, false);
		EtablissementMigratorTest.addEtablissementStable(etablissement, dateCreationFor, dateDissolution);

		// un immeuble
		final RegpmImmeuble immeuble = createImmeuble(Commune.ECHALLENS);
		EntrepriseMigratorTest.addRattachementProprietaire(e, RegDate.get(2006, 5, 1), RegDate.get(2010, 12, 31), immeuble);

		activityManager.setup(ALL_ACTIVE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         Collections.singletonList(etablissement),
		                                         null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// pour récupérer les numéros de tiers des établissements créés
		final MutableLong noEtablissementPrincipal = new MutableLong();
		final MutableLong noEtablissementSecondaire = new MutableLong();

		// vérification -> les fors doivent au final avoir été annulés par la migration en raison de l'absence totale d'assujettissement
		doInUniregTransaction(true, status -> {

			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<ForFiscal> fors = entreprise.getForsFiscaux();
			Assert.assertNotNull(fors);
			Assert.assertEquals(3, fors.size());

			final List<ForFiscal> forsTries = new ArrayList<>(fors);
			Collections.sort(forsTries, new DateRangeComparator<ForFiscal>() {
				@Override
				public int compare(ForFiscal o1, ForFiscal o2) {
					int comparaison = super.compare(o1, o2);
					if (comparaison == 0) {
						comparaison = Boolean.compare(o1.isPrincipal(), o2.isPrincipal());          // false < true
					}
					return comparaison;
				}
			});

			{
				final ForFiscal ff = forsTries.get(0);
				Assert.assertNotNull(ff);
				Assert.assertTrue(ff.isAnnule());       // tous sont annulés !!
				Assert.assertEquals(ForFiscalSecondaire.class, ff.getClass());
				Assert.assertEquals(dateCreationFor, ff.getDateDebut());
				Assert.assertEquals(dateDissolution, ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.RENENS.getNoOfs(), ff.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, ((ForFiscalRevenuFortune) ff).getMotifRattachement());
			}
			{
				final ForFiscal ff = forsTries.get(1);
				Assert.assertNotNull(ff);
				Assert.assertTrue(ff.isAnnule());       // tous sont annulés !!
				Assert.assertEquals(ForFiscalPrincipalPM.class, ff.getClass());
				Assert.assertEquals(dateCreationFor, ff.getDateDebut());
				Assert.assertEquals(dateDissolution, ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ff.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.DOMICILE, ((ForFiscalRevenuFortune) ff).getMotifRattachement());
			}
			{
				final ForFiscal ff = forsTries.get(2);
				Assert.assertNotNull(ff);
				Assert.assertTrue(ff.isAnnule());       // tous sont annulés !!
				Assert.assertEquals(ForFiscalSecondaire.class, ff.getClass());
				Assert.assertEquals(RegDate.get(2006, 5, 1), ff.getDateDebut());
				Assert.assertEquals(RegDate.get(2010, 12, 31), ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), ff.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ((ForFiscalRevenuFortune) ff).getMotifRattachement());
			}

			final Set<RapportEntreTiers> sujets = entreprise.getRapportsSujet();
			Assert.assertNotNull(sujets);
			Assert.assertEquals(2, sujets.size());

			// récupération des identifiants des établissements générés
			final Map<Boolean, ActiviteEconomique> liens = sujets.stream()
					.filter(lien -> lien instanceof ActiviteEconomique)
					.map(lien -> (ActiviteEconomique) lien)
					.collect(Collectors.toMap(ActiviteEconomique::isPrincipal, Function.identity()));
			Assert.assertEquals(2, liens.size());
			noEtablissementPrincipal.setValue(liens.get(Boolean.TRUE).getObjetId());
			noEtablissementSecondaire.setValue(liens.get(Boolean.FALSE).getObjetId());
			Assert.assertNotNull(noEtablissementPrincipal.getValue());
			Assert.assertNotNull(noEtablissementSecondaire.getValue());

			{
				final Etablissement etablissementPrincipal = uniregStore.getEntityFromDb(Etablissement.class, noEtablissementPrincipal.getValue());
				Assert.assertNotNull(etablissementPrincipal);

				// le domicile de l'établissement
				final Set<DomicileEtablissement> domiciles = etablissementPrincipal.getDomiciles();
				Assert.assertNotNull(domiciles);
				Assert.assertEquals(1, domiciles.size());

				final DomicileEtablissement domicile = domiciles.iterator().next();
				Assert.assertNotNull(domicile);
				Assert.assertEquals(dateCreationFor, domicile.getDateDebut());
				Assert.assertEquals(dateDissolution, domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), domicile.getNumeroOfsAutoriteFiscale());
				Assert.assertFalse(domicile.isAnnule());
			}
			{
				final Etablissement etablissementSecondaire = uniregStore.getEntityFromDb(Etablissement.class, noEtablissementSecondaire.getValue());
				Assert.assertNotNull(etablissementSecondaire);

				// le domicile de l'établissement
				final Set<DomicileEtablissement> domiciles = etablissementSecondaire.getDomiciles();
				Assert.assertNotNull(domiciles);
				Assert.assertEquals(1, domiciles.size());

				final DomicileEtablissement domicile = domiciles.iterator().next();
				Assert.assertNotNull(domicile);
				Assert.assertEquals(dateCreationFor, domicile.getDateDebut());
				Assert.assertEquals(dateDissolution, domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.RENENS.getNoOfs(), domicile.getNumeroOfsAutoriteFiscale());
				Assert.assertFalse(domicile.isAnnule());
			}
		});

		// et dans les messages de suivi ?
		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.ADRESSES,
		                               LogCategory.ETABLISSEMENTS,
		                               LogCategory.FORS,
		                               LogCategory.DONNEES_CIVILES_REGPM,
		                               LogCategory.RAPPORTS_ENTRE_TIERS,
		                               LogCategory.FORS_IGNORES_AUCUN_ASSUJETTISSEMENT),
		                    messages.keySet());

		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(11, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;" + idEtablissement + ";" + noEtablissementSecondaire.getValue() + ";;;" + noEntreprise + ";;;;;;;;Pas de numéro cantonal assigné sur l'établissement, pas de lien vers le civil.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;" + idEtablissement + ";" + noEtablissementSecondaire.getValue() + ";;;" + noEntreprise + ";;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;" + idEtablissement + ";" + noEtablissementSecondaire.getValue() + ";;;" + noEntreprise + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementSecondaire.getValue()) + ".", msgs.get(2));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Date de fin d'activité proposée (date de dissolution) : 12.06.2011.", msgs.get(4));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(5));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'un régime fiscal VD de type '01' sur la période [01.01.2009 -> ?] pour couvrir les fors de l'entreprise.", msgs.get(6));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'un régime fiscal CH de type '01' sur la période [01.01.2009 -> ?] pour couvrir les fors de l'entreprise.", msgs.get(7));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + ".", msgs.get(8));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [01.02.2005 -> 12.06.2011] sur COMMUNE_OU_FRACTION_VD/5586.", msgs.get(9));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : 26.23.", msgs.get(10));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;" + idEtablissement + ";" + noEtablissementSecondaire.getValue() + ";;;" + noEntreprise + ";;;;;;;;;;;;8100;Adresse trouvée sans rue ni localité postale.", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.ETABLISSEMENTS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + idEtablissement + ";" + noEtablissementSecondaire.getValue() + ";;;" + noEntreprise + ";;Domicile : [01.02.2005 -> 12.06.2011] sur COMMUNE_OU_FRACTION_VD/5591.", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(8, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5586 [01.02.2005 -> 12.06.2011] généré.", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For secondaire 'activité' [01.02.2005 -> 12.06.2011] ajouté sur la commune 5591.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For secondaire 'immeuble' [01.05.2006 -> 31.12.2010] ajouté sur la commune 5518.", msgs.get(2));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5518 (maintenant : [01.05.2006 -> 31.12.2010]).", msgs.get(3));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5591 (maintenant : [01.02.2005 -> 12.06.2011]).", msgs.get(4));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Abandon de la migration du for fiscal ForFiscalSecondaire [01.02.2005 -> 12.06.2011] sur COMMUNE_OU_FRACTION_VD/5591 en raison de l'absence totale d'assujettissement ICC dans RegPM pour cette entreprise.", msgs.get(5));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Abandon de la migration du for fiscal ForFiscalSecondaire [01.05.2006 -> 31.12.2010] sur COMMUNE_OU_FRACTION_VD/5518 en raison de l'absence totale d'assujettissement ICC dans RegPM pour cette entreprise.", msgs.get(6));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Abandon de la migration du for fiscal ForFiscalPrincipalPM [01.02.2005 -> 12.06.2011] sur COMMUNE_OU_FRACTION_VD/5586 en raison de l'absence totale d'assujettissement ICC dans RegPM pour cette entreprise.", msgs.get(7));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;Donnée de raison sociale migrée : sur la période [01.02.2005 -> 12.06.2011], 'Ma Petite Entreprise'.", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;Donnée de forme juridique migrée : sur la période [01.02.2005 -> 12.06.2011], SA.", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;2005-02-01;2011-06-12;;" + idEtablissement + ";;" + noEtablissementSecondaire.longValue() + ";" + noEntreprise + ";;;" + noEntreprise + ";", msgs.get(0));
			Assert.assertEquals("INFO;ETABLISSEMENT_ENTITE_JURIDIQUE;2005-02-01;2011-06-12;;;;" + noEtablissementPrincipal.longValue() + ";" + noEntreprise + ";;;" + noEntreprise + ";", msgs.get(1));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS_IGNORES_AUCUN_ASSUJETTISSEMENT);
			Assert.assertEquals(3, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Ma Petite Entreprise;COMMUNE_OU_FRACTION_VD;5591;2005-02-01;2011-06-12;ForFiscalSecondaire;ETABLISSEMENT_STABLE;", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Ma Petite Entreprise;COMMUNE_OU_FRACTION_VD;5518;2006-05-01;2010-12-31;ForFiscalSecondaire;IMMEUBLE_PRIVE;", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Ma Petite Entreprise;COMMUNE_OU_FRACTION_VD;5586;2005-02-01;2011-06-12;ForFiscalPrincipalPM;DOMICILE;", msgs.get(2));
		}
	}

	/**
	 * [SIFISC-17153] La date de réquisition de radiation avait été oubliée comme déclencheur du motif CESSATION_ACTIVITE
	 */
	@Test
	public void testMotifFermetureForPrincipalSurRequisitionDeRadiation() throws Exception {

		final long idEntreprise = 5410L;
		final RegDate dateRequisitionRadiation = RegDate.get(2010, 9, 28);
		final RegDate dateBouclementFutur = RegDate.get(2010, 12, 31);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		entreprise.setDateRequisitionRadiation(dateRequisitionRadiation);
		entreprise.setDateRadiationRC(dateRequisitionRadiation.addMonths(3));
		entreprise.setDateBouclementFutur(dateBouclementFutur);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1995, 5, 17), RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		EntrepriseMigratorTest.addAssujettissement(entreprise, RegDate.get(1995, 5, 17), null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, RegDate.get(1995, 5, 17), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, RegDate.get(1995, 5, 17), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		activityManager.setup(ALL_ACTIVE);

		final Graphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                     null,
		                                     null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification du for principal et de son motif de fermeture
		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final ForFiscalPrincipalPM ffp = e.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateRequisitionRadiation, ffp.getDateFin());
			Assert.assertEquals(MotifFor.CESSATION_ACTIVITE, ffp.getMotifFermeture());
		});
	}

	@Test
	public void testMotifForFaillite() throws Exception {

		final long noEntreprise = 2623L;
		final RegDate dateCreationFor = RegDate.get(2005, 2, 1);
		final RegDate datePrononceFaillite = RegDate.get(2010, 6, 2);

		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(e, dateCreationFor, "Titi", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateCreationFor, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateCreationFor, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		EntrepriseMigratorTest.addAssujettissement(e, dateCreationFor, datePrononceFaillite, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addPrononceFaillite(e, datePrononceFaillite, RegpmTypeEtatEntreprise.EN_FAILLITE, datePrononceFaillite);

		activityManager.setup(ALL_ACTIVE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// en base : le for principal doit avoir été limité à la date de radiation RC
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateCreationFor, ffp.getDateDebut());
			Assert.assertEquals(datePrononceFaillite, ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
			Assert.assertEquals(MotifFor.FAILLITE, ffp.getMotifFermeture());
		});

		// et dans les messages de suivi ?
		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertEquals(EnumSet.of(LogCategory.SUIVI,
		                               LogCategory.FORS,
		                               LogCategory.DONNEES_CIVILES_REGPM),
		                    messages.keySet());

		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(10, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Date de fin d'activité proposée (date de prononcé de faillite) : 02.06.2010.", msgs.get(2));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(3));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(4));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'un régime fiscal VD de type '01' sur la période [01.01.2009 -> ?] pour couvrir les fors de l'entreprise.", msgs.get(5));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'un régime fiscal CH de type '01' sur la période [01.01.2009 -> ?] pour couvrir les fors de l'entreprise.", msgs.get(6));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(7));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Etat 'EN_FAILLITE' migré, dès le 02.06.2010.", msgs.get(8));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : 26.23.", msgs.get(9));
		}
		{
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5586 [01.02.2005 -> 02.06.2010] généré.", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;Donnée de raison sociale migrée : sur la période [01.02.2005 -> 02.06.2010], 'Titi'.", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;Donnée de forme juridique migrée : sur la période [01.02.2005 -> 02.06.2010], SA.", msgs.get(1));
		}
	}

	@Test
	public void testListeEntrepriseAvecNumeroIdeSansIdentifiantCantonal() throws Exception {

		final long noEntreprise = 74984L;
		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(1900, 1, 1);
		final RegDate dateDebutForSecondaire = RegDate.get(1917, 11, 6);

		EntrepriseMigratorTest.addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addForSecondaire(e, dateDebutForSecondaire, null, Commune.LAUSANNE);
		e.setNumeroIDE(EntrepriseMigratorTest.buildNumeroIDE("CHE", 105833454L));

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// migration
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);
			Assert.assertNull(entreprise.getNumeroEntreprise());
		});

		// vérification des messages dans le contexte "IDE_SANS_NO_CANTONAL"
		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertTrue(messages.containsKey(LogCategory.IDE_SANS_NO_CANTONAL));
		{
			final List<String> msgs = messages.get(LogCategory.IDE_SANS_NO_CANTONAL);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;CHE105833454;;", msgs.get(0));
		}
	}

	@Test
	public void testEntrepriseAvecIdentifiantCantonalQuiNeCorrespondARien() throws Exception {

		final long noEntreprise = 74984L;
		final long noCantonalEntreprise = 42L;
		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(1900, 1, 1);
		final RegDate dateDebutForSecondaire = RegDate.get(1917, 11, 6);

		EntrepriseMigratorTest.addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addForSecondaire(e, dateDebutForSecondaire, null, Commune.LAUSANNE);
		e.setNumeroCantonal(noCantonalEntreprise);
		EntrepriseMigratorTest.addRegimeFiscalVD(e, RegDate.get(2005, 6, 12), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(e, RegDate.get(2005, 6, 12), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		organisationService.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				// rien du tout... et en particulier pas le 42...
			}
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// migration
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);
			Assert.assertNull(entreprise.getNumeroEntreprise());
		});

		// vérification des messages dans le contexte "SUIVI"
		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		{
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";;;;;;;;;;;;;Aucune donnée renvoyée par RCEnt pour cette entreprise.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(4));
		}
	}

	@Test
	public void testEntrepriseAvecIdentifiantCantonalMaisRaisonSocialeDifferente() throws Exception {

		final long noCantonalEntreprise = 42L;
		final long noCantonalEtablissementPrincipal = 43732L;
		final RegDate dateCreationEntrepriseCivile = RegDate.get(1924, 4, 1);

		final long noEntreprise = 74984L;
		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(1986, 1, 1);

		EntrepriseMigratorTest.addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addAssujettissement(e, dateDebut, null, RegpmTypeAssujettissement.LILIC);
		e.setNumeroCantonal(noCantonalEntreprise);
		e.setNumeroIDE(EntrepriseMigratorTest.buildNumeroIDE("CHE", 123456788L));
		EntrepriseMigratorTest.addRegimeFiscalVD(e, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(e, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addSiegeSuisse(e, dateDebut, Commune.GRANDSON);

		organisationService.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				final MockSiteOrganisation sitePrincipal = addSite(org, noCantonalEtablissementPrincipal, dateCreationEntrepriseCivile, new MockDonneesRegistreIDE(), new MockDonneesRC());
				sitePrincipal.changeTypeDeSite(dateCreationEntrepriseCivile, TypeDeSite.ETABLISSEMENT_PRINCIPAL);
				sitePrincipal.changeNom(dateCreationEntrepriseCivile, "Pittet Levage S.A.R.L.");
				sitePrincipal.changeFormeLegale(dateCreationEntrepriseCivile, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE);
				addNumeroIDE(org, "CHE123456788", RegDate.get(2009, 1, 1), null);
				sitePrincipal.addSiege(dateCreationEntrepriseCivile, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Grandson.getNoOFS());
			}
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// pour récupérer le numéro de l'établissement principal créé
		final Mutable<Long> noEtablissementPrincipal = new MutableObject<>();

		// migration
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);
			Assert.assertEquals((Long) noCantonalEntreprise, entreprise.getNumeroEntreprise());

			final Set<RapportEntreTiers> versObjets = entreprise.getRapportsSujet();
			Assert.assertNotNull(versObjets);
			Assert.assertEquals(1, versObjets.size());
			final RapportEntreTiers ret = versObjets.iterator().next();
			Assert.assertNotNull(ret);
			Assert.assertEquals(ActiviteEconomique.class, ret.getClass());
			noEtablissementPrincipal.setValue(ret.getObjetId());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		{
			// vérification des messages dans le contexte "SUIVI"
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(6, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" +  noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(1));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Etablissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.getValue()) + " créé en liaison avec le site civil " + noCantonalEtablissementPrincipal + ".", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Données civiles d'établissement principal présentes dès le 01.04.1924, tous les sièges ultérieurs de RegPM seront ignorés.", msgs.get(4));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(5));
		}
		// ... et dans la liste des différences
		{
			final List<String> msgs = messages.get(LogCategory.DIFFERENCES_DONNEES_CIVILES);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";Toto SA;Pittet Levage S.A.R.L.;Différentes;S.A.;N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE;Différentes;CHE123456788;CHE123456788;Identiques;COMMUNE_OU_FRACTION_VD/5561;COMMUNE_OU_FRACTION_VD/5561;Identiques;", msgs.get(0));
		}
	}

	@Test
	public void testEntrepriseAvecIdentifiantCantonalMaisSiegeDifferent() throws Exception {

		final long noCantonalEntreprise = 42L;
		final long noCantonalEtablissementPrincipal = 43732L;
		final RegDate dateCreationEntrepriseCivile = RegDate.get(1924, 4, 1);

		final long noEntreprise = 74984L;
		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(1986, 1, 1);

		EntrepriseMigratorTest.addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addAssujettissement(e, dateDebut, null, RegpmTypeAssujettissement.LILIC);
		e.setNumeroCantonal(noCantonalEntreprise);
		e.setNumeroIDE(EntrepriseMigratorTest.buildNumeroIDE("CHE", 123456788L));
		EntrepriseMigratorTest.addRegimeFiscalVD(e, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(e, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addSiegeSuisse(e, dateDebut, Commune.GRANDSON);

		organisationService.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				final MockSiteOrganisation sitePrincipal = addSite(org, noCantonalEtablissementPrincipal, dateCreationEntrepriseCivile, new MockDonneesRegistreIDE(), new MockDonneesRC());
				sitePrincipal.changeTypeDeSite(dateCreationEntrepriseCivile, TypeDeSite.ETABLISSEMENT_PRINCIPAL);
				sitePrincipal.changeNom(dateCreationEntrepriseCivile, "Toto SA");
				sitePrincipal.changeFormeLegale(dateCreationEntrepriseCivile, FormeLegale.N_0106_SOCIETE_ANONYME);
				addNumeroIDE(org, "CHE123456788", RegDate.get(2009, 1, 1), null);
				sitePrincipal.addSiege(dateCreationEntrepriseCivile, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.YverdonLesBains.getNoOFS());
			}
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// pour récupérer le numéro de l'établissement principal créé
		final Mutable<Long> noEtablissementPrincipal = new MutableObject<>();

		// migration
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);
			Assert.assertEquals((Long) noCantonalEntreprise, entreprise.getNumeroEntreprise());

			final Set<RapportEntreTiers> versObjets = entreprise.getRapportsSujet();
			Assert.assertNotNull(versObjets);
			Assert.assertEquals(1, versObjets.size());
			final RapportEntreTiers ret = versObjets.iterator().next();
			Assert.assertNotNull(ret);
			Assert.assertEquals(ActiviteEconomique.class, ret.getClass());
			noEtablissementPrincipal.setValue(ret.getObjetId());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		{
			// vérification des messages dans le contexte "SUIVI"
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(6, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" +  noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(1));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Etablissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.getValue()) + " créé en liaison avec le site civil " + noCantonalEtablissementPrincipal + ".", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Données civiles d'établissement principal présentes dès le 01.04.1924, tous les sièges ultérieurs de RegPM seront ignorés.", msgs.get(4));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(5));
		}
		// ... et dans la liste des différences
		{
			final List<String> msgs = messages.get(LogCategory.DIFFERENCES_DONNEES_CIVILES);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";Toto SA;Toto SA;Identiques;S.A.;N_0106_SOCIETE_ANONYME;Identiques;CHE123456788;CHE123456788;Identiques;COMMUNE_OU_FRACTION_VD/5561;COMMUNE_OU_FRACTION_VD/5938;Différents;", msgs.get(0));
		}
	}

	@Test
	public void testEntrepriseAvecIdentifiantCantonalEtPasseFiscal() throws Exception {

		final long noEntreprise = 74984L;
		final long noCantonalEntreprise = 42L;
		final long noCantonalEtablissementPrincipal = 12L;
		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(1986, 1, 1);
		final RegDate dateChangementNom = dateDebut.addYears(5);
		final RegDate dateChangementCapital = dateDebut.addYears(4);
		final RegDate dateChargementRCEnt = RegDate.get(2015, 8, 1);

		EntrepriseMigratorTest.addRaisonSociale(e, dateDebut, "Toto junior SARL", null, null, false);
		EntrepriseMigratorTest.addRaisonSociale(e, dateChangementNom, "Toto SA", null, null, false);
		EntrepriseMigratorTest.addRaisonSociale(e, dateChargementRCEnt, "Toto SA", "(Titi SA)", null, true);        // devrait être ignoré
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.R.L.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addFormeJuridique(e, dateChangementNom, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addCapital(e, dateDebut, 10000);
		EntrepriseMigratorTest.addCapital(e, dateChangementCapital, 200000);
		EntrepriseMigratorTest.addCapital(e, dateChargementRCEnt, 800000);      // devrait être ignoré
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addAssujettissement(e, dateDebut, null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addSiegeSuisse(e, dateDebut, Commune.ECHALLENS);
		e.setNumeroCantonal(noCantonalEntreprise);
		e.setNumeroIDE(EntrepriseMigratorTest.buildNumeroIDE("CHE", 123456788L));
		EntrepriseMigratorTest.addRegimeFiscalVD(e, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(e, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		organisationService.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				final MockSiteOrganisation site = addSite(org, noCantonalEtablissementPrincipal, dateChargementRCEnt, null, new DonneesRCBuilder()
						.addCapital(new Capital(dateChargementRCEnt, null, TypeDeCapital.CAPITAL_SOCIAL, MontantMonetaire.CHF, BigDecimal.valueOf(400000L), "répartition ??"))
						.build());
				site.changeTypeDeSite(dateChargementRCEnt, TypeDeSite.ETABLISSEMENT_PRINCIPAL);
				site.changeNom(dateChargementRCEnt, "Toto SA");
				site.changeFormeLegale(dateChargementRCEnt, FormeLegale.N_0106_SOCIETE_ANONYME);
				site.changeNumeroIDE(RegDate.get(2009, 1, 1), "CHE123456788");
			}
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// identifiant des établissements principaux générés
		final MutableLong idEtablissementPrincipal = new MutableLong();

		// données en base
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);
			Assert.assertEquals((Long) noCantonalEntreprise, entreprise.getNumeroEntreprise());

			final Map<Class, List<? extends DonneeCivileEntreprise>> map = entreprise.getDonneesCiviles().stream()
					.collect(Collectors.toMap(Object::getClass,
					                          Collections::singletonList,
					                          (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).sorted(Comparator.comparing(DonneeCivileEntreprise::getDateDebut)).collect(Collectors.toList())));
			Assert.assertNotNull(map);
			Assert.assertEquals(3, map.size());
			Assert.assertTrue(map.containsKey(RaisonSocialeFiscaleEntreprise.class));
			Assert.assertTrue(map.containsKey(FormeJuridiqueFiscaleEntreprise.class));
			Assert.assertTrue(map.containsKey(CapitalFiscalEntreprise.class));

			final List<RaisonSocialeFiscaleEntreprise> rss = (List<RaisonSocialeFiscaleEntreprise>) map.get(RaisonSocialeFiscaleEntreprise.class);
			Assert.assertNotNull(rss);
			Assert.assertEquals(2, rss.size());
			{
				final RaisonSocialeFiscaleEntreprise rs = rss.get(0);
				Assert.assertNotNull(rs);
				Assert.assertFalse(rs.isAnnule());
				Assert.assertEquals(dateDebut, rs.getDateDebut());
				Assert.assertEquals(dateChangementNom.getOneDayBefore(), rs.getDateFin());
				Assert.assertEquals("Toto junior SARL", rs.getRaisonSociale());
			}
			{
				final RaisonSocialeFiscaleEntreprise rs = rss.get(1);
				Assert.assertNotNull(rs);
				Assert.assertFalse(rs.isAnnule());
				Assert.assertEquals(dateChangementNom, rs.getDateDebut());
				Assert.assertEquals(dateChargementRCEnt.getOneDayBefore(), rs.getDateFin());
				Assert.assertEquals("Toto SA", rs.getRaisonSociale());
			}

			final List<FormeJuridiqueFiscaleEntreprise> fjs = (List<FormeJuridiqueFiscaleEntreprise>) map.get(FormeJuridiqueFiscaleEntreprise.class);
			Assert.assertNotNull(fjs);
			Assert.assertEquals(2, fjs.size());
			{
				final FormeJuridiqueFiscaleEntreprise fj = fjs.get(0);
				Assert.assertNotNull(fj);
				Assert.assertFalse(fj.isAnnule());
				Assert.assertEquals(dateDebut, fj.getDateDebut());
				Assert.assertEquals(dateChangementNom.getOneDayBefore(), fj.getDateFin());
				Assert.assertEquals(FormeJuridiqueEntreprise.SARL, fj.getFormeJuridique());
			}
			{
				final FormeJuridiqueFiscaleEntreprise fj = fjs.get(1);
				Assert.assertNotNull(fj);
				Assert.assertFalse(fj.isAnnule());
				Assert.assertEquals(dateChangementNom, fj.getDateDebut());
				Assert.assertEquals(dateChargementRCEnt.getOneDayBefore(), fj.getDateFin());
				Assert.assertEquals(FormeJuridiqueEntreprise.SA, fj.getFormeJuridique());
			}

			final List<CapitalFiscalEntreprise> capitaux = (List<CapitalFiscalEntreprise>) map.get(CapitalFiscalEntreprise.class);
			Assert.assertNotNull(capitaux);
			Assert.assertEquals(2, capitaux.size());
			{
				final CapitalFiscalEntreprise capital = capitaux.get(0);
				Assert.assertNotNull(capital);
				Assert.assertFalse(capital.isAnnule());
				Assert.assertEquals(dateDebut, capital.getDateDebut());
				Assert.assertEquals(dateChangementCapital.getOneDayBefore(), capital.getDateFin());
				Assert.assertEquals(MontantMonetaire.CHF, capital.getMontant().getMonnaie());
				Assert.assertEquals((Long) 10000L, capital.getMontant().getMontant());
			}
			{
				final CapitalFiscalEntreprise capital = capitaux.get(1);
				Assert.assertNotNull(capital);
				Assert.assertFalse(capital.isAnnule());
				Assert.assertEquals(dateChangementCapital, capital.getDateDebut());
				Assert.assertEquals(dateChargementRCEnt.getOneDayBefore(), capital.getDateFin());
				Assert.assertEquals(MontantMonetaire.CHF, capital.getMontant().getMonnaie());
				Assert.assertEquals((Long) 200000L, capital.getMontant().getMontant());
			}

			final Set<RapportEntreTiers> rapportsSujet = entreprise.getRapportsSujet();
			Assert.assertNotNull(rapportsSujet);
			Assert.assertEquals(1, rapportsSujet.size());

			final List<ActiviteEconomique> activitesEconomiques = rapportsSujet.stream()
					.filter(ret -> ret instanceof ActiviteEconomique)
					.map(ret -> (ActiviteEconomique) ret)
					.sorted(Comparator.comparing(RapportEntreTiers::getDateDebut))
					.collect(Collectors.toList());
			Assert.assertEquals(1, activitesEconomiques.size());
			{
				final ActiviteEconomique ae = activitesEconomiques.get(0);
				Assert.assertNotNull(ae);
				Assert.assertFalse(ae.isAnnule());
				Assert.assertEquals(dateDebut, ae.getDateDebut());
				Assert.assertNull(ae.getDateFin());
				Assert.assertTrue(ae.isPrincipal());
				idEtablissementPrincipal.setValue(ae.getObjetId());

				final Etablissement etb = uniregStore.getEntityFromDb(Etablissement.class, ae.getObjetId());
				Assert.assertNotNull(etb);
				Assert.assertTrue(etb.isConnuAuCivil());
				Assert.assertFalse(etb.getDomiciles().isEmpty());
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		{
			// vérification des messages dans le contexte "SUIVI"
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(8, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(1));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Etablissement principal " + FormatNumeroHelper.numeroCTBToDisplay(idEtablissementPrincipal.longValue()) + " créé en liaison avec le site civil " + noCantonalEtablissementPrincipal + ".", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Données civiles d'établissement principal présentes dès le 01.08.2015, tous les sièges ultérieurs de RegPM seront ignorés.", msgs.get(4));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Ré-utilisation de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(idEtablissementPrincipal.longValue()) + " identifié par son numéro cantonal.", msgs.get(5));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(idEtablissementPrincipal.longValue()) + " : [01.01.1986 -> 31.07.2015] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(6));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(7));
		}
		// ... et dans la liste des données civiles
		{
			final List<String> msgs = messages.get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertEquals(8, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;Données de forme juridique et/ou de raison sociale en provenance du registre civil dès le 01.08.2015 (les données ultérieures de RegPM seront ignorées).", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;Donnée de raison sociale migrée : sur la période [01.01.1986 -> 31.12.1990], 'Toto junior SARL'.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;Donnée de raison sociale migrée : sur la période [01.01.1991 -> 31.07.2015], 'Toto SA'.", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;Donnée de forme juridique migrée : sur la période [01.01.1986 -> 31.12.1990], SARL.", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;Donnée de forme juridique migrée : sur la période [01.01.1991 -> 31.07.2015], SA.", msgs.get(4));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;Les données de capital en provenance du registre civil font foi dès le 01.08.2015 (les données ultérieures de RegPM seront ignorées).", msgs.get(5));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;Donnée de capital migrée : sur la période [01.01.1986 -> 31.12.1989], 10000 CHF.", msgs.get(6));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;Donnée de capital migrée : sur la période [01.01.1990 -> 31.07.2015], 200000 CHF.", msgs.get(7));
		}
	}

	/**
	 * Cas des entreprises 17025, 63178 et 67113
	 * Les formes juridiques migrées fiscalement sont toutes SNC, mais RCEnt nous dit EI... (problématique du genre d'impôt du for principal généré)
	 */
	@Test
	public void testSncFiscaleEtEntrepriseIndividuelleCivile() throws Exception {

		final long noEntreprise = 17025L;
		final long noCantonalEntreprise = 101552826L;
		final long noCantonalEtablissementPrincipal = 101552827L;
		final RegDate dateChargementRCEnt = RegDate.get(2015, 12, 5);

		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(1986, 1, 1);

		EntrepriseMigratorTest.addRaisonSociale(e, dateDebut, "Toto", null, null, false);
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.N.C.", RegpmCategoriePersonneMorale.SP));
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addSiegeSuisse(e, dateDebut, Commune.ECHALLENS);
		e.setNumeroCantonal(noCantonalEntreprise);
		e.setNumeroIDE(EntrepriseMigratorTest.buildNumeroIDE("CHE", 123456788L));

		// mise en place civile
		organisationService.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				final MockSiteOrganisation sitePrincipal = addSite(org, noCantonalEtablissementPrincipal, dateChargementRCEnt, null, null);
				sitePrincipal.addSiege(dateChargementRCEnt, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Echallens.getNoOFS());
				sitePrincipal.changeTypeDeSite(dateChargementRCEnt, TypeDeSite.ETABLISSEMENT_PRINCIPAL);
				sitePrincipal.changeNom(dateChargementRCEnt, "Toto");
				sitePrincipal.changeFormeLegale(dateChargementRCEnt, FormeLegale.N_0101_ENTREPRISE_INDIVIDUELLE);
				sitePrincipal.changeNumeroIDE(dateChargementRCEnt, "CHE123456788");
			}
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification des données en base
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(dateDebut, ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ffp.getGenreImpot());
		});
	}

	@Test
	public void testEntrepriseEtEtablissementConnusAuCivil() throws Exception {

		final long noEntreprise = 74984L;
		final long noCantonalEntreprise = 42L;
		final long noCantonalEtablissementPrincipal = 12L;
		final long noEtablissementSecondaire = 53753L;
		final long noCantonalEtablissementSecondaire = 432L;

		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(1986, 1, 1);
		final RegDate dateChargementRCEnt = RegDate.get(2015, 8, 1);

		EntrepriseMigratorTest.addRaisonSociale(e, dateDebut, "Toto SA", null, null, false);
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addCapital(e, dateDebut, 10000);
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addAssujettissement(e, dateDebut, null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addSiegeSuisse(e, dateDebut, Commune.ECHALLENS);
		e.setNumeroCantonal(noCantonalEntreprise);
		e.setNumeroIDE(EntrepriseMigratorTest.buildNumeroIDE("CHE", 123456788L));
		EntrepriseMigratorTest.addRegimeFiscalVD(e, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(e, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		final RegpmEtablissement etb = EtablissementMigratorTest.buildEtablissement(noEtablissementSecondaire, e);
		EtablissementMigratorTest.addDomicileEtablissement(etb, dateDebut, Commune.MORGES, false);
		EtablissementMigratorTest.addEtablissementStable(etb, dateDebut, null);
		etb.setNumeroCantonal(noCantonalEtablissementSecondaire);

		organisationService.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				final MockSiteOrganisation sitePrincipal = addSite(org, noCantonalEtablissementPrincipal, dateChargementRCEnt, null, new DonneesRCBuilder()
						.addCapital(new Capital(dateChargementRCEnt, null, TypeDeCapital.CAPITAL_SOCIAL, MontantMonetaire.CHF, BigDecimal.valueOf(400000L), "répartition ??"))
						.build());
				sitePrincipal.addSiege(dateChargementRCEnt, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Echallens.getNoOFS());
				sitePrincipal.changeTypeDeSite(dateChargementRCEnt, TypeDeSite.ETABLISSEMENT_PRINCIPAL);
				sitePrincipal.changeNom(dateChargementRCEnt, "Toto SA");
				sitePrincipal.changeFormeLegale(dateChargementRCEnt, FormeLegale.N_0106_SOCIETE_ANONYME);
				sitePrincipal.changeNumeroIDE(RegDate.get(2009, 1, 1), "CHE123456788");

				final MockSiteOrganisation siteSecondaire = addSite(org, noCantonalEtablissementSecondaire, dateChargementRCEnt, null, null);
				siteSecondaire.addSiege(dateChargementRCEnt, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Morges.getNoOFS());
				siteSecondaire.changeTypeDeSite(dateChargementRCEnt, TypeDeSite.ETABLISSEMENT_SECONDAIRE);
			}
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         Collections.singletonList(etb),
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// récupération des numéros de contribuables des établissements créés
		final MutableLong idEtablissementPrincipal = new MutableLong();
		final MutableLong idEtablissementSecondaire = new MutableLong();

		// vérification des données en base
		doInUniregTransaction(true, status -> {

			// récupère les établissements dans le base de données, indexés par identifiant cantonal
			final Map<Long, Etablissement> etablissementByIdCantonal = uniregStore.getEntitiesFromDb(Etablissement.class, null).stream()
					.collect(Collectors.toMap(Etablissement::getNumeroEtablissement, Function.identity()));
			Assert.assertEquals(2, etablissementByIdCantonal.size());

			{
				final Etablissement etablissement = etablissementByIdCantonal.get(noCantonalEtablissementPrincipal);
				Assert.assertNotNull(etablissement);
				Assert.assertFalse(etablissement.isAnnule());

				final List<DomicileEtablissement> domiciles = etablissement.getSortedDomiciles(true);
				Assert.assertNotNull(domiciles);
				Assert.assertEquals(1, domiciles.size());
				{
					final DomicileEtablissement dom = domiciles.get(0);
					Assert.assertNotNull(dom);
					Assert.assertFalse(dom.isAnnule());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, dom.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), dom.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(dateDebut, dom.getDateDebut());
					Assert.assertEquals(dateChargementRCEnt.getOneDayBefore(), dom.getDateFin());
				}

				final ActiviteEconomique ae = (ActiviteEconomique) etablissement.getRapportsObjet().iterator().next();
				Assert.assertNotNull(ae);
				Assert.assertTrue(ae.isPrincipal());
				Assert.assertEquals(dateDebut, ae.getDateDebut());
				Assert.assertNull(ae.getDateFin());

				idEtablissementPrincipal.setValue(etablissement.getNumero());
			}
			{
				final Etablissement etablissement = etablissementByIdCantonal.get(noCantonalEtablissementSecondaire);
				Assert.assertNotNull(etablissement);
				Assert.assertFalse(etablissement.isAnnule());

				final List<DomicileEtablissement> domiciles = etablissement.getSortedDomiciles(true);
				Assert.assertNotNull(domiciles);
				Assert.assertEquals(1, domiciles.size());
				{
					final DomicileEtablissement dom = domiciles.get(0);
					Assert.assertNotNull(dom);
					Assert.assertFalse(dom.isAnnule());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, dom.getTypeAutoriteFiscale());
					Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), dom.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(dateDebut, dom.getDateDebut());
					Assert.assertEquals(dateChargementRCEnt.getOneDayBefore(), dom.getDateFin());
				}

				final ActiviteEconomique ae = (ActiviteEconomique) etablissement.getRapportsObjet().iterator().next();
				Assert.assertNotNull(ae);
				Assert.assertFalse(ae.isPrincipal());
				Assert.assertEquals(dateDebut, ae.getDateDebut());
				Assert.assertNull(ae.getDateFin());     // la date du rapport est donnée seulement par les dates d'établissement stable, pas par les données RCEnt // TODO ok avec ça ?

				idEtablissementSecondaire.setValue(etablissement.getNumero());
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		{
			// vérification des messages dans le contexte "SUIVI"
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(9, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";" + noEtablissementSecondaire + ";" + idEtablissementSecondaire.longValue() + ";;" + noCantonalEtablissementSecondaire + ";" + noEntreprise + ";;;;;;;;Etablissement migré : " + FormatNumeroHelper.numeroCTBToDisplay(idEtablissementSecondaire.longValue()) + ".", msgs.get(0));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Etablissement principal " + FormatNumeroHelper.numeroCTBToDisplay(idEtablissementPrincipal.longValue()) + " créé en liaison avec le site civil " + noCantonalEtablissementPrincipal + ".", msgs.get(4));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Données civiles d'établissement principal présentes dès le 01.08.2015, tous les sièges ultérieurs de RegPM seront ignorés.", msgs.get(5));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Ré-utilisation de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(idEtablissementPrincipal.longValue()) + " identifié par son numéro cantonal.", msgs.get(6));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(idEtablissementPrincipal.longValue()) + " : [01.01.1986 -> 31.07.2015] sur COMMUNE_OU_FRACTION_VD/5518.", msgs.get(7));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;CHE123456788;" + noCantonalEntreprise + ";;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(8));
		}
		{
			// vérification des messages dans le contexte "ETABLISSEMENTS"
			final List<String> msgs = messages.get(LogCategory.ETABLISSEMENTS);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + noEtablissementSecondaire + ";" + idEtablissementSecondaire.longValue() + ";;" + noCantonalEtablissementSecondaire + ";" + noEntreprise + ";;Présence de données civiles dès le 01.08.2015, tous les domiciles stables ultérieurs de RegPM seront ignorés.", msgs.get(0));
			Assert.assertEquals("INFO;" + noEtablissementSecondaire + ";" + idEtablissementSecondaire.longValue() + ";;" + noCantonalEtablissementSecondaire + ";" + noEntreprise + ";;Domicile : [01.01.1986 -> 31.07.2015] sur COMMUNE_OU_FRACTION_VD/5642.", msgs.get(1));
		}
	}

	/**
	 * [SIFISC-17342] les fors principaux ne doivent commencer qu'à la date de début du premier immeuble pour une entreprise DP avec immeuble
	 */
	@Test
	public void testEntrepriseDroitPublicAvecImmeubleDecalageForPrincipal() throws Exception {

		final long noEntreprise = 46237L;
		final RegDate dateDebutForPrincipal = RegDate.get(1960, 12, 3);
		final RegDate dateDebutImmeuble = dateDebutForPrincipal.addYears(18).addDays(53);

		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(e, dateDebutForPrincipal, "Le bien pour tous", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebutForPrincipal, EntrepriseMigratorTest.createTypeFormeJuridique("DP", RegpmCategoriePersonneMorale.APM));
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateDebutForPrincipal, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addAssujettissement(e, dateDebutForPrincipal, null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(e, dateDebutForPrincipal, null, RegpmTypeRegimeFiscal._70_ORDINAIRE_ASSOCIATION_FONDATION);
		EntrepriseMigratorTest.addRegimeFiscalCH(e, dateDebutForPrincipal, null, RegpmTypeRegimeFiscal._70_ORDINAIRE_ASSOCIATION_FONDATION);

		final RegpmImmeuble immeuble = EntrepriseMigratorTest.createImmeuble(Commune.LAUSANNE);
		EntrepriseMigratorTest.addRattachementProprietaire(e, dateDebutImmeuble, null, immeuble);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification en base (en particulier la date de début du for principal)
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final List<ForFiscalPrincipalPM> forsPrincipaux = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(forsPrincipaux);
			Assert.assertEquals(1, forsPrincipaux.size());

			final ForFiscalPrincipalPM forPrincipal = forsPrincipaux.get(0);
			Assert.assertNotNull(forPrincipal);
			Assert.assertFalse(forPrincipal.isAnnule());
			Assert.assertEquals(dateDebutImmeuble, forPrincipal.getDateDebut());
			Assert.assertEquals(MotifFor.INDETERMINE, forPrincipal.getMotifOuverture());
			Assert.assertNull(forPrincipal.getDateFin());
			Assert.assertNull(forPrincipal.getMotifFermeture());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, forPrincipal.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), forPrincipal.getNumeroOfsAutoriteFiscale());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		{
			// vérification des messages dans le contexte "SUIVI"
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(6, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(3));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(4));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(5));
		}
		{
			// vérification des messages dans le contexte "FORS"
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;Entreprise DP avec rattachement(s) propriétaire(s), on conservera donc les fors malgré la forme juridique DP.", msgs.get(0));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;La date de début de validité du for principal 1 est déplacée du 03.12.1960 au 25.01.1979 pour correspondre à la date de début du premier immeuble associé à l'entreprise DP.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5518 [25.01.1979 -> ?] généré.", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For secondaire 'immeuble' [25.01.1979 -> ?] ajouté sur la commune 5586.", msgs.get(3));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5586 (maintenant : [25.01.1979 -> ?]).", msgs.get(4));
		}
	}

	@Test
	public void testCouvertureRegimesFiscaux() throws Exception {

		final long noEntreprise = 46237L;
		final RegDate dateDebutForPrincipal = RegDate.get(2010, 5, 3);

		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(e, dateDebutForPrincipal, "Pognon SA", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebutForPrincipal, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateDebutForPrincipal, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addAssujettissement(e, dateDebutForPrincipal, null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRegimeFiscalVD(e, dateDebutForPrincipal.addMonths(2), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);   // VD -> commence plus tard que le for
//		EntrepriseMigratorTest.addRegimeFiscalCH(e, dateDebutForPrincipal, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);                // CH -> rien du tout

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification en base
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscaux();
			Assert.assertNotNull(regimesFiscaux);
			Assert.assertEquals(2, regimesFiscaux.size());

			final Map<RegimeFiscal.Portee, RegimeFiscal> mapRegimes = regimesFiscaux.stream()
					.collect(Collectors.toMap(RegimeFiscal::getPortee, Function.identity()));

			{
				final RegimeFiscal regime = mapRegimes.get(RegimeFiscal.Portee.VD);
				Assert.assertNotNull(regime);
				Assert.assertFalse(regime.isAnnule());
				Assert.assertEquals(dateDebutForPrincipal, regime.getDateDebut());
				Assert.assertNull(regime.getDateFin());
				Assert.assertEquals("01", regime.getCode());
			}
			{
				final RegimeFiscal regime = mapRegimes.get(RegimeFiscal.Portee.CH);
				Assert.assertNotNull(regime);
				Assert.assertFalse(regime.isAnnule());
				Assert.assertEquals(dateDebutForPrincipal, regime.getDateDebut());
				Assert.assertNull(regime.getDateFin());
				Assert.assertEquals("01", regime.getCode());
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		{
			// vérification des messages dans le contexte "SUIVI"
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(8, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(3));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Régime fiscal VD [03.07.2010 -> ?] de type '01' pris en compte dès le 03.05.2010 pour couvrir les fors de l'entreprise.", msgs.get(4));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'un régime fiscal CH de type '01' sur la période [03.05.2010 -> ?] pour couvrir les fors de l'entreprise.", msgs.get(5));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(6));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(7));
		}
	}

	/**
	 * Exemple des entreprises 18950 et 19012
	 */
	@Test
	public void testCouvertureRegimesFiscauxSurSncAvecImmeublesDisjoints() throws Exception {

		final long noEntreprise = 46237L;
		final RegDate dateDebutForPrincipal = RegDate.get(2010, 5, 3);
		final RegDate dateAchatImmeuble1 = RegDate.get(2011, 5, 1);
		final RegDate dateVenteImmeuble1 = RegDate.get(2011, 12, 30);
		final RegDate dateAchatImmeuble2 = RegDate.get(2012, 1, 1);     // noter le trou d'un jour sans immeuble

		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(e, dateDebutForPrincipal, "Pittet et fils", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebutForPrincipal, EntrepriseMigratorTest.createTypeFormeJuridique("S.N.C.", RegpmCategoriePersonneMorale.SP));
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateDebutForPrincipal, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addAssujettissement(e, dateDebutForPrincipal, null, RegpmTypeAssujettissement.LILIC);
		EntrepriseMigratorTest.addRattachementProprietaire(e, dateAchatImmeuble1, dateVenteImmeuble1, createImmeuble(Commune.ECHALLENS));
		EntrepriseMigratorTest.addRattachementProprietaire(e, dateAchatImmeuble2, null, createImmeuble(Commune.LAUSANNE));

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification en base
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscaux();
			Assert.assertNotNull(regimesFiscaux);
			Assert.assertEquals(2, regimesFiscaux.size());

			final Map<RegimeFiscal.Portee, RegimeFiscal> mapRegimes = regimesFiscaux.stream()
					.collect(Collectors.toMap(RegimeFiscal::getPortee, Function.identity()));

			{
				final RegimeFiscal regime = mapRegimes.get(RegimeFiscal.Portee.VD);
				Assert.assertNotNull(regime);
				Assert.assertFalse(regime.isAnnule());
				Assert.assertEquals(dateAchatImmeuble1, regime.getDateDebut());
				Assert.assertNull(regime.getDateFin());
				Assert.assertEquals("01", regime.getCode());
			}
			{
				final RegimeFiscal regime = mapRegimes.get(RegimeFiscal.Portee.CH);
				Assert.assertNotNull(regime);
				Assert.assertFalse(regime.isAnnule());
				Assert.assertEquals(dateAchatImmeuble1, regime.getDateDebut());
				Assert.assertNull(regime.getDateFin());
				Assert.assertEquals("01", regime.getCode());
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		{
			// vérification des messages dans le contexte "SUIVI"
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(8, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(2));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(3));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(4));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(5));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'un régime fiscal VD de type '01' sur la période [01.05.2011 -> ?] pour couvrir les fors de l'entreprise.", msgs.get(6));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'un régime fiscal CH de type '01' sur la période [01.05.2011 -> ?] pour couvrir les fors de l'entreprise.", msgs.get(7));
		}
	}

	/**
	 * Exemple de l'entreprise 57524
	 */
	@Test
	public void testCouvertureRegimesFiscauxAucunRegimeSourceEtDateDebutForPrincipalAdapteeEtRattrapageDeclarationSansExerciceCommercial() throws Exception {
		final long noEntreprise = 46237L;
		final RegDate dateDebutForPrincipal = RegDate.get(2010, 5, 3);

		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(e, dateDebutForPrincipal, "Pognon SA", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebutForPrincipal, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateDebutForPrincipal, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addRattachementProprietaire(e, dateDebutForPrincipal.getOneDayBefore(), null, createImmeuble(Commune.LAUSANNE));
		final RegpmAssujettissement assujettissement = EntrepriseMigratorTest.addAssujettissement(e, dateDebutForPrincipal, null, RegpmTypeAssujettissement.LILIC);

		EntrepriseMigratorTest.addDossierFiscal(e, assujettissement, 2015, RegDate.get(2016, 1, 1), RegpmModeImposition.POST);
		e.setDateBouclementFutur(RegDate.get(2016, 12, 31));

		// ajout de quelques périodes fiscales utiles
		doInUniregTransaction(false, status -> {
			addPeriodeFiscale(2015);
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification en base
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<RegimeFiscal> regimesFiscaux = entreprise.getRegimesFiscaux();
			Assert.assertNotNull(regimesFiscaux);
			Assert.assertEquals(2, regimesFiscaux.size());

			final Map<RegimeFiscal.Portee, RegimeFiscal> mapRegimes = regimesFiscaux.stream()
					.collect(Collectors.toMap(RegimeFiscal::getPortee, Function.identity()));

			{
				final RegimeFiscal regime = mapRegimes.get(RegimeFiscal.Portee.VD);
				Assert.assertNotNull(regime);
				Assert.assertFalse(regime.isAnnule());
				Assert.assertEquals(dateDebutForPrincipal.getOneDayBefore(), regime.getDateDebut());
				Assert.assertNull(regime.getDateFin());
				Assert.assertEquals("01", regime.getCode());
			}
			{
				final RegimeFiscal regime = mapRegimes.get(RegimeFiscal.Portee.CH);
				Assert.assertNotNull(regime);
				Assert.assertFalse(regime.isAnnule());
				Assert.assertEquals(dateDebutForPrincipal.getOneDayBefore(), regime.getDateDebut());
				Assert.assertNull(regime.getDateFin());
				Assert.assertEquals("01", regime.getCode());
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		{
			// vérification des messages dans le contexte "SUIVI"
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(16, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2015 pour combler l'absence d'exercice commercial dans RegPM sur la période [03.05.2010 -> 31.12.2016].", msgs.get(2));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2014 pour combler l'absence d'exercice commercial dans RegPM sur la période [03.05.2010 -> 31.12.2016].", msgs.get(3));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2013 pour combler l'absence d'exercice commercial dans RegPM sur la période [03.05.2010 -> 31.12.2016].", msgs.get(4));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2012 pour combler l'absence d'exercice commercial dans RegPM sur la période [03.05.2010 -> 31.12.2016].", msgs.get(5));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2011 pour combler l'absence d'exercice commercial dans RegPM sur la période [03.05.2010 -> 31.12.2016].", msgs.get(6));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'une date de bouclement estimée au 31.12.2010 pour combler l'absence d'exercice commercial dans RegPM sur la période [03.05.2010 -> 31.12.2016].", msgs.get(7));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Cycle de bouclements créé, applicable dès le 01.12.2010 : tous les 12 mois, à partir du premier 31.12.", msgs.get(8));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(9));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'un régime fiscal VD de type '01' sur la période [03.05.2010 -> ?] pour couvrir les fors de l'entreprise.", msgs.get(10));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Ajout d'un régime fiscal CH de type '01' sur la période [03.05.2010 -> ?] pour couvrir les fors de l'entreprise.", msgs.get(11));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(12));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(13));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Régime fiscal VD [03.05.2010 -> ?] de type '01' pris en compte dès le 02.05.2010 pour couvrir les fors de l'entreprise.", msgs.get(14));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Régime fiscal CH [03.05.2010 -> ?] de type '01' pris en compte dès le 02.05.2010 pour couvrir les fors de l'entreprise.", msgs.get(15));
		}
	}

	/**
	 * Cas de l'entreprise 38683, qui bouclait au 31.07 de chaque année, mais a annoncé fin 2015 (dans la DI 2015) que le prochain
	 * bouclement serait au 31.08.2016 (et pas 31.07 !!)
	 */
	@Test
	public void testAnnonceAlongementExerciceCommercialDansDerniereDeclaration() throws Exception {

		final long noEntreprise = 38683L;
		final RegDate dateDebutForPrincipal = RegDate.get(2013, 8, 1);

		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(e, dateDebutForPrincipal, "Pognon SA", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebutForPrincipal, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addForPrincipalSuisse(e, dateDebutForPrincipal, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		EntrepriseMigratorTest.addRegimeFiscalVD(e, dateDebutForPrincipal, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalCH(e, dateDebutForPrincipal, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		final RegpmAssujettissement assujettissement = EntrepriseMigratorTest.addAssujettissement(e, dateDebutForPrincipal, null, RegpmTypeAssujettissement.LILIC);

		final RegpmDossierFiscal df2013 = EntrepriseMigratorTest.addDossierFiscal(e, assujettissement, 2013, RegDate.get(2013, 8, 1), RegpmModeImposition.POST);
		df2013.setDateRetour(RegDate.get(2013, 10, 2));
		EntrepriseMigratorTest.addExerciceCommercial(e, df2013, RegDate.get(2012, 8, 1), RegDate.get(2013, 7, 31));

		final RegpmDossierFiscal df2014 = EntrepriseMigratorTest.addDossierFiscal(e, assujettissement, 2014, RegDate.get(2014, 8, 1), RegpmModeImposition.POST);
		df2014.setDateRetour(RegDate.get(2014, 10, 2));
		EntrepriseMigratorTest.addExerciceCommercial(e, df2014, RegDate.get(2013, 8, 1), RegDate.get(2014, 7, 31));

		final RegpmDossierFiscal df2015 = EntrepriseMigratorTest.addDossierFiscal(e, assujettissement, 2015, RegDate.get(2015, 8, 1), RegpmModeImposition.POST);
		df2015.setDateRetour(RegDate.get(2015, 10, 2));
		e.setDateBouclementFutur(RegDate.get(2016, 8, 31));
		EntrepriseMigratorTest.addExerciceCommercial(e, df2015, RegDate.get(2014, 8, 1), RegDate.get(2015, 7, 31));

		// ajout de quelques périodes fiscales utiles
		doInUniregTransaction(false, status -> {
			addPeriodeFiscale(2013);
			addPeriodeFiscale(2014);
			addPeriodeFiscale(2015);
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification en base
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<Bouclement> bouclements = entreprise.getBouclements();
			Assert.assertNotNull(bouclements);
			Assert.assertEquals(2, bouclements.size());

			final List<Bouclement> bouclementsTries = new ArrayList<>(bouclements);
			Collections.sort(bouclementsTries, Comparator.comparing(Bouclement::getDateDebut));

			{
				final Bouclement b = bouclementsTries.get(0);
				Assert.assertNotNull(b);
				Assert.assertFalse(b.isAnnule());
				Assert.assertEquals(RegDate.get(2013, 7, 1), b.getDateDebut());
				Assert.assertEquals(DayMonth.get(7, 31), b.getAncrage());
				Assert.assertEquals(12, b.getPeriodeMois());
			}
			{
				final Bouclement b = bouclementsTries.get(1);
				Assert.assertNotNull(b);
				Assert.assertFalse(b.isAnnule());
				Assert.assertEquals(RegDate.get(2016, 7, 1), b.getDateDebut());
				Assert.assertEquals(DayMonth.get(8, 31), b.getAncrage());
				Assert.assertEquals(12, b.getPeriodeMois());
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		{
			// vérification des messages dans le contexte "SUIVI"
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(8, msgs.size());
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Période [01.08.2015 -> 31.08.2016] sans exercice commercial dans RegPM reprise comme un seul exercice commercial.", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Cycle de bouclements créé, applicable dès le 01.07.2013 : tous les 12 mois, à partir du premier 31.07.", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Cycle de bouclements créé, applicable dès le 01.07.2016 : tous les 12 mois, à partir du premier 31.08.", msgs.get(4));
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Aucune date d'envoi de lettre de bienvenue trouvée malgré la présence d'assujettissement(s).", msgs.get(5));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(6));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", msgs.get(7));
		}
	}

	@Test
	public void testSocieteDirection() throws Exception {

		final long noEntreprise1 = 32321L;
		final long noEntreprise2 = 12131L;
		final long noFondsPlacement = 2312L;
		final RegDate dateDebutProprio = RegDate.get(1995, 1, 29);
		final RegDate dateDebutFonds1 = RegDate.get(2000, 5, 21);
		final RegDate dateDebutFonds2 = RegDate.get(2005, 4, 12);

		final RegpmEntreprise fonds = EntrepriseMigratorTest.buildEntreprise(noFondsPlacement);
		EntrepriseMigratorTest.addRaisonSociale(fonds, dateDebutFonds1, "Fonds Winwin", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(fonds, dateDebutFonds1, EntrepriseMigratorTest.createTypeFormeJuridique("FDS. PLAC.", RegpmCategoriePersonneMorale.APM));

		final RegpmEntreprise proprio1 = EntrepriseMigratorTest.buildEntreprise(noEntreprise1);
		EntrepriseMigratorTest.addRaisonSociale(proprio1, dateDebutProprio, "Gestionnaire de fortune 1", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(proprio1, dateDebutProprio, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));

		final RegpmEntreprise proprio2 = EntrepriseMigratorTest.buildEntreprise(noEntreprise2);
		EntrepriseMigratorTest.addRaisonSociale(proprio2, dateDebutProprio, "Gestionnaire de fortune 2", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(proprio2, dateDebutProprio, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));

		EntrepriseMigratorTest.addSocieteDirection(proprio1, fonds, dateDebutFonds1);
		EntrepriseMigratorTest.addSocieteDirection(proprio2, fonds, dateDebutFonds2);

		final MockGraphe graphe = new MockGraphe(Arrays.asList(proprio1, proprio2, fonds),
		                                         null,
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertNotNull(messages);

		// on va juste regarder les liens générés
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertNotNull(msgs);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;PROPRIETAIRE_FONDS_PLACEMENT;2000-05-21;2005-04-11;" + noEntreprise1 + ";;;" + noEntreprise1 + ";" + noFondsPlacement + ";;;" + noFondsPlacement + ";", msgs.get(0));
			Assert.assertEquals("INFO;PROPRIETAIRE_FONDS_PLACEMENT;2005-04-12;;" + noEntreprise2 + ";;;" + noEntreprise2 + ";" + noFondsPlacement + ";;;" + noFondsPlacement + ";", msgs.get(1));
		}

		// et en base ?
		doInUniregTransaction(true, status -> {
			final Entreprise fondsPlacement = uniregStore.getEntityFromDb(Entreprise.class, noFondsPlacement);
			Assert.assertNotNull(fondsPlacement);

			final Set<RapportEntreTiers> rets = fondsPlacement.getRapportsObjet();
			Assert.assertNotNull(rets);
			Assert.assertEquals(2, rets.size());

			final List<SocieteDirection> directions = rets.stream()
					.filter(ret -> ret instanceof SocieteDirection)
					.map(ret -> (SocieteDirection) ret)
					.sorted(DateRangeComparator::compareRanges)
					.collect(Collectors.toList());
			Assert.assertEquals(2, directions.size());
			{
				final SocieteDirection sd = directions.get(0);
				Assert.assertFalse(sd.isAnnule());
				Assert.assertEquals(RegDate.get(2000, 5, 21), sd.getDateDebut());
				Assert.assertEquals(RegDate.get(2005, 4, 11), sd.getDateFin());
				Assert.assertEquals((Long) noEntreprise1, sd.getSujetId());
			}
			{
				final SocieteDirection sd = directions.get(1);
				Assert.assertFalse(sd.isAnnule());
				Assert.assertEquals(RegDate.get(2005, 4, 12), sd.getDateDebut());
				Assert.assertNull(sd.getDateFin());
				Assert.assertEquals((Long) noEntreprise2, sd.getSujetId());
			}
		});
	}

	@Test
	public void testAdministrateurSocieteImmobiliere() throws Exception {

		final long noEntreprise = 12131L;
		final long noIndividu = 2312L;
		final RegDate dateDebutEntreprise = RegDate.get(2004, 2, 3);
		final RegDate dateDebutAdministration = RegDate.get(2005, 6, 12);
		final RegDate dateNaissanceAdministrateur = RegDate.get(1954, 3, 25);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(entreprise, dateDebutEntreprise, "Immob-génial", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, dateDebutEntreprise, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, RegpmTypeRegimeFiscal._35_SOCIETE_ORDINAIRE_SIAL);

		final RegpmIndividu administrateur = IndividuMigratorTest.buildBaseIndividu(noIndividu, "Natuzzi", "Kanapé", dateNaissanceAdministrateur, Sexe.MASCULIN);

		// administrateur actif, président terminé
		EntrepriseMigratorTest.addAdministrateur(entreprise, administrateur, RegpmFonction.ADMINISTRATEUR, dateDebutAdministration, null, false);
		EntrepriseMigratorTest.addAdministrateur(entreprise, administrateur, RegpmFonction.PRESIDENT, dateDebutAdministration, RegDate.get(2010, 12, 31), false);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         null,
		                                         Collections.singletonList(administrateur));

		activityManager.setup(ALL_ACTIVE);

		// mise en place fiscale de l'administrateur
		final long ppId = doInUniregTransaction(false, status -> {
			final PersonnePhysique pp = new PersonnePhysique(Boolean.FALSE);
			pp.setNom("Natuzzi");
			pp.setPrenomUsuel("Kanapé");
			pp.setDateNaissance(dateNaissanceAdministrateur);
			pp.setSexe(Sexe.MASCULIN);
			return (Long) getUniregSessionFactory().getCurrentSession().save(pp);
		});

		doInUniregTransaction(true, status -> {
			final PersonnePhysique pp = (PersonnePhysique) migrationContexte.getTiersDAO().get(ppId);
			migrationContexte.getNonHabitantIndex().index(pp);
		});

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(e);

			final Set<RapportEntreTiers> rets = e.getRapportsSujet();
			Assert.assertNotNull(rets);
			Assert.assertEquals(1, rets.size());

			final RapportEntreTiers ret = rets.iterator().next();
			Assert.assertNotNull(ret);
			Assert.assertEquals(AdministrationEntreprise.class, ret.getClass());
			Assert.assertFalse(ret.isAnnule());
			Assert.assertEquals(dateDebutAdministration, ret.getDateDebut());
			Assert.assertNull(ret.getDateFin());
			Assert.assertFalse(((AdministrationEntreprise) ret).isPresident());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertNotNull(messages);

		// on va juste regarder les liens générés
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertNotNull(msgs);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;ENTREPRISE_ADMINISTRATEUR;2005-06-12;;" + noEntreprise + ";;;" + noEntreprise + ";;;" + noIndividu + ";" + ppId + ";", msgs.get(0));
		}
	}

	@Test
	public void testAdministrateurPresidentSocieteImmobiliere() throws Exception {

		final long noEntreprise = 12131L;
		final long noIndividu = 2312L;
		final RegDate dateDebutEntreprise = RegDate.get(2004, 2, 3);
		final RegDate dateDebutAdministration = RegDate.get(2005, 6, 12);
		final RegDate dateNaissanceAdministrateur = RegDate.get(1954, 3, 25);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(entreprise, dateDebutEntreprise, "Immob-génial", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, dateDebutEntreprise, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, RegpmTypeRegimeFiscal._35_SOCIETE_ORDINAIRE_SIAL);

		final RegpmIndividu administrateur = IndividuMigratorTest.buildBaseIndividu(noIndividu, "Natuzzi", "Kanapé", dateNaissanceAdministrateur, Sexe.MASCULIN);

		// administrateur actif, président actif
		EntrepriseMigratorTest.addAdministrateur(entreprise, administrateur, RegpmFonction.ADMINISTRATEUR, dateDebutAdministration, null, false);
		EntrepriseMigratorTest.addAdministrateur(entreprise, administrateur, RegpmFonction.PRESIDENT, dateDebutAdministration, null, false);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         null,
		                                         Collections.singletonList(administrateur));

		activityManager.setup(ALL_ACTIVE);

		// mise en place fiscale de l'administrateur
		final long ppId = doInUniregTransaction(false, status -> {
			final PersonnePhysique pp = new PersonnePhysique(Boolean.FALSE);
			pp.setNom("Natuzzi");
			pp.setPrenomUsuel("Kanapé");
			pp.setDateNaissance(dateNaissanceAdministrateur);
			pp.setSexe(Sexe.MASCULIN);
			return (Long) getUniregSessionFactory().getCurrentSession().save(pp);
		});

		doInUniregTransaction(true, status -> {
			final PersonnePhysique pp = (PersonnePhysique) migrationContexte.getTiersDAO().get(ppId);
			migrationContexte.getNonHabitantIndex().index(pp);
		});

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(e);

			final Set<RapportEntreTiers> rets = e.getRapportsSujet();
			Assert.assertNotNull(rets);
			Assert.assertEquals(1, rets.size());

			final RapportEntreTiers ret = rets.iterator().next();
			Assert.assertNotNull(ret);
			Assert.assertEquals(AdministrationEntreprise.class, ret.getClass());
			Assert.assertFalse(ret.isAnnule());
			Assert.assertEquals(dateDebutAdministration, ret.getDateDebut());
			Assert.assertNull(ret.getDateFin());
			Assert.assertTrue(((AdministrationEntreprise) ret).isPresident());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertNotNull(messages);

		// on va juste regarder les liens générés
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertNotNull(msgs);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;ENTREPRISE_ADMINISTRATEUR;2005-06-12;;" + noEntreprise + ";;;" + noEntreprise + ";;;" + noIndividu + ";" + ppId + ";", msgs.get(0));
		}
	}

	@Test
	public void testPresidentNonAdministrateurSocieteImmobiliere() throws Exception {

		final long noEntreprise = 12131L;
		final long noIndividu = 2312L;
		final RegDate dateDebutEntreprise = RegDate.get(2004, 2, 3);
		final RegDate dateDebutAdministration = RegDate.get(2005, 6, 12);
		final RegDate dateNaissanceAdministrateur = RegDate.get(1954, 3, 25);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(entreprise, dateDebutEntreprise, "Immob-génial", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, dateDebutEntreprise, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, RegpmTypeRegimeFiscal._35_SOCIETE_ORDINAIRE_SIAL);

		final RegpmIndividu administrateur = IndividuMigratorTest.buildBaseIndividu(noIndividu, "Natuzzi", "Kanapé", dateNaissanceAdministrateur, Sexe.MASCULIN);

		// administrateur actif, président actif
		EntrepriseMigratorTest.addAdministrateur(entreprise, administrateur, RegpmFonction.ADMINISTRATEUR, dateDebutAdministration, RegDate.get(2010, 5, 2), false);
		EntrepriseMigratorTest.addAdministrateur(entreprise, administrateur, RegpmFonction.PRESIDENT, dateDebutAdministration, null, false);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         null,
		                                         Collections.singletonList(administrateur));

		activityManager.setup(ALL_ACTIVE);

		// mise en place fiscale de l'administrateur
		final long ppId = doInUniregTransaction(false, status -> {
			final PersonnePhysique pp = new PersonnePhysique(Boolean.FALSE);
			pp.setNom("Natuzzi");
			pp.setPrenomUsuel("Kanapé");
			pp.setDateNaissance(dateNaissanceAdministrateur);
			pp.setSexe(Sexe.MASCULIN);
			return (Long) getUniregSessionFactory().getCurrentSession().save(pp);
		});

		doInUniregTransaction(true, status -> {
			final PersonnePhysique pp = (PersonnePhysique) migrationContexte.getTiersDAO().get(ppId);
			migrationContexte.getNonHabitantIndex().index(pp);
		});

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(e);

			final Set<RapportEntreTiers> rets = e.getRapportsSujet();
			Assert.assertNotNull(rets);
			Assert.assertEquals(0, rets.size());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertNotNull(messages);

		// on va juste regarder les liens générés : aucun
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertNull(msgs);
		}
	}

	@Test
	public void testAdministrateurPresidentSocieteNonImmobiliere() throws Exception {

		final long noEntreprise = 12131L;
		final long noIndividu = 2312L;
		final RegDate dateDebutEntreprise = RegDate.get(2004, 2, 3);
		final RegDate dateDebutAdministration = RegDate.get(2005, 6, 12);
		final RegDate dateNaissanceAdministrateur = RegDate.get(1954, 3, 25);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(entreprise, dateDebutEntreprise, "Immob-génial", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, dateDebutEntreprise, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, dateDebutEntreprise, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, dateDebutEntreprise, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		final RegpmIndividu administrateur = IndividuMigratorTest.buildBaseIndividu(noIndividu, "Natuzzi", "Kanapé", dateNaissanceAdministrateur, Sexe.MASCULIN);

		// administrateur actif, président actif
		EntrepriseMigratorTest.addAdministrateur(entreprise, administrateur, RegpmFonction.ADMINISTRATEUR, dateDebutAdministration, null, false);
		EntrepriseMigratorTest.addAdministrateur(entreprise, administrateur, RegpmFonction.PRESIDENT, dateDebutAdministration, null, false);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         null,
		                                         Collections.singletonList(administrateur));

		activityManager.setup(ALL_ACTIVE);

		// mise en place fiscale de l'administrateur
		final long ppId = doInUniregTransaction(false, status -> {
			final PersonnePhysique pp = new PersonnePhysique(Boolean.FALSE);
			pp.setNom("Natuzzi");
			pp.setPrenomUsuel("Kanapé");
			pp.setDateNaissance(dateNaissanceAdministrateur);
			pp.setSexe(Sexe.MASCULIN);
			return (Long) getUniregSessionFactory().getCurrentSession().save(pp);
		});

		doInUniregTransaction(true, status -> {
			final PersonnePhysique pp = (PersonnePhysique) migrationContexte.getTiersDAO().get(ppId);
			migrationContexte.getNonHabitantIndex().index(pp);
		});

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(e);

			final Set<RapportEntreTiers> rets = e.getRapportsSujet();
			Assert.assertNotNull(rets);
			Assert.assertEquals(0, rets.size());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertNotNull(messages);

		// on va juste regarder les liens générés : aucun
		{
			final List<String> msgs = messages.get(LogCategory.RAPPORTS_ENTRE_TIERS);
			Assert.assertNull(msgs);
		}
	}

	@Test
	public void testMigrationAdressesAvecRCEnt() throws Exception {

		final long noEntreprise = 35623L;
		final long noCantonalEntreprise = 272462L;
		final long noCantonalEtablissementPrincipal = 3627472L;
		final RegDate dateDebut = RegDate.get(1995, 6, 12);
		final RegDate dateSnapshotRCEnt = RegDate.get(2016, 2, 5);

		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(e, dateDebut, "Tarlatata SARL", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.R.L.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addRegimeFiscalCH(e, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalVD(e, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addAdresse(e, RegpmTypeAdresseEntreprise.COURRIER, dateDebut.addMonths(4), null, null, null, "1bis", Rue.LONGEMALLE_RENENS, null);
		EntrepriseMigratorTest.addAdresse(e, RegpmTypeAdresseEntreprise.SIEGE, dateDebut, null, null, null, "1", Rue.LONGEMALLE_RENENS, null);
		e.setNumeroCantonal(noCantonalEntreprise);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		organisationService.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				final MockSiteOrganisation sitePrincipal = addSite(org, noCantonalEtablissementPrincipal, dateSnapshotRCEnt, new MockDonneesRegistreIDE(), new MockDonneesRC());
				sitePrincipal.changeTypeDeSite(dateSnapshotRCEnt, TypeDeSite.ETABLISSEMENT_PRINCIPAL);
				sitePrincipal.changeNom(dateSnapshotRCEnt, "Tarlatata S.A.R.L.");
				sitePrincipal.changeFormeLegale(dateSnapshotRCEnt, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE);
				org.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Renens.QuatorzeAvril, null, dateSnapshotRCEnt, null));
				org.addAdresse(new MockAdresse(TypeAdresseCivil.COURRIER, null, "Avenue de Longemalle", MockLocalite.Renens, dateSnapshotRCEnt, null));
				addNumeroIDE(org, "CHE123456788", RegDate.get(2009, 1, 1), null);
			}
		});

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification du contenu de la base de données (par rapport aux adresses, surtout...)
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<AdresseTiers> adresses = entreprise.getAdressesTiers();
			Assert.assertNotNull(adresses);
			Assert.assertEquals(2, adresses.size());

			final Map<TypeAdresseTiers, AdresseTiers> mapAdresses = adresses.stream()
					.collect(Collectors.toMap(AdresseTiers::getUsage, Function.identity()));
			{
				final AdresseTiers adresse = mapAdresses.get(TypeAdresseTiers.COURRIER);
				Assert.assertNotNull(adresse);
				Assert.assertFalse(adresse.isAnnule());
				Assert.assertEquals(AdresseSuisse.class, adresse.getClass());

				final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
				Assert.assertNotNull(adresseSuisse.getNumeroRue());
				Assert.assertEquals(MockLocalite.Renens.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
				Assert.assertEquals("1bis", adresseSuisse.getNumeroMaison());
				Assert.assertTrue(adresseSuisse.isPermanente());
				Assert.assertEquals(dateDebut.addMonths(4), adresseSuisse.getDateDebut());
				Assert.assertNull(adresseSuisse.getDateFin());
			}
			{
				final AdresseTiers adresse = mapAdresses.get(TypeAdresseTiers.POURSUITE);
				Assert.assertNotNull(adresse);
				Assert.assertFalse(adresse.isAnnule());
				Assert.assertEquals(AdresseSuisse.class, adresse.getClass());

				final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
				Assert.assertNotNull(adresseSuisse.getNumeroRue());
				Assert.assertEquals(MockLocalite.Renens.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
				Assert.assertEquals("1", adresseSuisse.getNumeroMaison());
				Assert.assertFalse(adresseSuisse.isPermanente());
				Assert.assertEquals(dateDebut, adresseSuisse.getDateDebut());
				Assert.assertEquals(dateSnapshotRCEnt.getOneDayBefore(), adresseSuisse.getDateFin());
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertNotNull(messages);

		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertNotNull(msgs);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";;;;;;;;;;;;;Avenue de Longemalle;1bis;;Renens (VD);;Adresse 'Avenue de Longemalle' à 'Renens (VD)' mappée sur l'estrid 1134510.", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";;;;;;;;;;;;;Avenue de Longemalle;1;;Renens (VD);;Adresse 'Avenue de Longemalle' à 'Renens (VD)' mappée sur l'estrid 1134510.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";;;;;;;;;;;;;;;;;;Adresse fiscale de siège migrée (en tant qu'adresse de poursuite) sur la période [12.06.1995 -> 04.02.2016].", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";;;;;;;;;;;;;;;;;;Adresse fiscale de courrier migrée sur la période [12.10.1995 -> ?].", msgs.get(3));
		}

		// on va regarder la liste des adresses permanentes
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES_PERMANENTES);
			Assert.assertNotNull(msgs);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";1995-10-12;;COURRIER;;Avenue de Longemalle;1bis;;Renens VD;8100;", msgs.get(0));
		}
	}

	/**
	 * [SIFISC-18360] Surcharge des adresses de représentation
	 */
	@Test
	public void testMigrationAdressesAvecRCEntEtRoleMandataire() throws Exception {

		final long noEntreprise = 35623L;
		final long noMandant = 6856L;
		final long noCantonalEntreprise = 272462L;
		final long noCantonalEtablissementPrincipal = 3627472L;
		final RegDate dateDebut = RegDate.get(1995, 6, 12);
		final RegDate dateSnapshotRCEnt = RegDate.get(2016, 2, 5);

		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(e, dateDebut, "Tarlatata SARL", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(e, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.R.L.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addRegimeFiscalCH(e, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalVD(e, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addAdresse(e, RegpmTypeAdresseEntreprise.COURRIER, dateDebut.addMonths(4), null, null, null, "1bis", Rue.LONGEMALLE_RENENS, null);
		EntrepriseMigratorTest.addAdresse(e, RegpmTypeAdresseEntreprise.SIEGE, dateDebut, null, null, null, "1", Rue.LONGEMALLE_RENENS, null);
		EntrepriseMigratorTest.addMandat(EntrepriseMigratorTest.buildEntreprise(noMandant), e, RegpmTypeMandat.GENERAL, null, dateDebut, null);
		e.setNumeroCantonal(noCantonalEntreprise);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		organisationService.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				final MockSiteOrganisation sitePrincipal = addSite(org, noCantonalEtablissementPrincipal, dateSnapshotRCEnt, new MockDonneesRegistreIDE(), new MockDonneesRC());
				sitePrincipal.changeTypeDeSite(dateSnapshotRCEnt, TypeDeSite.ETABLISSEMENT_PRINCIPAL);
				sitePrincipal.changeNom(dateSnapshotRCEnt, "Tarlatata S.A.R.L.");
				sitePrincipal.changeFormeLegale(dateSnapshotRCEnt, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE);
				org.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Renens.QuatorzeAvril, null, dateSnapshotRCEnt, null));
				org.addAdresse(new MockAdresse(TypeAdresseCivil.COURRIER, null, "Avenue de Longemalle", MockLocalite.Renens, dateSnapshotRCEnt, null));
				addNumeroIDE(org, "CHE123456788", RegDate.get(2009, 1, 1), null);
			}
		});

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification du contenu de la base de données (par rapport aux adresses, surtout...)
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<AdresseTiers> adresses = entreprise.getAdressesTiers();
			Assert.assertNotNull(adresses);
			Assert.assertEquals(3, adresses.size());

			final Map<TypeAdresseTiers, AdresseTiers> mapAdresses = adresses.stream()
					.collect(Collectors.toMap(AdresseTiers::getUsage, Function.identity()));
			{
				final AdresseTiers adresse = mapAdresses.get(TypeAdresseTiers.COURRIER);
				Assert.assertNotNull(adresse);
				Assert.assertFalse(adresse.isAnnule());
				Assert.assertEquals(AdresseSuisse.class, adresse.getClass());

				final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
				Assert.assertNotNull(adresseSuisse.getNumeroRue());
				Assert.assertEquals(MockLocalite.Renens.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
				Assert.assertEquals("1bis", adresseSuisse.getNumeroMaison());
				Assert.assertTrue(adresseSuisse.isPermanente());
				Assert.assertEquals(dateDebut.addMonths(4), adresseSuisse.getDateDebut());
				Assert.assertNull(adresseSuisse.getDateFin());
			}
			{
				final AdresseTiers adresse = mapAdresses.get(TypeAdresseTiers.REPRESENTATION);
				Assert.assertNotNull(adresse);
				Assert.assertFalse(adresse.isAnnule());
				Assert.assertEquals(AdresseSuisse.class, adresse.getClass());

				final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
				Assert.assertNotNull(adresseSuisse.getNumeroRue());
				Assert.assertEquals(MockLocalite.Renens.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
				Assert.assertEquals("1bis", adresseSuisse.getNumeroMaison());
				Assert.assertTrue(adresseSuisse.isPermanente());
				Assert.assertEquals(dateDebut.addMonths(4), adresseSuisse.getDateDebut());
				Assert.assertNull(adresseSuisse.getDateFin());
			}
			{
				final AdresseTiers adresse = mapAdresses.get(TypeAdresseTiers.POURSUITE);
				Assert.assertNotNull(adresse);
				Assert.assertFalse(adresse.isAnnule());
				Assert.assertEquals(AdresseSuisse.class, adresse.getClass());

				final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
				Assert.assertNotNull(adresseSuisse.getNumeroRue());
				Assert.assertEquals(MockLocalite.Renens.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
				Assert.assertEquals("1", adresseSuisse.getNumeroMaison());
				Assert.assertFalse(adresseSuisse.isPermanente());
				Assert.assertEquals(dateDebut, adresseSuisse.getDateDebut());
				Assert.assertEquals(dateSnapshotRCEnt.getOneDayBefore(), adresseSuisse.getDateFin());
			}
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		Assert.assertNotNull(messages);

		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertNotNull(msgs);
			Assert.assertEquals(5, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";;;;;;;;;;;;;Avenue de Longemalle;1bis;;Renens (VD);;Adresse 'Avenue de Longemalle' à 'Renens (VD)' mappée sur l'estrid 1134510.", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";;;;;;;;;;;;;Avenue de Longemalle;1;;Renens (VD);;Adresse 'Avenue de Longemalle' à 'Renens (VD)' mappée sur l'estrid 1134510.", msgs.get(1));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";;;;;;;;;;;;;;;;;;Adresse fiscale de siège migrée (en tant qu'adresse de poursuite) sur la période [12.06.1995 -> 04.02.2016].", msgs.get(2));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";;;;;;;;;;;;;;;;;;Adresse fiscale de courrier migrée sur la période [12.10.1995 -> ?].", msgs.get(3));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";;;;;;;;;;;;;;;;;;Adresse fiscale de représentation migrée sur la période [12.10.1995 -> ?].", msgs.get(4));
		}

		// on va regarder la liste des adresses permanentes
		{
			final List<String> msgs = messages.get(LogCategory.ADRESSES_PERMANENTES);
			Assert.assertNotNull(msgs);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";1995-10-12;;COURRIER;;Avenue de Longemalle;1bis;;Renens VD;8100;", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;" + noCantonalEntreprise + ";1995-10-12;;REPRESENTATION;;Avenue de Longemalle;1bis;;Renens VD;8100;", msgs.get(1));
		}
	}

	/**
	 * [SIFISC-18034] Cas d'un mandataire établissement directement lié à une personne physique
	 */
	@Test
	public void testMandataireActiviteIndependante() throws Exception {

		final long noIndividu = 234672L;
		final long noEntrepriseMandante = 32132L;
		final long noEtablissementMandataire = 432128L;
		final RegDate debutMandat = RegDate.get(1993, 5, 1);
		final RegDate finMandat = RegDate.get(2015, 8, 12);
		final RegDate dateDebut = RegDate.get(1990, 1, 1);

		final RegpmIndividu individu = IndividuMigratorTest.buildBaseIndividu(noIndividu, "Bollomittet", "Alphonse", RegDate.get(1954, 5, 12), Sexe.MASCULIN);
		final RegpmEtablissement etablissement = EtablissementMigratorTest.buildEtablissement(noEtablissementMandataire, individu);
		etablissement.setRaisonSociale1("Bollomittet consulting");
		etablissement.setRue(Rue.LONGEMALLE_RENENS);
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntrepriseMandante);
		EntrepriseMigratorTest.addRaisonSociale(entreprise, dateDebut, "Turlututu SA", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addMandat(entreprise, etablissement, RegpmTypeMandat.GENERAL, null, debutMandat, finMandat);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         Collections.singletonList(individu));

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification de la présence d'une adresse mandataire en base
		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, noEntrepriseMandante);
			Assert.assertNotNull(e);

			// pas de personne physique ni d'établissement
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertEquals(0, etablissements.size());
			final List<PersonnePhysique> personnesPhysiques = uniregStore.getEntitiesFromDb(PersonnePhysique.class, null);
			Assert.assertEquals(0, personnesPhysiques.size());

			// adresses mandataire -> 1
			final Set<AdresseMandataire> adresses = e.getAdressesMandataires();
			Assert.assertNotNull(adresses);
			Assert.assertEquals(1, adresses.size());

			final AdresseMandataire adresse = adresses.iterator().next();
			Assert.assertNotNull(adresse);
			Assert.assertFalse(adresse.isAnnule());
			Assert.assertEquals("Bollomittet consulting", adresse.getNomDestinataire());
			Assert.assertEquals(debutMandat, adresse.getDateDebut());
			Assert.assertEquals(finMandat, adresse.getDateFin());
			Assert.assertEquals(AdresseMandataireSuisse.class, adresse.getClass());

			final AdresseMandataireSuisse adresseSuisse = (AdresseMandataireSuisse) adresse;
			Assert.assertEquals((Integer) 1134510, adresseSuisse.getNumeroRue());
			Assert.assertEquals((Integer) LocalitePostale.RENENS.getNoOrdreP().intValue(), adresseSuisse.getNumeroOrdrePoste());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		{
			// vérification des messages dans le contexte "SUIVI"
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("WARN;" + noEntrepriseMandante + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni for principal.", msgs.get(2));
			Assert.assertEquals("WARN;" + noEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(3));
			Assert.assertEquals("WARN;" + noEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(4));
			Assert.assertEquals("INFO;" + noEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntrepriseMandante) + ".", msgs.get(5));
			Assert.assertEquals("INFO;" + noEntrepriseMandante + ";Active;;;" + noEtablissementMandataire + ";;;;;" + noIndividu + ";;;;;;;Mandat vers l'établissement 'activité indépendante' migré en tant que simple adresse mandataire sur la période [01.05.1993 -> 12.08.2015].", msgs.get(6));
		}
		{
			// vérification des messages dans le contexte "ADRESSES"
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + noEntrepriseMandante + ";Active;;;" + noEtablissementMandataire + ";;;;;" + noIndividu + ";;;;;;;Avenue de Longemalle;;;Renens (VD);8100;Adresse 'Avenue de Longemalle' à 'Renens (VD)' mappée sur l'estrid 1134510.", msgs.get(0));
		}
		{
			// vérification des messages dans le contexte "ETABLISSEMENTS"
			final List<String> msgs = messages.get(LogCategory.ETABLISSEMENTS);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("WARN;" + noEtablissementMandataire + ";;;;;" + noIndividu + ";Etablissement avec un lien vers un individu, donc non-migré en tant qu'établissement (au mieux en tant qu'adresses de mandataire).", msgs.get(0));
		}
	}

	/**
	 * [SIFISC-18034] Cas d'une entreprise dont le mandataire est un individu PM non repris pour lui-même (parce que non-reconnu dans RCPers/Unireg)
	 */
	@Test
	public void testMandataireIndividuNonMigre() throws Exception {
		final long noEntrepriseMandante = 32132L;
		final long noIndividuMandataire = Integer.MAX_VALUE;        // n'existe nulle-part avec ce numéro... ne sera donc pas migré
		final RegDate debutMandat = RegDate.get(1993, 5, 1);
		final RegDate finMandat = RegDate.get(2015, 8, 12);
		final RegDate dateDebut = RegDate.get(1990, 1, 1);

		final RegpmIndividu individu = IndividuMigratorTest.buildBaseIndividu(noIndividuMandataire, "Bollomittet", "Alphonse", RegDate.get(1954, 5, 12), Sexe.MASCULIN);
		IndividuMigratorTest.addAdresse(individu, RegpmTypeAdresseIndividu.COURRIER, dateDebut, null, "Avenue de Longemalle", "1bis", LocalitePostale.RENENS);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntrepriseMandante);
		EntrepriseMigratorTest.addRaisonSociale(entreprise, dateDebut, "Turlututu SA", null, null, true);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addMandat(entreprise, individu, RegpmTypeMandat.GENERAL, null, debutMandat, finMandat);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         null,
		                                         Collections.singletonList(individu));

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification de la présence d'une adresse mandataire en base
		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, noEntrepriseMandante);
			Assert.assertNotNull(e);

			// pas de personne physique ni d'établissement
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertEquals(0, etablissements.size());
			final List<PersonnePhysique> personnesPhysiques = uniregStore.getEntitiesFromDb(PersonnePhysique.class, null);
			Assert.assertEquals(0, personnesPhysiques.size());

			// adresses mandataire -> 1
			final Set<AdresseMandataire> adresses = e.getAdressesMandataires();
			Assert.assertNotNull(adresses);
			Assert.assertEquals(1, adresses.size());

			final AdresseMandataire adresse = adresses.iterator().next();
			Assert.assertNotNull(adresse);
			Assert.assertFalse(adresse.isAnnule());
			Assert.assertEquals("Alphonse Bollomittet", adresse.getNomDestinataire());
			Assert.assertEquals(debutMandat, adresse.getDateDebut());
			Assert.assertEquals(finMandat, adresse.getDateFin());
			Assert.assertEquals(AdresseMandataireSuisse.class, adresse.getClass());

			final AdresseMandataireSuisse adresseSuisse = (AdresseMandataireSuisse) adresse;
			Assert.assertEquals((Integer) 1134510, adresseSuisse.getNumeroRue());
			Assert.assertEquals("1bis", adresseSuisse.getNumeroMaison());
			Assert.assertEquals((Integer) LocalitePostale.RENENS.getNoOrdreP().intValue(), adresseSuisse.getNumeroOrdrePoste());
		});

		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		{
			// vérification des messages dans le contexte "SUIVI"
			final List<String> msgs = messages.get(LogCategory.SUIVI);
			Assert.assertEquals(7, msgs.size());
			Assert.assertEquals("WARN;" + noEntrepriseMandante + ";Active;;;;;;;;;;;;;;;L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", msgs.get(0));
			Assert.assertEquals("ERROR;" + noEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Pas de numéro cantonal assigné sur l'entreprise, pas de lien vers le civil.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni for principal.", msgs.get(2));
			Assert.assertEquals("WARN;" + noEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Entreprise sans exercice commercial ni date de bouclement futur.", msgs.get(3));
			Assert.assertEquals("WARN;" + noEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Pas de siège associé dans les données fiscales, pas d'établissement principal créé à partir des données fiscales.", msgs.get(4));
			Assert.assertEquals("INFO;" + noEntrepriseMandante + ";Active;;;;;;;;;;;;;;;Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntrepriseMandante) + ".", msgs.get(5));
			Assert.assertEquals("INFO;" + noEntrepriseMandante + ";Active;;;;;;;;;" + noIndividuMandataire + ";;Bollomittet;Alphonse;MASCULIN;1954-05-12;Mandat vers l'individu migré en tant que simple adresse mandataire sur la période [01.05.1993 -> 12.08.2015].", msgs.get(6));
		}
		{
			// vérification des messages dans le contexte "ADRESSES"
			final List<String> msgs = messages.get(LogCategory.ADRESSES);
			Assert.assertEquals(1, msgs.size());
			Assert.assertEquals("INFO;" + noEntrepriseMandante + ";Active;;;;;;;;;" + noIndividuMandataire + ";;Bollomittet;Alphonse;MASCULIN;1954-05-12;Avenue de Longemalle;1bis;;Renens (VD);;Adresse 'Avenue de Longemalle' à 'Renens (VD)' mappée sur l'estrid 1134510.", msgs.get(0));
		}
		{
			final List<String> msgs = messages.get(LogCategory.INDIVIDUS_PM);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("WARN;" + noIndividuMandataire + ";;Bollomittet;Alphonse;MASCULIN;1954-05-12;L'individu RCPers " + noIndividuMandataire + " ne peut être renvoyé (Personne 'CT.VD.RCPERS/" + noIndividuMandataire + "' introuvable).", msgs.get(0));
			Assert.assertEquals("INFO;" + noIndividuMandataire + ";;Bollomittet;Alphonse;MASCULIN;1954-05-12;Aucun résultat dans RCPers pour le nom (Bollomittet), prénom (Alphonse), sexe (MASCULIN) et date de naissance (12.05.1954).", msgs.get(1));
			Assert.assertEquals("INFO;" + noIndividuMandataire + ";;Bollomittet;Alphonse;MASCULIN;1954-05-12;Aucun non-habitant trouvé dans Unireg avec ces nom (Bollomittet), prénom (Alphonse), sexe (MASCULIN) et date de naissance (12.05.1954).", msgs.get(2));
			Assert.assertEquals("ERROR;" + noIndividuMandataire + ";;Bollomittet;Alphonse;MASCULIN;1954-05-12;Individu non migré car aucune correspondance univoque n'a pu être trouvée avec une personne physique existante dans Unireg.", msgs.get(3));
		}
	}

	/**
	 * [SIFISC-17164] les fors principaux de RegPM ont été ignorés car l'entreprise était SP et PM/APM au cours de son existence
	 * mais nous sommes également en présence d'établissements stables, donc de fors secondaires... et on ne veut pas de for principal 'pays inconnu'...
	 */
	@Test
	public void testGenerationForPrincipalBoucheTrouSurEntrepriseSPetPM() throws Exception {

		final long noEntreprise = 46232L;
		final long noEtablissement = 3236L;
		final RegDate dateDebut = RegDate.get(2000, 3, 1);
		final RegDate dateDebutPM = RegDate.get(2005, 6, 1);
		final RegDate dateDebutEtablissement = dateDebutPM.addMonths(-3);
		final RegDate dateFinEtablissement = RegDate.get(2015, 10, 3);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(noEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(entreprise, dateDebut, "Notre petite entreprise", "devenue grande", null, true);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.N.C.", RegpmCategoriePersonneMorale.SP));
		EntrepriseMigratorTest.addFormeJuridique(entreprise, dateDebutPM, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.R.L.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.BALE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, dateDebutPM, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, dateDebutPM, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addAssujettissement(entreprise, dateDebutEtablissement, dateFinEtablissement, RegpmTypeAssujettissement.LILIC);
		entreprise.setDateBouclementFutur(RegDate.get(2015, 12, 31));

		final RegpmEtablissement etablissement = EtablissementMigratorTest.buildEtablissement(noEtablissement, entreprise);
		EtablissementMigratorTest.addEtablissementStable(etablissement, dateDebutEtablissement, dateFinEtablissement);
		EtablissementMigratorTest.addDomicileEtablissement(etablissement, dateDebutPM, Commune.ECHALLENS, false);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification des fors fiscaux de l'entreprise migrée
		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(e);

			final ForsParType fors = e.getForsParType(true);
			Assert.assertNotNull(fors);

			Assert.assertEquals(0, fors.principauxPP.size());
			Assert.assertEquals(0, fors.dpis.size());
			Assert.assertEquals(0, fors.autresImpots.size());
			Assert.assertEquals(0, fors.autreElementImpot.size());

			// le for secondaire activité, c'est clair...
			final List<ForFiscalSecondaire> ffss = fors.secondaires;
			Assert.assertEquals(1, ffss.size());
			final ForFiscalSecondaire fs = ffss.get(0);
			Assert.assertNotNull(fs);
			Assert.assertFalse(fs.isAnnule());
			Assert.assertEquals(dateDebutEtablissement, fs.getDateDebut());
			Assert.assertEquals(dateFinEtablissement, fs.getDateFin());
			Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, fs.getMotifOuverture());
			Assert.assertEquals(MotifFor.FIN_EXPLOITATION, fs.getMotifFermeture());
			Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, fs.getGenreImpot());
			Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, fs.getMotifRattachement());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, fs.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), fs.getNumeroOfsAutoriteFiscale());

			// et les fors principaux, dans tout ça ?
			final List<ForFiscalPrincipalPM> ffps = fors.principauxPM;
			Assert.assertEquals(1, ffps.size());
			final ForFiscalPrincipalPM fp = ffps.get(0);
			Assert.assertNotNull(fp);
			Assert.assertFalse(fp.isAnnule());
			Assert.assertEquals(dateDebutEtablissement, fp.getDateDebut());
			Assert.assertEquals(dateFinEtablissement, fp.getDateFin());
			Assert.assertNull(fp.getMotifOuverture());          // c'est le premier for principal, et il est HC... on peut donc rester à null
			Assert.assertEquals(MotifFor.CESSATION_ACTIVITE, fp.getMotifFermeture());
			Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, fp.getGenreImpot());
			Assert.assertEquals(MotifRattachement.DOMICILE, fp.getMotifRattachement());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, fp.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.BALE.getNoOfs(), fp.getNumeroOfsAutoriteFiscale());
		});

		// vérification des messages de log
		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		{
			// vérification des messages dans le contexte "FORS"
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(4, msgs.size());
			Assert.assertEquals("ERROR;" + noEntreprise + ";Active;;;For fiscal principal 1 du 01.03.2000 non-migré car l'entreprise a été SP et PM/APM au cours de son existence.", msgs.get(0));
			Assert.assertEquals("INFO;" + noEntreprise + ";Active;;;For secondaire 'activité' [01.03.2005 -> 03.10.2015] ajouté sur la commune 5518.", msgs.get(1));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;Il n'y avait pas de fors secondaires sur la commune OFS 5518 (maintenant : [01.03.2005 -> 03.10.2015]).", msgs.get(2));
			Assert.assertEquals("WARN;" + noEntreprise + ";Active;;;For principal ForFiscalPrincipalPM [01.03.2005 -> 03.10.2015] sur COMMUNE_HC/2701 généré (d'après les données RegPM précédemment ignorées) pour couvrir les fors secondaires.", msgs.get(3));
		}
	}

	/**
	 * [SIFISC-17356] les périodes de stabilité d'établissement d'un jour seulement doivent être ignorées et transcrite en un for secondaire annulé
	 */
	@Test
	public void testEtablissementStableUnJour() throws Exception {

		final long idEntreprise = 4623L;
		final long idEtablissement = 43623L;
		final RegDate dateDebut = RegDate.get(2005, 7, 22);
		final RegDate dateJourEtablissementStable = RegDate.get(2006, 8, 13);

		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addRaisonSociale(entreprise, dateDebut, "Notre petite entreprise", "devenue grande", null, true);
		EntrepriseMigratorTest.addFormeJuridique(entreprise, dateDebut, EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		EntrepriseMigratorTest.addRegimeFiscalCH(entreprise, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addRegimeFiscalVD(entreprise, dateDebut, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		EntrepriseMigratorTest.addAssujettissement(entreprise, dateDebut, null, RegpmTypeAssujettissement.LILIC);
		entreprise.setDateBouclementFutur(RegDate.get(2015, 12, 31));

		final RegpmEtablissement etablissement = EtablissementMigratorTest.buildEtablissement(idEtablissement, entreprise);
		EtablissementMigratorTest.addEtablissementStable(etablissement, dateJourEtablissementStable, dateJourEtablissementStable);
		EtablissementMigratorTest.addDomicileEtablissement(etablissement, dateJourEtablissementStable, Commune.ECHALLENS, false);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(entreprise),
		                                         Collections.singletonList(etablissement),
		                                         null);

		activityManager.setup(ALL_ACTIVE);

		final LoggedMessages lms = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(lms);

		// vérification de la présence du for secondaire annulé
		doInUniregTransaction(true, status -> {
			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);
			Assert.assertNotNull(e);

			final List<ForFiscal> fors = e.getForsFiscauxSorted();
			Assert.assertNotNull(fors);
			Assert.assertEquals(2, fors.size());        // le for principal + le for secondaire annulé

			final Map<Class<?>, ForFiscal> mapFors = fors.stream()
					.collect(Collectors.toMap(ForFiscal::getClass, Function.identity()));
			Assert.assertTrue(mapFors.containsKey(ForFiscalPrincipalPM.class));
			Assert.assertTrue(mapFors.containsKey(ForFiscalSecondaire.class));
			{
				final ForFiscalPrincipalPM ffp = (ForFiscalPrincipalPM) mapFors.get(ForFiscalPrincipalPM.class);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(dateDebut, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getMotifFermeture());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffp.getGenreImpot());
				Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
			}
			{
				final ForFiscalSecondaire ffs = (ForFiscalSecondaire) mapFors.get(ForFiscalSecondaire.class);
				Assert.assertNotNull(ffs);
				Assert.assertTrue(ffs.isAnnule());
				Assert.assertEquals(dateJourEtablissementStable, ffs.getDateDebut());
				Assert.assertEquals(dateJourEtablissementStable, ffs.getDateFin());
				Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffs.getMotifOuverture());
				Assert.assertEquals(MotifFor.FIN_EXPLOITATION, ffs.getMotifFermeture());
				Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffs.getGenreImpot());
				Assert.assertEquals(MotifRattachement.ETABLISSEMENT_STABLE, ffs.getMotifRattachement());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), ffs.getNumeroOfsAutoriteFiscale());
			}
		});

		// vérification des messages de log
		final Map<LogCategory, List<String>> messages = buildTextualMessages(lms);
		{
			// vérification des messages dans le contexte "ETABLISSEMENTS"
			final List<String> msgs = messages.get(LogCategory.ETABLISSEMENTS);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("WARN;" + idEtablissement + ";;;;" + idEntreprise + ";;Période d'établissement stable d'un jour ignorée au 13.08.2006.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEtablissement + ";;;;" + idEntreprise + ";;Etablissement ignoré car sans établissement stable ni rôle de mandataire.", msgs.get(1));
		}
		{
			// vérification des messages dans le contexte "FORS"
			final List<String> msgs = messages.get(LogCategory.FORS);
			Assert.assertEquals(2, msgs.size());
			Assert.assertEquals("INFO;" + idEntreprise + ";Active;;;For principal COMMUNE_OU_FRACTION_VD/5586 [22.07.2005 -> ?] généré.", msgs.get(0));
			Assert.assertEquals("WARN;" + idEntreprise + ";Active;;;Création d'un for secondaire annulé ForFiscalSecondaire [13.08.2006 -> 13.08.2006] sur COMMUNE_OU_FRACTION_VD/5518 pour garder une trace d'un établissement stable d'un jour ignoré par ailleurs (établissement " + idEtablissement + ").", msgs.get(1));
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

		// l'activity manager doit être le bon, avec la véritable implémentation, histoire d'être cohérent avec ce qui se passe en vrai pendant la migration
		final ActivityManagerImpl activityManager = new ActivityManagerImpl(null, new DatesParticulieres(RegDate.get(2015, 1, 1),
		                                                                                                 RegDate.get(1900, 1, 1),
		                                                                                                 RegDate.get(2008, 1, 1)));
		this.activityManager.setup(activityManager);

		// utilisation du véritable service rcent
		organisationService.setUp(getBean(ServiceOrganisationRaw.class, "serviceOrganisationRCEnt"));

		// lancement de la migration du graphe (de la même façon, en ce qui concerne la gestion des exception, que ce qui est fait dans le MigrationWorker)
		LoggedMessages mr;
		try {
			mr = grapheMigrator.migrate(graphe);
			Assert.assertNotNull(mr);
		}
		catch (MigrationException e) {
			final Long[] idsEntreprises = graphe.getEntreprises().keySet().toArray(new Long[graphe.getEntreprises().size()]);
			final MessageLoggedElement elt = new MessageLoggedElement(LogLevel.ERROR,
			                                                          String.format("Les entreprises %s n'ont pas pu être migrées : %s",
			                                                                        Arrays.toString(idsEntreprises),
			                                                                        MigrationWorker.dump(e)));
			mr = LoggedMessages.singleton(LogCategory.EXCEPTIONS, elt.resolve());
		}

		// dump sur la sortie standard
		final String summary = mr.toString();
		System.out.println(summary);

		// on ouvre une session hibernate pour vérifier visuellement le contenu
		doInUniregTransaction(true, status ->  {
			final List<Entreprise> entreprises = uniregStore.getEntitiesFromDb(Entreprise.class, null);
			Assert.assertNotNull(entreprises);
		});
	}
}
