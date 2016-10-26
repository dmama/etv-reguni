package ch.vd.uniregctb.registrefoncier.helper;

import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.GewoehnlichesMiteigentum;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.Liegenschaft;
import ch.vd.capitastra.grundstueck.SDR;
import ch.vd.capitastra.grundstueck.StockwerksEinheit;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.MineRF;
import ch.vd.uniregctb.registrefoncier.PartCoproprieteRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.elements.BergwerkElement;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

public abstract class ImmeubleRFHelper {

	private ImmeubleRFHelper() {
	}

	/**
	 * Compare sur les données courantes d'un immeuble Unireg sont égales au données d'un immeuble du registre foncier.
	 * <p>
	 * En plus des données de base de l'immeuble, cette méthode tient compte des données suivantes :
	 * <ul>
	 * <li>les situations</li>
	 * <li>les estimations fiscales</li>
	 * </ul>
	 * <p>
	 * Les autres données rattachées à l'immeubles (bâtiments, surfaces, droits, ...) sont considérées indépendantes et ne sont pas comparées.
	 *
	 * @param immeuble    un immeuble stocké dans la DB Unireg
	 * @param grundstueck un immeuble reçu de l'import du registre foncier
	 * @return <b>vrai</b> si les données courante de l'immeuble Unireg sont égales aux données de l'import du registre foncier; <b>faux</b> si ce n'est pas le cas.
	 */
	public static boolean currentDataEquals(@NotNull ImmeubleRF immeuble, @NotNull Grundstueck grundstueck) {

		if (!immeuble.getIdRF().equals(grundstueck.getGrundstueckID())) {
			// erreur de programmation, on ne devrait jamais comparer deux immeubles avec des IDRef différents
			throw new ProgrammingException();
		}

		// [blindage] les valeurs suivantes ne doivent jamais changer (le modèle est construit sur ce prédicat)
		if (immeuble instanceof MineRF && !(grundstueck instanceof BergwerkElement) ||
				immeuble instanceof PartCoproprieteRF && !(grundstueck instanceof GewoehnlichesMiteigentum) ||
				immeuble instanceof BienFondRF && !(grundstueck instanceof Liegenschaft) ||
				immeuble instanceof DroitDistinctEtPermanentRF && !(grundstueck instanceof SDR) ||
				immeuble instanceof ProprieteParEtageRF && !(grundstueck instanceof StockwerksEinheit)) {
			throw new IllegalArgumentException("Le type de l'immeuble idRF=[" + immeuble.getIdRF() + "] a changé.");
		}
		if (immeuble instanceof BienFondRF) {
			final BienFondRF bienFond = (BienFondRF) immeuble;
			final String ligUnterartEnum = ((Liegenschaft) grundstueck).getLigUnterartEnum();
			if (bienFond.isCfa() != (ligUnterartEnum != null && ligUnterartEnum.contains("cfa"))) {
				throw new IllegalArgumentException("Le flag CFA de l'immeuble idRF=[" + immeuble.getIdRF() + "] a changé.");
			}
		}
		if (immeuble instanceof PartCoproprieteRF &&
				!FractionHelper.fractionEquals(((PartCoproprieteRF) immeuble).getQuotePart(),
				                               ((GewoehnlichesMiteigentum) grundstueck).getStammGrundstueck().getQuote())) {
			throw new IllegalArgumentException("La quote-part de l'immeuble idRF=[" + immeuble.getIdRF() + "] a changé.");
		}
		if (!immeuble.getEgrid().equals(grundstueck.getEGrid())) {
			throw new IllegalArgumentException("L'egrid de l'immeuble idRF=[" + immeuble.getIdRF() + "] a changé.");
		}
		// [/blindage]

		final SituationRF situation = immeuble.getSituations().stream()
				.filter(r -> r.isValidAt(RegDate.get()))
				.collect(Collectors.toList()).get(0);

		// on vérifie la situation courante
		if (!SituationRFHelper.situationEquals(situation, grundstueck.getGrundstueckNummer())) {
			return false;
		}

		final EstimationRF estimation = immeuble.getEstimations().stream()
				.filter(r -> r.isValidAt(RegDate.get()))
				.collect(Collectors.toList()).get(0);

		// on vérifie l'estimation fiscale courante
		if (!EstimationRFHelper.estimationEquals(estimation, grundstueck.getAmtlicheBewertung())) {
			return false;
		}

		// Note : les surfaces et les bâtiments sont des données reçues indépendamment de l'immeuble et possèdent leurs propres fonctions de comparaison.

		// les deux immeubles sont identiques
		return true;
	}

	@NotNull
	public static ImmeubleRFKey newImmeubleRFKey(@NotNull Grundstueck immeuble) {
		return new ImmeubleRFKey(immeuble.getGrundstueckID());
	}
}
