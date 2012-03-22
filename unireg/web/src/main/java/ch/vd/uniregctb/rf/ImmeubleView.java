package ch.vd.uniregctb.rf;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.utils.WebContextUtils;

@SuppressWarnings({"UnusedDeclaration"})
public class ImmeubleView {

	private final Long id;
	private final String dateDebut;
	private final String dateFin;
	private final String numero;
	private final Integer noCommune;
	private final String nomCommune;
	private final String nature;
	private final String estimationFiscale;
	private final String referenceEstimationFiscale;
	private final String typeImmeuble;
	private final String genrePropriete;
	private final String partPropriete;
	private final String dateDernierMutation;
	private final String derniereMutation;
	private final URL lienRF;
	
	private ThreadLocal<DecimalFormat> decimalFormat = new ThreadLocal<DecimalFormat>() {
		@Override
		protected DecimalFormat initialValue() {
			final DecimalFormatSymbols symbols = new DecimalFormatSymbols();
			symbols.setGroupingSeparator('\'');
			return new DecimalFormat("#,###", symbols);
		}
	};

	public ImmeubleView(Immeuble immeuble, MessageSource messageSource) {
		this.id = immeuble.getId();
		this.dateDebut = RegDateHelper.dateToDisplayString(immeuble.getDateDebut());
		this.dateFin = RegDateHelper.dateToDisplayString(immeuble.getDateFin());
		this.numero = removeNumeroCommune(immeuble.getNumero()); // [SIFISC-3157]
		this.noCommune = extractNumeroCommune(immeuble.getNumero()); // [SIFISC-3309]
		this.nomCommune = immeuble.getNomCommune(); // [SIFISC-3157]
		this.nature = immeuble.getNature();
		this.estimationFiscale = formatNumber(immeuble.getEstimationFiscale());
		this.referenceEstimationFiscale = immeuble.getReferenceEstimationFiscale();
		this.typeImmeuble = getMessage(messageSource, "option.rf.type.immeuble.", immeuble.getTypeImmeuble());
		this.genrePropriete = getMessage(messageSource, "option.rf.genre.propriete.", immeuble.getGenrePropriete());
		this.partPropriete = immeuble.getPartPropriete().toString();
		this.dateDernierMutation = RegDateHelper.dateToDisplayString(immeuble.getDateDerniereMutation());
		this.derniereMutation = getMessage(messageSource, "option.rf.type.mutation.", immeuble.getDerniereMutation());
		this.lienRF = immeuble.getLienRegistreFoncier();
	}

	private String formatNumber(Integer number) {
		return number != null ? decimalFormat.get().format(number) : null;
	}

	private static String getMessage(MessageSource messageSource, String keyPrefix, Enum<?> key) {
		if (key == null) {
			return null;
		}
		return messageSource.getMessage(keyPrefix + key.name(), null, WebContextUtils.getDefaultLocale());
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

	public String getEstimationFiscale() {
		return estimationFiscale;
	}

	public String getReferenceEstimationFiscale() {
		return referenceEstimationFiscale;
	}

	public String getTypeImmeuble() {
		return typeImmeuble;
	}

	public String getGenrePropriete() {
		return genrePropriete;
	}

	public String getPartPropriete() {
		return partPropriete;
	}

	public String getDateDernierMutation() {
		return dateDernierMutation;
	}

	public String getDerniereMutation() {
		return derniereMutation;
	}

	public URL getLienRF() {
		return lienRF;
	}
}
