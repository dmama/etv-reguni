package ch.vd.unireg.supergra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.unireg.supergra.delta.Delta;

/**
 * Données stockées dans la session Web pour le mode d'édition SuperGra.
 */
public class SuperGraSession {

	/**
	 * Les deltas (= changements d'attributs, ajout d'entités, ...) déjà enregistrés mais non encore commités
	 */
	private List<Delta> deltas = new ArrayList<>();

	/**
	 * Les états des entités principales (tiers, immeubles, bâtiments, ...) modifiées dans la session courante.
	 */
	private List<EntityState> entityStates = new ArrayList<>();

	private final Options options = new Options();

	/**
	 * L'id du dernier tiers affiché
	 */
	private Long lastKnownTiersId;

	/**
	 * La clé de la dernière entité top affichée.
	 */
	private EntityKey lastKnownTopEntity;

	public List<Delta> getDeltas() {
		return Collections.unmodifiableList(deltas);
	}

	public int deltaSize() {
		return deltas.size();
	}

	public void addDelta(Delta delta) {
		if (this.deltas == null) {
			this.deltas = new ArrayList<>();
		}
		this.deltas.add(delta);
	}

	public void addDeltas(List<? extends Delta> deltas) {
		if (this.deltas == null) {
			this.deltas = new ArrayList<>();
		}
		this.deltas.addAll(deltas);
	}

	public Delta removeDelta(int index) {
		return deltas.remove(index);
	}

	public void clearDeltas() {
		deltas.clear();
	}

	public List<EntityState> getEntityStates() {
		return entityStates;
	}

	public void setEntityStates(List<EntityState> entityStates) {
		this.entityStates = entityStates;
	}

	public Options getOptions() {
		return options;
	}

	public Long getLastKnownTiersId() {
		return lastKnownTiersId;
	}

	public EntityKey getLastKnownTopEntity() {
		return lastKnownTopEntity;
	}

	public void setLastKnownTopEntity(EntityKey lastKnownTopEntity) {
		this.lastKnownTopEntity = lastKnownTopEntity;
		if (lastKnownTopEntity != null && lastKnownTopEntity.getType() == EntityType.Tiers) {
			lastKnownTiersId = lastKnownTopEntity.getId();
		}
	}
}
