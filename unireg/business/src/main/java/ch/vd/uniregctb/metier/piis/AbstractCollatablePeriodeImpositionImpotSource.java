package ch.vd.uniregctb.metier.piis;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.metier.common.Fraction;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 *
 * @param <T> la typologie des periodes concrètes
 */
public abstract class AbstractCollatablePeriodeImpositionImpotSource<T extends Enum<T>> implements CollatableDateRange {

	private final PersonnePhysique contribuable;
	private final T type;
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
	protected AbstractCollatablePeriodeImpositionImpotSource(AbstractCollatablePeriodeImpositionImpotSource<T> courant, AbstractCollatablePeriodeImpositionImpotSource<T> suivant) {
		if (!courant.isCollatable(suivant)) {
			throw new IllegalArgumentException();
		}
		this.contribuable = courant.contribuable;
		this.type = suivant.type;
		this.typeAutoriteFiscale = suivant.typeAutoriteFiscale;
		this.noOfs = suivant.noOfs;
		this.localisation = suivant.localisation;
		this.dateDebut = courant.dateDebut;
		this.dateFin = suivant.dateFin;
		this.fractionDebut = courant.fractionDebut;
		this.fractionFin = courant.fractionFin;
	}

	/**
	 * Constructeur de duplication
	 * @param src source
	 */
	protected AbstractCollatablePeriodeImpositionImpotSource(AbstractCollatablePeriodeImpositionImpotSource<T> src) {
		this.contribuable = src.contribuable;
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
	protected AbstractCollatablePeriodeImpositionImpotSource(@NotNull PersonnePhysique pp, @NotNull T type, @NotNull RegDate dateDebut, @NotNull RegDate dateFin,
	                                                         @Nullable TypeAutoriteFiscale typeAutoriteFiscale, @Nullable Integer noOfs, @NotNull Localisation localisation,
	                                                         @Nullable Fraction fractionDebut, @Nullable Fraction fractionFin) {
		this.contribuable = pp;
		this.type = type;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.noOfs = noOfs;
		this.localisation = localisation;
		this.fractionDebut = fractionDebut;
		this.fractionFin = fractionFin;
	}

	@Override
	public boolean isCollatable(DateRange next) {
		if (getClass() != next.getClass()) {
			return false;
		}

		final AbstractCollatablePeriodeImpositionImpotSource<T> other = (AbstractCollatablePeriodeImpositionImpotSource<T>) next;

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
	public final RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public final RegDate getDateFin() {
		return dateFin;
	}

	public final PersonnePhysique getContribuable() {
		return contribuable;
	}

	public final T getType() {
		return type;
	}

	public final TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public final Integer getNoOfs() {
		return noOfs;
	}

	public final Localisation getLocalisation() {
		return localisation;
	}

	public final Fraction getFractionDebut() {
		return fractionDebut;
	}

	public final Fraction getFractionFin() {
		return fractionFin;
	}
}
