package ch.vd.uniregctb.rt.manager;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.rt.view.DebiteurListView;
import ch.vd.uniregctb.rt.view.RapportPrestationView;
import ch.vd.uniregctb.rt.view.SourcierListView;

public interface RapportPrestationEditManager {

	/**
	 * Alimente la vue RapportPrestationView
	 *
	 * @param numeroSrc
	 * @param numeroDpi
	 * @return
	 * @throws AdressesResolutionException
	 */
	public RapportPrestationView get (Long numeroSrc, Long numeroDpi, String provenance) ;

	/**
	 * Persiste le rapport de travail
	 * @param rapportView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(RapportPrestationView rapportView) ;


	/**
	 * Charge l'écran de recherche débiteurs pour un sourcier
	 *
	 * @param numeroSrc
	 * @return
	 */
	public DebiteurListView getDebiteurList(Long numeroSrc) ;


	/**
	 * Charge l'écran de recherche sourciers pour un debiteur
	 *
	 * @param numeroDpi
	 * @return
	 */
	public SourcierListView getSourcierList(Long numeroDpi) ;
}
