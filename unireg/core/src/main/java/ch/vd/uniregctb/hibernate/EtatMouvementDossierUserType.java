package ch.vd.uniregctb.hibernate;

import ch.vd.uniregctb.mouvement.EtatMouvementDossier;

/**
 * Classe de transtypage hibernate EtatMouvementDossier <-> varchar
 */
public class EtatMouvementDossierUserType extends EnumUserType<EtatMouvementDossier> {

	public EtatMouvementDossierUserType() {
		super(EtatMouvementDossier.class);
	}
}
