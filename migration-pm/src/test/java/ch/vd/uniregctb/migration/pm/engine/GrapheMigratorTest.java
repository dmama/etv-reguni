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
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.ValidationInterceptor;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
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
}
