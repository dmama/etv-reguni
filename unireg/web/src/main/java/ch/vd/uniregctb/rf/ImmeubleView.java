package ch.vd.uniregctb.rf;

import java.net.URL;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.Contribuable;

@SuppressWarnings({"UnusedDeclaration"})
public class ImmeubleView {

	private final Long id;
	private final String dateDebut;
	private final String dateFin;
	private final String numero;
	private final Integer noCommune;
	private final String nomCommune;
	private final String nature;
	private final int estimationFiscale;
	private final String referenceEstimationFiscale;
	private final TypeImmeuble typeImmeuble;
	private final GenrePropriete genrePropriete;
	private final String partPropriete;
	private final Contribuable contribuable;
	private final String dateDernierMutation;
	private final TypeMutation derniereMutation;
	private final URL lienRF;

	public ImmeubleView(Immeuble immeuble) {
		this.id = immeuble.getId();
		this.dateDebut = RegDateHelper.dateToDisplayString(immeuble.getDateDebut());
		this.dateFin = RegDateHelper.dateToDisplayString(immeuble.getDateFin());
		this.numero = removeNumeroCommune(immeuble.getNumero()); // [SIFISC-3157]
		this.noCommune = extractNumeroCommune(immeuble.getNumero()); // [SIFISC-3309]
		this.nomCommune = immeuble.getNomCommune(); // [SIFISC-3157]
		this.nature = immeuble.getNature();
		this.estimationFiscale = immeuble.getEstimationFiscale();
		this.referenceEstimationFiscale = immeuble.getReferenceEstimationFiscale();
		this.typeImmeuble = immeuble.getTypeImmeuble();
		this.genrePropriete = immeuble.getGenrePropriete();
		this.partPropriete = immeuble.getPartPropriete().toString();
		this.contribuable = immeuble.getContribuable();
		this.dateDernierMutation = RegDateHelper.dateToDisplayString(immeuble.getDateDerniereMutation());
		this.derniereMutation = immeuble.getDerniereMutation();
		this.lienRF = immeuble.getLienRegistreFoncier();
	}

	/**
	 * @param numeroImmeuble un numéro d'immeuble (e.g. "130-12-1-1").
	 * @return le numéro RF de commune du numéro d'immeuble spécifié (e.g. 130).
	 */
	private static Integer extractNumeroCommune(String numeroImmeuble) {
		if (numeroImmeuble == null) {
			return null;
		}
		final String numeroAsString = numeroImmeuble.split("[-/]")[0]; // on tolère l'ancien (/) et le nouveau séparateur (-) pour blinder un peu tout ça
		try {
			return Integer.valueOf(numeroAsString);
		}
		catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * @param numeroImmeuble un numéro d'immeuble (e.g. "130-12-1-1").
	 * @return le numéro de l'immeuble <b>sans</b> le numéro de commune (e.g. "12-1-1")
	 */
	private static String removeNumeroCommune(String numeroImmeuble) {
		if (numeroImmeuble == null) {
			return null;
		}
		int i = numeroImmeuble.indexOf('-');
		if (i < 0) {
			i = numeroImmeuble.indexOf('/');
		}
		if (i < 0) {
			return numeroImmeuble;
		}
		return numeroImmeuble.substring(i + 1);
	}

	public Long getId() {
		return id;
	}

	public String getDateDebut() {
		return dateDebut;
	}

	public String getDateFin() {
		return dateFin;
	}

	public String getNumero() {
		return numero;
	}

	public Integer getNoCommune() {
		return noCommune;
	}

	public String getNomCommune() {
		return nomCommune;
	}

	public String getNature() {
		return nature;
	}

	public int getEstimationFiscale() {
		return estimationFiscale;
	}

	public String getReferenceEstimationFiscale() {
		return referenceEstimationFiscale;
	}

	public TypeImmeuble getTypeImmeuble() {
		return typeImmeuble;
	}

	public GenrePropriete getGenrePropriete() {
		return genrePropriete;
	}

	public String getPartPropriete() {
		return partPropriete;
	}

	public Contribuable getContribuable() {
		return contribuable;
	}

	public String getDateDernierMutation() {
		return dateDernierMutation;
	}

	public TypeMutation getDerniereMutation() {
		return derniereMutation;
	}

	public URL getLienRF() {
		return lienRF;
	}
}
