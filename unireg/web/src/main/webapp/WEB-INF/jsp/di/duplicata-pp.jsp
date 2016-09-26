<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}"/>
<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">

    <tiles:put name="body">
        <form:form name="formImpression" id="formImpression">

            <fieldset>
                <legend><span>Veuillez choisir le type de document à imprimer</span></legend>
                <unireg:nextRowClass reset="0"/>

                <div style="margin-top: 5px"><fmt:message key="label.type.document"/>:
                   <%--@elvariable id="typesDeclarationImpot" type="java.util.Map<TypeDocument, String>"--%>
                    <form:select id="typedocument" path="selectedTypeDocument" items="${typesDeclarationImpot}"/>
                </div>
                <div id="radio-for-save" style="display:none; margin-top: 5px"><span>Sauvegarder le type de document choisi pour cette déclaration:</span>
                    <form:radiobutton id="radio-save" path="toSave" value="true"/>
                    <fmt:message key="label.type.document.sauver.oui"/>
                    <form:radiobutton id="radio-save" path="toSave" value="false" />
                    <fmt:message key="label.type.document.sauver.non"/>
                </div>

            </fieldset>
            <fieldset>
                <legend><span>Veuillez choisir la composition et le nombre de documents à imprimer</span></legend>
                <unireg:nextRowClass reset="0"/>

                <form:hidden path="idDI"/>

                <c:forEach items="${command.modelesDocumentView}" var="modele" varStatus="statusModele">
                    <c:set var="rowClass"><unireg:nextRowClass/></c:set>


                    <form:hidden path="modelesDocumentView[${statusModele.index}].typeDocument"/>
                    <table id="model-for-${command.modelesDocumentView[statusModele.index].typeDocument}" style="display:none" border="0" cellspacing="0">
                        <c:forEach items="${modele.modelesFeuilles}" var="feuille" varStatus="statusFeuille">
                            <tr class="odd">
                                <td width="10%">&nbsp;</td>
                                <td width="45%"><c:out value="${feuille.intituleFeuille}"/>&nbsp;:</td>
                                <td width="45%" style="padding-bottom:4px">
                                    <form:input path="modelesDocumentView[${statusModele.index}].modelesFeuilles[${statusFeuille.index}].nombreFeuilles"
                                                cssClass="document-type-${modele.typeDocument}" size="2" maxlength="1"/>
                                    <form:hidden path="modelesDocumentView[${statusModele.index}].modelesFeuilles[${statusFeuille.index}].intituleFeuille"/>
                                    <form:hidden path="modelesDocumentView[${statusModele.index}].modelesFeuilles[${statusFeuille.index}].noCADEV"/>
                                    <form:hidden path="modelesDocumentView[${statusModele.index}].modelesFeuilles[${statusFeuille.index}].noFormulaireACI"/>
                                    <form:hidden path="modelesDocumentView[${statusModele.index}].modelesFeuilles[${statusFeuille.index}].principal"/>
                                    <c:if test="${feuille.principal && fn:length(modele.modelesFeuilles) > 1}">
                                        <span class="info_icon" style="margin-left: 1em;" title="Feuillet principal"></span>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </table>
                </c:forEach>
                <input type="hidden" id="changerType" value="false"/>

            </fieldset>

            <script>
                function show_model_table(typeDocument,selectedTypeDocument){
                    $('table[id^="model-for"]').hide();
                    $('#model-for-'+typeDocument).show();
                    if(typeDocument == selectedTypeDocument){
                        $('#radio-for-save').hide();
                        $('#changerType').val('false');
                    }
                    else{
                        $('#radio-for-save').show();
                        $('#changerType').val('true');
                    }

                }

                $('#typedocument').change(function() {
                    var id = $(this).children(":selected").attr("value");
                    show_model_table(id,'${command.selectedTypeDocument}');
                });

                $(document).ready(function(){
                    var id = $('#typedocument').children(":selected").attr("value");
                    show_model_table(id,'${command.selectedTypeDocument}');
                });

            </script>

        </form:form>

    </tiles:put>
</tiles:insert>
