package ch.vd.uniregctb.migration.pm.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.adapter.rcent.service.RCEntAdapter;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.MigrationResultCollector;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.mapping.IdMapper;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAppartenanceGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntity;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmExerciceCommercial;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmImmeuble;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmMotifEnvoi;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRattachementProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscalCH;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscalVD;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeRegimeFiscal;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.TypeTiers;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeRegimeFiscal;

public class EntrepriseMigratorTest extends AbstractEntityMigratorTest {

	private EntrepriseMigrator migrator;
	private UniregStore uniregStore;

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();

		final ActivityManager activityManager = entreprise -> true;         // tout le monde est actif dans ces tests

		uniregStore = getBean(UniregStore.class, "uniregStore");
		migrator = new EntrepriseMigrator(
				uniregStore,
				activityManager,
				getBean(BouclementService.class, "bouclementService"),
				getBean(RCEntAdapter.class, "rcEntAdapter"),
				getBean(AdresseHelper.class, "adresseHelper")
		);
	}

	/**
	 * Construction d'une entreprise vide
	 * @param id identifiant
	 * @return une entreprise relativement vide...
	 */
	static RegpmEntreprise buildEntreprise(long id) {
		final RegpmEntreprise entreprise = new RegpmEntreprise();
		entreprise.setId(id);
		assignMutationVisa(entreprise, REGPM_VISA, REGPM_MODIF);

		// initialisation des collections à des collections vides tout comme on les trouverait avec une entité
		// extraite de la base de données
		entreprise.setAdresses(new HashSet<>());
		entreprise.setAllegementsFiscaux(new HashSet<>());
		entreprise.setAppartenancesGroupeProprietaire(new HashSet<>());
		entreprise.setAssociesSC(new HashSet<>());
		entreprise.setAssujettissements(new TreeSet<>());
		entreprise.setCapitaux(new TreeSet<>());
		entreprise.setDossiersFiscaux(new TreeSet<>());
		entreprise.setEtablissements(new HashSet<>());
		entreprise.setEtatsEntreprise(new TreeSet<>());
		entreprise.setExercicesCommerciaux(new TreeSet<>());
		entreprise.setFormesJuridiques(new TreeSet<>());
		entreprise.setForsPrincipaux(new TreeSet<>());
		entreprise.setForsSecondaires(new HashSet<>());
		entreprise.setFusionsApres(new HashSet<>());
		entreprise.setFusionsAvant(new HashSet<>());
		entreprise.setInscriptionsRC(new TreeSet<>());
		entreprise.setMandataires(new HashSet<>());
		entreprise.setQuestionnairesSNC(new TreeSet<>());
		entreprise.setRadiationsRC(new TreeSet<>());
		entreprise.setRaisonsSociales(new TreeSet<>());
		entreprise.setRattachementsProprietaires(new HashSet<>());
		entreprise.setRegimesFiscauxCH(new TreeSet<>());
		entreprise.setRegimesFiscauxVD(new TreeSet<>());
		entreprise.setSieges(new TreeSet<>());

		return entreprise;
	}

	static RegpmAssujettissement addAssujettissement(RegpmEntreprise entreprise, RegDate dateDebut, RegDate dateFin, RegpmTypeAssujettissement type) {
		final RegpmAssujettissement a = new RegpmAssujettissement();
		assignMutationVisa(a, REGPM_VISA, REGPM_MODIF);
		a.setDateDebut(dateDebut);
		a.setDateFin(dateFin);
		a.setType(type);
		a.setId(ID_GENERATOR.next());
		entreprise.getAssujettissements().add(a);
		return a;
	}

	static RegpmDossierFiscal addDossierFiscal(RegpmEntreprise entreprise, RegpmAssujettissement assujettissement, int pf, RegDate dateEnvoi) {
		final RegpmDossierFiscal df = new RegpmDossierFiscal();
		df.setId(new RegpmDossierFiscal.PK(computeNewSeqNo(entreprise.getDossiersFiscaux(), d -> d.getId().getSeqNo()), assujettissement.getId()));
		assignMutationVisa(df, REGPM_VISA, REGPM_MODIF);
		df.setAssujettissement(assujettissement);
		df.setDateEnvoi(dateEnvoi);
		df.setDelaiRetour(dateEnvoi.addDays(225));
		df.setDemandesDelai(new TreeSet<>());
		df.setEtat(RegpmTypeEtatDossierFiscal.ENVOYE);
		df.setMotifEnvoi(RegpmMotifEnvoi.FIN_EXERCICE);

		df.setNoParAnnee(computeNewSeqNo(entreprise.getDossiersFiscaux().stream().filter(d -> d.getPf() == pf).collect(Collectors.toList()),
		                                 RegpmDossierFiscal::getNoParAnnee));
		df.setPf(pf);
		entreprise.getDossiersFiscaux().add(df);
		return df;
	}

	static RegpmExerciceCommercial addExerciceCommercial(RegpmEntreprise entreprise, RegpmDossierFiscal dossierFiscal, RegDate dateDebut, RegDate dateFin) {
		final RegpmExerciceCommercial ex = new RegpmExerciceCommercial();
		ex.setId(new RegpmExerciceCommercial.PK(computeNewSeqNo(entreprise.getExercicesCommerciaux(), x -> x.getId().getSeqNo()), entreprise.getId()));
		assignMutationVisa(ex, REGPM_VISA, REGPM_MODIF);
		ex.setDateDebut(dateDebut);
		ex.setDateFin(dateFin);
		ex.setDossierFiscal(dossierFiscal);
		entreprise.getExercicesCommerciaux().add(ex);
		return ex;
	}

	static RegpmMandat addMandat(RegpmEntreprise mandant, RegpmEntity mandataire, RegpmTypeMandat type, String noCCP, RegDate dateDebut, RegDate dateFin) {
		final RegpmMandat mandat = new RegpmMandat();
		mandat.setId(new RegpmMandat.PK(computeNewSeqNo(mandant.getMandataires(), x -> x.getId().getNoSequence()), mandant.getId()));
		assignMutationVisa(mandat, REGPM_VISA, REGPM_MODIF);
		mandat.setNoCCP(noCCP);
		mandat.setType(type);
		mandat.setDateAttribution(dateDebut);
		mandat.setDateResiliation(dateFin);
		if (mandataire instanceof RegpmIndividu) {
			mandat.setMandataireIndividu((RegpmIndividu) mandataire);
		}
		else if (mandataire instanceof RegpmEtablissement) {
			mandat.setMandataireEtablissement((RegpmEtablissement) mandataire);
		}
		else if (mandataire instanceof RegpmEntreprise) {
			mandat.setMandataireEntreprise((RegpmEntreprise) mandataire);
		}
		else if (mandataire != null) {
			throw new IllegalArgumentException("Le mandataire doit être soit un individu, soit un établissement, soit une entreprise... (trouvé " + mandataire.getClass().getSimpleName() + ")");
		}

		mandant.getMandataires().add(mandat);
		return mandat;
	}

	static RegpmRattachementProprietaire addRattachementProprietaire(RegpmEntreprise entreprise, RegDate dateDebut, RegDate dateFin, RegpmImmeuble immeuble) {
		final RegpmRattachementProprietaire rrp = new RegpmRattachementProprietaire();
		rrp.setId(ID_GENERATOR.next());
		assignMutationVisa(rrp, REGPM_VISA, REGPM_MODIF);
		rrp.setDateDebut(dateDebut);
		rrp.setDateFin(dateFin);
		rrp.setImmeuble(immeuble);
		entreprise.getRattachementsProprietaires().add(rrp);
		return rrp;
	}

	static RegpmRattachementProprietaire addRattachementProprietaire(RegpmGroupeProprietaire groupe, RegDate dateDebut, RegDate dateFin, RegpmImmeuble immeuble) {
		final RegpmRattachementProprietaire rrp = new RegpmRattachementProprietaire();
		rrp.setId(ID_GENERATOR.next());
		assignMutationVisa(rrp, REGPM_VISA, REGPM_MODIF);
		rrp.setDateDebut(dateDebut);
		rrp.setDateFin(dateFin);
		rrp.setImmeuble(immeuble);
		groupe.getRattachementsProprietaires().add(rrp);
		return rrp;
	}

	static RegpmAppartenanceGroupeProprietaire addAppartenanceGroupeProprietaire(RegpmEntreprise entreprise, RegpmGroupeProprietaire groupe, RegDate dateDebut, RegDate dateFin, boolean leader) {
		final RegpmAppartenanceGroupeProprietaire ragp = new RegpmAppartenanceGroupeProprietaire();
		ragp.setId(new RegpmAppartenanceGroupeProprietaire.PK(NO_SEQUENCE_GENERATOR.next(), groupe.getId()));
		ragp.setDateDebut(dateDebut);
		ragp.setDateFin(dateFin);
		ragp.setGroupeProprietaire(groupe);
		ragp.setLeader(leader);
		entreprise.getAppartenancesGroupeProprietaire().add(ragp);
		return ragp;
	}

	static RegpmRegimeFiscalCH addRegimeFiscalCH(RegpmEntreprise entreprise, RegDate dateDebut, RegDate dateAnnulation, RegpmTypeRegimeFiscal type) {
		final RegpmRegimeFiscalCH rf = new RegpmRegimeFiscalCH();
		rf.setId(new RegpmRegimeFiscalCH.PK(computeNewSeqNo(entreprise.getRegimesFiscauxCH(), x -> x.getId().getSeqNo()), entreprise.getId()));
		assignMutationVisa(rf, REGPM_VISA, REGPM_MODIF);
		rf.setDateDebut(dateDebut);
		rf.setDateAnnulation(dateAnnulation);
		rf.setType(type);
		entreprise.getRegimesFiscauxCH().add(rf);
		return rf;
	}

	static RegpmRegimeFiscalVD addRegimeFiscalVD(RegpmEntreprise entreprise, RegDate dateDebut, RegDate dateAnnulation, RegpmTypeRegimeFiscal type) {
		final RegpmRegimeFiscalVD rf = new RegpmRegimeFiscalVD();
		rf.setId(new RegpmRegimeFiscalVD.PK(computeNewSeqNo(entreprise.getRegimesFiscauxVD(), x -> x.getId().getSeqNo()), entreprise.getId()));
		assignMutationVisa(rf, REGPM_VISA, REGPM_MODIF);
		rf.setDateDebut(dateDebut);
		rf.setDateAnnulation(dateAnnulation);
		rf.setType(type);
		entreprise.getRegimesFiscauxVD().add(rf);
		return rf;
	}

	private static RegpmForPrincipal addForPrincipal(RegpmEntreprise entreprise, RegDate dateDebut, RegpmTypeForPrincipal type, @Nullable RegpmCommune commune, @Nullable Integer noOfsPays) {
		final RegpmForPrincipal ffp = new RegpmForPrincipal();
		ffp.setId(new RegpmForPrincipal.PK(computeNewSeqNo(entreprise.getForsPrincipaux(), x -> x.getId().getSeqNo()), entreprise.getId()));
		assignMutationVisa(ffp, REGPM_VISA, REGPM_MODIF);
		ffp.setCommune(commune);
		ffp.setOfsPays(noOfsPays);
		ffp.setDateValidite(dateDebut);
		ffp.setType(type);
		entreprise.getForsPrincipaux().add(ffp);
		return ffp;
	}

	static RegpmForPrincipal addForPrincipalSuisse(RegpmEntreprise entreprise, RegDate dateDebut, RegpmTypeForPrincipal type, @NotNull RegpmCommune commune) {
		return addForPrincipal(entreprise, dateDebut, type, commune, null);
	}

	static RegpmForPrincipal addForPrincipalEtranger(RegpmEntreprise entreprise, RegDate dateDebut, RegpmTypeForPrincipal type, int noOfsPays) {
		return addForPrincipal(entreprise, dateDebut, type, null, noOfsPays);
	}

	@Test
	public void testMigrationSuperSimple() throws Exception {

		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> une nouvelle entreprise
		final long idEntreprise = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ENTREPRISE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertEquals(idEntreprise, idMapper.getIdUniregEntreprise(noEntreprise));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());

	}

	@Test
	public void testMigrationDeclarationEmise() throws Exception {

		final int pf = 2014;
		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegpmAssujettissement a = addAssujettissement(e, RegDate.get(2000, 7, 1), null, RegpmTypeAssujettissement.LIFD);
		final RegpmDossierFiscal df = addDossierFiscal(e, a, pf, RegDate.get(pf, 7, 12));
		final RegpmExerciceCommercial exerciceCommercial = addExerciceCommercial(e, df, RegDate.get(pf - 1, 7, 1), RegDate.get(pf, 6, 30));

		// on crée d'abord la PF en base
		doInUniregTransaction(false, status -> {
			addPeriodeFiscale(pf);
			return null;
		});

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> une nouvelle entreprise
		final long idEntreprise = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ENTREPRISE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertEquals(idEntreprise, idMapper.getIdUniregEntreprise(noEntreprise));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());

		// .. et une déclaration dessus
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getUniregSessionFactory().getCurrentSession().get(Entreprise.class, idEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<Declaration> declarations = entreprise.getDeclarations();
			Assert.assertNotNull(declarations);
			Assert.assertEquals(1, declarations.size());

			final Declaration declaration = declarations.iterator().next();
			Assert.assertNotNull(declaration);
			Assert.assertEquals(RegDate.get(pf, 7, 12), declaration.getDateExpedition());
			Assert.assertEquals(RegDate.get(pf - 1, 7, 1), declaration.getDateDebut());
			Assert.assertEquals(RegDate.get(pf, 6, 30), declaration.getDateFin());

			final EtatDeclaration etat = declaration.getDernierEtat();
			Assert.assertNotNull(etat);
			Assert.assertEquals(TypeEtatDeclaration.EMISE, etat.getEtat());
			Assert.assertEquals(RegDate.get(pf, 7, 12), etat.getDateObtention());
			return null;
		});
	}

	@Test
	public void testMigrationDeclarationAvecAutresEtats() throws Exception {

		final int pf = 2014;
		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegpmAssujettissement a = addAssujettissement(e, RegDate.get(2000, 1, 1), null, RegpmTypeAssujettissement.LIFD);
		final RegpmDossierFiscal df = addDossierFiscal(e, a, pf, RegDate.get(pf, 7, 12));
		df.setDateEnvoiSommation(df.getDelaiRetour().addDays(30));
		df.setDelaiSommation(df.getDateEnvoiSommation().addDays(45));
		df.setDateRetour(df.getDateEnvoiSommation().addDays(10));
		final RegpmExerciceCommercial ex = addExerciceCommercial(e, df, RegDate.get(pf - 1, 7, 1), RegDate.get(pf, 6, 30));

		// on crée d'abord la PF en base
		doInUniregTransaction(false, status -> {
			addPeriodeFiscale(pf);
			return null;
		});

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> une nouvelle entreprise
		final long idEntreprise = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ENTREPRISE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertEquals(idEntreprise, idMapper.getIdUniregEntreprise(noEntreprise));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());

		// .. et une déclaration dessus
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getUniregSessionFactory().getCurrentSession().get(Entreprise.class, idEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<Declaration> declarations = entreprise.getDeclarations();
			Assert.assertNotNull(declarations);
			Assert.assertEquals(1, declarations.size());

			final Declaration declaration = declarations.iterator().next();
			Assert.assertNotNull(declaration);
			Assert.assertEquals(RegDate.get(pf, 7, 12), declaration.getDateExpedition());
			Assert.assertEquals(RegDate.get(pf - 1, 7, 1), declaration.getDateDebut());
			Assert.assertEquals(RegDate.get(pf, 6, 30), declaration.getDateFin());

			final List<EtatDeclaration> etats = declaration.getEtatsSorted();
			Assert.assertNotNull(etats);
			Assert.assertEquals(3, etats.size());
			{
				final EtatDeclaration etat = etats.get(0);
				Assert.assertNotNull(etat);
				Assert.assertEquals(TypeEtatDeclaration.EMISE, etat.getEtat());
				Assert.assertEquals(RegDate.get(pf, 7, 12), etat.getDateObtention());
			}
			{
				final EtatDeclaration etat = etats.get(1);
				Assert.assertNotNull(etat);
				Assert.assertEquals(TypeEtatDeclaration.SOMMEE, etat.getEtat());
				Assert.assertEquals(RegDate.get(pf, 7, 12).addDays(225 + 30), etat.getDateObtention());
			}
			{
				final EtatDeclaration etat = etats.get(2);
				Assert.assertNotNull(etat);
				Assert.assertEquals(TypeEtatDeclaration.RETOURNEE, etat.getEtat());
				Assert.assertEquals(RegDate.get(pf, 7, 12).addDays(225 + 30 + 10), etat.getDateObtention());
			}
			return null;
		});
	}

	@Test
	public void testCoordonneesFinancieres() throws Exception {

		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		e.setCoordonneesFinancieres(createCoordonneesFinancieres(null, "POFICHBEXXX", null, "17-331-7", "Postfinance", null));

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> une nouvelle entreprise
		final long idEntreprise = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ENTREPRISE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertEquals(idEntreprise, idMapper.getIdUniregEntreprise(noEntreprise));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());

		// avec les coordonnées financières qui vont bien
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getUniregSessionFactory().getCurrentSession().get(Entreprise.class, idEntreprise);
			Assert.assertEquals("CH7009000000170003317", entreprise.getNumeroCompteBancaire());
			Assert.assertEquals("POFICHBEXXX", entreprise.getAdresseBicSwift());
			Assert.assertNull(entreprise.getTitulaireCompteBancaire());     // le jour où on saura quoi mettre là-dedans, ça pêtera ici...
			return null;
		});
	}

	@Test
	public void testMandataire() throws Exception {

		final long noEntrepriseMandant = 42L;
		final long noEntrepriseMandataire = 548L;
		final RegpmEntreprise mandant = buildEntreprise(noEntrepriseMandant);
		final RegpmEntreprise mandataire = buildEntreprise(noEntrepriseMandataire);
		addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, "17-331-7", RegDate.get(2001, 5, 1), null);

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(mandataire, migrator, mr, linkCollector, idMapper);
		migrate(mandant, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> deux nouvelles entreprises
		final long[] idEntreprise = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ENTREPRISE);
			final List<Long> sorted = new ArrayList<>(ids);
			Collections.sort(sorted);
			Assert.assertNotNull(sorted);
			Assert.assertEquals(2, sorted.size());
			return new long[] {sorted.get(0), sorted.get(1)};
		});

		// vérification de l'instantiation du lien
		final RapportEntreTiers ret = doInUniregTransaction(true, status -> {
			final List<EntityLinkCollector.EntityLink> collectedLinks = linkCollector.getCollectedLinks();
			Assert.assertEquals(1, collectedLinks.size());
			final EntityLinkCollector.EntityLink link = collectedLinks.get(0);
			Assert.assertNotNull(link);
			return link.toRapportEntreTiers();
		});
		Assert.assertEquals(Mandat.class, ret.getClass());
		Assert.assertEquals(RegDate.get(2001, 5, 1), ret.getDateDebut());
		Assert.assertNull(ret.getDateFin());
		Assert.assertEquals((Long) idEntreprise[0], ret.getSujetId());
		Assert.assertEquals((Long) idEntreprise[1], ret.getObjetId());
		Assert.assertEquals("CH7009000000170003317", ((Mandat) ret).getCoordonneesFinancieres().getIban());
	}

	@Test
	public void testMandataireDateAttributionFuture() throws Exception {

		final long noEntrepriseMandant = 42L;
		final long noEntrepriseMandataire = 548L;
		final RegpmEntreprise mandant = buildEntreprise(noEntrepriseMandant);
		final RegpmEntreprise mandataire = buildEntreprise(noEntrepriseMandataire);
		addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, "17-331-7", RegDate.get().addDays(2), null);        // après demain est toujours dans le futur...

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(mandataire, migrator, mr, linkCollector, idMapper);
		migrate(mandant, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> deux nouvelles entreprises
		final long[] idEntreprise = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ENTREPRISE);
			final List<Long> sorted = new ArrayList<>(ids);
			Collections.sort(sorted);
			Assert.assertNotNull(sorted);
			Assert.assertEquals(2, sorted.size());
			return new long[] {sorted.get(0), sorted.get(1)};
		});

		// vérification de la non-instantiation du lien (avec une date de début dans le futur, il doit être ignoré...)
		final List<EntityLinkCollector.EntityLink> collectedLinks = linkCollector.getCollectedLinks();
		Assert.assertEquals(0, collectedLinks.size());

		// vérification du message ad'hoc dans le collecteur
		final List<MigrationResultCollector.Message> msgSuivi = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(msgSuivi);
		final List<String> messages = msgSuivi.stream().map(msg -> msg.text).collect(Collectors.toList());
		final String messageMandatDateDebutFuture = messages.stream()
				.filter(s -> s.matches("La date d'attribution du mandat .* est dans le futur \\(.*\\), le mandat sera donc ignoré dans la migration\\."))
				.findAny()
				.orElse(null);
		if (messageMandatDateDebutFuture == null) {
			Assert.fail("Aucun message ne parle du mandat dont la date d'attribution est dans le futur... : " + Arrays.toString(messages.toArray(new String[messages.size()])));
		}
	}

	@Test
	public void testMandataireDateAttributionNulle() throws Exception {

		final long noEntrepriseMandant = 42L;
		final long noEntrepriseMandataire = 548L;
		final RegpmEntreprise mandant = buildEntreprise(noEntrepriseMandant);
		final RegpmEntreprise mandataire = buildEntreprise(noEntrepriseMandataire);
		addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, "17-331-7", null, null);

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(mandataire, migrator, mr, linkCollector, idMapper);
		migrate(mandant, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> deux nouvelles entreprises
		final long[] idEntreprise = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ENTREPRISE);
			final List<Long> sorted = new ArrayList<>(ids);
			Collections.sort(sorted);
			Assert.assertNotNull(sorted);
			Assert.assertEquals(2, sorted.size());
			return new long[] {sorted.get(0), sorted.get(1)};
		});

		// vérification de la non-instantiation du lien (avec une date de début nulle, il doit être ignoré...)
		final List<EntityLinkCollector.EntityLink> collectedLinks = linkCollector.getCollectedLinks();
		Assert.assertEquals(0, collectedLinks.size());

		// vérification du message ad'hoc dans le collecteur
		final List<MigrationResultCollector.Message> msgSuivi = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(msgSuivi);
		final List<String> messages = msgSuivi.stream().map(msg -> msg.text).collect(Collectors.toList());
		final String messageMandatDateDebutFuture = messages.stream()
				.filter(s -> s.matches("Le mandat .* n'a pas de date d'attribution \\(ou cette date est très loin dans le passé\\), il sera donc ignoré dans la migration\\."))
				.findAny()
				.orElse(null);
		if (messageMandatDateDebutFuture == null) {
			Assert.fail("Aucun message ne parle du mandat dont la date d'attribution nulle... : " + Arrays.toString(messages.toArray(new String[messages.size()])));
		}
	}

	@Test
	public void testMandataireDateResiliationFuture() throws Exception {

		final long noEntrepriseMandant = 42L;
		final long noEntrepriseMandataire = 548L;
		final RegpmEntreprise mandant = buildEntreprise(noEntrepriseMandant);
		final RegpmEntreprise mandataire = buildEntreprise(noEntrepriseMandataire);
		addMandat(mandant, mandataire, RegpmTypeMandat.GENERAL, "17-331-7", RegDate.get(2001, 5, 1), RegDate.get().addDays(2));     // après-demain est toujours dans le futur !

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(mandataire, migrator, mr, linkCollector, idMapper);
		migrate(mandant, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> deux nouvelles entreprises
		final long[] idEntreprise = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ENTREPRISE);
			final List<Long> sorted = new ArrayList<>(ids);
			Collections.sort(sorted);
			Assert.assertNotNull(sorted);
			Assert.assertEquals(2, sorted.size());
			return new long[] {sorted.get(0), sorted.get(1)};
		});

		// vérification de l'instantiation du lien
		final RapportEntreTiers ret = doInUniregTransaction(true, status -> {
			final List<EntityLinkCollector.EntityLink> collectedLinks = linkCollector.getCollectedLinks();
			Assert.assertEquals(1, collectedLinks.size());
			final EntityLinkCollector.EntityLink link = collectedLinks.get(0);
			Assert.assertNotNull(link);
			return link.toRapportEntreTiers();
		});
		Assert.assertEquals(Mandat.class, ret.getClass());
		Assert.assertEquals(RegDate.get(2001, 5, 1), ret.getDateDebut());
		Assert.assertNull(ret.getDateFin());                                // malgré la date présente dans RegPM, elle est nulle ici
		Assert.assertEquals((Long) idEntreprise[0], ret.getSujetId());
		Assert.assertEquals((Long) idEntreprise[1], ret.getObjetId());
		Assert.assertEquals("CH7009000000170003317", ((Mandat) ret).getCoordonneesFinancieres().getIban());

		// vérification du message ad'hoc dans le collecteur
		final List<MigrationResultCollector.Message> msgSuivi = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(msgSuivi);
		final List<String> messages = msgSuivi.stream().map(msg -> msg.text).collect(Collectors.toList());
		final String messageMandatDateDebutFuture = messages.stream()
				.filter(s -> s.matches("La date de résiliation du mandat .* est dans le futur \\(.*\\), le mandat sera donc laissé ouvert dans la migration\\."))
				.findAny()
				.orElse(null);
		if (messageMandatDateDebutFuture == null) {
			Assert.fail("Aucun message ne parle du mandat dont la date de résiliation est future... : " + Arrays.toString(messages.toArray(new String[messages.size()])));
		}
	}

	@Test
	public void testRegimesFiscaux() throws Exception {

		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addRegimeFiscalCH(e, RegDate.get(2000, 1, 3), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		addRegimeFiscalCH(e, RegDate.get(2005, 1, 1), RegDate.get(2006, 4, 12), RegpmTypeRegimeFiscal._109_PM_AVEC_EXONERATION_ART_90G);
		addRegimeFiscalCH(e, RegDate.get(2006, 1, 1), null, RegpmTypeRegimeFiscal._109_PM_AVEC_EXONERATION_ART_90G);
		addRegimeFiscalVD(e, RegDate.get(2000, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> une nouvelle entreprise
		final long idEntreprise = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ENTREPRISE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertEquals(idEntreprise, idMapper.getIdUniregEntreprise(noEntreprise));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());

		// vérification des régimes fiscaux migrés
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getUniregSessionFactory().getCurrentSession().get(Entreprise.class, idEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<RegimeFiscal> regimesFiscauxBruts = entreprise.getRegimesFiscaux();
			Assert.assertNotNull(regimesFiscauxBruts);
			Assert.assertEquals(3, regimesFiscauxBruts.size());     // 1 VD + 2 CH (l'annulé n'est pas migré)

			final List<RegimeFiscal> regimesFiscauxTries = entreprise.getRegimesFiscauxNonAnnulesTries();
			Assert.assertNotNull(regimesFiscauxTries);
			Assert.assertEquals(3, regimesFiscauxTries.size());

			{
				final RegimeFiscal rf = regimesFiscauxTries.get(0);
				Assert.assertNotNull(rf);
				Assert.assertEquals(RegimeFiscal.Portee.VD, rf.getPortee());
				Assert.assertEquals(RegDate.get(2000, 1, 1), rf.getDateDebut());
				Assert.assertNull(rf.getDateFin());
				Assert.assertNull(rf.getAnnulationDate());
				Assert.assertEquals(TypeRegimeFiscal.ORDINAIRE, rf.getType());      // pour le moment, on n'a que celui-là...
			}
			{
				final RegimeFiscal rf = regimesFiscauxTries.get(1);
				Assert.assertNotNull(rf);
				Assert.assertEquals(RegimeFiscal.Portee.CH, rf.getPortee());
				Assert.assertEquals(RegDate.get(2000, 1, 3), rf.getDateDebut());
				Assert.assertEquals(RegDate.get(2005, 12, 31), rf.getDateFin());
				Assert.assertNull(rf.getAnnulationDate());
				Assert.assertEquals(TypeRegimeFiscal.ORDINAIRE, rf.getType());      // pour le moment, on n'a que celui-là...
			}
			{
				final RegimeFiscal rf = regimesFiscauxTries.get(2);
				Assert.assertNotNull(rf);
				Assert.assertEquals(RegimeFiscal.Portee.CH, rf.getPortee());
				Assert.assertEquals(RegDate.get(2006, 1, 1), rf.getDateDebut());
				Assert.assertNull(rf.getDateFin());
				Assert.assertNull(rf.getAnnulationDate());
				Assert.assertEquals(TypeRegimeFiscal.ORDINAIRE, rf.getType());      // pour le moment, on n'a que celui-là...
			}
			return null;
		});
	}

	@Test
	public void testPeriodeFiscaleInexistante() throws Exception {
		final int pf = 2014;
		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegpmAssujettissement a = addAssujettissement(e, RegDate.get(2000, 7, 1), null, RegpmTypeAssujettissement.LIFD);
		final RegpmDossierFiscal df = addDossierFiscal(e, a, pf, RegDate.get(pf, 7, 12));
		final RegpmExerciceCommercial exerciceCommercial = addExerciceCommercial(e, df, RegDate.get(pf - 1, 7, 1), RegDate.get(pf, 6, 30));

		// on vérifie que la PF n'existe pas en base (= pré-requis au test)
		doInUniregTransaction(true, status -> {
			final List<PeriodeFiscale> allPfs = uniregStore.getEntitiesFromDb(PeriodeFiscale.class, null);
			if (allPfs != null && allPfs.stream().filter(p -> p.getAnnee() == pf).findAny().isPresent()) {
				Assert.fail("Les conditions de départ ne sont pas respectées : la pf " + pf + " existe déjà en base...");
			}
		});

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(e, migrator, mr, linkCollector, idMapper);

		// on vérifie que la PF existe maintenant en base
		doInUniregTransaction(true, status -> {
			final List<PeriodeFiscale> allPfs = uniregStore.getEntitiesFromDb(PeriodeFiscale.class, null);
			if (allPfs == null || !allPfs.stream().filter(p -> p.getAnnee() == pf).findAny().isPresent()) {
				Assert.fail("La pf " + pf + " n'existe toujours pas...");
			}
		});

		// vérification du contenu de la base -> une nouvelle entreprise
		final long idEntreprise = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ENTREPRISE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		Assert.assertEquals(idEntreprise, idMapper.getIdUniregEntreprise(noEntreprise));
		Assert.assertEquals(0, linkCollector.getCollectedLinks().size());

		// .. et une déclaration dessus
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getUniregSessionFactory().getCurrentSession().get(Entreprise.class, idEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<Declaration> declarations = entreprise.getDeclarations();
			Assert.assertNotNull(declarations);
			Assert.assertEquals(1, declarations.size());

			final Declaration declaration = declarations.iterator().next();
			Assert.assertNotNull(declaration);
			Assert.assertEquals(RegDate.get(pf, 7, 12), declaration.getDateExpedition());
			Assert.assertEquals(RegDate.get(pf - 1, 7, 1), declaration.getDateDebut());
			Assert.assertEquals(RegDate.get(pf, 6, 30), declaration.getDateFin());

			final EtatDeclaration etat = declaration.getDernierEtat();
			Assert.assertNotNull(etat);
			Assert.assertEquals(TypeEtatDeclaration.EMISE, etat.getEtat());
			Assert.assertEquals(RegDate.get(pf, 7, 12), etat.getDateObtention());
			return null;
		});
	}

	@Test
	public void testForPrincipalSurFraction() throws Exception {
		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate debut = RegDate.get(2005, 5, 7);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.Fraction.LE_BRASSUS);

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> une nouvelle entreprise
		final long idEntreprise = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ENTREPRISE);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		// vérification de la commune du for principal créé
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(idEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<ForFiscal> fors = entreprise.getForsFiscaux();
			Assert.assertNotNull(fors);
			Assert.assertEquals(1, fors.size());

			final ForFiscal ff = fors.iterator().next();
			Assert.assertNotNull(ff);
			Assert.assertFalse(ff.isAnnule());
			Assert.assertEquals(debut, ff.getDateDebut());
			Assert.assertNull(ff.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.Fraction.LE_BRASSUS.getId().intValue(), ff.getNumeroOfsAutoriteFiscale().intValue());
			Assert.assertTrue(ff instanceof ForFiscalPrincipalPM);

			final ForFiscalPrincipalPM ffpm = (ForFiscalPrincipalPM) ff;
			Assert.assertEquals(MotifFor.INDETERMINE, ffpm.getMotifOuverture());
			Assert.assertNull(ffpm.getMotifFermeture());
			Assert.assertEquals(MotifRattachement.DOMICILE, ffpm.getMotifRattachement());
			Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ffpm.getGenreImpot());
		});
	}

	@Test
	public void testEtablissementPrincipalAvecCommune() throws Exception {
		final long noEntreprise = 1234L;
		final RegDate debut = RegDate.get(2005, 5, 7);
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		e.setCommune(Commune.ECHALLENS);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.BERN);

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> on va regarder l'établissement créé
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		doInUniregTransaction(true, status -> {
			final Etablissement etb = uniregStore.getEntityFromDb(Etablissement.class, idEtablissement);
			Assert.assertNotNull(etb);
			Assert.assertTrue(etb.isPrincipal());

			final Set<DomicileEtablissement> domiciles = etb.getDomiciles();
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(1, domiciles.size());

			final DomicileEtablissement domicile = domiciles.iterator().next();
			Assert.assertNotNull(domicile);
			Assert.assertFalse(domicile.isAnnule());
			Assert.assertEquals(debut, domicile.getDateDebut());
			Assert.assertNull(domicile.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
		});
	}

	@Test
	public void testPlusieursForsPrincipauxALaMemeDate() throws Exception {
		final long noEntreprise = 1234L;
		final RegDate debut = RegDate.get(2005, 5, 7);
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);        // <- c'est lui, le deuxième, qui devrait être pris en compte

		final MigrationResultCollector mr = new MigrationResultCollector();
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> on va regarder l'établissement principal et les fors créés
		final long idEtablissement = doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(1, ids.size());
			return ids.get(0);
		});

		doInUniregTransaction(true, status -> {
			// l'établissement principal
			final Etablissement etb = uniregStore.getEntityFromDb(Etablissement.class, idEtablissement);
			Assert.assertNotNull(etb);
			Assert.assertTrue(etb.isPrincipal());

			final Set<DomicileEtablissement> domiciles = etb.getDomiciles();
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(1, domiciles.size());

			final DomicileEtablissement domicile = domiciles.iterator().next();
			Assert.assertNotNull(domicile);
			Assert.assertFalse(domicile.isAnnule());
			Assert.assertEquals(debut, domicile.getDateDebut());
			Assert.assertNull(domicile.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());

			// le for principal
			final Entreprise entr = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entr);
			Assert.assertEquals(1, entr.getForsFiscauxPrincipauxActifsSorted().size());

			final ForFiscalPrincipalPM ffp = entr.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(debut, ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
		});

		// vérification des messages dans le contexte "FORS"
		final List<MigrationResultCollector.Message> messagesFors = mr.getMessages().get(LogCategory.FORS);
		Assert.assertNotNull(messagesFors);
		final List<String> textesFors = messagesFors.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(2, textesFors.size());
		Assert.assertEquals("Plusieurs (2) fors principaux ont une date de début identique au 07.05.2005 : seul le dernier sera pris en compte pour la migration.", textesFors.get(0));
		Assert.assertEquals("Plusieurs (2) fors principaux ont une date de début identique au 07.05.2005 : seul le dernier sera pris en compte pour l'établissement principal.", textesFors.get(1));
	}

	// TODO il reste encore plein de tests à faire...
}
