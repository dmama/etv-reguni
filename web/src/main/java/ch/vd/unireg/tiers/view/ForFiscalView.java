package ch.vd.unireg.tiers.view;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalAutreImpot;
import ch.vd.unireg.tiers.ForFiscalAvecMotifs;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalRevenuFortune;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.ForGestion;
import ch.vd.unireg.tiers.NatureTiers;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

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

	private boolean annule;
	private String natureForFiscal;

	private NatureTiers natureTiers;

	private final boolean isPrincipalActif;

	private boolean dernierForPrincipalOuDebiteur;

	private Boolean forGestion;

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
		if (forsFiscauxSorted.isEmpty()) {
			return Collections.emptyList();
		}

		final ForGestion forGestion;
		final ForFiscal forPrincipalActif;
		final ForFiscal dernierForPrincipalOuDebiteur;
		final Comparator<ForFiscalView> comparator;

		if (tiers instanceof DebiteurPrestationImposable) {
			forGestion = null;
			forPrincipalActif = null;
			dernierForPrincipalOuDebiteur = ((DebiteurPrestationImposable) tiers).getDernierForDebiteur();
			comparator = new ForDebiteurViewComparator();
		}
		else if (tiers instanceof Contribuable) {
			final Contribuable ctb = (Contribuable) tiers;
			forGestion = (dernierForGestionProvider == null ? null : dernierForGestionProvider.apply(ctb, null));
			forPrincipalActif = ctb.getForFiscalPrincipalAt(null);
			dernierForPrincipalOuDebiteur = ctb.getDernierForFiscalPrincipal();
			comparator = new ForFiscalViewComparator();
		}
		else {
			return Collections.emptyList();
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

	public GenreImpot getGenreImpot() {
		return genreImpot;
	}

	public void setGenreImpot(GenreImpot genreImpot) {
		this.genreImpot = genreImpot;
	}

	public MotifRattachement getMotifRattachement() {
		return motifRattachement;
	}

	public void setMotifRattachement(MotifRattachement rattachement) {
		this.motifRattachement = rattachement;
	}

	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

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

	public Integer getNumeroForFiscalCommune() {
		return numeroForFiscalCommune;
	}

	public void setNumeroForFiscalCommune(Integer numeroForFiscalCommune) {
		this.numeroForFiscalCommune = numeroForFiscalCommune;
	}

	public Integer getNumeroForFiscalCommuneHorsCanton() {
		return numeroForFiscalCommuneHorsCanton;
	}

	public void setNumeroForFiscalCommuneHorsCanton(
			Integer numeroForFiscalCommuneHorsCanton) {
		this.numeroForFiscalCommuneHorsCanton = numeroForFiscalCommuneHorsCanton;
	}

	public Integer getNumeroForFiscalPays() {
		return numeroForFiscalPays;
	}

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

	public RegDate getRegDateOuverture() {
		return dateOuverture;
	}

	public void setDateOuverture(RegDate dateOuverture) {
		this.dateOuverture = dateOuverture;
	}

	public RegDate getRegDateFermeture() {
		return dateFermeture;
	}

	public void setDateFermeture(RegDate dateFermeture) {
		this.dateFermeture = dateFermeture;
	}

	public RegDate getRegDateEvenement() {
		return dateEvenement;
	}

	public void setDateEvenement(RegDate dateEvenement) {
		this.dateEvenement = dateEvenement;
	}

	public Date getDateOuverture() {
		return "ForFiscalAutreImpot".equals(getNatureForFiscal()) ?
				RegDate.asJavaDate(dateEvenement) :
				RegDate.asJavaDate(dateOuverture);
	}

	public void setDateOuverture(Date dateOuverture) {
		this.dateOuverture = RegDateHelper.get(dateOuverture);
	}

	public Date getDateFermeture() {
		return "ForFiscalAutreImpot".equals(getNatureForFiscal()) ?
				RegDate.asJavaDate(dateEvenement) :
				RegDate.asJavaDate(dateFermeture);
	}

	public void setDateFermeture(Date dateFermeture) {
		this.dateFermeture = RegDateHelper.get(dateFermeture);
	}

	public Date getDateEvenement() {
		return RegDate.asJavaDate(dateEvenement);
	}

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

	/**
	 * Compare d'apres la date de ForFiscalView
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(@NotNull ForFiscalView forFiscalView) {
		Date dateDebut = getDateOuverture();
		if (dateDebut == null) {
			dateDebut = getDateEvenement();
		}
		Date autreDateDebut = forFiscalView.getDateOuverture();
		if (autreDateDebut == null) {
			autreDateDebut = getDateEvenement();
		}
		return - dateDebut.compareTo(autreDateDebut);
	}

	public String getNatureForFiscal() {
		return natureForFiscal;
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

	public boolean isPrincipalActif() {
		return isPrincipalActif;
	}

	public boolean isDernierForPrincipalOuDebiteur() {
		return dernierForPrincipalOuDebiteur;
	}

	public Boolean getForGestion() {
		return forGestion;
	}

	public MotifFor getMotifImposition() {
		return motifImposition;
	}

	public void setMotifImposition(MotifFor motifImposition) {
		this.motifImposition = motifImposition;
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
