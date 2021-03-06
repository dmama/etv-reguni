package ch.vd.unireg.registrefoncier.immeuble.graph;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.MineRF;
import ch.vd.unireg.registrefoncier.PartCoproprieteRF;
import ch.vd.unireg.registrefoncier.ProprieteParEtageRF;
import ch.vd.unireg.registrefoncier.SituationRF;

/**
 * Elément qui représente un immeuble dans le graphe des liens entre immeubles et ayants-droits.
 */
@SuppressWarnings("unused")
public class Immeuble {

	public static final DecimalFormat MONTANT_FORMAT = new DecimalFormat("###,###", DecimalFormatSymbols.getInstance(new Locale("fr", "CH")));
	/**
	 * La clé d'identification unique dans le graphe de l'immeuble
	 */
	private final String key;
	private final Long id;
	private final String egrid;
	private final String idRF;
	private final String typeShort;
	private final String typeLong;
	private final String commune;
	private final String parcelle;
	private final String estimationFiscale;

	public Immeuble(@NotNull ImmeubleRF immeuble) {

		final RegDate today = RegDate.get();

		this.key = buildKey(immeuble);
		this.id = immeuble.getId();
		this.egrid = immeuble.getEgrid();
		this.idRF = immeuble.getIdRF();

		final SituationRF situation = immeuble.getSituations().stream()
				.filter(s -> s.isValidAt(today))
				.findFirst()
				.orElseThrow(IllegalArgumentException::new);

		final String typeShort;
		final String typeLong;
		if (immeuble instanceof BienFondsRF) {
			typeShort = "BF";
			typeLong = "Bien-fonds";
		}
		else if (immeuble instanceof DroitDistinctEtPermanentRF) {
			typeShort = "DDP";
			typeLong = "Droit distinct et permanent";
		}
		else if (immeuble instanceof MineRF) {
			typeShort = "Mine";
			typeLong = "Mine";
		}
		else if (immeuble instanceof PartCoproprieteRF) {
			typeShort = "PC";
			typeLong = "Part de copropriété";
		}
		else if (immeuble instanceof ProprieteParEtageRF) {
			typeShort = "PPE";
			typeLong = "Propriété par étage";
		}
		else {
			throw new IllegalArgumentException();
		}
		this.typeShort = typeShort;
		this.typeLong = typeLong;

		this.commune = situation.getCommune().getNomRf();

		String parcelle = String.valueOf(situation.getNoParcelle());
		if (situation.getIndex1() != null) {
			parcelle += "-" + situation.getIndex1();
		}
		if (situation.getIndex2() != null) {
			parcelle += "-" + situation.getIndex2();
		}
		if (situation.getIndex3() != null) {
			parcelle += "-" + situation.getIndex3();
		}
		this.parcelle = parcelle;

		this.estimationFiscale = immeuble.getEstimations().stream()
				.filter(e -> e.isValidAt(today))
				.findFirst()
				.map(EstimationRF::getMontant)
				.map(montant -> MONTANT_FORMAT.format(montant) + " CHF")
				.orElse(null);

	}

	/**
	 * Construit la clé d'identification dans le graphe de l'immeuble spécifié.
	 *
	 * @param immeuble un immeuble
	 * @return la clé d'identification unique de l'immeuble
	 */
	public static String buildKey(@NotNull ImmeubleRF immeuble) {
		final String egrid = immeuble.getEgrid();
		return egrid == null ? immeuble.getIdRF() : egrid;
	}

	/**
	 * Parse la clé spécifiée pour en extraire l'egrid de l'immeuble.
	 *
	 * @param key une clé
	 * @return l'egrid de l'immeuble <b>si</b> la clé est celle d'un immeuble; ou <b>null</b> s'il s'agit d'une autre clé.
	 */
	@Nullable
	public static String parseKey(@NotNull String key) {
		if (key.startsWith("CH")) {
			return key;
		}
		else {
			return null;    // pas un immeuble
		}
	}

	public String getKey() {
		return key;
	}

	public Long getId() {
		return id;
	}

	public String getEgrid() {
		return egrid;
	}

	public String getIdRF() {
		return idRF;
	}

	public String getTypeShort() {
		return typeShort;
	}

	public String getTypeLong() {
		return typeLong;
	}

	public String getCommune() {
		return commune;
	}

	public String getParcelle() {
		return parcelle;
	}

	public String getEstimationFiscale() {
		return estimationFiscale;
	}

	public String toDot(boolean showEstimationFiscales) {
		String s = key + " [shape=record, label=\"" + typeShort + "|" + commune + "/" + parcelle;
		if (showEstimationFiscales && StringUtils.isNotBlank(estimationFiscale)) {
			s += "|" + estimationFiscale;
		}
		s += "\", style=filled, color=sienna2]";
		return s;
	}
}
