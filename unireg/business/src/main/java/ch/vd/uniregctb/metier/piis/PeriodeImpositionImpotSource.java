package ch.vd.uniregctb.metier.piis;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class PeriodeImpositionImpotSource implements CollatableDateRange, Duplicable<PeriodeImpositionImpotSource> {

	private static final String HS = "HS";
	private static final String INCONNU_HC = "HC";
	private static final String INCONNU = "INCONNU";

	public static enum Type {
		MIXTE,
		SOURCE
	}

	private final PersonnePhysique pp;
	private final Type type;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final TypeAutoriteFiscale typeAutoriteFiscale;
	private final Integer noOfs;
	private final String cleLocalisation;

	private final MotifFor motifOuverture;
	private final MotifFor motifFermeture;
	private final ModeImposition modeImpositionDebut;
	private final ModeImposition modeImpositionFin;

	/**
	 * Constructeur utilisé lors du {@link #collate(ch.vd.registre.base.date.DateRange)}
	 * @param courant une période
	 * @param suivant la période suivante, qui doit fusionner avec la première
	 */
	private PeriodeImpositionImpotSource(PeriodeImpositionImpotSource courant, PeriodeImpositionImpotSource suivant) {
		if (!courant.isCollatable(suivant)) {
			throw new IllegalArgumentException();
		}
		this.pp = courant.pp;
		this.type = suivant.type;
		this.typeAutoriteFiscale = suivant.typeAutoriteFiscale;
		this.noOfs = suivant.noOfs;
		this.cleLocalisation = suivant.cleLocalisation;
		this.dateDebut = courant.dateDebut;
		this.dateFin = suivant.dateFin;
		this.motifOuverture = courant.motifOuverture;
		this.motifFermeture = suivant.motifFermeture;
		this.modeImpositionDebut = courant.modeImpositionDebut;
		this.modeImpositionFin = suivant.modeImpositionFin;
		DateRangeHelper.assertValidRange(this.dateDebut, this.dateFin);
	}

	private PeriodeImpositionImpotSource(PeriodeImpositionImpotSource src) {
		this.pp = src.pp;
		this.type = src.type;
		this.typeAutoriteFiscale = src.typeAutoriteFiscale;
		this.noOfs = src.noOfs;
		this.cleLocalisation = src.cleLocalisation;
		this.dateDebut = src.dateDebut;
		this.dateFin = src.dateFin;
		this.motifOuverture = src.motifOuverture;
		this.motifFermeture = src.motifFermeture;
		this.modeImpositionDebut = src.modeImpositionDebut;
		this.modeImpositionFin = src.modeImpositionFin;
	}

	private PeriodeImpositionImpotSource(PersonnePhysique pp, Type type, RegDate dateDebut, RegDate dateFin,
	                                     @Nullable TypeAutoriteFiscale typeAutoriteFiscale, @Nullable Integer noOfs, @Nullable String cleLocalisation,
	                                     @Nullable MotifFor motifOuverture, @Nullable MotifFor motifFermeture,
	                                     @Nullable ModeImposition modeImpositionDebut, @Nullable ModeImposition modeImpositionFin) {
		this.pp = pp;
		this.type = type;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.noOfs = noOfs;
		this.cleLocalisation = cleLocalisation;
		this.motifOuverture = motifOuverture;
		this.motifFermeture = motifFermeture;
		this.modeImpositionDebut = modeImpositionDebut;
		this.modeImpositionFin = modeImpositionFin;
		DateRangeHelper.assertValidRange(this.dateDebut, this.dateFin);
	}

	public PeriodeImpositionImpotSource(PeriodeImpositionImpotSource src, RegDate dateDebut, RegDate dateFin) {
		this(src.pp, src.type, dateDebut, dateFin, src.typeAutoriteFiscale, src.noOfs, src.cleLocalisation, src.motifOuverture, src.motifFermeture, src.modeImpositionDebut, src.modeImpositionFin);
	}

	public PeriodeImpositionImpotSource(PersonnePhysique pp, Type type, RegDate dateDebut, RegDate dateFin, @Nullable ForFiscalPrincipal ff, ServiceInfrastructureService infraService) {
		this(pp, type, dateDebut, dateFin,
		     ff != null ? ff.getTypeAutoriteFiscale() : null,
		     ff != null ? ff.getNumeroOfsAutoriteFiscale() : null,
		     buildCleLocalisation(ff, infraService),
		     ff != null ? ff.getMotifOuverture() : null,
		     ff != null ? ff.getMotifFermeture() : null,
		     ff != null ? ff.getModeImposition() : null,
		     ff != null ? ff.getModeImposition() : null);
	}

	/**
	 * Construction d'une clé de localisation différente pour chaque pays et chaque canton
	 * @param ff le for fiscal à analyser
	 * @param infraService le service infrastructure (nécessaire pour connaître le canton d'une commune donnée)
	 * @return une clé de localisation associée au for fiscal (s'il est <code>null</code>, la valeur {@link #INCONNU} sera renvoyée)
	 */
	@NotNull
	private static String buildCleLocalisation(@Nullable ForFiscalPrincipal ff, ServiceInfrastructureService infraService) {
		return ff != null ? buildCleLocalisation(ff.getTypeAutoriteFiscale(), ff.getNumeroOfsAutoriteFiscale(), ff.getDateDebut(), infraService) : INCONNU;
	}

	/**
	 * Construction d'une clé de localisation différente pour chaque pays et canton
	 * @param taf type d'autorité fiscale associée au numéro OFS
	 * @param noOfs numéro OFS de la commune/du pays (dépend du type d'autorité fiscale)
	 * @param dateReference date de référence
	 * @param infraService service infrastructure (nécessaire pour connaître le canton d'une commune donnée)
	 * @return une clé de localisation associée aux données indiquées
	 */
	@NotNull
	protected static String buildCleLocalisation(TypeAutoriteFiscale taf, int noOfs, RegDate dateReference, ServiceInfrastructureService infraService) {
		if (taf == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			return ServiceInfrastructureService.SIGLE_CANTON_VD;
		}
		else if (taf == TypeAutoriteFiscale.COMMUNE_HC) {
			final Commune commune = infraService.getCommuneByNumeroOfs(noOfs, dateReference);
			if (commune != null) {
				return commune.getSigleCanton();
			}
			else {
				return INCONNU_HC;
			}
		}
		else {
			return String.format("%s-%d", HS, noOfs);
		}
	}

	@Override
	public boolean isCollatable(DateRange next) {
		final PeriodeImpositionImpotSource other = (PeriodeImpositionImpotSource) next;

		// périodes fiscales différentes -> on ne colle jamais !
		if (dateDebut.year() != other.dateDebut.year()) {
			return false;
		}

		// changement de type d'autorité fiscale (ou conservation du même type en changeant de canton/pays) -> on ne colle pas
		if (!other.cleLocalisation.equals(cleLocalisation)) {
			return false;
		}

		// veuvage -> on ne colle pas !
		if (motifFermeture == MotifFor.VEUVAGE_DECES || other.motifOuverture == MotifFor.VEUVAGE_DECES) {
			return false;
		}

		// obtention d'un permis C / d'une nationalité avec passage de "source pure" à "ordinaire"
		if (motifFermeture == MotifFor.PERMIS_C_SUISSE || other.motifOuverture == MotifFor.PERMIS_C_SUISSE) {
			if (modeImpositionFin == ModeImposition.SOURCE && !other.modeImpositionDebut.isSource()) {
				return false;
			}
		}

		// mariage avec passage de "source pure" à "ordinaire" -> on ne colle pas
		if (motifFermeture == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION || other.motifOuverture == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION) {
			if (modeImpositionFin == ModeImposition.SOURCE && !other.modeImpositionDebut.isSource()) {
				return false;
			}
		}

		return DateRangeHelper.isCollatable(this, other);
	}

	@Override
	public DateRange collate(DateRange next) {
		return new PeriodeImpositionImpotSource(this, (PeriodeImpositionImpotSource) next);
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public PeriodeImpositionImpotSource duplicate() {
		return new PeriodeImpositionImpotSource(this);
	}

	public PersonnePhysique getContribuable() {
		return pp;
	}

	public Type getType() {
		return type;
	}

	@Nullable
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	@Nullable
	public Integer getNoOfs() {
		return noOfs;
	}

	public String getCleLocalisation() {
		return cleLocalisation;
	}
}
