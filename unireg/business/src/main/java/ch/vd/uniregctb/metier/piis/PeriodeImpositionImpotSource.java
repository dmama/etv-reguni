package ch.vd.uniregctb.metier.piis;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.metier.common.Fraction;
import ch.vd.uniregctb.metier.common.FractionSimple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class PeriodeImpositionImpotSource extends AbstractCollatablePeriodeImpositionImpotSource<PeriodeImpositionImpotSource.Type> implements Duplicable<PeriodeImpositionImpotSource> {

	public enum Type {
		MIXTE,
		SOURCE
	}

	/**
	 * Constructeur utilisé lors du {@link #collate(ch.vd.registre.base.date.DateRange)}
	 * @param courant une période
	 * @param suivant la période suivante, qui doit fusionner avec la première
	 */
	private PeriodeImpositionImpotSource(PeriodeImpositionImpotSource courant, PeriodeImpositionImpotSource suivant) {
		super(courant, suivant);
		DateRangeHelper.assertValidRange(getDateDebut(), getDateFin());
	}

	/**
	 * Constructeur de duplication
	 * @param src source
	 * @see #duplicate()
	 */
	private PeriodeImpositionImpotSource(PeriodeImpositionImpotSource src) {
		super(src);
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
		super(pp, type, dateDebut, dateFin, typeAutoriteFiscale, noOfs, localisation, fractionDebut, fractionFin);
		DateRangeHelper.assertValidRange(dateDebut, dateFin);
	}

	/**
	 * Constructeur utilisé par le calcul de remplissage des trous
	 * @param src période d'imposition existante à adapter
	 * @param dateDebut nouvelle date de début
	 * @param dateFin nouvelle date de fin
	 */
	public PeriodeImpositionImpotSource(PeriodeImpositionImpotSource src, RegDate dateDebut, RegDate dateFin) {
		this(src.getContribuable(), src.getType(), dateDebut, dateFin, src.getTypeAutoriteFiscale(), src.getNoOfs(), src.getLocalisation(),
		     dateDebut == src.getDateDebut() ? src.getFractionDebut() : null,
		     dateFin == src.getDateFin() ? src.getFractionFin() : null);
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
	public DateRange collate(DateRange next) {
		return new PeriodeImpositionImpotSource(this, (PeriodeImpositionImpotSource) next);
	}

	@Override
	public PeriodeImpositionImpotSource duplicate() {
		return new PeriodeImpositionImpotSource(this);
	}
}
