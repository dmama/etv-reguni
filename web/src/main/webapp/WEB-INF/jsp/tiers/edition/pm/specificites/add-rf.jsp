<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.ajout.regime.fiscal">
			<fmt:param value="${command.portee}"/>
			<fmt:param>
				<unireg:numCTB numero="${command.pmId}"/>
			</fmt:param>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${command.pmId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable"/>

		<form:form id="addRegimeForm" modelAttribute="command" action="add.do">

			<fieldset>
				<legend><span><fmt:message key="label.regime.fiscal"/></span></legend>

				<form:hidden path="pmId"/>
				<form:hidden path="portee"/>
				<unireg:nextRowClass reset="0"/>
				<table border="0">
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.debut"/>&nbsp;:</td>
						<td width="75%" colspan="3">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDebut" />
								<jsp:param name="id" value="dateDebut" />
								<jsp:param name="onChange" value="AddRegimeFiscal.displayTypeWarning"/>
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.regime.fiscal.type" />&nbsp;:</td>
						<td>
							<form:select path="code" onchange="AddRegimeFiscal.displayTypeWarning();" id="codeSelect">
								<form:option value=""/>
								<form:options items="${typesRegimeFiscal}"/>
							</form:select>
							<span class="mandatory">*</span>
							<form:errors path="code" cssClass="error"/>
						</td>
						<td colspan="2" ><span id="warnType" class="warn"></span></td>
					</tr>
				</table>

			</fieldset>

			<!-- Scripts -->
			<script type="text/javascript">

				var AddRegimeFiscal = {

					displayTypeWarning: function(obj) {
						var dateDebut = $('#dateDebut').get(0).value;
						var codeSelect = $('#codeSelect').get(0);
						var selectedCode = codeSelect.options[codeSelect.selectedIndex].value;
						if (dateDebut !== '' && selectedCode !== '') {
							var queryString = 'portee=${command.portee}&date=' + dateDebut + '&code=' + selectedCode + '&' + new Date().getTime();
							$.get('<c:url value="/regimefiscal/warning-message.do"/>?' + queryString, function(msg) {
								var spanSelector = $('#warnType');
								var span = spanSelector.get(0);
								while (span.firstChild) {
									span.removeChild(span.firstChild);
								}
								if (msg !== null && msg.text !== '') {
									spanSelector.addClass('warning_icon');
									span.appendChild(document.createTextNode(msg.text));
								}
								else {
									spanSelector.removeClass('warning_icon');
								}
							}, 'json');

						}
					}
				};

				$(function() {
					AddRegimeFiscal.displayTypeWarning(null);
				});

			</script>

			<!-- Debut Bouton -->
			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.ajouter" />"></td>
					<td width="25%"><unireg:buttonTo name="Retour" action="/regimefiscal/edit-list.do" params="{pmId:${command.pmId},portee:'${command.portee}'}" method="GET"/> </td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>
			<!-- Fin Bouton -->

		</form:form>

	</tiles:put>

</tiles:insert>