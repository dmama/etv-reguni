package ch.vd.uniregctb.lr.view;

import java.util.List;
import java.util.Optional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class ListeRecapitulativeSearchResult implements Annulable {

	private final long idDebiteur;
	private final long idListe;
	private final List<String> nomCourrier;
	private final CategorieImpotSource categorieImpotSource;
	private final ModeCommunication modeCommunication;
	private final RegDate dateDebutPeriode;
	private final RegDate dateFinPeriode;
	private final RegDate dateRetour;
	private final RegDate delaiAccorde;
	private final TypeEtatDeclaration etat;
	private final boolean annule;

	public ListeRecapitulativeSearchResult(DeclarationImpotSource lr, AdresseService adresseService) throws AdresseException {
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) lr.getTiers();
		this.idDebiteur = dpi.getNumero();
		this.idListe = lr.getId();
		this.nomCourrier = computeNomCourrier(dpi, adresseService);
		this.categorieImpotSource = dpi.getCategorieImpotSource();
		this.modeCommunication = dpi.getModeCommunication();
		this.dateDebutPeriode = lr.getDateDebut();
		this.dateFinPeriode = lr.getDateFin();
		this.dateRetour = lr.getDateRetour();
		this.delaiAccorde = lr.getDelaiAccordeAu();
		this.etat = Optional.ofNullable(lr.getDernierEtat()).map(EtatDeclaration::getEtat).orElse(null);
		this.annule = lr.isAnnule();
	}

	private static List<String> computeNomCourrier(DebiteurPrestationImposable dpi, AdresseService adresseService) throws AdresseException {
		// [SIFISC-24807] Pas la peine de rajouter le complément car il est déjà présent, pour les DPI, dans le nom
		// renvoyé par le service d'adresses
		return adresseService.getNomCourrier(dpi, null, false);
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public long getIdDebiteur() {
		return idDebiteur;
	}

	public long getIdListe() {
		return idListe;
	}

	public List<String> getNomCourrier() {
		return nomCourrier;
	}

	public CategorieImpotSource getCategorieImpotSource() {
		return categorieImpotSource;
	}

	public ModeCommunication getModeCommunication() {
		return modeCommunication;
	}

	public RegDate getDateDebutPeriode() {
		return dateDebutPeriode;
	}

	public RegDate getDateFinPeriode() {
		return dateFinPeriode;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public RegDate getDelaiAccorde() {
		return delaiAccorde;
	}

	public TypeEtatDeclaration getEtat() {
		return etat;
	}
}
