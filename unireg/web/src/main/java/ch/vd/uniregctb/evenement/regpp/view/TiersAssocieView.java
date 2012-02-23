package ch.vd.uniregctb.evenement.regpp.view;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.NomCourrierViewPart;

public class TiersAssocieView {

	private Long numero;

	private final NomCourrierViewPart nomCourrier = new NomCourrierViewPart();

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
		this.nomCourrier.setNomCourrier(nomCourrier);
	}

	public String getNomCourrier1() {
		return this.nomCourrier.getNomCourrier1();
	}

	public void setNomCourrier1(String nomCourrier1) {
		this.nomCourrier.setNomCourrier1(nomCourrier1);
	}

	public String getNomCourrier2() {
		return this.nomCourrier.getNomCourrier2();
	}

	public void setNomCourrier2(String nomCourrier2) {
		this.nomCourrier.setNomCourrier2(nomCourrier2);
	}

	public String getLocaliteOuPays() {
		return localiteOuPays;
	}

	public void setLocaliteOuPays(String localiteOuPays) {
		this.localiteOuPays = localiteOuPays;
	}

	public String getForPrincipal() {
		return forPrincipal;
	}

	public void setForPrincipal(String forPrincipal) {
		this.forPrincipal = forPrincipal;
	}

	public RegDate getDateOuvertureFor() {
		return dateOuvertureFor;
	}

	public void setDateOuvertureFor(RegDate dateOuvertureFor) {
		this.dateOuvertureFor = dateOuvertureFor;
	}

	public RegDate getDateFermetureFor() {
		return dateFermetureFor;
	}

	public void setDateFermetureFor(RegDate dateFermetureFor) {
		this.dateFermetureFor = dateFermetureFor;
	}

}
