package ch.vd.unireg.evenement.rapport.travail;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.event.rt.request.v1.FinRapportTravail;
import ch.vd.unireg.xml.event.rt.request.v1.FinRapportTravailType;
import ch.vd.unireg.xml.event.rt.request.v1.MiseAJourRapportTravailRequest;
import ch.vd.unireg.xml.DataHelper;

public class MiseAjourRapportTravail {
	private String businessId;
	private long idDebiteur;
	private long idSourcier;
	private RegDate DateDebutPeriodeDeclaration;
	private RegDate DateFinPeriodeDeclaration;
	private RegDate dateDebutVersementSalaire;
	private RegDate dateFinVersementSalaire;

	private boolean fermetureRapportTravail=false;
	private boolean sortie = false;
	private boolean deces =false;

	private RegDate dateEvenement;


	public static MiseAjourRapportTravail get(MiseAJourRapportTravailRequest request, String businessId){
		final MiseAjourRapportTravail miseAjourRapportTravail = new MiseAjourRapportTravail();
		miseAjourRapportTravail.setBusinessId(businessId);
		miseAjourRapportTravail.setIdDebiteur(request.getIdentifiantRapportTravail().getNumeroDebiteur());
		miseAjourRapportTravail.setIdSourcier(request.getIdentifiantRapportTravail().getNumeroContribuable());

		final RegDate dateDebutPeriodeDeclaration = DataHelper.xmlToCore(request.getIdentifiantRapportTravail().getDateDebutPeriodeDeclaration());
		miseAjourRapportTravail.setDateDebutPeriodeDeclaration(dateDebutPeriodeDeclaration);
		final RegDate dateFinPeriodeDeclaration = DataHelper.xmlToCore(request.getIdentifiantRapportTravail().getDateFinPeriodeDeclaration());
		miseAjourRapportTravail.setDateFinPeriodeDeclaration(dateFinPeriodeDeclaration);


		final RegDate dateDebutVersementSalaire = DataHelper.xmlToCore(request.getDateDebutVersementSalaire());
		miseAjourRapportTravail.setDateDebutVersementSalaire(dateDebutVersementSalaire);
		final RegDate dateFinVersementSalaire = DataHelper.xmlToCore(request.getDateFinVersementSalaire());
		miseAjourRapportTravail.setDateFinVersementSalaire(dateFinVersementSalaire);

		if(request.getFermetureRapportTravail()!=null){
			miseAjourRapportTravail.setFermetureRapportTravail(true);
		}


		FinRapportTravail finRapport = request.getFinRapportTravail();
		if (finRapport!=null) {
			miseAjourRapportTravail.setDateEvenement(DataHelper.xmlToCore(finRapport.getDateEvenement()));

			if(FinRapportTravailType.SORTIE.equals(finRapport.getCode())){
				miseAjourRapportTravail.setSortie(true);
			}
			if (FinRapportTravailType.DECES.equals(finRapport.getCode())) {
				miseAjourRapportTravail.setDeces(true);
			}
		}
		return miseAjourRapportTravail;
	}

	public long getIdDebiteur() {
		return idDebiteur;
	}

	public void setIdDebiteur(long idDebiteur) {
		this.idDebiteur = idDebiteur;
	}

	public long getIdSourcier() {
		return idSourcier;
	}

	public void setIdSourcier(long idSourcier) {
		this.idSourcier = idSourcier;
	}

	public RegDate getDateDebutPeriodeDeclaration() {
		return DateDebutPeriodeDeclaration;
	}

	public void setDateDebutPeriodeDeclaration(RegDate dateDebutPeriodeDeclaration) {
		DateDebutPeriodeDeclaration = dateDebutPeriodeDeclaration;
	}

	public RegDate getDateFinPeriodeDeclaration() {
		return DateFinPeriodeDeclaration;
	}

	public void setDateFinPeriodeDeclaration(RegDate dateFinPeriodeDeclaration) {
		DateFinPeriodeDeclaration = dateFinPeriodeDeclaration;
	}

	public RegDate getDateDebutVersementSalaire() {
		return dateDebutVersementSalaire;
	}

	public void setDateDebutVersementSalaire(RegDate dateDebutVersementSalaire) {
		this.dateDebutVersementSalaire = dateDebutVersementSalaire;
	}

	public RegDate getDateFinVersementSalaire() {
		return dateFinVersementSalaire;
	}

	public void setDateFinVersementSalaire(RegDate dateFinVersementSalaire) {
		this.dateFinVersementSalaire = dateFinVersementSalaire;
	}

	public boolean isFermetureRapportTravail() {
		return fermetureRapportTravail;
	}

	public void setFermetureRapportTravail(boolean fermetureRapportTravail) {
		this.fermetureRapportTravail = fermetureRapportTravail;
	}

	public boolean isSortie() {
		return sortie;
	}

	public void setSortie(boolean sortie) {
		this.sortie = sortie;
	}

	public boolean isDeces() {
		return deces;
	}

	public void setDeces(boolean deces) {
		this.deces = deces;
	}

	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	public void setDateEvenement(RegDate dateEvenement) {
		this.dateEvenement = dateEvenement;
	}

	public String getBusinessId() {
		return businessId;
	}

	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}
}
