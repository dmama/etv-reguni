package ch.vd.uniregctb.role.before2016;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.GentilComparator;
import ch.vd.uniregctb.metier.assujettissement.MotifAssujettissement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.MotifRattachement;

/**
 * Informations sur un contribuable pour une commune et une période fiscale données
 */
public abstract class InfoContribuable<T extends InfoContribuable<T>> implements Duplicable<T>, Comparable<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(InfoContribuable.class);

	public enum TypeContribuable {
		ORDINAIRE("vaudois ordinaire"), // --------------------------------------------------------------------------------
		HORS_CANTON("hors canton"), // ----------------------------------------------------------------------------
		HORS_SUISSE("hors Suisse"), // ----------------------------------------------------------------------------
		SOURCE("source"), // ---------------------------------------------------------------------------------
		DEPENSE("dépense"), // -------------------------------------------------------------------------------
		MIXTE("sourcier mixte"), // ----------------------------------------------------------------------------------------
		NON_ASSUJETTI("non-assujetti");

		private final String description;

		TypeContribuable(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	/**
	 * Type d'assujettissement : notons que les modalités sont données
	 * dans un ordre de priorité croissante (POURSUIVI_PF est prioritaire sur TERMINE_DANS_PF,
	 * qui est prioritaire sur NON_ASSUJETTI)
	 */
	public enum TypeAssujettissement {
		NON_ASSUJETTI("Non assujetti"),         // l'assujettissement s'est terminé avant le début de la période fiscale
		TERMINE_DANS_PF("Terminé"),             // l'assujettissement s'est terminé dans la période fiscale
		POURSUIVI_APRES_PF("Poursuivi");        // l'assujetissement existe et se poursuit dans la période fiscale suivante

		private final String description;

		TypeAssujettissement(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public final long noCtb;
	private final String[] adresseEnvoi;
	private final List<InfoFor> fors = new ArrayList<>();

	public InfoContribuable(Contribuable ctb, int annee, AdresseService adresseService) {
		this.noCtb = ctb.getNumero();

		AdresseEnvoiDetaillee adresseEnvoi;
		try {
			adresseEnvoi = adresseService.getAdresseEnvoi(ctb, RegDate.get(annee, 12, 31), TypeAdresseFiscale.COURRIER, false);
		}
		catch (AdresseException e) {
			LOGGER.warn("Résolution de l'adresse du contribuable " + ctb.getNumero() + " impossible", e);
			adresseEnvoi = null;
		}

		this.adresseEnvoi = Optional.ofNullable(adresseEnvoi)
				.map(AdresseEnvoiDetaillee::getLignes)
				.orElse(null);
	}

	/**
	 * Génère un clone (surtout pour la collection des fors qui ne doit pas être modifiée sur l'original
	 * quand on ajoute des éléments sur cette nouvelle structure)
	 * @param original la source des données
	 */
	protected InfoContribuable(T original) {
		this.noCtb = original.noCtb;
		this.adresseEnvoi = original.getAdresseEnvoi();
		fors.addAll(original.getFors());
	}

	public void addFor(InfoFor infoFor) {
		fors.add(infoFor);
	}

	public void copyForsFrom(T other) {
		fors.addAll(other.getFors());
	}

	/**
	 * Comparateur de motifs de rattachement : DOMICILE, ACTIVITE_INDEPENDANTE, IMMEUBLE_PRIVE puis tous les autres de manière indiférenciée
	 */
	private static final Comparator<MotifRattachement> COMPARATOR_MOTIF_RATTACHEMENT = new GentilComparator<>(Arrays.asList(MotifRattachement.DOMICILE, MotifRattachement.ACTIVITE_INDEPENDANTE, MotifRattachement.IMMEUBLE_PRIVE));

	/**
	 * Comparateur par date d'ouverture croissante, puis principal/non-principal, puis motif de rattachement
	 */
	private static final Comparator<InfoFor> COMPARATOR_OUVERTURE = new Comparator<InfoFor>() {
		@Override
		public int compare(InfoFor o1, InfoFor o2) {
			int compare = NullDateBehavior.EARLIEST.compare(o1.dateDebut, o2.dateDebut);
			if (compare == 0) {
				compare = - Boolean.valueOf(o1.forPrincipal).compareTo(o2.forPrincipal);        // principal avant non-principal
				if (compare == 0) {
					compare = COMPARATOR_MOTIF_RATTACHEMENT.compare(o1.motifRattachement, o2.motifRattachement);
					if (compare == 0) {
						compare = NullDateBehavior.EARLIEST.compare(o1.dateOuvertureFor, o2.dateOuvertureFor);
					}
				}
			}
			return compare;
		}
	};

	/**
	 * Comparateur par date de fermeture décroissante, puis principal/non-principal puis motif de rattachement
	 */
	private static final Comparator<InfoFor> COMPARATOR_FERMETURE = new Comparator<InfoFor>() {
		@Override
		public int compare(InfoFor o1, InfoFor o2) {
			int compare = - NullDateBehavior.LATEST.compare(o1.dateFin, o2.dateFin);
			if (compare == 0) {
				compare = - Boolean.valueOf(o1.forPrincipal).compareTo(o2.forPrincipal);        // principal avant non-principal
				if (compare == 0) {
					compare = COMPARATOR_MOTIF_RATTACHEMENT.compare(o1.motifRattachement, o2.motifRattachement);
					if (compare == 0) {
						compare = - NullDateBehavior.LATEST.compare(o1.dateFermetureFor, o2.dateFermetureFor);
					}
				}
			}
			return compare;
		}
	};

	private static final Comparator<InfoFor> COMPARATOR_GESTION = new Comparator<InfoFor>() {
		@Override
		public int compare(InfoFor o1, InfoFor o2) {
			int compare = - NullDateBehavior.LATEST.compare(o1.dateFin, o2.dateFin);
			if (compare == 0) {
				compare = - Integer.compare(o1.typeAssujettissement.ordinal() , o2.typeAssujettissement.ordinal());
				if (compare == 0) {
					compare = - Boolean.valueOf(o1.forPrincipal).compareTo(o2.forPrincipal);        // principal avant non-principal
					if (compare == 0) {
						compare = COMPARATOR_MOTIF_RATTACHEMENT.compare(o1.motifRattachement, o2.motifRattachement);
						if (compare == 0) {
							compare = - NullDateBehavior.LATEST.compare(o1.dateFermetureFor, o2.dateFermetureFor);
						}
					}
				}
			}
			return compare;
		}
	};

	private InfoFor getPremierForSelonComparateur(Comparator<InfoFor> comparator) {
		final InfoFor infoFor;
		if (fors.isEmpty()) {
			infoFor = null;
		}
		else if (fors.size() == 1) {
			infoFor = fors.get(0);
		}
		else {
			final List<InfoFor> aTrier = new ArrayList<>(fors);
			Collections.sort(aTrier, comparator);
			infoFor = aTrier.get(0);
		}
		return infoFor;
	}

	/**
	 * Prends le tout premier for et renvoie sa date d'ouverture et son motif d'ouverture
	 */
	public Pair<RegDate, MotifAssujettissement> getInfosOuverture() {
		final InfoFor forOuverture = getPremierForSelonComparateur(COMPARATOR_OUVERTURE);
		if (forOuverture == null || forOuverture.dateDebut == null) {
			return null;
		}
		else {
			return Pair.of(forOuverture.dateDebut, forOuverture.motifDebut);
		}
	}

	/**
	 * Prend le dernier for et extrait son motif et sa date de fermeture
	 */
	public Pair<RegDate, MotifAssujettissement> getInfosFermeture() {
		final InfoFor forFermeture = getPremierForSelonComparateur(COMPARATOR_FERMETURE);
		if (forFermeture == null || forFermeture.dateFin == null) {
			return null;
		}
		else {
			return Pair.of(forFermeture.dateFin, forFermeture.motifFin);
		}
	}

	public TypeAssujettissement getTypeAssujettissementAgrege() {
		TypeAssujettissement type = TypeAssujettissement.NON_ASSUJETTI;
		for (InfoFor infoFor : fors) {
			final TypeAssujettissement candidat = infoFor.typeAssujettissement;
			if (candidat.ordinal() > type.ordinal()) {
				type = candidat;
			}
		}
		return type;
	}

	public TypeContribuable getTypeCtb() {
		final TypeContribuable typeCtb;
		final InfoFor forGestion = getForGestionFinPeriode();
		if (forGestion == null) {
			typeCtb = null;
		}
		else {
			typeCtb = forGestion.typeCtb;
		}
		return typeCtb;
	}

	public TypeContribuable getAncienTypeContribuable() {
		final TypeContribuable typeCtb;
		final InfoFor forGestion = getForGestionFinPeriode();
		if (forGestion == null) {
			typeCtb = null;
		}
		else {
			typeCtb = forGestion.ancienTypeCtb;
		}
		return typeCtb;
	}

	public int getNoOfsDerniereCommune() {
		final InfoFor forGestion = getForGestionFinPeriode();
		if (forGestion != null) {
			return forGestion.ofsCommune;
		}
		else {
			throw new RuntimeException("Ne devrait pas être appelé sur un contribuable sans aucun for (" + noCtb + ')');
		}
	}

	private InfoFor getForGestionFinPeriode() {
		return getPremierForSelonComparateur(COMPARATOR_GESTION);
	}

	public String[] getAdresseEnvoi() {
		return adresseEnvoi;
	}

	public List<InfoFor> getFors() {
		return fors;
	}
}
