package ch.vd.uniregctb.rf;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.Contribuable;

public class ImmeubleView {

	private Long id;
	private String dateDebut;
	private String dateFin;
	private String dateMutation;
	private String numero;
	private NatureImmeuble nature;
	private int estimationFiscale;
	private GenrePropriete genrePropriete;
	private String partPropriete;
	private Contribuable proprietaire;

	public ImmeubleView(Immeuble immeuble) {
		this.id = immeuble.getId();
		this.dateDebut = RegDateHelper.dateToDisplayString(immeuble.getDateDebut());
		this.dateFin = RegDateHelper.dateToDisplayString(immeuble.getDateFin());
		this.dateMutation = RegDateHelper.dateToDisplayString(immeuble.getDateMutation());
		this.numero = immeuble.getNumero().toString();
		this.nature = immeuble.getNature();
		this.estimationFiscale = immeuble.getEstimationFiscale();
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

	public String getDateMutation() {
		return dateMutation;
	}

	public String getNumero() {
		return numero;
	}

	public NatureImmeuble getNature() {
		return nature;
	}

	public int getEstimationFiscale() {
		return estimationFiscale;
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
