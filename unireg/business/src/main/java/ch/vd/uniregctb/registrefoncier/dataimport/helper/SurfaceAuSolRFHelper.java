package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.CapiCode;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.registrefoncier.key.SurfaceAuSolRFKey;

public abstract class SurfaceAuSolRFHelper {

	private SurfaceAuSolRFHelper() {
	}

	public static SurfaceAuSolRFKey newKey(@NotNull Bodenbedeckung surface) {
		return new SurfaceAuSolRFKey(surface.getGrundstueckIDREF(), getType(surface), surface.getFlaeche());
	}

	public static SurfaceAuSolRF newSurfaceAuSolRF(@NotNull Bodenbedeckung bodenbedeckung) {
		final SurfaceAuSolRF surface = new SurfaceAuSolRF();
		surface.setSurface(bodenbedeckung.getFlaeche());
		surface.setType(getType(bodenbedeckung));
		return surface;
	}

	@NotNull
	private static String getType(@NotNull Bodenbedeckung bodenbedeckung) {
		return Optional.of(bodenbedeckung)
				                .map(Bodenbedeckung::getArt)
				                .map(CapiCode::getTextFr)
				                .orElse("Indéterminé"); // SIFISC-22504 + SIFISC-23055
	}

	public static boolean dataEquals(@Nullable Set<SurfaceAuSolRF> surfaces, @Nullable List<Bodenbedeckung> bodenbedeckung) {

		//noinspection Duplicates
		if ((surfaces == null || surfaces.isEmpty()) && (bodenbedeckung == null || bodenbedeckung.isEmpty())) {
			// les deux collections sont vides ou nulles
			return true;
		}
		else if (surfaces == null || bodenbedeckung == null) {
			// une seule collection est vide ou nulle
			return false;
		}
		else if (surfaces.size() != bodenbedeckung.size()) {
			// les collections ne sont pas de tailles identiques
			return false;
		}

		// l'égalité des deux collections revient à effectuer l'égalité entre les deux ensemble de clés, car la clé contient toutes les données de la surface.
		final Set<SurfaceAuSolRFKey> existantes = surfaces.stream()
				.map(SurfaceAuSolRFKey::new)
				.collect(Collectors.toSet());
		final Set<SurfaceAuSolRFKey> nouvelles = bodenbedeckung.stream()
				.map(SurfaceAuSolRFHelper::newKey).collect(Collectors.toSet());

		return existantes.equals(nouvelles);
	}

	public static boolean dataEquals(@NotNull SurfaceAuSolRF left, @NotNull SurfaceAuSolRF right) {
		return left.getSurface() == right.getSurface() &&
				Objects.equals(left.getType(), right.getType());
	}
}
