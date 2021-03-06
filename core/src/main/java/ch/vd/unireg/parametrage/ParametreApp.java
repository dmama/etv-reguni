package ch.vd.unireg.parametrage;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import ch.vd.unireg.common.HibernateEntity;

@Entity
@Table(name = "PARAMETRE")
public class ParametreApp extends HibernateEntity {

	private String nom;
	private String valeur;

	public ParametreApp () {
	}

	public ParametreApp (String nom, String valeur) {
		this.nom = nom;
		this.valeur= valeur;
	}

	@Transient
	@Override
	public Object getKey() {
		return nom;
	}

	@Id
	public String getNom() {
		return nom;
	}
	public void setNom(String nom) {
		this.nom = nom;
	}
	public String getValeur() {
		return valeur;
	}
	public void setValeur(String valeur) {
		this.valeur = valeur;
	}

}
