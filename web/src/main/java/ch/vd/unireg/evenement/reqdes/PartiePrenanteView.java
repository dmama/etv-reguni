package ch.vd.unireg.evenement.reqdes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.individu.OrigineView;
import ch.vd.unireg.reqdes.PartiePrenante;
import ch.vd.unireg.reqdes.RolePartiePrenante;
import ch.vd.unireg.type.CategorieEtranger;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.Sexe;

/**
 * Classe de visualisation des données d'une partie prenante
 */
public class PartiePrenanteView implements Serializable {

	private static final long serialVersionUID = 5292644065558986725L;

	private final long id;
	private final NomPrenom nomPrenom;
	private final String nomNaissance;
	private final RegDate dateNaissance;
	private final Sexe sexe;
	private final RegDate dateDeces;
	private final boolean sourceCivile;
	private final Long numeroContribuable;
	private final Long numeroContribuableCree;
	private final String avs;
	private final NomPrenom nomPrenomsMere;
	private final NomPrenom nomPrenomsPere;
	private final EtatCivil etatCivil;
	private final RegDate dateEtatCivil;
	private final RegDate dateSeparation;
	private final Integer ofsPaysNationalite;
	private final CategorieEtranger categorieEtranger;
	private final OrigineView origine;

	private final boolean conjointAutrePartiePrenante;
	private final NomPrenom nomPrenomConjoint;

	private final String texteCasePostale;
	private final Integer casePostale;
	private final String localite;
	private final Integer numeroOrdrePostal;
	private final String numeroPostal;
	private final Integer numeroPostalComplementaire;
	private final Integer ofsPays;
	private final String rue;
	private final String numeroMaison;
	private final String numeroAppartement;
	private final String titre;
	private final Integer ofsCommune;

	private final List<RolePartiePrenanteView> roles;

	public PartiePrenanteView(PartiePrenante source) {
		this.id = source.getId();
		this.nomPrenom = new NomPrenom(source.getNom(), source.getPrenoms());
		this.nomNaissance = source.getNomNaissance();
		this.dateNaissance = source.getDateNaissance();
		this.sexe = source.getSexe();
		this.dateDeces = source.getDateDeces();
		this.sourceCivile = source.isSourceCivile();
		this.numeroContribuable = source.getNumeroContribuable();
		this.numeroContribuableCree = source.getNumeroContribuableCree();
		this.avs = source.getAvs();
		this.nomPrenomsMere = new NomPrenom(source.getNomMere(), source.getPrenomsMere());
		this.nomPrenomsPere = new NomPrenom(source.getNomPere(), source.getPrenomsPere());
		this.etatCivil = source.getEtatCivil();
		this.dateEtatCivil = source.getDateEtatCivil();
		this.dateSeparation = source.getDateSeparation();
		this.ofsPaysNationalite = source.getOfsPaysNationalite();
		this.categorieEtranger = source.getCategorieEtranger();
		this.origine = source.getOrigine() != null ? new OrigineView(source.getOrigine()) : null;

		this.conjointAutrePartiePrenante = source.getConjointPartiePrenante() != null;
		this.nomPrenomConjoint = this.conjointAutrePartiePrenante ? new NomPrenom(source.getConjointPartiePrenante().getNom(), source.getConjointPartiePrenante().getPrenoms()) : new NomPrenom(source.getNomConjoint(), source.getPrenomConjoint());
		this.texteCasePostale = source.getTexteCasePostale();
		this.casePostale = source.getCasePostale();
		this.localite = source.getLocalite();
		this.numeroPostal = source.getNumeroPostal();
		this.numeroOrdrePostal = source.getNumeroOrdrePostal();
		this.numeroPostalComplementaire = source.getNumeroPostalComplementaire();
		this.ofsPays = source.getOfsPays();
		this.rue = source.getRue();
		this.numeroMaison = source.getNumeroMaison();
		this.numeroAppartement = source.getNumeroAppartement();
		this.titre = source.getTitre();
		this.ofsCommune = source.getOfsCommune();

		final Set<RolePartiePrenante> rolesSource = source.getRoles();
		final Set<RolePartiePrenanteView> rolesSet = new HashSet<>(rolesSource.size());     // élimination des doublons
		for (RolePartiePrenante roleSource : rolesSource) {
			rolesSet.add(new RolePartiePrenanteView(roleSource));
		}
		this.roles = new ArrayList<>(rolesSet);
		this.roles.sort(new Comparator<RolePartiePrenanteView>() {
			@Override
			public int compare(RolePartiePrenanteView o1, RolePartiePrenanteView o2) {
				int comparison = compareNullableValues(o1.getType(), o2.getType());
				if (comparison == 0) {
					comparison = compareNullableValues(o1.getModeInscription(), o2.getModeInscription());
					if (comparison == 0) {
						comparison = compareNullableValues(o1.getTypeInscription(), o2.getTypeInscription());
						if (comparison == 0) {
							comparison = Integer.compare(o1.getOfsCommune(), o2.getOfsCommune());
						}
					}
				}
				return comparison;
			}
		});
	}

	private static <T extends Comparable<T>> int compareNullableValues(@Nullable T o1, @Nullable T o2) {
		if (o1 == null && o2 == null) {
			return 0;
		}
		else if (o1 == null) {
			return 1;
		}
		else if (o2 == null) {
			return -1;
		}
		else {
			return o1.compareTo(o2);
		}
	}

	public long getId() {
		return id;
	}

	public NomPrenom getNomPrenom() {
		return nomPrenom;
	}

	public String getNomNaissance() {
		return nomNaissance;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public Sexe getSexe() {
		return sexe;
	}

	public RegDate getDateDeces() {
		return dateDeces;
	}

	public boolean isSourceCivile() {
		return sourceCivile;
	}

	public Long getNumeroContribuable() {
		return numeroContribuable;
	}

	public Long getNumeroContribuableCree() {
		return numeroContribuableCree;
	}

	public String getAvs() {
		return avs;
	}

	public NomPrenom getNomPrenomsMere() {
		return nomPrenomsMere;
	}

	public NomPrenom getNomPrenomsPere() {
		return nomPrenomsPere;
	}

	public EtatCivil getEtatCivil() {
		return etatCivil;
	}

	public RegDate getDateEtatCivil() {
		return dateEtatCivil;
	}

	public RegDate getDateSeparation() {
		return dateSeparation;
	}

	public Integer getOfsPaysNationalite() {
		return ofsPaysNationalite;
	}

	public CategorieEtranger getCategorieEtranger() {
		return categorieEtranger;
	}

	public OrigineView getOrigine() {
		return origine;
	}

	public boolean isConjointAutrePartiePrenante() {
		return conjointAutrePartiePrenante;
	}

	public NomPrenom getNomPrenomConjoint() {
		return nomPrenomConjoint;
	}

	public String getTexteCasePostale() {
		return texteCasePostale;
	}

	public Integer getCasePostale() {
		return casePostale;
	}

	public String getLocalite() {
		return localite;
	}

	public Integer getNumeroOrdrePostal() {
		return numeroOrdrePostal;
	}

	public String getNumeroPostal() {
		return numeroPostal;
	}

	public Integer getNumeroPostalComplementaire() {
		return numeroPostalComplementaire;
	}

	public Integer getOfsPays() {
		return ofsPays;
	}

	public String getRue() {
		return rue;
	}

	public String getNumeroMaison() {
		return numeroMaison;
	}

	public String getNumeroAppartement() {
		return numeroAppartement;
	}

	public String getTitre() {
		return titre;
	}

	public Integer getOfsCommune() {
		return ofsCommune;
	}

	public List<RolePartiePrenanteView> getRoles() {
		return roles;
	}
}
