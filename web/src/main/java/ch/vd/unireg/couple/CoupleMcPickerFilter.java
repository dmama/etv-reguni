package ch.vd.unireg.couple;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.search.SearchTiersFilterWithPostFiltering;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Filtre spécialisé pour l'écran de recherche d'un troisième tiers dans la constitution d'un couple.
 */
public class CoupleMcPickerFilter implements SearchTiersFilterWithPostFiltering {

	private final TiersDAO tiersDAO;

	public CoupleMcPickerFilter(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public String getDescription() {
		return "recherche limitée aux non-habitants avec for principal ouvert sans situation de famille active ni code sexe renseigné et aux ménages communs sans aucun lien d'appartenance ménage";
	}

	@Override
	public TiersCriteria.TypeVisualisation getTypeVisualisation() {
		return TiersCriteria.TypeVisualisation.COMPLETE;
	}

	@Override
	public Set<TiersCriteria.TypeTiers> getTypesTiers() {
		return EnumSet.of(TiersCriteria.TypeTiers.NON_HABITANT,
		                  TiersCriteria.TypeTiers.MENAGE_COMMUN);
	}

	@Override
	public boolean isInclureI107() {
		return false;
	}

	@Override
	public boolean isInclureTiersAnnules() {
		return false;
	}

	@Override
	public boolean isTiersAnnulesSeulement() {
		return false;
	}

	@Override
	public Boolean isTiersActif() {
		return null;
	}

	@Override
	public void postFilter(List<TiersIndexedData> list) {
		for (int i = list.size() - 1; i >= 0; i--) {
			final TiersIndexedData tiersIndexedData = list.get(i);
			final Tiers contribuable = tiersDAO.get(tiersIndexedData.getNumero());

			final boolean valide = isValideCommeTroisiemeTiers(contribuable);
			if (!valide) {
				list.remove(i);
			}
		}
	}

	/**
	 * @param tiers un tiers
	 * @return <b>vrai</b> si le tiers spécifié est valide pour être utilisé comme tiers ménage-commun lors de la création d'un couple.
	 */
	public static boolean isValideCommeTroisiemeTiers(Tiers tiers) {
		final boolean valide;
		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			// seulement les non-habitants (en fait, UNIREG-3264, il s'agit des inconnus CdH) ouverts et indéterminés doivent être affichés
			valide = (!pp.isConnuAuCivil() && pp.getSexe() == null && pp.getSituationFamilleActive() == null && pp.getForFiscalPrincipalAt(null) != null);
		}
		else if (tiers instanceof MenageCommun) {
			final MenageCommun menage = (MenageCommun) tiers;

			// [UNIREG-1212], [UNIREG-1881] Seuls les ménages communs ne possédant aucun lien d'appartenance ménage non-annulé sont considérés valides
			// [SIFISC-20656] dans les cas de réconciliation, on peut avoir à reprendre des cas où il existe des rapports non-annulés fermés
			valide = menage.getRapportsObjet().stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(r -> r.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE)
					.noneMatch(r -> r.getDateFin() == null);
		}
		else {
			valide = false;
		}
		return valide;
	}
}
