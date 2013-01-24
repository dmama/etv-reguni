<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">
    <tiles:put name="body">
	    <fmt:message key="label.efacture.valider.action">
		    <fmt:param value="${libelleAction}"/>
		    <fmt:param>
			    <unireg:numCTB numero="${ctb}"/>
		    </fmt:param>
	    </fmt:message>
	    <br/><br/>
	    <c:set var="url">
		    <c:url value="${actionUrl}"/>
	    </c:set>
	    <form:form name="commentForm" id="commentForm" action="${url}" method="post">
		    <fmt:message key="label.efacture.commentaire.optionnel"/>
		    <br/>
		    <form:textarea path="comment" cols="50" rows="4"/>
	    </form:form>
    </tiles:put>
</tiles:insert>
