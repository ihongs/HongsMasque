package io.github.ihongs.serv.centra;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.Preset;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Model;
import io.github.ihongs.dh.Stores;
import io.github.ihongs.util.Digest;
import io.github.ihongs.util.Synt;
import java.util.Map;

/**
 * 消息管理接口
 * @author hong
 */
@Action("centra/masque")
public class MasqueAction {

    //** 站点 **/

    @Action("site/search")
    @Preset(conf="masque", form="site")
    @Select(conf="masque", form="site")
    public void searchSite(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("site");
        Map    req = helper.getRequestData();
        Map    rsp = ett.search(req);
        helper.reply(rsp);
    }

    @Action("site/create")
    @Preset(conf="masque", form="site", defs={".defence"})
    @Verify(conf="masque", form="site")
    @CommitSuccess
    public void createSite(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("site");
        Map    req = helper.getRequestData();

        // 自动生成秘钥
//      if ("-".equals(req.get("sk"))) {
            req.put("sk", Digest.md5(Core.newIdentity()));
//      } else {
//          req.remove("sk");
//      }

        String nid = ett.create(req);
        helper.reply("", nid);
    }

    @Action("site/update")
    @Preset(conf="masque", form="site", defs={".defence"})
    @Verify(conf="masque", form="site")
    @CommitSuccess
    public void updateSite(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("site");
        Map    req = helper.getRequestData();

        // 重新生成秘钥
        if ("-".equals(req.get("sk"))) {
            req.put("sk", Digest.md5(Core.newIdentity()));
        } else {
            req.remove("sk");
        }

        int    num = ett.update(req);
        helper.reply("", num);
    }

    @Action("site/delete")
    @Preset(conf="masque", form="site", defs={".defence"})
    @CommitSuccess
    public void deleteSite(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("site");
        Map    req = helper.getRequestData();
        int    num = ett.delete(req);
        helper.reply("", num);
    }

    //** 房间 **/

    @Action("room/search")
    @Preset(conf="masque", form="room")
    @Select(conf="masque", form="room")
    public void searchRoom(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("room");
        Map    req = helper.getRequestData();
        Map    rsp = ett.search(req);
        helper.reply(rsp);
    }

    @Action("room/create")
    @Preset(conf="masque", form="room", defs={".defence"})
    @Verify(conf="masque", form="room")
    @CommitSuccess
    public void createRoom(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("room");
        Map    req = helper.getRequestData();
        String nid = ett.create(req);
        helper.reply("", nid);
    }

    @Action("room/update")
    @Preset(conf="masque", form="room", defs={".defence"})
    @Verify(conf="masque", form="room")
    @CommitSuccess
    public void updateRoom(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("room");
        Map    req = helper.getRequestData();
        int    num = ett.update(req);
        helper.reply("", num);
    }

    @Action("room/delete")
    @Preset(conf="masque", form="room", defs={".defence"})
    @CommitSuccess
    public void deleteRoom(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("room");
        Map    req = helper.getRequestData();
        int    num = ett.delete(req);
        helper.reply("", num);
    }

    //** 人员 **/

    @Action("mate/search")
    @Preset(conf="masque", form="mate")
    @Select(conf="masque", form="mate")
    public void searchMate(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("mate");
        Map    req = helper.getRequestData();
        Map    rsp = ett.search(req);
        helper.reply(rsp);
    }

    @Action("mate/create")
    @Preset(conf="masque", form="mate", defs={".defence"})
    @Verify(conf="masque", form="mate")
    @CommitSuccess
    public void createMate(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("mate");
        Map    req = helper.getRequestData();
        String nid = ett.create(req);
        helper.reply("", nid);
    }

    @Action("mate/update")
    @Preset(conf="masque", form="mate", defs={".defence"})
    @Verify(conf="masque", form="mate")
    @CommitSuccess
    public void updateMate(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("mate");
        Map    req = helper.getRequestData();
        int    num = ett.update(req);
        helper.reply("", num);
    }

    @Action("mate/delete")
    @Preset(conf="masque", form="mate", defs={".defence"})
    @CommitSuccess
    public void deleteMate(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("mate");
        Map    req = helper.getRequestData();
        int    num = ett.delete(req);
        helper.reply("", num);
    }

    //** 口令 **/

    @Action("token/create")
    public void createToken(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("site");
        Map    req = helper.getRequestData();
        Object sid = req.get("site_id");
        Object mid = req.get("mine_id");
        Object rid = req.get("room_id");

        // 简单校验
        if (sid == null || "".equals(sid)
        ||  mid == null || "".equals(mid)
        ||  rid == null || "".equals(rid)) {
            helper.fault("site_id, mine_id and room_id required");
            return;
        }

        // 查询秘钥
        Map    row = ett.getOne(Synt.mapOf (
                Cnst.RB_KEY, Synt.setOf( "sk" ),
                Cnst.ID_KEY, req.get("site_id")
            ));
        String tok = (String) row.get("sk");
        if (tok == null || "".equals(tok)) {
            helper.fault("Site record is not exists");
            return;
        }
        tok = Digest.md5(sid+"/"+mid+"/"+rid+"/"+tok);

        // 生成临时 token 一天有效
        String sec = Digest.md5(tok+"."+Core.newIdentity());
        Stores.put( "masque.token."+sec, tok, (3600 * 24) );

        helper.reply(Synt.mapOf(
            "token", sec
        ));
    }

}
