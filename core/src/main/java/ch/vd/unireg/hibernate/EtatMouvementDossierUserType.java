package ch.vd.unireg.hibernate;

import ch.vd.unireg.mouvement.EtatMouvementDossier;

/**
 * Classe de transtypage hibernate EtatMouvementDossier <-> varchar
 */
public class EtatMouvementDossierUserType extends EnumUserType<EtatMouvementDossier> {

	public EtatMouvementDossierUserType() {
		super(EtatMouvementDossier.class);
	}
}
