<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="menu" type="String">
	</tiles:put>

	<tiles:put name="title" type="String"><fmt:message key="title.import.script.DBUnit" /></tiles:put>
	<tiles:put name="connected" type="String">
	</tiles:put>
	<tiles:put name="body" type="String">

	<c:choose>
		<c:when test="${scriptResult eq 'success'}">
			<fmt:message key="label.admin.import.script.successful" />
		</c:when>
	
		<c:when test="${scriptResult eq 'noInputStream'}">
			<fmt:message key="label.admin.import.script.noInputStream" />
		</c:when>

		<c:when test="${scriptResult eq 'datasetException'}">
			<fmt:message key="label.admin.import.script.datasetException" /><br/><br/>
			<fmt:message key="label.admin.import.script.detailException" /><br/>
			<span class="error"><c:out value="${exception.message}"/></span>
		</c:when>

		<c:when test="${scriptResult eq 'databaseUnitException'}">
			<fmt:message key="label.admin.import.script.databaseUnitException" /><br/><br/>
			<fmt:message key="label.admin.import.script.detailException" /><br/>
			<span class="error"><c:out value="${exception.message}"/></span>
		</c:when>

		<c:when test="${scriptResult eq 'sqlException'}">
			<fmt:message key="label.admin.import.script.sqlException" /><br/><br/>
			<fmt:message key="label.admin.import.script.detailException" /><br/>
			<span class="error"><c:out value="${exception.message}"/></span>
		</c:when>
		
		<c:otherwise>
			<fmt:message key="label.admin.import.script.unknownResult" />&nbsp;
			<span class="error"><c:out value="${scriptResult}"/></span><br/><br/>
			<c:if test="${exception != null}">
				<fmt:message key="label.admin.import.script.detailException" /><br/>
				<span class="error"><c:out value="${exception.message}"/></span>
			</c:if>
		</c:otherwise>
	</c:choose>

    <br/><br/>
	<a href="tiersImport.do">Retour au formulaire</a>

	</tiles:put>
</tiles:insert>