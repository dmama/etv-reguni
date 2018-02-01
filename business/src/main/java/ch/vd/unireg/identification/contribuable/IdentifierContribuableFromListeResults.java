package ch.vd.unireg.identification.contribuable;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.unireg.tiers.TiersService;

public class IdentifierContribuableFromListeResults extends JobResults<CriteresPersonne, IdentifierContribuableFromListeResults> {
	public RegDate getDateTraitement() {
		return dateTraitement;
	}


	public static class InfoMessage {
		public final CriteresPersonne criteres;


		public InfoMessage(CriteresPersonne critere) {
			this.criteres = critere;
		}



	}


	public static class Erreur  {

		public final CriteresPersonne criteres;
		public final String raison;

		public Erreur(CriteresPersonne criteres, String raison) {

			this.raison = raison;
			this.criteres = criteres;
		}


	}

	public static class Identifie extends InfoMessage {
		public final Long noCtb;
		public final Long noCtbMenage;

		public Identifie(CriteresPersonne criteres, Long noCtb, Long noCtbMenage) {
			super(criteres);
			this.noCtb = noCtb;
			this.noCtbMenage = noCtbMenage;

		}
	}

	public static class PlusieursTrouves extends InfoMessage {
		public final List<Long> trouves;

		public PlusieursTrouves(CriteresPersonne criteres, List<Long> trouves) {
			super(criteres);
			this.trouves = trouves;

		}
	}

	public static class NonIdentifie extends InfoMessage {
		public final String raison = "Identification automatique sans resultat";


		public NonIdentifie(CriteresPersonne criteres) {
			super(criteres);
		}
	}

	public final RegDate dateTraitement;
	public int nbPersonnesTotal;
	public final List<Identifie> identifies = new ArrayList<>();
	public final List<PlusieursTrouves> plusieursTrouves = new ArrayList<>();
	public final List<NonIdentifie> nonIdentifies = new ArrayList<>();
	public final List<Erreur> erreurs = new ArrayList<>();
	public boolean interrompu;

	public IdentifierContribuableFromListeResults(RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
	}

	@Override
	public void addErrorException(CriteresPersonne element, Exception e) {
		erreurs.add(new Erreur( element, e.getMessage()));
	}

	@Override
	public void addAll(IdentifierContribuableFromListeResults right) {
		this.nbPersonnesTotal += right.nbPersonnesTotal;
		this.identifies.addAll(right.identifies);
		this.plusieursTrouves.addAll(right.plusieursTrouves);
		this.nonIdentifies.addAll(right.nonIdentifies);
		this.erreurs.addAll(right.erreurs);
	}

	public void addIdentifies(CriteresPersonne criteres, Long noContribuable,Long noMenageCommun ) {
		identifies.add(new Identifie(criteres, noContribuable, noMenageCommun));

	}

	public void addPlusieursTrouves(CriteresPersonne criteres, List<Long> trouves) {
		plusieursTrouves.add(new PlusieursTrouves(criteres, trouves));

	}

	public void addNonIdentifies(CriteresPersonne critere) {

		nonIdentifies.add(new NonIdentifie(critere));
	}

public boolean isInterrompu() {
		return interrompu;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}
	
public int getNbPersonnesTotal(){
	int nb = 0;
	if (CollectionUtils.isNotEmpty(identifies)) {
		nb = nb + identifies.size();

	}

	if (CollectionUtils.isNotEmpty(nonIdentifies)) {
		nb = nb + nonIdentifies.size();

	}
	if (CollectionUtils.isNotEmpty(plusieursTrouves)) {
		nb = nb + plusieursTrouves.size();

	}
	if (CollectionUtils.isNotEmpty(erreurs)) {
		nb = nb + erreurs.size();

	}
	return nb;
}

}
