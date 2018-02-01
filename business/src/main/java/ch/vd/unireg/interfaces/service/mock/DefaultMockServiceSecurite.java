package ch.vd.unireg.interfaces.service.mock;

import ch.vd.unireg.security.Role;

public class DefaultMockServiceSecurite extends MockServiceSecuriteService {

	@Override
	protected void init() {
		// nécessaire pour que norentes fonctionne
		addOperateur("iamtestuser", 0, Role.VISU_ALL);
		addOperateur("zaiptf", 611836, Role.VISU_ALL); // pour que l'InfoController retourne OK en mode Norentes

		addOperateur("iamtestAdrienneCuendet", 333908, Role.VISU_ALL);     // test des mouvements de dossiers
		addOperateur("iamtestMarcelDardare", 327706, Role.VISU_ALL);       // test de visualisation de tiers
	}
}
