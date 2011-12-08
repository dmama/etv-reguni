package ch.vd.uniregctb.rf;

import javax.persistence.Embeddable;

/**
 * Données de référence du registe foncier pour un propriétaire d'immeuble.
 */
@Embeddable
public class Proprietaire {

	private String id;
	private Long numeroIndividu;

	public Proprietaire() {
	}

	public Proprietaire(String id, Long numeroIndividu) {
		this.id = id;
		this.numeroIndividu = numeroIndividu;
	}

	/**
	 * @return l'identifiant unique du propriétaire dans le registre foncier
	 */
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return le numéro d'individu RF dans le registre foncier (à ne pas confondre avec le numéro d'individu du registre civil !)
	 */
	public Long getNumeroIndividu() {
		return numeroIndividu;
	}

	public void setNumeroIndividu(Long numeroIndividu) {
		this.numeroIndividu = numeroIndividu;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final Proprietaire that = (Proprietaire) o;

		if (id != null ? !id.equals(that.id) : that.id != null) return false;
		//noinspection RedundantIfStatement
		if (numeroIndividu != null ? !numeroIndividu.equals(that.numeroIndividu) : that.numeroIndividu != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (numeroIndividu != null ? numeroIndividu.hashCode() : 0);
		return result;
	}
}
