package ch.vd.uniregctb.evenement.common.view;

import java.io.Serializable;
import java.util.List;

import ch.vd.registre.base.date.RegDate;

public class TiersAssocieView implements Serializable {

	private static final long serialVersionUID = 2193252255063921163L;

	private Long numero;

	private List<String> nomCourrier;

	private String localiteOuPays;

	private String forPrincipal;

	private RegDate dateOuvertureFor;

	private RegDate dateFermetureFor;

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public void setNomCourrier(List<String> nomCourrier) {
		this.nomCourrier = nomCourrier;
	}

	public List<String> getNomCourrier() {
		return nomCourrier;
	}

	@SuppressWarnings("unused")
	public String getLocaliteOuPays() {
		return localiteOuPays;
	}

	public void setLocaliteOuPays(String localiteOuPays) {
		this.localiteOuPays = localiteOuPays;
	}

	@SuppressWarnings("unused")
	public String getForPrincipal() {
		return forPrincipal;
	}

	public void setForPrincipal(String forPrincipal) {
		this.forPrincipal = forPrincipal;
	}

	@SuppressWarnings("unused")
	public RegDate getDateOuvertureFor() {
		return dateOuvertureFor;
	}

	public void setDateOuvertureFor(RegDate dateOuvertureFor) {
		this.dateOuvertureFor = dateOuvertureFor;
	}

	@SuppressWarnings("unused")
	public RegDate getDateFermetureFor() {
		return dateFermetureFor;
	}

	public void setDateFermetureFor(RegDate dateFermetureFor) {
		this.dateFermetureFor = dateFermetureFor;
	}

}
