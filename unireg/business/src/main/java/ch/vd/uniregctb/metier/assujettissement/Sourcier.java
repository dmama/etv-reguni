package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Assujettissement de type sourcier; c'est-à-dire pour un contribuable (non-Suisse et sans permis C) dont l'impôt est directement
 * ponctionné sur son salaire (imposition à la source).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class Sourcier extends Assujettissement {

	private final TypeAutoriteFiscale typeAutoriteFiscale;

	protected Sourcier(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifFor motifFractDebut, MotifFor motifFractFin,
	                   TypeAutoriteFiscale typeAutoriteFiscale) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin);
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}

	protected Sourcier(Sourcier courant, Sourcier suivant) {
		super(courant, suivant);
		this.typeAutoriteFiscale = courant.typeAutoriteFiscale;
	}

	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	@Override
	public boolean isCollatable(DateRange next) {
		return super.isCollatable(next) && typeAutoriteFiscale.equals(((Sourcier) next).typeAutoriteFiscale);
	}
}
