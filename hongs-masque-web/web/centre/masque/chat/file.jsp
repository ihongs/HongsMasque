<%@page import="io.github.ihongs.util.verify.Wrongs"%>
<%@page import="java.util.Map"%>
<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.HongsException"%>
<%@page import="io.github.ihongs.action.ActionHelper"%>
<%@page import="io.github.ihongs.action.VerifyHelper"%>
<%@page extends="io.github.ihongs.jsp.Pagelet"%>
<%@page contentType="application/json" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    ActionHelper ah = Core.getInstance(ActionHelper.class);
    VerifyHelper vh = new VerifyHelper();
    Map     rd = ah.getRequestData (   );
    String  tp = ah.getParameter("type");

    // 接口权限校验
    vh.addRulesByForm ("masque", "auth");
    vh.verify  (rd, true, true);
    vh.getRules(   ).clear(   );

    // 文件上传处理
    if ("image".equals(tp)) {
        vh.addRulesByForm ("upload", "image");
        ah.reply(vh.verify( rd, true, true ));
    } else {
        throw new HongsException(0x1100, "Unsupported type!");
    }
%>