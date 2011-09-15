package ch.vd.uniregctb.interfaces.model;

public interface Origine {

	/**
	 * Retourne le nom du lieu d'origine. Il s'agit d'un texte libre qui ne doit servir qu'à l'affichage et en aucun cas
	 * à des décisions métier automatiques
	 * @return le nom du lieu (commune, localité, pays ?...) d'origine.
	 */
	String getNomLieu();
}
