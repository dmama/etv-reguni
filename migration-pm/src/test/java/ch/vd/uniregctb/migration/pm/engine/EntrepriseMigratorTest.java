package ch.vd.uniregctb.migration.pm.engine;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.MigrationResultCollector;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.mapping.IdMapper;
import ch.vd.uniregctb.migration.pm.rcent.service.RCEntService;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmExerciceCommercial;
import ch.vd.uniregctb.migration.pm.regpm.RegpmMotifEnvoi;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDossierFiscal;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.EntityLinkCollector;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TypeTiers;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class EntrepriseMigratorTest extends AbstractEntityMigratorTest {

	private EntrepriseMigrator migrator;

	private static final String REGPM_VISA = "REGPM";
	private static final Timestamp REGPM_MODIF = new Timestamp(DateHelper.getCurrentDate().getTime() - TimeUnit.DAYS.toMillis(2000));   // 2000 jours ~ 5.5 années

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();

		migrator = new EntrepriseMigrator(
				getBean(UniregStore.class, "uniregStore"),
				getBean(TiersDAO.class, "tiersDAO"),
				getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO"),
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

	// TODO il reste encore plein de tests à faire...
}
