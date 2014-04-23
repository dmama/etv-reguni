package ch.vd.uniregctb.metier.piis;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.metier.common.Fraction;
import ch.vd.uniregctb.metier.common.FractionSimple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
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
	private final Localisation localisation;
	private final Fraction fractionDebut;
	private final Fraction fractionFin;

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
		this.localisation = suivant.localisation;
		this.dateDebut = courant.dateDebut;
		this.dateFin = suivant.dateFin;
		this.fractionDebut = courant.fractionDebut;
		this.fractionFin = courant.fractionFin;
		DateRangeHelper.assertValidRange(this.dateDebut, this.dateFin);
	}

	/**
	 * Constructeur de duplication
	 * @param src source
	 * @see #duplicate()
	 */
	private PeriodeImpositionImpotSource(PeriodeImpositionImpotSource src) {
		this.pp = src.pp;
		this.type = src.type;
		this.typeAutoriteFiscale = src.typeAutoriteFiscale;
		this.noOfs = src.noOfs;
		this.localisation = src.localisation;
		this.dateDebut = src.dateDebut;
		this.dateFin = src.dateFin;
		this.fractionDebut = src.fractionDebut;
		this.fractionFin = src.fractionFin;
	}

	/**
	 * Constructeur membre à membre
	 * @param pp personne physique concernée par la période
	 * @param type type de période
	 * @param dateDebut date de début de la période
	 * @param dateFin date de fin de la période
	 * @param typeAutoriteFiscale (optionnel) type d'autorité fiscale
	 * @param noOfs (optionnel) numéro OFS de l'autorité fiscale
	 * @param localisation localisation de la période
	 * @param fractionDebut (optionnel) fraction en début de période
	 * @param fractionFin (optionnel) fraction en fin de période
	 */
	public PeriodeImpositionImpotSource(@NotNull PersonnePhysique pp, @NotNull Type type, @NotNull RegDate dateDebut, @NotNull RegDate dateFin,
	                                     @Nullable TypeAutoriteFiscale typeAutoriteFiscale, @Nullable Integer noOfs, @NotNull Localisation localisation,
	                                     @Nullable Fraction fractionDebut, @Nullable Fraction fractionFin) {
		this.pp = pp;
		this.type = type;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.noOfs = noOfs;
		this.localisation = localisation;
		this.fractionDebut = fractionDebut;
		this.fractionFin = fractionFin;
		DateRangeHelper.assertValidRange(this.dateDebut, this.dateFin);
	}

	/**
	 * Constructeur utilisé par le calcul de remplissage des trous
	 * @param src période d'imposition existante à adapter
	 * @param dateDebut nouvelle date de début
	 * @param dateFin nouvelle date de fin
	 */
	public PeriodeImpositionImpotSource(PeriodeImpositionImpotSource src, RegDate dateDebut, RegDate dateFin) {
		this(src.pp, src.type, dateDebut, dateFin, src.typeAutoriteFiscale, src.noOfs, src.localisation,
		     dateDebut == src.dateDebut ? src.fractionDebut : null,
		     dateFin == src.dateFin ? src.fractionFin : null);
	}

	/**
	 * La période "bouche trou", forcément SOURCE et sans for
	 * @param pp une personne physique
	 * @param dateDebut date de début du trou
	 * @param dateFin date de fin du trou
	 */
	public PeriodeImpositionImpotSource(PersonnePhysique pp, RegDate dateDebut, RegDate dateFin) {
		this(pp, Type.SOURCE, dateDebut, dateFin, null, null, Localisation.getInconnue(), new FractionSimple(dateDebut, null, null), new FractionSimple(dateFin, null, null));
	}


	@Override
	public boolean isCollatable(DateRange next) {
		final PeriodeImpositionImpotSource other = (PeriodeImpositionImpotSource) next;

		// périodes fiscales différentes -> on ne colle jamais !
		if (dateDebut.year() != other.dateDebut.year()) {
			return false;
		}

		// changement de type d'autorité fiscale (ou conservation du même type en changeant de canton/pays) -> on ne colle pas
		if (!other.localisation.equals(localisation)) {
			return false;
		}

		// si fraction présente, il faut réfléchir
		if (fractionFin != null || other.fractionDebut != null) {
			final MotifFor motif = Fraction.getMotifEffectif(fractionFin != null ? fractionFin.getMotif() : null, other.fractionDebut != null ? other.fractionDebut.getMotif() : null);
			if (motif == MotifFor.VEUVAGE_DECES) {
				return false;
			}
			else if (motif == MotifFor.PERMIS_C_SUISSE || motif == MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION) {
				if (type != other.type) {
					return false;
				}
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

	public Localisation getLocalisation() {
		return localisation;
	}
}
