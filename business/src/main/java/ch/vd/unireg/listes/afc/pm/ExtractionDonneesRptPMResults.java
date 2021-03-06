package ch.vd.unireg.listes.afc.pm;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.FiscalDateHelper;
import ch.vd.unireg.common.ListesResults;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.AllegementFiscalCanton;
import ch.vd.unireg.tiers.AllegementFiscalConfederation;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DecisionAci;
import ch.vd.unireg.tiers.DomicileHisto;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.FlagEntreprise;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.ForFiscalRevenuFortune;
import ch.vd.unireg.tiers.FormeLegaleHisto;
import ch.vd.unireg.tiers.LocalizedDateRange;
import ch.vd.unireg.tiers.RaisonSocialeHisto;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEtatEntreprise;

public class ExtractionDonneesRptPMResults extends ListesResults<ExtractionDonneesRptPMResults> {

	public static final String NON_ASSUJETTI = "Non assujetti sur la période fiscale";

	public final int periodeFiscale;
	public final ModeExtraction mode;
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

	public static class InfoCtbAvecDecisionACI extends InfoCtbBase<InfoCtbAvecDecisionACI> {
		public final int numeroOfs;
		public final RegDate dateDebut;
		public final RegDate dateFin;
		public final Date dateCreation;
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

	public static class InfoCtbIgnore extends InfoCtbBase<InfoCtbIgnore> {
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
		public final TypeAutoriteFiscale autoriteFiscalePrincipale;
		public final TypeEtatEntreprise etatEntreprise;
		public final TypeAutoriteFiscale typeForPrincipal;

		public InfoPeriodeImposition(Entreprise entreprise, PeriodeImpositionPersonnesMorales periode, TiersService tiersService, ServiceInfrastructureService infraService) {
			super(entreprise.getNumero());

			this.formeJuridique = getFormeJuridique(entreprise, periode.getDateFin(), tiersService);

			final ForFiscalRevenuFortune forPreponderant = findForVaudoisDeReference(entreprise, periode.getDateFin());
			if (forPreponderant == null) {
				this.noOfsCommune = null;
				this.nomCommune = null;

				this.motifRattachement = null;
				this.motifOuverture = null;
				this.motifFermeture = null;
			}
			else {
				// le for est vaudois, il pointe donc nécessairement, par construction, vers une commune
				this.noOfsCommune = forPreponderant.getNumeroOfsAutoriteFiscale();
				this.nomCommune = getNomAutorite(forPreponderant, infraService);

				this.motifRattachement = forPreponderant.getMotifRattachement();
				this.motifOuverture = forPreponderant.getMotifOuverture();
				this.motifFermeture = forPreponderant.getMotifFermeture();      // TODO faut-il vraiment indiquer cette valeur même si le for est fermé bien après l'année de la RPT ?
			}

			this.joursImposables = FiscalDateHelper.getLongueurEnJours(periode.getDateDebut(), periode.getDateFin());
			this.raisonSociale = getRaisonSociale(entreprise, periode.getDateFin(), tiersService);
			this.regimeFiscalCode = getRegimeFiscal(entreprise, periode.getDateFin());

			final AllegementFiscalConfederation allegementFiscalIFD = getAllegementFiscalIFD(entreprise, periode.getDateFin());
			this.typeAllegementFiscalIFD = allegementFiscalIFD != null ? allegementFiscalIFD.getType() : null;
			this.debutAllegementIFD = allegementFiscalIFD != null ? allegementFiscalIFD.getDateDebut() : null;
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
				this.nomSiegeCivil = getNomAutorite(siege, infraService);
			}
			else {
				this.noOfSiegeCivil = 0;
				this.nomSiegeCivil = null;
			}
			this.debutExerciceCommercial = periode.getExerciceCommercial().getDateDebut();
			this.finExerciceCommercial = periode.getExerciceCommercial().getDateFin();
			this.specifiteFiscale = getSpecialite(entreprise, periode.getDateFin());
			this.noIDE = tiersService.getNumeroIDE(entreprise);
			this.dateFondation = getDateCreation(entreprise);
			this.dateDebutPI = periode.getDateDebut();
			this.datefinPI = periode.getDateFin();

			final ForFiscalPrincipalPM forFiscal = entreprise.getDernierForFiscalPrincipalAvant(periode.getDateFin());
			if (forFiscal != null) {
				this.noOfsForPrincipal = forFiscal.getNumeroOfsAutoriteFiscale();
				this.typeForPrincipal = forFiscal.getTypeAutoriteFiscale();
			}
			else {
				this.noOfsForPrincipal = null;
				this.typeForPrincipal = null;
			}
			this.nomForPrincipal = getNomAutorite(forFiscal, infraService);
			this.autoriteFiscalePrincipale = periode.getTypeAutoriteFiscalePrincipale();

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
				"REG_EtatFiscal",
				"REG_typeForPri"
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
					asString(autoriteFiscalePrincipale),
					versionWS.of(etatEntreprise),
					asString(typeForPrincipal),
			};
		}

		private static FlagEntreprise getSpecialite(Entreprise entreprise, RegDate date){
			final List<FlagEntreprise> liste = entreprise.getFlagsNonAnnulesTries();
			return DateRangeHelper.rangeAt(liste, date);
		}

		private static String getNomAutorite(LocalizedDateRange localisation, ServiceInfrastructureService infraService) {
			if (localisation == null) {
				return null;
			}
			final String nom;
			final Integer numeroOfs = localisation.getNumeroOfsAutoriteFiscale();
			if (localisation.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
				final Pays pays = infraService.getPays(numeroOfs, localisation.getDateFin());
				nom = pays != null ? pays.getNomCourt() : null;
			}
			else {
				// on est en suisse
				final Commune commune = infraService.getCommuneByNumeroOfs(numeroOfs, localisation.getDateFin());
				nom = commune != null ? commune.getNomOfficiel() : null;
			}
			return nom;
		}

		@Nullable
		private static ForFiscalRevenuFortune findForVaudoisDeReference(Entreprise entreprise, RegDate date) {
			// liste des fors vaudois existant ou ayant existé avant (ou à) la date de référence
			final List<ForFiscalRevenuFortune> forsVaudois = entreprise.getForsFiscauxNonAnnules(false).stream()
					.filter(ff -> ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD)
					.filter(ff -> ff.getDateDebut().isBeforeOrEqual(date))
					.filter(ForFiscalRevenuFortune.class::isInstance)
					.map(ForFiscalRevenuFortune.class::cast)
					.collect(Collectors.toList());
			if (forsVaudois.isEmpty()) {
				// je me demande bien ce que l'on fait ici (avec une période d'imposition, et tout, sans for vaudois... mais bon...
				return null;
			}

			// dernière date de validité à regarder
			final RegDate finDernierFor = forsVaudois.stream()
					.max(Comparator.comparing(ForFiscal::getDateFin, NullDateBehavior.LATEST::compare))
					.map(ForFiscal::getDateFin)
					.orElse(null);      // null signifie que le dernier for est toujours actif
			final RegDate effectiveDate = RegDateHelper.minimum(date, finDernierFor, NullDateBehavior.LATEST);

			// fors vaudois valides à cette date effective
			final List<ForFiscalRevenuFortune> forsADateEffective = forsVaudois.stream()
					.filter(ff -> ff.isValidAt(effectiveDate))
					.collect(Collectors.toList());

			// y a-t-il un for principal vaudois (s'il y en a un, c'est lui...) ?
			final Optional<ForFiscalRevenuFortune> forPrincipalVaudois = forsADateEffective.stream()
					.filter(ForFiscal::isPrincipal)
					.findFirst();
			// si le for prépondérant n'est pas le for principal, il faut bien le choisir
			// - le plus vieux encore actif
			// - à date de début égale, celui qui a le numéro OFS le plus petit
			return forPrincipalVaudois.orElseGet(() -> forsADateEffective.stream()
					.min(Comparator.comparing(ForFiscal::getDateDebut, NullDateBehavior.EARLIEST::compare).thenComparing(ForFiscal::getNumeroOfsAutoriteFiscale))
					.orElse(null));
		}

		private static String getRaisonSociale(Entreprise entreprise, RegDate date,  TiersService tiersService) {
			final List<RaisonSocialeHisto> liste = tiersService.getRaisonsSociales(entreprise, false);
			final RaisonSocialeHisto raisonSocialeHisto = DateRangeHelper.rangeAt(liste, date);
			if (raisonSocialeHisto != null) {
				return raisonSocialeHisto.getRaisonSociale();
			}
			return null;
		}

		private static String getRegimeFiscal(Entreprise entreprise, RegDate date) {
			final List<RegimeFiscal> liste = entreprise.getRegimesFiscauxNonAnnulesTries(RegimeFiscal.Portee.VD);
			final RegimeFiscal regimeFiscal = DateRangeHelper.rangeAt(liste, date);
			if (regimeFiscal!= null) {
				return regimeFiscal.getCode();
			}

			return null;
		}

		private static String getValeurTauxOuMontant(AllegementFiscal allegementFiscal){
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

		private static FormeJuridiqueEntreprise getFormeJuridique(Entreprise entreprise, RegDate date, TiersService tiersService) {
			final List<FormeLegaleHisto> formesLegales = tiersService.getFormesLegales(entreprise, false);
			final FormeLegaleHisto formeLegaleHisto = DateRangeHelper.rangeAt(formesLegales, date);
			if (formeLegaleHisto != null) {
				return FormeJuridiqueEntreprise.fromCode(formeLegaleHisto.getFormeLegale().getCode());
			}
			return  null;
		}

		private static AllegementFiscalConfederation getAllegementFiscalIFD(Entreprise entreprise, RegDate date) {
			final List<AllegementFiscal> liste = entreprise.getAllegementsFiscauxNonAnnulesTries();
			return liste.stream()
					.filter(AllegementFiscalConfederation.class::isInstance)
					.map(AllegementFiscalConfederation.class::cast)
					.filter(a -> a.isValidAt(date))
					.findFirst()
					.orElse(null);
		}

		private static AllegementFiscalCanton getAllegementFiscalVD(Entreprise entreprise, RegDate date) {
			final List<AllegementFiscal> liste = entreprise.getAllegementsFiscauxNonAnnulesTries();
			return liste.stream()
					.filter(AllegementFiscalCanton.class::isInstance)
					.map(AllegementFiscalCanton.class::cast)
					.filter(a -> a.isValidAt(date))
					.findFirst()
					.orElse(null);
		}

		private static DomicileHisto getSiege(Entreprise entreprise, RegDate date,TiersService tiersService) {
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

	@Nullable
	private static String asString(TypeAutoriteFiscale taf) {
		if (taf == null) {
			return null;
		}
		switch (taf) {
		case COMMUNE_OU_FRACTION_VD:
			return "VD";
		case COMMUNE_HC:
			return "HC";
		case PAYS_HS:
			return "HS";
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnu : " + taf);
		}
	}

	public ExtractionDonneesRptPMResults(RegDate dateTraitement, int periodeFiscale, ModeExtraction mode, VersionWS versionWS, int nbThreads, TiersService tiersService, ServiceInfrastructureService infraService,
	                                     PeriodeImpositionService periodeImpositionService, AdresseService adresseService) {
		super(dateTraitement, nbThreads, tiersService, adresseService);
		this.periodeFiscale = periodeFiscale;
		this.mode = mode;
		this.versionWS = versionWS;
		this.infraService = infraService;
		this.periodeImpositionService = periodeImpositionService;
	}

	private int nbContribuablesAnalyses = 0;
	private final List<InfoPeriodeImposition> listePeriodes = new LinkedList<>();
	private final List<InfoCtbIgnore> listeCtbsIgnores = new LinkedList<>();
	private final List<InfoCtbAvecDecisionACI> listeCtbsDecisionACI = new LinkedList<>();

	@Override
	public final void addContribuable(Contribuable ctb) throws InfrastructureException {
		++nbContribuablesAnalyses;
		final Entreprise entreprise = (Entreprise) ctb;
		//Verification de présence de décision ACI

		try {
			if (hasDecisionsACI(entreprise, periodeFiscale)) {
				addContribuableAvecDecisionACI(ctb);
			}
			
			final List<PeriodeImposition> periodes = periodeImpositionService.determine(entreprise);
			if (periodes == null) {
				addContribuableIgnore(ctb, "Pas de période d'imposition trouvée");
			}
			else {
				final List<InfoPeriodeImposition> periodesImpositionFiltres = periodes.stream()
						.map(PeriodeImpositionPersonnesMorales.class::cast)
						.filter(pipm -> pipm.getPeriodeFiscale() == periodeFiscale)
						.map(pipm -> new InfoPeriodeImposition(entreprise, pipm, tiersService, infraService))
						.collect(Collectors.toList());
				if (periodesImpositionFiltres.isEmpty()) {
					addContribuableIgnore(ctb, "Pas de période d'imposition trouvée");
				}
				else {
					periodesImpositionFiltres.forEach(this::addPeriodeImposition);
				}
			}
		}
		catch (AssujettissementException e) {
			addErrorException(ctb, e);
		}
	}

	private boolean hasDecisionsACI(Entreprise entreprise, int periodeFiscale) {
		return entreprise.getDecisionsSorted().stream().anyMatch(d -> isDecisionMatchWithPeriode(d, periodeFiscale));
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
		ctb.getDecisionsSorted().stream()
				.filter(d -> isDecisionMatchWithPeriode(d, periodeFiscale))
				.map(d -> new InfoCtbAvecDecisionACI(ctb.getNumero(),
				                                     d.getNumeroOfsAutoriteFiscale(),
				                                     d.getDateDebut(),
				                                     d.getDateFin(),
				                                     d.getLogCreationDate()))
				.forEach(listeCtbsDecisionACI::add);
	}

	protected void addPeriodeImposition(InfoPeriodeImposition periode) {
		listePeriodes.add(periode);
	}

	@Override
	public void addTiersEnErreur(Tiers tiers) {
		// rien à faire, le tiers sera dans la liste des erreurs, c'est tout
	}

	@Override
	public void addAll(ExtractionDonneesRptPMResults sources) {
		super.addAll(sources);
		listeCtbsIgnores.addAll(sources.listeCtbsIgnores);
		listePeriodes.addAll(sources.listePeriodes);
		nbContribuablesAnalyses += sources.nbContribuablesAnalyses;
		listeCtbsDecisionACI.addAll(sources.listeCtbsDecisionACI);
	}

	@Override
	public void sort() {
		super.sort();
		Collections.sort(listePeriodes);
		Collections.sort(listeCtbsIgnores);
		Collections.sort(listeCtbsDecisionACI);
	}

	public int getNombreCtbAnalyses() {
		return nbContribuablesAnalyses;
	}

	public List<InfoPeriodeImposition> getListePeriodes() {
		return listePeriodes;
	}

	public List<InfoCtbIgnore> getListeCtbsIgnores() {
		return listeCtbsIgnores;
	}

	public List<InfoCtbAvecDecisionACI> getListeCtbsDecisionACI() {
		return listeCtbsDecisionACI;
	}
}

