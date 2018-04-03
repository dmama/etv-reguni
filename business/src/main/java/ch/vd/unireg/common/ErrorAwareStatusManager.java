package ch.vd.unireg.common;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

/**
 * Un statut manager avec une gestion des erreurs qui provoque son interruption.
 */
public class ErrorAwareStatusManager implements StatusManager {

	@NotNull
	private final StatusManager parent;
	@NotNull
	private final Supplier<Boolean> errorFlagSupplier;

	public ErrorAwareStatusManager(@NotNull StatusManager parent, @NotNull Supplier<Boolean> errorFlagSupplier) {
		this.parent = parent;
		this.errorFlagSupplier = errorFlagSupplier;
	}

	@Override
	public boolean isInterrupted() {
		// s'il y a une erreur, on arrête le traitement
		return parent.isInterrupted() || errorFlagSupplier.get();
	}

	@Override
	public void setMessage(String msg) {
		parent.setMessage(msg);
	}

	@Override
	public void setMessage(String msg, int percentProgression) {
		parent.setMessage(msg, percentProgression);
	}
}
