package ch.vd.unireg.tiers.manager;

import org.jetbrains.annotations.NotNull;

public interface AutorisationCache {
	@NotNull
	Autorisations getAutorisations(Long tiersId, String visa, int oid);
}
