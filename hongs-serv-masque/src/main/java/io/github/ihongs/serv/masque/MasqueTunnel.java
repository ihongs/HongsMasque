package io.github.ihongs.serv.masque;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.daemon.Async;
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
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

/**
 * 管道集合
 * @author hong
 */
public final class MasqueTunnel {

    /**
     * 后去接收管道对象
     * @return
     */
    public static Async<Map> getCeiver() {
        String name = Ceiver.class.getName();
        Ceiver inst = (Ceiver) Core.GLOBAL_CORE.get(name);
        if (inst == null) {
            CoreConfig conf = CoreConfig.getInstance("masque");
            inst =  new Ceiver( name,
                    conf.getProperty("core.masque.ceiver.max.tasks", Integer.MAX_VALUE),
                    conf.getProperty("core.masque.ceiver.max.servs", 1));
            Core.GLOBAL_CORE.set(name, inst);
        }
        return inst;
    }

    /**
     * 获取发送管道对象
     * @return
     */
    public static Async<Msg> getSender() {
        String name = Sender.class.getName();
        Sender inst = (Sender) Core.GLOBAL_CORE.get(name);
        if (inst == null) {
            CoreConfig conf = CoreConfig.getInstance("masque");
            inst =  new Sender( name,
                    conf.getProperty("core.masque.sender.max.tasks", Integer.MAX_VALUE),
                    conf.getProperty("core.masque.sender.max.servs", 2));
            Core.GLOBAL_CORE.set(name, inst);
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

        private Ceiver(String name, int maxTasks, int maxServs) {
            super(name, maxTasks, maxServs);
        }

        @Override
        public void run(Map info) {
            try {
                MasqueTunnel.send(info);
            }
            catch (CruxException e) {
                CoreLogger.error(e);
            }
            finally {
                Core.getInstance( ).reset( );
            }
        }

    }

    /**
     * 发送管道
     */
    private static class Sender
    extends Async<Msg>
    implements Core.Singleton {

        private Sender(String name, int maxTasks, int maxServs) {
            super(name, maxTasks, maxServs);
        }

        @Override
        public void run(Msg info) {
            try {
                MasqueTunnel.send(info);
            }
            catch (CruxException e) {
                CoreLogger.error(e);
            }
            finally {
                Core.getInstance( ).reset( );
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

        public ChatSet() throws CruxException {
            now = System.currentTimeMillis();
            db  = DB.getInstance( "masque" );
                  db.open();
            ctb = db.getTable("chat");
            stb = db.getTable("stat");
            upd = db.prepare("UPDATE `"+stb.tableName+"` SET `fresh`=`fresh`+1,`mtime`=? WHERE `site_id`=? AND `room_id`=? AND `mate_id`=?");
            ins = db.prepare("INSERT INTO `"+stb.tableName+"` (`fresh`,`mtime`,`site_id`,`room_id`,`mate_id`,`id`) VALUES (1, ?,?, ?,?, ?)");
        }

        public void store( Map info ) throws CruxException {
            ctb.insert(info);

            sid = (String) info.get("site_id");
            rid = (String) info.get("room_id");
        }

        public void fresh(String mid) throws CruxException {
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
                throw new CruxException(ex);
            }
        }

    }

    private static void send(Map info) throws CruxException {
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
            + Dist.toString(info)
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
            catch (CruxException ex) {
                CoreLogger.error(ex);
            }
        }

        if (mids.isEmpty()) {
            return;
        }

        msg = "{\"ok\":true,\"ern\":\"\",\"err\":\"\",\"msg\":\"\""
            + ",\"info\":"
            + Dist.toString(info)
            + ",\"mids\":"
            + Dist.toString(mids)
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

    private static void send(Msg info) throws CruxException {
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
                    throw new CruxException(ex);
                }
            }  else {
                try (
                    CloseableHttpClient client = HttpClientBuilder.create().build()
                ) {
                    ClassicHttpRequest request = ClassicRequestBuilder
                        .post(new URI(info.url))
                        .setEntity(new StringEntity(info.msg, ContentType.APPLICATION_JSON))
                        .build();
                    client.execute(request, (resp) -> {
                        // 调试输出
                        if (4 == (4 & Core.DEBUG)) {
                            int    code = resp.getCode();
                            String text = Syno.indent(EntityUtils.toString(resp.getEntity(),"UTF-8").trim());
                            CoreLogger.debug("Masque remote notify, URL: {}, MSG: {}, RSP({}): {}", info.url, info.msg, code, text);
                        }
                        return null;
                    });
                }
                catch (URISyntaxException ex) {
                    throw new CruxException(ex);
                }
                catch (IOException ex) {
                    throw new CruxException(ex);
                }
            }
        }
    }

    private static     String  getNoteUrl(String siteId)
    throws CruxException {
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
    throws CruxException {
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