package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.CapiCode;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.GebaeudeArt;
import ch.vd.uniregctb.registrefoncier.SurfaceBatimentRF;

public abstract class SurfaceBatimentRFHelper {

	private SurfaceBatimentRFHelper() {
	}

	public static boolean dataEquals(@Nullable SurfaceBatimentRF left, @Nullable SurfaceBatimentRF right) {
		if (left == null || right == null) {
			return left == null && right == null;
		}
		else {
			return Objects.equals(left.getType(), right.getType()) &&
					Objects.equals(left.getSurface(), right.getSurface());
		}
	}

	public static boolean dataEquals(@Nullable SurfaceBatimentRF left, @Nullable Gebaeude gebaeude) {
		return dataEquals(left, getSurfaceBatiment(gebaeude));
	}

	@Nullable
	public static SurfaceBatimentRF getSurfaceBatiment(@Nullable Gebaeude gebaeude) {
		if (gebaeude == null) {
			return null;
		}
		final String type = getTypeBatiment(gebaeude);
		final Integer surface = gebaeude.getFlaeche();
		if (type == null && surface == null) {
			return null;
		}
		return new SurfaceBatimentRF(type, surface);
	}

	@Nullable
	public static String getTypeBatiment(@NotNull Gebaeude gebaeude) {

		final Optional<GebaeudeArt> gebaeudeArt = Optional.of(gebaeude)
				.map(Gebaeude::getGebaeudeArten)
				.filter(l -> !l.isEmpty())
				.map(l -> l.get(0));

		// on prends soit le art-code (premier choix), soit le art-zusatz (second choix)
		return gebaeudeArt
				.map(GebaeudeArt::getGebaeudeArtCode)
				.map(CapiCode::getTextFr)
				.orElseGet(() -> gebaeudeArt
						.map(GebaeudeArt::getGebaeudeArtZusatz)
						.orElse(null)
				);
	}
}
