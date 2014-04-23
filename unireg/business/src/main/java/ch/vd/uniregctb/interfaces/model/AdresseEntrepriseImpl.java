package ch.vd.uniregctb.interfaces.model;

import java.io.Serializable;
import java.util.Date;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.PaysImpl;
import ch.vd.uniregctb.interfaces.model.helper.EntrepriseHelper;
import ch.vd.uniregctb.type.TypeAdressePM;

public class AdresseEntrepriseImpl implements AdresseEntreprise, Serializable {

	private static final long serialVersionUID = 2830120339685146006L;
	
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final PaysImpl pays;
	private final String complement;
	private final Integer numeroTechniqueRue;
	private final String localiteAbregeMinuscule;
	private final String localiteCompletMinuscule;
	private final String numeroMaison;
	private final int numeroOrdrePostal;
	private final String numeroPostal;
	private final String numeroPostalComplementaire;
	private final String rue;
	private final TypeAdressePM type;

	/**
	 * Crée une adresse d'entreprise Unireg à partir de l'adresse d'entreprise Host-interfaces.
	 *
	 * @param target l'adresse d'entreprise Host-interfaces
	 * @return l'adresse d'entreprise Unireg correspondante; ou <b>null</b> si l'adresse fournie est elle-même nulle ou si elle est située entièrement dans le futur (SIFISC-4625).
	 */
	@Nullable
	public static AdresseEntrepriseImpl get(ch.vd.registre.pm.model.AdresseEntreprise target) {
		if (target == null) {
			return null;
		}
		final RegDate today = RegDate.get();
		final AdresseEntrepriseImpl a = new AdresseEntrepriseImpl(target, today);
		if (a.getDateDebutValidite() != null && a.getDateDebutValidite().isAfter(today)) {
			// [SIFISC-4625] les adresses dans le futur sont ignorées
			return null;
		}
		return a;
	}

	private AdresseEntrepriseImpl(ch.vd.registre.pm.model.AdresseEntreprise target, RegDate today) {
		this.dateDebut = EntrepriseHelper.get(target.getDateDebutValidite());
		this.dateFin = initDateFin(target.getDateFinValidite(), today);
		this.pays = PaysImpl.get(target.getPays());
		this.complement = target.getComplement();
		this.numeroTechniqueRue = target.getNumeroTechniqueRue();
		this.localiteAbregeMinuscule = target.getLocaliteAbregeMinuscule();
		this.localiteCompletMinuscule = target.getLocaliteCompletMinuscule();
		this.numeroMaison = target.getNumeroMaison();
		this.numeroOrdrePostal = target.getNumeroOrdrePostal();
		this.numeroPostal = target.getNumeroPostal();
		this.numeroPostalComplementaire = target.getNumeroPostalComplementaire();
		this.rue = target.getRue();
		this.type = initTypeAdresse(target.getType());
	}

	private static TypeAdressePM initTypeAdresse(EnumTypeAdresseEntreprise type) {
		if (type == null) {
			return null;
		}
		if (type == EnumTypeAdresseEntreprise.COURRIER) {
			return TypeAdressePM.COURRIER;
		}
		else if (type == EnumTypeAdresseEntreprise.SIEGE) {
			return TypeAdressePM.SIEGE;
		}
		else if (type == EnumTypeAdresseEntreprise.FACTURATION) {
			return TypeAdressePM.FACTURATION;
		}
		else {
			throw new IllegalArgumentException("Type d'adresse PM inconnu = [" + type.getName() + ']');
		}
	}

	private static RegDate initDateFin(Date dateFinValidite, RegDate today) {
		final RegDate df = EntrepriseHelper.get(dateFinValidite);
		// [SIFISC-4625] les dates dans le futur sont ignorées
		return df == null || df.isAfter(today) ? null : df;
	}

	@Override
	public String getComplement() {
		return complement;
	}

	@Override
	public Integer getNumeroTechniqueRue() {
		return numeroTechniqueRue;
	}

	@Override
	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFinValidite() {
		return dateFin;
	}

	@Override
	public String getLocaliteAbregeMinuscule() {
		return localiteAbregeMinuscule;
	}

	@Override
	public String getLocaliteCompletMinuscule() {
		return localiteCompletMinuscule;
	}

	@Override
	public Pays getPays() {
		return pays;
	}

	@Override
	public String getNumeroMaison() {
		return numeroMaison;
	}

	@Override
	public int getNumeroOrdrePostal() {
		return numeroOrdrePostal;
	}

	@Override
	public String getNumeroPostal() {
		return numeroPostal;
	}

	@Override
	public String getNumeroPostalComplementaire() {
		return numeroPostalComplementaire;
	}

	@Override
	public String getRue() {
		return rue;
	}

	@Override
	public TypeAdressePM getType() {
		return type;
	}

}
