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
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.uniregctb.adapter.rcent.service.RCEntAdapter;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.MigrationResultCollector;
import ch.vd.uniregctb.migration.pm.communes.FractionsCommuneProvider;
import ch.vd.uniregctb.migration.pm.communes.FusionCommunesProvider;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.engine.helpers.DoublonProvider;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.mapping.IdMapper;
import ch.vd.uniregctb.migration.pm.regpm.NumeroIDE;
import ch.vd.uniregctb.migration.pm.regpm.RaisonSociale;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAdresseEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAllegementFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAppartenanceGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmBlocNotesEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCapital;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCategoriePersonneMorale;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCodeCollectivite;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCodeContribution;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDecisionTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDemandeDelaiSommation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntity;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEnvironnementTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtatEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmExerciceCommercial;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForSecondaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFormeJuridique;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFusion;
import ch.vd.uniregctb.migration.pm.regpm.RegpmGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmImmeuble;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmLocalitePostale;
import ch.vd.uniregctb.migration.pm.regpm.RegpmMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmModeImposition;
import ch.vd.uniregctb.migration.pm.regpm.RegpmMotifEnvoi;
import ch.vd.uniregctb.migration.pm.regpm.RegpmObjectImpot;
import ch.vd.uniregctb.migration.pm.regpm.RegpmPrononceFaillite;
import ch.vd.uniregctb.migration.pm.regpm.RegpmQuestionnaireSNC;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRattachementProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscalCH;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscalVD;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRue;
import ch.vd.uniregctb.migration.pm.regpm.RegpmSiegeEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAdresseEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeContribution;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeDemandeDelai;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDecisionTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDemandeDelai;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatQuestionnaireSNC;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeFormeJuridique;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeNatureDecisionTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeRegimeFiscal;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.DatesParticulieres;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DonneesRegistreCommerce;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.TypeTiers;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.EtatDelaiDeclaration;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

public class EntrepriseMigratorTest extends AbstractEntityMigratorTest {

	private EntrepriseMigrator migrator;
	private UniregStore uniregStore;

	@Override
	protected void onSetup() throws Exception {
		super.onSetup();
		uniregStore = getBean(UniregStore.class, "uniregStore");
		migrator = buildMigrator(false);        // par défaut, on n'active pas RCEnt
	}

	/**
	 * Méthode externalisée afin de permettre à un test ou l'autre de changer le comportement
	 * vis-à-vis de RCEnt pour ses propres besoins
	 * @param rcentEnabled <code>true</code> si RCEnt doit être intégré à la migration
	 * @return une nouvelle instance de {@link EntrepriseMigrator}
	 */
	private EntrepriseMigrator buildMigrator(boolean rcentEnabled) {
		return new EntrepriseMigrator(
				uniregStore,
				entreprise -> true,             // tout le monde est actif dans ces tests!!
				getBean(ServiceInfrastructureService.class, "serviceInfrastructureService"),
				getBean(BouclementService.class, "bouclementService"),
				getBean(AssujettissementService.class, "assujettissementService"),
				getBean(RCEntAdapter.class, "rcEntAdapter"),
				getBean(AdresseHelper.class, "adresseHelper"),
				getBean(FusionCommunesProvider.class, "fusionCommunesProvider"),
				getBean(FractionsCommuneProvider.class, "fractionsCommuneProvider"),
				getBean(DatesParticulieres.class, "datesParticulieres"),
				getBean(PeriodeImpositionService.class, "periodeImpositionService"),
				getBean(ParametreAppService.class, "parametreAppService"),
				rcentEnabled,
				getBean(DoublonProvider.class, "doublonProvider"));
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
		entreprise.setMandants(new HashSet<>());
		entreprise.setQuestionnairesSNC(new TreeSet<>());
		entreprise.setRadiationsRC(new TreeSet<>());
		entreprise.setRaisonsSociales(new TreeSet<>());
		entreprise.setRattachementsProprietaires(new HashSet<>());
		entreprise.setRegimesFiscauxCH(new TreeSet<>());
		entreprise.setRegimesFiscauxVD(new TreeSet<>());
		entreprise.setSieges(new TreeSet<>());
		entreprise.setNotes(new TreeSet<>());
		entreprise.setCriteresSegmentation(new HashSet<>());

		return entreprise;
	}

	static NumeroIDE buildNumeroIDE(String categorie, long identifiant) {
		Assert.assertEquals(3, categorie.length());
		Assert.assertTrue(identifiant >= 0);
		Assert.assertTrue(identifiant < 1000000000L);

		final NumeroIDE numeroIDE = new NumeroIDE();
		numeroIDE.setCategorie(categorie);
		numeroIDE.setNumero(identifiant);
		return numeroIDE;
	}

	static RaisonSociale addRaisonSociale(RegpmEntreprise entreprise, RegDate dateDebut, String ligne1, String ligne2, String ligne3, boolean last) {
		final RaisonSociale data = new RaisonSociale();
		data.setId(ID_GENERATOR.next());
		assignMutationVisa(data, REGPM_VISA, REGPM_MODIF);
		data.setDateValidite(dateDebut);
		data.setLigne1(ligne1);
		data.setLigne2(ligne2);
		data.setLigne3(ligne3);
		entreprise.getRaisonsSociales().add(data);

		if (last) {
			entreprise.setRaisonSociale1(ligne1);
			entreprise.setRaisonSociale2(ligne2);
			entreprise.setRaisonSociale3(ligne3);
		}
		return data;
	}

	static RegpmTypeFormeJuridique createTypeFormeJuridique(String code, RegpmCategoriePersonneMorale categorie) {
		final RegpmTypeFormeJuridique forme = new RegpmTypeFormeJuridique();
		forme.setCode(code);
		forme.setCategorie(categorie);
		return forme;
	}

	static RegpmFormeJuridique addFormeJuridique(RegpmEntreprise entreprise, RegDate dateDebut, RegpmTypeFormeJuridique type) {
		final RegpmFormeJuridique data = new RegpmFormeJuridique();
		data.setPk(new RegpmFormeJuridique.PK(computeNewSeqNo(entreprise.getFormesJuridiques(), x -> x.getPk().getSeqNo()), entreprise.getId()));
		assignMutationVisa(data, REGPM_VISA, REGPM_MODIF);
		data.setDateValidite(dateDebut);
		data.setType(type);
		entreprise.getFormesJuridiques().add(data);
		return data;
	}

	static RegpmCapital addCapital(RegpmEntreprise entreprise, RegDate dateDebut, long montant) {
		final RegpmCapital capital = new RegpmCapital();
		capital.setId(new RegpmCapital.PK(computeNewSeqNo(entreprise.getCapitaux(), x -> x.getId().getSeqNo()), entreprise.getId()));
		assignMutationVisa(capital, REGPM_VISA, REGPM_MODIF);
		capital.setDateEvolutionCapital(dateDebut);
		capital.setCapitalLibere(BigDecimal.valueOf(montant));
		entreprise.getCapitaux().add(capital);
		return capital;
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

	static RegpmDemandeDelaiSommation addDemandeDelai(RegpmDossierFiscal dossierFiscal, RegpmTypeDemandeDelai type, RegpmTypeEtatDemandeDelai etat, RegDate dateDemande, RegDate dateEnvoiCourrier, RegDate dateDelaiAccorde) {
		final RegpmDemandeDelaiSommation demande = new RegpmDemandeDelaiSommation();
		demande.setId(new RegpmDemandeDelaiSommation.PK(computeNewSeqNo(dossierFiscal.getDemandesDelai(), d -> d.getId().getNoSequence()), dossierFiscal.getId().getSeqNo(), dossierFiscal.getId().getIdAssujettissement()));
		assignMutationVisa(demande, REGPM_VISA, REGPM_MODIF);
		demande.setType(type);
		demande.setEtat(etat);
		demande.setDateDemande(dateDemande);
		demande.setDateEnvoi(dateEnvoiCourrier);
		demande.setDelaiAccorde(dateDelaiAccorde);
		dossierFiscal.getDemandesDelai().add(demande);
		return demande;
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

	static RegpmQuestionnaireSNC addQuestionnaireSNC(RegpmEntreprise e, int annee, RegpmTypeEtatQuestionnaireSNC etat) {
		final RegpmQuestionnaireSNC questionnaire = new RegpmQuestionnaireSNC();
		questionnaire.setId(new RegpmQuestionnaireSNC.PK(computeNewSeqNo(e.getQuestionnairesSNC(), x -> x.getId().getSeqNo()), e.getId()));
		assignMutationVisa(questionnaire, REGPM_VISA, REGPM_MODIF);
		questionnaire.setAnneeFiscale(annee);
		questionnaire.setEtat(etat);
		if (etat == RegpmTypeEtatQuestionnaireSNC.ANNULE) {
			questionnaire.setDateAnnulation(RegDate.get());
		}

		final List<RegpmQuestionnaireSNC> questionnairesMemeAnnee = e.getQuestionnairesSNC().stream()
				.filter(q -> q.getAnneeFiscale() == annee)
				.collect(Collectors.toList());
		questionnaire.setNoParAnnee(computeNewSeqNo(questionnairesMemeAnnee, RegpmQuestionnaireSNC::getNoParAnnee));
		e.getQuestionnairesSNC().add(questionnaire);
		return questionnaire;
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
			final RegpmIndividu individuMandataire = (RegpmIndividu) mandataire;
			mandat.setMandataireIndividu(individuMandataire);
			individuMandataire.getMandants().add(mandat);
		}
		else if (mandataire instanceof RegpmEtablissement) {
			final RegpmEtablissement etablissementMandataire = (RegpmEtablissement) mandataire;
			mandat.setMandataireEtablissement(etablissementMandataire);
			etablissementMandataire.getMandants().add(mandat);
		}
		else if (mandataire instanceof RegpmEntreprise) {
			final RegpmEntreprise entrepriseMandataire = (RegpmEntreprise) mandataire;
			mandat.setMandataireEntreprise(entrepriseMandataire);
			entrepriseMandataire.getMandants().add(mandat);
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

	private static RegpmSiegeEntreprise addSiege(RegpmEntreprise entreprise, RegDate dateDebut, @Nullable RegpmCommune commune, @Nullable Integer noOfsPays) {
		final RegpmSiegeEntreprise siege = new RegpmSiegeEntreprise();
		siege.setId(new RegpmSiegeEntreprise.PK(computeNewSeqNo(entreprise.getSieges(), x -> x.getId().getSeqNo()), entreprise.getId()));
		assignMutationVisa(siege, REGPM_VISA, REGPM_MODIF);
		siege.setCommune(commune);
		siege.setNoOfsPays(noOfsPays);
		siege.setDateValidite(dateDebut);
		entreprise.getSieges().add(siege);
		return siege;
	}

	static RegpmSiegeEntreprise addSiegeSuisse(RegpmEntreprise entreprise, RegDate dateDebut, @NotNull RegpmCommune commune) {
		return addSiege(entreprise, dateDebut, commune, null);
	}

	static RegpmSiegeEntreprise addSiegeEtranger(RegpmEntreprise entreprise, RegDate dateDebut, @NotNull Integer noOfsPays) {
		return addSiege(entreprise, dateDebut, null, noOfsPays);
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

	static RegpmEtatEntreprise addEtatEntreprise(RegpmEntreprise entreprise, RegDate dateValidite, RegpmTypeEtatEntreprise typeEtat) {
		final RegpmEtatEntreprise etat = new RegpmEtatEntreprise();
		etat.setId(new RegpmEtatEntreprise.PK(computeNewSeqNo(entreprise.getEtatsEntreprise(), x -> x.getId().getSeqNo()), entreprise.getId()));
		etat.setDateValidite(dateValidite);
		etat.setTypeEtat(typeEtat);
		etat.setLiquidations(new TreeSet<>());
		etat.setPrononcesFaillite(new TreeSet<>());
		entreprise.getEtatsEntreprise().add(etat);
		return etat;
	}

	static RegpmPrononceFaillite addPrononceFaillite(RegpmEtatEntreprise etat, RegDate datePrononceFaillite) {
		final RegpmPrononceFaillite prononce = new RegpmPrononceFaillite();
		prononce.setId(new RegpmPrononceFaillite.PK(computeNewSeqNo(etat.getPrononcesFaillite(), x -> x.getId().getNoSeq()), etat.getId().getSeqNo(), etat.getId().getIdEntreprise()));
		prononce.setDatePrononceFaillite(datePrononceFaillite);
		prononce.setFinsFaillite(new HashSet<>());
		prononce.setLiquidateurs(new HashSet<>());
		etat.getPrononcesFaillite().add(prononce);
		return prononce;
	}

	static RegpmPrononceFaillite addPrononceFaillite(RegpmEntreprise entreprise, RegDate dateValiditeEtat, RegpmTypeEtatEntreprise typeEtat, RegDate datePrononceFaillite) {
		final RegpmEtatEntreprise etat = addEtatEntreprise(entreprise, dateValiditeEtat, typeEtat);
		return addPrononceFaillite(etat, datePrononceFaillite);
	}

	static RegpmFusion addFusion(RegpmEntreprise avant, RegpmEntreprise apres, RegDate dateBilan) {
		final RegpmFusion fusion = new RegpmFusion();
		fusion.setEntrepriseAvant(avant);
		fusion.setEntrepriseApres(apres);
		fusion.setDateBilan(dateBilan);
		avant.getFusionsApres().add(fusion);
		apres.getFusionsAvant().add(fusion);
		return fusion;
	}

	static RegpmAdresseEntreprise addAdresse(RegpmEntreprise entreprise, RegpmTypeAdresseEntreprise type,
	                                         RegDate dateDebut, String lieu, RegpmLocalitePostale localitePostale, String nomRue, String noPolice, RegpmRue rue, Integer ofsPays) {
		if (entreprise.getAdressesTypees().containsKey(type)) {
			throw new IllegalArgumentException("Le type d'adresse " + type + " existe déjà sur cette entreprise.");
		}

		final RegpmAdresseEntreprise a = new RegpmAdresseEntreprise();
		a.setId(new RegpmAdresseEntreprise.PK(type, entreprise.getId()));
		assignMutationVisa(a, REGPM_VISA, REGPM_MODIF);
		a.setChez(null);
		a.setDateValidite(dateDebut);
		a.setLieu(lieu);
		a.setLocalitePostale(localitePostale);
		a.setNomRue(nomRue);
		a.setNoPolice(noPolice);
		a.setOfsPays(ofsPays);
		a.setRue(rue);

		entreprise.getAdresses().add(a);
		return a;
	}

	static RegpmBlocNotesEntreprise addNote(RegpmEntreprise entreprise, RegDate dateValidite, String texte) {
		final RegpmBlocNotesEntreprise note = new RegpmBlocNotesEntreprise();
		note.setId(new RegpmBlocNotesEntreprise.PK(computeNewSeqNo(entreprise.getNotes(), x -> x.getId().getSeqNo()), entreprise.getId()));
		assignMutationVisa(note, REGPM_VISA, REGPM_MODIF);
		note.setDateValidite(dateValidite);
		note.setCommentaire(texte);
		entreprise.getNotes().add(note);
		return note;
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
		migrator.initMigrationResult(mr, idMapper);
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
		migrator.initMigrationResult(mr, idMapper);
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
			Assert.assertEquals(DeclarationImpotOrdinairePM.class, declaration.getClass());
			Assert.assertEquals(RegDate.get(pf, 7, 12), declaration.getDateExpedition());
			Assert.assertEquals(RegDate.get(pf - 1, 7, 1), declaration.getDateDebut());
			Assert.assertEquals(RegDate.get(pf, 6, 30), declaration.getDateFin());
			Assert.assertNull(((DeclarationImpotOrdinairePM) declaration).getCodeControle());

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
		migrator.initMigrationResult(mr, idMapper);
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
			Assert.assertEquals(DeclarationImpotOrdinairePM.class, declaration.getClass());
			Assert.assertEquals(RegDate.get(pf, 7, 12), declaration.getDateExpedition());
			Assert.assertEquals(RegDate.get(pf - 1, 7, 1), declaration.getDateDebut());
			Assert.assertEquals(RegDate.get(pf, 6, 30), declaration.getDateFin());
			Assert.assertNull(((DeclarationImpotOrdinairePM) declaration).getCodeControle());

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
		migrator.initMigrationResult(mr, idMapper);
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
			Assert.assertEquals(DeclarationImpotOrdinairePM.class, declaration.getClass());
			Assert.assertEquals(RegDate.get(pf, 7, 12), declaration.getDateExpedition());
			Assert.assertEquals(RegDate.get(pf - 1, 7, 1), declaration.getDateDebut());
			Assert.assertEquals(RegDate.get(pf, 6, 30), declaration.getDateFin());
			Assert.assertNull(((DeclarationImpotOrdinairePM) declaration).getCodeControle());

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
	public void testMigrationDemandesDelai() throws Exception {

		final int pf = 2014;
		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegpmAssujettissement a = addAssujettissement(e, RegDate.get(2000, 1, 1), null, RegpmTypeAssujettissement.LIFD);
		final RegpmDossierFiscal df = addDossierFiscal(e, a, pf, RegDate.get(pf, 7, 12), RegpmModeImposition.POST);
		df.setDelaiRetour(RegDate.get(pf, 7, 22));
		df.setDateEnvoiSommation(RegDate.get(pf, 10, 5));
		addDemandeDelai(df, RegpmTypeDemandeDelai.AVANT_SOMMATION, RegpmTypeEtatDemandeDelai.REFUSEE, RegDate.get(pf, 8, 1), RegDate.get(pf, 8, 10), null);
		addDemandeDelai(df, RegpmTypeDemandeDelai.AVANT_SOMMATION, RegpmTypeEtatDemandeDelai.DEMANDEE, RegDate.get(pf, 8, 11), null, null);
		addDemandeDelai(df, RegpmTypeDemandeDelai.AVANT_SOMMATION, RegpmTypeEtatDemandeDelai.ACCORDEE, RegDate.get(pf, 8, 21), RegDate.get(pf, 9, 1), RegDate.get(pf, 9, 30));
		addDemandeDelai(df, RegpmTypeDemandeDelai.APRES_SOMMATION, RegpmTypeEtatDemandeDelai.REFUSEE, RegDate.get(pf, 10, 21), RegDate.get(pf, 10, 22), null);
		addDemandeDelai(df, RegpmTypeDemandeDelai.APRES_SOMMATION, RegpmTypeEtatDemandeDelai.DEMANDEE, RegDate.get(pf, 10, 25), null, null);
		addDemandeDelai(df, RegpmTypeDemandeDelai.APRES_SOMMATION, RegpmTypeEtatDemandeDelai.ACCORDEE, RegDate.get(pf, 10, 31), RegDate.get(pf, 11, 3), RegDate.get(pf, 12, 25));
		addExerciceCommercial(e, df, RegDate.get(pf - 1, 7, 1), RegDate.get(pf, 6, 30));

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
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			final List<Declaration> declarations = entreprise.getDeclarations().stream()
					.peek(d -> Assert.assertFalse("annulée?", d.isAnnule()))
					.peek(d -> Assert.assertEquals((Integer) 2014, d.getPeriode().getAnnee()))
					.collect(Collectors.toList());
			Assert.assertEquals(1, declarations.size());

			final Declaration declaration = declarations.get(0);
			Assert.assertNotNull(declaration);
			Assert.assertEquals(DeclarationImpotOrdinairePM.class, declaration.getClass());

			final List<DelaiDeclaration> delais = declaration.getDelais().stream()
					.peek(d -> Assert.assertFalse("annulé?", d.isAnnule()))
					.sorted(Comparator.comparing(DelaiDeclaration::getDateDemande))
					.collect(Collectors.toList());
			Assert.assertEquals(5, delais.size());      // les demande non-acceptées après sommation sont ignorées, mais pas les autres (ne pas oublier le délai initial !!)
			{
				final DelaiDeclaration delai = delais.get(0);
				Assert.assertNotNull(delai);
				Assert.assertFalse(delai.isAnnule());
				Assert.assertEquals(RegDate.get(pf, 7, 12), delai.getDateDemande());
				Assert.assertEquals(RegDate.get(pf, 7, 12), delai.getDateTraitement());
				Assert.assertEquals(RegDate.get(pf, 7, 22), delai.getDelaiAccordeAu());
				Assert.assertEquals(EtatDelaiDeclaration.ACCORDE, delai.getEtat());
				Assert.assertFalse(delai.isSursis());
				Assert.assertNull(delai.getCleArchivageCourrier());
			}
			{
				final DelaiDeclaration delai = delais.get(1);
				Assert.assertNotNull(delai);
				Assert.assertFalse(delai.isAnnule());
				Assert.assertEquals(RegDate.get(pf, 8, 1), delai.getDateDemande());
				Assert.assertEquals(RegDate.get(pf, 8, 10), delai.getDateTraitement());
				Assert.assertNull(delai.getDelaiAccordeAu());
				Assert.assertEquals(EtatDelaiDeclaration.REFUSE, delai.getEtat());
				Assert.assertFalse(delai.isSursis());
				Assert.assertNull(delai.getCleArchivageCourrier());
			}
			{
				final DelaiDeclaration delai = delais.get(2);
				Assert.assertNotNull(delai);
				Assert.assertFalse(delai.isAnnule());
				Assert.assertEquals(RegDate.get(pf, 8, 11), delai.getDateDemande());
				Assert.assertEquals(RegDate.get(pf, 8, 11), delai.getDateTraitement());
				Assert.assertNull(delai.getDelaiAccordeAu());
				Assert.assertEquals(EtatDelaiDeclaration.DEMANDE, delai.getEtat());
				Assert.assertFalse(delai.isSursis());
				Assert.assertNull(delai.getCleArchivageCourrier());
			}
			{
				final DelaiDeclaration delai = delais.get(3);
				Assert.assertNotNull(delai);
				Assert.assertFalse(delai.isAnnule());
				Assert.assertEquals(RegDate.get(pf, 8, 21), delai.getDateDemande());
				Assert.assertEquals(RegDate.get(pf, 9, 1), delai.getDateTraitement());
				Assert.assertEquals(RegDate.get(pf, 9, 30), delai.getDelaiAccordeAu());
				Assert.assertEquals(EtatDelaiDeclaration.ACCORDE, delai.getEtat());
				Assert.assertFalse(delai.isSursis());
				Assert.assertNull(delai.getCleArchivageCourrier());
			}
			{
				final DelaiDeclaration delai = delais.get(4);
				Assert.assertNotNull(delai);
				Assert.assertFalse(delai.isAnnule());
				Assert.assertEquals(RegDate.get(pf, 10, 31), delai.getDateDemande());
				Assert.assertEquals(RegDate.get(pf, 11, 3), delai.getDateTraitement());
				Assert.assertEquals(RegDate.get(pf, 12, 25), delai.getDelaiAccordeAu());
				Assert.assertEquals(EtatDelaiDeclaration.ACCORDE, delai.getEtat());
				Assert.assertTrue(delai.isSursis());
				Assert.assertNull(delai.getCleArchivageCourrier());
			}

		});

		// vérification des messages dans le contexte "DECLARATIONS"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.DECLARATIONS);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(10, textes.size());
		Assert.assertEquals("Génération d'une déclaration sur la PF 2014 à partir des dates [01.07.2013 -> 30.06.2014] de l'exercice commercial 1 et du dossier fiscal correspondant.", textes.get(0));
		Assert.assertEquals("Délai initial de retour fixé au 22.07.2014.", textes.get(1));
		Assert.assertEquals("Génération d'un délai refusé (demande du 01.08.2014).", textes.get(2));
		Assert.assertEquals("Génération d'un délai sans décision (demande du 11.08.2014).", textes.get(3));
		Assert.assertEquals("Génération d'un délai accordé au 30.09.2014 (demande du 21.08.2014).", textes.get(4));
		Assert.assertEquals("Demande de délai du 21.10.2014 ignorée car REFUSEE après sommation.", textes.get(5));
		Assert.assertEquals("Demande de délai du 25.10.2014 ignorée car DEMANDEE après sommation.", textes.get(6));
		Assert.assertEquals("Génération d'un sursis au 25.12.2014 (demande du 31.10.2014).", textes.get(7));
		Assert.assertEquals("Etat 'EMISE' migré au 12.07.2014.", textes.get(8));
		Assert.assertEquals("Etat 'SOMMEE' migré au 05.10.2014.", textes.get(9));
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
		migrator.initMigrationResult(mr, idMapper);
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
		Assert.assertEquals("Dossier fiscal sur la PF 2014 alors que la fin de l'exercice commercial ([01.07.2012 -> 30.06.2013]) est en 2013... N'est-ce pas étrange ?", textes.get(1));
		Assert.assertEquals("Délai initial de retour fixé au 22.02.2015.", textes.get(2));
		Assert.assertEquals("Etat 'EMISE' migré au 12.07.2014.", textes.get(3));
	}

	@Test
	public void testCoordonneesFinancieresSansRaisonSociale() throws Exception {

		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		e.setCoordonneesFinancieres(createCoordonneesFinancieres(null, "POFICHBEXXX", null, "17-331-7", "Postfinance", null));

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
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
			Assert.assertNull(entreprise.getTitulaireCompteBancaire());     // pas de raison sociale -> pas de titulaire du compte
			return null;
		});
	}

	@Test
	public void testCoordonneesFinancieresAvecRaisonSociale() throws Exception {

		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addRaisonSociale(e, RegDate.get(2000, 1, 1), "Ma", "petite", "entreprise", true);
		addFormeJuridique(e, RegDate.get(2000, 1, 1), createTypeFormeJuridique("S.A.R.L.", RegpmCategoriePersonneMorale.PM));
		e.setCoordonneesFinancieres(createCoordonneesFinancieres(null, "POFICHBEXXX", null, "17-331-7", "Postfinance", null));

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
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
			Assert.assertEquals("Ma petite entreprise", entreprise.getTitulaireCompteBancaire());     // utilisation de la raison sociale
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
		migrator.initMigrationResult(mr, idMapper);
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
		migrator.initMigrationResult(mr, idMapper);
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
				.filter(s -> s.matches("Le mandat .* est ignoré car sa date d'attribution est dans le futur \\(.*\\)\\."))
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
		migrator.initMigrationResult(mr, idMapper);
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
				.filter(s -> s.matches("Le mandat .* est ignoré car sa date d'attribution est nulle \\(ou antérieure au 01.08.1291\\)\\."))
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
		migrator.initMigrationResult(mr, idMapper);
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
				.filter(s -> s.matches("La date de résiliation du mandat .* est ignorée \\(= mandat ouvert\\) car elle est dans le futur \\(.*\\)\\."))
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
		addRegimeFiscalCH(e, RegDate.get(2005, 1, 1), RegDate.get(2006, 4, 12), RegpmTypeRegimeFiscal._109_PM_AVEC_EXONERATION_ART_90G);        // ignoré car annulé
		addRegimeFiscalCH(e, RegDate.get(2006, 1, 1), null, RegpmTypeRegimeFiscal._109_PM_AVEC_EXONERATION_ART_90G);
		addRegimeFiscalVD(e, RegDate.get(2000, 1, 1), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		addRegimeFiscalVD(e, RegDate.get(2105, 4, 2), null, RegpmTypeRegimeFiscal._31_SOCIETE_ORDINAIRE);       // ignoré car date dans le futur

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
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
			Assert.assertEquals(3, regimesFiscauxBruts.size());     // 1 VD (la date dans le futur n'est pas migrée) + 2 CH (l'annulé n'est pas migré)

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
				Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), rf.getCode());
			}
			{
				final RegimeFiscal rf = regimesFiscauxTries.get(1);
				Assert.assertNotNull(rf);
				Assert.assertEquals(RegimeFiscal.Portee.CH, rf.getPortee());
				Assert.assertEquals(RegDate.get(2000, 1, 3), rf.getDateDebut());
				Assert.assertEquals(RegDate.get(2005, 12, 31), rf.getDateFin());
				Assert.assertNull(rf.getAnnulationDate());
				Assert.assertEquals(MockTypeRegimeFiscal.ORDINAIRE_PM.getCode(), rf.getCode());
			}
			{
				final RegimeFiscal rf = regimesFiscauxTries.get(2);
				Assert.assertNotNull(rf);
				Assert.assertEquals(RegimeFiscal.Portee.CH, rf.getPortee());
				Assert.assertEquals(RegDate.get(2006, 1, 1), rf.getDateDebut());
				Assert.assertNull(rf.getDateFin());
				Assert.assertNull(rf.getAnnulationDate());
				Assert.assertEquals(MockTypeRegimeFiscal.EXO_90G.getCode(), rf.getCode());
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
		migrator.initMigrationResult(mr, idMapper);
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
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {
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
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {
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
		Assert.assertEquals("For fiscal principal 1 COMMUNE_OU_FRACTION_VD/5586 ignoré en raison de la présence à la même date (07.05.2005) d'un for fiscal principal différent de type ADMINISTRATION_EFFECTIVE.", textesFors.get(0));
		Assert.assertEquals("For fiscal principal 3 COMMUNE_HC/2701 ignoré en raison de la présence à la même date (07.05.2005) d'un for fiscal principal différent de type ADMINISTRATION_EFFECTIVE.", textesFors.get(1));
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
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {
			// l'établissement principal
			final List<Etablissement> etbs = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etbs);
			Assert.assertEquals(0, etbs.size());

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
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {
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
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {

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
		Assert.assertEquals("For fiscal principal 4 COMMUNE_HC/2701 ignoré en raison de la présence à la même date (07.05.2005) d'un for fiscal principal différent de type ADMINISTRATION_EFFECTIVE.", textesFors.get(1));
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
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {
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
		Assert.assertEquals("Le for principal 2 est ignoré car il a une date de début nulle (ou antérieure au 01.08.1291).", textesFors.get(1));
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
		migrator.initMigrationResult(mr, idMapper);
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
		Assert.assertEquals("Le for principal 2 est ignoré car il a une date de début nulle (ou antérieure au 01.08.1291).", textesFors.get(1));

		// .. et dans le contexte "SUIVI"
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bPas de siège associé, pas d'établissement principal créé\\.");
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
		migrator.initMigrationResult(mr, idMapper);
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
		Assert.assertEquals(7, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Régime fiscal CH _01_ORDINAIRE ignoré en raison de sa date de début nulle (ou antérieure au 01.08.1291).", textes.get(1));
		Assert.assertEquals("Régime fiscal VD _01_ORDINAIRE ignoré en raison de sa date de début nulle (ou antérieure au 01.08.1291).", textes.get(2));
		Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(3));
		Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(4));
		Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(5));
		Assert.assertEquals("Entreprise migrée : 12.34.", textes.get(6));
	}

	@Test
	public void testRegimesFiscauxDateDeDebutApresRequisitionRadiation() throws Exception {
		final long noEntreprise = 1234L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addRegimeFiscalCH(e, RegDate.get(2010, 7, 23), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		addRegimeFiscalVD(e, RegDate.get(2010, 7, 23), null, RegpmTypeRegimeFiscal._01_ORDINAIRE);
		e.setDateRequisitionRadiation(RegDate.get(2010, 6, 30));

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base (aucun régime fiscal migré car est commence trop tard)
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);
			Assert.assertEquals(Collections.emptySet(), entreprise.getRegimesFiscaux());
		});

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(8, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Date de fin d'activité proposée (date de réquisition de radiation) : 30.06.2010.", textes.get(1));
		Assert.assertEquals("Régime fiscal CH _01_ORDINAIRE ignoré en raison de sa date de début (23.07.2010) postérieure à la date de fin d'activité de l'entreprise (30.06.2010).", textes.get(2));
		Assert.assertEquals("Régime fiscal VD _01_ORDINAIRE ignoré en raison de sa date de début (23.07.2010) postérieure à la date de fin d'activité de l'entreprise (30.06.2010).", textes.get(3));
		Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(4));
		Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(5));
		Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(6));
		Assert.assertEquals("Entreprise migrée : 12.34.", textes.get(7));
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
		migrator.initMigrationResult(mr, idMapper);
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
		Assert.assertEquals(11, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Allègement fiscal généré [01.01.1950 -> 31.03.1955], collectivité CANTON, type BENEFICE : 12%.", textes.get(1));
		Assert.assertEquals("Allègement fiscal généré [01.01.1950 -> 31.03.1955], collectivité CANTON, type CAPITAL : 12%.", textes.get(2));
		Assert.assertEquals("Allègement fiscal généré [01.05.1956 -> 27.12.1956], collectivité CONFEDERATION, type BENEFICE : 13%.", textes.get(3));
		Assert.assertEquals("Allègement fiscal généré [01.05.1956 -> 27.12.1956], collectivité CONFEDERATION, type CAPITAL : 13%.", textes.get(4));
		Assert.assertEquals("Allègement fiscal généré [12.03.1957 -> ?], collectivité COMMUNE, type BENEFICE : 13.5%.", textes.get(5));
		Assert.assertEquals("Allègement fiscal généré [12.03.1957 -> ?], collectivité COMMUNE, type CAPITAL : 13.5%.", textes.get(6));
		Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(7));
		Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(8));
		Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(9));
		Assert.assertEquals("Entreprise migrée : 47.84.", textes.get(10));
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
		migrator.initMigrationResult(mr, idMapper);
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
		Assert.assertEquals(10, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Allègement fiscal généré [01.01.1950 -> 31.03.1955], collectivité COMMUNE, type CAPITAL : 12%.", textes.get(1));
		Assert.assertEquals("Allègement fiscal généré [01.05.1956 -> 27.12.1956], collectivité COMMUNE (5642), type BENEFICE : 13%.", textes.get(2));
		Assert.assertEquals("Allègement fiscal 3 sur une commune hors-canton (Bâle/2701/BS) -> ignoré.", textes.get(3));
		Assert.assertEquals("Allègement fiscal 4 avec un code de contribution IMPOT_BENEFICE_CAPITAL -> ignoré.", textes.get(4));
		Assert.assertEquals("Allègement fiscal généré [12.03.1958 -> ?], collectivité CONFEDERATION, type CAPITAL : 13.4%.", textes.get(5));
		Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(6));
		Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(7));
		Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(8));
		Assert.assertEquals("Entreprise migrée : 47.84.", textes.get(9));
	}

	@Test
	public void testAllegementsFiscauxDatesFutures() throws Exception {

		final long noEntreprise = 42613L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addAllegementFiscal(e, RegDate.get(1950, 1, 1), RegDate.get(2020, 3, 31), BigDecimal.valueOf(12L), RegpmCodeContribution.CAPITAL, RegpmCodeCollectivite.COMMUNE, null);
		addAllegementFiscal(e, RegDate.get(2020, 4, 1), null, BigDecimal.valueOf(10L), RegpmCodeContribution.CAPITAL, RegpmCodeCollectivite.COMMUNE, null);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
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
			Assert.assertEquals(1, allegementsTries.size());
			{
				final AllegementFiscal a = allegementsTries.get(0);
				Assert.assertNotNull(a);
				Assert.assertNull(a.getAnnulationDate());
				Assert.assertEquals(RegDate.get(1950, 1, 1), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertNull(a.getNoOfsCommune());
				Assert.assertEquals("Expected: 12, actual: " + a.getPourcentageAllegement(), 0, BigDecimal.valueOf(12L).compareTo(a.getPourcentageAllegement()));
				Assert.assertEquals(AllegementFiscal.TypeCollectivite.COMMUNE, a.getTypeCollectivite());
				Assert.assertEquals(AllegementFiscal.TypeImpot.CAPITAL, a.getTypeImpot());
			}
		});

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(8, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Date de fin (31.03.2020) de l'allègement fiscal 1 ignorée (date future).", textes.get(1));
		Assert.assertEquals("Allègement fiscal généré [01.01.1950 -> ?], collectivité COMMUNE, type CAPITAL : 12%.", textes.get(2));
		Assert.assertEquals("Allègement fiscal 2 ignoré en raison de sa date de début dans le futur (01.04.2020).", textes.get(3));
		Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(4));
		Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(5));
		Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(6));
		Assert.assertEquals("Entreprise migrée : 426.13.", textes.get(7));
	}

	/**
	 * Cas de l'entreprise 95.39 qui a un allègement fiscal ouvert sur une commune qui a disparu en 2011
	 */
	@Test
	public void testAllegementsFiscauxEtFusionsDeCommunes() throws Exception {

		final long noEntreprise = 9539L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addAllegementFiscal(e, RegDate.get(2001, 1, 1), null, BigDecimal.valueOf(100L), RegpmCodeContribution.BENEFICE, RegpmCodeCollectivite.COMMUNE, Commune.GRANGES_PRES_MARNAND);
		addAllegementFiscal(e, RegDate.get(2001, 1, 1), null, BigDecimal.valueOf(100L), RegpmCodeContribution.CAPITAL, RegpmCodeCollectivite.COMMUNE, Commune.GRANGES_PRES_MARNAND);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
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
			Assert.assertEquals(4, allegementsTries.size());
			{
				final AllegementFiscal a = allegementsTries.get(0);
				Assert.assertNotNull(a);
				Assert.assertNull(a.getAnnulationDate());
				Assert.assertEquals(RegDate.get(2001, 1, 1), a.getDateDebut());
				Assert.assertEquals(RegDate.get(2011, 12, 31), a.getDateFin());
				Assert.assertEquals(Commune.GRANGES_PRES_MARNAND.getNoOfs(), a.getNoOfsCommune());
				Assert.assertEquals("Expected: 100, actual: " + a.getPourcentageAllegement(), 0, BigDecimal.valueOf(100L).compareTo(a.getPourcentageAllegement()));
				Assert.assertEquals(AllegementFiscal.TypeCollectivite.COMMUNE, a.getTypeCollectivite());
				Assert.assertEquals(AllegementFiscal.TypeImpot.BENEFICE, a.getTypeImpot());
			}
			{
				final AllegementFiscal a = allegementsTries.get(1);
				Assert.assertNotNull(a);
				Assert.assertNull(a.getAnnulationDate());
				Assert.assertEquals(RegDate.get(2001, 1, 1), a.getDateDebut());
				Assert.assertEquals(RegDate.get(2011, 12, 31), a.getDateFin());
				Assert.assertEquals(Commune.GRANGES_PRES_MARNAND.getNoOfs(), a.getNoOfsCommune());
				Assert.assertEquals("Expected: 100, actual: " + a.getPourcentageAllegement(), 0, BigDecimal.valueOf(100L).compareTo(a.getPourcentageAllegement()));
				Assert.assertEquals(AllegementFiscal.TypeCollectivite.COMMUNE, a.getTypeCollectivite());
				Assert.assertEquals(AllegementFiscal.TypeImpot.CAPITAL, a.getTypeImpot());
			}
			{
				final AllegementFiscal a = allegementsTries.get(2);
				Assert.assertNotNull(a);
				Assert.assertNull(a.getAnnulationDate());
				Assert.assertEquals(RegDate.get(2012, 1, 1), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertEquals(Commune.VALBROYE.getNoOfs(), a.getNoOfsCommune());
				Assert.assertEquals("Expected: 100, actual: " + a.getPourcentageAllegement(), 0, BigDecimal.valueOf(100L).compareTo(a.getPourcentageAllegement()));
				Assert.assertEquals(AllegementFiscal.TypeCollectivite.COMMUNE, a.getTypeCollectivite());
				Assert.assertEquals(AllegementFiscal.TypeImpot.BENEFICE, a.getTypeImpot());
			}
			{
				final AllegementFiscal a = allegementsTries.get(3);
				Assert.assertNotNull(a);
				Assert.assertNull(a.getAnnulationDate());
				Assert.assertEquals(RegDate.get(2012, 1, 1), a.getDateDebut());
				Assert.assertNull(a.getDateFin());
				Assert.assertEquals(Commune.VALBROYE.getNoOfs(), a.getNoOfsCommune());
				Assert.assertEquals("Expected: 100, actual: " + a.getPourcentageAllegement(), 0, BigDecimal.valueOf(100L).compareTo(a.getPourcentageAllegement()));
				Assert.assertEquals(AllegementFiscal.TypeCollectivite.COMMUNE, a.getTypeCollectivite());
				Assert.assertEquals(AllegementFiscal.TypeImpot.CAPITAL, a.getTypeImpot());
			}
		});

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(13, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Entité AllegementFiscal [01.01.2001 -> ?] sur COMMUNE_OU_FRACTION_VD/5818 au moins partiellement remplacée par AllegementFiscal [01.01.2001 -> 31.12.2011] sur COMMUNE_OU_FRACTION_VD/5818 pour suivre les fusions de communes.", textes.get(1));
		Assert.assertEquals("Entité AllegementFiscal [01.01.2001 -> ?] sur COMMUNE_OU_FRACTION_VD/5818 au moins partiellement remplacée par AllegementFiscal [01.01.2012 -> ?] sur COMMUNE_OU_FRACTION_VD/5831 pour suivre les fusions de communes.", textes.get(2));
		Assert.assertEquals("Allègement fiscal généré [01.01.2001 -> 31.12.2011], collectivité COMMUNE (5818), type BENEFICE : 100%.", textes.get(3));
		Assert.assertEquals("Allègement fiscal généré [01.01.2012 -> ?], collectivité COMMUNE (5831), type BENEFICE : 100%.", textes.get(4));
		Assert.assertEquals("Entité AllegementFiscal [01.01.2001 -> ?] sur COMMUNE_OU_FRACTION_VD/5818 au moins partiellement remplacée par AllegementFiscal [01.01.2001 -> 31.12.2011] sur COMMUNE_OU_FRACTION_VD/5818 pour suivre les fusions de communes.", textes.get(5));
		Assert.assertEquals("Entité AllegementFiscal [01.01.2001 -> ?] sur COMMUNE_OU_FRACTION_VD/5818 au moins partiellement remplacée par AllegementFiscal [01.01.2012 -> ?] sur COMMUNE_OU_FRACTION_VD/5831 pour suivre les fusions de communes.", textes.get(6));
		Assert.assertEquals("Allègement fiscal généré [01.01.2001 -> 31.12.2011], collectivité COMMUNE (5818), type CAPITAL : 100%.", textes.get(7));
		Assert.assertEquals("Allègement fiscal généré [01.01.2012 -> ?], collectivité COMMUNE (5831), type CAPITAL : 100%.", textes.get(8));
		Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(9));
		Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(10));
		Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(11));
		Assert.assertEquals("Entreprise migrée : 95.39.", textes.get(12));
	}

	@Test
	public void testExercicesCommerciaux() throws Exception {

		final long noEntreprise = 4784L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegpmAssujettissement lilic = addAssujettissement(e, RegDate.get(2000, 1, 1), null, RegpmTypeAssujettissement.LILIC);
		final RegpmAssujettissement lifd = addAssujettissement(e, RegDate.get(2000, 1, 1), null, RegpmTypeAssujettissement.LIFD);
		addForPrincipalSuisse(e, RegDate.get(2000, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		addSiegeSuisse(e, RegDate.get(2000, 1, 1), Commune.LAUSANNE);

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
		migrator.initMigrationResult(mr, idMapper);
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
			noEtablissementPrincipal.setValue(etablissementPrincipal.getNumero());
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(6, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2015 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.2014 -> 31.03.2016].", textes.get(1));
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
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

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
			Assert.assertEquals(RegDate.get(1997, 3, 1), bouclement.getDateDebut());
			Assert.assertEquals(12, bouclement.getPeriodeMois());

			// pas d'établissement principal généré (pas de siège!!)
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(0, etablissements.size());
		});

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(14, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.1997 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.1997 -> 31.03.1997].", textes.get(1));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2007 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(2));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2006 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(3));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2005 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(4));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2004 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(5));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2003 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(6));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2002 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(7));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2001 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(8));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2000 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(9));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.1999 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2007].", textes.get(10));
		Assert.assertEquals("Cycle de bouclements créé, applicable dès le 01.03.1997 : tous les 12 mois, à partir du premier 31.03.", textes.get(11));
		Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(12));
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
		addSiegeSuisse(e, RegDate.get(1997, 1, 1), Commune.BALE);
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
		migrator.initMigrationResult(mr, idMapper);
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
				Assert.assertEquals(RegDate.get(1997, 3, 1), bouclement.getDateDebut());
				Assert.assertEquals(12, bouclement.getPeriodeMois());
			}
			{
				final Bouclement bouclement = bouclementsTries.get(1);
				Assert.assertNotNull(bouclement);
				Assert.assertFalse(bouclement.isAnnule());
				Assert.assertEquals(DayMonth.get(12, 31), bouclement.getAncrage());
				Assert.assertEquals(RegDate.get(1998, 12, 1), bouclement.getDateDebut());
				Assert.assertEquals(12, bouclement.getPeriodeMois());
			}

			// récupération du numéro fiscal de l'établissement principal généré
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissementPrincipal = etablissements.get(0);
			Assert.assertNotNull(etablissementPrincipal);
			noEtablissementPrincipal.setValue(etablissementPrincipal.getNumero());
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(17, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.1997 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.1997 -> 31.03.1997].", textes.get(1));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2007 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(2));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2006 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(3));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2005 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(4));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2004 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(5));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2003 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(6));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2002 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(7));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2001 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(8));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2000 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(9));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.1999 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(10));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.1998 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.12.2007].", textes.get(11));
		Assert.assertEquals("Cycle de bouclements créé, applicable dès le 01.03.1997 : tous les 12 mois, à partir du premier 31.03.", textes.get(12));
		Assert.assertEquals("Cycle de bouclements créé, applicable dès le 01.12.1998 : tous les 12 mois, à partir du premier 31.12.", textes.get(13));
		Assert.assertEquals(String.format("Création de l'établissement principal %s.", FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue())), textes.get(14));
		Assert.assertEquals(String.format("Domicile de l'établissement principal %s : [01.01.1997 -> ?] sur COMMUNE_HC/2701.", FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue())), textes.get(15));
		Assert.assertEquals("Entreprise migrée : 246.71.", textes.get(16));
	}

	/**
	 * Dans RegPM, les exercices commerciaux n'étaient mappés que si une DI était envoyée (et retournée), donc en particulier
	 * il pouvait ne pas y en avoir pendant plusieurs années si l'assujettissement était interrompu.
	 * Ici, on est dans le cas où l'assujettissement recommence juste maintenant et que la première DI après redémarrage de
	 * l'assujettissement n'est pas encore revenue (= l'exercice commcercial de RegPM n'a pas encore été créé)
	 */
	@Test
	public void testExercicesCommerciauxAbsentsPendantPlusieursAnneeAvantDateBouclementFutur() throws Exception {

		final long noEntreprise = 24671L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegpmAssujettissement assujettissement1998 = addAssujettissement(e, RegDate.get(1997, 4, 1), RegDate.get(1998, 3, 31), RegpmTypeAssujettissement.LILIC);
		addForPrincipalSuisse(e, RegDate.get(1997, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.BALE);
		final RegpmDossierFiscal df1998 = addDossierFiscal(e, assujettissement1998, 1998, RegDate.get(1998, 4, 21), RegpmModeImposition.POST);
		addExerciceCommercial(e, df1998, RegDate.get(1997, 4, 1), RegDate.get(1998, 3, 31));

		addAssujettissement(e, RegDate.get(2007, 5, 21), null, RegpmTypeAssujettissement.LILIC);
		e.setDateBouclementFutur(RegDate.get(2008, 3, 31));

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
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

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
			Assert.assertEquals(RegDate.get(1997, 3, 1), bouclement.getDateDebut());
			Assert.assertEquals(12, bouclement.getPeriodeMois());

			// pas d'établissement principal généré (pas de siège!!)
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(0, etablissements.size());
		});

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(14, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.1997 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.1997 -> 31.03.1997].", textes.get(1));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2007 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2008].", textes.get(2));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2006 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2008].", textes.get(3));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2005 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2008].", textes.get(4));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2004 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2008].", textes.get(5));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2003 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2008].", textes.get(6));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2002 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2008].", textes.get(7));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2001 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2008].", textes.get(8));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.2000 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2008].", textes.get(9));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.03.1999 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.04.1998 -> 31.03.2008].", textes.get(10));
		Assert.assertEquals("Cycle de bouclements créé, applicable dès le 01.03.1997 : tous les 12 mois, à partir du premier 31.03.", textes.get(11));
		Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(12));
		Assert.assertEquals("Entreprise migrée : 246.71.", textes.get(13));
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
		addForPrincipalSuisse(e, RegDate.get(1990, 1, 1), RegpmTypeForPrincipal.SIEGE, Commune.MORGES);
		final RegpmAssujettissement a = addAssujettissement(e, RegDate.get(1990, 1, 1), null, RegpmTypeAssujettissement.LILIC);
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
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

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
			Assert.assertEquals(RegDate.get(1990, 12, 1), bouclement.getDateDebut());
			Assert.assertEquals(12, bouclement.getPeriodeMois());

			// pas d'établissement principal généré (pas de siège)
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(0, etablissements.size());
		});

		// vérification des messages dans le contexte "SUIVI"
		final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
		Assert.assertNotNull(messages);
		final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(16, textes.size());
		Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
		Assert.assertEquals("Date de bouclement futur (31.12.1992) ignorée car antérieure à la date de fin du dernier exercice commercial connu (31.12.2001).", textes.get(1));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.2000 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.1990 -> 31.12.2000].", textes.get(2));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.1999 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.1990 -> 31.12.2000].", textes.get(3));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.1998 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.1990 -> 31.12.2000].", textes.get(4));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.1997 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.1990 -> 31.12.2000].", textes.get(5));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.1996 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.1990 -> 31.12.2000].", textes.get(6));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.1995 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.1990 -> 31.12.2000].", textes.get(7));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.1994 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.1990 -> 31.12.2000].", textes.get(8));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.1993 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.1990 -> 31.12.2000].", textes.get(9));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.1992 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.1990 -> 31.12.2000].", textes.get(10));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.1991 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.1990 -> 31.12.2000].", textes.get(11));
		Assert.assertEquals("Ajout d'une date de bouclement estimée au 31.12.1990 pour combler l'absence d'exercice commercial dans RegPM sur la période [01.01.1990 -> 31.12.2000].", textes.get(12));
		Assert.assertEquals("Cycle de bouclements créé, applicable dès le 01.12.1990 : tous les 12 mois, à partir du premier 31.12.", textes.get(13));
		Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(14));
		Assert.assertEquals("Entreprise migrée : 324.14.", textes.get(15));
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
		migrator.initMigrationResult(mr, idMapper);
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
		migrator.initMigrationResult(mr, idMapper);
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
		Assert.assertEquals("Entité ForFiscalPrincipalPM [07.04.1977 -> ?] sur COMMUNE_HC/261 au moins partiellement remplacée par ForFiscalPrincipalPM [07.04.1977 -> 31.12.1989] sur COMMUNE_HC/253 pour suivre les fusions de communes.",
		                    textes.get(0));
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
		migrator.initMigrationResult(mr, idMapper);
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
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

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
			Assert.assertEquals(0, etablissements.size());
		});

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(3, textes.size());
			Assert.assertEquals("Le pays 8213 (for principal 1) n'est pas un état souverain, remplacé par l'état 8215.", textes.get(0));
			Assert.assertEquals("For principal PAYS_HS/8215 [07.04.1977 -> ?] généré.", textes.get(1));
			Assert.assertEquals("Décision ACI PAYS_HS/8215 [07.04.1977 -> ?] générée.", textes.get(2));
		}

		// vérification des messages dans le contexte "SUIVI"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(4, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(1));
			Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(2));
			Assert.assertEquals("Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", textes.get(3));
		}
	}

	@Test
	public void testSiegeSurTerritoire() throws Exception {

		final long noEntreprise = 53465L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addSiegeEtranger(e, RegDate.get(1977, 4, 7), MockPays.Gibraltar.getNoOFS());
		addRaisonSociale(e, RegDate.get(1977, 4, 7), "Mon entreprise à moi", null, null, true);
		addFormeJuridique(e, RegDate.get(1977, 4, 7), createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
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
			{
				final ForFiscalPrincipalPM ff = ffps.get(0);
				Assert.assertNotNull(ff);
				Assert.assertFalse(ff.isAnnule());
				Assert.assertEquals(RegDate.get(1977, 4, 7), ff.getDateDebut());
				Assert.assertNull(ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ff.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
			}

			final List<DecisionAci> decisions = entreprise.getDecisionsSorted();
			Assert.assertNotNull(decisions);
			Assert.assertEquals(0, decisions.size());

			// établissement principal et son domicile
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissement = etablissements.get(0);
			Assert.assertNotNull(etablissement);
			Assert.assertFalse(etablissement.isAnnule());
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

		// vérification des messages dans le contexte "SUIVI"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(7, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(1));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(2));
			Assert.assertEquals("Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + ".", textes.get(3));
			Assert.assertEquals("Le pays 8213 (siège 1) n'est pas un état souverain, remplacé par l'état 8215.", textes.get(4));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [07.04.1977 -> ?] sur PAYS_HS/8215.", textes.get(5));
			Assert.assertEquals("Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", textes.get(6));
		}
	}

	@Test
	public void testForPrincipalEtSiegeSurTerritoireExGibraltar() throws Exception {

		final long noEntreprise = 53465L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalEtranger(e, RegDate.get(1977, 4, 7), RegpmTypeForPrincipal.ADMINISTRATION_EFFECTIVE, 8997);      // et oui, dans le mainframe, c'est 'Ex-Gibraltar', qui n'a pas été repris dans FiDoR
		addSiegeEtranger(e, RegDate.get(1977, 4, 7), 8997);      // et oui, dans le mainframe, c'est 'Ex-Gibraltar', qui n'a pas été repris dans FiDoR

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
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
			Assert.assertEquals("Le pays 8997 (for principal 1) n'est pas un état souverain, remplacé par l'état 8215.", textes.get(0));
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
			Assert.assertEquals("Le pays 8997 (siège 1) n'est pas un état souverain, remplacé par l'état 8215.", textes.get(3));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [07.04.1977 -> ?] sur PAYS_HS/8215.", textes.get(4));
			Assert.assertEquals("Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", textes.get(5));
		}
	}

	@Test
	public void testForsSansSiege() throws Exception {
		final long noEntreprise = 1234L;
		final RegDate debut = RegDate.get(2005, 5, 7);
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, debut, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
//		addSiegeSuisse(e, debut, Commune.LAUSANNE);       // pas de siège explicite

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base -> pas d'établissement principal, mais quand-même une entreprise avec for principal
		doInUniregTransaction(true, status -> {
			final List<Long> ids = getTiersDAO().getAllIdsFor(true, TypeTiers.ETABLISSEMENT);
			Assert.assertNotNull(ids);
			Assert.assertEquals(0, ids.size());

			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);
			Assert.assertEquals(1, entreprise.getForsFiscaux().size());

			final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(debut, ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
			Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getMotifFermeture());
		});

		// vérification des messages dans le contexte "FORS"
		final List<MigrationResultCollector.Message> messagesFors = mr.getMessages().get(LogCategory.FORS);
		Assert.assertNotNull(messagesFors);
		final List<String> textesFors = messagesFors.stream().map(msg -> msg.text).collect(Collectors.toList());
		Assert.assertEquals(1, textesFors.size());
		Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5586 [07.05.2005 -> ?] généré.", textesFors.get(0));

		// .. et dans le contexte "SUIVI"
		assertExistMessageWithContent(mr, LogCategory.SUIVI, "\\bPas de siège associé, pas d'établissement principal créé\\.");
	}

	@Test
	public void testMultiplesSieges() throws Exception {

		final long noEntreprise = 43674L;
		final RegDate dateDebut = RegDate.get(2000, 1, 1);
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addRaisonSociale(e, dateDebut, "Mon entreprise qui bouge", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addSiegeSuisse(e, dateDebut, Commune.MORGES);
		addSiegeSuisse(e, RegDate.get(2004, 2, 23), Commune.LAUSANNE);
		addSiegeSuisse(e, RegDate.get(2001, 5, 2), Commune.ECHALLENS);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// récupération du numéro de l'établissement principal
		final MutableLong noEtablissementPrincipal = new MutableLong();

		// vérification du contenu de la base de données
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			// vérification des fors fiscaux principaux générés : aucuns
			final List<ForFiscalPrincipalPM> ffps = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(ffps);
			Assert.assertEquals(3, ffps.size());
			{
				final ForFiscalPrincipalPM ff = ffps.get(0);
				Assert.assertNotNull(ff);
				Assert.assertFalse(ff.isAnnule());
				Assert.assertEquals(dateDebut, ff.getDateDebut());
				Assert.assertEquals(RegDate.get(2001, 5, 1), ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
			}
			{
				final ForFiscalPrincipalPM ff = ffps.get(1);
				Assert.assertNotNull(ff);
				Assert.assertFalse(ff.isAnnule());
				Assert.assertEquals(RegDate.get(2001, 5, 2), ff.getDateDebut());
				Assert.assertEquals(RegDate.get(2004, 2, 22), ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
			}
			{
				final ForFiscalPrincipalPM ff = ffps.get(2);
				Assert.assertNotNull(ff);
				Assert.assertFalse(ff.isAnnule());
				Assert.assertEquals(RegDate.get(2004, 2, 23), ff.getDateDebut());
				Assert.assertNull(ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
			}

			// récupération du numéro fiscal de l'établissement principal généré
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissementPrincipal = etablissements.get(0);
			Assert.assertNotNull(etablissementPrincipal);
			noEtablissementPrincipal.setValue(etablissementPrincipal.getNumero());

			// vérification du domicile généré
			final Set<DomicileEtablissement> domiciles = etablissementPrincipal.getDomiciles();
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(3, domiciles.size());

			final List<DomicileEtablissement> domicilesTries = new ArrayList<>(domiciles);
			Collections.sort(domicilesTries, new DateRangeComparator<>());

			{
				final DomicileEtablissement domicile = domicilesTries.get(0);
				Assert.assertNotNull(domicile);
				Assert.assertEquals(dateDebut, domicile.getDateDebut());
				Assert.assertEquals(RegDate.get(2001, 5, 1), domicile.getDateFin());
				Assert.assertEquals(Commune.MORGES.getNoOfs(), domicile.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertFalse(domicile.isAnnule());
			}
			{
				final DomicileEtablissement domicile = domicilesTries.get(1);
				Assert.assertNotNull(domicile);
				Assert.assertEquals(RegDate.get(2001, 5, 2), domicile.getDateDebut());
				Assert.assertEquals(RegDate.get(2004, 2, 22), domicile.getDateFin());
				Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), domicile.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertFalse(domicile.isAnnule());
			}
			{
				final DomicileEtablissement domicile = domicilesTries.get(2);
				Assert.assertNotNull(domicile);
				Assert.assertEquals(RegDate.get(2004, 2, 23), domicile.getDateDebut());
				Assert.assertNull(domicile.getDateFin());
				Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), domicile.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertFalse(domicile.isAnnule());
			}
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());

		// analyse des logs

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(9, textes.size());
			Assert.assertEquals("Données du siège 1 utilisées pour les fors principaux : COMMUNE_OU_FRACTION_VD/5642 depuis le 01.01.2000.", textes.get(0));
			Assert.assertEquals("Utilisation d'un siège vaudois (1 sur commune 5642 dès le 01.01.2000) dans la génération des fors principaux... Pourquoi n'y avait-il pas de for principal vaudois dans RegPM ?", textes.get(1));
            Assert.assertEquals("Données du siège 3 utilisées pour les fors principaux : COMMUNE_OU_FRACTION_VD/5518 depuis le 02.05.2001.", textes.get(2));
			Assert.assertEquals("Utilisation d'un siège vaudois (3 sur commune 5518 dès le 02.05.2001) dans la génération des fors principaux... Pourquoi n'y avait-il pas de for principal vaudois dans RegPM ?", textes.get(3));
			Assert.assertEquals("Données du siège 2 utilisées pour les fors principaux : COMMUNE_OU_FRACTION_VD/5586 depuis le 23.02.2004.", textes.get(4));
			Assert.assertEquals("Utilisation d'un siège vaudois (2 sur commune 5586 dès le 23.02.2004) dans la génération des fors principaux... Pourquoi n'y avait-il pas de for principal vaudois dans RegPM ?", textes.get(5));
			Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5642 [01.01.2000 -> 01.05.2001] généré.", textes.get(6));
			Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5518 [02.05.2001 -> 22.02.2004] généré.", textes.get(7));
			Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5586 [23.02.2004 -> ?] généré.", textes.get(8));
		}

		// vérification des messages dans le contexte "SUIVI"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(8, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(1));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(2));
			Assert.assertEquals("Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + ".", textes.get(3));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [01.01.2000 -> 01.05.2001] sur COMMUNE_OU_FRACTION_VD/5642.", textes.get(4));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [02.05.2001 -> 22.02.2004] sur COMMUNE_OU_FRACTION_VD/5518.", textes.get(5));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [23.02.2004 -> ?] sur COMMUNE_OU_FRACTION_VD/5586.", textes.get(6));
			Assert.assertEquals("Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", textes.get(7));
		}
	}

	@Test
	public void testDateValiditeFutureSiege() throws Exception {

		final long noEntreprise = 43674L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addRaisonSociale(e, RegDate.get(2003, 1, 1), "Mon entreprise qui bouge", null, null, true);
		addFormeJuridique(e, RegDate.get(2003, 1, 1), createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addSiegeSuisse(e, RegDate.get(2003, 1, 1), Commune.MORGES);
		addSiegeSuisse(e, RegDate.get(2104, 2, 23), Commune.LAUSANNE);      // de par la date, celui-ci devrait être conservé, mais il est dans le futur
		addSiegeSuisse(e, RegDate.get(2001, 5, 2), Commune.ECHALLENS);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// récupération du numéro de l'établissement principal
		final MutableLong noEtablissementPrincipal = new MutableLong();

		// vérification du contenu de la base de données
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			// vérification des fors fiscaux principaux générés : aucuns
			final List<ForFiscalPrincipalPM> ffps = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(ffps);
			Assert.assertEquals(2, ffps.size());
			{
				final ForFiscalPrincipalPM ff = ffps.get(0);
				Assert.assertNotNull(ff);
				Assert.assertFalse(ff.isAnnule());
				Assert.assertEquals(RegDate.get(2001, 5, 2), ff.getDateDebut());
				Assert.assertEquals(RegDate.get(2002, 12, 31), ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
			}
			{
				final ForFiscalPrincipalPM ff = ffps.get(1);
				Assert.assertNotNull(ff);
				Assert.assertFalse(ff.isAnnule());
				Assert.assertEquals(RegDate.get(2003, 1, 1), ff.getDateDebut());
				Assert.assertNull(ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
			}

			// récupération du numéro fiscal de l'établissement principal généré
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissementPrincipal = etablissements.get(0);
			Assert.assertNotNull(etablissementPrincipal);
			noEtablissementPrincipal.setValue(etablissementPrincipal.getNumero());

			// vérification du domicile généré
			final Set<DomicileEtablissement> domiciles = etablissementPrincipal.getDomiciles();
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(2, domiciles.size());

			final List<DomicileEtablissement> domicilesTries = new ArrayList<>(domiciles);
			Collections.sort(domicilesTries, new DateRangeComparator<>());

			{
				final DomicileEtablissement domicile = domicilesTries.get(0);
				Assert.assertNotNull(domicile);
				Assert.assertEquals(RegDate.get(2001, 5, 2), domicile.getDateDebut());
				Assert.assertEquals(RegDate.get(2002, 12, 31), domicile.getDateFin());
				Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), domicile.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertFalse(domicile.isAnnule());
			}
			{
				final DomicileEtablissement domicile = domicilesTries.get(1);
				Assert.assertNotNull(domicile);
				Assert.assertEquals(RegDate.get(2003, 1, 1), domicile.getDateDebut());
				Assert.assertNull(domicile.getDateFin());
				Assert.assertEquals(Commune.MORGES.getNoOfs(), domicile.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertFalse(domicile.isAnnule());
			}
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());

		// analyse des logs

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(6, textes.size());
			Assert.assertEquals("Données du siège 3 utilisées pour les fors principaux : COMMUNE_OU_FRACTION_VD/5518 depuis le 02.05.2001.", textes.get(0));
			Assert.assertEquals("Utilisation d'un siège vaudois (3 sur commune 5518 dès le 02.05.2001) dans la génération des fors principaux... Pourquoi n'y avait-il pas de for principal vaudois dans RegPM ?", textes.get(1));
			Assert.assertEquals("Données du siège 1 utilisées pour les fors principaux : COMMUNE_OU_FRACTION_VD/5642 depuis le 01.01.2003.", textes.get(2));
			Assert.assertEquals("Utilisation d'un siège vaudois (1 sur commune 5642 dès le 01.01.2003) dans la génération des fors principaux... Pourquoi n'y avait-il pas de for principal vaudois dans RegPM ?", textes.get(3));
			Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5518 [02.05.2001 -> 31.12.2002] généré.", textes.get(4));
			Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5642 [01.01.2003 -> ?] généré.", textes.get(5));
		}

		// vérification des messages dans le contexte "SUIVI"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(8, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(1));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(2));
			Assert.assertEquals("Le siège 2 est ignoré car il a une date de début de validité dans le futur (23.02.2104).", textes.get(3));
			Assert.assertEquals("Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + ".", textes.get(4));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [02.05.2001 -> 31.12.2002] sur COMMUNE_OU_FRACTION_VD/5518.", textes.get(5));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [01.01.2003 -> ?] sur COMMUNE_OU_FRACTION_VD/5642.", textes.get(6));
			Assert.assertEquals("Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", textes.get(7));
		}
	}

	@Test
	public void testDateValiditeNulleSiege() throws Exception {

		final long noEntreprise = 43674L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addSiegeSuisse(e, null, Commune.MORGES);        // c'est le seul qu'on ait, mais sa date de validité est nulle...

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// vérification du contenu de la base de données
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			// vérification des fors fiscaux principaux générés : aucuns
			final List<ForFiscalPrincipalPM> ffps = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(ffps);
			Assert.assertEquals(0, ffps.size());

			// récupération du numéro fiscal de l'établissement principal généré
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(0, etablissements.size());
		});

		// analyse des logs

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNull(messages);
		}

		// vérification des messages dans le contexte "SUIVI"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(6, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(1));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(2));
			Assert.assertEquals("Le siège 1 est ignoré car il a une date de début de validité nulle (ou antérieure au 01.08.1291).", textes.get(3));
			Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(4));
			Assert.assertEquals("Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", textes.get(5));
		}
	}

	@Test
	public void testMultiplesSiegesMemeDate() throws Exception {

		final long noEntreprise = 43674L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addSiegeSuisse(e, RegDate.get(2000, 1, 1), Commune.MORGES);
		addSiegeSuisse(e, RegDate.get(2000, 1, 1), Commune.LAUSANNE);
		addSiegeSuisse(e, RegDate.get(2000, 1, 1), Commune.ECHALLENS);  // de par le numéro de séquence, c'est lui qui doit être pris

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// récupération du numéro de l'établissement principal
		final MutableLong noEtablissementPrincipal = new MutableLong();

		// vérification du contenu de la base de données
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = (Entreprise) getTiersDAO().get(noEntreprise);
			Assert.assertNotNull(entreprise);

			// vérification des fors fiscaux principaux générés : aucuns
			final List<ForFiscalPrincipalPM> ffps = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(ffps);
			Assert.assertEquals(1, ffps.size());
			{
				final ForFiscalPrincipalPM ff = ffps.get(0);
				Assert.assertNotNull(ff);
				Assert.assertFalse(ff.isAnnule());
				Assert.assertEquals(RegDate.get(2000, 1, 1), ff.getDateDebut());
				Assert.assertNull(ff.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
			}

			// récupération du numéro fiscal de l'établissement principal généré
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissementPrincipal = etablissements.get(0);
			Assert.assertNotNull(etablissementPrincipal);
			noEtablissementPrincipal.setValue(etablissementPrincipal.getNumero());

			// vérification du domicile généré
			final Set<DomicileEtablissement> domiciles = etablissementPrincipal.getDomiciles();
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(1, domiciles.size());       // on ne garde que le dernier

			final DomicileEtablissement domicile = domiciles.iterator().next();
			Assert.assertNotNull(domicile);
			Assert.assertEquals(RegDate.get(2000, 1, 1), domicile.getDateDebut());
			Assert.assertNull(domicile.getDateFin());
			Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), domicile.getNumeroOfsAutoriteFiscale());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
			Assert.assertFalse(domicile.isAnnule());
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());

		// analyse des logs

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(3, textes.size());
			Assert.assertEquals("Données du siège 3 utilisées pour les fors principaux : COMMUNE_OU_FRACTION_VD/5518 depuis le 01.01.2000.", textes.get(0));
			Assert.assertEquals("Utilisation d'un siège vaudois (3 sur commune 5518 dès le 01.01.2000) dans la génération des fors principaux... Pourquoi n'y avait-il pas de for principal vaudois dans RegPM ?", textes.get(1));
			Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5518 [01.01.2000 -> ?] généré.", textes.get(2));
		}

		// vérification des messages dans le contexte "SUIVI"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(8, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(1));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(2));
			Assert.assertEquals("Le siège 1 est ignoré car il est suivi d'un autre à la même date.", textes.get(3));
			Assert.assertEquals("Le siège 2 est ignoré car il est suivi d'un autre à la même date.", textes.get(4));
			Assert.assertEquals("Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + ".", textes.get(5));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [01.01.2000 -> ?] sur COMMUNE_OU_FRACTION_VD/5518.", textes.get(6));
			Assert.assertEquals("Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", textes.get(7));
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
		addSiegeSuisse(e, RegDate.get(2001, 7, 1), Commune.ZURICH);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
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
			Assert.assertEquals(5, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(1));
			Assert.assertEquals("Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + ".", textes.get(2));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [01.07.2001 -> ?] sur COMMUNE_HC/261.", textes.get(3));
			Assert.assertEquals("Entreprise migrée : " + FormatNumeroHelper.numeroCTBToDisplay(noEntreprise) + ".", textes.get(4));
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
		migrator.initMigrationResult(mr, idMapper);
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

	@Test
	public void testDetectionPositiveDoublon() throws Exception {

		final long noEntreprise = 2623L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addRaisonSociale(e, null, "*Chez-moi SA", null, null, true);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// en base : le flag débiteur inactif doit avoir été mis
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);
			Assert.assertTrue(entreprise.isAnnule());               // une étoile au début de la raison sociale -> annulation du tiers
		});

		// et dans les messages de suivi ?
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(6, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise identifiée comme un doublon.", textes.get(1));
			Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(2));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(3));
			Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(4));
			Assert.assertEquals("Entreprise migrée : 26.23.", textes.get(5));
		}
	}

	@Test
	public void testDetectionNegativeDoublon() throws Exception {

		final long noEntreprise = 2623L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addRaisonSociale(e, null, "Chez-moi SA", null, null, true);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// en base : le flag débiteur inactif ne doit pas avoir été mis
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);
			Assert.assertFalse(entreprise.isDebiteurInactif());     // pas d'étoile dans la raison sociale -> actif!
		});

		// et dans les messages de suivi ?
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(5, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(1));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(2));
			Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(3));
			Assert.assertEquals("Entreprise migrée : 26.23.", textes.get(4));
		}
	}

	@Test
	public void testDateFinActiviteDateRequisitionRadiation() throws Exception {

		final long noEntreprise = 2623L;
		final long noEntrepriseApresFusion = 42632L;
		final RegDate dateCreationFor = RegDate.get(2005, 2, 1);
		final RegDate dateBilanFusion = RegDate.get(2007, 3, 12);
		final RegDate dateRequisitionRadiation = RegDate.get(2006, 4, 21);
		final RegDate datePrononceFaillite = RegDate.get(2005, 6, 2);
		final RegDate dateDissolution = RegDate.get(2007, 3, 4);

		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, dateCreationFor, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		e.setDateRequisitionRadiation(dateRequisitionRadiation);
		addFusion(e, buildEntreprise(noEntrepriseApresFusion), dateBilanFusion);
		addPrononceFaillite(e, datePrononceFaillite.addDays(-10), RegpmTypeEtatEntreprise.EN_FAILLITE, datePrononceFaillite);
		e.setDateDissolution(dateDissolution);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// en base : le for principal doit avoir été limité à la date de radiation RC
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateCreationFor, ffp.getDateDebut());
			Assert.assertEquals(dateRequisitionRadiation, ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
			Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifFermeture());
		});

		// et dans les messages de suivi ?
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(7, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Plusieurs dates de fin d'activité en concurrence : date de réquisition de radiation (21.04.2006), date de bilan de fusion (12.03.2007), date de prononcé de faillite (02.06.2005), date de dissolution (04.03.2007).", textes.get(1));
			Assert.assertEquals("Date de fin d'activité proposée (date de réquisition de radiation) : 21.04.2006.", textes.get(2));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(3));
			Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(4));
			Assert.assertEquals("Etat 'EN_FAILLITE' migré, dès le 23.05.2005.", textes.get(5));
			Assert.assertEquals("Entreprise migrée : 26.23.", textes.get(6));
		}
	}

	@Test
	public void testDateFinActiviteDateBilanFusion() throws Exception {

		final long noEntreprise = 2623L;
		final long noEntrepriseApresFusion = 42632L;
		final RegDate dateCreationFor = RegDate.get(2005, 2, 1);
		final RegDate dateBilanFusion = RegDate.get(2007, 3, 12);
		final RegDate dateRequisitionRadiation = RegDate.get(2006, 4, 21);
		final RegDate datePrononceFaillite = RegDate.get(2005, 6, 2);
		final RegDate dateDissolution = RegDate.get(2007, 3, 4);

		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, dateCreationFor, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
//		e.setDateRequisitionRadiation(dateRequisitionRadiation);
		addFusion(e, buildEntreprise(noEntrepriseApresFusion), dateBilanFusion);
		addPrononceFaillite(e, datePrononceFaillite.addDays(-10), RegpmTypeEtatEntreprise.EN_FAILLITE, datePrononceFaillite);
		e.setDateDissolution(dateDissolution);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// en base : le for principal doit avoir été limité à la date de radiation RC
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateCreationFor, ffp.getDateDebut());
			Assert.assertEquals(dateBilanFusion, ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
			Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifFermeture());
		});

		// et dans les messages de suivi ?
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(7, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Plusieurs dates de fin d'activité en concurrence : date de bilan de fusion (12.03.2007), date de prononcé de faillite (02.06.2005), date de dissolution (04.03.2007).", textes.get(1));
			Assert.assertEquals("Date de fin d'activité proposée (date de bilan de fusion) : 12.03.2007.", textes.get(2));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(3));
			Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(4));
			Assert.assertEquals("Etat 'EN_FAILLITE' migré, dès le 23.05.2005.", textes.get(5));
			Assert.assertEquals("Entreprise migrée : 26.23.", textes.get(6));
		}
	}

	@Test
	public void testDateFinActivitePrononceFaillite() throws Exception {

		final long noEntreprise = 2623L;
		final RegDate dateCreationFor = RegDate.get(2005, 2, 1);
		final RegDate dateRequisitionRadiation = RegDate.get(2006, 4, 21);
		final RegDate datePrononceFaillite = RegDate.get(2005, 6, 2);
		final RegDate dateDissolution = RegDate.get(2007, 3, 4);

		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, dateCreationFor, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
//		e.setDateRequisitionRadiation(dateRequisitionRadiation);
//		addFusion(e, buildEntreprise(noEntrepriseApresFusion), dateBilanFusion);
		addPrononceFaillite(e, datePrononceFaillite.addDays(-10), RegpmTypeEtatEntreprise.EN_FAILLITE, datePrononceFaillite);
		e.setDateDissolution(dateDissolution);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

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
			Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifFermeture());
		});

		// et dans les messages de suivi ?
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(7, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Plusieurs dates de fin d'activité en concurrence : date de prononcé de faillite (02.06.2005), date de dissolution (04.03.2007).", textes.get(1));
			Assert.assertEquals("Date de fin d'activité proposée (date de prononcé de faillite) : 02.06.2005.", textes.get(2));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(3));
			Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(4));
			Assert.assertEquals("Etat 'EN_FAILLITE' migré, dès le 23.05.2005.", textes.get(5));
			Assert.assertEquals("Entreprise migrée : 26.23.", textes.get(6));
		}
	}

	@Test
	public void testDateFinActiviteDissolution() throws Exception {

		final long noEntreprise = 2623L;
		final RegDate dateCreationFor = RegDate.get(2005, 2, 1);
		final RegDate dateRequisitionRadiation = RegDate.get(2006, 4, 21);
		final RegDate datePrononceFaillite = RegDate.get(2005, 6, 2);
		final RegDate dateDissolution = RegDate.get(2007, 3, 4);

		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, dateCreationFor, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
//		e.setDateRadiationRC(dateRadiationRC);      pas de date de radiation -> c'est la date de dissolution qui prime, maintenant
//		addFusion(e, buildEntreprise(noEntrepriseApresFusion), dateBilanFusion);
//		addPrononceFaillite(e, datePrononceFaillite.addDays(-10), RegpmTypeEtatEntreprise.EN_FAILLITE, datePrononceFaillite);
		e.setDateDissolution(dateDissolution);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// en base : le for principal doit avoir été limité à la date de radiation RC
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateCreationFor, ffp.getDateDebut());
			Assert.assertEquals(dateDissolution, ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
			Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifFermeture());
		});

		// et dans les messages de suivi ?
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(5, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Date de fin d'activité proposée (date de dissolution) : 04.03.2007.", textes.get(1));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(2));
			Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(3));
			Assert.assertEquals("Entreprise migrée : 26.23.", textes.get(4));
		}
	}

	@Test
	public void testDateFinActiviteVide() throws Exception {

		final long noEntreprise = 2623L;
		final RegDate dateCreationFor = RegDate.get(2005, 2, 1);

		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, dateCreationFor, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// en base : le for principal doit avoir été limité à la date de radiation RC
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateCreationFor, ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
			Assert.assertNull(ffp.getMotifFermeture());
		});
	}

	@Test
	public void testDateFinActiviteAvantDebutForPrincipal() throws Exception {

		final long noEntreprise = 2623L;
		final RegDate dateCreationFor = RegDate.get(2005, 2, 1);
		final RegDate dateDissolution = RegDate.get(2006, 6, 12);
		final RegDate dateCreationDeuxiemeFor = dateDissolution.addDays(5);

		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addForPrincipalSuisse(e, dateCreationFor, RegpmTypeForPrincipal.SIEGE, Commune.LAUSANNE);
		addForPrincipalSuisse(e, dateCreationDeuxiemeFor, RegpmTypeForPrincipal.SIEGE, Commune.MORGES);
		addSiegeSuisse(e, dateCreationFor, Commune.LAUSANNE);
		addSiegeSuisse(e, dateCreationDeuxiemeFor, Commune.MORGES);
		e.setDateDissolution(dateDissolution);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// pour récupérer le numéro de tiers de l'établissement principal créé
		final MutableLong noEtablissementPrincipal = new MutableLong();

		// en base : la date de fin doit avoir été ignorée car incohérente avec un for principal qui commence après...
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final List<ForFiscalPrincipalPM> ffps = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(ffps);
			Assert.assertEquals(2, ffps.size());

			{
				final ForFiscalPrincipalPM ffp = ffps.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateCreationFor, ffp.getDateDebut());
				Assert.assertEquals(dateCreationDeuxiemeFor.getOneDayBefore(), ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifFermeture());
			}
			{
				final ForFiscalPrincipalPM ffp = ffps.get(1);
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateCreationDeuxiemeFor, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.MORGES.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getMotifFermeture());
			}

			// récupération du numéro fiscal de l'établissement principal généré
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(1, etablissements.size());

			final Etablissement etablissementPrincipal = etablissements.get(0);
			Assert.assertNotNull(etablissementPrincipal);
			noEtablissementPrincipal.setValue(etablissementPrincipal.getNumero());

			// cet établissement ne doit avoir qu'un seul domicile (on ne prend que le premier)
			final Set<DomicileEtablissement> domiciles = etablissementPrincipal.getDomiciles();
			Assert.assertNotNull(domiciles);
			Assert.assertEquals(2, domiciles.size());

			final List<DomicileEtablissement> domicilesTries = new ArrayList<>(domiciles);
			Collections.sort(domicilesTries, new DateRangeComparator<>());

			{
				final DomicileEtablissement domicile = domicilesTries.get(0);
				Assert.assertNotNull(domicile);
				Assert.assertEquals(dateCreationFor, domicile.getDateDebut());
				Assert.assertEquals(dateCreationDeuxiemeFor.getOneDayBefore(), domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.LAUSANNE.getNoOfs(), domicile.getNumeroOfsAutoriteFiscale());
				Assert.assertFalse(domicile.isAnnule());
			}
			{
				final DomicileEtablissement domicile = domicilesTries.get(1);
				Assert.assertNotNull(domicile);
				Assert.assertEquals(dateCreationDeuxiemeFor, domicile.getDateDebut());
				Assert.assertNull(domicile.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, domicile.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.MORGES.getNoOfs(), domicile.getNumeroOfsAutoriteFiscale());
				Assert.assertFalse(domicile.isAnnule());
			}
		});
		Assert.assertNotNull(noEtablissementPrincipal.getValue());

		// et dans les messages de suivi ?
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(8, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Date de fin d'activité proposée (date de dissolution) : 12.06.2006.", textes.get(1));
			Assert.assertEquals("La date de fin d'activité proposée (12.06.2006) est antérieure à la date de début du for principal 2 (17.06.2006), cette date de fin d'activité sera donc ignorée.", textes.get(2));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(3));
			Assert.assertEquals("Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + ".", textes.get(4));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [01.02.2005 -> 16.06.2006] sur COMMUNE_OU_FRACTION_VD/5586.", textes.get(5));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.longValue()) + " : [17.06.2006 -> ?] sur COMMUNE_OU_FRACTION_VD/5642.", textes.get(6));
			Assert.assertEquals("Entreprise migrée : 26.23.", textes.get(7));
		}
	}

	@Test
	public void testUniqueRaisonSocialeAvecDateNulle() throws Exception {

		final long noEntreprise = 2623L;

		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RaisonSociale raisonSociale = addRaisonSociale(e, null, "Ma société à moi", "tout seul", "vraiment", true);
		addFormeJuridique(e, RegDate.get(2007, 6, 14), createTypeFormeJuridique("S.A.R.L.", RegpmCategoriePersonneMorale.PM));

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {

			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<DonneesRegistreCommerce> donneesRC = entreprise.getDonneesRC();
			Assert.assertNotNull(donneesRC);
			Assert.assertEquals(1, donneesRC.size());

			final DonneesRegistreCommerce drc = donneesRC.iterator().next();
			Assert.assertNotNull(drc);
			Assert.assertFalse(drc.isAnnule());
			Assert.assertEquals(RegDate.get(2007, 6, 14), drc.getDateDebut());
			Assert.assertNull(drc.getDateFin());
			Assert.assertEquals(FormeJuridiqueEntreprise.SARL, drc.getFormeJuridique());
			Assert.assertEquals("Ma société à moi tout seul vraiment", drc.getRaisonSociale());

			// pas d'établissement principal généré
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(0, etablissements.size());
		});

		// et dans les messages de migration des données civiles RegPM ?
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(3, textes.size());
			Assert.assertEquals("Raison sociale " + raisonSociale.getId() + " (Ma société à moi tout seul vraiment) ignorée car sa date de début de validité est nulle (ou antérieure au 01.08.1291).", textes.get(0));
			Assert.assertEquals("En l'absence de donnée valide pour la raison sociale, repêchage de 'Ma société à moi tout seul vraiment'.", textes.get(1));
			Assert.assertEquals("Données 'civiles' migrées : sur la période [14.06.2007 -> ?], raison sociale (Ma société à moi tout seul vraiment) et forme juridique (SARL).", textes.get(2));
		}
	}

	@Test
	public void testUniqueFormeJuridiqueAvecDateNulle() throws Exception {

		final long noEntreprise = 2623L;

		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addRaisonSociale(e, RegDate.get(2004, 8, 27), "Ma société à moi", "tout seul", "si si vraiment", true);
		addFormeJuridique(e, null, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {

			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<DonneesRegistreCommerce> donneesRC = entreprise.getDonneesRC();
			Assert.assertNotNull(donneesRC);
			Assert.assertEquals(1, donneesRC.size());

			final DonneesRegistreCommerce drc = donneesRC.iterator().next();
			Assert.assertNotNull(drc);
			Assert.assertFalse(drc.isAnnule());
			Assert.assertEquals(RegDate.get(2004, 8, 27), drc.getDateDebut());
			Assert.assertNull(drc.getDateFin());
			Assert.assertEquals(FormeJuridiqueEntreprise.SA, drc.getFormeJuridique());
			Assert.assertEquals("Ma société à moi tout seul si si vraiment", drc.getRaisonSociale());

			// pas d'établissement principal généré
			final List<Etablissement> etablissements = uniregStore.getEntitiesFromDb(Etablissement.class, null);
			Assert.assertNotNull(etablissements);
			Assert.assertEquals(0, etablissements.size());
		});

		// et dans les messages de migration des données civiles RegPM ?
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.DONNEES_CIVILES_REGPM);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(3, textes.size());
			Assert.assertEquals("Forme juridique 1 (S.A.) ignorée car sa date de début de validité est nulle (ou antérieure au 01.08.1291).", textes.get(0));
			Assert.assertEquals("En l'absence de donnée valide pour la forme juridique, repêchage de 'S.A.'.", textes.get(1));
			Assert.assertEquals("Données 'civiles' migrées : sur la période [27.08.2004 -> ?], raison sociale (Ma société à moi tout seul si si vraiment) et forme juridique (SA).", textes.get(2));
		}
	}

	@Test
	public void testMigrationAdresseCourrierEntreprise() throws Exception {

		final long noEntreprise = 2623L;

		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addRaisonSociale(e, RegDate.get(2004, 8, 27), "Ma société à moi", "tout seul", "si si vraiment", true);
		addFormeJuridique(e, null, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		final RegpmAdresseEntreprise a = addAdresse(e, RegpmTypeAdresseEntreprise.COURRIER, RegDate.get(2004, 8, 27), null, LocalitePostale.RENENS, "Rue des champs", "42", null, null);
		a.setChez("c/o moi");

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {

			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<AdresseTiers> adresses = entreprise.getAdressesTiers();
			Assert.assertNotNull(adresses);
			Assert.assertEquals(1, adresses.size());

			final AdresseTiers adresse = adresses.iterator().next();
			Assert.assertNotNull(adresse);
			Assert.assertFalse(adresse.isAnnule());
			Assert.assertEquals(RegDate.get(2004, 8, 27), adresse.getDateDebut());
			Assert.assertNull(adresse.getDateFin());
			Assert.assertEquals(AdresseSuisse.class, adresse.getClass());
			Assert.assertEquals(TypeAdresseTiers.COURRIER, adresse.getUsage());

			final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
			Assert.assertEquals("42", adresseSuisse.getNumeroMaison());
			Assert.assertEquals("c/o moi", adresseSuisse.getComplement());
			Assert.assertEquals("Rue des champs", adresseSuisse.getRue());
			Assert.assertNull(adresseSuisse.getNumeroRue());
			Assert.assertEquals((Integer) LocalitePostale.RENENS.getNoOrdreP().intValue(), adresseSuisse.getNumeroOrdrePoste());
		});
	}

	@Test
	public void testMigrationAdresseSiegeEntrepriseSansAdresseCourrier() throws Exception {

		final long noEntreprise = 2623L;

		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addRaisonSociale(e, RegDate.get(2004, 8, 27), "Ma société à moi", "tout seul", "si si vraiment", true);
		addFormeJuridique(e, null, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addAdresse(e, RegpmTypeAdresseEntreprise.SIEGE, RegDate.get(2004, 8, 27), null, LocalitePostale.RENENS, "Rue des champs", "42", null, null);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {

			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<AdresseTiers> adresses = entreprise.getAdressesTiers();
			Assert.assertNotNull(adresses);
			Assert.assertEquals(1, adresses.size());

			final AdresseTiers adresse = adresses.iterator().next();
			Assert.assertNotNull(adresse);
			Assert.assertFalse(adresse.isAnnule());
			Assert.assertEquals(RegDate.get(2004, 8, 27), adresse.getDateDebut());
			Assert.assertNull(adresse.getDateFin());
			Assert.assertEquals(AdresseSuisse.class, adresse.getClass());
			Assert.assertEquals(TypeAdresseTiers.COURRIER, adresse.getUsage());

			final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
			Assert.assertEquals("42", adresseSuisse.getNumeroMaison());
			Assert.assertEquals("Rue des champs", adresseSuisse.getRue());
			Assert.assertNull(adresseSuisse.getNumeroRue());
			Assert.assertEquals((Integer) LocalitePostale.RENENS.getNoOrdreP().intValue(), adresseSuisse.getNumeroOrdrePoste());
		});
	}

	@Test
	public void testMigrationAdresseEntrepriseAvecAdresseCourrierEtSiege() throws Exception {

		final long noEntreprise = 2623L;

		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addRaisonSociale(e, RegDate.get(2004, 8, 27), "Ma société à moi", "tout seul", "si si vraiment", true);
		addFormeJuridique(e, null, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addAdresse(e, RegpmTypeAdresseEntreprise.COURRIER, RegDate.get(2010, 7, 22), null, LocalitePostale.RENENS, "Rue des étangs", "24", null, null);
		addAdresse(e, RegpmTypeAdresseEntreprise.SIEGE, RegDate.get(2004, 8, 27), null, LocalitePostale.RENENS, "Rue des champs", "42", null, null);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {

			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<AdresseTiers> adresses = entreprise.getAdressesTiers();
			Assert.assertNotNull(adresses);
			Assert.assertEquals(1, adresses.size());

			final AdresseTiers adresse = adresses.iterator().next();
			Assert.assertNotNull(adresse);
			Assert.assertFalse(adresse.isAnnule());
			Assert.assertEquals(RegDate.get(2010, 7, 22), adresse.getDateDebut());
			Assert.assertNull(adresse.getDateFin());
			Assert.assertEquals(AdresseSuisse.class, adresse.getClass());
			Assert.assertEquals(TypeAdresseTiers.COURRIER, adresse.getUsage());

			final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
			Assert.assertEquals("24", adresseSuisse.getNumeroMaison());
			Assert.assertEquals("Rue des étangs", adresseSuisse.getRue());
			Assert.assertNull(adresseSuisse.getNumeroRue());
			Assert.assertEquals((Integer) LocalitePostale.RENENS.getNoOrdreP().intValue(), adresseSuisse.getNumeroOrdrePoste());
		});
	}

	/**
	 * Les fors d'une SNC doivent être migrés en genre impôt "revenu/fortune"
	 */
	@Test
	public void testMigrationForsSNC() throws Exception {

		final long noEntreprise = 2623L;

		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		addRaisonSociale(e, RegDate.get(2004, 8, 27), "Ma société à moi", "tout seul", "si si vraiment", true);
		addFormeJuridique(e, RegDate.get(2004, 8, 27), createTypeFormeJuridique("S.N.C.", RegpmCategoriePersonneMorale.SP));
		addAdresse(e, RegpmTypeAdresseEntreprise.COURRIER, RegDate.get(2010, 7, 22), null, LocalitePostale.RENENS, "Rue des étangs", "24", null, null);
		addForPrincipalSuisse(e, RegDate.get(2004, 8, 27), RegpmTypeForPrincipal.SIEGE, Commune.RENENS);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<ForFiscal> forsFiscaux = entreprise.getForsFiscaux();
			Assert.assertNotNull(forsFiscaux);
			Assert.assertEquals(1, forsFiscaux.size());

			final ForFiscal ff = forsFiscaux.iterator().next();
			Assert.assertNotNull(ff);
			Assert.assertFalse(ff.isAnnule());
			Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());         // <-- c'est ce point qui va faire en sorte qu'il n'y ait pas d'assujettissement
			Assert.assertEquals(RegDate.get(2004, 8, 27), ff.getDateDebut());
			Assert.assertNull(ff.getDateFin());
			Assert.assertEquals(ForFiscalPrincipalPM.class, ff.getClass());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.RENENS.getNoOfs(), ff.getNumeroOfsAutoriteFiscale());
		});
	}

	@Test
	public void testForsDepuisSiegesDroitPublic() throws Exception {

		final long noEntreprise = 2623L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(2004, 8, 27);
		addRaisonSociale(e, dateDebut, "Basel Gemeinde", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("DP", RegpmCategoriePersonneMorale.APM));
		addSiegeSuisse(e, dateDebut, Commune.BALE);
		addAdresse(e, RegpmTypeAdresseEntreprise.COURRIER, RegDate.get(2010, 7, 22), null, LocalitePostale.RENENS, "Rue des étangs", "24", null, null);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<ForFiscal> forsFiscaux = entreprise.getForsFiscaux();
			Assert.assertNotNull(forsFiscaux);
			Assert.assertEquals(0, forsFiscaux.size());
		});

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNull(messages);
		}
		// vérification des messages dans le contexte "DP_APM"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.DP_APM);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(1, textes.size());
			Assert.assertEquals("Forme juridique DP/APM depuis le 27.08.2004.", textes.get(0));
		}
	}

	@Test
	public void testForsDepuisSiegesNonDroitPublic() throws Exception {

		final long noEntreprise = 2623L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(2004, 8, 27);
		addRaisonSociale(e, dateDebut, "Basel Mühlentsorgung AG", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addSiegeSuisse(e, dateDebut, Commune.BALE);
		addAdresse(e, RegpmTypeAdresseEntreprise.COURRIER, RegDate.get(2010, 7, 22), null, LocalitePostale.RENENS, "Rue des étangs", "24", null, null);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<ForFiscal> forsFiscaux = entreprise.getForsFiscaux();
			Assert.assertNotNull(forsFiscaux);
			Assert.assertEquals(1, forsFiscaux.size());

			final ForFiscal ff = forsFiscaux.iterator().next();
			Assert.assertNotNull(ff);
			Assert.assertFalse(ff.isAnnule());
			Assert.assertEquals(GenreImpot.BENEFICE_CAPITAL, ff.getGenreImpot());
			Assert.assertEquals(dateDebut, ff.getDateDebut());
			Assert.assertNull(ff.getDateFin());
			Assert.assertEquals(ForFiscalPrincipalPM.class, ff.getClass());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ff.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.BALE.getNoOfs(), ff.getNumeroOfsAutoriteFiscale());
		});

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(2, textes.size());
			Assert.assertEquals("Données du siège 1 utilisées pour les fors principaux : COMMUNE_HC/2701 depuis le 27.08.2004.", textes.get(0));
			Assert.assertEquals("For principal COMMUNE_HC/2701 [27.08.2004 -> ?] généré.", textes.get(1));
		}
		// vérification des messages dans le contexte "DP_APM"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.DP_APM);
			Assert.assertNull(messages);
		}
	}

	@Test
	public void testMigrationEtatsEntreprise() throws Exception {

		final long noEntreprise = 2623L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(2004, 8, 27);
		addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addEtatEntreprise(e, dateDebut, RegpmTypeEtatEntreprise.FONDEE);
		addEtatEntreprise(e, RegDate.get(2005, 3, 1), RegpmTypeEtatEntreprise.INSCRITE_AU_RC);
		addEtatEntreprise(e, RegDate.get(2007, 3, 1), RegpmTypeEtatEntreprise.RADIEE_DU_RC);
		addEtatEntreprise(e, RegDate.get(2007, 3, 1), RegpmTypeEtatEntreprise.INSCRITE_AU_RC);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<EtatEntreprise> etats = entreprise.getEtats();
			Assert.assertNotNull(etats);
			Assert.assertEquals(4, etats.size());

			final List<EtatEntreprise> etatsTries = new ArrayList<>(etats);
			Collections.sort(etatsTries);
			{
				final EtatEntreprise etat = etatsTries.get(0);
				Assert.assertNotNull(etat);
				Assert.assertFalse(etat.isAnnule());
				Assert.assertEquals(dateDebut, etat.getDateObtention());
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etat.getType());
			}
			{
				final EtatEntreprise etat = etatsTries.get(1);
				Assert.assertNotNull(etat);
				Assert.assertFalse(etat.isAnnule());
				Assert.assertEquals(RegDate.get(2005, 3, 1), etat.getDateObtention());
				Assert.assertEquals(TypeEtatEntreprise.INSCRITE_RC, etat.getType());
			}
			{
				final EtatEntreprise etat = etatsTries.get(2);
				Assert.assertNotNull(etat);
				Assert.assertFalse(etat.isAnnule());
				Assert.assertEquals(RegDate.get(2007, 3, 1), etat.getDateObtention());
				Assert.assertEquals(TypeEtatEntreprise.RADIEE_RC, etat.getType());
			}
			{
				final EtatEntreprise etat = etatsTries.get(3);
				Assert.assertNotNull(etat);
				Assert.assertFalse(etat.isAnnule());
				Assert.assertEquals(RegDate.get(2007, 3, 1), etat.getDateObtention());
				Assert.assertEquals(TypeEtatEntreprise.INSCRITE_RC, etat.getType());
			}
		});

		// vérification des messages dans le contexte "SUIVI"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(9, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(1));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(2));
			Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(3));
			Assert.assertEquals("Etat 'FONDEE' migré, dès le 27.08.2004.", textes.get(4));
			Assert.assertEquals("Etat 'INSCRITE_RC' migré, dès le 01.03.2005.", textes.get(5));
			Assert.assertEquals("Etat 'RADIEE_RC' migré, dès le 01.03.2007.", textes.get(6));
			Assert.assertEquals("Etat 'INSCRITE_RC' migré, dès le 01.03.2007.", textes.get(7));
			Assert.assertEquals("Entreprise migrée : 26.23.", textes.get(8));
		}
	}

	@Test
	public void testMigrationEtatsEntrepriseAvecFusion() throws Exception {

		final long noEntreprise = 2623L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(2004, 8, 27);
		addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addEtatEntreprise(e, dateDebut, RegpmTypeEtatEntreprise.FONDEE);
		addEtatEntreprise(e, RegDate.get(2005, 3, 1), RegpmTypeEtatEntreprise.INSCRITE_AU_RC);
		addEtatEntreprise(e, RegDate.get(2007, 3, 1), RegpmTypeEtatEntreprise.INSCRITE_AU_RC);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final Set<EtatEntreprise> etats = entreprise.getEtats();
			Assert.assertNotNull(etats);
			Assert.assertEquals(2, etats.size());

			final List<EtatEntreprise> etatsTries = new ArrayList<>(etats);
			Collections.sort(etatsTries);

			{
				final EtatEntreprise etat = etatsTries.get(0);
				Assert.assertNotNull(etat);
				Assert.assertFalse(etat.isAnnule());
				Assert.assertEquals(dateDebut, etat.getDateObtention());
				Assert.assertEquals(TypeEtatEntreprise.FONDEE, etat.getType());
			}
			{
				final EtatEntreprise etat = etatsTries.get(1);
				Assert.assertNotNull(etat);
				Assert.assertFalse(etat.isAnnule());
				Assert.assertEquals(RegDate.get(2005, 3, 1), etat.getDateObtention());
				Assert.assertEquals(TypeEtatEntreprise.INSCRITE_RC, etat.getType());
			}
		});

		// vérification des messages dans le contexte "SUIVI"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(8, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise sans exercice commercial ni for principal.", textes.get(1));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(2));
			Assert.assertEquals("Pas de siège associé, pas d'établissement principal créé.", textes.get(3));
			Assert.assertEquals("Fusion des deux états d'entreprise 'INSCRITE_RC' successifs obtenus les 01.03.2005 et 01.03.2007.", textes.get(4));
			Assert.assertEquals("Etat 'FONDEE' migré, dès le 27.08.2004.", textes.get(5));
			Assert.assertEquals("Etat 'INSCRITE_RC' migré, dès le 01.03.2005.", textes.get(6));
			Assert.assertEquals("Entreprise migrée : 26.23.", textes.get(7));
		}
	}

	@Test
	public void testMigrationNotes() throws Exception {

		final long noEntreprise = 2623L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(2004, 8, 27);
		addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addNote(e, RegDate.get(2000, 1, 1), "Première note");
		addNote(e, RegDate.get(1999, 1, 1), "Seconde note");
		addNote(e, RegDate.get(2005, 7, 12), "Cette sté n'a plus qu'une activité de facturation dès le    01.01.1993 selon lettre du 02.06.1993 au dossier. Ne plus   toucher ce dossier qui a été vu et revu par STL. Suite à la réorganisation sous forme de scissions multiples un nouveau dossier a été constitué soit : no xxxxx.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       ");

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final List<Remarque> remarques = uniregStore.getEntitiesFromDb(Remarque.class, Collections.singletonMap("tiers", entreprise));
			Assert.assertNotNull(remarques);
			Assert.assertEquals(3, remarques.size());
			{
				final Remarque remarque = remarques.get(0);
				Assert.assertNotNull(remarque);
				Assert.assertEquals("01.01.2000 - Première note", remarque.getTexte());
			}
			{
				final Remarque remarque = remarques.get(1);
				Assert.assertNotNull(remarque);
				Assert.assertEquals("01.01.1999 - Seconde note", remarque.getTexte());
			}
			{
				final Remarque remarque = remarques.get(2);
				Assert.assertNotNull(remarque);
				Assert.assertEquals("12.07.2005 - Cette sté n'a plus qu'une activité de facturation dès le\n01.01.1993 selon lettre du 02.06.1993 au dossier. Ne plus\ntoucher ce dossier qui a été vu et revu par STL. Suite à la\nréorganisation sous forme de scissions multiples un nouveau\ndossier a été constitué soit : no xxxxx.", remarque.getTexte());
			}
		});
	}

	/**
	 * [SIFISC-16333][SIFISC-17112] Une administration de droit public (actuelle) doit être migrée sans for
	 */
	@Test
	public void testMigrationForsAdministrationDroitPublic() throws Exception {

		final long noEntreprise = 74984L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(2004, 8, 27);
		final RegDate dateChangementFormeJuridique = dateDebut.addYears(3);

		addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addFormeJuridique(e, dateChangementFormeJuridique, createTypeFormeJuridique("DP", RegpmCategoriePersonneMorale.APM));
		addForPrincipalSuisse(e, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final ForFiscalPrincipal ffp = entreprise.getDernierForFiscalPrincipal();
			Assert.assertNull(ffp);     // DP -> pas de for...
		});

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(1, textes.size());
			Assert.assertEquals("For fiscal principal 1 du 27.08.2004 non-migré (administration de droit public).", textes.get(0));
		}
	}

	/**
	 * [SIFISC-16333][SIFISC-17112] Une ancienne administration de droit public (qui ne l'est plus) doit être migrée avec for si elle en a...
	 */
	@Test
	public void testMigrationForsAncienneAdministrationDroitPublic() throws Exception {

		final long noEntreprise = 74984L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(2004, 8, 27);
		final RegDate dateChangementFormeJuridique = dateDebut.addYears(3);

		addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("DP", RegpmCategoriePersonneMorale.APM));
		addFormeJuridique(e, dateChangementFormeJuridique, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addForPrincipalSuisse(e, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final ForFiscalPrincipal ffp = entreprise.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);     // plus DP -> for migré..
			Assert.assertEquals(dateDebut, ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
			Assert.assertFalse(ffp.isAnnule());
		});

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(2, textes.size());
			Assert.assertEquals("Entreprise non-DP (dernière forme juridique : 'S.A.') ayant possédé une forme juridique DP par le passé, des fors fiscaux pourront donc être repris.", textes.get(0));
			Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5518 [27.08.2004 -> ?] généré.", textes.get(1));
		}
	}

	/**
	 * Cas du départ HC...
	 */
	@Test
	public void testSiegePosterieurFinAssujettissementICC() throws Exception {

		final long noEntreprise = 74984L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(2004, 8, 27);
		final RegDate dateDepartHC = dateDebut.addYears(3);

		addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addForPrincipalSuisse(e, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		addForPrincipalSuisse(e, dateDepartHC.getOneDayAfter(), RegpmTypeForPrincipal.SIEGE, Commune.BERN);
		addSiegeSuisse(e, dateDebut, Commune.ECHALLENS);
		addSiegeSuisse(e, dateDepartHC.getOneDayAfter(), Commune.BERN);
		addAssujettissement(e, dateDebut, dateDepartHC, RegpmTypeAssujettissement.LILIC);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		final MutableLong noEtablissementPrincipal = new MutableLong();
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final List<ForFiscalPrincipalPM> forsPrincipaux = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(forsPrincipaux);
			Assert.assertEquals(2, forsPrincipaux.size());

			{
				final ForFiscalPrincipalPM ffp = forsPrincipaux.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateDebut, ffp.getDateDebut());
				Assert.assertEquals(dateDepartHC, ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEPART_HC, ffp.getMotifFermeture());
				Assert.assertFalse(ffp.isAnnule());
			}
			{
				final ForFiscalPrincipalPM ffp = forsPrincipaux.get(1);
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateDepartHC.getOneDayAfter(), ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.BERN.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(MotifFor.DEPART_HC, ffp.getMotifOuverture());
				Assert.assertFalse(ffp.isAnnule());
			}

			noEtablissementPrincipal.setValue(uniregStore.getEntitiesFromDb(Etablissement.class, null).get(0).getNumero());
		});

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(2, textes.size());
			Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5518 [27.08.2004 -> 27.08.2007] généré.", textes.get(0));
			Assert.assertEquals("For principal COMMUNE_HC/351 [28.08.2007 -> ?] généré.", textes.get(1));
		}
		// vérification des messages dans le contexte "SUIVI"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(6, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(1));
			Assert.assertEquals("Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.getValue()) + ".", textes.get(2));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.getValue()) + " : [27.08.2004 -> 27.08.2007] sur COMMUNE_OU_FRACTION_VD/5518.", textes.get(3));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.getValue()) + " : [28.08.2007 -> ?] sur COMMUNE_HC/351.", textes.get(4));
			Assert.assertEquals("Entreprise migrée : 749.84.", textes.get(5));
		}
	}

	/**
	 * Cas de la fin d'activité (= réquisition de radiation)
	 */
	@Test
	public void testSiegePosterieurFinActivite() throws Exception {

		final long noEntreprise = 74984L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(2004, 8, 27);
		final RegDate dateFinActivite = dateDebut.addYears(3);

		addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addForPrincipalSuisse(e, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		addSiegeSuisse(e, dateDebut, Commune.ECHALLENS);
		addSiegeSuisse(e, dateFinActivite.getOneDayAfter(), Commune.BERN);
		addAssujettissement(e, dateDebut, dateFinActivite, RegpmTypeAssujettissement.LILIC);
		e.setDateRequisitionRadiation(dateFinActivite);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		final MutableLong noEtablissementPrincipal = new MutableLong();
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final List<ForFiscalPrincipalPM> forsPrincipaux = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(forsPrincipaux);
			Assert.assertEquals(1, forsPrincipaux.size());

			{
				final ForFiscalPrincipalPM ffp = forsPrincipaux.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateDebut, ffp.getDateDebut());
				Assert.assertEquals(dateFinActivite, ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertFalse(ffp.isAnnule());
			}

			noEtablissementPrincipal.setValue(uniregStore.getEntitiesFromDb(Etablissement.class, null).get(0).getNumero());
		});

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(1, textes.size());
			Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5518 [27.08.2004 -> 27.08.2007] généré.", textes.get(0));
		}
		// vérification des messages dans le contexte "SUIVI"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.SUIVI);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(7, textes.size());
			Assert.assertEquals("L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.", textes.get(0));
			Assert.assertEquals("Date de fin d'activité proposée (date de réquisition de radiation) : 27.08.2007.", textes.get(1));
			Assert.assertEquals("Entreprise sans exercice commercial ni date de bouclement futur.", textes.get(2));
			Assert.assertEquals("Le siège 2 est ignoré car sa date de début de validité (28.08.2007) est postérieure à la date de fin d'activité de l'entreprise (27.08.2007).", textes.get(3));
			Assert.assertEquals("Création de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.getValue()) + ".", textes.get(4));
			Assert.assertEquals("Domicile de l'établissement principal " + FormatNumeroHelper.numeroCTBToDisplay(noEtablissementPrincipal.getValue()) + " : [27.08.2004 -> 27.08.2007] sur COMMUNE_OU_FRACTION_VD/5518.", textes.get(5));
			Assert.assertEquals("Entreprise migrée : 749.84.", textes.get(6));
		}
	}

	@Test
	public void testDeplacementDateDebutPremierForPrincipalPremierJanvier1900SansForSecondaire() throws Exception {

		final long noEntreprise = 74984L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(1900, 1, 1);

		addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addForPrincipalEtranger(e, dateDebut, RegpmTypeForPrincipal.SIEGE, MockPays.Russie.getNoOFS());

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// la date du for principal de l'entreprise ne doit pas avoir été modifiée
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(dateDebut, ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.Russie.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
		});

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(1, textes.size());
			Assert.assertEquals("For principal PAYS_HS/8264 [01.01.1900 -> ?] généré.", textes.get(0));
		}
	}

	@Test
	public void testDeplacementDateDebutPremierForPrincipalHorsSuissePremierJanvier1900AvecForSecondaire() throws Exception {

		final long noEntreprise = 74984L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(1900, 1, 1);
		final RegDate dateDebutForSecondaire = RegDate.get(1917, 11, 6);

		addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addForPrincipalEtranger(e, dateDebut, RegpmTypeForPrincipal.SIEGE, MockPays.Russie.getNoOFS());
		addForSecondaire(e, dateDebutForSecondaire, null, Commune.LAUSANNE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// la date du for principal de l'entreprise ne doit pas avoir été modifiée
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(dateDebutForSecondaire, ffp.getDateDebut());
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals((Integer) MockPays.Russie.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
		});

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(2, textes.size());
			Assert.assertEquals("La date de début de validité du for principal 1 est passée du 01.01.1900 au 06.11.1917 pour suivre le premier for secondaire existant.", textes.get(0));
			Assert.assertEquals("For principal PAYS_HS/8264 [06.11.1917 -> ?] généré.", textes.get(1));
		}
	}

	@Test
	public void testDeplacementDateDebutPremierForPrincipalHorsSuissePremierJanvier1900AvecForSecondaireEtAutreForPrincipalEnsuite() throws Exception {

		final long noEntreprise = 74984L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(1900, 1, 1);
		final RegDate dateDebutDeuxiemeForPrincipal = RegDate.get(1922, 7, 1);
		final RegDate dateDebutForSecondaire = RegDate.get(1917, 11, 6);

		addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addForPrincipalEtranger(e, dateDebut, RegpmTypeForPrincipal.SIEGE, MockPays.Russie.getNoOFS());
		addForPrincipalEtranger(e, dateDebutDeuxiemeForPrincipal, RegpmTypeForPrincipal.SIEGE, MockPays.RoyaumeUni.getNoOFS());
		addForSecondaire(e, dateDebutForSecondaire, null, Commune.LAUSANNE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// la date du for principal de l'entreprise ne doit pas avoir été modifiée
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final List<ForFiscalPrincipalPM> forsPrincipaux = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(forsPrincipaux);
			Assert.assertEquals(2, forsPrincipaux.size());

			{
				final ForFiscalPrincipalPM ffp = forsPrincipaux.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(dateDebutForSecondaire, ffp.getDateDebut());
				Assert.assertEquals(dateDebutDeuxiemeForPrincipal.getOneDayBefore(), ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockPays.Russie.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			}
			{
				final ForFiscalPrincipalPM ffp = forsPrincipaux.get(1);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(dateDebutDeuxiemeForPrincipal, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			}
		});

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(3, textes.size());
			Assert.assertEquals("La date de début de validité du for principal 1 est passée du 01.01.1900 au 06.11.1917 pour suivre le premier for secondaire existant.", textes.get(0));
			Assert.assertEquals("For principal PAYS_HS/8264 [06.11.1917 -> 30.06.1922] généré.", textes.get(1));
			Assert.assertEquals("For principal PAYS_HS/8215 [01.07.1922 -> ?] généré.", textes.get(2));
		}
	}

	@Test
	public void testDeplacementDateDebutPremierForPrincipalHorsSuissePremierJanvier1900AvecForSecondaireEtAutreForPrincipalEntre() throws Exception {

		final long noEntreprise = 74984L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(1900, 1, 1);
		final RegDate dateDebutDeuxiemeForPrincipal = RegDate.get(1914, 8, 31);
		final RegDate dateDebutForSecondaire = RegDate.get(1917, 11, 6);

		addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addForPrincipalEtranger(e, dateDebut, RegpmTypeForPrincipal.SIEGE, MockPays.Russie.getNoOFS());
		addForPrincipalEtranger(e, dateDebutDeuxiemeForPrincipal, RegpmTypeForPrincipal.SIEGE, MockPays.RoyaumeUni.getNoOFS());
		addForSecondaire(e, dateDebutForSecondaire, null, Commune.LAUSANNE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// la date du for principal de l'entreprise ne doit pas avoir été modifiée
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final List<ForFiscalPrincipalPM> forsPrincipaux = entreprise.getForsFiscauxPrincipauxActifsSorted();
			Assert.assertNotNull(forsPrincipaux);
			Assert.assertEquals(2, forsPrincipaux.size());

			{
				final ForFiscalPrincipalPM ffp = forsPrincipaux.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(dateDebut, ffp.getDateDebut());
				Assert.assertEquals(dateDebutDeuxiemeForPrincipal.getOneDayBefore(), ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockPays.Russie.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			}
			{
				final ForFiscalPrincipalPM ffp = forsPrincipaux.get(1);
				Assert.assertNotNull(ffp);
				Assert.assertFalse(ffp.isAnnule());
				Assert.assertEquals(dateDebutDeuxiemeForPrincipal, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
			}
		});

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(3, textes.size());
			Assert.assertEquals("La date de début de validité du for principal 1, bien qu'au 01.01.1900, ne sera pas déplacée à la date de début du premier for secondaire (06.11.1917) en raison de la présence d'un autre for principal dès le 31.08.1914.", textes.get(0));
			Assert.assertEquals("For principal PAYS_HS/8264 [01.01.1900 -> 30.08.1914] généré.", textes.get(1));
			Assert.assertEquals("For principal PAYS_HS/8215 [31.08.1914 -> ?] généré.", textes.get(2));
		}
	}

	@Test
	public void testDeplacementDateDebutPremierForPrincipalHorsCantonPremierJanvier1900AvecForSecondaire() throws Exception {

		final long noEntreprise = 74984L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(1900, 1, 1);
		final RegDate dateDebutForSecondaire = RegDate.get(1917, 11, 6);

		addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addForPrincipalSuisse(e, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.BERN);
		addForSecondaire(e, dateDebutForSecondaire, null, Commune.LAUSANNE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// la date du for principal de l'entreprise ne doit pas avoir été modifiée
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(dateDebut, ffp.getDateDebut());         // for HC -> on ne change rien
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.BERN.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
		});

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(1, textes.size());
			Assert.assertEquals("For principal COMMUNE_HC/351 [01.01.1900 -> ?] généré.", textes.get(0));
		}
	}

	@Test
	public void testDeplacementDateDebutPremierForPrincipalVaudoisPremierJanvier1900AvecForSecondaire() throws Exception {

		final long noEntreprise = 74984L;
		final RegpmEntreprise e = buildEntreprise(noEntreprise);
		final RegDate dateDebut = RegDate.get(1900, 1, 1);
		final RegDate dateDebutForSecondaire = RegDate.get(1917, 11, 6);

		addRaisonSociale(e, dateDebut, "Toto SA", null, null, true);
		addFormeJuridique(e, dateDebut, createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM));
		addForPrincipalSuisse(e, dateDebut, RegpmTypeForPrincipal.SIEGE, Commune.ECHALLENS);
		addForSecondaire(e, dateDebutForSecondaire, null, Commune.LAUSANNE);

		final MockGraphe graphe = new MockGraphe(Collections.singletonList(e),
		                                         null,
		                                         null);
		final MigrationResultCollector mr = new MigrationResultCollector(graphe);
		final EntityLinkCollector linkCollector = new EntityLinkCollector();
		final IdMapper idMapper = new IdMapper();
		migrator.initMigrationResult(mr, idMapper);
		migrate(e, migrator, mr, linkCollector, idMapper);

		// la date du for principal de l'entreprise ne doit pas avoir été modifiée
		doInUniregTransaction(true, status -> {
			final Entreprise entreprise = uniregStore.getEntityFromDb(Entreprise.class, noEntreprise);
			Assert.assertNotNull(entreprise);

			final ForFiscalPrincipalPM ffp = entreprise.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertFalse(ffp.isAnnule());
			Assert.assertEquals(dateDebut, ffp.getDateDebut());         // for VD -> on ne change rien
			Assert.assertNull(ffp.getDateFin());
			Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
			Assert.assertEquals(Commune.ECHALLENS.getNoOfs(), ffp.getNumeroOfsAutoriteFiscale());
		});

		// vérification des messages dans le contexte "FORS"
		{
			final List<MigrationResultCollector.Message> messages = mr.getMessages().get(LogCategory.FORS);
			Assert.assertNotNull(messages);
			final List<String> textes = messages.stream().map(msg -> msg.text).collect(Collectors.toList());
			Assert.assertEquals(1, textes.size());
			Assert.assertEquals("For principal COMMUNE_OU_FRACTION_VD/5518 [01.01.1900 -> ?] généré.", textes.get(0));
		}
	}
}
