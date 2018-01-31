<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<table>
    <tr class="<unireg:nextRowClass/>">
        <td width="50%"><fmt:message key="label.numero.ide"/>&nbsp;:</td>
        <td>
            <c:choose>
                <c:when test="${param.pathTiers !=null}">
                    <c:set var="bind" value="command.${param.pathTiers}.identificationsEntreprise" scope="request"/>
                </c:when>
                <c:when test="${param.path !=null}">
                    <c:set var="bind" value="command.${param.path}.identificationsEntreprise" scope="request"/>
                </c:when>
                <c:otherwise>

                </c:otherwise>
            </c:choose>
            <spring:bind path="${bind}">
                <c:forEach var="noIde" items="${status.value}">
                    <unireg:numIDE numeroIDE="${noIde.numeroIde}"/><br/>
                </c:forEach>
            </spring:bind>
        </td>
    </tr>
</table>