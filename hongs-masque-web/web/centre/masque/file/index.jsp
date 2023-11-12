<%@page import="java.util.Map"%>
<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.CruxException"%>
<%@page import="io.github.ihongs.action.ActionHelper"%>
<%@page import="io.github.ihongs.action.VerifyHelper"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page import="io.github.ihongs.util.verify.Thumb" %>
<%@page import="io.github.ihongs.util.verify.Wrongs"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="application/json" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    ActionHelper ah = Core.getInstance(ActionHelper.class);
    VerifyHelper vh = new VerifyHelper();
    Map     rd = ah.getRequestData (   );
    String  tp = ah.getParameter("type");
    String  si = ah.getParameter("site_id");
    String  ri = ah.getParameter("room_id");

    // 接口权限校验
    vh.addRulesByForm ("masque", "auth");
    vh.verify  (rd, true, true);
    vh.getRules(   ).clear(   );

    // 文件上传处理
    if ("image".equals(tp)) {
        vh.addRule("file", new Thumb().config(Synt.mapOf(
            "href", "${BASE_HREF}/center/masque/file/"+si+"/"+ri+"/image",
            "path", "${BASE_PATH}/centre/masque/file/"+si+"/"+ri+"/image",
            "extn", "jpeg,jpg,png,gif,bmp" ,
            "thumb-size", "_sm:300*600"
        )));
        ah.reply(vh.verify(rd, true, true));
    } else {
        throw new CruxException(400, "Unsupported type!");
    }
%>