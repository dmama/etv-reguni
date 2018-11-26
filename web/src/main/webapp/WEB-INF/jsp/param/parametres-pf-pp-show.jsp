<fieldset style="margin: 10px" class="information">
	<%--@elvariable id="parametrePeriodeFiscalePPVaud" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePP"--%>
	<%--@elvariable id="parametrePeriodeFiscalePPHorsCanton" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePP"--%>
	<%--@elvariable id="parametrePeriodeFiscalePPHorsSuisse" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePP"--%>
	<%--@elvariable id="parametrePeriodeFiscalePPDepense" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePP"--%>
	<%--@elvariable id="parametrePeriodeFiscalePPDiplomateSuisse" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePP"--%>
	<legend>
		<fmt:message key="label.param.parametres.personnes.physiques"/>
	</legend>
	<a href="pf-edit-pp.do?pf=${periodeSelectionnee.id}" class="edit" title="${titleParametres}"><fmt:message key="label.param.edit"/>&nbsp;</a>

	<div class="checkbox">
		<input type="checkbox" disabled="disabled" <%--@elvariable id="codeControleSurSommationDIPP" type="java.lang.Boolean"--%>
		       <c:if test="${codeControleSurSommationDIPP}">checked</c:if>/>
		<fmt:message key="label.param.code.controle.sur.sommation.pp"/>
	</div>
	<%--@elvariable id="parametrePeriodeFiscaleEmomulementSommationDIPP" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscaleEmolument"--%>
	<c:if test="${parametrePeriodeFiscaleEmomulementSommationDIPP.montant != null}">
		<div class="emolument">
			<fmt:message key="label.param.emolument.sommation"/>&nbsp;:
			<span style="font-weight: bold;"><c:out value="${parametrePeriodeFiscaleEmomulementSommationDIPP.montant}"/></span>
			<fmt:message key="label.chf"/>
		</div>
	</c:if>

	<table>
		<tr>
			<th class="colonneTitreParametres">&nbsp;</th>
			<th><fmt:message key="label.param.entete.VD"/></th>
			<th><fmt:message key="label.param.entete.HC"/></th>
			<th><fmt:message key="label.param.entete.HS"/></th>
			<th><fmt:message key="label.param.entete.dep"/></th>
			<th><fmt:message key="label.param.entete.DS"/></th>
		</tr>
		<tr>
			<th><fmt:message key="label.param.som.reg"/></th>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPVaud.termeGeneralSommationReglementaire}"/>
			</td>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPHorsCanton.termeGeneralSommationReglementaire}"/>
			</td>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPHorsSuisse.termeGeneralSommationReglementaire}"/>
			</td>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPDepense.termeGeneralSommationReglementaire}"/>
			</td>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPDiplomateSuisse.termeGeneralSommationReglementaire}"/>
			</td>
		</tr>
		<tr>
			<th><fmt:message key="label.param.som.eff"/></th>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPVaud.termeGeneralSommationEffectif}"/>
			</td>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPHorsCanton.termeGeneralSommationEffectif}"/>
			</td>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPHorsSuisse.termeGeneralSommationEffectif}"/>
			</td>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPDepense.termeGeneralSommationEffectif}"/>
			</td>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPDiplomateSuisse.termeGeneralSommationEffectif}"/>
			</td>
		</tr>
		<tr>
			<th><fmt:message key="label.param.masse.di"/></th>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPVaud.dateFinEnvoiMasseDI}"/>
			</td>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPHorsCanton.dateFinEnvoiMasseDI}"/>
			</td>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPHorsSuisse.dateFinEnvoiMasseDI}"/>
			</td>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPDepense.dateFinEnvoiMasseDI}"/>
			</td>
			<td>
				<unireg:date date="${parametrePeriodeFiscalePPDiplomateSuisse.dateFinEnvoiMasseDI}"/>
			</td>
		</tr>
	</table>

</fieldset>