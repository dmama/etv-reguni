<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<table class="civil">
	<tr>
		<td>
			<fieldset class="individu">
				<legend><span><fmt:message key="label.habitant" /></span></legend>
				<jsp:include page="individu-core.jsp">
					<jsp:param name="path" value="individu" />
					<jsp:param name="pathTiers" value="tiers" />
				</jsp:include>
			</fieldset>
		</td>
	</tr>
</table>



