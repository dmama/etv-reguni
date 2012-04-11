<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />
<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">

	<tiles:put name="body">
		<form:form name="formImpression" id="formImpression">

		<fieldset><legend><span>Veuillez choisir le type et le nombre de documents à imprimer</span></legend>
		<unireg:nextRowClass reset="0"/>
		<table id="modeleDocumentTable" border="0" cellspacing="0">

			<c:forEach items="${command.modelesDocumentView}" var="modele" varStatus="statusModele">
				<c:set var="rowClass"><unireg:nextRowClass/></c:set>
				<tr class="${rowClass}">
					<td colspan="3" style="padding:4px">
						<form:radiobutton id="radio-${modele.typeDocument}" path="selectedTypeDocument" value="${modele.typeDocument}" onclick="refresh_input('${modele.typeDocument}')"/>
						<b><label for="radio-${modele.typeDocument}"><fmt:message key="option.type.document.${modele.typeDocument}"/>&nbsp;:</label></b>
					</td>
				</tr>

				<c:forEach items="${modele.modelesFeuilles}" var="feuille" varStatus="statusFeuille">
				<tr class="${rowClass}">
					<td width="10%">&nbsp;</td>
					<td width="45%">${feuille.intituleFeuille}&nbsp;:</td>
					<td width="45%" style="padding-bottom:4px">
						<form:input path="modelesDocumentView[${statusModele.index}].modelesFeuilles[${statusFeuille.index}].nbreIntituleFeuille"
							cssClass="document-type-${modele.typeDocument}" size="2" maxlength="1"/>
					</td>
				</tr>
				</c:forEach>

			</c:forEach>

		</table>
		</fieldset>

		<script>
			function refresh_input(typeDocument) {
				$('input:text', '#modeleDocumentTable').each(function(index) {
					if ($(this).hasClass('document-type-' + typeDocument)) {
						$(this).removeAttr('disabled');
					}
					else {
						$(this).attr('disabled', 'disabled');
					}
				});
			}
			refresh_input('${command.selectedTypeDocument}');

		</script>

		</form:form>

	</tiles:put>
</tiles:insert>
