package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.JobResults;

public class ResolutionAdresseResults extends JobResults<Long, ResolutionAdresseResults> {
	public RegDate getDateTraitement() {
		return dateTraitement;
	}


	public static class InfoAdresse {
		public final Long adresseId;
		public final Long tiersId;
		public final Integer rueId;
		public final String nomRue;
		public final String localite;
		public final Integer numeroOrdrePostal;

		public InfoAdresse(Long adresseId, Long tiersId, Integer rueId, String nomRue, String localite, Integer numeroOrdrePostal) {
			this.adresseId = adresseId;
			this.tiersId = tiersId;
			this.rueId = rueId;
			this.nomRue = nomRue;
			this.localite = localite;
			this.numeroOrdrePostal = numeroOrdrePostal;
		}
	}


	public static class Erreur{

		public final Long id;
		public final String raison;

		public Erreur(Long id, String raison) {
			this.id = id;
			this.raison = raison;
		}
	}


	public final RegDate dateTraitement;
	public int nbAdresseTotal;
	public final List<InfoAdresse> listeAdresseResolues = new ArrayList<InfoAdresse>();
	public final List<Erreur> erreurs = new ArrayList<Erreur>();
	public boolean interrompu;

	public ResolutionAdresseResults(RegDate dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	@Override
	public void addErrorException(Long adresseId, Exception e) {
		erreurs.add(new Erreur(adresseId, e.getMessage()));
	}

	@Override
	public void addAll(ResolutionAdresseResults right) {
		this.nbAdresseTotal += right.nbAdresseTotal;
		this.listeAdresseResolues.addAll(right.listeAdresseResolues);
		this.erreurs.addAll(right.erreurs);
	}

	
	public void addAdresseResolue(AdresseSuisse adresseSuisse,String localite) {
		final Long adresseId = adresseSuisse.getId();
		final Long tiersId = adresseSuisse.getTiers().getId();
		final Integer rueId = adresseSuisse.getNumeroRue();
		final String nomRue = adresseSuisse.getRue();
		final Integer numeroOrdrePostal = adresseSuisse.getNumeroOrdrePoste();
		listeAdresseResolues.add(new InfoAdresse(adresseId, tiersId, rueId, nomRue, localite, numeroOrdrePostal));
	}



	public boolean isInterrompu() {
		return interrompu;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}


}