package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.CapiCode;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.GebaeudeArt;
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
		if (!Objects.equals(batiment.getType(), getTypeBatiment(gebaeude))) {
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
		batiment.setType(getTypeBatiment(gebaeude));

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

	@Nullable
	private static String getTypeBatiment(@NotNull Gebaeude gebaeude) {

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
