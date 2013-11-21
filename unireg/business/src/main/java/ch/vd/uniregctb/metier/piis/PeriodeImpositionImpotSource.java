package ch.vd.uniregctb.metier.piis;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class PeriodeImpositionImpotSource implements CollatableDateRange, Duplicable<PeriodeImpositionImpotSource> {

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
		this.dateDebut = src.dateDebut;
		this.dateFin = src.dateFin;
		this.motifOuverture = src.motifOuverture;
		this.motifFermeture = src.motifFermeture;
		this.modeImpositionDebut = src.modeImpositionDebut;
		this.modeImpositionFin = src.modeImpositionFin;
	}

	private PeriodeImpositionImpotSource(PersonnePhysique pp, Type type, RegDate dateDebut, RegDate dateFin,
	                                     @Nullable TypeAutoriteFiscale typeAutoriteFiscale, @Nullable Integer noOfs,
	                                     @Nullable MotifFor motifOuverture, @Nullable MotifFor motifFermeture,
	                                     @Nullable ModeImposition modeImpositionDebut, @Nullable ModeImposition modeImpositionFin) {
		this.pp = pp;
		this.type = type;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.noOfs = noOfs;
		this.motifOuverture = motifOuverture;
		this.motifFermeture = motifFermeture;
		this.modeImpositionDebut = modeImpositionDebut;
		this.modeImpositionFin = modeImpositionFin;
		DateRangeHelper.assertValidRange(this.dateDebut, this.dateFin);
	}

	public PeriodeImpositionImpotSource(PeriodeImpositionImpotSource src, RegDate dateDebut, RegDate dateFin) {
		this(src.pp, src.type, dateDebut, dateFin, src.typeAutoriteFiscale, src.noOfs, src.motifOuverture, src.motifFermeture, src.modeImpositionDebut, src.modeImpositionFin);
	}

	public PeriodeImpositionImpotSource(PersonnePhysique pp, Type type, RegDate dateDebut, RegDate dateFin, @Nullable ForFiscalPrincipal ff) {
		this(pp, type, dateDebut, dateFin,
		     ff != null ? ff.getTypeAutoriteFiscale() : null,
		     ff != null ? ff.getNumeroOfsAutoriteFiscale() : null,
		     ff != null ? ff.getMotifOuverture() : null,
		     ff != null ? ff.getMotifFermeture() : null,
		     ff != null ? ff.getModeImposition() : null,
		     ff != null ? ff.getModeImposition() : null);
	}

	@Override
	public boolean isCollatable(DateRange next) {
		final PeriodeImpositionImpotSource other = (PeriodeImpositionImpotSource) next;

		// périodes fiscales différentes -> on ne colle jamais !
		if (dateDebut.year() != other.dateDebut.year()) {
			return false;
		}
		// changement de type d'autorité fiscale -> on ne colle pas
		if (other.typeAutoriteFiscale != typeAutoriteFiscale) {
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
}
