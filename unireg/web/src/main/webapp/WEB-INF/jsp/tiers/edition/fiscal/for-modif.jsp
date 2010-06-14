<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />
<tiles:insert template="/WEB-INF/jsp/templates/templateIFrame.jsp">
	<tiles:put name="head"></tiles:put>

	<tiles:put name="title"></tiles:put>
	<tiles:put name="body">
		<form:form name="formFor" id="formFor">
		<fieldset><legend><span><fmt:message key="label.for.fiscal" /></span></legend>		
		<!-- Debut For -->
		<table border="0">
		
			<tr class="<unireg:nextRowClass/>" >

				<%-- Genre d'impôt --%>
				<td><fmt:message key="label.genre.impot"/>&nbsp;:</td>
				<td><fmt:message key="option.genre.impot.${command.genreImpot}" /></td>

				<%-- Motif de rattachement --%>
				<c:if test="${command.natureForFiscal != 'ForFiscalAutreImpot'}">
					<td id="div_rattachement_label"><fmt:message key="label.rattachement"/>&nbsp;:</td>
					<td id="div_rattachement"><fmt:message key="option.rattachement.${command.motifRattachement}" /></td>
				</c:if>
				<c:if test="${command.natureForFiscal == 'ForFiscalAutreImpot'}">
					<td>&nbsp;</td>
					<td>&nbsp;</td>
				</c:if>
			</tr>
			<!-- Date et motif d'ouverture et de fermeture  pour les fors ForFiscalRevenuFortune 
					ForFiscalPrincipal - ForFiscalSecondaire - ForFiscalAutreElementImposable -->
			<c:if test="${command.natureForFiscal != 'ForFiscalAutreImpot'}">
			<tr id="date_for_periodique" class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.date.ouverture" />&nbsp;:</td>
				<td><fmt:formatDate value="${command.dateOuverture}" pattern="dd.MM.yyyy"/></td>
				<td><fmt:message key="label.motif.ouverture" />&nbsp;:</td>
				<td><fmt:message key="option.motif.ouverture.${command.motifOuverture}" /></td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
				<td><fmt:message key="option.type.for.fiscal.${command.typeAutoriteFiscale}" /></td>
				<td>
					<c:choose>
						<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }"><fmt:message key="option.type.for.fiscal.COMMUNE_OU_FRACTION_VD"/>&nbsp;:</c:when>
						<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_HC' }"><fmt:message key="option.type.for.fiscal.COMMUNE_HC"/>&nbsp;:</c:when>
						<c:when test="${command.typeAutoriteFiscale == 'PAYS_HS' }"><fmt:message key="option.type.for.fiscal.PAYS_HS"/>&nbsp;:</c:when>
					</c:choose>						
				</td>
				<td>
					<c:choose>
						<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
							<unireg:infra entityId="${command.numeroForFiscalCommune}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra>
						</c:when>
						<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_HC' }">
							<unireg:infra entityId="${command.numeroForFiscalCommuneHorsCanton}" entityType="commune" entityPropertyName="nomMinuscule"></unireg:infra>
						</c:when>
						<c:when test="${command.typeAutoriteFiscale == 'PAYS_HS' }">
							<unireg:infra entityId="${command.numeroForFiscalPays}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
						</c:when>
					</c:choose>
				</td>
			</tr>	
			<tr id="motif_for_periodique" class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.date.fermeture" />&nbsp;:</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="dateFermeture" />
						<jsp:param name="id" value="dateFermeture" />
					</jsp:include>
				</td>
				<td><fmt:message key="label.motif.fermeture" />&nbsp;:</td>
				<td>
					<form:select path="motifFermeture" />
					<form:errors path="motifFermeture" cssClass="error" />
				</td>
			</tr>
			</c:if>	
			<c:if test="${command.natureForFiscal == 'ForFiscalPrincipal'}">
			<form:hidden path="changementModeImposition" />
			<tr>
				<td colspan="4"><hr /></td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.mode.imposition"/>&nbsp;:</td>
				<td>
					<form:select path="modeImposition" items="${modesImposition}" 
					 onchange="selectModeImposition(this.options[this.selectedIndex].value, '${command.modeImposition}');" />
					 <form:errors path="modeImposition" cssClass="error" />
				</td>
				<td>
					<div id="date_changement_label"  <c:if test="${!command.changementModeImposition}">style="display:none;"</c:if>>
						<fmt:message key="label.date.changement"/>&nbsp;:
					</div>
				</td>
				<td>
					<div id="date_changement_input"  <c:if test="${!command.changementModeImposition}">style="display:none;"</c:if>>
						<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
							<jsp:param name="path" value="dateChangement" />
							<jsp:param name="id" value="dateChangement" />
						</jsp:include>
					</div>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" id="motif_changement" <c:if test="${!command.changementModeImposition}">style="display:none;"</c:if> >
				<td><fmt:message key="label.motif.mode.imposition"/>&nbsp;:</td>
				<td>
					<form:select path="motifImposition" >
						<form:option value="PERMIS_C_SUISSE" ><fmt:message key="option.motif.ouverture.PERMIS_C_SUISSE" /></form:option>
						<form:option value="CHGT_MODE_IMPOSITION" ><fmt:message key="option.motif.ouverture.CHGT_MODE_IMPOSITION" /></form:option>
					</form:select>
				</td>
			</tr>
			</c:if>
		</table>

		<script type="text/javascript">
			// on met-à-jour les motifs de fermeture au chargement de la page (genre impôt et rattachement sont fixés)
			updateMotifsFermeture(E$('motifFermeture'), 'motifFermeture', '${command.numeroCtb}', '${command.genreImpot}', '${command.motifRattachement}', '${command.motifFermeture}');
		</script>
		
	</fieldset>
	<form:errors cssClass="error" />
	<table border="0">
		<tr>
			<td width="25%">&nbsp;</td>
			<td width="25%"><input type="submit" id="maj" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
			<td width="25%"><input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler" />" onclick="self.parent.tb_remove()"></td>
			<td width="25%">&nbsp;</td>
		</tr>
	</table>
	<c:if test="${command.natureForFiscal == 'ForFiscalPrincipal'}">
		<script type="text/javascript">
			selectForFiscal('${command.typeAutoriteFiscale}');
		</script>
	</c:if>
	</form:form>
	</tiles:put>
</tiles:insert>

		
