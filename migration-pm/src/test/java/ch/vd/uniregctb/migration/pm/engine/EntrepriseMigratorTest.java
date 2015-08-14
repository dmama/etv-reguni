package ch.vd.uniregctb.migration.pm.engine;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.adapter.rcent.service.RCEntAdapter;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.MigrationResultCollector;
import ch.vd.uniregctb.migration.pm.communes.FractionsCommuneProvider;
import ch.vd.uniregctb.migration.pm.communes.FusionCommunesProvider;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.mapping.IdMapper;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAllegementFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAppartenanceGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCodeCollectivite;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCodeContribution;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDecisionTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntity;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEnvironnementTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmExerciceCommercial;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForSecondaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmImmeuble;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmModeImposition;
import ch.vd.uniregctb.migration.pm.regpm.RegpmMotifEnvoi;
import ch.vd.uniregctb.migration.pm.regpm.RegpmObjectImpot;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRattachementProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscalCH;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscalVD;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeContribution;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDecisionTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeNatureDecisionTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeRegimeFiscal;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.TypeTiers;
import ch.vd.uniregctb.type.DayMonth;
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
				getBean(ServiceInfrastructureService.class, "serviceInfrastructureService"),
				getBean(BouclementService.class, "bouclementService"),
				getBean(AssujettissementService.class, "assujettissementService"),
				getBean(RCEntAdapter.class, "rcEntAdapter"),
				getBean(AdresseHelper.class, "adresseHelper"),
				getBean(FusionCommunesProvider.class, "fusionCommunesProvider"),
				getBean(FractionsCommuneProvider.class, "fractionsCommuneProvider"));
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

	static RegpmDossierFiscal addDossierFiscal(RegpmEntreprise entreprise, RegpmAssujettissement assujettissement, int pf, RegDate dateEnvoi, RegpmModeImposition modeImposition) {
		final RegpmDossierFiscal df = new RegpmDossierFiscal();
		df.setId(new RegpmDossierFiscal.PK(computeNewSeqNo(entreprise.getDossiersFiscaux(), d -> d.getId().getSeqNo()), assujettissement.getId()));
		assignMutationVisa(df, REGPM_VISA, REGPM_MODIF);
		df.setAssujettissement(assujettissement);
		df.setDateEnvoi(dateEnvoi);
		df.setDelaiRetour(dateEnvoi.addDays(225));
		df.setDemandesDelai(new TreeSet<>());
		df.setEtat(RegpmTypeEtatDossierFiscal.ENVOYE);
		df.setMotifEnvoi(RegpmMotifEnvoi.FIN_EXERCICE);
		df.setModeImposition(modeImposition);
		df.setEnvironnementsTaxation(new TreeSet<>());

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

	static RegpmEnvironnementTaxation addEnvironnementTaxation(RegpmEntreprise entreprise, RegpmDossierFiscal dossier, RegDate dateCreation) {
		final RegpmEnvironnementTaxation et = new RegpmEnvironnementTaxation();
		et.setId(new RegpmEnvironnementTaxation.PK(NO_SEQUENCE_GENERATOR.next(), entreprise.getId(), dossier.getPf()));
		assignMutationVisa(et, REGPM_VISA, REGPM_MODIF);
		et.setDateCreation(dateCreation);
		et.setDecisionsTaxation(new HashSet<>());
		dossier.getEnvironnementsTaxation().add(et);
		return et;
	}

	static RegpmDecisionTaxation addDecisionTaxation(RegpmEnvironnementTaxation et, boolean derniereTaxation, RegpmTypeEtatDecisionTaxation etat, RegpmTypeNatureDecisionTaxation nature, RegDate date) {
		final RegpmDecisionTaxation dt = new RegpmDecisionTaxation();
		dt.setId(new RegpmDecisionTaxation.PK(NO_SEQUENCE_GENERATOR.next(), et.getId().getIdEntreprise(), et.getId().getAnneeFiscale(), et.getId().getSeqNo()));
		assignMutationVisa(dt, REGPM_VISA, new Timestamp(DateHelper.getDate(date.year(), date.month(), date.day()).getTime()));
		dt.setDerniereTaxation(derniereTaxation);
		dt.setEtatCourant(etat);
		dt.setNatureDecision(nature);
		et.getDecisionsTaxation().add(dt);
		return dt;
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

	static RegpmForSecondaire addForSecondaire(RegpmEntreprise entreprise, RegDate dateDebut, RegDate dateFin, @NotNull RegpmCommune commune) {
		final RegpmForSecondaire ffs = new RegpmForSecondaire();
		ffs.setId(ID_GENERATOR.next());
		assignMutationVisa(ffs, REGPM_VISA, REGPM_MODIF);
		ffs.setCommune(commune);
		ffs.setDateDebut(dateDebut);
		ffs.setDateFin(dateFin);
		entreprise.getForsSecondaires().add(ffs);
		return ffs;
	}

	static RegpmAllegementFiscal addAllegementFiscal(RegpmEntreprise entreprise, RegDate dateDebut, @Nullable RegDate dateFin, @NotNull BigDecimal pourcentage, @NotNull RegpmObjectImpot objectImpot) {
		final RegpmAllegementFiscal a = new RegpmAllegementFiscal();
		a.setId(new RegpmAllegementFiscal.PK(computeNewSeqNo(entreprise.getAllegementsFiscaux(), x -> x.getId().getSeqNo()), entreprise.getId()));
		assignMutationVisa(a, REGPM_VISA, REGPM_MODIF);
		a.setCommune(null);
		a.setDateAnnulation(null);
		a.setDateDebut(dateDebut);
		a.setDateFin(dateFin);
		a.setObjectImpot(objectImpot);
		a.setPourcentage(pourcentage);
		a.setTypeContribution(null);
		entreprise.getAllegementsFiscaux().add(a);
		return a;
	}

	static RegpmAllegementFiscal addAllegementFiscal(RegpmEntreprise entreprise, RegDate dateDebut, @Nullable RegDate dateFin, @NotNull BigDecimal pourcentage,
	                                                 @NotNull RegpmCodeContribution codeContribution, @NotNull RegpmCodeCollectivite codeCollectivite, @Nullable RegpmCommune commune) {
		final RegpmAllegementFiscal a = new RegpmAllegementFiscal();
		a.setId(new RegpmAllegementFiscal.PK(computeNewSeqNo(entreprise.getAllegementsFiscaux(), x -> x.getId().getSeqNo()), entreprise.getId()));
		assignMutationVisa(a, REGPM_VISA, REGPM_MODIF);
		a.setCommune(commune);
		a.setDateAnnulation(null);
		a.setDateDebut(dateDebut);
		a.setDateFin(dateFin);
		a.setObjectImpot(null);
		a.setPourcentage(pourcentage);

		final RegpmTypeContribution typeContribution = new RegpmTypeContribution();
		typeContribution.setCodeCollectivite(codeCollectivite);
		typeContribution.setCodeContribution(codeContribution);
		typeContribution.setId(ID_GENERATOR.next());
		a.setTypeContribution(typeContribution);

		entreprise.getAllegementsFiscaux().add(a);
		return a;
	}

	@Test
	public void testMigrationSuperSimple() throws Exception {

		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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
		final RegpmDossierFiscal df = addDossierFiscal(e, a, pf, RegDate.get(pf, 7, 12), RegpmModeImposition.POST);
		final RegpmExerciceCommercial exerciceCommercial = addExerciceCommercial(e, df, RegDate.get(pf - 1, 7, 1), RegDate.get(pf, 6, 30));

		// on crée d'abord la PF en base
		doInUniregTransaction(false, status -> {
			addPeriodeFiscale(pf);
			return null;
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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

		// vérification des messages dans le contexte "DECLARATIONS"
		final List<MigrationResultCollector.Message> messagesDeclarations = mr.getMessages().get(LogCategory.DECLARATIONS);
		Assert.assertNotNull(messagesDeclarations);
		final List<String> textesDeclarations = messagesDeclarations.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(3, textesDeclarations.size());
		Assert.assertEquals("Génération d'une déclaration sur la PF 2014 à partir des dates [01.07.2013 -> 30.06.2014] de l'exercice commercial 1 et du dossier fiscal correspondant.", textesDeclarations.get(0));
		Assert.assertEquals("Délai initial de retour fixé au 22.02.2015.", textesDeclarations.get(1));
		Assert.assertEquals("Etat 'EMISE' migré au 12.07.2014.", textesDeclarations.get(2));
	}

	@Test
	public void testMigrationDeclarationAvecAutresEtats() throws Exception {

		final int pf = 2014;
		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegpmAssujettissement a = addAssujettissement(e, RegDate.get(2000, 1, 1), null, RegpmTypeAssujettissement.LIFD);
		final RegpmDossierFiscal df = addDossierFiscal(e, a, pf, RegDate.get(pf, 7, 12), RegpmModeImposition.POST);
		df.setDateEnvoiSommation(df.getDelaiRetour().addDays(30));
		df.setDelaiSommation(df.getDateEnvoiSommation().addDays(45));
		df.setDateRetour(df.getDateEnvoiSommation().addDays(10));
		final RegpmExerciceCommercial ex = addExerciceCommercial(e, df, RegDate.get(pf - 1, 7, 1), RegDate.get(pf, 6, 30));

		// on crée d'abord la PF en base
		doInUniregTransaction(false, status -> {
			addPeriodeFiscale(pf);
			return null;
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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

		// vérification des messages dans le contexte "DECLARATIONS"
		final List<MigrationResultCollector.Message> messagesDeclarations = mr.getMessages().get(LogCategory.DECLARATIONS);
		Assert.assertNotNull(messagesDeclarations);
		final List<String> textesDeclarations = messagesDeclarations.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(5, textesDeclarations.size());
		Assert.assertEquals("Génération d'une déclaration sur la PF 2014 à partir des dates [01.07.2013 -> 30.06.2014] de l'exercice commercial 1 et du dossier fiscal correspondant.",
		                    textesDeclarations.get(0));
		Assert.assertEquals("Délai initial de retour fixé au 22.02.2015.", textesDeclarations.get(1));
		Assert.assertEquals("Etat 'EMISE' migré au 12.07.2014.", textesDeclarations.get(2));
		Assert.assertEquals("Etat 'SOMMEE' migré au 24.03.2015.", textesDeclarations.get(3));
		Assert.assertEquals("Etat 'RETOURNEE' migré au 03.04.2015.", textesDeclarations.get(4));
	}

	@Test
	public void testMigrationDeclarationEchue() throws Exception {

		final int pf = 2014;
		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegpmAssujettissement a = addAssujettissement(e, RegDate.get(2000, 1, 1), null, RegpmTypeAssujettissement.LIFD);
		final RegpmDossierFiscal df = addDossierFiscal(e, a, pf, RegDate.get(pf, 7, 12), RegpmModeImposition.POST);
		df.setDateEnvoiSommation(df.getDelaiRetour().addDays(30));
		df.setDelaiSommation(df.getDateEnvoiSommation().addDays(45));
		df.setDateRetour(df.getDateEnvoiSommation().addDays(100));
		final RegpmExerciceCommercial ex = addExerciceCommercial(e, df, RegDate.get(pf - 1, 7, 1), RegDate.get(pf, 6, 30));
		final RegpmEnvironnementTaxation envTaxation = addEnvironnementTaxation(e, df, RegDate.get(pf, 9, 10));
		addDecisionTaxation(envTaxation, true, RegpmTypeEtatDecisionTaxation.ANNULEE, RegpmTypeNatureDecisionTaxation.DEFINITIVE, RegDate.get(pf, 9, 25));
		addDecisionTaxation(envTaxation, false, RegpmTypeEtatDecisionTaxation.NOTIFIEE, RegpmTypeNatureDecisionTaxation.TAXATION_OFFICE_DEFAUT_DOSSIER, df.getDateEnvoiSommation().addDays(50));
		addDecisionTaxation(envTaxation, true, RegpmTypeEtatDecisionTaxation.ENTREE_EN_FORCE, RegpmTypeNatureDecisionTaxation.DEFINITIVE, df.getDateRetour().addDays(15));

		// on crée d'abord la PF en base
		doInUniregTransaction(false, status -> {
			addPeriodeFiscale(pf);
			return null;
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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
			Assert.assertEquals(4, etats.size());
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
				Assert.assertEquals(TypeEtatDeclaration.ECHUE, etat.getEtat());
				Assert.assertEquals(RegDate.get(pf, 7, 12).addDays(225 + 30 + 50), etat.getDateObtention());
			}
			{
				final EtatDeclaration etat = etats.get(3);
				Assert.assertNotNull(etat);
				Assert.assertEquals(TypeEtatDeclaration.RETOURNEE, etat.getEtat());
				Assert.assertEquals(RegDate.get(pf, 7, 12).addDays(225 + 30 + 100), etat.getDateObtention());
			}
			return null;
		});

		// vérification des messages dans le contexte "DECLARATIONS"
		final List<MigrationResultCollector.Message> messagesDeclarations = mr.getMessages().get(LogCategory.DECLARATIONS);
		Assert.assertNotNull(messagesDeclarations);
		final List<String> textesDeclarations = messagesDeclarations.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(6, textesDeclarations.size());
		Assert.assertEquals("Génération d'une déclaration sur la PF 2014 à partir des dates [01.07.2013 -> 30.06.2014] de l'exercice commercial 1 et du dossier fiscal correspondant.", textesDeclarations.get(
				0));
		Assert.assertEquals("Délai initial de retour fixé au 22.02.2015.", textesDeclarations.get(1));
		Assert.assertEquals("Etat 'EMISE' migré au 12.07.2014.", textesDeclarations.get(2));
		Assert.assertEquals("Etat 'SOMMEE' migré au 24.03.2015.", textesDeclarations.get(3));
		Assert.assertEquals("Etat 'ECHUE' migré au 13.05.2015.", textesDeclarations.get(4));
		Assert.assertEquals("Etat 'RETOURNEE' migré au 02.07.2015.", textesDeclarations.get(5));
	}

	@Test
	public void testExerciceCommercialEtDossierFiscalSurAnneesDifferentes() throws Exception {
		final int pf = 2014;
		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegpmAssujettissement a = addAssujettissement(e, RegDate.get(2000, 7, 1), null, RegpmTypeAssujettissement.LIFD);
		final RegpmDossierFiscal df = addDossierFiscal(e, a, pf, RegDate.get(pf, 7, 12), RegpmModeImposition.POST);
		final RegpmExerciceCommercial exerciceCommercial = addExerciceCommercial(e, df, RegDate.get(pf - 2, 7, 1), RegDate.get(pf - 1, 6, 30));     // décalage entre les années du df et de l'exercice

		// ajout de la période fiscale
		doInUniregTransaction(false, status -> {
			addPeriodeFiscale(pf);
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> régimes fiscaux associés à l'entreprise (aucun, car ils doivent être ignorés en raison de leur date nulle)
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);
			Assert.assertEquals(Collections.emptySet(), entreprise.getRegimesFiscaux());
		});

		// vérification des messages dans le contexte "DECLARATIONS"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.DECLARATIONS);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(4, textes.size());
		Assert.assertEquals("Génération d'une déclaration sur la PF 2014 à partir des dates [01.07.2012 -> 30.06.2013] de l'exercice commercial 1 et du dossier fiscal correspondant.", textes.get(0));
		Assert.assertEquals("Dossier fiscal sur la PF 2014 alors que la fin de l'exercice commercial est en 2013... N'est-ce pas étrange ?", textes.get(1));
		Assert.assertEquals("Délai initial de retour fixé au 22.02.2015.", textes.get(2));
		Assert.assertEquals("Etat 'EMISE' migré au 12.07.2014.", textes.get(3));
	}

	@Test
	public void testCoordonneesFinancieres() throws Exception {

		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		e.setCoordonneesFinancieres(createCoordonneesFinancieres(null, "POFICHBEXXX", null, "17-331-7", "Postfinance", null));

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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

		final MockGraphe graphe = new MockGraphe(Arrays.asList(mandant, mandataire),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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

		final MockGraphe graphe = new MockGraphe(Arrays.asList(mandant, mandataire),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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

		final MockGraphe graphe = new MockGraphe(Arrays.asList(mandant, mandataire),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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

		final MockGraphe graphe = new MockGraphe(Arrays.asList(mandant, mandataire),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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
	public void testForPrincipalSurFraction() throws Exception {
		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate debut = RegDate.get(2005, 5, 7);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.Fraction.LE_BRASSUS);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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
	public void testPlusieursForsPrincipauxDeMemeTypeALaMemeDate() throws Exception {
		final long noEntreprise = 1234L;
		final RegDate debut = RegDate.get(2005, 5, 7);
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);        // <- c'est lui, le deuxième, qui devrait être pris en compte

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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
		Assert.assertEquals("Plusieurs (2) fors principaux de même type (SIEGE) mais sur des autorités fiscales différentes (COMMUNE_OU_FRACTION_VD/5586, COMMUNE_OU_FRACTION_VD/5518) ont une date de début identique au 07.05.2005 : seul le dernier sera pris en compte.", textesFors.get(0));
		Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5518 [07.05.2005 -> ?] généré.", textesFors.get(1));
	}

	@Test
	public void testPlusieursForsPrincipauxDeTypesDifferentsALaMemeDate() throws Exception {
		final long noEntreprise = 1234L;
		final RegDate debut = RegDate.get(2005, 5, 7);
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE, Commune.ECHALLENS);        // <- c'est lui, l'administration effective, qui devrait être pris en compte
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.BALE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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

			// la décision ACI, car le for principal source est une administration effective
			final List<DecisionAci> decisions = entr.getDecisionsSorted();
			Assert.assertNotNull(decisions);
			Assert.assertEquals(1, decisions.size());

			final DecisionAci decision = decisions.get(0);
			Assert.assertNotNull(decision);
			Assert.assertFalse(decision.isAnnule());
			Assert.assertEquals(debut, decision.getDateDebut());
			Assert.assertNull(decision.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, decision.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), decision.getNumeroOfsAutoriteFiscale());
			Assert.assertEquals(String.format("Selon décision OIPM du %s par %s.", RegDateHelper.dateToDisplayString(RegDateHelper.get(REGPM_MODIF)), REGPM_VISA), decision.getRemarque());
		});

		// vérification des messages dans le contexte "FORS"
		final List<MigrationResultCollector.Message> messagesFors = mr.getMessages().get(LogCategory.FORS);
		Assert.assertNotNull(messagesFors);
		final List<String> textesFors = messagesFors.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(4, textesFors.size());
		Assert.assertEquals(
				"For fiscal principal 1 COMMUNE_OU_FRACTION_VD/5586 ignoré en raison de la présence à la même date (07.05.2005) d'un for fiscal principal différent de type ADMINISTRATION_EFFECTIVE.",
				textesFors.get(0));
		Assert.assertEquals("For fiscal principal 3 COMMUNE_HC/2701 ignoré en raison de la présence à la même date (07.05.2005) d'un for fiscal principal différent de type ADMINISTRATION_EFFECTIVE.", textesFors.get(
				1));
		Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5518 [07.05.2005 -> ?] généré.", textesFors.get(2));
		Assert.assertEquals("Décision ACI COMMUNE_OU_FRACTION_VD/5518 [07.05.2005 -> ?] générée.", textesFors.get(3));
	}

	@Test
	public void testPlusieursForsPrincipauxIdentiquesALaMemeDatePremierAdministrationEffective() throws Exception {
		final long noEntreprise = 1234L;
		final RegDate debut = RegDate.get(2005, 5, 7);
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE, Commune.LAUSANNE);         // <- on prendra le premier
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());

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
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());

			// la décision ACI, car le for principal source est une administration effective
			final List<DecisionAci> decisions = entr.getDecisionsSorted();
			Assert.assertNotNull(decisions);
			Assert.assertEquals(1, decisions.size());

			final DecisionAci decision = decisions.get(0);
			Assert.assertNotNull(decision);
			Assert.assertFalse(decision.isAnnule());
			Assert.assertEquals(debut, decision.getDateDebut());
			Assert.assertNull(decision.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, decision.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), decision.getNumeroOfsAutoriteFiscale());
			Assert.assertEquals(String.format("Selon décision OIPM du %s par %s.", RegDateHelper.dateToDisplayString(RegDateHelper.get(REGPM_MODIF)), REGPM_VISA), decision.getRemarque());
		});

		// vérification des messages dans le contexte "FORS"
		final List<MigrationResultCollector.Message> messagesFors = mr.getMessages().get(LogCategory.FORS);
		Assert.assertNotNull(messagesFors);
		final List<String> textesFors = messagesFors.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(3, textesFors.size());
		Assert.assertEquals("Plusieurs (3) fors principaux sur la même autorité fiscale (COMMUNE_OU_FRACTION_VD/5586) ont une date de début identique au 07.05.2005 : seul le premier sera pris en compte.", textesFors.get(0));
		Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5586 [07.05.2005 -> ?] généré.", textesFors.get(1));
		Assert.assertEquals("Décision ACI COMMUNE_OU_FRACTION_VD/5586 [07.05.2005 -> ?] générée.", textesFors.get(2));
	}

	@Test
	public void testPlusieursForsPrincipauxIdentiquesALaMemeDate() throws Exception {
		final long noEntreprise = 1234L;
		final RegDate debut = RegDate.get(2005, 5, 7);
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);         // <- on prendra le premier
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE, Commune.LAUSANNE);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE, Commune.LAUSANNE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());

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
			Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());

			// pas de décision ACI, car le for principal source choisi n'est pas une administration effective
			final List<DecisionAci> decisions = entr.getDecisionsSorted();
			Assert.assertNotNull(decisions);
			Assert.assertEquals(0, decisions.size());
		});

		// vérification des messages dans le contexte "FORS"
		final List<MigrationResultCollector.Message> messagesFors = mr.getMessages().get(LogCategory.FORS);
		Assert.assertNotNull(messagesFors);
		final List<String> textesFors = messagesFors.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(2, textesFors.size());
		Assert.assertEquals("Plusieurs (3) fors principaux sur la même autorité fiscale (COMMUNE_OU_FRACTION_VD/5586) ont une date de début identique au 07.05.2005 : seul le premier sera pris en compte.", textesFors.get(0));
		Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5586 [07.05.2005 -> ?] généré.", textesFors.get(1));
	}

	@Test
	public void testPlusieursForsPrincipauxDeTypesDifferentsAvecPlusieursAdministrationsEffectivesALaMemeDate() throws Exception {
		final long noEntreprise = 1234L;
		final RegDate debut = RegDate.get(2005, 5, 7);
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE, Commune.ECHALLENS);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE, Commune.MORGES);        // <- c'est lui, la dernière administration effective, qui devrait être pris en compte
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.BALE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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
			Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());

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
			Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());

			// la décision ACI, car le for principal source est une administration effective
			final List<DecisionAci> decisions = entr.getDecisionsSorted();
			Assert.assertNotNull(decisions);
			Assert.assertEquals(1, decisions.size());

			final DecisionAci decision = decisions.get(0);
			Assert.assertNotNull(decision);
			Assert.assertFalse(decision.isAnnule());
			Assert.assertEquals(debut, decision.getDateDebut());
			Assert.assertNull(decision.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, decision.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), decision.getNumeroOfsAutoriteFiscale());
			Assert.assertEquals(String.format("Selon décision OIPM du %s par %s.", RegDateHelper.dateToDisplayString(RegDateHelper.get(REGPM_MODIF)), REGPM_VISA), decision.getRemarque());
		});

		// vérification des messages dans le contexte "FORS"
		final List<MigrationResultCollector.Message> messagesFors = mr.getMessages().get(LogCategory.FORS);
		Assert.assertNotNull(messagesFors);
		final List<String> textesFors = messagesFors.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(5, textesFors.size());
		Assert.assertEquals("For fiscal principal 1 COMMUNE_OU_FRACTION_VD/5586 ignoré en raison de la présence à la même date (07.05.2005) d'un for fiscal principal différent de type ADMINISTRATION_EFFECTIVE.", textesFors.get(0));
		Assert.assertEquals("For fiscal principal 4 COMMUNE_HC/2701 ignoré en raison de la présence à la même date (07.05.2005) d'un for fiscal principal différent de type ADMINISTRATION_EFFECTIVE.",
		                    textesFors.get(1));
		Assert.assertEquals("Plusieurs (2) fors principaux de type ADMINISTRATION_EFFECTIVE sur des autorités fiscales différentes (COMMUNE_OU_FRACTION_VD/5518, COMMUNE_OU_FRACTION_VD/5642) ont une date de début identique au 07.05.2005 : seul le dernier sera pris en compte.", textesFors.get(
				2));
		Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5642 [07.05.2005 -> ?] généré.", textesFors.get(3));
		Assert.assertEquals("Décision ACI COMMUNE_OU_FRACTION_VD/5642 [07.05.2005 -> ?] générée.", textesFors.get(4));
	}

	@Test
	public void testForsPrincipauxMultiplesAvecDateDebutNulle() throws Exception {
		final long noEntreprise = 1234L;
		final RegDate debut = RegDate.get(2005, 5, 7);
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, null, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);      // <- celui-là devrait être éliminé car le suivant a une date de validité nulle
		addForPrincipalSuisse(e, null, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);     // <- celui-là devrait être éliminé car il a lui-même une date de validité nulle...
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.MORGES);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
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
			Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());

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
			Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
		});

		// vérification des messages dans le contexte "FORS"
		final List<MigrationResultCollector.Message> messagesFors = mr.getMessages().get(LogCategory.FORS);
		Assert.assertNotNull(messagesFors);
		final List<String> textesFors = messagesFors.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(3, textesFors.size());
		Assert.assertEquals("Plusieurs (2) fors principaux de même type (SIEGE) mais sur des autorités fiscales différentes (COMMUNE_OU_FRACTION_VD/5586, COMMUNE_OU_FRACTION_VD/5518) ont une date de début identique au ? : seul le dernier sera pris en compte.", textesFors.get(0));
		Assert.assertEquals("Le for principal 2 est ignoré car il a une date de début nulle.", textesFors.get(1));
		Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5642 [07.05.2005 -> ?] généré.", textesFors.get(2));
	}

	@Test
	public void testTousForsPrincipauxAvecDateDebutNulle() throws Exception {
		final long noEntreprise = 1234L;
		final RegDate debut = RegDate.get(2005, 5, 7);
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, null, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);      // <- celui-là devrait être éliminé car le suivant a une date de validité nulle
		addForPrincipalSuisse(e, null, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);     // <- celui-là devrait être éliminé car il a lui-même une date de validité nulle...

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> pas d'établissement principal, mais quand-même une entreprise (sans for principal)
		doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(0, ids.size());

			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);
			Assert.assertEquals(0, entreprise.getForsFiscaux().size());
		});

		// vérification des messages dans le contexte "FORS"
		final List<MigrationResultCollector.Message> messagesFors = mr.getMessages().get(LogCategory.FORS);
		Assert.assertNotNull(messagesFors);
		final List<String> textesFors = messagesFors.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(2, textesFors.size());
		Assert.assertEquals("Plusieurs (2) fors principaux de même type (SIEGE) mais sur des autorités fiscales différentes (COMMUNE_OU_FRACTION_VD/5586, COMMUNE_OU_FRACTION_VD/5518) ont une date de début identique au ? : seul le dernier sera pris en compte.", textesFors.get(0));
		Assert.assertEquals("Le for principal 2 est ignoré car il a une date de début nulle.", textesFors.get(1));

		// .. et dans le contexte "SUIVI"
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bPas de commune ni de for principal associé, pas d'établissement principal créé\\.");
	}

	@Test
	public void testRegimesFiscauxDateDeDebutNulle() throws Exception {
		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addRegimeFiscalCH(e, null, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		addRegimeFiscalVD(e, null, null, RegpmTypeRegimeFiscal._01_ORDINAIRE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> régimes fiscaux associés à l'entreprise (aucun, car ils doivent être ignorés en raison de leur date nulle)
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);
			Assert.assertEquals(Collections.emptySet(), entreprise.getRegimesFiscaux());
		});

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(6, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Régime fiscal CH _01_ORDINAIRE ignoré en raison de sa date de début nulle.", textes.get(1));
		Assert.assertEquals("Régime fiscal VD _01_ORDINAIRE ignoré en raison de sa date de début nulle.", textes.get(2));
		Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(3));
		Assert.assertEquals("Pas de commune ni de for principal associé, pas d'établissement principal créé.", textes.get(4));
		Assert.assertEquals("Entreprise migrée : 12.34.", textes.get(5));
	}

	@Test
	public void testAllegementsFiscauxObjectImpot() throws Exception {
		final long noEntreprise = 4784L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addAllegementFiscal(e, RegDate.get(1950, 1, 1), RegDate.get(1955, 3, 31), BigDecimal.valueOf(12L), RegpmObjectImpot.CANTONAL);
		addAllegementFiscal(e, RegDate.get(1956, 5, 1), RegDate.get(1956, 12, 27), BigDecimal.valueOf(13L), RegpmObjectImpot.FEDERAL);
		addAllegementFiscal(e, RegDate.get(1957, 3, 12), null, BigDecimal.valueOf(135L, 1), RegpmObjectImpot.COMMUNAL);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<AllegementFiscal> allegements = entreprise.getAllegementsFiscaux();
			final List<AllegementFiscal> allegementsTries = allegements.stream()
					.sorted(Comparator.comparing(AllegementFiscal::getDateDebut).thenComparing(AllegementFiscal::getTypeImpot))
					.collect(Collectors.toList());
			Assert.assertNotNull(allegementsTries);
			Assert.assertEquals(6, allegementsTries.size());
			{
				final AllegementFiscal a = allegementsTries.get(0);
				Assert.assertNotNull(a);
				Assert.assertNull(a.getAnnulationDate());
				Assert.assertEquals(RegDate.get(1950, 1, 1), a.getDateDebut());
				Assert.assertEquals(RegDate.get(1955, 3, 31), a.getDateFin());
				Assert.assertNull(a.getNoOfsCommune());
				Assert.assertEquals("Expected: 12, actual: " + a.getPourcentageAllegement(), 0, BigDecimal.valueOf(12L).compareTo(a.getPourcentageAllegement()));
				Assert.assertEquals(AllegementFiscal.TypeCollectivite.CANTON, a.getTypeCollectivite());
				Assert.assertEquals(AllegementFiscal.TypeImpot.BENEFICE, a.getTypeImpot());
			}
			{
				final AllegementFiscal a = allegementsTries.get(1);
				Assert.assertNotNull(a);
				Assert.assertNull(a.getAnnulationDate());
				Assert.assertEquals(RegDate.get(1950, 1, 1), a.getDateDebut());
				Assert.assertEquals(RegDate.get(1955, 3, 31), a.getDateFin());
				Assert.assertNull(a.getNoOfsCommune());
				Assert.assertEquals("Expected: 12, actual: " + a.getPourcentageAllegement(), 0, BigDecimal.valueOf(12L).compareTo(a.getPourcentageAllegement()));
				Assert.assertEquals(AllegementFiscal.TypeCollectivite.CANTON, a.getTypeCollectivite());
				Assert.assertEquals(AllegementFiscal.TypeImpot.CAPITAL, a.getTypeImpot());
			}
			{
				final AllegementFiscal a = allegementsTries.get(2);
				Assert.assertNotNull(a);
				Assert.assertNull(a.getAnnulationDate());
				Assert.assertEquals(RegDate.get(1956, 5, 1), a.getDateDebut());
				Assert.assertEquals(RegDate.get(1956, 12, 27), a.getDateFin());
				Assert.assertNull(a.getNoOfsCommune());
				Assert.assertEquals("Expected: 13, actual: " + a.getPourcentageAllegement(), 0, BigDecimal.valueOf(13L).compareTo(a.getPourcentageAllegement()));
				Assert.assertEquals(AllegementFiscal.TypeCollectivite.CONFEDERATION, a.getTypeCollectivite());
				Assert.assertEquals(AllegementFiscal.TypeImpot.BENEFICE, a.getTypeImpot());
			}
			{
				final AllegementFiscal a = allegementsTries.get(3);
				Assert.assertNotNull(a);
				Assert.assertNull(a.getAnnulationDate());
				Assert.assertEquals(RegDate.get(1956, 5, 1), a.getDateDebut());
				Assert.assertEquals(RegDate.get(1956, 12, 27), a.getDateFin());
				Assert.assertNull(a.getNoOfsCommune());
				Assert.assertEquals("Expected: 13, actual: " + a.getPourcentageAllegement(), 0, BigDecimal.valueOf(13L).compareTo(a.getPourcentageAllegement()));
				Assert.assertEquals(AllegementFiscal.TypeCollectivite.CONFEDERATION, a.getTypeCollectivite());
				Assert.assertEquals(AllegementFiscal.TypeImpot.CAPITAL, a.getTypeImpot());
			}
			{
				final AllegementFiscal a = allegementsTries.get(4);
				Assert.assertNotNull(a);
				Assert.assertNull(a.getAnnulationDate());
				Assert.assertEquals(RegDate.get(1957, 3, 12), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertNull(a.getNoOfsCommune());
				Assert.assertEquals("Expected: 13.5, actual: " + a.getPourcentageAllegement(), 0, BigDecimal.valueOf(135L, 1).compareTo(a.getPourcentageAllegement()));
				Assert.assertEquals(AllegementFiscal.TypeCollectivite.COMMUNE, a.getTypeCollectivite());
				Assert.assertEquals(AllegementFiscal.TypeImpot.BENEFICE, a.getTypeImpot());
			}
			{
				final AllegementFiscal a = allegementsTries.get(5);
				Assert.assertNotNull(a);
				Assert.assertNull(a.getAnnulationDate());
				Assert.assertEquals(RegDate.get(1957, 3, 12), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertNull(a.getNoOfsCommune());
				Assert.assertEquals("Expected: 13.5, actual: " + a.getPourcentageAllegement(), 0, BigDecimal.valueOf(135L, 1).compareTo(a.getPourcentageAllegement()));
				Assert.assertEquals(AllegementFiscal.TypeCollectivite.COMMUNE, a.getTypeCollectivite());
				Assert.assertEquals(AllegementFiscal.TypeImpot.CAPITAL, a.getTypeImpot());
			}
		});

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(10, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Allègement fiscal généré [01.01.1950 -> 31.03.1955], collectivité CANTON, type BENEFICE : 12%.", textes.get(1));
		Assert.assertEquals("Allègement fiscal généré [01.01.1950 -> 31.03.1955], collectivité CANTON, type CAPITAL : 12%.", textes.get(2));
		Assert.assertEquals("Allègement fiscal généré [01.05.1956 -> 27.12.1956], collectivité CONFEDERATION, type BENEFICE : 13%.", textes.get(3));
		Assert.assertEquals("Allègement fiscal généré [01.05.1956 -> 27.12.1956], collectivité CONFEDERATION, type CAPITAL : 13%.", textes.get(4));
		Assert.assertEquals("Allègement fiscal généré [12.03.1957 -> ?], collectivité COMMUNE, type BENEFICE : 13.5%.", textes.get(5));
		Assert.assertEquals("Allègement fiscal généré [12.03.1957 -> ?], collectivité COMMUNE, type CAPITAL : 13.5%.", textes.get(6));
		Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(7));
		Assert.assertEquals("Pas de commune ni de for principal associé, pas d'établissement principal créé.", textes.get(8));
		Assert.assertEquals("Entreprise migrée : 47.84.", textes.get(9));
	}

	@Test
	public void testAllegementsFiscauxTypeContribution() throws Exception {
		final long noEntreprise = 4784L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addAllegementFiscal(e, RegDate.get(1950, 1, 1), RegDate.get(1955, 3, 31), BigDecimal.valueOf(12L), RegpmCodeContribution.CAPITAL, RegpmCodeCollectivite.COMMUNE, null);
		addAllegementFiscal(e, RegDate.get(1956, 5, 1), RegDate.get(1956, 12, 27), BigDecimal.valueOf(13L), RegpmCodeContribution.BENEFICE, RegpmCodeCollectivite.COMMUNE, Commune.MORGES);
		addAllegementFiscal(e, RegDate.get(1956, 6, 1), RegDate.get(1956, 12, 28), BigDecimal.valueOf(14L), RegpmCodeContribution.BENEFICE, RegpmCodeCollectivite.COMMUNE, Commune.BALE);       // ignoré HC
		addAllegementFiscal(e, RegDate.get(1957, 3, 12), null, BigDecimal.valueOf(135L, 1), RegpmCodeContribution.IMPOT_BENEFICE_CAPITAL, RegpmCodeCollectivite.CONFEDERATION, null);           // ignoré pas le bon code de contribution
		addAllegementFiscal(e, RegDate.get(1958, 3, 12), null, BigDecimal.valueOf(134L, 1), RegpmCodeContribution.CAPITAL, RegpmCodeCollectivite.CONFEDERATION, null);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<AllegementFiscal> allegements = entreprise.getAllegementsFiscaux();
			final List<AllegementFiscal> allegementsTries = allegements.stream()
					.sorted(Comparator.comparing(AllegementFiscal::getDateDebut).thenComparing(AllegementFiscal::getTypeImpot))
					.collect(Collectors.toList());
			Assert.assertNotNull(allegementsTries);
			Assert.assertEquals(3, allegementsTries.size());
			{
				final AllegementFiscal a = allegementsTries.get(0);
				Assert.assertNotNull(a);
				Assert.assertNull(a.getAnnulationDate());
				Assert.assertEquals(RegDate.get(1950, 1, 1), a.getDateDebut());
				Assert.assertEquals(RegDate.get(1955, 3, 31), a.getDateFin());
				Assert.assertNull(a.getNoOfsCommune());
				Assert.assertEquals("Expected: 12, actual: " + a.getPourcentageAllegement(), 0, BigDecimal.valueOf(12L).compareTo(a.getPourcentageAllegement()));
				Assert.assertEquals(AllegementFiscal.TypeCollectivite.COMMUNE, a.getTypeCollectivite());
				Assert.assertEquals(AllegementFiscal.TypeImpot.CAPITAL, a.getTypeImpot());
			}
			{
				final AllegementFiscal a = allegementsTries.get(1);
				Assert.assertNotNull(a);
				Assert.assertNull(a.getAnnulationDate());
				Assert.assertEquals(RegDate.get(1956, 5, 1), a.getDateDebut());
				Assert.assertEquals(RegDate.get(1956, 12, 27), a.getDateFin());
				Assert.assertEquals(Commune.MORGES.getNoOfs(), a.getNoOfsCommune());
				Assert.assertEquals("Expected: 13, actual: " + a.getPourcentageAllegement(), 0, BigDecimal.valueOf(13L).compareTo(a.getPourcentageAllegement()));
				Assert.assertEquals(AllegementFiscal.TypeCollectivite.COMMUNE, a.getTypeCollectivite());
				Assert.assertEquals(AllegementFiscal.TypeImpot.BENEFICE, a.getTypeImpot());
			}
			{
				final AllegementFiscal a = allegementsTries.get(2);
				Assert.assertNotNull(a);
				Assert.assertNull(a.getAnnulationDate());
				Assert.assertEquals(RegDate.get(1958, 3, 12), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertNull(a.getNoOfsCommune());
				Assert.assertEquals("Expected: 13.4, actual: " + a.getPourcentageAllegement(), 0, BigDecimal.valueOf(134L, 1).compareTo(a.getPourcentageAllegement()));
				Assert.assertEquals(AllegementFiscal.TypeCollectivite.CONFEDERATION, a.getTypeCollectivite());
				Assert.assertEquals(AllegementFiscal.TypeImpot.CAPITAL, a.getTypeImpot());
			}
		});

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(9, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Allègement fiscal généré [01.01.1950 -> 31.03.1955], collectivité COMMUNE, type CAPITAL : 12%.", textes.get(1));
		Assert.assertEquals("Allègement fiscal généré [01.05.1956 -> 27.12.1956], collectivité COMMUNE (5642), type BENEFICE : 13%.", textes.get(2));
		Assert.assertEquals("Allègement fiscal 3 sur une commune hors-canton (Bâle/2701/BS) -> ignoré.", textes.get(3));
		Assert.assertEquals("Allègement fiscal 4 avec un code de contribution IMPOT_BENEFICE_CAPITAL -> ignoré.", textes.get(4));
		Assert.assertEquals("Allègement fiscal généré [12.03.1958 -> ?], collectivité CONFEDERATION, type CAPITAL : 13.4%.", textes.get(5));
		Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(6));
		Assert.assertEquals("Pas de commune ni de for principal associé, pas d'établissement principal créé.", textes.get(7));
		Assert.assertEquals("Entreprise migrée : 47.84.", textes.get(8));
	}

	@Test
	public void testExercicesCommerciaux() throws Exception {

		final long noEntreprise = 4784L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegpmAssujettissement lilic = addAssujettissement(e, RegDate.get(2000, 1, 1), null, RegpmTypeAssujettissement.LILIC);
		final RegpmAssujettissement lifd = addAssujettissement(e, RegDate.get(2000, 1, 1), null, RegpmTypeAssujettissement.LIFD);
		addForPrincipalSuisse(e, RegDate.get(2000, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);

		// 15 exercices commerciaux entre 2000 et 2014, avec des bouclements au 03.31
		for (int pf = 2000 ; pf < 2015 ; ++ pf) {
			final RegpmDossierFiscal df = addDossierFiscal(e, lilic, pf, RegDate.get(pf, 4, 5), RegpmModeImposition.POST);
			addExerciceCommercial(e, df, RegDateHelper.maximum(RegDate.get(2000, 1, 1), RegDate.get(pf - 1, 4, 1), NullDateBehavior.EARLIEST), RegDate.get(pf, 3, 31));
		}
		addDossierFiscal(e, lilic, 2015, RegDate.get(2015, 4, 5), RegpmModeImposition.POST);
		e.setDateBouclementFutur(RegDate.get(2016, 3, 31));

		// ajout des périodes fiscales dans Unireg
		doInUniregTransaction(false, status -> {
			for (int pf = 2000; pf < 2015; ++pf) {
				addPeriodeFiscale(pf);
			}
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// récupération du numéro de l'établissement principal
		final MutableLong noEtablissementPrincipal = new MutableLong();

		// vérification du contenu de la base de données (surtout pour les bouclements..)
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<Bouclement> bouclements = entreprise.getBouclements();
			Assert.assertNotNull(bouclements);
			Assert.assertEquals(1, bouclements.size());

			final Bouclement bouclement = bouclements.iterator().next();
			Assert.assertNotNull(bouclement);
			Assert.assertFalse(bouclement.isAnnule());
			Assert.assertEquals(DayMonth.get(3, 31), bouclement.getAncrage());
			Assert.assertEquals(RegDate.get(2000, 3, 1), bouclement.getDateDebut());
			Assert.assertEquals(12, bouclement.getPeriodeMois());

			// récupération du numéro fiscal de l'établissement principal généré
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissementPrincipal = etablissements.get(0);
			Assert.assertNotNull(etablissementPrincipal);
			Assert.assertTrue(etablissementPrincipal.isPrincipal());
			noEtablissementPrincipal.setValue(etablissementPrincipal.getNumero());
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(6, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Prise en compte d'une date de bouclement estimée au 31.03.2015 (un an avant la date de bouclement futur).", textes.get(1));
		Assert.assertEquals("Cycle de bouclements créé, applicable dès le 01.03.2000 : tous les 12 mois, à partir du premier 31.03.", textes.get(2));
		Assert.assertEquals(String.format("Création de l'établissement principal %s.", FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue())), textes.get(3));
		Assert.assertEquals(String.format("Domicile de l'établissement principal %s : [01.01.2000 -> ?] sur COMMUNE_OU_FRACTION_VD/5586.", FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue())), textes.get(4));
		Assert.assertEquals("Entreprise migrée : 47.84.", textes.get(5));
	}

	/**
	 * Dans RegPM, les exercices commerciaux n'étaient mappés que si une DI était envoyée (et retournée), donc en particulier
	 * il pouvait ne pas y en avoir pendant plusieurs années si l'assujettissement était interrompu.
	 * Ici, c'est le cas sans changement de cycle de bouclement.
	 */
	@Test
	public void testExercicesCommerciauxAbsentsPendantLesPeriodesDeNonAssujettissementSansChangement() throws Exception {

		final long noEntreprise = 24671L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegpmAssujettissement assujettissement1998 = addAssujettissement(e, RegDate.get(1997, 4, 1), RegDate.get(1998, 3, 31), RegpmTypeAssujettissement.LILIC);
		addForPrincipalSuisse(e, RegDate.get(1997, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BALE);
		final RegpmDossierFiscal df1998 = addDossierFiscal(e, assujettissement1998, 1998, RegDate.get(1998, 4, 21), RegpmModeImposition.POST);
		addExerciceCommercial(e, df1998, RegDate.get(1997, 4, 1), RegDate.get(1998, 3, 31));

		final RegpmAssujettissement assujettissementRecent = addAssujettissement(e, RegDate.get(2007, 5, 21), null, RegpmTypeAssujettissement.LILIC);
		for (int pf = 2008 ; pf < 2014 ; ++ pf) {
			final RegpmDossierFiscal df = addDossierFiscal(e, assujettissementRecent, pf, RegDate.get(pf, 4, 12), RegpmModeImposition.POST);
			addExerciceCommercial(e, df, RegDate.get(pf - 1, 4, 1), RegDate.get(pf, 3, 31));
		}

		// ajout des périodes fiscales dans Unireg
		doInUniregTransaction(false, status -> {
			for (int pf = 1998; pf < 2014; ++pf) {
				addPeriodeFiscale(pf);
			}
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// récupération du numéro de l'établissement principal
		final MutableLong noEtablissementPrincipal = new MutableLong();

		// vérification du contenu de la base de données (surtout pour les bouclements..)
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<Bouclement> bouclements = entreprise.getBouclements();
			Assert.assertNotNull(bouclements);
			Assert.assertEquals(1, bouclements.size());

			final Bouclement bouclement = bouclements.iterator().next();
			Assert.assertNotNull(bouclement);
			Assert.assertFalse(bouclement.isAnnule());
			Assert.assertEquals(DayMonth.get(3, 31), bouclement.getAncrage());
			Assert.assertEquals(RegDate.get(1998, 3, 1), bouclement.getDateDebut());
			Assert.assertEquals(12, bouclement.getPeriodeMois());

			// récupération du numéro fiscal de l'établissement principal généré
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissementPrincipal = etablissements.get(0);
			Assert.assertNotNull(etablissementPrincipal);
			Assert.assertTrue(etablissementPrincipal.isPrincipal());
			noEtablissementPrincipal.setValue(etablissementPrincipal.getNumero());
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(14, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2007 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(1));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2006 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(2));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2005 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(3));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2004 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(4));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2003 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(5));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2002 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(6));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2001 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(7));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2000 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(8));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.1999 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(9));
		Assert.assertEquals("Cycle de bouclements créé, applicable dès le 01.03.1998 : tous les 12 mois, à partir du premier 31.03.", textes.get(10));
		Assert.assertEquals(String.format("Création de l'établissement principal %s.", FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue())), textes.get(11));
		Assert.assertEquals(String.format("Domicile de l'établissement principal %s : [01.01.1997 -> ?] sur COMMUNE_HC/2701.", FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue())), textes.get(12));
		Assert.assertEquals("Entreprise migrée : 246.71.", textes.get(13));
	}

	/**
	 * Dans RegPM, les exercices commerciaux n'étaient mappés que si une DI était envoyée (et retournée), donc en particulier
	 * il pouvait ne pas y en avoir pendant plusieurs années si l'assujettissement était interrompu.
	 * Ici, c'est le cas avec changement de cycle de bouclement.
	 */
	@Test
	public void testExercicesCommerciauxAbsentsPendantLesPeriodesDeNonAssujettissementAvecChangement() throws Exception {

		final long noEntreprise = 24671L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegpmAssujettissement assujettissement1998 = addAssujettissement(e, RegDate.get(1997, 4, 1), RegDate.get(1998, 3, 31), RegpmTypeAssujettissement.LILIC);
		addForPrincipalSuisse(e, RegDate.get(1997, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BALE);
		final RegpmDossierFiscal df1998 = addDossierFiscal(e, assujettissement1998, 1998, RegDate.get(1998, 4, 21), RegpmModeImposition.POST);
		addExerciceCommercial(e, df1998, RegDate.get(1997, 4, 1), RegDate.get(1998, 3, 31));

		final RegpmAssujettissement assujettissementRecent = addAssujettissement(e, RegDate.get(2007, 5, 21), null, RegpmTypeAssujettissement.LILIC);
		for (int pf = 2008 ; pf < 2014 ; ++ pf) {
			final RegpmDossierFiscal df = addDossierFiscal(e, assujettissementRecent, pf, RegDate.get(pf, 1, 12), RegpmModeImposition.POST);
			addExerciceCommercial(e, df, RegDate.get(pf, 1, 1), RegDate.get(pf, 12, 31));
		}

		// ajout des périodes fiscales dans Unireg
		doInUniregTransaction(false, status -> {
			for (int pf = 1998; pf < 2014; ++pf) {
				addPeriodeFiscale(pf);
			}
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// récupération du numéro de l'établissement principal
		final MutableLong noEtablissementPrincipal = new MutableLong();

		// vérification du contenu de la base de données (surtout pour les bouclements..)
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<Bouclement> bouclements = entreprise.getBouclements();
			Assert.assertNotNull(bouclements);
			Assert.assertEquals(2, bouclements.size());

			final List<Bouclement> bouclementsTries = bouclements.stream().sorted(Comparator.comparing(Bouclement::getDateDebut)).collect(Collectors.toList());
			{
				final Bouclement bouclement = bouclementsTries.get(0);
				Assert.assertNotNull(bouclement);
				Assert.assertFalse(bouclement.isAnnule());
				Assert.assertEquals(DayMonth.get(3, 31), bouclement.getAncrage());
				Assert.assertEquals(RegDate.get(1998, 3, 1), bouclement.getDateDebut());
				Assert.assertEquals(9, bouclement.getPeriodeMois());
			}
			{
				final Bouclement bouclement = bouclementsTries.get(1);
				Assert.assertNotNull(bouclement);
				Assert.assertFalse(bouclement.isAnnule());
				Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
				Assert.assertEquals(RegDate.get(1999, 9, 1), bouclement.getDateDebut());
				Assert.assertEquals(12, bouclement.getPeriodeMois());
			}

			// récupération du numéro fiscal de l'établissement principal généré
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissementPrincipal = etablissements.get(0);
			Assert.assertNotNull(etablissementPrincipal);
			Assert.assertTrue(etablissementPrincipal.isPrincipal());
			noEtablissementPrincipal.setValue(etablissementPrincipal.getNumero());
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(16, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2007 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(1));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2006 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(2));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2005 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(3));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2004 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(4));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2003 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(5));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2002 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(6));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2001 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(7));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2000 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(8));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.1999 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(9));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.1998 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(10));
		Assert.assertEquals("Cycle de bouclements créé, applicable dès le 01.03.1998 : tous les 9 mois, à partir du premier 31.03.", textes.get(11));
		Assert.assertEquals("Cycle de bouclements créé, applicable dès le 01.09.1999 : tous les 12 mois, à partir du premier 31.12.", textes.get(12));
		Assert.assertEquals(String.format("Création de l'établissement principal %s.", FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue())), textes.get(13));
		Assert.assertEquals(String.format("Domicile de l'établissement principal %s : [01.01.1997 -> ?] sur COMMUNE_HC/2701.", FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue())), textes.get(14));
		Assert.assertEquals("Entreprise migrée : 246.71.", textes.get(15));
	}

	/**
	 * Cas de la PM 32414, pour laquelle la date de bouclement futur est en 1992, alors que le for vaudois et les assujettissements sont toujours ouverts,
	 * et que le seul exercice commercial existant est en 2001
	 */
	@Test
	public void testDateBouclementFuturDansLePasseDesExcercicesCommerciauxExistant() throws Exception {

		final long noEntreprise = 32414;
		final RegDate dateBouclementFutur = RegDate.get(1992, 12, 31);

		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		e.setDateBouclementFutur(dateBouclementFutur);
		addForPrincipalSuisse(e, RegDate.get(1960, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.MORGES);
		final RegpmAssujettissement a = addAssujettissement(e, RegDate.get(1960, 1, 1), null, RegpmTypeAssujettissement.LILIC);
		final RegpmDossierFiscal df = addDossierFiscal(e, a, 2001, RegDate.get(2001, 12, 12), RegpmModeImposition.POST);
		addExerciceCommercial(e, df, RegDate.get(2001, 1, 1), RegDate.get(2001, 12, 31));

		// ajout des périodes fiscales dans Unireg
		doInUniregTransaction(false, status -> {
			addPeriodeFiscale(2001);
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// récupération du numéro de l'établissement principal
		final MutableLong noEtablissementPrincipal = new MutableLong();

		// vérification du contenu de la base de données (surtout pour les bouclements..)
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<Bouclement> bouclements = entreprise.getBouclements();
			Assert.assertNotNull(bouclements);
			Assert.assertEquals(1, bouclements.size());

			final Bouclement bouclement = bouclements.iterator().next();
			Assert.assertNotNull(bouclement);
			Assert.assertFalse(bouclement.isAnnule());
			Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			Assert.assertEquals(RegDate.get(2001, 12, 1), bouclement.getDateDebut());
			Assert.assertEquals(12, bouclement.getPeriodeMois());

			// récupération du numéro fiscal de l'établissement principal généré
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissementPrincipal = etablissements.get(0);
			Assert.assertNotNull(etablissementPrincipal);
			Assert.assertTrue(etablissementPrincipal.isPrincipal());
			noEtablissementPrincipal.setValue(etablissementPrincipal.getNumero());
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(6, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Date de bouclement futur (31.12.1992) ignorée car antérieure à la date de fin du dernier exercice commercial connu (31.12.2001).", textes.get(1));
		Assert.assertEquals("Cycle de bouclements créé, applicable dès le 01.12.2001 : tous les 12 mois, à partir du premier 31.12.", textes.get(2));
		Assert.assertEquals(String.format("Création de l'établissement principal %s.", FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue())), textes.get(3));
		Assert.assertEquals(String.format("Domicile de l'établissement principal %s : [01.01.1960 -> ?] sur COMMUNE_OU_FRACTION_VD/5642.", FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue())), textes.get(4));
		Assert.assertEquals("Entreprise migrée : 324.14.", textes.get(5));
	}

	@Test
	public void testForPrincipalAvecDateDansLeFutur() throws Exception {

		final long noEntreprise = 4815;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, RegDate.get(2010, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.MORGES);
		addForPrincipalSuisse(e, RegDate.get().addMonths(3), RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);

		final RegpmAssujettissement a = addAssujettissement(e, RegDate.get(2010, 1, 1), null, RegpmTypeAssujettissement.LILIC);
		final RegpmDossierFiscal df = addDossierFiscal(e, a, 2010, RegDate.get(2010, 12, 20), RegpmModeImposition.POST);
		addExerciceCommercial(e, df, RegDate.get(2010, 1, 1), RegDate.get(2010, 12, 31));

		// ajout des périodes fiscales dans Unireg
		doInUniregTransaction(false, status -> {
			addPeriodeFiscale(2010);
		});

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base de données
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<Bouclement> bouclements = entreprise.getBouclements();
			Assert.assertNotNull(bouclements);
			Assert.assertEquals(1, bouclements.size());

			final Bouclement bouclement = bouclements.iterator().next();
			Assert.assertNotNull(bouclement);
			Assert.assertFalse(bouclement.isAnnule());
			Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
			Assert.assertEquals(RegDate.get(2010, 12, 1), bouclement.getDateDebut());
			Assert.assertEquals(12, bouclement.getPeriodeMois());

			final List<ForFiscalPrincipalPM> ffps = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(ffps);
			Assert.assertEquals(1, ffps.size());       // l'autre doit avoir été ignoré (= date dans le futur) !

			final ForFiscalPrincipalPM ffp = ffps.get(0);
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(RegDate.get(2010, 1, 1), ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.MORGES.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
		});

		// vérification des messages dans le contexte "FORS"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(2, textes.size());
		Assert.assertEquals("Le for principal 2 est ignoré car il a une date de début dans le futur (" + RegDateHelper.dateToDisplayString(RegDate.get().addMonths(3)) + ").", textes.get(0));
		Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5642 [01.01.2010 -> ?] généré.", textes.get(1));
	}

	/**
	 * La commune de Zürich n'a le numéro OFS 261 que depuis le 01.01.1990 (avant, c'était 253, c'est en tout cas ce que dit RefInf)
	 * mais RegPM a toujours utilisé le numéro 261... même pour les fors d'avant 1990 -> c'est malheureusement invalide dans Unireg
	 * (= for ouvert en dehors de la période de validité de la commune dans RefInf), donc un mapping doit être fait dans la migration
	 * (= dans notre exemple : avant 1990 -> 253, après -> 261)
	 */
	@Test
	public void testFusionCommuneLointaineInconnueDeRegpmSiege() throws Exception {

		Assert.assertEquals((Integer) 261, Commune.ZURICH.getNoOfs());

		final long noEntreprise = 53465L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, RegDate.get(1977, 4, 7), RegpmTypeForPrincipal.SIEGE, Commune.ZURICH);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base de données
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			final List<ForFiscalPrincipalPM> ffps = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(ffps);
			Assert.assertEquals(2, ffps.size());       // avant et après la fusion ZH

			{
				final ForFiscalPrincipalPM ffp = ffps.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(RegDate.get(1977, 4, 7), ffp.getDateDebut());
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

			final List<DecisionAci> decisions = entreprise.getDecisionsSorted();
			Assert.assertNotNull(decisions);
			Assert.assertEquals(0, decisions.size());
		});

		// vérification des messages dans le contexte "FORS"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(4, textes.size());
		Assert.assertEquals("Entité ForFiscalPrincipalPM [07.04.1977 -> ?] sur COMMUNE_HC/261 au moins partiellement remplacée par ForFiscalPrincipalPM [07.04.1977 -> 31.12.1989] sur COMMUNE_HC/253 pour suivre les fusions de communes.", textes.get(0));
		Assert.assertEquals("Entité ForFiscalPrincipalPM [07.04.1977 -> ?] sur COMMUNE_HC/261 au moins partiellement remplacée par ForFiscalPrincipalPM [01.01.1990 -> ?] sur COMMUNE_HC/261 pour suivre les fusions de communes.", textes.get(1));
		Assert.assertEquals("For principal COMMUNE_HC/253 [07.04.1977 -> 31.12.1989] généré.", textes.get(2));
		Assert.assertEquals("For principal COMMUNE_HC/261 [01.01.1990 -> ?] généré.", textes.get(3));
	}

	/**
	 * La commune de Zürich n'a le numéro OFS 261 que depuis le 01.01.1990 (avant, c'était 253, c'est en tout cas ce que dit RefInf)
	 * mais RegPM a toujours utilisé le numéro 261... même pour les fors d'avant 1990 -> c'est malheureusement invalide dans Unireg
	 * (= for ouvert en dehors de la période de validité de la commune dans RefInf), donc un mapping doit être fait dans la migration
	 * (= dans notre exemple : avant 1990 -> 253, après -> 261)
	 */
	@Test
	public void testFusionCommuneLointaineInconnueDeRegpmAdministrationEffective() throws Exception {

		Assert.assertEquals((Integer) 261, Commune.ZURICH.getNoOfs());

		final long noEntreprise = 53465L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, RegDate.get(1977, 4, 7), RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE, Commune.ZURICH);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base de données
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			final List<ForFiscalPrincipalPM> ffps = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(ffps);
			Assert.assertEquals(2, ffps.size());       // avant et après la fusion ZH

			{
				final ForFiscalPrincipalPM ffp = ffps.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(RegDate.get(1977, 4, 7), ffp.getDateDebut());
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

			final List<DecisionAci> decisions = entreprise.getDecisionsSorted();
			Assert.assertNotNull(decisions);
			Assert.assertEquals(2, decisions.size());
			{
				final DecisionAci decision = decisions.get(0);
				Assert.assertNotNull(decision);
				Assert.assertFalse(decision.isAnnule());
				Assert.assertEquals(RegDate.get(1977, 4, 7), decision.getDateDebut());
				Assert.assertEquals(RegDate.get(1989, 12, 31), decision.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, decision.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) 253, decision.getNumeroOfsAutoriteFiscale());
			}
			{
				final DecisionAci decision = decisions.get(1);
				Assert.assertNotNull(decision);
				Assert.assertFalse(decision.isAnnule());
				Assert.assertEquals(RegDate.get(1990, 1, 1), decision.getDateDebut());
				Assert.assertNull(decision.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, decision.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) 261, decision.getNumeroOfsAutoriteFiscale());
			}
		});

		// vérification des messages dans le contexte "FORS"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(8, textes.size());
		Assert.assertEquals("Entité ForFiscalPrincipalPM [07.04.1977 -> ?] sur COMMUNE_HC/261 au moins partiellement remplacée par ForFiscalPrincipalPM [07.04.1977 -> 31.12.1989] sur COMMUNE_HC/253 pour suivre les fusions de communes.", textes.get(0));
		Assert.assertEquals("Entité ForFiscalPrincipalPM [07.04.1977 -> ?] sur COMMUNE_HC/261 au moins partiellement remplacée par ForFiscalPrincipalPM [01.01.1990 -> ?] sur COMMUNE_HC/261 pour suivre les fusions de communes.", textes.get(1));
		Assert.assertEquals("For principal COMMUNE_HC/253 [07.04.1977 -> 31.12.1989] généré.", textes.get(2));
		Assert.assertEquals("For principal COMMUNE_HC/261 [01.01.1990 -> ?] généré.", textes.get(3));
		Assert.assertEquals("Entité DecisionAci [07.04.1977 -> ?] sur COMMUNE_HC/261 au moins partiellement remplacée par DecisionAci [07.04.1977 -> 31.12.1989] sur COMMUNE_HC/253 pour suivre les fusions de communes.", textes.get(4));
		Assert.assertEquals("Entité DecisionAci [07.04.1977 -> ?] sur COMMUNE_HC/261 au moins partiellement remplacée par DecisionAci [01.01.1990 -> ?] sur COMMUNE_HC/261 pour suivre les fusions de communes.", textes.get(5));
		Assert.assertEquals("Décision ACI COMMUNE_HC/253 [07.04.1977 -> 31.12.1989] générée.", textes.get(6));
		Assert.assertEquals("Décision ACI COMMUNE_HC/261 [01.01.1990 -> ?] générée.", textes.get(7));
	}

	@Test
	public void testForPrincipalSurTerritoire() throws Exception {

		final long noEntreprise = 53465L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalEtranger(e, RegDate.get(1977, 4, 7), RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE, MockPays.Gibraltar.getNoOFS());

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// récupération du numéro de l'établissement principal
		final MutableLong noEtablissementPrincipal = new MutableLong();

		// vérification du contenu de la base de données
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			final List<ForFiscalPrincipalPM> ffps = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(ffps);
			Assert.assertEquals(1, ffps.size());

			final ForFiscalPrincipalPM ffp = ffps.get(0);
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(RegDate.get(1977, 4, 7), ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getMotifFermeture());

			final List<DecisionAci> decisions = entreprise.getDecisionsSorted();
			Assert.assertNotNull(decisions);
			Assert.assertEquals(1, decisions.size());

			final DecisionAci decision = decisions.get(0);
			Assert.assertNotNull(decision);
			Assert.assertFalse(decision.isAnnule());
			Assert.assertEquals(RegDate.get(1977, 4, 7), decision.getDateDebut());
			Assert.assertNull(decision.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, decision.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), decision.getNumeroOfsAutoriteFiscale());

			// établissement principal et son domicile
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissement = etablissements.get(0);
			Assert.assertNotNull(etablissement);
			Assert.assertFalse(etablissement.isAnnule());
			Assert.assertTrue(etablissement.isPrincipal());
			noEtablissementPrincipal.setValue(etablissement.getNumero());

			final Set<DomicileEtablissement> domiciles = etablissement.getDomiciles();
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(1, domiciles.size());

			final DomicileEtablissement domicile = domiciles.iterator().next();
			Assert.assertNotNull(domicile);
			Assert.assertFalse(domicile.isAnnule());
			Assert.assertEquals(RegDate.get(1977, 4, 7), domicile.getDateDebut());
			Assert.assertNull(domicile.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, domicile.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(3, textes.size());
			Assert.assertEquals("Le pays 8213 du for principal 1 n'est pas un état souverain, for déplacé sur l'état 8215.", textes.get(0));
			Assert.assertEquals("For principal PAYS_HS/8215 [07.04.1977 -> ?] généré.", textes.get(1));
			Assert.assertEquals("Décision ACI PAYS_HS/8215 [07.04.1977 -> ?] générée.", textes.get(2));
		}

		// vérification des messages dans le contexte "SUIVI"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(6, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(1));
			Assert.assertEquals("Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + ".", textes.get(2));
			Assert.assertEquals("Le pays 8213 du siège n'est pas un état souverain, déplacé sur l'état 8215.", textes.get(3));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [07.04.1977 -> ?] sur PAYS_HS/8215.", textes.get(4));
			Assert.assertEquals("Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", textes.get(5));
		}
	}

	@Test
	public void testForPrincipalSurTerritoireExGibraltar() throws Exception {

		final long noEntreprise = 53465L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalEtranger(e, RegDate.get(1977, 4, 7), RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE, 8997);      // et oui, dans le mainframe, c'est 'Ex-Gibraltar', qui n'a pas été repris dans FiDoR

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// récupération du numéro de l'établissement principal
		final MutableLong noEtablissementPrincipal = new MutableLong();

		// vérification du contenu de la base de données
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			final List<ForFiscalPrincipalPM> ffps = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(ffps);
			Assert.assertEquals(1, ffps.size());

			final ForFiscalPrincipalPM ffp = ffps.get(0);
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(RegDate.get(1977, 4, 7), ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getMotifFermeture());

			final List<DecisionAci> decisions = entreprise.getDecisionsSorted();
			Assert.assertNotNull(decisions);
			Assert.assertEquals(1, decisions.size());

			final DecisionAci decision = decisions.get(0);
			Assert.assertNotNull(decision);
			Assert.assertFalse(decision.isAnnule());
			Assert.assertEquals(RegDate.get(1977, 4, 7), decision.getDateDebut());
			Assert.assertNull(decision.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, decision.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), decision.getNumeroOfsAutoriteFiscale());

			// établissement principal et son domicile
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissement = etablissements.get(0);
			Assert.assertNotNull(etablissement);
			Assert.assertFalse(etablissement.isAnnule());
			Assert.assertTrue(etablissement.isPrincipal());
			noEtablissementPrincipal.setValue(etablissement.getNumero());

			final Set<DomicileEtablissement> domiciles = etablissement.getDomiciles();
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(1, domiciles.size());

			final DomicileEtablissement domicile = domiciles.iterator().next();
			Assert.assertNotNull(domicile);
			Assert.assertFalse(domicile.isAnnule());
			Assert.assertEquals(RegDate.get(1977, 4, 7), domicile.getDateDebut());
			Assert.assertNull(domicile.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, domicile.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), domicile.getNumeroOfsAutoriteFiscale());
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(3, textes.size());
			Assert.assertEquals("Le pays 8997 du for principal 1 n'est pas un état souverain, for déplacé sur l'état 8215.", textes.get(0));
			Assert.assertEquals("For principal PAYS_HS/8215 [07.04.1977 -> ?] généré.", textes.get(1));
			Assert.assertEquals("Décision ACI PAYS_HS/8215 [07.04.1977 -> ?] générée.", textes.get(2));
		}

		// vérification des messages dans le contexte "SUIVI"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(6, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(1));
			Assert.assertEquals("Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + ".", textes.get(2));
			Assert.assertEquals("Le pays 8997 du siège n'est pas un état souverain, déplacé sur l'état 8215.", textes.get(3));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [07.04.1977 -> ?] sur PAYS_HS/8215.", textes.get(4));
			Assert.assertEquals("Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", textes.get(5));
		}
	}

	/**
	 * Cas éventuellement problèmatique quand le for de RegPM est sur une commune qui n'est valide (= dans RefINF)
	 * que sur un intervale sans intersection (ni même collé) avec les dates du for
	 */
	@Test
	public void testAlgorithmeFusionsCommunesIncomplet() throws Exception {

		final long noEntreprise = 68002;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, RegDate.get(1936, 4, 2), RegpmTypeForPrincipal.SIEGE, Commune.WIL);        // commune valide dans RefINF/FiDoR dès le 01.01.2013
		addForPrincipalSuisse(e, RegDate.get(2001, 7, 1), RegpmTypeForPrincipal.SIEGE, Commune.ZURICH);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// récupération du numéro de l'établissement principal
		final MutableLong noEtablissementPrincipal = new MutableLong();

		// vérification du contenu de la base de données
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			// vérification des fors fiscaux principaux générés
			final List<ForFiscalPrincipalPM> ffps = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(ffps);
			Assert.assertEquals(2, ffps.size());
			{
				final ForFiscalPrincipalPM ffp = ffps.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(RegDate.get(1936, 4, 2), ffp.getDateDebut());
				Assert.assertEquals(RegDate.get(2001, 6, 30), ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) 3421, ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifFermeture());
			}
			{
				final ForFiscalPrincipalPM ffp = ffps.get(1);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(RegDate.get(2001, 7, 1), ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.ZURICH.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getMotifFermeture());
			}

			// récupération du numéro fiscal de l'établissement principal généré
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissementPrincipal = etablissements.get(0);
			Assert.assertNotNull(etablissementPrincipal);
			Assert.assertTrue(etablissementPrincipal.isPrincipal());
			noEtablissementPrincipal.setValue(etablissementPrincipal.getNumero());
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());

		// analyse des logs

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(4, textes.size());
			Assert.assertEquals("Entité ForFiscalPrincipalPM [02.04.1936 -> 30.06.2001] sur COMMUNE_HC/3427 : plusieurs communes connues à l'origine de la commune 3427 avant le 01.01.2013 (on prend la première) : 3421, 3425.", textes.get(0));
			Assert.assertEquals("Entité ForFiscalPrincipalPM [02.04.1936 -> 30.06.2001] sur COMMUNE_HC/3427 au moins partiellement remplacée par ForFiscalPrincipalPM [02.04.1936 -> 30.06.2001] sur COMMUNE_HC/3421 pour suivre les fusions de communes.", textes.get(1));
			Assert.assertEquals("For principal COMMUNE_HC/3421 [02.04.1936 -> 30.06.2001] généré.", textes.get(2));
			Assert.assertEquals("For principal COMMUNE_HC/261 [01.07.2001 -> ?] généré.", textes.get(3));
		}

		// vérification des messages dans le contexte "SUIVI"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(8, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(1));
			Assert.assertEquals("Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + ".", textes.get(2));
			Assert.assertEquals("Entité DomicileEtablissement [02.04.1936 -> 30.06.2001] sur COMMUNE_HC/3427 : plusieurs communes connues à l'origine de la commune 3427 avant le 01.01.2013 (on prend la première) : 3421, 3425.", textes.get(3));
			Assert.assertEquals("Entité DomicileEtablissement [02.04.1936 -> 30.06.2001] sur COMMUNE_HC/3427 au moins partiellement remplacée par DomicileEtablissement [02.04.1936 -> 30.06.2001] sur COMMUNE_HC/3421 pour suivre les fusions de communes.", textes.get(4));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [02.04.1936 -> 30.06.2001] sur COMMUNE_HC/3421.", textes.get(5));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [01.07.2001 -> ?] sur COMMUNE_HC/261.", textes.get(6));
			Assert.assertEquals("Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", textes.get(7));
		}
	}

	@Test
	public void testForSurCommuneFaitiere() throws Exception {

		final long noEntreprise = 4545;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, RegDate.get(2000, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.LE_CHENIT);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(2, textes.size());
			Assert.assertEquals("La commune de l'entité ForFiscalPrincipalPM [01.01.2000 -> ?] sur COMMUNE_OU_FRACTION_VD/5872 est une commune faîtière de fractions, elle sera déplacée sur la PREMIERE fraction correspondante : 8000, 8001, 8002, 8003 !", textes.get(0));
			Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/8000 [01.01.2000 -> ?] généré.", textes.get(1));
		}
	}

	// TODO il reste encore plein de tests à faire...
}
