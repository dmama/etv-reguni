package ch.vd.unireg.scheduler;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * Les données d'une exécution retardée d'un job.
 */
public class QueuedExecutionInfo {

	/**
	 * L'utilisateur qui a demandé le démarrage du job.
	 */
	@NotNull
	private final String user;

	/**
	 * Les paramètres de démarrage du job
	 */
	@NotNull
	private final Map<String, Object> params;

	public QueuedExecutionInfo(@NotNull String user, @NotNull Map<String, Object> params) {
		this.user = user;
		this.params = params;
	}

	@NotNull
	public String getUser() {
		return user;
	}

	@NotNull
	public Map<String, Object> getParams() {
		return params;
	}
}
