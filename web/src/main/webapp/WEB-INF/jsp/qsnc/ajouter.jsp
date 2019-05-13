<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.impression.nouveau.qsnc" /></tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${added.entrepriseId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques de l'entreprise" />

		<form:form name="theForm" method="post" action="add.do" modelAttribute="added">

			<!-- Debut Caracteristiques generales -->
			<fieldset>
				<legend><span><fmt:message key="label.caracteristiques.questionnaire.snc" /></span></legend>

				<input type="hidden" name="entrepriseId" value="${added.entrepriseId}"/>
				<input type="hidden" name="periodeFiscale" value="${added.periodeFiscale}"/>
				<input type="hidden" name="depuisTache" value="${added.depuisTache}"/>

				<table border="0">
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.periode.fiscale" />&nbsp;:</td>
						<td width="75%">${added.periodeFiscale}</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.delai.accorde" />&nbsp;:</td>
						<td width="75%">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="delaiAccorde" />
								<jsp:param name="id" value="delaiAccorde" />
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
					</tr>
				</table>

			</fieldset>
			<!-- Fin Caracteristiques generales -->

			<!-- Debut Bouton -->
			<c:choose>
				<c:when test="${added.depuisTache}">
					<unireg:buttonTo name="Retour" action="/tache/list.do" method="get"/>
				</c:when>
				<c:otherwise>
					<unireg:buttonTo name="Retour" action="/qsnc/list.do" method="get" params="{tiersId:${added.entrepriseId}}" />
				</c:otherwise>
			</c:choose>
			<input type="button" name="imprimer" id="imprimer" value="<fmt:message key="label.bouton.imprimer"/>" onclick="AddQuestionnaireSNC.imprimer(this);"/>
			<!-- Fin Bouton -->

		</form:form>


		<script type="application/javascript">
			var AddQuestionnaireSNC = {
				imprimer: function(button) {
					$('span.error').hide(); // on cache d'éventuelles erreurs datant d'un ancien submit
					$(button).closest("form").submit();
					$('#imprimer').disabled = true;
				}
			};
		</script>

	</tiles:put>

</tiles:insert>