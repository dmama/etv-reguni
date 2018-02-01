package ch.vd.unireg.di.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.context.MessageSource;

import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;

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
		views.sort(new Comparator<DeclarationImpotView>() {
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
