package io.github.ihongs.serv.masque;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.reflex.Async;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.websocket.Session;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;

/**
 * 管道集合
 * @author hong
 */
public final class MasqueTunnel {

    /**
     * 后去接收管道对象
     * @return
     * @throws HongsException
     */
    public static Async<Map> getCeiver()
    throws HongsException {
        String name = Ceiver.class.getName();
        Ceiver inst = (Ceiver) Core.GLOBAL_CORE.got(name);
        if (inst == null) {
            CoreConfig conf = CoreConfig.getInstance("masque");
            inst =  new Ceiver( name,
                    conf.getProperty("core.masque.ceiver.max.tasks", Integer.MAX_VALUE),
                    conf.getProperty("core.masque.ceiver.max.servs", 1));
            Core.GLOBAL_CORE.put(name, inst);
        }
        return inst;
    }

    /**
     * 获取发送管道对象
     * @return
     * @throws HongsException
     */
    public static Async<Msg> getSender()
    throws HongsException {
        String name = Sender.class.getName();
        Sender inst = (Sender) Core.GLOBAL_CORE.got(name);
        if (inst == null) {
            CoreConfig conf = CoreConfig.getInstance("masque");
            inst =  new Sender( name,
                    conf.getProperty("core.masque.sender.max.tasks", Integer.MAX_VALUE),
                    conf.getProperty("core.masque.sender.max.servs", 2));
            Core.GLOBAL_CORE.put(name, inst);
        }
        return inst;
    }

    /**
     * 发送消息结构体
     */
    public static class Msg {

        final  String  msg ;
        final  String  url ;
        final  Session ses ;

        public Msg(String msg, String  url) {
            if ("".equals(url)) {
                 url = null;
            }
            this.msg = msg ;
            this.url = url ;
            this.ses = null;
        }

        public Msg(String msg, Session ses) {
            this.msg = msg ;
            this.ses = ses ;
            this.url = null;
        }

    }

    /**
     * 接收管道
     */
    private static class Ceiver
    extends Async<Map>
    implements Core.Singleton {

        private Ceiver(String name, int maxTasks, int maxServs)
        throws HongsException {
            super(name, maxTasks, maxServs);
        }

        @Override
        public void run(Map info) {
            try {
                MasqueTunnel.send(info);
            }
            catch (HongsException ex) {
                CoreLogger.error (ex);
            }
            finally {
                Core.getInstance (  ).close();
            }
        }

    }

    /**
     * 发送管道
     */
    private static class Sender
    extends Async<Msg>
    implements Core.Singleton {

        private Sender(String name, int maxTasks, int maxServs)
        throws HongsException {
            super(name, maxTasks, maxServs);
        }

        @Override
        public void run(Msg info) {
            try {
                MasqueTunnel.send(info);
            }
            catch (HongsException ex) {
                CoreLogger.error (ex);
            }
            finally {
                Core.getInstance (  ).close();
            }
        }

    }

    private static class ChatSet {

        private final DB db;
        private final Table ctb;
        private final Table stb;
        private final long  now;
        private final PreparedStatement upd;
        private final PreparedStatement ins;
        private String sid = null;
        private String rid = null;

        public ChatSet() throws HongsException {
            now = System.currentTimeMillis();
            db  = DB.getInstance( "masque" );
                  db.open();
            ctb = db.getTable("chat");
            stb = db.getTable("stat");
            upd = db.prepareStatement("UPDATE `"+stb.tableName+"` SET `fresh`=`fresh`+1,`mtime`=? WHERE `site_id`=? AND `room_id`=? AND `mate_id`=?");
            ins = db.prepareStatement("INSERT INTO `"+stb.tableName+"` (`fresh`,`mtime`,`site_id`,`room_id`,`mate_id`,`id`) VALUES (1, ?,?, ?,?, ?)");
        }

        public void store( Map info ) throws HongsException {
            ctb.insert(info);

            sid = (String) info.get("site_id");
            rid = (String) info.get("room_id");
        }

        public void fresh(String mid) throws HongsException {
            try {
                    upd.setObject(1, now);
                    upd.setObject(2, sid);
                    upd.setObject(3, rid);
                    upd.setObject(4, mid);
                if (upd.executeUpdate(  ) == 0) {
                    ins.setObject(1, now);
                    ins.setObject(2, sid);
                    ins.setObject(3, rid);
                    ins.setObject(4, mid);
                    ins.setObject(5, Core.newIdentity());
                    ins.executeUpdate(  );
                }
            }
            catch (SQLException ex) {
                throw new HongsException(ex);
            }
        }

    }

    private static void send(Map info) throws HongsException {
        String siteId = (String) info.get("site_id");
        String roomId = (String) info.get("room_id");
        String mateId = (String) info.get("mate_id");

        ChatSet     chat = new ChatSet();
        Set<String> mids = new HashSet();
        Async <Msg> sndr = getSender(  );
        Map<String, Set<Session> > sess ;

        // 保存消息
        chat.store(info);

        // 排除来源
        mids.add(mateId);

        String msg;
        msg = "{\"ok\":true,\"ern\":\"\",\"err\":\"\",\"msg\":\"\""
            + ",\"info\":"
            + Dawn.toString(info)
            + "}" ;

        // 发送消息
        sess = Dict.getValue(MasqueSocket.SESSIONS, Map.class, siteId, roomId);
        if (sess != null) {
            for(Map.Entry<String, Set<Session>> et : sess.entrySet()) {
                mids.add(et.getKey());
                Set<Session> ss = et.getValue();
                for(Session  se : ss) {
                    sndr.add(new MasqueTunnel.Msg(msg, se));
                }
            }
        }

        // 发送通知
        sess = Dict.getValue(MasqueSocket.SESSIONS, Map.class, siteId, "!");
        mids = getMateIds(siteId, roomId, mids);
        for(String mid : mids) {
            if (sess != null ) {
                Set<Session> ss = sess.get(mid);
            if (  ss != null ) {
                for(Session  se : ss) {
                    sndr.add(new MasqueTunnel.Msg(msg, se));
                }
                mids.remove(mid);
            }}

            // 未读数量
            try {
                chat.fresh (mid);
            }
            catch (HongsException ex) {
                CoreLogger.error (ex);
            }
        }

        if (mids.isEmpty()) {
            return;
        }

        msg = "{\"ok\":true,\"ern\":\"\",\"err\":\"\",\"msg\":\"\""
            + ",\"info\":"
            + Dawn.toString(info)
            + ",\"mids\":"
            + Dawn.toString(mids)
            + "}" ;

        // 自定通知
        sess = Dict.getValue(MasqueSocket.SESSIONS, Map.class, siteId, ".");
        if (sess != null) {
            for(Map.Entry<String, Set<Session>> et : sess.entrySet()) {
//              mids.add(et.getKey());
                Set<Session> ss = et.getValue();
                for(Session  se : ss) {
                    sndr.add(new MasqueTunnel.Msg(msg, se));
                }
            }
        }

        // HTTP回调
        else {
                String ur  = getNoteUrl(siteId);
                if (   ur != null   ) {
                    sndr.add(new MasqueTunnel.Msg(msg, ur));
                }
        }
    }

    private static void send(Msg info) throws HongsException {
        if (info.ses != null) {
            info.ses
                .getAsyncRemote (  )
                .sendText (info.msg);
        } else
        if (info.url != null) {
            if (info.url.startsWith("class://")) {
                try {
                    String cls = info.url.substring(8);
                    Object obj = Core.getInstance(cls);
                    ( (Consumer) obj).accept(info.msg);
                }
                catch (ClassCastException ex) {
                    throw new HongsException(ex);
                }
            }  else {
                try {
                    StringEntity enti = new StringEntity(info.msg, "UTF-8");
                    enti.setContentType("application/json");
                    enti.setContentEncoding("UTF-8");

                    HttpPost http = new HttpPost();
                    http.setURI(new URI(info.url));
                    http.setEntity(enti);

                    HttpResponse resp = HttpClients
                        .createDefault()
                        .execute( http );

                    // 调试输出
                    if (4 == (4 & Core.DEBUG)) {
                        int    code = resp.getStatusLine().getStatusCode();
                        String text = Syno.indent(EntityUtils.toString(resp.getEntity(),"UTF-8").trim());
                        CoreLogger.debug("Masque remote notify, URL: {}, MSG: {}, RSP({}): {}", info.url, info.msg, code, text);
                    }
                }
                catch (URISyntaxException ex) {
                    throw new HongsException(ex);
                }
                catch (IOException ex) {
                    throw new HongsException(ex);
                }
            }
        }
    }

    private static     String  getNoteUrl(String siteId)
    throws HongsException {
        DB   db = DB.getInstance("masque");
        Loop rs = db.with  ("site"       )
                    .field ("note_url"   )
                    .where ("id = ?" , siteId)
                    .limit (1)
                    .select( );
        for(Map ro : rs) {
            return (String) ro.get("note_url");
        }
        return null;
    }

    private static Set<String> getMateIds(String siteId, String roomId, Set<String> mateIds)
    throws HongsException {
        Set  ms = new HashSet();
        DB   db = DB.getInstance("masque");
        Loop rs = db.with  ("stat"       )
                    .field ("mate_id"    )
                    .where ("site_id=? AND room_id=? AND mate_id NOT IN (?)", siteId, roomId, mateIds)
                    .select( );
        for(Map ro : rs) {
            ms.add ( ro.get("mate_id")   );
        }
        return  ms ;
    }

}