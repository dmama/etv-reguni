package ch.vd.uniregctb.etiquette;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.tiers.CollectiviteAdministrative;

/**
 * Service de gestion des étiquettes et de leur assignation temporelles à des tiers
 */
public interface EtiquetteService {

	/**
	 * @return la liste de toutes les étiquettes existantes
	 */
	List<Etiquette> getAllEtiquettes();

	/**
	 * @param id identifiant de l'étiquette
	 * @return l'étiquette correspondant à l'identifiant, ou <code>null</code> s'il n'y en a pas
	 */
	Etiquette getEtiquette(long id);

	/**
	 * Création d'une nouvelle étiquette
	 * @param code un code (unique)
	 * @param libelle un libellé (d'affichage)
	 * @param collectiviteAdministrative (optionnel) une collectivité administrative liée
	 * @return la nouvelle étiquette
	 */
	Etiquette newEtiquette(String code, String libelle, @Nullable CollectiviteAdministrative collectiviteAdministrative);

}
