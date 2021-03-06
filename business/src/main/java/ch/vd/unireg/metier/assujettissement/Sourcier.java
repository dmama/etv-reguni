package ch.vd.unireg.metier.assujettissement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Assujettissement de type sourcier; c'est-à-dire pour un contribuable (non-Suisse et sans permis C) dont l'impôt est directement
 * ponctionné sur son salaire (imposition à la source).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class Sourcier extends Assujettissement {

	private final TypeAutoriteFiscale typeAutoriteFiscale;

	protected Sourcier(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifFractDebut, MotifAssujettissement motifFractFin,
	                   TypeAutoriteFiscale typeAutoriteFiscale, AssujettissementSurCommuneAnalyzer communeAnalyzer) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, communeAnalyzer);
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}

	protected Sourcier(Sourcier source, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		super(source, dateDebut, dateFin, motifDebut, motifFin);
		this.typeAutoriteFiscale = source.typeAutoriteFiscale;
	}

	protected Sourcier(Sourcier courant, Sourcier suivant) {
		super(courant, suivant);
		this.typeAutoriteFiscale = courant.typeAutoriteFiscale;
	}

	public TypeAutoriteFiscale getTypeAutoriteFiscalePrincipale() {
		return typeAutoriteFiscale;
	}

	@Override
	public boolean isCollatable(Assujettissement next) {
		return super.isCollatable(next) && typeAutoriteFiscale == ((Sourcier) next).typeAutoriteFiscale;
	}
}
