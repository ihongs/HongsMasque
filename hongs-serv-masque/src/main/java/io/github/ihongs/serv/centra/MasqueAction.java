package io.github.ihongs.serv.centra;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.Preset;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Model;
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
    @Preset(conf="masque", form="site", defs={":defence"})
    @Verify(conf="masque", form="site")
    @CommitSuccess
    public void createSite(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("site");
        Map    req = helper.getRequestData();
        Map    rsp = ett.create(req);
        helper.reply("", rsp);
    }

    @Action("site/update")
    @Preset(conf="masque", form="site", defs={":defence"})
    @Verify(conf="masque", form="site")
    @CommitSuccess
    public void updateSite(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("site");
        Map    req = helper.getRequestData();
        int    num = ett.update(req);
        helper.reply("", num);
    }

    @Action("site/delete")
    @Preset(conf="masque", form="site", defs={":defence"})
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
    @Preset(conf="masque", form="room", defs={":defence"})
    @Verify(conf="masque", form="room")
    @CommitSuccess
    public void createRoom(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("room");
        Map    req = helper.getRequestData();
        Map    rsp = ett.create(req);
        helper.reply("", rsp);
    }

    @Action("room/update")
    @Preset(conf="masque", form="room", defs={":defence"})
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
    @Preset(conf="masque", form="room", defs={":defence"})
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
    @Preset(conf="masque", form="mate", defs={":defence"})
    @Verify(conf="masque", form="mate")
    @CommitSuccess
    public void createMate(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("mate");
        Map    req = helper.getRequestData();
        Map    rsp = ett.create(req);
        helper.reply("", rsp);
    }

    @Action("mate/update")
    @Preset(conf="masque", form="mate", defs={":defence"})
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
    @Preset(conf="masque", form="mate", defs={":defence"})
    @CommitSuccess
    public void deleteMate(ActionHelper helper)
    throws HongsException {
        Model  ett = DB.getInstance("masque").getModel("mate");
        Map    req = helper.getRequestData();
        int    num = ett.delete(req);
        helper.reply("", num);
    }

}
