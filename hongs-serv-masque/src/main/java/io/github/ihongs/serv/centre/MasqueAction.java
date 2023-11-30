package io.github.ihongs.serv.centre;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Model;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.db.util.FetchMore;
import io.github.ihongs.dh.Roster;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Digest;
import io.github.ihongs.util.Remote;
import io.github.ihongs.util.Synt;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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
    public void searchChat(ActionHelper helper) throws CruxException {
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
            String mid = (String) req.get("mine_id"); // 当前用户
            long   now = Core.ACTION_TIME.get()/1000;
            sta.db.updates (sql, now, sid, rid, mid);
        }
    }

    @Action("stat/search")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    public void searchStat(ActionHelper helper) throws CruxException {
        Map    req = helper.getRequestData( );
        String sid = (String) req.get("site_id");
        String rid = (String) req.get("room_id");
        String mid = (String) req.get("mine_id"); // 仅能获取当前用户的状态

        /**
         * room_id 为:
         * '.' 返回分页状态列表数据,
         * '!' 统计全部未读消息数量.
         */
        if (".".equals(rid)) {
            req = new HashMap (req);
            req.put("mate_id", mid);
            req.remove( "room_id" );
            req.remove(      "id" );

            Set ob  = Synt.toTerms(req.get(Cnst.OB_KEY));
            if (ob == null || ob.isEmpty()) {
                ob  = Synt.setOf("-mtime");
                req.put( Cnst.OB_KEY, ob );
            }

            Set rb  = Synt.toTerms(req.get(Cnst.RB_KEY));
            if (rb == null || ob.isEmpty()) {
                rb  = Synt.setOf( "fresh" , "mtime" , "last_chat");
                req.put( Cnst.RB_KEY, rb );
            }
            rb.add("room_id");

            // 获取状态列表
            Model  mod = DB.getInstance("masque").getModel("stat");
            Map    rsp = mod.search( req );
            List<Map> ls = (List) rsp.get("list");

            if (ls != null && ! ls.isEmpty()) {
                // 追加最新消息
                if (rb.contains("last_chat")) {
                    addLastChat(sid, ls);
                }
                // 追加频道信息
                if (rb.contains("room")) {
                    addRoomInfo(sid, ls);
                }
                // 追加用户信息
                if (rb.contains("mate")) {
                    addMateInfo(sid, ls);
                }
            }

            helper.reply(rsp);
            return;
        }

        Set rb  = Synt.toTerms(req.get(Cnst.RB_KEY));
        if (rb == null || rb.isEmpty()) {
            rb  = Synt.setOf( "fresh" , "mtime" , "last_chat");
        }

        Table  sta = DB.getInstance("masque").getTable("stat");
        Table  cha = DB.getInstance("masque").getTable("chat");
        String sql;
        Map    row;
        Map    rst;

        if ("!".equals(rid)) {
            if (rb.contains("mtime" )
            ||  rb.contains("fresh")) {
                sql = "SELECT MAX(s.mtime) AS mtime, SUM(s.fresh) AS fresh"
                    + " FROM `"+sta.tableName+"` AS s"
                    + " WHERE s.site_id=? AND s.mate_id=?";
                row = sta.db.fetchOne (sql, sid, mid /**/);
                rst = Synt.mapOf(
                    "mtime"  , Synt.declare(row.get("mtime"  ), 0L),
                    "fresh"  , Synt.declare(row.get("fresh"  ), 0 )
                );
            } else {
                rst = new HashMap();
            }

            // 追加最新消息
            if (rb.contains("last_chat")) {
                sql = "SELECT s.room_id,c.mate_id,c.ctime,c.kind,c.note"
                    + " FROM `"+cha.tableName+"` AS c"
                    + " INNER JOIN `"+sta.tableName+"` AS s ON s.site_id=c.site_id AND s.room_id=c.room_id"
                    + " WHERE c.site_id=? AND s.mate_id=?"
                    + " ORDER BY c.ctime";
                row = cha.db.fetchOne (sql, sid, mid /**/);
                rst.put("room_id"  , row.get( "room_id" ));
                rst.put("mate_id"  , row.get( "mate_id" ));
                rst.put("last_chat", row);

                List<Map> ls = Synt.listOf(rst);
                // 追加频道信息
                if (rb.contains("room")) {
                    addRoomInfo(sid, ls);
                }
                // 追加用户信息
                if (rb.contains("mate")) {
                    addMateInfo(sid, ls);
                }
            }
        } else {
            if (rb.contains("mtime" )
            ||  rb.contains("fresh")) {
                sql = "SELECT MAX(s.mtime) AS mtime, SUM(s.fresh) AS fresh"
                    + " FROM `"+sta.tableName+"` AS s"
                    + " WHERE s.site_id=? AND s.mate_id=? AND s.room_id=?";
                row = sta.db.fetchOne (sql, sid, mid, rid);
                rst = Synt.mapOf(
                    "mtime"  , Synt.declare(row.get("mtime"  ), 0L),
                    "fresh"  , Synt.declare(row.get("fresh"  ), 0 )
                );
            } else {
                rst = new HashMap();
            }

            // 追加最新消息
            if (rb.contains("last_chat")) {
                sql = "SELECT s.room_id,c.mate_id,c.ctime,c.kind,c.note"
                    + " FROM `"+cha.tableName+"` AS c"
                    + " INNER JOIN `"+sta.tableName+"` AS s ON s.site_id=c.site_id AND s.room_id=c.room_id"
                    + " WHERE c.site_id=? AND s.mate_id=? AND c.room_id=?"
                    + " ORDER BY c.ctime";
                row = cha.db.fetchOne (sql, sid, mid, rid);
                rst.put("room_id"  , row.get( "room_id" ));
                rst.put("mate_id"  , row.get( "mate_id" ));
                rst.put("last_chat", row);

                List<Map> ls = Synt.listOf(rst);
                // 追加频道信息
                if (rb.contains("room")) {
                    addRoomInfo(sid, ls);
                }
                // 追加用户信息
                if (rb.contains("mate")) {
                    addMateInfo(sid, ls);
                }
            }
        }

        helper.reply(Synt.mapOf("info", rst));
    }

    @Action("stat/create")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    @CommitSuccess
    public void createStat(ActionHelper helper) throws CruxException {
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

        req.put("mate_id", req.get("mine_id")); // 当前用户

        /**
         * 1045 表示 SQL 错误,
         * 当触发唯一索引约束时就会抛出,
         * 此处用于区分记录是否已经存在.
         * createRoom,createMate 同此.
         */
        try {
            mod.add(req);
            helper.reply("", 1);
        } catch (CruxException e) {
        if (e.getErrno( ) == 1045) {
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
    public void searchRoom(ActionHelper helper) throws CruxException {
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
            helper.write("application/json" , rst);
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
    @CommitSuccess
    public void createRoom(ActionHelper helper) throws CruxException {
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
        } catch (CruxException e) {
        if (e.getErrno( ) == 1045) {
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
    public void searchMate(ActionHelper helper) throws CruxException {
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
            helper.write("application/json" , rst);
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
    @CommitSuccess
    public void createMate(ActionHelper helper) throws CruxException {
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
        } catch (CruxException e) {
        if (e.getErrno( ) == 1045) {
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
    @CommitSuccess
    public void createToken(ActionHelper helper) throws CruxException {
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
        Roster.put( "masque.token."+sec, tok, (3600 * 24) );

        helper.reply(Synt.mapOf(
            "token", sec
        ));
    }

    @Action("token/delete")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    @CommitSuccess
    public void deleteToken(ActionHelper helper) throws CruxException {
        Map    req = helper.getRequestData( );
        String tok = (String)req.get("token");

        // 仅为临时 token 才能访问
        if (Synt.declare(req.get("token_level"), 0) != 2) {
            helper.fault(CoreLocale.getInstance("masque")
                  .translate( "core.masque.wrong.level" ) );
            return;
        }

        // 移除当前 token
        Roster.del( "masque.token."+tok );

        helper.reply("");
    }

    /**
     * 追加最新消息
     * @param sid
     * @param ls
     * @throws CruxException
     */
    private void addLastChat(String sid, List<Map> ls) throws CruxException {
        Table tb = DB.getInstance("masque")
                     .getTable   ( "chat" );
        try (
            PreparedStatement ps = tb.db.prepare(
                "SELECT `mate_id`,`ctime`,`kind`,`note`"
              +  " FROM `"  + tb.tableName +  "`"
              + " WHERE `site_id`=?"
              +   " AND `room_id`=?"
              + " ORDER BY `ctime` DESC"
              + " LIMIT 1" );
        )   {
                Object   rid;
                ResultSet rs;
                ps.setString( 1 , sid );
                Map em = new HashMap( );
            for(Map ra : ls) {
                rid = ra.get("room_id");
                ps.setObject( 2 , rid );
                rs  = ps.executeQuery();
                if (! rs.next() ) {
                    ra.put("mate_id"  , "" );
                    ra.put("last_chat", em );
                } else {
                    ra.put("mate_id"  , rs.getString(1));
                    ra.put("last_chat", Synt.mapOf(
                           "mate_id"  , rs.getString(1) ,
                           "ctime"    , rs.getLong  (2) ,
                           "kind"     , rs.getString(3) ,
                           "note"     , rs.getString(4)
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new CruxException(ex);
        }
    }

    /**
     * 追加频道信息
     * @param sid
     * @param ls
     * @throws CruxException
     */
    private void addRoomInfo(String sid, List<Map> ls) throws CruxException {
        Table tb = DB.getInstance("masque")
                     .getTable   ( "room" );
        new FetchMore(ls).join(tb,
        new FetchCase(  )
            .select("`room_id`,`name`,`icon`,`note`")
            .filter("`site_id` = ?", sid),
            "room_id", "room_id"
        );
    }

    /**
     * 追加用户信息
     * @param sid
     * @param ls
     * @throws CruxException
     */
    private void addMateInfo(String sid, List<Map> ls) throws CruxException {
        Table tb = DB.getInstance("masque")
                     .getTable   ( "mate" );
        new FetchMore(ls).join(tb,
        new FetchCase(  )
            .select("`mate_id`,`name`,`icon`,`note`")
            .filter("`site_id` = ?", sid),
            "mate_id", "mate_id"
        );
    }

    /**
     * 调用客户自定接口获取 mate 或 room 的信息
     * @param tab 取值: room,mate
     * @param sid
     * @param ids
     * @return
     * @throws CruxException
     */
    private String getForeData(String tab, String sid, Set ids) throws CruxException {
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
            for(Map info  : ( List<Map> ) list ) {
            String  icon  = (String)  info.get("icon");
                if (icon != null && ! icon.isEmpty() ) {
                    icon  = ActionDriver.fixUrl( url );
                }
                info.put("icon", icon);
            }
            return Dist.toString(Synt.mapOf(
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
                return Dist.toString(rst);
            }
            catch (ClassCastException ex) {
                throw new CruxException(ex);
            }
        }

        // 调用外部接口
        return Remote.post(url, Synt.mapOf(
            tab+"_ids[]" , ids
        ));
    }

}
