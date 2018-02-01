package ch.vd.unireg.declaration.ordinaire.pm;

import java.util.LinkedList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.IdentifiantDeclaration;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;

public class EchoirDIsPMResults extends JobResults<IdentifiantDeclaration, EchoirDIsPMResults> {

	public enum ErreurType {
		EXCEPTION(EXCEPTION_DESCRIPTION),
		ETAT_DECLARATION_INCOHERENT("L'état de la déclaration est incohérent");

		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public enum MotifIgnorance {
		DECLARATION_SUSPENDUE("Déclaration suspendue"),
		SURSIS_ACCORDE("Sursis accordé non-échu");

		private final String description;

		MotifIgnorance(String description) {
			this.description = description;
		}

		public String getDescription() {
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

	public static abstract class BaseEcheance {
		public final long diId;
		public final long ctbId;
		public final RegDate dateDebut;
		public final RegDate dateFin;

		public BaseEcheance(long ctbId, long diId, RegDate dateDebut, RegDate dateFin) {
			this.diId = diId;
			this.ctbId = ctbId;
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
		}
	}

	public static class Echue extends BaseEcheance {
		public Echue(long ctbId, long diId, RegDate dateDebut, RegDate dateFin) {
			super(ctbId, diId, dateDebut, dateFin);
		}
	}

	public static class Ignoree extends BaseEcheance {
		public final MotifIgnorance motif;
		public Ignoree(long ctbId, long diId, RegDate dateDebut, RegDate dateFin, MotifIgnorance motif) {
			super(ctbId, diId, dateDebut, dateFin);
			this.motif = motif;
		}
	}

	// Paramètres d'entrée
	public final RegDate dateTraitement;

	// Données de processing
	public int nbDIsTotal;
	public final List<Echue> disEchues = new LinkedList<>();
	public final List<Erreur> disEnErrors = new LinkedList<>();
	public final List<Ignoree> disIgnorees = new LinkedList<>();

	public boolean interrompu;

	public EchoirDIsPMResults(RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.dateTraitement = dateTraitement;
	}

	public void addDeclarationTraitee(DeclarationImpotOrdinaire di) {
		++nbDIsTotal;
		final Tiers tiers = di.getTiers();
		disEchues.add(new Echue(tiers.getNumero(), di.getId(), di.getDateDebut(), di.getDateFin()));
	}

	public void addErrorEtatIncoherent(DeclarationImpotOrdinaire di, String message) {
		++nbDIsTotal;
		final Tiers tiers = di.getTiers();
		disEnErrors.add(new Erreur(tiers.getNumero(), tiers.getOfficeImpotId(), di.getId(), ErreurType.ETAT_DECLARATION_INCOHERENT, message, getNom(tiers.getNumero())));
	}

	public void addDISuspendueIgnoree(DeclarationImpotOrdinaire di) {
		++nbDIsTotal;
		final Tiers tiers = di.getTiers();
		disIgnorees.add(new Ignoree(tiers.getNumero(), di.getId(), di.getDateDebut(), di.getDateFin(), MotifIgnorance.DECLARATION_SUSPENDUE));
	}

	public void addDIAvecSursisAccordeIgnoree(DeclarationImpotOrdinaire di) {
		++nbDIsTotal;
		final Tiers tiers = di.getTiers();
		disIgnorees.add(new Ignoree(tiers.getNumero(), di.getId(), di.getDateDebut(), di.getDateFin(), MotifIgnorance.SURSIS_ACCORDE));
	}

	@Override
	public void addErrorException(IdentifiantDeclaration ident, Exception e) {
		++nbDIsTotal;
		disEnErrors.add(new Erreur(ident.getNumeroTiers(), ident.getNumeroOID(), ident.getIdDeclaration(), ErreurType.EXCEPTION, e.getMessage(), getNom(ident.getNumeroTiers())));
	}

	@Override
	public void addAll(EchoirDIsPMResults rapport) {
		nbDIsTotal += rapport.nbDIsTotal;
		disEchues.addAll(rapport.disEchues);
		disEnErrors.addAll(rapport.disEnErrors);
		disIgnorees.addAll(rapport.disIgnorees);
	}
}
