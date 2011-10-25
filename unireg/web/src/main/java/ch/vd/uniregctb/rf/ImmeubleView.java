package ch.vd.uniregctb.rf;

import java.net.URL;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.Contribuable;

@SuppressWarnings({"UnusedDeclaration"})
public class ImmeubleView {

	private Long id;
	private String dateDebut;
	private String dateFin;
	private String numero;
	private String nomCommune;
	private String nature;
	private int estimationFiscale;
	private String referenceEstimationFiscale;
	private GenrePropriete genrePropriete;
	private String partPropriete;
	private Contribuable proprietaire;
	private URL lienRF;

	public ImmeubleView(Immeuble immeuble) {
		this.id = immeuble.getId();
		this.dateDebut = RegDateHelper.dateToDisplayString(immeuble.getDateDebut());
		this.dateFin = RegDateHelper.dateToDisplayString(immeuble.getDateFin());
		this.numero = immeuble.getNumero();
		this.nomCommune = immeuble.getNomCommune();
		this.nature = immeuble.getNature();
		this.estimationFiscale = immeuble.getEstimationFiscale();
		this.referenceEstimationFiscale = immeuble.getReferenceEstimationFiscale();
		this.genrePropriete = immeuble.getGenrePropriete();
		this.partPropriete = immeuble.getPartPropriete().toString();
		this.proprietaire = immeuble.getProprietaire();
		this.lienRF = immeuble.getLienRegistreFoncier();
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

	public GenrePropriete getGenrePropriete() {
		return genrePropriete;
	}

	public String getPartPropriete() {
		return partPropriete;
	}

	public Contribuable getProprietaire() {
		return proprietaire;
	}

	public URL getLienRF() {
		return lienRF;
	}
}
