package ch.vd.uniregctb.migration.pm.engine;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationMessage;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.MigrationResultMessageProvider;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.migration.pm.rcent.service.RCEntService;
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

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();

		uniregStore = getBean(UniregStore.class, "uniregStore");

		final BouclementService bouclementService = getBean(BouclementService.class, "bouclementService");
		final RCEntService rcEntService = getBean(RCEntService.class, "rcEntService");
		final AdresseHelper adresseHelper = getBean(AdresseHelper.class, "adresseHelper");
		final TiersDAO tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		final RcPersClient rcpersClient = getBean(RcPersClient.class, "rcpersClient");
		final NonHabitantIndex nonHabitantIndex = getBean(NonHabitantIndex.class, "nonHabitantIndex");
		final ValidationInterceptor validationInterceptor = getBean(ValidationInterceptor.class, "validationInterceptor");

		grapheMigrator = new GrapheMigrator();
		grapheMigrator.setEntrepriseMigrator(new EntrepriseMigrator(uniregStore, bouclementService, rcEntService, adresseHelper));
		grapheMigrator.setEtablissementMigrator(new EtablissementMigrator(uniregStore, rcEntService, adresseHelper));
		grapheMigrator.setIndividuMigrator(new IndividuMigrator(uniregStore, tiersDAO, rcpersClient, nonHabitantIndex));
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
	public void testRemplissageRapportsEntreTiersDansTransaction() throws Exception {

		final long idEntrepriseMandante = 42L;
		final long idEntrepriseMandataire = 131L;
		final long idEtablissementMandataire = 3562L;
		final RegpmEntreprise mandant = EntrepriseMigratorTest.buildEntreprise(idEntrepriseMandante);

		final RegpmEntreprise entrepriseMandataire = EntrepriseMigratorTest.buildEntreprise(idEntrepriseMandataire);
		final RegpmEtablissement mandataire = EtablissementMigratorTest.buildEtablissement(idEtablissementMandataire, entrepriseMandataire);
		EntrepriseMigratorTest.addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, null, RegDate.get(2000, 1, 1), RegDate.get(2006, 12, 31));
		EntrepriseMigratorTest.addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, null, RegDate.get(2010, 1, 1), null);

		final Graphe graphe = new Graphe();
		graphe.register(mandant);
		graphe.register(entrepriseMandataire);
		graphe.register(mandataire);

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
		EtablissementMigratorTest.addDomicileEtablissement(mandataire, RegDate.get(1995, 1, 1), ECHALLENS, false);
		EtablissementMigratorTest.addEtablissementStable(mandataire, RegDate.get(1995, 1, 1), null);

		EntrepriseMigratorTest.addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, null, RegDate.get(2000, 1, 1), RegDate.get(2006, 12, 31));
		EntrepriseMigratorTest.addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, null, RegDate.get(2010, 1, 1), null);

		final Graphe graphe = new Graphe();
		graphe.register(mandant);
		graphe.register(entrepriseMandataire);
		graphe.register(mandataire);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// migration terminée -> vérification en base !
		doInUniregTransaction(true, status -> {

			// entreprises : 2
			final List<Entreprise> entreprises = uniregStore.getEntitiesFromDb(Entreprise.class, null);
			Assert.assertNotNull(entreprises);
			Assert.assertEquals(2, entreprises.size());
			final Map<Long, Entreprise> entrepriseMap = entreprises.stream().collect(Collectors.toMap(Entreprise::getId, Function.identity()));
			Assert.assertTrue(entrepriseMap.containsKey(idEntrepriseMandante));
			Assert.assertTrue(entrepriseMap.containsKey(idEntrepriseMandataire));

			// établissements : 1
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etb = etablissements.get(0);
			Assert.assertEquals(0, etb.getRapportsSujet().size());

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
	}

	@Test
	public void testMigrationEtablissementsMultiples() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, BALE);       // un for principal de base

		// les établissements
		final long idEtablissement1 = 235612L;
		final RegpmEtablissement etablissement1 = EtablissementMigratorTest.buildEtablissement(idEtablissement1, entreprise);
		etablissement1.setEnseigne("Le chat qui fume");
		EtablissementMigratorTest.addDomicileEtablissement(etablissement1, RegDate.get(1999, 5, 12), MORGES, false);
		EtablissementMigratorTest.addDomicileEtablissement(etablissement1, RegDate.get(2003, 1, 27), LAUSANNE, false);
		EtablissementMigratorTest.addEtablissementStable(etablissement1, RegDate.get(1999, 5, 12), RegDate.get(2006, 10, 31));

		final long idEtablissement2 = 4367324L;
		final RegpmEtablissement etablissement2 = EtablissementMigratorTest.buildEtablissement(idEtablissement2, entreprise);
		etablissement2.setEnseigne("Le chien qui pête");
		EtablissementMigratorTest.addDomicileEtablissement(etablissement2, RegDate.get(2002, 7, 14), LAUSANNE, false);
		EtablissementMigratorTest.addDomicileEtablissement(etablissement2, RegDate.get(2004, 3, 22), ECHALLENS, false);
		EtablissementMigratorTest.addEtablissementStable(etablissement2, RegDate.get(2002, 7, 14), RegDate.get(2010, 11, 25));

		final Graphe graphe = new Graphe();
		graphe.register(entreprise);
		graphe.register(etablissement1);
		graphe.register(etablissement2);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// vérification du résultat
		doInUniregTransaction(true, status -> {

			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);       // c'est le même identifiant dans RegPM et dans Unireg
			Assert.assertNotNull(e);

			// on vérifie d'abord qu'il y a bien deux établissements liés
			final Set<RapportEntreTiers> rapports = e.getRapportsSujet();
			Assert.assertNotNull(rapports);
			Assert.assertEquals(2, rapports.size());
			rapports.stream().filter(r -> !(r instanceof ActiviteEconomique)).findAny().ifPresent(r -> Assert.fail("Rapport " + r + " trouvé là où seuls des rapports d'activité économiques étaient attendus"));
			final List<ActiviteEconomique> activitesEconomiques = rapports.stream()
					.map(r -> (ActiviteEconomique) r)
					.sorted(Comparator.comparing(ActiviteEconomique::getDateDebut))
					.collect(Collectors.toList());

			final Long idEtb1;
			{
				final ActiviteEconomique ae = activitesEconomiques.get(0);
				Assert.assertNotNull(ae);
				Assert.assertEquals(RegDate.get(1999, 5, 12), ae.getDateDebut());
				Assert.assertEquals(RegDate.get(2006, 10, 31), ae.getDateFin());
				Assert.assertEquals((Long) idEntreprise, ae.getSujetId());

				idEtb1 = ae.getObjetId();
				Assert.assertNotNull(idEtb1);
			}
			final Long idEtb2;
			{
				final ActiviteEconomique ae = activitesEconomiques.get(1);
				Assert.assertNotNull(ae);
				Assert.assertEquals(RegDate.get(2002, 7, 14), ae.getDateDebut());
				Assert.assertEquals(RegDate.get(2010, 11, 25), ae.getDateFin());
				Assert.assertEquals((Long) idEntreprise, ae.getSujetId());

				idEtb2 = ae.getObjetId();
				Assert.assertNotNull(idEtb2);
			}
			Assert.assertNotEquals(idEtb1, idEtb2);

			// vérification qu'il n'y a bien que ces deux établissement-là en base
			final List<Etablissement> allEtablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(allEtablissements);
			Assert.assertEquals(2, allEtablissements.size());

			// vérification des domiciles de ces établissements
			{
				final Etablissement etb = uniregStore.getEntityFromDb(Etablissement.class, idEtb1);
				Assert.assertNotNull(etb);
				Assert.assertEquals("Le chat qui fume", etb.getEnseigne());

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
				final Etablissement etb = uniregStore.getEntityFromDb(Etablissement.class, idEtb2);
				Assert.assertNotNull(etb);
				Assert.assertEquals("Le chien qui pête", etb.getEnseigne());

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
	}

	@Test
	public void testCalculForsSecondairesActiviteEtImmeuble() throws Exception {

		final long idEntreprise = 42L;
		final RegpmEntreprise entreprise = EntrepriseMigratorTest.buildEntreprise(idEntreprise);
		EntrepriseMigratorTest.addForPrincipalSuisse(entreprise, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, BALE);       // un for principal de base

		// les établissements
		final long idEtablissement1 = 235612L;
		final RegpmEtablissement etablissement1 = EtablissementMigratorTest.buildEtablissement(idEtablissement1, entreprise);
		etablissement1.setEnseigne("Le chat qui fume");
		EtablissementMigratorTest.addDomicileEtablissement(etablissement1, RegDate.get(1999, 5, 12), MORGES, false);
		EtablissementMigratorTest.addDomicileEtablissement(etablissement1, RegDate.get(2003, 1, 27), LAUSANNE, false);
		EtablissementMigratorTest.addEtablissementStable(etablissement1, RegDate.get(1999, 5, 12), RegDate.get(2006, 10, 31));

		final long idEtablissement2 = 4367324L;
		final RegpmEtablissement etablissement2 = EtablissementMigratorTest.buildEtablissement(idEtablissement2, entreprise);
		etablissement2.setEnseigne("Le chien qui pête");
		EtablissementMigratorTest.addDomicileEtablissement(etablissement2, RegDate.get(2002, 7, 14), LAUSANNE, false);
		EtablissementMigratorTest.addDomicileEtablissement(etablissement2, RegDate.get(2004, 3, 22), ECHALLENS, false);
		EtablissementMigratorTest.addEtablissementStable(etablissement2, RegDate.get(2002, 7, 14), RegDate.get(2010, 11, 25));

		// les immeubles
		final RegpmImmeuble immeuble1 = createImmeuble(ECHALLENS);
		EntrepriseMigratorTest.addRattachementProprietaire(entreprise, RegDate.get(2006, 5, 1), RegDate.get(2010, 12, 31), immeuble1);

		final RegpmImmeuble immeuble2 = createImmeuble(ECHALLENS);
		final RegpmGroupeProprietaire groupe2 = createGroupeProprietaire("Zoo", RegpmTypeGroupeProprietaire.CONSORTIUM_SOCIETE_SIMPLE, RegDate.get(2000, 1, 1), null);
		EntrepriseMigratorTest.addAppartenanceGroupeProprietaire(entreprise, groupe2, RegDate.get(2004, 5, 29), RegDate.get(2009, 12, 21), false);
		EntrepriseMigratorTest.addRattachementProprietaire(groupe2, RegDate.get(2004, 7, 1), null, immeuble2);

		final Graphe graphe = new Graphe();
		graphe.register(entreprise);
		graphe.register(etablissement1);
		graphe.register(etablissement2);

		final MigrationResultMessageProvider mr = grapheMigrator.migrate(graphe);
		Assert.assertNotNull(mr);

		// vérification du résultat
		doInUniregTransaction(true, status -> {

			final Entreprise e = uniregStore.getEntityFromDb(Entreprise.class, idEntreprise);       // c'est le même identifiant dans RegPM et dans Unireg
			Assert.assertNotNull(e);

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
	}
}
