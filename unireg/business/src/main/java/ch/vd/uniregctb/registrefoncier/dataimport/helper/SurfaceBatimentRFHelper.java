package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.registrefoncier.SurfaceBatimentRF;

public abstract class SurfaceBatimentRFHelper {

	private SurfaceBatimentRFHelper() {
	}

	public static boolean dataEquals(@Nullable SurfaceBatimentRF left, @Nullable SurfaceBatimentRF right) {
		if (left == null || right == null) {
			return left == null && right == null;
		}
		else {
			return Objects.equals(left.getSurface(), right.getSurface());
		}
	}
}
