package ch.vd.uniregctb.mouvement;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.mouvement.manager.MouvementMasseManager;

public abstract class AbstractMouvementMasseController extends AbstractMouvementController {

	private MouvementMasseManager mouvementManager;

	public void setMouvementManager(MouvementMasseManager mouvementManager) {
		this.mouvementManager = mouvementManager;
	}

	protected MouvementMasseManager getMouvementManager() {
		return mouvementManager;
	}

	/**
	 * Renvoie le numéro de collectivité administrative courant (celle avec
	 * laquelle l'utilisateur s'est autentifié dans l'application), ou <code>null</code>
	 * si c'est l'ACI
	 * @return
	 */
	protected Integer getNoCollAdmFiltree() {
		final Integer oid = AuthenticationHelper.getCurrentOID();
		if (oid != null && oid != ServiceInfrastructureService.noACI) {
			return oid;
		}
		else {
			return null;
		}
	}
}
