<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<fmt:message key="${param.helpCode}" var="helpMessage"/>

<c:if test="${not empty helpMessage}">
	<div class="help">
		<c:out value="${helpMessage}"/>
		 Lorem
		ipsum dolor sit amet, consectetuer adipiscing elit. Sed non risus.
		Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies
		sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius
		a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy
		molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat.
		Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium
		a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra
		tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede.
		Praesent blandit odio eu enim. Pellentesque sed dui ut augue blandit
		sodales. Vestibulum ante ipsum primis in faucibus orci luctus et
		ultrices posuere cubilia Curae; Aliquam nibh. Mauris ac mauris sed pede
		pellentesque fermentum. Maecenas adipiscing ante non diam sodales
		hendrerit.
	</div>
</c:if>

