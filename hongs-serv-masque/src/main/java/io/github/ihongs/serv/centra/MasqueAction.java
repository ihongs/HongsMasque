package io.github.ihongs.serv.centra;

import io.github.ihongs.Core;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.Preset;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Model;
import io.github.ihongs.util.Digest;
import java.util.Map;
import java.util.Random;

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
    throws CruxException {
        Model  ett = DB.getInstance("masque").getModel("site");
        Map    req = helper.getRequestData();
        Map    rsp = ett.search(req);
        helper.reply(rsp);
    }

    @Action("site/recite")
    @Preset(conf="masque", form="site")
    @Select(conf="masque", form="site")
    public void reciteSite(ActionHelper helper)
    throws CruxException {
        Model  ett = DB.getInstance("masque").getModel("site");
        Map    req = helper.getRequestData();
        Map    rsp = ett.recite(req);
        helper.reply(rsp);
    }

    @Action("site/create")
    @Preset(conf="masque", form="site", defs={".defence"})
    @Verify(conf="masque", form="site")
    @CommitSuccess
    public void createSite(ActionHelper helper)
    throws CruxException {
        Model  ett = DB.getInstance("masque").getModel("site");
        Map    req = helper.getRequestData();

        // 自动生成秘钥
//      if ("-".equals(req.get("sk"))) {
            req.put("sk", Digest.md5(new Random().nextLong() + Core.newIdentity()));
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
    throws CruxException {
        Model  ett = DB.getInstance("masque").getModel("site");
        Map    req = helper.getRequestData();

        // 重新生成秘钥
        if ("-".equals(req.get("sk"))) {
            req.put("sk", Digest.md5(new Random().nextLong() + Core.newIdentity()));
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
    throws CruxException {
        Model  ett = DB.getInstance("masque").getModel("site");
        Map    req = helper.getRequestData();
        int    num = ett.delete(req);
        helper.reply("", num);
    }

    //** 人员 **/

    @Action("mate/search")
    @Preset(conf="masque", form="mate")
    @Select(conf="masque", form="mate")
    public void searchMate(ActionHelper helper)
    throws CruxException {
        Model  ett = DB.getInstance("masque").getModel("mate");
        Map    req = helper.getRequestData();
        Map    rsp = ett.search(req);
        helper.reply(rsp);
    }

    @Action("mate/recite")
    @Preset(conf="masque", form="mate")
    @Select(conf="masque", form="mate")
    public void reciteMate(ActionHelper helper)
    throws CruxException {
        Model  ett = DB.getInstance("masque").getModel("mate");
        Map    req = helper.getRequestData();
        Map    rsp = ett.recite(req);
        helper.reply(rsp);
    }

    @Action("mate/create")
    @Preset(conf="masque", form="mate", defs={".defence"})
    @Verify(conf="masque", form="mate")
    @CommitSuccess
    public void createMate(ActionHelper helper)
    throws CruxException {
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
    throws CruxException {
        Model  ett = DB.getInstance("masque").getModel("mate");
        Map    req = helper.getRequestData();
        int    num = ett.update(req);
        helper.reply("", num);
    }

    @Action("mate/delete")
    @Preset(conf="masque", form="mate", defs={".defence"})
    @CommitSuccess
    public void deleteMate(ActionHelper helper)
    throws CruxException {
        Model  ett = DB.getInstance("masque").getModel("mate");
        Map    req = helper.getRequestData();
        int    num = ett.delete(req);
        helper.reply("", num);
    }

}
