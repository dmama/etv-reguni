package ch.vd.uniregctb.registrefoncier.helper;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.uniregctb.registrefoncier.key.SurfaceAuSolRFKey;

public abstract class SurfaceAuSolRFHelper {

	private SurfaceAuSolRFHelper() {
	}

	public static SurfaceAuSolRFKey newKey(@NotNull Bodenbedeckung surface) {
		return new SurfaceAuSolRFKey(surface.getGrundstueckIDREF(), surface.getArt().getTextFr(), surface.getFlaeche());
	}
}
