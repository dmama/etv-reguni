package ch.vd.uniregctb.tiers.view;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.general.view.TiersGeneralView;

public class RapportsPrestationView {

	public static class Rapport implements Annulable {

		public Long id;
		public boolean annule;
		public RegDate dateDebut;
		public RegDate dateFin;
		public Long noCTB;
		public List<String> nomCourrier;
		public String noAVS;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public boolean isAnnule() {
			return annule;
		}

		public void setAnnule(boolean annule) {
			this.annule = annule;
		}

		public RegDate getDateDebut() {
			return dateDebut;
		}

		public void setDateDebut(RegDate dateDebut) {
			this.dateDebut = dateDebut;
		}

		public RegDate getDateFin() {
			return dateFin;
		}

		public void setDateFin(RegDate dateFin) {
			this.dateFin = dateFin;
		}

		public Long getNoCTB() {
			return noCTB;
		}

		public void setNoCTB(Long noCTB) {
			this.noCTB = noCTB;
		}

		public List<String> getNomCourrier() {
			return nomCourrier;
		}

		public void setNomCourrier(List<String> nomCourrier) {
			this.nomCourrier = nomCourrier;
		}

		public String getNoAVS() {
			return noAVS;
		}

		public void setNoAVS(String noAVS) {
			this.noAVS = noAVS;
		}
	}

	// Données vraiment utiles
	public long idDpi;
	public List<Rapport> rapports;
	public boolean editionAllowed;

	// Données pour le bandeau
	public TiersGeneralView tiersGeneral;

	public long getIdDpi() {
		return idDpi;
	}

	public void setIdDpi(long idDpi) {
		this.idDpi = idDpi;
	}

	public List<Rapport> getRapports() {
		return rapports;
	}

	public void setRapports(List<Rapport> rapports) {
		this.rapports = rapports;
	}

	public boolean isEditionAllowed() {
		return editionAllowed;
	}

	public void setEditionAllowed(boolean editionAllowed) {
		this.editionAllowed = editionAllowed;
	}

	public TiersGeneralView getTiersGeneral() {
		return tiersGeneral;
	}

	public void setTiersGeneral(TiersGeneralView tiersGeneral) {
		this.tiersGeneral = tiersGeneral;
	}
}
