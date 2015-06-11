package ch.vd.uniregctb.migration.pm.engine;

import java.util.ArrayList;
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
import ch.vd.uniregctb.adapter.rcent.service.RCEntService;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.MigrationResultCollector;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
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
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.TypeTiers;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeRegimeFiscal;

public class EntrepriseMigratorTest extends AbstractEntityMigratorTest {

	private EntrepriseMigrator migrator;

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();

		migrator = new EntrepriseMigrator(
				getBean(UniregStore.class, "uniregStore"),
				getBean(BouclementService.class, "bouclementService"),
				getBean(RCEntService.class, "rcEntService"),
				getBean(AdresseHelper.class, "adresseHelper"));
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

	// TODO il reste encore plein de tests à faire...
}
