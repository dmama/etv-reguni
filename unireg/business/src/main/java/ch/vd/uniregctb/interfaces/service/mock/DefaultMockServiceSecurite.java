package ch.vd.uniregctb.interfaces.service.mock;

public class DefaultMockServiceSecurite extends MockServiceSecuriteService {

	@Override
	protected void init() {
		// nécessaire pour que norentes fonctionne
		addOperateur("iamtestuser", 0, "UR000002"); // Role.VISU_ALL
		addOperateur("zaiptf", 611836, "UR000002"); // pour que l'InfoController retourne OK en mode Norentes

		addOperateur("iamtestAdrienneCuendet", 333908, "UR000002");     // test des mouvements de dossiers
		addOperateur("iamtestMarcelDardare", 327706, "UR000002");       // test de visualisation de tiers
	}
}
