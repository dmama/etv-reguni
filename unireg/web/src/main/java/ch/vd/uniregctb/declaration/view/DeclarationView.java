package ch.vd.uniregctb.declaration.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class DeclarationView implements Annulable {

	private final long id;
	private final Long tiersId;
	private final int periodeFiscale;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final boolean annule;
	private final TypeEtatDeclaration etat;
	private final List<EtatDeclarationView> etats;
	private final List<DelaiDeclarationView> delais;
	private final RegDate delaiAccorde;
	private final RegDate dateRetour;
	private final String sourceRetour;

	public DeclarationView(Declaration declaration, ServiceInfrastructureService infraService, MessageSource messageSource) {
		this.id = declaration.getId();
		this.tiersId = declaration.getTiers().getNumero();
		this.periodeFiscale = declaration.getPeriode().getAnnee();
		this.dateDebut = declaration.getDateDebut();
		this.dateFin = declaration.getDateFin();
		this.annule = declaration.isAnnule();

		final EtatDeclaration etat = declaration.getDernierEtat();
		this.etat = (etat == null ? null : etat.getEtat());
		if (etat instanceof EtatDeclarationRetournee) {
			this.sourceRetour = ((EtatDeclarationRetournee) etat).getSource();
		}
		else {
			this.sourceRetour = null;
		}

		this.etats = initEtats(declaration.getEtats(), infraService, messageSource);
		this.delais = initDelais(declaration.getDelais(), declaration.getPremierDelai(), infraService, messageSource);

		this.delaiAccorde = declaration.getDelaiAccordeAu();
		this.dateRetour = declaration.getDateRetour();
	}

	private static List<EtatDeclarationView> initEtats(Set<EtatDeclaration> etats, ServiceInfrastructureService infraService, MessageSource messageSource) {
		final List<EtatDeclarationView> list = new ArrayList<>();
		for (EtatDeclaration etat : etats) {
			list.add(new EtatDeclarationView(etat, infraService, messageSource));
		}
		Collections.sort(list);
		return list;
	}

	private static List<DelaiDeclarationView> initDelais(Set<DelaiDeclaration> delais, RegDate premierDelai, ServiceInfrastructureService infraService, MessageSource messageSource) {
		final List<DelaiDeclarationView> list = new ArrayList<>();
		for (DelaiDeclaration delai : delais) {
			final DelaiDeclarationView delaiView = new DelaiDeclarationView(delai, infraService, messageSource);
			delaiView.setFirst(premierDelai == delai.getDelaiAccordeAu());
			list.add(delaiView);
		}
		Collections.sort(list);
		return list;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public long getId() {
		return id;
	}

	public Long getTiersId() {
		return tiersId;
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

	public TypeEtatDeclaration getEtat() {
		return etat;
	}

	public List<EtatDeclarationView> getEtats() {
		return etats;
	}

	public List<DelaiDeclarationView> getDelais() {
		return delais;
	}

	public RegDate getDelaiAccorde() {
		return delaiAccorde;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public String getSourceRetour() {
		return sourceRetour;
	}
}
