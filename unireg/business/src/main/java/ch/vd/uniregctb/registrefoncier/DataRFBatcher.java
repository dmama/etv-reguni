package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;

/**
 * Cette classe reçoit les données du parser de données du registre foncier et les regroupes par lots de taille déterminée avant de les envoyer plus loin.
 */
public class DataRFBatcher implements FichierImmeublesRFParser.Callback {

	private final List<Grundstueck> immeubles;
	private final List<PersonEigentumAnteil> droits;
	private final List<Personstamm> proprietaires;
	private final List<Gebaeude> batiments;
	private final List<Bodenbedeckung> surfaces;

	private final int batchSize;
	private final Callback callback;

	public DataRFBatcher(int batchSize, Callback callback) {
		this.immeubles = new ArrayList<>(batchSize);
		this.droits = new ArrayList<>(batchSize);
		this.proprietaires = new ArrayList<>(batchSize);
		this.batiments = new ArrayList<>(batchSize);
		this.surfaces = new ArrayList<>(batchSize);
		this.batchSize = batchSize;
		this.callback = callback;
	}

	/**
	 * Interface orientée-événement pour recevoir les entités RF regroupées par groupe de taille détermineé.
	 * <p/>
	 * L'ordre de réception des données est le suivant :
	 * <ul>
	 * <li>Immeubles</li>
	 * <li>Droits</li>
	 * <li>Propriétaires</li>
	 * <li>Bâtiments</li>
	 * <li>Surfaces au sol</li>
	 * </ul>
	 */
	public static interface Callback {

		void onImmeubles(@NotNull List<Grundstueck> immeubles);

		void onDroits(@NotNull List<PersonEigentumAnteil> droits);

		void onProprietaires(@NotNull List<Personstamm> personnes);

		void onBatiments(@NotNull List<Gebaeude> batiments);

		void onSurfaces(@NotNull List<Bodenbedeckung> surfaces);

		/**
		 * Méthode appelée lorsque toutes les données ont été envoyées.
		 */
		void done();
	}

	@Override
	public void onImmeuble(@NotNull Grundstueck immeuble) {
		if (immeubles.size() == batchSize) {
			flushImmeubles();
		}
		immeubles.add(immeuble);
	}

	@Override
	public void onDroit(@NotNull PersonEigentumAnteil droit) {
		if (!immeubles.isEmpty()) {
			flushImmeubles();
		}
		if (droits.size() == batchSize) {
			flushDroits();
		}
		droits.add(droit);
	}

	@Override
	public void onProprietaire(@NotNull Personstamm personne) {
		if (!immeubles.isEmpty()) {
			flushImmeubles();
		}
		if (!droits.isEmpty()) {
			flushDroits();
		}
		if (proprietaires.size() == batchSize) {
			flushProprietaires();
		}
		proprietaires.add(personne);
	}

	@Override
	public void onBatiment(@NotNull Gebaeude batiment) {
		if (!immeubles.isEmpty()) {
			flushImmeubles();
		}
		if (!droits.isEmpty()) {
			flushDroits();
		}
		if (!proprietaires.isEmpty()) {
			flushProprietaires();
		}
		if (batiments.size() == batchSize) {
			flushBatiments();
		}
		batiments.add(batiment);
	}

	@Override
	public void onSurface(@NotNull Bodenbedeckung surface) {
		if (!immeubles.isEmpty()) {
			flushImmeubles();
		}
		if (!droits.isEmpty()) {
			flushDroits();
		}
		if (!proprietaires.isEmpty()) {
			flushProprietaires();
		}
		if (!batiments.isEmpty()) {
			flushBatiments();
		}
		if (surfaces.size() == batchSize) {
			flushSurfaces();
		}
		surfaces.add(surface);
	}

	@Override
	public void done() {
		if (!immeubles.isEmpty()) {
			flushImmeubles();
		}
		if (!droits.isEmpty()) {
			flushDroits();
		}
		if (!proprietaires.isEmpty()) {
			flushProprietaires();
		}
		if (!batiments.isEmpty()) {
			flushBatiments();
		}
		if (!surfaces.isEmpty()) {
			flushSurfaces();
		}
		callback.done();
	}

	private void flushImmeubles() {
		callback.onImmeubles(new ArrayList<>(immeubles));   // copie de la liste au cas où le callback garde une référence
		immeubles.clear();
	}

	private void flushDroits() {
		callback.onDroits(new ArrayList<>(droits)); // copie de la liste au cas où le callback garde une référence
		droits.clear();
	}

	private void flushProprietaires() {
		callback.onProprietaires(new ArrayList<>(proprietaires));   // copie de la liste au cas où le callback garde une référence
		proprietaires.clear();
	}

	private void flushBatiments() {
		callback.onBatiments(new ArrayList<>(batiments));   // copie de la liste au cas où le callback garde une référence
		batiments.clear();
	}

	private void flushSurfaces() {
		callback.onSurfaces(new ArrayList<>(surfaces)); // copie de la liste au cas où le callback garde une référence
		surfaces.clear();
	}
}
