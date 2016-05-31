<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.di.periode.selection" /></tiles:put>

	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		
		<form:form name="theForm">
			<fieldset class="select-di">
				<legend><span><fmt:message key="label.di.periode.selection" /></span></legend>
			
				<!-- Debut liste de ranges -->
				<c:forEach var="range" items="${ranges}">
						<input type="radio" id="<c:out value="${range.id}"/>" name="selection" value="<c:out value="${range.id}"/>">
						<c:if test="${range.optionnelle}">
							<label class="optionnel" for="<c:out value="${range.id}"/>"><c:out value="${range.description}" /> (*)</label><br/>
						</c:if>
						<c:if test="${!range.optionnelle}">
							<label class="obligatoire" for="<c:out value="${range.id}"/>"><c:out value="${range.description}" /></label><br/>
						</c:if>
				</c:forEach>
				<!-- Fin liste de ranges -->

			</fieldset>
			
			<!-- Debut boutons -->
			<input type="button" onclick="return creerDi();" value="Créer"/>
			<unireg:buttonTo name="Annuler" action="/di/list.do" method="get" params="{tiersId:${tiersId}}"/>
			<!-- Fin boutons -->
		</form:form>
		
		<br/>
		<span>(*) cette déclaration n'est pas obligatoire : elle peut être émise sur demande du contribuable.</span>

		<script type="text/javascript">
			function creerDi() {
				var selection = $('input[name=selection]:checked').val();
				if (!selection) {
					alert("Veuillez sélectionner une période");
					return false;
				}
				var dates = selection.split('-');
				window.location.href='<c:url value="/di/${actionImpression}.do?tiersId=${tiersId}"/>&debut=' + dates[0] + '&fin=' + dates[1];
				return false;
			}
		</script>

	</tiles:put>
</tiles:insert>