package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.RegDate;

/**
 * Contient les informations disponibles sur les fors fiscaux d'un contribuable lors de l'apparition d'un événement (un événement correspondant à une ouverture ou une fermeture de for fiscal).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class EvenementFors {

	/**
	 * La date de l'événement.
	 */
	public final RegDate dateEvenement;

	/**
	 * Les fors fiscaux ouverts précisement à la date de l'événement.
	 */
	public final ForsAt ouverts;

	/**
	 * Les fors fiscaux fermés précisement à la date de l'événement.
	 */
	public final ForsAt fermes;

	/**
	 * Les fors fiscaux actifs à la date de l'événement.
	 */
	public final ForsAt actifs;

	/**
	 * Les fors fiscaux actifs le jour d'avant la date de l'événement.
	 */
	public final ForsAt actifsVeille;

	/**
	 * Les fors fiscaux actifs le jour d'avant la date de l'événement.
	 */
	public final ForsAt actifsLendemain;

	public EvenementFors(RegDate dateEvenement, ForsAt ouverts, ForsAt fermes, ForsAt actifs, ForsAt actifsVeille, ForsAt actifsLendemain) {
		this.dateEvenement = dateEvenement;
		this.ouverts = ouverts;
		this.fermes = fermes;
		this.actifs = actifs;
		this.actifsVeille = actifsVeille;
		this.actifsLendemain = actifsLendemain;
	}
}
