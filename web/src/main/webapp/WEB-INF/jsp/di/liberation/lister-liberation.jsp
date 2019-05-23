<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:if test="${not empty command.liberations}">
	<fieldset>
		<legend><span><fmt:message key="label.liberation.panel.title"/></span></legend>
		<display:table name="command.liberations" id="liberation" pagesize="10" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">

			<display:column titleKey="label.date.liberation">
				<unireg:regdate regdate="${liberation.dateLiberation}"/>
			</display:column>

			<display:column titleKey="label.user.liberation">
				${liberation.logCreationUser}
			</display:column>

			<display:column titleKey="label.motivation.liberation">
				${liberation.motif}
			</display:column>

			<display:column style="action">
				<unireg:consulterLog entityNature="${liberation.entity}" entityId="${liberation.id}"/>
			</display:column>

		</display:table>

	</fieldset>
</c:if>
