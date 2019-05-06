package ch.vd.unireg.interfaces.service.mock;

import ch.vd.unireg.security.Role;

public class DefaultMockServiceSecurite extends MockServiceSecuriteService {

	@Override
	protected void init() {
		// nécessaire pour que norentes fonctionne
		addOperateur("iamtestuser", Role.VISU_ALL);
		addOperateur("zaiptf", Role.VISU_ALL); // pour que l'InfoController retourne OK en mode Norentes

		addOperateur("iamtestAdrienneCuendet", Role.VISU_ALL);     // test des mouvements de dossiers
		addOperateur("iamtestMarcelDardare", Role.VISU_ALL);       // test de visualisation de tiers
	}
}
