package ch.vd.uniregctb.tiers.view;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalAvecMotifs;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ForFiscalView implements Comparable<ForFiscalView>, DateRange, Annulable {

	private Long id;

	private GenreImpot genreImpot;

	private MotifRattachement motifRattachement;

	private TypeAutoriteFiscale typeAutoriteFiscale;

	private Integer numeroForFiscalCommune;

	private Integer numeroForFiscalCommuneHorsCanton;

	private Integer numeroForFiscalPays;

	private RegDate dateOuverture;

	private RegDate dateFermeture;

	private RegDate dateEvenement;

	private Long numeroCtb;

	private ModeImposition modeImposition;

	private RegDate dateChangement;

	private MotifFor motifOuverture;

	private MotifFor motifFermeture;

	private MotifFor motifImposition;

	private String libFractionCommune;

	private String libCommuneHorsCanton;

	private String libPays;

	private boolean annule;
	private String natureForFiscal;

	private NatureTiers natureTiers;

	private final boolean isPrincipalActif;

	private boolean dernierForPrincipalOuDebiteur;

	private boolean changementModeImposition;

	private Boolean forGestion;

	private boolean dateOuvertureEditable = true;

	private boolean dateFermetureEditable = true;

	private final boolean principal;

	private final boolean secondaire;

	/**
	 * Construit la vue des fors fiscaux d'un contribuable.
	 *
	 * @param tiers              un contribuable
	 * @param dernierForGestionProvider une fonction qui retourne le dernier for de gestion
	 * @return la vue des fors fiscaux.
	 */
	@NotNull
	public static List<ForFiscalView> getList(@NotNull Tiers tiers, @Nullable BiFunction<Contribuable, RegDate, ForGestion> dernierForGestionProvider) {

		final List<ForFiscal> forsFiscauxSorted = tiers.getForsFiscauxSorted();
		if (forsFiscauxSorted == null || forsFiscauxSorted.isEmpty()) {
			return Collections.emptyList();
		}

		final ForGestion forGestion;
		final ForFiscal forPrincipalActif;
		final ForFiscal dernierForPrincipalOuDebiteur;
		final Comparator<ForFiscalView> comparator;

		if (tiers instanceof DebiteurPrestationImposable) {
			forGestion = null;
			forPrincipalActif = null;
			dernierForPrincipalOuDebiteur = tiers.getDernierForDebiteur();
			comparator = new ForDebiteurViewComparator();
		}
		else {
			forGestion = (dernierForGestionProvider == null ? null : dernierForGestionProvider.apply((Contribuable) tiers, null));
			forPrincipalActif = tiers.getForFiscalPrincipalAt(null);
			dernierForPrincipalOuDebiteur = tiers.getDernierForFiscalPrincipal();
			comparator = new ForFiscalViewComparator();
		}

		return forsFiscauxSorted.stream()
				.map(f -> get(f, forGestion, forPrincipalActif, dernierForPrincipalOuDebiteur))
				.sorted(comparator)
				.collect(Collectors.toList());
	}

	private static ForFiscalView get(@NotNull ForFiscal forFiscal, @Nullable ForGestion forGestion, @Nullable ForFiscal forPrincipalActif, @Nullable ForFiscal dernierForPrincipalOuDebiteur) {
		final boolean isForGestion = forGestion != null && forGestion.getSousjacent() == forFiscal;
		final boolean isPrincipalActif = (forPrincipalActif == forFiscal);
		final boolean isDernierForPrincipalOuDebiteur = (dernierForPrincipalOuDebiteur == forFiscal);

		return new ForFiscalView(forFiscal, isForGestion, isPrincipalActif, isDernierForPrincipalOuDebiteur);
	}

	public ForFiscalView(ForFiscal forFiscal, boolean isForGestion, boolean isPrincipalActif, boolean dernierForPrincipalOuDebiteur) {
		this.id = forFiscal.getId();
		this.numeroCtb = forFiscal.getTiers().getNumero();
		this.genreImpot = forFiscal.getGenreImpot();
		this.annule = forFiscal.isAnnule();

		if (forFiscal instanceof ForFiscalAutreImpot) {
			this.dateEvenement = forFiscal.getDateDebut();
		}
		else {
			this.dateOuverture = forFiscal.getDateDebut();
			this.dateFermeture = forFiscal.getDateFin();

			if (forFiscal instanceof ForFiscalSecondaire) {
				this.dateOuvertureEditable = true;
				this.dateFermetureEditable = true;
			}
			else {
				this.dateOuvertureEditable = dateOuverture == null;
				this.dateFermetureEditable = dateFermeture == null || !RegDateHelper.isBeforeOrEqual(dateFermeture, RegDate.get(), NullDateBehavior.LATEST);
			}
		}

		setTypeEtNumeroForFiscal(forFiscal.getTypeAutoriteFiscale(), forFiscal.getNumeroOfsAutoriteFiscale());

		if (forFiscal instanceof ForFiscalAvecMotifs) {
			final ForFiscalAvecMotifs ffam = (ForFiscalAvecMotifs) forFiscal;
			this.motifOuverture = ffam.getMotifOuverture();
			this.motifFermeture = ffam.getMotifFermeture();
		}

		if (forFiscal instanceof ForFiscalRevenuFortune) {
			final ForFiscalRevenuFortune forFiscalRevenuFortune = (ForFiscalRevenuFortune) forFiscal;
			this.motifRattachement = forFiscalRevenuFortune.getMotifRattachement();
			this.forGestion = isForGestion;
		}

		if (forFiscal instanceof ForFiscalPrincipalPP) {
			final ForFiscalPrincipalPP forFiscalPrincipal = (ForFiscalPrincipalPP) forFiscal;
			this.modeImposition = forFiscalPrincipal.getModeImposition();
		}

		this.isPrincipalActif = isPrincipalActif;
		this.dernierForPrincipalOuDebiteur = dernierForPrincipalOuDebiteur;
		this.natureForFiscal = forFiscal.getClass().getSimpleName();

		this.principal = forFiscal.isPrincipal();
		this.secondaire = forFiscal instanceof ForFiscalSecondaire;
	}

	/**
	 * @return the genreImpot
	 */
	public GenreImpot getGenreImpot() {
		return genreImpot;
	}

	/**
	 * @param genreImpot the genreImpot to set
	 */
	public void setGenreImpot(GenreImpot genreImpot) {
		this.genreImpot = genreImpot;
	}

	/**
	 * @return the rattachement
	 */
	public MotifRattachement getMotifRattachement() {
		return motifRattachement;
	}

	/**
	 * @param rattachement the rattachement to set
	 */
	public void setMotifRattachement(MotifRattachement rattachement) {
		this.motifRattachement = rattachement;
	}

	/**
	 * @return the typeForFiscal
	 */
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	/**
	 * @param typeAutoriteFiscale the typeAutoriteFiscale to set
	 */
	public void setTypeAutoriteFiscale(TypeAutoriteFiscale typeAutoriteFiscale) {
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}

	/**
	 * @return le numéro d'autorité fiscale
	 */
	public Integer getNumeroAutoriteFiscale() {
		switch (typeAutoriteFiscale) {
		case COMMUNE_OU_FRACTION_VD:
			return numeroForFiscalCommune;
		case COMMUNE_HC:
			return numeroForFiscalCommuneHorsCanton;
		case PAYS_HS:
			return numeroForFiscalPays;
		default:
			throw new IllegalArgumentException("Type d'autorité fiscale inconnu =[" + typeAutoriteFiscale + ']');
		}
	}

	/**
	 * @return the numeroForFiscalCommune
	 */
	public Integer getNumeroForFiscalCommune() {
		return numeroForFiscalCommune;
	}

	/**
	 * @param numeroForFiscalCommune the numeroForFiscalCommune to set
	 */
	public void setNumeroForFiscalCommune(Integer numeroForFiscalCommune) {
		this.numeroForFiscalCommune = numeroForFiscalCommune;
	}

	/**
	 * @return the numeroForFiscalCommuneHorsCanton
	 */
	public Integer getNumeroForFiscalCommuneHorsCanton() {
		return numeroForFiscalCommuneHorsCanton;
	}

	/**
	 * @param numeroForFiscalCommuneHorsCanton the numeroForFiscalCommuneHorsCanton to set
	 */
	public void setNumeroForFiscalCommuneHorsCanton(
			Integer numeroForFiscalCommuneHorsCanton) {
		this.numeroForFiscalCommuneHorsCanton = numeroForFiscalCommuneHorsCanton;
	}

	/**
	 * @return the numeroForFiscalPays
	 */
	public Integer getNumeroForFiscalPays() {
		return numeroForFiscalPays;
	}

	/**
	 * @param numeroForFiscalPays the numeroForFiscalPays to set
	 */
	public void setNumeroForFiscalPays(Integer numeroForFiscalPays) {
		this.numeroForFiscalPays = numeroForFiscalPays;
	}

	public void setTypeEtNumeroForFiscal(TypeAutoriteFiscale taf, int noOfs) {
		this.typeAutoriteFiscale = taf;
		switch (taf) {
		case COMMUNE_OU_FRACTION_VD:
			this.numeroForFiscalCommune = noOfs;
			break;
		case COMMUNE_HC:
			this.numeroForFiscalCommuneHorsCanton = noOfs;
			break;
		case PAYS_HS:
			this.numeroForFiscalPays = noOfs;
			break;
		}
	}

	/**
	 * @return the dateOuverture
	 */
	public RegDate getRegDateOuverture() {
		return dateOuverture;
	}

	/**
	 * @param dateOuverture the dateOuverture to set
	 */
	public void setDateOuverture(RegDate dateOuverture) {
		this.dateOuverture = dateOuverture;
	}

	/**
	 * @return the dateFermeture
	 */
	public RegDate getRegDateFermeture() {
		return dateFermeture;
	}

	/**
	 * @param dateFermeture the dateFermeture to set
	 */
	public void setDateFermeture(RegDate dateFermeture) {
		this.dateFermeture = dateFermeture;
	}

	/**
	 * @return the dateEvenement
	 */
	public RegDate getRegDateEvenement() {
		return dateEvenement;
	}

	/**
	 * @param dateEvenement the dateEvenement to set
	 */
	public void setDateEvenement(RegDate dateEvenement) {
		this.dateEvenement = dateEvenement;
	}

	/**
	 * @return the dateOuverture
	 */
	public Date getDateOuverture() {
		return "ForFiscalAutreImpot".equals(getNatureForFiscal()) ?
				RegDate.asJavaDate(dateEvenement) :
				RegDate.asJavaDate(dateOuverture);
	}

	/**
	 * @param dateOuverture the dateOuverture to set
	 */
	public void setDateOuverture(Date dateOuverture) {
		this.dateOuverture = RegDateHelper.get(dateOuverture);
	}

	/**
	 * @return the dateFermeture
	 */
	public Date getDateFermeture() {
		return "ForFiscalAutreImpot".equals(getNatureForFiscal()) ?
				RegDate.asJavaDate(dateEvenement) :
				RegDate.asJavaDate(dateFermeture);
	}

	/**
	 * @param dateFermeture the dateFermeture to set
	 */
	public void setDateFermeture(Date dateFermeture) {
		this.dateFermeture = RegDateHelper.get(dateFermeture);
	}

	/**
	 * @return the dateEvenement
	 */
	public Date getDateEvenement() {
		return RegDate.asJavaDate(dateEvenement);
	}

	/**
	 * @param dateEvenement the dateEvenement to set
	 */
	public void setDateEvenement(Date dateEvenement) {
		this.dateEvenement = RegDateHelper.get(dateEvenement);
	}

	public ModeImposition getModeImposition() {
		return modeImposition;
	}

	public void setModeImposition(ModeImposition mode) {
		this.modeImposition = mode;
	}

	public Date getDateChangement() {
		return RegDate.asJavaDate(dateChangement);
	}

	public RegDate getRegDateChangement() {
		return dateChangement;
	}

	public void setDateChangement(Date dateChangement) {
		this.dateChangement = RegDateHelper.get(dateChangement);
	}


	public void setDateChangement(RegDate dateChangement) {
		this.dateChangement = dateChangement;
	}

	public Long getNumeroCtb() {
		return numeroCtb;
	}

	public void setNumeroCtb(Long numeroCtb) {
		this.numeroCtb = numeroCtb;
	}

	public MotifFor getMotifOuverture() {
		return motifOuverture;
	}

	public void setMotifOuverture(MotifFor motifOuverture) {
		this.motifOuverture = motifOuverture;
	}

	public MotifFor getMotifFermeture() {
		return motifFermeture;
	}

	public void setMotifFermeture(MotifFor motifFermeture) {
		this.motifFermeture = motifFermeture;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLibFractionCommune() {
		return libFractionCommune;
	}

	public void setLibFractionCommune(String libFractionCommune) {
		this.libFractionCommune = libFractionCommune;
	}

	public String getLibCommuneHorsCanton() {
		return libCommuneHorsCanton;
	}

	public void setLibCommuneHorsCanton(String libCommuneHorsCanton) {
		this.libCommuneHorsCanton = libCommuneHorsCanton;
	}

	public String getLibPays() {
		return libPays;
	}

	/**
	 * Compare d'apres la date de ForFiscalView
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ForFiscalView forFiscalView) {
		Date dateDebut = getDateOuverture();
		if (dateDebut == null) {
			dateDebut = getDateEvenement();
		}
		Date autreDateDebut = forFiscalView.getDateOuverture();
		if (autreDateDebut == null) {
			autreDateDebut = getDateEvenement();
		}
		int value = -dateDebut.compareTo(autreDateDebut);
		return value;
	}

	public void setLibPays(String libPays) {
		this.libPays = libPays;
	}

	public String getNatureForFiscal() {
		return natureForFiscal;
	}

	public void setNatureForFiscal(String natureForFiscal) {
		this.natureForFiscal = natureForFiscal;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public NatureTiers getNatureTiers() {
		return natureTiers;
	}

	public void setNatureTiers(NatureTiers natureTiers) {
		this.natureTiers = natureTiers;
	}

	public boolean isPrincipalActif() {
		return isPrincipalActif;
	}

	public boolean isDernierForPrincipalOuDebiteur() {
		return dernierForPrincipalOuDebiteur;
	}

	public void setDernierForPrincipalOuDebiteur(boolean dernierForPrincipalOuDebiteur) {
		this.dernierForPrincipalOuDebiteur = dernierForPrincipalOuDebiteur;
	}

	public boolean isChangementModeImposition() {
		return changementModeImposition;
	}

	public void setChangementModeImposition(boolean changementModeImposition) {
		this.changementModeImposition = changementModeImposition;
	}

	public Boolean getForGestion() {
		return forGestion;
	}

	public void setForGestion(Boolean forGestion) {
		this.forGestion = forGestion;
	}

	public MotifFor getMotifImposition() {
		return motifImposition;
	}

	public void setMotifImposition(MotifFor motifImposition) {
		this.motifImposition = motifImposition;
	}

	public boolean isDateOuvertureEditable() {
		return dateOuvertureEditable;
	}

	public boolean isDateFermetureEditable() {
		return dateFermetureEditable;
	}

	@Override
	public RegDate getDateDebut() {
		return dateOuverture;
	}

	@Override
	public RegDate getDateFin() {
		return dateFermeture;
	}

	public boolean isPrincipal() {
		return principal;
	}

	public boolean isSecondaire() {
		return secondaire;
	}
}
