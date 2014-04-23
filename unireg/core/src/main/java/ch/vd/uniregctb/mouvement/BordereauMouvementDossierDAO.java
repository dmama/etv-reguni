package ch.vd.uniregctb.mouvement;

import java.util.List;

import ch.vd.registre.base.dao.GenericDAO;

public interface BordereauMouvementDossierDAO extends GenericDAO<BordereauMouvementDossier, Long> {

	/**
	 * Retourne les bordereaux de dossiers d'envois d'OID à OID pour lesquels
	 * il y a au moins un mouvement dans l'état "traité" (en opposition à
	 * "bordereau_reçu")
	 * @param noCollAdmReceptrice si assigné, ne prend que les bordereaux d'envoi qui sont envoyés vers la collectivité administrative donnée
	 * @return Les bordereaux concernés
	 */
	List<BordereauMouvementDossier> getBordereauxAReceptionner(Integer noCollAdmReceptrice);

}
