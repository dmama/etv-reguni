package ch.vd.uniregctb.interfaces.model;

/**
 * Identification unique d'une commune
 * <p/>
 * <b>Note:</b> normalement le numéro Ofs est suffisant pour déterminer une commune de manière unique. Il y a malheureusement une exception : l'Ofs a attribué le même numéro à Lussery (avant fusion)
 * qu'à Lussery-Villars (après fusion). Le numéro technique est donc nécessaire pour distinguer ces deux communes.
 */
public interface CommuneId {

	/**
	 * @return le numéro Ofs de la commune.
	 */
	long getNoOfs();

	/**
	 * @return le numéro technique de la commune (uniquement nécessaire pour distinguer les communes de Lussery et Lussery-Villars qui partagent le même numéro Ofs = 5487).
	 */
	int getNumeroTechnique();
}
