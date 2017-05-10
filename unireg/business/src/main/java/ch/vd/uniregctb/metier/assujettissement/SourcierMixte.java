package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Assujettissement de type sourcier mixte; c'est-à-dire pour un contribuable imposé à la source mais qui remplit une déclaration d'impôt
 * car :
 * <ul>
 * <li>il possède un revenu suffisamment élevé (mixte Art. 137 Al. 2), ou</li>
 * <li>il possède une fortune élevée ou un immeuble (mixte Art. 137 Al. 1)</li>
 * </ul>
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class SourcierMixte extends Sourcier {

	public SourcierMixte(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifFractDebut, MotifAssujettissement motifFractFin, TypeAutoriteFiscale typeAutoriteFiscale, AssujettissementSurCommuneAnalyzer communeAnalyzer) {
		super(contribuable, dateDebut, dateFin, motifFractDebut, motifFractFin, typeAutoriteFiscale, communeAnalyzer);
	}

	protected SourcierMixte(SourcierMixte source, RegDate dateDebut, RegDate dateFin, MotifAssujettissement motifDebut, MotifAssujettissement motifFin) {
		super(source, dateDebut, dateFin, motifDebut, motifFin);
	}

	public SourcierMixte(SourcierMixte courant, SourcierMixte suivant) {
		super(courant, suivant);
	}
}
