package ch.vd.uniregctb.di.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Vue d'une déclaration d'impôt (ordinaire ou source).
 */
@SuppressWarnings("UnusedDeclaration")
public class DeclarationView implements Annulable {

	private final long id;
	private final Long tiersId;
	private final String codeControle;
	private final int periodeFiscale;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final RegDate delaiAccorde;
	private final RegDate dateRetour;
	private final TypeEtatDeclaration etat;
	private final String sourceRetour;
	private final boolean annule;
	private final TypeDocument typeDocument;
	private final String typeDocumentMessage;
	private final List<DelaiDeclarationView> delais;
	private final List<EtatDeclarationView> etats;
	private final boolean diPP;
	private final boolean diPM;
	private final RegDate dateDebutExercice;
	private final RegDate dateFinExercice;

	public DeclarationView(Declaration decl, MessageSource messageSource) {
		this.id = decl.getId();
		this.tiersId = decl.getTiers().getId();
		this.periodeFiscale = decl.getPeriode().getAnnee();
		this.dateDebut = decl.getDateDebut();
		this.dateFin = decl.getDateFin();
		this.delaiAccorde = decl.getDelaiAccordeAu();
		this.dateRetour = decl.getDateRetour();

		final EtatDeclaration etat = decl.getDernierEtat();
		this.etat = (etat == null ? null : etat.getEtat());
		if (etat instanceof EtatDeclarationRetournee) {
			this.sourceRetour = ((EtatDeclarationRetournee) etat).getSource();
		}
		else {
			this.sourceRetour = null;
		}

		this.annule = decl.isAnnule();
		if (decl instanceof DeclarationImpotOrdinaire) {
			final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) decl;
			this.codeControle = di.getCodeControle();
			this.typeDocument = di.getTypeDeclaration();
			if (this.typeDocument != null) {
				this.typeDocumentMessage = messageSource.getMessage("option.type.document." + this.typeDocument.name(), null, WebContextUtils.getDefaultLocale());
			}
			else {
				this.typeDocumentMessage = null;
			}
		}
		else {
			this.codeControle = null;
			this.typeDocument = null;
			this.typeDocumentMessage = null;
		}

		if (decl instanceof DeclarationImpotOrdinairePM) {
			final DeclarationImpotOrdinairePM di = (DeclarationImpotOrdinairePM) decl;
			this.dateDebutExercice = di.getDateDebutExerciceCommercial();
			this.dateFinExercice = di.getDateFinExerciceCommercial();
		}
		else {
			this.dateDebutExercice = null;
			this.dateFinExercice = null;
		}

		this.delais = initDelais(decl.getDelais(), decl.getPremierDelai(), messageSource);
		this.etats = initEtats(decl.getEtats(), messageSource);

		this.diPP = decl instanceof DeclarationImpotOrdinairePP;
		this.diPM = decl instanceof DeclarationImpotOrdinairePM;
	}

	private static List<DelaiDeclarationView> initDelais(Set<DelaiDeclaration> delais, RegDate premierDelai, MessageSource messageSource) {
		final List<DelaiDeclarationView> list = new ArrayList<>();
		for (DelaiDeclaration delai : delais) {
			final DelaiDeclarationView delaiView = new DelaiDeclarationView(delai, messageSource);
			delaiView.setFirst(premierDelai == delai.getDelaiAccordeAu());
			list.add(delaiView);
		}
		Collections.sort(list);
		return list;
	}

	private static List<EtatDeclarationView> initEtats(Set<EtatDeclaration> etats, MessageSource messageSource) {
		final List<EtatDeclarationView> list = new ArrayList<>();
		for (EtatDeclaration etat : etats) {
			list.add(new EtatDeclarationView(etat, messageSource));
		}
		Collections.sort(list);
		return list;
	}

	public long getId() {
		return id;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public String getCodeControle() {
		return codeControle;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public RegDate getDateDebutExercice() {
		return dateDebutExercice;
	}

	public RegDate getDateFinExercice() {
		return dateFinExercice;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public String getTypeDocumentMessage() {
		return typeDocumentMessage;
	}

	public List<DelaiDeclarationView> getDelais() {
		return delais;
	}

	public List<EtatDeclarationView> getEtats() {
		return etats;
	}

	public RegDate getDelaiAccorde() {
		return delaiAccorde;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public TypeEtatDeclaration getEtat() {
		return etat;
	}

	public String getSourceRetour() {
		return sourceRetour;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public boolean isDiPP() {
		return diPP;
	}

	public boolean isDiPM() {
		return diPM;
	}
}
