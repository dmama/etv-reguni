<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

    <tiles:put name="title"><fmt:message key="title.efacture.quittancement" /></tiles:put>
    <tiles:put name="body">
        <fieldset>
            <legend><fmt:message key="label.efacture.contribuable"/></legend>
            <label for="ctb"><fmt:message key="label.efacture.noctb"/>&nbsp;:&nbsp;</label>
            <input id="ctb" size="15" type="text" onkeypress="return QuittanceEFacture.keypress(this, event);"/>
            <input id="commit" type="button" value="<fmt:message key='label.efacture.bouton.ok'/>" onclick="QuittanceEFacture.validate($('#ctb')[0]);"/>
        </fieldset>

        <script type="text/javascript">

            var QuittanceEFacture = {

                validate: function(input) {
                    var value = new String(input.value);
                    value = value.replace(/[^0-9]*/g, ''); // remove non-numeric chars
                    var id = parseInt(value, 10);
                    if (!isNaN(id)) {
                        var form = $('<form method="POST" action="' + App.curl("/efacture/quittancement/beep.do") + "?noctb=" + id + '"/>');
                        form.appendTo('body');
                        form.submit();
                    }
                    else {
                        this.initFocus(input);
                    }
                },

                keypress: function(input, e) {
                    var characterCode;

                    if (e && e.which) {
                        e = e;
                        characterCode = e.which;
                    } else {
                        e = event;
                        characterCode = e.keyCode;
                    }

                    if (characterCode == 13) {
                        this.validate(input);
                        return false;
                    } else {
                        return true;
                    }
                },

                initFocus: function(input) {
                    input.focus();
                }
            };

            $(QuittanceEFacture.initFocus($('#ctb')));

        </script>

    </tiles:put>
</tiles:insert>
