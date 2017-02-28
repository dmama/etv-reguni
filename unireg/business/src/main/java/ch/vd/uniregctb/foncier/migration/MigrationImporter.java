package ch.vd.uniregctb.foncier.migration;

import java.util.Collection;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;

public abstract class MigrationImporter {

	private final ImmeubleRFDAO immeubleRFDAO;

	public MigrationImporter(ImmeubleRFDAO immeubleRFDAO) {
		this.immeubleRFDAO = immeubleRFDAO;
	}

	protected static String canonizeName(String name) {
		return name.replaceAll("[-.()]", " ").replaceAll("[\\s]+", " ").trim().toLowerCase();
	}

	@NotNull
	protected ImmeubleRF determinerImmeuble(MigrationKey key, Map<String, Commune> mapCommunes, DonneesFusionsCommunes fusionData) throws ObjectNotFoundException {
		final Commune commune = mapCommunes.get(canonizeName(key.nomCommune));
		if (commune == null) {
			throw new ObjectNotFoundException("La commune avec le nom [" + key.nomCommune + "] n'existe pas.");
		}

		final MigrationParcelle parcelle;
		try {
			parcelle = MigrationParcelle.valueOf(key.noBaseParcelle, key.noParcelle, key.noLotPPE);
		}
		catch (RuntimeException e) {
			throw new IllegalArgumentException("Impossible de parser le numéro de parcelle : " + e.getMessage());
		}

		return findImmeuble(commune, parcelle, mapCommunes, fusionData);
	}

	/**
	 * Ici, si la commune de l'immeuble est associée à des données de fusion, on va d'abord éventuellement déterminer la nouvelle
	 * commune et le nouveau numéro de parcelle
	 * @param commune commune de l'immeuble
	 * @param parcelle numéro de parcelle
	 * @param mapCommunes map des communes indexées par nom "canonique"
	 * @param fusionData données des fusions de communes avec renumérotation parcellaire
	 * @return l'immeuble trouvé
	 */
	@NotNull
	private ImmeubleRF findImmeuble(Commune commune, MigrationParcelle parcelle, Map<String, Commune> mapCommunes, DonneesFusionsCommunes fusionData) {
		final DonneesFusionsCommunes.OffsetCommune offset = fusionData.getOffsetAndDestination(commune.getNoOFS());
		if (offset == null) {
			// pas de données de fusion, on fait au plus simple
			return findImmeuble(commune, parcelle, mapCommunes);
		}

		// lors de la fusion, il y a peut-être eu un offset appliqué sur les numéros de parcelle
		final MigrationParcelle nouvelleParcelle = parcelle.withNoParcelle(parcelle.getNoParcelle() + offset.offset);

		// si le numéro OFS a été conservé mais les parcelles renumérotées, il faut faire attention à ne pas partir en récursion infinie...
		if (offset.ofsCommuneDestination == commune.getNoOFS()) {
			// algo de base avec juste un numéro de parcelle modifié
			return findImmeuble(commune, nouvelleParcelle, mapCommunes);
		}
		else {
			// fusion avec changement de numéro OFS de commune
			final Commune nouvelleCommune = findCommune(offset.ofsCommuneDestination, mapCommunes.values());
			if (nouvelleCommune == null) {
				// bizarre, le fichier d'entrée est pourri ?
				throw new ObjectNotFoundException("La commune avec le numéro OFS " + offset.ofsCommuneDestination + " (fourni dans le fichier des fusions) est introuvable !");
			}

			// appel récursif avec nouvelle commune et nouveau numéro de parcelle
			return findImmeuble(nouvelleCommune, nouvelleParcelle, mapCommunes, fusionData);
		}
	}

	/**
	 * Ici, on cherche l'immeuble sur la commune donnée avec le numéro de parcelle donné, avec une tentative de rattrapage par rapport aux fractions de communes vaudoises
	 * @param commune commune de l'immeuble
	 * @param parcelle numéro de parcelle
	 * @param mapCommunes map des communes indexées par nom "canonique"
	 * @return l'immeuble trouvé
	 */
	@NotNull
	private ImmeubleRF findImmeuble(Commune commune, MigrationParcelle parcelle, Map<String, Commune> mapCommunes) {
		try {
			return IdentificationImmeubleHelper.findImmeuble(immeubleRFDAO, commune, parcelle);
		}
		catch (ImmeubleNotFoundException e) {
			// [SIFISC-23185] peut-être que l'immeuble n'est connu que sur la commune faîtière dans le RF...
			if (commune.isFraction()) {
				final Commune communeFaitiere = mapCommunes.values().stream()
						.filter(Commune::isPrincipale)
						.filter(c -> c.getNoOFS() == commune.getOfsCommuneMere())
						.findFirst()
						.orElse(null);
				if (communeFaitiere != null) {
					try {
						return IdentificationImmeubleHelper.findImmeuble(immeubleRFDAO, communeFaitiere, parcelle);
					}
					catch (ImmeubleNotFoundException ex) {
						// pas trouvé non plus... on laisse passer et on renvoie l'exception initiale
					}
				}
			}

			// pas mieux, on laisse passer...
			throw e;
		}
	}

	@Nullable
	private Commune findCommune(int noOfs, Collection<Commune> communes) {
		return communes.stream()
				.filter(c -> c.getNoOFS() == noOfs)
				.findFirst()
				.orElse(null);
	}
}
