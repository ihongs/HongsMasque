package io.github.ihongs.serv.centre;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Model;
import io.github.ihongs.db.Table;
import io.github.ihongs.normal.serv.Record;
import io.github.ihongs.util.Data;
import io.github.ihongs.util.Digest;
import io.github.ihongs.util.Remote;
import io.github.ihongs.util.Synt;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 常规消息接口
 * @author hong
 */
@Action("centre/masque")
public class MasqueAction {

    @Action("chat/search")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    public void searchChat(ActionHelper helper) throws HongsException {
        Model  mod = DB.getInstance("masque").getModel("chat");
        Table  sta = DB.getInstance("masque").getTable("stat");
        Map    req = helper.getRequestData( );
        Set    ab  = Synt.toTerms(req.get(Cnst.AB_KEY));

        /**
         * 默认逆序
         */
        if ( ! req.containsKey(Cnst.OB_KEY)) {
            req.put (Cnst.OB_KEY, "-ctime");
        }

        helper.reply(mod.search(req));

        /**
         * 消除未读
         */
        if (ab != null && ab.contains("fresh")) {
            String sql = "UPDATE `"+sta.tableName+"` SET `fresh`=0,`mtime`=? WHERE `fresh`!=0 AND site_id=? AND room_id=? AND mate_id=?";
            String sid = (String) req.get("site_id");
            String rid = (String) req.get("room_id");
            String mid = (String) req.get("mate_id");
            long   now = Core.ACTION_TIME.get()/1000;
            sta.db.updates (sql, now, sid, rid, mid);
        }
    }

    @Action("stat/search")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    public void searchStat(ActionHelper helper) throws HongsException {
        Model  mod = DB.getInstance("masque").getModel("stat");
        Table  sta = DB.getInstance("masque").getTable("stat");
        Table  cha = DB.getInstance("masque").getTable("chat");
        Map    req = helper.getRequestData( );

        String sid = (String) req.get("site_id");
        String rid = (String) req.get("room_id");
        String mid = (String) req.get("mine_id"); // 仅能获取当前用户的状态

        String sql;
        String msg; // 最近的一条消息
        String mat; // 最近发消息的人
        long   tim; // 最近发送的时间
        int    num; // 未读消息的数量
        Map    row;

        /**
         * room_id 为:
         * '.' 返回分页状态列表数据,
         * '!' 统计全部未读消息数量.
         */
        if (".".equals(rid)) {
            Map rsq = Synt.mapOf(
                "site_id", sid,
                "mate_id", mid
            );

            Set rb =  Synt.toTerms(req.get(Cnst.RB_KEY));
            if (rb == null) {
                rb =  Synt.setOf("room_id", "fresh", "mtime", "last");
                rsq.put (Cnst.RB_KEY , rb);
            }

            Set ob =  Synt.toTerms(req.get(Cnst.OB_KEY));
            if (ob == null) {
                ob =  Synt.setOf("-mtime");
                rsq.put (Cnst.OB_KEY , ob);
            }

            Map rsp = mod.search(rsq);

            // TODO: 追加最后消息

            helper.reply(rsp);
            return;
        } else
        if ("!".equals(rid)) {
            sql = "SELECT SUM(s.fresh) AS fresh"
                + " FROM `"+sta.tableName+"` AS s"
                + " WHERE s.site_id=? AND s.mate_id=?";
            row = sta.db.fetchOne(sql, sid, mid /**/);
            num = Synt.declare(row.get("fresh"),  0 );

            sql = "SELECT c.ctime,c.note,c.mate_id"
                + " FROM `"+cha.tableName+"` AS c"
                + " INNER JOIN `"+sta.tableName+"` AS s ON s.site_id=c.site_id AND s.room_id=c.room_id"
                + " WHERE c.site_id=? AND s.mate_id=?"
                + " ORDER BY c.ctime";
            row = cha.db.fetchOne(sql, sid, mid /**/);
            tim = Synt.declare(row.get("ctime"),  0 );
            msg = Synt.declare(row.get("note" ),  "");
            mat = Synt.declare(row.get("mate_id"),"");
        } else {
            sql = "SELECT SUM(s.fresh) AS fresh"
                + " FROM `"+sta.tableName+"` AS s"
                + " WHERE s.site_id=? AND s.mate_id=? AND s.room_id=?";
            row = sta.db.fetchOne(sql, sid, mid, rid);
            num = Synt.declare(row.get("fresh"),  0 );

            sql = "SELECT c.ctime,c.note,c.mate_id"
                + " FROM `"+cha.tableName+"` AS c"
                + " INNER JOIN `"+sta.tableName+"` AS s ON s.site_id=c.site_id AND s.room_id=c.room_id"
                + " WHERE c.site_id=? AND s.mate_id=? AND c.room_id=?"
                + " ORDER BY c.ctime";
            row = cha.db.fetchOne(sql, sid, mid, rid);
            tim = Synt.declare(row.get("ctime"),  0 );
            msg = Synt.declare(row.get("note" ),  "");
            mat = Synt.declare(row.get("mate_id"),"");
        }

        helper.reply("", Synt.mapOf(
            "fresh", num,
            "mtime", tim,
            "last_note" , msg,
            "last_mate" , mat
        ));
    }

    @Action("stat/create")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    public void createStat(ActionHelper helper) throws HongsException {
        Model  mod = DB.getInstance("masque").getModel("stat");
        Map    req = helper.getRequestData( );

        /**
         * 等级: 1 原始, 2 临时
         * 要登记关系必须要给出原始口令,
         * 即必须由客户的服务端发起才行.
         * createRoom,createMate 同此.
         */
        if (Synt.declare(req.get("token_level"), 0) != 1) {
            helper.fault(CoreLocale.getInstance("masque")
                  .translate( "core.masque.wrong.level" ) );
            return;
        }

        /**
         * 0x104e 表示 SQL 错误,
         * 当触发唯一索引约束时就会抛出,
         * 此处用于区分记录是否已经存在.
         * createRoom,createMate 同此.
         */
        try {
            mod.add(req);
            helper.reply("", 1);
        } catch ( HongsException e) {
        if (e.getErrno() == 0x104e) {
            helper.reply("", 0);
        } else {
            helper.fault(e.getLocalizedMessage());
        }}
    }

    @Action("room/search")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    public void searchRoom(ActionHelper helper) throws HongsException {
        Map    req = helper.getRequestData( );
        Set    ids = Synt.asSet(req.get("room_id"));
        String sid = ( String ) req.get("site_id");

        /**
         * 通过定制接口获取信息.
         * 注意: 返回的是字符串,
         * 直接输出, 无法再加工.
         */
        String rst = getForeData("room", sid, ids);
        if ( null != rst) {
            helper.print(rst);
            return;
        }

        Model  mod = DB.getInstance("masque").getModel("room");
        Map    rsp = mod.search(req);
        helper.reply(rsp);
    }

    @Action("room/create")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    public void createRoom(ActionHelper helper) throws HongsException {
        Model  mod = DB.getInstance("masque").getModel("room");
        Map    req = helper.getRequestData( );

        // 需要原始 token 才能访问
        if (Synt.declare(req.get("token_level"), 0) != 1) {
            helper.fault(CoreLocale.getInstance("masque")
                  .translate( "core.masque.wrong.level" ) );
            return;
        }

        try {
            Map row = mod.table.fetchCase()
                .filter("site_id = ?", req.get("site_id") )
                .filter("room_id = ?", req.get("room_id") )
                .select("id")
                .getOne();
            if (row != null && ! row.isEmpty(/**/)) {
                String id = (String) row.get("id");
                mod.put(id, req);
            } else {
                mod.add(/**/req);
            }
            helper.reply("", 1 );
        } catch ( HongsException e) {
        if (e.getErrno() == 0x104e) {
            helper.reply("", 0 );
        } else {
            helper.fault(e.getLocalizedMessage());
        }}
    }

    @Action("mate/search")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    public void searchMate(ActionHelper helper) throws HongsException {
        Map    req = helper.getRequestData( );
        Set    ids = Synt.asSet(req.get("mate_id"));
        String sid = ( String ) req.get("site_id");

        /**
         * 通过定制接口获取信息.
         * 注意: 返回的是字符串,
         * 直接输出, 无法再加工.
         */
        String rst = getForeData("mate", sid, ids);
        if ( null != rst) {
            helper.print(rst);
            return;
        }

        Model  mod = DB.getInstance("masque").getModel("mate");
        Map    rsp = mod.search(req);
        helper.reply(rsp);
    }

    @Action("mate/create")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    public void createMate(ActionHelper helper) throws HongsException {
        Model  mod = DB.getInstance("masque").getModel("mate");
        Map    req = helper.getRequestData( );

        // 需要原始 token 才能访问
        if (Synt.declare(req.get("token_level"), 0) != 1) {
            helper.fault(CoreLocale.getInstance("masque")
                  .translate( "core.masque.wrong.level" ) );
            return;
        }

        try {
            Map row = mod.table.fetchCase()
                .filter("site_id = ?", req.get("site_id") )
                .filter("mate_id = ?", req.get("mate_id") )
                .select("id")
                .getOne();
            if (row != null && ! row.isEmpty(/**/)) {
                String id = (String) row.get("id");
                mod.put(id, req);
            } else {
                mod.add(/**/req);
            }
            helper.reply("", 1 );
        } catch ( HongsException e) {
        if (e.getErrno() == 0x104e) {
            helper.reply("", 0 );
        } else {
            helper.fault(e.getLocalizedMessage());
        }}
    }

    @Action("token/create")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    public void createToken(ActionHelper helper) throws HongsException {
        Map    req = helper.getRequestData( );
        String tok = (String)req.get("token");

        // 需要原始 token 才能访问
        if (Synt.declare(req.get("token_level"), 0) != 1) {
            helper.fault(CoreLocale.getInstance("masque")
                  .translate( "core.masque.wrong.level" ) );
            return;
        }

        // 生成临时 token 一天有效
        String sec = Digest.md5(tok+"."+Core.newIdentity());
        Record.put( "masque.token."+sec, tok, (3600 * 24) );

        helper.reply(Synt.mapOf(
            "token", sec
        ));
    }

    @Action("token/delete")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    public void deleteToken(ActionHelper helper) throws HongsException {
        Map    req = helper.getRequestData( );
        String tok = (String)req.get("token");

        // 仅为临时 token 才能访问
        if (Synt.declare(req.get("token_level"), 0) != 2) {
            helper.fault(CoreLocale.getInstance("masque")
                  .translate( "core.masque.wrong.level" ) );
            return;
        }

        // 移除当前 token
        Record.del( "masque.token."+tok );

        helper.reply("");
    }

    /**
     * 调用客户自定接口获取 mate 或 room 的信息
     * @param tab 取值: room,mate
     * @param sid
     * @param ids
     * @return
     * @throws HongsException
     */
    private String getForeData(String tab, String sid, Set ids) throws HongsException {
        Map row = DB.getInstance("masque")
            .with  ("site")
            .field (tab + "_url")
            .where ("id=?", sid )
            .getOne(  );
        if (row.isEmpty()) {
            return null;
        }
        String url = (String) row.get(tab + "_url");

        // 从数据库获取
        if (url == null || url.isEmpty()) {
            List list = DB.getInstance("masque")
                .with  (tab)
                .where (tab +"_id IN (?)", ids )
                .getAll(   );
            // 补全头像链接
            for (Map info : ( List<Map> ) list ) {
                info.put("icon", fixHostName(info.get("icon")));
            }
            return Data.toString(Synt.mapOf(
                "ok"  , true,
                "list", list
            ));
        }

        // 调用内部对象
        if (url.startsWith( "class://" )) {
            try {
                Object obj = Core.getInstance(url.substring(8));
                Object rst = ( (Function) obj).apply(Synt.mapOf(
                    "site_id", sid,
                    tab+"_id", ids
                ));
                return Data.toString(rst);
            }
            catch (ClassCastException ex) {
                throw new HongsException.Common(ex);
            }
        }

        // 调用外部接口
        return Remote.post(url, Synt.mapOf(
            tab+"_ids[]" , ids
        ));
    }

    /**
     * 补全相对路径的域名和前缀
     * @param val
     * @return
     */
    private String fixHostName(Object val) {
        String url = (String) val;
        if (url == null || url.isEmpty( )) {
            return url;
        }
        if (PRE.matcher(url).find()) {
            return url;
        } else
        if (url.startsWith ( "/" ) ) {
            return Core.SITE_HREF
                 + url;
        } else {
            return Core.SITE_HREF
                 + Core.BASE_HREF
                 + "/"
                 + url;
        }
    }
    private static final Pattern PRE = Pattern.compile("^(\\w+:)?//");

}
