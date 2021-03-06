package ch.vd.unireg.registrefoncier.dataimport.helper;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.common.GrundstueckMitFlaeche;
import ch.vd.capitastra.grundstueck.Bergwerk;
import ch.vd.capitastra.grundstueck.GewoehnlichesMiteigentum;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.GrundstueckFlaeche;
import ch.vd.capitastra.grundstueck.Liegenschaft;
import ch.vd.capitastra.grundstueck.SDR;
import ch.vd.capitastra.grundstueck.StockwerksEinheit;
import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.MineRF;
import ch.vd.unireg.registrefoncier.PartCoproprieteRF;
import ch.vd.unireg.registrefoncier.ProprieteParEtageRF;
import ch.vd.unireg.registrefoncier.QuotePartRF;
import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.registrefoncier.SurfaceTotaleRF;
import ch.vd.unireg.registrefoncier.dataimport.elements.principal.BergwerkElement;
import ch.vd.unireg.registrefoncier.key.ImmeubleRFKey;

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
				immeuble instanceof BienFondsRF && !(grundstueck instanceof Liegenschaft) ||
				immeuble instanceof DroitDistinctEtPermanentRF && !(grundstueck instanceof SDR) ||
				immeuble instanceof ProprieteParEtageRF && !(grundstueck instanceof StockwerksEinheit)) {
			throw new IllegalArgumentException("Le type de l'immeuble idRF=[" + immeuble.getIdRF() + "] a changé.");
		}
		if (immeuble instanceof BienFondsRF) {
			final BienFondsRF bienFonds = (BienFondsRF) immeuble;
			final String ligUnterartEnum = ((Liegenschaft) grundstueck).getLigUnterartEnum();
			if (bienFonds.isCfa() != (ligUnterartEnum != null && ligUnterartEnum.contains("cfa"))) {
				throw new IllegalArgumentException("Le flag CFA de l'immeuble idRF=[" + immeuble.getIdRF() + "] a changé.");
			}
		}
		// [/blindage]

		// on vérifie l'état de radiation
		if (immeuble.getDateRadiation() != null) {
			// un immeuble radié est forcément différent d'un immeuble que l'on reçoit du RF (qui par définition n'est pas radié)
			return false;
		}

		if (!Objects.equals(immeuble.getEgrid(), grundstueck.getEGrid())) {
			// un egrid peut être attribué avec plusieurs semaines de retard, on détecte donc tout changement dessus.
			return false;
		}

		// on vérifie la situation courante
		final SituationRF situation = immeuble.getSituations().stream()
				.filter(r -> r.isValidAt(null))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("L'immeuble idRF=[" + immeuble.getIdRF() + "] ne contient pas de situation."));

		if (!SituationRFHelper.dataEquals(situation, grundstueck.getGrundstueckNummer())) {
			return false;
		}

		// [SIFISC-24715] on vérifie la quote part
		if (immeuble instanceof ProprieteParEtageRF) {
			final ProprieteParEtageRF ppe = (ProprieteParEtageRF) immeuble;
			final Fraction quotePart = ppe.getQuotesParts().stream()
					.filter(q -> q.isValidAt(null))
					.map(QuotePartRF::getQuotePart)
					.findFirst()
					.orElse(null);
			final StockwerksEinheit se = (StockwerksEinheit) grundstueck;
			if (!FractionHelper.dataEquals(quotePart, se.getStammGrundstueck().getQuote())) {
				return false;
			}
		}
		else if (immeuble instanceof PartCoproprieteRF) {
			final PartCoproprieteRF pcp = (PartCoproprieteRF) immeuble;
			final Fraction quotePart = pcp.getQuotesParts().stream()
					.filter(q -> q.isValidAt(null))
					.map(QuotePartRF::getQuotePart)
					.findFirst()
					.orElse(null);
			final GewoehnlichesMiteigentum gm = (GewoehnlichesMiteigentum) grundstueck;
			if (!FractionHelper.dataEquals(quotePart, gm.getStammGrundstueck().getQuote())) {
				return false;
			}
		}

		// on vérifie l'estimation fiscale courante
		final EstimationRF estimation = immeuble.getEstimations().stream()
				.filter(r -> r.isValidAt(null))
				.findFirst()
				.orElse(null);

		if (!EstimationRFHelper.dataEquals(estimation, grundstueck.getAmtlicheBewertung())) {
			return false;
		}

		// on vérifie la surface totale courante
		final SurfaceTotaleRF surfaceTotale = immeuble.getSurfacesTotales().stream()
				.filter(r -> r.isValidAt(null))
				.findFirst()
				.orElse(null);
		final GrundstueckFlaeche grundstueckFlaeche = Optional.of(grundstueck)
				.filter(GrundstueckMitFlaeche.class::isInstance)        // seuls certains immeubles possèdent une surface totale
				.map(GrundstueckMitFlaeche.class::cast)
				.map(GrundstueckMitFlaeche::getGrundstueckFlaeche)
				.orElse(null);

		if (!SurfaceTotaleRFHelper.dataEquals(surfaceTotale, grundstueckFlaeche)) {
			return false;
		}

		// Note : les surfaces et les bâtiments sont des données reçues indépendamment de l'immeuble et possèdent leurs propres fonctions de comparaison.

		// les deux immeubles sont identiques
		return true;
	}

	public static boolean idRFEquals(@NotNull ImmeubleRF left, @NotNull ImmeubleRF right) {
		return Objects.equals(left.getIdRF(), right.getIdRF());
	}

	@NotNull
	public static ImmeubleRFKey newImmeubleRFKey(@NotNull Grundstueck immeuble) {
		return new ImmeubleRFKey(immeuble.getGrundstueckID());
	}

	@NotNull
	public static ImmeubleRF newImmeubleRF(@NotNull Grundstueck grundstueck, @NotNull Function<Integer, CommuneRF> communeProvider) {

		final ImmeubleRF immeuble;

		// on crée l'immeuble qui va bien
		if (grundstueck instanceof Bergwerk) {
			immeuble = new MineRF();
		}
		else if (grundstueck instanceof GewoehnlichesMiteigentum) {
			final GewoehnlichesMiteigentum gm = (GewoehnlichesMiteigentum) grundstueck;
			final PartCoproprieteRF copro = new PartCoproprieteRF();
			final QuotePartRF quotePart = QuotePartRFHelper.get(gm.getStammGrundstueck());
			if (quotePart != null) {
				copro.addQuotePart(quotePart);
			}
			immeuble = copro;
		}
		else if (grundstueck instanceof Liegenschaft) {
			final Liegenschaft liegenschaft =(Liegenschaft) grundstueck;
			final BienFondsRF bienFonds = new BienFondsRF();
			final String ligUnterartEnum = liegenschaft.getLigUnterartEnum();
			bienFonds.setCfa(ligUnterartEnum != null && ligUnterartEnum.contains("cfa"));
			immeuble = bienFonds;
		}
		else if (grundstueck instanceof SDR) {
			immeuble = new DroitDistinctEtPermanentRF();
		}
		else if (grundstueck instanceof StockwerksEinheit) {
			final StockwerksEinheit stockwerksEinheit = (StockwerksEinheit) grundstueck;
			final ProprieteParEtageRF ppe = new ProprieteParEtageRF();
			final QuotePartRF quotePart = QuotePartRFHelper.get(stockwerksEinheit.getStammGrundstueck());
			if (quotePart != null) {
				ppe.addQuotePart(quotePart);
			}
			immeuble = ppe;
		}
		else {
			throw new IllegalArgumentException("Type de bâtiment inconnu = [" + grundstueck.getClass() + "]");
		}

		// on ajoute les données communes
		immeuble.setIdRF(grundstueck.getGrundstueckID());
		immeuble.setEgrid(grundstueck.getEGrid());
		immeuble.addSituation(SituationRFHelper.newSituationRF(grundstueck.getGrundstueckNummer(), communeProvider));
		final EstimationRF estimation = EstimationRFHelper.get(grundstueck.getAmtlicheBewertung());
		if (estimation != null) {
			immeuble.addEstimation(estimation);
		}

		// on ajoute la surface totale, si renseignée
		Optional.of(grundstueck)
				.filter(GrundstueckMitFlaeche.class::isInstance)                // seuls certains immeubles possèdent une surface totale
				.map(GrundstueckMitFlaeche.class::cast)
				.map(GrundstueckMitFlaeche::getGrundstueckFlaeche)
				.map(GrundstueckFlaeche::getFlaeche)
				.ifPresent(flaeche -> {
					final SurfaceTotaleRF surfaceTotale = SurfaceTotaleRFHelper.newSurfaceTotaleRF(flaeche);
					immeuble.addSurfaceTotale(surfaceTotale);
				});

		return immeuble;
	}

}
