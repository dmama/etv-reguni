package ch.vd.unireg.registrefoncier.dao;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.ModeleCommunauteRF;

public interface ModeleCommunauteRFDAO extends GenericDAO<ModeleCommunauteRF, Long> {

	/**
	 * Recherche le modèle de communauté qui correspond aux membres spécifiés.
	 *
	 * @param membres les membres de la communauté
	 * @return le modèle trouvé; ou <b>null</b> si aucun modèle existant ne correspond.
	 */
	@Nullable
	ModeleCommunauteRF findByMembers(@NotNull Set<? extends AyantDroitRF> membres);

	/**
	 * Crée un nouveau modèle de communauté basé sur la liste des membres spécifiés.
	 * <p/>
	 * <b>Attention !</b> Cette méthode ne vérifie pas l'existance d'un modèle préexistant.
	 *
	 * @param membresIds les ids des membres de la communauté.
	 * @return l'id du modèle de communauté créé.
	 */
	long createWith(Set<Long> membresIds);
}
