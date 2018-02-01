package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.GrundstueckZuGebaeude;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;

public abstract class ImplantationRFHelper {

	private ImplantationRFHelper() {
	}

	public static boolean dataEquals(@Nullable List<ImplantationRF> implantations, @Nullable List<GrundstueckZuGebaeude> gzgs) {

		//noinspection Duplicates
		if ((implantations == null || implantations.isEmpty()) && (gzgs == null || gzgs.isEmpty())) {
			// les deux collections sont vides ou nulles
			return true;
		}
		else if (implantations == null || gzgs == null) {
			// une seule collection est vide ou nulle
			return false;
		}
		else if (implantations.size() != gzgs.size()) {
			// les collections ne sont pas de tailles identiques
			return false;
		}

		List<ImplantationRF> remaining = new ArrayList<>(implantations);
		for (GrundstueckZuGebaeude gzg : gzgs) {
			boolean found = false;
			for (int i = 0; i < remaining.size(); i++) {
				ImplantationRF implantationRF = remaining.get(i);
				if (dataEquals(implantationRF, gzg)) {
					remaining.remove(i);
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}

		// si on arrive là, c'est que les deux collections sont identiques
		return true;
	}

	public static boolean dataEquals(@Nullable ImplantationRF implantation, @Nullable GrundstueckZuGebaeude gzg) {
		if (implantation == null || gzg == null) {
			return implantation == null && gzg == null;
		}
		else {
			return dataEquals(implantation, newImplantation(gzg, ImplantationRFHelper::simplisticImmeubleProvider));
		}
	}

	public static boolean dataEquals(@Nullable ImplantationRF left, @Nullable ImplantationRF right) {
		if (left == null || right == null) {
			return left == null && right == null;
		}
		else {
			return Objects.equals(left.getSurface(), right.getSurface()) &&
					Objects.equals(left.getImmeuble().getIdRF(), right.getImmeuble().getIdRF());
		}
	}

	/**
	 * Provider d'immeuble simplifié au maximum pour retourner un immeuble avec juste l'idRF de renseigné.
	 */
	@NotNull
	private static ImmeubleRF simplisticImmeubleProvider(String idRef) {
		final ImmeubleRF i = new ImmeubleRF() {
		};
		i.setIdRF(idRef);
		return i;
	}

	@NotNull
	public static ImplantationRF newImplantation(@NotNull GrundstueckZuGebaeude gzg,
	                                             @NotNull Function<String, ImmeubleRF> immeubleProvider) {
		final ImplantationRF i = new ImplantationRF();
		i.setSurface(gzg.getAbschnittFlaeche());
		i.setImmeuble(immeubleProvider.apply(gzg.getGrundstueckIDREF()));
		return i;
	}
}
