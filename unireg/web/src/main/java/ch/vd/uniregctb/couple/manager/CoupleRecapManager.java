package ch.vd.uniregctb.couple.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.couple.view.CoupleRecapView;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.tiers.MenageCommun;

/**
 *  Methodes pour gerer CoupleRecapController
 *
 * @author xcifde
 *
 */
public interface CoupleRecapManager {

	/**
	 * Alimente la vue CoupleRecapView avec deux membres
	 *
	 * @param numeroPremier
	 * @param numeroSecond
	 * @return
	 */
	@Transactional(readOnly = true)
	public CoupleRecapView get(Long numeroPremierPP, Long numeroSecondPP) ;

	/**
	 * Alimente la vue CoupleRecapView avec un ou deux membres et un contribuable
	 * ouvert et indéterminé qui deviendra le ménage commun par la suite.
	 *
	 * @param numeroPP1 la première personne physique composant le ménage
	 * @param numeroPP2 (optionnel) la seconde personne physique.
	 * @param numeroCTB le numéro du contribuable ouvert.
	 * @return
	 */
	@Transactional(readOnly = true)
	public CoupleRecapView get(Long numeroPremierPP, Long numeroSecondPP, Long numeroCTB);

	/**
	 * Alimente la vue CoupleRecapView avec un seul membre
	 *
	 * @param numeroPremier
	 * @return
	 */
	@Transactional(readOnly = true)
	public CoupleRecapView get(Long numeroPP1);

	/**
	 * Persiste le rapport de travail
	 * @param rapportView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public MenageCommun save(CoupleRecapView coupleRecapView) ;

	/**
	 * Determine si la personne identifiée par son numéro est en ménage commun
	 *
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	public boolean estDejaEnMenage(Long numero) ;

	/**
	 * Renvoie si la PP est majeur ou non lors de la date de mariage
	 *
	 * @param tiersGeneralView
	 * @param dateDebut
	 * @return
	 */
	@Transactional(readOnly = true)
	public boolean isMajeurAt(TiersGeneralView tiersGeneralView, RegDate dateDebut) ;

}
