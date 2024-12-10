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

    }

    private static class SimpleTunnel implements Tunnel {

        @Override
        public void accept(Map msg) {
            MasqueTunnel.getCeiver().accept(msg);
        }

    }

    private Masque () {}

}
