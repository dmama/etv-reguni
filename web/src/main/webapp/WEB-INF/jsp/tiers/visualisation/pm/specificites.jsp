<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<jsp:include page="regimes-fiscaux.jsp"/>
<jsp:include page="allegements-fiscaux.jsp"/>
<jsp:include page="flags.jsp">
	<jsp:param name="group" value="SI_SERVICE_UTILITE_PUBLIQUE"/>
</jsp:include>
<jsp:include page="flags.jsp">
	<jsp:param name="group" value="LIBRE"/>
</jsp:include>
