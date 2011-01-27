<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.changement.mode.imposition">
  			<fmt:param><unireg:numCTB numero="${command.numeroCtb}"/></fmt:param>
  		</fmt:message>
  	</tiles:put>

	<tiles:put name="body">

		<table border="0"><tr valign="top">
		<td>

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

				<tr id="date_for_periodique" class="<unireg:nextRowClass/>" >
					<%-- Date d'ouverture --%>
					<td><fmt:message key="label.date.ouverture" />&nbsp;:</td>
					<td><fmt:formatDate value="${command.dateOuverture}" pattern="dd.MM.yyyy"/></td>

					<%-- Motif d'ouverture --%>
					<td><fmt:message key="label.motif.ouverture" />&nbsp;:</td>
					<td><fmt:message key="option.motif.ouverture.${command.motifOuverture}" /></td>
				</tr>

				<tr class="<unireg:nextRowClass/>" >
					<%-- Type d'autorité fiscale --%>
					<td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
					<td><fmt:message key="option.type.for.fiscal.${command.typeAutoriteFiscale}"/></td>

					<%-- Nom de l'autorité fiscale --%>
					<td><fmt:message key="option.type.for.fiscal.${command.typeAutoriteFiscale}"/>&nbsp;:</td>
					<td>
						<c:choose>
							<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
								<unireg:commune ofs="${command.numeroForFiscalCommune}" displayProperty="nomMinuscule" date="${command.regDateOuverture}"/>
							</c:when>
							<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_HC' }">
								<unireg:commune ofs="${command.numeroForFiscalCommuneHorsCanton}" displayProperty="nomMinuscule" date="${command.regDateOuverture}"/>
								(<unireg:commune ofs="${command.numeroForFiscalCommuneHorsCanton}" displayProperty="sigleCanton" date="${command.regDateOuverture}"/>)
							</c:when>
							<c:when test="${command.typeAutoriteFiscale == 'PAYS_HS' }">
								<unireg:infra entityId="${command.numeroForFiscalPays}" entityType="pays" entityPropertyName="nomMinuscule"></unireg:infra>
							</c:when>
						</c:choose>
					</td>
				</tr>

				<form:hidden path="changementModeImposition" />

				<tr><td colspan="4"><hr/></td></tr>

				<tr class="<unireg:nextRowClass/>" >

					<%-- Mode d'imposition --%>
					<td><fmt:message key="label.mode.imposition"/>&nbsp;:</td>
					<td>
						<form:select path="modeImposition" id="modeImposition" items="${modesImposition}" onchange="updateSyncActions();" onkeyup="updateSyncActions();"/>
						<form:errors path="modeImposition" cssClass="error" />
					</td>

					<%-- Date de changement --%>
					<td>
						<div id="date_changement_label">
							<fmt:message key="label.date.changement"/>&nbsp;:
						</div>
					</td>
					<td>
						<div id="date_changement_input">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateChangement" />
								<jsp:param name="id" value="dateChangement" />
								<jsp:param name="onChange" value="dateChangement_onChange" />
								<jsp:param name="onkeyup" value="dateChangement_onChange" />
							</jsp:include>
						</div>
					</td>
				</tr>

				<tr class="<unireg:nextRowClass/>" id="motif_changement">

					<%-- Motif de changement --%>
					<td><fmt:message key="label.motif.mode.imposition"/>&nbsp;:</td>
					<td>
						<form:select path="motifImposition" id="motifImposition" onchange="updateSyncActions();" onkeyup="updateSyncActions();">
							<form:option value="PERMIS_C_SUISSE" ><fmt:message key="option.motif.ouverture.PERMIS_C_SUISSE" /></form:option>
							<form:option value="CHGT_MODE_IMPOSITION" ><fmt:message key="option.motif.ouverture.CHGT_MODE_IMPOSITION" /></form:option>
						</form:select>
					</td>
				</tr>

			</table>

			<script type="text/javascript">
				var form = document.getElementById('formFor');
				form.changementModeImposition.value = "true";
			</script>

			</fieldset>

			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" id="maj" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
					<td width="25%"><input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='../fiscal/edit.do?id=' + ${command.numeroCtb}" /></td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>

		</form:form>

		</td>
		<td id="actions_column" style="display:none">
			<div id="actions_list"/>
		</td>
		</tr></table>

		<script type="text/javascript">
			function updateSyncActions() {

				var modeImposition = $('#modeImposition').val();
				var motifChangement = $('#motifImposition').val();

				XT.doAjaxAction('updateActionListSurModificationDuModeImposition', $('#actions_list').get(), {
					'forId' : ${command.id},
					'dateChangement' : $('#dateChangement').val(),
					'modeImposition' : modeImposition,
					'motifChangement' : motifChangement
				});
			}

			function dateChangement_onChange() {
				updateSyncActions();
			}
		</script>

	</tiles:put>
</tiles:insert>

		
