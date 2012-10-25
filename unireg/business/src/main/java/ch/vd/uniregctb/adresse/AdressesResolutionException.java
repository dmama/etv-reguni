package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.uniregctb.tiers.Tiers;

public class AdressesResolutionException extends AdresseException {

	private static final long serialVersionUID = -1378499728497492149L;

	/**
	 * Numéros de contribuables touchés par le problème
	 */
	private final List<Tiers> tiers = new ArrayList<Tiers>();

	/**
	 * Id des adresses fiscales ayant provoqué l'exception
	 */
	private final List<AdresseTiers> adresses = new ArrayList<AdresseTiers>();

	public AdressesResolutionException(String string) {
		super(string);
	}

	public void addTiers(Tiers o) {
		tiers.add(o);
	}

	public void addAdresse(AdresseTiers o) {
		adresses.add(o);
	}

	public List<Tiers> getTiers() {
		return Collections.unmodifiableList(tiers);
	}

	public List<AdresseTiers> getAdresse() {
		return Collections.unmodifiableList(adresses);
	}
}
