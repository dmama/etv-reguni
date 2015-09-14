package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;

public class Organisation implements Serializable {

	private static final long serialVersionUID = 6382164139477542003L;

	/**
	 * Le numéro technique de l'organisation pour Unireg
	 */
	private final long no;

	@NotNull
	private Map<String, List<DateRanged<String>>> identifiants;

	@NotNull
	private List<DateRanged<String>> nom;
	private List<DateRanged<String>> nomsAdditionels;
	private List<DateRanged<FormeLegale>> formeLegale;

	@NotNull
	private List<DateRanged<Long>> sites;
	@NotNull
	private Map<Long, SiteOrganisation> donneesSites;

	private List<DateRanged<Long>> transfereA;
	private List<DateRanged<Long>> transferDe;
	private List<DateRanged<Long>> remplacePar;
	private List<DateRanged<Long>> enRemplacementDe;

	public Organisation(long no, @NotNull Map<String, List<DateRanged<String>>> identifiants, @NotNull List<DateRanged<String>> nom,
	                    List<DateRanged<String>> nomsAdditionels, List<DateRanged<FormeLegale>> formeLegale, @NotNull List<DateRanged<Long>> sites,
	                    @NotNull Map<Long, SiteOrganisation> donneesSites, List<DateRanged<Long>> transfereA, List<DateRanged<Long>> transferDe,
	                    List<DateRanged<Long>> remplacePar, List<DateRanged<Long>> enRemplacementDe) {
		this.no = no;
		this.identifiants = identifiants;
		this.nom = nom;
		this.nomsAdditionels = nomsAdditionels;
		this.formeLegale = formeLegale;
		this.sites = sites;
		this.donneesSites = donneesSites;
		this.transfereA = transfereA;
		this.transferDe = transferDe;
		this.remplacePar = remplacePar;
		this.enRemplacementDe = enRemplacementDe;
	}

	/**
	 *
	 * @return Le numéro technique de l'organisation pour Unireg
	 */
	public long getNo() {
		return no;
	}

	/**
	 * Prepare une liste de plages représantant la succession des sièges des établissements principaux
	 *
	 * Pour y arriver, pour chaque etablissement (site), on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche le siege qui lui est contemporain.
	 *
	 * On extraie ensuite toute les plages sièges correspondant à la plage type principal.
	 *
	 * @return La succession de plage contenant l'information de siege.
	 */
	public List<DateRanged<Integer>> getSiegesPrincipaux() {
		List<DateRanged<Integer>> sieges = new ArrayList<>();
		for (Map.Entry<Long, SiteOrganisation> entry : donneesSites.entrySet()) {
			SiteOrganisation site =	entry.getValue();
			for (DateRanged<TypeDeSite> type : site.getTypeDeSite()) {
				if (type.getPayload() == TypeDeSite.ETABLISSEMENT_PRINCIPAL && site.getSiege() != null) {
					List<DateRanged<Integer>> extractedSieges = DateRangeHelper.extract(site.getSiege(),
					                                                                    type.getDateDebut(),
					                                                                    type.getDateFin(),
					                                                                    new DateRangeHelper.AdapterCallback<DateRanged<Integer>>() {
						                                                                    @Override
						                                                                    public DateRanged<Integer> adapt(DateRanged<Integer> range, RegDate debut, RegDate fin) {
							                                                                    return new DateRanged<>(debut != null ? debut : range.getDateDebut(),
							                                                                                            fin != null ? fin : range.getDateFin(),
							                                                                                            range.getPayload());
						                                                                    }
					                                                                    });
					sieges.addAll(extractedSieges);
				}
			}
		}
		Collections.sort(sieges, new DateRangeComparator<DateRanged<Integer>>());
		return sieges;
	}

	/**
	 * Retourne l'identifiant OFS de la commune de siège à la date donnée, ou à la date du jour.
	 * si pas de date.
	 *
	 * @param date
	 * @return L'identifiant OFS, ou null si absent
	 */
	public Integer getSiegePrincipal(RegDate date) {
		DateRanged<Integer> siegeRanged = DateRangeHelper.rangeAt(getSiegesPrincipaux(), date != null ? date : RegDate.get());
		return siegeRanged != null ? siegeRanged.getPayload() : null;
	}

	/**
	 * Retourne l'identifiant OFS de la commune de siège à la date donnée, ou à la date du jour.
	 * si pas de date.
	 *
	 * @param date
	 * @return La forme legale, ou null si absente
	 */
	public FormeLegale getFormeLegale(RegDate date) {
		List<DateRanged<FormeLegale>> formeLegaleRanges = getFormeLegale();
		if (formeLegaleRanges != null) {
			DateRanged<FormeLegale> formeLegaleRange = DateRangeHelper.rangeAt(formeLegaleRanges, date != null ? date : RegDate.get());
			return formeLegaleRange != null ? formeLegaleRange.getPayload() : null;
		}
		return null;
	}

	/**
	 * Retourne une liste représantant la succession des valeurs de capital de l'entreprise.
	 *
	 * Pour y arriver, pour chaque etablissement (site), on parcoure la liste des plages de type (principal ou secondaire)
	 * et pour chaque plage principale on recherche la plage de capital qui lui est contemporaine.
	 *
	 * On recrée l'information du capital dans une nouvelle plage aux limites de la plage type principale qui a permis
	 * de la trouver.
	 *
	 * @return La succession de plage contenant l'information de capital.
	 */
	public List<DateRanged<Capital>> getCapitaux() {
		List<DateRanged<Capital>> capitalsValides = new ArrayList<>();
		for (Map.Entry<Long, SiteOrganisation> entry : donneesSites.entrySet()) {
			SiteOrganisation site =	entry.getValue();
			List<DateRanged<Capital>> capitals = site.getRc().getCapital();
			if (capitals != null) {
				for (DateRanged<TypeDeSite> type : site.getTypeDeSite()) {
					if (type.getPayload() == TypeDeSite.ETABLISSEMENT_PRINCIPAL) {
						List<DateRanged<Capital>> extractedSieges = DateRangeHelper.extract(capitals,
						                                                                    type.getDateDebut(),
						                                                                    type.getDateFin(),
						                                                                    new DateRangeHelper.AdapterCallback<DateRanged<Capital>>() {
							                                                                    @Override
							                                                                    public DateRanged<Capital> adapt(DateRanged<Capital> range, RegDate debut, RegDate fin) {
								                                                                    return new DateRanged<>(debut != null ? debut : range.getDateDebut(),
								                                                                                            fin != null ? fin : range.getDateFin(),
								                                                                                            range.getPayload());
							                                                                    }
						                                                                    });
						capitalsValides.addAll(extractedSieges);
					}
				}
			}
		}
		Collections.sort(capitalsValides, new DateRangeComparator<DateRanged<Capital>>());
		return capitalsValides;
	}

	public List<DateRanged<String>> getNoIDE() {
		return getIdentifiants().get("CH.IDE");
	}

	@NotNull
	public Map<Long, SiteOrganisation> getDonneesSites() {
		return donneesSites;
	}

	public List<DateRanged<Long>> getEnRemplacementDe() {
		return enRemplacementDe;
	}

	public List<DateRanged<FormeLegale>> getFormeLegale() {
		return formeLegale;
	}

	@NotNull
	public Map<String, List<DateRanged<String>>> getIdentifiants() {
		return identifiants;
	}

	@NotNull
	public List<DateRanged<String>> getNom() {
		return nom;
	}

	public List<DateRanged<String>> getNomsAdditionels() {
		return nomsAdditionels;
	}

	public List<DateRanged<Long>> getRemplacePar() {
		return remplacePar;
	}

	/**
	 * Permet de connaître les plages de "présence" des sites dans l'organisation.
	 *
	 * @return
	 */
	@NotNull
	public List<DateRanged<Long>> getSites() {
		return sites;
	}

	public List<DateRanged<Long>> getTransferDe() {
		return transferDe;
	}

	public List<DateRanged<Long>> getTransfereA() {
		return transfereA;
	}

	/*
		Setters réservés au Mock
	 */

	protected void setDonneesSites(@NotNull Map<Long, SiteOrganisation> donneesSites) {
		this.donneesSites = donneesSites;
	}

	protected void setEnRemplacementDe(List<DateRanged<Long>> enRemplacementDe) {
		this.enRemplacementDe = enRemplacementDe;
	}

	protected void setFormeLegale(List<DateRanged<FormeLegale>> formeLegale) {
		this.formeLegale = formeLegale;
	}

	protected void setIdentifiants(@NotNull Map<String, List<DateRanged<String>>> identifiants) {
		this.identifiants = identifiants;
	}

	protected void setNom(@NotNull List<DateRanged<String>> nom) {
		this.nom = nom;
	}

	protected void setNomsAdditionels(List<DateRanged<String>> nomsAdditionels) {
		this.nomsAdditionels = nomsAdditionels;
	}

	protected void setRemplacePar(List<DateRanged<Long>> remplacePar) {
		this.remplacePar = remplacePar;
	}

	protected void setSites(@NotNull List<DateRanged<Long>> sites) {
		this.sites = sites;
	}

	protected void setTransferDe(List<DateRanged<Long>> transferDe) {
		this.transferDe = transferDe;
	}

	protected void setTransfereA(List<DateRanged<Long>> transfereA) {
		this.transfereA = transfereA;
	}
}
