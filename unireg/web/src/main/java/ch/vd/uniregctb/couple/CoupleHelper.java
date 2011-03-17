package ch.vd.uniregctb.couple;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.couple.view.TypeUnion;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.*;

public class CoupleHelper {

	public class Couple {
		private TypeUnion typeUnion;
		private Contribuable premierTiers;
		private Contribuable secondTiers;
		
		public Couple(TypeUnion typeUnion, Contribuable premierTiers, Contribuable secondTiers) {
			this.typeUnion = typeUnion;
			this.premierTiers = premierTiers;
			this.secondTiers = secondTiers;
		}

		public TypeUnion getTypeUnion() {
			return typeUnion;
		}

		public Contribuable getPremierTiers() {
			return premierTiers;
		}

		public Contribuable getSecondTiers() {
			return secondTiers;
		}
	}
	
	private TiersService tiersService;
	
	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public Couple getCoupleForReconstitution(PersonnePhysique pp1, PersonnePhysique pp2, RegDate date) {
		EnsembleTiersCouple couplePP1 = tiersService.getEnsembleTiersCouple(pp1, date);
		EnsembleTiersCouple couplePP2 = tiersService.getEnsembleTiersCouple(pp2, date);
		MenageCommun menageAReconstituer = null;
		PersonnePhysique tiersAAjouter = null;
		if (couplePP1 != null) {
			menageAReconstituer = couplePP1.getMenage();
			tiersAAjouter = pp2;
		}
		else {
			menageAReconstituer = couplePP2.getMenage();
			tiersAAjouter = pp1;
		}
		
		return new Couple(TypeUnion.RECONSTITUTION_MENAGE, menageAReconstituer, tiersAAjouter);
	}

	public Couple getCoupleForFusion(PersonnePhysique pp1, PersonnePhysique pp2, RegDate date) {
		EnsembleTiersCouple couplePP1 = tiersService.getEnsembleTiersCouple(pp1, date);
		EnsembleTiersCouple couplePP2 = tiersService.getEnsembleTiersCouple(pp2, date);
		MenageCommun menagePP1 = null;
		if (couplePP1 != null) {
			menagePP1 = couplePP1.getMenage();
		}
		MenageCommun menagePP2 = null;
		if (couplePP2 != null) {
			menagePP2 = couplePP2.getMenage();
		}
		
		return new Couple(TypeUnion.FUSION_MENAGES, menagePP1, menagePP2);
	}
}
