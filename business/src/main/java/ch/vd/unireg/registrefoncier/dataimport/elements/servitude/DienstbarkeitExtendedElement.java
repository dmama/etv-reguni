package ch.vd.unireg.registrefoncier.dataimport.elements.servitude;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.rechteregister.BerechtigtePerson;
import ch.vd.capitastra.rechteregister.Dienstbarkeit;
import ch.vd.capitastra.rechteregister.DienstbarkeitExtended;
import ch.vd.capitastra.rechteregister.JuristischePersonGb;
import ch.vd.capitastra.rechteregister.LastRechtGruppe;
import ch.vd.capitastra.rechteregister.NatuerlichePersonGb;
import ch.vd.capitastra.rechteregister.PersonGb;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "DienstbarkeitExtended", namespace = "http://bedag.ch/capitastra/schemas/A51/v20101231/Datenexport/Rechteregister")
public class DienstbarkeitExtendedElement extends DienstbarkeitExtended {

	public DienstbarkeitExtendedElement() {
	}

	public DienstbarkeitExtendedElement(Dienstbarkeit dienstbarkeit) {
		super(dienstbarkeit, null);
	}

	/**
	 * Ajoute un groupe de bénéficiaires, c'est-à-dire le ou les immeubles concernés et le ou les bénéficiaires concernés. Cette méthode regroupe les immeubles par bénéficiaire.
	 *
	 * @param gruppe une groupe de bénéficiaires
	 */
	public void addLastRechtGruppe(@NotNull LastRechtGruppe gruppe) {
		if (lastRechtGruppe == null) {
			lastRechtGruppe = gruppe;
		}
		else {
			final Set<@NotNull String> existingPeopleKeys = lastRechtGruppe.getBerechtigtePerson().stream()
					.map(DienstbarkeitExtendedElement::getPersonIDRef)
					.collect(Collectors.toSet());

			// [SIFISC-29540] on ajoute les personnes qui manquent (parce qu'on s'est rendu compte que les bénéficiaires peuvent varier d'un immeuble à l'autre
			//                à l'intérieur d'une même servitude. Actuellement, notre modèle ne permet pas de le faire, alors on s'assure juste que toutes
			//                les personnes sont bien prises en compte).
			gruppe.getBerechtigtePerson().stream()
					.filter(b -> !existingPeopleKeys.contains(getPersonIDRef(b)))
					.forEach(b -> lastRechtGruppe.getBerechtigtePerson().add(b));


			// on ajoute les immeubles
			lastRechtGruppe.getBelastetesGrundstueck().addAll(gruppe.getBelastetesGrundstueck());
		}
	}

	@NotNull
	public static String getPersonIDRef(BerechtigtePerson bperson) {

		final PersonGb person = getPerson(bperson);

		final String idRef;
		if (person instanceof NatuerlichePersonGb) {
			idRef = ((NatuerlichePersonGb) person).getPersonstammIDREF();
		}
		else if (person instanceof JuristischePersonGb) {
			idRef = ((JuristischePersonGb) person).getPersonstammIDREF();
		}
		else {
			throw new IllegalArgumentException("Type de personne inconnue = [" + person.getClass().getName() + "]");
		}

		if (idRef == null) {
			throw new IllegalArgumentException("La personne masterId=[" + person.getMasterID() + "] ne possède pas d'idRef.");
		}

		return idRef;
	}

	@NotNull
	public static PersonGb getPerson(BerechtigtePerson p) {
		final NatuerlichePersonGb pp = p.getNatuerlichePersonGb();
		final JuristischePersonGb pm = p.getJuristischePersonGb();
		if (pp == null && pm == null) {
			throw new IllegalArgumentException("Le bénéficiaire versonID=[" + p.getVersionID() + "] n'est ni une personne physique ni une personne morale");
		}
		return pp == null ? pm : pp;
	}
}
