package ch.vd.unireg.tiers.rattrapage.etatdeclaration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class CorrectionEtatDeclarationResults extends JobResults<Long, CorrectionEtatDeclarationResults> {

	public static class Doublon {
		public final Long ctbId;
		public final Long diId;
		public final Long id;
		public final RegDate dateObtention;
		public final TypeEtatDocumentFiscal type;
		public final Date logCreationDate;
		public final String logCreationUser;
		public final Date logModificationDate;
		public final String logModificationUser;
		public final Date annulationDate;
		public final String annulationUser;

		public Doublon(EtatDeclaration etat) {
			this.ctbId = etat.getDeclaration().getTiers().getId();
			this.diId = etat.getDeclaration().getId();
			this.id = etat.getId();
			this.dateObtention = etat.getDateObtention();
			this.type = etat.getEtat();
			this.logCreationDate = etat.getLogCreationDate();
			this.logCreationUser = etat.getLogCreationUser();
			this.logModificationDate = etat.getLogModifDate();
			this.logModificationUser = etat.getLogModifUser();
			this.annulationDate = etat.getAnnulationDate();
			this.annulationUser = etat.getAnnulationUser();
		}
	}

	public static class Erreur extends Info {

		public Erreur(long noCtb, Integer officeImpotID, Exception exception, String nomCtb) {
			super(noCtb, officeImpotID, exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage(), nomCtb);
		}

		@Override
		public String getDescriptionRaison() {
			return "Erreur inattendue";
		}
	}

	public final RegDate dateTraitement = RegDate.get();
	public int nbDeclarationsTotal;
	public int nbEtatsTotal;
	public final List<Doublon> doublons = new ArrayList<>();
	public final List<Erreur> erreurs = new ArrayList<>();
	public boolean interrompu;

	public CorrectionEtatDeclarationResults(TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
	}

	public void addEtatsAvantTraitement(Collection<EtatDeclaration> etats) {
		nbDeclarationsTotal++;
		nbEtatsTotal += (etats == null ? 0 : etats.size());
	}

	public void addDoublonSupprime(EtatDeclaration etat) {
		doublons.add(new Doublon(etat));
	}

	@Override
	public void addErrorException(Long idCtb, Exception e) {
		erreurs.add(new Erreur(idCtb, null, e, getNom(idCtb)));
	}

	@Override
	public void addAll(CorrectionEtatDeclarationResults right) {
		this.nbDeclarationsTotal += right.nbDeclarationsTotal;
		this.nbEtatsTotal += right.nbEtatsTotal;
		this.doublons.addAll(right.doublons);
		this.erreurs.addAll(right.erreurs);
	}

	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	@Override
	public void end() {
		doublons.sort((o1, o2) -> {
			if (o1.ctbId.equals(o2.ctbId)) {
				if (o1.diId.equals(o2.diId)) {
					return o1.id.compareTo(o2.id);
				}
				else {
					return o1.diId.compareTo(o2.diId);
				}
			}
			else {
				return o1.ctbId.compareTo(o2.ctbId);
			}
		});
		super.end();
	}
}
