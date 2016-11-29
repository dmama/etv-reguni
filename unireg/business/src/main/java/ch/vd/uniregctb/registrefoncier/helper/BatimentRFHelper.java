package ch.vd.uniregctb.registrefoncier.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.GrundstueckZuGebaeude;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceBatimentRF;
import ch.vd.uniregctb.registrefoncier.key.BatimentRFKey;

public abstract class BatimentRFHelper {

	private BatimentRFHelper() {
	}

	public static BatimentRFKey newBatimentRFKey(@NotNull Gebaeude batiment) {
		return new BatimentRFKey(batiment.getMasterID());
	}

	public static boolean currentDataEquals(@NotNull BatimentRF batiment, @NotNull Gebaeude gebaeude) {

		if (!batiment.getMasterIdRF().equals(gebaeude.getMasterID())) {
			// erreur de programmation, on ne devrait jamais comparer deux bâtiments avec des masterIDRef différents
			throw new ProgrammingException();
		}

		// [blindage] les valeurs suivantes ne doivent jamais changer (le modèle est construit sur ce prédicat)
		if (!Objects.equals(batiment.getType(), gebaeude.getGebaeudeArten().get(0).getGebaeudeArtCode().getTextFr())) {
			throw new IllegalArgumentException("Le type du bâtiment masterIdRF=[" + batiment.getMasterIdRF() + "] a changé.");
		}
		// [/blindage]

		// on vérifie la surface courante
		final Integer surface = batiment.getSurfaces().stream()
				.filter(s -> s.isValidAt(null))
				.findFirst()
				.map(SurfaceBatimentRF::getSurface)
				.orElse(null);
		if (!Objects.equals(surface, gebaeude.getFlaeche())) {
			return false;
		}

		// on vérifie les implantations courantes
		final List<ImplantationRF> implantations = batiment.getImplantations().stream()
				.filter(i -> i.isValidAt(null))
				.collect(Collectors.toList());
		if (!ImplantationRFHelper.dataEquals(implantations, gebaeude.getGrundstueckZuGebaeude())) {
			return false;
		}

		// les deux bâtiments sont identiques
		return true;
	}

	@NotNull
	public static BatimentRF newBatimentRF(@NotNull Gebaeude gebaeude, @NotNull Function<String, ImmeubleRF> immeubleProvider) {

		final BatimentRF batiment = new BatimentRF();
		batiment.setMasterIdRF(gebaeude.getMasterID());
		batiment.setType(gebaeude.getGebaeudeArten().get(0).getGebaeudeArtCode().getTextFr());

		final Integer flaeche = gebaeude.getFlaeche();
		if (flaeche != null) {
			batiment.addSurface(new SurfaceBatimentRF(flaeche));
		}
		else {
			batiment.setSurfaces(new HashSet<>());
		}

		final List<GrundstueckZuGebaeude> gzg = gebaeude.getGrundstueckZuGebaeude();
		gzg.forEach(i -> batiment.addImplantation(ImplantationRFHelper.newImplantation(i, immeubleProvider)));

		return batiment;
	}
}
