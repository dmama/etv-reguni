<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.edition.di.periode.selection" /></tiles:put>

	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		
		<form:form name="theForm">
			<input type="hidden"  name="__TARGET__" value="">
			<input type="hidden"  name="__EVENT_ARGUMENT__" value="">
			
			<fieldset class="select-di">
				<legend><span><fmt:message key="label.di.periode.selection" /></span></legend>
			
				<!-- Debut liste de ranges -->
				<c:forEach var="range" items="${command.ranges}">
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
			<input type="button" value="<fmt:message key="label.bouton.creer"/>" onclick="javascript:creerDi();">
			<input type="button" value="<fmt:message key="label.bouton.annuler"/>" onclick="document.location='edit.do?action=listdis&numero=${command.contribuable.numero}'">
			<!-- Fin boutons -->
		</form:form>
		
		<br/>
		<span>(*) cette déclaration n'est pas obligatoire : elle peut être émise sur demande du contribuable.</span>

		<script type="text/javascript" language="Javascript1.3">
		 	function creerDi() {		 
				var form = F$("theForm");
				form.doPostBack("creerDI", "");
		 	}
		</script>	
		
	</tiles:put>
</tiles:insert>