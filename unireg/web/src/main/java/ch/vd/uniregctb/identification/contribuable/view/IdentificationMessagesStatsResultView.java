package ch.vd.uniregctb.identification.contribuable.view;

public class IdentificationMessagesStatsResultView implements Comparable{
	private String etat;
	private String resultatIdentification;
	private String etatTechnique;
	private String typeMessage;
	private int periode;
	private int nombre;


	public String getEtat() {
		return etat;
	}

	public void setEtat(String etat) {
		this.etat = etat;
	}

	public String getResultatIdentification() {
		return resultatIdentification;
	}

	public void setResultatIdentification(String resultatIdentification) {
		this.resultatIdentification = resultatIdentification;
	}

	public int getNombre() {
		return nombre;
	}

	public void setNombre(int nombre) {
		this.nombre = nombre;
	}

	public int compareTo(Object o) {
		String otherEtat = ((IdentificationMessagesStatsResultView)o).getEtat();
		return etat.compareToIgnoreCase(otherEtat);
	}

	public void setEtatTechnique(String etatTechnique) {
		this.etatTechnique = etatTechnique;
	}

	public String getEtatTechnique() {
		return etatTechnique;
	}

	public void setTypeMessage(String typeMessage) {
		this.typeMessage = typeMessage;
	}

	public String getTypeMessage() {
		return typeMessage;
	}

	public void setPeriode(int periode) {
		this.periode = periode;
	}

	public int getPeriode() {
		return periode;
	}


}
