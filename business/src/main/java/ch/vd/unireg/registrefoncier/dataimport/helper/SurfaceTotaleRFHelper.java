package ch.vd.unireg.registrefoncier.dataimport.helper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.GrundstueckFlaeche;
import ch.vd.unireg.registrefoncier.SurfaceTotaleRF;

public abstract class SurfaceTotaleRFHelper {

	private SurfaceTotaleRFHelper() {
	}

	public static boolean dataEquals(@Nullable SurfaceTotaleRF estimation, @Nullable GrundstueckFlaeche flaeche) {
		return dataEquals(estimation, get(flaeche));
	}

	public static boolean dataEquals(@Nullable SurfaceTotaleRF left, @Nullable SurfaceTotaleRF right) {
		if (left == null && right == null) {
			return true;
		}
		else //noinspection SimplifiableIfStatement
			if (left == null || right == null) {
			return false;
		}
		else {
			return left.getSurface() == right.getSurface();
		}
	}

	@NotNull
	public static SurfaceTotaleRF newSurfaceTotaleRF(int flaeche) {
		final SurfaceTotaleRF surface = new SurfaceTotaleRF();
		surface.setSurface(flaeche);
		return surface;
	}

	@Nullable
	public static SurfaceTotaleRF get(@Nullable GrundstueckFlaeche flaeche) {
		if (flaeche == null || flaeche.getFlaeche() == null) {
			return null;
		}
		return newSurfaceTotaleRF(flaeche.getFlaeche());
	}
}
