package ch.vd.uniregctb.tiers;

import java.io.Serializable;
import java.util.Calendar;

public class TiersResume implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1334074654812888367L;

	/**
	 * Le num�ro
	 */
	private Long numero;

	/**
	 * Le nom de courrier1
	 */
	private String nomCourrier1;

	/**
	 * Le nom de courrier2
	 */
	private String nomCourrier2;

	/**
	 * La date de naissance
	 */
	private Calendar dateNaissance;

	/**
	 * Le registre source (PP ou PM)
	 */
	private String registre;

	public Calendar getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(Calendar dateNaissance) {
		this.dateNaissance = dateNaissance;
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

	public Long getNumero() {
		return numero;
	}

	public void setNumero(Long numero) {
		this.numero = numero;
	}

	public String getRegistre() {
		return registre;
	}

	public void setRegistre(String registre) {
		this.registre = registre;
	}
}
