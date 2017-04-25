<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.changement.mode.imposition">
			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">

		<table border="0">
			<tr valign="top">
				<td>

					<form:form name="formFor" id="formFor" action="editModeImposition.do">
						<fieldset>
							<legend><span><fmt:message key="label.for.fiscal"/></span></legend>

							<form:hidden path="id"/>

							<!-- Debut For -->
							<table border="0">

								<tr class="<unireg:nextRowClass/>">
										<%-- Genre d'impôt --%>
									<td><fmt:message key="label.genre.impot"/>&nbsp;:</td>
									<td><fmt:message key="option.genre.impot.REVENU_FORTUNE"/></td>

										<%-- Motif de rattachement --%>
									<td id="div_rattachement_label"><fmt:message key="label.rattachement"/>&nbsp;:</td>
									<td id="div_rattachement"><fmt:message key="option.rattachement.${command.motifRattachement}"/></td>
								</tr>

								<tr id="date_for_periodique" class="<unireg:nextRowClass/>">
										<%-- Date d'ouverture --%>
									<td><fmt:message key="label.date.ouverture"/>&nbsp;:</td>
									<td><unireg:regdate regdate="${command.dateDebut}"/></td>

										<%-- Motif d'ouverture --%>
									<td><fmt:message key="label.motif.ouverture"/>&nbsp;:</td>
									<td><fmt:message key="option.motif.ouverture.${command.motifDebut}"/></td>
								</tr>

								<tr class="<unireg:nextRowClass/>">
										<%-- Type d'autorité fiscale --%>
									<td><fmt:message key="label.type.for.fiscal"/>&nbsp;:</td>
									<td><fmt:message key="option.type.for.fiscal.${command.typeAutoriteFiscale}"/></td>

										<%-- Nom de l'autorité fiscale --%>
									<td><fmt:message key="option.type.for.fiscal.${command.typeAutoriteFiscale}"/>&nbsp;:</td>
									<td>
										<c:choose>
											<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
												<unireg:commune ofs="${command.noAutoriteFiscale}" displayProperty="nomOfficiel" date="${command.dateDebut}"/>
											</c:when>
											<c:when test="${command.typeAutoriteFiscale == 'COMMUNE_HC' }">
												<unireg:commune ofs="${command.noAutoriteFiscale}" displayProperty="nomOfficielAvecCanton" date="${command.dateDebut}"/>
											</c:when>
											<c:when test="${command.typeAutoriteFiscale == 'PAYS_HS' }">
												<unireg:pays ofs="${command.noAutoriteFiscale}" displayProperty="nomCourt" date="${command.dateDebut}"/>
											</c:when>
										</c:choose>
									</td>
								</tr>

								<tr>
									<td colspan="4">
										<hr/>
									</td>
								</tr>

								<tr class="<unireg:nextRowClass/>">

										<%-- Mode d'imposition --%>
									<td><fmt:message key="label.mode.imposition"/>&nbsp;:</td>
									<td>
										<%--@elvariable id="modesImposition" type="java.util.Map<ModeImposition, String>"--%>
										<form:select path="modeImposition" items="${modesImposition}" onchange="updateSyncActions();" onkeyup="updateSyncActions();"/>
										<span style="color: red;">*</span>
										<form:errors path="modeImposition" cssClass="error"/>
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
												<jsp:param name="path" value="dateChangement"/>
												<jsp:param name="id" value="dateChangement"/>
												<jsp:param name="onChange" value="updateSyncActions"/>
												<jsp:param name="onkeyup" value="updateSyncActions"/>
												<jsp:param name="mandatory" value="true" />
											</jsp:include>
										</div>
									</td>
								</tr>

								<tr class="<unireg:nextRowClass/>" id="motif_changement">

										<%-- Motif de changement --%>
									<td><fmt:message key="label.motif.mode.imposition"/>&nbsp;:</td>
									<td>
										<form:select path="motifChangement" id="motifChangement" onchange="updateSyncActions();" onkeyup="updateSyncActions();">
											<form:option value="PERMIS_C_SUISSE"><fmt:message key="option.motif.ouverture.PERMIS_C_SUISSE"/></form:option>
											<form:option value="CHGT_MODE_IMPOSITION"><fmt:message key="option.motif.ouverture.CHGT_MODE_IMPOSITION"/></form:option>
										</form:select>
										<span style="color: red;">*</span>
									</td>
								</tr>

							</table>

						</fieldset>

						<table border="0">
							<tr>
								<td width="25%">&nbsp;</td>
								<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.mettre.a.jour" />"></td>
								<td width="25%"><unireg:buttonTo name="Retour" action="/fiscal/edit.do" params="{id:${command.tiersId}}" method="GET"/></td>
								<td width="25%">&nbsp;</td>
							</tr>
						</table>

					</form:form>

				</td>
				<td id="actions_column" style="display:none">
					<div id="actions_list"/>
				</td>
			</tr>
		</table>

		<script type="text/javascript">

			function updateSyncActions() {

				var idFor = ${command.id};
				var dateChangement = $('#dateChangement').val();
				var modeImposition = $('#modeImposition').val();
				var motifChangement = $('#motifChangement').val();

				$.get('<c:url value="/simulate/modeImpositionUpdate.do"/>?idFor=' + idFor + '&changeOn=' + dateChangement + '&newMode=' + modeImposition +
						'&reason=' + motifChangement + '&' + new Date().getTime(), function (results) {
					if (!results || results.empty) {
						$('#actions_column').hide();
					}
					else {
						$('#actions_list').html(Fors.buildActionTableHtml(results));
						$('#actions_column').show();
					}
				}, 'json')
				.error(Ajax.notifyErrorHandler("simulation des changements"));
			}
		</script>

	</tiles:put>
</tiles:insert>

		
