package ch.vd.uniregctb.listes.afc.pm;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.listes.afc.TypeExtractionDonneesRpt;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
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
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.FormeLegaleHisto;
import ch.vd.uniregctb.tiers.RaisonSocialeHisto;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

public class ExtractionDonneesRptPMResults extends ListesResults<ExtractionDonneesRptPMResults> {

	public static final String NON_ASSUJETTI = "Non assujetti sur la période fiscale";

	public final int periodeFiscale;
	public final VersionWS versionWS;

	private final ServiceInfrastructureService infraService;
	private final PeriodeImpositionService periodeImpositionService;

	public abstract static class InfoCtbBase<T extends InfoCtbBase> implements Comparable<T> {
		public final long noCtb;

		public InfoCtbBase(long noCtb) {
			this.noCtb = noCtb;
		}

		@Override
		public int compareTo(@NotNull T o) {
			return (int) (noCtb - o.noCtb);
		}

		public abstract String[] getNomsColonnes();

		public abstract Object[] getValeursColonnes(@NotNull VersionWS versionWS);
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
		public Object[] getValeursColonnes(@NotNull VersionWS versionWS) {
			return new Object[]{noCtb, numeroOfs, dateDebut, dateFin, dateCreation};
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
		public Object[] getValeursColonnes(@NotNull VersionWS versionWS) {
			return new Object[]{noCtb, raisonIgnore};
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

		public InfoPeriodeImposition(Entreprise entreprise, PeriodeImpositionPersonnesMorales periode, TiersService tiersService, ServiceInfrastructureService infraService) {
			super(entreprise.getNumero());

			this.formeJuridique = getFormeJuridique(entreprise, periode.getDateFin(), tiersService );
			final RolePopulationPMExtractor extractor = new RolePopulationPMExtractor(MotifFor.DEPART_HC);
			final ForFiscalRevenuFortune forTrouve = extractor.getForPourRoles(periode.getPeriodeFiscale(), entreprise);
			this.noOfsCommune = forTrouve!=null? extractor.getCommunePourRoles(periode.getPeriodeFiscale(), entreprise) : null;
			this.nomCommune = getNomAutoriteFor(forTrouve, periode, infraService);
			this.joursImposables = FiscalDateHelper.getLongueurEnJours(periode.getDateDebut(), periode.getDateFin());
			this.raisonSociale = getRaisonSociale(entreprise, periode.getDateFin(), tiersService);
			this.regimeFiscalCode = getRegimeFiscal(entreprise, periode.getDateFin());
			final AllegementFiscalConfederation allegementFiscalIFD = getAllegementFiscalIFD(entreprise, periode.getDateFin());
			this.typeAllegementFiscalIFD = allegementFiscalIFD != null ? allegementFiscalIFD.getType() : null;
			this.debutAllegementIFD =allegementFiscalIFD != null ? allegementFiscalIFD.getDateDebut() : null;
			this.finAllegementIFD = allegementFiscalIFD != null ? allegementFiscalIFD.getDateFin() : null;
			this.tauxOuMontantIFD = getValeurTauxOuMontant(allegementFiscalIFD);
			this.tauxAllegementIFD = allegementFiscalIFD != null ? allegementFiscalIFD.getPourcentageAllegement() : null;
			final AllegementFiscalCanton allegementFiscalVD = getAllegementFiscalVD(entreprise, periode.getDateFin());
			this.typeAllegementFiscalVD = allegementFiscalVD != null ? allegementFiscalVD.getType() : null;
			this.tauxOuMontantVD = getValeurTauxOuMontant(allegementFiscalVD);
			this.tauxAllegemenVD = allegementFiscalVD != null ? allegementFiscalVD.getPourcentageAllegement() : null;

			final DomicileHisto siege = getSiege(entreprise, periode.getDateFin(), tiersService);
			if (siege != null) {
				this.noOfSiegeCivil = siege.getNumeroOfsAutoriteFiscale();
				final Commune communeSiege = infraService.getCommuneByNumeroOfs(noOfSiegeCivil, null);
				this.nomSiegeCivil = communeSiege !=null ? communeSiege.getNomOfficiel() : null;
			}
			else {
				this.noOfSiegeCivil = 0;
				this.nomSiegeCivil = null;
			}
			this.debutExerciceCommercial = periode.getExerciceCommercial().getDateDebut();
			this.finExerciceCommercial = periode.getExerciceCommercial().getDateFin();
			this.specifiteFiscale = getSpecialite(entreprise,periode.getDateFin());
			this.noIDE = tiersService.getNumeroIDE(entreprise);
			this.dateFondation = getDateCreation(entreprise);
			this.dateDebutPI = periode.getDateDebut();
			this.datefinPI = periode.getDateFin();

			final ForFiscalPrincipalPM forFiscal = getDernierForFiscalPrincipalSurPeriode(entreprise, periode);
			if (forFiscal != null) {
				this.motifRattachement = forFiscal.getMotifRattachement();
				this.motifOuverture = forFiscal.getMotifOuverture();
				this.motifFermeture = forFiscal.getMotifFermeture();
				this.noOfsForPrincipal = forFiscal.getNumeroOfsAutoriteFiscale();
			}
			else {
				this.motifRattachement = null;
				this.motifOuverture = null;
				this.motifFermeture = null;
				this.noOfsForPrincipal = null;
			}
			this.nomForPrincipal = getNomAutoriteFor(forFiscal, periode, infraService);
			this.autoriteFiscaleForPrincipal = getTypeAutoriteFiscale(forFiscal);

			final EtatEntreprise etatEntreprise = entreprise.getEtatAt(periode.getDateFin());
			this.etatEntreprise = etatEntreprise != null ? etatEntreprise.getType() : null;
		}

		@Override
		public int compareTo(@NotNull InfoPeriodeImposition o) {
			int comparison = super.compareTo(o);
			if (comparison == 0) {
				// on compare ensuite la date de début de la période d'assujettissement
				comparison = NullDateBehavior.EARLIEST.compare(dateDebutPI, o.dateDebutPI);
			}
			return comparison;
		}

		private static final String[] NOMS_COLONNES = {
				"REG_FormeJuridique",
				"REG_NoCommune",
				"REG_Commune",
				"REG_NoCTB",
				"REG_JoursImposables",
				"REG_RaisonSociale",
				"REG_RegimeFiscal",
				"REG_TypeAllegementIFD",
				"REG_DateDebutAllegementIFD",
				"REG_DateFinAllegementIFD",
				"REG_TauxOuMontantIFD",
				"REG_TauxAllegementIFD",
				"REG_TypeAllegementVD",
				"REG_TauxOuMontantVD",
				"REG_TauxAllegementVD",
				"REG_SiegeCiviNolOFS",
				"REG_SiegeCivil",
				"REG_DebutExerciceCommercial",
				"REG_FinExerciceCommercial",
				"REG_SpecificiteFiscale",
				"REG_NoIDE",
				"REG_DateFondation",
				"REG_DateDebutPI",
				"REG_DateFinPI",
				"REG_MotifRattachement",
				"REG_MotifOuvertureFor",
				"REG_MotifFermetureFor",
				"REG_NoOFSForPri",
				"REG_NomForPri",
				"REG_AutoriteFiscalePrincipale",
				"REG_EtatFiscal"
		};

		@Override
		public String[] getNomsColonnes() {
			return NOMS_COLONNES;
		}

		@Override
		public Object[] getValeursColonnes(@NotNull VersionWS versionWS) {
			return new Object[] {
					versionWS.of(formeJuridique),
					noOfsCommune,
					nomCommune,
					noCtb,
					joursImposables,
					raisonSociale,
					regimeFiscalCode,
					typeAllegementFiscalIFD,
					debutAllegementIFD,
					finAllegementIFD,
					tauxOuMontantIFD,
					tauxAllegementIFD,
					typeAllegementFiscalVD,
					tauxOuMontantVD,
					tauxAllegemenVD,
					noOfSiegeCivil,
					nomSiegeCivil,
					debutExerciceCommercial,
					finExerciceCommercial,
					specifiteFiscale != null ? versionWS.of(specifiteFiscale.getType()) : null,
					noIDE,
					dateFondation,
					dateDebutPI,
					datefinPI,
					versionWS.of(motifRattachement),
					versionWS.of(motifOuverture),
					versionWS.of(motifFermeture),
					noOfsForPrincipal,
					nomForPrincipal,
					autoriteFiscaleForPrincipal,
					versionWS.of(etatEntreprise)
			};
		}

		protected FlagEntreprise getSpecialite(Entreprise entreprise, RegDate date){
			final List<FlagEntreprise> liste = entreprise.getFlagsNonAnnulesTries();
			return DateRangeHelper.rangeAt(liste, date);
		}

		protected String getNomAutoriteFor(ForFiscalRevenuFortune ffpm,PeriodeImpositionPersonnesMorales periode,ServiceInfrastructureService infraService){
			if (ffpm == null) {
				return null;
			}
			final String nom;
			final Integer numeroOfs = ffpm.getNumeroOfsAutoriteFiscale();
			if (ffpm.getTypeAutoriteFiscale()== TypeAutoriteFiscale.PAYS_HS) {
				final Pays pays = infraService.getPays(numeroOfs, periode.getDateFin());
				nom = pays != null ? pays.getNomOfficiel() : null;
			}
			else{
				//on est en suisse
				final Commune commune = infraService.getCommuneByNumeroOfs(numeroOfs, periode.getDateFin());
				nom = commune != null ? commune.getNomOfficiel() : null;
			}
			return nom;
		}

		protected String getRaisonSociale(Entreprise entreprise, RegDate date,  TiersService tiersService) {
			final List<RaisonSocialeHisto> liste = tiersService.getRaisonsSociales(entreprise, false);
			final RaisonSocialeHisto raisonSocialeHisto = DateRangeHelper.rangeAt(liste, date);
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
			return liste.stream()
					.filter(AllegementFiscalConfederation.class::isInstance)
					.map(AllegementFiscalConfederation.class::cast)
					.filter(a -> a.isValidAt(date))
					.findFirst()
					.orElse(null);
		}

		protected AllegementFiscalCanton getAllegementFiscalVD(Entreprise entreprise, RegDate date) {
			final List<AllegementFiscal> liste = entreprise.getAllegementsFiscauxNonAnnulesTries();
			return liste.stream()
					.filter(AllegementFiscalCanton.class::isInstance)
					.map(AllegementFiscalCanton.class::cast)
					.filter(a -> a.isValidAt(date))
					.findFirst()
					.orElse(null);
		}

		protected DomicileHisto getSiege(Entreprise entreprise, RegDate date,TiersService tiersService) {
			return tiersService.getSieges(entreprise,false).stream()
					.filter(a -> a.isValidAt(date))
					.findFirst()
					.orElse(null);
		}

		private static RegDate getDateCreation(Entreprise entreprise) {
			return entreprise.getEtatsNonAnnulesTries().stream()
					.findFirst()
					.map(EtatEntreprise::getDateObtention)
					.orElse(null);
		}

	}

	private static String getTypeAutoriteFiscale(ForFiscalPrincipalPM forFiscal) {
		if (forFiscal == null) {
			return null;
		}
		//CAS PARTICULIER du for fiscal avec départ HC qui a pour autorité fiscale VD,
		// on doit  considérer ces cas commes des autorité fiscales HC dans le cadre de la RPT (CF revue TAB technique dans SIFISC-25638)
		if (forFiscal.getDateFin() != null && forFiscal.getMotifFermeture() == MotifFor.DEPART_HC) {
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

	public ExtractionDonneesRptPMResults(RegDate dateTraitement, int periodeFiscale, VersionWS versionWS, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService,
	                                     PeriodeImpositionService periodeImpositionService, AdresseService adresseService) {
		super(dateTraitement, nbThreads, tiersService, adresseService);
		this.periodeFiscale = periodeFiscale;
		this.versionWS = versionWS;
		this.infraService = infraService;
		this.periodeImpositionService = periodeImpositionService;
	}

	public TypeExtractionDonneesRpt getMode() {
		return TypeExtractionDonneesRpt.IBC;
	}

	private int nbContribuablesAnalyses = 0;
	private final List<InfoPeriodeImposition> listePeriode = new LinkedList<>();
	private final List<InfoCtbIgnore> listeCtbsIgnores = new LinkedList<>();
	private final List<InfoCtbAvecDecisionACI> listeCtbDecisionACI = new LinkedList<>();

	protected static class ContribuableIgnoreException extends Exception {
		public ContribuableIgnoreException(String message) {
			super(message);
		}
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
					.map(PeriodeImpositionPersonnesMorales.class::cast)
					.filter(pipm -> pipm.getPeriodeFiscale() == periodeFiscale)
					.collect(Collectors.toList());
			if (CollectionUtils.isNotEmpty(periodesImpositionFiltres)) {
				for (PeriodeImpositionPersonnesMorales periode : periodesImpositionFiltres) {
					addPeriodeImposition(new InfoPeriodeImposition(entreprise, periode, tiersService, infraService));
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
			final Optional<DecisionAci> decisionAci = decisions.stream()
					.filter(d -> isDecisionMatchWithPeriode(d, periodeFiscale))
					.findFirst();
			return decisionAci.isPresent();
		}
		else {
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
		}
		else {
			//Decision aci qui fini sur la période N ou la période N-1
			if (d.getDateFin().year() == periodeFiscale || d.getDateFin().year() == periodeFiscale - 1) {
				return true;
			}
			else {
				return false;
			}

		}

		
	}

	protected void addContribuableIgnore(Contribuable ctb, String raison) {
		listeCtbsIgnores.add(new InfoCtbIgnore(ctb.getNumero(), raison));
	}

	private void addContribuableAvecDecisionACI(Contribuable ctb) {
		final List<DecisionAci> decisions = ctb.getDecisionsSorted().stream()
				.filter(d->isDecisionMatchWithPeriode(d, periodeFiscale))
				.collect(Collectors.toList());
		for (DecisionAci d : decisions) {
			listeCtbDecisionACI.add(new InfoCtbAvecDecisionACI(ctb.getNumero(),
			                                                   d.getNumeroOfsAutoriteFiscale(),
					                                           d.getDateDebut(),
					                                           d.getDateFin(),
					                                           d.getLogCreationDate()));
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
}

