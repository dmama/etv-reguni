package ch.vd.uniregctb.lr.view;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.view.DelaiDeclarationView;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class ListeRecapitulativeDetailView implements Annulable, DateRange {

	private final long idDebiteur;
	private final Long idListe;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final RegDate dateRetour;
	private final RegDate delaiAccorde;
	private final List<DelaiDeclarationView> delais;
	private final TypeEtatDeclaration etat;
	private final RegDate dateObtentionEtat;
	private final boolean annule;
	private final boolean isAllowedDelai;
	private final boolean imprimable;
	private final boolean annulable;

	/**
	 * Constructeur dans le cas de l'édition d'une LR déjà existante
	 * @param lr la LR à montrer
	 * @param infraService service d'infrastructure
	 * @param messageSource provider de messages éventuellement traduits
	 */
	public ListeRecapitulativeDetailView(DeclarationImpotSource lr, ServiceInfrastructureService infraService, MessageSource messageSource) {
		this.idDebiteur = lr.getTiers().getNumero();
		this.idListe = lr.getId();
		this.dateDebut = lr.getDateDebut();
		this.dateFin = lr.getDateFin();
		this.dateRetour = lr.getDateRetour();
		this.delais = buildDelais(lr, infraService, messageSource);
		this.delaiAccorde = this.delais.stream()
				.filter(AnnulableHelper::nonAnnule)
				.map(DelaiDeclarationView::getDelaiAccordeAu)
				.max(Comparator.naturalOrder())
				.orElse(null);
		this.annule = lr.isAnnule();

		final EtatDeclaration dernierEtat = lr.getDernierEtat();
		this.etat = dernierEtat.getEtat();
		this.dateObtentionEtat = dernierEtat.getDateObtention();
		this.imprimable = !lr.isAnnule();

		// [SIFISC-17743] ajout de délai seulement autorisée si lr seulement émise
		this.isAllowedDelai = this.etat == TypeEtatDeclaration.EMISE;

		// [SIFISC-10283] LR annulable si EMISE, SOMMEE ou ECHUE
		this.annulable = !lr.isAnnule() && this.etat != TypeEtatDeclaration.RETOURNEE;
	}

	/**
	 * Constructeur dans le cas de l'ajout d'une nouvelle LR
	 * @param dpi débiteur concerné par la LR
	 * @param periode période sur laquelle la LR doit être créée
	 */
	public ListeRecapitulativeDetailView(DebiteurPrestationImposable dpi, DateRange periode, RegDate delaiAccorde) {
		this.idDebiteur = dpi.getNumero();
		this.idListe = null;
		this.dateDebut = periode.getDateDebut();
		this.dateFin = periode.getDateFin();
		this.delaiAccorde = delaiAccorde;
		this.dateRetour = null;
		this.delais = null;
		this.etat = null;
		this.dateObtentionEtat = null;
		this.annule = false;
		this.annulable = false;
		this.isAllowedDelai = true;
		this.imprimable = true;
	}

	private static List<DelaiDeclarationView> buildDelais(DeclarationImpotSource lr, ServiceInfrastructureService infraService, MessageSource messageSource) {
		final List<DelaiDeclarationView> delais = lr.getDelais().stream()
				.sorted(new AnnulableHelper.AnnulesApresWrappingComparator<>(Comparator.comparing(DelaiDeclaration::getDateDemande).reversed()))
				.map(delai -> new DelaiDeclarationView(delai, infraService, messageSource))
				.collect(Collectors.toList());

		// le premier n'est pas annulable, il faut dont l'identifier comme tel
		delais.stream()
				.filter(AnnulableHelper::nonAnnule)
				.min(Comparator.comparingLong(DelaiDeclarationView::getId))
				.ifPresent(delai -> delai.setFirst(true));

		return delais;
	}

	public long getIdDebiteur() {
		return idDebiteur;
	}

	public Long getIdListe() {
		return idListe;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public RegDate getDelaiAccorde() {
		return delaiAccorde;
	}

	public List<DelaiDeclarationView> getDelais() {
		return delais;
	}

	public TypeEtatDeclaration getEtat() {
		return etat;
	}

	public RegDate getDateObtentionEtat() {
		return dateObtentionEtat;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public boolean isAllowedDelai() {
		return isAllowedDelai;
	}

	public boolean isImprimable() {
		return imprimable;
	}

	public boolean isAnnulable() {
		return annulable;
	}
}
