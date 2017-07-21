package ch.vd.uniregctb.listes.afc.pm;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.listes.afc.TypeExtractionDonneesRpt;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.DecompositionForsAnneeComplete;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.role.RolePopulationPMExtractor;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscalCanton;
import ch.vd.uniregctb.tiers.AllegementFiscalConfederation;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.DomicileHisto;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RaisonSocialeHisto;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.xml.EnumHelper;

public  class ExtractionDonneesRptPMResults extends ListesResults<ExtractionDonneesRptPMResults> {

	public final int periodeFiscale;

	public static final String NON_ASSUJETTI = "Non assujetti sur la période fiscale";

	protected final ServiceInfrastructureService infraService;
	private final AssujettissementService assujettissementService;
	private final PeriodeImpositionService periodeImpositionService;

	public abstract static class InfoCtbBase<T extends InfoCtbBase> implements Comparable<T> {
		public final long noCtb;

		public InfoCtbBase(long noCtb) {
			this.noCtb = noCtb;
		}

		@Override
		public int compareTo(T o) {
			return (int) (noCtb - o.noCtb);
		}

		public abstract String[] getNomsColonnes();

		public abstract Object[] getValeursColonnes();
	}

	public static class InfoCtbAvecDecisionACI extends InfoCtbBase {
		public final  int numeroOfs;
		public RegDate dateDebut;
		public RegDate dateFin;
		public Date dateCreation;
		private static final String[] NOMS_COLONNES = {"NO_CTB", "FOR_DEFINI","DATE_DEBUT","DATE_FIN","DATE_CREATION"};

		public InfoCtbAvecDecisionACI(long noCtb, int numeroOfs,RegDate dateDebut,RegDate dateFin, Date dateCreation) {
			super(noCtb);
			this.numeroOfs = numeroOfs;
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
			this.dateCreation = dateCreation;
		}

		@Override
		public String[] getNomsColonnes() {
			return NOMS_COLONNES;
		}

		@Override
		public Object[] getValeursColonnes() {
			return new Object[]{noCtb, numeroOfs,dateDebut,dateFin,dateCreation};
		}
	}

	public static class InfoCtbIgnore extends InfoCtbBase {
		public final String raisonIgnore;

		private static final String[] NOMS_COLONNES = {"NO_CTB", "RAISON"};

		public InfoCtbIgnore(long noCtb, String raisonIgnore) {
			super(noCtb);
			this.raisonIgnore = raisonIgnore;
		}

		@Override
		public String[] getNomsColonnes() {
			return NOMS_COLONNES;
		}

		@Override
		public Object[] getValeursColonnes() {
			return new Object[]{noCtb, raisonIgnore};
		}
	}

	public static class InfoIdentificationCtb {
		public final String nom;
		public final String prenom;
		public final String numeroAvs;
		public final RegDate dateNaissance;
		public final Long noCtbPrincipal;
		public final Long noCtbConjoint;

		public InfoIdentificationCtb(String nom, String prenom, String numeroAvs, RegDate dateNaissance, Long noCtbPrincipal, Long noCtbConjoint) {
			this.nom = nom;
			this.prenom = prenom;
			this.numeroAvs = numeroAvs;
			this.dateNaissance = dateNaissance;
			this.noCtbPrincipal = noCtbPrincipal;
			this.noCtbConjoint = noCtbConjoint;
		}
	}

	public static class InfoPeriodeImposition extends InfoCtbBase<InfoPeriodeImposition> {
		public final FormeJuridiqueEntreprise formeJuridique;
		public final Integer noOfsCommune;
		public final String nomCommune;
		public final int joursImposables;
		public final String raisonSociale;
		public final String regimeFiscalCode;
		public final AllegementFiscalConfederation.Type typeAllegementFiscalIFD;
		public final RegDate debutAllegementIFD;
		public final RegDate finAllegementIFD;
		public final String tauxOuMontantIFD;
		public final BigDecimal tauxAllegementIFD;
		public final AllegementFiscalCanton.Type typeAllegementFiscalVD;
		public final String tauxOuMontantVD;
		public final BigDecimal tauxAllegemenVD;
		public final int noOfSiegeCivil;
		public final String nomSiegeCivil;
		public final RegDate debutExerciceCommercial;
		public final RegDate finExerciceCommercial;
		public final FlagEntreprise specifiteFiscale;
		public final String noIDE;
		public final RegDate dateFondation;
		public final RegDate dateDebutPI;
		public final RegDate datefinPI;
		public final MotifRattachement motifRattachement;
		public final MotifFor motifOuverture;
		public final MotifFor motifFermeture;
		public final Integer noOfsForPrincipal;
		public final String nomForPrincipal;
		public final String autoriteFiscaleForPrincipal;
		public final TypeEtatEntreprise etatEntreprise;


		protected static final String[] NOMS_COLONNES =
				{"REG_FormeJuridique", "REG_NoCommune", "REG_Commune", "REG_NoCTB", "REG_JoursImposables", "REG_RaisonSociale", "REG_RegimeFiscal", "REG_TypeAllegementIFD",
						"REG_DateDebutAllegementIFD", "REG_DateFinAllegementIFD", "REG_TauxOuMontantIFD", "REG_TauxAllegementIFD", "REG_TypeAllegementVD",
						"REG_TauxOuMontantVD", "REG_TauxAllegementVD","REG_SiegeCiviNolOFS", "REG_SiegeCivil", "REG_DebutExerciceCommercial",
						"REG_FinExerciceCommercial", "REG_SpecificiteFiscale", "REG_NoIDE", "REG_DateFondation", "REG_DateDebutPI", "REG_DateFinPI", "REG_MotifRattachement",
						"REG_MotifOuvertureFor", "REG_MotifFermetureFor","REG_NoOFSForPri","REG_NomForPri", "REG_AutoriteFiscalePrincipale", "REG_EtatFiscal"};




		public InfoPeriodeImposition(Entreprise entreprise, PeriodeImpositionPersonnesMorales periode, TiersService tiersService, ServiceInfrastructureService infraService) {
			super(entreprise.getNumero());

			this.formeJuridique = getFormeJuridique(entreprise,periode.getDateFin(),tiersService );
			RolePopulationPMExtractor extractor = new RolePopulationPMExtractor(MotifFor.DEPART_HC);
			final ForFiscalRevenuFortune forTrouve = extractor.getForPourRoles(periode.getPeriodeFiscale(),entreprise);
			this.noOfsCommune = forTrouve!=null? extractor.getCommunePourRoles(periode.getPeriodeFiscale(),entreprise):null;
			this.nomCommune = getNomAutoriteFor(forTrouve,periode,infraService);
			this.joursImposables = FiscalDateHelper.getLongueurEnJours(periode.getDateDebut(), periode.getDateFin());
			this.raisonSociale = getRaisonSociale(entreprise,periode.getDateFin(),tiersService);
			this.regimeFiscalCode = getRegimeFiscal(entreprise,periode.getDateFin());
			final AllegementFiscalConfederation allegementFiscalIFD = getAllegementFiscalIFD(entreprise, periode.getDateFin());
			this.typeAllegementFiscalIFD = allegementFiscalIFD!=null?allegementFiscalIFD.getType():null;
			this.debutAllegementIFD =allegementFiscalIFD!=null?allegementFiscalIFD.getDateDebut():null;
			this.finAllegementIFD = allegementFiscalIFD!=null?allegementFiscalIFD.getDateFin():null;
			this.tauxOuMontantIFD = getValeurTauxOuMontant(allegementFiscalIFD);
			this.tauxAllegementIFD = allegementFiscalIFD!=null?allegementFiscalIFD.getPourcentageAllegement():null;
			final AllegementFiscalCanton allegementFiscalVD = getAllegementFiscalVD(entreprise, periode.getDateFin());
			this.typeAllegementFiscalVD = allegementFiscalVD!=null?allegementFiscalVD.getType():null;
			this.tauxOuMontantVD = getValeurTauxOuMontant(allegementFiscalVD);
			this.tauxAllegemenVD = allegementFiscalVD!=null?allegementFiscalVD.getPourcentageAllegement():null;
			final DomicileHisto siege = getSiege(entreprise,periode.getDateFin(),tiersService);
			this.noOfSiegeCivil = siege!=null?siege.getNumeroOfsAutoriteFiscale():0;
			final Commune communeSiege = siege != null ? infraService.getCommuneByNumeroOfs(noOfSiegeCivil, null) : null;
			this.nomSiegeCivil = communeSiege!=null?communeSiege.getNomOfficiel():null;
			this.debutExerciceCommercial = periode.getExerciceCommercial().getDateDebut();
			this.finExerciceCommercial = periode.getExerciceCommercial().getDateFin();
			this.specifiteFiscale = getSpecialite(entreprise,periode.getDateFin());
			this.noIDE = tiersService.getNumeroIDE(entreprise);
			this.dateFondation = getDateCreation(entreprise);
			this.dateDebutPI = periode.getDateDebut();
			this.datefinPI = periode.getDateFin();
			final ForFiscalPrincipalPM forFiscal = getDernierForFiscalPrincipalSurPeriode(entreprise, periode);
			this.motifRattachement = forFiscal!=null?forFiscal.getMotifRattachement():null;
			this.motifOuverture = forFiscal!=null?forFiscal.getMotifOuverture():null;
			this.motifFermeture = forFiscal!=null?forFiscal.getMotifFermeture():null;
			this.noOfsForPrincipal = forFiscal!=null?forFiscal.getNumeroOfsAutoriteFiscale():null;
			final Commune communeFor = noOfsForPrincipal!=null?infraService.getCommuneByNumeroOfs(noOfsForPrincipal, null):null;
			this.nomForPrincipal =getNomAutoriteFor(forFiscal,periode,infraService);
			this.autoriteFiscaleForPrincipal = getTypeAutoriteFiscale(forFiscal);
			final EtatEntreprise etatEntreprise = entreprise.getEtatAt(periode.getDateFin());
			this.etatEntreprise = etatEntreprise!=null?etatEntreprise.getType():null;

		}



		@Override
		public int compareTo(InfoPeriodeImposition o) {
			int comparison = super.compareTo(o);
			if (comparison == 0) {
				// on compare ensuite la date de début de la période d'assujettissement
				comparison = NullDateBehavior.EARLIEST.compare(dateDebutPI, o.dateDebutPI);
			}
			return comparison;
		}

		@Override
		public String[] getNomsColonnes() {
			return NOMS_COLONNES;
		}

		@Override
		public Object[] getValeursColonnes() {
			final String specificiteFiscaleName = specifiteFiscale!=null?EnumHelper.coreToXMLv5(specifiteFiscale.getType()).name():null;
			return new Object[]{EnumHelper.coreToXMLv5(formeJuridique), noOfsCommune, nomCommune, noCtb, joursImposables, raisonSociale, regimeFiscalCode,  typeAllegementFiscalIFD,
					debutAllegementIFD, finAllegementIFD, tauxOuMontantIFD, tauxAllegementIFD,typeAllegementFiscalVD,tauxOuMontantVD,tauxAllegemenVD, noOfSiegeCivil, nomSiegeCivil, debutExerciceCommercial, finExerciceCommercial, specificiteFiscaleName, noIDE,
					dateFondation, dateDebutPI, datefinPI, EnumHelper.coreToXMLv1(motifRattachement), EnumHelper.coreToXMLv1(motifOuverture), EnumHelper.coreToXMLv1(motifFermeture),
					noOfsForPrincipal,nomForPrincipal,autoriteFiscaleForPrincipal, EnumHelper.coreToXMLv5(etatEntreprise)};
		}


		protected FlagEntreprise getSpecialite(Entreprise entreprise, RegDate date){
			final List<FlagEntreprise> liste = entreprise.getFlagsNonAnnulesTries();
			return  DateRangeHelper.rangeAt(liste, date);

		}

		protected String getNomAutoriteFor(ForFiscalRevenuFortune ffpm,PeriodeImpositionPersonnesMorales periode,ServiceInfrastructureService infraService){
			if (ffpm == null) {
				return null;
			}
			String nom=null;
			final Integer numeroOfs = ffpm.getNumeroOfsAutoriteFiscale();
			if (ffpm.getTypeAutoriteFiscale()== TypeAutoriteFiscale.PAYS_HS) {
				final Pays pays = infraService.getPays(numeroOfs, periode.getDateFin());
				nom = pays!=null?pays.getNomOfficiel():null;
			}
			else{
				//on est en suisse
				final Commune commune = infraService.getCommuneByNumeroOfs(numeroOfs,periode.getDateFin());
				nom = commune!=null?commune.getNomOfficiel():null;
			}
			return nom;
		}

		protected String getRaisonSociale(Entreprise entreprise, RegDate date,  TiersService tiersService) {
			final List<RaisonSocialeHisto> liste = tiersService.getRaisonsSociales(entreprise, false);
			RaisonSocialeHisto raisonSocialeHisto = DateRangeHelper.rangeAt(liste, date);
			if (raisonSocialeHisto != null) {
				return raisonSocialeHisto.getRaisonSociale();
			}
			return null;
		}

		protected String getRegimeFiscal(Entreprise entreprise, RegDate date) {
			final List<RegimeFiscal> liste = entreprise.getRegimesFiscauxNonAnnulesTries(RegimeFiscal.Portee.VD);
			final RegimeFiscal regimeFiscal = DateRangeHelper.rangeAt(liste, date);
			if (regimeFiscal!= null) {
				return regimeFiscal.getCode();
			}

			return null;

		}

		protected String getValeurTauxOuMontant(AllegementFiscal allegementFiscal){
			if (allegementFiscal == null) {
				return null;
			}
			if (allegementFiscal.isAllegementMontant()) {
				return "MONTANT";
			}
			else{
				return "TAUX";
			}
		}


		protected FormeJuridiqueEntreprise getFormeJuridique(Entreprise entreprise, RegDate date, TiersService tiersService) {
			final List<FormeLegaleHisto> formesLegales = tiersService.getFormesLegales(entreprise, false);
			final FormeLegaleHisto formeLegaleHisto = DateRangeHelper.rangeAt(formesLegales, date);
			if (formeLegaleHisto != null) {
				return FormeJuridiqueEntreprise.fromCode(formeLegaleHisto.getFormeLegale().getCode());
			}
			return  null;
		}

		protected AllegementFiscalConfederation getAllegementFiscalIFD(Entreprise entreprise, RegDate date) {
			final List<AllegementFiscal> liste = entreprise.getAllegementsFiscauxNonAnnulesTries();
			return liste.stream().filter(AllegementFiscalConfederation.class::isInstance)
					.map(AllegementFiscalConfederation.class::cast).filter(a->a.isValidAt(date)).findFirst().orElse(null);
		}

		protected AllegementFiscalCanton getAllegementFiscalVD(Entreprise entreprise, RegDate date) {
			final List<AllegementFiscal> liste = entreprise.getAllegementsFiscauxNonAnnulesTries();
			return liste.stream().filter(AllegementFiscalCanton.class::isInstance)
					.map(AllegementFiscalCanton.class::cast).filter(a->a.isValidAt(date)).findFirst().orElse(null);
		}

		protected DomicileHisto getSiege(Entreprise entreprise, RegDate date,TiersService tiersService) {
			final List<DomicileHisto> liste = tiersService.getSieges(entreprise,false);
			return DateRangeHelper.rangeAt(liste, date);
		}
		/*protected EtatEntreprise getEtatEntreprise(Entreprise entreprise, RegDate date) {
			final List<EtatEntreprise> liste = entreprise.getEtatsNonAnnulesTries();
			return DateRangeHelper.rangeAt(liste, date));
		}*/

		private static RegDate getDateCreation(Entreprise entreprise) {
			final List<EtatEntreprise> etats = entreprise.getEtatsNonAnnulesTries();
			return etats == null || etats.isEmpty() ? null : etats.get(0).getDateObtention();
		}

	}

	private static String getTypeAutoriteFiscale(ForFiscalPrincipalPM forFiscal) {
		if (forFiscal == null) {
			return null;
		}
		//CAS PARTICULIER du for fiscal avec départ HC qiui a pour autorité fiscale VD,
		// on doit  considérer ces cas commes des autorité fiscales HC dans le cadre de la RPT (CF revue TAB technique dans SIFISC-25638)
		if (forFiscal.getDateFin()!=null && forFiscal.getMotifFermeture() ==MotifFor.DEPART_HC  ) {
			return "HC";
		}
		switch (forFiscal.getTypeAutoriteFiscale()){
		case PAYS_HS:
			return "HS";
		case COMMUNE_HC:
			return "HC";
		case COMMUNE_OU_FRACTION_VD:
			return "VD";
		default:
			return null;
		}
	}

	private static ForFiscalPrincipalPM getDernierForFiscalPrincipalSurPeriode(Entreprise entreprise, PeriodeImpositionPersonnesMorales periode) {

		final ForFiscalPrincipalPM forFiscal = entreprise.getForFiscalPrincipalAt(periode.getDateFin());
		if (forFiscal != null) {
			return forFiscal;
		}
		//On a pas de for à la fin de la période, il faut que l'on remonte jusqu'au dernier for vaudois fermé avant ls fin de la période

		return entreprise.getDernierForFiscalPrincipalAvant(periode.getDateFin());
	}

	protected static class CoupleInvalideException extends Exception {

		private final MenageCommun menageCommun;

		public CoupleInvalideException(MenageCommun menageCommun) {
			this.menageCommun = menageCommun;
		}

		public MenageCommun getMenageCommun() {
			return menageCommun;
		}
	}

	public ExtractionDonneesRptPMResults(RegDate dateTraitement, int periodeFiscale, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService,
	                                     AssujettissementService assujettissementService, PeriodeImpositionService periodeImpositionService, AdresseService adresseService) {
		super(dateTraitement, nbThreads, tiersService, adresseService);
		this.periodeFiscale = periodeFiscale;
		this.infraService = infraService;
		this.assujettissementService = assujettissementService;
		this.periodeImpositionService = periodeImpositionService;
	}

	public TypeExtractionDonneesRpt getMode() {
		return TypeExtractionDonneesRpt.IBC;
	}

	;

	private int nbContribuablesAnalyses = 0;
	private final List<InfoPeriodeImposition> listePeriode = new LinkedList<>();
	private final List<InfoCtbIgnore> listeCtbsIgnores = new LinkedList<>();
	private final List<InfoCtbAvecDecisionACI> listeCtbDecisionACI = new LinkedList<>();

	protected static class ContribuableIgnoreException extends Exception {
		public ContribuableIgnoreException(String message) {
			super(message);
		}
	}
	protected static class ContribuableAvecDecisionACIException extends Exception {
		public ContribuableAvecDecisionACIException(String message) {
			super(message);
		}
	}
	/**
	 * Calcul de la liste des périodes à faire apparaître dans l'extraction pour un contribuable donné
	 *
	 * @param ctb le contribuable à analyser
	 * @return périodes à faire apparaître dans l'extraction
	 * @throws ServiceInfrastructureException problème dans l'infrastructure fiscale
	 * @throws CoupleInvalideException        contribuable ménage commun sans lien vers des personnes physiques
	 * @throws AssujettissementException      problème dans le calcul de l'assujettissement du contribuable
	 * @throws ContribuableIgnoreException    si le contribuable est ignoré (le message inclus dans l'exception en explique la raison)
	 */
	private List<InfoPeriodeImposition> buildInfoPeriodes(Contribuable ctb) throws ServiceInfrastructureException, CoupleInvalideException, AssujettissementException, ContribuableIgnoreException {
		final DecompositionForsAnneeComplete decomposition = new DecompositionForsAnneeComplete(ctb, periodeFiscale);
		return buildInfoPeriodes(decomposition);
	}

	/**
	 * Calcul de la liste des périodes à faire apparaître dans l'extraction pour un contribuable donné
	 *
	 * @param decomposition décomposition des fors pour le contribuable inspecté sur l'année de la période fiscale
	 * @return périodes à faire apparaître dans l'extraction
	 * @throws ServiceInfrastructureException problème dans l'infrastructure fiscale
	 * @throws CoupleInvalideException        contribuable ménage commun sans lien vers des personnes physiques
	 * @throws AssujettissementException      problème dans le calcul de l'assujettissement du contribuable
	 * @throws ContribuableIgnoreException    si le contribuable est ignoré (le message inclus dans l'exception en explique la raison)
	 */
	protected List<InfoPeriodeImposition> buildInfoPeriodes(DecompositionForsAnneeComplete decomposition) throws ServiceInfrastructureException, CoupleInvalideException, AssujettissementException,
			ContribuableIgnoreException {
		return null;
	}

	/**
	 * Construit un objet {@link InfoIdentificationCtb} pour le contribuable donné à la date donnée
	 *
	 * @param ctb contribuable
	 * @param pf  période de référence de référence (pour les rapports d'appartenance ménage)
	 * @return une nouvelle instance de InfoIdentificationCtb correspondant au contribuable
	 * @throws CoupleInvalideException si le ménage commun n'a pas de contribuable principal à la date donnée
	 */
	protected InfoIdentificationCtb buildInfoIdentification(Contribuable ctb, int pf) throws CoupleInvalideException {

		final String nom;
		final String prenom;
		final RegDate dateNaissance;
		final String numeroAvs;
		final Long noCtbPrincipal;
		final Long noCtbConjoint;
		if (ctb instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) ctb;
			final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(pp, false);
			if (nomPrenom != null) {
				nom = nomPrenom.getNom();
				prenom = nomPrenom.getPrenom();
			}
			else {
				nom = null;
				prenom = null;
			}
			numeroAvs = tiersService.getNumeroAssureSocial(pp);
			dateNaissance = tiersService.getDateNaissance(pp);
			noCtbPrincipal = null;
			noCtbConjoint = null;
		}
		else if (ctb instanceof MenageCommun) {
			final MenageCommun mc = (MenageCommun) ctb;

			final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, pf);
			if (couple == null || couple.getPrincipal() == null) {
				throw new CoupleInvalideException(mc);
			}

			final PersonnePhysique principal = couple.getPrincipal();
			final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(principal, false);
			if (nomPrenom != null) {
				nom = nomPrenom.getNom();
				prenom = nomPrenom.getPrenom();
			}
			else {
				nom = null;
				prenom = null;
			}
			numeroAvs = tiersService.getNumeroAssureSocial(principal);
			dateNaissance = tiersService.getDateNaissance(principal);
			noCtbPrincipal = principal.getNumero();

			final PersonnePhysique conjoint = couple.getConjoint(principal);
			noCtbConjoint = conjoint != null ? conjoint.getNumero() : null;
		}
		else {
			throw new IllegalArgumentException("Type de tiers non-supporté : " + ctb.getClass().getName() + " (" + ctb.getNumero() + ')');
		}

		return new InfoIdentificationCtb(nom, prenom, numeroAvs, dateNaissance, noCtbPrincipal, noCtbConjoint);
	}



	@Override
	public final void addContribuable(Contribuable ctb) throws ServiceInfrastructureException {
		++nbContribuablesAnalyses;
		final Entreprise entreprise = (Entreprise) ctb;
		//Verification de présence de décision ACI


		try {
			if (hasDecisionsACI(entreprise, periodeFiscale)) {
				addContribuableAvecDecisionACI(ctb);
			}
			
			final List<PeriodeImposition> periodes = periodeImpositionService.determine(entreprise);
			if (periodes == null) {
				throw new ContribuableIgnoreException("Pas de période d'imposition trouvée");
			}
			final List<PeriodeImpositionPersonnesMorales> periodesImpositionFiltres = periodes.stream()
					.map(p -> (PeriodeImpositionPersonnesMorales) p)
					.filter(pipm -> pipm.getExerciceCommercial().getDateFin().year() == periodeFiscale).collect(Collectors.toList());
			if (CollectionUtils.isNotEmpty(periodesImpositionFiltres)) {
				for (PeriodeImpositionPersonnesMorales periode : periodesImpositionFiltres) {
					addPeriodeImposition(new InfoPeriodeImposition(entreprise,periode,tiersService,infraService));
				}
			}else{
				throw new ContribuableIgnoreException("Pas de période d'imposition trouvée");
			}
		}
		catch (AssujettissementException e) {
			addErrorException(ctb, e);
		}

		catch (ContribuableIgnoreException e) {
			addContribuableIgnore(ctb, e.getMessage());
		}
	}



	private boolean hasDecisionsACI(Entreprise entreprise, int periodeFiscale) {
		List<DecisionAci> decisions = entreprise.getDecisionsSorted();
		if (CollectionUtils.isNotEmpty(decisions)) {
			final Optional<DecisionAci> decisionAci = decisions.stream().filter(d->isDecisionMatchWithPeriode(d,periodeFiscale))
					.findFirst();
			return decisionAci.isPresent();

		}else{
			return false;
		}


	}
	
	private boolean isDecisionMatchWithPeriode(DecisionAci d,int periodeFiscale){
		//Si c'est une décision ACI qui est créé après la pf alors on ne la prend pas en compte
		if (d.getDateDebut().isAfter(RegDateHelper.get(periodeFiscale,12,31))) {
			return false;
		}
		//La decision Aci est toujours ouverte sur la période, on la prend en compte
		if (d.getDateFin() == null){
			return true;
		}else{
			//Decision aci qui fini sur la période N ou la période N-1
			if (d.getDateFin().year() == periodeFiscale || d.getDateFin().year() == periodeFiscale - 1) {
				return true;
			}else{
				return false;
			}

		}

		
	}
	
	private void buildInfosPeriodes(Entreprise ese, List<PeriodeImpositionPersonnesMorales> periodesImposition) {


	}

	protected void addContribuableIgnore(Contribuable ctb, String raison) {
		listeCtbsIgnores.add(new InfoCtbIgnore(ctb.getNumero(), raison));
	}
	private void addContribuableAvecDecisionACI(Contribuable ctb) {
		final List<DecisionAci> decisions = ctb.getDecisionsSorted().stream()
				.filter(d->isDecisionMatchWithPeriode(d,periodeFiscale))
				.collect(Collectors.toList());
		for (DecisionAci d : decisions) {
			listeCtbDecisionACI.add(new InfoCtbAvecDecisionACI(ctb.getNumero(),
					d.getNumeroOfsAutoriteFiscale(),d.getDateDebut(),d.getDateFin(),d.getLogCreationDate()));
		}
	}

	protected void addPeriodeImposition(InfoPeriodeImposition periode) {
		listePeriode.add(periode);
	}

	@Override
	public void addTiersEnErreur(Tiers tiers) {
		// rien à faire, le tiers sera dans la liste des erreurs, c'est tout
	}

	@Override
	public void addAll(ExtractionDonneesRptPMResults sources) {
		super.addAll(sources);
		listeCtbsIgnores.addAll(sources.listeCtbsIgnores);
		listePeriode.addAll(sources.listePeriode);
		nbContribuablesAnalyses += sources.nbContribuablesAnalyses;
		listeCtbDecisionACI.addAll(sources.listeCtbDecisionACI);
	}

	@Override
	public void sort() {
		super.sort();
		Collections.sort(listePeriode);
		Collections.sort(listeCtbsIgnores);
		Collections.sort(listeCtbDecisionACI);
	}

	public int getNombreCtbAnalyses() {
		return nbContribuablesAnalyses;
	}

	public List<InfoPeriodeImposition> getListePeriode() {
		return listePeriode;
	}

	public List<InfoCtbIgnore> getListeCtbsIgnores() {
		return listeCtbsIgnores;
	}

	public List<InfoCtbAvecDecisionACI> getListeCtbDecisionACI() {
		return listeCtbDecisionACI;
	}

	/**
	 * @param noOfs         numéro OFS de l'entité
	 * @param taf           type d'autorité fiscale (= type de l'entité)
	 * @param dateReference date de validité du numéro OFS donné
	 * @return Le numéro OFS de la commune vaudoise principale, ou <code>null</code> s'il s'agit d'une commune HC ou d'un pays étranger
	 */
	@Nullable
	protected Integer getNumeroOfsCommuneVaudoise(int noOfs, TypeAutoriteFiscale taf, RegDate dateReference) {

		// hors du territoire cantonal -> vite vu !
		if (taf != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			return null;
		}

		final Commune commune = infraService.getCommuneByNumeroOfs(noOfs, dateReference);
		if (commune != null && commune.isFraction()) {
			final Commune faitiere = infraService.getCommuneFaitiere(commune, dateReference);
			return faitiere.getNoOFS();
		}
		else {
			return commune != null && commune.isVaudoise() ? commune.getNoOFS() : null;
		}


	}




}

