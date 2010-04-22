package ch.vd.uniregctb.tiers.view;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.type.TypeActivite;

public class RapportsPrestationView {

	public static class Rapport {

		public Long id;
		public boolean annule;
		public RegDate dateDebut;
		public RegDate dateFin;
		public TypeActivite typeActivite;
		public Integer tauxActivite;
		public Long noCTB;
		public String nomCourrier1;
		public String nomCourrier2;
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

		public TypeActivite getTypeActivite() {
			return typeActivite;
		}

		public void setTypeActivite(TypeActivite typeActivite) {
			this.typeActivite = typeActivite;
		}

		public Integer getTauxActivite() {
			return tauxActivite;
		}

		public void setTauxActivite(Integer tauxActivite) {
			this.tauxActivite = tauxActivite;
		}

		public Long getNoCTB() {
			return noCTB;
		}

		public void setNoCTB(Long noCTB) {
			this.noCTB = noCTB;
		}

		public String getNomCourrier1() {
			return nomCourrier1;
		}

		public void setNomCourrier1(String nomCourrier1) {
			this.nomCourrier1 = nomCourrier1;
		}

		public String getNomCourrier2() {
			return nomCourrier2;
		}

		public void setNomCourrier2(String nomCourrier2) {
			this.nomCourrier2 = nomCourrier2;
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
	private String urlSipf;
	private String urlTaoPP;
	private String urlTaoBA;
	private String urlTaoIS;
	private String urlCAT;


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

	public String getUrlSipf() {
		return urlSipf;
	}

	public void setUrlSipf(String urlSipf) {
		this.urlSipf = urlSipf;
	}

	public String getUrlTaoPP() {
		return urlTaoPP;
	}

	public void setUrlTaoPP(String urlTaoPP) {
		this.urlTaoPP = urlTaoPP;
	}

	public String getUrlTaoBA() {
		return urlTaoBA;
	}

	public void setUrlTaoBA(String urlTaoBA) {
		this.urlTaoBA = urlTaoBA;
	}

	public String getUrlTaoIS() {
		return urlTaoIS;
	}

	public void setUrlTaoIS(String urlTaoIS) {
		this.urlTaoIS = urlTaoIS;
	}

	public String getUrlCAT() {
		return urlCAT;
	}

	public void setUrlCAT(String urlCAT) {
		this.urlCAT = urlCAT;
	}
}
