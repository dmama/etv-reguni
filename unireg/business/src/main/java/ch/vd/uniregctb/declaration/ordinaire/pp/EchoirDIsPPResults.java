package ch.vd.uniregctb.declaration.ordinaire.pp;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.IdentifiantDeclaration;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

public class EchoirDIsPPResults extends JobResults<IdentifiantDeclaration, EchoirDIsPPResults> {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION), ETAT_DECLARATION_INCOHERENT("L'état de la déclaration est incohérent");

		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Erreur extends Info {
		public final ErreurType raison;
		public final long diId;

		public Erreur(long noCtb, Integer officeImpotID, long diId, ErreurType raison, String details, String nomCtb) {
			super(noCtb, officeImpotID, details, nomCtb);
			this.raison = raison;
			this.diId = diId;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public static class Echue {
		public final long diId;
		public final long ctbId;
		public final Integer officeImpotID;
		public final RegDate dateDebut;
		public final RegDate dateFin;

		public Echue(long ctbId, Integer officeImpotID, long diId, RegDate dateDebut, RegDate dateFin) {
			this.diId = diId;
			this.ctbId = ctbId;
			this.officeImpotID = officeImpotID;
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
		}
	}

	// Paramètres d'entrée
	public final RegDate dateTraitement;

	// Données de processing
	public int nbDIsTotal;
	public final List<Echue> disEchues = new ArrayList<>();
	public final List<Erreur> disEnErrors = new ArrayList<>();

	public boolean interrompu;

	public EchoirDIsPPResults(RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
	}

	public void addDeclarationTraitee(DeclarationImpotOrdinaire di) {
		++nbDIsTotal;
		final Tiers tiers = di.getTiers();
		disEchues.add(new Echue(tiers.getNumero(), tiers.getOfficeImpotId(), di.getId(), di.getDateDebut(), di.getDateFin()));
	}

	public void addErrorEtatIncoherent(DeclarationImpotOrdinaire di, String message) {
		++nbDIsTotal;
		final Tiers tiers = di.getTiers();
		disEnErrors.add(new Erreur(tiers.getNumero(), tiers.getOfficeImpotId(), di.getId(), ErreurType.ETAT_DECLARATION_INCOHERENT, message, getNom(tiers.getNumero())));
	}

	@Override
	public void addErrorException(IdentifiantDeclaration ident, Exception e) {
		++nbDIsTotal;
		disEnErrors.add(new Erreur(ident.getNumeroTiers(), ident.getNumeroOID(), ident.getIdDeclaration(), ErreurType.EXCEPTION, e.getMessage(), getNom(ident.getNumeroTiers())));
	}

	@Override
	public void addAll(EchoirDIsPPResults rapport) {
		nbDIsTotal += rapport.nbDIsTotal;
		disEchues.addAll(rapport.disEchues);
		disEnErrors.addAll(rapport.disEnErrors);
	}
}
