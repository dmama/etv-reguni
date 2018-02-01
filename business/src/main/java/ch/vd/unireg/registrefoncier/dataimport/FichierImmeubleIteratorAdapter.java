package ch.vd.unireg.registrefoncier.dataimport;

import java.util.Iterator;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.EigentumAnteil;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.Personstamm;

/**
 * Classe qui transforme un flux de données reçues par callback en des itérateurs traditionnels. Cette classe nécessite deux threads au minimum pour fonctionner : un thread pour envoyer les éléments (Callback) et un thread pour itérer.
 */
public class FichierImmeubleIteratorAdapter implements FichierImmeublesRFParser.Callback {

	// on ne garde que 100 éléments en mémoire, ça semble suffisant
	private static final int QUEUE_SIZE = 100;

	private final QueuedIterator<Grundstueck> immeublesIterator = new QueuedIterator<>(QUEUE_SIZE);
	private final QueuedIterator<EigentumAnteil> droitsIterator = new QueuedIterator<>(QUEUE_SIZE);
	private final QueuedIterator<Personstamm> proprietairesIterator = new QueuedIterator<>(QUEUE_SIZE);
	private final QueuedIterator<Gebaeude> constructionsIterator = new QueuedIterator<>(QUEUE_SIZE);
	private final QueuedIterator<Bodenbedeckung> surfacesIterator = new QueuedIterator<>(QUEUE_SIZE);

	public FichierImmeubleIteratorAdapter() {
	}

	/**
	 * @return un itérateur sur les immeubles.
	 */
	public Iterator<Grundstueck> getImmeublesIterator() {
		return immeublesIterator;
	}

	/**
	 * @return un itérateur sur les droits.
	 */
	public Iterator<EigentumAnteil> getDroitsIterator() {
		return droitsIterator;
	}

	/**
	 * @return un itérateur sur les propriétaires.
	 */
	public Iterator<Personstamm> getProprietairesIterator() {
		return proprietairesIterator;
	}

	/**
	 * @return un itérateur sur les constructions.
	 */
	public Iterator<Gebaeude> getConstructionsIterator() {
		return constructionsIterator;
	}

	/**
	 * @return un itérateur sur les surfaces.
	 */
	public Iterator<Bodenbedeckung> getSurfacesIterator() {
		return surfacesIterator;
	}

	@Override
	public void onImmeuble(@NotNull Grundstueck immeuble) {
		immeublesIterator.put(immeuble);
	}

	@Override
	public void onDroit(EigentumAnteil droit) {
		immeublesIterator.done();
		droitsIterator.put(droit);
	}

	@Override
	public void onProprietaire(@NotNull Personstamm personne) {
		immeublesIterator.done();
		droitsIterator.done();
		proprietairesIterator.put(personne);
	}

	@Override
	public void onBatiment(@NotNull Gebaeude construction) {
		immeublesIterator.done();
		droitsIterator.done();
		proprietairesIterator.done();
		constructionsIterator.put(construction);
	}

	@Override
	public void onSurface(@NotNull Bodenbedeckung surface) {
		immeublesIterator.done();
		droitsIterator.done();
		proprietairesIterator.done();
		constructionsIterator.done();
		surfacesIterator.put(surface);
	}

	@Override
	public void done() {
		immeublesIterator.done();
		droitsIterator.done();
		proprietairesIterator.done();
		constructionsIterator.done();
		surfacesIterator.done();
	}
}
