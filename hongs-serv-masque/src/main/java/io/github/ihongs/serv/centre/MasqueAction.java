package io.github.ihongs.serv.centre;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.VerifyHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Model;
import io.github.ihongs.db.Table;
import io.github.ihongs.dh.Roster;
import io.github.ihongs.serv.masque.Masque;
import io.github.ihongs.util.Digest;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.verify.Wrongs;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 常规消息接口
 * @author hong
 */
@Action("centre/masque")
public class MasqueAction {

    @Action("auth/create")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    @CommitSuccess
    public void createAuth(ActionHelper helper) throws CruxException {
        Map    req = helper.getRequestData( );
        String tok = (String)req.get("token");

        /**
         * 等级: 1 原始, 2 临时
         * 要开启对话必须给出原始的口令,
         * 即必须由客户的服务端发起才行.
         */
        if (Synt.declare(req.get("token_level"), 0) != 1) {
            helper.fault(CoreLocale.getInstance("masque")
                  .translate( "core.masque.wrong.level" ) );
            return;
        }

        String mid = (String) req.get("mate_id");
        String rid = (String) req.get("meet_id");

        // 获取当前 socket
        String sok = Masque.getTunnel().socket(mid , rid);

        // 生成临时 token
        String sec = Digest.md5(Core.newIdentity()+"/"+tok);
        String rec = Digest.md5( mid + "/" + rid + "/"+tok);
        Roster.put( "masque.token."+sec, rec, 3960);

        // 注册状态
        Boolean cl = Synt.asBool(req.get("clue"));
        if (cl != null) {
            Table tab = DB.getInstance("masque").getTable("clue");
            if (cl) {
                try {
                    long now = System.currentTimeMillis();
                    tab.db.updates(
                        "INSERT INTO `"+tab.tableName+"` (`mate_id`, `meet_id`, `mtime`) VALUES (?, ?, ?)",
                         mid, rid, now
                    );
                } catch (CruxException e) {
                if (e.getErrno() != 1045) {
                    helper.fault(e.getLocalizedMessage());
                }}
            } else {
                try {
                    tab.db.updates(
                        "DELETE FROM `"+tab.tableName+"` WHERE `mate_id` = ? AND `meet_id` = ?",
                         mid, rid
                    );
                } catch (CruxException e) {
                    helper.fault(e.getLocalizedMessage());
                }
            }
        }

        helper.reply(Synt.mapOf(
            "socket", sok,
            "token" , sec
        ));
    }

    @Action("auth/remove")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    @CommitSuccess
    public void removeAuth(ActionHelper helper) throws CruxException {
        Map    req = helper.getRequestData( );

        // 需要原始 token 才能访问
        if (Synt.declare(req.get("token_level"), 0) != 1) {
            helper.fault(CoreLocale.getInstance("masque")
                  .translate( "core.masque.wrong.level" ) );
            return;
        }

        String rid = (String) req.get("room_id");
        String mid = (String) req.get("mate_id");

        // 移除会话
        Masque.getTunnel().remove(rid, mid);

        // 移除状态
        Table  tab = DB.getInstance("masque").getTable("clue");
        tab.delete("`room_id` = ? AND `mate_id` = ?", rid,mid);

        helper.reply("");
    }

    @Action("auth/update")
    public void updateAuth(ActionHelper helper) throws CruxException {
        Map    req = helper.getRequestData( );
        String tok = (String)req.get("token");

        // 仅为临时 token 才能访问
        if (Synt.declare(req.get("token_level"), 0) != 2) {
            helper.fault(CoreLocale.getInstance("masque")
                  .translate( "core.masque.wrong.level" ) );
            return;
        }

        // 延时当前 token
        Roster.put("masque.token."+tok, 3960);

        helper.reply("");
    }

    @Action("auth/delete")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    @CommitSuccess
    public void deleteAuth(ActionHelper helper) throws CruxException {
        Map    req = helper.getRequestData( );
        String tok = (String)req.get("token");

        // 仅为临时 token 才能访问
        if (Synt.declare(req.get("token_level"), 0) != 2) {
            helper.fault(CoreLocale.getInstance("masque")
                  .translate( "core.masque.wrong.level" ) );
            return;
        }

        // 移除当前 token
        Roster.del("masque.token."+tok);

        helper.reply("");
    }

    @Action("clue/create")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    @CommitSuccess
    public void createClue(ActionHelper helper) throws CruxException {
        Map    req = helper.getRequestData( );

        // 需要原始 token 才能访问
        if (Synt.declare(req.get("token_level"), 0) != 1) {
            helper.fault(CoreLocale.getInstance("masque")
                  .translate( "core.masque.wrong.level" ) );
            return;
        }

        String mid = (String) req.get("mate_id");
        String rid = (String) req.get("meet_id");

        /**
         * 1045 表示 SQL 错误,
         * 当触发唯一索引约束时就会抛出,
         * 此处用于区分记录是否已经存在.
         */
        Table  tab = DB.getInstance("masque").getTable("clue");
        long   now = System.currentTimeMillis( );
        try {
            int  n = tab.db.updates("INSERT INTO `"+tab.tableName+"` (`mate_id`, `meet_id`, `mtime`) VALUES (?, ?, ?)",
                 mid, rid, now
            );
            helper.reply( "" , n);
        } catch (CruxException e) {
        if (e.getErrno() == 1045) {
            helper.reply( "" , 0);
        } else {
            helper.fault(e.getLocalizedMessage());
        }}
    }

    @Action("clue/delete")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    @CommitSuccess
    public void deleteClue(ActionHelper helper) throws CruxException {
        Map    req = helper.getRequestData( );

        // 需要原始 token 才能访问
        if (Synt.declare(req.get("token_level"), 0) != 1) {
            helper.fault(CoreLocale.getInstance("masque")
                  .translate("core.masque.wrong.level") );
            return;
        }

        String mid = (String) req.get("mate_id");
        String rid = (String) req.get("meet_id");

        Table  tab = DB.getInstance("masque").getTable("clue");
        try {
            int n = tab.db.updates(
                "DELETE FROM `"+tab.tableName+"` WHERE `mate_id` = ? AND `meet_id` = ?",
                mid, rid
            );
            helper.reply( "" , n);
        } catch (CruxException e) {
            helper.fault(e.getLocalizedMessage());
        }
    }

    @Action("clue/search")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    public void searchClue(ActionHelper helper) throws CruxException {
        Map    req = helper.getRequestData( );
        String mid = (String) req.get("mate_id");
        String rid = (String) req.get("meet_id");

        Set rb  = Synt.toTerms(req.get(Cnst.RB_KEY));
        if (rb == null || rb.isEmpty()) {
            rb  = Synt.setOf("unread", "unsend", "mtime", "last_chat");
        }

        Table  sta = DB.getInstance("masque").getTable("clue");
        Table  cha = DB.getInstance("masque").getTable("chat");
        String sql;
        Map    row;
        Map    inf;

        if (Masque.ROOM_ALL.equals(rid)) {
            if (rb.contains("unread")
            ||  rb.contains("unsend")
            ||  rb.contains("mtime")) {
                sql = "SELECT MAX(mtime) AS mtime, SUM(unread) AS unread, SUM(unsend) AS unsend"
                    + " FROM `"+sta.tableName+"`"
                    + " WHERE mate_id = ?"
                    + " GROUP BY mate_id";
                row = sta.db.fetchOne(sql, mid);
                inf = Synt.mapOf(
                    "mtime" , Synt.declare(row.get("mtime" ), 0L),
                    "unread", Synt.declare(row.get("unread"), 0 ),
                    "unsend", Synt.declare(row.get("unsend"), 0 )
                );
            } else {
                inf = new HashMap(1);
            }

            // 追加最新消息
            if (rb.contains("last_chat")) {
                sql = "SELECT *"
                    + " FROM `"+cha.tableName+"`"
                    + " WHERE mate_id = ?"
                    + " ORDER BY ctime DESC";
                row = cha.db.fetchOne(sql, mid);
                inf.put("last_chat" , row);
            }
        } else {
            if (rb.contains("unread")
            ||  rb.contains("unsend")
            ||  rb.contains("mtime")) {
                sql = "SELECT mtime, unread, unsend"
                    + " FROM `"+sta.tableName+"`"
                    + " WHERE mate_id = ?"
                    +   " AND meet_id = ?";
                row = sta.db.fetchOne(sql, mid, rid);
                inf = Synt.mapOf(
                    "mtime" , Synt.declare(row.get("mtime" ), 0L),
                    "unread", Synt.declare(row.get("unread"), 0 ),
                    "unsend", Synt.declare(row.get("unsend"), 0 )
                );
            } else {
                inf = new HashMap(1);
            }

            // 追加最新消息
            if (rb.contains("last_chat")) {
                sql = "SELECT *"
                    + " FROM `"+cha.tableName+"`"
                    + " WHERE mate_id = ?"
                    +   " AND meet_id = ?"
                    + " ORDER BY ctime DESC";
                row = cha.db.fetchOne(sql, mid, rid);
                inf.put("last_chat" , row);
            }
        }

        helper.reply(Synt.mapOf("info", inf));
    }

    @Action("chat/search")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    public void searchChat(ActionHelper helper) throws CruxException {
        Map    req = helper.getRequestData( );
        String mid = (String) req.get("mate_id");
        String rid = (String) req.get("meet_id");

        // 不限用户
        req.remove("mate_id");

        // 默认逆序
        if ( ! req.containsKey(Cnst.OB_KEY) ) {
            req.put(Cnst.OB_KEY, Synt.setOf("ctime!"));
        }
        // 默认字段
        if ( ! req.containsKey(Cnst.RB_KEY) ) {
            req.put(Cnst.RB_KEY, Synt.setOf("ctime", "id", "kind", "text", "data"));
        }

        Model  mod = DB.getInstance("masque").getModel("chat");
        Table  tab = DB.getInstance("masque").getTable("clue");

        helper.reply(mod.search(req));

        // 消除未读
        Set ab  = Synt.toTerms(req.get(Cnst.AB_KEY));
        if (ab != null && ab.contains (  "clean"  )) {
            String sql = "UPDATE `"+tab.tableName+"` SET `unread`=0,`unsend`=0,`mtime`=?"
                       + " WHERE (`unread`!=0 OR `unsend`!=0) AND `mate_id`=? AND `room_id`=?";
            long   now = System.currentTimeMillis( );
            tab.db.updates (sql, now, mid, rid);
        }
    }

    @Action("chat/create")
    @Verify(
        conf = "masque",
        form = "auth"
    )
    public void createChat(ActionHelper helper) throws CruxException {
        Map req  = helper.getRequestData();
        Set  ab  = Synt.toTerms(req.get(Cnst.AB_KEY));
        byte em  = 0;
        if ( ab != null) {
        if ( ab.contains(".errs")) {
             em  = 1;
        } else
        if ( ab.contains("!errs")) {
             em  = 2;
        }}

        // 校验数据
        Map info ;
        try {
            info = new VerifyHelper()
                .addRulesByForm("masque", "chat")
                .verify (req, false, em  ==  0  );
        }
        catch (Wrongs wr) {
            helper.reply( wr.toReply(em) );
            return;
        }
        
        Model  mod = DB.getInstance("masque").getModel("chat");
        String  id = mod.create(req);
        helper.reply("", id);

        // 推入管道
        Masque.getTunnel( ).accept( info );
    }

}
