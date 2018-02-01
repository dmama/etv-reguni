package ch.vd.unireg.role.before2016;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.TiersService;

/**
 * Classe de base du container des données pour les rôles (PP ou PM) d'un ensemble de communes
 * @param <ICTB> Type d'infos contribuables
 * @param <ICOM> Type d'infos communes
 * @param <T> Type de contribuable
 */
public abstract class Roles<ICTB extends InfoContribuable<ICTB>, ICOM extends InfoCommune<ICTB, ICOM>, T extends Contribuable> implements RolesResults<ICOM> {

	private final Map<Integer, ICOM> infosCommunes = new HashMap<>();

	/**
	 * Point d'entrée pour la prise en compte d'un nouveau for
	 * @param infoFor informations sur le for à prendre en compte
	 * @param ctb contribuable concerné
	 * @param assujettissement (optionnel) assujettissement en cours de traitement
	 * @param dateFinAssujettissementPrecedent
	 * @param annee année des rôles
	 * @param noOfsCommune numéro OFS de la commune concernée
	 * @param adresseService le service de calcul d'adresses
	 * @param tiersService le service des tiers
	 */
	public abstract void digestInfoFor(InfoFor infoFor, T ctb, Assujettissement assujettissement, RegDate dateFinAssujettissementPrecedent, int annee, int noOfsCommune, AdresseService adresseService, TiersService tiersService);

	/**
	 * @param contribuable un contribuable
	 * @param tiersService le service des tiers
	 * @return la liste des périodes fiscales (théorique, i.e. qui ne fait pas intervenir un quelconque calcul d'assujettissement) du contribuable dans la période où il possède des fors principaux
	 * (si son dernier for principal est ouvert, la dernière période fiscale fournie correspond à la période fiscale courante)
	 */
	public abstract List<DateRange> getPeriodesFiscales(T contribuable, TiersService tiersService);

	/**
	 * Construit un agrégat des données par contribuables pour ce qui concerne les communes indiquées
	 * @param nosOfsCommunes listes des numéros OFS des communes à considérer
	 * @return les informations pour les contribuables agrégées pour l'ensemble des communes données
	 */
	public List<ICTB> buildInfosPourRegroupementCommunes(Collection<Integer> nosOfsCommunes) {

		final Map<Object, ICTB> map = new HashMap<>();

		// boucle sur chacune des communes demandées
		for (Integer noOfsCommune : nosOfsCommunes) {
			if (noOfsCommune != null) {

				final InfoCommune<ICTB, ICOM> infoCommune = infosCommunes.get(noOfsCommune);
				if (infoCommune != null) {

					// boucle sur tous les contribuables connus dans cette commune
					final Collection<ICTB> infosCtbsCommune = infoCommune.getInfosContribuables();
					for (ICTB infoCtb : infosCtbsCommune) {
						final Object key = buildInfoContribuableKey(infoCtb);
						final ICTB infoDejaConnue = map.get(key);
						if (infoDejaConnue != null) {
							// le contribuable était déjà connu sur une autre commune de la liste -> fusion nécessaire
							infoDejaConnue.copyForsFrom(infoCtb);
						}
						else {
							// il faut cloner l'info pour éviter que les fusions successives ne détruisent les données initiales
							final ICTB clone = infoCtb.duplicate();
							map.put(key, clone);
						}
					}
				}
			}
		}

		final List<ICTB> values = new ArrayList<>(map.values());
		Collections.sort(values);
		return values;
	}

	/**
	 * Fusion dans l'instance courante des données de l'instance passée en paramètre
	 * @param source source des données à fusionner dans l'instance courante
	 */
	public void addAll(Roles<ICTB, ICOM, T> source) {
		for (Map.Entry<Integer, ICOM> e : source.infosCommunes.entrySet()) {
			final ICOM thisInfo = getOrCreateInfoCommune(e.getKey());
			thisInfo.addAll(e.getValue());
		}
	}

	/**
	 * @param infoContribuable informations de contribuable
	 * @return clé de regroupement qui permet de déterminer si deux informations de contribuable doivent être fusionnées (= si même clé)
	 */
	protected abstract Object buildInfoContribuableKey(ICTB infoContribuable);

	/**
	 * @param noOfsCommune un numéro OFS de commune
	 * @return l'information de commune correspondant à ce numéro OFS, créée pour l'occasion si nécessaire
	 * @see #createInfoCommune(int)
	 */
	protected final ICOM getOrCreateInfoCommune(int noOfsCommune) {
		final ICOM commune = infosCommunes.get(noOfsCommune);
		if (commune != null) {
			return commune;
		}
		final ICOM newCommune = createInfoCommune(noOfsCommune);
		infosCommunes.put(noOfsCommune, newCommune);
		return newCommune;
	}

	/**
	 * @param noOfsCommune un numéro OFS de commune
	 * @return une nouvelle instance d'information de commune pour ce numéro OFS
	 */
	protected abstract ICOM createInfoCommune(int noOfsCommune);

	public Set<Integer> getNoOfsCommunesTraitees() {
		return infosCommunes.keySet();
	}

	@Override
	public Map<Integer, ICOM> getInfosCommunes() {
		return infosCommunes;
	}
}
