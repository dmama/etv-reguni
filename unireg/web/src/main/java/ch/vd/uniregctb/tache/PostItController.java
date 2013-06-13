package ch.vd.uniregctb.tache;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.common.AuthenticationHelper;

@Controller
@RequestMapping(value = "/postit")
public class PostItController {

	private TacheService tacheService;

	@SuppressWarnings("UnusedDeclaration")
	public void setTacheService(TacheService tacheService) {
		this.tacheService = tacheService;
	}

	@SuppressWarnings("UnusedDeclaration")
	public static class ToDo {
		int taches;
		int dossiers;

		private ToDo(int taches, int dossiers) {
			this.taches = taches;
			this.dossiers = dossiers;
		}

		public int getTaches() {
			return taches;
		}

		public int getDossiers() {
			return dossiers;
		}
	}

	/**
	 * Retourne le nombre de tâches (et dossiers) en instance pour l'utilisateur courant sous forme JSON.
	 *
	 * @return le nombre de tâches et de dossiers en instance
	 */
	@RequestMapping(value = "/todo.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public ToDo todo() {

		final Integer oid;
		if (AuthenticationHelper.getAuthentication() != null) {
			oid = AuthenticationHelper.getCurrentOID();
		}
		else {
			oid = -1;
		}

		// on récupère les infos concernant les tâches et les dossiers
		int tachesEnInstanceCount = tacheService.getTachesEnInstanceCount(oid);
		int dossiersEnInstanceCount = tacheService.getDossiersEnInstanceCount(oid);

		return new ToDo(tachesEnInstanceCount, dossiersEnInstanceCount);
	}
}
