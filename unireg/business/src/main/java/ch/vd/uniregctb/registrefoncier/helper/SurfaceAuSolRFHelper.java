package ch.vd.uniregctb.registrefoncier.helper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF;
import ch.vd.uniregctb.registrefoncier.key.SurfaceAuSolRFKey;

public abstract class SurfaceAuSolRFHelper {

	private SurfaceAuSolRFHelper() {
	}

	public static SurfaceAuSolRFKey newKey(@NotNull Bodenbedeckung surface) {
		return new SurfaceAuSolRFKey(surface.getGrundstueckIDREF(), surface.getArt().getTextFr(), surface.getFlaeche());
	}

	public static boolean dataEquals(@Nullable Set<SurfaceAuSolRF> surfaces, @Nullable List<Bodenbedeckung> bodenbedeckung) {

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
}
