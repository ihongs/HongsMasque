package io.github.ihongs.serv.masque;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 消息总管
 *
 * 可在 masque.properties 中配置 core.masque.tunnel.builder 通道构造工厂类, 需实现 Supplier&lt;Tunnel&gt; 接口.
 *
 * @author Hongs
 */
public class Masque {

    /**
     * 全局通知通道
     */
    public static String ROOM_ALL = "!all";

    /**
     * 离线通知通道
     */
    public static String ROOM_OFF = "!off";

    /**
     * 获取通道
     * @return
     */
    public static Tunnel getTunnel() {
        return Core.getInterior().got(Tunnel.class.getName(), () -> {
            CoreConfig cc = CoreConfig.getInstance("masque");
            String c = cc.getProperty( "core.masque.tunnel.builder" );
            if (c != null && ! c.isEmpty()) {
                return ((Supplier<Tunnel>) Core.newInstance(c)).get();
            }
            return new SimpleTunnel();
        });
    }

    /**
     * 消息通道
     */
    public static interface Tunnel {

        /**
         * 写入消息
         * @param msg chat 数据
         */
        public void accept(Map msg);

        /**
         * 移除会话
         * @param sid Site ID
         * @param mid Mate ID
         * @param rid Room ID
         */
        public void remove(String sid, String mid, String rid);

        /**
         * 消息接口
         * @param sid Site ID
         * @param mid Mate ID
         * @param rid Room ID
         * @return
         */
        public String socket(String sid, String mid, String rid);

    }

    private static class SimpleTunnel implements Tunnel {

        @Override
        public void accept(Map msg) {
            MasqueTunnel.getCeiver().accept(msg);
        }

        @Override
        public void remove(String sid, String mid, String rid) {
            MasqueSocket.delSessions(sid, rid, mid);
        }

        @Override
        public String socket(String sid, String mid, String rid) {
            return Core.SERVER_HREF.get()+Core.SERVER_PATH.get()
                 + "/centre/masque/socket/"+sid+"/"+mid+"/"+rid;
        }

    }

    private Masque () {}

}
