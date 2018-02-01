package ch.vd.unireg.supergra;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.supergra.delta.Delta;
import ch.vd.unireg.supergra.view.CollectionView;
import ch.vd.unireg.supergra.view.EntityView;

/**
 * Manager du mode SuperGra de Unireg. Il est responsable des opérations métier permettant de charger, créer et sauver les entités hibernate manipulées.
 */
public interface SuperGraManager {

	void fillView(EntityKey key, EntityView view, SuperGraSession session);

	void fillView(EntityKey key, String collName, CollectionView view, SuperGraSession session);

	/**
	 * Alloue et retourne le prochain id valable pour une nouvelle entité de la classe spécifiée.
	 *
	 * @param clazz une classe qui représente une entité hibernate
	 * @return le nouvelle id alloué
	 */
	Long nextId(Class<? extends HibernateEntity> clazz);

	/**
	 * Applique les modifications aux entités de la base de données et sauvegarde le tout.
	 *
	 * @param deltas les modifications à appliquer et sauver.
	 */
	void commitDeltas(List<Delta> deltas);

	/**
	 * Transforme une personne physique en ménage-commun.
	 *
	 * @param ppId         l'id de la personne physique à tranformer en ménage-commun
	 * @param dateDebut    la date d'ouverture du futur rapport d'appartenance ménage entre le nouveau ménage et le contribuable principal
	 * @param dateFin      la date de fermeture du futur rapport d'appartenance ménage entre le nouveau ménage et le contribuable principal (optionnelle)
	 * @param idPrincipal  l'id du futur contribuable principal du ménage-commun
	 * @param idSecondaire l'id du futur contribuable secondaire du ménage-commun (optionnel)
	 */
	void transformPp2Mc(long ppId, RegDate dateDebut, @Nullable RegDate dateFin, long idPrincipal, @Nullable Long idSecondaire);

	/**
	 * Transforme un ménage-commun en personne physique
	 *
	 * @param mcId  l'id du ménage-commun à transformer en personne physique
	 * @param indNo le numéro d'individu de la personne physique résultante
	 */
	void transformMc2Pp(long mcId, long indNo);
}
