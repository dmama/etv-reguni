package ch.vd.uniregctb.di.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.context.MessageSource;

import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;

@SuppressWarnings("UnusedDeclaration")
public class DeclarationImpotListView {

	private final long ctbId;
	private final List<DeclarationImpotView> dis;
	private boolean ctbPP;
	private boolean ctbPM;

	public DeclarationImpotListView(Contribuable ctb, ServiceInfrastructureService infraService, MessageSource messageSource) {
		this.ctbId = ctb.getId();
		this.dis = initDeclarations(ctb.getDeclarationsTriees(DeclarationImpotOrdinaire.class, true), infraService, messageSource);
		this.ctbPP = ctb instanceof ContribuableImpositionPersonnesPhysiques;
		this.ctbPM = ctb instanceof ContribuableImpositionPersonnesMorales;
	}

	public static List<DeclarationImpotView> initDeclarations(Collection<? extends DeclarationImpotOrdinaire> declarations, ServiceInfrastructureService infraService, MessageSource messageSource) {
		if (declarations == null || declarations.isEmpty()) {
			return Collections.emptyList();
		}
		final List<DeclarationImpotView> views = new ArrayList<>(declarations.size());
		for (DeclarationImpotOrdinaire declaration : declarations) {
			views.add(new DeclarationImpotView(declaration, infraService, messageSource));
		}
		Collections.sort(views, new Comparator<DeclarationImpotView>() {
			@Override
			public int compare(DeclarationImpotView o1, DeclarationImpotView o2) {
				if (o1.isAnnule() && !o2.isAnnule()) {
					return 1;
				}
				else if (!o1.isAnnule() && o2.isAnnule()) {
					return -1;
				}
				else {
					return o2.getDateDebut().compareTo(o1.getDateDebut());
				}
			}
		});
		return views;
	}

	public long getCtbId() {
		return ctbId;
	}

	public List<DeclarationImpotView> getDis() {
		return dis;
	}

	public boolean isCtbPP() {
		return ctbPP;
	}

	public boolean isCtbPM() {
		return ctbPM;
	}
}
