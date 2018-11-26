<fieldset style="margin: 10px" class="information">
	<%--@elvariable id="parametrePeriodeFiscalePMVaud" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePM"--%>
	<%--@elvariable id="parametrePeriodeFiscalePMHorsCanton" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePM"--%>
	<%--@elvariable id="parametrePeriodeFiscalePMHorsSuisse" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePM"--%>
	<%--@elvariable id="parametrePeriodeFiscalePMUtilitePublique" type="ch.vd.unireg.parametrage.ParametrePeriodeFiscalePM"--%>
	<legend>
		<fmt:message key="label.param.parametres.personnes.morales"/>
	</legend>
	<a href="pf-edit-pm.do?pf=${periodeSelectionnee.id}" class="edit" title="${titleParametres}"><fmt:message key="label.param.edit"/>&nbsp;</a>

	<div class="checkbox">
		<input type="checkbox" disabled="disabled" <%--@elvariable id="codeControleSurSommationDIPM" type="java.lang.Boolean"--%>
		       <c:if test="${codeControleSurSommationDIPM}">checked</c:if>/>
		<fmt:message key="label.param.code.controle.sur.sommation.pm"/>
	</div>

	<table>
		<tr>
			<th class="colonneTitreParametres">&nbsp;</th>
			<th><fmt:message key="label.param.entete.VD"/></th>
			<th><fmt:message key="label.param.entete.HC"/></th>
			<th><fmt:message key="label.param.entete.HS"/></th>
			<th><fmt:message key="label.param.entete.utilite.publique"/></th>
		</tr>
		<tr>
			<th><fmt:message key="label.param.pm.delai.imprime"/></th>
			<td>
				<c:if test="${parametrePeriodeFiscalePMVaud.delaiImprimeMois != null}">
					${parametrePeriodeFiscalePMVaud.delaiImprimeMois}
					<fmt:message key="label.param.pm.delai.mois"/>
					<fmt:message key="option.reference.delai.${parametrePeriodeFiscalePMVaud.referenceDelaiInitial}"/>
					<c:if test="${parametrePeriodeFiscalePMVaud.delaiImprimeRepousseFinDeMois}">
						(<fmt:message key="label.param.pm.report.fin.mois"/>)
					</c:if>
				</c:if>
			</td>
			<td>
				<c:if test="${parametrePeriodeFiscalePMHorsCanton.delaiImprimeMois != null}">
					${parametrePeriodeFiscalePMHorsCanton.delaiImprimeMois}
					<fmt:message key="label.param.pm.delai.mois"/>
					<fmt:message key="option.reference.delai.${parametrePeriodeFiscalePMHorsCanton.referenceDelaiInitial}"/>
					<c:if test="${parametrePeriodeFiscalePMHorsCanton.delaiImprimeRepousseFinDeMois}">
						(<fmt:message key="label.param.pm.report.fin.mois"/>)
					</c:if>
				</c:if>
			</td>
			<td>
				<c:if test="${parametrePeriodeFiscalePMHorsSuisse.delaiImprimeMois != null}">
					${parametrePeriodeFiscalePMHorsSuisse.delaiImprimeMois}
					<fmt:message key="label.param.pm.delai.mois"/>
					<fmt:message key="option.reference.delai.${parametrePeriodeFiscalePMHorsSuisse.referenceDelaiInitial}"/>
					<c:if test="${parametrePeriodeFiscalePMHorsSuisse.delaiImprimeRepousseFinDeMois}">
						(<fmt:message key="label.param.pm.report.fin.mois"/>)
					</c:if>
				</c:if>
			</td>
			<td>
				<c:if test="${parametrePeriodeFiscalePMUtilitePublique.delaiImprimeMois != null}">
					${parametrePeriodeFiscalePMUtilitePublique.delaiImprimeMois}
					<fmt:message key="label.param.pm.delai.mois"/>
					<fmt:message key="option.reference.delai.${parametrePeriodeFiscalePMUtilitePublique.referenceDelaiInitial}"/>
					<c:if test="${parametrePeriodeFiscalePMUtilitePublique.delaiImprimeRepousseFinDeMois}">
						(<fmt:message key="label.param.pm.report.fin.mois"/>)
					</c:if>
				</c:if>
			</td>
		</tr>
		<tr>
			<th><fmt:message key="label.param.pm.delai.tolerance"/></th>
			<td>
				<c:if test="${parametrePeriodeFiscalePMVaud.delaiToleranceJoursEffective != null}">
					${parametrePeriodeFiscalePMVaud.delaiToleranceJoursEffective}
					<c:choose>
						<c:when test="${parametrePeriodeFiscalePMVaud.delaiToleranceJoursEffective == 1}">
							<fmt:message key="label.param.pm.delai.jour"/>
						</c:when>
						<c:otherwise>
							<fmt:message key="label.param.pm.delai.jours"/>
						</c:otherwise>
					</c:choose>
					<c:if test="${parametrePeriodeFiscalePMVaud.delaiTolereRepousseFinDeMois}">
						(<fmt:message key="label.param.pm.report.fin.mois"/>)
					</c:if>
				</c:if>
			</td>
			<td>
				<c:if test="${parametrePeriodeFiscalePMHorsCanton.delaiToleranceJoursEffective != null}">
					${parametrePeriodeFiscalePMHorsCanton.delaiToleranceJoursEffective}
					<c:choose>
						<c:when test="${parametrePeriodeFiscalePMHorsCanton.delaiToleranceJoursEffective == 1}">
							<fmt:message key="label.param.pm.delai.jour"/>
						</c:when>
						<c:otherwise>
							<fmt:message key="label.param.pm.delai.jours"/>
						</c:otherwise>
					</c:choose>
					<c:if test="${parametrePeriodeFiscalePMHorsCanton.delaiTolereRepousseFinDeMois}">
						(<fmt:message key="label.param.pm.report.fin.mois"/>)
					</c:if>
				</c:if>
			</td>
			<td>
				<c:if test="${parametrePeriodeFiscalePMHorsSuisse.delaiToleranceJoursEffective != null}">
					${parametrePeriodeFiscalePMHorsSuisse.delaiToleranceJoursEffective}
					<c:choose>
						<c:when test="${parametrePeriodeFiscalePMHorsSuisse.delaiToleranceJoursEffective == 1}">
							<fmt:message key="label.param.pm.delai.jour"/>
						</c:when>
						<c:otherwise>
							<fmt:message key="label.param.pm.delai.jours"/>
						</c:otherwise>
					</c:choose>
					<c:if test="${parametrePeriodeFiscalePMHorsSuisse.delaiTolereRepousseFinDeMois}">
						(<fmt:message key="label.param.pm.report.fin.mois"/>)
					</c:if>
				</c:if>
			</td>
			<td>
				<c:if test="${parametrePeriodeFiscalePMUtilitePublique.delaiToleranceJoursEffective != null}">
					${parametrePeriodeFiscalePMUtilitePublique.delaiToleranceJoursEffective}
					<c:choose>
						<c:when test="${parametrePeriodeFiscalePMUtilitePublique.delaiToleranceJoursEffective == 1}">
							<fmt:message key="label.param.pm.delai.jour"/>
						</c:when>
						<c:otherwise>
							<fmt:message key="label.param.pm.delai.jours"/>
						</c:otherwise>
					</c:choose>
					<c:if test="${parametrePeriodeFiscalePMUtilitePublique.delaiTolereRepousseFinDeMois}">
						(<fmt:message key="label.param.pm.report.fin.mois"/>)
					</c:if>
				</c:if>
			</td>
		</tr>
	</table>

</fieldset>