package ch.vd.uniregctb.rf;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.Contribuable;

public class ImmeubleView {

	private Long id;
	private String dateDebut;
	private String dateFin;
	private String numero;
	private String nature;
	private int estimationFiscale;
	private String dateEstimationFiscale;
	private Integer ancienneEstimationFiscale;
	private GenrePropriete genrePropriete;
	private String partPropriete;
	private Contribuable proprietaire;

	public ImmeubleView(Immeuble immeuble) {
		this.id = immeuble.getId();
		this.dateDebut = RegDateHelper.dateToDisplayString(immeuble.getDateDebut());
		this.dateFin = RegDateHelper.dateToDisplayString(immeuble.getDateFin());
		this.numero = immeuble.getNumero();
		this.nature = immeuble.getNature();
		this.estimationFiscale = immeuble.getEstimationFiscale();
		this.dateEstimationFiscale = RegDateHelper.dateToDisplayString(immeuble.getDateEstimationFiscale());
		this.ancienneEstimationFiscale = immeuble.getAncienneEstimationFiscale();
		this.genrePropriete = immeuble.getGenrePropriete();
		this.partPropriete = immeuble.getPartPropriete().toString();
		this.proprietaire = immeuble.getProprietaire();
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

	public String getNature() {
		return nature;
	}

	public int getEstimationFiscale() {
		return estimationFiscale;
	}

	public String getDateEstimationFiscale() {
		return dateEstimationFiscale;
	}

	public Integer getAncienneEstimationFiscale() {
		return ancienneEstimationFiscale;
	}

	public GenrePropriete getGenrePropriete() {
		return genrePropriete;
	}

	public String getPartPropriete() {
		return partPropriete;
	}

	public Contribuable getProprietaire() {
		return proprietaire;
	}
}
